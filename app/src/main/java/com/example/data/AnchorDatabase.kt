package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [AnchorItem::class, HabitMetric::class],
    version = 3,
    exportSchema = false
)
abstract class AnchorDatabase : RoomDatabase() {
    abstract fun anchorDao(): AnchorDao
    abstract fun habitDao(): HabitDao

    companion object {
        @Volatile
        private var INSTANCE: AnchorDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE anchor_items ADD COLUMN parentId INTEGER DEFAULT NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_anchor_items_parentId ON anchor_items(parentId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_anchor_items_status ON anchor_items(status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_anchor_items_dueDate ON anchor_items(dueDate)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE anchor_items ADD COLUMN isLockedSchedule INTEGER NOT NULL DEFAULT 0")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS habit_metrics (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        itemId INTEGER NOT NULL,
                        actionType TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        hourOfDay INTEGER NOT NULL,
                        dayOfWeek INTEGER NOT NULL,
                        estimatedMinutes INTEGER NOT NULL,
                        actualMinutes INTEGER,
                        snoozeCountAtAction INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_habit_metrics_actionType ON habit_metrics(actionType)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_habit_metrics_timestamp ON habit_metrics(timestamp)")
            }
        }

        fun getDatabase(context: Context): AnchorDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AnchorDatabase::class.java,
                    "anchor_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration(false)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
