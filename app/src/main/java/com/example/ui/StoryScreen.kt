package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MessageEntity
import com.example.data.repository.TurnProgress
import com.example.ui.components.ManualStateEditDialog
import com.example.ui.components.NotebookDrawer
import com.example.ui.components.SettingsSheet
import com.example.ui.components.StoryMessageItem
import com.example.ui.components.StorySelectorDialog
import com.example.ui.theme.AmberGoldContainer
import com.example.ui.theme.AmberGoldLight
import com.example.ui.theme.AmberGoldPrimary
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.SlateDark600
import com.example.ui.theme.SlateDark700
import com.example.ui.theme.SlateDark800
import com.example.ui.theme.SlateDark900
import com.example.ui.theme.SlateDark950
import com.example.ui.theme.TextParchment
import com.example.ui.theme.TextParchmentFaint
import com.example.ui.theme.TextParchmentMuted
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

  var inputText by remember { mutableStateOf("") }
  var showNotebookSheet by remember { mutableStateOf(false) }
  var showSettingsSheet by remember { mutableStateOf(false) }
  var showStorySelector by remember { mutableStateOf(false) }
  var showManualEditDialog by remember { mutableStateOf(false) }

  // Message edit state
  var messageToEdit by remember { mutableStateOf<MessageEntity?>(null) }
  var editMessageInput by remember { mutableStateOf("") }

  val notebookSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  // Scroll to bottom when new messages arrive or stream updates
  LaunchedEffect(uiState.messages.size, uiState.streamChunk) {
    if (uiState.messages.isNotEmpty()) {
      listState.animateScrollToItem(uiState.messages.size)
    }
  }

  val story = uiState.currentStory
  val checkpoint = uiState.latestCheckpoint

  // Dynamic context suggestions based on setting
  val suggestions = remember(story?.id) {
    listOf(
      "Elena leise fragen, was sie vorhat",
      "Lagerhalle gründlich durchsuchen",
      "Die schwere Schiebetür genauer prüfen",
      "Taschenlampe ausschalten und lauschen"
    )
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(SlateDark900),
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = SlateDark950,
          titleContentColor = TextParchment
        ),
        title = {
          Column(
            modifier = Modifier.clickable { showStorySelector = true }
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = story?.title ?: "StoryForge",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                maxLines = 1
              )
              Spacer(modifier = Modifier.width(4.dp))
              Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = "Geschichte wechseln",
                tint = AmberGoldPrimary,
                modifier = Modifier.size(16.dp)
              )
            }
            Text(
              text = "${story?.genre ?: "Interaktives RPG"} • ${story?.selectedModel ?: ""}",
              style = MaterialTheme.typography.labelSmall,
              color = AmberGoldLight
            )
          }
        },
        actions = {
          // In-Game Time Pill (Tappable to view notebook)
          Surface(
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .clickable { showNotebookSheet = true }
              .testTag("in_game_time_pill"),
            color = AmberGoldContainer,
            border = androidx.compose.foundation.BorderStroke(1.dp, AmberGoldPrimary.copy(alpha = 0.4f))
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                tint = AmberGoldPrimary,
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = checkpoint?.inGameTime ?: "Tag 1, 20:00",
                style = MaterialTheme.typography.labelSmall,
                color = AmberGoldLight,
                fontWeight = FontWeight.Bold
              )
            }
          }

          Spacer(modifier = Modifier.width(6.dp))

          // Notebook Button ("Das Notizbuch")
          IconButton(
            onClick = { showNotebookSheet = true },
            modifier = Modifier.testTag("open_notebook_button")
          ) {
            Icon(
              imageVector = Icons.Default.MenuBook,
              contentDescription = "Das Notizbuch öffnen",
              tint = AmberGoldPrimary
            )
          }

          // Settings Button
          IconButton(
            onClick = { showSettingsSheet = true },
            modifier = Modifier.testTag("open_settings_button")
          ) {
            Icon(
              imageVector = Icons.Default.Tune,
              contentDescription = "Einstellungen",
              tint = TextParchmentMuted
            )
          }
        }
      )
    },
    bottomBar = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(SlateDark950)
          .navigationBarsPadding()
          .imePadding()
          .padding(horizontal = 12.dp, vertical = 8.dp)
      ) {
        // Quick Action Suggestion Chips
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 6.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          suggestions.forEach { suggestion ->
            Surface(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(enabled = !uiState.isGenerating) {
                  inputText = suggestion
                },
              color = SlateDark800,
              border = androidx.compose.foundation.BorderStroke(1.dp, SlateDark600)
            ) {
              Text(
                text = suggestion,
                style = MaterialTheme.typography.labelSmall,
                color = TextParchmentMuted,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
              )
            }
          }
        }

        // Input field and send button
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            placeholder = {
              Text(
                text = "Was tut dein Charakter? (z. B. Ich spreche Elena an...)",
                style = MaterialTheme.typography.bodyMedium,
                color = TextParchmentFaint
              )
            },
            modifier = Modifier
              .weight(1f)
              .testTag("action_input_field"),
            maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = SlateDark800,
              unfocusedContainerColor = SlateDark800,
              focusedBorderColor = AmberGoldPrimary,
              unfocusedBorderColor = SlateDark600,
              focusedTextColor = TextParchment,
              unfocusedTextColor = TextParchment
            ),
            shape = RoundedCornerShape(14.dp)
          )

          Spacer(modifier = Modifier.width(8.dp))

          IconButton(
            onClick = {
              if (inputText.isNotBlank()) {
                val actionToSend = inputText
                inputText = ""
                viewModel.sendAction(actionToSend)
              }
            },
            enabled = inputText.isNotBlank() && !uiState.isGenerating,
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(
                if (inputText.isNotBlank() && !uiState.isGenerating) AmberGoldPrimary else SlateDark700
              )
              .testTag("send_action_button")
          ) {
            Icon(
              imageVector = Icons.Default.Send,
              contentDescription = "Aktion ausführen",
              tint = if (inputText.isNotBlank() && !uiState.isGenerating) SlateDark950 else TextParchmentFaint
            )
          }
        }
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        // API Key Warning Banner if missing
        if (!uiState.effectiveApiKeyPresent) {
          item(key = "api_key_banner") {
            Card(
              colors = CardDefaults.cardColors(containerColor = AmberGoldContainer),
              border = androidx.compose.foundation.BorderStroke(1.dp, AmberGoldPrimary),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.Key,
                  contentDescription = null,
                  tint = AmberGoldPrimary,
                  modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Gemini API-Schlüssel hinterlegen",
                    style = MaterialTheme.typography.titleSmall,
                    color = AmberGoldLight,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = "Trage deinen Google AI Studio Key in den Einstellungen ein, um interaktiv zu spielen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextParchment
                  )
                }
                Button(
                  onClick = { showSettingsSheet = true },
                  colors = ButtonDefaults.buttonColors(
                    containerColor = AmberGoldPrimary,
                    contentColor = SlateDark900
                  ),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                  Text("Eingeben", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }

        // Error message banner if any
        uiState.errorMessage?.let { error ->
          item(key = "error_banner") {
            Card(
              colors = CardDefaults.cardColors(containerColor = CrimsonDanger.copy(alpha = 0.15f)),
              border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonDanger),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = CrimsonDanger)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text = error,
                  style = MaterialTheme.typography.bodySmall,
                  color = TextParchment,
                  modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { viewModel.dismissError() }) {
                  Icon(imageVector = Icons.Default.Close, contentDescription = "Schließen", tint = TextParchmentMuted)
                }
              }
            }
          }
        }

        // Render conversation messages
        items(
          items = uiState.messages,
          key = { it.id }
        ) { message ->
          StoryMessageItem(
            message = message,
            onRewind = { targetMsg ->
              viewModel.rewindTo(targetMsg)
            },
            onEditUserMessage = { targetMsg ->
              messageToEdit = targetMsg
              editMessageInput = targetMsg.content
            },
            onBranchFromHere = { targetMsg ->
              if (story != null) {
                viewModel.branchStory(story.id, "${story.title} (Zweig)")
              }
            }
          )
        }

        // Real-time streaming or generation indicator
        if (uiState.isGenerating) {
          item(key = "streaming_response") {
            Card(
              colors = CardDefaults.cardColors(containerColor = SlateDark800.copy(alpha = 0.85f)),
              border = androidx.compose.foundation.BorderStroke(1.dp, AmberGoldPrimary.copy(alpha = 0.4f)),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Default.AutoAwesome,
                      contentDescription = null,
                      tint = AmberGoldPrimary,
                      modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = when (uiState.turnStatus) {
                        is TurnProgress.Thinking -> "Game Master denkt nach..."
                        is TurnProgress.Streaming -> "Geschichte wird fortgesetzt..."
                        is TurnProgress.ExtractingState -> "Hintergrund: Aktualisiere Weltzustand & Notizbuch..."
                        else -> "Antwort wird generiert..."
                      },
                      style = MaterialTheme.typography.labelMedium,
                      color = AmberGoldPrimary
                    )
                  }
                  CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = AmberGoldPrimary,
                    strokeWidth = 2.dp
                  )
                }

                if (uiState.streamChunk.isNotBlank()) {
                  Spacer(modifier = Modifier.height(10.dp))
                  Text(
                    text = uiState.streamChunk,
                    style = MaterialTheme.typography.bodyLarge.copy(
                      fontFamily = FontFamily.Serif,
                      lineHeight = 26.sp,
                      color = TextParchment
                    )
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  // Modals & Bottom Sheets

  // 1. Notebook Sheet ("Das Notizbuch")
  if (showNotebookSheet) {
    ModalBottomSheet(
      onDismissRequest = { showNotebookSheet = false },
      sheetState = notebookSheetState,
      containerColor = SlateDark900,
      scrimColor = SlateDark950.copy(alpha = 0.7f)
    ) {
      NotebookDrawer(
        checkpoint = checkpoint,
        onClose = {
          scope.launch {
            notebookSheetState.hide()
            showNotebookSheet = false
          }
        },
        onOpenManualEdit = {
          showManualEditDialog = true
        }
      )
    }
  }

  // 2. Settings Sheet
  if (showSettingsSheet && story != null) {
    ModalBottomSheet(
      onDismissRequest = { showSettingsSheet = false },
      sheetState = settingsSheetState,
      containerColor = SlateDark900,
      scrimColor = SlateDark950.copy(alpha = 0.7f)
    ) {
      SettingsSheet(
        story = story,
        globalDefaultPrompt = uiState.globalDefaultPrompt,
        customApiKey = uiState.customApiKey,
        onSaveStorySettings = { title, prompt, model, temp, thinkingBudget, adult ->
          viewModel.updateStorySettings(title, prompt, model, temp, thinkingBudget, adult)
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
  if (messageToEdit != null) {
    val targetMessage = messageToEdit!!
    AlertDialog(
      onDismissRequest = { messageToEdit = null },
      title = { Text("Nachricht bearbeiten & Zukunft neu berechnen", color = TextParchment) },
      text = {
        Column {
          Text(
            text = "Alle nachfolgenden Nachrichten und Antworten werden aus der Datenbank gelöscht. Die Geschichte wird ab diesem Punkt mit deinen neuen Worten fortgesetzt.",
            style = MaterialTheme.typography.bodySmall,
            color = CrimsonDanger
          )
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = editMessageInput,
            onValueChange = { editMessageInput = it },
            modifier = Modifier
              .fillMaxWidth()
              .height(120.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = SlateDark900,
              unfocusedContainerColor = SlateDark900,
              focusedTextColor = TextParchment,
              unfocusedTextColor = TextParchment,
              focusedBorderColor = AmberGoldPrimary,
              unfocusedBorderColor = SlateDark600
            ),
            shape = RoundedCornerShape(8.dp)
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val edited = editMessageInput
            messageToEdit = null
            viewModel.editUserMessage(targetMessage, edited)
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = AmberGoldPrimary,
            contentColor = SlateDark900
          )
        ) {
          Text("Neu berechnen & absenden", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { messageToEdit = null }) {
          Text("Abbrechen", color = TextParchment)
        }
      },
      containerColor = SlateDark800
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

  // 5. Story Selector Dialog
  if (showStorySelector) {
    StorySelectorDialog(
      currentStoryId = story?.id,
      stories = uiState.allStories,
      onSelectStory = { id ->
        viewModel.switchStory(id)
      },
      onDeleteStory = { id ->
        viewModel.deleteStory(id)
      },
      onBranchStory = { sourceId, branchTitle ->
        viewModel.branchStory(sourceId, branchTitle)
      },
      onCreateNewStory = { title, genre, perspective, prompt, model, temp, thinking, adult, loc, outfit, inv, npcName, npcOutfit, npcRel, opening ->
        viewModel.createNewStory(
          title, genre, perspective, prompt, model, temp, thinking, adult, loc, outfit, inv, npcName, npcOutfit, npcRel, opening
        )
      },
      onDismiss = { showStorySelector = false }
    )
  }
}
