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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.BodyOrgan
import com.example.data.model.BodyPart
import com.example.data.model.CharacterGender
import com.example.data.model.CharacterInjury
import com.example.data.model.InjurySeverity
import com.example.ui.dna.DnaColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * Anatomischer, interaktiver Mannequin-Canvas.
 *
 * Aufbau in Schichten statt flacher Silhouette: Grundkörper mit Hautton-Verlauf, darüber die
 * Schattierung der Muskelgruppen, dann Kontur und Rim-Light. Erst dadurch liest sich die Figur
 * als Körper und nicht als Piktogramm.
 *
 * Sämtliche Regionen entstehen einmal als [BodyPaths] und werden für das Zeichnen UND die
 * Trefferprüfung genutzt. Die frühere Prüfung über den nächstgelegenen Mittelpunkt traf bei
 * schmalen Gliedmaßen regelmäßig die falsche Region.
 */
/** Darstellungsmodus des Mannequins. */
enum class MannequinView {
  /** Oberflaeche: Hautton, Muskelzeichnung, Wunden auf der Haut. */
  SURFACE,

  /** Durchleuchtung: Knochen und Organe wie in einem CT/MRT-Schnittbild. */
  SCAN
}

@Composable
fun CharacterMannequin(
  gender: CharacterGender,
  injuries: List<CharacterInjury>,
  selectedBodyPart: BodyPart?,
  onBodyPartSelected: (BodyPart) -> Unit,
  modifier: Modifier = Modifier,
  view: MannequinView = MannequinView.SCAN,
  selectedOrgan: BodyOrgan? = null,
  onOrganSelected: (BodyOrgan) -> Unit = {},
  testTag: String = "character_mannequin"
) {
  val infiniteTransition = rememberInfiniteTransition(label = "injury_pulse_transition")
  // Wandernde Scanlinie. Sie ist der Unterschied zwischen einem Schnittbild und einem
  // Biologie-Schaubild: Erst die Bewegung lässt das Bild wie eine laufende Aufnahme wirken.
  val scanSweep by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 4200, easing = androidx.compose.animation.core.LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "scan_sweep"
  )
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

  // Der Umriss haengt nur von Geschlecht und Groesse ab -- beides aendert sich waehrend einer
  // Animation nicht. Ohne diesen Zwischenspeicher entstanden die rund fuenfzig Path-Objekte in
  // jedem einzelnen Frame neu, und der Scan-Sweep und der Verletzungs-Puls laufen endlos: etwa
  // 3000 weggeworfene Pfade pro Sekunde, solange das Mannequin sichtbar ist.
  val pathCache = remember { BodyPathCache() }

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
        .pointerInput(gender, view) {
          detectTapGestures { tapOffset ->
            val w = size.width.toFloat()
            val h = size.height.toFloat()
            // In der Scan-Ansicht haben Organe Vorrang: Sie liegen innerhalb der Regionen,
            // wären über die Regionenprüfung also nie erreichbar.
            if (view == MannequinView.SCAN) {
              val organ = hitTestOrgan(gender, tapOffset, w, h)
              if (organ != null) {
                onOrganSelected(organ)
                return@detectTapGestures
              }
            }
            pathCache.hole(gender, w, h).hitTest(tapOffset)?.let(onBodyPartSelected)
          }
        }
    ) {
      val w = size.width
      val h = size.height
      val paths = pathCache.hole(gender, w, h)

      drawTechnicalGrid(w, h)

      if (view == MannequinView.SCAN) {
        drawScanLayers(paths, gender, selectedBodyPart, selectedOrgan, injuries, w, h)
        drawScanSweep(scanSweep, w, h)
      } else {
        drawBodyLayers(paths, selectedBodyPart, h)
      }

      drawInjuries(
        paths = paths,
        injuries = injuries,
        selectedBodyPart = selectedBodyPart,
        pulseRadius = pulseRadiusFraction,
        pulseAlpha = pulseAlphaFraction,
        width = w,
        height = h
      )
    }
  }
}

/**
 * Hält den zuletzt gebauten Umriss fest und baut ihn nur neu, wenn Geschlecht oder Größe sich
 * ändern. Bewusst eine Klasse und kein `remember(gender, size)`: Die Größe steht erst im
 * DrawScope fest, und dort gibt es kein remember.
 *
 * Nicht threadsicher, muss es aber auch nicht sein -- Zeichnen und Antippen laufen beide auf
 * dem Hauptthread.
 */
private class BodyPathCache {
  private var gender: CharacterGender? = null
  private var width = 0f
  private var height = 0f
  private var paths: BodyPaths? = null

