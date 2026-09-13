package com.example.domain.service

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.StoryDao
import com.example.data.db.StoryDatabase
import com.example.data.model.CheckpointEntity
import com.example.data.model.MemoryEntity
import com.example.data.model.MessageEntity
import com.example.data.model.NpcEntity
import com.example.data.model.StoryEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StoryBackupServiceTest {

  private lateinit var db: StoryDatabase
  private lateinit var dao: StoryDao
  private lateinit var backupService: StoryBackupService

  @Before
  fun setUp() {
    db = Room.inMemoryDatabaseBuilder(
      ApplicationProvider.getApplicationContext(),
      StoryDatabase::class.java
    ).allowMainThreadQueries().build()
    dao = db.storyDao()
    backupService = StoryBackupService(dao)
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun `export and import roundtrip preserves all story data, checkpoints, npcs, and memories`() = runBlocking {
    // 1. Ursprungsgeschichte mit Daten aufbauen
    val storyId = dao.insertStory(
      StoryEntity(
        title = "Flucht aus New Shore",
        systemPrompt = "Düsterer Cyberpunk-Thriller",
        genre = "Cyberpunk",
        perspective = "Zweite Person (Du)",
        playTimeSeconds = 3600L
      )
    )

    val cp1 = dao.insertCheckpoint(
      CheckpointEntity(
        storyId = storyId,
        turnNumber = 1,
        inGameTime = "Tag 1, 02:00 Uhr",
        location = "Regennasse Seitengasse",
        weather = "Saurer Dauerregen",
        playerOutfit = "Schwarzer Trenchcoat, Kampfstiefel",
        playerInventory = "[\"Glock 19\", \"Datenchip\"]",
        playerCondition = "Platzwunde an der Stirn",
        npcsJson = "[{\"name\": \"Elena\", \"status\": \"Anwesend\"}]",
        previousEventsSummary = "Du bist knapp den Schlägern entkommen.",
        rawStateJson = "{\"injuries\":[{\"bodyPart\":\"HEAD\",\"characterName\":\"Du\",\"description\":\"Platzwunde\",\"organ\":\"SKULL\",\"severity\":\"MODERATE\"}]}"
      )
    )

    val npc1 = dao.insertNpc(
      NpcEntity(
        storyId = storyId,
        canonicalName = "Elena",
        aliasesJson = "[\"Leni\"]",
        gender = "FEMALE",
        appearance = "Rote Haare, Lederjacke",
        personality = "Misstrauisch, loyal",
        condition = "Erschöpft",
        outfit = "Schwarze Lederjacke"
      )
    )

    dao.insertMessage(
      MessageEntity(
        storyId = storyId,
        sender = "user",
        content = "Ich ducke mich hinter den Müllcontainer.",
        inGameTimeTag = "Tag 1, 02:05 Uhr",
        checkpointId = cp1
      )
    )

    dao.insertMessage(
      MessageEntity(
        storyId = storyId,
        sender = "model",
        content = "Die Schläger laufen fluchend an dir vorbei.",
        inGameTimeTag = "Tag 1, 02:06 Uhr",
        checkpointId = cp1
      )
    )

    val dummyEmbedding = byteArrayOf(1, 2, 3, 4, 5)
    dao.insertMemory(
      MemoryEntity(
        storyId = storyId,
        kind = "event",
        dayNumber = 1,
        inGameTime = "Tag 1, 02:00 Uhr",
        turnNumber = 1,
        npcId = npc1,
        text = "Du hast Elena vor den Schlägern gerettet.",
        embedding = dummyEmbedding
      )
    )

    // 2. Export nach JSON
    val json = backupService.exportStoryToJson(storyId)
    assertTrue("JSON sollte Format-Header enthalten", json.contains(StoryBackupService.FORMAT_HEADER))
    assertTrue("JSON sollte Titel enthalten", json.contains("Flucht aus New Shore"))
    assertTrue("JSON sollte Checkpoint-Details enthalten", json.contains("Regennasse Seitengasse"))
    assertTrue("JSON sollte Platzwunde enthalten", json.contains("Platzwunde"))

    // 3. Import als neue Story
    val importedStoryId = backupService.importStoryFromJson(json)
    assertTrue("Neue Story-ID muss positiv und ungleich 0 sein", importedStoryId > 0)
    assertTrue("Neue Story-ID muss sich von der alten unterscheiden", importedStoryId != storyId)

    // 4. Verifikation der importierten Story
    val importedStory = dao.getStoryById(importedStoryId)
    assertNotNull(importedStory)
    assertEquals("Flucht aus New Shore", importedStory!!.title)
    assertEquals("Düsterer Cyberpunk-Thriller", importedStory.systemPrompt)
    assertEquals(3600L, importedStory.playTimeSeconds)

    // Checkpoints
    val importedCps = dao.getCheckpointsSnapshot(importedStoryId)
    assertEquals(1, importedCps.size)
    val importedCp = importedCps[0]
    assertEquals(1, importedCp.turnNumber)
    assertEquals("Regennasse Seitengasse", importedCp.location)
    assertEquals("Platzwunde an der Stirn", importedCp.playerCondition)
    assertTrue(importedCp.rawStateJson.contains("SKULL"))

    // NPCs
    val importedNpcs = dao.getNpcs(importedStoryId)
    assertEquals(1, importedNpcs.size)
    val importedNpc = importedNpcs[0]
    assertEquals("Elena", importedNpc.canonicalName)
    assertEquals("Erschöpft", importedNpc.condition)

    // Messages & Checkpoint remapping
    val importedMsgs = dao.getMessagesSnapshot(importedStoryId)
    assertEquals(2, importedMsgs.size)
    assertEquals(importedStoryId, importedMsgs[0].storyId)
    assertEquals(importedCp.id, importedMsgs[0].checkpointId)
    assertEquals("Ich ducke mich hinter den Müllcontainer.", importedMsgs[0].content)

    // Memories & NPC remapping & Embedding
    val importedMems = dao.getMemories(importedStoryId)
    assertEquals(1, importedMems.size)
    assertEquals(importedStoryId, importedMems[0].storyId)
    assertEquals(importedNpc.id, importedMems[0].npcId)
    assertEquals("Du hast Elena vor den Schlägern gerettet.", importedMems[0].text)
    assertNotNull(importedMems[0].embedding)
    assertTrue(dummyEmbedding.contentEquals(importedMems[0].embedding!!))
  }
}
