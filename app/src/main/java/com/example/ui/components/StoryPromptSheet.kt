package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.data.model.StoryEntity
import com.example.ui.dna.DnaButton
import com.example.ui.dna.DnaButtonVariant
import com.example.ui.dna.DnaColors
import com.example.ui.dna.DnaTypography

/**
 * Titel und Prompt einer einzelnen Geschichte.
 *
 * Bewusst getrennt von den Einstellungen: Dort stehen nur noch Werkzeug-Entscheidungen, die für
 * alle Geschichten gelten. Was diese eine Geschichte ausmacht, gehört neben den Chat — deshalb
 * führt der Stift in der Kopfleiste hierher.
 */
@Composable
fun StoryPromptSheet(
  story: StoryEntity,
  onSave: (title: String, systemPrompt: String) -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  var title by remember(story) { mutableStateOf(story.title) }
  var prompt by remember(story) { mutableStateOf(story.systemPrompt) }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(DnaColors.Surface)
      .padding(horizontal = 20.dp)
      .verticalScroll(rememberScrollState())
      .testTag("story_prompt_sheet")
  ) {
    Spacer(modifier = Modifier.height(12.dp))

    Box(
      modifier = Modifier
        .align(Alignment.CenterHorizontally)
        .width(40.dp)
        .height(4.dp)
        .background(DnaColors.Border, RoundedCornerShape(2.dp))
    )

    Spacer(modifier = Modifier.height(16.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Edit,
          contentDescription = null,
          tint = DnaColors.Primary,
          modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "Diese Geschichte",
          style = MaterialTheme.typography.titleMedium,
          fontFamily = DnaTypography.ManropeFamily,
          fontWeight = FontWeight.Bold,
          color = DnaColors.OnSurface
        )
      }

      IconButton(onClick = onClose, modifier = Modifier.testTag("close_story_prompt_button")) {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = "Schließen",
          tint = DnaColors.OnSurfaceVariant
        )
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text(
      text = "TITEL",
      style = MaterialTheme.typography.labelSmall,
      fontFamily = DnaTypography.InterFamily,
      color = DnaColors.Primary,
      fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
      value = title,
      onValueChange = { title = it },
      placeholder = {
        Text("Die KI benennt sie nach dem ersten Zug.", color = DnaColors.MutedForeground)
      },
      modifier = Modifier
        .fillMaxWidth()
        .testTag("story_title_input"),
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = DnaColors.SurfaceContainer,
        unfocusedContainerColor = DnaColors.SurfaceContainer,
        focusedBorderColor = DnaColors.Primary,
        unfocusedBorderColor = DnaColors.Border,
        focusedTextColor = DnaColors.OnSurface,
        unfocusedTextColor = DnaColors.OnSurface
      ),
      shape = RoundedCornerShape(10.dp)
    )

    Spacer(modifier = Modifier.height(20.dp))

    Text(
      text = "PROMPT",
      style = MaterialTheme.typography.labelSmall,
      fontFamily = DnaTypography.InterFamily,
      color = DnaColors.Primary,
      fontWeight = FontWeight.Bold
    )
    Text(
      text = "Welt, Ausgangslage und Ton. Änderungen gelten ab dem nächsten Zug.",
      style = MaterialTheme.typography.bodySmall,
      fontFamily = DnaTypography.InterFamily,
      color = DnaColors.OnSurfaceVariant
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
      value = prompt,
      onValueChange = { prompt = it },
      modifier = Modifier
        .fillMaxWidth()
        .height(280.dp)
        .testTag("story_prompt_input"),
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = DnaColors.SurfaceContainer,
        unfocusedContainerColor = DnaColors.SurfaceContainer,
        focusedBorderColor = DnaColors.Primary,
        unfocusedBorderColor = DnaColors.Border,
        focusedTextColor = DnaColors.OnSurface,
        unfocusedTextColor = DnaColors.OnSurface
      ),
      shape = RoundedCornerShape(10.dp),
      textStyle = MaterialTheme.typography.bodySmall.copy(
        lineHeight = 18.sp,
        fontFamily = DnaTypography.InterFamily
      )
    )

    Spacer(modifier = Modifier.height(24.dp))

    DnaButton(
      text = "Speichern",
      onClick = {
        onSave(title, prompt)
        onClose()
      },
      variant = DnaButtonVariant.PRIMARY,
      fullWidth = true,
      testTag = "save_story_prompt_button"
    )

    Spacer(modifier = Modifier.height(24.dp))
  }
}
