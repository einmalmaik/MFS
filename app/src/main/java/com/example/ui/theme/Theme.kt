package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * MauntingStudios Design-DNA Theme Configuration.
 * Basiert auf der Dark-First Core-Palette, Elevation-Surfaces (Ink) und Ice-Cyan/Mint-Akzenten.
 */
private val MauntingDnaColorScheme = darkColorScheme(
  primary = IceCyanPrimary,
  onPrimary = TextOnPrimary,
  primaryContainer = IceCyanContainer,
  onPrimaryContainer = IceCyanLight,
  secondary = MintSuccess,
  onSecondary = TextOnPrimary,
  secondaryContainer = InkOverlay,
  onSecondaryContainer = TextForeground,
  tertiary = IceCyanMuted,
  onTertiary = TextForeground,
  background = InkBackground,
  onBackground = TextForeground,
  surface = InkPanel,
  onSurface = TextForeground,
  surfaceVariant = InkOverlay,
  onSurfaceVariant = TextMuted,
  outline = IceBorder,
  outlineVariant = IceBorderFaint,
  error = CrimsonDanger,
  onError = Color.White
)

@Composable
fun MauntingStoryTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = MauntingDnaColorScheme,
    typography = Typography,
    content = content
  )
}

@Composable
fun StoryForgeTheme(
  content: @Composable () -> Unit,
) = MauntingStoryTheme(content = content)
