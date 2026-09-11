package com.example.domain.engine

import android.util.Log
import com.example.data.api.GeminiClient
import com.example.data.db.StoryDao
import com.example.data.model.CheckpointEntity
import com.example.data.model.MessageEntity
import com.example.data.model.StoryEntity
import com.example.domain.model.TurnProgress
import com.example.domain.service.StoryPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Core domain engine coordinating the end-to-end execution of a story turn.
 * Manages prompt construction, history windowing, SSE token streaming,
 * candidate validation, and handoff to [StateExtractionEngine].
 */
class StoryTurnEngine(
  private val geminiClient: GeminiClient,
  private val storyDao: StoryDao,
  private val stateExtractionEngine: StateExtractionEngine,
  private val preferences: StoryPreferences
) {
  companion object {
    private const val TAG = "StoryTurnEngine"
    private const val SLIDING_WINDOW_SIZE = 8
  }

  /**
   * Executes a standard story turn:
   * 1. Inserts user message into the database.
   * 2. Builds context anchor (state, milestones, summary) & sliding history window.
   * 3. Streams AI Game Master response in real-time.
   * 4. Persists the GM message and invokes background state extraction.
   */
  fun executeTurn(
    storyId: Long,
    userAction: String,
    onChunk: (String) -> Unit
  ): Flow<TurnProgress> = flow {
    emit(TurnProgress.Thinking)

    val story = storyDao.getStoryById(storyId)
      ?: throw IllegalStateException("Story $storyId existiert nicht.")

    val latestCheckpoint = storyDao.getLatestCheckpoint(storyId)

    // 1. Insert user message into DB
    storyDao.insertMessage(
      MessageEntity(
        storyId = storyId,
        sender = "user",
        content = userAction,
        inGameTimeTag = latestCheckpoint?.inGameTime
      )
    )

    emit(TurnProgress.Streaming)

    // 2. Prepare sliding window context
    val allMessages = storyDao.getMessagesSnapshot(storyId)
    val slidingWindow = allMessages.takeLast(SLIDING_WINDOW_SIZE).map {
      it.sender to it.content
    }

    // 3. Stream generation
    val finalResponseText = streamContent(
      story = story,
      latestCheckpoint = latestCheckpoint,
      slidingWindow = slidingWindow,
      userAction = userAction,
      onChunk = onChunk
    ) ?: run {
      emit(TurnProgress.Failed("Die KI hat keine Antwort generiert. Bitte prüfe den API-Key oder die Verbindung."))
      return@flow
    }

    // 4. Persist GM message and extract state
    finalizeTurn(
      story = story,
      latestCheckpoint = latestCheckpoint,
      userAction = userAction,
      finalResponseText = finalResponseText
    )

    emit(TurnProgress.Completed)
  }.flowOn(Dispatchers.IO)

  /**
   * Re-generates a turn following a user message edit (after history has been truncated).
   */
  fun regenerateTurn(
    storyId: Long,
    userAction: String,
    onChunk: (String) -> Unit
  ): Flow<TurnProgress> = flow {
    emit(TurnProgress.Thinking)

    val story = storyDao.getStoryById(storyId)
      ?: throw IllegalStateException("Story $storyId existiert nicht.")

    emit(TurnProgress.Streaming)

    val allMessages = storyDao.getMessagesSnapshot(storyId)
    val latestCheckpoint = storyDao.getLatestCheckpoint(storyId)

    // Exclude the last message from the previous window since user action is supplied explicitly
    val slidingWindow = allMessages.dropLast(1).takeLast(6).map {
      it.sender to it.content
    }

    val finalResponseText = streamContent(
      story = story,
      latestCheckpoint = latestCheckpoint,
      slidingWindow = slidingWindow,
      userAction = userAction,
      onChunk = onChunk
    ) ?: run {
      emit(TurnProgress.Failed("Keine Antwort erhalten."))
      return@flow
    }

    finalizeTurn(
      story = story,
      latestCheckpoint = latestCheckpoint,
      userAction = userAction,
      finalResponseText = finalResponseText
    )

    emit(TurnProgress.Completed)
  }.flowOn(Dispatchers.IO)

  private suspend fun streamContent(
    story: StoryEntity,
    latestCheckpoint: CheckpointEntity?,
    slidingWindow: List<Pair<String, String>>,
    userAction: String,
    onChunk: (String) -> Unit
  ): String? {
    val stateJson = latestCheckpoint?.rawStateJson ?: ""
    val summary = latestCheckpoint?.previousEventsSummary ?: ""
    val milestones = latestCheckpoint?.getMilestonesList() ?: emptyList()

    val effectivePrompt = if (story.systemPrompt.isNotBlank()) {
      story.systemPrompt
    } else {
      preferences.getGlobalDefaultSystemPrompt()
    }

    val buffer = StringBuilder()

    try {
      geminiClient.streamGenerateStory(
        model = story.selectedModel,
        systemInstruction = effectivePrompt,
        stateJson = stateJson,
        episodicSummary = summary,
        milestones = milestones,
        recentHistory = slidingWindow,
        userAction = userAction,
        temperature = story.temperature,
        supportsTemperature = story.supportsTemperature,
        thinkingLevel = story.thinkingLevel,
        thinkingBudget = story.thinkingBudget,
        allowAdultContent = story.adultContentEnabled
      ).collect { chunk ->
        buffer.append(chunk)
        onChunk(chunk)
      }
    } catch (e: Exception) {
      Log.e(TAG, "Turn streaming failed", e)
      throw e
    }

    val text = buffer.toString()
    return if (text.isNotBlank()) text else null
  }

  private suspend fun finalizeTurn(
    story: StoryEntity,
    latestCheckpoint: CheckpointEntity?,
    userAction: String,
    finalResponseText: String
  ) {
    val currentTurn = (latestCheckpoint?.turnNumber ?: 0) + 1

    // Initial insert of model response
    val modelMsgId = storyDao.insertMessage(
      MessageEntity(
        storyId = story.id,
        sender = "model",
        content = finalResponseText,
        inGameTimeTag = latestCheckpoint?.inGameTime
      )
    )

    // Extract state and save new checkpoint
    val newCheckpointId = stateExtractionEngine.extractAndCommitCheckpoint(
      story = story,
      latestCheckpoint = latestCheckpoint,
      userAction = userAction,
      modelResponse = finalResponseText,
      turnNumber = currentTurn
    )

    // Retrieve committed checkpoint to fetch updated inGameTime
    val committedCheckpoint = storyDao.getLatestCheckpoint(story.id)
    val inGameTime = committedCheckpoint?.inGameTime ?: latestCheckpoint?.inGameTime

    // Update message entity with checkpoint association
    storyDao.insertMessage(
      MessageEntity(
        id = modelMsgId,
        storyId = story.id,
        sender = "model",
        content = finalResponseText,
        inGameTimeTag = inGameTime,
        checkpointId = newCheckpointId
      )
    )
  }
}
