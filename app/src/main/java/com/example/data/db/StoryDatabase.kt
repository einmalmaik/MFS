package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.CheckpointEntity
import com.example.data.model.MessageEntity
import com.example.data.model.StoryEntity

@Database(
  entities = [StoryEntity::class, CheckpointEntity::class, MessageEntity::class],
  version = 4,
  exportSchema = false
)
abstract class StoryDatabase : RoomDatabase() {
  abstract fun storyDao(): StoryDao

  companion object {
    @Volatile
    private var INSTANCE: StoryDatabase? = null

    fun getInstance(context: Context): StoryDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          StoryDatabase::class.java,
          "storyforge_database"
        )
          .fallbackToDestructiveMigration(true)
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
