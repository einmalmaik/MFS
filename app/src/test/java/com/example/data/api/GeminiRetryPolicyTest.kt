package com.example.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Die Entscheidungstabelle, wann ein fehlgeschlagener Gemini-Aufruf wiederholt wird.
 *
 * Sie ist binnen zweier Commits schon einmal gekippt — 429 war erst drin, dann bewusst draußen —
 * und beide Fehlrichtungen kosten den Nutzer etwas, ohne dass der Build rot wird:
 * Fällt 503 heraus, kehrt das lautlose Einfrieren des Spielstands zurück, weil rund 40 % aller
 * Aufrufe damit antworten. Kommt 429 zurück, verbraucht ein einziger abgewiesener Aufruf vier
 * statt einer Anfrage des Tageskontingents — und das sind im Free-Tier zwanzig pro Modell und Tag.
 */
class GeminiRetryPolicyTest {

  @Test
  fun `Googles voruebergehende Fehler werden mit wachsendem Abstand wiederholt`() {
    for (code in listOf(500, 502, 503, 504)) {
      assertEquals("Code $code, erster Versuch", 1_000L, GeminiClient.retryDelayMs(code, 0))
      assertEquals("Code $code, zweiter Versuch", 3_000L, GeminiClient.retryDelayMs(code, 1))
      assertEquals("Code $code, dritter Versuch", 6_000L, GeminiClient.retryDelayMs(code, 2))
    }
  }

  @Test
  fun `nach dem vierten Versuch wird aufgegeben`() {
    // Vier Versuche insgesamt, höchstens zehn Sekunden Verzug. Länger zu warten hieße, den
    // Spieler vor einem stehenden Bildschirm sitzen zu lassen.
    assertNull(GeminiClient.retryDelayMs(503, 3))
    assertNull(GeminiClient.retryDelayMs(503, 99))
  }

  @Test
  fun `ein erschoepftes Tageskontingent wird nicht wiederholt`() {
    // Googles 429 im Free-Tier ist GenerateRequestsPerDayPerProjectPerModel. Gegen ein
    // aufgebrauchtes Tageskontingent hilft kein dritter Versuch; er kostet nur zehn Sekunden,
    // bevor dieselbe Meldung erscheint — und drei weitere Anfragen des Kontingents.
    assertNull(GeminiClient.retryDelayMs(429, 0))
  }

  @Test
  fun `dauerhafte Fehler werden nicht wiederholt`() {
    for (code in listOf(400, 401, 403, 404)) {
      assertNull("Code $code", GeminiClient.retryDelayMs(code, 0))
    }
  }

  @Test
  fun `negative Versuchszahl fuehrt nicht in den Index`() {
    assertNull(GeminiClient.retryDelayMs(503, -1))
  }
}