  fun hole(gender: CharacterGender, width: Float, height: Float): BodyPaths {
    val vorhanden = paths
    if (vorhanden != null && this.gender == gender && this.width == width && this.height == height) {
      return vorhanden
    }
    val neu = BodyPaths.build(gender, width, height)
    this.gender = gender
    this.width = width
    this.height = height
    this.paths = neu
    return neu
  }
}

/**
 * Die anatomischen Regionen einer Figur als Pfade, zusammen mit der Reihenfolge, in der sie
 * gezeichnet und getroffen werden.
 */
private class BodyPaths(
  val regions: List<Region>,
  val muscleLines: List<Path>,
  val width: Float,
  val height: Float
) {
  class Region(val part: BodyPart, val path: Path, val center: Offset)

  /**
   * Trefferprüfung über die echten Pfadflächen. Gliedmaßen werden zuerst geprüft, weil sie im
   * Rumpf-Umriss liegen können.
   */
  fun hitTest(offset: Offset): BodyPart? {
    val clip = android.graphics.Region(0, 0, width.toInt(), height.toInt())
    val x = offset.x.toInt()
    val y = offset.y.toInt()

    for (region in regions) {
      val androidRegion = android.graphics.Region()
      androidRegion.setPath(region.path.asAndroidPath(), clip)
      if (androidRegion.contains(x, y)) return region.part
    }

    // Fallback für Treffer knapp neben dem Körper: nächstgelegene Region innerhalb eines Radius.
    val threshold = width * 0.18f
    return regions
      .map { it to (it.center - offset).getDistance() }
      .filter { it.second < threshold }
      .minByOrNull { it.second }
      ?.first?.part
  }

  fun centerOf(part: BodyPart): Offset =
    regions.firstOrNull { it.part == part }?.center
      ?: Offset(width * part.xPercent, height * part.yPercent)

  companion object {
    fun build(gender: CharacterGender, w: Float, h: Float): BodyPaths {
      val female = gender == CharacterGender.FEMALE
      val cx = w * 0.5f

      // Geschlechtsabhängige Proportionen: Schulterbreite, Taille, Hüfte, Gliedmaßenstärke.
      val shoulder = if (female) 0.175f else 0.225f
      val waist = if (female) 0.115f else 0.145f
      val hip = if (female) 0.165f else 0.150f
      val limb = if (female) 0.042f else 0.052f

      val head = Path().apply {
        moveTo(cx, h * 0.050f)
        cubicTo(cx + w * (if (female) 0.098f else 0.108f), h * 0.052f, cx + w * (if (female) 0.092f else 0.104f), h * 0.142f, cx + w * 0.058f, h * 0.172f)
        cubicTo(cx + w * 0.040f, h * 0.188f, cx - w * 0.040f, h * 0.188f, cx - w * 0.058f, h * 0.172f)
        cubicTo(cx - w * (if (female) 0.092f else 0.104f), h * 0.142f, cx - w * (if (female) 0.098f else 0.108f), h * 0.052f, cx, h * 0.050f)
        close()
      }

      val neck = Path().apply {
        moveTo(cx - w * 0.038f, h * 0.178f)
        lineTo(cx + w * 0.038f, h * 0.178f)
        cubicTo(cx + w * 0.046f, h * 0.205f, cx + w * 0.052f, h * 0.220f, cx + w * 0.060f, h * 0.232f)
        lineTo(cx - w * 0.060f, h * 0.232f)
        cubicTo(cx - w * 0.052f, h * 0.220f, cx - w * 0.046f, h * 0.205f, cx - w * 0.038f, h * 0.178f)
        close()
      }

      // Brustkorb bis Taille
      val chest = Path().apply {
        moveTo(cx - w * shoulder, h * 0.245f)
        cubicTo(cx - w * (shoulder - 0.02f), h * 0.232f, cx + w * (shoulder - 0.02f), h * 0.232f, cx + w * shoulder, h * 0.245f)
        cubicTo(cx + w * (shoulder + 0.005f), h * 0.300f, cx + w * (waist + 0.03f), h * 0.345f, cx + w * waist, h * 0.390f)
        lineTo(cx - w * waist, h * 0.390f)
        cubicTo(cx - w * (waist + 0.03f), h * 0.345f, cx - w * (shoulder + 0.005f), h * 0.300f, cx - w * shoulder, h * 0.245f)
        close()
      }

      // Bauch bis Hüfte
      val abdomen = Path().apply {
        moveTo(cx - w * waist, h * 0.390f)
        lineTo(cx + w * waist, h * 0.390f)
        cubicTo(cx + w * (hip - 0.01f), h * 0.450f, cx + w * hip, h * 0.500f, cx + w * (hip - 0.015f), h * 0.545f)
        cubicTo(cx + w * 0.08f, h * 0.565f, cx - w * 0.08f, h * 0.565f, cx - w * (hip - 0.015f), h * 0.545f)
        cubicTo(cx - w * hip, h * 0.500f, cx - w * (hip - 0.01f), h * 0.450f, cx - w * waist, h * 0.390f)
        close()
      }

      val genitals = Path().apply {
        moveTo(cx - w * 0.046f, h * 0.548f)
        cubicTo(cx - w * 0.030f, h * 0.585f, cx + w * 0.030f, h * 0.585f, cx + w * 0.046f, h * 0.548f)
        cubicTo(cx + w * 0.020f, h * 0.602f, cx - w * 0.020f, h * 0.602f, cx - w * 0.046f, h * 0.548f)
        close()
      }

      fun arm(sign: Float): Path = Path().apply {
        val s = sign
        moveTo(cx + s * w * (shoulder - 0.015f), h * 0.248f)
        // Deltoid nach außen, dann Oberarm
        cubicTo(cx + s * w * (shoulder + 0.045f), h * 0.268f, cx + s * w * (shoulder + 0.040f), h * 0.330f, cx + s * w * (shoulder + 0.018f), h * 0.380f)
        // Ellenbogen und Unterarm
        cubicTo(cx + s * w * (shoulder + 0.010f), h * 0.420f, cx + s * w * (shoulder + 0.012f), h * 0.460f, cx + s * w * (shoulder + 0.004f), h * 0.498f)
        lineTo(cx + s * w * (shoulder - 0.030f), h * 0.492f)
        cubicTo(cx + s * w * (shoulder - 0.028f), h * 0.440f, cx + s * w * (shoulder - 0.036f), h * 0.360f, cx + s * w * (shoulder - 0.040f), h * 0.300f)
        close()
      }

      fun hand(sign: Float): Path = Path().apply {
        val s = sign
        val hx = cx + s * w * (shoulder - 0.012f)
        moveTo(hx - s * w * 0.022f, h * 0.494f)
        cubicTo(hx + s * w * 0.030f, h * 0.500f, hx + s * w * 0.032f, h * 0.542f, hx + s * w * 0.010f, h * 0.552f)
        cubicTo(hx - s * w * 0.014f, h * 0.560f, hx - s * w * 0.030f, h * 0.536f, hx - s * w * 0.026f, h * 0.505f)
        close()
      }

      fun leg(sign: Float): Path = Path().apply {
        val s = sign
        moveTo(cx + s * w * (hip - 0.015f), h * 0.545f)
        // Außenlinie: Oberschenkel, Knie, Wade
        cubicTo(cx + s * w * (limb + 0.105f), h * 0.620f, cx + s * w * (limb + 0.085f), h * 0.700f, cx + s * w * (limb + 0.062f), h * 0.742f)
        cubicTo(cx + s * w * (limb + 0.072f), h * 0.800f, cx + s * w * (limb + 0.050f), h * 0.860f, cx + s * w * (limb + 0.022f), h * 0.900f)
        lineTo(cx + s * w * 0.022f, h * 0.900f)
        // Innenlinie zurück zum Schritt
        cubicTo(cx + s * w * 0.030f, h * 0.840f, cx + s * w * 0.034f, h * 0.760f, cx + s * w * 0.030f, h * 0.700f)
        cubicTo(cx + s * w * 0.026f, h * 0.640f, cx + s * w * 0.020f, h * 0.590f, cx + s * w * 0.014f, h * 0.560f)
        close()
      }

      val feet = Path().apply {
        // Beide Füße als eine Region - die Anatomie unterscheidet sie ohnehin nicht.
        moveTo(cx - w * (limb + 0.024f), h * 0.900f)
        lineTo(cx - w * 0.020f, h * 0.900f)
        cubicTo(cx - w * 0.020f, h * 0.940f, cx - w * 0.030f, h * 0.960f, cx - w * (limb + 0.046f), h * 0.958f)
        cubicTo(cx - w * (limb + 0.060f), h * 0.950f, cx - w * (limb + 0.040f), h * 0.924f, cx - w * (limb + 0.024f), h * 0.900f)
        close()
        moveTo(cx + w * (limb + 0.024f), h * 0.900f)
        lineTo(cx + w * 0.020f, h * 0.900f)
        cubicTo(cx + w * 0.020f, h * 0.940f, cx + w * 0.030f, h * 0.960f, cx + w * (limb + 0.046f), h * 0.958f)
        cubicTo(cx + w * (limb + 0.060f), h * 0.950f, cx + w * (limb + 0.040f), h * 0.924f, cx + w * (limb + 0.024f), h * 0.900f)
        close()
      }

      // Reihenfolge = Trefferpriorität: Kleinteiliges vor Großflächigem.
      val regions = listOf(
        Region(BodyPart.HEAD, head, Offset(cx, h * 0.115f)),
        Region(BodyPart.NECK, neck, Offset(cx, h * 0.205f)),
        Region(BodyPart.LEFT_HAND, hand(-1f), Offset(cx - w * (shoulder + 0.004f), h * 0.524f)),
        Region(BodyPart.RIGHT_HAND, hand(1f), Offset(cx + w * (shoulder + 0.004f), h * 0.524f)),
        Region(BodyPart.LEFT_ARM, arm(-1f), Offset(cx - w * (shoulder + 0.015f), h * 0.360f)),
        Region(BodyPart.RIGHT_ARM, arm(1f), Offset(cx + w * (shoulder + 0.015f), h * 0.360f)),
        Region(BodyPart.FEET, feet, Offset(cx, h * 0.930f)),
        Region(BodyPart.GENITALS, genitals, Offset(cx, h * 0.568f)),
        Region(BodyPart.LEFT_LEG, leg(-1f), Offset(cx - w * 0.075f, h * 0.720f)),
        Region(BodyPart.RIGHT_LEG, leg(1f), Offset(cx + w * 0.075f, h * 0.720f)),
        Region(BodyPart.CHEST, chest, Offset(cx, h * 0.305f)),
        Region(BodyPart.ABDOMEN, abdomen, Offset(cx, h * 0.455f))
      )

      return BodyPaths(regions, buildMuscleLines(cx, w, h, female, waist), w, h)
    }

    /**
     * Feine Binnenzeichnung: Schlüsselbein, Brustansatz, Bauchlinien, Kniescheiben.
     * Sie trägt den größten Teil des plastischen Eindrucks.
     */
    private fun buildMuscleLines(cx: Float, w: Float, h: Float, female: Boolean, waist: Float): List<Path> {
      val lines = mutableListOf<Path>()

      // Schlüsselbein
      lines.add(
        Path().apply {
          moveTo(cx - w * 0.125f, h * 0.256f)
          cubicTo(cx - w * 0.070f, h * 0.270f, cx - w * 0.022f, h * 0.266f, cx, h * 0.262f)
          cubicTo(cx + w * 0.022f, h * 0.266f, cx + w * 0.070f, h * 0.270f, cx + w * 0.125f, h * 0.256f)
        }
      )

      // Brustansatz: bei weiblicher Anatomie geschwungen, sonst Pectoralis-Kante
      if (female) {
        lines.add(
          Path().apply {
            moveTo(cx - w * 0.105f, h * 0.288f)
            cubicTo(cx - w * 0.085f, h * 0.340f, cx - w * 0.020f, h * 0.338f, cx, h * 0.312f)
            cubicTo(cx + w * 0.020f, h * 0.338f, cx + w * 0.085f, h * 0.340f, cx + w * 0.105f, h * 0.288f)
          }
        )
      } else {
        lines.add(
          Path().apply {
            moveTo(cx - w * 0.130f, h * 0.292f)
            cubicTo(cx - w * 0.090f, h * 0.330f, cx - w * 0.030f, h * 0.332f, cx, h * 0.326f)
            cubicTo(cx + w * 0.030f, h * 0.332f, cx + w * 0.090f, h * 0.330f, cx + w * 0.130f, h * 0.292f)
          }
        )
      }

      // Mittellinie des Bauches
      lines.add(
        Path().apply {
          moveTo(cx, h * 0.335f)
          lineTo(cx, h * 0.500f)
        }
      )

      // Rectus-Abdominis-Querlinien
      for (y in listOf(0.378f, 0.418f, 0.458f)) {
        lines.add(
          Path().apply {
            moveTo(cx - w * (waist - 0.035f), h * y)
            lineTo(cx + w * (waist - 0.035f), h * y)
          }
        )
      }

      // Kniescheiben
      for (sign in listOf(-1f, 1f)) {
        lines.add(
          Path().apply {
            moveTo(cx + sign * w * 0.088f, h * 0.735f)
            cubicTo(cx + sign * w * 0.060f, h * 0.752f, cx + sign * w * 0.042f, h * 0.750f, cx + sign * w * 0.036f, h * 0.732f)
          }
        )
      }

      return lines
    }
  }
}

