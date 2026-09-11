package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.BodyPart
import com.example.data.model.CharacterGender
import com.example.data.model.CharacterInjury
import com.example.data.model.InjurySeverity
import com.example.ui.dna.DnaColors

/**
 * Anatomischer interaktiver Mannequin-Canvas fuer maennliche und weibliche Koerpersilhouetten.
 *
 * Zeichnet eine echte menschliche Silhouette mit differenzierten Proportionen:
 * - MAENNLICH: Breitere Schultern, athletischer Brustkorb, V-Form zur Taille, kraeftige Beine.
 * - WEIBLICH: Schmalere Schultern, betonte Taille, geschwungene Hueft- und Beinlinie.
 *
 * Verwaltet interaktive Koerperteile, visualisiert aktive Verletzungen mit pulsierenden Beacons
 * und ermoeglicht das Antippen einzelner Regionen zur gezielten Wundinspektion.
 */
@Composable
fun CharacterMannequin(
  gender: CharacterGender,
  injuries: List<CharacterInjury>,
  selectedBodyPart: BodyPart?,
  onBodyPartSelected: (BodyPart) -> Unit,
  modifier: Modifier = Modifier,
  testTag: String = "character_mannequin"
) {
  // Kontinuierliche Puls-Animation fuer aktive Verletzungs-Beacons
  val infiniteTransition = rememberInfiniteTransition(label = "injury_pulse_transition")
  val pulseRadiusFraction by infiniteTransition.animateFloat(
    initialValue = 0.8f,
    targetValue = 2.4f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "injury_pulse_radius"
  )
  val pulseAlphaFraction by infiniteTransition.animateFloat(
    initialValue = 0.7f,
    targetValue = 0.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "injury_pulse_alpha"
  )

  Box(
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .background(DnaColors.SurfaceContainerLow)
      .border(1.dp, DnaColors.Border, RoundedCornerShape(14.dp))
      .testTag(testTag)
  ) {
    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .pointerInput(gender) {
          detectTapGestures { tapOffset ->
            val w = size.width.toFloat()
            val h = size.height.toFloat()

            // Bestimme das der Antipp-Koordinate am naechsten liegende Koerperteil
            var closestPart: BodyPart? = null
            var minDistanceSq = Float.MAX_VALUE

            for (part in BodyPart.entries) {
              val px = part.xPercent * w
              val py = part.yPercent * h
              val dx = px - tapOffset.x
              val dy = py - tapOffset.y
              val distSq = dx * dx + dy * dy

              // Maximaler Trefferradius (ca. 48dp Skalierung)
              val thresholdSq = (w * 0.22f) * (w * 0.22f)
              if (distSq < thresholdSq && distSq < minDistanceSq) {
                minDistanceSq = distSq
                closestPart = part
              }
            }

            if (closestPart != null) {
              onBodyPartSelected(closestPart)
            }
          }
        }
    ) {
      val w = size.width
      val h = size.height

      // Hintergrund: Subtiler technischer Vermessungsraster im Maunting-Stil
      drawTechnicalGrid(w, h)

      // Silhouette: Basierend auf gewaehltem Geschlecht
      if (gender == CharacterGender.MALE) {
        drawMaleSilhouette(w, h, selectedBodyPart)
      } else {
        drawFemaleSilhouette(w, h, selectedBodyPart)
      }

      // Anatomische Zielmarkierungen fuer aktive Verletzungen
      drawInjuryBeacons(
        width = w,
        height = h,
        injuries = injuries,
        selectedBodyPart = selectedBodyPart,
        pulseRadius = pulseRadiusFraction,
        pulseAlpha = pulseAlphaFraction
      )
    }
  }
}

/**
 * Zeichnet zarte technische Hilfslinien und Markierungen im Hintergrund des Canvas.
 */
