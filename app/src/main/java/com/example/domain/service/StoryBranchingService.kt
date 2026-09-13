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

    // Den Checkpoint übernehmen, der zum Verzweigungspunkt gehört - nicht den neuesten.
    // Andernfalls startet ein Zweig ab einem frühen Zug mit Zeit, Ort und Inventar aus der
    // Zukunft des Ursprungsstrangs (CLAUDE.md §0.7).
    val branchCheckpoint = checkpointAtMessage(sourceStoryId, messagesToCopy.lastOrNull())
      ?: storyDao.getLatestCheckpoint(sourceStoryId)
    val newCheckpointId = if (branchCheckpoint != null) {
      storyDao.insertCheckpoint(
        branchCheckpoint.copy(
          id = 0,
          storyId = newStoryId
        )
      )
    } else {
      null
    }

    // checkpointId zeigt auf eine Zeile in `checkpoints`, und die IDs sind global. Kopiert man
    // sie unverändert mit, verweisen die Nachrichten des Zweigs auf die Checkpoints des
    // Ursprungsstrangs -- eine fremde Zeitlinie, aus der Zurückspulen und Bearbeiten dann ihre
    // Zugnummern zögen. Der Zweig übernimmt genau einen Checkpoint, also darf auch nur genau
    // eine Nachricht auf ihn zeigen: die letzte. Für alles davor ist `null` die Wahrheit, und
    // getCheckpointAtOrBeforeMessage kommt damit zurecht.
    val lastSourceId = messagesToCopy.lastOrNull()?.id
    for (m in messagesToCopy) {
      storyDao.insertMessage(
        m.copy(
          id = 0,
          storyId = newStoryId,
          checkpointId = if (m.id == lastSourceId && m.checkpointId != null) newCheckpointId else null
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
   * Findet den Zustand, der an [message] galt. Bevorzugt den direkt an der Nachricht hinterlegten
   * Checkpoint; fehlt der (etwa bei Nutzer-Nachrichten), wird der jüngste Checkpoint verwendet,
   * der nicht nach dieser Nachricht entstanden ist.
   *
   * Gibt bewusst `null` zurück, statt auf den neuesten Checkpoint auszuweichen. Für das
   * Verzweigen ist dieser Ausweg vertretbar, fürs Zurückspulen nicht: Dort heißt "neuester
   * Checkpoint" nicht "irgendein Zustand", sondern "lösche alles danach" — und danach liegt
   * nichts. Jede Aufrufstelle entscheidet ihren Ausweg deshalb selbst.
   */
  private suspend fun checkpointAtMessage(
    storyId: Long,
    message: MessageEntity?
  ): CheckpointEntity? {
    if (message == null) return null

    message.checkpointId?.let { id ->
      // Die Zugehörigkeit prüfen: Checkpoint-IDs sind global, und ein Spielstand aus der Zeit
      // vor dem Umschreiben der Zweig-Kopie kann noch auf eine fremde Geschichte zeigen. Deren
      // Zugnummer hier zu verwenden hieße, im eigenen Strang nach fremdem Maß zu löschen.
      storyDao.getCheckpointById(id)?.takeIf { it.storyId == storyId }?.let { return it }
    }

    return storyDao.getCheckpointAtOrBeforeMessage(storyId, message.id)
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

      // Maßgeblich ist der Zug, zu dem die Nachricht gehört -- nicht der neueste. Vorher stand
      // hier latestCp.turnNumber - 1, und das löschte immer genau einen Checkpoint, egal wie
      // weit zurückgespult wurde. Wer über zehn Züge zurückgeht, behielt neun Checkpoints und
      // neun Erinnerungen aus einer Zukunft, die es nicht mehr gibt: Die Erzählung fährt mit
      // Uhrzeit, Ort und Inventar von damals fort und erinnert sich an Ereignisse, die in
      // dieser Zeitlinie nie stattfanden.
      val zielCp = checkpointAtMessage(storyId, message)
      if (zielCp != null) {
        storyDao.deleteCheckpointsAfterTurn(storyId, zielCp.turnNumber)
        storyDao.deleteMemoriesAfterTurn(storyId, zielCp.turnNumber)
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

      // Die Zugnummer stand hier als Anzahl der Spieler-Nachrichten minus eins -- eine Schätzung,
      // die um genau einen Zug daneben lag: Die Eröffnung belegt Zug 1, ohne dass ihr eine
      // Spieler-Nachricht vorausgeht. Bei jeder Bearbeitung fiel deshalb ein Checkpoint zu viel
      // weg, und mit ihm der Zustand eines Zugs, der stehen bleiben sollte.
      //
      // Bearbeitet werden nur Spieler-Nachrichten (StoryMessageItem bietet es nur dort an), die
      // tragen selbst keinen Checkpoint. Gesucht ist der Zustand, der vor dieser Nachricht galt.
      val zielCp = storyDao.getCheckpointAtOrBeforeMessage(storyId, messageId)
      if (zielCp != null) {
        storyDao.deleteCheckpointsAfterTurn(storyId, zielCp.turnNumber)
        storyDao.deleteMemoriesAfterTurn(storyId, zielCp.turnNumber)
      }
    }
  }
}