// ---------------------------------------------------------------------------------------------
// Scan-Ansicht
// ---------------------------------------------------------------------------------------------

/** Mittelpunkt eines Organs auf der Zeichenfläche. */
private fun organCenter(organ: BodyOrgan, w: Float, h: Float): Offset =
  Offset(w * organ.xPercent, h * organ.yPercent)

/** Umriss eines Organs als Oval. Der Darm bekommt seine Schlingen separat. */
private fun organPath(organ: BodyOrgan, w: Float, h: Float): Path {
  val center = organCenter(organ, w, h)
  val rx = w * organ.widthPercent * 0.5f
  val ry = h * organ.heightPercent * 0.5f
  return Path().apply {
    addOval(
      androidx.compose.ui.geometry.Rect(
        left = center.x - rx,
        top = center.y - ry,
        right = center.x + rx,
        bottom = center.y + ry
      )
    )
  }
}

/**
 * Trefferprüfung für Organe. Geprüft wird von vorn nach hinten, damit das obenauf liegende
 * Organ gewinnt — beim Herz vor der Lunge ist das entscheidend.
 */
private fun hitTestOrgan(gender: CharacterGender, offset: Offset, w: Float, h: Float): BodyOrgan? =
  BodyOrgan.forGender(gender)
    .sortedByDescending { it.depth }
    .firstOrNull { organ ->
      val center = organCenter(organ, w, h)
      val rx = w * organ.widthPercent * 0.5f
      val ry = h * organ.heightPercent * 0.5f
      if (rx <= 0f || ry <= 0f) return@firstOrNull false
      val dx = (offset.x - center.x) / rx
      val dy = (offset.y - center.y) / ry
      dx * dx + dy * dy <= 1f
    }

