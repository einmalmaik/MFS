package com.example.domain.service

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.StoryDao
import com.example.data.db.StoryDatabase
import com.example.data.model.CheckpointEntity
import com.example.data.model.MemoryEntity
import com.example.data.model.MessageEntity
import com.example.data.model.StoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Sichert die Verzweigungs-Invariante aus CLAUDE.md §0.7 ab: Ein Zweig übernimmt den Zustand
 * seines Verzweigungspunkts und lässt den Ursprungsstrang unangetastet.
 *
 * Der Dienst hatte bis hierher keinen einzigen Test -- und war in allen drei Methoden falsch:
 * Der Zweig startete mit dem Zustand von jetzt statt dem von damals, Zurückspulen löschte immer
 * genau einen Checkpoint egal wie weit es ging, und Bearbeiten warf einen Checkpoint zu viel weg.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StoryBranchingServiceTest {

  private lateinit var db: StoryDatabase
  private lateinit var dao: StoryDao
  private lateinit var service: StoryBranchingService

  @Before
  fun setUp() {
    db = Room.inMemoryDatabaseBuilder(
      ApplicationProvider.getApplicationContext(),
      StoryDatabase::class.java
    ).allowMainThreadQueries().build()
    dao = db.storyDao()
    service = StoryBranchingService(dao, db)
  }

  @After
  fun tearDown() {
    db.close()
  }

  /**
   * Baut den Verlauf, den die Engine tatsächlich erzeugt: Die Eröffnung belegt Zug 1 ohne
   * vorausgehende Spieler-Nachricht -- genau die Eigenheit, an der die alte Zugzählung scheiterte.
   *
   * Zug 1: [model]             -> cp(turn 1), Ort "Hafen"
   * Zug 2: [user] [model]      -> cp(turn 2), Ort "Markt"
   * Zug 3: [user] [model]      -> cp(turn 3), Ort "Turm"
   */
  private data class Verlauf(
    val storyId: Long,
    val eroeffnung: MessageEntity,
    val userZug2: MessageEntity,
    val modelZug2: MessageEntity,
    val userZug3: MessageEntity,
    val modelZug3: MessageEntity
  )

  private suspend fun baueVerlauf(): Verlauf {
    val storyId = dao.insertStory(StoryEntity(title = "Quelle", systemPrompt = "p"))

    suspend fun zug(nummer: Int, ort: String, mitUserNachricht: Boolean): Pair<MessageEntity?, MessageEntity> {
      val user = if (mitUserNachricht) {
        val id = dao.insertMessage(MessageEntity(storyId = storyId, sender = "user", content = "Aktion $nummer"))
        dao.getMessageById(id)
      } else {
        null
      }
      val cpId = dao.insertCheckpoint(
        CheckpointEntity(
          storyId = storyId,
          turnNumber = nummer,
          inGameTime = "Tag $nummer, 12:00 Uhr",
          location = ort,
          playerOutfit = "Reisekleidung"
        )
      )
      val modelId = dao.insertMessage(
        MessageEntity(storyId = storyId, sender = "model", content = "Erzählung $nummer", checkpointId = cpId)
      )
      dao.insertMemory(MemoryEntity(storyId = storyId, turnNumber = nummer, text = "Erinnerung an Zug $nummer"))
      return user to dao.getMessageById(modelId)!!
    }

    val (_, eroeffnung) = zug(1, "Hafen", mitUserNachricht = false)
    val (user2, model2) = zug(2, "Markt", mitUserNachricht = true)
    val (user3, model3) = zug(3, "Turm", mitUserNachricht = true)

    return Verlauf(storyId, eroeffnung, user2!!, model2, user3!!, model3)
  }

  private suspend fun checkpoints(storyId: Long) = dao.getAllCheckpoints(storyId).first()

  // --- Verzweigen ---

  @Test
  fun `Zweig uebernimmt den Zustand des Verzweigungspunkts, nicht den neuesten`() = runBlocking {
    val v = baueVerlauf()

    val zweigId = service.branchStory(v.storyId, "Zweig", upToMessageId = v.modelZug2.id)

    val zweigCp = dao.getLatestCheckpoint(zweigId)
    assertNotNull(zweigCp)
    assertEquals("Der Zweig darf nicht mit dem Ort aus der Zukunft starten", "Markt", zweigCp!!.location)
    assertEquals(2, zweigCp.turnNumber)
  }

  @Test
  fun `Zweig uebernimmt nur die Nachrichten bis zum Verzweigungspunkt`() = runBlocking {
    val v = baueVerlauf()

    val zweigId = service.branchStory(v.storyId, "Zweig", upToMessageId = v.modelZug2.id)

    val nachrichten = dao.getMessagesSnapshot(zweigId)
    assertEquals(3, nachrichten.size)
    assertTrue(nachrichten.none { it.content == "Erzählung 3" })
  }

  @Test
  fun `der Ursprungsstrang bleibt unveraendert`() = runBlocking {
    val v = baueVerlauf()

    service.branchStory(v.storyId, "Zweig", upToMessageId = v.modelZug2.id)

    assertEquals("CLAUDE.md §0.7: Der Ursprung darf nicht mutieren", 5, dao.getMessagesSnapshot(v.storyId).size)
    assertEquals(3, checkpoints(v.storyId).size)
    assertEquals("Turm", dao.getLatestCheckpoint(v.storyId)!!.location)
    assertEquals(3, dao.getMemories(v.storyId).size)
  }

  @Test
  fun `kopierte Nachrichten zeigen nicht mehr auf die Checkpoints der Quelle`() = runBlocking {
    val v = baueVerlauf()

    val zweigId = service.branchStory(v.storyId, "Zweig", upToMessageId = v.modelZug2.id)

    val quellCpIds = checkpoints(v.storyId).map { it.id }.toSet()
    val verweise = dao.getMessagesSnapshot(zweigId).mapNotNull { it.checkpointId }
    assertTrue("Kein Verweis darf in die fremde Zeitlinie zeigen", verweise.none { it in quellCpIds })

    // Genau eine Nachricht trägt einen Verweis: die letzte, auf den kopierten Checkpoint.
    assertEquals(1, verweise.size)
    assertEquals(dao.getLatestCheckpoint(zweigId)!!.id, verweise.first())
  }

  @Test
  fun `Zweig ohne Verzweigungspunkt kopiert die ganze Geschichte`() = runBlocking {
    val v = baueVerlauf()

    val zweigId = service.branchStory(v.storyId, "Vollkopie", upToMessageId = null)

    assertEquals(5, dao.getMessagesSnapshot(zweigId).size)
    assertEquals("Turm", dao.getLatestCheckpoint(zweigId)!!.location)
  }

  @Test
  fun `der Zweig nimmt nur Erinnerungen bis zum Verzweigungspunkt mit`() = runBlocking {
    val v = baueVerlauf()

    val zweigId = service.branchStory(v.storyId, "Zweig", upToMessageId = v.modelZug2.id)

    val erinnerungen = dao.getMemories(zweigId)
    assertEquals(2, erinnerungen.size)
    assertTrue(erinnerungen.none { it.turnNumber > 2 })
  }

  // --- Zurückspulen ---

  @Test
  fun `Zurueckspulen ueber mehrere Zuege loescht alle spaeteren Checkpoints`() = runBlocking {
    val v = baueVerlauf()

    service.rewindToMessage(v.storyId, v.eroeffnung)

    val verbleibend = checkpoints(v.storyId)
    assertEquals("Nur der Zustand der Eröffnung darf übrig bleiben", 1, verbleibend.size)
    assertEquals("Hafen", verbleibend.first().location)
  }

  @Test
  fun `Zurueckspulen nimmt die Erinnerungen der verworfenen Zuege mit`() = runBlocking {
    val v = baueVerlauf()

    service.rewindToMessage(v.storyId, v.eroeffnung)

    val erinnerungen = dao.getMemories(v.storyId)
    assertEquals(1, erinnerungen.size)
    assertTrue("Die Welt darf sich nicht an Gelöschtes erinnern", erinnerungen.none { it.turnNumber > 1 })
  }

  @Test
  fun `Zurueckspulen um einen Zug behaelt den Zustand dieses Zugs`() = runBlocking {
    val v = baueVerlauf()

    service.rewindToMessage(v.storyId, v.modelZug2)

    val verbleibend = checkpoints(v.storyId)
    assertEquals(2, verbleibend.size)
    assertEquals("Markt", dao.getLatestCheckpoint(v.storyId)!!.location)
  }

  @Test
  fun `Zurueckspulen auf eine Spieler-Nachricht verwirft deren Antwort`() = runBlocking {
    val v = baueVerlauf()

    service.rewindToMessage(v.storyId, v.userZug3)

    // Die Antwort auf Zug 3 ist weg, also auch ihr Checkpoint und ihre Erinnerung.
    assertEquals("Markt", dao.getLatestCheckpoint(v.storyId)!!.location)
    assertEquals(2, checkpoints(v.storyId).size)
    assertTrue(dao.getMessagesSnapshot(v.storyId).none { it.content == "Erzählung 3" })
  }

  @Test
  fun `Zurueckspulen loescht die Nachrichten danach`() = runBlocking {
    val v = baueVerlauf()

    service.rewindToMessage(v.storyId, v.modelZug2)

    val nachrichten = dao.getMessagesSnapshot(v.storyId)
    assertEquals(3, nachrichten.size)
    assertEquals("Erzählung 2", nachrichten.last().content)
  }

  // --- Bearbeiten ---

  @Test
  fun `Bearbeiten behaelt den Checkpoint des abgeschlossenen Zugs`() = runBlocking {
    val v = baueVerlauf()

    service.truncateAndPrepareEdit(v.storyId, v.userZug3.id, "Ich gehe stattdessen nach Süden.")

    // Zug 2 war abgeschlossen, bevor die bearbeitete Nachricht geschrieben wurde -- sein Zustand
    // bleibt. Die alte Zählung über die Spieler-Nachrichten warf ihn mit weg.
    val verbleibend = checkpoints(v.storyId)
    assertEquals(2, verbleibend.size)
    assertEquals("Markt", dao.getLatestCheckpoint(v.storyId)!!.location)
  }

  @Test
  fun `Bearbeiten setzt den neuen Text und loescht die Antwort darauf`() = runBlocking {
    val v = baueVerlauf()

    service.truncateAndPrepareEdit(v.storyId, v.userZug3.id, "Ich gehe stattdessen nach Süden.")

    val nachrichten = dao.getMessagesSnapshot(v.storyId)
    assertEquals(4, nachrichten.size)
    assertEquals("Ich gehe stattdessen nach Süden.", nachrichten.last().content)
    assertTrue(nachrichten.none { it.content == "Erzählung 3" })
  }

  @Test
  fun `Bearbeiten der ersten Spieler-Nachricht behaelt die Eroeffnung`() = runBlocking {
    val v = baueVerlauf()

    service.truncateAndPrepareEdit(v.storyId, v.userZug2.id, "Ich bleibe am Hafen.")

    val verbleibend = checkpoints(v.storyId)
    assertEquals("Die Eröffnung belegt Zug 1 und darf nicht mit verschwinden", 1, verbleibend.size)
    assertEquals("Hafen", verbleibend.first().location)
    assertEquals(1, dao.getMemories(v.storyId).size)
  }

  @Test
  fun `Bearbeiten im Zweig greift nicht auf die Checkpoints der Quelle zu`() = runBlocking {
    val v = baueVerlauf()
    val zweigId = service.branchStory(v.storyId, "Zweig", upToMessageId = v.modelZug2.id)

    // Im Zweig weitererzählen und dann die dortige Spieler-Nachricht bearbeiten.
    val userId = dao.insertMessage(MessageEntity(storyId = zweigId, sender = "user", content = "Neue Richtung"))
    val cpId = dao.insertCheckpoint(
      CheckpointEntity(
        storyId = zweigId, turnNumber = 3, inGameTime = "Tag 3, 12:00 Uhr",
        location = "Wald", playerOutfit = "Reisekleidung"
      )
    )
    dao.insertMessage(MessageEntity(storyId = zweigId, sender = "model", content = "Zweig-Erzählung", checkpointId = cpId))

    service.truncateAndPrepareEdit(zweigId, userId, "Doch nicht.")

    assertEquals("Markt", dao.getLatestCheckpoint(zweigId)!!.location)
    assertEquals("Der Ursprung bleibt unberührt", 3, checkpoints(v.storyId).size)
  }

  @Test
  fun `ohne zuordenbaren Checkpoint wird nichts geloescht`() = runBlocking {
    // Eine Geschichte ohne jeden Checkpoint: Der sichere Ausweg ist, nichts anzufassen.
    val storyId = dao.insertStory(StoryEntity(title = "Leer", systemPrompt = "p"))
    val a = dao.insertMessage(MessageEntity(storyId = storyId, sender = "user", content = "A"))
    dao.insertMessage(MessageEntity(storyId = storyId, sender = "model", content = "B"))

    service.truncateAndPrepareEdit(storyId, a, "A neu")

    assertNull(dao.getLatestCheckpoint(storyId))
    assertEquals(1, dao.getMessagesSnapshot(storyId).size)
  }
}
