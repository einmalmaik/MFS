package com.example.ui.dna

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Zulaessige visuelle Varianten fuer MauntingStudios Buttons.
 */
enum class DnaButtonVariant {
  PRIMARY,
  SECONDARY,
  DESTRUCTIVE,
  GHOST
}

/**
 * Zentrale Button-Komponente des MauntingStudios Design-Systems.
 *
 * Implementiert die zulaessigen Varianten:
 * - PRIMARY: Helles Logo Ice-Cyan mit leichtem Mint-Verlauf und dunklem Text
 * - SECONDARY: Abgedunkelte Container-Flaeche mit feiner Umrandung
 * - DESTRUCTIVE: Rote Gefahr-Faerbung fuer irreversible Aktionen
 * - GHOST: Reine Text-/Icon-Aktion ohne Flaeche
 *
 * Garantiert eine zugaengliche Mindest-Touchflaeche von 48dp.
 */
@Composable
fun DnaButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  variant: DnaButtonVariant = DnaButtonVariant.PRIMARY,
  icon: ImageVector? = null,
  trailingIcon: ImageVector? = null,
  enabled: Boolean = true,
  loading: Boolean = false,
  fullWidth: Boolean = false,
  testTag: String = "dna_button"
) {
  val shape = RoundedCornerShape(12.dp)

  val borderStroke = when {
    !enabled -> BorderStroke(1.dp, DnaColors.Border.copy(alpha = 0.3f))
    variant == DnaButtonVariant.SECONDARY -> BorderStroke(1.dp, DnaColors.Border)
    variant == DnaButtonVariant.DESTRUCTIVE -> BorderStroke(1.dp, DnaColors.StatusDestructive.copy(alpha = 0.35f))
    else -> null
  }

  val backgroundColor = when {
    !enabled -> DnaColors.SurfaceContainerHighest.copy(alpha = 0.4f)
    variant == DnaButtonVariant.PRIMARY -> Color.Transparent // Nutzt Gradient
    variant == DnaButtonVariant.SECONDARY -> DnaColors.SurfaceContainerHigh
    variant == DnaButtonVariant.DESTRUCTIVE -> DnaColors.StatusDestructive.copy(alpha = 0.12f)
    variant == DnaButtonVariant.GHOST -> Color.Transparent
    else -> Color.Transparent
  }

  val contentColor = when {
    !enabled -> DnaColors.TextDisabled
    variant == DnaButtonVariant.PRIMARY -> DnaColors.PrimaryForeground
    variant == DnaButtonVariant.SECONDARY -> DnaColors.OnSurface
    variant == DnaButtonVariant.DESTRUCTIVE -> Color(0xFFFECDD3)
    variant == DnaButtonVariant.GHOST -> DnaColors.OnSurfaceVariant
    else -> DnaColors.OnSurface
  }

  Surface(
    onClick = { if (enabled && !loading) onClick() },
    enabled = enabled && !loading,
    shape = shape,
    color = backgroundColor,
    border = borderStroke,
    modifier = modifier
      .testTag(testTag)
      .defaultMinSize(minHeight = 48.dp)
      .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
  ) {
    Box(
      modifier = Modifier
        .then(
          if (variant == DnaButtonVariant.PRIMARY && enabled) {
            Modifier.background(DnaColors.PrimaryButtonBrush, shape = shape)
          } else {
            Modifier
          }
        )
        .padding(horizontal = 16.dp, vertical = 10.dp),
      contentAlignment = Alignment.Center
    ) {
      if (loading) {
        CircularProgressIndicator(
          modifier = Modifier.size(20.dp),
          color = contentColor,
          strokeWidth = 2.dp
        )
      } else {
        Row(
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (icon != null) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = contentColor,
              modifier = Modifier
                .padding(end = 8.dp)
                .size(18.dp)
            )
          }

          Text(
            text = text,
            fontFamily = DnaTypography.InterFamily,
            fontWeight = if (variant == DnaButtonVariant.PRIMARY) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 14.sp,
            color = contentColor
          )

          if (trailingIcon != null) {
            Icon(
              imageVector = trailingIcon,
              contentDescription = null,
              tint = contentColor,
              modifier = Modifier
                .padding(start = 8.dp)
                .size(18.dp)
            )
          }
        }
      }
    }
  }
}