/**
 * Die Durchleuchtungs-Ansicht: Gewebe, Knochen, Organe.
 *
 * Der Unterschied zu einer Lehrbuch-Grafik liegt in der Dichte-Logik statt in bunten Flächen:
 * Gewebe bleibt fast durchsichtig, Knochen sind hell (hohe Dichte), Organe liegen als mittlere
 * Graustufen dazwischen und werden nach ihrer Tiefe übereinandergelegt.
 */
private fun DrawScope.drawScanLayers(
  paths: BodyPaths,
  gender: CharacterGender,
  selectedBodyPart: BodyPart?,
  selectedOrgan: BodyOrgan?,
  injuries: List<CharacterInjury>,
  w: Float,
  h: Float
) {
  // 1. Weichteilgewebe: kaum sichtbar, gibt nur den Umriss.
  for (region in paths.regions) {
    drawPath(
      path = region.path,
      brush = Brush.verticalGradient(
        colors = listOf(
          DnaColors.SurfaceContainerHigh.copy(alpha = 0.55f),
          DnaColors.SurfaceContainer.copy(alpha = 0.40f)
        ),
        startY = 0f,
        endY = h
      )
    )
    drawPath(
      path = region.path,
      color = DnaColors.Ring.copy(alpha = if (region.part == selectedBodyPart) 0.75f else 0.20f),
      style = Stroke(width = if (region.part == selectedBodyPart) 2.2.dp.toPx() else 1.dp.toPx())
    )
  }

  // 2. Knochen: das hellste Element, wie hohe Röntgendichte.
  val boneColor = DnaColors.Tertiary.copy(alpha = 0.80f)
  for (bone in buildBonePaths(gender, w, h)) {
    drawPath(bone, color = boneColor, style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round))
    drawPath(bone, color = DnaColors.Primary.copy(alpha = 0.18f), style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round))
  }

  // 3. Organe von hinten nach vorn.
  val affectedOrgans = injuries.mapNotNull { it.organ }.toSet()
  for (organ in BodyOrgan.forGender(gender)) {
    val isSelected = organ == selectedOrgan
    val isHurt = organ in affectedOrgans
    val center = organCenter(organ, w, h)

    val body = if (organ == BodyOrgan.INTESTINES) {
      intestinePath(center, w * organ.widthPercent, h * organ.heightPercent)
    } else {
      organPath(organ, w, h)
    }

    // Mittlere Graustufe: heller als das Weichteilgewebe, dunkler als der Knochen. Genau diese
    // Dichte-Staffelung macht den Unterschied zum bunten Lehrbuchbild.
    val fill = when {
      isHurt -> DnaColors.StoryBloodCrimson.copy(alpha = 0.60f)
      isSelected -> DnaColors.Primary.copy(alpha = 0.55f)
      else -> DnaColors.OnSurfaceVariant.copy(alpha = 0.42f)
    }

    if (organ == BodyOrgan.INTESTINES) {
      drawPath(body, color = fill, style = Stroke(width = w * 0.013f, cap = StrokeCap.Round))
    } else {
      drawPath(
        path = body,
        brush = Brush.radialGradient(
          colors = listOf(fill, fill.copy(alpha = fill.alpha * 0.45f)),
          center = Offset(center.x - w * organ.widthPercent * 0.18f, center.y - h * organ.heightPercent * 0.18f),
          radius = maxOf(w * organ.widthPercent, h * organ.heightPercent)
        )
      )
      drawPath(
        path = body,
        color = when {
          isHurt -> DnaColors.StoryBloodCrimson
          isSelected -> DnaColors.Primary
          else -> DnaColors.OnSurfaceVariant.copy(alpha = 0.85f)
        },
        style = Stroke(width = if (isSelected || isHurt) 2.dp.toPx() else 1.dp.toPx())
      )
    }

    if (isSelected) {
      drawCircle(
        color = DnaColors.Primary.copy(alpha = 0.55f),
        radius = maxOf(w * organ.widthPercent, h * organ.heightPercent) * 0.85f,
        center = center,
        style = Stroke(width = 1.4.dp.toPx())
      )
    }
  }
}

