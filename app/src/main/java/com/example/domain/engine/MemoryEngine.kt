package com.example.domain.engine

import android.util.Log
import com.example.data.api.GeminiClient
import com.example.data.db.StoryDao
import com.example.data.model.MemoryEntity
import com.example.data.model.MemoryKind
import com.example.domain.model.TimeAnchor
import org.json.JSONArray
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Episodisches Gedächtnis: schreibt atomare Erinnerungen und findet sie über Monate hinweg wieder.
 *
 * Ersetzt die frühere Suche in [StoryTurnEngine], die drei strukturelle Schwächen hatte:
 * 1. Sie lud bei jedem Zug ALLE Embeddings und parste sie neu als JSON.
 * 2. Sie bettete ganze Game-Master-Antworten als eine Erinnerung ein, wodurch die enthaltenen
 *    Fakten in einem einzigen Vektor verschmierten.
 * 3. Sie nahm stur die fünf ähnlichsten Treffer — nach hundert Tagen gewinnen dabei immer die
 *    jüngsten Erinnerungen, und alte Ereignisse werden unauffindbar.
 */
class MemoryEngine(
  private val geminiClient: GeminiClient,
  private val storyDao: StoryDao
) {

  companion object {
    private const val TAG = "MemoryEngine"

    /** Anzahl der Erinnerungen, die pro Zug in den Prompt wandern. */
    const val MAX_MEMORIES = 6

    /** Treffer unterhalb dieser Ähnlichkeit sind Rauschen und würden den Prompt verwässern. */
    const val MIN_SIMILARITY = 0.30f

    /** Ab diesem Alter gilt eine Erinnerung als "alt" und konkurriert um die Reserveplätze. */
    const val OLD_MEMORY_DAY_THRESHOLD = 7

    /**
     * Wie viele fehlende Einbettungen ein einzelner Zug nachträgt. Drei, weil das kostenlose
     * Kontingent bei wenigen Anfragen pro Modell und Tag liegt und ein Zug ohnehin schon
     * mehrere davon verbraucht. Der Rückstand wird so über die nächsten Züge abgetragen,
     * statt einen einzigen Zug lahmzulegen.
     */
    const val MAX_BACKFILL_PER_RETRIEVE = 3

    /**
     * Fest reservierte Plätze für alte Erinnerungen.
     *
     * Der eigentliche Grund für diese Klasse: Ohne Reservierung verdrängen die Ereignisse der
     * letzten Tage rein statistisch alles Ältere, weil es davon schlicht mehr gibt. Damit wäre
     * ein prägender Moment von Tag 1 an Tag 100 faktisch verloren.
     */
    const val RESERVED_OLD_SLOTS = 2

    /** Obergrenze für den gesamten Erinnerungsblock im Prompt. */
    const val MEMORY_CHAR_BUDGET = 4000

    /** Backfill-Erinnerungen aus Altnachrichten werden gekürzt, um den Prompt nicht zu sprengen. */
    private const val LEGACY_MEMORY_MAX_CHARS = 600

    fun floatsToBytes(vector: FloatArray): ByteArray {
      val buffer = ByteBuffer.allocate(vector.size * 4).order(ByteOrder.LITTLE_ENDIAN)
      vector.forEach { buffer.putFloat(it) }
      return buffer.array()
    }

    fun bytesToFloats(bytes: ByteArray): FloatArray {
      val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
      return FloatArray(bytes.size / 4) { buffer.getFloat() }
    }

    fun parseEmbeddingJson(json: String): FloatArray {
      return try {
        val array = JSONArray(json)
        FloatArray(array.length()) { array.getDouble(it).toFloat() }
      } catch (_: Exception) {
        FloatArray(0)
      }
    }

    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
      // Unterschiedliche Längen bedeuten unterschiedliche Embedding-Modelle und damit
      // unterschiedliche Vektorräume. Ein Vergleich der ersten N Komponenten liefert dann eine
      // Zahl, die nach Ähnlichkeit aussieht, aber keine ist. Lieber gar kein Treffer.
      if (a.size != b.size) return 0f
      val size = a.size
      if (size == 0) return 0f
      var dot = 0f
      var normA = 0f
      var normB = 0f
      for (i in 0 until size) {
        dot += a[i] * b[i]
        normA += a[i] * a[i]
        normB += b[i] * b[i]
      }
      if (normA == 0f || normB == 0f) return 0f
      return dot / (kotlin.math.sqrt(normA) * kotlin.math.sqrt(normB))
    }

    /**
     * Bewertet einen Treffer. Die Kosinus-Ähnlichkeit bleibt bewusst dominant — die Boni
     * verschieben die Reihenfolge nur innerhalb ähnlich guter Treffer.
     */
    fun scoreMemory(
      similarity: Float,
      memoryDay: Int,
      currentDay: Int,
      kind: String,
      textLower: String,
      presentNamesLower: List<String>
    ): Float {
      val age = (currentDay - memoryDay).coerceAtLeast(0)
      val recencyBonus = 0.06f / (1f + age / 5f)
      val nameBonus = if (presentNamesLower.any { it.isNotBlank() && textLower.contains(it) }) 0.12f else 0f
      val kindBonus = when (kind) {
        MemoryKind.MILESTONE -> 0.05f
        MemoryKind.DAY_SUMMARY -> 0.03f
        MemoryKind.NPC -> 0.02f
        else -> 0f
      }
      return similarity + recencyBonus + nameBonus + kindBonus
    }

    /**
     * Wählt aus bewerteten Kandidaten aus und hält dabei [RESERVED_OLD_SLOTS] für Erinnerungen
     * frei, die älter als [OLD_MEMORY_DAY_THRESHOLD] Tage sind.
     */
    fun selectWithAgeDiversity(
      scored: List<ScoredMemory>,
      currentDay: Int,
      limit: Int = MAX_MEMORIES
    ): List<ScoredMemory> {
      if (scored.isEmpty()) return emptyList()
      val ranked = scored.sortedByDescending { it.score }

      val freshSlots = (limit - RESERVED_OLD_SLOTS).coerceAtLeast(1)
      val selected = ranked.take(freshSlots).toMutableList()

      val isOld = { m: ScoredMemory -> currentDay - m.memory.dayNumber > OLD_MEMORY_DAY_THRESHOLD }
      ranked.asSequence()
        .filter { isOld(it) && it !in selected }
        .take(limit - selected.size)
        .forEach { selected.add(it) }

      // Falls es nicht genug alte Erinnerungen gibt, mit den nächstbesten auffüllen.
      if (selected.size < limit) {
        ranked.asSequence()
          .filter { it !in selected }
          .take(limit - selected.size)
          .forEach { selected.add(it) }
      }

      return selected.sortedBy { it.memory.dayNumber }
    }
  }

  data class ScoredMemory(val memory: MemoryEntity, val score: Float)

  // Vektor-Cache für die aktuell gespielte Story. Ersetzt das frühere JSON-Parsing pro Zug.
  private var cachedStoryId: Long? = null
  private val vectorCache = mutableMapOf<Long, FloatArray>()
  private var cachedMemories: List<MemoryEntity> = emptyList()

  /**
   * Stellt sicher, dass das Gedächtnis der Story geladen ist, und übernimmt einmalig die
   * Embeddings aus der alten `messages.embeddingJson`-Spalte. Ohne diese Übernahme würden
   * laufende Geschichten beim Update ihr gesamtes Gedächtnis verlieren.
   */
  suspend fun ensureStoryIndexed(storyId: Long) {
    if (cachedStoryId == storyId && cachedMemories.isNotEmpty()) return

    if (storyDao.countMemories(storyId) == 0) {
      backfillFromLegacyMessages(storyId)
    }
    loadCache(storyId)
  }

  private suspend fun backfillFromLegacyMessages(storyId: Long) {
    val legacy = storyDao.getMessagesWithEmbeddings(storyId)
    if (legacy.isEmpty()) return

    Log.i(TAG, "Übernehme ${legacy.size} Alt-Embeddings in das episodische Gedächtnis")
    for (message in legacy) {
      val json = message.embeddingJson ?: continue
      val vector = parseEmbeddingJson(json)
      if (vector.isEmpty()) continue

      // Der Text wird gekürzt, das Embedding stammt aber vom vollständigen Text. Für Altdaten
      // ist das hinnehmbar: der Vektor bleibt aussagekräftig, nur die Anzeige ist knapper.
      storyDao.insertMemory(
        MemoryEntity(
          storyId = storyId,
          kind = MemoryKind.EVENT,
          dayNumber = TimeAnchor.parseDayNumber(message.inGameTimeTag),
          inGameTime = message.inGameTimeTag ?: "",
          text = message.content.take(LEGACY_MEMORY_MAX_CHARS),
          embedding = floatsToBytes(vector),
          createdAt = message.timestamp
        )
      )
    }

    // Die Alt-Spalte hat ihren Zweck erfüllt und wird nie wieder gelesen. Bleibt sie gefüllt,
    // zieht die Oberfläche bei jeder neuen Nachricht Dutzende Megabyte toter Vektoren mit.
    storyDao.clearLegacyEmbeddings(storyId)
  }

  private suspend fun loadCache(storyId: Long) {
    vectorCache.clear()
    // Der Vektor wird in den vectorCache umgezogen und in der Entity verworfen. Bliebe er
    // stehen, läge jede Zahl doppelt im Speicher — als BLOB und als FloatArray. Bei 2000
    // Erinnerungen sind das 6 MB zuviel, die bei jedem Geschichtenwechsel neu entstehen.
    cachedMemories = storyDao.getMemories(storyId).map { memory ->
      memory.embedding?.let { vectorCache[memory.id] = bytesToFloats(it) }
      memory.copy(embedding = null)
    }
    cachedStoryId = storyId
  }

  /** Verwirft den Cache, etwa nach einem Zweig-Wechsel oder einer Historien-Kürzung. */
  fun invalidate() {
    cachedStoryId = null
    cachedMemories = emptyList()
    vectorCache.clear()
  }

  /**
   * Legt eine neue Erinnerung an und bettet sie ein. Fehlt der Schlüssel oder ist das Gerät
   * offline, wird die Erinnerung trotzdem gespeichert — nur eben ohne Vektor.
   */
  suspend fun recordMemory(
    storyId: Long,
    kind: String,
    text: String,
    dayNumber: Int,
    inGameTime: String,
    turnNumber: Int,
    npcId: Long? = null,
    embeddingModel: String
  ): Long {
    if (text.isBlank()) return 0L

    val vector = try {
      geminiClient.generateEmbedding(text, embeddingModel, taskType = "RETRIEVAL_DOCUMENT")
        ?.let { parseEmbeddingJson(it) }
    } catch (e: Exception) {
      Log.w(TAG, "Einbettung fehlgeschlagen, Erinnerung wird ohne Vektor gespeichert: ${e.message}")
      null
    }

    val entity = MemoryEntity(
      storyId = storyId,
      kind = kind,
      dayNumber = dayNumber,
      inGameTime = inGameTime,
      turnNumber = turnNumber,
      npcId = npcId,
      text = text.trim(),
      embedding = vector?.takeIf { it.isNotEmpty() }?.let { floatsToBytes(it) }
    )
    val id = storyDao.insertMemory(entity)

    if (cachedStoryId == storyId) {
      // Wie in loadCache: der Vektor gehört in den vectorCache, nicht zusätzlich in die Entity.
      cachedMemories = cachedMemories + entity.copy(id = id, embedding = null)
      vector?.takeIf { it.isNotEmpty() }?.let { vectorCache[id] = it }
    }
    return id
  }

  /**
   * Holt Einbettungen nach, die beim Anlegen fehlgeschlagen sind.
   *
   * `recordMemory` speichert eine Erinnerung auch dann, wenn die Einbettung scheitert -- richtig
   * so, der Text ist wertvoller als der Vektor. Nur bleibt sie damit dauerhaft unauffindbar: Die
   * Suche überspringt jede Erinnerung ohne Vektor, und bisher hat sie nie wieder jemand
   * angefasst. Ein Tag ohne Netz löschte das Gedächtnis dieses Tages faktisch aus.
   *
   * Bewusst gedeckelt: Das kostenlose Kontingent liegt bei wenigen Anfragen pro Modell und Tag.
   * Ein Rückstand von hundert Erinnerungen darf nicht das Kontingent des Spielzugs aufbrauchen,
   * der ihn gerade abarbeitet -- er verteilt sich stattdessen über die nächsten Züge.
   */
  private suspend fun backfillMissingEmbeddings(embeddingModel: String) {
    val storyId = cachedStoryId ?: return
    var nachgetragen = 0

    for (memory in cachedMemories) {
      if (nachgetragen >= MAX_BACKFILL_PER_RETRIEVE) return
      if (vectorCache.containsKey(memory.id)) continue

      val vector = try {
        geminiClient.generateEmbedding(memory.text, embeddingModel, taskType = "RETRIEVAL_DOCUMENT")
          ?.let { parseEmbeddingJson(it) }
      } catch (e: Exception) {
        Log.w(TAG, "Nachträgliche Einbettung fehlgeschlagen: ${e.message}")
        return
      }

      // Scheitert eine, scheitern die nächsten mit hoher Wahrscheinlichkeit auch (Kontingent
      // erschöpft, Modell abgekündigt). Weiterzuprobieren verbrennt nur den Rest.
      if (vector == null || vector.isEmpty()) return

      val bytes = floatsToBytes(vector)
      storyDao.updateMemoryEmbedding(memory.id, bytes)
      // Nur der Cache dieser Geschichte darf befüllt werden -- wechselt der Spieler mitten im
      // Nachtragen, gehört der Vektor nicht mehr hierher.
      if (cachedStoryId == storyId) vectorCache[memory.id] = vector
      nachgetragen++
    }
  }

  /**
   * Sucht die relevantesten Erinnerungen und rendert sie mit Tagesnummer und Abstand zu heute.
   *
   * [queryText] sollte nicht nur die Spieler-Aktion enthalten, sondern auch Ort und anwesende
   * Figuren: Eine Frage wie "Wie geht es dir?" hat für sich genommen keine semantische Nähe zu
   * dem Ereignis, auf das sie sich bezieht.
   */
  suspend fun retrieve(
    storyId: Long,
    queryText: String,
    currentInGameTime: String,
    presentNpcNames: List<String>,
    embeddingModel: String,
    excludeTurnNumbers: Set<Int> = emptySet()
  ): List<String> {
    ensureStoryIndexed(storyId)
    if (cachedMemories.isEmpty() || queryText.isBlank()) return emptyList()

    val queryJson = try {
      geminiClient.generateEmbedding(queryText, embeddingModel, taskType = "RETRIEVAL_QUERY")
    } catch (e: Exception) {
      Log.w(TAG, "Query-Einbettung fehlgeschlagen: ${e.message}")
      null
    } ?: return emptyList()

    val queryVector = parseEmbeddingJson(queryJson)
    if (queryVector.isEmpty()) return emptyList()

    // Die Query-Einbettung hat gerade funktioniert -- also liegt ein Schlüssel vor und das Gerät
    // ist online. Genau jetzt, und nur jetzt, lohnt der Versuch, Liegengebliebenes nachzuholen.
    backfillMissingEmbeddings(embeddingModel)

    val currentDay = TimeAnchor.parseDayNumber(currentInGameTime)
    val presentNamesLower = presentNpcNames.map { it.lowercase().trim() }.filter { it.length >= 3 }

    val scored = cachedMemories.mapNotNull { memory ->
      if (memory.turnNumber in excludeTurnNumbers) return@mapNotNull null
      val vector = vectorCache[memory.id] ?: return@mapNotNull null
      val similarity = cosineSimilarity(queryVector, vector)
      if (similarity < MIN_SIMILARITY) return@mapNotNull null

      ScoredMemory(
        memory = memory,
        score = scoreMemory(
          similarity = similarity,
          memoryDay = memory.dayNumber,
          currentDay = currentDay,
          kind = memory.kind,
          textLower = memory.text.lowercase(),
          presentNamesLower = presentNamesLower
        )
      )
    }

    val selected = selectWithAgeDiversity(scored, currentDay)

    val rendered = mutableListOf<String>()
    var usedChars = 0
    for (item in selected) {
      val age = TimeAnchor.describeAge(item.memory.dayNumber, currentDay)
      val line = "[Tag ${item.memory.dayNumber} - $age] ${item.memory.text}"
      if (usedChars + line.length > MEMORY_CHAR_BUDGET) break
      rendered.add(line)
      usedChars += line.length
    }
    return rendered
  }

  /**
   * Persönliche Erinnerungen bestimmter Figuren, nach Relevanz zur aktuellen Situation.
   *
   * Nutzt denselben Cache wie [retrieve]; ohne Query-Vektor wird auf "die jüngsten" zurückgefallen,
   * damit die Figuren auch offline nicht gedächtnislos wirken.
   */
  suspend fun retrieveForNpcs(
    storyId: Long,
    npcIds: Set<Long>,
    queryText: String,
    currentInGameTime: String,
    embeddingModel: String,
    limitPerNpc: Int = 3
  ): List<String> {
    if (npcIds.isEmpty()) return emptyList()
    ensureStoryIndexed(storyId)

    val candidates = cachedMemories.filter { it.kind == MemoryKind.NPC && it.npcId in npcIds }
    if (candidates.isEmpty()) return emptyList()

    val currentDay = TimeAnchor.parseDayNumber(currentInGameTime)
    val queryVector = try {
      geminiClient.generateEmbedding(queryText, embeddingModel, taskType = "RETRIEVAL_QUERY")
        ?.let { parseEmbeddingJson(it) }
    } catch (_: Exception) {
      null
    }

    return candidates
      .groupBy { it.npcId }
      .flatMap { (_, memories) ->
        val ranked = if (queryVector != null && queryVector.isNotEmpty()) {
          memories.sortedByDescending { memory ->
            vectorCache[memory.id]?.let { cosineSimilarity(queryVector, it) } ?: -1f
          }
        } else {
          memories.sortedByDescending { it.dayNumber }
        }
        ranked.take(limitPerNpc)
      }
      .sortedBy { it.dayNumber }
      .map { "  · [Tag ${it.dayNumber} - ${TimeAnchor.describeAge(it.dayNumber, currentDay)}] ${it.text}" }
  }

  /**
   * Die letzten abgeschlossenen Tage wörtlich — die Schicht zwischen dem kurzen Arbeitsfenster
   * und der semantischen Suche.
   */
  suspend fun getRecentDaySummaries(storyId: Long, limit: Int = 3): List<String> {
    return storyDao.getRecentMemoriesOfKind(storyId, MemoryKind.DAY_SUMMARY, limit)
      .sortedBy { it.dayNumber }
      .map { "[Tag ${it.dayNumber}] ${it.text}" }
  }

  /** Die neuesten Meilensteine wörtlich; ältere erreicht das Modell über [retrieve]. */
  suspend fun getRecentMilestones(storyId: Long, limit: Int = 10): List<String> {
    return storyDao.getRecentMemoriesOfKind(storyId, MemoryKind.MILESTONE, limit)
      .sortedBy { it.dayNumber }
      .map { "[Tag ${it.dayNumber}] ${it.text}" }
  }
}
