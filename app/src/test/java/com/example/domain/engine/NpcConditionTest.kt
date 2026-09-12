package com.example.domain.engine

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Sichert ab, dass die körperliche Verfassung einer Figur nicht verschwindet, nur weil eine
 * einzelne Extraktion sie ausgelassen hat.
 *
 * Hintergrund: Nach zwei Monaten ohne Nahrung stand beim Spieler "stark abgemagert,
 * unterernährt", während die Figur daneben als völlig gesund geführt wurde.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NpcConditionTest {

  private fun npc(name: String, condition: String? = null, gender: String? = null): JSONObject =
    JSONObject().put("name", name).apply {
      if (condition != null) put("condition", condition)
      if (gender != null) put("gender", gender)
    }

  private fun array(vararg objects: JSONObject): JSONArray =
    JSONArray().apply { objects.forEach { put(it) } }

  @Test
  fun `ausgelassener Zustand wird aus der Vorrunde uebernommen`() {
    val previous = array(npc("Kael", condition = "Stark ausgezehrt, dehydriert")).toString()
    val current = array(npc("Kael"))

    StateExtractionEngine.carryOverNpcFields(current, previous)

    assertEquals(
      "Stark ausgezehrt, dehydriert",
      current.getJSONObject(0).optString("condition")
    )
  }

  @Test
  fun `ein neu gelieferter Zustand ueberschreibt den alten`() {
    val previous = array(npc("Kael", condition = "Leicht erschöpft")).toString()
    val current = array(npc("Kael", condition = "Am Rand des Zusammenbruchs"))

    StateExtractionEngine.carryOverNpcFields(current, previous)

    assertEquals(
      "Am Rand des Zusammenbruchs",
      current.getJSONObject(0).optString("condition")
    )
  }

  @Test
  fun `das Geschlecht bleibt erhalten - die Organdarstellung haengt daran`() {
    val previous = array(npc("Mira", gender = "FEMALE")).toString()
    val current = array(npc("Mira", condition = "Unterkühlt"))

    StateExtractionEngine.carryOverNpcFields(current, previous)

    assertEquals("FEMALE", current.getJSONObject(0).optString("gender"))
  }

  @Test
  fun `eine neue Figur bekommt nichts untergeschoben`() {
    val previous = array(npc("Kael", condition = "Ausgezehrt")).toString()
    val current = array(npc("Fremde"))

    StateExtractionEngine.carryOverNpcFields(current, previous)

    assertEquals("", current.getJSONObject(0).optString("condition"))
  }

  @Test
  fun `Namen werden unabhaengig von Gross- und Kleinschreibung zugeordnet`() {
    val previous = array(npc("Kael", condition = "Ausgezehrt")).toString()
    val current = array(npc("kael"))

    StateExtractionEngine.carryOverNpcFields(current, previous)

    assertEquals("Ausgezehrt", current.getJSONObject(0).optString("condition"))
  }

  @Test
  fun `kaputter Vorrunden-Stand fuehrt nicht zum Absturz`() {
    val current = array(npc("Kael"))

    StateExtractionEngine.carryOverNpcFields(current, "kein json")
    StateExtractionEngine.carryOverNpcFields(current, null)
    StateExtractionEngine.carryOverNpcFields(current, "")

    assertEquals(1, current.length())
  }
}
