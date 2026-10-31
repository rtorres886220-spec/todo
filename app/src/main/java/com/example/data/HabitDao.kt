package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

data class HourCount(
    val hourOfDay: Int,
    val count: Int
)

@Dao
interface HabitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metric: HabitMetric): Long

    @Query("SELECT * FROM habit_metrics ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMetrics(limit: Int = 50): List<HabitMetric>

    @Query("SELECT * FROM habit_metrics WHERE actionType = 'complete' AND actualMinutes IS NOT NULL ORDER BY timestamp DESC LIMIT 30")
    suspend fun getRecentCompletedDurations(): List<HabitMetric>

    @Query("SELECT hourOfDay, COUNT(*) as count FROM habit_metrics WHERE actionType = 'complete' GROUP BY hourOfDay ORDER BY count DESC LIMIT 3")
    suspend fun getPeakHoursRaw(): List<HourCount>

    @Query("SELECT COUNT(*) FROM habit_metrics WHERE actionType = 'snooze' AND timestamp >= :sinceTimestamp")
    suspend fun countSnoozesSince(sinceTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM habit_metrics WHERE actionType = 'complete' AND timestamp >= :sinceTimestamp")
    suspend fun countCompletionsSince(sinceTimestamp: Long): Int
}
