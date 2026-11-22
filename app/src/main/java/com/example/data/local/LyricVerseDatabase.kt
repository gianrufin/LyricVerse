package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LyricsCacheEntity::class], version = 1, exportSchema = false)
abstract class LyricVerseDatabase : RoomDatabase() {
    abstract fun lyricsCacheDao(): LyricsCacheDao

    companion object {
        @Volatile
        private var INSTANCE: LyricVerseDatabase? = null

        fun getInstance(context: Context): LyricVerseDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LyricVerseDatabase::class.java,
                    "lyricverse_cache.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
