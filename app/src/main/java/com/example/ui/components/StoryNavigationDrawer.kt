package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ForkRight
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StoryEntity
import com.example.ui.theme.AmberGoldContainer
import com.example.ui.theme.AmberGoldLight
import com.example.ui.theme.AmberGoldPrimary
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.OnAmberGoldContainer
import com.example.ui.theme.SlateDark600
import com.example.ui.theme.SlateDark700
import com.example.ui.theme.SlateDark800
import com.example.ui.theme.SlateDark900
import com.example.ui.theme.SlateDark950
import com.example.ui.theme.TextParchment
import com.example.ui.theme.TextParchmentFaint
import com.example.ui.theme.TextParchmentMuted

/**
 * Left-side navigation drawer providing swipe-to-open and quick story switching,
 * story creation, active vs archived view, branching, and global settings triggers.
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
    drawerContainerColor = SlateDark950
  ) {
    Column(
      modifier = Modifier
        .fillMaxHeight()
        .padding(16.dp)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = CircleShape,
            color = AmberGoldContainer,
            modifier = Modifier.size(36.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.MenuBook,
                contentDescription = null,
                tint = AmberGoldPrimary,
                modifier = Modifier.size(20.dp)
              )
            }
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "StoryForge",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Serif,
              color = TextParchment
            )
            Text(
              text = "Deine Geschichten & Chroniken",
              style = MaterialTheme.typography.labelSmall,
              color = TextParchmentFaint
            )
          }
        }

        IconButton(onClick = onCloseDrawer) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Menü schließen",
            tint = TextParchmentMuted
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Button "Neue Geschichte beginnen"
      Button(
        onClick = {
          onCloseDrawer()
          onOpenCreateStory()
        },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("drawer_new_story_button"),
        colors = ButtonDefaults.buttonColors(
          containerColor = AmberGoldPrimary,
          contentColor = SlateDark900
        ),
        shape = RoundedCornerShape(10.dp)
      ) {
        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Neue Geschichte beginnen", fontWeight = FontWeight.Bold)
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Tabs: Aktive vs Archiviert
      TabRow(
        selectedTabIndex = selectedTab,
        containerColor = SlateDark900,
        contentColor = AmberGoldPrimary,
        indicator = { tabPositions ->
          TabRowDefaults.SecondaryIndicator(
            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
            color = AmberGoldPrimary
          )
        }
      ) {
        Tab(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          text = {
            Text(
              text = "Aktiv (${activeStories.size})",
              fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
              color = if (selectedTab == 0) AmberGoldPrimary else TextParchmentMuted
            )
          }
        )
        Tab(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          text = {
            Text(
              text = "Archiv (${archivedStories.size})",
              fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
              color = if (selectedTab == 1) AmberGoldPrimary else TextParchmentMuted
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
            color = TextParchmentMuted
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
      HorizontalDivider(color = SlateDark700)
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
        color = SlateDark900,
        border = BorderStroke(1.dp, SlateDark700)
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
              tint = AmberGoldPrimary,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Einstellungen & API-Key",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextParchment
              )
              Text(
                text = "Modelle, Denkkraft & Prompts",
                style = MaterialTheme.typography.labelSmall,
                color = TextParchmentFaint
              )
            }
          }
        }
      }
    }
  }

  // Confirm Delete Dialog
  storyToDelete?.let { story ->
    AlertDialog(
      onDismissRequest = { storyToDelete = null },
      title = { Text("Geschichte löschen?", color = TextParchment) },
      text = {
        Text(
          text = "Möchtest du '${story.title}' wirklich unwiderruflich löschen? Alle Nachrichten, Meilensteine und der Speicherstand gehen verloren.",
          color = TextParchmentMuted
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onDeleteStory(story.id)
            storyToDelete = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = CrimsonDanger)
        ) {
          Text("Endgültig löschen")
        }
      },
      dismissButton = {
        TextButton(onClick = { storyToDelete = null }) {
          Text("Abbrechen", color = TextParchment)
        }
      },
      containerColor = SlateDark800
    )
  }

  // Branch Timeline Dialog
  storyToBranch?.let { sourceStory ->
    AlertDialog(
      onDismissRequest = { storyToBranch = null },
      title = { Text("Zeitlinie verzweigen", color = TextParchment) },
      text = {
        Text(
          text = "Erstelle eine neue alternative Geschichte basierend auf dem aktuellen Stand von '${sourceStory.title}'. Du kannst dort andere Entscheidungen testen.",
          color = TextParchmentMuted
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onBranchStory(sourceStory.id, "${sourceStory.title} (Zweig)")
            storyToBranch = null
            onCloseDrawer()
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = AmberGoldPrimary,
            contentColor = SlateDark900
          )
        ) {
          Text("Zweig erstellen", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { storyToBranch = null }) {
          Text("Abbrechen", color = TextParchment)
        }
      },
      containerColor = SlateDark800
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
      containerColor = if (isSelected) SlateDark800 else SlateDark900
    ),
    shape = RoundedCornerShape(10.dp),
    border = BorderStroke(
      width = if (isSelected) 1.5.dp else 1.dp,
      color = if (isSelected) AmberGoldPrimary else SlateDark700
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
              tint = AmberGoldPrimary,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
          }
          Text(
            text = story.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) AmberGoldPrimary else TextParchment,
            maxLines = 1
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = "${story.genre} • ${story.selectedModel}",
        style = MaterialTheme.typography.labelSmall,
        color = TextParchmentMuted,
        fontSize = 11.sp
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
            tint = AmberGoldLight,
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
            tint = TextParchmentMuted,
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
            tint = CrimsonDanger.copy(alpha = 0.8f),
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }
  }
}