/**
 * Die Darmschlingen als durchgehender Mäander — ein Oval sähe hier falsch aus.
 *
 * Drei Windungen, nicht mehr: Bei vier lagen die Bahnen enger beieinander als die Strichbreite,
 * sodass der Darm im Bild zu einem geschlossenen Block verschmolz.
 */
private fun intestinePath(center: Offset, width: Float, height: Float): Path = Path().apply {
  val left = center.x - width * 0.5f
  val right = center.x + width * 0.5f
  val top = center.y - height * 0.5f
  val rows = 3
  val rowHeight = height / rows

  moveTo(left, top + rowHeight * 0.5f)
  for (row in 0 until rows) {
    val y = top + rowHeight * (row + 0.5f)
    val goRight = row % 2 == 0
    val startX = if (goRight) left else right
    val endX = if (goRight) right else left

    // Bogen quer durch den Bauchraum
    cubicTo(
      startX + (endX - startX) * 0.3f, y - rowHeight * 0.32f,
      startX + (endX - startX) * 0.7f, y + rowHeight * 0.32f,
      endX, y
    )
    // Kehre am Rand: halbrund nach unten in die nächste Bahn
    if (row < rows - 1) {
      val nextY = y + rowHeight
      val bulge = (endX - startX) * 0.12f
      cubicTo(endX + bulge, y + rowHeight * 0.34f, endX + bulge, nextY - rowHeight * 0.34f, endX, nextY)
    }
  }
}

