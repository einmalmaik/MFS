package com.example.domain.engine

import com.example.data.model.MemoryEntity
import com.example.data.model.MemoryKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.abs

/**
 * Sichert die Auswahl-Logik des episodischen Gedächtnisses ab.
 *
 * Der wichtigste Fall ist die Langzeit-Simulation: Ohne reservierte Plätze für alte Erinnerungen
 * verdrängen die Ereignisse der letzten Tage rein zahlenmäßig alles Ältere — ein prägender
 * Moment von Tag 1 wäre an Tag 100 unauffindbar. Genau das prüft
 * [Erinnerung von Tag 1 ueberlebt hundert Tage voller neuer Ereignisse].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MemoryEngineTest {

  private fun memory(
    id: Long,
    day: Int,
    text: String = "Ereignis $id",
    kind: String = MemoryKind.EVENT
  ) = MemoryEntity(
    id = id,
    storyId = 1L,
    kind = kind,
    dayNumber = day,
    inGameTime = "Tag $day, 12:00 Uhr",
    turnNumber = id.toInt(),
    text = text
  )

  // --- Vektor-Umwandlung ---

  @Test
  fun `FloatArray ueberlebt die Umwandlung nach BLOB und zurueck`() {
    val original = floatArrayOf(0.1f, -0.5f, 0.98765f, 0f, 1f)
    val restored = MemoryEngine.bytesToFloats(MemoryEngine.floatsToBytes(original))

    assertEquals(original.size, restored.size)
    original.forEachIndexed { i, value ->
      assertTrue("Index $i wich ab: $value vs ${restored[i]}", abs(value - restored[i]) < 1e-6)
    }
  }

  @Test
  fun `parst Alt-Embeddings aus JSON`() {
    val parsed = MemoryEngine.parseEmbeddingJson("[0.5,-0.25,0.125]")
    assertEquals(3, parsed.size)
    assertTrue(abs(0.5f - parsed[0]) < 1e-6)
    assertTrue(abs(-0.25f - parsed[1]) < 1e-6)
  }

  @Test
  fun `liefert leeren Vektor bei kaputtem JSON statt zu werfen`() {
    assertEquals(0, MemoryEngine.parseEmbeddingJson("kein json").size)
  }

  // --- Ähnlichkeit ---

  @Test
  fun `identische Vektoren haben Aehnlichkeit eins`() {
    val v = floatArrayOf(1f, 2f, 3f)
    assertTrue(abs(1f - MemoryEngine.cosineSimilarity(v, v)) < 1e-5)
  }

  @Test
  fun `orthogonale Vektoren haben Aehnlichkeit null`() {
    val a = floatArrayOf(1f, 0f)
    val b = floatArrayOf(0f, 1f)
    assertTrue(abs(MemoryEngine.cosineSimilarity(a, b)) < 1e-5)
  }

  @Test
  fun `Vektoren aus verschiedenen Modellen werden verworfen statt verglichen`() {
    // text-embedding-004 lieferte 768 Dimensionen, gemini-embedding-001 liefert 3072. Ein
    // Vergleich der ersten 768 Komponenten ergäbe eine Zahl, die nach Ähnlichkeit aussieht,
    // aber aus zwei verschiedenen Vektorräumen stammt.
    val alt = FloatArray(768) { 0.5f }
    val neu = FloatArray(3072) { 0.5f }

    assertEquals(0f, MemoryEngine.cosineSimilarity(alt, neu))
  }

  @Test
  fun `Nullvektor fuehrt nicht zu Division durch null`() {
    assertEquals(0f, MemoryEngine.cosineSimilarity(floatArrayOf(0f, 0f), floatArrayOf(1f, 1f)))
    assertEquals(0f, MemoryEngine.cosineSimilarity(FloatArray(0), floatArrayOf(1f)))
  }

  // --- Bewertung ---

  @Test
  fun `Namenstreffer einer anwesenden Figur hebt die Bewertung`() {
    val withName = MemoryEngine.scoreMemory(
      similarity = 0.5f, memoryDay = 10, currentDay = 20, kind = MemoryKind.EVENT,
      textLower = "lena bedankt sich für die rettung", presentNamesLower = listOf("lena")
    )
    val withoutName = MemoryEngine.scoreMemory(
      similarity = 0.5f, memoryDay = 10, currentDay = 20, kind = MemoryKind.EVENT,
      textLower = "ein fremder bedankt sich", presentNamesLower = listOf("lena")
    )
    assertTrue("Namenstreffer muss besser bewertet werden", withName > withoutName)
  }

  @Test
  fun `Aehnlichkeit bleibt dominant gegenueber allen Boni`() {
    val weakButBoosted = MemoryEngine.scoreMemory(
      similarity = 0.40f, memoryDay = 20, currentDay = 20, kind = MemoryKind.MILESTONE,
      textLower = "lena", presentNamesLower = listOf("lena")
    )
    val strongWithoutBonus = MemoryEngine.scoreMemory(
      similarity = 0.70f, memoryDay = 1, currentDay = 20, kind = MemoryKind.EVENT,
      textLower = "etwas anderes", presentNamesLower = listOf("lena")
    )
    assertTrue(
      "Boni dürfen einen deutlich besseren Treffer nicht überholen",
      strongWithoutBonus > weakButBoosted
    )
  }

  // --- Alters-Diversität: der Kern der Langzeit-Tauglichkeit ---

  @Test
  fun `Erinnerung von Tag 1 ueberlebt hundert Tage voller neuer Ereignisse`() {
    val currentDay = 100

    // 500 jüngere Erinnerungen mit durchweg besserer Ähnlichkeit.
    val recent = (1..500).map { i ->
      MemoryEngine.ScoredMemory(
        memory = memory(id = i.toLong(), day = 94 + (i % 7)),
        score = 0.80f + (i % 10) * 0.001f
      )
    }

    // Das prägende Ereignis von Tag 1 - schlechter bewertet, aber unverzichtbar.
    val dayOne = MemoryEngine.ScoredMemory(
      memory = memory(id = 9001, day = 1, text = "Zwei Frauen vor drei Männern gerettet"),
      score = 0.55f
    )

    val selected = MemoryEngine.selectWithAgeDiversity(recent + dayOne, currentDay)

    assertTrue(
      "Die Erinnerung von Tag 1 muss trotz 500 besser bewerteter neuer Einträge dabei sein",
      selected.any { it.memory.id == 9001L }
    )
    assertEquals(MemoryEngine.MAX_MEMORIES, selected.size)
  }

  @Test
  fun `reserviert zwei Plaetze fuer alte Erinnerungen`() {
    val currentDay = 50
    val recent = (1..20).map {
      MemoryEngine.ScoredMemory(memory(it.toLong(), day = 49), score = 0.9f)
    }
    val old = (1..5).map {
      MemoryEngine.ScoredMemory(memory(100L + it, day = 5), score = 0.4f)
    }

    val selected = MemoryEngine.selectWithAgeDiversity(recent + old, currentDay)
    val oldCount = selected.count { currentDay - it.memory.dayNumber > MemoryEngine.OLD_MEMORY_DAY_THRESHOLD }

    assertEquals(MemoryEngine.RESERVED_OLD_SLOTS, oldCount)
  }

  @Test
  fun `fuellt mit den besten Treffern auf wenn es keine alten Erinnerungen gibt`() {
    val currentDay = 10
    val onlyRecent = (1..20).map {
      MemoryEngine.ScoredMemory(memory(it.toLong(), day = 10), score = 1f - it * 0.01f)
    }

    val selected = MemoryEngine.selectWithAgeDiversity(onlyRecent, currentDay)
    assertEquals(MemoryEngine.MAX_MEMORIES, selected.size)
  }

  @Test
  fun `gibt bei weniger Kandidaten als Plaetzen alle zurueck`() {
    val selected = MemoryEngine.selectWithAgeDiversity(
      listOf(MemoryEngine.ScoredMemory(memory(1, 1), 0.9f)),
      currentDay = 5
    )
    assertEquals(1, selected.size)
  }

  @Test
  fun `liefert leere Auswahl ohne Kandidaten`() {
    assertTrue(MemoryEngine.selectWithAgeDiversity(emptyList(), currentDay = 5).isEmpty())
  }

  @Test
  fun `sortiert die Auswahl chronologisch fuer den Prompt`() {
    val candidates = listOf(
      MemoryEngine.ScoredMemory(memory(1, day = 50), 0.9f),
      MemoryEngine.ScoredMemory(memory(2, day = 2), 0.8f),
      MemoryEngine.ScoredMemory(memory(3, day = 30), 0.7f)
    )
    val selected = MemoryEngine.selectWithAgeDiversity(candidates, currentDay = 60)
    val days = selected.map { it.memory.dayNumber }

    assertEquals(days.sorted(), days)
  }
}
