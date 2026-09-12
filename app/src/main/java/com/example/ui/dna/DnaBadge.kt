package com.example.ui.dna

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Zulaessige Farbmodi fuer DnaBadges.
 */
enum class DnaBadgeTone {
  ICE,
  MINT,
  AMBER,
  DANGER,
  NEUTRAL
}

/**
 * Stadium-Status-Pille des MauntingStudios Design-Systems.
 * Zur einheitlichen Darstellung von Zuständen, Zeitanzeigen und Kategorien.
 */
@Composable
fun DnaBadge(
  text: String,
  modifier: Modifier = Modifier,
  tone: DnaBadgeTone = DnaBadgeTone.ICE,
  icon: ImageVector? = null,
  showDot: Boolean = true,
  /** JetBrains Mono für Zeitstempel, Würfelwürfe und IDs (CLAUDE.md §0.5). */
  fontFamily: FontFamily = DnaTypography.InterFamily
) {
  val (bgColor, borderColor, contentColor, dotColor) = when (tone) {
    DnaBadgeTone.ICE -> Quad(
      DnaColors.PrimaryContainer.copy(alpha = 0.35f),
      DnaColors.Border,
      DnaColors.Primary,
      DnaColors.Primary
    )
    DnaBadgeTone.MINT -> Quad(
      DnaColors.SecondaryContainer.copy(alpha = 0.25f),
      DnaColors.Secondary.copy(alpha = 0.35f),
      DnaColors.MintAccent,
      DnaColors.Secondary
    )
    DnaBadgeTone.AMBER -> Quad(
      DnaColors.StatusWarning.copy(alpha = 0.15f),
      DnaColors.StatusWarning.copy(alpha = 0.35f),
      DnaColors.StatusWarning,
      DnaColors.StatusWarning
    )
    DnaBadgeTone.DANGER -> Quad(
      DnaColors.StatusDestructive.copy(alpha = 0.15f),
      DnaColors.StatusDestructive.copy(alpha = 0.4f),
      DnaColors.StatusDestructive,
      DnaColors.StatusDestructive
    )
    DnaBadgeTone.NEUTRAL -> Quad(
      DnaColors.SurfaceContainerHighest.copy(alpha = 0.5f),
      DnaColors.Border,
      DnaColors.OnSurfaceVariant,
      DnaColors.OnSurfaceVariant
    )
  }

  Surface(
    shape = CircleShape,
    color = bgColor,
    border = BorderStroke(1.dp, borderColor),
    modifier = modifier
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
      if (showDot) {
        Box(
          modifier = Modifier
            .padding(end = 6.dp)
            .size(6.dp)
            .background(dotColor, CircleShape)
        )
      }

      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = contentColor,
          modifier = Modifier
            .padding(end = 5.dp)
            .size(13.dp)
        )
      }

      Text(
        text = text,
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 0.2.sp,
        color = contentColor
      )
    }
  }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
