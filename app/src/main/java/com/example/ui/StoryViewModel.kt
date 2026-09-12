package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiClient
import com.example.data.db.StoryDatabase
import com.example.data.model.CheckpointEntity
import com.example.data.model.GeminiModelInfo
import com.example.data.model.MessageEntity
import com.example.data.model.StoryEntity
import com.example.data.repository.StoryRepository
import com.example.domain.model.TurnProgress
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StoryUiState(
  val currentStory: StoryEntity? = null,
  val allStories: List<StoryEntity> = emptyList(),
  val messages: List<MessageEntity> = emptyList(),
  val latestCheckpoint: CheckpointEntity? = null,
  val allCheckpoints: List<CheckpointEntity> = emptyList(),
  val isGenerating: Boolean = false,
  val streamChunk: String = "",
  val turnStatus: TurnProgress? = null,
  val errorMessage: String? = null,
  val customApiKey: String = "",
  val globalDefaultPrompt: String = "",
  val effectiveApiKeyPresent: Boolean = false,
  val availableModels: List<GeminiModelInfo> = GeminiClient.DEFAULT_CHAT_MODELS,
  val availableChatModels: List<GeminiModelInfo> = GeminiClient.DEFAULT_CHAT_MODELS,
  val availableEmbeddingModels: List<GeminiModelInfo> = GeminiClient.DEFAULT_EMBEDDING_MODELS,
  val availableTranscriptionModels: List<GeminiModelInfo> = GeminiClient.DEFAULT_TRANSCRIPTION_MODELS,
  val isFetchingModels: Boolean = false,
  val isTranscribingAudio: Boolean = false,
  /** false = Modell-Dropdowns zeigen die Fallback-Liste, der Schlüssel ist also unbestätigt. */
  val modelCatalogIsLive: Boolean = false
)

class StoryViewModel(application: Application) : AndroidViewModel(application) {
  private val database = StoryDatabase.getInstance(application)
  val repository = StoryRepository(database.storyDao(), application)

  private val _activeStoryId = MutableStateFlow<Long?>(null)
  private val _isGenerating = MutableStateFlow(false)
  private val _streamChunk = MutableStateFlow("")
  private val _turnStatus = MutableStateFlow<TurnProgress?>(null)
  private val _errorMessage = MutableStateFlow<String?>(null)
  private val _customApiKey = MutableStateFlow(repository.getCustomApiKey() ?: "")
  private val _globalDefaultPrompt = MutableStateFlow(repository.getGlobalDefaultSystemPrompt())
  private val _availableModels = MutableStateFlow<List<GeminiModelInfo>>(GeminiClient.DEFAULT_CHAT_MODELS)
  private val _availableChatModels = MutableStateFlow<List<GeminiModelInfo>>(GeminiClient.DEFAULT_CHAT_MODELS)
  private val _availableEmbeddingModels = MutableStateFlow<List<GeminiModelInfo>>(GeminiClient.DEFAULT_EMBEDDING_MODELS)
  private val _availableTranscriptionModels = MutableStateFlow<List<GeminiModelInfo>>(GeminiClient.DEFAULT_TRANSCRIPTION_MODELS)
  private val _isFetchingModels = MutableStateFlow(false)
  private val _isTranscribingAudio = MutableStateFlow(false)
  private val _modelCatalogIsLive = MutableStateFlow(false)

  private var activeTurnJob: Job? = null

  val stories: StateFlow<List<StoryEntity>> = repository.getAllStories()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val _messages = MutableStateFlow<List<MessageEntity>>(emptyList())
  val messages: StateFlow<List<MessageEntity>> = _messages.asStateFlow()

  private val _latestCheckpoint = MutableStateFlow<CheckpointEntity?>(null)
  val latestCheckpoint: StateFlow<CheckpointEntity?> = _latestCheckpoint.asStateFlow()

  private val _allCheckpoints = MutableStateFlow<List<CheckpointEntity>>(emptyList())
  val allCheckpoints: StateFlow<List<CheckpointEntity>> = _allCheckpoints.asStateFlow()

  private var messagesJob: Job? = null
  private var checkpointJob: Job? = null
  private var allCheckpointsJob: Job? = null

  init {
    viewModelScope.launch {
      val defaultStoryId = repository.ensureInitialData()
      switchStory(defaultStoryId)
      refreshModelsFromGoogle()
    }
  }

