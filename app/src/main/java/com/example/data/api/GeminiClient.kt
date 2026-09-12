package com.example.data.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.GeminiModelInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
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

  companion object {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

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
        id = "gemini-3.8-flash",
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
      ),
      GeminiModelInfo(
        id = "gemini-3.1-flash-lite-preview",
        displayName = "Gemini 3.1 Flash-Lite (Turbo)",
        description = "Optimiert für blitzschnelle Reaktionen und direkte Dialoge.",
        supportsTemperature = true,
        defaultTemperature = 0.85f,
        isThinkingModel = true,
        usesThinkingLevel = true,
        supportedThinkingLevels = listOf("LOW", "MEDIUM", "HIGH")
      ),
      GeminiModelInfo(
        id = "gemini-2.5-flash",
        displayName = "Gemini 2.5 Flash (Token-Budget Thinking)",
        description = "Nutzt das klassische tokenbasierte Thinking-Budget.",
        supportsTemperature = true,
        defaultTemperature = 0.85f,
        isThinkingModel = true,
        usesThinkingLevel = false,
        supportedThinkingLevels = emptyList()
      )
    )

    val DEFAULT_EMBEDDING_MODELS = listOf(
      GeminiModelInfo(
        id = "text-embedding-004",
        displayName = "Text Embedding 004 (Empfohlen)",
        description = "Aktuellstes Modell für semantisches episodisches Vektorgedächtnis.",
        supportsTemperature = false,
        isThinkingModel = false,
        usesThinkingLevel = false,
        supportedThinkingLevels = emptyList()
      ),
      GeminiModelInfo(
        id = "embedding-001",
        displayName = "Embedding 001 (Legacy)",
        description = "Klassisches Embedding-Modell für semantische Vektorsuche.",
        supportsTemperature = false,
        isThinkingModel = false,
        usesThinkingLevel = false,
        supportedThinkingLevels = emptyList()
      )
    )

    val DEFAULT_TRANSCRIPTION_MODELS = listOf(
      GeminiModelInfo(
        id = "gemini-2.5-flash",
        displayName = "Gemini 2.5 Flash (Empfohlen für Sprache)",
        description = "Hervorragende Audio- & Sprachtranskription mit minimaler Latenz.",
        supportsTemperature = false,
        isThinkingModel = false,
        usesThinkingLevel = false,
        supportedThinkingLevels = emptyList()
      ),
      GeminiModelInfo(
        id = "gemini-3.8-flash",
        displayName = "Gemini 3.8 Flash (Neueste Generation)",
        description = "Googles Flaggschiff für multimodale Transkription.",
        supportsTemperature = false,
        isThinkingModel = false,
        usesThinkingLevel = false,
        supportedThinkingLevels = emptyList()
      ),
      GeminiModelInfo(
        id = "gemini-3.5-flash",
        displayName = "Gemini 3.5 Flash",
        description = "Schnelle multimodale Verarbeitung.",
        supportsTemperature = false,
        isThinkingModel = false,
        usesThinkingLevel = false,
        supportedThinkingLevels = emptyList()
      ),
      GeminiModelInfo(
        id = "gemini-3.1-flash-lite-preview",
        displayName = "Gemini 3.1 Flash-Lite",
        description = "Extrem geringe Latenz bei kurzen Spracheingaben.",
        supportsTemperature = false,
        isThinkingModel = false,
        usesThinkingLevel = false,
        supportedThinkingLevels = emptyList()
      )
    )

    val DEFAULT_FALLBACK_MODELS = DEFAULT_CHAT_MODELS

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

          val supportsGenerateContent = methodsList.contains("generateContent")
          val supportsEmbedContent = methodsList.contains("embedContent") || id.contains("embedding")

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

          // 1. Embedding models
          if (supportsEmbedContent || id.contains("embedding")) {
            parsedEmbeddingModels.add(
              modelInfo.copy(
                supportsTemperature = false,
                isThinkingModel = false,
                usesThinkingLevel = false,
                supportedThinkingLevels = emptyList()
              )
            )
          }

          // Gemma taugt für MSF nicht: kein JSON-Mode (bricht die State-Extraction), kein
          // thinkingLevel und keine über safetySettings abschaltbaren Sicherheitsfilter.
          val isGemma = id.contains("gemma")

          // 2. Chat / Story Generation Models
          if (supportsGenerateContent && !isGemma && !id.contains("embedding") && !id.contains("tts") && !id.contains("veo") && !id.contains("imagen") && !id.contains("aqa")) {
            // Keep modern 2.5 and 3.x models
            if (!id.startsWith("gemini-1.0") && !id.startsWith("gemini-1.5") && !id.startsWith("gemini-2.0")) {
              parsedChatModels.add(modelInfo)
            }
          }

          // 3. Audio / Transcription models
          if (supportsGenerateContent && !isGemma && !id.contains("embedding") && !id.contains("imagen") && !id.contains("veo") && !id.contains("aqa")) {
            if (!id.startsWith("gemini-1.") && !id.startsWith("gemini-2.0") && (id.contains("flash") || id.contains("transcrib") || id.contains("audio"))) {
              parsedTranscriptionModels.add(
                modelInfo.copy(
                  supportsTemperature = false,
                  isThinkingModel = false,
                  usesThinkingLevel = false,
                  supportedThinkingLevels = emptyList()
                )
              )
            }
          }
        }

        // Sort chat models: 3.8 -> 3.1 pro -> 3.5 -> 3.1 -> 2.5
        parsedChatModels.sortWith(
          compareByDescending<GeminiModelInfo> { it.id.contains("3.8") }
            .thenByDescending { it.id.contains("3.1-pro") }
            .thenByDescending { it.id.contains("3.5") }
            .thenByDescending { it.id.contains("3.1") }
            .thenByDescending { it.id.contains("2.5") }
        )

        // Sort embedding models: 004 -> 001
        parsedEmbeddingModels.sortWith(
          compareByDescending<GeminiModelInfo> { it.id.contains("004") }
            .thenByDescending { it.id.contains("001") }
        )

        // Sort transcription models: 2.5-flash (recommended) -> 3.8-flash -> 3.5-flash
        parsedTranscriptionModels.sortWith(
          compareByDescending<GeminiModelInfo> { it.id == "gemini-2.5-flash" }
            .thenByDescending { it.id.contains("3.8") }
            .thenByDescending { it.id.contains("3.5") }
            .thenByDescending { it.id.contains("2.5") }
        )

        return@withContext com.example.data.model.GeminiModelCatalog(
          chatModels = if (parsedChatModels.isNotEmpty()) parsedChatModels else DEFAULT_CHAT_MODELS,
          embeddingModels = if (parsedEmbeddingModels.isNotEmpty()) parsedEmbeddingModels else DEFAULT_EMBEDDING_MODELS,
          transcriptionModels = if (parsedTranscriptionModels.isNotEmpty()) parsedTranscriptionModels else DEFAULT_TRANSCRIPTION_MODELS,
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
    milestones: List<String> = emptyList(),
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

    // 1st anchor message: State + Episodic summary + Milestones as context anchor
    val stateAnchorText = buildString {
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

      if (milestones.isNotEmpty()) {
        appendLine("\n[BEDEUTSAME MEILENSTEINE & LANGZEITERINNERUNGEN]:")
        milestones.forEach { m -> appendLine("- $m") }
      }
      if (episodicSummary.isNotBlank()) {
        appendLine("\n[WAS BISHER GESCHAH (EPISODISCHE ZUSAMMENFASSUNG)]:")
        appendLine(episodicSummary)
      }

      if (semanticMemories.isNotEmpty()) {
        appendLine("\n[SEMANTISCHE ERINNERUNGEN (TIEFES GEDÄCHTNIS)]:")
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

    val response = client.newCall(request).execute()
    if (!response.isSuccessful) {
      val errBody = response.body?.string() ?: "Unknown error"
      Log.e(TAG, "Gemini API error code: ${response.code} body: $errBody")
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
    milestones: List<String> = emptyList(),
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
    milestones = milestones,
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
      "name": "NPC Name",
      "outfit": "Kleidung/Zustand dieses NPCs",
      "relationship_to_player": "Aktuelle Beziehung/Haltung zum Spieler",
      "current_mood": "Stimmung",
      "status": "Anwesend oder Abwesend"
    }
  ],
  "injuries": [
    {
      "character": "Du oder NPC-Name",
      "body_part": "HEAD, NECK, CHEST, ABDOMEN, LEFT_ARM, RIGHT_ARM, LEFT_HAND, RIGHT_HAND, LEFT_LEG, RIGHT_LEG oder FEET",
      "description": "Exakte Wundbeschreibung z. B. Schnittwunde, Brandblase, Prellung",
      "severity": "LIGHT, MEDIUM, SEVERE oder CRITICAL",
      "is_treated": false
    }
  ],
  "milestones": [
    "Dauerhafte, prägende Ereignisse, Zeitsprünge, Schwüre, Enthüllungen, intime Momente oder Verluste"
  ],
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

[BISHERIGER ZUSTAND]:
$currentStateJson

[BISHERIGE MEILENSTEINE]:
$milestonesBlock

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
      val response = client.newCall(request).execute()
      if (!response.isSuccessful) {
        val err = response.body?.string() ?: ""
        Log.e(TAG, "State extraction failed: ${response.code} $err")
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
  suspend fun testConnection(apiKey: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
    val cleanKey = apiKey.trim().replace("\\s+".toRegex(), "")
    if (cleanKey.isBlank()) {
      return@withContext false to "API-Schlüssel darf nicht leer sein."
    }

    // Ein Modell genügt: Ein Authentifizierungsfehler wird durch einen Modellwechsel nicht besser.
    val url = "$BASE_URL/gemini-2.5-flash:generateContent"
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
      false to parseErrorMessage(response.code, err)
    } catch (e: Exception) {
      false to "Verbindungsfehler: ${e.localizedMessage ?: e.message}"
    }
  }

  suspend fun transcribeAudio(
    audioBytes: ByteArray,
    mimeType: String = "audio/mp4",
    model: String = "gemini-2.5-flash"
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

      val isGemini3Plus = cleanModel.contains("gemini-3") || cleanModel.contains("-3.")
      val isThinkingModel = isGemini3Plus || cleanModel.contains("2.5") || cleanModel.contains("thinking")
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

    val response = client.newCall(request).execute()
    if (!response.isSuccessful) {
      val errBody = response.body?.string() ?: "Unknown error"
      Log.e(TAG, "Audio transcription error: ${response.code} body: $errBody")
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

  suspend fun generateEmbedding(text: String, model: String = "text-embedding-004"): String? = withContext(Dispatchers.IO) {
    val apiKey = getEffectiveApiKey()
    if (apiKey.isBlank() || text.isBlank()) return@withContext null
    
    val cleanModel = model.removePrefix("models/").trim()
    val url = "$BASE_URL/$cleanModel:embedContent"
    val payload = JSONObject()
    payload.put("model", "models/$cleanModel")
    val content = JSONObject()
    val parts = JSONArray()
    parts.put(JSONObject().put("text", text))
    content.put("parts", parts)
    payload.put("content", content)
    
    val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
    val request = buildRequest(url, apiKey, body)

    try {
      val response = client.newCall(request).execute()
      if (response.isSuccessful) {
        val jsonStr = response.body?.string() ?: return@withContext null
        val root = JSONObject(jsonStr)
        val embeddingObj = root.optJSONObject("embedding")
        val valuesArr = embeddingObj?.optJSONArray("values")
        return@withContext valuesArr?.toString()
      }
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
        return "Dieser Schlüssel-Typ wird von der Gemini-API nicht akzeptiert. Auth-Keys aus Google AI Studio (beginnen mit 'AQ.') werden derzeit abgelehnt. Erstelle in der Google Cloud Console unter 'Anmeldedaten' einen Standard-API-Schlüssel (beginnt mit 'AIzaSy…') und beschränke ihn auf die 'Generative Language API'."
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
      404 -> "Modell nicht gefunden (404). Wähle ein unterstütztes Modell wie gemini-2.5-flash."
      429 -> "Ratenlimit erreicht (429). Bitte warte einen Moment, bevor du weiterspielst."
      500, 503 -> "Google Gemini Server temporär überlastet (500/503). Bitte versuche es in wenigen Sekunden erneut."
      else -> "Fehler beim Aufruf der Gemini API (Code $code): $body"
    }
  }
}
