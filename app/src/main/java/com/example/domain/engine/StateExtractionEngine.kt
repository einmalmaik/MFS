package com.example.domain.engine

import android.util.Log
import com.example.data.api.GeminiClient
import com.example.data.db.StoryDao
import com.example.data.model.CheckpointEntity
import com.example.data.model.StoryEntity
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

      val newInGameTime = updatedStateJsonObj.optString(
        "in_game_time",
        latestCheckpoint?.inGameTime ?: "Tag 1, 21:45 Uhr"
      )
      val newLocation = updatedStateJsonObj.optString(
        "location",
        latestCheckpoint?.location ?: "Aktueller Ort"
      )
      val newWeather = updatedStateJsonObj.optString(
        "weather",
        latestCheckpoint?.weather ?: "Klar"
      )
      val newOutfit = updatedStateJsonObj.optString(
        "player_outfit",
        latestCheckpoint?.playerOutfit ?: ""
      )
      val newCondition = updatedStateJsonObj.optString(
        "player_condition",
        latestCheckpoint?.playerCondition ?: "Unverletzt"
      )

      val newInventoryArray = updatedStateJsonObj.optJSONArray("player_inventory")
      val invJson = newInventoryArray?.toString() ?: (latestCheckpoint?.playerInventory ?: "[]")

      val npcsArray = updatedStateJsonObj.optJSONArray("npcs")
      val npcsJson = npcsArray?.toString() ?: (latestCheckpoint?.npcsJson ?: "[]")

      val milestonesArray = updatedStateJsonObj.optJSONArray("milestones")
      val milestonesJson = milestonesArray?.toString() ?: (latestCheckpoint?.milestonesJson ?: "[]")

      val newSummary = updatedStateJsonObj.optString("previous_events_summary", summary)

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
        milestonesJson = milestonesJson,
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
}