  fun refreshModelsFromGoogle() {
    viewModelScope.launch {
      _isFetchingModels.value = true
      try {
        val catalog = repository.fetchModelCatalog()
        _modelCatalogIsLive.value = catalog.isLive
        if (catalog.chatModels.isNotEmpty()) {
          _availableModels.value = catalog.chatModels
          _availableChatModels.value = catalog.chatModels
        }
        if (catalog.embeddingModels.isNotEmpty()) {
          _availableEmbeddingModels.value = catalog.embeddingModels
        }
        if (catalog.transcriptionModels.isNotEmpty()) {
          _availableTranscriptionModels.value = catalog.transcriptionModels
        }
      } catch (e: Exception) {
        Log.e("StoryViewModel", "Failed to refresh model catalog", e)
        _modelCatalogIsLive.value = false
      } finally {
        _isFetchingModels.value = false
      }
    }
  }

  fun switchStory(storyId: Long) {
    _activeStoryId.value = storyId
    messagesJob?.cancel()
    checkpointJob?.cancel()
    allCheckpointsJob?.cancel()

    messagesJob = viewModelScope.launch {
      repository.getMessagesForStory(storyId).collect { msgs ->
        _messages.value = msgs
      }
    }

    checkpointJob = viewModelScope.launch {
      repository.observeLatestCheckpoint(storyId).collect { cp ->
        _latestCheckpoint.value = cp
      }
    }

    allCheckpointsJob = viewModelScope.launch {
      repository.getAllCheckpoints(storyId).collect { list ->
        _allCheckpoints.value = list
      }
    }
  }

  fun toggleStoryArchived(storyId: Long, isArchived: Boolean) {
    viewModelScope.launch {
      repository.toggleStoryArchived(storyId, isArchived)
    }
  }

  fun sendAction(actionText: String) {
    val cleanAction = actionText.trim()
    if (cleanAction.isBlank()) return
    val storyId = _activeStoryId.value ?: return

    if (_isGenerating.value) return

    _isGenerating.value = true
    _streamChunk.value = ""
    _turnStatus.value = TurnProgress.Thinking
    _errorMessage.value = null

    activeTurnJob?.cancel()
    activeTurnJob = viewModelScope.launch {
      try {
        repository.executeTurn(
          storyId = storyId,
          userAction = cleanAction,
          onChunk = { chunk ->
            _streamChunk.value += chunk
          }
        ).catch { e ->
          Log.e("StoryViewModel", "Turn execution failed", e)
          _errorMessage.value = e.message ?: "Ein unerwarteter Fehler ist aufgetreten."
          _isGenerating.value = false
          _streamChunk.value = ""
          _turnStatus.value = null
        }.collect { progress ->
          _turnStatus.value = progress
          when (progress) {
            is TurnProgress.Failed -> {
              _errorMessage.value = progress.error
              _isGenerating.value = false
              _streamChunk.value = ""
            }
            TurnProgress.Completed -> {
              _isGenerating.value = false
              _streamChunk.value = ""
              _turnStatus.value = null
            }
            else -> {}
          }
        }
      } catch (e: Throwable) {
        Log.e("StoryViewModel", "Turn coroutine failed", e)
        _errorMessage.value = e.message ?: "Ein unerwarteter Fehler ist aufgetreten."
        _isGenerating.value = false
        _streamChunk.value = ""
        _turnStatus.value = null
      }
    }
  }

  fun rewindTo(message: MessageEntity) {
    val storyId = _activeStoryId.value ?: return
    viewModelScope.launch {
      repository.rewindToMessage(storyId, message)
    }
  }

  fun editMessageAndRewind(messageId: Long, newContent: String) {
    val clean = newContent.trim()
    if (clean.isBlank()) return
    val storyId = _activeStoryId.value ?: return

    if (_isGenerating.value) return

    _isGenerating.value = true
    _streamChunk.value = ""
    _turnStatus.value = TurnProgress.Thinking
    _errorMessage.value = null

    activeTurnJob?.cancel()
    activeTurnJob = viewModelScope.launch {
      try {
        repository.editUserMessageAndRegenerate(
          storyId = storyId,
          messageId = messageId,
          newContent = clean,
          onChunk = { chunk ->
            _streamChunk.value += chunk
          }
        ).catch { e ->
          Log.e("StoryViewModel", "Edit message failed in flow", e)
          _errorMessage.value = e.message ?: "Fehler beim Bearbeiten der Nachricht"
          _isGenerating.value = false
          _streamChunk.value = ""
          _turnStatus.value = null
        }.collect { progress ->
          _turnStatus.value = progress
          when (progress) {
            is TurnProgress.Failed -> {
              _errorMessage.value = progress.error
              _isGenerating.value = false
              _streamChunk.value = ""
            }
            TurnProgress.Completed -> {
              _isGenerating.value = false
              _streamChunk.value = ""
              _turnStatus.value = null
            }
            else -> {}
          }
        }
      } catch (e: Throwable) {
        Log.e("StoryViewModel", "Edit message coroutine failed", e)
        _errorMessage.value = e.message ?: "Fehler beim Bearbeiten der Nachricht"
        _isGenerating.value = false
        _streamChunk.value = ""
        _turnStatus.value = null
      }
    }
  }

