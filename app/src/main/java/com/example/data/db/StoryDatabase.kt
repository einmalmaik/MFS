package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.CheckpointEntity
import com.example.data.model.MessageEntity
import com.example.data.model.StoryEntity

@Database(
  entities = [StoryEntity::class, CheckpointEntity::class, MessageEntity::class],
  version = 7,
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

    fun getInstance(context: Context): StoryDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          StoryDatabase::class.java,
          "storyforge_database"
        )
          .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
          .fallbackToDestructiveMigration(true)
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
