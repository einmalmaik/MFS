package com.example.domain.engine

import android.util.Log
import com.example.data.api.GeminiClient
import com.example.data.db.StoryDao
import com.example.data.model.CheckpointEntity
import com.example.data.model.StoryEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * Dedicated engine responsible for analyzing in-game events after each story turn,
 * extracting updated state (time, location, weather, outfit, inventory, NPCs, milestones),
 * and committing new checkpoints to the database.
 */
class StateExtractionEngine(
  private val geminiClient: GeminiClient,
  private val storyDao: StoryDao
) {
  companion object {
    private const val TAG = "StateExtractionEngine"
  }

  /**
   * Performs post-turn background state extraction and saves the resulting CheckpointEntity.
   * Returns the newly generated checkpoint ID.
   */
  suspend fun extractAndCommitCheckpoint(
    story: StoryEntity,
    latestCheckpoint: CheckpointEntity?,
    userAction: String,
    modelResponse: String,
    turnNumber: Int
  ): Long {
    val currentStateJson = latestCheckpoint?.rawStateJson ?: ""
    val summary = latestCheckpoint?.previousEventsSummary ?: ""
    val milestones = latestCheckpoint?.getMilestonesList() ?: emptyList()

    return try {
      val updatedStateJsonObj = geminiClient.extractUpdatedState(
        model = story.selectedModel,
        currentStateJson = currentStateJson,
        userAction = userAction,
        storyResponse = modelResponse,
        existingMilestones = milestones
      )

      // Time calculation with deterministic time-skip protection
      val rawExtractedTime = updatedStateJsonObj.optString(
        "in_game_time",
        latestCheckpoint?.inGameTime ?: "Tag 1, 20:00 Uhr"
      ).ifBlank { latestCheckpoint?.inGameTime ?: "Tag 1, 20:00 Uhr" }

      val newInGameTime = computeDeterministicTimeProgression(
        previousTime = latestCheckpoint?.inGameTime ?: "Tag 1, 20:00 Uhr",
        extractedTime = rawExtractedTime,
        userAction = userAction,
        modelResponse = modelResponse
      )

      val newLocation = updatedStateJsonObj.optString("location")
        .ifBlank { latestCheckpoint?.location ?: "Aktueller Ort" }

      var newWeather = updatedStateJsonObj.optString("weather")
        .ifBlank { latestCheckpoint?.weather ?: "Klar" }

      // If a day jump occurred and weather wasn't changed, generate a realistic evolution
      if (newInGameTime != (latestCheckpoint?.inGameTime ?: "") &&
        newWeather == (latestCheckpoint?.weather ?: "") &&
        (userAction.contains("Tag", ignoreCase = true) || modelResponse.contains("Tage", ignoreCase = true))
      ) {
        newWeather = when {
          newWeather.contains("Regen", ignoreCase = true) -> "Aufklarender Himmel, frische Meeresbrise"
          newWeather.contains("Klar", ignoreCase = true) -> "Leicht bewölkt, angenehme Kühle"
          else -> "Ruhig, trocken"
        }
      }

      // Check both nested "player" object and flat keys
      val playerObj = updatedStateJsonObj.optJSONObject("player")

      val newOutfit = updatedStateJsonObj.optString("player_outfit")
        .ifBlank { playerObj?.optString("outfit") ?: "" }
        .ifBlank { latestCheckpoint?.playerOutfit ?: "" }

      val newCondition = updatedStateJsonObj.optString("player_condition")
        .ifBlank { playerObj?.optString("condition") ?: "" }
        .ifBlank { latestCheckpoint?.playerCondition ?: "Unverletzt" }

      val newInventoryArray = updatedStateJsonObj.optJSONArray("player_inventory")
        ?: playerObj?.optJSONArray("inventory")
        ?: updatedStateJsonObj.optJSONArray("inventory")
      val invJson = newInventoryArray?.toString() ?: (latestCheckpoint?.playerInventory ?: "[]")

      val npcsArray = updatedStateJsonObj.optJSONArray("npcs")
        ?: updatedStateJsonObj.optJSONArray("npcs_present")
      val npcsJson = npcsArray?.toString() ?: (latestCheckpoint?.npcsJson ?: "[]")

      // Cumulative Milestones: preserve history & add newly discovered ones
      val cumulativeMilestones = mutableListOf<String>()
      cumulativeMilestones.addAll(milestones)

      val extractedMilestonesArray = updatedStateJsonObj.optJSONArray("milestones")
      if (extractedMilestonesArray != null) {
        for (i in 0 until extractedMilestonesArray.length()) {
          val m = extractedMilestonesArray.optString(i).trim()
          if (m.isNotBlank() && !cumulativeMilestones.contains(m)) {
            cumulativeMilestones.add(m)
          }
        }
      }

      // If time skip happened but model returned no milestone, record the passage of time
      val dayDelta = parseDayNumber(newInGameTime) - parseDayNumber(latestCheckpoint?.inGameTime ?: "Tag 1")
      if (dayDelta > 0) {
        val timeSkipEntry = "Zeitsprung: $dayDelta Tag(e) sind vergangen ($newInGameTime)"
        if (!cumulativeMilestones.contains(timeSkipEntry)) {
          cumulativeMilestones.add(timeSkipEntry)
        }
      }

      val milestonesJsonArray = JSONArray()
      cumulativeMilestones.forEach { milestonesJsonArray.put(it) }

      val newSummary = updatedStateJsonObj.optString("previous_events_summary", summary)
        .ifBlank { summary }

      // Preserve existing injuries if the extraction did not return an updated array
      if (!updatedStateJsonObj.has("injuries") && latestCheckpoint != null && latestCheckpoint.rawStateJson.isNotBlank()) {
        try {
          val prevRoot = JSONObject(latestCheckpoint.rawStateJson)
          val prevInjuries = prevRoot.optJSONArray("injuries")
          if (prevInjuries != null) {
            updatedStateJsonObj.put("injuries", prevInjuries)
          }
        } catch (_: Exception) { }
      }

      val newCheckpoint = CheckpointEntity(
        storyId = story.id,
        turnNumber = turnNumber,
        inGameTime = newInGameTime,
        location = newLocation,
        weather = newWeather,
        playerOutfit = newOutfit,
        playerInventory = invJson,
        playerCondition = newCondition,
        npcsJson = npcsJson,
        milestonesJson = milestonesJsonArray.toString(),
        previousEventsSummary = newSummary,
        rawStateJson = updatedStateJsonObj.toString()
      )

      storyDao.insertCheckpoint(newCheckpoint)
    } catch (e: Exception) {
      Log.w(TAG, "State extraction fallback triggered", e)
      if (latestCheckpoint != null) {
        storyDao.insertCheckpoint(
          latestCheckpoint.copy(
            id = 0,
            turnNumber = turnNumber,
            timestamp = System.currentTimeMillis()
          )
        )
      } else {
        0L
      }
    }
  }

  /**
   * Deterministically calculates time progression when time skips are detected,
   * guaranteeing that statements like "Es vergehen zwei Tage" advance Day 1 -> Day 3 even if
   * the model hallucinated the same day.
   */
  private fun computeDeterministicTimeProgression(
    previousTime: String,
    extractedTime: String,
    userAction: String,
    modelResponse: String
  ): String {
    val previousDay = parseDayNumber(previousTime)
    val extractedDay = parseDayNumber(extractedTime)

    // Check if user action requested a specific day jump
    val actionLower = userAction.lowercase()
    val daysToAdvance = when {
      actionLower.contains("zwei tage") || actionLower.contains("2 tage") -> 2
      actionLower.contains("drei tage") || actionLower.contains("3 tage") -> 3
      actionLower.contains("vier tage") || actionLower.contains("4 tage") -> 4
      actionLower.contains("fünf tage") || actionLower.contains("5 tage") -> 5
      actionLower.contains("eine woche") || actionLower.contains("1 woche") -> 7
      actionLower.contains("übernachten") || actionLower.contains("nächsten morgen") ||
        actionLower.contains("nächster morgen") || actionLower.contains("schlafe bis morgen") -> 1
      else -> {
        val regex = Regex("""(\d+)\s+tage(?:\s+später|\s+vergehen|\s+rasten|\s+warten)?""", RegexOption.IGNORE_CASE)
        val match = regex.find(userAction)
        match?.groupValues?.get(1)?.toIntOrNull() ?: 0
      }
    }

    if (daysToAdvance > 0) {
      val targetDay = previousDay + daysToAdvance
      if (extractedDay < targetDay) {
        // Model didn't advance enough, correct to target day
        val timePart = parseTimeOfDay(extractedTime).ifBlank {
          if (actionLower.contains("morgen")) "08:00 Uhr" else "20:00 Uhr"
        }
        return "Tag $targetDay, $timePart"
      }
    }

    return extractedTime
  }

  private fun parseDayNumber(timeStr: String): Int {
    val regex = Regex("""Tag\s*(\d+)""", RegexOption.IGNORE_CASE)
    val match = regex.find(timeStr)
    return match?.groupValues?.get(1)?.toIntOrNull() ?: 1
  }

  private fun parseTimeOfDay(timeStr: String): String {
    val regex = Regex("""(\d{1,2}:\d{2}(?:\s*Uhr)?)""", RegexOption.IGNORE_CASE)
    val match = regex.find(timeStr)
    val found = match?.groupValues?.get(1) ?: ""
    return if (found.isNotBlank() && !found.endsWith("Uhr", ignoreCase = true)) {
      "$found Uhr"
    } else {
      found
    }
  }
}
