package com.example.domain.engine

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Sichert ab, dass Wunden über die Zeit abklingen, lebensbedrohliche Verletzungen aber niemals
 * von selbst verschwinden.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class InjuryHealingTest {

  private fun injuries(vararg specs: Triple<String, Boolean, String>): JSONArray {
    val array = JSONArray()
    specs.forEach { (severity, treated, character) ->
      array.put(
        JSONObject()
          .put("character", character)
          .put("body_part", "CHEST")
          .put("description", "Schnittwunde")
          .put("severity", severity)
          .put("is_treated", treated)
      )
    }
    return array
  }

  @Test
  fun `leichte Wunde ist nach drei Tagen verheilt`() {
    val result = StateExtractionEngine.applyHealing(
      injuries("LIGHT" to false to "Du"),
      dayDelta = 3
    )
    assertEquals(0, result.injuries.length())
  }

  @Test
  fun `leichte Wunde besteht am selben Tag fort`() {
    val result = StateExtractionEngine.applyHealing(
      injuries("LIGHT" to false to "Du"),
      dayDelta = 1
    )
    assertEquals(1, result.injuries.length())
  }

  @Test
  fun `kritische Wunde heilt auch nach dreissig Tagen nicht von selbst`() {
    val result = StateExtractionEngine.applyHealing(
      injuries("CRITICAL" to false to "Du"),
      dayDelta = 30
    )
    assertEquals("Lebensbedrohliches darf nicht verschwinden", 1, result.injuries.length())
  }

  @Test
  fun `behandelte Wunden heilen schneller`() {
    val untreated = StateExtractionEngine.applyHealing(injuries("MEDIUM" to false to "Du"), dayDelta = 3)
    val treated = StateExtractionEngine.applyHealing(injuries("MEDIUM" to true to "Du"), dayDelta = 3)

    assertEquals(1, untreated.injuries.length())
    assertEquals(0, treated.injuries.length())
  }

  @Test
  fun `schwere Wunde hinterlaesst eine Narbe`() {
    val result = StateExtractionEngine.applyHealing(
      injuries("SEVERE" to false to "Lena"),
      dayDelta = 20
    )
    assertEquals(0, result.injuries.length())
    assertEquals(1, result.scars.size)
    assertEquals("Lena", result.scars.first().characterName)
  }

  @Test
  fun `leichte Wunde hinterlaesst keine Narbe`() {
    val result = StateExtractionEngine.applyHealing(injuries("LIGHT" to false to "Du"), dayDelta = 10)
    assertTrue(result.scars.isEmpty())
  }

  @Test
  fun `ohne Zeitfortschritt bleibt alles unveraendert`() {
    val input = injuries("LIGHT" to false to "Du", "SEVERE" to false to "Lena")
    val result = StateExtractionEngine.applyHealing(input, dayDelta = 0)
    assertEquals(2, result.injuries.length())
    assertTrue(result.scars.isEmpty())
  }

  @Test
  fun `Heilung wirkt fuer NPCs genauso wie fuer den Spieler`() {
    val result = StateExtractionEngine.applyHealing(
      injuries("LIGHT" to false to "Du", "LIGHT" to false to "Lena"),
      dayDelta = 5
    )
    assertEquals(0, result.injuries.length())
  }

  @Test
  fun `Heilung sammelt sich ueber mehrere kleine Zeitspruenge`() {
    // Ein MEDIUM-Fall braucht 6 Tage; drei Sprünge à 2 Tage müssen genügen.
    var current = injuries("MEDIUM" to false to "Du")
    repeat(2) {
      current = StateExtractionEngine.applyHealing(current, dayDelta = 2).injuries
      assertEquals("Zu früh verheilt", 1, current.length())
    }
    val finalResult = StateExtractionEngine.applyHealing(current, dayDelta = 2)
    assertEquals(0, finalResult.injuries.length())
  }

  /**
   * Der Test darüber reicht `applyHealing` die eigene Ausgabe zurück -- und die trägt
   * `days_elapsed` bereits. Die Produktion tut das nie: Dort kommt die Wunde jedes Mal frisch
   * aus der Extraktion, ohne Alter. Genau dieser Weg wird hier nachgestellt.
   */
  @Test
  fun `Wunde altert weiter, obwohl die Extraktion sie ohne Alter zurueckgibt`() {
    var gespeichert = injuries("MEDIUM" to false to "Du")

    repeat(2) { runde ->
      // Was das Modell liefert: dieselbe Wunde, aber ohne days_elapsed.
      val ausExtraktion = injuries("MEDIUM" to false to "Du")
      val zusammengefuehrt = StateExtractionEngine.carryOverInjuryAges(ausExtraktion, gespeichert)
      gespeichert = StateExtractionEngine.applyHealing(zusammengefuehrt, dayDelta = 2).injuries
      assertEquals("Zu früh verheilt in Runde $runde", 1, gespeichert.length())
    }

    val ausExtraktion = injuries("MEDIUM" to false to "Du")
    val zusammengefuehrt = StateExtractionEngine.carryOverInjuryAges(ausExtraktion, gespeichert)
    val ergebnis = StateExtractionEngine.applyHealing(zusammengefuehrt, dayDelta = 2)
    assertEquals("Nach 6 Tagen muss die Wunde verheilt sein", 0, ergebnis.injuries.length())
  }

  @Test
  fun `ohne Uebertragung bleibt die Wunde ewig offen`() {
    // Der Gegenbeweis: derselbe Ablauf ohne carryOverInjuryAges heilt nie.
    var gespeichert = injuries("MEDIUM" to false to "Du")
    repeat(5) {
      gespeichert = StateExtractionEngine.applyHealing(
        injuries("MEDIUM" to false to "Du"),
        dayDelta = 2
      ).injuries
    }
    assertEquals("Beleg für den Fehler, den die Übertragung behebt", 1, gespeichert.length())
  }

  @Test
  fun `zwei Wunden am selben Koerperteil erben kein fremdes Alter`() {
    val alt = JSONArray()
      .put(
        JSONObject().put("character", "Du").put("body_part", "ARM")
          .put("description", "Schnittwunde").put("severity", "LIGHT").put("days_elapsed", 5)
      )
      .put(
        JSONObject().put("character", "Du").put("body_part", "ARM")
          .put("description", "Prellung").put("severity", "LIGHT").put("days_elapsed", 0)
      )

    // Beschreibung weicht ab, Körperteil ist mehrdeutig -> kein Alter, statt des falschen.
    val neu = JSONArray().put(
      JSONObject().put("character", "Du").put("body_part", "ARM")
        .put("description", "tiefe Schnittwunde").put("severity", "LIGHT")
    )

    val ergebnis = StateExtractionEngine.carryOverInjuryAges(neu, alt)
    assertEquals(0, ergebnis.getJSONObject(0).optInt("days_elapsed", 0))
  }

  @Test
  fun `abweichende Beschreibung erbt das Alter, solange das Koerperteil eindeutig ist`() {
    val alt = JSONArray().put(
      JSONObject().put("character", "Du").put("body_part", "ARM")
        .put("description", "Schnittwunde").put("severity", "LIGHT").put("days_elapsed", 4)
    )
    val neu = JSONArray().put(
      JSONObject().put("character", "Du").put("body_part", "ARM")
        .put("description", "vernarbende Schnittwunde").put("severity", "LIGHT")
    )

    val ergebnis = StateExtractionEngine.carryOverInjuryAges(neu, alt)
    assertEquals(4, ergebnis.getJSONObject(0).optInt("days_elapsed", 0))
  }

  @Test
  fun `ein bereits gesetztes Alter wird nicht ueberschrieben`() {
    val alt = JSONArray().put(
      JSONObject().put("character", "Du").put("body_part", "ARM")
        .put("description", "Schnittwunde").put("severity", "LIGHT").put("days_elapsed", 4)
    )
    val neu = JSONArray().put(
      JSONObject().put("character", "Du").put("body_part", "ARM")
        .put("description", "Schnittwunde").put("severity", "LIGHT").put("days_elapsed", 1)
    )

    val ergebnis = StateExtractionEngine.carryOverInjuryAges(neu, alt)
    assertEquals(1, ergebnis.getJSONObject(0).optInt("days_elapsed", -1))
  }

  private infix fun <A, B> Pair<A, B>.to(third: String): Triple<A, B, String> =
    Triple(first, second, third)
}
