package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.theme.AmberGoldPrimary
import com.example.ui.theme.OnAmberGoldContainer
import com.example.ui.theme.SlateDark600
import com.example.ui.theme.SlateDark700
import com.example.ui.theme.SlateDark800
import com.example.ui.theme.SlateDark900
import com.example.ui.theme.TextParchment
import com.example.ui.theme.TextParchmentFaint
import com.example.ui.theme.TextParchmentMuted

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotebookDrawer(
  checkpoint: CheckpointEntity?,
  onClose: () -> Unit,
  onOpenManualEdit: () -> Unit,
  modifier: Modifier = Modifier
) {
  val npcs = checkpoint?.getNpcList() ?: emptyList()
  val inventory = checkpoint?.getInventoryList() ?: emptyList()

  Column(
    modifier = modifier
      .fillMaxHeight()
      .background(SlateDark900)
      .padding(horizontal = 16.dp, vertical = 12.dp)
      .verticalScroll(rememberScrollState())
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.MenuBook,
          contentDescription = null,
          tint = AmberGoldPrimary,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "Das Notizbuch",
          style = MaterialTheme.typography.titleLarge,
          color = TextParchment,
          fontFamily = FontFamily.Serif
        )
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

    Text(
      text = "Aktiver Checkpoint & Kontext-Anker für Gemini",
      style = MaterialTheme.typography.labelSmall,
      color = TextParchmentFaint
    )

    Spacer(modifier = Modifier.height(14.dp))
    HorizontalDivider(color = SlateDark700)
    Spacer(modifier = Modifier.height(14.dp))

    // Section 1: In-Game Time & Environment
    SectionHeader(title = "Zeit & Umwelt", icon = Icons.Default.AccessTime)

    Card(
      colors = CardDefaults.cardColors(containerColor = SlateDark800),
      shape = RoundedCornerShape(12.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, SlateDark600)
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        InfoRow(
          icon = Icons.Default.AccessTime,
          label = "In-Game Zeit",
          value = checkpoint?.inGameTime ?: "Tag 1, 21:30 Uhr"
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

    Spacer(modifier = Modifier.height(18.dp))

    // Section 2: Player Status
    SectionHeader(title = "Dein Charakter", icon = Icons.Default.Person)

    Card(
      colors = CardDefaults.cardColors(containerColor = SlateDark800),
      shape = RoundedCornerShape(12.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, SlateDark600)
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Text(
          text = "AKTUELLES OUTFIT",
          style = MaterialTheme.typography.labelSmall,
          color = AmberGoldPrimary,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = checkpoint?.playerOutfit?.ifBlank { "Keine Kleidung angegeben" }
            ?: "Keine Kleidung angegeben",
          style = MaterialTheme.typography.bodyMedium,
          color = TextParchment
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "KÖRPERLICHER ZUSTAND",
          style = MaterialTheme.typography.labelSmall,
          color = AmberGoldPrimary,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = checkpoint?.playerCondition?.ifBlank { "Unverletzt" } ?: "Unverletzt",
          style = MaterialTheme.typography.bodyMedium,
          color = TextParchment
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "INVENTAR",
          style = MaterialTheme.typography.labelSmall,
          color = AmberGoldPrimary,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))

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
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateDark600)
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

    Spacer(modifier = Modifier.height(18.dp))

    // Section 3: NPCs
    SectionHeader(title = "Bekannte NPCs (${npcs.size})", icon = Icons.Default.Face)

    if (npcs.isEmpty()) {
      Card(
        colors = CardDefaults.cardColors(containerColor = SlateDark800),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateDark600),
        modifier = Modifier.fillMaxWidth()
      ) {
        Box(modifier = Modifier.padding(16.dp)) {
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
          border = androidx.compose.foundation.BorderStroke(1.dp, SlateDark600),
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
                style = MaterialTheme.typography.titleMedium,
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

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = "Outfit: ${npc.outfit}",
              style = MaterialTheme.typography.bodySmall,
              color = TextParchment
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Beziehung: ${npc.relationshipToPlayer}",
              style = MaterialTheme.typography.bodySmall,
              color = TextParchmentMuted
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    // Section 4: Milestones & Long-Term Memories
    val milestones = checkpoint?.getMilestonesList() ?: emptyList()
    SectionHeader(title = "Bedeutsame Meilensteine", icon = Icons.Default.Book)

    Card(
      colors = CardDefaults.cardColors(containerColor = SlateDark800),
      shape = RoundedCornerShape(12.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, SlateDark600)
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

    Spacer(modifier = Modifier.height(18.dp))

    // Section 5: Episodic Summary
    SectionHeader(title = "Was bisher geschah", icon = Icons.Default.MenuBook)

    Card(
      colors = CardDefaults.cardColors(containerColor = SlateDark800),
      shape = RoundedCornerShape(12.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, SlateDark600)
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Text(
          text = checkpoint?.previousEventsSummary?.ifBlank { "Die Geschichte hat gerade erst begonnen." }
            ?: "Die Geschichte hat gerade erst begonnen.",
          style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
          color = TextParchmentMuted
        )
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

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

    Spacer(modifier = Modifier.height(24.dp))
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
