package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.api.GeminiClient
import com.example.data.db.StoryDao
import com.example.data.model.CheckpointEntity
import com.example.data.model.MessageEntity
import com.example.data.model.StoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class StoryRepository(
  private val storyDao: StoryDao,
  private val context: Context
) {
  private val prefs: SharedPreferences = context.getSharedPreferences("storyforge_prefs", Context.MODE_PRIVATE)

  private val geminiClient = GeminiClient(
    customApiKeyProvider = { getCustomApiKey() }
  )

  companion object {
    private const val TAG = "StoryRepository"
    private const val PREF_CUSTOM_API_KEY = "custom_gemini_api_key"
    private const val PREF_GLOBAL_DEFAULT_PROMPT = "global_default_system_prompt"
  }

  fun getCustomApiKey(): String? {
    return prefs.getString(PREF_CUSTOM_API_KEY, null)
  }

  fun setCustomApiKey(key: String?) {
    prefs.edit().putString(PREF_CUSTOM_API_KEY, key?.trim()).apply()
  }

  fun getGlobalDefaultSystemPrompt(): String {
    return prefs.getString(PREF_GLOBAL_DEFAULT_PROMPT, GeminiClient.DEFAULT_SYSTEM_PROMPT)
      ?: GeminiClient.DEFAULT_SYSTEM_PROMPT
  }

  fun setGlobalDefaultSystemPrompt(prompt: String) {
    prefs.edit().putString(PREF_GLOBAL_DEFAULT_PROMPT, prompt.trim()).apply()
  }

  fun getEffectiveApiKey(): String = geminiClient.getEffectiveApiKey()

  suspend fun testApiKey(): Pair<Boolean, String> = geminiClient.testConnection(getEffectiveApiKey())

  fun getAllStories(): Flow<List<StoryEntity>> = storyDao.getAllStories()

  fun getMessagesForStory(storyId: Long): Flow<List<MessageEntity>> = storyDao.getMessagesForStory(storyId)

  fun observeLatestCheckpoint(storyId: Long): Flow<CheckpointEntity?> = storyDao.observeLatestCheckpoint(storyId)

  fun getAllCheckpoints(storyId: Long): Flow<List<CheckpointEntity>> = storyDao.getAllCheckpoints(storyId)

  suspend fun getStoryById(storyId: Long): StoryEntity? = storyDao.getStoryById(storyId)

  suspend fun updateStory(story: StoryEntity) = storyDao.updateStory(story)

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
      selectedModel = "gemini-2.5-flash",
      temperature = 0.85f,
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

  fun executeTurn(
    storyId: Long,
    userAction: String,
    onChunk: (String) -> Unit
  ): Flow<TurnProgress> = streamNextTurn(storyId, userAction, onChunk)

  suspend fun createStory(
    title: String,
    genre: String,
    perspective: String = "Zweite Person (Du)",
    systemPrompt: String = "", // Empty means: use global default prompt
    selectedModel: String = "gemini-2.5-flash",
    temperature: Float = 0.85f,
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
      temperature = temperature,
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

  /**
   * Branches a story (Zweig erstellen): creates an isolated clone of the story,
   * its messages up to target turn, and its state.
   */
  suspend fun branchStory(sourceStoryId: Long, branchTitle: String, upToMessageId: Long? = null): Long = withContext(Dispatchers.IO) {
    val sourceStory = storyDao.getStoryById(sourceStoryId) ?: throw IllegalArgumentException("Quell-Story nicht gefunden")
    val newStory = sourceStory.copy(
      id = 0,
      title = branchTitle.ifBlank { "${sourceStory.title} (Zweig)" },
      createdAt = System.currentTimeMillis(),
      updatedAt = System.currentTimeMillis()
    )
    val newStoryId = storyDao.insertStory(newStory)

    // Copy messages
    val messages = storyDao.getMessagesSnapshot(sourceStoryId)
    val messagesToCopy = if (upToMessageId != null) {
      messages.filter { it.id <= upToMessageId }
    } else {
      messages
    }

    for (m in messagesToCopy) {
      storyDao.insertMessage(
        m.copy(
          id = 0,
          storyId = newStoryId
        )
      )
    }

    // Copy latest checkpoint
    val latestCp = storyDao.getLatestCheckpoint(sourceStoryId)
    if (latestCp != null) {
      storyDao.insertCheckpoint(
        latestCp.copy(
          id = 0,
          storyId = newStoryId
        )
      )
    }

    newStoryId
  }

  /**
   * Main turn-based loop:
   * 1. Inserts user message
   * 2. Retrieves state anchor, milestones & history
   * 3. Streams GM response from Gemini
   * 4. Inserts GM message
   * 5. Asynchronously triggers state & milestone extraction
   */
  fun streamNextTurn(
    storyId: Long,
    userAction: String,
    onChunk: (String) -> Unit
  ): Flow<TurnProgress> = flow {
    emit(TurnProgress.Thinking)

    val story = storyDao.getStoryById(storyId)
      ?: throw IllegalStateException("Story $storyId existiert nicht.")

    // 1. Insert user message into DB
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

    // Prepare context window
    val allMessages = storyDao.getMessagesSnapshot(storyId)
    // Take last 8 turns (sliding window)
    val slidingWindow = allMessages.takeLast(8).map {
      it.sender to it.content
    }

    val stateJson = latestCheckpoint?.rawStateJson ?: ""
    val summary = latestCheckpoint?.previousEventsSummary ?: ""
    val milestones = latestCheckpoint?.getMilestonesList() ?: emptyList()

    // Determine effective system instruction: story-specific prompt overrides global default
    val effectivePrompt = if (story.systemPrompt.isNotBlank()) {
      story.systemPrompt
    } else {
      getGlobalDefaultSystemPrompt()
    }

    val fullResponseBuffer = StringBuilder()

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
        thinkingBudget = story.thinkingBudget,
        allowAdultContent = story.adultContentEnabled
      ).collect { chunk ->
        fullResponseBuffer.append(chunk)
        onChunk(chunk)
      }
    } catch (e: Exception) {
      Log.e(TAG, "Turn generation failed", e)
      emit(TurnProgress.Failed(e.localizedMessage ?: "Fehler bei der Textgenerierung."))
      return@flow
    }

    val finalGmText = fullResponseBuffer.toString()
    if (finalGmText.isBlank()) {
      emit(TurnProgress.Failed("Die KI hat keine Antwort generiert. Bitte prüfe den API-Key oder die Verbindung."))
      return@flow
    }

    // Insert GM message
    val currentTurn = (latestCheckpoint?.turnNumber ?: 0) + 1
    val modelMsgId = storyDao.insertMessage(
      MessageEntity(
        storyId = storyId,
        sender = "model",
        content = finalGmText,
        inGameTimeTag = latestCheckpoint?.inGameTime
      )
    )

    emit(TurnProgress.ExtractingState)

    // Background State Extractor: extract discrete state & update checkpoint
    try {
      val updatedStateJsonObj = geminiClient.extractUpdatedState(
        model = "gemini-2.5-flash",
        currentStateJson = stateJson,
        userAction = userAction,
        modelNarrative = finalGmText,
        previousMilestones = milestones
      )

      val newInGameTime = updatedStateJsonObj.optString("in_game_time", latestCheckpoint?.inGameTime ?: "Tag 1, 21:45 Uhr")
      val newLocation = updatedStateJsonObj.optString("location", latestCheckpoint?.location ?: "Aktueller Ort")
      val newWeather = updatedStateJsonObj.optString("weather", latestCheckpoint?.weather ?: "Klar")
      val newOutfit = updatedStateJsonObj.optString("player_outfit", latestCheckpoint?.playerOutfit ?: "")
      val newCondition = updatedStateJsonObj.optString("player_condition", latestCheckpoint?.playerCondition ?: "Unverletzt")

      val newInventoryArray = updatedStateJsonObj.optJSONArray("player_inventory")
      val invJson = newInventoryArray?.toString() ?: (latestCheckpoint?.playerInventory ?: "[]")

      val npcsArray = updatedStateJsonObj.optJSONArray("npcs")
      val npcsJson = npcsArray?.toString() ?: (latestCheckpoint?.npcsJson ?: "[]")

      val milestonesArray = updatedStateJsonObj.optJSONArray("milestones")
      val milestonesJson = milestonesArray?.toString() ?: (latestCheckpoint?.milestonesJson ?: "[]")

      val newSummary = updatedStateJsonObj.optString("previous_events_summary", summary)

      val newCheckpoint = CheckpointEntity(
        storyId = storyId,
        turnNumber = currentTurn,
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

      val newCpId = storyDao.insertCheckpoint(newCheckpoint)

      // Link message with new checkpoint
      storyDao.insertMessage(
        MessageEntity(
          id = modelMsgId,
          storyId = storyId,
          sender = "model",
          content = finalGmText,
          inGameTimeTag = newInGameTime,
          checkpointId = newCpId
        )
      )
    } catch (e: Exception) {
      Log.w(TAG, "State extraction fallback", e)
      if (latestCheckpoint != null) {
        storyDao.insertCheckpoint(
          latestCheckpoint.copy(
            id = 0,
            turnNumber = currentTurn,
            timestamp = System.currentTimeMillis()
          )
        )
      }
    }

    emit(TurnProgress.Completed)
  }

  /**
   * Edit user message & truncate future:
   * 1. Deletes all messages following the edited message (clean purge).
   * 2. Updates the message content in DB.
   * 3. Re-generates GM response from this point.
   */
  fun editUserMessageAndRegenerate(
    storyId: Long,
    messageId: Long,
    newContent: String,
    onChunk: (String) -> Unit
  ): Flow<TurnProgress> = flow {
    emit(TurnProgress.Thinking)

    withContext(Dispatchers.IO) {
      // 1. Delete all messages strictly after this one
      storyDao.deleteMessagesAfter(storyId, messageId)
      // 2. Update message content
      storyDao.updateMessageContent(messageId, newContent)

      // Rollback checkpoints if needed
      val allRemaining = storyDao.getMessagesSnapshot(storyId)
      val userTurnIdx = allRemaining.count { it.sender == "user" }
      storyDao.deleteCheckpointsAfterTurn(storyId, userTurnIdx)
    }

    val story = storyDao.getStoryById(storyId)
      ?: throw IllegalStateException("Story $storyId nicht gefunden.")

    emit(TurnProgress.Streaming)

    val allMessages = storyDao.getMessagesSnapshot(storyId)
    val latestCheckpoint = storyDao.getLatestCheckpoint(storyId)
    val stateJson = latestCheckpoint?.rawStateJson ?: ""
    val summary = latestCheckpoint?.previousEventsSummary ?: ""
    val milestones = latestCheckpoint?.getMilestonesList() ?: emptyList()

    val slidingWindow = allMessages.dropLast(1).takeLast(6).map {
      it.sender to it.content
    }

    val effectivePrompt = if (story.systemPrompt.isNotBlank()) {
      story.systemPrompt
    } else {
      getGlobalDefaultSystemPrompt()
    }

    val fullResponseBuffer = StringBuilder()

    try {
      geminiClient.streamGenerateStory(
        model = story.selectedModel,
        systemInstruction = effectivePrompt,
        stateJson = stateJson,
        episodicSummary = summary,
        milestones = milestones,
        recentHistory = slidingWindow,
        userAction = newContent,
        temperature = story.temperature,
        thinkingBudget = story.thinkingBudget,
        allowAdultContent = story.adultContentEnabled
      ).collect { chunk ->
        fullResponseBuffer.append(chunk)
        onChunk(chunk)
      }
    } catch (e: Exception) {
      Log.e(TAG, "Edit turn regeneration failed", e)
      emit(TurnProgress.Failed(e.localizedMessage ?: "Fehler beim Fortführen der Geschichte."))
      return@flow
    }

    val finalGmText = fullResponseBuffer.toString()
    if (finalGmText.isBlank()) {
      emit(TurnProgress.Failed("Keine Antwort erhalten."))
      return@flow
    }

    val currentTurn = (latestCheckpoint?.turnNumber ?: 0) + 1
    val modelMsgId = storyDao.insertMessage(
      MessageEntity(
        storyId = storyId,
        sender = "model",
        content = finalGmText,
        inGameTimeTag = latestCheckpoint?.inGameTime
      )
    )

    emit(TurnProgress.ExtractingState)

    try {
      val updatedStateJsonObj = geminiClient.extractUpdatedState(
        model = "gemini-2.5-flash",
        currentStateJson = stateJson,
        userAction = newContent,
        modelNarrative = finalGmText,
        previousMilestones = milestones
      )

      val newInGameTime = updatedStateJsonObj.optString("in_game_time", latestCheckpoint?.inGameTime ?: "Tag 1, 21:45 Uhr")
      val newLocation = updatedStateJsonObj.optString("location", latestCheckpoint?.location ?: "Aktueller Ort")
      val newWeather = updatedStateJsonObj.optString("weather", latestCheckpoint?.weather ?: "Klar")
      val newOutfit = updatedStateJsonObj.optString("player_outfit", latestCheckpoint?.playerOutfit ?: "")
      val newCondition = updatedStateJsonObj.optString("player_condition", latestCheckpoint?.playerCondition ?: "Unverletzt")

      val newInventoryArray = updatedStateJsonObj.optJSONArray("player_inventory")
      val invJson = newInventoryArray?.toString() ?: (latestCheckpoint?.playerInventory ?: "[]")

      val npcsArray = updatedStateJsonObj.optJSONArray("npcs")
      val npcsJson = npcsArray?.toString() ?: (latestCheckpoint?.npcsJson ?: "[]")

      val milestonesArray = updatedStateJsonObj.optJSONArray("milestones")
      val milestonesJson = milestonesArray?.toString() ?: (latestCheckpoint?.milestonesJson ?: "[]")

      val newSummary = updatedStateJsonObj.optString("previous_events_summary", summary)

      val newCheckpoint = CheckpointEntity(
        storyId = storyId,
        turnNumber = currentTurn,
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

      val newCpId = storyDao.insertCheckpoint(newCheckpoint)

      storyDao.insertMessage(
        MessageEntity(
          id = modelMsgId,
          storyId = storyId,
          sender = "model",
          content = finalGmText,
          inGameTimeTag = newInGameTime,
          checkpointId = newCpId
        )
      )
    } catch (e: Exception) {
      Log.w(TAG, "State extraction fallback on edit", e)
    }

    emit(TurnProgress.Completed)
  }

  suspend fun rewindToMessage(storyId: Long, message: MessageEntity) = withContext(Dispatchers.IO) {
    storyDao.deleteMessagesAfter(storyId, message.id)
    val latestCp = storyDao.getLatestCheckpoint(storyId)
    if (message.checkpointId != null && latestCp != null && message.checkpointId < latestCp.id) {
      storyDao.deleteCheckpointsAfterTurn(storyId, latestCp.turnNumber - 1)
    }
  }

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
}

sealed class TurnProgress {
  data object Thinking : TurnProgress()
  data object Streaming : TurnProgress()
  data object ExtractingState : TurnProgress()
  data object Completed : TurnProgress()
  data class Failed(val error: String) : TurnProgress()
}
