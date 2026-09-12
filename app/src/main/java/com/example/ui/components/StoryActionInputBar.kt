package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MessageEntity
import com.example.ui.dna.DnaColors
import com.example.ui.dna.DnaTypography
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
 * - MSM Edit-Mode Banner für direktes Bearbeiten in der Chatleiste
 * - Gemini Transkriptions- & Sprachaufnahmemodus mit Live-Soundbar
 * - Dezent abgestimmter Primary Gradient-Button (Cyan -> Mint)
 */
@Composable
fun StoryActionInputBar(
  inputText: String,
  onInputTextChange: (String) -> Unit,
  onSendAction: (String) -> Unit,
  isGenerating: Boolean,
  editingMessage: MessageEntity? = null,
  onCancelEdit: () -> Unit = {},
  isRecordingVoice: Boolean = false,
  isTranscribing: Boolean = false,
  voiceAmplitude: Float = 0f,
  recordingDurationSeconds: Int = 0,
  onStartVoiceRecord: () -> Unit = {},
  onStopVoiceRecord: () -> Unit = {},
  onCancelVoiceRecord: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(InkBackground)
      .padding(horizontal = 12.dp, vertical = 8.dp)
  ) {
    // 1. MSM-Style Editing Mode Banner
    if (editingMessage != null) {
      Surface(
        color = DnaColors.Primary.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, DnaColors.Primary.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 8.dp)
          .testTag("editing_message_banner")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(28.dp)
              .background(DnaColors.Primary.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Edit,
              contentDescription = null,
              tint = DnaColors.Primary,
              modifier = Modifier.size(16.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Nachricht bearbeiten",
              style = MaterialTheme.typography.labelSmall,
              fontFamily = DnaTypography.InterFamily,
              color = DnaColors.Primary,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = editingMessage.content,
              style = MaterialTheme.typography.bodySmall,
              fontFamily = DnaTypography.InterFamily,
              color = DnaColors.OnSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
          IconButton(
            onClick = onCancelEdit,
            modifier = Modifier
              .size(28.dp)
              .testTag("cancel_edit_button")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Bearbeiten abbrechen",
              tint = DnaColors.OnSurfaceVariant,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }
    }

    // 2. Voice Recording Bar (MSM VoiceRecordingBar Pattern)
    if (isRecordingVoice) {
      val infiniteTransition = rememberInfiniteTransition(label = "pulse")
      val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
          animation = tween(600),
          repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_dot"
      )

      val minutes = recordingDurationSeconds / 60
      val seconds = recordingDurationSeconds % 60
      val durationFormatted = "%d:%02d".format(minutes, seconds)

      Surface(
        color = InkPanel,
        border = BorderStroke(1.dp, DnaColors.StatusDestructive.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 6.dp)
          .testTag("voice_recording_bar")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            // Pulsing live red dot
            Box(
              modifier = Modifier
                .size(10.dp)
                .scale(pulseScale)
                .background(DnaColors.StatusDestructive, CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = durationFormatted,
              style = MaterialTheme.typography.titleMedium,
              fontFamily = DnaTypography.JetBrainsMonoFamily,
              fontWeight = FontWeight.Bold,
              color = TextForeground
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = "Jetzt sprechen …",
              style = MaterialTheme.typography.bodySmall,
              fontFamily = DnaTypography.InterFamily,
              color = TextMuted
            )
          }

          // Animated Sound Level Waves
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(horizontal = 8.dp)
          ) {
            val barHeights = listOf(
              (10 + voiceAmplitude * 20).coerceIn(6f, 26f).dp,
              (14 + voiceAmplitude * 28).coerceIn(8f, 32f).dp,
              (8 + voiceAmplitude * 18).coerceIn(6f, 22f).dp,
              (16 + voiceAmplitude * 24).coerceIn(8f, 28f).dp
            )
            barHeights.forEach { h ->
              Box(
                modifier = Modifier
                  .width(3.dp)
                  .height(h)
                  .background(DnaColors.Primary, RoundedCornerShape(2.dp))
              )
            }
          }

          // Action Buttons: Cancel (X) and Finish (Check)
          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
              onClick = onCancelVoiceRecord,
              modifier = Modifier
                .size(36.dp)
                .testTag("cancel_voice_record_button")
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Aufnahme verwerfen",
                tint = TextFaint,
                modifier = Modifier.size(18.dp)
              )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(PrimaryButtonBrush)
                .clickable { onStopVoiceRecord() },
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Aufnahme beenden",
                tint = TextOnPrimary,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }
      }
    } else if (isTranscribing) {
      Surface(
        color = InkPanel,
        border = BorderStroke(1.dp, IceBorderFaint),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 6.dp)
          .testTag("transcribing_status_banner")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          CircularProgressIndicator(
            color = DnaColors.Primary,
            strokeWidth = 2.dp,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = "Wird transkribiert …",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = DnaTypography.InterFamily,
            color = DnaColors.Primary
          )
        }
      }
    }

    // 3. Input field, voice mic button, and send button
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      OutlinedTextField(
        value = inputText,
        onValueChange = onInputTextChange,
        placeholder = {
          Text(
            text = if (editingMessage != null) "Nachricht bearbeiten …" else "Was tust du?",
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

      // Microphone button for Gemini speech transcription
      IconButton(
        onClick = onStartVoiceRecord,
        enabled = !isGenerating && !isRecordingVoice && !isTranscribing,
        modifier = Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(InkPanel)
          .border(1.dp, IceBorderFaint, CircleShape)
          .testTag("voice_record_button")
      ) {
        Icon(
          imageVector = Icons.Default.Mic,
          contentDescription = "Sprachaufnahme",
          tint = if (!isGenerating && !isRecordingVoice && !isTranscribing) IceCyanLight else TextFaint,
          modifier = Modifier.size(20.dp)
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      val canSend = inputText.isNotBlank() && !isGenerating && !isTranscribing

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
            contentDescription = if (editingMessage != null) "Speichern und neu erzählen" else "Aktion ausführen",
            tint = if (canSend) TextOnPrimary else TextFaint
          )
        }
      }
    }
  }
}

