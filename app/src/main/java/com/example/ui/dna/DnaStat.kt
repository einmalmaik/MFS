package com.example.ui.dna

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Eine Kennzahl, die gesehen werden soll.
 *
 * Gedacht für Werte, auf die man stolz ist oder an denen man sich orientiert — die Spielzeit
 * einer Geschichte, der erreichte Tag. Der Wert ist das Größte im Block; die Beschriftung
 * darunter ist bewusst klein und ruhig, damit sie erklärt statt abzulenken.
 *
 * Zahlen stehen in JetBrains Mono (CLAUDE.md §0.5) und damit in gleichbleibender Breite:
 * "3h 38m" springt beim Weiterzählen nicht, was die Anzeige ruhig wirken lässt.
 */
@Composable
fun DnaStat(
  value: String,
  label: String,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  emphasized: Boolean = false
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = if (emphasized) DnaColors.PrimaryContainer.copy(alpha = 0.45f) else DnaColors.SurfaceContainerHigh,
    border = BorderStroke(
      1.dp,
      if (emphasized) DnaColors.Primary.copy(alpha = 0.40f) else DnaColors.Border
    ),
    modifier = modifier
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (emphasized) DnaColors.Primary else DnaColors.OnSurfaceVariant,
          modifier = Modifier.size(18.dp)
        )
      }

      Column {
        Text(
          text = value,
          fontFamily = DnaTypography.JetBrainsMonoFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp,
          color = if (emphasized) DnaColors.Primary else DnaColors.OnSurface
        )
        Text(
          text = label,
          fontFamily = DnaTypography.InterFamily,
          fontWeight = FontWeight.Medium,
          fontSize = 9.sp,
          letterSpacing = 0.8.sp,
          color = DnaColors.OnSurfaceVariant
        )
      }
    }
  }
}

/**
 * Formatiert Sekunden als Spielzeit.
 *
 * Getrennt von der Darstellung, damit die Regel an einer Stelle steht: Der Wert tauchte bisher
 * in Navigationsleiste und Auswahldialog doppelt und leicht unterschiedlich auf.
 */
fun formatPlayTime(seconds: Long): String {
  val hours = seconds / 3600
  val minutes = (seconds % 3600) / 60
  return when {
    hours > 0 -> "${hours}h ${minutes}m"
    minutes > 0 -> "${minutes}m"
    else -> "< 1m"
  }
}
