package com.example.ui.theme

import androidx.compose.material3.Typography
import com.example.ui.dna.DnaTypography

/**
 * Zentrale Typografie-Konfiguration der Anwendung.
 * Vollstaendig angebunden an die MauntingStudios Design-DNA (DnaTypography):
 * - Manrope fuer Titel, Headlines und Displays
 * - Inter fuer UI, Menues, Dialoge und Story-Lesetexte
 * - JetBrains Mono fuer Zeitstempel, Statuswerte und technische IDs
 */
val Typography: Typography = DnaTypography.MaterialTypography

val ManropeFamily = DnaTypography.ManropeFamily
val InterFamily = DnaTypography.InterFamily
val JetBrainsMonoFamily = DnaTypography.JetBrainsMonoFamily
