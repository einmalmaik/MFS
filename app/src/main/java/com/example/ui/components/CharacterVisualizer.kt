package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CheckpointEntity
import com.example.data.model.NpcInfo
import com.example.ui.theme.AmberGoldContainer
import com.example.ui.theme.AmberGoldLight
import com.example.ui.theme.AmberGoldPrimary
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.SlateDark600
import com.example.ui.theme.SlateDark700
import com.example.ui.theme.SlateDark800
import com.example.ui.theme.SlateDark900
import com.example.ui.theme.SlateDark950
import com.example.ui.theme.TextParchment
import com.example.ui.theme.TextParchmentFaint
import com.example.ui.theme.TextParchmentMuted

/**
 * Interactive RPG Character & Gear Visualizer with stylized 2.5D mannequin display,
 * active character switching (Player + current active NPCs), and full Adult/NSFW support.
 */
@Composable
fun CharacterVisualizer(
  checkpoint: CheckpointEntity?,
  adultContentEnabled: Boolean = true,
  modifier: Modifier = Modifier
) {
  val npcs = checkpoint?.getNpcList() ?: emptyList()
  var selectedCharacterIndex by remember { mutableStateOf(0) } // 0 = Player, 1..N = NPCs

  val isPlayer = selectedCharacterIndex == 0
  val currentNpc = if (!isPlayer && selectedCharacterIndex - 1 < npcs.size) {
    npcs[selectedCharacterIndex - 1]
  } else null

  val characterName = if (isPlayer) "Dein Charakter" else (currentNpc?.name ?: "NPC")
  val characterOutfit = if (isPlayer) {
    checkpoint?.playerOutfit?.ifBlank { "Standard-Zivilkleidung" } ?: "Standard-Zivilkleidung"
  } else {
    currentNpc?.outfit?.ifBlank { "Passende Zivilkleidung" } ?: "Passende Zivilkleidung"
  }

  val characterCondition = if (isPlayer) {
    checkpoint?.playerCondition?.ifBlank { "Unverletzt" } ?: "Unverletzt"
  } else {
    currentNpc?.currentMood ?: "Ruhig"
  }

  val relationship = currentNpc?.relationshipToPlayer ?: "Protagonist"

  val isAdultOutfit = remember(characterOutfit) {
    val lower = characterOutfit.lowercase()
    lower.contains("dessous") || lower.contains("nackt") || lower.contains("entblößt") ||
      lower.contains("freizügig") || lower.contains("zerrissen") || lower.contains("reizwäsche") ||
      lower.contains("knapp") || lower.contains("oben ohne") || lower.contains("transparent")
  }

  Card(
    colors = CardDefaults.cardColors(containerColor = SlateDark800),
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.dp, if (isAdultOutfit) CrimsonDanger.copy(alpha = 0.6f) else AmberGoldPrimary.copy(alpha = 0.4f)),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      // Top header with Title & Adult Mode Badge
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = AmberGoldPrimary,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Charakter & Ausrüstung",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextParchment,
            fontFamily = FontFamily.Serif
          )
        }

        if (adultContentEnabled) {
          Surface(
            color = if (isAdultOutfit) CrimsonDanger.copy(alpha = 0.2f) else SlateDark700,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, if (isAdultOutfit) CrimsonDanger else SlateDark600)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = if (isAdultOutfit) CrimsonDanger else AmberGoldPrimary,
                modifier = Modifier.size(12.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (isAdultOutfit) "Adult / Freizügig" else "18+ Adult Aktiv",
                style = MaterialTheme.typography.labelSmall,
                color = if (isAdultOutfit) CrimsonDanger else TextParchmentMuted,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Character Tabs: Player + currently active NPCs for this day
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Player Chip
        CharacterChip(
          name = "Du (Spieler)",
          isSelected = selectedCharacterIndex == 0,
          onClick = { selectedCharacterIndex = 0 }
        )

        // NPC Chips
        npcs.forEachIndexed { index, npc ->
          CharacterChip(
            name = npc.name,
            isSelected = selectedCharacterIndex == index + 1,
            onClick = { selectedCharacterIndex = index + 1 }
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Main Visual Stage: 2.5D Stylized Mannequin & Gear Slots
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(SlateDark950)
          .border(1.dp, SlateDark700, RoundedCornerShape(12.dp))
          .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Left: Stylized Silhouette Canvas Display
        Box(
          modifier = Modifier
            .size(width = 110.dp, height = 180.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SlateDark900),
          contentAlignment = Alignment.Center
        ) {
          AvatarSilhouetteCanvas(
            isAdult = isAdultOutfit,
            condition = characterCondition
          )

          // Glowing In-Game Time tag at bottom of avatar
          Text(
            text = checkpoint?.inGameTime ?: "Tag 1",
            style = MaterialTheme.typography.labelSmall,
            color = AmberGoldLight,
            fontSize = 9.sp,
            modifier = Modifier
              .align(Alignment.BottomCenter)
              .padding(bottom = 6.dp)
              .background(SlateDark950.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
              .padding(horizontal = 4.dp, vertical = 2.dp)
          )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Right: Gear & State Breakdown
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = characterName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AmberGoldPrimary
          )

          if (!isPlayer) {
            Text(
              text = "Beziehung: $relationship",
              style = MaterialTheme.typography.labelSmall,
              color = TextParchmentMuted
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          // Outfit Box
          Text(
            text = "AKTUELLES OUTFIT",
            style = MaterialTheme.typography.labelSmall,
            color = if (isAdultOutfit) CrimsonDanger else AmberGoldLight,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
          )
          Text(
            text = characterOutfit,
            style = MaterialTheme.typography.bodySmall,
            color = TextParchment,
            maxLines = 4
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Condition Box
          Text(
            text = if (isPlayer) "VERFASSUNG" else "STIMMUNG",
            style = MaterialTheme.typography.labelSmall,
            color = AmberGoldLight,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
          )
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = if (characterCondition.contains("verletzt", ignoreCase = true)) Icons.Default.Warning else Icons.Default.CheckCircle,
              contentDescription = null,
              tint = if (characterCondition.contains("verletzt", ignoreCase = true)) CrimsonDanger else AmberGoldPrimary,
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = characterCondition,
              style = MaterialTheme.typography.bodySmall,
              color = TextParchmentMuted
            )
          }
        }
      }
    }
  }
}

@Composable
private fun CharacterChip(
  name: String,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Surface(
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .clickable { onClick() }
      .testTag("char_chip_$name"),
    color = if (isSelected) AmberGoldContainer else SlateDark700,
    border = BorderStroke(1.dp, if (isSelected) AmberGoldPrimary else SlateDark600)
  ) {
    Text(
      text = name,
      style = MaterialTheme.typography.labelSmall,
      color = if (isSelected) AmberGoldLight else TextParchmentMuted,
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
    )
  }
}

/**
 * Custom Canvas rendering a stylized silhouette with glowing aura and gear markers.
 */
@Composable
private fun AvatarSilhouetteCanvas(
  isAdult: Boolean,
  condition: String
) {
  val primaryColor = if (isAdult) CrimsonDanger else AmberGoldPrimary
  val auraColor = if (isAdult) CrimsonDanger.copy(alpha = 0.25f) else AmberGoldPrimary.copy(alpha = 0.2f)

  Canvas(modifier = Modifier.fillMaxWidth()) {
    val centerX = size.width / 2f
    val centerY = size.height / 2f

    // Ambient glow circle
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(auraColor, Color.Transparent),
        center = Offset(centerX, centerY),
        radius = size.width * 0.7f
      ),
      center = Offset(centerX, centerY),
      radius = size.width * 0.7f
    )

    // Stylized Head
    drawCircle(
      color = primaryColor.copy(alpha = 0.85f),
      center = Offset(centerX, size.height * 0.22f),
      radius = 16.dp.toPx()
    )

    // Stylized Torso
    val torsoPath = Path().apply {
      moveTo(centerX - 20.dp.toPx(), size.height * 0.35f)
      lineTo(centerX + 20.dp.toPx(), size.height * 0.35f)
      lineTo(centerX + 14.dp.toPx(), size.height * 0.65f)
      lineTo(centerX - 14.dp.toPx(), size.height * 0.65f)
      close()
    }
    drawPath(
      path = torsoPath,
      color = primaryColor.copy(alpha = if (isAdult) 0.5f else 0.8f)
    )

    // Stylized Legs
    drawLine(
      color = primaryColor.copy(alpha = 0.75f),
      start = Offset(centerX - 8.dp.toPx(), size.height * 0.65f),
      end = Offset(centerX - 12.dp.toPx(), size.height * 0.90f),
      strokeWidth = 6.dp.toPx()
    )
    drawLine(
      color = primaryColor.copy(alpha = 0.75f),
      start = Offset(centerX + 8.dp.toPx(), size.height * 0.65f),
      end = Offset(centerX + 12.dp.toPx(), size.height * 0.90f),
      strokeWidth = 6.dp.toPx()
    )

    // Outer framing box outline
    drawRoundRect(
      color = primaryColor.copy(alpha = 0.3f),
      topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
      size = Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx()),
      cornerRadius = CornerRadius(6.dp.toPx()),
      style = Stroke(width = 1.dp.toPx())
    )
  }
}