  fun transcribeVoiceInput(audioBytes: ByteArray, onTranscribed: (String) -> Unit) {
    if (audioBytes.isEmpty()) return
    val currentStory = stories.value.firstOrNull { it.id == _activeStoryId.value }
    val transcriptionModel = currentStory?.selectedTranscriptionModel ?: "gemini-2.5-flash"
    viewModelScope.launch {
      _isTranscribingAudio.value = true
      try {
        val transcribedText = repository.transcribeAudio(
          audioBytes = audioBytes,
          mimeType = "audio/mp4",
          model = transcriptionModel
        )
        if (transcribedText.isNotBlank()) {
          onTranscribed(transcribedText)
        }
      } catch (e: Throwable) {
        Log.e("StoryViewModel", "Voice transcription failed", e)
        _errorMessage.value = "Sprachtranskription fehlgeschlagen: ${e.message ?: "Unbekannter Fehler"}"
      } finally {
        _isTranscribingAudio.value = false
      }
    }
  }

  fun branchStoryFromTurn(targetMessageId: Long, branchTitle: String) {
    val currentStoryId = _activeStoryId.value ?: return
    viewModelScope.launch {
      val newId = repository.branchStory(
        sourceStoryId = currentStoryId,
        branchTitle = branchTitle,
        upToMessageId = targetMessageId
      )
      switchStory(newId)
    }
  }

  fun deleteCurrentStory() {
    val currentStoryId = _activeStoryId.value ?: return
    viewModelScope.launch {
      repository.deleteStoryById(currentStoryId)
      val allRemaining = repository.getAllStories()
      val firstRemaining = stories.value.firstOrNull { it.id != currentStoryId }
      if (firstRemaining != null) {
        switchStory(firstRemaining.id)
      } else {
        val newId = repository.ensureInitialData()
        switchStory(newId)
      }
    }
  }

  fun deleteStoryById(id: Long) {
    viewModelScope.launch {
      repository.deleteStoryById(id)
      if (_activeStoryId.value == id) {
        val firstRemaining = stories.value.firstOrNull { it.id != id }
        if (firstRemaining != null) {
          switchStory(firstRemaining.id)
        } else {
          val newId = repository.ensureInitialData()
          switchStory(newId)
        }
      }
    }
  }

  fun editUserMessage(message: MessageEntity, newContent: String) {
    editMessageAndRewind(message.id, newContent)
  }

  fun branchStory(sourceStoryId: Long, branchTitle: String) {
    viewModelScope.launch {
      val newId = repository.branchStory(
        sourceStoryId = sourceStoryId,
        branchTitle = branchTitle,
        upToMessageId = null
      )
      switchStory(newId)
    }
  }

  fun deleteStory(id: Long) {
    deleteStoryById(id)
  }

  fun updateStorySettings(
    title: String,
    systemPrompt: String,
    model: String,
    embeddingModel: String = "text-embedding-004",
    transcriptionModel: String = "gemini-2.5-flash",
    temperature: Float,
    supportsTemperature: Boolean,
    thinkingLevel: String,
    thinkingBudget: Int,
    adultContent: Boolean
  ) {
    updateStoryConfig(
      title = title,
      systemPrompt = systemPrompt,
      model = model,
      embeddingModel = embeddingModel,
      transcriptionModel = transcriptionModel,
      temperature = temperature,
      supportsTemperature = supportsTemperature,
      thinkingLevel = thinkingLevel,
      thinkingBudget = thinkingBudget,
      adultContent = adultContent
    )
  }

