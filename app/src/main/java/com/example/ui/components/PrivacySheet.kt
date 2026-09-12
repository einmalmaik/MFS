package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.dna.DnaBadge
import com.example.ui.dna.DnaBadgeTone
import com.example.ui.dna.DnaCard
import com.example.ui.dna.DnaColors
import com.example.ui.dna.DnaTypography

/**
 * Ein Abschnitt der Erklärung. [punkte] sind die Einzelheiten darunter.
 */
internal data class PrivacySection(
  val heading: String,
  val body: String,
  val punkte: List<String> = emptyList()
)

/** Stand und Fassung. Wird eine Zusage geändert, gehören beide Werte mit geändert. */
internal const val PRIVACY_VERSION = "1.0"
internal const val PRIVACY_DATE = "2026-09-12"

/**
 * Die Sätze dieser Liste sind gegen den Quelltext geprüft, nicht gegen die Absicht.
 *
 * Wer hier etwas ändert, ändert eine Zusage an den Spieler. Wer am Datenfluss etwas ändert —
 * ein neuer Netzaufruf, ein neues Feld im Prompt, eine neue Berechtigung —, muss diese Liste
 * mitziehen, sonst steht hier eine Unwahrheit.
 */
internal val PRIVACY_SECTIONS = listOf(
  PrivacySection(
    heading = "1. Worum es geht",
    body = "Maunting Story Fable ist ein Erzählspiel, das auf deinem Gerät läuft. Es gibt " +
      "keinen Server von uns, kein Konto und keine Anmeldung. Wir betreiben nichts, was deine " +
      "Geschichten sehen könnte. Genau eine Gegenstelle ist beteiligt, und die hast du selbst " +
      "eingerichtet: Googles Gemini-API, erreichbar nur mit dem Schlüssel, den du einträgst. " +
      "Ohne diesen Schlüssel geht kein einziges Byte hinaus."
  ),
  PrivacySection(
    heading = "2. Was auf dem Gerät bleibt",
    body = "In der lokalen Datenbank (storyforge_database) liegt alles, was deine Geschichten " +
      "ausmacht. Nichts davon wird von allein irgendwohin übertragen.",
    punkte = listOf(
      "Geschichten: Titel, dein Prompt, Genre, Erzählperspektive, Spielzeit.",
      "Jeder Zug wörtlich — deine Eingaben und die Antworten des Game Masters, mit Zeitstempel.",
      "Checkpoints: Ort, Wetter, Uhrzeit, Kleidung, Zustand, Inventar, Figuren, Meilensteine " +
        "und der vollständige Weltzustand als JSON — einschließlich Verletzungen mit " +
        "Körperregion, betroffenem Organ und Schweregrad.",
      "Erinnerungen: der Text jeder Erinnerung und ihr Vektor als Binärwert.",
      "Figuren: Name, Aussehen, Persönlichkeit, Beziehung zu dir und was sie selbst erlebt haben."
    )
  ),
  PrivacySection(
    heading = "3. Was das Gerät verlässt",
    body = "Ausschließlich an generativelanguage.googleapis.com (Google LLC). Es gibt keine " +
      "zweite Adresse im Programm. Übertragen wird:",
    punkte = listOf(
      "Bei jedem Zug: die Standard-Regie, der Prompt dieser Geschichte, der vollständige " +
        "Weltzustand als JSON samt Verletzungen, die Meilensteine, die letzten Tagesüberblicke, " +
        "die Profile der anwesenden Figuren, die passenden Erinnerungen, die letzten acht " +
        "Nachrichten und deine aktuelle Eingabe.",
      "Direkt danach ein zweiter Aufruf zur Zustands-Extraktion: derselbe Zustand, deine " +
        "Aktion und die eben erzählte Antwort. Ein Zug sind also zwei Anfragen.",
      "Beim Anlegen einer Erinnerung: ihr Text einzeln, zur Umrechnung in einen Vektor.",
      "Bei der Spracheingabe: die vollständige Aufnahme.",
      "Beim Öffnen der Einstellungen: nur der Schlüssel, um die Modellliste abzurufen."
    )
  ),
  PrivacySection(
    heading = "4. Was Google damit tut",
    body = "Das bestimmt Google, nicht diese App. Und der Unterschied zwischen kostenlos und " +
      "bezahlt ist hier größer, als er aussieht.",
    punkte = listOf(
      "Im kostenlosen Kontingent der Gemini-API behält Google sich ausdrücklich vor, Eingaben " +
        "und Antworten zur Verbesserung seiner Produkte zu verwenden — und dass Menschen sie " +
        "lesen. Für kostenpflichtige Nutzung sagt Google das Gegenteil zu.",
      "Maßgeblich sind Googles Nutzungsbedingungen in ihrer jeweils gültigen Fassung. Sie " +
        "können sich ändern, ohne dass diese App davon erfährt.",
      "Was einmal bei Google ist, kann MSF nicht zurückholen und nicht löschen.",
      "Praktisch heißt das: Schreibe in eine Geschichte nichts, was dich oder andere " +
        "identifizierbar macht, wenn du nicht möchtest, dass es dort liegt."
    )
  ),
  PrivacySection(
    heading = "5. Die Sicherheitsfilter sind aus",
    body = "Bei jedem Erzähl-Aufruf setzt MSF Googles vier einstellbare Filter — sexuell " +
      "explizite Inhalte, Hassrede, Belästigung, gefährliche Inhalte — auf OFF. Das ist eine " +
      "bewusste Entscheidung für kompromissloses Erzählen und keine Nebenwirkung.",
    punkte = listOf(
      "Die Erzählung kann dadurch Gewalt, Sexualität und Grausamkeit ungefiltert schildern.",
      "Der Kinderschutz läuft auf Googles Seite und lässt sich nicht abschalten — auch von " +
        "MSF nicht.",
      "Abgeschaltete Filter heißen nicht, dass Google nicht mitliest. Siehe Abschnitt 4."
    )
  ),
  PrivacySection(
    heading = "6. Dein API-Schlüssel",
    body = "Er liegt unverschlüsselt in den App-Einstellungen (storyforge_prefs). Das ist die " +
      "unangenehme Wahrheit, und sie gehört hierher.",
    punkte = listOf(
      "Auf einem Gerät ohne Root kommt keine andere App an diese Datei heran — Android trennt " +
        "die Bereiche der Apps.",
      "Wer dein entsperrtes Gerät in der Hand hat oder es gerootet hat, kommt heran.",
      "Cloud-Backup ist für diese App abgeschaltet. Der Schlüssel und deine Geschichten wandern " +
        "also nicht in Googles Sicherung — und beim Gerätewechsel auch nicht automatisch mit.",
      "Auf der Reise steht der Schlüssel im Kopffeld der Anfrage, niemals in der Adresse. " +
        "Adressen landen in Protokollen, Kopffelder nicht.",
      "Er erscheint in keinem Protokoll, keiner Fehlermeldung und keiner Anzeige."
    )
  ),
  PrivacySection(
    heading = "7. Mikrofon",
    body = "Das Mikrofon läuft nur, während du es laufen lässt. Es gibt kein Weckwort und kein " +
      "Mithören im Hintergrund.",
    punkte = listOf(
      "Die Aufnahme beginnt erst beim Antippen des Mikrofon-Knopfes und endet beim Antippen " +
        "des Hakens oder des Kreuzes.",
      "Sie endet außerdem von selbst nach fünf Minuten und sobald du die App verlässt.",
      "Das Kreuz verwirft die Aufnahme sofort; dann wird nichts gesendet.",
      "Der Haken schickt die vollständige Aufnahme zur Abschrift an Google — samt allem, was " +
        "im Raum sonst zu hören war.",
      "Die Datei liegt bis dahin im privaten Zwischenspeicher der App und wird direkt nach dem " +
        "Einlesen gelöscht. Reste eines Absturzes werden bei der nächsten Aufnahme entfernt."
    )
  ),
  PrivacySection(
    heading = "8. Aktualisierung",
    body = "MSF kann bei GitHub nachsehen, ob eine neuere Fassung vorliegt. Das ist die " +
      "einzige Gegenstelle neben Google — und sie ist ab Werk abgeschaltet.",
    punkte = listOf(
      "Beim ersten Start wirst du einmal gefragt. Sagst du nein, wird nie angefragt, und " +
        "GitHub erfährt nicht einmal, dass die App existiert.",
      "Sagst du ja, fragt MSF höchstens einmal pro Tag und erst fünf Sekunden nach dem Start. " +
        "GitHub erfährt dabei deine IP-Adresse, den Zeitpunkt und die abgerufene Datei.",
      "Heruntergeladen wird erst auf deinen Tastendruck — nichts läuft im Hintergrund über " +
        "dein Mobilfunkvolumen.",
      "Installiert wird nur über den Dialog des Systems. MSF verzichtet bewusst dauerhaft auf " +
        "die Möglichkeit, ohne Nachfrage zu installieren.",
      "Vor der Installation prüft MSF Prüfsumme, Paketname und Signatur der geladenen Datei. " +
        "Passt etwas nicht, wird die Datei gelöscht statt installiert.",
      "Die Berechtigung, Apps zu installieren, ist der Preis dafür. Sie bleibt dauerhaft " +
        "bestehen, solange die Funktion eingebaut ist.",
      "Findet die Prüfung nichts, sagt sie nichts. Nur wenn du in den Einstellungen selbst auf " +
        "'Jetzt prüfen' tippst, bekommst du in jedem Fall eine Antwort.",
      "Der Schalter steht in den Einstellungen unter AKTUALISIERUNG und ist jederzeit umkehrbar."
    )
  ),
  PrivacySection(
    heading = "9. Was es nicht gibt",
    body = "Diese Liste ist so wichtig wie die davor, weil sie das benennt, was viele Apps " +
      "stillschweigend tun.",
    punkte = listOf(
      "Keine Telemetrie, keine Nutzungsstatistik, keine Absturzberichte.",
      "Keine Werbung, keine Werbekennung, kein Tracking.",
      "Kein Konto, keine Registrierung, keine E-Mail-Adresse.",
      "Keine Standortabfrage, kein Kontaktzugriff, keine Kamera.",
      "Die App fordert genau drei Berechtigungen an: Internet, Mikrofon und das Anstoßen von " +
        "App-Installationen für die Aktualisierung."
    )
  ),
  PrivacySection(
    heading = "10. Löschen",
    body = "Du entscheidest, was verschwindet, und nichts davon ist ein Umweg.",
    punkte = listOf(
      "Eine Geschichte löschen entfernt in einem Zug auch ihre Nachrichten, Checkpoints, " +
        "Erinnerungen und Figuren. Es bleibt nichts liegen.",
      "Den Schlüssel entfernst du, indem du das Feld in den Einstellungen leerst.",
      "Die Deinstallation löscht die gesamte Datenbank und die Einstellungen restlos. Da es " +
        "kein Backup gibt, ist danach wirklich nichts mehr da.",
      "Was bereits bei Google liegt, erreichst du nur über dein Google-Konto — nicht über " +
        "diese App."
    )
  ),
  PrivacySection(
    heading = "11. Grenzen dieser Zusage",
    body = "Was hier steht, gilt für diese App. Es gilt nicht für das, was daneben liegt.",
    punkte = listOf(
      "MSF wird außerhalb von Google Play verteilt. Die App wird nicht durch Googles Prüfung " +
        "kontrolliert — vertraue der Quelle, von der du sie geladen hast.",
      "Die über GitHub verteilte Fassung ist ein Debug-Build und damit als debuggable " +
        "gekennzeichnet. Das schwächt Abschnitt 6 ab: Wer dein Gerät mit eingeschaltetem " +
        "USB-Debugging an einen Rechner anschließt, kann über adb auf die Datenbank und den " +
        "Schlüssel zugreifen, ohne das Gerät zu rooten. Lass USB-Debugging aus, wenn du das " +
        "nicht brauchst.",
      "Auf einem gerooteten oder mit Schadsoftware versehenen Gerät schützt keine App-Trennung " +
        "mehr; dann sind Datenbank und Schlüssel lesbar.",
      "Diese Erklärung beschreibt den Stand des Programms zum unten genannten Datum."
    )
  )
)

