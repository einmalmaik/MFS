package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ForkRight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.data.model.StoryEntity
import com.example.ui.dna.DnaActionConfirmDialog
import com.example.ui.dna.DnaButton
import com.example.ui.dna.DnaButtonVariant
import com.example.ui.dna.DnaColors
import com.example.ui.dna.DnaTypography

/**
 * Left-side navigation drawer styled after MauntingStudios Design-DNA:
 * - Employs brand logo with ambient edge glow
 * - Technical calm dark surfaces and precise Ice Cyan highlights
 * - Full story lifecycle controls (New, Active, Archived, Branching, Deletion, Settings)
 */
@Composable
fun StoryNavigationDrawer(
  currentStoryId: Long?,
  stories: List<StoryEntity>,
  onSelectStory: (Long) -> Unit,
  onOpenCreateStory: () -> Unit,
  onToggleArchiveStory: (storyId: Long, isArchived: Boolean) -> Unit,
  onBranchStory: (sourceStoryId: Long, branchTitle: String) -> Unit,
  onDeleteStory: (Long) -> Unit,
  onOpenSettings: () -> Unit,
  onCloseDrawer: () -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedTab by remember { mutableIntStateOf(0) } // 0 = Aktive, 1 = Archiviert
  var storyToDelete by remember { mutableStateOf<StoryEntity?>(null) }
  var storyToBranch by remember { mutableStateOf<StoryEntity?>(null) }

  val activeStories = stories.filter { !it.isArchived }
  val archivedStories = stories.filter { it.isArchived }
  val displayedStories = if (selectedTab == 0) activeStories else archivedStories

  ModalDrawerSheet(
    modifier = modifier
      .width(320.dp)
      .fillMaxHeight(),
    drawerContainerColor = DnaColors.Surface
  ) {
    Column(
      modifier = Modifier
        .fillMaxHeight()
        .padding(16.dp)
    ) {
      // Header with Logo
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = CircleShape,
            color = DnaColors.SurfaceContainerHigh,
            border = BorderStroke(1.5.dp, DnaColors.Primary),
            modifier = Modifier.size(42.dp)
          ) {
            Image(
              painter = painterResource(id = R.drawable.app_logo),
              contentDescription = "Maunting Story Fable Logo",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Maunting Story Fable",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              fontFamily = DnaTypography.ManropeFamily,
              color = DnaColors.OnSurface
            )
            Text(
              text = "Deine Geschichten & Chroniken",
              style = MaterialTheme.typography.labelSmall,
              fontFamily = DnaTypography.InterFamily,
              color = DnaColors.OnSurfaceVariant
            )
          }
        }

        IconButton(onClick = onCloseDrawer) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Menü schließen",
            tint = DnaColors.OnSurfaceVariant
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Button "Neue Geschichte beginnen"
      DnaButton(
        text = "Neue Geschichte beginnen",
        onClick = {
          onCloseDrawer()
          onOpenCreateStory()
        },
        icon = Icons.Default.Add,
        variant = DnaButtonVariant.PRIMARY,
        fullWidth = true,
        testTag = "drawer_new_story_button"
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Tabs: Aktive vs Archiviert
      TabRow(
        selectedTabIndex = selectedTab,
        containerColor = DnaColors.SurfaceContainerLow,
        contentColor = DnaColors.Primary,
        indicator = { tabPositions ->
          TabRowDefaults.SecondaryIndicator(
            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
            color = DnaColors.Primary
          )
        }
      ) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = {
            Text(
              text = "Aktiv (${activeStories.size})",
              fontFamily = DnaTypography.InterFamily,
              fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
              color = if (selectedTab == 0) DnaColors.Primary else DnaColors.OnSurfaceVariant
            )
          }
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = {
            Text(
              text = "Archiv (${archivedStories.size})",
              fontFamily = DnaTypography.InterFamily,
              fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
              color = if (selectedTab == 1) DnaColors.Primary else DnaColors.OnSurfaceVariant
            )
          }
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Story List
      if (displayedStories.isEmpty()) {
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = if (selectedTab == 0) "Keine aktiven Geschichten vorhanden." else "Keine archivierten Geschichten.",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = DnaTypography.InterFamily,
            color = DnaColors.OnSurfaceVariant
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(displayedStories, key = { it.id }) { story ->
            val isSelected = story.id == currentStoryId

            DrawerStoryCard(
              story = story,
              isSelected = isSelected,
              onClick = {
                onSelectStory(story.id)
                onCloseDrawer()
              },
              onToggleArchive = {
                onToggleArchiveStory(story.id, !story.isArchived)
              },
              onBranch = {
                storyToBranch = story
              },
              onDelete = {
                storyToDelete = story
              }
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(color = DnaColors.BorderFaint)
      Spacer(modifier = Modifier.height(10.dp))

      // Bottom Bar: Settings & API Configuration
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .clickable {
            onCloseDrawer()
            onOpenSettings()
          }
          .testTag("drawer_settings_button"),
        color = DnaColors.SurfaceContainerHigh,
        border = BorderStroke(1.dp, DnaColors.Border)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Tune,
              contentDescription = null,
              tint = DnaColors.Primary,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Einstellungen & API-Key",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = DnaTypography.InterFamily,
                fontWeight = FontWeight.SemiBold,
                color = DnaColors.OnSurface
              )
              Text(
                text = "Modelle, Denkkraft & Prompts",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = DnaTypography.InterFamily,
                color = DnaColors.OnSurfaceVariant
              )
            }
          }
        }
      }
    }
  }

  // Confirm Delete Dialog
  storyToDelete?.let { story ->
    DnaActionConfirmDialog(
      title = "Geschichte löschen?",
      description = "Möchtest du '${story.title}' wirklich unwiderruflich löschen? Alle Nachrichten, Meilensteine und der Speicherstand gehen verloren.",
      confirmButtonText = "Endgültig löschen",
      cancelButtonText = "Abbrechen",
      isDestructive = true,
      onConfirm = {
        onDeleteStory(story.id)
        storyToDelete = null
      },
      onDismiss = { storyToDelete = null },
      testTag = "delete_story_confirm_dialog"
    )
  }

  // Branch Timeline Dialog
  storyToBranch?.let { sourceStory ->
    DnaActionConfirmDialog(
      title = "Zeitlinie verzweigen",
      description = "Erstelle eine neue alternative Geschichte basierend auf dem aktuellen Stand von '${sourceStory.title}'. Du kannst dort andere Entscheidungen testen.",
      confirmButtonText = "Zweig erstellen",
      cancelButtonText = "Abbrechen",
      isDestructive = false,
      onConfirm = {
        onBranchStory(sourceStory.id, "${sourceStory.title} (Zweig)")
        storyToBranch = null
        onCloseDrawer()
      },
      onDismiss = { storyToBranch = null },
      testTag = "branch_story_confirm_dialog"
    )
  }
}

