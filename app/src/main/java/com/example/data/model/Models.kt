package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

/**
 * Die einzigen Modell-Ids, die in der App fest stehen dürfen.
 *
 * Sie dienen als Startwert für neue Geschichten und als Notnagel, wenn die Live-Abfrage der
 * Modellliste scheitert (kein Netz, kein Schlüssel). Alle drei wurden zuletzt gegen die echte
 * API geprüft und antworten mit HTTP 200.
 *
 * Warum das überhaupt eine eigene Stelle braucht: Googles ListModels kennzeichnet abgekündigte
 * Modelle nicht. `gemini-2.5-flash` steht dort mit vollständiger Beschreibung und liefert beim
 * Aufruf trotzdem HTTP 404. Über die App verstreute Id-Literale werden dadurch früher oder
 * später alle falsch — hier ist die einzige Stelle, die dann angefasst werden muss.
 */
object GeminiDefaults {
  const val CHAT_MODEL = "gemini-3.8-flash"
  const val TRANSCRIPTION_MODEL = "gemini-3.5-transcribe"
  const val EMBEDDING_MODEL = "gemini-embedding-001"
}

@Entity(tableName = "stories")
data class StoryEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val title: String,
  val systemPrompt: String = "", // Empty means: use global default prompt
  val genre: String = "Dark Noir & Mystery",
  val perspective: String = "Zweite Person (Du)",
  val selectedModel: String = GeminiDefaults.CHAT_MODEL,
  val selectedEmbeddingModel: String = GeminiDefaults.EMBEDDING_MODEL,
  val selectedTranscriptionModel: String = GeminiDefaults.TRANSCRIPTION_MODEL,
  val temperature: Float = 0.85f,
  val supportsTemperature: Boolean = true,
  val thinkingLevel: String = "MEDIUM", // "LOW", "MEDIUM", "HIGH", "OFF"
  val thinkingBudget: Int = 2048, // Legacy für 2.5: 0 = Aus, 1024 = Gering, 2048 = Standard, 4096 = Tief, 8192 = Max
  val adultContentEnabled: Boolean = true,
  val isArchived: Boolean = false,
  val playTimeSeconds: Long = 0L, // Total tracked play time in seconds
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
)

data class GeminiModelInfo(
  val id: String,
  val displayName: String,
  val description: String,
  val supportsTemperature: Boolean = true,
  val defaultTemperature: Float = 0.85f,
  val isThinkingModel: Boolean = true,
  val usesThinkingLevel: Boolean = true, // true für Gemini 3+, false für 2.5
  val supportedThinkingLevels: List<String> = listOf("LOW", "MEDIUM", "HIGH")
)

data class GeminiModelCatalog(
  val chatModels: List<GeminiModelInfo> = emptyList(),
  val embeddingModels: List<GeminiModelInfo> = emptyList(),
  val transcriptionModels: List<GeminiModelInfo> = emptyList(),
  /** true, wenn die Liste live von Google kam. false = hartkodierte Fallback-Liste. */
  val isLive: Boolean = false
)

