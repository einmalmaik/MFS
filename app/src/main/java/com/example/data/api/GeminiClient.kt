package com.example.data.api

import android.util.Log
import com.example.BuildConfig
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

    val AVAILABLE_MODELS = listOf(
      "gemini-2.5-flash" to "Gemini 2.5 Flash (Schnell & präzise)",
      "gemini-3.5-flash" to "Gemini 3.5 Flash (Next-Gen Allrounder)",
      "gemini-3.1-pro-preview" to "Gemini 3.1 Pro (Literarische Tiefe & High-Thinking)",
      "gemini-3.1-flash-lite-preview" to "Gemini 3.1 Flash-Lite (High-Speed Turbo)"
    )

    val THINKING_BUDGET_PRESETS = listOf(
      0 to "Deaktiviert (Keine Denkzeit)",
      1024 to "Niedrig (1024 Token - Schnelle Reflexion)",
      2048 to "Standard (2048 Token - Ausgewogene Tiefe)",
      4096 to "Tiefgründig (4096 Token - Komplexe Psychologie)",
      8192 to "Maximum / Episch (8192 Token - Höchste logische Tiefe)"
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
   * Streams content chunk by chunk for ultra low-latency response rendering.
   */
  fun streamGenerateStory(
    model: String,
    systemInstruction: String,
    stateJson: String,
    episodicSummary: String,
    milestones: List<String> = emptyList(),
    recentHistory: List<Pair<String, String>>, // role ("user" | "model"), text
    userAction: String,
    temperature: Float = 0.85f,
    thinkingBudget: Int = 2048,
    allowAdultContent: Boolean = true
  ): Flow<String> = flow {
    val apiKey = getEffectiveApiKey()
    if (apiKey.isBlank()) {
      throw IllegalStateException("API-Schlüssel fehlt. Bitte trage deinen Gemini API-Key in den Einstellungen ein.")
    }

    val url = "$BASE_URL/$model:streamGenerateContent?key=$apiKey&alt=sse"

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
    genConfig.put("temperature", temperature.coerceIn(0.0f, 2.0f))
    genConfig.put("topP", 0.95)

    // Add thinkingConfig if model supports it and budget > 0
    if (thinkingBudget > 0 && (model.contains("2.5") || model.contains("3."))) {
      val thinkingObj = JSONObject()
      thinkingObj.put("thinkingBudget", thinkingBudget)
      genConfig.put("thinkingConfig", thinkingObj)
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

  /**
   * Background extractor: updates discrete world state, time, inventory, outfits,
   * NPCs, and landmark memories via structured JSON output.
   */
  suspend fun extractUpdatedState(
    model: String,
    currentStateJson: String,
    userAction: String,
    modelNarrative: String,
    previousMilestones: List<String> = emptyList()
  ): JSONObject = withContext(Dispatchers.IO) {
    val apiKey = getEffectiveApiKey()
    if (apiKey.isBlank()) {
      return@withContext JSONObject(currentStateJson.ifBlank { "{}" })
    }

    val url = "$BASE_URL/gemini-2.5-flash:generateContent?key=$apiKey"

    val extractionPrompt = """
Du bist der präzise State-Tracking-Engine eines Text-RPGs.
Deine Aufgabe ist es, den neuen Weltzustand und wichtige Langzeiterinnerungen als exaktes JSON-Objekt zu aktualisieren.

Bisheriger Zustand:
$currentStateJson

Bisherige Meilensteine:
${previousMilestones.joinToString("\n- ")}

Spieler-Aktion im aktuellen Zug:
$userAction

Antwort des Game Masters im aktuellen Zug:
$modelNarrative

Gib AUSSCHLIESSLICH ein valides JSON-Objekt mit folgender Struktur zurück:
{
  "in_game_time": "Aktualisierte In-Game Zeit (z. B. 'Tag 1, 22:15 Uhr' oder 'Tag 2, 08:30 Uhr')",
  "location": "Aktueller Aufenthaltsort",
  "weather": "Aktuelles Wetter / Atmosphäre",
  "player_outfit": "Genaue Kleidung des Spielers (inklusive abgelegter oder gewechselter Teile)",
  "player_inventory": ["Gegenstand 1", "Gegenstand 2"],
  "player_condition": "Gesundheitszustand, Verletzungen, Müdigkeit",
  "npcs": [
    {
      "name": "Name des NPCs",
      "outfit": "Aktuelle Kleidung des NPCs",
      "relationship_to_player": "Beziehung zum Spieler (z. B. 'Misstrauisch', 'Zieht sich zurück', 'Verliebt', 'Loyal')",
      "current_mood": "Stimmung",
      "status": "Anwesend oder Abwesend"
    }
  ],
  "milestones": [
    "Prägende Schlüsselmomente, Schwüre, Enthüllungen oder Verluste, die auch an Tag 30 noch relevant sind"
  ],
  "previous_events_summary": "Kurze, prägnante Zusammenfassung (max. 3-4 Sätze) aller bisherigen Ereignisse als Kontext-Brücke"
}
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
    payload.put("generationConfig", genConfig)

    // BLOCK_NONE safety settings for state extraction
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

    val url = "$BASE_URL/gemini-2.5-flash:generateContent?key=${apiKey.trim()}"
    val payload = JSONObject()
    val contents = JSONArray()
    val msg = JSONObject()
    msg.put("role", "user")
    msg.put("parts", JSONArray().put(JSONObject().put("text", "Ping. Antworte mit einem Wort: Pong")))
    contents.put(msg)
    payload.put("contents", contents)

    val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
    val request = Request.Builder().url(url).post(body).build()

    try {
      val response = client.newCall(request).execute()
      if (response.isSuccessful) {
        true to "Verbindung erfolgreich! Gemini 2.5 Flash hat geantwortet."
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
      404 -> "Modell nicht gefunden (404). Wähle ein unterstütztes Modell wie gemini-2.5-flash."
      429 -> "Ratenlimit erreicht (429). Bitte warte einen Moment, bevor du weiterspielst."
      500, 503 -> "Gemini Server vorübergehend überlastet ($code). Bitte versuche es gleich erneut."
      else -> "Fehler ($code): $body"
    }
  }
}