private fun DrawScope.drawTechnicalGrid(w: Float, h: Float) {
  val gridColor = DnaColors.BorderFaint.copy(alpha = 0.5f)

  // Vertikale Zentrierungsachse
  drawLine(
    color = gridColor,
    start = Offset(w * 0.5f, h * 0.04f),
    end = Offset(w * 0.5f, h * 0.96f),
    strokeWidth = 1.dp.toPx()
  )

  // Horizontale Referenzebenen (Schulter, Taille, Knie)
  val levels = listOf(0.24f, 0.44f, 0.72f)
  for (lvl in levels) {
    drawLine(
      color = gridColor,
      start = Offset(w * 0.15f, h * lvl),
      end = Offset(w * 0.85f, h * lvl),
      strokeWidth = 1.dp.toPx()
    )
  }
}

/**
 * Zeichnet die vollstaendige maennliche menschliche Silhouette mit athletischen Proportionen.
 */
private fun DrawScope.drawMaleSilhouette(
  w: Float,
  h: Float,
  selectedBodyPart: BodyPart?
) {
  val cx = w * 0.5f
  val bodyFill = DnaColors.SurfaceContainerHighest
  val bodyBorder = DnaColors.Border.copy(alpha = 0.9f)
  val highlightBorder = DnaColors.Primary

  // Kopf (Anatomische Schaedelform mit Kinnkontur)
  val headPath = Path().apply {
    moveTo(cx, h * 0.05f)
    cubicTo(cx + w * 0.11f, h * 0.05f, cx + w * 0.11f, h * 0.15f, cx + w * 0.07f, h * 0.18f)
    lineTo(cx + w * 0.035f, h * 0.195f)
    lineTo(cx - w * 0.035f, h * 0.195f)
    lineTo(cx - w * 0.07f, h * 0.18f)
    cubicTo(cx - w * 0.11f, h * 0.15f, cx - w * 0.11f, h * 0.05f, cx, h * 0.05f)
    close()
  }
  drawPath(headPath, color = bodyFill)
  drawPath(
    headPath,
    color = if (selectedBodyPart == BodyPart.HEAD) highlightBorder else bodyBorder,
    style = Stroke(width = if (selectedBodyPart == BodyPart.HEAD) 2.5.dp.toPx() else 1.5.dp.toPx())
  )

  // Hals & Nacken (Kraeftiger Nackenuebergang zu den Schultern)
  val neckPath = Path().apply {
    moveTo(cx - w * 0.045f, h * 0.19f)
    lineTo(cx + w * 0.045f, h * 0.19f)
    lineTo(cx + w * 0.06f, h * 0.235f)
    lineTo(cx - w * 0.06f, h * 0.235f)
    close()
  }
  drawPath(neckPath, color = bodyFill)
  drawPath(
    neckPath,
    color = if (selectedBodyPart == BodyPart.NECK) highlightBorder else bodyBorder,
    style = Stroke(width = if (selectedBodyPart == BodyPart.NECK) 2.5.dp.toPx() else 1.5.dp.toPx())
  )

  // Torso & Brustkorb (Athletische V-Form: Breite Schultern, Rippenbogen, Tailleneinzug)
  val torsoPath = Path().apply {
    moveTo(cx - w * 0.22f, h * 0.235f) // Linke Schulterecke
    lineTo(cx + w * 0.22f, h * 0.235f) // Rechte Schulterecke
    cubicTo(cx + w * 0.20f, h * 0.33f, cx + w * 0.16f, h * 0.40f, cx + w * 0.14f, h * 0.48f) // Rechter Rippenbogen zur Taille
    lineTo(cx + w * 0.15f, h * 0.54f) // Rechte Huefte
    lineTo(cx, h * 0.56f)             // Schrittansatz
    lineTo(cx - w * 0.15f, h * 0.54f) // Linke Huefte
    cubicTo(cx - w * 0.14f, h * 0.40f, cx - w * 0.20f, h * 0.33f, cx - w * 0.22f, h * 0.235f) // Linker Rippenbogen zur Schulter
    close()
  }
  drawPath(torsoPath, color = bodyFill)
  val isTorsoSelected = selectedBodyPart == BodyPart.CHEST || selectedBodyPart == BodyPart.ABDOMEN
  drawPath(
    torsoPath,
    color = if (isTorsoSelected) highlightBorder else bodyBorder,
    style = Stroke(width = if (isTorsoSelected) 2.5.dp.toPx() else 1.5.dp.toPx())
  )

  // Genitalbereich (männlich)
  val genitalsPath = Path().apply {
    moveTo(cx - w * 0.05f, h * 0.56f)
    cubicTo(cx - w * 0.03f, h * 0.60f, cx + w * 0.03f, h * 0.60f, cx + w * 0.05f, h * 0.56f)
    cubicTo(cx + w * 0.02f, h * 0.64f, cx - w * 0.02f, h * 0.64f, cx - w * 0.05f, h * 0.56f)
    close()
  }
  drawPath(genitalsPath, color = bodyFill)
  val isGenitalsSelected = selectedBodyPart == BodyPart.GENITALS
  drawPath(
    genitalsPath,
    color = if (isGenitalsSelected) highlightBorder else bodyBorder,
    style = Stroke(width = if (isGenitalsSelected) 2.5.dp.toPx() else 1.5.dp.toPx())
  )

  // Anatomische Brust-Teilungslinie (Pectoralis)
  drawLine(
    color = DnaColors.Border,
    start = Offset(cx - w * 0.12f, h * 0.31f),
    end = Offset(cx + w * 0.12f, h * 0.31f),
    strokeWidth = 1.dp.toPx()
  )

  // Linker Arm (Bizeps, Ellbogen, Unterarm und Hand)
  val leftArmPath = Path().apply {
    moveTo(cx - w * 0.22f, h * 0.235f)
    cubicTo(cx - w * 0.27f, h * 0.29f, cx - w * 0.26f, h * 0.38f, cx - w * 0.24f, h * 0.45f)
    lineTo(cx - w * 0.22f, h * 0.52f) // Handgelenk
    cubicTo(cx - w * 0.23f, h * 0.56f, cx - w * 0.19f, h * 0.56f, cx - w * 0.18f, h * 0.52f) // Hand/Finger
    lineTo(cx - w * 0.19f, h * 0.45f)
    cubicTo(cx - w * 0.18f, h * 0.36f, cx - w * 0.16f, h * 0.28f, cx - w * 0.16f, h * 0.24f)
    close()
  }
  drawPath(leftArmPath, color = bodyFill)
  val isLeftArmSelected = selectedBodyPart == BodyPart.LEFT_ARM || selectedBodyPart == BodyPart.LEFT_HAND
  drawPath(
    leftArmPath,
    color = if (isLeftArmSelected) highlightBorder else bodyBorder,
    style = Stroke(width = if (isLeftArmSelected) 2.5.dp.toPx() else 1.5.dp.toPx())
  )

  // Rechter Arm (Bizeps, Ellbogen, Unterarm und Hand)
  val rightArmPath = Path().apply {
    moveTo(cx + w * 0.22f, h * 0.235f)
    cubicTo(cx + w * 0.27f, h * 0.29f, cx + w * 0.26f, h * 0.38f, cx + w * 0.24f, h * 0.45f)
    lineTo(cx + w * 0.22f, h * 0.52f) // Handgelenk
    cubicTo(cx + w * 0.23f, h * 0.56f, cx + w * 0.19f, h * 0.56f, cx + w * 0.18f, h * 0.52f) // Hand/Finger
    lineTo(cx + w * 0.19f, h * 0.45f)
    cubicTo(cx + w * 0.18f, h * 0.36f, cx + w * 0.16f, h * 0.28f, cx + w * 0.16f, h * 0.24f)
    close()
  }
  drawPath(rightArmPath, color = bodyFill)
  val isRightArmSelected = selectedBodyPart == BodyPart.RIGHT_ARM || selectedBodyPart == BodyPart.RIGHT_HAND
  drawPath(
    rightArmPath,
    color = if (isRightArmSelected) highlightBorder else bodyBorder,
    style = Stroke(width = if (isRightArmSelected) 2.5.dp.toPx() else 1.5.dp.toPx())
  )

  // Linkes Bein (Oberschenkel, Knie, Wade, Knoechel und Fuss)
  val leftLegPath = Path().apply {
    moveTo(cx - w * 0.15f, h * 0.54f)
    cubicTo(cx - w * 0.17f, h * 0.63f, cx - w * 0.15f, h * 0.72f, cx - w * 0.14f, h * 0.76f) // Aussenseite Oberschenkel & Knie
    cubicTo(cx - w * 0.15f, h * 0.82f, cx - w * 0.13f, h * 0.88f, cx - w * 0.14f, h * 0.94f) // Wade zu Knoechel
    lineTo(cx - w * 0.06f, h * 0.94f)                                                          // Fusssohle
    cubicTo(cx - w * 0.05f, h * 0.88f, cx - w * 0.07f, h * 0.80f, cx - w * 0.06f, h * 0.74f) // Innenseite Unterschenkel
    lineTo(cx - w * 0.02f, h * 0.56f)                                                          // Innenschenkel zum Schritt
    close()
  }
  drawPath(leftLegPath, color = bodyFill)
  val isLeftLegSelected = selectedBodyPart == BodyPart.LEFT_LEG || selectedBodyPart == BodyPart.FEET
  drawPath(
    leftLegPath,
    color = if (isLeftLegSelected) highlightBorder else bodyBorder,
    style = Stroke(width = if (isLeftLegSelected) 2.5.dp.toPx() else 1.5.dp.toPx())
  )

  // Rechtes Bein (Oberschenkel, Knie, Wade, Knoechel und Fuss)
  val rightLegPath = Path().apply {
    moveTo(cx + w * 0.15f, h * 0.54f)
    cubicTo(cx + w * 0.17f, h * 0.63f, cx + w * 0.15f, h * 0.72f, cx + w * 0.14f, h * 0.76f) // Aussenseite Oberschenkel & Knie
    cubicTo(cx + w * 0.15f, h * 0.82f, cx + w * 0.13f, h * 0.88f, cx + w * 0.14f, h * 0.94f) // Wade zu Knoechel
    lineTo(cx + w * 0.06f, h * 0.94f)                                                          // Fusssohle
    cubicTo(cx + w * 0.05f, h * 0.88f, cx + w * 0.07f, h * 0.80f, cx + w * 0.06f, h * 0.74f) // Innenseite Unterschenkel
    lineTo(cx + w * 0.02f, h * 0.56f)                                                          // Innenschenkel zum Schritt
    close()
  }
  drawPath(rightLegPath, color = bodyFill)
  val isRightLegSelected = selectedBodyPart == BodyPart.RIGHT_LEG || selectedBodyPart == BodyPart.FEET
  drawPath(
    rightLegPath,
    color = if (isRightLegSelected) highlightBorder else bodyBorder,
    style = Stroke(width = if (isRightLegSelected) 2.5.dp.toPx() else 1.5.dp.toPx())
  )
}

