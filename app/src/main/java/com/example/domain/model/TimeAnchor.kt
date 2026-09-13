package com.example.domain.model

/**
 * Rechnet mit In-Game-Zeit im Format "Tag X, HH:MM Uhr".
 *
 * Existiert, weil relative Zeitangaben die häufigste Fehlerquelle langer Geschichten sind: Das
 * Modell sieht im Prompt nur absolute Tagesnummern und muss die Differenz zum Heute selbst
 * bilden. Bei großem Kontext geht diese Rechnung verloren und aus "Tag 3" wird "gestern",
 * obwohl 94 Tage dazwischen liegen. Deshalb wird der Abstand hier deterministisch berechnet
 * und dem Modell fertig hingeschrieben.
 */
object TimeAnchor {

  private val DAY_REGEX = Regex("""Tag\s*(\d+)""", RegexOption.IGNORE_CASE)
  private val TIME_REGEX = Regex("""\b(\d{1,2}):(\d{2})(?:\s*Uhr)?\b""", RegexOption.IGNORE_CASE)
  private val HOUR_ONLY_REGEX = Regex("""\b(\d{1,2})\s*Uhr\b""", RegexOption.IGNORE_CASE)
  private val WORD_HOUR_REGEX = Regex("""\b(ein|eins|eine|zwei|drei|vier|fünf|sechs|sieben|acht|neun|zehn|elf|zwölf)\s*Uhr\b""", RegexOption.IGNORE_CASE)

  private val GERMAN_WORD_HOURS = mapOf(
    "ein" to 1,
    "eins" to 1,
    "eine" to 1,
    "zwei" to 2,
    "drei" to 3,
    "vier" to 4,
    "fünf" to 5,
    "sechs" to 6,
    "sieben" to 7,
    "acht" to 8,
    "neun" to 9,
    "zehn" to 10,
    "elf" to 11,
    "zwölf" to 12
  )

  /** Liest die Tagesnummer aus einem Zeitstempel. Fällt auf Tag 1 zurück. */
  fun parseDayNumber(timeStr: String?): Int {
    if (timeStr.isNullOrBlank()) return 1
    return DAY_REGEX.find(timeStr)?.groupValues?.get(1)?.toIntOrNull() ?: 1
  }

  /** Liest die Uhrzeit aus einem Zeitstempel und normalisiert sie auf "HH:MM Uhr". */
  fun parseTimeOfDay(timeStr: String?): String {
    if (timeStr.isNullOrBlank()) return ""
    if (timeStr.contains("mitternacht", ignoreCase = true)) {
      return "00:00 Uhr"
    }
    val colonMatch = TIME_REGEX.find(timeStr)
    if (colonMatch != null) {
      val h = colonMatch.groupValues[1].toIntOrNull() ?: 0
      val m = colonMatch.groupValues[2].toIntOrNull() ?: 0
      return "%02d:%02d Uhr".format(h, m)
    }
    val hourMatch = HOUR_ONLY_REGEX.find(timeStr)?.groupValues?.get(1)?.toIntOrNull()
    if (hourMatch != null && hourMatch in 0..23) {
      return "%02d:00 Uhr".format(hourMatch)
    }
    val wordMatch = WORD_HOUR_REGEX.find(timeStr)?.groupValues?.get(1)?.lowercase()
    if (wordMatch != null) {
      val h = GERMAN_WORD_HOURS[wordMatch]
      if (h != null) {
        return "%02d:00 Uhr".format(h)
      }
    }
    return ""
  }

  /**
   * Beschreibt den Abstand zwischen zwei In-Game-Tagen in natürlicher Sprache.
   *
   * Die Tagesanzahl steht ab einer Woche immer zusätzlich dabei, damit das Modell die Angabe
   * nicht neu interpretieren muss.
   */
  fun describeAge(memoryDay: Int, currentDay: Int): String {
    val delta = currentDay - memoryDay
    return when {
      delta <= 0 -> "heute"
      delta == 1 -> "gestern"
      delta < 7 -> "vor $delta Tagen"
      delta == 7 -> "vor einer Woche (7 Tage)"
      delta < 30 -> "vor ${delta / 7} Wochen ($delta Tage)"
      delta < 60 -> "vor etwa einem Monat ($delta Tage)"
      else -> "vor etwa ${delta / 30} Monaten ($delta Tage)"
    }
  }

  /**
   * Der verbindliche Zeitblock für den Prompt. Ohne diese explizite Ableitungsregel bezeichnet
   * das Modell im langen Kontext auch Monate alte Ereignisse als "gestern".
   */
  fun buildTimeRules(currentTime: String): String {
    val today = parseDayNumber(currentTime)
    return buildString {
      appendLine("[ZEITRECHNUNG - VERBINDLICH]")
      appendLine("Heute ist $currentTime. Die heutige Tagesnummer ist $today.")
      appendLine("Jede Erinnerung unten trägt ihre absolute Tagesnummer und den Abstand zu heute.")
      appendLine("Leite relative Zeitangaben AUSSCHLIESSLICH aus diesen Tagesnummern ab:")

      if (today <= 1) {
        appendLine("- Die Geschichte beginnt gerade erst. Es gibt noch kein 'gestern'.")
      } else {
        appendLine("- Tag ${today - 1} = gestern")
        val weekStart = (today - 6).coerceAtLeast(1)
        if (weekStart < today - 1) {
          appendLine("- Tag $weekStart bis Tag ${today - 1} = diese Woche")
        }
      }

      appendLine("- Alles davor liegt Wochen oder Monate zurück und darf NIEMALS 'gestern',")
      appendLine("  'neulich' oder 'vorhin' genannt werden.")
      append("Im Zweifel nenne den Abstand konkret ('vor zwei Monaten') statt vage.")
    }
  }
}
