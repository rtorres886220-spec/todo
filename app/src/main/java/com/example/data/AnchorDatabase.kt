package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AnchorItem::class],
    version = 1,
    exportSchema = false
)
abstract class AnchorDatabase : RoomDatabase() {
    abstract fun anchorDao(): AnchorDao

    companion object {
        @Volatile
        private var INSTANCE: AnchorDatabase? = null

        fun getDatabase(context: Context): AnchorDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AnchorDatabase::class.java,
                    "anchor_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
