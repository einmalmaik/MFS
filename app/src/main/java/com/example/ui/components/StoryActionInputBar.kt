package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Send
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
import com.example.ui.theme.AmberGoldPrimary
import com.example.ui.theme.SlateDark600
import com.example.ui.theme.SlateDark700
import com.example.ui.theme.SlateDark800
import com.example.ui.theme.SlateDark950
import com.example.ui.theme.TextParchment
import com.example.ui.theme.TextParchmentFaint
import com.example.ui.theme.TextParchmentMuted

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
            .clickable(enabled = !isGenerating) {
              onInputTextChange(suggestion)
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
        onValueChange = onInputTextChange,
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

      val canSend = inputText.isNotBlank() && !isGenerating

      IconButton(
        onClick = {
          if (canSend) {
            onSendAction(inputText.trim())
          }
        },
        enabled = canSend,
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(if (canSend) AmberGoldPrimary else SlateDark700)
          .testTag("send_action_button")
      ) {
        Icon(
          imageVector = Icons.Default.Send,
          contentDescription = "Aktion ausführen",
          tint = if (canSend) SlateDark950 else TextParchmentFaint
        )
      }
    }
  }
}
