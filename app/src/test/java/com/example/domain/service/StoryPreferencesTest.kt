package com.example.domain.service

import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AiSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Die globalen KI-Einstellungen sind seit dem Umbau die einzige Quelle für Modellwahl,
 * Denkstufe, Kreativität und Adult Content. Geht hier etwas verloren, erzählt die App
 * plötzlich mit einem anderen Modell — und ein anderes Einbettungsmodell macht das gesamte
 * bisherige Gedächtnis unlesbar.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StoryPreferencesTest {

  private lateinit var preferences: StoryPreferences

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    context.getSharedPreferences("storyforge_prefs", android.content.Context.MODE_PRIVATE)
      .edit().clear().commit()
    preferences = StoryPreferences(context)
  }

  @Test
  fun `speichert und liest die Einstellungen unveraendert zurueck`() {
    val settings = AiSettings(
      chatModel = "gemini-3.8-flash",
      embeddingModel = "gemini-embedding-2",
      transcriptionModel = "gemini-3.5-transcribe",
      thinkingLevel = "HIGH",
      thinkingBudget = 8192,
      temperature = 0.42f,
      supportsTemperature = false,
      adultContentEnabled = false
    )

    preferences.setAiSettings(settings)

    assertEquals(settings, preferences.getAiSettings())
  }

  @Test
  fun `ohne gespeicherte Werte gelten die Vorgaben`() {
    assertEquals(AiSettings(), preferences.getAiSettings())
  }

  @Test
  fun `uebernimmt die Werte einer Geschichte genau einmal`() {
    val fromStory = AiSettings(chatModel = "gemini-3.1-pro-preview", thinkingLevel = "LOW")

    assertTrue(preferences.seedAiSettingsOnce(fromStory))
    assertEquals(fromStory, preferences.getAiSettings())

    // Zweiter Versuch darf eine spätere Nutzerentscheidung nicht mehr überschreiben.
    preferences.setAiSettings(AiSettings(chatModel = "gemini-3.5-flash"))
    assertFalse(preferences.seedAiSettingsOnce(fromStory))
    assertEquals("gemini-3.5-flash", preferences.getAiSettings().chatModel)
  }

  @Test
  fun `wer selbst gespeichert hat, wird nicht mehr ueberschrieben`() {
    // Der Fall, der auf einer frischen Installation eintrat: Beim ersten Start gibt es keine
    // Geschichte, aus der übernommen werden könnte — der Riegel blieb offen. Legte der Nutzer
    // danach eine Geschichte an und wählte ein anderes Einbettungsmodell, lief die Übernahme
    // beim nächsten App-Start zum ersten Mal und warf genau diese Wahl weg. Weil
    // cosineSimilarity bei abweichender Dimension bewusst 0 liefert, war damit das gesamte
    // bis dahin aufgebaute Gedächtnis unauffindbar, ohne dass irgendetwas sichtbar brach.
    preferences.setAiSettings(AiSettings(embeddingModel = "gemini-embedding-2"))

    val ausAltbestand = AiSettings(embeddingModel = "text-embedding-004")
    assertFalse(preferences.seedAiSettingsOnce(ausAltbestand))
    assertEquals("gemini-embedding-2", preferences.getAiSettings().embeddingModel)
  }

  @Test
  fun `ohne eingetragenen Schluessel gibt es keinen Schluessel`() {
    // Früher fiel diese Stelle auf BuildConfig.GEMINI_API_KEY zurück. Ein für einen lokalen
    // Build in die .env eingetragener Schlüssel wäre damit im APK gelandet und still benutzt
    // worden, obwohl in den Einstellungen nichts steht.
    assertEquals("", preferences.getEffectiveApiKey())
  }

  @Test
  fun `geleerte Standard-Regie bleibt leer`() {
    // Die Standard-Regie darf komplett entfernt werden; dann gilt nur noch der Prompt der
    // jeweiligen Geschichte. Ein Rückfall auf den Vorgabetext wäre hier ein Fehler.
    preferences.setGlobalDefaultSystemPrompt("")
    assertEquals("", preferences.getGlobalDefaultSystemPrompt())
  }
}
