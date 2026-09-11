package com.example.ui.dna

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Zentrale Card-Container-Komponente des MauntingStudios Design-Systems.
 * Nutzt 'SurfaceContainer' (#101B1F) mit praeziser Kantenlinie (#284147).
 */
@Composable
fun DnaCard(
  modifier: Modifier = Modifier,
  shapeRadius: Dp = 14.dp,
  backgroundColor: Color = DnaColors.SurfaceContainer,
  borderColor: Color = DnaColors.Border,
  borderWidth: Dp = 1.dp,
  content: @Composable () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(shapeRadius),
    color = backgroundColor,
    border = BorderStroke(borderWidth, borderColor),
    modifier = modifier
  ) {
    Box(modifier = Modifier.padding(14.dp)) {
      content()
    }
  }
}
