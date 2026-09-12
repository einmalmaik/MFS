package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import com.example.ui.dna.DnaButton
import com.example.ui.dna.DnaButtonVariant
import com.example.ui.dna.DnaStat
import com.example.ui.dna.formatPlayTime
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.StoryEntity
import com.example.data.model.displayTitle
import com.example.ui.dna.DnaColors
import com.example.ui.dna.DnaTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorySelectorDialog(
  currentStoryId: Long?,
  stories: List<StoryEntity>,
  onSelectStory: (Long) -> Unit,
  onDeleteStory: (Long) -> Unit,
  onBranchStory: (sourceStoryId: Long, branchTitle: String) -> Unit,
  onCreateNewStory: (systemPrompt: String) -> Unit,
  initialCreateMode: Boolean = false,
  onDismiss: () -> Unit
) {
  var isCreatingNew by remember { mutableStateOf(initialCreateMode) }
  var storyToDelete by remember { mutableStateOf<StoryEntity?>(null) }
  var storyToBranch by remember { mutableStateOf<StoryEntity?>(null) }
  var branchTitleInput by remember { mutableStateOf("") }
  var newStoryPrompt by remember { mutableStateOf("") }

  // Delete confirmation dialog
  if (storyToDelete != null) {
    val target = storyToDelete!!
    com.example.ui.dna.DnaActionConfirmDialog(
      title = "Geschichte löschen?",
      description = "'${target.displayTitle}' wird endgültig gelöscht — mit allen Nachrichten, Checkpoints und Figuren.",
      confirmButtonText = "Endgültig löschen",
      cancelButtonText = "Abbrechen",
      isDestructive = true,
      onConfirm = {
        onDeleteStory(target.id)
        storyToDelete = null
      },
      onDismiss = { storyToDelete = null }
    )
  }

  // Branch dialog
  if (storyToBranch != null) {
    val target = storyToBranch!!
    AlertDialog(
      onDismissRequest = { storyToBranch = null },
      title = { Text("Zweig erstellen (Branching)", color = DnaColors.OnSurface) },
      text = {
        Column {
          Text(
            "Eine unabhängige Kopie von '${target.displayTitle}':",
            color = DnaColors.OnSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
          )
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = branchTitleInput,
            onValueChange = { branchTitleInput = it },
            placeholder = { Text("${target.displayTitle} (Alternativer Pfad)", color = DnaColors.MutedForeground) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = DnaColors.Surface,
              unfocusedContainerColor = DnaColors.Surface,
              focusedTextColor = DnaColors.OnSurface,
              unfocusedTextColor = DnaColors.OnSurface,
              focusedBorderColor = DnaColors.Primary,
              unfocusedBorderColor = DnaColors.Border
            )
          )
        }
      },
      confirmButton = {
        DnaButton(
          text = "Zweig starten",
          onClick = {
            onBranchStory(target.id, branchTitleInput)
            storyToBranch = null
            onDismiss()
          },
          variant = DnaButtonVariant.PRIMARY,
          testTag = "confirm_branch_story_button"
        )
      },
      dismissButton = {
        DnaButton(
          text = "Abbrechen",
          onClick = { storyToBranch = null },
          variant = DnaButtonVariant.GHOST,
          testTag = "cancel_branch_story_button"
        )
      },
      containerColor = DnaColors.SurfaceContainer
    )
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .padding(16.dp),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = DnaColors.Surface),
      border = androidx.compose.foundation.BorderStroke(1.dp, DnaColors.Border)
    ) {
      Column(
        modifier = Modifier
          .padding(20.dp)
          .verticalScroll(rememberScrollState())
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            if (isCreatingNew) {
              IconButton(onClick = { isCreatingNew = false }) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück", tint = DnaColors.Primary)
              }
            } else {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = DnaColors.Primary
              )
              Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
              text = if (isCreatingNew) "Neues Abenteuer erschaffen" else "Deine Geschichten",
              style = MaterialTheme.typography.titleMedium,
              color = DnaColors.OnSurface,
              fontFamily = DnaTypography.ManropeFamily,
              fontWeight = FontWeight.Bold
            )
          }

          IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_story_selector")) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Schließen", tint = DnaColors.OnSurfaceVariant)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = DnaColors.SurfaceContainerHigh)
        Spacer(modifier = Modifier.height(14.dp))

        if (!isCreatingNew) {
          // List existing stories
          Text(
            text = "GESCHICHTEN (${stories.size})",
            style = MaterialTheme.typography.labelSmall,
            color = DnaColors.Primary,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(8.dp))

          stories.forEach { story ->
            val isCurrent = story.id == currentStoryId
            Surface(
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clickable {
                  onSelectStory(story.id)
                  onDismiss()
                },
              shape = RoundedCornerShape(10.dp),
              color = if (isCurrent) DnaColors.PrimaryContainer else DnaColors.SurfaceContainer,
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isCurrent) DnaColors.Primary else DnaColors.Border
              )
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = story.displayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isCurrent) DnaColors.Primary else DnaColors.OnSurface,
                    fontWeight = FontWeight.Bold
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  // Modell und Denkstufe standen hier doppelt (auch in den Einstellungen) und
                  // sagen beim Auswählen einer Geschichte nichts aus.
                  Text(
                    text = story.genre,
                    style = MaterialTheme.typography.bodySmall,
                    color = DnaColors.OnSurfaceVariant
                  )
                  Spacer(modifier = Modifier.height(8.dp))
                  DnaStat(
                    value = formatPlayTime(story.playTimeSeconds),
                    label = "SPIELZEIT",
                    icon = Icons.Default.Schedule,
                    emphasized = isCurrent
                  )
                }

                Row {
                  IconButton(
                    onClick = {
                      branchTitleInput = "${story.displayTitle} (Zweig)"
                      storyToBranch = story
                    }
                  ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.AltRoute, contentDescription = "Zweig erstellen", tint = DnaColors.Secondary)
                  }

                  if (stories.size > 1) {
                    IconButton(
                      onClick = { storyToDelete = story }
                    ) {
                      Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Löschen", tint = DnaColors.StatusDestructive)
                    }
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          DnaButton(
            text = "Neue Geschichte",
            onClick = { isCreatingNew = true },
            icon = Icons.Default.Add,
            variant = DnaButtonVariant.PRIMARY,
            fullWidth = true,
            testTag = "create_new_story_button"
          )
        } else {
          // --- NEUE GESCHICHTE: nur der Prompt ---
          //
          // Vorher standen hier vier Vorlagen, drei Modell-Dropdowns, Denkstufe, Kreativitaet
          // und zehn Detailfelder bis hinunter zum Prolog. Titel, Genre, Ort, Outfit, Inventar
          // und Begleiter holt sich die Extraktion jetzt aus genau diesem Text; die Modelle
          // stehen global in den Einstellungen.
          Text(
            text = "Beschreibe, worum es geht: Welt, Ausgangslage, Ton und was die KI über " +
              "diese Geschichte wissen muss. Den Rest stellt sie selbst her.",
            style = MaterialTheme.typography.bodySmall,
            color = DnaColors.OnSurfaceVariant,
            fontFamily = DnaTypography.InterFamily
          )

          Spacer(modifier = Modifier.height(12.dp))

          OutlinedTextField(
            value = newStoryPrompt,
            onValueChange = { newStoryPrompt = it },
            placeholder = {
              Text(
                text = "Du erwachst in einer verfallenen Bergfeste …",
                style = MaterialTheme.typography.bodyMedium,
                color = DnaColors.OnSurfaceVariant
              )
            },
            modifier = Modifier
              .fillMaxWidth()
              .height(260.dp)
              .testTag("new_story_prompt_field"),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = DnaColors.SurfaceContainer,
              unfocusedContainerColor = DnaColors.SurfaceContainer,
              focusedTextColor = DnaColors.OnSurface,
              unfocusedTextColor = DnaColors.OnSurface,
              focusedBorderColor = DnaColors.Primary,
              unfocusedBorderColor = DnaColors.Border
            ),
            shape = RoundedCornerShape(10.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp)
          )

          Spacer(modifier = Modifier.height(20.dp))

          DnaButton(
            text = "Beginnen",
            onClick = {
              onCreateNewStory(newStoryPrompt.trim())
              onDismiss()
            },
            enabled = newStoryPrompt.isNotBlank(),
            variant = DnaButtonVariant.PRIMARY,
            fullWidth = true,
            testTag = "confirm_create_story_button"
          )
        }
      }
    }
  }
}
