package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CheckpointEntity
import com.example.data.model.MemoryEntity
import com.example.data.model.MessageEntity
import com.example.data.model.NpcEntity
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

  /**
   * Schreibt nur Titel und Genre.
   *
   * Die Zustands-Extraktion trägt beides nach dem ersten Zug nach, arbeitet dabei aber auf dem
   * Schnappschuss vom Zugbeginn. Mit `updateStory` hätte sie die ganze Zeile zurückgeschrieben
   * und dabei einen währenddessen gespeicherten Prompt, die mitgezählte Spielzeit und den
   * Archiv-Status auf den alten Stand zurückgedreht. Diese Abfrage kann das strukturell nicht.
   */
  @Query("UPDATE stories SET title = :title, genre = :genre, updatedAt = :updatedAt WHERE id = :id")
  suspend fun updateStoryTitleAndGenre(id: Long, title: String, genre: String, updatedAt: Long)

  /** Was der Prompt-Bogen ändern darf — aus demselben Grund gezielt statt zeilenweise. */
  @Query("UPDATE stories SET title = :title, systemPrompt = :systemPrompt, updatedAt = :updatedAt WHERE id = :id")
  suspend fun updateStoryTitleAndPrompt(id: Long, title: String, systemPrompt: String, updatedAt: Long)

  @Query("DELETE FROM stories WHERE id = :storyId")
  suspend fun deleteStoryById(storyId: Long)

  // Messages
  @Query("SELECT * FROM messages WHERE storyId = :storyId ORDER BY id ASC")
  fun getMessagesForStory(storyId: Long): Flow<List<MessageEntity>>

  @Query("SELECT * FROM messages WHERE storyId = :storyId ORDER BY id ASC")
  suspend fun getMessagesSnapshot(storyId: Long): List<MessageEntity>

  /**
   * Lädt nur das Arbeitsfenster statt der gesamten Historie.
   * Ergebnis ist absteigend sortiert und muss vom Aufrufer umgedreht werden.
   */
  @Query("SELECT * FROM messages WHERE storyId = :storyId ORDER BY id DESC LIMIT :limit")
  suspend fun getRecentMessages(storyId: Long, limit: Int): List<MessageEntity>

  @Query("SELECT * FROM messages WHERE storyId = :storyId AND embeddingJson IS NOT NULL ORDER BY id ASC")
  suspend fun getMessagesWithEmbeddings(storyId: Long): List<MessageEntity>

  /**
   * Gibt die Alt-Embeddings frei, nachdem sie ins episodische Gedächtnis übernommen wurden.
   *
   * In `embeddingJson` steht pro Nachricht ein Vektor als JSON-Text, 8 bis 12 KB. Gelesen wird
   * die Spalte nur ein einziges Mal — beim Übernehmen. Danach zieht die Oberfläche sie bei jeder
   * neuen Nachricht sinnlos mit: bei einer vor DB-Version 9 begonnenen Geschichte mit 5000
   * Nachrichten sind das 40 bis 60 MB, die pro Zug neu aus der Datenbank materialisiert werden.
   * Die Spalte selbst bleibt bestehen — ein Spalten-Drop in SQLite erzwänge einen Tabellen-Rebuild.
   */
  @Query("UPDATE messages SET embeddingJson = NULL WHERE storyId = :storyId AND embeddingJson IS NOT NULL")
  suspend fun clearLegacyEmbeddings(storyId: Long)

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

  @Query("SELECT * FROM checkpoints WHERE id = :checkpointId LIMIT 1")
  suspend fun getCheckpointById(checkpointId: Long): CheckpointEntity?

  /**
   * Der zuletzt gültige Zustand bis einschließlich einer Nachricht — die Grundlage dafür,
   * dass ein Handlungszweig nicht mit einem Zustand aus der Zukunft startet.
   */
  @Query(
    """
    SELECT c.* FROM checkpoints c
    WHERE c.storyId = :storyId
      AND c.id <= COALESCE(
        (SELECT MAX(m.checkpointId) FROM messages m
          WHERE m.storyId = :storyId AND m.id <= :messageId AND m.checkpointId IS NOT NULL),
        0
      )
    ORDER BY c.turnNumber DESC, c.id DESC
    LIMIT 1
    """
  )
  suspend fun getCheckpointAtOrBeforeMessage(storyId: Long, messageId: Long): CheckpointEntity?

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

  // Memories (episodisches Gedächtnis)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMemory(memory: MemoryEntity): Long

  @Query("SELECT * FROM memories WHERE storyId = :storyId ORDER BY id ASC")
  suspend fun getMemories(storyId: Long): List<MemoryEntity>

  @Query("SELECT * FROM memories WHERE storyId = :storyId AND kind = :kind ORDER BY dayNumber DESC, id DESC LIMIT :limit")
  suspend fun getRecentMemoriesOfKind(storyId: Long, kind: String, limit: Int): List<MemoryEntity>

  @Query("SELECT COUNT(*) FROM memories WHERE storyId = :storyId")
  suspend fun countMemories(storyId: Long): Int

  @Query("DELETE FROM memories WHERE storyId = :storyId AND turnNumber > :turnNumber")
  suspend fun deleteMemoriesAfterTurn(storyId: Long, turnNumber: Int)

  @Query("DELETE FROM memories WHERE storyId = :storyId")
  suspend fun deleteAllMemoriesForStory(storyId: Long)

  // NPCs (Identitätsgedächtnis)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertNpc(npc: NpcEntity): Long

  @Update
  suspend fun updateNpc(npc: NpcEntity)

  @Query("SELECT * FROM npcs WHERE storyId = :storyId ORDER BY firstMetDay ASC, id ASC")
  suspend fun getNpcs(storyId: Long): List<NpcEntity>

  @Query("SELECT * FROM npcs WHERE storyId = :storyId ORDER BY firstMetDay ASC, id ASC")
  fun observeNpcs(storyId: Long): Flow<List<NpcEntity>>

  @Query("DELETE FROM npcs WHERE storyId = :storyId")
  suspend fun deleteAllNpcsForStory(storyId: Long)
}
