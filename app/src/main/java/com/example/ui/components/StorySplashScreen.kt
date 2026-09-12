package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.dna.DnaColors
import com.example.ui.dna.DnaTypography

/**
 * StorySplashScreen - Der stimmungsvolle Startbildschirm von Maunting Story Fable.
 * Zeigt das kreisrunde Leucht-Emblem mit sanfter Puls-Animation und Design-DNA Typografie.
 */
@Composable
fun StorySplashScreen(
  visible: Boolean,
  onAnimationFinished: () -> Unit = {}
) {
  val scale = remember { Animatable(0.85f) }
  val alpha = remember { Animatable(0f) }

  LaunchedEffect(visible) {
    if (visible) {
      alpha.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
      )
      scale.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
      )
      // Pulse loop
      scale.animateTo(
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
          animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Reverse
        )
      )
    }
  }

  AnimatedVisibility(
    visible = visible,
    enter = fadeIn(animationSpec = tween(300)),
    exit = fadeOut(animationSpec = tween(400))
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(DnaColors.SurfaceDim),
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        // Glowing circular logo container
        Surface(
          shape = CircleShape,
          color = DnaColors.SurfaceContainerHigh,
          border = BorderStroke(2.dp, DnaColors.Primary),
          modifier = Modifier
            .size(140.dp)
            .scale(scale.value)
            .alpha(alpha.value)
        ) {
          Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = "Maunting Story Fable Logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
          text = "Maunting Story Fable",
          style = MaterialTheme.typography.headlineMedium,
          fontFamily = DnaTypography.ManropeFamily,
          fontWeight = FontWeight.Bold,
          color = DnaColors.OnSurface,
          modifier = Modifier.alpha(alpha.value)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "Deine interaktiven Chroniken & Abenteuer",
          style = MaterialTheme.typography.bodyMedium,
          fontFamily = DnaTypography.InterFamily,
          color = DnaColors.OnSurfaceVariant,
          fontSize = 14.sp,
          modifier = Modifier.alpha(alpha.value)
        )

        Spacer(modifier = Modifier.height(32.dp))

        CircularProgressIndicator(
          color = DnaColors.Primary,
          strokeWidth = 2.dp,
          modifier = Modifier
            .size(24.dp)
            .alpha(alpha.value)
        )
      }
    }
  }
}