  fun updateStoryConfig(
    title: String,
    systemPrompt: String,
    model: String,
    embeddingModel: String = "text-embedding-004",
    transcriptionModel: String = "gemini-2.5-flash",
    temperature: Float,
    supportsTemperature: Boolean,
    thinkingLevel: String,
    thinkingBudget: Int,
    adultContent: Boolean
  ) {
    val current = stories.value.firstOrNull { it.id == _activeStoryId.value } ?: return
    viewModelScope.launch {
      repository.updateStory(
        current.copy(
          title = title,
          systemPrompt = systemPrompt,
          selectedModel = model,
          selectedEmbeddingModel = embeddingModel,
          selectedTranscriptionModel = transcriptionModel,
          temperature = temperature,
          supportsTemperature = supportsTemperature,
          thinkingLevel = thinkingLevel,
          thinkingBudget = thinkingBudget,
          adultContentEnabled = adultContent,
          updatedAt = System.currentTimeMillis()
        )
      )
    }
  }

  fun saveGlobalDefaultPrompt(prompt: String) {
    _globalDefaultPrompt.value = prompt
    repository.setGlobalDefaultSystemPrompt(prompt)
  }

  fun updateManualState(
    inGameTime: String,
    location: String,
    weather: String,
    playerOutfit: String,
    playerCondition: String,
    inventory: List<String>,
    npcsJson: String,
    summary: String
  ) {
    val storyId = _activeStoryId.value ?: return
    viewModelScope.launch {
      repository.updateCurrentState(
        storyId = storyId,
        inGameTime = inGameTime,
        location = location,
        weather = weather,
        playerOutfit = playerOutfit,
        playerCondition = playerCondition,
        playerInventory = inventory,
        npcs = npcsJson,
        summary = summary
      )
    }
  }

  /**
   * Aktualisiert koerperliche Verletzungen fuer einen bestimmten Charakter
   * und synchronisiert sie direkt im Checkpoint und der State-JSON.
   */
  fun updateCharacterInjuries(characterName: String, updatedInjuries: List<com.example.data.model.CharacterInjury>) {
    val currentCp = _latestCheckpoint.value ?: return
    viewModelScope.launch {
      val rawObj = try {
        if (currentCp.rawStateJson.isNotBlank()) org.json.JSONObject(currentCp.rawStateJson) else org.json.JSONObject()
      } catch (_: Exception) {
        org.json.JSONObject()
      }

      val allInjuriesList = mutableListOf<com.example.data.model.CharacterInjury>()
      val existingInjuries = try {
        val arr = rawObj.optJSONArray("injuries") ?: org.json.JSONArray()
        val list = mutableListOf<com.example.data.model.CharacterInjury>()
        for (i in 0 until arr.length()) {
          arr.optJSONObject(i)?.let { obj -> list.add(com.example.data.model.CharacterInjury.fromJson(obj)) }
        }
        list
      } catch (_: Exception) {
        emptyList()
      }

      val isPlayer = characterName.equals("Du", ignoreCase = true) || characterName.equals("Spieler", ignoreCase = true)
      allInjuriesList.addAll(existingInjuries.filterNot {
        if (isPlayer) it.characterName.equals("Du", ignoreCase = true) || it.characterName.equals("Spieler", ignoreCase = true)
        else it.characterName.equals(characterName, ignoreCase = true)
      })
      allInjuriesList.addAll(updatedInjuries)

      val injuriesArray = org.json.JSONArray()
      allInjuriesList.forEach { injuriesArray.put(it.toJson()) }
      rawObj.put("injuries", injuriesArray)

      val newCondition = if (isPlayer) {
        if (updatedInjuries.isEmpty()) "Unverletzt"
        else updatedInjuries.joinToString("; ") { "${it.bodyPart.displayName}: ${it.description} (${it.severity.displayName})" }
      } else currentCp.playerCondition

      val updatedCp = currentCp.copy(
        playerCondition = newCondition,
        rawStateJson = rawObj.toString()
      )
      repository.updateCheckpointDirectly(updatedCp)
      _latestCheckpoint.value = updatedCp
    }
  }

  fun saveCustomApiKey(key: String) {
    val clean = key.trim().replace("\\s+".toRegex(), "")
    _customApiKey.value = clean
    repository.setCustomApiKey(if (clean.isBlank()) null else clean)
    refreshModelsFromGoogle()
  }

  suspend fun testApiKeyConnection(): Pair<Boolean, String> {
    val result = repository.testApiKey()
    if (result.first) {
      refreshModelsFromGoogle()
    }
    return result
  }

