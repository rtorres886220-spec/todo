package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AnchorDao {

    @Query("SELECT * FROM anchor_items ORDER BY createdAt DESC")
    fun getAllItems(): Flow<List<AnchorItem>>

    @Query("SELECT * FROM anchor_items WHERE status = 'INBOX' AND type = 'task' ORDER BY createdAt DESC")
    fun getInboxItems(): Flow<List<AnchorItem>>

    @Query("SELECT * FROM anchor_items WHERE status = 'TODAY' AND type = 'task' ORDER BY todayOrder ASC, createdAt ASC")
    fun getTodayItems(): Flow<List<AnchorItem>>

    @Query("SELECT * FROM anchor_items WHERE status != 'DONE' AND type = 'task' AND dueDate IS NOT NULL ORDER BY dueDate ASC, dueTime ASC")
    fun getUpcomingItems(): Flow<List<AnchorItem>>

    @Query("SELECT * FROM anchor_items WHERE type = 'note' AND status != 'DONE' ORDER BY createdAt DESC")
    fun getNotes(): Flow<List<AnchorItem>>

    @Query("SELECT * FROM anchor_items WHERE status = 'DONE' ORDER BY completedAt DESC")
    fun getDoneItems(): Flow<List<AnchorItem>>

    @Query("SELECT * FROM anchor_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): AnchorItem?

    @Query("SELECT * FROM anchor_items WHERE status != 'DONE' AND type = 'task' AND dueDate IS NOT NULL AND (dueDate < :currentDate OR (dueDate = :currentDate AND dueTime IS NOT NULL AND dueTime < :currentTime))")
    suspend fun getOverdueItems(currentDate: String, currentTime: String): List<AnchorItem>

    @Query("SELECT * FROM anchor_items WHERE status != 'DONE' AND type = 'task' AND dueDate BETWEEN :startDate AND :endDate ORDER BY dueDate ASC, dueTime ASC")
    fun getItemsForDateRange(startDate: String, endDate: String): Flow<List<AnchorItem>>

    @Query("SELECT * FROM anchor_items WHERE status != 'DONE' AND snoozeCount >= :minSnoozes")
    fun getHabitualSnoozedItems(minSnoozes: Int = 3): Flow<List<AnchorItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: AnchorItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<AnchorItem>): List<Long>

    @Update
    suspend fun update(item: AnchorItem)

    @Delete
    suspend fun delete(item: AnchorItem)

    @Query("DELETE FROM anchor_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM anchor_items WHERE status = 'DONE' AND completedAt >= :sinceTimestamp")
    suspend fun countDoneSince(sinceTimestamp: Long): Int

    @Query("SELECT * FROM anchor_items WHERE status != 'DONE' AND type = 'task'")
    suspend fun getAllActiveTasksSync(): List<AnchorItem>
}
