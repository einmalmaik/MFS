package com.example.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.data.api.GeminiClient
import com.example.data.db.StoryDatabase
import com.example.data.model.AiSettings
import com.example.data.model.CheckpointEntity
import com.example.data.model.GeminiDefaults
import com.example.data.model.MessageEntity
import com.example.data.model.StoryEntity
import com.example.domain.engine.MemoryEngine
import com.example.domain.engine.NpcEngine
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
  private val database: StoryDatabase,
  context: Context
) {
  private val storyDao = database.storyDao()

  val preferences = StoryPreferences(context)

  val geminiClient = GeminiClient(
    customApiKeyProvider = { preferences.getCustomApiKey() }
  )

  private val memoryEngine = MemoryEngine(
    geminiClient = geminiClient,
    storyDao = storyDao
  )

  private val npcEngine = NpcEngine(
    storyDao = storyDao
  )

  private val stateExtractionEngine = StateExtractionEngine(
    geminiClient = geminiClient,
    storyDao = storyDao,
    memoryEngine = memoryEngine,
    npcEngine = npcEngine,
    preferences = preferences
  )

  private val turnEngine = StoryTurnEngine(
    geminiClient = geminiClient,
    storyDao = storyDao,
    stateExtractionEngine = stateExtractionEngine,
    memoryEngine = memoryEngine,
    npcEngine = npcEngine,
    preferences = preferences
  )

  private val branchingService = StoryBranchingService(
    storyDao = storyDao,
    database = database
  )

  // --- PREFERENCES & CREDENTIALS ---

  fun getCustomApiKey(): String? = preferences.getCustomApiKey()

  fun setCustomApiKey(key: String?) = preferences.setCustomApiKey(key)

  fun getGlobalDefaultSystemPrompt(): String = preferences.getGlobalDefaultSystemPrompt()

  fun setGlobalDefaultSystemPrompt(prompt: String) = preferences.setGlobalDefaultSystemPrompt(prompt)

  fun getAiSettings(): AiSettings = preferences.getAiSettings()

  fun setAiSettings(settings: AiSettings) = preferences.setAiSettings(settings)

  fun seedAiSettingsOnce(from: AiSettings): Boolean = preferences.seedAiSettingsOnce(from)

  fun getEffectiveApiKey(): String = preferences.getEffectiveApiKey()

  /**
   * Testet den Schlüssel mit genau dem Modell, das auch erzählt. Ein fest verdrahtetes
   * Testmodell hätte den 404 verschwiegen, den der Test finden soll.
   */
  suspend fun testApiKey(model: String = preferences.getAiSettings().chatModel): Pair<Boolean, String> =
    geminiClient.testConnection(getEffectiveApiKey(), model)

  suspend fun fetchModelCatalog(): com.example.data.model.GeminiModelCatalog = geminiClient.fetchModelCatalog()

  suspend fun transcribeAudio(
    audioBytes: ByteArray,
    mimeType: String = "audio/mp4"
  ): String = geminiClient.transcribeAudio(
    audioBytes,
    mimeType,
    preferences.getAiSettings().transcriptionModel
  )

  // --- STORY PERSISTENCE (ROOM) ---

  fun getAllStories(): Flow<List<StoryEntity>> = storyDao.getAllStories()

  fun getMessagesForStory(storyId: Long): Flow<List<MessageEntity>> = storyDao.getMessagesForStory(storyId)

  fun observeLatestCheckpoint(storyId: Long): Flow<CheckpointEntity?> = storyDao.observeLatestCheckpoint(storyId)

  fun getAllCheckpoints(storyId: Long): Flow<List<CheckpointEntity>> = storyDao.getAllCheckpoints(storyId)

  suspend fun getStoryById(storyId: Long): StoryEntity? = storyDao.getStoryById(storyId)

  suspend fun updateStory(story: StoryEntity) = storyDao.updateStory(story)

  /** Was der Prompt-Bogen ändern darf — gezielt, damit kein laufender Zug es zurückdreht. */
  suspend fun updateStoryTitleAndPrompt(storyId: Long, title: String, systemPrompt: String) =
    storyDao.updateStoryTitleAndPrompt(
      id = storyId,
      title = title.trim(),
      systemPrompt = systemPrompt.trim(),
      updatedAt = System.currentTimeMillis()
    )

  suspend fun toggleStoryArchived(storyId: Long, isArchived: Boolean) = withContext(Dispatchers.IO) {
    val story = storyDao.getStoryById(storyId) ?: return@withContext
    storyDao.updateStory(story.copy(isArchived = isArchived, updatedAt = System.currentTimeMillis()))
  }

  /**
   * Löscht eine Geschichte samt allem, was an ihr hängt — in einem Stück.
   *
   * Es gibt keine ForeignKeys in diesem Schema: Bricht eine der fünf Löschungen ab, bleiben
   * Nachrichten, Checkpoints oder Erinnerungen als Waisen einer Geschichte liegen, die der
   * Nutzer für gelöscht hält. Die Transaktion macht daraus ein Alles-oder-nichts.
   */
  suspend fun deleteStoryById(storyId: Long) = withContext(Dispatchers.IO) {
    database.withTransaction {
      storyDao.deleteAllMessagesForStory(storyId)
      storyDao.deleteAllCheckpointsForStory(storyId)
      storyDao.deleteAllMemoriesForStory(storyId)
      storyDao.deleteAllNpcsForStory(storyId)
      storyDao.deleteStoryById(storyId)
    }
    memoryEngine.invalidate()
  }

  /**
   * Legt eine Geschichte an, die nur aus ihrem Prompt besteht.
   *
   * Titel, Genre, Ort, Wetter, Outfit, Inventar und Begleiter bleiben leer: Sie stehen im
   * Prompt und werden von der Extraktion des ersten Zuges daraus hergeleitet. Ein
   * vorgeschriebener Prolog entfällt — die Geschichte beginnt direkt mit dem ersten Zug.
   */
  suspend fun createStory(systemPrompt: String): Long = withContext(Dispatchers.IO) {
    val storyId = storyDao.insertStory(
      StoryEntity(
        title = "",
        genre = "",
        systemPrompt = systemPrompt.trim()
      )
    )

    val rawStateObj = JSONObject().apply {
      put("in_game_time", "Tag 1, 20:00 Uhr")
      put("location", "")
      put("weather", "")
      put("player", JSONObject().apply {
        put("outfit", "")
        put("condition", "Unverletzt")
        put("inventory", JSONArray())
      })
      put("npcs_present", JSONArray())
      put("previous_events_summary", "Das Abenteuer beginnt.")
    }

    storyDao.insertCheckpoint(
      CheckpointEntity(
        storyId = storyId,
        turnNumber = 0,
        inGameTime = "Tag 1, 20:00 Uhr",
        location = "",
        weather = "",
        playerOutfit = "",
        playerInventory = "[]",
        playerCondition = "Unverletzt",
        npcsJson = "[]",
        milestonesJson = "[]",
        previousEventsSummary = "Das Abenteuer beginnt.",
        rawStateJson = rawStateObj.toString()
      )
    )

    storyId
  }

  /** Erzählt den Eröffnungszug, ohne dass der Spieler etwas eingeben muss. */
  fun openStory(storyId: Long, onChunk: (String) -> Unit): Flow<TurnProgress> =
    turnEngine.openStory(storyId, onChunk)

  /** true, solange noch kein einziger Zug erzählt wurde. */
  suspend fun isUnopened(storyId: Long): Boolean = withContext(Dispatchers.IO) {
    storyDao.getRecentMessages(storyId, 1).isEmpty()
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
    // Der Vektor-Cache zeigt jetzt auf gelöschte Erinnerungen.
    memoryEngine.invalidate()
    return turnEngine.regenerateTurn(storyId, newContent, onChunk)
  }

  // --- BRANCHING & ROLLBACK ---

  suspend fun branchStory(
    sourceStoryId: Long,
    branchTitle: String,
    upToMessageId: Long? = null
  ): Long {
    val newId = branchingService.branchStory(sourceStoryId, branchTitle, upToMessageId)
    memoryEngine.invalidate()
    return newId
  }

  suspend fun rewindToMessage(
    storyId: Long,
    message: MessageEntity
  ) {
    branchingService.rewindToMessage(storyId, message)
    memoryEngine.invalidate()
  }

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
