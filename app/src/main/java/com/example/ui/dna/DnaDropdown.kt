package com.example.ui.dna

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Modell fuer eine zulaessige Option im DnaDropdown.
 */
data class DnaDropdownOption<T>(
  val value: T,
  val label: String,
  val hint: String? = null,
  val icon: ImageVector? = null,
  val enabled: Boolean = true
)

/**
 * Zentrale typisierte Dropdown-Komponente des MauntingStudios Design-Systems.
 *
 * Entspricht der Implementierung in 'Dropdown.tsx' aus dem MSM-Repository:
 * - Keine nativen Formular-/Picker-Elemente
 * - Gestylte Trigger-Flaeche (SurfaceContainerHigh mit Border)
 * - Rotierender Chevron-Indikator
 * - Popover mit optischer Rueckmeldung, Hinweistexten und Selektionshaekchen
 */
@Composable
fun <T> DnaDropdown(
  options: List<DnaDropdownOption<T>>,
  selectedValue: T,
  onOptionSelected: (T) -> Unit,
  modifier: Modifier = Modifier,
  label: String? = null,
  placeholder: String = "Option wählen...",
  icon: ImageVector? = null,
  enabled: Boolean = true,
  testTag: String = "dna_dropdown"
) {
  var expanded by remember { mutableStateOf(false) }
  val currentOption = options.find { it.value == selectedValue }
  val rotationState by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "chevron_rotation")

  Column(modifier = modifier) {
    if (label != null) {
      Text(
        text = label,
        fontFamily = DnaTypography.InterFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        color = DnaColors.OnSurfaceVariant,
        modifier = Modifier.padding(bottom = 6.dp)
      )
    }

    Box(modifier = Modifier.fillMaxWidth()) {
      Surface(
        onClick = { if (enabled) expanded = !expanded },
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        color = DnaColors.SurfaceContainerHigh,
        border = BorderStroke(
          width = 1.dp,
          color = if (expanded) DnaColors.Primary else DnaColors.Border
        ),
        modifier = Modifier
          .fillMaxWidth()
          .defaultMinSize(minHeight = 48.dp)
          .testTag(testTag)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
          ) {
            val displayIcon = currentOption?.icon ?: icon
            if (displayIcon != null) {
              Icon(
                imageVector = displayIcon,
                contentDescription = null,
                tint = if (enabled) DnaColors.Primary else DnaColors.TextDisabled,
                modifier = Modifier
                  .padding(end = 10.dp)
                  .size(18.dp)
              )
            }

            Text(
              text = currentOption?.label ?: placeholder,
              fontFamily = DnaTypography.InterFamily,
              fontWeight = FontWeight.Normal,
              fontSize = 14.sp,
              color = when {
                !enabled -> DnaColors.TextDisabled
                currentOption != null -> DnaColors.OnSurface
                else -> DnaColors.OnSurfaceVariant
              }
            )
          }

          Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = if (enabled) DnaColors.OnSurfaceVariant else DnaColors.TextDisabled,
            modifier = Modifier
              .size(20.dp)
              .rotate(rotationState)
          )
        }
      }

      DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier
          .background(DnaColors.SurfaceContainerHigh)
          .border(1.dp, DnaColors.Border, RoundedCornerShape(10.dp))
          .padding(vertical = 4.dp)
      ) {
        options.forEach { option ->
          val isSelected = option.value == selectedValue

          DropdownMenuItem(
            text = {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.weight(1f, fill = false)
                ) {
                  if (option.icon != null) {
                    Icon(
                      imageVector = option.icon,
                      contentDescription = null,
                      tint = if (isSelected) DnaColors.Primary else DnaColors.OnSurfaceVariant,
                      modifier = Modifier
                        .padding(end = 10.dp)
                        .size(18.dp)
                    )
                  }

                  Column {
                    Text(
                      text = option.label,
                      fontFamily = DnaTypography.InterFamily,
                      fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                      fontSize = 14.sp,
                      color = if (isSelected) DnaColors.Primary else DnaColors.OnSurface
                    )
                    if (option.hint != null) {
                      Text(
                        text = option.hint,
                        fontFamily = DnaTypography.InterFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 11.sp,
                        color = DnaColors.OnSurfaceVariant
                      )
                    }
                  }
                }

                if (isSelected) {
                  Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = DnaColors.Secondary,
                    modifier = Modifier
                      .padding(start = 12.dp)
                      .size(18.dp)
                  )
                }
              }
            },
            onClick = {
              onOptionSelected(option.value)
              expanded = false
            },
            enabled = option.enabled,
            modifier = Modifier.background(
              if (isSelected) DnaColors.SurfaceContainerHighest else Color.Transparent
            )
          )
        }
      }
    }
  }
}
