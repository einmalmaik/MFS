package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.StoryDatabase
import com.example.data.model.CheckpointEntity
import com.example.data.model.MessageEntity
import com.example.data.model.StoryEntity
import com.example.data.repository.StoryRepository
import com.example.data.repository.TurnProgress
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StoryUiState(
  val currentStory: StoryEntity? = null,
  val allStories: List<StoryEntity> = emptyList(),
  val messages: List<MessageEntity> = emptyList(),
  val latestCheckpoint: CheckpointEntity? = null,
  val isGenerating: Boolean = false,
  val streamChunk: String = "",
  val turnStatus: TurnProgress? = null,
  val errorMessage: String? = null,
  val customApiKey: String = "",
  val globalDefaultPrompt: String = "",
  val effectiveApiKeyPresent: Boolean = false
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

  private var activeTurnJob: Job? = null

  val stories: StateFlow<List<StoryEntity>> = repository.getAllStories()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val _messages = MutableStateFlow<List<MessageEntity>>(emptyList())
  val messages: StateFlow<List<MessageEntity>> = _messages.asStateFlow()

  private val _latestCheckpoint = MutableStateFlow<CheckpointEntity?>(null)
  val latestCheckpoint: StateFlow<CheckpointEntity?> = _latestCheckpoint.asStateFlow()

  private var messagesJob: Job? = null
  private var checkpointJob: Job? = null

  init {
    viewModelScope.launch {
      val defaultStoryId = repository.ensureInitialData()
      switchStory(defaultStoryId)
    }
  }

  fun switchStory(storyId: Long) {
    _activeStoryId.value = storyId
    messagesJob?.cancel()
    checkpointJob?.cancel()

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
      repository.executeTurn(
        storyId = storyId,
        userAction = cleanAction,
        onChunk = { chunk ->
          _streamChunk.value += chunk
        }
      ).collect { progress ->
        _turnStatus.value = progress
        when (progress) {
          is TurnProgress.Completed -> {
            _isGenerating.value = false
            _streamChunk.value = ""
          }
          is TurnProgress.Failed -> {
            _isGenerating.value = false
            _errorMessage.value = progress.error
          }
          else -> {}
        }
      }
    }
  }

  /**
   * Edit user message: deletes all messages after this one, updates the text in DB,
   * and regenerates the story from that exact point onwards.
   */
  fun editUserMessage(message: MessageEntity, newText: String) {
    val cleanText = newText.trim()
    if (cleanText.isBlank()) return
    val storyId = _activeStoryId.value ?: return
    if (_isGenerating.value) return

    _isGenerating.value = true
    _streamChunk.value = ""
    _turnStatus.value = TurnProgress.Thinking
    _errorMessage.value = null

    activeTurnJob?.cancel()
    activeTurnJob = viewModelScope.launch {
      repository.editUserMessageAndRegenerate(
        storyId = storyId,
        messageId = message.id,
        newContent = cleanText,
        onChunk = { chunk ->
          _streamChunk.value += chunk
        }
      ).collect { progress ->
        _turnStatus.value = progress
        when (progress) {
          is TurnProgress.Completed -> {
            _isGenerating.value = false
            _streamChunk.value = ""
          }
          is TurnProgress.Failed -> {
            _isGenerating.value = false
            _errorMessage.value = progress.error
          }
          else -> {}
        }
      }
    }
  }

  /**
   * Creates a branch from current story or specific message
   */
  fun branchStory(sourceStoryId: Long, branchTitle: String, upToMessageId: Long? = null) {
    viewModelScope.launch {
      val newId = repository.branchStory(sourceStoryId, branchTitle, upToMessageId)
      switchStory(newId)
    }
  }

  fun deleteStory(storyId: Long) {
    viewModelScope.launch {
      repository.deleteStoryById(storyId)
      val remaining = stories.value.filter { it.id != storyId }
      if (remaining.isNotEmpty()) {
        switchStory(remaining.first().id)
      } else {
        val newId = repository.ensureInitialData()
        switchStory(newId)
      }
    }
  }

  fun rewindTo(message: MessageEntity) {
    val storyId = _activeStoryId.value ?: return
    viewModelScope.launch {
      repository.rewindToMessage(storyId, message)
      _streamChunk.value = ""
      _isGenerating.value = false
      _errorMessage.value = null
    }
  }

  fun updateStorySettings(
    title: String,
    systemPrompt: String,
    model: String,
    temperature: Float,
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
          temperature = temperature,
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

  fun saveCustomApiKey(key: String) {
    _customApiKey.value = key.trim()
    repository.setCustomApiKey(key.trim())
  }

  suspend fun testApiKeyConnection(): Pair<Boolean, String> {
    return repository.testApiKey()
  }

  fun createNewStory(
    title: String,
    genre: String,
    perspective: String,
    systemPrompt: String,
    selectedModel: String = "gemini-2.5-flash",
    temperature: Float = 0.85f,
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
        temperature = temperature,
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

  fun dismissError() {
    _errorMessage.value = null
  }

  val uiState: StateFlow<StoryUiState> = combine(
    stories,
    _activeStoryId,
    _messages,
    _latestCheckpoint,
    _isGenerating,
    _streamChunk,
    _turnStatus,
    _errorMessage,
    _customApiKey,
    _globalDefaultPrompt
  ) { args: Array<Any?> ->
    @Suppress("UNCHECKED_CAST")
    val storyList = args[0] as List<StoryEntity>
    val activeId = args[1] as Long?
    @Suppress("UNCHECKED_CAST")
    val msgs = args[2] as List<MessageEntity>
    val cp = args[3] as CheckpointEntity?
    val generating = args[4] as Boolean
    val chunk = args[5] as String
    val turnStat = args[6] as TurnProgress?
    val err = args[7] as String?
    val customKey = args[8] as String
    val globalPrompt = args[9] as String

    val activeStory = storyList.firstOrNull { it.id == activeId } ?: storyList.firstOrNull()

    StoryUiState(
      currentStory = activeStory,
      allStories = storyList,
      messages = msgs,
      latestCheckpoint = cp,
      isGenerating = generating,
      streamChunk = chunk,
      turnStatus = turnStat,
      errorMessage = err,
      customApiKey = customKey,
      globalDefaultPrompt = globalPrompt,
      effectiveApiKeyPresent = repository.getEffectiveApiKey().isNotBlank()
    )
  }.stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    StoryUiState()
  )
}