/**
 * Zeichnet die weibliche menschliche Silhouette mit geschwungenen, femininen Konturen.
 */
private fun DrawScope.drawFemaleSilhouette(
  w: Float,
  h: Float,
  selectedBodyPart: BodyPart?
) {
  val cx = w * 0.5f
  val bodyFill = DnaColors.SurfaceContainerHighest
  val bodyBorder = DnaColors.Border.copy(alpha = 0.9f)
  val highlightBorder = DnaColors.Primary

  // Kopf (Etwas grazilerer Schaedel & feineres Kinn)
  val headPath = Path().apply {
    moveTo(cx, h * 0.055f)
    cubicTo(cx + w * 0.10f, h * 0.055f, cx + w * 0.095f, h * 0.145f, cx + w * 0.06f, h * 0.175f)
    lineTo(cx + w * 0.028f, h * 0.19f)
    lineTo(cx - w * 0.028f, h * 0.19f)
    lineTo(cx - w * 0.06f, h * 0.175f)
    cubicTo(cx - w * 0.095f, h * 0.145f, cx - w * 0.10f, h * 0.055f, cx, h * 0.055f)
    close()
  }
  drawPath(headPath, color = bodyFill)
  drawPath(
    headPath,
    color = if (selectedBodyPart == BodyPart.HEAD) highlightBorder else bodyBorder,
    style = Stroke(width = if (selectedBodyPart == BodyPart.HEAD) 2.5.dp.toPx() else 1.5.dp.toPx())
  )

  // Hals (Schlankere, laengere Nackenlinie)
  val neckPath = Path().apply {
    moveTo(cx - w * 0.035f, h * 0.19f)
    lineTo(cx + w * 0.035f, h * 0.19f)
    lineTo(cx + w * 0.045f, h * 0.235f)
    lineTo(cx - w * 0.045f, h * 0.235f)
    close()
  }
  drawPath(neckPath, color = bodyFill)
  drawPath(
    neckPath,
    color = if (selectedBodyPart == BodyPart.NECK) highlightBorder else bodyBorder,
    style = Stroke(width = if (selectedBodyPart == BodyPart.NECK) 2.5.dp.toPx() else 1.5.dp.toPx())
  )

  // Torso (Feminine Sanduhr-Form: Schmalere Schultern, betonte Brust/Taille, weicher Hueftschwung)
  val torsoPath = Path().apply {
    moveTo(cx - w * 0.18f, h * 0.235f) // Linke Schulter
    lineTo(cx + w * 0.18f, h * 0.235f) // Rechte Schulter
    cubicTo(cx + w * 0.18f, h * 0.30f, cx + w * 0.19f, h * 0.34f, cx + w * 0.13f, h * 0.42f) // Rechte Brust & geschwungene Taille
    cubicTo(cx + w * 0.10f, h * 0.46f, cx + w * 0.17f, h * 0.50f, cx + w * 0.16f, h * 0.55f) // Rechte Huefte
    lineTo(cx, h * 0.565f)                                                                    // Schritt
    lineTo(cx - w * 0.16f, h * 0.55f)                                                         // Linke Huefte
    cubicTo(cx - w * 0.17f, h * 0.50f, cx - w * 0.10f, h * 0.46f, cx - w * 0.13f, h * 0.42f) // Linke geschwungene Taille
    cubicTo(cx - w * 0.19f, h * 0.34f, cx - w * 0.18f, h * 0.30f, cx - w * 0.18f, h * 0.235f) // Linke Brust zur Schulter
    close()
  }
  drawPath(torsoPath, color = bodyFill)
  val isTorsoSelected = selectedBodyPart == BodyPart.CHEST || selectedBodyPart == BodyPart.ABDOMEN
  drawPath(
    torsoPath,
    color = if (isTorsoSelected) highlightBorder else bodyBorder,
    style = Stroke(width = if (isTorsoSelected) 2.5.dp.toPx() else 1.5.dp.toPx())
  )

  // Genitalbereich (weiblich)
  val genitalsPath = Path().apply {
    moveTo(cx - w * 0.04f, h * 0.565f)
    cubicTo(cx - w * 0.02f, h * 0.585f, cx + w * 0.02f, h * 0.585f, cx + w * 0.04f, h * 0.565f)
    cubicTo(cx + w * 0.02f, h * 0.605f, cx - w * 0.02f, h * 0.605f, cx - w * 0.04f, h * 0.565f)
    close()
  }
  drawPath(genitalsPath, color = bodyFill)
  val isGenitalsSelected = selectedBodyPart == BodyPart.GENITALS
  drawPath(
    genitalsPath,
    color = if (isGenitalsSelected) highlightBorder else bodyBorder,
    style = Stroke(width = if (isGenitalsSelected) 2.5.dp.toPx() else 1.5.dp.toPx())
  )

  // Zarte feminine Dekolleté-Linie
  val bustPath = Path().apply {
    moveTo(cx - w * 0.08f, h * 0.285f)
    cubicTo(cx - w * 0.05f, h * 0.33f, cx, h * 0.34f, cx, h * 0.32f)
    cubicTo(cx, h * 0.34f, cx + w * 0.05f, h * 0.33f, cx + w * 0.08f, h * 0.285f)
  }
  drawPath(bustPath, color = DnaColors.Border, style = Stroke(width = 1.dp.toPx()))

  // Linker Arm (Schlank, grazile Hand)
  val leftArmPath = Path().apply {
    moveTo(cx - w * 0.18f, h * 0.235f)
    cubicTo(cx - w * 0.22f, h * 0.29f, cx - w * 0.22f, h * 0.38f, cx - w * 0.20f, h * 0.45f)
    lineTo(cx - w * 0.18f, h * 0.53f) // Handgelenk & Hand
    cubicTo(cx - w * 0.19f, h * 0.56f, cx - w * 0.16f, h * 0.56f, cx - w * 0.15f, h * 0.52f)
    lineTo(cx - w * 0.16f, h * 0.45f)
    cubicTo(cx - w * 0.16f, h * 0.36f, cx - w * 0.14f, h * 0.28f, cx - w * 0.13f, h * 0.245f)
    close()
  }
  drawPath(leftArmPath, color = bodyFill)
  val isLeftArmSelected = selectedBodyPart == BodyPart.LEFT_ARM || selectedBodyPart == BodyPart.LEFT_HAND
  drawPath(
    leftArmPath,
    color = if (isLeftArmSelected) highlightBorder else bodyBorder,
    style = Stroke(width = if (isLeftArmSelected) 2.5.dp.toPx() else 1.5.dp.toPx())
  )

  // Rechter Arm (Schlank, grazile Hand)
  val rightArmPath = Path().apply {
    moveTo(cx + w * 0.18f, h * 0.235f)
    cubicTo(cx + w * 0.22f, h * 0.29f, cx + w * 0.22f, h * 0.38f, cx + w * 0.20f, h * 0.45f)
    lineTo(cx + w * 0.18f, h * 0.53f) // Handgelenk & Hand
    cubicTo(cx + w * 0.19f, h * 0.56f, cx + w * 0.16f, h * 0.56f, cx + w * 0.15f, h * 0.52f)
    lineTo(cx + w * 0.16f, h * 0.45f)
    cubicTo(cx + w * 0.16f, h * 0.36f, cx + w * 0.14f, h * 0.28f, cx + w * 0.13f, h * 0.245f)
    close()
  }
  drawPath(rightArmPath, color = bodyFill)
  val isRightArmSelected = selectedBodyPart == BodyPart.RIGHT_ARM || selectedBodyPart == BodyPart.RIGHT_HAND
  drawPath(
    rightArmPath,
    color = if (isRightArmSelected) highlightBorder else bodyBorder,
    style = Stroke(width = if (isRightArmSelected) 2.5.dp.toPx() else 1.5.dp.toPx())
  )

  // Linkes Bein (Geschwungener weiblicher Oberschenkel, schlanke Wade und Fuesse)
  val leftLegPath = Path().apply {
    moveTo(cx - w * 0.16f, h * 0.55f)
    cubicTo(cx - w * 0.18f, h * 0.63f, cx - w * 0.16f, h * 0.72f, cx - w * 0.13f, h * 0.76f)
    cubicTo(cx - w * 0.14f, h * 0.82f, cx - w * 0.12f, h * 0.88f, cx - w * 0.12f, h * 0.94f)
    lineTo(cx - w * 0.05f, h * 0.94f)
    cubicTo(cx - w * 0.05f, h * 0.88f, cx - w * 0.06f, h * 0.80f, cx - w * 0.05f, h * 0.74f)
    lineTo(cx - w * 0.015f, h * 0.565f)
    close()
  }
  drawPath(leftLegPath, color = bodyFill)
  val isLeftLegSelected = selectedBodyPart == BodyPart.LEFT_LEG || selectedBodyPart == BodyPart.FEET
  drawPath(
    leftLegPath,
    color = if (isLeftLegSelected) highlightBorder else bodyBorder,
    style = Stroke(width = if (isLeftLegSelected) 2.5.dp.toPx() else 1.5.dp.toPx())
  )

  // Rechtes Bein (Geschwungener weiblicher Oberschenkel, schlanke Wade und Fuesse)
  val rightLegPath = Path().apply {
    moveTo(cx + w * 0.16f, h * 0.55f)
    cubicTo(cx + w * 0.18f, h * 0.63f, cx + w * 0.16f, h * 0.72f, cx + w * 0.13f, h * 0.76f)
    cubicTo(cx + w * 0.14f, h * 0.82f, cx + w * 0.12f, h * 0.88f, cx + w * 0.12f, h * 0.94f)
    lineTo(cx + w * 0.05f, h * 0.94f)
    cubicTo(cx + w * 0.05f, h * 0.88f, cx + w * 0.06f, h * 0.80f, cx + w * 0.05f, h * 0.74f)
    lineTo(cx + w * 0.015f, h * 0.565f)
    close()
  }
  drawPath(rightLegPath, color = bodyFill)
  val isRightLegSelected = selectedBodyPart == BodyPart.RIGHT_LEG || selectedBodyPart == BodyPart.FEET
  drawPath(
    rightLegPath,
    color = if (isRightLegSelected) highlightBorder else bodyBorder,
    style = Stroke(width = if (isRightLegSelected) 2.5.dp.toPx() else 1.5.dp.toPx())
  )
}

