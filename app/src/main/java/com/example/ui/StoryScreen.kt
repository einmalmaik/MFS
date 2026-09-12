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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
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
import com.example.ui.components.ManualStateEditDialog
import com.example.ui.components.NotebookDrawer
import com.example.ui.components.SettingsSheet
import com.example.ui.components.StoryActionInputBar
import com.example.ui.components.StoryChatArea
import com.example.ui.components.StoryNavigationDrawer
import com.example.ui.components.StorySelectorDialog
import com.example.ui.components.StorySplashScreen
import com.example.ui.components.StoryTopBar
import com.example.ui.dna.DnaActionConfirmDialog
import com.example.ui.dna.DnaColors
import com.example.ui.dna.DnaConfirmVariant
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
  var showStorySelector by remember { mutableStateOf(false) }
  var storySelectorCreateMode by remember { mutableStateOf(false) }
  var showManualEditDialog by remember { mutableStateOf(false) }

  // Inline Message Editing (Design DNA & MSM pattern)
  var editingMessage by remember { mutableStateOf<MessageEntity?>(null) }
  var showConfirmEditDialog by remember { mutableStateOf(false) }
  var pendingEditContent by remember { mutableStateOf("") }

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

  LaunchedEffect(isRecordingVoice) {
    if (isRecordingVoice) {
      val startTime = System.currentTimeMillis()
      while (isRecordingVoice) {
        voiceAmplitude = audioRecorder.getMaxAmplitudeRatio()
        recordingDurationSeconds = ((System.currentTimeMillis() - startTime) / 1000).toInt()
        delay(100L)
      }
    } else {
      voiceAmplitude = 0f
      recordingDurationSeconds = 0
    }
  }

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

  val notebookSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var isAppInitializing by remember { mutableStateOf(false) }

  // Playtime Tracking
  LaunchedEffect(uiState.currentStory?.id) {
    uiState.currentStory?.id?.let { storyId ->
      while (true) {
        delay(60000L) // Track every minute
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

  // Dynamic context suggestions based on current story
  val suggestions = remember(story?.id) {
    listOf(
      "Elena leise fragen, was sie vorhat",
      "Lagerhalle gründlich durchsuchen",
      "Die schwere Schiebetür genauer prüfen",
      "Taschenlampe ausschalten und lauschen"
    )
  }

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
          onOpenNotebook = { showNotebookSheet = true }
        )
      }
    ) { innerPadding ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(top = innerPadding.calculateTopPadding())
          .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
      ) {
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
            onRewindToMessage = { targetMsg -> viewModel.rewindTo(targetMsg) },
            onEditMessage = { targetMsg ->
              editingMessage = targetMsg
              inputText = targetMsg.content
            },
            onBranchFromMessage = { _ ->
              if (story != null) {
                viewModel.branchStory(story.id, "${story.title} (Zweig)")
              }
            }
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
          suggestions = suggestions,
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
        adultContentEnabled = story?.adultContentEnabled ?: true,
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

  // 2. Settings Sheet
  if (showSettingsSheet && story != null) {
    ModalBottomSheet(
      onDismissRequest = { showSettingsSheet = false },
      sheetState = settingsSheetState,
      containerColor = DnaColors.Surface,
      scrimColor = DnaColors.SurfaceDim.copy(alpha = 0.7f)
    ) {
      SettingsSheet(
        story = story,
        globalDefaultPrompt = uiState.globalDefaultPrompt,
        customApiKey = uiState.customApiKey,
        availableModels = uiState.availableModels,
        availableEmbeddingModels = uiState.availableEmbeddingModels,
        availableTranscriptionModels = uiState.availableTranscriptionModels,
        isFetchingModels = uiState.isFetchingModels,
        modelCatalogIsLive = uiState.modelCatalogIsLive,
        onRefreshModels = { viewModel.refreshModelsFromGoogle() },
        onSaveStorySettings = { title, prompt, model, embeddingModel, transcriptionModel, temp, supportsTemp, thinkingLvl, thinkingBudget, adult ->
          viewModel.updateStorySettings(title, prompt, model, embeddingModel, transcriptionModel, temp, supportsTemp, thinkingLvl, thinkingBudget, adult)
        },
        onSaveGlobalDefaultPrompt = { globalPrompt ->
          viewModel.saveGlobalDefaultPrompt(globalPrompt)
        },
        onSaveApiKey = { key ->
          viewModel.saveCustomApiKey(key)
        },
        onTestApiKey = {
          viewModel.testApiKeyConnection()
        },
        onClose = {
          scope.launch {
            settingsSheetState.hide()
            showSettingsSheet = false
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
      availableModels = uiState.availableModels,
      availableEmbeddingModels = uiState.availableEmbeddingModels,
      availableTranscriptionModels = uiState.availableTranscriptionModels,
      isFetchingModels = uiState.isFetchingModels,
      onRefreshModels = { viewModel.refreshModelsFromGoogle() },
      onSelectStory = { id ->
        viewModel.switchStory(id)
        showStorySelector = false
      },
      onDeleteStory = { id -> viewModel.deleteStory(id) },
      onBranchStory = { sourceId, branchTitle -> viewModel.branchStory(sourceId, branchTitle) },
      onCreateNewStory = { title, genre, perspective, prompt, model, embeddingModel, transcriptionModel, temp, supportsTemp, thinkingLvl, thinkingBudget, adult, loc, outfit, inv, npcName, npcOutfit, npcRel, opening ->
        viewModel.createNewStory(
          title = title,
          genre = genre,
          perspective = perspective,
          systemPrompt = prompt,
          selectedModel = model,
          selectedEmbeddingModel = embeddingModel,
          selectedTranscriptionModel = transcriptionModel,
          temperature = temp,
          supportsTemperature = supportsTemp,
          thinkingLevel = thinkingLvl,
          thinkingBudget = thinkingBudget,
          adultContent = adult,
          initialLocation = loc,
          initialOutfit = outfit,
          initialInventory = inv,
          initialNpcName = npcName,
          initialNpcOutfit = npcOutfit,
          initialNpcRelation = npcRel,
          openingText = opening
        )
        showStorySelector = false
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
