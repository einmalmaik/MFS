package com.example.ui.components

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
import com.example.ui.theme.AmberGoldContainer
import com.example.ui.theme.AmberGoldLight
import com.example.ui.theme.AmberGoldPrimary
import com.example.ui.theme.SlateDark600
import com.example.ui.theme.SlateDark700
import com.example.ui.theme.SlateDark800
import com.example.ui.theme.SlateDark900
import com.example.ui.theme.SlateDark950
import com.example.ui.theme.TextParchment
import com.example.ui.theme.TextParchmentFaint
import com.example.ui.theme.TextParchmentMuted

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotebookDrawer(
  checkpoint: CheckpointEntity?,
  allCheckpoints: List<CheckpointEntity> = emptyList(),
  adultContentEnabled: Boolean = true,
  onClose: () -> Unit,
  onOpenManualEdit: () -> Unit,
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
      .background(SlateDark900)
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
          tint = AmberGoldPrimary,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = "Das Notizbuch",
            style = MaterialTheme.typography.titleLarge,
            color = TextParchment,
            fontFamily = FontFamily.Serif
          )
          Text(
            text = "Aktiver Checkpoint & Langzeitgedächtnis",
            style = MaterialTheme.typography.labelSmall,
            color = TextParchmentFaint
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
            contentDescription = "Zustand manuell bearbeiten",
            tint = AmberGoldPrimary
          )
        }
        IconButton(
          onClick = onClose,
          modifier = Modifier.testTag("close_notebook_button")
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Schließen",
            tint = TextParchmentMuted
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 3 View Tabs
    TabRow(
      selectedTabIndex = selectedTab,
      containerColor = SlateDark950,
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
            text = "CHRONIK DER TAGE",
            style = MaterialTheme.typography.labelSmall,
            color = AmberGoldLight,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(8.dp))

          if (dayGroups.isEmpty()) {
            Card(
              colors = CardDefaults.cardColors(containerColor = SlateDark800),
              shape = RoundedCornerShape(12.dp)
            ) {
              Box(modifier = Modifier.padding(16.dp)) {
                Text(
                  text = "Die Geschichte hat gerade erst begonnen. Tage und Erlebnisse werden hier chronologisch archiviert.",
                  style = MaterialTheme.typography.bodySmall,
                  color = TextParchmentMuted
                )
              }
            }
          } else {
            dayGroups.forEach { (dayNumber, dayCheckpoints) ->
              val latestInDay = dayCheckpoints.firstOrNull() ?: checkpoint
              val isCurrentDay = latestInDay?.extractDayNumber() == (checkpoint?.extractDayNumber() ?: 1)

              Card(
                colors = CardDefaults.cardColors(
                  containerColor = if (isCurrentDay) SlateDark800 else SlateDark950
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                  1.dp,
                  if (isCurrentDay) AmberGoldPrimary.copy(alpha = 0.6f) else SlateDark700
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
                        color = if (isCurrentDay) AmberGoldContainer else SlateDark700,
                        shape = RoundedCornerShape(6.dp)
                      ) {
                        Text(
                          text = "TAG $dayNumber",
                          style = MaterialTheme.typography.labelSmall,
                          color = if (isCurrentDay) AmberGoldLight else TextParchmentMuted,
                          fontWeight = FontWeight.Bold,
                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                      }
                      if (isCurrentDay) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                          text = "(Aktuell)",
                          style = MaterialTheme.typography.labelSmall,
                          color = AmberGoldLight
                        )
                      }
                    }

                    Text(
                      text = latestInDay?.inGameTime ?: "",
                      style = MaterialTheme.typography.labelSmall,
                      color = TextParchmentMuted
                    )
                  }

                  Spacer(modifier = Modifier.height(10.dp))

                  // Location & Weather for this day
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Default.LocationOn,
                      contentDescription = null,
                      tint = AmberGoldPrimary,
                      modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = latestInDay?.location ?: "Unbekannter Ort",
                      style = MaterialTheme.typography.bodySmall,
                      fontWeight = FontWeight.SemiBold,
                      color = TextParchment
                    )
                  }

                  Spacer(modifier = Modifier.height(4.dp))

                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Default.WbCloudy,
                      contentDescription = null,
                      tint = TextParchmentMuted,
                      modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = latestInDay?.weather ?: "Ruhig",
                      style = MaterialTheme.typography.bodySmall,
                      color = TextParchmentMuted
                    )
                  }

                  Spacer(modifier = Modifier.height(10.dp))

                  // Day Events Summary
                  val daySummary = latestInDay?.previousEventsSummary?.ifBlank { null }
                  if (daySummary != null) {
                    Text(
                      text = daySummary,
                      style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                      color = TextParchment
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
            adultContentEnabled = adultContentEnabled
          )

          Spacer(modifier = Modifier.height(16.dp))

          // Detailed NPC list
          SectionHeader(title = "Bekannte NPCs (${npcs.size})", icon = Icons.Default.Face)

          if (npcs.isEmpty()) {
            Card(
              colors = CardDefaults.cardColors(containerColor = SlateDark800),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Box(modifier = Modifier.padding(14.dp)) {
                Text(
                  text = "Noch keine NPCs erfasst. Die KI ergänzt automatisch Charaktere, wenn sie auftauchen.",
                  style = MaterialTheme.typography.bodySmall,
                  color = TextParchmentMuted
                )
              }
            }
          } else {
            npcs.forEach { npc ->
              Card(
                colors = CardDefaults.cardColors(containerColor = SlateDark800),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, SlateDark600),
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
                      color = AmberGoldPrimary,
                      fontWeight = FontWeight.Bold
                    )
                    Surface(
                      color = SlateDark700,
                      shape = RoundedCornerShape(6.dp)
                    ) {
                      Text(
                        text = npc.currentMood,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextParchmentMuted,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "Kleidung: ${npc.outfit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextParchment
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = "Beziehung: ${npc.relationshipToPlayer}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextParchmentMuted
                  )
                }
              }
            }
          }
        }

        2 -> {
          // --- TAB 2: WORLD & INVENTORY ---
          SectionHeader(title = "Aktueller Aufenthaltsort", icon = Icons.Default.LocationOn)

          Card(
            colors = CardDefaults.cardColors(containerColor = SlateDark800),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, SlateDark600),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              InfoRow(
                icon = Icons.Default.AccessTime,
                label = "In-Game Zeit",
                value = checkpoint?.inGameTime ?: "Tag 1, 20:00 Uhr"
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
                label = "Wetter / Atmosphäre",
                value = checkpoint?.weather ?: "Ruhig"
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Inventory
          SectionHeader(title = "Inventar (${inventory.size})", icon = Icons.Default.Luggage)

          Card(
            colors = CardDefaults.cardColors(containerColor = SlateDark800),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, SlateDark600),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              if (inventory.isEmpty()) {
                Text(
                  text = "Keine Gegenstände im Inventar",
                  style = MaterialTheme.typography.bodySmall,
                  color = TextParchmentFaint
                )
              } else {
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  inventory.forEach { item ->
                    Surface(
                      color = SlateDark700,
                      shape = RoundedCornerShape(8.dp),
                      border = BorderStroke(1.dp, SlateDark600)
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                      ) {
                        Icon(
                          imageVector = Icons.Default.Luggage,
                          contentDescription = null,
                          tint = AmberGoldPrimary,
                          modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                          text = item,
                          style = MaterialTheme.typography.labelSmall,
                          color = TextParchment
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
            colors = CardDefaults.cardColors(containerColor = SlateDark800),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, SlateDark600),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              if (milestones.isEmpty()) {
                Text(
                  text = "Noch keine tiefen Meilensteine verzeichnet. Die KI wird prägende Schwüre, Verluste und Wendepunkte hier dauerhaft verankern.",
                  style = MaterialTheme.typography.bodySmall,
                  color = TextParchmentFaint
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
                      color = AmberGoldPrimary,
                      fontWeight = FontWeight.Bold
                    )
                    Text(
                      text = milestone,
                      style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                      color = TextParchment
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
      Button(
        onClick = onOpenManualEdit,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("manual_edit_button"),
        colors = ButtonDefaults.buttonColors(
          containerColor = AmberGoldPrimary,
          contentColor = SlateDark900
        ),
        shape = RoundedCornerShape(10.dp)
      ) {
        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Notizbuch manuell korrigieren", fontWeight = FontWeight.Bold)
      }

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
      tint = AmberGoldPrimary,
      modifier = Modifier.size(16.dp)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      color = TextParchment,
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
      tint = AmberGoldPrimary.copy(alpha = 0.8f),
      modifier = Modifier.size(16.dp)
    )
    Spacer(modifier = Modifier.width(8.dp))
    Column {
      Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = TextParchmentFaint,
        fontSize = 10.sp
      )
      Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium,
        color = TextParchment
      )
    }
  }
}
