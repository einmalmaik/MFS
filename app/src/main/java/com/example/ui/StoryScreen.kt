package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.data.model.MessageEntity
import com.example.data.model.UpdateState
import com.example.data.model.displayTitle
import com.example.ui.components.ManualStateEditDialog
import com.example.ui.components.NotebookDrawer
import com.example.ui.components.PrivacySheet
import com.example.ui.components.SettingsSheet
import com.example.ui.components.UpdateDialog
import com.example.ui.components.UpdateHinweisLeiste
import com.example.util.ApkInstaller
import com.example.ui.components.StoryActionInputBar
import com.example.ui.components.StoryChatArea
import com.example.ui.components.StoryPromptSheet
import com.example.ui.components.StoryNavigationDrawer
import com.example.ui.components.StorySelectorDialog
import com.example.ui.components.StorySplashScreen
import com.example.ui.components.StoryTopBar
import com.example.ui.dna.DnaActionConfirmDialog
import com.example.ui.dna.DnaColors
import com.example.ui.dna.DnaConfirmVariant
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.util.AudioRecorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryScreen(
  viewModel: StoryViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsState()
  val listState = rememberLazyListState()
  val scope = rememberCoroutineScope()
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

  var inputText by remember { mutableStateOf("") }
  var showNotebookSheet by remember { mutableStateOf(false) }
  var showSettingsSheet by remember { mutableStateOf(false) }
  var showPrivacySheet by remember { mutableStateOf(false) }

  // --- Aktualisierung ---
  val updateState by viewModel.updateState.collectAsState()
  var showUpdateConsent by remember { mutableStateOf(false) }
  var showUpdateDialog by remember { mutableStateOf(false) }
  var updateBannerDismissed by remember { mutableStateOf(false) }
  var updateCheckEnabled by remember { mutableStateOf(viewModel.updateService.istPruefungAktiv()) }

  LaunchedEffect(Unit) {
    // Zuerst die Zustimmung, dann erst die Prüfung. Ohne Zustimmung kehrt pruefe() sofort
    // zurück — GitHub erfährt in diesem Fall nichts, nicht einmal, dass die App existiert.
    if (!viewModel.updateService.wurdeZustimmungGefragt()) {
      showUpdateConsent = true
    } else {
      viewModel.pruefeAufUpdate(manuell = false)
    }
  }
  var showStoryPromptSheet by remember { mutableStateOf(false) }
  var showStorySelector by remember { mutableStateOf(false) }
  var storySelectorCreateMode by remember { mutableStateOf(false) }
  var showManualEditDialog by remember { mutableStateOf(false) }

  // --- Story Backup & Export/Import ---
  var pendingExportJson by remember { mutableStateOf<String?>(null) }
  var pendingExportFileName by remember { mutableStateOf("story_backup.json") }

  val exportLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/json")
  ) { uri ->
    if (uri != null && pendingExportJson != null) {
      scope.launch(kotlinx.coroutines.Dispatchers.IO) {
        try {
          context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(pendingExportJson!!.toByteArray(Charsets.UTF_8))
          }
          pendingExportJson = null
        } catch (e: Exception) {
          android.util.Log.e("StoryScreen", "Export write error", e)
        }
      }
    }
  }

  val importLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      scope.launch(kotlinx.coroutines.Dispatchers.IO) {
        try {
          val json = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader(Charsets.UTF_8).readText()
          }
          if (!json.isNullOrBlank()) {
            viewModel.importStory(json) { success, _ ->
              if (success) {
                showStorySelector = false
                showSettingsSheet = false
              }
            }
          }
        } catch (e: Exception) {
          android.util.Log.e("StoryScreen", "Import read error", e)
        }
      }
    }
  }

  val triggerExport: (Long, String) -> Unit = { storyId, storyTitle ->
    val safeTitle = storyTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(30)
    pendingExportFileName = "${safeTitle}_backup.json"
    viewModel.exportStory(storyId) { json ->
      if (json != null) {
        pendingExportJson = json
        exportLauncher.launch(pendingExportFileName)
      }
    }
  }

  // Inline Message Editing (Design DNA & MSM pattern)
  var editingMessage by remember { mutableStateOf<MessageEntity?>(null) }
  var showConfirmEditDialog by remember { mutableStateOf(false) }
  var pendingEditContent by remember { mutableStateOf("") }
  var rewindTargetMessage by remember { mutableStateOf<MessageEntity?>(null) }

  // Audio Recording & Gemini Transcription
  val audioRecorder = remember { AudioRecorder(context) }
  DisposableEffect(Unit) {
    onDispose {
      audioRecorder.cancelRecording()
    }
  }

  var isRecordingVoice by remember { mutableStateOf(false) }
  var voiceAmplitude by remember { mutableFloatStateOf(0f) }
  var recordingDurationSeconds by remember { mutableIntStateOf(0) }

  fun startVoiceRecording() {
    if (isRecordingVoice) return
    val started = audioRecorder.startRecording()
    if (started) {
      isRecordingVoice = true
    } else {
      Toast.makeText(context, "Mikrofonaufnahme konnte nicht gestartet werden.", Toast.LENGTH_SHORT).show()
    }
  }

  val micPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      startVoiceRecording()
    } else {
      Toast.makeText(context, "Mikrofon-Berechtigung wird für die Spracheingabe benötigt.", Toast.LENGTH_SHORT).show()
    }
  }

  fun checkAndStartRecording() {
    val hasPermission = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    if (hasPermission) {
      startVoiceRecording()
    } else {
      micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
  }

  fun stopVoiceRecording() {
    if (!isRecordingVoice) return
    val bytes = audioRecorder.stopRecording()
    isRecordingVoice = false
    if (bytes != null && bytes.isNotEmpty()) {
      viewModel.transcribeVoiceInput(bytes) { transcribedText ->
        if (transcribedText.isNotBlank()) {
          inputText = if (inputText.isBlank()) transcribedText.trim() else "${inputText.trim()} ${transcribedText.trim()}"
        }
      }
    }
  }

  fun cancelVoiceRecording() {
    audioRecorder.cancelRecording()
    isRecordingVoice = false
  }

  LaunchedEffect(isRecordingVoice) {
    if (!isRecordingVoice) {
      voiceAmplitude = 0f
      recordingDurationSeconds = 0
      return@LaunchedEffect
    }

    val startTime = System.currentTimeMillis()
    while (isRecordingVoice) {
      voiceAmplitude = audioRecorder.getMaxAmplitudeRatio()
      recordingDurationSeconds = ((System.currentTimeMillis() - startTime) / 1000).toInt()
      // Harte Obergrenze: Eine vergessene Aufnahme soll nicht den halben Abend mitschneiden
      // und am Ende vollständig bei Google landen. Das Aufgenommene geht nicht verloren —
      // es wird wie bei einem Tippen auf den Haken transkribiert.
      if (recordingDurationSeconds >= AudioRecorder.MAX_RECORDING_SECONDS) {
        stopVoiceRecording()
        return@LaunchedEffect
      }
      delay(100L)
    }
  }

  // Die Aufnahme endet, sobald die App in den Hintergrund geht. Ohne das lief der
  // MediaRecorder weiter, wenn jemand mitten im Sprechen die Home-Taste drückte — und beim
  // Zurückkehren ging die gesamte Spanne an Google.
  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_STOP && isRecordingVoice) {
        cancelVoiceRecording()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  val notebookSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val privacySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val storyPromptSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var isAppInitializing by remember { mutableStateOf(false) }

  // Spielzeit mitzählen -- nur solange die App wirklich vorn ist.
  //
  // Vorher lief die Schleife unabhängig vom Lebenszyklus weiter: Wer die App über Nacht offen
  // liegen ließ, bekam acht Stunden "Spielzeit" gutgeschrieben und eine Datenbankschreibung pro
  // Minute, jede davon mit aufgewecktem Flash. repeatOnLifecycle hält sie bei ON_PAUSE an und
  // nimmt sie bei ON_RESUME wieder auf.
  LaunchedEffect(uiState.currentStory?.id, lifecycleOwner) {
    val storyId = uiState.currentStory?.id ?: return@LaunchedEffect
    lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
      while (true) {
        delay(60_000L)
        viewModel.updatePlayTime(storyId, 60L)
      }
    }
  }

  // Automatically scroll to the latest message or generation stream
  LaunchedEffect(uiState.messages.size, uiState.streamChunk) {
    if (uiState.messages.isNotEmpty()) {
      listState.animateScrollToItem(uiState.messages.size)
    }
  }

  val story = uiState.currentStory
  val checkpoint = uiState.latestCheckpoint

  ModalNavigationDrawer(
    drawerState = drawerState,
    gesturesEnabled = true, // Enables left edge swipe!
    drawerContent = {
      StoryNavigationDrawer(
        currentStoryId = story?.id,
        stories = uiState.allStories,
        onSelectStory = { id -> viewModel.switchStory(id) },
        onOpenCreateStory = {
          storySelectorCreateMode = true
          showStorySelector = true
        },
        onToggleArchiveStory = { storyId, isArchived ->
          viewModel.toggleStoryArchived(storyId, isArchived)
        },
        onBranchStory = { sourceStoryId, branchTitle ->
          viewModel.branchStory(sourceStoryId, branchTitle)
        },
        onDeleteStory = { id -> viewModel.deleteStory(id) },
        onOpenSettings = { showSettingsSheet = true },
        onCloseDrawer = {
          scope.launch { drawerState.close() }
        }
      )
    }
  ) {
    Scaffold(
      modifier = modifier
        .fillMaxSize()
        .background(DnaColors.Surface),
      topBar = {
        StoryTopBar(
          story = story,
          checkpoint = checkpoint,
          onOpenDrawer = {
            scope.launch { drawerState.open() }
          },
          onOpenStorySelector = {
            scope.launch { drawerState.open() }
          },
          onOpenNotebook = { showNotebookSheet = true },
          onEditStoryPrompt = { showStoryPromptSheet = true }
        )
      }
    ) { innerPadding ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(top = innerPadding.calculateTopPadding())
          .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
      ) {
        // Über der Erzählung, nicht darüber gelegt: Wer mitten in einer Szene ist, soll
        // weiterlesen können. Der Hinweis kommt beim nächsten Start wieder.
        if (updateState is UpdateState.Verfuegbar && !updateBannerDismissed && !showUpdateDialog) {
          UpdateHinweisLeiste(
            versionName = (updateState as UpdateState.Verfuegbar).release.versionName,
            onAnsehen = { showUpdateDialog = true },
            onVerwerfen = { updateBannerDismissed = true }
          )
        }

        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
        ) {
          StoryChatArea(
            uiState = uiState,
            listState = listState,
            onOpenSettings = { showSettingsSheet = true },
            onDismissError = { viewModel.dismissError() },
            onRewindToMessage = { targetMsg -> rewindTargetMessage = targetMsg },
            onEditMessage = { targetMsg ->
              editingMessage = targetMsg
              inputText = targetMsg.content
            },
            onResendMessage = { targetMsg ->
              viewModel.resendUserMessage(targetMsg)
            },
            onBranchFromMessage = { targetMsg ->
              if (story != null) {
                viewModel.branchStory(
                  sourceStoryId = story.id,
                  branchTitle = "${story.displayTitle} (Zweig)",
                  upToMessageId = targetMsg.id
                )
              }
            },
            onRetryTurn = { viewModel.retryLastTurn() }
          )
        }

        StoryActionInputBar(
          inputText = inputText,
          onInputTextChange = { inputText = it },
          onSendAction = { action ->
            if (editingMessage != null) {
              pendingEditContent = action
              showConfirmEditDialog = true
            } else {
              inputText = ""
              viewModel.sendAction(action)
            }
          },
          isGenerating = uiState.isGenerating,
          editingMessage = editingMessage,
          onCancelEdit = {
            editingMessage = null
            inputText = ""
          },
          isRecordingVoice = isRecordingVoice,
          isTranscribing = uiState.isTranscribingAudio,
          voiceAmplitude = voiceAmplitude,
          recordingDurationSeconds = recordingDurationSeconds,
          onStartVoiceRecord = { checkAndStartRecording() },
          onStopVoiceRecord = { stopVoiceRecording() },
          onCancelVoiceRecord = { cancelVoiceRecording() }
        )
      }
    }
  }

  // --- MODALS & BOTTOM SHEETS ---

  // 1. Notebook Sheet ("Das Notizbuch") with Day-by-Day Chronicler & Character Visualizer
  if (showNotebookSheet) {
    ModalBottomSheet(
      onDismissRequest = { showNotebookSheet = false },
      sheetState = notebookSheetState,
      containerColor = DnaColors.Surface,
      scrimColor = DnaColors.SurfaceDim.copy(alpha = 0.7f)
    ) {
      NotebookDrawer(
        checkpoint = checkpoint,
        allCheckpoints = uiState.allCheckpoints,
        adultContentEnabled = uiState.aiSettings.adultContentEnabled,
        onClose = {
          scope.launch {
            notebookSheetState.hide()
            showNotebookSheet = false
          }
        },
        onOpenManualEdit = { showManualEditDialog = true },
        onUpdateInjuries = { charName, updatedInjuries ->
          viewModel.updateCharacterInjuries(charName, updatedInjuries)
        }
      )
    }
  }

  // 1b. Aktualisierung: Zustimmung, Hinweis und Dialog.
  //
  // Drei getrennte Entscheidungen des Spielers, jede an ihrer eigenen Stelle: ob überhaupt
  // geprüft wird, ob geladen wird, ob installiert wird. Keine davon geschieht von allein.
  if (showUpdateConsent) {
    DnaActionConfirmDialog(
      title = "Nach Aktualisierungen suchen?",
      message = "MSF kann beim Start bei GitHub nachsehen, ob eine neuere Version vorliegt. " +
        "GitHub erfährt dabei deine IP-Adresse und den Zeitpunkt — mehr nicht. Heruntergeladen " +
        "und installiert wird nichts ohne deine ausdrückliche Zustimmung.\n\n" +
        "Deine Geschichten, Checkpoints und dein API-Schlüssel bleiben davon unberührt und " +
        "verlassen das Gerät nicht. Du kannst das jederzeit in den Einstellungen ändern.",
      confirmButtonText = "Ja, suchen",
      dismissButtonText = "Nein, nicht suchen",
      variant = DnaConfirmVariant.PRIMARY,
      testTag = "update_consent_dialog",
      onConfirm = {
        showUpdateConsent = false
        updateCheckEnabled = true
        viewModel.setzeUpdatePruefung(true)
      },
      onDismiss = {
        showUpdateConsent = false
        updateCheckEnabled = false
        viewModel.setzeUpdatePruefung(false)
      }
    )
  }

  // Meldung und Fehler stammen immer aus einer vom Nutzer ausgelösten Prüfung — die
  // automatische schweigt. Deshalb dürfen sie hier sichtbar werden.
  LaunchedEffect(updateState) {
    if (updateState is UpdateState.Verfuegbar) {
      showUpdateDialog = true
      if (showSettingsSheet) {
        showSettingsSheet = false
      }
    }
    val text = when (val s = updateState) {
      is UpdateState.Meldung -> s.text
      is UpdateState.Fehler -> s.text
      else -> null
    }
    if (text != null) {
      Toast.makeText(context, text, Toast.LENGTH_LONG).show()
      viewModel.verwerfeUpdateZustand()
    }
  }

  val verfuegbaresRelease = when (val s = updateState) {
    is UpdateState.Verfuegbar -> s.release
    is UpdateState.Laedt -> s.release
    is UpdateState.Bereit -> s.release
    else -> null
  }

  if (showUpdateDialog && verfuegbaresRelease != null) {
    UpdateDialog(
      release = verfuegbaresRelease,
      installierteVersion = viewModel.updateService.installierterVersionName,
      fortschritt = (updateState as? UpdateState.Laedt)?.fortschritt,
      bereit = updateState is UpdateState.Bereit,
      darfInstallieren = ApkInstaller.darfInstallieren(context),
      onLaden = { viewModel.ladeUpdate(verfuegbaresRelease) },
      onInstallieren = {
        if (!viewModel.installiereUpdate(verfuegbaresRelease)) {
          Toast.makeText(context, "Der Installer ließ sich nicht öffnen.", Toast.LENGTH_SHORT).show()
        }
      },
      onFreigabeOeffnen = { ApkInstaller.oeffneFreigabeEinstellung(context) },
      onUeberspringen = {
        viewModel.ueberspringeUpdate(verfuegbaresRelease)
        showUpdateDialog = false
      },
      onAbbrechen = {
        viewModel.brichUpdateDownloadAb(verfuegbaresRelease)
        showUpdateDialog = false
      },
      onSpaeter = { showUpdateDialog = false }
    )
  }

  // 2. Settings Sheet — global, deshalb auch ohne aktive Geschichte erreichbar.
  if (showSettingsSheet) {
    ModalBottomSheet(
      onDismissRequest = { showSettingsSheet = false },
      sheetState = settingsSheetState,
      containerColor = DnaColors.Surface,
      scrimColor = DnaColors.SurfaceDim.copy(alpha = 0.7f)
    ) {
      SettingsSheet(
        aiSettings = uiState.aiSettings,
        globalDefaultPrompt = uiState.globalDefaultPrompt,
        customApiKey = uiState.customApiKey,
        availableModels = uiState.availableModels,
        availableEmbeddingModels = uiState.availableEmbeddingModels,
        availableTranscriptionModels = uiState.availableTranscriptionModels,
        isFetchingModels = uiState.isFetchingModels,
        modelCatalogIsLive = uiState.modelCatalogIsLive,
        onRefreshModels = { viewModel.refreshModelsFromGoogle() },
        onSaveAiSettings = { settings -> viewModel.saveAiSettings(settings) },
        onSaveGlobalDefaultPrompt = { globalPrompt ->
          viewModel.saveGlobalDefaultPrompt(globalPrompt)
        },
        onSaveApiKey = { key ->
          viewModel.saveCustomApiKey(key)
        },
        onTestApiKey = {
          viewModel.testApiKeyConnection()
        },
        updateCheckEnabled = updateCheckEnabled,
        installedVersion = "${viewModel.updateService.installierterVersionName} " +
          "(${viewModel.updateService.installierterVersionCode})",
        onSetUpdateCheck = { aktiv ->
          updateCheckEnabled = aktiv
          viewModel.setzeUpdatePruefung(aktiv)
        },
        onCheckForUpdate = {
          updateBannerDismissed = false
          viewModel.pruefeAufUpdate(manuell = true)
        },
        onExportCurrentStory = story?.let { s -> { triggerExport(s.id, s.displayTitle) } },
        onImportStory = {
          importLauncher.launch(arrayOf("application/json", "*/*"))
        },
        onOpenPrivacy = { showPrivacySheet = true },
        onClose = {
          scope.launch {
            settingsSheetState.hide()
            showSettingsSheet = false
          }
        }
      )
    }
  }

  // 2a. Datenschutzerklärung — liegt in der App, nicht hinter einem Netz-Aufruf.
  if (showPrivacySheet) {
    ModalBottomSheet(
      onDismissRequest = { showPrivacySheet = false },
      sheetState = privacySheetState,
      containerColor = DnaColors.Surface,
      scrimColor = DnaColors.SurfaceDim.copy(alpha = 0.7f)
    ) {
      PrivacySheet(
        onClose = {
          scope.launch {
            privacySheetState.hide()
            showPrivacySheet = false
          }
        }
      )
    }
  }

  // 2b. Prompt dieser Geschichte
  if (showStoryPromptSheet && story != null) {
    ModalBottomSheet(
      onDismissRequest = { showStoryPromptSheet = false },
      sheetState = storyPromptSheetState,
      containerColor = DnaColors.Surface,
      scrimColor = DnaColors.SurfaceDim.copy(alpha = 0.7f)
    ) {
      StoryPromptSheet(
        story = story,
        onSave = { title, prompt -> viewModel.updateStorySettings(title, prompt) },
        onClose = {
          scope.launch {
            storyPromptSheetState.hide()
            showStoryPromptSheet = false
          }
        }
      )
    }
  }

  // 3. Confirm Edit Message Dialog (Design DNA & MSM Standard)
  if (showConfirmEditDialog && editingMessage != null) {
    val targetMsg = editingMessage!!
    DnaActionConfirmDialog(
      title = "Nachricht bearbeiten & Zukunft neu berechnen?",
      message = "Das Bearbeiten dieser Nachricht setzt die Geschichte auf diesen Zeitpunkt zurück. Alle darauffolgenden Antworten werden abgeschnitten und die Handlung wird mit dem geänderten Text neu berechnet.",
      confirmButtonText = "Ja, neu berechnen",
      dismissButtonText = "Nein, abbrechen",
      variant = DnaConfirmVariant.PRIMARY,
      icon = Icons.Default.Edit,
      onConfirm = {
        showConfirmEditDialog = false
        val newText = pendingEditContent
        editingMessage = null
        inputText = ""
        viewModel.editUserMessage(targetMsg, newText)
      },
      onDismiss = {
        showConfirmEditDialog = false
      },
      testTag = "confirm_edit_message_dialog"
    )
  }

  // 3b. Confirm Rewind Dialog (Design DNA & MSM Standard)
  rewindTargetMessage?.let { targetMsg ->
    DnaActionConfirmDialog(
      title = "Zu diesem Zug zurückkehren?",
      description = "Alle späteren Nachrichten und Entscheidungen nach diesem Zeitpunkt werden verworfen. Die Handlung wird ab diesem Punkt fortgeführt.",
      confirmButtonText = "Ja, zurückkehren",
      cancelButtonText = "Abbrechen",
      isDestructive = true,
      icon = androidx.compose.material.icons.Icons.Default.History,
      onConfirm = {
        val msg = targetMsg
        rewindTargetMessage = null
        viewModel.rewindTo(msg)
      },
      onDismiss = {
        rewindTargetMessage = null
      },
      testTag = "confirm_rewind_dialog"
    )
  }

  // 4. Manual State Edit Dialog
  if (showManualEditDialog) {
    ManualStateEditDialog(
      checkpoint = checkpoint,
      onDismiss = { showManualEditDialog = false },
      onSave = { time, loc, weather, outfit, condition, inv, npcs, summary ->
        viewModel.updateManualState(time, loc, weather, outfit, condition, inv, npcs, summary)
      }
    )
  }

  // 5. Story Selector & Creator Dialog
  if (showStorySelector) {
    StorySelectorDialog(
      currentStoryId = story?.id,
      stories = uiState.allStories,
      onSelectStory = { id ->
        viewModel.switchStory(id)
        showStorySelector = false
      },
      onDeleteStory = { id -> viewModel.deleteStory(id) },
      onBranchStory = { sourceId, branchTitle -> viewModel.branchStory(sourceId, branchTitle) },
      onCreateNewStory = { prompt ->
        viewModel.createNewStory(prompt)
        showStorySelector = false
      },
      onExportStory = { id, title -> triggerExport(id, title) },
      onImportStory = {
        importLauncher.launch(arrayOf("application/json", "*/*"))
      },
      initialCreateMode = storySelectorCreateMode,
      onDismiss = {
        showStorySelector = false
        storySelectorCreateMode = false
      }
    )
  }
  // 6. Startup Splash Screen
  StorySplashScreen(visible = isAppInitializing)
}