@Entity(tableName = "checkpoints", indices = [Index("storyId")])
data class CheckpointEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val storyId: Long,
  val turnNumber: Int,
  val inGameTime: String,
  val location: String,
  val weather: String = "Klar",
  val playerOutfit: String,
  val playerInventory: String = "[]",
  val playerCondition: String = "Unverletzt",
  val npcsJson: String = "[]",
  val milestonesJson: String = "[]", // Langzeitgedächtnis für prägende Momente über Tage hinweg
  val previousEventsSummary: String = "",
  val rawStateJson: String = "",
  val timestamp: Long = System.currentTimeMillis()
) {
  fun getNpcList(): List<NpcInfo> {
    val list = mutableListOf<NpcInfo>()
    try {
      val array = JSONArray(npcsJson)
      for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        list.add(
          NpcInfo(
            name = obj.optString("name", "Unbekannt"),
            outfit = obj.optString("outfit", "-"),
            relationshipToPlayer = obj.optString("relationship_to_player", "Neutral"),
            currentMood = obj.optString("current_mood", "Ruhig"),
            status = obj.optString("status", "Anwesend"),
            condition = obj.optString("condition", ""),
            gender = obj.optString("gender", "")
          )
        )
      }
    } catch (_: Exception) { }
    return list
  }

  fun getInventoryList(): List<String> {
    val list = mutableListOf<String>()
    try {
      val array = JSONArray(playerInventory)
      for (i in 0 until array.length()) {
        list.add(array.getString(i))
      }
    } catch (_: Exception) {
      if (playerInventory.isNotBlank() && playerInventory != "[]") {
        list.addAll(playerInventory.split(",").map { it.trim() })
      }
    }
    return list
  }

  fun getMilestonesList(): List<String> {
    val list = mutableListOf<String>()
    try {
      val array = JSONArray(milestonesJson)
      for (i in 0 until array.length()) {
        list.add(array.getString(i))
      }
    } catch (_: Exception) {
      if (milestonesJson.isNotBlank() && milestonesJson != "[]") {
        list.addAll(milestonesJson.split("\n").map { it.trim() }.filter { it.isNotBlank() })
      }
    }
    return list
  }

  fun extractDayNumber(): Int {
    val regex = Regex("""Tag\s*(\d+)""", RegexOption.IGNORE_CASE)
    val match = regex.find(inGameTime)
    return match?.groupValues?.get(1)?.toIntOrNull() ?: 1
  }

  /**
   * Liefert alle koerperlichen Verletzungen fuer den angegebenen Charakter
   * (oder den Spieler, falls "Du" oder leer).
   */
  fun getCharacterInjuries(characterName: String): List<CharacterInjury> {
    val list = mutableListOf<CharacterInjury>()
    val isPlayer = characterName.equals("Du", ignoreCase = true) || characterName.equals("Spieler", ignoreCase = true)

    try {
      if (rawStateJson.isNotBlank()) {
        val root = JSONObject(rawStateJson)
        val injuriesArray = root.optJSONArray("injuries")
        if (injuriesArray != null) {
          for (i in 0 until injuriesArray.length()) {
            val obj = injuriesArray.optJSONObject(i) ?: continue
            val injury = CharacterInjury.fromJson(obj)
            val match = if (isPlayer) {
              injury.characterName.equals("Du", ignoreCase = true) || injury.characterName.equals("Spieler", ignoreCase = true)
            } else {
              injury.characterName.equals(characterName, ignoreCase = true)
            }
            if (match) {
              list.add(injury)
            }
          }
        }
      }
    } catch (_: Exception) { }

    // Fallback: Wenn in rawStateJson noch keine structured injuries hinterlegt waren,
    // aber in playerCondition eine Verletzung erwaehnt ist, erzeuge entsprechende anatomische Wunde
    if (list.isEmpty() && isPlayer && playerCondition.isNotBlank() && !playerCondition.equals("Unverletzt", ignoreCase = true)) {
      list.add(CharacterInjury.fromNaturalText(playerCondition, "Du"))
    }

    return list
  }
}

data class NpcInfo(
  val name: String,
  val outfit: String,
  val relationshipToPlayer: String,
  val currentMood: String,
  val status: String = "Anwesend",
  /** Körperliche Verfassung — getrennt von der Stimmung, siehe [NpcEntity.condition]. */
  val condition: String = "",
  /** "MALE" oder "FEMALE". Leer, wenn die Extraktion nichts geliefert hat. */
  val gender: String = ""
)

@Entity(tableName = "messages", indices = [Index("storyId")])
data class MessageEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val storyId: Long,
  val sender: String, // "user", "model", "system"
  val content: String,
  val inGameTimeTag: String? = null,
  val checkpointId: Long? = null,
  val timestamp: Long = System.currentTimeMillis(),
  /**
   * Historisch: Embedding als JSON-Array. Wird seit DB-Version 9 nicht mehr gelesen —
   * das episodische Gedächtnis liegt in [MemoryEntity]. Die Spalte bleibt erhalten, weil ein
   * Spalten-Drop in SQLite einen Tabellen-Rebuild erzwänge (unnötiges Risiko für Spielstände).
   */
  val embeddingJson: String? = null
)

/**
 * Art einer episodischen Erinnerung. Bestimmt Gewichtung beim Retrieval und Darstellung.
 */
object MemoryKind {
  /** Was in einem einzelnen Zug faktisch geschah (1-2 Sätze aus der Extraktion). */
  const val EVENT = "event"

  /** Prägender, dauerhafter Moment. Wird beim Retrieval bevorzugt. */
  const val MILESTONE = "milestone"

  /** Ein Absatz über einen abgeschlossenen In-Game-Tag. */
  const val DAY_SUMMARY = "day_summary"

  /** Persönliche Erinnerung einer Figur: was sie erlebt hat, weiß oder empfindet. */
  const val NPC = "npc"
}

/**
 * Eine atomare, semantisch durchsuchbare Erinnerung.
 *
 * Bewusst klein gehalten: Eine ganze Game-Master-Antwort in einem einzigen 768-dim-Vektor
 * verschmiert alle enthaltenen Fakten und macht die Suche unscharf. Ein Eintrag = ein Sachverhalt.
 */
