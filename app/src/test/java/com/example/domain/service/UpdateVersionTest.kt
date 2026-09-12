package com.example.domain.service

import com.example.data.api.UpdateClient
import com.example.data.api.versionCodeAusTag
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die Entscheidungen des Updaters, die ohne Netz und ohne Android prüfbar sind.
 *
 * Der teuerste Fehler dieses Systems ist nicht ein Angriff, sondern ein falsch angebotenes
 * Update: Der Nutzer lädt, installiert, es scheitert — und der naheliegende Ausweg wäre,
 * die App zu deinstallieren. Damit wären alle Geschichten weg.
 */
class UpdateVersionTest {

  @Test
  fun `nur ein hoeherer versionCode gilt als neuer`() {
    assertTrue(UpdateService.istNeuereVersion(fernerCode = 3, lokalerCode = 2))
    assertFalse(UpdateService.istNeuereVersion(fernerCode = 2, lokalerCode = 2))
  }

  @Test
  fun `ein Downgrade wird nie angeboten`() {
    // Android lehnt es ohnehin mit INSTALL_FAILED_VERSION_DOWNGRADE ab. Es gar nicht erst
    // anzubieten erspart dem Nutzer den Weg bis zur Fehlermeldung.
    assertFalse(UpdateService.istNeuereVersion(fernerCode = 1, lokalerCode = 2))
  }

  @Test
  fun `ein numerischer Tag wird in einen versionCode uebersetzt`() {
    assertEquals(10002, versionCodeAusTag("1.0.2"))
    assertEquals(10002, versionCodeAusTag("v1.0.2"))
    assertEquals(20000, versionCodeAusTag("2"))
    assertEquals(10100, versionCodeAusTag("1.1"))
  }

  @Test
  fun `ein Vorabversions-Tag liefert nichts`() {
    // Die Tauri-Vorlage wirft nicht-numerische Teile stillschweigend weg: "1.0.2-beta" wird
    // dort zu [1, 0] und gilt gegenüber 1.0.1 als nicht neuer -- ein Vorabversions-Tag
    // verschwindet lautlos. Umgekehrt kann "2.x" zu [2] werden und gegen 1.9.9 gewinnen.
    // MSF bietet in beiden Richtungen lieber gar nichts an.
    assertNull(versionCodeAusTag("1.0.2-beta"))
    assertNull(versionCodeAusTag("nightly"))
    assertNull(versionCodeAusTag(""))
    assertNull(versionCodeAusTag("1.0.2.3"))
  }

  @Test
  fun `zweistellige Abschnitte bleiben sortierbar`() {
    assertTrue(versionCodeAusTag("1.2.0")!! > versionCodeAusTag("1.1.99")!!)
    assertTrue(versionCodeAusTag("2.0.0")!! > versionCodeAusTag("1.99.99")!!)
    // Drei Stellen passen nicht mehr in das Schema und werden abgelehnt, statt still zu kippen.
    assertNull(versionCodeAusTag("1.100.0"))
  }

  @Test
  fun `die APK darf nur von GitHub kommen`() {
    assertTrue(
      UpdateClient.istErlaubteApkAdresse(
        "https://github.com/einmalmaik/MFS/releases/latest/download/MauntingStoryFable.apk"
      )
    )
    assertTrue(
      UpdateClient.istErlaubteApkAdresse("https://objects.githubusercontent.com/irgendwas.apk")
    )
  }

  @Test
  fun `eine umgelenkte Adresse wird abgelehnt`() {
    // Ohne diese Prüfung könnte eine veränderte latest.json den Download auf einen fremden
    // Server umlenken. Die Datei würde dort zwar an der Signaturprüfung scheitern -- aber erst
    // nach etlichen Megabyte und einem Kontakt, der die IP-Adresse des Spielers mitschreibt.
    assertFalse(UpdateClient.istErlaubteApkAdresse("https://example.com/boese.apk"))
    assertFalse(UpdateClient.istErlaubteApkAdresse("http://github.com/x.apk"))
    assertFalse(UpdateClient.istErlaubteApkAdresse("https://github.com.example.com/x.apk"))
    assertFalse(UpdateClient.istErlaubteApkAdresse(""))
    assertFalse(UpdateClient.istErlaubteApkAdresse("kein url"))
  }
}
