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
  private val memoryEngine: MemoryEngine,
  private val npcEngine: NpcEngine,
  private val preferences: StoryPreferences
) {

  companion object {
    private const val TAG = "StoryTurnEngine"
    private const val SLIDING_WINDOW_SIZE = 8
    private const val REGENERATE_WINDOW_SIZE = 6
  }

  /**
   * Executes a standard story turn:
   * 1. Inserts user message into the database.
   * 2. Builds context anchor (Zeitrechnung, Zustand, Figuren, Erinnerungen) & sliding window.
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

    storyDao.insertMessage(
      MessageEntity(
        storyId = storyId,
        sender = "user",
        content = userAction,
        inGameTimeTag = latestCheckpoint?.inGameTime
      )
    )

    emit(TurnProgress.Streaming)

    val slidingWindow = loadSlidingWindow(storyId, SLIDING_WINDOW_SIZE)

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
    val latestCheckpoint = storyDao.getLatestCheckpoint(storyId)

    emit(TurnProgress.Streaming)

    // Die letzte Nachricht ist die bearbeitete Aktion; sie wird separat übergeben.
    val slidingWindow = loadSlidingWindow(storyId, REGENERATE_WINDOW_SIZE + 1).dropLast(1)

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

  /**
   * Lädt nur das benötigte Fenster per SQL statt die gesamte Historie in den Speicher zu ziehen.
   */
  private suspend fun loadSlidingWindow(storyId: Long, size: Int): List<Pair<String, String>> {
    return storyDao.getRecentMessages(storyId, size)
      .asReversed()
      .filter { it.content.isNotBlank() }
      .map { it.sender to it.content }
  }

  /**
   * Baut den Suchtext für das episodische Gedächtnis.
   *
   * Die Spieler-Aktion allein reicht nicht: "Wie geht es dir?" hat für sich genommen keinerlei
   * semantische Nähe zu dem Ereignis, auf das sich die Frage bezieht. Erst mit den anwesenden
   * Figuren und dem Ort wird daraus eine Anfrage, die alte Szenen wiederfindet.
   */
  private fun buildRetrievalQuery(
    userAction: String,
    checkpoint: CheckpointEntity?,
    presentNpcNames: List<String>
  ): String = buildString {
    append(userAction)
    if (presentNpcNames.isNotEmpty()) {
      append(" | Anwesend: ")
      append(presentNpcNames.joinToString(", "))
    }
    checkpoint?.location?.takeIf { it.isNotBlank() }?.let {
      append(" | Ort: ")
      append(it)
    }
  }

  private suspend fun streamContent(
    story: StoryEntity,
    latestCheckpoint: CheckpointEntity?,
    slidingWindow: List<Pair<String, String>>,
    userAction: String,
    onChunk: (String) -> Unit
  ): String? {
    val stateJson = latestCheckpoint?.rawStateJson ?: ""
    val summary = latestCheckpoint?.previousEventsSummary ?: ""
    val currentInGameTime = latestCheckpoint?.inGameTime ?: "Tag 1, 09:00 Uhr"

    val presentNpcNames = latestCheckpoint?.getNpcList()
      ?.filter { it.status.equals("Anwesend", ignoreCase = true) }
      ?.map { it.name }
      ?: emptyList()

    val retrievalQuery = buildRetrievalQuery(userAction, latestCheckpoint, presentNpcNames)

    // Vier Gedächtnisschichten: Meilensteine, Tagesüberblick, Figuren, semantische Erinnerungen.
    val milestones = memoryEngine.getRecentMilestones(story.id)
      .ifEmpty { latestCheckpoint?.getMilestonesList() ?: emptyList() }
    val daySummaries = memoryEngine.getRecentDaySummaries(story.id)
    val semanticMemories = memoryEngine.retrieve(
      storyId = story.id,
      queryText = retrievalQuery,
      currentInGameTime = currentInGameTime,
      presentNpcNames = presentNpcNames,
      embeddingModel = story.selectedEmbeddingModel
    )
    val npcProfiles = npcEngine.buildProfiles(
      storyId = story.id,
      memoryEngine = memoryEngine,
      currentInGameTime = currentInGameTime,
      embeddingModel = story.selectedEmbeddingModel,
      queryText = retrievalQuery
    )

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
        currentInGameTime = currentInGameTime,
        episodicSummary = summary,
        milestones = milestones,
        daySummaries = daySummaries,
        npcProfiles = npcProfiles,
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

    val modelMsgId = storyDao.insertMessage(
      MessageEntity(
        storyId = story.id,
        sender = "model",
        content = finalResponseText,
        inGameTimeTag = latestCheckpoint?.inGameTime
      )
    )

    // Die Extraktion schreibt Checkpoint, Erinnerungen und Figuren in einem Durchgang.
    val newCheckpointId = stateExtractionEngine.extractAndCommitCheckpoint(
      story = story,
      latestCheckpoint = latestCheckpoint,
      userAction = userAction,
      modelResponse = finalResponseText,
      turnNumber = currentTurn
    )

    val committedCheckpoint = storyDao.getLatestCheckpoint(story.id)
    val inGameTime = committedCheckpoint?.inGameTime ?: latestCheckpoint?.inGameTime

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
