package com.example.domain.engine

import android.util.Log
import com.example.data.db.StoryDao
import com.example.data.model.NpcEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * Identitätsgedächtnis: hält Figuren über die gesamte Geschichte hinweg konsistent.
 *
 * Bisher lagen NPCs nur als Snapshot im Checkpoint und wurden von der Extraktion jeden Zug neu
 * erzeugt. Dabei driften genau die Merkmale, die einen Charakter ausmachen — Haarfarbe, Augen,
 * Statur. Diese Engine friert das Erscheinungsbild beim ersten Auftreten ein und lässt es nur
 * dann verändern, wenn die Erzählung die Änderung ausdrücklich herbeigeführt hat.
 */
class NpcEngine(
  private val storyDao: StoryDao
) {

  companion object {
    private const val TAG = "NpcEngine"

    /** Anzahl persönlicher Erinnerungen, die pro anwesender Figur in den Prompt wandern. */
    const val MEMORIES_PER_NPC = 3
  }

  /**
   * Löst einen vom Modell gelieferten Namen gegen die kanonischen Figuren auf.
   * Ersetzt den früheren exakten Namensvergleich, der bei "Lena" vs. "Lena Vogt" scheiterte.
   */
  fun resolve(npcs: List<NpcEntity>, name: String): NpcEntity? {
    if (name.isBlank()) return null
    return npcs.firstOrNull { it.canonicalName.equals(name.trim(), ignoreCase = true) }
      ?: npcs.firstOrNull { it.matchesName(name) }
  }

  /**
   * Gleicht die von der Extraktion gelieferten Figuren mit dem Bestand ab.
   *
   * Neue Figuren werden angelegt, bekannte aktualisiert — wobei [NpcEntity.appearance] und
   * [NpcEntity.personality] nur beim ersten Mal geschrieben werden. Gibt die Namen aller Figuren
   * zurück, für die in dieser Runde etwas Erinnerungswürdiges geschah.
   */
  suspend fun syncFromExtraction(
    storyId: Long,
    npcsArray: JSONArray?,
    currentDay: Int
  ): List<NpcTurnKnowledge> {
    if (npcsArray == null || npcsArray.length() == 0) return emptyList()

    val existing = storyDao.getNpcs(storyId).toMutableList()
    val knowledge = mutableListOf<NpcTurnKnowledge>()

    for (i in 0 until npcsArray.length()) {
      val obj = npcsArray.optJSONObject(i) ?: continue
      val name = obj.optString("name").trim()
      if (name.isBlank() || name.equals("Unbekannt", ignoreCase = true)) continue

      val match = resolve(existing, name)
      val npcId: Long

      if (match == null) {
        val created = NpcEntity(
          storyId = storyId,
          canonicalName = name,
          gender = normalizeGender(obj.optString("gender")),
          appearance = obj.optString("appearance").trim(),
          personality = obj.optString("personality").trim(),
          firstMetDay = currentDay,
          lastSeenDay = currentDay,
          relationship = obj.optString("relationship_to_player", "Neutral").ifBlank { "Neutral" },
          currentMood = obj.optString("current_mood", "Ruhig").ifBlank { "Ruhig" },
          condition = obj.optString("condition").trim(),
          outfit = obj.optString("outfit").trim(),
          status = obj.optString("status", "Anwesend").ifBlank { "Anwesend" },
          isAlive = obj.optBoolean("is_alive", true)
        )
        npcId = storyDao.insertNpc(created)
        existing.add(created.copy(id = npcId))
        Log.i(TAG, "Neue Figur angelegt: $name (Tag $currentDay)")
      } else {
        npcId = match.id
        val updated = applyUpdate(match, obj, currentDay)
        if (updated != match) {
          storyDao.updateNpc(updated)
          existing[existing.indexOf(match)] = updated
        }
      }

      val learned = obj.optString("knowledge").trim()
      if (learned.isNotBlank()) {
        knowledge.add(NpcTurnKnowledge(npcId = npcId, npcName = name, text = learned))
      }
    }

    return knowledge
  }

  /**
   * Überträgt die Felder einer Runde auf eine bekannte Figur.
   *
   * Aussehen und Persönlichkeit sind bewusst schreibgeschützt: Sie werden nur befüllt, wenn sie
   * noch leer sind, oder wenn die Extraktion eine belegte Änderung meldet. Genau das verhindert,
   * dass eine Figur nach fünfzig Zügen plötzlich andere Haare hat.
   */
  private fun applyUpdate(current: NpcEntity, obj: JSONObject, currentDay: Int): NpcEntity {
    val extractedAppearance = obj.optString("appearance").trim()
    val appearanceChanged = obj.optBoolean("appearance_changed", false)
    val changeReason = obj.optString("appearance_change_reason").trim()

    val newAppearance = when {
      current.appearance.isBlank() -> extractedAppearance
      appearanceChanged && changeReason.isNotBlank() && extractedAppearance.isNotBlank() ->
        "${current.appearance} | Veränderung an Tag $currentDay: $changeReason"
      else -> current.appearance
    }

    val extractedPersonality = obj.optString("personality").trim()
    val newPersonality = if (current.personality.isBlank()) extractedPersonality else current.personality

    val status = obj.optString("status", current.status).ifBlank { current.status }

    return current.copy(
      appearance = newAppearance,
      personality = newPersonality,
      relationship = obj.optString("relationship_to_player", current.relationship).ifBlank { current.relationship },
      currentMood = obj.optString("current_mood", current.currentMood).ifBlank { current.currentMood },
      condition = obj.optString("condition", current.condition).ifBlank { current.condition },
      outfit = obj.optString("outfit", current.outfit).ifBlank { current.outfit },
      status = status,
      isAlive = obj.optBoolean("is_alive", current.isAlive),
      lastSeenDay = if (status.equals("Anwesend", ignoreCase = true)) currentDay else current.lastSeenDay,
      updatedAt = System.currentTimeMillis()
    )
  }

  private fun normalizeGender(raw: String): String {
    val clean = raw.trim().uppercase()
    return if (clean.startsWith("M") || clean.contains("MÄNN") || clean.contains("MANN")) "MALE" else "FEMALE"
  }

  /**
   * Baut die Figuren-Profile für den Prompt: kanonisches Aussehen, Beziehung und die
   * persönlichen Erinnerungen der anwesenden Figuren.
   */
  suspend fun buildProfiles(
    storyId: Long,
    memoryEngine: MemoryEngine,
    currentInGameTime: String,
    embeddingModel: String,
    queryText: String
  ): List<String> {
    val npcs = storyDao.getNpcs(storyId)
    if (npcs.isEmpty()) return emptyList()

    val present = npcs.filter { it.status.equals("Anwesend", ignoreCase = true) && it.isAlive }
    val relevant = present.ifEmpty { npcs.takeLast(3) }

    return relevant.map { npc ->
      buildString {
        append("- ${npc.canonicalName}")
        if (npc.appearance.isNotBlank()) append(" | Aussehen: ${npc.appearance}")
        if (npc.personality.isNotBlank()) append(" | Wesen: ${npc.personality}")
        append(" | Beziehung: ${npc.relationship}")
        append(" | Stimmung: ${npc.currentMood}")
        // Getrennt von der Stimmung im Prompt, damit das Modell Verfassung und Laune nicht
        // vermischt — sonst wird aus "erschöpft" eine schlechte Laune statt einer Schwäche.
        if (npc.condition.isNotBlank()) append(" | Körperliche Verfassung: ${npc.condition}")
        if (!npc.isAlive) append(" | VERSTORBEN")
        else if (!npc.status.equals("Anwesend", ignoreCase = true)) append(" | Abwesend")
      }
    } + buildNpcMemoryLines(storyId, relevant, memoryEngine, currentInGameTime, embeddingModel, queryText)
  }

  /**
   * Persönliche Erinnerungen der anwesenden Figuren — die Grundlage dafür, dass eine Gerettete
   * Tage später von sich aus auf die Rettung zu sprechen kommen kann.
   */
  private suspend fun buildNpcMemoryLines(
    storyId: Long,
    npcs: List<NpcEntity>,
    memoryEngine: MemoryEngine,
    currentInGameTime: String,
    embeddingModel: String,
    queryText: String
  ): List<String> {
    if (npcs.isEmpty()) return emptyList()

    val lines = memoryEngine.retrieveForNpcs(
      storyId = storyId,
      npcIds = npcs.map { it.id }.toSet(),
      queryText = queryText,
      currentInGameTime = currentInGameTime,
      embeddingModel = embeddingModel,
      limitPerNpc = MEMORIES_PER_NPC
    )
    return if (lines.isEmpty()) emptyList() else listOf("  Persönliche Erinnerungen dieser Figuren:") + lines
  }

  data class NpcTurnKnowledge(val npcId: Long, val npcName: String, val text: String)
}
