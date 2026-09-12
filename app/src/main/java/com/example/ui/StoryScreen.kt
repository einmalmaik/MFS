package com.example.ui


import com.example.ui.dna.DnaColors
import com.example.ui.components.StorySplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.model.MessageEntity
import com.example.ui.components.EditMessageDialog
import com.example.ui.components.ManualStateEditDialog
import com.example.ui.components.NotebookDrawer
import com.example.ui.components.SettingsSheet
import com.example.ui.components.StoryActionInputBar
import com.example.ui.components.StoryChatArea
import com.example.ui.components.StoryNavigationDrawer
import com.example.ui.components.StorySelectorDialog
import com.example.ui.components.StoryTopBar


import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryScreen(
  viewModel: StoryViewModel,
  modifier: Modifier = Modifier
) {
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
  var messageToEdit by remember { mutableStateOf<MessageEntity?>(null) }

  val notebookSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var isAppInitializing by remember { mutableStateOf(true) }

  LaunchedEffect(Unit) {
    kotlinx.coroutines.delay(1100L)
    isAppInitializing = false
  }


  // Playtime Tracking
  LaunchedEffect(uiState.currentStory?.id) {
    uiState.currentStory?.id?.let { storyId ->
      while (true) {
        kotlinx.coroutines.delay(60000L) // Track every minute
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
      },
      bottomBar = {
        StoryActionInputBar(
          inputText = inputText,
          onInputTextChange = { inputText = it },
          onSendAction = { action ->
            inputText = ""
            viewModel.sendAction(action)
          },
          isGenerating = uiState.isGenerating,
          suggestions = suggestions
        )
      }
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        StoryChatArea(
          uiState = uiState,
          listState = listState,
          onOpenSettings = { showSettingsSheet = true },
          onDismissError = { viewModel.dismissError() },
          onRewindToMessage = { targetMsg -> viewModel.rewindTo(targetMsg) },
          onEditMessage = { targetMsg -> messageToEdit = targetMsg },
          onBranchFromMessage = { targetMsg ->
            if (story != null) {
              viewModel.branchStory(story.id, "${story.title} (Zweig)")
            }
          }
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
        isFetchingModels = uiState.isFetchingModels,
        onRefreshModels = { viewModel.refreshModelsFromGoogle() },
        onSaveStorySettings = { title, prompt, model, embeddingModel, temp, supportsTemp, thinkingLvl, thinkingBudget, adult ->
          viewModel.updateStorySettings(title, prompt, model, embeddingModel, temp, supportsTemp, thinkingLvl, thinkingBudget, adult)
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

  // 3. Edit Message Dialog (Truncate future & regenerate)
  messageToEdit?.let { targetMessage ->
    EditMessageDialog(
      message = targetMessage,
      onConfirmEdit = { newContent ->
        messageToEdit = null
        viewModel.editUserMessage(targetMessage, newContent)
      },
      onDismiss = { messageToEdit = null }
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
      isFetchingModels = uiState.isFetchingModels,
      onRefreshModels = { viewModel.refreshModelsFromGoogle() },
      onSelectStory = { id ->
        viewModel.switchStory(id)
        showStorySelector = false
      },
      onDeleteStory = { id -> viewModel.deleteStory(id) },
      onBranchStory = { sourceId, branchTitle -> viewModel.branchStory(sourceId, branchTitle) },
      onCreateNewStory = { title, genre, perspective, prompt, model, embeddingModel, temp, supportsTemp, thinkingLvl, thinkingBudget, adult, loc, outfit, inv, npcName, npcOutfit, npcRel, opening ->
        viewModel.createNewStory(
          title = title,
          genre = genre,
          perspective = perspective,
          systemPrompt = prompt,
          selectedModel = model,
          selectedEmbeddingModel = embeddingModel,
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
  // 4. Startup Splash Screen
  StorySplashScreen(visible = isAppInitializing)
}
