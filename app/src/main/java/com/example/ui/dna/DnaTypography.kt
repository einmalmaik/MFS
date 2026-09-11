package com.example.ui.dna

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * MauntingStudios Typografie-System.
 * Basierend auf den Schriftarten aus dem Referenz-Repository:
 * - Headlines & Display: Manrope (font-headline)
 * - Body & UI: Inter (font-body-md, font-sans)
 * - Code, IDs & Zeitstempel: JetBrains Mono (font-mono)
 */
object DnaTypography {

  val UnifiedFontFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
    Font(R.font.inter, FontWeight.Bold)
  )

  // Keep these aliases so existing code doesn't break, but point them all to UnifiedFontFamily
  val ManropeFamily = UnifiedFontFamily
  val InterFamily = UnifiedFontFamily
  val JetBrainsMonoFamily = UnifiedFontFamily

  val MaterialTypography = Typography(
    displayLarge = TextStyle(
      fontFamily = UnifiedFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 32.sp,
      lineHeight = 38.sp,
      letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
      fontFamily = ManropeFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 28.sp,
      lineHeight = 34.sp,
      letterSpacing = (-0.25).sp
    ),
    headlineLarge = TextStyle(
      fontFamily = ManropeFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 24.sp,
      lineHeight = 30.sp,
      letterSpacing = (-0.2).sp
    ),
    headlineMedium = TextStyle(
      fontFamily = ManropeFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 20.sp,
      lineHeight = 26.sp,
      letterSpacing = (-0.15).sp
    ),
    headlineSmall = TextStyle(
      fontFamily = ManropeFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 18.sp,
      lineHeight = 24.sp,
      letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
      fontFamily = ManropeFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 17.sp,
      lineHeight = 24.sp,
      letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
      fontFamily = InterFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 15.sp,
      lineHeight = 22.sp,
      letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
      fontFamily = InterFamily,
      fontWeight = FontWeight.Medium,
      fontSize = 13.sp,
      lineHeight = 18.sp,
      letterSpacing = 0.1.sp
    ),
    // Hauptlesetext fuer Story-Erzaehlungen: Hohe Lesbarkeit mit Inter und optimaler Zeilenhoehe
    bodyLarge = TextStyle(
      fontFamily = InterFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 16.sp,
      lineHeight = 26.sp,
      letterSpacing = 0.2.sp
    ),
    bodyMedium = TextStyle(
      fontFamily = InterFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 14.sp,
      lineHeight = 20.sp,
      letterSpacing = 0.15.sp
    ),
    bodySmall = TextStyle(
      fontFamily = InterFamily,
      fontWeight = FontWeight.Normal,
      fontSize = 12.sp,
      lineHeight = 16.sp,
      letterSpacing = 0.2.sp
    ),
    labelLarge = TextStyle(
      fontFamily = InterFamily,
      fontWeight = FontWeight.SemiBold,
      fontSize = 14.sp,
      lineHeight = 20.sp,
      letterSpacing = 0.2.sp
    ),
    labelMedium = TextStyle(
      fontFamily = InterFamily,
      fontWeight = FontWeight.Medium,
      fontSize = 12.sp,
      lineHeight = 16.sp,
      letterSpacing = 0.3.sp
    ),
    labelSmall = TextStyle(
      fontFamily = InterFamily,
      fontWeight = FontWeight.Medium,
      fontSize = 11.sp,
      lineHeight = 14.sp,
      letterSpacing = 0.4.sp
    )
  )

  // JetBrains Mono Hilfsstile fuer Metriken, Zeitangaben und Terminal-Logs
  val MonoMedium = TextStyle(
    fontFamily = JetBrainsMonoFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.sp,
    color = DnaColors.Primary
  )

  val MonoSmall = TextStyle(
    fontFamily = JetBrainsMonoFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp,
    lineHeight = 15.sp,
    letterSpacing = 0.2.sp,
    color = DnaColors.OnSurfaceVariant
  )
}
