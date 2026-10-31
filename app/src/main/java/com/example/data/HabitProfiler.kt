package com.example.data

import kotlin.math.roundToInt

data class HabitProfile(
    val estimationMultiplier: Float = 1.0f,
    val peakHours: List<Int> = listOf(9, 10, 14),
    val totalCompletionsLogged: Int = 0,
    val recentSnoozeCount: Int = 0
) {
    fun calibrateDuration(estimatedMinutes: Int?): Int {
        val base = estimatedMinutes ?: 30
        return (base * estimationMultiplier).roundToInt().coerceIn(5, 480)
    }
}

class HabitProfiler(private val habitDao: HabitDao) {

    suspend fun getProfile(): HabitProfile {
        return try {
            val completedMetrics = habitDao.getRecentCompletedDurations()
            val multiplier = if (completedMetrics.isNotEmpty()) {
                val ratios = completedMetrics.mapNotNull { metric ->
                    val est = metric.estimatedMinutes
                    val act = metric.actualMinutes
                    if (est > 0 && act != null && act > 0) {
                        act.toFloat() / est.toFloat()
                    } else null
                }
                if (ratios.isNotEmpty()) {
                    ratios.average().toFloat().coerceIn(0.7f, 2.5f)
                } else 1.0f
            } else 1.0f

            val peakHourCounts = habitDao.getPeakHoursRaw()
            val peakHours = if (peakHourCounts.isNotEmpty()) {
                peakHourCounts.map { it.hourOfDay }
            } else listOf(9, 10, 14)

            val weekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
            val completions = habitDao.countCompletionsSince(weekAgo)
            val snoozes = habitDao.countSnoozesSince(weekAgo)

            HabitProfile(
                estimationMultiplier = multiplier,
                peakHours = peakHours,
                totalCompletionsLogged = completions,
                recentSnoozeCount = snoozes
            )
        } catch (_: Exception) {
            HabitProfile()
        }
    }
}
