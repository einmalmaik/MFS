package com.example.ui.components

import com.example.ui.dna.DnaColors
import com.example.ui.dna.DnaTypography
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import com.example.ui.dna.DnaButton
import com.example.ui.dna.DnaButtonVariant
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CheckpointEntity

/**
 * @param onSave meldet mit `false` zurück, dass der Zustand nicht geschrieben wurde. Der Dialog
 *   bleibt dann offen. Ohne diese Rückmeldung würde er sich schließen, während der Zug-Wächter
 *   die Änderung abweist — acht von Hand gefüllte Felder wären still verloren, und der einzige
 *   Hinweis wäre eine Fehlermeldung hinter dem gerade geschlossenen Dialog.
 */
@Composable
fun ManualStateEditDialog(
  checkpoint: CheckpointEntity?,
  onDismiss: () -> Unit,
  onSave: (
    inGameTime: String,
    location: String,
    weather: String,
    playerOutfit: String,
    playerCondition: String,
    inventory: List<String>,
    npcsJson: String,
    summary: String
  ) -> Boolean
) {
  var inGameTime by remember { mutableStateOf(checkpoint?.inGameTime ?: "Tag 1, 21:30 Uhr") }
  var location by remember { mutableStateOf(checkpoint?.location ?: "") }
  var weather by remember { mutableStateOf(checkpoint?.weather ?: "") }
  var playerOutfit by remember { mutableStateOf(checkpoint?.playerOutfit ?: "") }
  var playerCondition by remember { mutableStateOf(checkpoint?.playerCondition ?: "") }
  var inventoryText by remember {
    mutableStateOf(checkpoint?.getInventoryList()?.joinToString(", ") ?: "")
  }
  var npcsJson by remember { mutableStateOf(checkpoint?.npcsJson ?: "[]") }
  var summary by remember { mutableStateOf(checkpoint?.previousEventsSummary ?: "") }

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
            Icon(
              imageVector = Icons.Default.EditNote,
              contentDescription = null,
              tint = DnaColors.Primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Zustand anpassen",
              style = MaterialTheme.typography.titleMedium,
              color = DnaColors.OnSurface,
              fontFamily = FontFamily.Serif
            )
          }

          IconButton(onClick = onDismiss, modifier = Modifier.testTag("dismiss_manual_edit")) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Schließen", tint = DnaColors.OnSurfaceVariant)
          }
        }

        Text(
          text = "Deine Werte gelten ab sofort als Fakt.",
          style = MaterialTheme.typography.bodySmall,
          color = DnaColors.OnSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = DnaColors.SurfaceContainerHigh)
        Spacer(modifier = Modifier.height(14.dp))

        // In-game time & Location
        EditField(label = "Zeit", value = inGameTime, onValueChange = { inGameTime = it }, testTag = "edit_time_input")
        Spacer(modifier = Modifier.height(10.dp))
        EditField(label = "Aufenthaltsort", value = location, onValueChange = { location = it }, testTag = "edit_location_input")
        Spacer(modifier = Modifier.height(10.dp))
        EditField(label = "Wetter", value = weather, onValueChange = { weather = it }, testTag = "edit_weather_input")
        Spacer(modifier = Modifier.height(10.dp))

        // Outfit (highlighted)
        EditField(
          label = "Outfit",
          value = playerOutfit,
          onValueChange = { playerOutfit = it },
          multiline = true,
          testTag = "edit_outfit_input"
        )
        Spacer(modifier = Modifier.height(10.dp))

        EditField(label = "Verfassung", value = playerCondition, onValueChange = { playerCondition = it }, testTag = "edit_condition_input")
        Spacer(modifier = Modifier.height(10.dp))

        EditField(
          label = "Inventar",
          value = inventoryText,
          onValueChange = { inventoryText = it },
          testTag = "edit_inventory_input"
        )
        Spacer(modifier = Modifier.height(10.dp))

        EditField(
          label = "Was bisher geschah",
          value = summary,
          onValueChange = { summary = it },
          multiline = true,
          testTag = "edit_summary_input"
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically
        ) {
          DnaButton(
            text = "Abbrechen",
            onClick = onDismiss,
            variant = DnaButtonVariant.GHOST,
            testTag = "cancel_manual_edit_button"
          )
          Spacer(modifier = Modifier.width(10.dp))
          DnaButton(
            text = "Speichern",
            onClick = {
              val invList = inventoryText.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
              val gespeichert = onSave(
                inGameTime,
                location,
                weather,
                playerOutfit,
                playerCondition,
                invList,
                npcsJson,
                summary
              )
              if (gespeichert) onDismiss()
            },
            variant = DnaButtonVariant.PRIMARY,
            testTag = "save_manual_edit_button"
          )
        }
      }
    }
  }
}

@Composable
private fun EditField(
  label: String,
  value: String,
  onValueChange: (String) -> Unit,
  multiline: Boolean = false,
  testTag: String
) {
  Column {
    Text(
      text = label.uppercase(),
      style = MaterialTheme.typography.labelSmall,
      color = DnaColors.Primary,
      fontWeight = FontWeight.Bold,
      fontSize = 11.sp
    )
    Spacer(modifier = Modifier.height(4.dp))
    OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      modifier = Modifier
        .fillMaxWidth()
        .testTag(testTag),
      singleLine = !multiline,
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = DnaColors.SurfaceContainer,
        unfocusedContainerColor = DnaColors.SurfaceContainer,
        focusedBorderColor = DnaColors.Primary,
        unfocusedBorderColor = DnaColors.Border,
        focusedTextColor = DnaColors.OnSurface,
        unfocusedTextColor = DnaColors.OnSurface
      ),
      shape = RoundedCornerShape(8.dp)
    )
  }
}
