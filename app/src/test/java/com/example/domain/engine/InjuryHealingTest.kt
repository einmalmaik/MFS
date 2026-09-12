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

  private infix fun <A, B> Pair<A, B>.to(third: String): Triple<A, B, String> =
    Triple(first, second, third)
}
