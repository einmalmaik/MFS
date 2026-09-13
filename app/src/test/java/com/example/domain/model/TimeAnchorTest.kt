package com.example.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sichert die Zeitrechnung ab, die verhindert, dass das Modell Monate alte Ereignisse
 * als "gestern" bezeichnet.
 */
class TimeAnchorTest {

  @Test
  fun `parst Tagesnummer aus Standardformat`() {
    assertEquals(97, TimeAnchor.parseDayNumber("Tag 97, 14:30 Uhr"))
    assertEquals(3, TimeAnchor.parseDayNumber("Tag 3, 09:30 Uhr"))
    assertEquals(1, TimeAnchor.parseDayNumber("Tag 1"))
  }

  @Test
  fun `faellt auf Tag 1 zurueck wenn keine Tagesnummer vorhanden ist`() {
    assertEquals(1, TimeAnchor.parseDayNumber(null))
    assertEquals(1, TimeAnchor.parseDayNumber(""))
    assertEquals(1, TimeAnchor.parseDayNumber("Irgendwann"))
  }

  @Test
  fun `normalisiert Uhrzeit auf HH MM Uhr`() {
    assertEquals("09:30 Uhr", TimeAnchor.parseTimeOfDay("Tag 3, 09:30"))
    assertEquals("14:00 Uhr", TimeAnchor.parseTimeOfDay("Tag 3, 14:00 Uhr"))
    assertEquals("02:00 Uhr", TimeAnchor.parseTimeOfDay("Sonntag, 2 Uhr nachts"))
    assertEquals("02:00 Uhr", TimeAnchor.parseTimeOfDay("Sonntag, zwei Uhr nachts. Ich schließe die Kneipe ab."))
    assertEquals("00:00 Uhr", TimeAnchor.parseTimeOfDay("Es ist Mitternacht."))
    assertEquals("", TimeAnchor.parseTimeOfDay("Tag 3"))
  }

  @Test
  fun `beschreibt gestern korrekt`() {
    assertEquals("gestern", TimeAnchor.describeAge(memoryDay = 96, currentDay = 97))
  }

  @Test
  fun `beschreibt heute korrekt`() {
    assertEquals("heute", TimeAnchor.describeAge(memoryDay = 97, currentDay = 97))
  }

  @Test
  fun `nennt bei grossem Abstand niemals gestern`() {
    val description = TimeAnchor.describeAge(memoryDay = 3, currentDay = 97)
    assertTrue(
      "Erwartete Monatsangabe, war: $description",
      description.contains("Monaten")
    )
    assertTrue("Tagesanzahl muss enthalten sein: $description", description.contains("94"))
  }

  @Test
  fun `beschreibt Wochen mit konkreter Tagesanzahl`() {
    assertEquals("vor einer Woche (7 Tage)", TimeAnchor.describeAge(90, 97))
    assertEquals("vor 2 Wochen (14 Tage)", TimeAnchor.describeAge(83, 97))
  }

  @Test
  fun `Zeitregeln nennen die heutige Tagesnummer und die Gestern-Ableitung`() {
    val rules = TimeAnchor.buildTimeRules("Tag 97, 14:30 Uhr")
    assertTrue(rules.contains("Die heutige Tagesnummer ist 97"))
    assertTrue(rules.contains("Tag 96 = gestern"))
    assertTrue(rules.contains("NIEMALS"))
  }

  @Test
  fun `Zeitregeln erfinden an Tag 1 kein gestern`() {
    val rules = TimeAnchor.buildTimeRules("Tag 1, 09:00 Uhr")
    assertTrue(rules.contains("Es gibt noch kein 'gestern'"))
    assertTrue("Kein Tag 0 im Text: $rules", !rules.contains("Tag 0"))
  }
}