  fun createNewStory(
    title: String,
    genre: String,
    perspective: String,
    systemPrompt: String,
    selectedModel: String = "gemini-3.8-flash",
    selectedEmbeddingModel: String = "text-embedding-004",
    selectedTranscriptionModel: String = "gemini-2.5-flash",
    temperature: Float = 0.85f,
    supportsTemperature: Boolean = true,
    thinkingLevel: String = "MEDIUM",
    thinkingBudget: Int = 2048,
    adultContent: Boolean = true,
    initialLocation: String,
    initialOutfit: String,
    initialInventory: List<String>,
    initialNpcName: String,
    initialNpcOutfit: String,
    initialNpcRelation: String,
    openingText: String
  ) {
    viewModelScope.launch {
      val newId = repository.createStory(
        title = title,
        genre = genre,
        perspective = perspective,
        systemPrompt = systemPrompt,
        selectedModel = selectedModel,
        selectedEmbeddingModel = selectedEmbeddingModel,
        selectedTranscriptionModel = selectedTranscriptionModel,
        temperature = temperature,
        supportsTemperature = supportsTemperature,
        thinkingLevel = thinkingLevel,
        thinkingBudget = thinkingBudget,
        adultContent = adultContent,
        initialLocation = initialLocation,
        initialOutfit = initialOutfit,
        initialInventory = initialInventory,
        initialNpcName = initialNpcName,
        initialNpcOutfit = initialNpcOutfit,
        initialNpcRelation = initialNpcRelation,
        initialPromptOpening = openingText
      )
      switchStory(newId)
    }
  }

  fun updatePlayTime(storyId: Long, incrementSeconds: Long) {
    viewModelScope.launch {
      repository.updatePlayTime(storyId, incrementSeconds)
    }
  }

  fun dismissError() {
    _errorMessage.value = null
  }

  val uiState: StateFlow<StoryUiState> = combine(
    stories,
    _activeStoryId,
    _messages,
    _latestCheckpoint,
    _allCheckpoints,
    _isGenerating,
    _streamChunk,
    _turnStatus,
    _errorMessage,
    _customApiKey,
    _globalDefaultPrompt,
    _availableChatModels,
    _availableEmbeddingModels,
    _availableTranscriptionModels,
    _isFetchingModels,
    _isTranscribingAudio,
    _modelCatalogIsLive
  ) { args: Array<Any?> ->
    @Suppress("UNCHECKED_CAST")
    val storyList = args[0] as List<StoryEntity>
    val activeId = args[1] as Long?
    @Suppress("UNCHECKED_CAST")
    val msgs = args[2] as List<MessageEntity>
    val cp = args[3] as CheckpointEntity?
    @Suppress("UNCHECKED_CAST")
    val allCps = args[4] as List<CheckpointEntity>
    val generating = args[5] as Boolean
    val chunk = args[6] as String
    val turnStat = args[7] as TurnProgress?
    val err = args[8] as String?
    val customKey = args[9] as String
    val globalPrompt = args[10] as String
    @Suppress("UNCHECKED_CAST")
    val chatModels = args[11] as List<GeminiModelInfo>
    @Suppress("UNCHECKED_CAST")
    val embModels = args[12] as List<GeminiModelInfo>
    @Suppress("UNCHECKED_CAST")
    val transModels = args[13] as List<GeminiModelInfo>
    val fetchingModels = args[14] as Boolean
    val transcribingAudio = args[15] as Boolean
    val catalogIsLive = args[16] as Boolean

    val activeStory = storyList.firstOrNull { it.id == activeId } ?: storyList.firstOrNull()

    StoryUiState(
      currentStory = activeStory,
      allStories = storyList,
      messages = msgs,
      latestCheckpoint = cp,
      allCheckpoints = allCps,
      isGenerating = generating,
      streamChunk = chunk,
      turnStatus = turnStat,
      errorMessage = err,
      customApiKey = customKey,
      globalDefaultPrompt = globalPrompt,
      effectiveApiKeyPresent = repository.getEffectiveApiKey().isNotBlank(),
      availableModels = chatModels,
      availableChatModels = chatModels,
      availableEmbeddingModels = embModels,
      availableTranscriptionModels = transModels,
      isFetchingModels = fetchingModels,
      isTranscribingAudio = transcribingAudio,
      modelCatalogIsLive = catalogIsLive
    )
  }.stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    StoryUiState()
  )
}
