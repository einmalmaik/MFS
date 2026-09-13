package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiClient
import com.example.data.db.StoryDatabase
import com.example.data.model.AiSettings
import com.example.data.model.CheckpointEntity
import com.example.data.model.GeminiDefaults
import com.example.data.model.GeminiModelInfo
import com.example.data.model.MessageEntity
import com.example.data.model.StoryEntity
import com.example.data.model.UpdateRelease
import com.example.data.model.UpdateState
import com.example.data.repository.StoryRepository
import com.example.domain.model.TurnProgress
import com.example.domain.service.UpdateService
import com.example.util.ApkInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
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
  val availableEmbeddingModels: List<GeminiModelInfo> = GeminiClient.DEFAULT_EMBEDDING_MODELS,
  val availableTranscriptionModels: List<GeminiModelInfo> = GeminiClient.DEFAULT_TRANSCRIPTION_MODELS,
  val isFetchingModels: Boolean = false,
  val isTranscribingAudio: Boolean = false,
  /** false = Modell-Dropdowns zeigen die Fallback-Liste, der Schlüssel ist also unbestätigt. */
  val modelCatalogIsLive: Boolean = false,
  /** Gesetzt, wenn ein nicht mehr erreichbares Modell automatisch ersetzt wurde. */
  val modelNotice: String? = null,
  /** Modelle, Denkstufe, Kreativität und Adult Content — global, nicht pro Geschichte. */
  val aiSettings: AiSettings = AiSettings()
)

class StoryViewModel(application: Application) : AndroidViewModel(application) {
  private val database = StoryDatabase.getInstance(application)
  val repository = StoryRepository(database, application)

  /**
   * Die Selbstaktualisierung. Bewusst neben dem Repository und nicht darin: Sie hat mit
   * Geschichten nichts zu tun, spricht eine andere Gegenstelle an und darf sich mit dem
   * Gemini-Pfad nicht vermischen.
   */
  val updateService = UpdateService(application, repository.preferences)

  val updateState: StateFlow<UpdateState> = updateService.state

