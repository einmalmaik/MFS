package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "stories")
data class StoryEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val title: String,
  val systemPrompt: String = "", // Empty means: use global default prompt
  val genre: String = "Dark Noir & Mystery",
  val perspective: String = "Zweite Person (Du)",
  val selectedModel: String = "gemini-3.8-flash",
  val temperature: Float = 0.85f,
  val supportsTemperature: Boolean = true,
  val thinkingLevel: String = "MEDIUM", // "MINIMAL", "LOW", "MEDIUM", "HIGH", "OFF"
  val thinkingBudget: Int = 2048, // Legacy für 2.5: 0 = Aus, 1024 = Gering, 2048 = Standard, 4096 = Tief, 8192 = Max
  val adultContentEnabled: Boolean = true,
  val isArchived: Boolean = false,
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
  val supportedThinkingLevels: List<String> = listOf("MINIMAL", "LOW", "MEDIUM", "HIGH")
)

@Entity(tableName = "checkpoints")
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
            status = obj.optString("status", "Anwesend")
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
  val status: String = "Anwesend"
)

@Entity(tableName = "messages")
data class MessageEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val storyId: Long,
  val sender: String, // "user", "model", "system"
  val content: String,
  val inGameTimeTag: String? = null,
  val checkpointId: Long? = null,
  val timestamp: Long = System.currentTimeMillis()
)
