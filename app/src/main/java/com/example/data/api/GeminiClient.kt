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
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
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
      "MINIMAL" to "Minimal (Höchste Geschwindigkeit, minimale Denkzeit)",
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

    val DEFAULT_FALLBACK_MODELS = listOf(
      GeminiModelInfo(
        id = "gemini-3.8-flash",
        displayName = "Gemini 3.8 Flash (Aktuellstes Flaggschiff)",
        description = "Googles neuestes Modell mit anpassbaren Denkstufen (Minimal, Low, Medium, High).",
        supportsTemperature = true,
        defaultTemperature = 0.85f,
        isThinkingModel = true,
        usesThinkingLevel = true,
        supportedThinkingLevels = listOf("MINIMAL", "LOW", "MEDIUM", "HIGH")
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
        supportedThinkingLevels = listOf("MINIMAL", "LOW", "MEDIUM", "HIGH")
      ),
      GeminiModelInfo(
        id = "gemini-3.1-flash-lite-preview",
        displayName = "Gemini 3.1 Flash-Lite (Turbo)",
        description = "Optimiert für blitzschnelle Reaktionen und direkte Dialoge.",
        supportsTemperature = true,
        defaultTemperature = 0.85f,
        isThinkingModel = true,
        usesThinkingLevel = true,
        supportedThinkingLevels = listOf("MINIMAL", "LOW", "MEDIUM")
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
    if (!custom.isNullOrBlank()) return custom.trim()
    val buildKey = BuildConfig.GEMINI_API_KEY
    if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") {
      return buildKey
    }
    return ""
  }

  /**
   * Fetches the current live list of models dynamically from Google Gemini API.
   * Filters out legacy prohibited models, parses temperature and thinking levels.
   */
  suspend fun fetchAvailableModels(): List<GeminiModelInfo> = withContext(Dispatchers.IO) {
    val apiKey = getEffectiveApiKey()
    if (apiKey.isBlank()) {
      return@withContext DEFAULT_FALLBACK_MODELS
    }

    try {
      val url = URL("https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey")
      val conn = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 8000
        readTimeout = 8000
      }

      if (conn.responseCode == 200) {
        val responseText = conn.inputStream.bufferedReader().use { it.readText() }
        val root = JSONObject(responseText)
        val modelsArray = root.optJSONArray("models") ?: JSONArray()
        val parsedList = mutableListOf<GeminiModelInfo>()

        for (i in 0 until modelsArray.length()) {
          val modelObj = modelsArray.getJSONObject(i)
          val rawName = modelObj.optString("name", "")
          val id = rawName.removePrefix("models/")
          val methods = modelObj.optJSONArray("supportedGenerationMethods") ?: JSONArray()
          var supportsGenerateContent = false
          for (j in 0 until methods.length()) {
            if (methods.getString(j) == "generateContent") {
              supportsGenerateContent = true
              break
            }
          }
          if (!supportsGenerateContent) continue

          // Filter out disallowed legacy or non-text models
          if (id.contains("embedding") || id.contains("tts") || id.contains("veo") || id.contains("aqa")) continue
          if (id.startsWith("gemini-1.5") || id.startsWith("gemini-1.0") || id.startsWith("gemini-2.0")) continue

          val displayName = modelObj.optString("displayName", id)
          val description = modelObj.optString("description", "")

          // Check temperature support
          val hasExplicitTemp = modelObj.has("temperature")
          val defaultTemp = modelObj.optDouble("temperature", 0.85).toFloat()
          // Certain reasoning models enforce fixed temperature
          val supportsTemp = hasExplicitTemp || (!id.contains("thinking-only") && !id.contains("o1"))

          // Determine thinking level support
          // Gemini 3.x+ models use thinkingLevel ("MINIMAL", "LOW", "MEDIUM", "HIGH")
          val isGemini3Plus = id.contains("gemini-3") || id.contains("-3.")
          val isGemini25 = id.contains("gemini-2.5")
          val isThinking = isGemini3Plus || isGemini25 || id.contains("thinking")

          val thinkingLevels = if (isGemini3Plus) {
            if (id.contains("pro")) {
              listOf("LOW", "MEDIUM", "HIGH")
            } else {
              listOf("MINIMAL", "LOW", "MEDIUM", "HIGH")
            }
          } else emptyList()

          parsedList.add(
            GeminiModelInfo(
              id = id,
              displayName = displayName,
              description = description,
              supportsTemperature = supportsTemp,
              defaultTemperature = defaultTemp,
              isThinkingModel = isThinking,
              usesThinkingLevel = isGemini3Plus,
              supportedThinkingLevels = thinkingLevels
            )
          )
        }

        if (parsedList.isNotEmpty()) {
          // Sort: Prioritize newest flagship models at the top (3.8 -> 3.1 pro -> 3.5 -> 2.5)
          parsedList.sortWith(compareByDescending<GeminiModelInfo> { it.id.contains("3.8") }
            .thenByDescending { it.id.contains("3.1-pro") }
            .thenByDescending { it.id.contains("3.5") }
            .thenByDescending { it.id.contains("3.1") }
            .thenByDescending { it.id.contains("2.5") })
          return@withContext parsedList
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to dynamically query models from Google: ${e.message}")
    }

    DEFAULT_FALLBACK_MODELS
  }

  /**
   * Builds the safety settings JSON array with BLOCK_NONE across all 5 harm categories
   */
  private fun buildFullBlockNoneSafetyArray(): JSONArray {
    val safetyArray = JSONArray()
    val categories = listOf(
      "HARM_CATEGORY_SEXUALLY_EXPLICIT",
      "HARM_CATEGORY_HATE_SPEECH",
      "HARM_CATEGORY_HARASSMENT",
      "HARM_CATEGORY_DANGEROUS_CONTENT",
      "HARM_CATEGORY_CIVIC_INTEGRITY"
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
   * Streams the next turn from Gemini via Server-Sent Events (SSE).
   */
  fun streamGenerateContent(
    model: String,
    systemInstruction: String,
    stateJson: String,
    milestones: List<String> = emptyList(),
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

    val url = "$BASE_URL/$model:streamGenerateContent?alt=sse&key=$apiKey"

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
      if (milestones.isNotEmpty()) {
        appendLine("\n[BEDEUTSAME MEILENSTEINE & LANGZEITERINNERUNGEN]:")
        milestones.forEach { m -> appendLine("- $m") }
      }
      if (episodicSummary.isNotBlank()) {
        appendLine("\n[WAS BISHER GESCHAH (EPISODISCHE ZUSAMMENFASSUNG)]:")
        appendLine(episodicSummary)
      }
    }

    val anchorObj = JSONObject()
    anchorObj.put("role", "user")
    anchorObj.put("parts", JSONArray().put(JSONObject().put("text", stateAnchorText)))
    contents.put(anchorObj)

    val anchorAck = JSONObject()
    anchorAck.put("role", "model")
    anchorAck.put("parts", JSONArray().put(JSONObject().put("text", "Verstanden. Ich kenne den aktuellen Weltzustand, die Kleidung aller Personen, das Inventar, frühere Meilensteine und halte mich strikt an die Dialogregeln.")))
    contents.put(anchorAck)

    // Recent conversation history (sliding window)
    for ((role, text) in recentHistory) {
      val msgObj = JSONObject()
      msgObj.put("role", if (role == "user") "user" else "model")
      msgObj.put("parts", JSONArray().put(JSONObject().put("text", text)))
      contents.put(msgObj)
    }

    // Latest user action
    val latestActionObj = JSONObject()
    latestActionObj.put("role", "user")
    latestActionObj.put("parts", JSONArray().put(JSONObject().put("text", "[SPIELER-AKTION]:\n$userAction")))
    contents.put(latestActionObj)

    payload.put("contents", contents)

    // Generation config
    val genConfig = JSONObject()
    if (supportsTemperature) {
      genConfig.put("temperature", temperature.coerceIn(0.0f, 2.0f))
    }
    genConfig.put("topP", 0.95)

    // Dynamic Thinking Config:
    // Gemini 3.x+ models use thinkingLevel ("MINIMAL", "LOW", "MEDIUM", "HIGH")
    // Gemini 2.5 models use thinkingBudget (Tokens)
    val isGemini3Plus = model.contains("gemini-3") || model.contains("-3.")
    if (isGemini3Plus) {
      if (thinkingLevel != "OFF") {
        val thinkingObj = JSONObject()
        thinkingObj.put("thinkingLevel", thinkingLevel)
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
      payload.put("safetySettings", buildFullBlockNoneSafetyArray())
    }

    val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
    val request = Request.Builder()
      .url(url)
      .post(requestBody)
      .build()

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

    val url = "$BASE_URL/$model:generateContent?key=$apiKey"

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

    // For state extraction, use minimal thinking effort to respond instantly
    val isGemini3Plus = model.contains("gemini-3") || model.contains("-3.")
    if (isGemini3Plus) {
      val thinkingObj = JSONObject()
      thinkingObj.put("thinkingLevel", "MINIMAL")
      genConfig.put("thinkingConfig", thinkingObj)
    } else if (model.contains("2.5")) {
      val thinkingObj = JSONObject()
      thinkingObj.put("thinkingBudget", 0)
      genConfig.put("thinkingConfig", thinkingObj)
    }

    payload.put("generationConfig", genConfig)
    payload.put("safetySettings", buildFullBlockNoneSafetyArray())

    val requestBody = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
    val request = Request.Builder()
      .url(url)
      .post(requestBody)
      .build()

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
    if (apiKey.isBlank()) {
      return@withContext false to "API-Schlüssel darf nicht leer sein."
    }

    // Try testing with latest model or flash
    val url = "$BASE_URL/gemini-3.8-flash:generateContent?key=${apiKey.trim()}"
    val payload = JSONObject()
    val contents = JSONArray()
    val msg = JSONObject()
    msg.put("role", "user")
    msg.put("parts", JSONArray().put(JSONObject().put("text", "Ping. Antworte mit einem Wort: Pong")))
    contents.put(msg)
    payload.put("contents", contents)

    val genConfig = JSONObject()
    genConfig.put("thinkingConfig", JSONObject().put("thinkingLevel", "MINIMAL"))
    payload.put("generationConfig", genConfig)

    val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
    val request = Request.Builder().url(url).post(body).build()

    try {
      val response = client.newCall(request).execute()
      if (response.isSuccessful) {
        true to "Verbindung erfolgreich! Google Gemini 3.8 Flash hat geantwortet."
      } else {
        val err = response.body?.string() ?: ""
        false to parseErrorMessage(response.code, err)
      }
    } catch (e: Exception) {
      false to "Verbindungsfehler: ${e.localizedMessage ?: e.message}"
    }
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

  private fun parseErrorMessage(code: Int, body: String): String {
    return when (code) {
      400 -> "Ungültige Anfrage (400). Überprüfe das gewählte Modell oder die Parameter. ($body)"
      403 -> "Ungültiger API-Schlüssel oder keine Berechtigung (403). Bitte überprüfe deinen Schlüssel in Google AI Studio."
      404 -> "Modell nicht gefunden (404). Wähle ein unterstütztes Modell wie gemini-3.8-flash."
      429 -> "Ratenlimit erreicht (429). Bitte warte einen Moment, bevor du weiterspielst."
      500, 503 -> "Google Gemini Server temporär überlastet (500/503). Bitte versuche es in wenigen Sekunden erneut."
      else -> "Fehler beim Aufruf der Gemini API (Code $code): $body"
    }
  }
}
