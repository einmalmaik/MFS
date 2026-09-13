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
import androidx.compose.material.icons.filled.Edit
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
import com.example.data.model.displayTitle
import androidx.compose.ui.text.style.TextOverflow
import com.example.ui.dna.DnaBadge
import com.example.ui.dna.DnaBadgeTone
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
  onEditStoryPrompt: () -> Unit,
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
              text = story?.displayTitle ?: "Maunting Story Fable",
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
          // Ohne die Modell-Id: Zusammen mit ihr brach die Zeile dreifach um und schob den
          // Titel aus der Leiste. Welches Modell läuft, steht in den Einstellungen.
          Text(
            text = story?.genre ?: "Interaktives RPG",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = DnaTypography.InterFamily,
            color = DnaColors.OnSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }
    },
    actions = {
      // In-Game-Zeit als echte Stadium-Pille.
      //
      // Vorher war das ein handgebauter Nachbau von DnaBadge mit einem Surface ohne
      // shape-Parameter. Surface zeichnet Hintergrund und Rahmen aber mit seinem eigenen shape,
      // das auf RectangleShape steht — das .clip() am Modifier beschnitt nur, sodass an den
      // Ecken sichtbar ein gestrecktes Rechteck stehen blieb. DnaBadge setzt CircleShape direkt
      // am Surface und ist damit unter jedem Zoom rund.
      // Vor dem ersten Zug gibt es noch keine Spielzeit. Ein Platzhalter statt einer erfundenen
      // Uhrzeit -- die Pille verschwinden zu lassen würde die Leiste beim ersten Zug umbauen.
      DnaBadge(
        text = checkpoint?.inGameTime?.takeIf { it.isNotBlank() } ?: "Noch nicht begonnen",
        tone = DnaBadgeTone.ICE,
        icon = Icons.Default.AccessTime,
        showDot = false,
        fontFamily = DnaTypography.JetBrainsMonoFamily,
        modifier = Modifier
          .clip(CircleShape)
          .clickable { onOpenNotebook() }
          .testTag("in_game_time_pill")
      )

      Spacer(modifier = Modifier.width(2.dp))

      // Prompt dieser Geschichte. Steht hier und nicht in den Einstellungen, weil dort nur
      // noch Globales liegt — und weil man beim Lesen merkt, dass die Regie nachgeschärft
      // gehört, nicht beim Aufräumen der Modellwahl.
      if (story != null) {
        IconButton(
          onClick = onEditStoryPrompt,
          modifier = Modifier.testTag("edit_story_prompt_button")
        ) {
          Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Prompt dieser Geschichte bearbeiten",
            tint = DnaColors.Primary
          )
        }
      }

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
