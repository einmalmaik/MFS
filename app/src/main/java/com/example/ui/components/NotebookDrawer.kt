package com.example.ui.components

import com.example.ui.dna.DnaColors
import com.example.ui.dna.DnaTypography
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import com.example.ui.dna.DnaButton
import com.example.ui.dna.DnaButtonVariant
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CheckpointEntity

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotebookDrawer(
  checkpoint: CheckpointEntity?,
  allCheckpoints: List<CheckpointEntity> = emptyList(),
  adultContentEnabled: Boolean = true,
  onClose: () -> Unit,
  onOpenManualEdit: () -> Unit,
  onUpdateInjuries: ((characterName: String, updatedInjuries: List<com.example.data.model.CharacterInjury>) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  var selectedTab by remember { mutableIntStateOf(0) } // 0 = Tagebuch, 1 = Charakter (Visualizer), 2 = Welt & Inventar

  val npcs = checkpoint?.getNpcList() ?: emptyList()
  val inventory = checkpoint?.getInventoryList() ?: emptyList()
  val milestones = checkpoint?.getMilestonesList() ?: emptyList()

  // Group all checkpoints by day number
  val dayGroups = remember(allCheckpoints) {
    if (allCheckpoints.isEmpty() && checkpoint != null) {
      listOf(checkpoint).groupBy { it.extractDayNumber() }
    } else {
      allCheckpoints.groupBy { it.extractDayNumber() }
    }.toSortedMap(compareByDescending { it })
  }

  Column(
    modifier = modifier
      .fillMaxHeight()
      .background(DnaColors.Surface)
      .padding(horizontal = 16.dp, vertical = 12.dp)
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.MenuBook,
          contentDescription = null,
          tint = DnaColors.Primary,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = "Das Notizbuch",
            style = MaterialTheme.typography.titleLarge,
            color = DnaColors.OnSurface,
            fontFamily = DnaTypography.ManropeFamily
          )
          Text(
            text = "Chronik dieser Geschichte",
            style = MaterialTheme.typography.labelSmall,
            color = DnaColors.MutedForeground
          )
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onOpenManualEdit,
          modifier = Modifier.testTag("edit_notebook_button")
        ) {
          Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Bearbeiten",
            tint = DnaColors.Primary
          )
        }
        IconButton(
          onClick = onClose,
          modifier = Modifier.testTag("close_notebook_button")
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Schließen",
            tint = DnaColors.OnSurfaceVariant
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 3 View Tabs
    TabRow(
      selectedTabIndex = selectedTab,
      containerColor = DnaColors.SurfaceDim,
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
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Tagebuch", fontSize = 12.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
          }
        }
      )
      Tab(
        selected = selectedTab == 1,
        onClick = { selectedTab = 1 },
        text = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Charakter", fontSize = 12.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
          }
        }
      )
      Tab(
        selected = selectedTab == 2,
        onClick = { selectedTab = 2 },
        text = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Welt & Items", fontSize = 12.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal)
          }
        }
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Content area with vertical scroll
    Column(
      modifier = Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
    ) {
      when (selectedTab) {
        0 -> {
          // --- TAB 0: DAY-BY-DAY CHRONICLE (PRO TAG) ---
          Text(
            text = "TAGE",
            style = MaterialTheme.typography.labelSmall,
            color = DnaColors.StoryAmberCampfire,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(8.dp))

          if (dayGroups.isEmpty()) {
            Card(
              colors = CardDefaults.cardColors(containerColor = DnaColors.SurfaceContainer),
              shape = RoundedCornerShape(12.dp)
            ) {
              Box(modifier = Modifier.padding(16.dp)) {
                Text(
                  text = "Hier sammeln sich deine Tage, sobald die Geschichte läuft.",
                  style = MaterialTheme.typography.bodySmall,
                  color = DnaColors.OnSurfaceVariant
                )
              }
            }
          } else {
            dayGroups.forEach { (dayNumber, dayCheckpoints) ->
              val latestInDay = dayCheckpoints.firstOrNull() ?: checkpoint
              val isCurrentDay = latestInDay?.extractDayNumber() == (checkpoint?.extractDayNumber() ?: 1)

              Card(
                colors = CardDefaults.cardColors(
                  containerColor = if (isCurrentDay) DnaColors.SurfaceContainer else DnaColors.SurfaceDim
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                  1.dp,
                  if (isCurrentDay) DnaColors.Primary.copy(alpha = 0.6f) else DnaColors.SurfaceContainerHigh
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(bottom = 12.dp)
              ) {
                Column(modifier = Modifier.padding(14.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Surface(
                        color = if (isCurrentDay) DnaColors.PrimaryContainer else DnaColors.SurfaceContainerHigh,
                        shape = RoundedCornerShape(6.dp)
                      ) {
                        Text(
                          text = "TAG $dayNumber",
                          style = MaterialTheme.typography.labelSmall,
                          color = if (isCurrentDay) DnaColors.StoryAmberCampfire else DnaColors.OnSurfaceVariant,
                          fontWeight = FontWeight.Bold,
                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                      }
                      if (isCurrentDay) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                          text = "(Aktuell)",
                          style = MaterialTheme.typography.labelSmall,
                          color = DnaColors.StoryAmberCampfire
                        )
                      }
                    }

                    Text(
                      text = latestInDay?.inGameTime ?: "",
                      style = MaterialTheme.typography.labelSmall,
                      color = DnaColors.OnSurfaceVariant
                    )
                  }

                  Spacer(modifier = Modifier.height(10.dp))

                  // Location & Weather for this day
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Default.LocationOn,
                      contentDescription = null,
                      tint = DnaColors.Primary,
                      modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = latestInDay?.location ?: "Unbekannter Ort",
                      style = MaterialTheme.typography.bodySmall,
                      fontWeight = FontWeight.SemiBold,
                      color = DnaColors.OnSurface
                    )
                  }

                  Spacer(modifier = Modifier.height(4.dp))

                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Default.WbCloudy,
                      contentDescription = null,
                      tint = DnaColors.OnSurfaceVariant,
                      modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = latestInDay?.weather ?: "Ruhig",
                      style = MaterialTheme.typography.bodySmall,
                      color = DnaColors.OnSurfaceVariant
                    )
                  }

                  Spacer(modifier = Modifier.height(10.dp))

                  // Day Events Summary
                  val daySummary = latestInDay?.previousEventsSummary?.ifBlank { null }
                  if (daySummary != null) {
                    Text(
                      text = daySummary,
                      style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                      color = DnaColors.OnSurface
                    )
                  }
                }
              }
            }
          }
        }

        1 -> {
          // --- TAB 1: 3D/GAME CHARACTER VISUALIZER ---
          CharacterVisualizer(
            checkpoint = checkpoint,
            adultContentEnabled = adultContentEnabled,
            onUpdateInjuries = onUpdateInjuries
          )

          Spacer(modifier = Modifier.height(16.dp))

          // Detailed NPC list
          SectionHeader(title = "Figuren (${npcs.size})", icon = Icons.Default.Face)

          if (npcs.isEmpty()) {
            Card(
              colors = CardDefaults.cardColors(containerColor = DnaColors.SurfaceContainer),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Box(modifier = Modifier.padding(14.dp)) {
                Text(
                  text = "Figuren erscheinen hier, sobald sie auftauchen.",
                  style = MaterialTheme.typography.bodySmall,
                  color = DnaColors.OnSurfaceVariant
                )
              }
            }
          } else {
            npcs.forEach { npc ->
              Card(
                colors = CardDefaults.cardColors(containerColor = DnaColors.SurfaceContainer),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, DnaColors.Border),
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(bottom = 8.dp)
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Text(
                      text = npc.name,
                      style = MaterialTheme.typography.titleSmall,
                      color = DnaColors.Primary,
                      fontWeight = FontWeight.Bold
                    )
                    Surface(
                      color = DnaColors.SurfaceContainerHigh,
                      shape = RoundedCornerShape(6.dp)
                    ) {
                      Text(
                        text = npc.currentMood,
                        style = MaterialTheme.typography.labelSmall,
                        color = DnaColors.OnSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "${npc.outfit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = DnaColors.OnSurface
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = "${npc.relationshipToPlayer}",
                    style = MaterialTheme.typography.bodySmall,
                    color = DnaColors.OnSurfaceVariant
                  )
                }
              }
            }
          }
        }

        2 -> {
          // --- TAB 2: WORLD & INVENTORY ---
          SectionHeader(title = "Ort", icon = Icons.Default.LocationOn)

          Card(
            colors = CardDefaults.cardColors(containerColor = DnaColors.SurfaceContainer),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, DnaColors.Border),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              InfoRow(
                icon = Icons.Default.AccessTime,
                label = "In-Game Zeit",
                value = checkpoint?.inGameTime?.takeIf { it.isNotBlank() }
                  ?: "Noch nicht begonnen"
              )
              Spacer(modifier = Modifier.height(8.dp))
              InfoRow(
                icon = Icons.Default.LocationOn,
                label = "Ort",
                value = checkpoint?.location ?: "Unbekannter Ort"
              )
              Spacer(modifier = Modifier.height(8.dp))
              InfoRow(
                icon = Icons.Default.WbCloudy,
                label = "Wetter",
                value = checkpoint?.weather ?: "Ruhig"
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Inventory
          SectionHeader(title = "Inventar (${inventory.size})", icon = Icons.Default.Luggage)

          Card(
            colors = CardDefaults.cardColors(containerColor = DnaColors.SurfaceContainer),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, DnaColors.Border),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              if (inventory.isEmpty()) {
                Text(
                  text = "Keine Gegenstände im Inventar",
                  style = MaterialTheme.typography.bodySmall,
                  color = DnaColors.MutedForeground
                )
              } else {
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  inventory.forEach { item ->
                    Surface(
                      color = DnaColors.SurfaceContainerHigh,
                      shape = RoundedCornerShape(8.dp),
                      border = BorderStroke(1.dp, DnaColors.Border)
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                      ) {
                        Icon(
                          imageVector = Icons.Default.Luggage,
                          contentDescription = null,
                          tint = DnaColors.Primary,
                          modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                          text = item,
                          style = MaterialTheme.typography.labelSmall,
                          color = DnaColors.OnSurface
                        )
                      }
                    }
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Milestones & Long-Term Memories
          SectionHeader(title = "Bedeutsame Meilensteine (${milestones.size})", icon = Icons.Default.Book)

          Card(
            colors = CardDefaults.cardColors(containerColor = DnaColors.SurfaceContainer),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, DnaColors.Border),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              if (milestones.isEmpty()) {
                Text(
                  text = "Noch keine tiefen Meilensteine verzeichnet. Die KI wird prägende Schwüre, Verluste und Wendepunkte hier dauerhaft verankern.",
                  style = MaterialTheme.typography.bodySmall,
                  color = DnaColors.MutedForeground
                )
              } else {
                milestones.forEach { milestone ->
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                  ) {
                    Text(
                      text = "• ",
                      color = DnaColors.Primary,
                      fontWeight = FontWeight.Bold
                    )
                    Text(
                      text = milestone,
                      style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                      color = DnaColors.OnSurface
                    )
                  }
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Manual edit button
      DnaButton(
        text = "Notizbuch manuell korrigieren",
        onClick = onOpenManualEdit,
        icon = Icons.Default.Edit,
        variant = DnaButtonVariant.PRIMARY,
        fullWidth = true,
        testTag = "manual_edit_button"
      )

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.padding(vertical = 6.dp)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = DnaColors.Primary,
      modifier = Modifier.size(16.dp)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      color = DnaColors.OnSurface,
      fontWeight = FontWeight.SemiBold
    )
  }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = DnaColors.Primary.copy(alpha = 0.8f),
      modifier = Modifier.size(16.dp)
    )
    Spacer(modifier = Modifier.width(8.dp))
    Column {
      Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = DnaColors.MutedForeground,
        fontSize = 10.sp
      )
      Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium,
        color = DnaColors.OnSurface
      )
    }
  }
}