/**
 * Knochen als Linienzug. Bewusst schematisch: Ein Schnittbild zeigt Achsen und Dichte, keine
 * ausmodellierten Knochenkörper — und mehr Detail wäre auf 250 dp ohnehin nicht lesbar.
 */
private fun buildBonePaths(gender: CharacterGender, w: Float, h: Float): List<Path> {
  val female = gender == CharacterGender.FEMALE
  val cx = w * 0.5f
  val shoulder = if (female) 0.175f else 0.225f
  val bones = mutableListOf<Path>()

  // Schädel
  bones.add(
    Path().apply {
      addOval(
        androidx.compose.ui.geometry.Rect(
          cx - w * 0.062f, h * 0.058f, cx + w * 0.062f, h * 0.168f
        )
      )
    }
  )

  // Wirbelsäule vom Nacken bis zum Becken
  bones.add(
    Path().apply {
      moveTo(cx, h * 0.180f)
      lineTo(cx, h * 0.520f)
    }
  )

  // Schlüsselbeine
  bones.add(
    Path().apply {
      moveTo(cx - w * (shoulder - 0.04f), h * 0.258f)
      cubicTo(cx - w * 0.070f, h * 0.272f, cx - w * 0.020f, h * 0.266f, cx, h * 0.262f)
      cubicTo(cx + w * 0.020f, h * 0.266f, cx + w * 0.070f, h * 0.272f, cx + w * (shoulder - 0.04f), h * 0.258f)
    }
  )

  // Rippenbogen: je Seite drei Paare
  for (sign in listOf(-1f, 1f)) {
    for ((index, y) in listOf(0.288f, 0.316f, 0.344f).withIndex()) {
      val reach = (0.118f - index * 0.010f)
      bones.add(
        Path().apply {
          moveTo(cx, h * y)
          cubicTo(
            cx + sign * w * reach * 0.55f, h * (y - 0.004f),
            cx + sign * w * reach, h * (y + 0.014f),
            cx + sign * w * reach * 0.82f, h * (y + 0.034f)
          )
        }
      )
    }
  }

  // Becken
  bones.add(
    Path().apply {
      moveTo(cx - w * 0.108f, h * 0.505f)
      cubicTo(cx - w * 0.092f, h * 0.548f, cx - w * 0.040f, h * 0.562f, cx, h * 0.556f)
      cubicTo(cx + w * 0.040f, h * 0.562f, cx + w * 0.092f, h * 0.548f, cx + w * 0.108f, h * 0.505f)
    }
  )

  // Arm- und Beinknochen als Längsachsen
  for (sign in listOf(-1f, 1f)) {
    bones.add(
      Path().apply {
        moveTo(cx + sign * w * (shoulder - 0.005f), h * 0.262f)
        lineTo(cx + sign * w * (shoulder + 0.012f), h * 0.382f)
        lineTo(cx + sign * w * (shoulder - 0.010f), h * 0.492f)
      }
    )
    bones.add(
      Path().apply {
        moveTo(cx + sign * w * 0.060f, h * 0.552f)
        lineTo(cx + sign * w * 0.072f, h * 0.738f)
        lineTo(cx + sign * w * 0.048f, h * 0.898f)
      }
    )
  }

  return bones
}

