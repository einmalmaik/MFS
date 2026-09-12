package com.example.data.api

import android.util.Log
import com.example.BuildConfig
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
   * 429 wird mitwiederholt: kurze Burst-Limits erholen sich in Sekunden. Ist das Tageskontingent
   * erschöpft, kostet die Wiederholung nur die Wartezeit und die Meldung bleibt dieselbe.
   */
  private suspend fun executeWithRetry(request: Request): Response {
    var attempt = 0
    while (true) {
      val response = client.newCall(request).execute()
      val transient = !response.isSuccessful && response.code in TRANSIENT_HTTP_CODES
      if (!transient || attempt >= RETRY_DELAYS_MS.size) return response

      response.close()
      Log.w(TAG, "Gemini antwortete ${response.code} — Versuch ${attempt + 2} von ${RETRY_DELAYS_MS.size + 1}")
      delay(RETRY_DELAYS_MS[attempt])
      attempt++
    }
  }

  companion object {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    /** Fehler, die Google selbst als vorübergehend bezeichnet. */
    private val TRANSIENT_HTTP_CODES = setOf(429, 500, 502, 503, 504)

    /** Wartezeiten zwischen den Versuchen. Vier Versuche insgesamt, höchstens 10 s Verzug. */
    private val RETRY_DELAYS_MS = longArrayOf(1_000L, 3_000L, 6_000L)

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

    val DEFAULT_SYSTEM_PROMPT = """
# ROLLE & ERZÄHLHALTUNG
Du bist ein virtuoser Game Master für eine kompromisslose, hochimmersive interaktive Geschichte.
Du steuerst die gesamte Umwelt, NPCs, Geräusche, Wetter und die realen Konsequenzen der Entscheidungen des Spielers.
Der Spieler steuert einzig und allein seinen eigenen Charakter.

# STRENGSTE DIALOG- & FORMATIERUNGS-REGELN:
1. GESPROCHENE WORTE HERVORHEBEN: Jedes gesprochene Wort von Charakteren MUSS ausnahmslos in Anführungszeichen gesetzt werden (z. B. "Wir haben nicht mehr viel Zeit", flüstert sie).
2. ATMOSPHÄRE & HINTERGRUND-AKTIONEN: Leise Geräusche, Nebenhandlungen oder Gedanken in Klammern setzen (z. B. (Im Gebälk über euch knarrt das morsche Holz)).
3. KEINE STATUS-ZUSAMMENFASSUNGEN IM TEXT: Schreibe NIEMALS Zusammenfassungen, Statusblöcke, Item-Listen oder "Tag X, XX:XX Uhr" in die eigentliche Geschichte! Der Text ist reines, ununterbrochenes Leseerlebnis. Alle Statusmeldungen laufen unsichtbar im Hintergrund ab.
4. ABSOLUTES VERBOT KITSCHIGER SPITZNAMEN: NPCs reden wie echte Menschen in Extremsituationen. Niemals künstliche Titel wie "Herr Wachtmeister", "Kumpel", "Boss", "Held" oder unpassende Kosenamen erfinden, es sei denn, die etablierte Vorgeschichte fordert es zwingend.
5. SUBTEXT & MIKRO-GESTIK: Menschen sagen selten direkt, was sie empfinden. Lass Gefühle durch Blicke, Zögern, die Haltung der Hände, verändertes Atmen und unausgesprochene Spannungen lebendig werden.

# AUTONOMES LEBEN DER WELT & NPC-AGENDA:
- Die Charaktere existieren unabhängig vom Spieler. Sie haben eigene Geheimnisse, Sehnsüchte, Verletzungen und Zeitpläne.
- Wenn der Spieler schweigt oder nichts tut, handeln NPCs eigenständig weiter und reden untereinander.
- Beziehe dich organisch auf gemeinsam Erlebtes von früheren Tagen (aus den [BEDEUTSAME MEILENSTEINE]-Einträgen), sodass die Beziehung wächst und niemals wie ein Reset wirkt.

# EXPLIZITE DETAILTREUE & KEIN ABBLENDEN:
- Sei schonungslos atmosphärisch und detailreich. Bei intimen, sinnlichen, erotischen oder brutalen Szenen wird NICHT ausgeblendet ("fade to black").
- Beschreibe physische Nähe, Berührungen, Schmerz, Angst, Atem und Verlangen mit realistischer Schärfe und psychologischem Gewicht.

# AGENTUR-GRENZE:
- Bestimme NIEMALS die Gedanken, Gefühle oder Reaktionen des Spieler-Charakters. Beschreibe nur, was um ihn geschieht.
""".trimIndent()
  }

  fun getEffectiveApiKey(): String {
    val custom = customApiKeyProvider()
    if (!custom.isNullOrBlank()) {
      return custom.trim().replace("\\s+".toRegex(), "")
    }
    val buildKey = BuildConfig.GEMINI_API_KEY
    if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") {
      return buildKey.trim().replace("\\s+".toRegex(), "")
    }
    return ""
  }

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

  suspend fun fetchAvailableModels(): List<GeminiModelInfo> = withContext(Dispatchers.IO) {
    fetchModelCatalog().chatModels
  }

  /**
   * Schaltet die vier einstellbaren Sicherheitsfilter ab.
   *
   * Schwelle `OFF` statt `BLOCK_NONE`: OFF deaktiviert den Filter komplett, BLOCK_NONE liefert
   * weiterhin Bewertungs-Metadaten. `HARM_CATEGORY_CIVIC_INTEGRITY` ist von Google als deprecated
   * markiert ("the election filter is no longer supported") und darf nicht mehr gesendet werden.
   * Kindersicherheit bleibt serverseitig immer aktiv und ist nicht abschaltbar.
   */
  private fun buildSafetyOffArray(): JSONArray {
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
      s.put("threshold", "OFF")
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
      appendLine(stateJson.ifBlank { "{\"in_game_time\": \"Tag 1, 09:00 Uhr\", \"location\": \"Startort\"}" })

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
    if (allowAdultContent) {
      payload.put("safetySettings", buildSafetyOffArray())
    }

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
          val textChunk = extractTextFromCandidates(json)
          if (textChunk.isNotBlank()) {
            emit(textChunk)
          }
        } catch (e: Exception) {
          Log.w(TAG, "SSE chunk parse error: ${e.message}")
        }
      }
    }
  }.flowOn(Dispatchers.IO)

  fun streamGenerateStory(
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
  ): Flow<String> = streamGenerateContent(
    model = model,
    systemInstruction = systemInstruction,
    stateJson = stateJson,
    currentInGameTime = currentInGameTime,
    milestones = milestones,
    daySummaries = daySummaries,
    npcProfiles = npcProfiles,
    semanticMemories = semanticMemories,
    episodicSummary = episodicSummary,
    recentHistory = recentHistory,
    userAction = userAction,
    temperature = temperature,
    supportsTemperature = supportsTemperature,
    thinkingLevel = thinkingLevel,
    thinkingBudget = thinkingBudget,
    allowAdultContent = allowAdultContent
  )

  /**
   * Fast background call: Extracts updated state after a story turn.
   */
  suspend fun extractUpdatedState(
    model: String,
    thinkingLevel: String = "MEDIUM",
    thinkingBudget: Int = 2048,
    currentStateJson: String,
    existingMilestones: List<String> = emptyList(),
    knownNpcs: List<String> = emptyList(),
    userAction: String,
    storyResponse: String = "",
    previousMilestones: List<String> = existingMilestones,
    modelNarrative: String = storyResponse
  ): JSONObject = withContext(Dispatchers.IO) {
    val apiKey = getEffectiveApiKey()
    if (apiKey.isBlank()) {
      return@withContext JSONObject(currentStateJson.ifBlank { "{}" })
    }

    val finalNarrative = storyResponse.ifBlank { modelNarrative }
    val finalMilestones = if (existingMilestones.isNotEmpty()) existingMilestones else previousMilestones

    val cleanModel = model.removePrefix("models/").trim()
    val url = "$BASE_URL/$cleanModel:generateContent"

    val milestonesBlock = if (finalMilestones.isNotEmpty()) {
      "Bisherige bedeutsame Meilensteine:\n" + finalMilestones.joinToString("\n") { "- $it" }
    } else {
      "Bisher keine Meilensteine verzeichnet."
    }

    // Ohne diese Liste erfindet das Modell bei jedem Zug neue Schreibweisen und Beschreibungen
    // für längst etablierte Figuren.
    val knownNpcsBlock = if (knownNpcs.isNotEmpty()) {
      "Diese Figuren sind bereits etabliert. Verwende exakt diese Namen und beschreibe ihr " +
        "Aussehen NICHT erneut:\n" + knownNpcs.joinToString("\n") { "- $it" }
    } else {
      "Bisher sind keine Figuren etabliert."
    }

    val extractionPrompt = """
Du bist die State-Tracking-Engine des interaktiven Spiels. Analysiere den bisherigen Zustand, die Spieler-Aktion und die Game-Master-Erzählung dieser Runde.
Gib AUSSCHLIESSLICH ein valides JSON-Objekt zurück, das exakt folgendes Schema erfüllt:

{
  "in_game_time": "Format IMMER: Tag X, HH:MM Uhr (z. B. Tag 3, 09:30 Uhr)",
  "location": "Aktueller Aufenthaltsort des Spielers",
  "weather": "Aktuelle Wetterlage & Atmosphäre",
  "player_outfit": "Aktuelle Kleidung/Rüstung (inkl. Adult/NSFW z. B. entblößt, Dessous, zerrissen, voller Schutz)",
  "player_condition": "Körperlicher/mentaler/erotischer Zustand (z. B. Unverletzt, Erschöpft, Erregt, Angeschlagen)",
  "player_inventory": ["Item 1", "Item 2"],
  "npcs": [
    {
      "name": "Vollständiger, IMMER gleich geschriebener Name dieser Figur",
      "gender": "MALE oder FEMALE",
      "appearance": "NUR beim ersten Auftreten ausfüllen: Haarfarbe, Frisur, Augenfarbe, Statur, Alter, auffällige Merkmale, Narben",
      "appearance_changed": false,
      "appearance_change_reason": "Nur setzen, wenn sich das Aussehen in DIESER Runde nachweislich geändert hat (Haarschnitt, Narbe, Entstellung)",
      "personality": "Wesenszüge, Sprechweise, Ängste, Sehnsüchte",
      "outfit": "Kleidung dieses NPCs",
      "condition": "KÖRPERLICHE Verfassung dieser Figur - dieselbe Skala wie player_condition (z. B. Unverletzt, Ausgezehrt, Dehydriert, Unterkühlt, Fiebrig, Erschöpft, Angeschlagen, Erregt). NICHT die Stimmung.",
      "relationship_to_player": "Aktuelle Beziehung/Haltung zum Spieler",
      "current_mood": "Seelische Stimmung - getrennt von der körperlichen Verfassung",
      "status": "Anwesend oder Abwesend",
      "is_alive": true,
      "knowledge": "Was diese Figur in DIESER Runde erlebt, erfahren oder empfunden hat - aus ihrer Sicht, in einem Satz. Leer lassen, wenn nichts Relevantes geschah."
    }
  ],
  "injuries": [
    {
      "character": "Du oder exakter NPC-Name",
      "body_part": "HEAD, NECK, CHEST, ABDOMEN, GENITALS, LEFT_ARM, RIGHT_ARM, LEFT_HAND, RIGHT_HAND, LEFT_LEG, RIGHT_LEG oder FEET",
      "organ": "NUR bei inneren Verletzungen: BRAIN, LEFT_EAR, RIGHT_EAR, NOSE, HEART, LEFT_LUNG, RIGHT_LUNG, STOMACH, LIVER, SPLEEN, LEFT_KIDNEY, RIGHT_KIDNEY, INTESTINES, BLADDER, UTERUS, LEFT_OVARY, RIGHT_OVARY, LEFT_TESTICLE oder RIGHT_TESTICLE. Sonst leer lassen.",
      "description": "Exakte Wundbeschreibung z. B. Schnittwunde, Brandblase, Prellung",
      "severity": "LIGHT, MEDIUM, SEVERE oder CRITICAL",
      "is_treated": false
    }
  ],
  "milestones": [
    "Dauerhafte, prägende Ereignisse, Zeitsprünge, Schwüre, Enthüllungen, intime Momente oder Verluste"
  ],
  "turn_memory": "1-2 nüchterne, faktische Sätze über das, was in DIESER Runde geschah. Nenne die beteiligten Personen beim Namen und den Ort. Keine Ausschmückung, keine Wertung - das ist ein Gedächtniseintrag, kein Erzähltext.",
  "completed_day_summary": "NUR ausfüllen, wenn in dieser Runde ein neuer Tag begonnen hat: ein Absatz über den gerade abgeschlossenen Tag. Sonst leer lassen.",
  "previous_events_summary": "1-3 prägnante Sätze über die wichtigsten Ereignisse bisher."
}

[REGELN ZUR ZEIT- & TAGEBERECHNUNG - EXTREM WICHTIG]:
1. Lies den bisherigen Zeitstempel genau (z. B. bisher "Tag 1, 20:00 Uhr").
2. Wenn in der Spieleraktion oder der Erzählung Tage oder Stunden vergehen (z. B. "es vergehen zwei Tage", "3 Tage später", "am nächsten Morgen", "wir schlafen eine Nacht", "zwei Tage Vorbereitung"), addiere diese Tage mathematisch zur bisherigen Tageszahl!
   - Beispiel: Aus bisher "Tag 1, 20:00 Uhr" wird bei "Es vergehen zwei Tage" zwingend "Tag 3, 08:00 Uhr" (oder 20:00 Uhr je nach Kontext).
   - Aus "Tag 1" wird nach einer Nacht "Tag 2, 07:30 Uhr".
3. Wetter & Atmosphäre: Bei Zeitsprüngen über Tage oder bei Ortswechseln MUSS sich das Wetter realistisch weiterentwickeln (z. B. Regen klart auf, Nebel zieht auf, schwüle Hitze, Sturm).
4. Meilensteine: Wenn ein Zeitsprung ("Zwei Tage im Lager verbracht") oder ein wichtiges Ereignis (Geheimnis gelüftet, Vertrauen gewonnen, Erotik/Intimität, Kampf) stattfand, MUSS ein neuer Meilenstein der Liste hinzugefügt werden. Behalte bestehende Meilensteine bei!
5. Adult Content & Erotik: Sei unzensiert und akkurat bei Kleidung und Verfassung.

[REGELN ZU FIGUREN - IDENTITÄT DARF NIEMALS DRIFTEN]:
6. Schreibe den Namen einer Figur IMMER exakt gleich. Aus "Lena" wird nicht in der nächsten Runde "Lena Vogt" oder "die Blonde".
7. Das Feld "appearance" füllst du NUR, wenn die Figur zum ersten Mal auftritt. Danach lässt du es leer.
   Erfinde niemals nachträglich eine andere Haar- oder Augenfarbe. Setze "appearance_changed" nur dann auf true, wenn die Erzählung dieser Runde die Änderung ausdrücklich schildert.
8. Führe JEDE Figur weiter, die bereits bekannt ist - auch abwesende. Setze bei Abwesenden "status": "Abwesend", statt sie wegzulassen.
9. "knowledge": Halte fest, was die Figur SELBST erlebt hat. Eine Gerettete erinnert sich an ihre Rettung und kann Tage später davon erzählen. Nur füllen, wenn für diese Figur wirklich etwas geschah.

[REGELN ZU VERLETZUNGEN - GILT FÜR ALLE, NICHT NUR DEN SPIELER]:
10. Erfasse körperliche Schäden für JEDE betroffene Person, also auch für NPCs. Wer im Kampf getroffen wird, bekommt einen Eintrag mit seinem Namen.
11. Übernimm bestehende Verletzungen unverändert, solange sie nicht versorgt oder verheilt sind. Entferne einen Eintrag erst, wenn die Wunde erzählerisch abgeheilt oder behandelt ist.
12. Schwere Wunden hinterlassen Narben. Ist eine SEVERE- oder CRITICAL-Wunde verheilt, vermerke die Narbe über "appearance_changed" im Erscheinungsbild der Figur.

[REGELN ZUM KÖRPERLICHEN ZUSTAND - MANGEL TRIFFT ALLE ANWESENDEN]:
12a. Hunger, Durst, Kälte, Hitze, schlechte Luft, Schlafmangel, Krankheit, Erschöpfung und Vergiftung entstehen aus der UMGEBUNG. Wer sich in derselben Lage befindet, ist ebenfalls betroffen - ausnahmslos. Trage das in "condition" JEDER anwesenden Figur ein, nicht nur in "player_condition".
12b. Der GRAD darf sich unterscheiden: Konstitution, Alter, Willenskraft, Vorräte, Vorerkrankungen und bisheriges Verhalten führen zu unterschiedlichen Ausprägungen. Eine zähe Figur ist "ausgezehrt, aber gefasst", eine geschwächte "am Rand des Zusammenbruchs". Das vollständige FEHLEN des Mangels ist niemals zulässig.
12c. Prüfe vor jeder Antwort: Wie lange dauert dieser Zustand schon an? Zwei Monate ohne Nahrung bedeuten für ALLE Beteiligten schwere Auszehrung. Niemand ist nach Wochen ohne Wasser oder Essen "völlig gesund".
12d. Der Zustand ist kumulativ und darf sich nicht ohne Grund zurücksetzen. Eine Besserung braucht eine Ursache im Text - gefundene Nahrung, Wasser, Wärme, Schlaf oder Behandlung.
12e. Halte "condition" (Körper) und "current_mood" (Seele) strikt getrennt. "Verängstigt" ist keine körperliche Verfassung, "unterernährt" keine Stimmung.

[REGELN ZUR NEGATIVEN SEITE - GENAUSO WICHTIG WIE DIE POSITIVE]:
13. Beziehungen dürfen und sollen sich verschlechtern. Halte Misstrauen, Groll, Angst, Eifersucht, Enttäuschung und Schuld genauso konsequent fest wie Zuneigung und Vertrauen.
14. Gebrochene Versprechen, Lügen, Verrat und Grausamkeit MÜSSEN als Meilenstein und im "knowledge" der betroffenen Figur landen. Eine Figur, die belogen wurde, vergisst das nicht.
15. Traumatische Erlebnisse wirken nach: Vermerke sie im Zustand und in der Persönlichkeit der Figur, nicht nur im Moment des Geschehens.

[BISHERIGER ZUSTAND]:
$currentStateJson

[BISHERIGE MEILENSTEINE]:
$milestonesBlock

[BEKANNTE FIGUREN]:
$knownNpcsBlock

[SPIELER-AKTION DIESER RUNDE]:
$userAction

[ANTWORT DES GAME MASTERS DIESER RUNDE]:
$storyResponse
""".trimIndent()

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
    payload.put("safetySettings", buildSafetyOffArray())

    val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
    val request = buildRequest(url, apiKey, requestBody)

    try {
      val response = executeWithRetry(request)
      if (!response.isSuccessful) {
        val err = response.body?.string() ?: ""
        Log.e(TAG, "State extraction failed: ${response.code} $err")
        if (response.code == 404) markUnusable(cleanModel)
        return@withContext JSONObject(currentStateJson.ifBlank { "{}" })
      }

      val respStr = response.body?.string() ?: "{}"
      val rootJson = JSONObject(respStr)
      val candidates = rootJson.optJSONArray("candidates") ?: return@withContext JSONObject(currentStateJson.ifBlank { "{}" })
      if (candidates.length() == 0) return@withContext JSONObject(currentStateJson.ifBlank { "{}" })

      val firstCandidate = candidates.getJSONObject(0)
      val content = firstCandidate.optJSONObject("content") ?: return@withContext JSONObject(currentStateJson.ifBlank { "{}" })
      val parts = content.optJSONArray("parts") ?: return@withContext JSONObject(currentStateJson.ifBlank { "{}" })
      if (parts.length() == 0) return@withContext JSONObject(currentStateJson.ifBlank { "{}" })

      val text = parts.getJSONObject(0).optString("text", "{}")
      val cleanJsonText = text.trim()
        .removePrefix("```json")
        .removePrefix("```")
        .removeSuffix("```")
        .trim()

      JSONObject(cleanJsonText)
    } catch (e: Exception) {
      Log.e(TAG, "Error in extractUpdatedState", e)
      JSONObject(currentStateJson.ifBlank { "{}" })
    }
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

  private fun extractTextFromCandidates(json: JSONObject): String {
    val candidates = json.optJSONArray("candidates") ?: return ""
    if (candidates.length() == 0) return ""
    val candidate = candidates.getJSONObject(0)
    val content = candidate.optJSONObject("content") ?: return ""
    val parts = content.optJSONArray("parts") ?: return ""
    val sb = StringBuilder()
    for (i in 0 until parts.length()) {
      val p = parts.getJSONObject(i)
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
      429 -> "Ratenlimit erreicht (429). Bitte warte einen Moment, bevor du weiterspielst."
      500, 503 -> "Google Gemini Server temporär überlastet (500/503). Bitte versuche es in wenigen Sekunden erneut."
      else -> "Fehler beim Aufruf der Gemini API (Code $code): $body"
    }
  }
}