/**
 * Zeichnet animierte, leuchtende Wund- und Verletzungs-Beacons an den betroffenen Koerperteilen.
 */
private fun DrawScope.drawInjuryBeacons(
  width: Float,
  height: Float,
  injuries: List<CharacterInjury>,
  selectedBodyPart: BodyPart?,
  pulseRadius: Float,
  pulseAlpha: Float
) {
  for (injury in injuries) {
    val px = injury.bodyPart.xPercent * width
    val py = injury.bodyPart.yPercent * height

    val beaconColor = when (injury.severity) {
      InjurySeverity.CRITICAL -> DnaColors.StatusDestructive
      InjurySeverity.SEVERE -> DnaColors.StatusDestructive
      InjurySeverity.MEDIUM -> DnaColors.StatusWarning
      InjurySeverity.LIGHT -> DnaColors.StatusWarning
    }

    val isSelected = selectedBodyPart == injury.bodyPart

    // Pulsierende aeussere Warnwelle
    drawCircle(
      color = beaconColor.copy(alpha = pulseAlpha),
      center = Offset(px, py),
      radius = (10.dp.toPx() * pulseRadius)
    )

    // Mittlerer Leuchtkranz
    drawCircle(
      color = beaconColor.copy(alpha = 0.35f),
      center = Offset(px, py),
      radius = (if (isSelected) 10.dp.toPx() else 7.dp.toPx())
    )

    // Massiver Kern-Leuchtpunkt
    drawCircle(
      color = beaconColor,
      center = Offset(px, py),
      radius = (if (isSelected) 4.5.dp.toPx() else 3.5.dp.toPx())
    )

    // Zielmarkierungskreuz bei aktiver Selektion
    if (isSelected) {
      val markerLength = 12.dp.toPx()
      drawLine(
        color = DnaColors.Primary,
        start = Offset(px - markerLength, py),
        end = Offset(px + markerLength, py),
        strokeWidth = 1.5.dp.toPx()
      )
      drawLine(
        color = DnaColors.Primary,
        start = Offset(px, py - markerLength),
        end = Offset(px, py + markerLength),
        strokeWidth = 1.5.dp.toPx()
      )
    }
  }
}
