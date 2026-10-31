package com.example.ai

import android.content.Context
import android.util.Log
import com.example.data.AnchorDao
import com.example.data.AnchorItem
import com.example.data.HabitProfiler
import com.example.notification.AnchorAlarmScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Autonomous, behind-the-scenes cognitive orchestrator.
 * Debounces task mutation events (1500ms) to conserve API quota,
 * enriches tasks with tactical micro-steps, and self-corrects flexible schedules
 * around non-negotiable locked commitments without altering the UI.
 */
class StrategicOrchestrator(
    private val dao: AnchorDao,
    private val habitProfiler: HabitProfiler,
    private val geminiService: GeminiService,
    private val appContext: Context,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    companion object {
        private const val TAG = "StrategicOrchestrator"
        private const val DEBOUNCE_MS = 1500L
    }

    private var debounceJob: Job? = null
    private val mutex = Mutex()
    private var lastEvaluatedHash: Int = 0

    fun onTaskMutated() {
        debounceJob?.cancel()
        debounceJob = externalScope.launch {
            delay(DEBOUNCE_MS)
            executeAutonomousOptimization()
        }
    }

    suspend fun executeAutonomousOptimization() = mutex.withLock {
        try {
            val activeTasks = dao.getAllActiveTasksSync()
            if (activeTasks.isEmpty()) return@withLock

            // Quota Guard: compute lightweight hash of active tasks state
            val currentHash = activeTasks.map { "${it.id}:${it.status}:${it.snoozeCount}:${it.dueDate}:${it.dueTime}" }.hashCode()
            if (currentHash == lastEvaluatedHash) {
                return@withLock
            }
            lastEvaluatedHash = currentHash

            val profile = habitProfiler.getProfile()

            // 1. Enrich any task lacking micro-steps with strategic breakdown
            val pendingBreakdown = activeTasks.filter { it.aiBreakdownSteps.isNullOrBlank() }.take(2)
            for (task in pendingBreakdown) {
                val rawSteps = geminiService.enrichTaskWithStrategicMicroSteps(task, profile)
                val formatted = rawSteps.joinToString("\n") { "[ ] $it" }
                dao.updateBreakdownSteps(task.id, formatted)
            }

            // 2. Evaluate autonomous rebalance for today's tasks
            val todayTasks = dao.getTodayItemsSync()
            if (todayTasks.isNotEmpty()) {
                val adjustments = geminiService.evaluateAutonomousRebalance(todayTasks, profile)
                for (adj in adjustments) {
                    val target = dao.getItemById(adj.taskId)
                    // Strict safety constraint: NEVER adjust non-negotiable locked commitments
                    if (target != null && !target.isLockedSchedule && target.status != AnchorItem.STATUS_DONE) {
                        val updated = target.copy(
                            dueTime = adj.suggestedTime,
                            estimatedMinutes = adj.calibratedDuration
                        )
                        dao.update(updated)
                        if (updated.dueDate != null) {
                            AnchorAlarmScheduler.scheduleTaskReminder(appContext, updated)
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Autonomous optimization encountered error: ${e.message}")
        }
    }
}
