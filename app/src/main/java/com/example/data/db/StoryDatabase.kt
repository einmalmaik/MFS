package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.CheckpointEntity
import com.example.data.model.MemoryEntity
import com.example.data.model.MessageEntity
import com.example.data.model.NpcEntity
import com.example.data.model.StoryEntity

@Database(
  entities = [
    StoryEntity::class,
    CheckpointEntity::class,
    MessageEntity::class,
    MemoryEntity::class,
    NpcEntity::class
  ],
  version = 10,
  exportSchema = false
)
abstract class StoryDatabase : RoomDatabase() {
  abstract fun storyDao(): StoryDao

  companion object {
    @Volatile
    private var INSTANCE: StoryDatabase? = null

    private val MIGRATION_4_5 = object : Migration(4, 5) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE stories ADD COLUMN playTimeSeconds INTEGER NOT NULL DEFAULT 0")
      }
    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE messages ADD COLUMN embeddingJson TEXT")
      }
    }

    private val MIGRATION_6_7 = object : Migration(6, 7) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE stories ADD COLUMN selectedEmbeddingModel TEXT NOT NULL DEFAULT 'text-embedding-004'")
      }
    }

    private val MIGRATION_7_8 = object : Migration(7, 8) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE stories ADD COLUMN selectedTranscriptionModel TEXT NOT NULL DEFAULT 'gemini-2.5-flash'")
      }
    }

    /**
     * Führt das episodische Gedächtnis (`memories`) und die kanonischen Figuren (`npcs`) ein.
     *
     * Legt ausschließlich Tabellen und Indices an — reines SQL, damit das Öffnen der Datenbank
     * schnell bleibt. Die Übernahme vorhandener `messages.embeddingJson` in `memories` erfolgt
     * bewusst NICHT hier, sondern einmalig im Hintergrund über `MemoryEngine.ensureStoryIndexed`.
     */
    private val MIGRATION_8_9 = object : Migration(8, 9) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `memories` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `storyId` INTEGER NOT NULL,
            `kind` TEXT NOT NULL,
            `dayNumber` INTEGER NOT NULL,
            `inGameTime` TEXT NOT NULL,
            `turnNumber` INTEGER NOT NULL,
            `npcId` INTEGER,
            `text` TEXT NOT NULL,
            `embedding` BLOB,
            `createdAt` INTEGER NOT NULL
          )
          """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_memories_storyId` ON `memories` (`storyId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_memories_npcId` ON `memories` (`npcId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_memories_dayNumber` ON `memories` (`dayNumber`)")

        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `npcs` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `storyId` INTEGER NOT NULL,
            `canonicalName` TEXT NOT NULL,
            `aliasesJson` TEXT NOT NULL,
            `gender` TEXT NOT NULL,
            `appearance` TEXT NOT NULL,
            `personality` TEXT NOT NULL,
            `firstMetDay` INTEGER NOT NULL,
            `lastSeenDay` INTEGER NOT NULL,
            `relationship` TEXT NOT NULL,
            `currentMood` TEXT NOT NULL,
            `outfit` TEXT NOT NULL,
            `status` TEXT NOT NULL,
            `isAlive` INTEGER NOT NULL,
            `updatedAt` INTEGER NOT NULL
          )
          """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_npcs_storyId` ON `npcs` (`storyId`)")

        // Nachgezogene Indices für die bestehenden Tabellen: bisher lief jede Abfrage
        // über storyId als vollständiger Tabellenscan.
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_storyId` ON `messages` (`storyId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_checkpoints_storyId` ON `checkpoints` (`storyId`)")
      }
    }

    /**
     * Gibt Figuren eine eigene körperliche Verfassung.
     *
     * Bis hierher hatte nur der Spieler ein Zustandsfeld. Bei geteiltem Mangel — Hunger, Kälte,
     * schlechte Luft — stand deshalb beim Spieler „unterernährt", während die Figur daneben
     * scheinbar unversehrt blieb. Reines Hinzufügen einer Spalte mit Vorgabewert: bestehende
     * Figuren behalten alles, nichts wird neu geschrieben.
     */
    private val MIGRATION_9_10 = object : Migration(9, 10) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE npcs ADD COLUMN condition TEXT NOT NULL DEFAULT ''")
      }
    }

    fun getInstance(context: Context): StoryDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          StoryDatabase::class.java,
          "storyforge_database"
        )
          .addMigrations(
            MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8,
            MIGRATION_8_9, MIGRATION_9_10
          )
          // Kein pauschales fallbackToDestructiveMigration: das würde bei einer fehlenden
          // Migration sämtliche Spielstände löschen (CLAUDE.md §0.1, §4). Verworfen wird nur,
          // was aus der Zeit vor der ersten Migration stammt und ohnehin keinen Pfad hat.
          .fallbackToDestructiveMigrationFrom(true, 1, 2, 3)
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
