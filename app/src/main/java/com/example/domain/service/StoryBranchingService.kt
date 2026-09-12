package com.example.domain.service

import androidx.room.withTransaction
import com.example.data.db.StoryDao
import com.example.data.db.StoryDatabase
import com.example.data.model.CheckpointEntity
import com.example.data.model.MessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Domain service responsible for story branching (forking narrative timelines)
 * and history rollback/truncation (ensuring data integrity when rewinding or editing turns).
 */
class StoryBranchingService(
  private val storyDao: StoryDao,
  private val database: StoryDatabase
) {

  /**
   * Creates an isolated branch of an existing story, copying all messages and checkpoints
   * up to [upToMessageId] (or the full history if null).
   *
   * Alles in einer Transaktion: Der Zweig kopiert Nachricht für Nachricht und Erinnerung für
   * Erinnerung. Bei einer langen Geschichte sind das mehrere tausend Schreibvorgänge — einzeln
   * committet kostet jeder davon eine eigene Runde auf den Geräte-Flash, zusammen zehn bis
   * vierzig Sekunden. Schlimmer noch: Stirbt der Prozess dazwischen, bleibt ein halber Zweig
   * liegen, den niemand als unvollständig erkennen kann (CLAUDE.md §0.7, §4).
   */
  suspend fun branchStory(
    sourceStoryId: Long,
    branchTitle: String,
    upToMessageId: Long? = null
  ): Long = withContext(Dispatchers.IO) {
    database.withTransaction { copyIntoNewStory(sourceStoryId, branchTitle, upToMessageId) }
  }

  private suspend fun copyIntoNewStory(
    sourceStoryId: Long,
    branchTitle: String,
    upToMessageId: Long?
  ): Long {
    val sourceStory = storyDao.getStoryById(sourceStoryId)
      ?: throw IllegalArgumentException("Quell-Story $sourceStoryId nicht gefunden.")

    val newStory = sourceStory.copy(
      id = 0,
      title = branchTitle.ifBlank { "${sourceStory.title} (Zweig)" },
      createdAt = System.currentTimeMillis(),
      updatedAt = System.currentTimeMillis()
    )
    val newStoryId = storyDao.insertStory(newStory)

    // Copy messages
    val messages = storyDao.getMessagesSnapshot(sourceStoryId)
    val messagesToCopy = if (upToMessageId != null) {
      messages.filter { it.id <= upToMessageId }
    } else {
      messages
    }

    for (m in messagesToCopy) {
      storyDao.insertMessage(
        m.copy(
          id = 0,
          storyId = newStoryId
        )
      )
    }

    // Den Checkpoint übernehmen, der zum Verzweigungspunkt gehört - nicht den neuesten.
    // Andernfalls startet ein Zweig ab einem frühen Zug mit Zeit, Ort und Inventar aus der
    // Zukunft des Ursprungsstrangs (CLAUDE.md §0.7).
    val branchCheckpoint = resolveBranchCheckpoint(sourceStoryId, messagesToCopy.lastOrNull())
    if (branchCheckpoint != null) {
      storyDao.insertCheckpoint(
        branchCheckpoint.copy(
          id = 0,
          storyId = newStoryId
        )
      )
    }

    // Figuren und episodisches Gedächtnis mitnehmen, sonst beginnt der Zweig gedächtnislos.
    val branchDay = branchCheckpoint?.extractDayNumber() ?: Int.MAX_VALUE
    val branchTurn = branchCheckpoint?.turnNumber ?: Int.MAX_VALUE

    for (npc in storyDao.getNpcs(sourceStoryId)) {
      if (npc.firstMetDay <= branchDay) {
        storyDao.insertNpc(npc.copy(id = 0, storyId = newStoryId))
      }
    }

    for (memory in storyDao.getMemories(sourceStoryId)) {
      if (memory.turnNumber <= branchTurn) {
        storyDao.insertMemory(memory.copy(id = 0, storyId = newStoryId))
      }
    }

    return newStoryId
  }

  /**
   * Findet den Zustand, der am Verzweigungspunkt galt. Bevorzugt den direkt an der Nachricht
   * hinterlegten Checkpoint; fehlt der (etwa bei Nutzer-Nachrichten), wird der jüngste
   * Checkpoint verwendet, der nicht nach dieser Nachricht entstanden ist.
   */
  private suspend fun resolveBranchCheckpoint(
    sourceStoryId: Long,
    lastCopiedMessage: MessageEntity?
  ): CheckpointEntity? {
    if (lastCopiedMessage == null) return storyDao.getLatestCheckpoint(sourceStoryId)

    lastCopiedMessage.checkpointId?.let { id ->
      storyDao.getCheckpointById(id)?.let { return it }
    }

    return storyDao.getCheckpointAtOrBeforeMessage(sourceStoryId, lastCopiedMessage.id)
      ?: storyDao.getLatestCheckpoint(sourceStoryId)
  }

  /**
   * Rewinds story history back to the specified [message].
   * Deletes all subsequent messages and rolls back stale checkpoints.
   */
  suspend fun rewindToMessage(
    storyId: Long,
    message: MessageEntity
  ) = withContext(Dispatchers.IO) {
    database.withTransaction {
      storyDao.deleteMessagesAfter(storyId, message.id)
      val latestCp = storyDao.getLatestCheckpoint(storyId)
      if (message.checkpointId != null && latestCp != null && message.checkpointId < latestCp.id) {
        storyDao.deleteCheckpointsAfterTurn(storyId, latestCp.turnNumber - 1)
        // Erinnerungen an zurückgenommene Züge müssen mit verschwinden, sonst erinnert sich die
        // Welt an Ereignisse, die es in dieser Zeitlinie nicht mehr gibt.
        storyDao.deleteMemoriesAfterTurn(storyId, latestCp.turnNumber - 1)
      }
    }
  }

  /**
   * Prepares a message edit by purging all forward history and updating the content.
   */
  suspend fun truncateAndPrepareEdit(
    storyId: Long,
    messageId: Long,
    newContent: String
  ) = withContext(Dispatchers.IO) {
    database.withTransaction {
      storyDao.deleteMessagesAfter(storyId, messageId)
      storyDao.updateMessageContent(messageId, newContent)

      val allRemaining = storyDao.getMessagesSnapshot(storyId)
      val userTurnIdx = allRemaining.count { it.sender == "user" }
      val keepUpToTurn = (userTurnIdx - 1).coerceAtLeast(0)
      storyDao.deleteCheckpointsAfterTurn(storyId, keepUpToTurn)
      storyDao.deleteMemoriesAfterTurn(storyId, keepUpToTurn)
    }
  }
}
