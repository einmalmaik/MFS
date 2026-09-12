package com.example.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sichert ab, dass die Vitalitätsanzeige eine ausgezehrte Figur nicht länger als
 * "vollständig einsatzbereit" ausweist, nur weil sie keine offene Wunde hat.
 */
class ConditionPenaltyTest {

  /** Der gemeldete Fall, wörtlich aus dem Spielstand. */
  private val kael =
    "Schwer unterernährt, dehydriert, hohes Fieber, rasselnder Atem, trockener Husten, " +
      "extreme körperliche Schwäche"

  private val spieler =
    "Schwer unterernährt, dehydriert, erschöpft, Schwindel, metallischer Geschmack im Mund, " +
      "brennender Hunger"

  @Test
  fun `eine ausgezehrte Figur ohne Wunde gilt nicht mehr als einsatzbereit`() {
    val abzug = conditionPenaltyOf(kael)
    assertTrue("Kein Abzug trotz schwerer Auszehrung", abzug > 0)
    assertTrue("Die Vitalität muss deutlich unter 100 fallen", 100 - abzug < 60)
  }

  @Test
  fun `Spieler und NPC im selben Mangel liegen nah beieinander`() {
    // Der Grad darf sich unterscheiden - die Größenordnung nicht.
    val abstand = kotlin.math.abs(conditionPenaltyOf(kael) - conditionPenaltyOf(spieler))
    assertTrue("Die Abzüge klaffen zu weit auseinander: $abstand", abstand <= 25)
  }

  @Test
  fun `Unverletzt bleibt ohne Abzug`() {
    assertEquals(0, conditionPenaltyOf("Unverletzt"))
    assertEquals(0, conditionPenaltyOf(""))
    assertEquals(0, conditionPenaltyOf("   "))
  }

  @Test
  fun `unbekannter Text zieht nichts ab statt zu raten`() {
    assertEquals(0, conditionPenaltyOf("Gut gelaunt und ausgeruht"))
  }

  @Test
  fun `schwer wiegt mehr als die blosse Nennung`() {
    assertTrue(conditionPenaltyOf("Schwer erschöpft") > conditionPenaltyOf("Erschöpft"))
  }

  @Test
  fun `der Abzug bleibt gedeckelt damit Wunden noch wirken koennen`() {
    val extrem = conditionPenaltyOf(
      "Sterbend, bewusstlos, vergiftet, verhungert, verdurstet, extrem entkräftet, hohes Fieber"
    )
    assertTrue("Der Deckel von 70 wurde überschritten: $extrem", extrem <= 70)
  }
}
