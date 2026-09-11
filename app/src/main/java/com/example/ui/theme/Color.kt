package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * MauntingStudios Design-DNA Design-Tokens & Farben (Singra / Singra Vault Basis)
 *
 * Basierend auf:
 * - Tiefes Kaltes Ink/Dunkelheit als Grundraum (--background / --el-0 bis --el-5)
 * - Eisblau / Cyan / Mint als vertrauensbildende Akzente (--primary / ice / mint)
 * - Mint/Grün für Schutz, Erfolg und Vault-Stärke
 * - Amber für Hinweise und Warnungen
 * - Rot (Destructive) nur für echte Gefahr oder destruktive Aktionen
 * - Glassmorphism- und Glow-Stile
 */

// --- MauntingStudios Core Ink & Elevation Surfaces ---
val InkBackground = Color(0xFF071014)     // --el-0: 206 31% 4% (tiefster Hintergrund / App-Shell)
val InkCanvas = Color(0xFF0B1418)         // --el-1: 205 30% 7% (Grundfläche)
val InkPanel = Color(0xFF101E24)          // --el-2: 204 28% 10% (Cards, Panels, Modals)
val InkOverlay = Color(0xFF162730)        // --el-3: 202 26% 14% (Dropdowns, Banner, Overlays)
val InkHover = Color(0xFF1C313C)          // --el-4: 200 24% 19% (Hover-Flächen)
val InkActive = Color(0xFF233C49)         // --el-5: 198 22% 26% (Aktive/selektierte Flächen)

// --- Borders & Outlines ---
val IceBorder = Color(0xFF2C434C)         // hsl(198 22% 26%) - Maunting subtle border
val IceBorderFaint = Color(0xFF1A2D35)    // border-ice-300/12 - hairline borders
val IceBorderHighlight = Color(0xFF76BEC7)// border-ice-300 - glow/focus highlight

// --- Maunting Ice-Cyan & Brand Primary ---
val IceCyanPrimary = Color(0xFF9BD4DA)    // MauntingStudios Ice-Cyan Brand Akzent
val IceCyanLight = Color(0xFFD9F3F5)      // Ice-100 / Light Highlight
val IceCyanMuted = Color(0xFF76BEC7)      // Ice-300 Mid Tone
val IceCyanDark = Color(0xFF386B73)       // Dark Cyan Container Tint
val IceCyanContainer = Color(0xFF0E252D)  // Primary container background

// --- Mint / Vault Protection / Online Green ---
val MintSuccess = Color(0xFF70E7BD)       // Vault-Grün / Schutz / Erfolg
val MintContainer = Color(0xFF0D2C24)     // Mint subtle container
val MintLight = Color(0xFFAEF4DA)

// --- Amber / Hint / Warning ---
val AmberWarning = Color(0xFFF59E0B)      // Warnungen, non-blocking Hints, Meilensteine
val AmberWarningContainer = Color(0xFF332005)

// --- Crimson / Destructive Danger ---
val CrimsonDanger = Color(0xFFEF4444)     // Echte Gefahr / Destruktiv / Löschen
val CrimsonDangerContainer = Color(0xFF331010)

// --- Typography: Clean Ice & Neutral Text ---
val TextForeground = Color(0xFFF1F8F9)    // hsl(188 29% 95%) - Primärtext, glasklar lesbar
val TextMuted = Color(0xFFA3C0C6)         // hsl(196 19% 67%) - Sekundärtext / Narration
val TextFaint = Color(0xFF67858C)         // Timestamps, Meta-Labels, Subtle Footers
val TextOnPrimary = Color(0xFF071014)     // Dunkler Text auf Cyan-Buttons

// --- Gradients & Glass Brushes (Maunting DNA) ---
val PrimaryButtonBrush = Brush.linearGradient(
  colors = listOf(
    Color(0xFFD9F3F5),
    Color(0xFF9BD4DA),
    Color(0xFF70E7BD)
  )
)

val GlassCardBrush = Brush.linearGradient(
  colors = listOf(
    Color(0xEE102229),
    Color(0xB80A151A)
  )
)

val EdgeHighlightBrush = Brush.horizontalGradient(
  colors = listOf(
    Color.Transparent,
    Color(0x73BEE7EB),
    Color.Transparent
  )
)

// --- Abwärtskompatible Aliase für nahtlose Migration ---
val SlateDark950 = InkBackground
val SlateDark900 = InkCanvas
val SlateDark800 = InkPanel
val SlateDark700 = InkOverlay
val SlateDark600 = IceBorder

val AmberGoldPrimary = IceCyanPrimary
val AmberGoldLight = IceCyanLight
val AmberGoldDark = IceCyanMuted
val AmberGoldContainer = IceCyanContainer
val OnAmberGoldContainer = IceCyanLight

val TextParchment = TextForeground
val TextParchmentMuted = TextMuted
val TextParchmentFaint = TextFaint

val EmeraldSuccess = MintSuccess
val BlueInfo = IceCyanPrimary
val PurpleMagic = Color(0xFFB57EDC)
