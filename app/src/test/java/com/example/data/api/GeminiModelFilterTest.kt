package com.example.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sichert die Zuordnung der Modelle ab.
 *
 * Die Ids stammen aus einer echten ListModels-Antwort, damit der Test die Realität prüft und
 * nicht eine Wunschvorstellung davon.
 */
class GeminiModelFilterTest {

  private val generate = listOf("generateContent", "countTokens")
  private val embed = listOf("embedContent")

  // --- Ausschluss fremder Modalitäten ---

  @Test
  fun `Bild- Musik- und Robotikmodelle gehoeren nicht in die Erzaehler-Auswahl`() {
    val fremde = listOf(
      "lyria-3.5",
      "lyria-3-pro-preview",
      "nano-banana-pro-preview",
      "gemini-3-pro-image",
      "gemini-3.1-flash-image-preview",
      "gemini-2.5-flash-preview-tts",
      "gemini-3.1-flash-tts-preview",
      "gemini-robotics-er-2-preview",
      "gemini-2.5-computer-use-preview-10-2025",
      "antigravity-preview-05-2026",
      "deep-research-pro-preview-12-2025",
      "gemini-3.1-pro-preview-customtools",
      "gemini-2.5-flash-native-audio-latest"
    )
    fremde.forEach { id ->
      assertFalse("$id darf kein Chat-Modell sein", GeminiModelFilter.isChatModel(id, generate))
    }
  }

  @Test
  fun `Gemma bleibt aussen vor`() {
    // Kein JSON-Modus: Die State-Extraction bräche daran.
    assertFalse(GeminiModelFilter.isChatModel("gemma-4-31b-it", generate))
    assertFalse(GeminiModelFilter.isTranscriptionModel("gemma-4-26b-a4b-it", generate))
  }

  @Test
  fun `echte Erzaehlmodelle bleiben drin`() {
    listOf("gemini-3.8-flash", "gemini-3.5-flash", "gemini-3.1-pro-preview", "gemini-2.5-pro")
      .forEach { assertTrue("$it fehlt", GeminiModelFilter.isChatModel(it, generate)) }
  }

  // --- Transkription ---

  @Test
  fun `dediziertes Transkriptionsmodell wird erkannt und steht vorn`() {
    assertTrue(GeminiModelFilter.isTranscriptionModel("gemini-3.5-transcribe", generate))

    val sorted = GeminiModelFilter.sortTranscription(
      listOf("gemini-3.8-flash", "gemini-3.5-transcribe", "gemini-3.5-flash")
    ) { it }

    assertEquals("gemini-3.5-transcribe", sorted.first())
  }

  @Test
  fun `Transkriptionsmodelle tauchen nicht in der Erzaehler-Auswahl auf`() {
    assertFalse(GeminiModelFilter.isChatModel("gemini-3.5-transcribe", generate))
  }

  @Test
  fun `Live-Varianten ohne generateContent fallen raus`() {
    assertFalse(GeminiModelFilter.isTranscriptionModel("gemini-3.5-transcribe-live", emptyList()))
    assertFalse(GeminiModelFilter.isTranscriptionModel("gemini-3.1-flash-live-preview", generate))
  }

  // --- Embedding ---

  @Test
  fun `Embedding-Modelle werden ueber die Methode erkannt`() {
    assertTrue(GeminiModelFilter.isEmbeddingModel("gemini-embedding-001", embed))
    assertTrue(GeminiModelFilter.isEmbeddingModel("gemini-embedding-2", embed))
    assertFalse(GeminiModelFilter.isEmbeddingModel("gemini-3.8-flash", generate))
  }

  // --- Sortierung ---

  @Test
  fun `neueste Version steht vorn`() {
    val sorted = GeminiModelFilter.sortByVersion(
      listOf("gemini-2.5-pro", "gemini-3.8-flash", "gemini-3.5-flash", "gemini-3.1-flash-lite")
    ) { it }

    assertEquals(
      listOf("gemini-3.8-flash", "gemini-3.5-flash", "gemini-3.1-flash-lite", "gemini-2.5-pro"),
      sorted
    )
  }

  @Test
  fun `stabile Fassung steht vor der Vorschau derselben Version`() {
    val sorted = GeminiModelFilter.sortByVersion(
      listOf("gemini-3.1-flash-lite-preview", "gemini-3.1-flash-lite")
    ) { it }

    assertEquals("gemini-3.1-flash-lite", sorted.first())
  }

  @Test
  fun `ein kuenftiges Modell sortiert sich ohne Codeaenderung nach vorn`() {
    // Genau das war der Grund, die frühere contains("3.8")-Kette zu ersetzen.
    val sorted = GeminiModelFilter.sortByVersion(
      listOf("gemini-3.8-flash", "gemini-4.0-flash")
    ) { it }

    assertEquals("gemini-4.0-flash", sorted.first())
  }

  @Test
  fun `Ids ohne Versionsnummer landen hinten statt zu werfen`() {
    assertEquals(0f, GeminiModelFilter.versionOf("gemini-flash-latest"))
    val sorted = GeminiModelFilter.sortByVersion(listOf("gemini-flash-latest", "gemini-3.8-flash")) { it }
    assertEquals("gemini-3.8-flash", sorted.first())
  }
}