/** Die wandernde Aufnahmelinie samt Nachleuchten. */
private fun DrawScope.drawScanSweep(progress: Float, w: Float, h: Float) {
  val y = h * (0.03f + progress * 0.94f)
  val glow = h * 0.05f

  drawRect(
    brush = Brush.verticalGradient(
      colors = listOf(
        Color.Transparent,
        DnaColors.Ring.copy(alpha = 0.10f),
        Color.Transparent
      ),
      startY = y - glow,
      endY = y + glow
    ),
    topLeft = Offset(0f, y - glow),
    size = Size(w, glow * 2f)
  )
  drawLine(
    color = DnaColors.Ring.copy(alpha = 0.40f),
    start = Offset(0f, y),
    end = Offset(w, y),
    strokeWidth = 1.dp.toPx()
  )
}

/**
 * Zeichnet die Körperschichten: Hautton-Verlauf, Muskelschattierung, Kontur, Rim-Light.
 */
private fun DrawScope.drawBodyLayers(
  paths: BodyPaths,
  selectedBodyPart: BodyPart?,
  height: Float
) {
  // Hautton als vertikaler Verlauf - oben angeleuchtet, unten im Schatten.
  val skinBrush = Brush.verticalGradient(
    colors = listOf(
      DnaColors.SurfaceBright,
      DnaColors.SurfaceContainerHighest,
      DnaColors.SurfaceVariant,
      DnaColors.SurfaceContainerHigh
    ),
    startY = 0f,
    endY = height
  )

  for (region in paths.regions) {
    drawPath(region.path, brush = skinBrush)
  }

  // Volumen: weiches Licht von links oben auf jede Region.
  for (region in paths.regions) {
    val bounds = region.path.getBounds()
    if (bounds.width <= 0f || bounds.height <= 0f) continue
    val radius = maxOf(bounds.width, bounds.height) * 0.85f
    drawPath(
      path = region.path,
      brush = Brush.radialGradient(
        colors = listOf(
          DnaColors.Primary.copy(alpha = 0.10f),
          Color.Transparent,
          DnaColors.SurfaceContainerLowest.copy(alpha = 0.55f)
        ),
        center = Offset(bounds.left + bounds.width * 0.34f, bounds.top + bounds.height * 0.26f),
        radius = radius
      )
    )
  }

  // Binnenzeichnung der Muskelgruppen.
  for (line in paths.muscleLines) {
    drawPath(
      path = line,
      color = DnaColors.SurfaceContainerLowest.copy(alpha = 0.6f),
      style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
    )
  }

  // Kontur, ausgewählte Region hervorgehoben.
  for (region in paths.regions) {
    val selected = region.part == selectedBodyPart
    drawPath(
      path = region.path,
      color = if (selected) DnaColors.Primary else DnaColors.Border.copy(alpha = 0.85f),
      style = Stroke(width = if (selected) 2.4.dp.toPx() else 1.2.dp.toPx())
    )
  }

  // Rim-Light: dünne kalte Kante, die den Körper vom Hintergrund löst.
  for (region in paths.regions) {
    drawPath(
      path = region.path,
      brush = Brush.horizontalGradient(
        colors = listOf(
          DnaColors.Ring.copy(alpha = 0.22f),
          Color.Transparent,
          Color.Transparent
        )
      ),
      style = Stroke(width = 2.dp.toPx())
    )
  }
}

/**
 * Zeichnet Wunden als schweregrad-abhängige Flecken statt als gleichförmige Punkte.
 * Behandelte Wunden bekommen eine Bandage; unbehandelte pulsieren.
 */
