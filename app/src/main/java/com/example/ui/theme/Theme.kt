package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StoryForgeColorScheme = darkColorScheme(
  primary = AmberGoldPrimary,
  onPrimary = SlateDark950,
  primaryContainer = AmberGoldContainer,
  onPrimaryContainer = OnAmberGoldContainer,
  secondary = AmberGoldLight,
  onSecondary = SlateDark950,
  secondaryContainer = SlateDark700,
  onSecondaryContainer = TextParchment,
  tertiary = AmberGoldDark,
  onTertiary = TextParchment,
  background = SlateDark900,
  onBackground = TextParchment,
  surface = SlateDark800,
  onSurface = TextParchment,
  surfaceVariant = SlateDark700,
  onSurfaceVariant = TextParchmentMuted,
  outline = SlateDark600,
  outlineVariant = SlateDark700,
  error = CrimsonDanger,
  onError = Color.White
)

@Composable
fun StoryForgeTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = StoryForgeColorScheme,
    typography = Typography,
    content = content
  )
}