  private val _activeStoryId = MutableStateFlow<Long?>(null)
  private val _isGenerating = MutableStateFlow(false)
  private val _streamChunk = MutableStateFlow("")
  private val _turnStatus = MutableStateFlow<TurnProgress?>(null)
  private val _errorMessage = MutableStateFlow<String?>(null)
  private val _customApiKey = MutableStateFlow(repository.getCustomApiKey() ?: "")
  private val _globalDefaultPrompt = MutableStateFlow(repository.getGlobalDefaultSystemPrompt())
  private val _availableChatModels = MutableStateFlow<List<GeminiModelInfo>>(GeminiClient.DEFAULT_CHAT_MODELS)
  private val _availableEmbeddingModels = MutableStateFlow<List<GeminiModelInfo>>(GeminiClient.DEFAULT_EMBEDDING_MODELS)
  private val _availableTranscriptionModels = MutableStateFlow<List<GeminiModelInfo>>(GeminiClient.DEFAULT_TRANSCRIPTION_MODELS)
  private val _isFetchingModels = MutableStateFlow(false)
  private val _isTranscribingAudio = MutableStateFlow(false)
  private val _modelCatalogIsLive = MutableStateFlow(false)
  private val _modelNotice = MutableStateFlow<String?>(null)
  private val _aiSettings = MutableStateFlow(repository.getAiSettings())

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
      // Die erste Emission der Datenbank abwarten. `stories` ist ein stateIn-Flow und liefert
      // beim Start noch die leere Vorgabeliste — darauf zu prüfen hieße, jede vorhandene
      // Geschichte zu übersehen.
      val existing = repository.getAllStories().firstOrNull().orEmpty()
      seedAiSettingsFromExistingStory(existing)
      existing.firstOrNull { !it.isArchived }?.let { switchStory(it.id) }
      refreshModelsFromGoogle()
    }
  }

  /**
   * Übernimmt die Einstellungen der zuletzt bearbeiteten Geschichte genau einmal in die globalen.
   *
   * Vor diesem Umbau standen Modellwahl, Denkstufe und Kreativität in jeder Geschichte einzeln.
   * Ohne die Übernahme stünde nach dem Update überall wieder der Vorgabewert — und ein
   * gewechseltes Einbettungsmodell macht das bisherige Gedächtnis unlesbar.
   */
  private fun seedAiSettingsFromExistingStory(stories: List<StoryEntity>) {
    val source = stories.maxByOrNull { it.updatedAt } ?: return
    val seeded = repository.seedAiSettingsOnce(
      AiSettings(
        chatModel = source.selectedModel,
        embeddingModel = source.selectedEmbeddingModel,
        transcriptionModel = source.selectedTranscriptionModel,
        thinkingLevel = source.thinkingLevel,
        thinkingBudget = source.thinkingBudget,
        temperature = source.temperature,
        supportsTemperature = source.supportsTemperature,
        adultContentEnabled = source.adultContentEnabled
      )
    )
    if (seeded) _aiSettings.value = repository.getAiSettings()
  }

  fun refreshModelsFromGoogle() {
    viewModelScope.launch {
      _isFetchingModels.value = true
      try {
        val catalog = repository.fetchModelCatalog()
        _modelCatalogIsLive.value = catalog.isLive
        if (catalog.chatModels.isNotEmpty()) {
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
      healStoredModelSelection()
    }
  }

  /**
   * Stellt die globale Modellwahl um, wenn sie nicht mehr funktioniert.
   *
   * Nötig, weil Google Modelle abkündigt, ohne sie aus ListModels zu entfernen: `gemini-2.5-flash`
   * und `text-embedding-004` stehen in alten Einstellungen, antworten aber mit HTTP 404.
   * Beim Erzählmodell fällt das sofort auf, beim Embedding-Modell nicht — dort verliert die
   * Geschichte still ihr semantisches Gedächtnis. Deshalb wird hier korrigiert statt gewartet.
   *
   * Die Umstellung wird dem Nutzer angezeigt; stillschweigend etwas anderes zu benutzen, als in
   * den Einstellungen steht, wäre genau das unbemerkte Handeln, das MSF vermeiden will.
   */
  private fun healStoredModelSelection() {
    if (!_modelCatalogIsLive.value) return
    val settings = _aiSettings.value

    val chat = GeminiClient.resolveModel(
      settings.chatModel, _availableChatModels.value, GeminiDefaults.CHAT_MODEL
    )
    val embedding = GeminiClient.resolveModel(
      settings.embeddingModel, _availableEmbeddingModels.value, GeminiDefaults.EMBEDDING_MODEL
    )
    val transcription = GeminiClient.resolveModel(
      settings.transcriptionModel, _availableTranscriptionModels.value, GeminiDefaults.TRANSCRIPTION_MODEL
    )

    val changes = buildList {
      if (chat != settings.chatModel) add("Erzähler: ${settings.chatModel} → $chat")
      if (embedding != settings.embeddingModel) add("Gedächtnis: ${settings.embeddingModel} → $embedding")
      if (transcription != settings.transcriptionModel) add("Sprache: ${settings.transcriptionModel} → $transcription")
    }
    if (changes.isEmpty()) return

    saveAiSettings(
      settings.copy(
        chatModel = chat,
        embeddingModel = embedding,
        transcriptionModel = transcription
      )
    )
    _modelNotice.value =
      "Nicht mehr erreichbare Modelle wurden umgestellt:\n" + changes.joinToString("\n")
  }

  fun saveAiSettings(settings: AiSettings) {
    repository.setAiSettings(settings)
    _aiSettings.value = settings
  }

  fun dismissModelNotice() {
    _modelNotice.value = null
  }

  /**
   * Verweigert einen Eingriff, solange ein Zug läuft, und sagt dem Spieler warum.
   *
   * Ein laufender Zug ist über die kopierte storyId fest an seine Geschichte gebunden und
   * schreibt am Ende Nachricht, Checkpoint und Erinnerungen. Wird währenddessen zurückgespult,
   * verzweigt, gelöscht oder gewechselt, landet dieses Ergebnis in einer Zeitlinie, die es nicht
   * mehr gibt: ein Checkpoint aus gelöschter Vergangenheit, Waisen-Datensätze ohne Geschichte
   * oder — beim Wechseln — der Text der alten Geschichte im Fenster der neuen.
   *
   * @return true, wenn abgewiesen wurde.
   */
  private fun blockedByRunningTurn(what: String): Boolean {
    if (!_isGenerating.value) return false
    _errorMessage.value = "$what geht erst, wenn der laufende Zug zu Ende erzählt ist."
    return true
  }

  fun switchStory(storyId: Long) {
    // Dieselbe Geschichte erneut zu öffnen ist kein Wechsel — aber es ist die Gelegenheit, eine
    // beim Anlegen gescheiterte Eröffnung nachzuholen. Ein früher Ausstieg hier hätte die
    // Geschichte für immer leer gelassen.
    if (storyId == _activeStoryId.value) {
      openStoryIfUnopened(storyId)
      return
    }
    if (blockedByRunningTurn("Die Geschichte zu wechseln")) return

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

    healStoredModelSelection()
    openStoryIfUnopened(storyId)
  }

  /** Kein aktiver Spielstand — die App zeigt den leeren Zustand. */
  private fun clearActiveStory() {
    _activeStoryId.value = null
    messagesJob?.cancel()
    checkpointJob?.cancel()
    allCheckpointsJob?.cancel()
    _messages.value = emptyList()
    _latestCheckpoint.value = null
    _allCheckpoints.value = emptyList()
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
    runTurn { onChunk -> repository.executeTurn(storyId, cleanAction, onChunk) }
  }

  /**
   * Führt einen Zug aus und hält Streaming-Puffer, Fortschritt und Fehlermeldung nach.
   *
   * Gemeinsam genutzt von der Spieler-Aktion, dem Eröffnungszug einer neuen Geschichte und dem
   * Neuerzählen nach einer bearbeiteten Nachricht — alle drei unterscheiden sich nur darin,
   * welchen Flow sie liefern. [start] ist suspend, weil das Neuerzählen erst die Historie kürzt.
   */
  private fun runTurn(start: suspend (onChunk: (String) -> Unit) -> Flow<TurnProgress>) {
    if (_isGenerating.value) return

    _isGenerating.value = true
    _streamChunk.value = ""
    _turnStatus.value = TurnProgress.Thinking
    _errorMessage.value = null

    activeTurnJob?.cancel()
    activeTurnJob = viewModelScope.launch {
      try {
        start { chunk -> _streamChunk.value += chunk }
          .catch { e ->
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
              TurnProgress.StateFrozen -> {
                _modelNotice.value = "Der Spielstand konnte diesen Zug nicht mitschreiben — " +
                  "Google war nicht erreichbar. Ort, Uhrzeit, Inventar und Erinnerungen stehen " +
                  "noch auf dem Stand davor."
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
    if (blockedByRunningTurn("Zurückzuspulen")) return
    viewModelScope.launch {
      repository.rewindToMessage(storyId, message)
    }
  }

  /**
   * Bearbeitet eine Spieler-Nachricht und erzählt den Zug neu.
   *
   * Geht durch denselben [runTurn] wie die Spieler-Aktion — beide unterscheiden sich nur darin,
   * welchen Flow das Repository liefert. Vorher stand der komplette Zug-Lebenszyklus hier ein
   * zweites Mal, inklusive des Hinweises auf den eingefrorenen Spielstand Wort für Wort.
   */
  fun editMessageAndRewind(messageId: Long, newContent: String) {
    val clean = newContent.trim()
    if (clean.isBlank()) return
    val storyId = _activeStoryId.value ?: return

    runTurn { onChunk -> repository.editUserMessageAndRegenerate(storyId, messageId, clean, onChunk) }
  }

  fun transcribeVoiceInput(audioBytes: ByteArray, onTranscribed: (String) -> Unit) {
    if (audioBytes.isEmpty()) return
    viewModelScope.launch {
      _isTranscribingAudio.value = true
      try {
        val transcribedText = repository.transcribeAudio(
          audioBytes = audioBytes,
          mimeType = "audio/mp4"
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

  fun deleteStoryById(id: Long) {
    if (blockedByRunningTurn("Eine Geschichte zu löschen")) return
    viewModelScope.launch {
      repository.deleteStoryById(id)
      if (_activeStoryId.value != id) return@launch

      // War es die letzte, bleibt die App leer. Ungefragt eine Beispielgeschichte anzulegen
      // wäre genau das unbemerkte Handeln, das MSF vermeiden will.
      val firstRemaining = stories.value.firstOrNull { it.id != id }
      if (firstRemaining != null) switchStory(firstRemaining.id) else clearActiveStory()
    }
  }

  fun editUserMessage(message: MessageEntity, newContent: String) {
    editMessageAndRewind(message.id, newContent)
  }

  /**
   * @param upToMessageId der Verzweigungspunkt; `null` kopiert die ganze Geschichte. Aus der
   *   Nachrichtenliste heraus ("Zweig ab hier") ist er gesetzt — sonst hieße der Befehl das
   *   Gegenteil dessen, was er tut, und der Zweig begänne mit dem Zustand von jetzt statt dem
   *   von damals (CLAUDE.md §0.7). Aus der Geschichtenliste heraus gibt es keinen Punkt, und
   *   `null` ist dort richtig.
   */
  fun branchStory(sourceStoryId: Long, branchTitle: String, upToMessageId: Long? = null) {
    if (blockedByRunningTurn("Einen Handlungszweig anzulegen")) return
    viewModelScope.launch {
      val newId = repository.branchStory(
        sourceStoryId = sourceStoryId,
        branchTitle = branchTitle,
        upToMessageId = upToMessageId
      )
      switchStory(newId)
    }
  }

  fun deleteStory(id: Long) {
    deleteStoryById(id)
  }

  /**
   * Alles, was noch zur einzelnen Geschichte gehört: ihr Titel und ihr Prompt.
   *
   * Gezielt geschrieben statt als ganze Zeile — sonst würde die mitlaufende Spielzeit auf den
   * Stand zurückgedreht, den die Liste zufällig gerade zwischengespeichert hat.
   */
  fun updateStorySettings(title: String, systemPrompt: String) {
    val storyId = _activeStoryId.value ?: return
    viewModelScope.launch {
      repository.updateStoryTitleAndPrompt(storyId, title, systemPrompt)
    }
  }

  fun saveGlobalDefaultPrompt(prompt: String) {
    _globalDefaultPrompt.value = prompt
    repository.setGlobalDefaultSystemPrompt(prompt)
  }

  /**
   * @return false, wenn die Änderung abgewiesen wurde. Der Dialog bleibt dann offen und behält
   *   die Eingaben — beides Gründe, warum hier nicht einfach `Unit` zurückkommt. Ein `true`
   *   heißt "angenommen und in Auftrag gegeben"; beide Ablehnungsgründe entscheiden sich
   *   synchron, bevor die Coroutine startet.
   */
  fun updateManualState(
    inGameTime: String,
    location: String,
    weather: String,
    playerOutfit: String,
    playerCondition: String,
    inventory: List<String>,
    npcsJson: String,
    summary: String
  ): Boolean {
    val storyId = _activeStoryId.value ?: run {
      _errorMessage.value = "Es ist gerade keine Geschichte geöffnet."
      return false
    }
    if (blockedByRunningTurn("Den Zustand von Hand zu ändern")) return false
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
    return true
  }

  /**
   * Aktualisiert koerperliche Verletzungen fuer einen bestimmten Charakter
   * und synchronisiert sie direkt im Checkpoint und der State-JSON.
   */
  fun updateCharacterInjuries(characterName: String, updatedInjuries: List<com.example.data.model.CharacterInjury>) {
    val currentCp = _latestCheckpoint.value ?: return
    if (blockedByRunningTurn("Verletzungen zu ändern")) return
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
    // Geprüft wird mit dem eingestellten Erzählmodell. Ein fest verdrahtetes Testmodell hat
    // genau den 404 verschwiegen, den der Nutzer beim Spielen zu sehen bekam.
    val model = GeminiClient.resolveModel(
      stored = _aiSettings.value.chatModel,
      catalog = _availableChatModels.value,
      fallback = GeminiDefaults.CHAT_MODEL
    )
    val result = repository.testApiKey(model)
    if (result.first) {
      refreshModelsFromGoogle()
    }
    return result
  }

  /**
   * Legt eine Geschichte aus ihrem Prompt an und lässt sie sofort beginnen.
   *
   * Es gibt keinen Prolog mehr, den man vorher ausfüllen müsste: Der erste Zug wird hier
   * gestartet und stellt Ort, Zeit, Ausgangslage und Figuren selbst her.
   */
  fun createNewStory(systemPrompt: String) {
    if (systemPrompt.isBlank()) return
    viewModelScope.launch {
      // switchStory stößt den Eröffnungszug selbst an — sonst liefen hier zwei Versuche parallel.
      switchStory(repository.createStory(systemPrompt))
    }
  }

  /**
   * Holt den Eröffnungszug nach, wenn er noch aussteht.
   *
   * Scheitert er beim Anlegen — Ratenlimit, kein Netz, Google überlastet —, bliebe die
   * Geschichte sonst dauerhaft leer, ohne dass der Spieler sie starten könnte. Beim nächsten
   * Öffnen wird der Versuch deshalb wiederholt.
   */
  fun openStoryIfUnopened(storyId: Long) {
    viewModelScope.launch {
      if (!repository.isUnopened(storyId)) return@launch
      // Nicht still abbrechen: Eine leere Geschichte ohne Spinner und ohne Meldung sieht für
      // den Spieler aus wie ein Defekt.
      if (blockedByRunningTurn("Die Geschichte zu eröffnen")) return@launch
      runTurn { onChunk -> repository.openStory(storyId, onChunk) }
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

  // --- AKTUALISIERUNG ---
  //
  // Nicht im init-Block: Der ist für Datenbank und Modellkatalog da, und die Prüfung darf den
  // Kaltstart nicht verlängern. Angestossen wird sie von der Oberfläche, nachdem sie steht.

  fun pruefeAufUpdate(manuell: Boolean) {
    viewModelScope.launch {
      try {
        updateService.pruefe(manuell)
      } catch (e: Exception) {
        Log.w("StoryViewModel", "Update-Prüfung fehlgeschlagen", e)
      }
    }
  }

  /**
   * Der laufende Download. Wird festgehalten, damit "Abbrechen" ihn auch abbrechen kann und
   * nicht nur das Fenster schließt.
   */
  private var updateLadeJob: Job? = null

  fun ladeUpdate(release: UpdateRelease) {
    updateLadeJob?.cancel()
    updateLadeJob = viewModelScope.launch { updateService.lade(release) }
  }

  /**
   * Bricht den Download wirklich ab: Übertragung stoppen, halbe Datei entfernen, Zustand zurück.
   *
   * Vorher schloss die Schaltfläche nur den Dialog. Der Download lief weiter -- über
   * Mobilfunkvolumen und ohne dass der Spieler ihn noch sehen oder ein zweites Mal abbrechen
   * konnte. Abschnitt 8 der Datenschutzerklärung sagt ausdrücklich das Gegenteil zu.
   */
  fun brichUpdateDownloadAb(release: UpdateRelease) {
    updateLadeJob?.cancel()
    updateLadeJob = null
    updateService.verwerfeZustand()
    viewModelScope.launch(Dispatchers.IO) {
      runCatching { updateService.apkDatei(release).delete() }
    }
  }

  fun installiereUpdate(release: UpdateRelease): Boolean =
    ApkInstaller.installiere(getApplication(), updateService.apkDatei(release))

  fun ueberspringeUpdate(release: UpdateRelease) = updateService.ueberspringe(release)

  fun verwerfeUpdateZustand() = updateService.verwerfeZustand()

  fun setzeUpdatePruefung(aktiv: Boolean) {
    updateService.setzePruefung(aktiv)
    if (aktiv) pruefeAufUpdate(manuell = false)
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
    _modelCatalogIsLive,
    _modelNotice,
    _aiSettings
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
    val notice = args[17] as String?
    val settings = args[18] as AiSettings

    // Kein Rückfall auf die erste Geschichte: Ist keine aktiv, soll die App das auch zeigen.
    val activeStory = storyList.firstOrNull { it.id == activeId }

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
      availableEmbeddingModels = embModels,
      availableTranscriptionModels = transModels,
      isFetchingModels = fetchingModels,
      isTranscribingAudio = transcribingAudio,
      modelCatalogIsLive = catalogIsLive,
      modelNotice = notice,
      aiSettings = settings
    )
  }.stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    StoryUiState()
  )
}
