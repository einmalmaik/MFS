package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.IceBorder
import com.example.ui.theme.IceBorderFaint
import com.example.ui.theme.IceBorderHighlight
import com.example.ui.theme.IceCyanLight
import com.example.ui.theme.IceCyanPrimary
import com.example.ui.theme.InkBackground
import com.example.ui.theme.InkHover
import com.example.ui.theme.InkOverlay
import com.example.ui.theme.InkPanel
import com.example.ui.theme.PrimaryButtonBrush
import com.example.ui.theme.TextFaint
import com.example.ui.theme.TextForeground
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextOnPrimary

/**
 * StoryActionInputBar — Gestaltet nach den Standards der MauntingStudios Design-DNA:
 * - Glassmorphism-Panel mit feiner Edge-Border
 * - Dezent pulsierender/abgestimmter Primary Gradient-Button (Cyan -> Mint)
 * - Ergonomische Chips und Touch-Flächen (mindestens 48dp)
 */
@Composable
fun StoryActionInputBar(
  inputText: String,
  onInputTextChange: (String) -> Unit,
  onSendAction: (String) -> Unit,
  isGenerating: Boolean,
  suggestions: List<String>,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(InkBackground)
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
            .clickable(enabled = !isGenerating) {
              onInputTextChange(suggestion)
            },
          color = InkPanel,
          border = BorderStroke(1.dp, IceBorderFaint)
        ) {
          Text(
            text = suggestion,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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
        onValueChange = onInputTextChange,
        placeholder = {
          Text(
            text = "Was tut dein Charakter? (z. B. Ich spreche Elena an...)",
            style = MaterialTheme.typography.bodyMedium,
            color = TextFaint
          )
        },
        modifier = Modifier
          .weight(1f)
          .testTag("action_input_field"),
        maxLines = 4,
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = InkPanel,
          unfocusedContainerColor = InkPanel,
          focusedBorderColor = IceCyanPrimary,
          unfocusedBorderColor = IceBorder,
          focusedTextColor = TextForeground,
          unfocusedTextColor = TextForeground,
          cursorColor = IceCyanPrimary
        ),
        shape = RoundedCornerShape(12.dp)
      )

      Spacer(modifier = Modifier.width(8.dp))

      val canSend = inputText.isNotBlank() && !isGenerating

      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .then(
            if (canSend) {
              Modifier.background(PrimaryButtonBrush)
            } else {
              Modifier.background(InkOverlay)
            }
          )
          .border(
            width = 1.dp,
            color = if (canSend) IceBorderHighlight.copy(alpha = 0.5f) else IceBorderFaint,
            shape = CircleShape
          ),
        contentAlignment = Alignment.Center
      ) {
        IconButton(
          onClick = {
            if (canSend) {
              onSendAction(inputText.trim())
            }
          },
          enabled = canSend,
          modifier = Modifier
            .size(48.dp)
            .testTag("send_action_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "Aktion ausführen",
            tint = if (canSend) TextOnPrimary else TextFaint
          )
        }
      }
    }
  }
}
