package com.example.ui.dna

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * DnaNumberStepper - MauntingStudios Design-DNA Stepper-Komponente.
 * Exakte Portierung von NumberStepper.tsx aus @maunting/design-dna nach Jetpack Compose:
 *
 * - Abgerundeter Elevation-Container (SurfaceContainerHigh) mit Ice-Border
 * - Ergonomische Minus- und Plus-Schaltflaeche mit haptischem State und Disabled-Handling
 * - Praezise Eingabe und Anzeige in JetBrains Mono / Inter
 * - Min-, Max- und Step-Begrenzung
 */
@Composable
fun DnaNumberStepper(
  value: Int,
  onValueChange: (Int) -> Unit,
  min: Int,
  max: Int,
  step: Int = 1,
  label: String? = null,
  unit: String? = null,
  enabled: Boolean = true,
  modifier: Modifier = Modifier,
  testTag: String = "dna_number_stepper"
) {
  val canDecrease = enabled && value > min
  val canIncrease = enabled && value < max

  Column(modifier = modifier) {
    if (label != null) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = label,
          fontFamily = DnaTypography.InterFamily,
          fontWeight = FontWeight.Medium,
          fontSize = 12.sp,
          color = DnaColors.OnSurfaceVariant
        )
        if (unit != null) {
          Text(
            text = "$value $unit",
            fontFamily = DnaTypography.JetBrainsMonoFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = DnaColors.Primary
          )
        }
      }
    }

    Surface(
      shape = RoundedCornerShape(10.dp),
      color = DnaColors.SurfaceContainerHigh,
      border = BorderStroke(1.dp, DnaColors.Border),
      modifier = Modifier
        .fillMaxWidth()
        .height(44.dp)
        .testTag(testTag)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Minus Button
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .width(44.dp)
            .height(44.dp)
            .clickable(enabled = canDecrease) {
              val next = (value - step).coerceIn(min, max)
              onValueChange(next)
            }
            .background(if (canDecrease) DnaColors.SurfaceContainerHighest.copy(alpha = 0.4f) else DnaColors.SurfaceDim)
        ) {
          Icon(
            imageVector = Icons.Default.Remove,
            contentDescription = "Verringern",
            tint = if (canDecrease) DnaColors.OnSurface else DnaColors.TextDisabled,
            modifier = Modifier.size(18.dp)
          )
        }

        // Value Display
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .weight(1f)
            .padding(horizontal = 8.dp)
        ) {
          Text(
            text = if (unit != null && label == null) "$value $unit" else "$value",
            fontFamily = DnaTypography.JetBrainsMonoFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = if (enabled) DnaColors.OnSurface else DnaColors.TextDisabled,
            textAlign = TextAlign.Center
          )
        }

        // Plus Button
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .width(44.dp)
            .height(44.dp)
            .clickable(enabled = canIncrease) {
              val next = (value + step).coerceIn(min, max)
              onValueChange(next)
            }
            .background(if (canIncrease) DnaColors.SurfaceContainerHighest.copy(alpha = 0.4f) else DnaColors.SurfaceDim)
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Erhoehen",
            tint = if (canIncrease) DnaColors.OnSurface else DnaColors.TextDisabled,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}

/**
 * Float-Ueberladung des DnaNumberSteppers fuer Parameter wie Temperature (0.0 bis 2.0).
 */
@Composable
fun DnaFloatNumberStepper(
  value: Float,
  onValueChange: (Float) -> Unit,
  min: Float = 0.0f,
  max: Float = 2.0f,
  step: Float = 0.1f,
  label: String? = null,
  unit: String? = null,
  enabled: Boolean = true,
  modifier: Modifier = Modifier,
  testTag: String = "dna_float_number_stepper"
) {
  val canDecrease = enabled && value > min + 0.001f
  val canIncrease = enabled && value < max - 0.001f

  Column(modifier = modifier) {
    if (label != null) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = label,
          fontFamily = DnaTypography.InterFamily,
          fontWeight = FontWeight.Medium,
          fontSize = 12.sp,
          color = DnaColors.OnSurfaceVariant
        )
        Text(
          text = String.format("%.1f", value) + (if (unit != null) " $unit" else ""),
          fontFamily = DnaTypography.JetBrainsMonoFamily,
          fontWeight = FontWeight.SemiBold,
          fontSize = 12.sp,
          color = DnaColors.Primary
        )
      }
    }

    Surface(
      shape = RoundedCornerShape(10.dp),
      color = DnaColors.SurfaceContainerHigh,
      border = BorderStroke(1.dp, DnaColors.Border),
      modifier = Modifier
        .fillMaxWidth()
        .height(44.dp)
        .testTag(testTag)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Minus Button
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .width(44.dp)
            .height(44.dp)
            .clickable(enabled = canDecrease) {
              val raw = ((value - step) * 10f).roundToInt() / 10f
              val next = raw.coerceIn(min, max)
              onValueChange(next)
            }
            .background(if (canDecrease) DnaColors.SurfaceContainerHighest.copy(alpha = 0.4f) else DnaColors.SurfaceDim)
        ) {
          Icon(
            imageVector = Icons.Default.Remove,
            contentDescription = "Verringern",
            tint = if (canDecrease) DnaColors.OnSurface else DnaColors.TextDisabled,
            modifier = Modifier.size(18.dp)
          )
        }

        // Value Display
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .weight(1f)
            .padding(horizontal = 8.dp)
        ) {
          Text(
            text = String.format("%.1f", value) + (if (unit != null && label == null) " $unit" else ""),
            fontFamily = DnaTypography.JetBrainsMonoFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = if (enabled) DnaColors.OnSurface else DnaColors.TextDisabled,
            textAlign = TextAlign.Center
          )
        }

        // Plus Button
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .width(44.dp)
            .height(44.dp)
            .clickable(enabled = canIncrease) {
              val raw = ((value + step) * 10f).roundToInt() / 10f
              val next = raw.coerceIn(min, max)
              onValueChange(next)
            }
            .background(if (canIncrease) DnaColors.SurfaceContainerHighest.copy(alpha = 0.4f) else DnaColors.SurfaceDim)
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Erhoehen",
            tint = if (canIncrease) DnaColors.OnSurface else DnaColors.TextDisabled,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}
