package com.example.domain.engine

import com.example.data.model.BodyOrgan
import com.example.data.model.BodyPart
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Der Wächter, der bisher fehlte.
 *
 * Die Körperregionen und Organe standen von Hand im Extraktions-Prompt. Ein neu aufgenommenes
 * Organ wurde damit sofort im Scan gezeichnet und im Verletzungsdialog angeboten — aber das
 * Modell erfuhr nie, dass es den Namen verwenden darf. Ein Treffer dorthin landete als leeres
 * `organ`-Feld, das Organ blieb auf ewig unverletzt, und kein Compiler und kein Test sagte etwas.
 */
class StoryPromptsTest {

  private val prompt = StoryPrompts.stateExtraction(
    currentStateJson = "{}",
    existingMilestones = emptyList(),
    knownNpcs = emptyList(),
    userAction = "Ich sehe mich um.",
    storyResponse = "Der Raum ist leer."
  )

  @Test
  fun `jede Koerperregion steht im Prompt`() {
    BodyPart.entries.forEach { part ->
      assertTrue("Körperregion ${part.id} fehlt im Extraktions-Prompt", prompt.contains(part.id))
    }
  }

  @Test
  fun `jedes Organ steht im Prompt`() {
    BodyOrgan.entries.forEach { organ ->
      assertTrue("Organ ${organ.id} fehlt im Extraktions-Prompt", prompt.contains(organ.id))
    }
  }

  @Test
  fun `Zustand, Aktion und Antwort werden mitgegeben`() {
    // Ohne diese drei Blöcke schreibt die Extraktion den Zustand nicht fort, sondern erfindet ihn.
    assertTrue(prompt.contains("Ich sehe mich um."))
    assertTrue(prompt.contains("Der Raum ist leer."))
    assertTrue(prompt.contains("[BISHERIGER ZUSTAND]"))
  }

  @Test
  fun `bekannte Figuren und Meilensteine landen im Prompt`() {
    val mitVorgeschichte = StoryPrompts.stateExtraction(
      currentStateJson = "{}",
      existingMilestones = listOf("Der Schwur am Fluss"),
      knownNpcs = listOf("Lena Vogt (rotes Haar, Narbe über der Braue)"),
      userAction = "Ich frage sie.",
      storyResponse = "Sie schweigt."
    )

    assertTrue(mitVorgeschichte.contains("Der Schwur am Fluss"))
    assertTrue(mitVorgeschichte.contains("Lena Vogt"))
  }
}
