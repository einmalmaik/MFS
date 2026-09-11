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
import org.json.JSONArray

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
   * 1. Inserts user message into the database (with semantic embedding).
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

    // Generate semantic embedding for the user's action
    val userEmbeddingStr = geminiClient.generateEmbedding(userAction, story.selectedEmbeddingModel)

    // 1. Insert user message into DB
    val userMsgId = storyDao.insertMessage(
      MessageEntity(
        storyId = storyId,
        sender = "user",
        content = userAction,
        inGameTimeTag = latestCheckpoint?.inGameTime,
        embeddingJson = userEmbeddingStr
      )
    )

    emit(TurnProgress.Streaming)

    // 2. Prepare sliding window context
    val allMessages = storyDao.getMessagesSnapshot(storyId)
    val slidingWindowMessages = allMessages.takeLast(SLIDING_WINDOW_SIZE)
    val slidingWindow = slidingWindowMessages.map {
      it.sender to it.content
    }
    val slidingWindowIds = slidingWindowMessages.map { it.id }.toSet()

    // 3. Generate Semantic Memories
    val semanticMemories = generateSemanticMemories(storyId, userEmbeddingStr, slidingWindowIds)

    // 4. Stream generation
    val finalResponseText = streamContent(
      story = story,
      latestCheckpoint = latestCheckpoint,
      slidingWindow = slidingWindow,
      semanticMemories = semanticMemories,
      userAction = userAction,
      onChunk = onChunk
    ) ?: run {
      emit(TurnProgress.Failed("Die KI hat keine Antwort generiert. Bitte prüfe den API-Key oder die Verbindung."))
      return@flow
    }

    // 5. Persist GM message and extract state
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

    val userEmbeddingStr = geminiClient.generateEmbedding(userAction, story.selectedEmbeddingModel)
    // Here we could technically update the user action's embedding in DB, but since regenerate 
    // happens after truncateAndPrepareEdit, the message is already in DB, we'll just use it in-memory.

    emit(TurnProgress.Streaming)

    val allMessages = storyDao.getMessagesSnapshot(storyId)
    val latestCheckpoint = storyDao.getLatestCheckpoint(storyId)

    // Exclude the last message from the previous window since user action is supplied explicitly
    val slidingWindowMessages = allMessages.dropLast(1).takeLast(6)
    val slidingWindow = slidingWindowMessages.map {
      it.sender to it.content
    }
    val slidingWindowIds = slidingWindowMessages.map { it.id }.toSet()
    
    val semanticMemories = generateSemanticMemories(storyId, userEmbeddingStr, slidingWindowIds)

    val finalResponseText = streamContent(
      story = story,
      latestCheckpoint = latestCheckpoint,
      slidingWindow = slidingWindow,
      semanticMemories = semanticMemories,
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

  private suspend fun generateSemanticMemories(
    storyId: Long,
    userEmbeddingStr: String?,
    excludeMessageIds: Set<Long>
  ): List<String> {
    if (userEmbeddingStr == null) return emptyList()
    val userVec = parseEmbedding(userEmbeddingStr)
    if (userVec.isEmpty()) return emptyList()

    val allDbMemories = storyDao.getMessagesWithEmbeddings(storyId)
    val candidates = allDbMemories.filter { it.id !in excludeMessageIds }

    val scored = candidates.mapNotNull { msg ->
      msg.embeddingJson?.let { jsonStr ->
        val vec = parseEmbedding(jsonStr)
        if (vec.isNotEmpty()) msg to cosineSimilarity(userVec, vec) else null
      }
    }

    // Top 5 most relevant past events
    return scored.sortedByDescending { it.second }.take(5).map {
      "[Tag/Zeit: ${it.first.inGameTimeTag ?: "Unbekannt"}] ${it.first.sender.uppercase()}: ${it.first.content}"
    }
  }

  private fun parseEmbedding(jsonStr: String): List<Float> {
    val list = mutableListOf<Float>()
    try {
      val arr = JSONArray(jsonStr)
      for (i in 0 until arr.length()) {
        list.add(arr.getDouble(i).toFloat())
      }
    } catch (_: Exception) {}
    return list
  }

  private fun cosineSimilarity(v1: List<Float>, v2: List<Float>): Float {
    var dotProduct = 0.0f
    var normA = 0.0f
    var normB = 0.0f
    for (i in v1.indices) {
      if (i >= v2.size) break
      dotProduct += v1[i] * v2[i]
      normA += v1[i] * v1[i]
      normB += v2[i] * v2[i]
    }
    return if (normA == 0.0f || normB == 0.0f) 0.0f else (dotProduct / (kotlin.math.sqrt(normA) * kotlin.math.sqrt(normB)))
  }

  private suspend fun streamContent(
    story: StoryEntity,
    latestCheckpoint: CheckpointEntity?,
    slidingWindow: List<Pair<String, String>>,
    semanticMemories: List<String>,
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
        semanticMemories = semanticMemories,
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

    // Generate embedding for AI's response in the background to ensure it's searchable for the next turn
    val aiEmbeddingStr = geminiClient.generateEmbedding(finalResponseText, story.selectedEmbeddingModel)

    // Initial insert of model response
    val modelMsgId = storyDao.insertMessage(
      MessageEntity(
        storyId = story.id,
        sender = "model",
        content = finalResponseText,
        inGameTimeTag = latestCheckpoint?.inGameTime,
        embeddingJson = aiEmbeddingStr
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
        checkpointId = newCheckpointId,
        embeddingJson = aiEmbeddingStr
      )
    )
  }
}
