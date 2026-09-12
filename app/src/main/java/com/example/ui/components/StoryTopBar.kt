package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.CheckpointEntity
import com.example.data.model.StoryEntity
import com.example.ui.dna.DnaColors
import com.example.ui.dna.DnaTypography

/**
 * StoryTopBar - Gestaltet nach der MauntingStudios Design-DNA (Header-Standard):
 * - Sticky, dezent transluzente Flaeche mit Ice-Border
 * - Ice-Cyan Akzenttoene und klare Pill-Badges
 * - Branding "Maunting Story Fable"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryTopBar(
  story: StoryEntity?,
  checkpoint: CheckpointEntity?,
  onOpenDrawer: () -> Unit,
  onOpenStorySelector: () -> Unit,
  onOpenNotebook: () -> Unit,
  modifier: Modifier = Modifier
) {
  TopAppBar(
    modifier = modifier,
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = DnaColors.Surface,
      titleContentColor = DnaColors.OnSurface
    ),
    navigationIcon = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp)
      ) {
        IconButton(
          onClick = onOpenDrawer,
          modifier = Modifier.testTag("open_drawer_button")
        ) {
          Icon(
            imageVector = Icons.Default.Menu,
            contentDescription = "Geschichten & Menü öffnen",
            tint = DnaColors.Primary
          )
        }
      }
    },
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onOpenStorySelector() }
      ) {
        Surface(
          shape = CircleShape,
          color = DnaColors.SurfaceContainerHigh,
          border = BorderStroke(1.dp, DnaColors.Primary.copy(alpha = 0.5f)),
          modifier = Modifier.size(28.dp)
        ) {
          Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = story?.title ?: "Maunting Story Fable",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              fontFamily = DnaTypography.ManropeFamily,
              maxLines = 1,
              color = DnaColors.OnSurface
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
              imageVector = Icons.Default.SwapHoriz,
              contentDescription = "Geschichte wechseln",
              tint = DnaColors.Primary,
              modifier = Modifier.size(16.dp)
            )
          }
          Text(
            text = "${story?.genre ?: "Interaktives RPG"} • ${story?.selectedModel ?: "Gemini"}",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = DnaTypography.InterFamily,
            color = DnaColors.OnSurfaceVariant
          )
        }
      }
    },
    actions = {
      // In-Game Time Pill (Stadium Badge style)
      Surface(
        modifier = Modifier
          .clip(RoundedCornerShape(999.dp))
          .clickable { onOpenNotebook() }
          .testTag("in_game_time_pill"),
        color = DnaColors.PrimaryContainer,
        border = BorderStroke(1.dp, DnaColors.Primary.copy(alpha = 0.35f))
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Icon(
            imageVector = Icons.Default.AccessTime,
            contentDescription = null,
            tint = DnaColors.Primary,
            modifier = Modifier.size(13.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = checkpoint?.inGameTime ?: "Tag 1, 20:00",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = DnaTypography.JetBrainsMonoFamily,
            color = DnaColors.OnPrimaryContainer,
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
          imageVector = Icons.AutoMirrored.Filled.MenuBook,
          contentDescription = "Das Notizbuch öffnen",
          tint = DnaColors.Primary
        )
      }
    }
  )
}
