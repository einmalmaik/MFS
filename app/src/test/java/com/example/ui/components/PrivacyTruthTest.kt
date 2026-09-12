package com.example.ui.components

import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Bindet die Datenschutzerklärung an das, was die App tatsächlich tut.
 *
 * Eine Erklärung, die nur beim Schreiben gestimmt hat, ist schlimmer als keine: Sie erzeugt
 * ein Vertrauen, das die App dann nicht mehr einlöst. Der teuerste Weg, auf dem das passiert,
 * ist eine neue Berechtigung — sie wird im Manifest ergänzt, der Build bleibt grün, und
 * niemand denkt an den Text. Genau das fängt dieser Test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PrivacyTruthTest {

  private val erwarteteBerechtigungen = setOf(
    "android.permission.INTERNET",
    "android.permission.RECORD_AUDIO",
    "android.permission.REQUEST_INSTALL_PACKAGES"
  )

  @Test
  fun `die Erklaerung nennt genau die Berechtigungen, die das Manifest anfordert`() {
    val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    val angefordert = context.packageManager
      .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
      .requestedPermissions
      .orEmpty()
      // Auf den eigenen Paketnamen lautende Berechtigungen zählen nicht: Das sind
      // selbstdefinierte Signatur-Berechtigungen, die AndroidX beim Zusammenführen des
      // Manifests einfügt (DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION). Sie werden niemandem
      // angezeigt und gewähren keiner anderen App irgendetwas.
      .filterNot { it.startsWith(context.packageName) }
      .toSet()

    assertEquals(
      "Das Manifest fordert andere Berechtigungen an, als die Datenschutzerklärung nennt. " +
        "Entweder gehört die Berechtigung nicht dorthin, oder Abschnitt 9 der Erklärung " +
        "(und PRIVACY_VERSION) gehört aktualisiert.",
      erwarteteBerechtigungen,
      angefordert
    )

    val abschnitt9 = PRIVACY_SECTIONS.single { it.heading.startsWith("9.") }
    assertTrue(
      "Abschnitt 9 muss die Anzahl der Berechtigungen benennen.",
      abschnitt9.punkte.any { it.contains("genau drei Berechtigungen") }
    )
  }

  @Test
  fun `kein Abschnitt ist leer`() {
    PRIVACY_SECTIONS.forEach { abschnitt ->
      assertTrue("Überschrift fehlt", abschnitt.heading.isNotBlank())
      assertTrue("Text zu '${abschnitt.heading}' fehlt", abschnitt.body.length > 40)
      abschnitt.punkte.forEach { punkt ->
        assertTrue("Leerer Punkt in '${abschnitt.heading}'", punkt.length > 20)
      }
    }
  }

  @Test
  fun `die Abschnitte sind lueckenlos durchnummeriert`() {
    // Ein eingeschobener Abschnitt ohne Umnummerierung ergäbe zwei Punkte mit derselben Zahl —
    // und Querverweise wie "siehe Abschnitt 4" zeigten ins Leere.
    val nummern = PRIVACY_SECTIONS.map { it.heading.substringBefore(".").toInt() }
    assertEquals((1..PRIVACY_SECTIONS.size).toList(), nummern)
  }

  @Test
  fun `die unbequemen Stellen stehen drin`() {
    // Die vier Punkte, die eine Werbebroschüre weglassen würde. Verschwindet einer davon,
    // soll jemand das bewusst tun müssen und nicht nebenbei.
    val alles = PRIVACY_SECTIONS.joinToString(" ") { it.heading + " " + it.body + " " + it.punkte.joinToString(" ") }

    assertTrue("Die abgeschalteten Sicherheitsfilter fehlen", alles.contains("OFF"))
    assertTrue("Der Klartext-Schlüssel fehlt", alles.contains("unverschlüsselt"))
    assertTrue(
      "Dass Google im kostenlosen Kontingent mitliest, fehlt",
      alles.contains("kostenlosen Kontingent")
    )
    assertTrue("Dass es kein Backup gibt, fehlt", alles.contains("Cloud-Backup"))
    assertTrue("Dass der ausgelieferte Build debuggable ist, fehlt", alles.contains("debuggable"))
  }

  @Test
  fun `ein debuggable Build muss in der Erklaerung stehen`() {
    // Ein Debug-Build laesst sich mit eingeschaltetem USB-Debugging ueber `adb run-as`
    // auslesen -- Datenbank und API-Schluessel inbegriffen, ohne Root. Wird spaeter auf einen
    // Release-Build umgestellt, faellt diese Einschraenkung weg und der Satz gehoert geloescht;
    // dieser Test erinnert dann daran.
    val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    val debuggable = (context.applicationInfo.flags and
      android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0

    val erwaehnt = PRIVACY_SECTIONS
      .flatMap { it.punkte }
      .any { it.contains("debuggable") }

    assertEquals(
      "Die Erklärung und der Build sagen Verschiedenes über debuggable.",
      debuggable,
      erwaehnt
    )
  }

  @Test
  fun `Fassung und Stand sind gesetzt`() {
    assertTrue(PRIVACY_VERSION.isNotBlank())
    assertTrue(PRIVACY_DATE.matches(Regex("""\d{4}-\d{2}-\d{2}""")))
  }
}
