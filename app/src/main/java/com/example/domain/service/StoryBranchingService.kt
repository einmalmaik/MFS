package com.example.domain.service

import com.example.data.db.StoryDao
import com.example.data.model.MessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Domain service responsible for story branching (forking narrative timelines)
 * and history rollback/truncation (ensuring data integrity when rewinding or editing turns).
 */
class StoryBranchingService(
  private val storyDao: StoryDao
) {

  /**
   * Creates an isolated branch of an existing story, copying all messages and checkpoints
   * up to [upToMessageId] (or the full history if null).
   */
  suspend fun branchStory(
    sourceStoryId: Long,
    branchTitle: String,
    upToMessageId: Long? = null
  ): Long = withContext(Dispatchers.IO) {
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

    // Copy latest checkpoint
    val latestCp = storyDao.getLatestCheckpoint(sourceStoryId)
    if (latestCp != null) {
      storyDao.insertCheckpoint(
        latestCp.copy(
          id = 0,
          storyId = newStoryId
        )
      )
    }

    newStoryId
  }

  /**
   * Rewinds story history back to the specified [message].
   * Deletes all subsequent messages and rolls back stale checkpoints.
   */
  suspend fun rewindToMessage(
    storyId: Long,
    message: MessageEntity
  ) = withContext(Dispatchers.IO) {
    storyDao.deleteMessagesAfter(storyId, message.id)
    val latestCp = storyDao.getLatestCheckpoint(storyId)
    if (message.checkpointId != null && latestCp != null && message.checkpointId < latestCp.id) {
      storyDao.deleteCheckpointsAfterTurn(storyId, latestCp.turnNumber - 1)
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
    storyDao.deleteMessagesAfter(storyId, messageId)
    storyDao.updateMessageContent(messageId, newContent)

    val allRemaining = storyDao.getMessagesSnapshot(storyId)
    val userTurnIdx = allRemaining.count { it.sender == "user" }
    storyDao.deleteCheckpointsAfterTurn(storyId, userTurnIdx)
  }
}
