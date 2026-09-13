package com.example.domain.service

import android.util.Base64
import com.example.data.db.StoryDao
import com.example.data.model.CheckpointEntity
import com.example.data.model.MemoryEntity
import com.example.data.model.MessageEntity
import com.example.data.model.NpcEntity
import com.example.data.model.StoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Verantwortlich für den vollständigen, verlustfreien Export und Import einzelner Geschichten.
 *
 * Sichert die Story mit allen Checkpoints, Nachrichten, NPCs und dem episodischen Vektor-Gedächtnis.
 * Beim Import werden neue Primärschlüssel vergeben und alle Fremdschlüssel (`checkpointId`, `npcId`)
 * konsistent neu verdrahtet, damit niemals Konflikte mit bestehenden Geschichten entstehen.
 */
class StoryBackupService(
  private val storyDao: StoryDao
) {

  companion object {
    const val FORMAT_HEADER = "maunting_story_backup"
    const val CURRENT_FORMAT_VERSION = 1
  }

  /**
   * Exportiert eine Geschichte mit allen abhängigen Daten in ein versioniertes JSON.
   */
  suspend fun exportStoryToJson(storyId: Long): String = withContext(Dispatchers.IO) {
    val story = storyDao.getStoryById(storyId)
      ?: throw IllegalArgumentException("Geschichte mit ID $storyId nicht gefunden.")

    val checkpoints = storyDao.getCheckpointsSnapshot(storyId)
    val messages = storyDao.getMessagesSnapshot(storyId)
    val npcs = storyDao.getNpcs(storyId)
    val memories = storyDao.getMemories(storyId)

    val root = JSONObject().apply {
      put("format", FORMAT_HEADER)
      put("version", CURRENT_FORMAT_VERSION)
      put("exportedAt", System.currentTimeMillis())

      // Story
      put("story", JSONObject().apply {
        put("title", story.title)
        put("systemPrompt", story.systemPrompt)
        put("genre", story.genre)
        put("perspective", story.perspective)
        put("playTimeSeconds", story.playTimeSeconds)
        put("createdAt", story.createdAt)
        put("updatedAt", story.updatedAt)
      })

      // Checkpoints
      val cpArray = JSONArray()
      for (cp in checkpoints) {
        cpArray.put(JSONObject().apply {
          put("originalId", cp.id)
          put("turnNumber", cp.turnNumber)
          put("inGameTime", cp.inGameTime)
          put("location", cp.location)
          put("weather", cp.weather)
          put("playerOutfit", cp.playerOutfit)
          put("playerInventory", cp.playerInventory)
          put("playerCondition", cp.playerCondition)
          put("npcsJson", cp.npcsJson)
          put("previousEventsSummary", cp.previousEventsSummary)
          put("milestonesJson", cp.milestonesJson)
          put("rawStateJson", cp.rawStateJson)
          put("timestamp", cp.timestamp)
        })
      }
      put("checkpoints", cpArray)

      // Messages
      val msgArray = JSONArray()
      for (msg in messages) {
        msgArray.put(JSONObject().apply {
          put("sender", msg.sender)
          put("content", msg.content)
          put("inGameTimeTag", msg.inGameTimeTag ?: "")
          put("originalCheckpointId", msg.checkpointId ?: 0L)
          put("timestamp", msg.timestamp)
        })
      }
      put("messages", msgArray)

      // NPCs
      val npcArray = JSONArray()
      for (npc in npcs) {
        npcArray.put(JSONObject().apply {
          put("originalId", npc.id)
          put("canonicalName", npc.canonicalName)
          put("aliasesJson", npc.aliasesJson)
          put("gender", npc.gender)
          put("appearance", npc.appearance)
          put("personality", npc.personality)
          put("firstMetDay", npc.firstMetDay)
          put("lastSeenDay", npc.lastSeenDay)
          put("relationship", npc.relationship)
          put("currentMood", npc.currentMood)
          put("condition", npc.condition)
          put("outfit", npc.outfit)
          put("status", npc.status)
          put("isAlive", npc.isAlive)
          put("updatedAt", npc.updatedAt)
        })
      }
      put("npcs", npcArray)

      // Memories
      val memArray = JSONArray()
      for (mem in memories) {
        memArray.put(JSONObject().apply {
          put("kind", mem.kind)
          put("dayNumber", mem.dayNumber)
          put("inGameTime", mem.inGameTime)
          put("turnNumber", mem.turnNumber)
          put("originalNpcId", mem.npcId ?: 0L)
          put("text", mem.text)
          if (mem.embedding != null && mem.embedding.isNotEmpty()) {
            put("embeddingBase64", Base64.encodeToString(mem.embedding, Base64.NO_WRAP))
          }
          put("createdAt", mem.createdAt)
        })
      }
      put("memories", memArray)
    }

    root.toString(2)
  }

  /**
   * Importiert ein Backup-JSON als neue, eigenständige Geschichte.
   *
   * @return Die neu generierte `storyId` in der lokalen Room-Datenbank.
   */
  suspend fun importStoryFromJson(jsonString: String): Long = withContext(Dispatchers.IO) {
    val root = JSONObject(jsonString)
    val format = root.optString("format", "")
    if (format != FORMAT_HEADER && format != "storyforge_backup") {
      throw IllegalArgumentException("Ungültiges Backup-Format: $format")
    }

    val storyObj = root.getJSONObject("story")
    val title = storyObj.optString("title", "Importierte Geschichte")
    val systemPrompt = storyObj.optString("systemPrompt", "")
    val genre = storyObj.optString("genre", "")
    val perspective = storyObj.optString("perspective", "Zweite Person (Du)")
    val playTimeSeconds = storyObj.optLong("playTimeSeconds", 0L)
    val createdAt = storyObj.optLong("createdAt", System.currentTimeMillis())
    val updatedAt = storyObj.optLong("updatedAt", System.currentTimeMillis())

    // 1. Neue Story in DB anlegen
    val newStory = StoryEntity(
      id = 0, // Auto-Generate
      title = title,
      systemPrompt = systemPrompt,
      genre = genre,
      perspective = perspective,
      playTimeSeconds = playTimeSeconds,
      createdAt = createdAt,
      updatedAt = updatedAt
    )
    val newStoryId = storyDao.insertStory(newStory)

    // 2. Checkpoints importieren & Id-Mapping aufbauen
    val checkpointIdMap = mutableMapOf<Long, Long>()
    val cpArray = root.optJSONArray("checkpoints")
    if (cpArray != null) {
      for (i in 0 until cpArray.length()) {
        val cpObj = cpArray.getJSONObject(i)
        val origId = cpObj.optLong("originalId", 0L)
        val entity = CheckpointEntity(
          id = 0, // Auto-Generate
          storyId = newStoryId,
          turnNumber = cpObj.optInt("turnNumber", 0),
          inGameTime = cpObj.optString("inGameTime", ""),
          location = cpObj.optString("location", ""),
          weather = cpObj.optString("weather", ""),
          playerOutfit = cpObj.optString("playerOutfit", ""),
          playerInventory = cpObj.optString("playerInventory", "[]"),
          playerCondition = cpObj.optString("playerCondition", "Unverletzt"),
          npcsJson = cpObj.optString("npcsJson", "[]"),
          previousEventsSummary = cpObj.optString("previousEventsSummary", ""),
          milestonesJson = cpObj.optString("milestonesJson", "[]"),
          rawStateJson = cpObj.optString("rawStateJson", "{}"),
          timestamp = cpObj.optLong("timestamp", System.currentTimeMillis())
        )
        val newCpId = storyDao.insertCheckpoint(entity)
        if (origId > 0) {
          checkpointIdMap[origId] = newCpId
        }
      }
    }

    // 3. NPCs importieren & Id-Mapping aufbauen
    val npcIdMap = mutableMapOf<Long, Long>()
    val npcArray = root.optJSONArray("npcs")
    if (npcArray != null) {
      for (i in 0 until npcArray.length()) {
        val npcObj = npcArray.getJSONObject(i)
        val origId = npcObj.optLong("originalId", 0L)
        val entity = NpcEntity(
          id = 0, // Auto-Generate
          storyId = newStoryId,
          canonicalName = npcObj.optString("canonicalName", "Unbekannt"),
          aliasesJson = npcObj.optString("aliasesJson", "[]"),
          gender = npcObj.optString("gender", "FEMALE"),
          appearance = npcObj.optString("appearance", ""),
          personality = npcObj.optString("personality", ""),
          firstMetDay = npcObj.optInt("firstMetDay", 1),
          lastSeenDay = npcObj.optInt("lastSeenDay", 1),
          relationship = npcObj.optString("relationship", "Neutral"),
          currentMood = npcObj.optString("currentMood", "Ruhig"),
          condition = npcObj.optString("condition", ""),
          outfit = npcObj.optString("outfit", ""),
          status = npcObj.optString("status", "Anwesend"),
          isAlive = npcObj.optBoolean("isAlive", true),
          updatedAt = npcObj.optLong("updatedAt", System.currentTimeMillis())
        )
        val newNpcId = storyDao.insertNpc(entity)
        if (origId > 0) {
          npcIdMap[origId] = newNpcId
        }
      }
    }

    // 4. Messages importieren & CheckpointId remappen
    val msgArray = root.optJSONArray("messages")
    if (msgArray != null) {
      for (i in 0 until msgArray.length()) {
        val msgObj = msgArray.getJSONObject(i)
        val origCpId = msgObj.optLong("originalCheckpointId", 0L)
        val remappedCpId = if (origCpId > 0) checkpointIdMap[origCpId] else null
        val inGameTag = msgObj.optString("inGameTimeTag", "").ifBlank { null }

        val entity = MessageEntity(
          id = 0, // Auto-Generate
          storyId = newStoryId,
          sender = msgObj.optString("sender", "model"),
          content = msgObj.optString("content", ""),
          inGameTimeTag = inGameTag,
          checkpointId = remappedCpId,
          timestamp = msgObj.optLong("timestamp", System.currentTimeMillis())
        )
        storyDao.insertMessage(entity)
      }
    }

    // 5. Memories importieren & NpcId remappen
    val memArray = root.optJSONArray("memories")
    if (memArray != null) {
      for (i in 0 until memArray.length()) {
        val memObj = memArray.getJSONObject(i)
        val origNpcId = memObj.optLong("originalNpcId", 0L)
        val remappedNpcId = if (origNpcId > 0) npcIdMap[origNpcId] else null
        val b64 = memObj.optString("embeddingBase64", "")
        val embeddingBytes = if (b64.isNotBlank()) {
          try { Base64.decode(b64, Base64.DEFAULT) } catch (_: Exception) { null }
        } else null

        val entity = MemoryEntity(
          id = 0, // Auto-Generate
          storyId = newStoryId,
          kind = memObj.optString("kind", "event"),
          dayNumber = memObj.optInt("dayNumber", 1),
          inGameTime = memObj.optString("inGameTime", ""),
          turnNumber = memObj.optInt("turnNumber", 0),
          npcId = remappedNpcId,
          text = memObj.optString("text", ""),
          embedding = embeddingBytes,
          createdAt = memObj.optLong("createdAt", System.currentTimeMillis())
        )
        storyDao.insertMemory(entity)
      }
    }

    newStoryId
  }
}
