package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.dna.DnaColors

/**
 * MauntingStudios Design-DNA Farbreferenzen.
 * Greift direkt auf den zentralen DnaColors-Token-Katalog zu.
 */
val InkBackground = DnaColors.Surface
val InkCanvas = DnaColors.SurfaceContainerLow
val InkPanel = DnaColors.SurfaceContainer
val InkOverlay = DnaColors.SurfaceContainerHigh
val InkHover = DnaColors.SurfaceContainerHighest
val InkActive = DnaColors.SurfaceVariant

val IceBorder = DnaColors.Border
val IceBorderFaint = DnaColors.BorderFaint
val IceBorderHighlight = DnaColors.Ring

val IceCyanPrimary = DnaColors.Primary
val IceCyanLight = DnaColors.OnPrimaryContainer
val IceCyanMuted = DnaColors.Outline
val IceCyanDark = DnaColors.PrimaryContainer
val IceCyanContainer = DnaColors.PrimaryContainer

val MintSuccess = DnaColors.StatusSuccess
val MintContainer = DnaColors.SecondaryContainer
val MintLight = DnaColors.MintAccent

val AmberWarning = DnaColors.StatusWarning
val AmberWarningContainer = Color(0xFF332005)

val CrimsonDanger = DnaColors.StatusDestructive
val CrimsonDangerContainer = Color(0xFF331010)

val TextForeground = DnaColors.OnSurface
val TextMuted = DnaColors.OnSurfaceVariant
val TextFaint = DnaColors.MutedForeground
val TextOnPrimary = DnaColors.PrimaryForeground

val PrimaryButtonBrush = DnaColors.PrimaryButtonBrush
val GlassPanelBrush = DnaColors.GlassPanelBrush
val CyanGlow = DnaColors.Primary.copy(alpha = 0.16f)

// -------------------------------------------------------------------------
// Kompatibilitaets-Aliase fuer bestehende Komponenten (Vollstaendig auf DnaColors gemappt)
// -------------------------------------------------------------------------
val AmberGoldPrimary = DnaColors.Primary
val AmberGoldLight = DnaColors.OnPrimaryContainer
val AmberGoldDark = DnaColors.PrimaryContainer
val AmberGoldContainer = DnaColors.PrimaryContainer
val OnAmberGoldContainer = DnaColors.OnPrimaryContainer

val SlateDark950 = DnaColors.Surface
val SlateDark900 = DnaColors.SurfaceContainerLow
val SlateDark800 = DnaColors.SurfaceContainer
val SlateDark700 = DnaColors.SurfaceContainerHigh
val SlateDark600 = DnaColors.Border

val TextParchment = DnaColors.OnSurface
val TextParchmentMuted = DnaColors.OnSurfaceVariant
val TextParchmentFaint = DnaColors.MutedForeground

val EmeraldSuccess = DnaColors.StatusSuccess