/**
 * Die Datenschutzerklärung, wie sie in der App steht.
 *
 * Sie liegt bewusst hier und nicht als Verweis ins Netz: Eine App, die verspricht, dass nichts
 * unbemerkt hinausgeht, kann ihre eigene Erklärung nicht hinter einem Aufruf verstecken, der
 * wieder etwas hinausschickt.
 */
@Composable
fun PrivacySheet(
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(DnaColors.Surface)
      .padding(horizontal = 20.dp)
      .verticalScroll(rememberScrollState())
      .testTag("privacy_sheet")
  ) {
    Spacer(modifier = Modifier.height(12.dp))

    Box(
      modifier = Modifier
        .align(Alignment.CenterHorizontally)
        .width(40.dp)
        .height(4.dp)
        .background(DnaColors.Border, RoundedCornerShape(2.dp))
    )

    Spacer(modifier = Modifier.height(16.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Shield,
          contentDescription = null,
          tint = DnaColors.Primary,
          modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "Datenschutz",
          style = MaterialTheme.typography.titleMedium,
          fontFamily = DnaTypography.ManropeFamily,
          fontWeight = FontWeight.Bold,
          color = DnaColors.OnSurface
        )
      }

      IconButton(onClick = onClose, modifier = Modifier.testTag("close_privacy_button")) {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = "Schließen",
          tint = DnaColors.OnSurfaceVariant
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    DnaCard(
      modifier = Modifier.fillMaxWidth(),
      backgroundColor = DnaColors.PrimaryContainer.copy(alpha = 0.30f),
      borderColor = DnaColors.Primary.copy(alpha = 0.35f)
    ) {
      Column {
        Text(
          text = "Maunting Studios — Schutz braucht Vertrauen",
          style = MaterialTheme.typography.labelMedium,
          fontFamily = DnaTypography.ManropeFamily,
          fontWeight = FontWeight.Bold,
          color = DnaColors.Primary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Deine Geschichten gehören dir und liegen auf deinem Gerät. Was hinausgeht, " +
            "geht an eine einzige Stelle, die du selbst eingerichtet hast. Diese Erklärung " +
            "nennt auch, was daran unbequem ist.",
          style = MaterialTheme.typography.bodySmall,
          fontFamily = DnaTypography.InterFamily,
          color = DnaColors.OnPrimaryContainer
        )
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    PRIVACY_SECTIONS.forEach { section ->
      PrivacySectionBlock(section)
      Spacer(modifier = Modifier.height(22.dp))
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
      DnaBadge(
        text = "Fassung $PRIVACY_VERSION",
        tone = DnaBadgeTone.ICE,
        showDot = false,
        fontFamily = DnaTypography.JetBrainsMonoFamily
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "Stand $PRIVACY_DATE",
        style = DnaTypography.MonoSmall
      )
    }

    Spacer(modifier = Modifier.height(32.dp))
  }
}

@Composable
private fun PrivacySectionBlock(section: PrivacySection) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Text(
      text = section.heading,
      style = MaterialTheme.typography.titleSmall,
      fontFamily = DnaTypography.ManropeFamily,
      fontWeight = FontWeight.Bold,
      color = DnaColors.Primary
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = section.body,
      style = MaterialTheme.typography.bodySmall,
      fontFamily = DnaTypography.InterFamily,
      color = DnaColors.OnSurface
    )

    if (section.punkte.isNotEmpty()) {
      Spacer(modifier = Modifier.height(10.dp))
      section.punkte.forEach { punkt ->
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
          Text(
            text = "·",
            style = MaterialTheme.typography.bodySmall,
            color = DnaColors.Primary,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = punkt,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = DnaTypography.InterFamily,
            color = DnaColors.OnSurfaceVariant
          )
        }
      }
    }
  }
}
