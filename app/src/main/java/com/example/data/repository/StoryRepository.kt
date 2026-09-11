package com.example.data.repository

import android.content.Context
import com.example.data.api.GeminiClient
import com.example.data.db.StoryDao
import com.example.data.model.CheckpointEntity
import com.example.data.model.GeminiModelInfo
import com.example.data.model.MessageEntity
import com.example.data.model.StoryEntity
import com.example.domain.engine.StateExtractionEngine
import com.example.domain.engine.StoryTurnEngine
import com.example.domain.model.TurnProgress
import com.example.domain.service.StoryBranchingService
import com.example.domain.service.StoryPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

// Re-export TurnProgress in repository package for caller compatibility
typealias TurnProgress = com.example.domain.model.TurnProgress

/**
 * Main Repository facade orchestrating story persistence, AI generation,
 * state extraction, and branching logic.
 */
class StoryRepository(
  private val storyDao: StoryDao,
  context: Context
) {
  val preferences = StoryPreferences(context)

  val geminiClient = GeminiClient(
    customApiKeyProvider = { preferences.getCustomApiKey() }
  )

  private val stateExtractionEngine = StateExtractionEngine(
    geminiClient = geminiClient,
    storyDao = storyDao
  )

  private val turnEngine = StoryTurnEngine(
    geminiClient = geminiClient,
    storyDao = storyDao,
    stateExtractionEngine = stateExtractionEngine,
    preferences = preferences
  )

  private val branchingService = StoryBranchingService(
    storyDao = storyDao
  )

  // --- PREFERENCES & CREDENTIALS ---

  fun getCustomApiKey(): String? = preferences.getCustomApiKey()

  fun setCustomApiKey(key: String?) = preferences.setCustomApiKey(key)

  fun getGlobalDefaultSystemPrompt(): String = preferences.getGlobalDefaultSystemPrompt()

  fun setGlobalDefaultSystemPrompt(prompt: String) = preferences.setGlobalDefaultSystemPrompt(prompt)

  fun getEffectiveApiKey(): String = preferences.getEffectiveApiKey()

  suspend fun testApiKey(): Pair<Boolean, String> = geminiClient.testConnection(getEffectiveApiKey())

  suspend fun fetchAvailableModels(): List<GeminiModelInfo> = geminiClient.fetchAvailableModels()

  // --- STORY PERSISTENCE (ROOM) ---

  fun getAllStories(): Flow<List<StoryEntity>> = storyDao.getAllStories()

  fun getMessagesForStory(storyId: Long): Flow<List<MessageEntity>> = storyDao.getMessagesForStory(storyId)

  fun observeLatestCheckpoint(storyId: Long): Flow<CheckpointEntity?> = storyDao.observeLatestCheckpoint(storyId)

  fun getAllCheckpoints(storyId: Long): Flow<List<CheckpointEntity>> = storyDao.getAllCheckpoints(storyId)

  suspend fun getStoryById(storyId: Long): StoryEntity? = storyDao.getStoryById(storyId)

  suspend fun updateStory(story: StoryEntity) = storyDao.updateStory(story)

  suspend fun toggleStoryArchived(storyId: Long, isArchived: Boolean) = withContext(Dispatchers.IO) {
    val story = storyDao.getStoryById(storyId) ?: return@withContext
    storyDao.updateStory(story.copy(isArchived = isArchived, updatedAt = System.currentTimeMillis()))
  }

  suspend fun deleteStory(story: StoryEntity) = deleteStoryById(story.id)

  suspend fun deleteStoryById(storyId: Long) = withContext(Dispatchers.IO) {
    storyDao.deleteAllMessagesForStory(storyId)
    storyDao.deleteAllCheckpointsForStory(storyId)
    storyDao.deleteStoryById(storyId)
  }

  suspend fun ensureInitialData(): Long = withContext(Dispatchers.IO) {
    val firstSnapshot = storyDao.getAllStories().firstOrNull() ?: emptyList()
    if (firstSnapshot.isNotEmpty()) {
      return@withContext firstSnapshot.first().id
    }
    createStory(
      title = "Schatten über der Hafenstadt",
      genre = "Dark Noir & Mystery",
      perspective = "Zweite Person (Du)",
      systemPrompt = "",
      selectedModel = "gemini-3.8-flash",
      temperature = 0.85f,
      supportsTemperature = true,
      thinkingLevel = "MEDIUM",
      thinkingBudget = 2048,
      adultContent = true,
      initialLocation = "Alte Lagerhalle am Nordhafen",
      initialOutfit = "Durchnässte dunkle Lederjacke, grauer Hoodie, robuste Stiefel",
      initialInventory = listOf("Taschenlampe", "Dietrich-Set", "altes Notizbuch"),
      initialNpcName = "Elena",
      initialNpcOutfit = "Dunkelblauer Wollmantel, Lederstiefel, hochgeschlagener Kragen",
      initialNpcRelation = "Misstrauisch, hält Abstand wegen der Ereignisse am Vortag",
      initialPromptOpening = "Der Regen schlägt unbarmherzig gegen die verrosteten Wellblechwände der alten Lagerhalle am Nordhafen. Der Geruch von feuchtem Beton, Motorenöl und salziger Meeresluft hängt schwer im Raum.\n\nElena steht ein paar Schritte entfernt an einer umgestürzten Holzkiste. Sie hat die Hände tief in den Taschen ihres dunkelblauen Wollmantels vergraben. Ihr Blick wandert zur schweren Schiebetür, die nur einen Spalt breit offen steht. Im trüben Schein deiner schwachen Taschenlampe wirkt ihr Gesicht angespannt.\n\n\"Sie wissen, dass wir hier sind\", sagt sie leise, ohne dich direkt anzusehen. \"Die Frage ist nur, wie viel Zeit wir noch haben.\""
    )
  }

  suspend fun createStory(
    title: String,
    genre: String,
    perspective: String = "Zweite Person (Du)",
    systemPrompt: String = "",
    selectedModel: String = "gemini-3.8-flash",
    selectedEmbeddingModel: String = "text-embedding-004",
    temperature: Float = 0.85f,
    supportsTemperature: Boolean = true,
    thinkingLevel: String = "MEDIUM",
    thinkingBudget: Int = 2048,
    adultContent: Boolean = true,
    initialLocation: String = "Startort",
    initialOutfit: String = "Alltagskleidung",
    initialInventory: List<String> = emptyList(),
    initialNpcName: String = "",
    initialNpcOutfit: String = "",
    initialNpcRelation: String = "",
    initialPromptOpening: String = ""
  ): Long = withContext(Dispatchers.IO) {
    val story = StoryEntity(
      title = title,
      systemPrompt = systemPrompt,
      genre = genre,
      perspective = perspective,
      selectedModel = selectedModel,
      selectedEmbeddingModel = selectedEmbeddingModel,
      temperature = temperature,
      supportsTemperature = supportsTemperature,
      thinkingLevel = thinkingLevel,
      thinkingBudget = thinkingBudget,
      adultContentEnabled = adultContent
    )
    val storyId = storyDao.insertStory(story)

    val npcsJsonArray = JSONArray()
    if (initialNpcName.isNotBlank()) {
      val npcObj = JSONObject().apply {
        put("name", initialNpcName)
        put("outfit", initialNpcOutfit.ifBlank { "Passende Zivilkleidung" })
        put("relationship_to_player", initialNpcRelation.ifBlank { "Zurückhaltend" })
        put("current_mood", "Aufmerksam")
        put("status", "Anwesend")
      }
      npcsJsonArray.put(npcObj)
    }

    val invArray = JSONArray()
    initialInventory.forEach { invArray.put(it) }

    val rawStateObj = JSONObject().apply {
      put("in_game_time", "Tag 1, 20:00 Uhr")
      put("location", initialLocation)
      put("weather", "Klar, kühl")
      put("player", JSONObject().apply {
        put("outfit", initialOutfit)
        put("condition", "Unverletzt")
        put("inventory", invArray)
      })
      put("npcs_present", npcsJsonArray)
      put("previous_events_summary", "Das Abenteuer beginnt.")
    }

    val initialCheckpoint = CheckpointEntity(
      storyId = storyId,
      turnNumber = 0,
      inGameTime = "Tag 1, 20:00 Uhr",
      location = initialLocation,
      weather = "Klar, kühl",
      playerOutfit = initialOutfit,
      playerInventory = invArray.toString(),
      playerCondition = "Unverletzt",
      npcsJson = npcsJsonArray.toString(),
      milestonesJson = "[]",
      previousEventsSummary = "Das Abenteuer beginnt.",
      rawStateJson = rawStateObj.toString()
    )
    val cpId = storyDao.insertCheckpoint(initialCheckpoint)

    if (initialPromptOpening.isNotBlank()) {
      storyDao.insertMessage(
        MessageEntity(
          storyId = storyId,
          sender = "model",
          content = initialPromptOpening,
          inGameTimeTag = "Tag 1, 20:00 Uhr",
          checkpointId = cpId
        )
      )
    }

    storyId
  }

  // --- TURN EXECUTION & STREAMING ---

  fun executeTurn(
    storyId: Long,
    userAction: String,
    onChunk: (String) -> Unit
  ): Flow<TurnProgress> = turnEngine.executeTurn(storyId, userAction, onChunk)

  suspend fun editUserMessageAndRegenerate(
    storyId: Long,
    messageId: Long,
    newContent: String,
    onChunk: (String) -> Unit
  ): Flow<TurnProgress> {
    branchingService.truncateAndPrepareEdit(storyId, messageId, newContent)
    return turnEngine.regenerateTurn(storyId, newContent, onChunk)
  }

  // --- BRANCHING & ROLLBACK ---

  suspend fun branchStory(
    sourceStoryId: Long,
    branchTitle: String,
    upToMessageId: Long? = null
  ): Long = branchingService.branchStory(sourceStoryId, branchTitle, upToMessageId)

  suspend fun rewindToMessage(
    storyId: Long,
    message: MessageEntity
  ) = branchingService.rewindToMessage(storyId, message)

  // --- MANUAL STATE OVERRIDE ---

  suspend fun updateCurrentState(
    storyId: Long,
    inGameTime: String,
    location: String,
    weather: String,
    playerOutfit: String,
    playerCondition: String,
    playerInventory: List<String>,
    npcs: String,
    summary: String
  ) = withContext(Dispatchers.IO) {
    val latest = storyDao.getLatestCheckpoint(storyId)
    val invArray = JSONArray()
    playerInventory.forEach { invArray.put(it) }

    val rawObj = JSONObject().apply {
      put("in_game_time", inGameTime)
      put("location", location)
      put("weather", weather)
      put("player_outfit", playerOutfit)
      put("player_condition", playerCondition)
      put("player_inventory", invArray)
      try {
        put("npcs", JSONArray(npcs))
      } catch (_: Exception) {
        put("npcs", JSONArray())
      }
      put("previous_events_summary", summary)
    }

    val updatedCheckpoint = if (latest != null) {
      latest.copy(
        inGameTime = inGameTime,
        location = location,
        weather = weather,
        playerOutfit = playerOutfit,
        playerCondition = playerCondition,
        playerInventory = invArray.toString(),
        npcsJson = npcs,
        previousEventsSummary = summary,
        rawStateJson = rawObj.toString()
      )
    } else {
      CheckpointEntity(
        storyId = storyId,
        turnNumber = 0,
        inGameTime = inGameTime,
        location = location,
        weather = weather,
        playerOutfit = playerOutfit,
        playerCondition = playerCondition,
        playerInventory = invArray.toString(),
        npcsJson = npcs,
        milestonesJson = "[]",
        previousEventsSummary = summary,
        rawStateJson = rawObj.toString()
      )
    }

    if (latest != null) {
      storyDao.updateCheckpoint(updatedCheckpoint)
    } else {
      storyDao.insertCheckpoint(updatedCheckpoint)
    }
  }

  suspend fun updateCheckpointDirectly(checkpoint: CheckpointEntity) {
    storyDao.updateCheckpoint(checkpoint)
  }

  suspend fun updatePlayTime(storyId: Long, incrementSeconds: Long) = withContext(Dispatchers.IO) {
    val story = storyDao.getStoryById(storyId) ?: return@withContext
    storyDao.updateStory(story.copy(playTimeSeconds = story.playTimeSeconds + incrementSeconds))
  }
}
