package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CheckpointEntity
import com.example.data.model.StoryEntity
import com.example.ui.theme.AmberGoldContainer
import com.example.ui.theme.AmberGoldLight
import com.example.ui.theme.AmberGoldPrimary
import com.example.ui.theme.SlateDark950
import com.example.ui.theme.TextParchment
import com.example.ui.theme.TextParchmentMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryTopBar(
  story: StoryEntity?,
  checkpoint: CheckpointEntity?,
  onOpenStorySelector: () -> Unit,
  onOpenNotebook: () -> Unit,
  onOpenSettings: () -> Unit,
  modifier: Modifier = Modifier
) {
  TopAppBar(
    modifier = modifier,
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = SlateDark950,
      titleContentColor = TextParchment
    ),
    title = {
      Column(
        modifier = Modifier.clickable { onOpenStorySelector() }
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
          .clickable { onOpenNotebook() }
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

      // Notebook Button
      IconButton(
        onClick = onOpenNotebook,
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
        onClick = onOpenSettings,
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
}
