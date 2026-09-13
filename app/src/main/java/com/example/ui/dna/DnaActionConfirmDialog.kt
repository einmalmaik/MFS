package com.example.ui.dna

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

/**
 * Zulaessige Varianten fuer Bestaetigungsdialoge.
 */
enum class DnaConfirmVariant {
  PRIMARY,
  DESTRUCTIVE
}

/**
 * Zentraler Sicherheits-Bestaetigungsdialog des MauntingStudios Design-Systems.
 *
 * Folgt dem Grundsatz: "Sicherheit braucht Vertrauen / Schutz braucht Vertrauen".
 * Keine destruktiven oder sicherheitsrelevanten Operationen ohne explizite,
 * unmissverstaendliche Bestaetigung mit binaerer Entscheidungsstruktur:
 * - Positiver Button: z. B. "Ja, löschen" oder "Ja, zurücksetzen"
 * - Negativer Button: "Nein, abbrechen"
 */
@Composable
fun DnaActionConfirmDialog(
  title: String,
  message: String = "",
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
  confirmButtonText: String = "Ja, ausführen",
  dismissButtonText: String = "Nein, abbrechen",
  variant: DnaConfirmVariant = DnaConfirmVariant.DESTRUCTIVE,
  icon: ImageVector? = null,
  testTag: String = "dna_confirm_dialog",
  description: String? = null,
  cancelButtonText: String? = null,
  isDestructive: Boolean? = null
) {
  val resolvedMessage = description ?: message
  val resolvedDismissText = cancelButtonText ?: dismissButtonText
  val resolvedVariant = when {
    isDestructive == false -> DnaConfirmVariant.PRIMARY
    isDestructive == true -> DnaConfirmVariant.DESTRUCTIVE
    else -> variant
  }

  val dialogIcon = icon ?: when (resolvedVariant) {
    DnaConfirmVariant.DESTRUCTIVE -> Icons.Default.Warning
    DnaConfirmVariant.PRIMARY -> Icons.Outlined.Shield
  }

  val accentColor = when (resolvedVariant) {
    DnaConfirmVariant.DESTRUCTIVE -> DnaColors.StatusDestructive
    DnaConfirmVariant.PRIMARY -> DnaColors.Primary
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = DnaColors.SurfaceContainer,
      border = BorderStroke(1.dp, DnaColors.Border),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag(testTag)
    ) {
      Column(
        modifier = Modifier.padding(20.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.Top
        ) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = accentColor.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
            modifier = Modifier.size(42.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = dialogIcon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
              )
            }
          }

          Spacer(modifier = Modifier.width(14.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = title,
              fontFamily = DnaTypography.ManropeFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = 18.sp,
              color = DnaColors.OnSurface
            )

            if (resolvedMessage.isNotBlank()) {
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = resolvedMessage,
                fontFamily = DnaTypography.InterFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = DnaColors.OnSurfaceVariant
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hairline divider (border-t border-border/40)
        HorizontalDivider(
          color = DnaColors.BorderFaint,
          thickness = 1.dp,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          DnaButton(
            text = resolvedDismissText,
            onClick = onDismiss,
            variant = DnaButtonVariant.GHOST,
            modifier = Modifier.padding(end = 8.dp),
            testTag = "dna_confirm_dismiss"
          )

          DnaButton(
            text = confirmButtonText,
            onClick = onConfirm,
            variant = if (resolvedVariant == DnaConfirmVariant.DESTRUCTIVE) DnaButtonVariant.DESTRUCTIVE else DnaButtonVariant.PRIMARY,
            testTag = "dna_confirm_accept"
          )
        }
      }
    }
  }
}
