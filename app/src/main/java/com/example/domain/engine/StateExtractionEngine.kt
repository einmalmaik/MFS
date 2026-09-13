package com.example.domain.engine

import android.util.Log
import com.example.data.api.GeminiClient
import com.example.data.db.StoryDao
import com.example.data.model.CheckpointEntity
import com.example.data.model.MemoryKind
import com.example.data.model.StoryEntity
import com.example.domain.model.TimeAnchor
import com.example.domain.service.StoryPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Dedicated engine responsible for analyzing in-game events after each story turn,
 * extracting updated state (time, location, weather, outfit, inventory, NPCs, milestones),
 * and committing new checkpoints to the database.
 */
class StateExtractionEngine(
  private val geminiClient: GeminiClient,
  private val storyDao: StoryDao,
  private val memoryEngine: MemoryEngine,
  private val npcEngine: NpcEngine,
  private val preferences: StoryPreferences
) {
  companion object {
    private const val TAG = "StateExtractionEngine"

    /**
     * Deterministically calculates time progression when time skips are detected,
     * guaranteeing that statements like "Es vergehen zwei Tage" advance Day 1 -> Day 3 even if
     * the model hallucinated the same day.
     */
    fun computeDeterministicTimeProgression(
      previousTime: String,
      extractedTime: String,
      userAction: String,
      modelResponse: String
    ): String {
      // Erster Zug: Ein leerer bisheriger Zeitstempel heißt, dass die Geschichte gerade erst
      // anfängt. Dann ist der Tag zwingend 1 -- allein die Uhrzeit kommt aus der Erzählung.
      //
      // Das Modell rechnet hier sonst die Vorgeschichte mit: Aus "ich bin vor einer Woche in
      // diese Stadt gezogen" wird "Tag 8", und die Geschichte beginnt in ihrer eigenen Zukunft.
      // Alles Weitere haengt daran -- Wundalterung, Tageszusammenfassungen und der Abstand, den
      // TimeAnchor jeder Erinnerung mitgibt, rechnen ab dieser Zahl. Der Prompt sagt dem Modell
      // dasselbe (Regel 0); hier steht die Zusicherung, die auch dann gilt, wenn es nicht hoert.
      if (previousTime.isBlank()) {
        val timePart = TimeAnchor.parseTimeOfDay(extractedTime)
        return if (timePart.isBlank()) "Tag 1" else "Tag 1, $timePart"
      }

      val previousDay = TimeAnchor.parseDayNumber(previousTime)
      val extractedDay = TimeAnchor.parseDayNumber(extractedTime)

      // Check if user action requested a specific day jump
      val actionLower = userAction.lowercase()
      val daysToAdvance = when {
        actionLower.contains("zwei tage") || actionLower.contains("2 tage") -> 2
        actionLower.contains("drei tage") || actionLower.contains("3 tage") -> 3
        actionLower.contains("vier tage") || actionLower.contains("4 tage") -> 4
        actionLower.contains("fünf tage") || actionLower.contains("5 tage") -> 5
        actionLower.contains("eine woche") || actionLower.contains("1 woche") -> 7
        actionLower.contains("übernachten") || actionLower.contains("nächsten morgen") ||
          actionLower.contains("nächster morgen") || actionLower.contains("schlafe bis morgen") -> 1
        else -> {
          val regex = Regex("""(\d+)\s+tage(?:\s+später|\s+vergehen|\s+rasten|\s+warten)?""", RegexOption.IGNORE_CASE)
          val match = regex.find(userAction)
          match?.groupValues?.get(1)?.toIntOrNull() ?: 0
        }
      }

      if (daysToAdvance > 0) {
        val targetDay = previousDay + daysToAdvance
        if (extractedDay < targetDay) {
          // Model didn't advance enough, correct to target day
          val timePart = TimeAnchor.parseTimeOfDay(extractedTime).ifBlank {
            if (actionLower.contains("morgen")) "08:00 Uhr" else "20:00 Uhr"
          }
          return "Tag $targetDay, $timePart"
        }
      }

      // Die Uhr lief bisher nur in eine Richtung zu langsam -- zu schnell rückwärts war nie
      // geprüft. Nennt das Modell einen früheren Tag (ein Rückblick, eine verlesene Zahl), stand
      // dieser Tag danach im Spielstand. Was daran hängt, rechnet mit negativem dayDelta: Wunden
      // altern zurück, "Tag 3" folgt auf "Tag 5", und Tages-Zusammenfassungen greifen nie wieder,
      // weil sie einen größeren Tag verlangen.
      //
      // Der Tag bleibt deshalb stehen, die Tageszeit nicht: Ein reiner Rückfall auf previousTime
      // würde die Uhr in dem Moment anhalten, in dem das Modell einmal irrt, und die Geschichte
      // bliebe in derselben Minute stecken.
      if (extractedDay < previousDay) {
        val timePart = TimeAnchor.parseTimeOfDay(extractedTime)
          .ifBlank { TimeAnchor.parseTimeOfDay(previousTime) }
          .ifBlank { "20:00 Uhr" }
        return "Tag $previousDay, $timePart"
      }

      return extractedTime
    }

    /**
     * Tage, nach denen eine unbehandelte Wunde des jeweiligen Schweregrads als verheilt gilt.
     * CRITICAL fehlt bewusst: lebensbedrohliche Verletzungen heilen niemals von selbst weg.
     */
    private val HEALING_DAYS = mapOf(
      "LIGHT" to 2,
      "MEDIUM" to 6,
      "SEVERE" to 16
    )

    /** Ab diesem Schweregrad bleibt nach der Heilung eine Narbe zurück. */
    private val SCARRING_SEVERITIES = setOf("SEVERE", "CRITICAL")

    /**
     * Felder, die eine Figur dauerhaft beschreiben und deshalb nicht verloren gehen dürfen,
     * wenn eine einzelne Extraktion sie auslässt.
     */
    private val NPC_STICKY_FIELDS = listOf("condition", "gender")

    /**
     * Entscheidet, ob ein von der KI vorgeschlagener Wert übernommen wird.
     *
     * Übernommen wird nur in ein leeres Feld. Eine frisch angelegte Geschichte hat weder Titel
     * noch Genre und bekommt beides aus dem ersten Zug; hat der Spieler dagegen selbst einen
     * Titel gesetzt, gehört er ihm und darf bei keiner späteren Extraktion umbenannt werden.
     */
    fun fillIfBlank(current: String, extracted: String): String =
      if (current.isBlank() && extracted.isNotBlank()) extracted.trim() else current

    /**
     * Trägt [NPC_STICKY_FIELDS] aus dem vorherigen Checkpoint nach, wo die neue Extraktion sie
     * leer gelassen hat. Verändert [npcs] an Ort und Stelle.
     *
     * Zuordnung über den Namen: Die Extraktion ist angewiesen, ihn immer gleich zu schreiben,
     * und Teiltreffer würden hier eher schaden als helfen.
     */
    fun carryOverNpcFields(npcs: JSONArray, previousNpcsJson: String?) {
      if (previousNpcsJson.isNullOrBlank()) return

      val previousByName = mutableMapOf<String, JSONObject>()
      try {
        val previous = JSONArray(previousNpcsJson)
        for (i in 0 until previous.length()) {
          val obj = previous.optJSONObject(i) ?: continue
          val name = obj.optString("name").trim()
          if (name.isNotBlank()) previousByName[name.lowercase()] = obj
        }
      } catch (_: Exception) {
        return
      }

      for (i in 0 until npcs.length()) {
        val current = npcs.optJSONObject(i) ?: continue
        val previous = previousByName[current.optString("name").trim().lowercase()] ?: continue
        NPC_STICKY_FIELDS.forEach { field ->
          if (current.optString(field).isBlank()) {
            val carried = previous.optString(field)
            if (carried.isNotBlank()) current.put(field, carried)
          }
        }
      }
    }

    /**
     * Lässt Verletzungen über die vergangene Zeit deterministisch abklingen.
     *
     * Nötig, weil das Modell Wunden entweder für immer mitschleppt oder willkürlich verschwinden
     * lässt. Behandelte Wunden heilen doppelt so schnell; CRITICAL bleibt bis zur Behandlung.
     * Gibt die verbliebenen Verletzungen und die dabei entstandenen Narben zurück.
     */
    /**
     * Überträgt das Alter bereits bekannter Wunden auf die frisch extrahierten.
     *
     * Die KI wird nie nach `days_elapsed` gefragt -- das Feld gehört der Heilungsrechnung, nicht
     * der Erzählung. Nennt das Modell eine bestehende Wunde erneut (und das tut es, solange sie
     * offen ist), kommt sie ohne Alter zurück. Ohne diese Übertragung beginnt ihre Frist damit
     * in jedem Zug von vorn: Eine leichte Schnittwunde, die nach zwei Tagen verheilt sein müsste,
     * bleibt ewig offen, solange die Erzählung sie erwähnt.
     *
     * Zugeordnet wird über die id, sonst über Figur + Körperteil + Beschreibung. Weicht die
     * Beschreibung ab, greift Figur + Körperteil -- aber nur, wenn dort genau eine Wunde lag.
     * Bei zwei Wunden am selben Arm ist das falsche Alter schlechter als ein neuer Anfang.
     */
    fun carryOverInjuryAges(extracted: JSONArray, previous: JSONArray): JSONArray {
      val exact = mutableMapOf<String, Int>()
      val coarse = mutableMapOf<String, MutableList<Int>>()
      for (i in 0 until previous.length()) {
        val obj = previous.optJSONObject(i) ?: continue
        val age = obj.optInt("days_elapsed", 0)
        exact[injuryExactKey(obj)] = age
        obj.optString("id").takeIf { it.isNotBlank() }?.let { exact["id:$it"] = age }
        coarse.getOrPut(injuryCoarseKey(obj)) { mutableListOf() }.add(age)
      }

      val result = JSONArray()
      for (i in 0 until extracted.length()) {
        val obj = extracted.optJSONObject(i) ?: continue
        if (!obj.has("days_elapsed")) {
          val age = obj.optString("id").takeIf { it.isNotBlank() }?.let { exact["id:$it"] }
            ?: exact[injuryExactKey(obj)]
            ?: coarse[injuryCoarseKey(obj)]?.singleOrNull()
          if (age != null) obj.put("days_elapsed", age)
        }
        result.put(obj)
      }
      return result
    }

    private fun injuryCoarseKey(obj: JSONObject): String =
      obj.optString("character", "Du").trim().lowercase() + "|" +
        obj.optString("body_part").trim().uppercase()

    private fun injuryExactKey(obj: JSONObject): String =
      injuryCoarseKey(obj) + "|" + obj.optString("description").trim().lowercase()

    fun applyHealing(injuries: JSONArray, dayDelta: Int): HealingResult {
      if (dayDelta <= 0) return HealingResult(injuries, emptyList())

      val remaining = JSONArray()
      val scars = mutableListOf<Scar>()

      for (i in 0 until injuries.length()) {
        val injury = injuries.optJSONObject(i) ?: continue
        val severity = injury.optString("severity", "MEDIUM").uppercase()
        val treated = injury.optBoolean("is_treated", false)

        val baseDays = HEALING_DAYS[severity]
        if (baseDays == null) {
          // CRITICAL oder unbekannter Grad: bleibt bestehen.
          remaining.put(injury)
          continue
        }

        val daysNeeded = if (treated) (baseDays / 2).coerceAtLeast(1) else baseDays
        val elapsed = injury.optInt("days_elapsed", 0) + dayDelta

        if (elapsed >= daysNeeded) {
          if (severity in SCARRING_SEVERITIES) {
            scars.add(
              Scar(
                characterName = injury.optString("character", "Du"),
                description = injury.optString("description", "Verletzung")
              )
            )
          }
          continue
        }

        remaining.put(injury.put("days_elapsed", elapsed))
      }

      return HealingResult(remaining, scars)
    }
  }

  data class Scar(val characterName: String, val description: String)

  data class HealingResult(val injuries: JSONArray, val scars: List<Scar>)

  /**
   * Ergebnis eines Extraktionsversuchs.
   *
   * [carriedOver] ist true, wenn der vorherige Zustand fortgeschrieben werden musste. Der
   * Spielstand ist dann unbeschädigt, aber auch unverändert: Ort, Uhrzeit, Inventar,
   * Verletzungen und Erinnerungen stehen still, während die Erzählung weitergelaufen ist.
   * Ohne diese Rückmeldung geschah das lautlos.
   */
  data class CheckpointResult(val checkpointId: Long, val carriedOver: Boolean)

  /**
   * Performs post-turn background state extraction and saves the resulting CheckpointEntity.
   */
  suspend fun extractAndCommitCheckpoint(
    story: StoryEntity,
    latestCheckpoint: CheckpointEntity?,
    userAction: String,
    modelResponse: String,
    turnNumber: Int
  ): CheckpointResult {
    val currentStateJson = latestCheckpoint?.rawStateJson ?: ""
    val summary = latestCheckpoint?.previousEventsSummary ?: ""
    val milestones = latestCheckpoint?.getMilestonesList() ?: emptyList()

    return try {
      // Bekannte Figuren mitgeben, damit das Modell Namen und Aussehen nicht neu erfindet.
      val knownNpcs = storyDao.getNpcs(story.id).map { npc ->
        buildString {
          append(npc.canonicalName)
          if (npc.appearance.isNotBlank()) append(" (${npc.appearance})")
        }
      }

      val settings = preferences.getAiSettings()

      val updatedStateJsonObj = geminiClient.extractStructuredState(
        model = settings.chatModel,
        thinkingLevel = settings.thinkingLevel,
        thinkingBudget = settings.thinkingBudget,
        prompt = StoryPrompts.stateExtraction(
          currentStateJson = currentStateJson,
          existingMilestones = milestones,
          knownNpcs = knownNpcs,
          userAction = userAction,
          storyResponse = modelResponse
        )
      )

      // Time calculation with deterministic time-skip protection
      //
      // Kein "Tag 1, 20:00 Uhr" mehr als Ersatzwert: Eine frisch angelegte Geschichte hat
      // bewusst keine Uhrzeit, und dieser Ersatzwert hat sie ihr wieder untergeschoben. Bleibt
      // hier alles leer, entscheidet computeDeterministicTimeProgression -- die kennt den Fall.
      val previousTime = latestCheckpoint?.inGameTime.orEmpty()
      val rawExtractedTime = updatedStateJsonObj.optString("in_game_time")
        .ifBlank { previousTime }

      val newInGameTime = computeDeterministicTimeProgression(
        previousTime = previousTime,
        extractedTime = rawExtractedTime,
        userAction = userAction,
        modelResponse = modelResponse
      )

      // Die Korrektur zurückschreiben. rawStateJson entsteht weiter unten aus genau diesem
      // Objekt; ohne diese Zeile trägt derselbe Checkpoint zwei verschiedene Uhrzeiten -- in der
      // Spalte die geprüfte, im JSON die ungeprüfte. Der nächste Zug liest das JSON.
      updatedStateJsonObj.put("in_game_time", newInGameTime)

      val newLocation = updatedStateJsonObj.optString("location")
        .ifBlank { latestCheckpoint?.location ?: "Aktueller Ort" }

      var newWeather = updatedStateJsonObj.optString("weather")
        .ifBlank { latestCheckpoint?.weather ?: "Klar" }

      // If a day jump occurred and weather wasn't changed, generate a realistic evolution
      if (newInGameTime != (latestCheckpoint?.inGameTime ?: "") &&
        newWeather == (latestCheckpoint?.weather ?: "") &&
        (userAction.contains("Tag", ignoreCase = true) || modelResponse.contains("Tage", ignoreCase = true))
      ) {
        newWeather = when {
          newWeather.contains("Regen", ignoreCase = true) -> "Aufklarender Himmel, frische Meeresbrise"
          newWeather.contains("Klar", ignoreCase = true) -> "Leicht bewölkt, angenehme Kühle"
          else -> "Ruhig, trocken"
        }
      }

      // Check both nested "player" object and flat keys
      val playerObj = updatedStateJsonObj.optJSONObject("player")

      val newOutfit = updatedStateJsonObj.optString("player_outfit")
        .ifBlank { playerObj?.optString("outfit") ?: "" }
        .ifBlank { latestCheckpoint?.playerOutfit ?: "" }

      val newCondition = updatedStateJsonObj.optString("player_condition")
        .ifBlank { playerObj?.optString("condition") ?: "" }
        .ifBlank { latestCheckpoint?.playerCondition ?: "Unverletzt" }

      val newInventoryArray = updatedStateJsonObj.optJSONArray("player_inventory")
        ?: playerObj?.optJSONArray("inventory")
        ?: updatedStateJsonObj.optJSONArray("inventory")
      val invJson = newInventoryArray?.toString() ?: (latestCheckpoint?.playerInventory ?: "[]")

      val npcsArray = updatedStateJsonObj.optJSONArray("npcs")
        ?: updatedStateJsonObj.optJSONArray("npcs_present")

      // Zustand und Geschlecht aus der Vorrunde übernehmen, wenn das Modell sie diesmal
      // weggelassen hat. Ohne das fiele eine ausgezehrte Figur bei jedem zweiten Zug auf
      // "keine Angabe" zurück — genau die Sprunghaftigkeit, die lange Geschichten kaputt macht.
      if (npcsArray != null) {
        carryOverNpcFields(npcsArray, latestCheckpoint?.npcsJson)
      }

      val npcsJson = npcsArray?.toString() ?: (latestCheckpoint?.npcsJson ?: "[]")

      // Cumulative Milestones: preserve history & add newly discovered ones
      val cumulativeMilestones = mutableListOf<String>()
      cumulativeMilestones.addAll(milestones)

      val extractedMilestonesArray = updatedStateJsonObj.optJSONArray("milestones")
      if (extractedMilestonesArray != null) {
        for (i in 0 until extractedMilestonesArray.length()) {
          val m = extractedMilestonesArray.optString(i).trim()
          if (m.isNotBlank() && !cumulativeMilestones.contains(m)) {
            cumulativeMilestones.add(m)
          }
        }
      }

      // If time skip happened but model returned no milestone, record the passage of time.
      // previousTime ist beim ersten Zug leer; parseDayNumber liefert dafür 1, der Abstand ist
      // also 0 -- der Anfang einer Geschichte ist kein Zeitsprung und bekommt keinen Meilenstein.
      val dayDelta = TimeAnchor.parseDayNumber(newInGameTime) -
        TimeAnchor.parseDayNumber(previousTime)
      if (dayDelta > 0) {
        val timeSkipEntry = "Zeitsprung: $dayDelta Tag(e) sind vergangen ($newInGameTime)"
        if (!cumulativeMilestones.contains(timeSkipEntry)) {
          cumulativeMilestones.add(timeSkipEntry)
        }
      }

      val newSummary = updatedStateJsonObj.optString("previous_events_summary", summary)
        .ifBlank { summary }

      // Die Wunden des letzten Zustands werden in beiden Fällen gebraucht: Nennt die Extraktion
      // keine, gelten die alten weiter. Nennt sie welche, fehlt ihnen das Alter -- und nur das
      // Alter entscheidet, wann eine Wunde verheilt.
      val prevInjuries = if (latestCheckpoint != null && latestCheckpoint.rawStateJson.isNotBlank()) {
        try {
          JSONObject(latestCheckpoint.rawStateJson).optJSONArray("injuries")
        } catch (_: Exception) {
          null
        }
      } else {
        null
      }

      if (!updatedStateJsonObj.has("injuries")) {
        if (prevInjuries != null) updatedStateJsonObj.put("injuries", prevInjuries)
      } else if (prevInjuries != null) {
        updatedStateJsonObj.put(
          "injuries",
          carryOverInjuryAges(
            extracted = updatedStateJsonObj.optJSONArray("injuries") ?: JSONArray(),
            previous = prevInjuries
          )
        )
      }

      // Wunden über die vergangene Zeit abklingen lassen, statt sie ewig mitzuschleppen.
      val healing = applyHealing(
        injuries = updatedStateJsonObj.optJSONArray("injuries") ?: JSONArray(),
        dayDelta = dayDelta
      )
      updatedStateJsonObj.put("injuries", healing.injuries)
      healing.scars.forEach { scar ->
        val entry = "Narbe: ${scar.characterName} trägt bleibende Spuren (${scar.description})"
        if (!cumulativeMilestones.contains(entry)) cumulativeMilestones.add(entry)
      }

      // Erst jetzt serialisieren: Narben aus der Heilung sind Meilensteine.
      val milestonesJsonArray = JSONArray()
      cumulativeMilestones.forEach { milestonesJsonArray.put(it) }

      val newCheckpoint = CheckpointEntity(
        storyId = story.id,
        turnNumber = turnNumber,
        inGameTime = newInGameTime,
        location = newLocation,
        weather = newWeather,
        playerOutfit = newOutfit,
        playerInventory = invJson,
        playerCondition = newCondition,
        npcsJson = npcsJson,
        milestonesJson = milestonesJsonArray.toString(),
        previousEventsSummary = newSummary,
        rawStateJson = updatedStateJsonObj.toString()
      )

      val checkpointId = storyDao.insertCheckpoint(newCheckpoint)

      nameStoryIfUnnamed(story, updatedStateJsonObj)

      persistMemories(
        story = story,
        updatedState = updatedStateJsonObj,
        npcsArray = npcsArray,
        newInGameTime = newInGameTime,
        previousInGameTime = latestCheckpoint?.inGameTime,
        turnNumber = turnNumber,
        newMilestones = cumulativeMilestones - milestones.toSet()
      )

      CheckpointResult(checkpointId, carriedOver = false)
    } catch (e: Exception) {
      Log.w(TAG, "State extraction fallback triggered", e)
      val carriedId = if (latestCheckpoint != null) {
        storyDao.insertCheckpoint(
          latestCheckpoint.copy(
            id = 0,
            turnNumber = turnNumber,
            timestamp = System.currentTimeMillis()
          )
        )
      } else {
        0L
      }
      CheckpointResult(carriedId, carriedOver = true)
    }
  }

  /**
   * Gibt der Geschichte ihren Namen, sobald die Erzählung ihn hergibt.
   *
   * Beim Anlegen wird nichts abgefragt außer dem Prompt — Titel und Genre bleiben leer und
   * werden hier aus dem ersten Zug nachgetragen. Ein bereits vorhandener Wert wird niemals
   * überschrieben: Hat der Spieler selbst einen Titel gesetzt, gehört er ihm.
   *
   * Geschrieben wird gezielt, nicht die ganze Zeile: [story] ist der Schnappschuss vom
   * Zugbeginn. Wer währenddessen den Prompt-Bogen speichert, verlor seine Eingabe wieder,
   * sobald der Zug diesen Schnappschuss zurückschrieb — zusammen mit Spielzeit und Archiv-Status.
   */
  private suspend fun nameStoryIfUnnamed(story: StoryEntity, updatedState: JSONObject) {
    val newTitle = fillIfBlank(story.title, updatedState.optString("story_title"))
    val newGenre = fillIfBlank(story.genre, updatedState.optString("genre"))
    if (newTitle == story.title && newGenre == story.genre) return

    storyDao.updateStoryTitleAndGenre(
      id = story.id,
      title = newTitle,
      genre = newGenre,
      updatedAt = System.currentTimeMillis()
    )
  }

  /**
   * Schreibt die Erinnerungen dieser Runde in das episodische und das Identitätsgedächtnis.
   *
   * Bewusst nach dem Checkpoint: Schlägt eine Einbettung fehl (offline, Ratenlimit), darf das
   * den Spielstand nicht gefährden.
   */
  private suspend fun persistMemories(
    story: StoryEntity,
    updatedState: JSONObject,
    npcsArray: JSONArray?,
    newInGameTime: String,
    previousInGameTime: String?,
    turnNumber: Int,
    newMilestones: List<String>
  ) {
    val day = TimeAnchor.parseDayNumber(newInGameTime)

    try {
      // 1. Was in dieser Runde geschah - ein Sachverhalt, nicht die ganze Erzählung.
      val turnMemory = updatedState.optString("turn_memory").trim()
      if (turnMemory.isNotBlank()) {
        memoryEngine.recordMemory(
          storyId = story.id,
          kind = MemoryKind.EVENT,
          text = turnMemory,
          dayNumber = day,
          inGameTime = newInGameTime,
          turnNumber = turnNumber,
          embeddingModel = preferences.getAiSettings().embeddingModel
        )
      }

      // 2. Neue Meilensteine einzeln einbetten, damit sie auch nach Monaten auffindbar bleiben.
      for (milestone in newMilestones) {
        memoryEngine.recordMemory(
          storyId = story.id,
          kind = MemoryKind.MILESTONE,
          text = milestone,
          dayNumber = day,
          inGameTime = newInGameTime,
          turnNumber = turnNumber,
          embeddingModel = preferences.getAiSettings().embeddingModel
        )
      }

      // 3. Tages-Zusammenfassung, sobald ein Tag abgeschlossen wurde.
      val daySummary = updatedState.optString("completed_day_summary").trim()
      val previousDay = TimeAnchor.parseDayNumber(previousInGameTime ?: "Tag 1")
      if (daySummary.isNotBlank() && day > previousDay) {
        memoryEngine.recordMemory(
          storyId = story.id,
          kind = MemoryKind.DAY_SUMMARY,
          text = daySummary,
          dayNumber = previousDay,
          inGameTime = previousInGameTime ?: "",
          turnNumber = turnNumber,
          embeddingModel = preferences.getAiSettings().embeddingModel
        )
      }

      // 4. Figuren abgleichen und festhalten, was jede von ihnen selbst erlebt hat.
      val knowledge = npcEngine.syncFromExtraction(story.id, npcsArray, day)
      for (entry in knowledge) {
        memoryEngine.recordMemory(
          storyId = story.id,
          kind = MemoryKind.NPC,
          text = "${entry.npcName}: ${entry.text}",
          dayNumber = day,
          inGameTime = newInGameTime,
          turnNumber = turnNumber,
          npcId = entry.npcId,
          embeddingModel = preferences.getAiSettings().embeddingModel
        )
      }
    } catch (e: Exception) {
      Log.w(TAG, "Erinnerungen konnten nicht vollständig gespeichert werden", e)
    }
  }
}
