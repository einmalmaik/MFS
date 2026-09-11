package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MessageEntity
import com.example.ui.theme.AmberGoldContainer
import com.example.ui.theme.AmberGoldLight
import com.example.ui.theme.AmberGoldPrimary
import com.example.ui.theme.OnAmberGoldContainer
import com.example.ui.theme.SlateDark600
import com.example.ui.theme.SlateDark700
import com.example.ui.theme.SlateDark800
import com.example.ui.theme.TextParchment
import com.example.ui.theme.TextParchmentFaint
import com.example.ui.theme.TextParchmentMuted

/**
 * Richly formats story text:
 * - Spoken dialogue in quotes ("...", „...“, “...”, »...«) is highlighted in gold/amber with semi-bold font.
 * - Parenthetical thoughts/background actions (...) or [...] are styled in italic faint text.
 * - Narrative remains in classic book parchment serif.
 */
fun formatStoryText(content: String): AnnotatedString {
  return buildAnnotatedString {
    var index = 0
    val length = content.length

    while (index < length) {
      val ch = content[index]

      when {
        // Direct speech opening
        ch == '"' || ch == '„' || ch == '“' || ch == '»' -> {
          val closingChar = when (ch) {
            '„' -> '“'
            '»' -> '«'
            '“' -> '”'
            else -> '"'
          }
          val nextIndex = content.indexOf(closingChar, index + 1)
          if (nextIndex != -1) {
            val dialogue = content.substring(index, nextIndex + 1)
            pushStyle(
              SpanStyle(
                color = AmberGoldLight,
                fontWeight = FontWeight.SemiBold
              )
            )
            append(dialogue)
            pop()
            index = nextIndex + 1
          } else {
            // Unclosed quote: style to end of line or next quote
            val endOfLine = content.indexOf('\n', index)
            val endIdx = if (endOfLine != -1) endOfLine else length
            pushStyle(SpanStyle(color = AmberGoldLight, fontWeight = FontWeight.SemiBold))
            append(content.substring(index, endIdx))
            pop()
            index = endIdx
          }
        }

        // Parenthetical thought / background action opening
        ch == '(' || ch == '[' -> {
          val closingChar = if (ch == '(') ')' else ']'
          val nextIndex = content.indexOf(closingChar, index + 1)
          if (nextIndex != -1) {
            val note = content.substring(index, nextIndex + 1)
            pushStyle(
              SpanStyle(
                color = TextParchmentFaint,
                fontStyle = FontStyle.Italic
              )
            )
            append(note)
            pop()
            index = nextIndex + 1
          } else {
            append(ch)
            index++
          }
        }

        else -> {
          append(ch)
          index++
        }
      }
    }
  }
}

@Composable
fun StoryMessageItem(
  message: MessageEntity,
  onRewind: (MessageEntity) -> Unit,
  onEditUserMessage: (MessageEntity) -> Unit = {},
  onBranchFromHere: (MessageEntity) -> Unit = {},
  modifier: Modifier = Modifier
) {
  val clipboardManager = LocalClipboardManager.current
  val isUser = message.sender == "user"

  if (isUser) {
    // Player Action bubble
    Row(
      modifier = modifier
        .fillMaxWidth()
        .padding(vertical = 6.dp, horizontal = 4.dp),
      horizontalArrangement = Arrangement.End
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth(0.90f)
          .clip(RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp))
          .background(AmberGoldContainer)
          .border(1.dp, AmberGoldPrimary.copy(alpha = 0.35f), RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp))
          .padding(14.dp)
      ) {
        Column {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(24.dp)
                  .clip(CircleShape)
                  .background(AmberGoldPrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Person,
                  contentDescription = "Charakter",
                  tint = AmberGoldPrimary,
                  modifier = Modifier.size(15.dp)
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Deine Handlung",
                style = MaterialTheme.typography.labelSmall,
                color = OnAmberGoldContainer,
                fontWeight = FontWeight.Bold
              )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              if (!message.inGameTimeTag.isNullOrBlank()) {
                Text(
                  text = message.inGameTimeTag,
                  style = MaterialTheme.typography.labelSmall,
                  color = TextParchmentFaint,
                  modifier = Modifier.padding(end = 4.dp)
                )
              }

              // Edit user action button
              IconButton(
                onClick = { onEditUserMessage(message) },
                modifier = Modifier
                  .size(28.dp)
                  .testTag("edit_user_message_button_${message.id}")
              ) {
                Icon(
                  imageVector = Icons.Default.Edit,
                  contentDescription = "Nachricht bearbeiten & Zukunft neu berechnen",
                  tint = AmberGoldPrimary,
                  modifier = Modifier.size(14.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = message.content,
            style = MaterialTheme.typography.bodyMedium.copy(
              fontStyle = FontStyle.Italic,
              lineHeight = 22.sp
            ),
            color = TextParchment
          )
        }
      }
    }
  } else {
    // Model / Game Master narrative entry (Book Style)
    Surface(
      modifier = modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp),
      shape = RoundedCornerShape(12.dp),
      color = SlateDark800.copy(alpha = 0.88f),
      border = androidx.compose.foundation.BorderStroke(1.dp, SlateDark600.copy(alpha = 0.6f))
    ) {
      Column(
        modifier = Modifier.padding(16.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(SlateDark700),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Game Master",
                tint = AmberGoldPrimary,
                modifier = Modifier.size(16.dp)
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Game Master",
              style = MaterialTheme.typography.labelLarge,
              color = AmberGoldPrimary,
              fontWeight = FontWeight.Bold
            )
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            if (!message.inGameTimeTag.isNullOrBlank()) {
              Surface(
                color = SlateDark700,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.padding(end = 6.dp)
              ) {
                Text(
                  text = message.inGameTimeTag,
                  style = MaterialTheme.typography.labelSmall,
                  color = TextParchmentMuted,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }

            // Copy text
            IconButton(
              onClick = { clipboardManager.setText(AnnotatedString(message.content)) },
              modifier = Modifier
                .size(32.dp)
                .testTag("copy_message_button")
            ) {
              Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Text kopieren",
                tint = TextParchmentFaint,
                modifier = Modifier.size(16.dp)
              )
            }

            // Branch from this story checkpoint
            IconButton(
              onClick = { onBranchFromHere(message) },
              modifier = Modifier
                .size(32.dp)
                .testTag("branch_message_button_${message.id}")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.AltRoute,
                contentDescription = "Zweig von hier erstellen",
                tint = AmberGoldLight,
                modifier = Modifier.size(16.dp)
              )
            }

            // Rewind to this turn
            IconButton(
              onClick = { onRewind(message) },
              modifier = Modifier
                .size(32.dp)
                .testTag("rewind_message_button_${message.id}")
            ) {
              Icon(
                imageVector = Icons.Default.History,
                contentDescription = "Hierhin zurücksetzen",
                tint = AmberGoldPrimary.copy(alpha = 0.85f),
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Narrative Content: Formatted with golden dialogues and muted thoughts
        val formattedText = remember(message.content) { formatStoryText(message.content) }

        Text(
          text = formattedText,
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
