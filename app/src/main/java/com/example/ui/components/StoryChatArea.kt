package com.example.ui.components

import com.example.ui.dna.DnaColors
import com.example.ui.dna.DnaTypography
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.example.ui.dna.DnaButton
import com.example.ui.dna.DnaButtonVariant
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MessageEntity
import com.example.domain.model.TurnProgress
import com.example.ui.StoryUiState

@Composable
fun StoryChatArea(
  uiState: StoryUiState,
  listState: LazyListState,
  onOpenSettings: () -> Unit,
  onDismissError: () -> Unit,
  onRewindToMessage: (MessageEntity) -> Unit,
  onEditMessage: (MessageEntity) -> Unit,
  onResendMessage: (MessageEntity) -> Unit = {},
  onBranchFromMessage: (MessageEntity) -> Unit,
  onRetryTurn: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  LaunchedEffect(uiState.errorMessage, uiState.messages.size, uiState.isGenerating) {
    if (uiState.errorMessage != null || uiState.isGenerating) {
      val count = listState.layoutInfo.totalItemsCount
      if (count > 0) {
        listState.animateScrollToItem(count - 1)
      }
    }
  }

  LazyColumn(
    state = listState,
    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
    modifier = modifier.fillMaxSize()
  ) {
    // API Key Warning Banner if missing
    if (!uiState.effectiveApiKeyPresent) {
      item(key = "api_key_banner") {
        Surface(
          color = DnaColors.PrimaryContainer,
          border = BorderStroke(1.dp, DnaColors.Primary),
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
              tint = DnaColors.Primary,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "API-Schlüssel fehlt",
                style = MaterialTheme.typography.titleSmall,
                color = DnaColors.StoryAmberCampfire,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Trage ihn in den Einstellungen ein, um zu spielen.",
                style = MaterialTheme.typography.bodySmall,
                color = DnaColors.OnSurface
              )
            }
            DnaButton(
              text = "Eingeben",
              onClick = onOpenSettings,
              variant = DnaButtonVariant.PRIMARY,
              testTag = "enter_api_key_button"
            )
          }
        }
      }
    }

    // Eine Geschichte ohne einzige Nachricht war früher ein Sekundenbruchteil: Die KI eröffnete
    // sie ungefragt. Jetzt schreibt der Spieler den ersten Zug, und ohne diesen Hinweis stünde
    // er vor einer leeren Fläche und wüsste nicht, dass er am Zug ist.
    if (uiState.messages.isEmpty() && !uiState.isGenerating) {
      item(key = "empty_story_hint") {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 56.dp)
        ) {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = DnaColors.Primary.copy(alpha = 0.6f),
            modifier = Modifier.size(34.dp)
          )
          Spacer(modifier = Modifier.height(14.dp))
          Text(
            text = "Die Geschichte wartet auf dich",
            style = MaterialTheme.typography.titleMedium,
            fontFamily = DnaTypography.ManropeFamily,
            fontWeight = FontWeight.Bold,
            color = DnaColors.OnSurface
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Schreib den ersten Zug. Wo du bist, welcher Tag es ist und wie spät — " +
              "das ergibt sich aus deinem Prompt und dem, was du jetzt schreibst.",
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = DnaTypography.InterFamily,
            color = DnaColors.OnSurfaceVariant,
            textAlign = TextAlign.Center
          )
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
        onRewind = onRewindToMessage,
        onEditUserMessage = onEditMessage,
        onResendUserMessage = onResendMessage,
        onBranchFromHere = onBranchFromMessage
      )
    }

    // Real-time streaming or generation indicator
    if (uiState.isGenerating) {
      item(key = "streaming_response") {
        Surface(
          color = DnaColors.SurfaceContainer.copy(alpha = 0.85f),
          border = BorderStroke(1.dp, DnaColors.Primary.copy(alpha = 0.4f)),
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
                  tint = DnaColors.Primary,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = when (uiState.turnStatus) {
                    is TurnProgress.Thinking -> "Denkt nach …"
                    is TurnProgress.Streaming -> "Erzählt weiter …"
                    is TurnProgress.ExtractingState -> "Notizbuch wird aktualisiert …"
                    else -> "Schreibt …"
                  },
                  style = MaterialTheme.typography.labelMedium,
                  color = DnaColors.Primary
                )
              }
              CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = DnaColors.Primary,
                strokeWidth = 2.dp
              )
            }

            if (uiState.streamChunk.isNotBlank()) {
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = uiState.streamChunk,
                style = MaterialTheme.typography.bodyLarge.copy(
                  fontFamily = DnaTypography.InterFamily,
                  lineHeight = 26.sp,
                  color = DnaColors.OnSurface
                )
              )
            }
          }
        }
      }
    }

    // Error message banner at the bottom where user is looking & reading
    uiState.errorMessage?.let { error ->
      item(key = "error_banner") {
        Surface(
          color = DnaColors.StatusDestructive.copy(alpha = 0.12f),
          border = BorderStroke(1.dp, DnaColors.StatusDestructive.copy(alpha = 0.35f)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = DnaColors.StatusDestructive,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = error,
                style = MaterialTheme.typography.bodySmall.copy(
                  fontFamily = DnaTypography.InterFamily,
                  lineHeight = 18.sp
                ),
                color = DnaColors.OnSurface,
                modifier = Modifier.weight(1f)
              )
              IconButton(onClick = onDismissError) {
                Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = "Schließen",
                  tint = DnaColors.OnSurfaceVariant,
                  modifier = Modifier.size(16.dp)
                )
              }
            }
            if (onRetryTurn != null) {
              Spacer(modifier = Modifier.height(10.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
              ) {
                DnaButton(
                  text = "Erneut versuchen",
                  onClick = onRetryTurn,
                  variant = DnaButtonVariant.SECONDARY,
                  testTag = "retry_turn_button"
                )
              }
            }
          }
        }
      }
    }
  }
}
