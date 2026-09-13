package com.example.domain.engine

import com.example.data.model.StoryEntity
import com.example.data.model.displayTitle
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Seit dem Umbau hat eine Geschichte zwei Prompt-Ebenen: die generische Standard-Regie für alle
 * Geschichten und den Prompt dieser einen. Vorher verdrängte der zweite den ersten — jede
 * Geschichte mit eigenem Prompt verlor damit sämtliche Erzähl- und Formatregeln.
 */
class StoryPromptCompositionTest {

  @Test
  fun `beide Ebenen werden hintereinander geschickt`() {
    val composed = StoryTurnEngine.composeSystemPrompt("Standard-Regie", "Diese Geschichte")
    assertEquals("Standard-Regie\n\nDiese Geschichte", composed)
  }

  @Test
  fun `ohne Standard-Regie bleibt der Prompt der Geschichte`() {
    assertEquals("Nur die Geschichte", StoryTurnEngine.composeSystemPrompt("", "Nur die Geschichte"))
  }

  @Test
  fun `ohne Geschichten-Prompt bleibt die Standard-Regie`() {
    assertEquals("Nur global", StoryTurnEngine.composeSystemPrompt("Nur global", ""))
  }

  @Test
  fun `sind beide leer entsteht kein Leerzeilen-Rest`() {
    assertEquals("", StoryTurnEngine.composeSystemPrompt("   ", "\n"))
  }

  @Test
  fun `mit Adult Content wird Freiheits-Abschnitt ergaenzt falls noch nicht vorhanden`() {
    val composed = StoryTurnEngine.composeSystemPrompt("Standard-Regie", "Diese Geschichte", allowAdultContent = true)
    org.junit.Assert.assertTrue(composed.contains("FREIHEIT DER ERZÄHLUNG & ERWACHSENEN-INHALTE"))
  }

  @Test
  fun `mit vorhandenem Adult-Abschnitt wird nicht doppelt ergaenzt`() {
    val promptMitAdult = "Standard-Regie mit ERWACHSENEN-INHALTE"
    val composed = StoryTurnEngine.composeSystemPrompt(promptMitAdult, "", allowAdultContent = true)
    assertEquals(promptMitAdult, composed)
  }

  @Test
  fun `Titel wird nur in ein leeres Feld uebernommen`() {
    assertEquals(
      "Die verfluchte Feste",
      StateExtractionEngine.fillIfBlank("", "Die verfluchte Feste")
    )
    assertEquals(
      "Mein eigener Titel",
      StateExtractionEngine.fillIfBlank("Mein eigener Titel", "Die verfluchte Feste")
    )
    assertEquals("", StateExtractionEngine.fillIfBlank("", "   "))
  }

  @Test
  fun `eine noch unbenannte Geschichte zeigt einen Platzhalter`() {
    assertEquals("Neue Geschichte", StoryEntity(title = "").displayTitle)
    assertEquals("Alden", StoryEntity(title = "Alden").displayTitle)
  }
}
