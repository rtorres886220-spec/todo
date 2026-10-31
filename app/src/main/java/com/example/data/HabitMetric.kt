package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habit_metrics",
    indices = [
        Index(value = ["actionType"]),
        Index(value = ["timestamp"])
    ]
)
data class HabitMetric(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemId: Long,
    val actionType: String, // "complete" | "snooze" | "create"
    val timestamp: Long = System.currentTimeMillis(),
    val hourOfDay: Int,
    val dayOfWeek: Int,
    val estimatedMinutes: Int,
    val actualMinutes: Int? = null,
    val snoozeCountAtAction: Int = 0
)
