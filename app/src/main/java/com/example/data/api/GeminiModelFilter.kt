package com.example.data.api

/**
 * Ordnet die Modelle aus Googles ListModels-Antwort den drei Verwendungszwecken zu.
 *
 * Bewusst frei von Android- und JSON-Abhängigkeiten, damit die Zuordnung ohne Emulator
 * geprüft werden kann.
 *
 * Wichtig: ListModels sagt NICHT, ob ein Modell noch benutzbar ist. `gemini-2.5-flash` steht
 * dort mit vollständiger Beschreibung und `generateContent`, liefert beim Aufruf aber HTTP 404
 * ("no longer available to new users"). Eine statische Sperrliste für abgekündigte Modelle ist
 * deshalb unmöglich — dafür lernt [GeminiClient.markUnusable] aus echten 404-Antworten.
 * Hier wird nur nach Modalität gefiltert.
 */
object GeminiModelFilter {

  /**
   * Modell-Familien, die zwar `generateContent` können, für eine Textgeschichte oder eine
   * Transkription aber untauglich sind. Ohne diese Liste stehen Bildgeneratoren, Musikmodelle
   * und Robotik-Modelle in der Erzähler-Auswahl.
   *
   * Gemma fliegt aus einem inhaltlichen Grund raus: kein JSON-Modus (bricht die State-Extraction),
   * kein thinkingLevel und keine über safetySettings abschaltbaren Sicherheitsfilter.
   */
  private val EXCLUDED_FRAGMENTS = listOf(
    "gemma",
    "veo",
    "imagen",
    "aqa",
    "lyria",
    "nano-banana",
    "robotics",
    "computer-use",
    "antigravity",
    "deep-research",
    "-tts",
    "-image",
    "native-audio",
    "-live",
    "customtools"
  )

  private val VERSION_REGEX = Regex("""(\d+(?:\.\d+)?)""")

  fun isExcluded(id: String): Boolean {
    val lower = id.lowercase()
    return EXCLUDED_FRAGMENTS.any { lower.contains(it) }
  }

  /** Modelle, die eine Geschichte erzählen und den Zustand als JSON extrahieren können. */
  fun isChatModel(id: String, methods: List<String>): Boolean =
    methods.contains("generateContent") &&
      !isExcluded(id) &&
      !id.contains("embedding") &&
      !id.contains("transcribe")

  /**
   * Modelle, die Audio entgegennehmen. Dedizierte Transkriptionsmodelle zuerst, danach die
   * multimodalen Flash-Modelle als Ausweichlösung.
   */
  fun isTranscriptionModel(id: String, methods: List<String>): Boolean =
    methods.contains("generateContent") &&
      !isExcluded(id) &&
      !id.contains("embedding") &&
      (id.contains("transcribe") || id.contains("flash") || id.contains("audio"))

  fun isEmbeddingModel(id: String, methods: List<String>): Boolean =
    methods.contains("embedContent")

  /**
   * Die Versionsnummer aus der Id, z. B. "gemini-3.8-flash" -> 3.8f.
   *
   * Ersetzt die früheren `id.contains("3.8")`-Vergleiche: Ein künftiges 3.9 sortiert sich damit
   * von selbst nach vorn, ohne dass jemand die App anfassen muss.
   */
  fun versionOf(id: String): Float =
    VERSION_REGEX.find(id)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f

  private fun isPreview(id: String): Boolean = id.contains("preview")

  /** Neueste stabile Version zuerst; Vorschau-Versionen hinter der stabilen Fassung. */
  fun <T> sortByVersion(models: List<T>, idOf: (T) -> String): List<T> =
    models.sortedWith(
      compareByDescending<T> { versionOf(idOf(it)) }
        .thenBy { isPreview(idOf(it)) }
        .thenBy { idOf(it) }
    )

  /** Wie [sortByVersion], stellt aber dedizierte Transkriptionsmodelle an den Anfang. */
  fun <T> sortTranscription(models: List<T>, idOf: (T) -> String): List<T> =
    models.sortedWith(
      compareByDescending<T> { idOf(it).contains("transcribe") }
        .thenByDescending { versionOf(idOf(it)) }
        .thenBy { isPreview(idOf(it)) }
        .thenBy { idOf(it) }
    )
}
