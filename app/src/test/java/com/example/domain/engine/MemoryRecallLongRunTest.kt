package com.example.domain.engine

import com.example.data.api.GeminiClient
import com.example.data.model.GeminiDefaults
import com.example.data.model.MemoryEntity
import com.example.data.model.MemoryKind
import com.example.domain.model.TimeAnchor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Der eigentliche Beleg dafür, dass das Gedächtnis über hundert Spieltage trägt.
 *
 * Die übrigen Tests prüfen die Auswahllogik gegen erfundene Zahlen. Die offene Frage war eine
 * andere: Findet die semantische Suche ein prägendes Ereignis von Tag 1 auch dann noch wieder,
 * wenn seither über hundert gewöhnliche Erinnerungen dazugekommen sind, in denen dieselben
 * Namen und Orte ständig vorkommen? Das lässt sich nur mit echten Vektoren beantworten.
 *
 * Deshalb bettet dieser Test alle Erinnerungen wirklich über die Gemini-API ein.
 *
 * Ausführen:
 *   MSF_GEMINI_TEST_KEY=… ./gradlew :app:testDebugUnitTest --tests "*MemoryRecallLongRun*"
 *
 * Ohne gesetzte Umgebungsvariable überspringt sich der Test. Der Schlüssel darf nach CLAUDE.md §4
 * niemals im Repository, in Fixtures oder in Logs landen — deshalb die Umgebungsvariable und
 * keine Konstante.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MemoryRecallLongRunTest {

  private val apiKey: String? = System.getenv("MSF_GEMINI_TEST_KEY")?.trim()?.ifBlank { null }

  /** Gleichzeitige Anfragen. Hoch genug für kurze Laufzeit, niedrig genug fürs Ratenlimit. */
  private val parallelism = 3

  private val currentDay = 100
  private val currentTime = "Tag $currentDay, 21:00 Uhr"

  /** Das prägende Ereignis, das auch nach hundert Tagen auffindbar bleiben muss. */
  private val dayOneEvent =
    "In der Gasse hinter der Brauerei hat Jan zwei Frauen, Mira und Sonja, vor drei Angreifern " +
      "gerettet. Einer der Männer zog ein Messer, Jan hat es ihm aus der Hand geschlagen."

  /**
   * Die Anfrage nimmt bewusst NICHT die Worte der Erinnerung auf. Genau das war die Schwäche der
   * alten Suche: Sie fand nur, was fast wörtlich übereinstimmte.
   */
  private val queryAtDayHundred =
    "Mira fragt mich am Feuer, ob ich noch weiß, wie wir uns kennengelernt haben. | " +
      "Anwesend: Mira, Sonja | Ort: Lager am Waldrand"

  // ------------------------------------------------------------------------------------------

  @Test
  fun `Ereignis von Tag 1 wird an Tag 100 mit echten Embeddings wiedergefunden`() {
    assumeTrue(
      "Übersprungen: MSF_GEMINI_TEST_KEY ist nicht gesetzt.",
      apiKey != null
    )

    val client = GeminiClient(customApiKeyProvider = { apiKey })
    val corpus = buildCorpus()

    val selected = runBlocking {
      // Die Anfrage zuerst: Nach über hundert Einbettungen greift im Free-Tier das Ratenlimit,
      // und ein Fehlschlag ausgerechnet hier würde wie ein Treffer-Problem aussehen.
      val queryVector = embedOne(client, queryAtDayHundred, "RETRIEVAL_QUERY")
      assertTrue("Die Anfrage konnte nicht eingebettet werden.", queryVector.isNotEmpty())

      val vectors = embedAll(client, corpus.map { it.text }, "RETRIEVAL_DOCUMENT")

      assertEquals(
        "Jede Erinnerung muss einen Vektor haben.",
        corpus.size,
        vectors.count { it.isNotEmpty() }
      )

      val presentNames = listOf("mira", "sonja")
      val scored = corpus.mapIndexedNotNull { index, memory ->
        val similarity = MemoryEngine.cosineSimilarity(queryVector, vectors[index])
        if (similarity < MemoryEngine.MIN_SIMILARITY) return@mapIndexedNotNull null
        MemoryEngine.ScoredMemory(
          memory = memory,
          score = MemoryEngine.scoreMemory(
            similarity = similarity,
            memoryDay = memory.dayNumber,
            currentDay = currentDay,
            kind = memory.kind,
            textLower = memory.text.lowercase(),
            presentNamesLower = presentNames
          )
        )
      }

      MemoryEngine.selectWithAgeDiversity(scored, currentDay)
    }

    val treffer = selected.firstOrNull { it.memory.dayNumber == 1 }

    assertTrue(
      "Das Ereignis von Tag 1 fehlt in der Auswahl. Gefunden wurden Tage: " +
        selected.joinToString { "Tag ${it.memory.dayNumber}" },
      treffer != null
    )

    // Die Erinnerung darf nicht nur gefunden werden, sie muss auch richtig datiert im Prompt
    // landen - sonst nennt das Modell sie später wieder "gestern".
    assertEquals(
      "vor etwa 3 Monaten (99 Tage)",
      TimeAnchor.describeAge(memoryDay = 1, currentDay = currentDay)
    )
  }

  // ------------------------------------------------------------------------------------------

  /**
   * Bettet einen Text ein und wiederholt bei Fehlschlag mit wachsender Pause.
   *
   * Das Free-Tier drosselt bei über hundert Anfragen hintereinander. Ohne die Wiederholung
   * würde der Test das Ratenlimit als fehlende Trefferqualität melden.
   */
  private suspend fun embedOne(client: GeminiClient, text: String, taskType: String): FloatArray {
    repeat(4) { attempt ->
      val json = client.generateEmbedding(
        text = text,
        model = GeminiDefaults.EMBEDDING_MODEL,
        taskType = taskType
      )
      if (json != null) {
        val vector = MemoryEngine.parseEmbeddingJson(json)
        if (vector.isNotEmpty()) return vector
      }
      kotlinx.coroutines.delay(1500L * (attempt + 1))
    }
    return FloatArray(0)
  }

  private suspend fun embedAll(
    client: GeminiClient,
    texts: List<String>,
    taskType: String
  ): List<FloatArray> = withContext(Dispatchers.IO) {
    val gate = Semaphore(parallelism)
    coroutineScope {
      texts.map { text ->
        async { gate.withPermit { embedOne(client, text, taskType) } }
      }.awaitAll()
    }
  }

  /**
   * Baut ein realistisches Gedächtnis über hundert Spieltage.
   *
   * Entscheidend ist die Zusammensetzung: Die neueren Erinnerungen enthalten dieselben Namen,
   * Orte und Alltagshandlungen wie die Anfrage. Sie konkurrieren damit direkt mit dem Ereignis
   * von Tag 1 — ein Korpus aus zusammenhanglosem Text würde nichts beweisen.
   */
  private fun buildCorpus(): List<MemoryEntity> {
    val memories = mutableListOf<MemoryEntity>()
    var id = 1L

    fun add(day: Int, text: String, kind: String = MemoryKind.EVENT) {
      memories.add(
        MemoryEntity(
          id = id,
          storyId = 1L,
          kind = kind,
          dayNumber = day,
          inGameTime = "Tag $day, 12:00 Uhr",
          turnNumber = id.toInt(),
          text = text
        )
      )
      id++
    }

    add(1, dayOneEvent, MemoryKind.MILESTONE)

    val alltag = listOf(
      "Mira und Jan haben am Fluss Wasser geholt und die Kanister zurück ins Lager getragen.",
      "Sonja hat Feuerholz gesammelt, das Holz war feucht und qualmte den ganzen Abend.",
      "Jan hat die Zeltplane geflickt, Mira hat ihm die Nadel gehalten.",
      "Am Feuer wurde über die Vorräte gesprochen; der Speck reicht noch drei Tage.",
      "Mira hat Wache gehalten, bis Sonja sie nach Mitternacht abgelöst hat.",
      "Sonja hat sich beim Holzhacken in den Daumen geschnitten, Jan hat sie verbunden.",
      "Die Gruppe ist dem alten Forstweg nach Norden gefolgt und hat am Waldrand gerastet.",
      "Jan und Mira haben Pilze gesucht, den Großteil aber wieder weggeworfen.",
      "Es hat den ganzen Tag geregnet, niemand hat das Lager verlassen.",
      "Sonja hat ihre Stiefel getrocknet und dabei von ihrer Schwester erzählt.",
      "Jan hat die Vorräte neu sortiert und eine Liste ins Notizbuch geschrieben.",
      "Mira hat am Bach Wäsche gewaschen, das Wasser war eiskalt."
    )

    // Tag 4 bis 100: gewöhnliche Erinnerungen, die dieselben Namen tragen wie die Anfrage.
    var tag = 4
    var index = 0
    while (tag <= currentDay) {
      add(tag, alltag[index % alltag.size])
      if (index % 4 == 3) {
        add(tag, "Tagesrückblick Tag $tag: ruhiger Verlauf im Lager, keine Zwischenfälle.", MemoryKind.DAY_SUMMARY)
      }
      index++
      tag += if (index % 3 == 0) 2 else 1
    }

    return memories
  }
}
