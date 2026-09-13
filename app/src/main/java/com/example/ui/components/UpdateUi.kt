package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.UpdateRelease
import com.example.ui.dna.DnaButton
import com.example.ui.dna.DnaButtonVariant
import com.example.ui.dna.DnaCard
import com.example.ui.dna.DnaColors
import com.example.ui.dna.DnaTypography

/**
 * Die schmale Leiste, die eine verfügbare Fassung meldet.
 *
 * Bewusst nicht-modal: Wer mitten in einer Szene ist, soll nicht von einem Dialog
 * herausgerissen werden. Das Kreuz schiebt sie bis zum nächsten Start weg.
 */
@Composable
fun UpdateHinweisLeiste(
  versionName: String,
  onAnsehen: () -> Unit,
  onVerwerfen: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .background(DnaColors.SurfaceContainerHigh)
      .padding(horizontal = 14.dp, vertical = 8.dp)
      .testTag("update_notice_bar"),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = Icons.Default.Download,
      contentDescription = null,
      tint = DnaColors.Primary,
      modifier = Modifier.size(18.dp)
    )
    Spacer(modifier = Modifier.width(10.dp))
    Text(
      text = "Version $versionName verfügbar",
      style = MaterialTheme.typography.bodySmall,
      fontFamily = DnaTypography.InterFamily,
      color = DnaColors.OnSurface,
      modifier = Modifier.weight(1f)
    )
    DnaButton(
      text = "Ansehen",
      onClick = onAnsehen,
      variant = DnaButtonVariant.GHOST,
      testTag = "update_show_button"
    )
    IconButton(onClick = onVerwerfen, modifier = Modifier.testTag("update_dismiss_button")) {
      Icon(
        imageVector = Icons.Default.Close,
        contentDescription = "Hinweis ausblenden",
        tint = DnaColors.OnSurfaceVariant,
        modifier = Modifier.size(18.dp)
      )
    }
  }
}

/**
 * Der Dialog, in dem der Spieler entscheidet.
 *
 * Geladen wird erst hier und nur auf Tastendruck — nicht beim Start, nicht im Hintergrund.
 * Ein stiller Download verbrauchte fremdes Mobilfunkvolumen und wäre genau das unbemerkte
 * Handeln, das MSF vermeiden will.
 */
@Composable
fun UpdateDialog(
  release: UpdateRelease,
  installierteVersion: String,
  fortschritt: Float?,
  bereit: Boolean,
  darfInstallieren: Boolean,
  onLaden: () -> Unit,
  onInstallieren: () -> Unit,
  onFreigabeOeffnen: () -> Unit,
  onUeberspringen: () -> Unit,
  onAbbrechen: () -> Unit,
  onSpaeter: () -> Unit
) {
  Dialog(onDismissRequest = onSpaeter) {
    DnaCard(
      modifier = Modifier.fillMaxWidth().testTag("update_dialog"),
      backgroundColor = DnaColors.SurfaceContainerHigh
    ) {
      Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text(
          text = "Aktualisierung verfügbar",
          style = MaterialTheme.typography.titleMedium,
          fontFamily = DnaTypography.ManropeFamily,
          fontWeight = FontWeight.Bold,
          color = DnaColors.OnSurface
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "$installierteVersion  →  ${release.versionName}",
          style = DnaTypography.MonoMedium
        )

        if (release.sizeBytes > 0) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "%.1f MB".format(release.sizeBytes / 1_048_576f),
            style = DnaTypography.MonoSmall
          )
        }

        if (release.notes.isNotBlank()) {
          Spacer(modifier = Modifier.height(14.dp))
          Text(
            text = release.notes.trim().take(600),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = DnaTypography.InterFamily,
            color = DnaColors.OnSurfaceVariant
          )
        }

        if (fortschritt != null) {
          Spacer(modifier = Modifier.height(16.dp))
          LinearProgressIndicator(
            progress = { fortschritt },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = DnaColors.Primary,
            trackColor = DnaColors.SurfaceContainerLow
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(text = "${(fortschritt * 100).toInt()} %", style = DnaTypography.MonoSmall)
        }

        if (bereit && !darfInstallieren) {
          Spacer(modifier = Modifier.height(14.dp))
          Text(
            // Die Hürde gehört dem System. Sie vorher zu erklären ist besser, als den Spieler
            // in eine Systemmeldung laufen zu lassen, die er nicht einordnen kann.
            text = "Android muss MSF einmalig erlauben, Apps zu installieren. Ohne diese " +
              "Freigabe bricht der nächste Schritt ohne Erklärung ab.",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = DnaTypography.InterFamily,
            color = DnaColors.StatusWarning
          )
          Spacer(modifier = Modifier.height(10.dp))
          DnaButton(
            text = "Freigabe öffnen",
            onClick = onFreigabeOeffnen,
            variant = DnaButtonVariant.SECONDARY,
            fullWidth = true,
            testTag = "update_permission_button"
          )
        }

        Spacer(modifier = Modifier.height(18.dp))

        when {
          bereit -> DnaButton(
            text = "Installieren",
            onClick = onInstallieren,
            variant = DnaButtonVariant.PRIMARY,
            fullWidth = true,
            enabled = darfInstallieren,
            testTag = "update_install_button"
          )
          // Bisher lag hier onSpaeter -- das schloss nur das Fenster, waehrend der Download
          // weiterlief und weiter Mobilfunkvolumen verbrauchte. Abschnitt 8 der Erklaerung sagt
          // zu, dass nichts im Hintergrund laeuft; diese Schaltflaeche muss das auch einloesen.
          fortschritt != null -> DnaButton(
            text = "Abbrechen",
            onClick = onAbbrechen,
            variant = DnaButtonVariant.GHOST,
            fullWidth = true,
            testTag = "update_cancel_button"
          )
          else -> DnaButton(
            text = "Herunterladen",
            onClick = onLaden,
            variant = DnaButtonVariant.PRIMARY,
            fullWidth = true,
            testTag = "update_download_button"
          )
        }

        if (fortschritt == null && !bereit) {
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            DnaButton(
              text = "Später",
              onClick = onSpaeter,
              variant = DnaButtonVariant.GHOST,
              testTag = "update_later_button"
            )
            DnaButton(
              text = "Diese Version überspringen",
              onClick = onUeberspringen,
              variant = DnaButtonVariant.GHOST,
              testTag = "update_skip_button"
            )
          }
        }
      }
    }
  }
}