@Entity(
  tableName = "memories",
  indices = [Index("storyId"), Index("npcId"), Index("dayNumber")]
)
data class MemoryEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val storyId: Long,
  val kind: String = MemoryKind.EVENT,
  /** In-Game-Tag, an dem die Erinnerung entstand. Basis für Alters-Diversität und "vor N Tagen". */
  val dayNumber: Int = 1,
  val inGameTime: String = "",
  val turnNumber: Int = 0,
  /** Gesetzt bei [MemoryKind.NPC]: zu welcher Figur die Erinnerung gehört. */
  val npcId: Long? = null,
  val text: String,
  /** Embedding als BLOB (4 Byte/Float) statt JSON (~15 Byte/Float): kleiner und ohne Parsing. */
  val embedding: ByteArray? = null,
  val createdAt: Long = System.currentTimeMillis()
) {
  // ByteArray braucht explizites equals/hashCode, sonst vergleicht Kotlin die Referenz.
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is MemoryEntity) return false
    return id == other.id &&
      storyId == other.storyId &&
      kind == other.kind &&
      dayNumber == other.dayNumber &&
      inGameTime == other.inGameTime &&
      turnNumber == other.turnNumber &&
      npcId == other.npcId &&
      text == other.text &&
      createdAt == other.createdAt &&
      (embedding?.contentEquals(other.embedding) ?: (other.embedding == null))
  }

  override fun hashCode(): Int {
    var result = id.hashCode()
    result = 31 * result + storyId.hashCode()
    result = 31 * result + kind.hashCode()
    result = 31 * result + dayNumber
    result = 31 * result + inGameTime.hashCode()
    result = 31 * result + turnNumber
    result = 31 * result + (npcId?.hashCode() ?: 0)
    result = 31 * result + text.hashCode()
    result = 31 * result + createdAt.hashCode()
    result = 31 * result + (embedding?.contentHashCode() ?: 0)
    return result
  }
}

/**
 * Kanonische Identität einer Figur — überlebt jeden Zug.
 *
 * Der Grund für diese Tabelle: NPCs lagen bisher nur als Snapshot im Checkpoint und wurden vom
 * Modell jeden Zug neu generiert. Dabei driften Merkmale wie Haar- oder Augenfarbe. [appearance]
 * wird deshalb genau einmal geschrieben und danach nur noch bei einer im Text belegten Änderung.
 */
@Entity(tableName = "npcs", indices = [Index("storyId")])
data class NpcEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val storyId: Long,
  val canonicalName: String,
  /** Weitere Schreibweisen/Kurzformen als JSON-Array, z. B. ["Lena","Frau Vogt"]. */
  val aliasesJson: String = "[]",
  val gender: String = "FEMALE",
  /** Eingefrorenes Erscheinungsbild: Haarfarbe, Augen, Statur, Narben, Merkmale. */
  val appearance: String = "",
  val personality: String = "",
  val firstMetDay: Int = 1,
  val lastSeenDay: Int = 1,
  val relationship: String = "Neutral",
  val currentMood: String = "Ruhig",
  /**
   * Körperliche Verfassung — das Gegenstück zu [CheckpointEntity.playerCondition].
   *
   * Ohne dieses Feld hatte nur der Spieler einen Zustand: Nach zwei Monaten ohne Nahrung stand
   * beim Spieler „stark abgemagert, unterernährt", während die Figur daneben als „völlig gesund"
   * geführt wurde. Hunger, Kälte und Erschöpfung treffen aber jeden im selben Raum.
   */
  val condition: String = "",
  val outfit: String = "",
  val status: String = "Anwesend",
  val isAlive: Boolean = true,
  val updatedAt: Long = System.currentTimeMillis()
) {
  fun getAliases(): List<String> {
    val list = mutableListOf<String>()
    try {
      val array = JSONArray(aliasesJson)
      for (i in 0 until array.length()) {
        val alias = array.optString(i).trim()
        if (alias.isNotBlank()) list.add(alias)
      }
    } catch (_: Exception) { }
    return list
  }

  /**
   * Prüft, ob ein vom Modell gelieferter Name diese Figur meint.
   * Deckt Vollname, Aliase, Vornamen und Teilstrings ab — das Modell schreibt mal "Lena",
   * mal "Lena Vogt", mal "Frau Vogt".
   */
  fun matchesName(candidate: String): Boolean {
    val needle = candidate.trim()
    if (needle.isBlank()) return false
    if (canonicalName.equals(needle, ignoreCase = true)) return true
    if (getAliases().any { it.equals(needle, ignoreCase = true) }) return true

    val canonicalParts = canonicalName.split(" ", "-").filter { it.length >= 3 }
    val needleParts = needle.split(" ", "-").filter { it.length >= 3 }
    return canonicalParts.any { part -> needleParts.any { it.equals(part, ignoreCase = true) } }
  }
}