private fun DrawScope.drawInjuries(
  paths: BodyPaths,
  injuries: List<CharacterInjury>,
  selectedBodyPart: BodyPart?,
  pulseRadius: Float,
  pulseAlpha: Float,
  width: Float,
  height: Float
) {
  for (injury in injuries) {
    // Innere Verletzungen sitzen am Organ, nicht in der Mitte der Körperregion.
    val center = injury.organ?.let { organCenter(it, width, height) }
      ?: paths.centerOf(injury.bodyPart)
    val baseRadius = width * when (injury.severity) {
      InjurySeverity.LIGHT -> 0.026f
      InjurySeverity.MEDIUM -> 0.036f
      InjurySeverity.SEVERE -> 0.048f
      InjurySeverity.CRITICAL -> 0.060f
    }
    val woundColor = when (injury.severity) {
      InjurySeverity.LIGHT -> DnaColors.StatusWarning
      InjurySeverity.MEDIUM -> DnaColors.StatusDestructive
      InjurySeverity.SEVERE, InjurySeverity.CRITICAL -> DnaColors.StoryBloodCrimson
    }

    // Unregelmäßiger Wundfleck: ein verzerrter Kreis wirkt organisch, ein exakter nicht.
    val blotch = Path().apply {
      val points = 11
      for (i in 0 until points) {
        val angle = (i.toFloat() / points) * 2f * Math.PI.toFloat()
        // Deterministische Verzerrung - derselbe Wundeintrag sieht immer gleich aus.
        val wobble = 0.72f + 0.28f * kotlin.math.abs(sin(angle * 3f + injury.id.hashCode() % 7))
        val r = baseRadius * wobble
        val px = center.x + cos(angle) * r
        val py = center.y + sin(angle) * r
        if (i == 0) moveTo(px, py) else lineTo(px, py)
      }
      close()
    }

    if (!injury.isTreated) {
      // Pulsierender Hof nur bei offenen Wunden.
      drawCircle(
        color = woundColor.copy(alpha = pulseAlpha * 0.45f),
        radius = baseRadius * pulseRadius,
        center = center
      )
    }

    drawPath(
      path = blotch,
      brush = Brush.radialGradient(
        colors = listOf(woundColor, woundColor.copy(alpha = 0.35f)),
        center = center,
        radius = baseRadius * 1.4f
      )
    )
    drawPath(
      path = blotch,
      color = woundColor.copy(alpha = 0.9f),
      style = Stroke(width = 1.dp.toPx())
    )

    if (injury.isTreated) {
      drawBandage(center, baseRadius)
    }

    if (injury.bodyPart == selectedBodyPart) {
      drawCircle(
        color = DnaColors.Primary,
        radius = baseRadius * 1.9f,
        center = center,
        style = Stroke(width = 1.6.dp.toPx())
      )
    }
  }
}

/** Kreuzverband über einer versorgten Wunde. */
private fun DrawScope.drawBandage(center: Offset, radius: Float) {
  val gauze = DnaColors.OnSurface.copy(alpha = 0.82f)
  val strokeWidth = radius * 0.42f

  drawLine(
    color = gauze,
    start = Offset(center.x - radius * 1.3f, center.y - radius * 0.55f),
    end = Offset(center.x + radius * 1.3f, center.y + radius * 0.55f),
    strokeWidth = strokeWidth,
    cap = StrokeCap.Round
  )
  drawLine(
    color = gauze,
    start = Offset(center.x - radius * 1.3f, center.y + radius * 0.55f),
    end = Offset(center.x + radius * 1.3f, center.y - radius * 0.55f),
    strokeWidth = strokeWidth,
    cap = StrokeCap.Round
  )
}

/**
 * Zarte technische Hilfslinien im Hintergrund - Maunting-Vermessungsraster.
 */
private fun DrawScope.drawTechnicalGrid(w: Float, h: Float) {
  val gridColor = DnaColors.BorderFaint.copy(alpha = 0.5f)

  drawLine(
    color = gridColor,
    start = Offset(w * 0.5f, h * 0.04f),
    end = Offset(w * 0.5f, h * 0.96f),
    strokeWidth = 1.dp.toPx()
  )

  for (lvl in listOf(0.24f, 0.44f, 0.72f)) {
    drawLine(
      color = gridColor,
      start = Offset(w * 0.15f, h * lvl),
      end = Offset(w * 0.85f, h * lvl),
      strokeWidth = 1.dp.toPx()
    )
  }

  // Eckmarken als dezenter Messrahmen
  val tick = w * 0.04f
  for ((x, y) in listOf(
    Offset(w * 0.08f, h * 0.06f) to Size(1f, 1f),
    Offset(w * 0.92f, h * 0.06f) to Size(-1f, 1f),
    Offset(w * 0.08f, h * 0.94f) to Size(1f, -1f),
    Offset(w * 0.92f, h * 0.94f) to Size(-1f, -1f)
  )) {
    drawLine(gridColor, x, Offset(x.x + tick * y.width, x.y), strokeWidth = 1.dp.toPx())
    drawLine(gridColor, x, Offset(x.x, x.y + tick * y.height), strokeWidth = 1.dp.toPx())
  }
}