@Composable
private fun DrawerStoryCard(
  story: StoryEntity,
  isSelected: Boolean,
  onClick: () -> Unit,
  onToggleArchive: () -> Unit,
  onBranch: () -> Unit,
  onDelete: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("drawer_story_card_${story.id}"),
    colors = CardDefaults.cardColors(
      containerColor = if (isSelected) DnaColors.SurfaceContainerHighest else DnaColors.SurfaceContainer
    ),
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(
      width = if (isSelected) 1.5.dp else 1.dp,
      color = if (isSelected) DnaColors.Primary else DnaColors.Border
    )
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          if (isSelected) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = "Aktiv",
              tint = DnaColors.Primary,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
          }
          Text(
            text = story.title,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = DnaTypography.InterFamily,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) DnaColors.Primary else DnaColors.OnSurface,
            maxLines = 1
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = "${story.genre} • ${story.selectedModel}",
        style = MaterialTheme.typography.labelSmall,
        fontFamily = DnaTypography.InterFamily,
        color = DnaColors.OnSurfaceVariant,
        fontSize = 11.sp
      )

      Spacer(modifier = Modifier.height(2.dp))
      val hours = story.playTimeSeconds / 3600
      val minutes = (story.playTimeSeconds % 3600) / 60
      val timeString = if (hours > 0) {
        "${hours}h ${minutes}m"
      } else if (minutes > 0) {
        "${minutes}m"
      } else {
        "< 1m"
      }
      Text(
        text = "Spielzeit: $timeString",
        style = MaterialTheme.typography.labelSmall,
        fontFamily = DnaTypography.JetBrainsMonoFamily,
        color = DnaColors.MutedForeground,
        fontSize = 10.sp
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Action row: Archive, Branch, Delete
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Archive / Unarchive
        IconButton(
          onClick = onToggleArchive,
          modifier = Modifier.size(28.dp)
        ) {
          Icon(
            imageVector = if (story.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
            contentDescription = if (story.isArchived) "Wiederherstellen" else "Archivieren",
            tint = DnaColors.Secondary,
            modifier = Modifier.size(16.dp)
          )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Branch
        IconButton(
          onClick = onBranch,
          modifier = Modifier.size(28.dp)
        ) {
          Icon(
            imageVector = Icons.Default.ForkRight,
            contentDescription = "Zweig erstellen",
            tint = DnaColors.OnSurfaceVariant,
            modifier = Modifier.size(16.dp)
          )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Delete
        IconButton(
          onClick = onDelete,
          modifier = Modifier.size(28.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Löschen",
            tint = DnaColors.StatusDestructive.copy(alpha = 0.85f),
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }
  }
}
