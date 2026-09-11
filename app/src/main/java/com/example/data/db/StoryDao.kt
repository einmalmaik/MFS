package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CheckpointEntity
import com.example.data.model.MessageEntity
import com.example.data.model.StoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryDao {

  // Stories
  @Query("SELECT * FROM stories ORDER BY updatedAt DESC")
  fun getAllStories(): Flow<List<StoryEntity>>

  @Query("SELECT * FROM stories WHERE id = :id LIMIT 1")
  suspend fun getStoryById(id: Long): StoryEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertStory(story: StoryEntity): Long

  @Update
  suspend fun updateStory(story: StoryEntity)

  @Delete
  suspend fun deleteStory(story: StoryEntity)

  @Query("DELETE FROM stories WHERE id = :storyId")
  suspend fun deleteStoryById(storyId: Long)

  // Messages
  @Query("SELECT * FROM messages WHERE storyId = :storyId ORDER BY id ASC")
  fun getMessagesForStory(storyId: Long): Flow<List<MessageEntity>>

  @Query("SELECT * FROM messages WHERE storyId = :storyId ORDER BY id ASC")
  suspend fun getMessagesSnapshot(storyId: Long): List<MessageEntity>

  @Query("SELECT * FROM messages WHERE id = :messageId LIMIT 1")
  suspend fun getMessageById(messageId: Long): MessageEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMessage(message: MessageEntity): Long

  @Query("UPDATE messages SET content = :newContent WHERE id = :messageId")
  suspend fun updateMessageContent(messageId: Long, newContent: String)

  @Query("DELETE FROM messages WHERE storyId = :storyId AND id > :messageId")
  suspend fun deleteMessagesAfter(storyId: Long, messageId: Long)

  @Query("DELETE FROM messages WHERE id = :messageId")
  suspend fun deleteMessage(messageId: Long)

  // Checkpoints
  @Query("SELECT * FROM checkpoints WHERE storyId = :storyId ORDER BY turnNumber DESC, id DESC LIMIT 1")
  suspend fun getLatestCheckpoint(storyId: Long): CheckpointEntity?

  @Query("SELECT * FROM checkpoints WHERE storyId = :storyId ORDER BY turnNumber DESC, id DESC LIMIT 1")
  fun observeLatestCheckpoint(storyId: Long): Flow<CheckpointEntity?>

  @Query("SELECT * FROM checkpoints WHERE storyId = :storyId ORDER BY turnNumber DESC, id DESC")
  fun getAllCheckpoints(storyId: Long): Flow<List<CheckpointEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCheckpoint(checkpoint: CheckpointEntity): Long

  @Update
  suspend fun updateCheckpoint(checkpoint: CheckpointEntity)

  @Query("DELETE FROM checkpoints WHERE storyId = :storyId AND turnNumber > :turnNumber")
  suspend fun deleteCheckpointsAfterTurn(storyId: Long, turnNumber: Int)

  @Query("DELETE FROM checkpoints WHERE storyId = :storyId")
  suspend fun deleteAllCheckpointsForStory(storyId: Long)

  @Query("DELETE FROM messages WHERE storyId = :storyId")
  suspend fun deleteAllMessagesForStory(storyId: Long)
}
