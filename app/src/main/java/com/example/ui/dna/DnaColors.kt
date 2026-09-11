package com.example.ui.dna

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * MauntingStudios Design-DNA Farbdefinitionen und Tokens.
 * Exakt synchronisiert mit dem Referenz-Repository: maunting-server-manager (frontend/tailwind.config.ts).
 *
 * Kernprinzipien:
 * - Technical Calm: Tiefe, neutrale Oberflaechen fuer langes, blendfreies Lesen und Arbeiten
 * - Ice Cyan & Teal/Mint als praezise vertrauensbildende Akzente
 * - Echte Statusfarben fuer Erfolg (Mint/Gruen), Warnungen (Amber) und Gefahr (Rot)
 * - Erhabene Glassmorphism-Layer von Surface-Container bis Surface-Container-Highest
 */
object DnaColors {
  // --- MSM Surface Elevation (Hintergruende & Flaechen) ---
  val Surface = Color(0xFF071013)                 // Tiefster Grundraum / App-Shell (#071013)
  val SurfaceDim = Color(0xFF04090B)              // Abgedunkelter Kontrastgrund (#04090b)
  val SurfaceBright = Color(0xFF253238)           // Aufgehellte Kante (#253238)
  val SurfaceContainerLowest = Color(0xFF03080A)  // #03080a
  val SurfaceContainerLow = Color(0xFF0B1518)     // #0b1518
  val SurfaceContainer = Color(0xFF101B1F)        // Cards, Panels, Listen-Container (#101b1f)
  val SurfaceContainerHigh = Color(0xFF162328)    // Modals, Dropdowns, Sheets (#162328)
  val SurfaceContainerHighest = Color(0xFF203038) // Hover- & aktive Auswahlflaechen (#203038)
  val SurfaceVariant = Color(0xFF22343B)          // Subtile Kontrastflaechen (#22343b)

  // --- Linien, Kanten & Fokusringe ---
  val Border = Color(0xFF284147)                  // Standard-Border (#284147)
  val OutlineVariant = Color(0xFF284147)          // Input- & Card-Border
  val Outline = Color(0xFF5B737A)                 // Aktivere Umrandung (#5b737a)
  val BorderFaint = Color(0xFF1A2A2E)             // Zarte Trennlinien
  val Ring = Color(0xFF67E8F9)                    // Fokusring (#67e8f9)

  // --- Primaer-Akzent (Maunting Logo Ice Cyan) ---
  val Primary = Color(0xFFB9F6FF)                 // Glasklares Logo Ice Cyan (#b9f6ff)
  val PrimaryForeground = Color(0xFF031316)       // Dunkler Text auf Primaerflaechen (#031316)
  val PrimaryContainer = Color(0xFF0C3B45)        // Primaer-Container (#0c3b45)
  val OnPrimaryContainer = Color(0xFFD9FBFF)      // Text auf Primaer-Container (#d9fbff)

  // --- Sekundaer-Akzent (Teal / Mint Accent) ---
  val Secondary = Color(0xFF5EEAD4)               // Kontrolliertes Teal/Mint (#5eead4)
  val SecondaryForeground = Color(0xFF031316)     // #031316
  val SecondaryContainer = Color(0xFF0F766E)      // #0f766e
  val OnSecondaryContainer = Color(0xFFECFEFF)    // #ecfeff
  val MintAccent = Color(0xFF86EFAC)              // Frischer Mint-Akzent (#86efac)

  // --- Tertiär (Ice Blue) ---
  val Tertiary = Color(0xFFD9F7FF)                // #d9f7ff
  val OnTertiary = Color(0xFF06222A)              // #06222a
  val TertiaryContainer = Color(0xFF12323B)       // #12323b

  // --- Text- & Inhaltsfarben ---
  val OnSurface = Color(0xFFE7F4F7)               // Haupttext (#e7f4f7) - maximale Lesbarkeit
  val OnSurfaceVariant = Color(0xFFA9BDC3)        // Gedaempfter Text (#a9bdc3) - Metadaten, Narration
  val MutedForeground = Color(0xFF9DB3B8)         // Dezent (#9db3b8)
  val TextDisabled = Color(0xFF52686E)            // Deaktivierte Textelemente

  // --- Status & Feedback ---
  val StatusSuccess = Color(0xFF2FCF89)           // Vault-Schutz & Erfolg (hsl 158 64% 52%)
  val StatusWarning = Color(0xFFF59E0B)           // Ruhige Warnungen & Hinweise (hsl 38 92% 50%)
  val StatusDestructive = Color(0xFFE53E3E)       // Echte Gefahr & Verletzungen (hsl 0 70% 55%)
  val DestructiveForeground = Color(0xFFFFF1F2)   // #fff1f2

  // --- Maunting Studiopins & Verlaeufe ---
  val PrimaryButtonBrush = Brush.linearGradient(
    colors = listOf(
      Color(0xFFD9FBFF),
      Color(0xFFB9F6FF),
      Color(0xFF5EEAD4)
    )
  )

  val GlassPanelBrush = Brush.linearGradient(
    colors = listOf(
      Color(0xF0101B1F),
      Color(0xD0071013)
    )
  )

  val EdgeHighlightBrush = Brush.horizontalGradient(
    colors = listOf(
      Color.Transparent,
      Color(0x55B9F6FF),
      Color.Transparent
    )
  )

  // --- Zusaetzliche Atmosphaeren-Akzente fuer das interaktive Story-System ---
  val StoryAmberCampfire = Color(0xFFFBBF24)       // Warmes Gold/Bernstein fuer Tavernen & Lagerfeuer
  val StoryArcanePurple = Color(0xFFC084FC)        // Mystisches Violett fuer Zauber & Daemonie
  val StoryBloodCrimson = Color(0xFFDC2626)        // Anatomisches Blutrot fuer schwere Wunden
}
