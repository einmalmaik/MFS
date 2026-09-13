package com.example.data.api

import android.util.Log
import com.example.data.model.GeminiDefaults
import com.example.data.model.GeminiModelInfo
import com.example.domain.model.TimeAnchor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class GeminiClient(
  private val customApiKeyProvider: () -> String? = { null }
) {
  private val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .build()

  /**
   * Führt eine Anfrage aus und wiederholt sie bei Googles vorübergehenden Fehlern.
   *
   * Am 2026-09-12 gemessen: von zehn Aufrufen desselben Modells antworteten vier mit
   * HTTP 503 "This model is currently experiencing high demand". Ohne Wiederholung fiel damit
   * fast jede dritte Zustands-Extraktion aus — und zwar lautlos, weil der Aufrufer dann den
   * vorherigen Checkpoint fortschreibt: Ort, Uhrzeit, Inventar, Verletzungen und Erinnerungen
   * blieben stehen, während die Erzählung weiterlief.
   *
   * 429 wird nicht wiederholt: Googles Free-Tier begrenzt pro Modell und Tag, und gegen ein
   * erschöpftes Tageskontingent hilft kein dritter Versuch — er kostet nur zehn Sekunden,
   * bevor dieselbe Meldung erscheint.
   */
  private suspend fun executeWithRetry(request: Request): Response {
    var attempt = 0
    while (true) {
      val response = client.newCall(request).execute()
      val wait = if (response.isSuccessful) null else retryDelayMs(response.code, attempt)
      if (wait == null) return response

      response.close()
      Log.w(TAG, "Gemini antwortete ${response.code} — Versuch ${attempt + 2} von ${RETRY_DELAYS_MS.size + 1}")
      delay(wait)
      attempt++
    }
  }

  companion object {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    /** Fehler, die Google selbst als vorübergehend bezeichnet und die sich von allein erholen. */
    private val TRANSIENT_HTTP_CODES = setOf(500, 502, 503, 504)

    /** Wartezeiten zwischen den Versuchen. Vier Versuche insgesamt, höchstens 10 s Verzug. */
    private val RETRY_DELAYS_MS = longArrayOf(1_000L, 3_000L, 6_000L)

    /**
     * Wie lange vor dem nächsten Versuch gewartet wird — oder null, wenn nicht wiederholt wird.
     *
     * Steht als eigene Funktion da, weil diese Entscheidungstabelle binnen zweier Commits schon
     * einmal gekippt ist: 429 war erst drin, dann bewusst draußen. Beide Fehlrichtungen sind
     * teuer und beide bleiben ohne Test unsichtbar — fällt 503 heraus, kehrt das lautlose
     * Einfrieren des Spielstands zurück; kommt 429 zurück, verbraucht ein einziger abgewiesener
     * Aufruf vier statt einer Anfrage des Tageskontingents.
     *
     * @param attempt Nummer des bereits erfolgten Versuchs, beginnend bei 0.
     */
    internal fun retryDelayMs(code: Int, attempt: Int): Long? {
      if (code !in TRANSIENT_HTTP_CODES) return null
      if (attempt < 0 || attempt >= RETRY_DELAYS_MS.size) return null
      return RETRY_DELAYS_MS[attempt]
    }

    val THINKING_LEVEL_PRESETS = listOf(
      "LOW" to "Niedrig (Schnelle Reflexion, geringe Latenz)",
      "MEDIUM" to "Mittel (Ausgewogene Tiefe & psychologische Konsistenz)",
      "HIGH" to "Hoch (Tiefgründige Konsistenz & maximale Reflexion)"
    )

    val THINKING_BUDGET_PRESETS = listOf(
      0 to "Deaktiviert (Keine Denkzeit)",
      1024 to "Niedrig (1024 Token - Schnelle Reflexion)",
      2048 to "Standard (2048 Token - Ausgewogene Tiefe)",
      4096 to "Tiefgründig (4096 Token - Komplexe Psychologie)",
      8192 to "Maximum / Episch (8192 Token - Höchste logische Tiefe)"
    )

    val DEFAULT_CHAT_MODELS = listOf(
      GeminiModelInfo(
        id = GeminiDefaults.CHAT_MODEL,
        displayName = "Gemini 3.8 Flash (Aktuellstes Flaggschiff)",
        description = "Googles neuestes Modell mit anpassbaren Denkstufen (Niedrig, Mittel, Hoch).",
        supportsTemperature = true,
        defaultTemperature = 0.85f,
        isThinkingModel = true,
        usesThinkingLevel = true,
        supportedThinkingLevels = listOf("LOW", "MEDIUM", "HIGH")
      ),
      GeminiModelInfo(
        id = "gemini-3.1-pro-preview",
        displayName = "Gemini 3.1 Pro (Höchste literarische Tiefe)",
        description = "Für tiefgründige Erzählungen, unvorhersehbare Twists und komplexe Psychologie.",
        supportsTemperature = true,
        defaultTemperature = 0.85f,
        isThinkingModel = true,
        usesThinkingLevel = true,
        supportedThinkingLevels = listOf("LOW", "MEDIUM", "HIGH")
      ),
      GeminiModelInfo(
        id = "gemini-3.5-flash",
        displayName = "Gemini 3.5 Flash (Allrounder)",
        description = "Ausgewogenes Verhältnis zwischen Reaktionszeit und logischer Tiefe.",
        supportsTemperature = true,
        defaultTemperature = 0.85f,
        isThinkingModel = true,
        usesThinkingLevel = true,
        supportedThinkingLevels = listOf("LOW", "MEDIUM", "HIGH")
      )
    )

    val DEFAULT_EMBEDDING_MODELS = listOf(
      GeminiModelInfo(
        id = GeminiDefaults.EMBEDDING_MODEL,
        displayName = "Gemini Embedding 001 (Empfohlen)",
        description = "Vektorgedächtnis mit 3072 Dimensionen für die semantische Erinnerungssuche.",
        supportsTemperature = false,
        isThinkingModel = false,
        usesThinkingLevel = false,
        supportedThinkingLevels = emptyList()
      )
    )

    val DEFAULT_TRANSCRIPTION_MODELS = listOf(
      GeminiModelInfo(
        id = GeminiDefaults.TRANSCRIPTION_MODEL,
        displayName = "Gemini 3.5 Transcribe (Empfohlen für Sprache)",
        description = "Googles eigenes Sprachmodell für präzise Transkription.",
        supportsTemperature = false,
        isThinkingModel = false,
        usesThinkingLevel = false,
        supportedThinkingLevels = emptyList()
      ),
      GeminiModelInfo(
        id = GeminiDefaults.CHAT_MODEL,
        displayName = "Gemini 3.8 Flash (Multimodal)",
        description = "Ausweichlösung, falls kein dediziertes Sprachmodell verfügbar ist.",
        supportsTemperature = false,
        isThinkingModel = false,
        usesThinkingLevel = false,
        supportedThinkingLevels = emptyList()
      )
    )

    val DEFAULT_FALLBACK_MODELS = DEFAULT_CHAT_MODELS

    /**
     * Modelle, die ListModels zwar auflistet, die beim Aufruf aber mit HTTP 404 antworten.
     *
     * Googles Katalog trägt für abgekündigte Modelle kein Kennzeichen — `gemini-2.5-flash` sieht
     * dort aus wie jedes gesunde Modell und liefert dennoch "no longer available to new users".
     * Nutzbarkeit lässt sich daher nur beim echten Aufruf feststellen; hier wird das Ergebnis
     * für die Laufzeit der App gemerkt, damit ein totes Modell nicht in den Auswahllisten
     * stehen bleibt.
     */
    private val unusableModels: MutableSet<String> =
      java.util.concurrent.ConcurrentHashMap.newKeySet()

    fun markUnusable(modelId: String) {
      val clean = modelId.removePrefix("models/").trim()
      if (clean.isNotBlank() && unusableModels.add(clean)) {
        Log.w(TAG, "Modell als nicht nutzbar markiert (404): $clean")
      }
    }

    fun isUnusable(modelId: String): Boolean =
      unusableModels.contains(modelId.removePrefix("models/").trim())

    /**
     * Liefert die Modell-Id, die tatsächlich verwendet werden soll.
     *
     * Steht die gespeicherte Wahl nicht mehr im Live-Katalog oder ist sie beim Aufruf schon
     * einmal mit 404 abgewiesen worden, gewinnt das beste verfügbare Modell. Damit heilen sich
     * bestehende Spielstände selbst, ohne dass eine Datenbank-Migration nötig wäre.
     */
    fun resolveModel(stored: String, catalog: List<GeminiModelInfo>, fallback: String): String {
      val clean = stored.removePrefix("models/").trim()
      if (clean.isNotBlank() && !isUnusable(clean) && catalog.any { it.id == clean }) {
        return clean
      }
      return catalog.firstOrNull { !isUnusable(it.id) }?.id ?: fallback
    }

    /**
     * Prüft, ob Google die Anfrage oder Antwort über Inhalts- und Sicherheitsrichtlinien abgewiesen hat.
     */
    internal fun checkContentSafetyBlock(json: JSONObject) {
      val promptFeedback = json.optJSONObject("promptFeedback")
      val blockReason = promptFeedback?.optString("blockReason", "").orEmpty()
      if (blockReason.isNotBlank()) {
        val reasonMsg = when (blockReason.uppercase()) {
          "PROHIBITED_CONTENT", "SAFETY" ->
            "Google hat diese Eingabe aufgrund von Inhaltsrichtlinien blockiert (Sicherheitsfilter: $blockReason). Bitte formuliere deine Aktion etwas um."
          "BLOCKLIST" ->
            "Die Eingabe enthält einen von Google gesperrten Begriff ($blockReason)."
          else ->
            "Die Anfrage wurde von Google blockiert ($blockReason). Bitte passe die Eingabe an."
        }
        throw IllegalStateException(reasonMsg)
      }

      val candidates = json.optJSONArray("candidates")
      if (candidates != null && candidates.length() > 0) {
        val candidate = candidates.getJSONObject(0)
        val finishReason = candidate.optString("finishReason", "").uppercase()
        if (finishReason in listOf("SAFETY", "RECITATION", "BLOCKLIST", "PROHIBITED_CONTENT")) {
          throw IllegalStateException("Die KI-Antwort wurde durch Googles Sicherheitsfilter blockiert ($finishReason). Bitte versuche eine andere Formulierung.")
        }
      }
    }
  }

  /**
   * Der Schlüssel des Nutzers — und ausschließlich der.
   *
   * Früher lag hier ein Rückfall auf `BuildConfig.GEMINI_API_KEY`. Trägt jemand für einen
   * lokalen Build einen echten Schlüssel in die `.env` ein, landet der als
   * `public static final String` im APK, wo `strings classes.dex` genügt, um ihn zu lesen —
   * und niemand würde merken, dass die App gar nicht den Schlüssel benutzt, der in den
   * Einstellungen steht. Ohne Eintrag gibt es hier nichts, und die Oberfläche sagt das auch.
   */
  fun getEffectiveApiKey(): String =
    customApiKeyProvider()?.trim()?.replace("\\s+".toRegex(), "").orEmpty()

  /**
   * Builds a request that carries the API key in the `x-goog-api-key` header.
   *
   * The key must never end up in the URL: request URLs leak into logcat, crash reports
   * and proxies (siehe CLAUDE.md §6). Der Header ist zugleich der von Google dokumentierte Weg.
   */
  private fun buildRequest(url: String, apiKey: String, body: RequestBody? = null): Request =
    Request.Builder()
      .url(url)
      .addHeader("x-goog-api-key", apiKey)
      .apply { if (body != null) post(body) else get() }
      .build()

  /**
   * Fetches the current live list of models dynamically from Google Gemini API
   * and segregates them into Chat, Embedding, and Transcription catalogs.
   */
  suspend fun fetchModelCatalog(): com.example.data.model.GeminiModelCatalog = withContext(Dispatchers.IO) {
    val apiKey = getEffectiveApiKey()
    if (apiKey.isBlank()) {
      return@withContext com.example.data.model.GeminiModelCatalog(
        chatModels = DEFAULT_CHAT_MODELS,
        embeddingModels = DEFAULT_EMBEDDING_MODELS,
        transcriptionModels = DEFAULT_TRANSCRIPTION_MODELS
      )
    }

    try {
      val response = client.newCall(buildRequest(BASE_URL, apiKey)).execute()

      if (response.isSuccessful) {
        val responseText = response.body?.string().orEmpty()
        val root = JSONObject(responseText)
        val modelsArray = root.optJSONArray("models") ?: JSONArray()

        val parsedChatModels = mutableListOf<GeminiModelInfo>()
        val parsedEmbeddingModels = mutableListOf<GeminiModelInfo>()
        val parsedTranscriptionModels = mutableListOf<GeminiModelInfo>()

        for (i in 0 until modelsArray.length()) {
          val modelObj = modelsArray.getJSONObject(i)
          val rawName = modelObj.optString("name", "")
          val id = rawName.removePrefix("models/")
          val displayName = modelObj.optString("displayName", id)
          val description = modelObj.optString("description", "")
          val methods = modelObj.optJSONArray("supportedGenerationMethods") ?: JSONArray()
          val methodsList = mutableListOf<String>()
          for (j in 0 until methods.length()) {
            methodsList.add(methods.getString(j))
          }

          // Ein Modell, das beim Aufruf schon einmal 404 geliefert hat, gehört in keine Liste.
          if (isUnusable(id)) continue

          // Check temperature support
          val hasExplicitTemp = modelObj.has("temperature")
          val defaultTemp = modelObj.optDouble("temperature", 0.85).toFloat()
          val supportsTemp = hasExplicitTemp || (!id.contains("thinking-only") && !id.contains("o1"))

          // Determine thinking level support
          val isGemini3Plus = id.contains("gemini-3") || id.contains("-3.")
          val isGemini25 = id.contains("gemini-2.5")
          val isThinking = isGemini3Plus || isGemini25 || id.contains("thinking")
          val thinkingLevels = if (isGemini3Plus) {
            listOf("LOW", "MEDIUM", "HIGH")
          } else emptyList()

          val modelInfo = GeminiModelInfo(
            id = id,
            displayName = displayName,
            description = description,
            supportsTemperature = supportsTemp,
            defaultTemperature = defaultTemp,
            isThinkingModel = isThinking,
            usesThinkingLevel = isGemini3Plus,
            supportedThinkingLevels = thinkingLevels
          )

          val voiceInfo = modelInfo.copy(
            supportsTemperature = false,
            isThinkingModel = false,
            usesThinkingLevel = false,
            supportedThinkingLevels = emptyList()
          )

          if (GeminiModelFilter.isEmbeddingModel(id, methodsList)) {
            parsedEmbeddingModels.add(voiceInfo)
          }
          if (GeminiModelFilter.isChatModel(id, methodsList)) {
            parsedChatModels.add(modelInfo)
          }
          if (GeminiModelFilter.isTranscriptionModel(id, methodsList)) {
            parsedTranscriptionModels.add(voiceInfo)
          }
        }

        val sortedChat = GeminiModelFilter.sortByVersion(parsedChatModels) { it.id }
        val sortedEmbedding = GeminiModelFilter.sortByVersion(parsedEmbeddingModels) { it.id }
        val sortedTranscription = GeminiModelFilter.sortTranscription(parsedTranscriptionModels) { it.id }

        return@withContext com.example.data.model.GeminiModelCatalog(
          chatModels = sortedChat.ifEmpty { DEFAULT_CHAT_MODELS },
          embeddingModels = sortedEmbedding.ifEmpty { DEFAULT_EMBEDDING_MODELS },
          transcriptionModels = sortedTranscription.ifEmpty { DEFAULT_TRANSCRIPTION_MODELS },
          isLive = true
        )
      } else {
        // Auth-Fehler hier nicht verschlucken: sonst sehen die Modell-Dropdowns gesund aus,
        // obwohl der Schlüssel gar nicht funktioniert.
        Log.e(TAG, "Model catalog request rejected: ${response.code}")
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to dynamically query models from Google: ${e.message}")
    }

    com.example.data.model.GeminiModelCatalog(
      chatModels = DEFAULT_CHAT_MODELS,
      embeddingModels = DEFAULT_EMBEDDING_MODELS,
      transcriptionModels = DEFAULT_TRANSCRIPTION_MODELS,
      isLive = false
    )
  }

  /**
   * Schaltet die vier einstellbaren Sicherheitsfilter vollständig ab.
   *
   * Schwelle `BLOCK_NONE`: BLOCK_NONE deaktiviert das Blockieren für alle vier konfigurierbaren
   * Kategorien vollständig ("Always show regardless of probability of content being unsafe").
   * `HARM_CATEGORY_CIVIC_INTEGRITY` ist von Google als deprecated markiert ("the election filter
   * is no longer supported") und darf nicht mehr gesendet werden.
   * Kindersicherheit bleibt serverseitig immer aktiv und ist nicht abschaltbar.
   */
  private fun buildSafetyNoneArray(): JSONArray {
    val safetyArray = JSONArray()
    val categories = listOf(
      "HARM_CATEGORY_SEXUALLY_EXPLICIT",
      "HARM_CATEGORY_HATE_SPEECH",
      "HARM_CATEGORY_HARASSMENT",
      "HARM_CATEGORY_DANGEROUS_CONTENT"
    )
    for (cat in categories) {
      val s = JSONObject()
      s.put("category", cat)
      s.put("threshold", "BLOCK_NONE")
      safetyArray.put(s)
    }
    return safetyArray
  }

  /**
   * Googles Standard-Sicherheitsfilter (mittlere und hohe Risiken werden geblockt).
   */
  private fun buildSafetyStandardArray(): JSONArray {
    val safetyArray = JSONArray()
    val categories = listOf(
      "HARM_CATEGORY_SEXUALLY_EXPLICIT",
      "HARM_CATEGORY_HATE_SPEECH",
      "HARM_CATEGORY_HARASSMENT",
      "HARM_CATEGORY_DANGEROUS_CONTENT"
    )
    for (cat in categories) {
      val s = JSONObject()
      s.put("category", cat)
      s.put("threshold", "BLOCK_MEDIUM_AND_ABOVE")
      safetyArray.put(s)
    }
    return safetyArray
  }

  /**
   * Streams the next turn from Gemini via Server-Sent Events (SSE).
   */
  fun streamGenerateContent(
    model: String,
    systemInstruction: String,
    stateJson: String,
    currentInGameTime: String = "",
    milestones: List<String> = emptyList(),
    daySummaries: List<String> = emptyList(),
    npcProfiles: List<String> = emptyList(),
    semanticMemories: List<String> = emptyList(),
    episodicSummary: String = "",
    recentHistory: List<Pair<String, String>>,
    userAction: String,
    temperature: Float = 0.85f,
    supportsTemperature: Boolean = true,
    thinkingLevel: String = "MEDIUM",
    thinkingBudget: Int = 2048,
    allowAdultContent: Boolean = true
  ): Flow<String> = flow {
    val apiKey = getEffectiveApiKey()
    if (apiKey.isBlank()) {
      throw IllegalStateException("Kein Gemini API-Schlüssel hinterlegt. Bitte öffne die Einstellungen und trage deinen Key ein.")
    }

    val cleanModel = model.removePrefix("models/").trim()
    val url = "$BASE_URL/$cleanModel:streamGenerateContent?alt=sse"

    val payload = JSONObject()

    // System instruction
    val sysObj = JSONObject()
    val sysParts = JSONArray()
    sysParts.put(JSONObject().put("text", systemInstruction))
    sysObj.put("parts", sysParts)
    payload.put("system_instruction", sysObj)

    // Contents
    val contents = JSONArray()

    // 1st anchor message: Zeitrechnung, Zustand, Figuren und die vier Gedächtnisschichten
    val stateAnchorText = buildString {
      if (currentInGameTime.isNotBlank()) {
        appendLine(TimeAnchor.buildTimeRules(currentInGameTime))
        appendLine()
      }

      appendLine("[AKTUELLER WELTZUSTAND / CHECKPOINT]:")
      // Kein erfundener Ersatzzustand mehr. "Tag 1, 09:00 Uhr" und "Startort" waren für das
      // Modell nicht von einer echten Angabe zu unterscheiden -- es übernahm beides und legte
      // damit Zeit und Ort fest, bevor der Spieler das erste Wort geschrieben hatte.
      appendLine(
        stateJson.ifBlank {
          "Die Geschichte hat noch nicht begonnen. Es gibt keinen Ort, keine Uhrzeit und " +
            "keine Figuren -- leite alles aus dem Prompt und der ersten Eingabe des Spielers her."
        }
      )

      try {
        if (stateJson.isNotBlank()) {
          val root = JSONObject(stateJson)
          val injuriesArr = root.optJSONArray("injuries")
          if (injuriesArr != null && injuriesArr.length() > 0) {
            appendLine("\n[KÖRPERLICHE VERLETZUNGEN & ANATOMISCHER ZUSTAND]:")
            for (i in 0 until injuriesArr.length()) {
              val obj = injuriesArr.optJSONObject(i) ?: continue
              val charName = obj.optString("character", "Spieler")
              val part = obj.optString("body_part", "-")
              val desc = obj.optString("description", "")
              val severity = obj.optString("severity", "MEDIUM")
              val treated = if (obj.optBoolean("is_treated", false)) " (erstversorgt/verbunden)" else " (offen/schmerzend)"
              appendLine("- $charName: $part -> $desc [$severity]$treated")
            }
            appendLine("Regel: Beachte diese physischen Verletzungen, Schmerzen und Einschränkungen bei allen Aktionen und Dialogen!")
          }
        }
      } catch (_: Exception) { }

      if (npcProfiles.isNotEmpty()) {
        appendLine("\n[FIGUREN - KANONISCHES ERSCHEINUNGSBILD UND BEZIEHUNG]:")
        appendLine("Diese Beschreibungen sind verbindlich und unveränderlich. Erfinde NIEMALS")
        appendLine("abweichende Haar-, Augen- oder Körpermerkmale. Änderungen geschehen nur,")
        appendLine("wenn die Handlung sie ausdrücklich herbeiführt (Haarschnitt, Narbe, Verletzung).")
        npcProfiles.forEach { p -> appendLine(p) }
      }

      if (milestones.isNotEmpty()) {
        appendLine("\n[BEDEUTSAME MEILENSTEINE & LANGZEITERINNERUNGEN]:")
        milestones.forEach { m -> appendLine("- $m") }
      }

      if (daySummaries.isNotEmpty()) {
        appendLine("\n[DIE LETZTEN TAGE IM ÜBERBLICK]:")
        daySummaries.forEach { d -> appendLine("- $d") }
      }

      if (episodicSummary.isNotBlank()) {
        appendLine("\n[WAS BISHER GESCHAH (EPISODISCHE ZUSAMMENFASSUNG)]:")
        appendLine(episodicSummary)
      }

      if (semanticMemories.isNotEmpty()) {
        appendLine("\n[ERINNERUNGEN AUS DEM TIEFEN GEDÄCHTNIS]:")
        appendLine("Jede Zeile trägt ihre Tagesnummer und den Abstand zu heute. Halte dich daran.")
        semanticMemories.forEach { m -> appendLine("- $m") }
      }
    }

    val rawTurns = mutableListOf<Pair<String, String>>()
    rawTurns.add("user" to stateAnchorText)
    rawTurns.add("model" to "Verstanden. Ich kenne den aktuellen Weltzustand, die Kleidung aller Personen, das Inventar, frühere Meilensteine und halte mich strikt an die Dialogregeln.")

    // Recent conversation history (sliding window)
    for ((role, text) in recentHistory) {
      if (text.isNotBlank()) {
        rawTurns.add((if (role == "user") "user" else "model") to text)
      }
    }

    // Latest user action
    rawTurns.add("user" to "[SPIELER-AKTION]:\n$userAction")

    // Normalize: Merge consecutive turns with the same role so it strictly alternates user/model
    val mergedTurns = mutableListOf<Pair<String, String>>()
    for (turn in rawTurns) {
      if (mergedTurns.isNotEmpty() && mergedTurns.last().first == turn.first) {
        val last = mergedTurns.removeAt(mergedTurns.size - 1)
        mergedTurns.add(last.first to "${last.second}\n\n${turn.second}")
      } else {
        mergedTurns.add(turn)
      }
    }

    for ((role, text) in mergedTurns) {
      val msgObj = JSONObject()
      msgObj.put("role", role)
      msgObj.put("parts", JSONArray().put(JSONObject().put("text", text)))
      contents.put(msgObj)
    }

    payload.put("contents", contents)

    // Generation config
    val genConfig = JSONObject()
    if (supportsTemperature) {
      genConfig.put("temperature", temperature.coerceIn(0.0f, 2.0f))
    }
    genConfig.put("topP", 0.95)
    // 8192 Tokens stellen sicher, dass Thinking-Tokens plus vollwertige Erzählantworten nicht abgeschnitten werden
    genConfig.put("maxOutputTokens", 8192)

    // Dynamic Thinking Config:
    // Gemini 3.x+ models use thinkingLevel ("LOW", "MEDIUM", "HIGH")
    // Gemini 2.5 models use thinkingBudget (Tokens)
    val isGemini3Plus = model.contains("gemini-3") || model.contains("-3.")
    if (isGemini3Plus) {
      if (thinkingLevel != "OFF") {
        val safeLevel = when (thinkingLevel.trim().uppercase()) {
          "LOW", "MINIMAL" -> "LOW"
          "HIGH" -> "HIGH"
          else -> "MEDIUM"
        }
        val thinkingObj = JSONObject()
        thinkingObj.put("thinkingLevel", safeLevel)
        genConfig.put("thinkingConfig", thinkingObj)
      }
    } else if (model.contains("2.5") || model.contains("thinking")) {
      if (thinkingBudget > 0) {
        val thinkingObj = JSONObject()
        thinkingObj.put("thinkingBudget", thinkingBudget)
        genConfig.put("thinkingConfig", thinkingObj)
      }
    }

    payload.put("generationConfig", genConfig)

    // Full Safety settings across all categories
    payload.put(
      "safetySettings",
      if (allowAdultContent) buildSafetyNoneArray() else buildSafetyStandardArray()
    )

    val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
    val request = buildRequest(url, apiKey, requestBody)

    val response = executeWithRetry(request)
    if (!response.isSuccessful) {
      val errBody = response.body?.string() ?: "Unknown error"
      Log.e(TAG, "Gemini API error code: ${response.code} body: $errBody")
      if (response.code == 404) markUnusable(cleanModel)
      val errorMsg = parseErrorMessage(response.code, errBody)
      throw Exception(errorMsg)
    }

    val body = response.body ?: throw Exception("Leere Antwort von Gemini erhalten.")
    val reader = BufferedReader(InputStreamReader(body.byteStream()))

    var line: String?
    while (reader.readLine().also { line = it } != null) {
      val l = line?.trim() ?: continue
      if (l.startsWith("data:")) {
        val jsonData = l.substring(5).trim()
        if (jsonData == "[DONE]" || jsonData.isBlank()) continue
        try {
          val json = JSONObject(jsonData)
          checkContentSafetyBlock(json)
          val textChunk = extractTextFromCandidates(json, includeThoughts = false)
          if (textChunk.isNotBlank()) {
            emit(textChunk)
          }
        } catch (e: Exception) {
          if (e is IllegalStateException) throw e
          Log.w(TAG, "SSE chunk parse error: ${e.message}")
        }
      }
    }
  }.flowOn(Dispatchers.IO)

  /**
   * Extrahiert den neuen Spielzustand (Ort, Wetter, Kleidung, Inventar, NPCs, Meilensteine)
   * aus dem letzten Zug als striktes JSON.
   *
   * Gedanken-Tokens des Modells werden über [extractTextFromCandidates] herausgefiltert —
   * sonst parste die Engine das JSON aus dem inneren Monolog und übersah den finalen Zustand.
   * Ein Fallback auf das Vorrundenmodell existiert nicht: Scheitert die Extraktion, erfährt
   * es der Aufrufer als Exception, damit der Fehler nicht still im Hintergrund verschwindet.
   * Nur der 404-Fallback auf den Katalog ist erlaubt.
   *
   * [turnNumber] wurde entfernt: Sie war nur für das Logcat da, und dort gehört sie nicht hin —
   * nur dort ist auch bekannt, dass es einer war, und nur von dort erfährt es der Spieler.
   */
  suspend fun extractStructuredState(
    model: String,
    thinkingLevel: String,
    thinkingBudget: Int,
    prompt: String
  ): JSONObject = withContext(Dispatchers.IO) {
    val apiKey = getEffectiveApiKey()
    if (apiKey.isBlank()) {
      throw IllegalStateException("Kein Gemini API-Schlüssel hinterlegt.")
    }

    val cleanModel = model.removePrefix("models/").trim()
    val url = "$BASE_URL/$cleanModel:generateContent"

    val extractionPrompt = prompt


    val payload = JSONObject()
    val contents = JSONArray()
    val msg = JSONObject()
    msg.put("role", "user")
    msg.put("parts", JSONArray().put(JSONObject().put("text", extractionPrompt)))
    contents.put(msg)
    payload.put("contents", contents)

    val genConfig = JSONObject()
    genConfig.put("responseMimeType", "application/json")
    genConfig.put("temperature", 0.2)
    genConfig.put("maxOutputTokens", 8192)

    val isGemini3Plus = cleanModel.contains("gemini-3") || cleanModel.contains("-3.")
    val isThinkingModel = isGemini3Plus || cleanModel.contains("2.5") || cleanModel.contains("thinking")

    if (isThinkingModel) {
      if (isGemini3Plus && thinkingLevel != "OFF") {
        val safeLevel = when (thinkingLevel.trim().uppercase()) {
          "LOW", "MINIMAL" -> "LOW"
          "HIGH" -> "HIGH"
          else -> "MEDIUM"
        }
        val thinkingObj = JSONObject().apply {
          put("thinkingLevel", safeLevel)
        }
        genConfig.put("thinkingConfig", thinkingObj)
      } else if (!isGemini3Plus && thinkingBudget > 0) {
        val thinkingObj = JSONObject().apply {
          put("thinkingBudget", thinkingBudget)
        }
        genConfig.put("thinkingConfig", thinkingObj)
      }
    }

    payload.put("generationConfig", genConfig)
    payload.put("safetySettings", buildSafetyNoneArray())

    val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
    val request = buildRequest(url, apiKey, requestBody)

    val response = executeWithRetry(request)
    if (!response.isSuccessful) {
      val err = response.body?.string() ?: ""
      Log.e(TAG, "State extraction failed: ${response.code} $err")
      if (response.code == 404) markUnusable(cleanModel)
      throw Exception(parseErrorMessage(response.code, err))
    }

    // Über alle Teile hinweg zusammensetzen, nicht nur parts[0]: Denkende Modelle liefern
    // die Antwort gern in mehreren Stücken, und das erste allein ist dann kein gültiges JSON.
    // Gedanken-Fragmente (thought: true) werden dabei standardmäßig herausgefiltert.
    val responseBodyString = response.body?.string().orEmpty()
    val jsonObj = JSONObject(responseBodyString)
    checkContentSafetyBlock(jsonObj)
    val rawText = extractTextFromCandidates(jsonObj, includeThoughts = false)
    val text = if (rawText.contains('{')) {
      rawText.trim()
    } else {
      // Fallback: Falls das Modell fälschlicherweise das JSON im Gedanken-Part lieferte
      extractTextFromCandidates(jsonObj, includeThoughts = true).trim()
    }

    val firstBrace = text.indexOf('{')
    val lastBrace = text.lastIndexOf('}')
    if (firstBrace == -1 || lastBrace == -1 || firstBrace > lastBrace) {
      throw IllegalStateException("Gemini lieferte zur Zustands-Extraktion kein valides JSON-Objekt: $text")
    }
    val cleanJsonText = text.substring(firstBrace, lastBrace + 1)

    JSONObject(cleanJsonText)
  }

  /**
   * Lightweight connection test.
   */
  suspend fun testConnection(
    apiKey: String,
    model: String = GeminiDefaults.CHAT_MODEL
  ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
    val cleanKey = apiKey.trim().replace("\\s+".toRegex(), "")
    if (cleanKey.isBlank()) {
      return@withContext false to "API-Schlüssel darf nicht leer sein."
    }

    // Geprüft wird mit dem Modell, das die Geschichte wirklich benutzt. Ein fest verdrahtetes
    // Testmodell hat genau den Fehler erzeugt, den der Test eigentlich finden soll.
    val cleanModel = model.removePrefix("models/").trim().ifBlank { GeminiDefaults.CHAT_MODEL }
    val url = "$BASE_URL/$cleanModel:generateContent"
    val payload = JSONObject()
    val contents = JSONArray()
    val msg = JSONObject()
    msg.put("role", "user")
    msg.put("parts", JSONArray().put(JSONObject().put("text", "Ping. Antworte mit: Pong")))
    contents.put(msg)
    payload.put("contents", contents)
    payload.put("generationConfig", JSONObject())

    val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

    try {
      val response = client.newCall(buildRequest(url, cleanKey, body)).execute()
      if (response.isSuccessful) {
        return@withContext true to "Verbindung erfolgreich! Google Gemini hat geantwortet."
      }
      val err = response.body?.string() ?: ""
      Log.e(TAG, "Connection test failed: ${response.code}")
      if (response.code == 404) markUnusable(cleanModel)
      false to parseErrorMessage(response.code, err)
    } catch (e: Exception) {
      false to "Verbindungsfehler: ${e.localizedMessage ?: e.message}"
    }
  }

  suspend fun transcribeAudio(
    audioBytes: ByteArray,
    mimeType: String = "audio/mp4",
    model: String = GeminiDefaults.TRANSCRIPTION_MODEL
  ): String = withContext(Dispatchers.IO) {
    val apiKey = getEffectiveApiKey()
    if (apiKey.isBlank()) {
      throw IllegalStateException("Kein Gemini API-Schlüssel hinterlegt.")
    }

    val cleanModel = model.removePrefix("models/").trim()
    val url = "$BASE_URL/$cleanModel:generateContent"

    val base64Audio = android.util.Base64.encodeToString(audioBytes, android.util.Base64.NO_WRAP)

    val payload = JSONObject().apply {
      val contentsArr = JSONArray()
      val userTurn = JSONObject().apply {
        put("role", "user")
        val partsArr = JSONArray().apply {
          put(JSONObject().apply {
            put("inlineData", JSONObject().apply {
              put("mimeType", mimeType)
              put("data", base64Audio)
            })
          })
          put(JSONObject().apply {
            put("text", "Transkribiere das gesprochene Audio präzise auf Deutsch. Gib ausschließlich den gesprochenen Text ohne Zeitstempel, Formatierungen, Anmerkungen oder Einleitungen zurück.")
          })
        }
        put("parts", partsArr)
      }
      contentsArr.put(userTurn)
      put("contents", contentsArr)

      // Dedizierte Sprachmodelle wie gemini-3.5-transcribe kennen keine thinkingConfig und
      // weisen die Anfrage damit ab. Sie denken nicht nach, sie hören zu — die Namensprüfung
      // auf "gemini-3" hätte ihnen trotzdem eine Denkstufe mitgeschickt.
      val isDedicatedTranscriber = cleanModel.contains("transcribe")
      val isGemini3Plus = cleanModel.contains("gemini-3") || cleanModel.contains("-3.")
      val isThinkingModel = !isDedicatedTranscriber &&
        (isGemini3Plus || cleanModel.contains("2.5") || cleanModel.contains("thinking"))

      val genConfig = JSONObject().apply {
        put("temperature", 0.0)
        if (isThinkingModel) {
          val thinkingObj = JSONObject().apply {
            if (isGemini3Plus) {
              put("thinkingLevel", "LOW")
            } else {
              put("thinkingBudget", 0)
            }
          }
          put("thinkingConfig", thinkingObj)
        }
      }
      put("generationConfig", genConfig)
    }
    payload.put("safetySettings", buildSafetyNoneArray())

    val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
    val request = buildRequest(url, apiKey, requestBody)

    val response = executeWithRetry(request)
    if (!response.isSuccessful) {
      val errBody = response.body?.string() ?: "Unknown error"
      Log.e(TAG, "Audio transcription error: ${response.code} body: $errBody")
      if (response.code == 404) markUnusable(cleanModel)
      val errorMsg = parseErrorMessage(response.code, errBody)
      throw Exception(errorMsg)
    }

    val body = response.body?.string() ?: throw Exception("Leere Antwort erhalten.")
    val json = JSONObject(body)
    checkContentSafetyBlock(json)
    val text = extractTextFromCandidates(json).trim()
    if (text.isBlank()) {
      throw Exception("Es konnte keine Sprache erkannt werden.")
    }
    text
  }

  /**
   * @param taskType Steuert, wofür der Vektor optimiert wird: "RETRIEVAL_DOCUMENT" beim Ablegen
   *   einer Erinnerung, "RETRIEVAL_QUERY" beim Suchen. Google bettet beide Seiten dadurch passend
   *   zueinander ein, was Treffer über lange Zeiträume verlässlicher macht.
   */
  suspend fun generateEmbedding(
    text: String,
    model: String = GeminiDefaults.EMBEDDING_MODEL,
    taskType: String? = null
  ): String? = withContext(Dispatchers.IO) {
    val apiKey = getEffectiveApiKey()
    if (apiKey.isBlank() || text.isBlank()) return@withContext null
    
    val cleanModel = model.removePrefix("models/").trim()
    val url = "$BASE_URL/$cleanModel:embedContent"
    val payload = JSONObject()
    payload.put("model", "models/$cleanModel")
    if (!taskType.isNullOrBlank()) {
      payload.put("taskType", taskType)
    }
    val content = JSONObject()
    val parts = JSONArray()
    parts.put(JSONObject().put("text", text))
    content.put("parts", parts)
    payload.put("content", content)
    
    val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
    val request = buildRequest(url, apiKey, body)

    try {
      val response = executeWithRetry(request)
      if (response.isSuccessful) {
        val jsonStr = response.body?.string() ?: return@withContext null
        val root = JSONObject(jsonStr)
        val embeddingObj = root.optJSONObject("embedding")
        val valuesArr = embeddingObj?.optJSONArray("values")
        return@withContext valuesArr?.toString()
      }
      // Ein stiller Fehlschlag hier kostet die Geschichte ihr semantisches Gedächtnis, ohne dass
      // es jemandem auffällt. Deshalb protokollieren und das Modell als untauglich merken.
      if (response.code == 404) markUnusable(cleanModel)
      Log.e(TAG, "Embedding rejected for model $cleanModel: HTTP ${response.code}")
    } catch (e: Exception) {
      Log.e(TAG, "Embedding failed", e)
    }
    return@withContext null
  }

  private fun extractTextFromCandidates(json: JSONObject, includeThoughts: Boolean = false): String {
    val candidates = json.optJSONArray("candidates") ?: return ""
    if (candidates.length() == 0) return ""
    val candidate = candidates.getJSONObject(0)
    val content = candidate.optJSONObject("content") ?: return ""
    val parts = content.optJSONArray("parts") ?: return ""
    val sb = StringBuilder()
    for (i in 0 until parts.length()) {
      val p = parts.getJSONObject(i)
      if (!includeThoughts && p.optBoolean("thought", false)) {
        continue
      }
      val t = p.optString("text", "")
      sb.append(t)
    }
    return sb.toString()
  }

  /**
   * Reads Google's machine-readable `error.details[].reason` (google.rpc.ErrorInfo).
   * Genau dieses Feld unterscheidet „Schlüssel falsch" von „Schlüssel-Typ nicht unterstützt".
   */
  private fun extractErrorReason(body: String): String {
    return try {
      val details = JSONObject(body).optJSONObject("error")?.optJSONArray("details") ?: return ""
      for (i in 0 until details.length()) {
        val reason = details.optJSONObject(i)?.optString("reason", "").orEmpty()
        if (reason.isNotBlank()) return reason
      }
      ""
    } catch (_: Exception) {
      ""
    }
  }

  /**
   * Unterscheidet die kurze Drosselung vom erschöpften Tageskontingent.
   *
   * Googles 429 heißt beides. Der Free-Tier erlaubt pro Modell und Tag nur eine feste Zahl von
   * Anfragen (am 2026-09-12 zwanzig für `gemini-3.8-flash`) — "warte einen Moment" wäre dann
   * eine Falschauskunft, die den Nutzer die Ursache in der App suchen lässt. Google selbst
   * liefert die Unterscheidung in `quotaId` und die Wartezeit in `retryDelay`.
   */
  private fun buildRateLimitMessage(body: String): String {
    val perDay = body.contains("PerDay", ignoreCase = true)
    val retryHint = Regex("\"retryDelay\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1)
    val model = Regex("model:\\s*([\\w.\\-]+)").find(body)?.groupValues?.get(1)

    return buildString {
      if (perDay) {
        append("Das Tageskontingent deines Schlüssels ist aufgebraucht")
        model?.let { append(" (Modell $it)") }
        append(". Es füllt sich erst wieder auf — wähle in den Einstellungen ein anderes Modell ")
        append("oder spiele morgen weiter.")
      } else {
        append("Zu viele Anfragen in kurzer Zeit (429).")
        retryHint?.let { append(" Google nennt eine Wartezeit von $it.") }
      }
    }
  }

  private fun parseErrorMessage(code: Int, body: String): String {
    // Auth-Fehler zuerst über den reason-Code auflösen – die HTTP-Codes allein sind mehrdeutig.
    when (extractErrorReason(body)) {
      "ACCESS_TOKEN_TYPE_UNSUPPORTED" ->
        return "Google hat diesen Schlüssel-Typ für die Gemini-API abgelehnt. Erzeuge in Google AI Studio unter 'Get API key' einen neuen Schlüssel für dasselbe Projekt und trage ihn hier erneut ein."
      "API_KEY_SERVICE_BLOCKED" ->
        return "Der Schlüssel ist für die Gemini-API gesperrt. Aktiviere die 'Generative Language API' im zugehörigen Google-Cloud-Projekt und prüfe die API-Einschränkungen des Schlüssels."
      "API_KEY_INVALID" ->
        return "Der API-Schlüssel ist ungültig oder wurde unvollständig kopiert. Bitte trage ihn erneut vollständig ein."
      "SERVICE_DISABLED" ->
        return "Die 'Generative Language API' ist im zugehörigen Google-Cloud-Projekt nicht aktiviert. Bitte aktiviere sie und versuche es erneut."
    }

    return when (code) {
      400 -> "Ungültige Anfrage (400). Überprüfe das gewählte Modell oder den API-Key. ($body)"
      401 -> "Authentifizierung fehlgeschlagen (401). Google hat den hinterlegten Schlüssel nicht akzeptiert. Prüfe ihn in den Einstellungen über 'Testen'."
      403 -> "Keine Berechtigung (403). Der Schlüssel darf nicht auf die Gemini-API zugreifen. Bitte prüfe seine Einschränkungen."
      404 -> "Dieses Modell ist über deinen Schlüssel nicht mehr erreichbar (404). Es wurde aus der Auswahl entfernt — wähle in den Einstellungen ein anderes."
      429 -> buildRateLimitMessage(body)
      500, 503 -> "Google Gemini Server temporär überlastet (500/503). Bitte versuche es in wenigen Sekunden erneut."
      else -> "Fehler beim Aufruf der Gemini API (Code $code): $body"
    }
  }
}
