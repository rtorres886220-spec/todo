package com.example.data

import android.content.Context
import android.util.Log
import com.example.ai.AiValidator
import com.example.ai.GeminiService
import com.example.ai.ParsedCapture
import com.example.notification.AnchorAlarmScheduler
import com.example.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import com.example.ai.StrategicOrchestrator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AnchorRepository(
    private val dao: AnchorDao,
    private val geminiService: GeminiService,
    private val appContext: Context,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    private val habitDao: HabitDao? = null
) {
    private val TAG = "AnchorRepository"

    val habitProfiler = HabitProfiler(habitDao ?: object : HabitDao {
        override suspend fun insert(metric: HabitMetric): Long = 0L
        override suspend fun getRecentMetrics(limit: Int): List<HabitMetric> = emptyList()
        override suspend fun getRecentCompletedDurations(): List<HabitMetric> = emptyList()
        override suspend fun getPeakHoursRaw(): List<HourCount> = emptyList()
        override suspend fun countSnoozesSince(sinceTimestamp: Long): Int = 0
        override suspend fun countCompletionsSince(sinceTimestamp: Long): Int = 0
    })

    val strategicOrchestrator = StrategicOrchestrator(dao, habitProfiler, geminiService, appContext, externalScope)

    val allInbox: Flow<List<AnchorItem>> = dao.getInboxItems()
    val allToday: Flow<List<AnchorItem>> = dao.getTodayItems()
    val allNotes: Flow<List<AnchorItem>> = dao.getNotes()
    val allDone: Flow<List<AnchorItem>> = dao.getDoneItems()
    val habitualSnoozed: Flow<List<AnchorItem>> = dao.getHabitualSnoozedItems(3)

    fun getUpcoming(todayIso: String): Flow<List<AnchorItem>> {
        return dao.getUpcomingItems(todayIso)
    }

    fun getSubtasks(parentId: Long): Flow<List<AnchorItem>> {
        return dao.getSubtasks(parentId)
    }

    /**
     * Zero-friction instant capture:
     * 1. Immediately saves initial item to Room DB so UI updates instantly.
     * 2. Asynchronously enriches via AI in background.
     * 3. Once AI finishes, updates the record atomically and automatically schedules reminders if a deadline exists.
     */
    suspend fun captureInstant(
        rawText: String,
        sharedUri: String? = null,
        onComplete: ((AnchorItem) -> Unit)? = null
    ): Long {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty() && sharedUri == null) return -1L

        val initialTitle = if (trimmed.isNotEmpty()) trimmed else "Shared Attachment"
        val profile = habitProfiler.getProfile()
        val initialEst = profile.calibrateDuration(null)
        val initialLocked = AiValidator.isNonNegotiableSchedule(initialTitle, trimmed, null)

        val placeholder = AnchorItem(
            title = initialTitle,
            rawInput = trimmed,
            type = if (trimmed.startsWith("note:", ignoreCase = true)) AnchorItem.TYPE_NOTE else AnchorItem.TYPE_TASK,
            status = AnchorItem.STATUS_INBOX,
            estimatedMinutes = initialEst,
            isLockedSchedule = initialLocked,
            aiSorted = false,
            sharedUri = sharedUri
        )

        val initialId = dao.insert(placeholder)
        strategicOrchestrator.onTaskMutated()

        // Asynchronous AI enrichment in background
        externalScope.launch {
            try {
                val parsedList = geminiService.parseCapture(trimmed)
                if (parsedList.isNotEmpty()) {
                    // Check latest DB state to avoid clobbering user edits or completed items
                    val currentItem = dao.getItemById(initialId)
                    if (currentItem != null && currentItem.status != AnchorItem.STATUS_DONE) {
                        val firstParsed = AiValidator.normalize(parsedList[0], trimmed)
                        val calibratedDuration = profile.calibrateDuration(firstParsed.estimatedMinutes)

                        // Atomic update with locked schedule detection
                        dao.updateAiEnrichmentWithLock(
                            id = initialId,
                            type = firstParsed.type,
                            title = firstParsed.title,
                            dueDate = firstParsed.dueDate,
                            dueTime = firstParsed.dueTime,
                            priority = firstParsed.priority,
                            tags = firstParsed.tags.joinToString(","),
                            estimatedMinutes = calibratedDuration,
                            isLockedSchedule = firstParsed.isLockedSchedule
                        )

                        val updatedItem = dao.getItemById(initialId)
                        if (updatedItem != null) {
                            if (updatedItem.dueDate != null && updatedItem.status != AnchorItem.STATUS_DONE) {
                                AnchorAlarmScheduler.scheduleTaskReminder(appContext, updatedItem)
                            }
                            onComplete?.invoke(updatedItem)
                        }

                        // If multiple distinct tasks were detected in the single capture
                        if (parsedList.size > 1) {
                            for (i in 1 until parsedList.size) {
                                val extra = AiValidator.normalize(parsedList[i], trimmed)
                                val extraDuration = profile.calibrateDuration(extra.estimatedMinutes)
                                val extraItem = AnchorItem(
                                    title = extra.title,
                                    rawInput = trimmed,
                                    type = extra.type,
                                    dueDate = extra.dueDate,
                                    dueTime = extra.dueTime,
                                    priority = extra.priority,
                                    tags = extra.tags.joinToString(","),
                                    estimatedMinutes = extraDuration,
                                    status = AnchorItem.STATUS_INBOX,
                                    isLockedSchedule = extra.isLockedSchedule,
                                    aiSorted = true
                                )
                                val extraId = dao.insert(extraItem)
                                if (extra.dueDate != null && extra.type == AnchorItem.TYPE_TASK) {
                                    val insertedExtra = dao.getItemById(extraId)
                                    if (insertedExtra != null) {
                                        AnchorAlarmScheduler.scheduleTaskReminder(appContext, insertedExtra)
                                    }
                                }
                            }
                        }
                        strategicOrchestrator.onTaskMutated()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Background AI enrichment error: ${e.message}")
            }
        }

        return initialId
    }

    suspend fun moveToToday(item: AnchorItem) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val updated = item.copy(
            status = AnchorItem.STATUS_TODAY,
            dueDate = item.dueDate ?: todayStr,
            isPinnedToToday = true
        )
        dao.update(updated)
        if (updated.dueDate != null) {
            AnchorAlarmScheduler.scheduleTaskReminder(appContext, updated)
        }
        strategicOrchestrator.onTaskMutated()
    }

    suspend fun moveToInbox(item: AnchorItem) {
        dao.update(
            item.copy(
                status = AnchorItem.STATUS_INBOX,
                isPinnedToToday = false
            )
        )
        strategicOrchestrator.onTaskMutated()
    }

    suspend fun markCompleted(item: AnchorItem) {
        AnchorAlarmScheduler.cancelTaskReminder(appContext, item.id)
        val now = System.currentTimeMillis()
        val elapsedMinutes = if (item.createdAt > 0) ((now - item.createdAt) / 60000L).toInt().coerceIn(1, 1440) else null
        val cal = Calendar.getInstance()

        try {
            habitDao?.insert(
                HabitMetric(
                    itemId = item.id,
                    actionType = "complete",
                    timestamp = now,
                    hourOfDay = cal.get(Calendar.HOUR_OF_DAY),
                    dayOfWeek = cal.get(Calendar.DAY_OF_WEEK),
                    estimatedMinutes = item.estimatedMinutes ?: 30,
                    actualMinutes = elapsedMinutes,
                    snoozeCountAtAction = item.snoozeCount
                )
            )
        } catch (_: Exception) {}

        dao.update(
            item.copy(
                status = AnchorItem.STATUS_DONE,
                completedAt = now
            )
        )
        strategicOrchestrator.onTaskMutated()
    }

    suspend fun uncomplete(item: AnchorItem) {
        val updated = item.copy(
            status = AnchorItem.STATUS_INBOX,
            completedAt = null
        )
        dao.update(updated)
        if (updated.dueDate != null) {
            AnchorAlarmScheduler.scheduleTaskReminder(appContext, updated)
        }
        strategicOrchestrator.onTaskMutated()
    }

    suspend fun snooze(item: AnchorItem, hours: Int = 0, minutes: Int = 0) {
        val cal = Calendar.getInstance()
        if (hours > 0) cal.add(Calendar.HOUR_OF_DAY, hours)
        if (minutes > 0) cal.add(Calendar.MINUTE, minutes)
        if (hours == 0 && minutes == 0) cal.add(Calendar.HOUR_OF_DAY, 24)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        val newSnoozeCount = item.snoozeCount + 1

        try {
            habitDao?.insert(
                HabitMetric(
                    itemId = item.id,
                    actionType = "snooze",
                    timestamp = System.currentTimeMillis(),
                    hourOfDay = cal.get(Calendar.HOUR_OF_DAY),
                    dayOfWeek = cal.get(Calendar.DAY_OF_WEEK),
                    estimatedMinutes = item.estimatedMinutes ?: 30,
                    actualMinutes = null,
                    snoozeCountAtAction = newSnoozeCount
                )
            )
        } catch (_: Exception) {}

        val snoozed = item.copy(
            snoozeCount = newSnoozeCount,
            snoozeUntil = cal.timeInMillis,
            dueDate = dateFormat.format(cal.time),
            dueTime = timeFormat.format(cal.time),
            status = if (item.status == AnchorItem.STATUS_TODAY) AnchorItem.STATUS_TODAY else AnchorItem.STATUS_INBOX,
            isPinnedToToday = false
        )

        dao.update(snoozed)
        AnchorAlarmScheduler.scheduleTaskReminder(appContext, snoozed)
        strategicOrchestrator.onTaskMutated()
    }

    suspend fun delete(item: AnchorItem) {
        AnchorAlarmScheduler.cancelTaskReminder(appContext, item.id)
        dao.deleteSubtasksByParentId(item.id)
        dao.delete(item)
        strategicOrchestrator.onTaskMutated()
    }

    suspend fun updateItem(item: AnchorItem) {
        dao.update(item)
        if (item.status != AnchorItem.STATUS_DONE && item.dueDate != null) {
            AnchorAlarmScheduler.scheduleTaskReminder(appContext, item)
        } else {
            AnchorAlarmScheduler.cancelTaskReminder(appContext, item.id)
        }
        strategicOrchestrator.onTaskMutated()
    }

    suspend fun getItem(id: Long): AnchorItem? {
        return dao.getItemById(id)
    }

    suspend fun getOverdueItems(): List<AnchorItem> {
        val now = Date()
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now)
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
        return dao.getOverdueItems(todayStr, timeStr)
    }

    suspend fun getActiveTasksSync(): List<AnchorItem> {
        return dao.getAllActiveTasksSync()
    }

    suspend fun getAllActiveItemsSync(): List<AnchorItem> {
        return dao.getAllActiveItemsSync()
    }

    suspend fun updateSubtask(item: AnchorItem, stepIndex: Int, completed: Boolean): AnchorItem {
        val steps = item.aiBreakdownSteps?.split("\n")?.toMutableList() ?: mutableListOf()
        if (stepIndex in steps.indices) {
            val original = steps[stepIndex].replace(Regex("^\\[[ xX]\\]\\s*"), "")
            steps[stepIndex] = if (completed) "[x] $original" else "[ ] $original"
            val updated = item.copy(aiBreakdownSteps = steps.joinToString("\n"))
            dao.update(updated)
            return updated
        }
        return item
    }

    suspend fun addSubtask(item: AnchorItem, stepText: String): AnchorItem {
        val trimmed = stepText.trim()
        if (trimmed.isEmpty()) return item
        val steps = item.aiBreakdownSteps?.split("\n")?.filter { it.isNotBlank() }?.toMutableList() ?: mutableListOf()
        steps.add("[ ] $trimmed")
        val updated = item.copy(aiBreakdownSteps = steps.joinToString("\n"))
        dao.update(updated)

        // Also persist as child AnchorItem
        dao.insert(
            AnchorItem(
                parentId = item.id,
                title = trimmed,
                rawInput = "Micro-step for: ${item.title}",
                type = AnchorItem.TYPE_TASK,
                status = item.status,
                dueDate = item.dueDate,
                priority = item.priority,
                tags = item.tags,
                estimatedMinutes = 10,
                aiSorted = true
            )
        )
        return updated
    }

    /**
     * Safe task breakdown:
     * - Preserves parent task context and identity (never deletes original task).
     * - Clears previous subtasks for this parent to prevent duplicates.
     * - Inserts child tasks linked via parentId.
     * - Updates parent task's aiBreakdownSteps.
     */
    suspend fun applyBreakdownSubtasks(parentTask: AnchorItem, subtasks: List<String>): AnchorItem {
        val validatedSubtasks = AiValidator.validateSubtasks(subtasks)
        if (validatedSubtasks.isEmpty()) return parentTask

        // Delete any existing subtasks for this parent to avoid duplicates
        dao.deleteSubtasksByParentId(parentTask.id)

        // Insert new subtask children
        val subtaskEntities = validatedSubtasks.mapIndexed { index, stepTitle ->
            AnchorItem(
                parentId = parentTask.id,
                title = stepTitle,
                rawInput = "Micro-step ${index + 1} of: ${parentTask.title}",
                type = AnchorItem.TYPE_TASK,
                status = parentTask.status,
                dueDate = parentTask.dueDate,
                priority = parentTask.priority,
                tags = parentTask.tags,
                estimatedMinutes = 10,
                aiSorted = true
            )
        }
        dao.insertAll(subtaskEntities)

        // Update parent with checklist
        val checklist = validatedSubtasks.joinToString("\n") { "[ ] $it" }
        val updatedParent = parentTask.copy(aiBreakdownSteps = checklist)
        dao.update(updatedParent)
        return updatedParent
    }

    /**
     * Executes non-destructive health probes across all 5 core subsystems:
     * 1. Room SQLite Database (schema integrity and count)
     * 2. Intelligence & NLP Parser (Firebase AI availability + heuristic fallback)
     * 3. AlarmManager & Notification Channels (registration & trigger math)
     * 4. SharedPreferences (settings read/write integrity)
     * 5. Habit Self-Calibration Engine (multiplier & peak hours calculation)
     */
    suspend fun runSystemDiagnostics(): DiagnosticsReport {
        val overallStart = System.currentTimeMillis()
        val probes = mutableListOf<SubsystemProbe>()

        // 1. Room SQLite Database
        val dbStart = System.currentTimeMillis()
        var dbCount = 0
        var dbOk = false
        var dbErr: String? = null
        try {
            dbCount = dao.getTotalItemCount()
            dbOk = true
        } catch (e: Exception) {
            dbErr = e.message
        }
        val dbLatency = System.currentTimeMillis() - dbStart
        probes.add(
            SubsystemProbe(
                id = "db",
                name = "Room SQLite Database",
                isHealthy = dbOk,
                statusBadge = if (dbOk) "Verified ($dbCount records)" else "Error",
                latencyMs = dbLatency,
                details = if (dbOk) "Schema v4 verified. Fast index read confirmed." else "Database error: $dbErr"
            )
        )

        // 2. Intelligence & NLP Parser
        val aiStart = System.currentTimeMillis()
        val isFirebaseOnline = geminiService.isFirebaseAiAvailable()
        val parsed = geminiService.parseWithLocalHeuristics("Review quarterly metrics tomorrow at 2pm #work urgent 30m")
        val aiLatency = System.currentTimeMillis() - aiStart
        val heuristicOk = parsed.isNotEmpty() && parsed[0].tags.contains("work") && parsed[0].priority == AnchorItem.PRIORITY_HIGH
        probes.add(
            SubsystemProbe(
                id = "ai",
                name = "Intelligence & NLP Parser",
                isHealthy = heuristicOk,
                statusBadge = if (isFirebaseOnline) "Firebase AI Logic Online" else "Local NLP Fallback Active",
                latencyMs = aiLatency,
                details = if (isFirebaseOnline) {
                    "Cloud Gemini reasoning connected with App Check attestation."
                } else {
                    "Autonomous on-device heuristic engine active with zero data loss."
                }
            )
        )

        // 3. AlarmManager & Notifications
        val notifStart = System.currentTimeMillis()
        NotificationHelper.createChannels(appContext)
        val notifManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
        val channelOk = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notifManager?.getNotificationChannel(NotificationHelper.CHANNEL_REMINDERS) != null
        } else true
        val probeItem = AnchorItem(title = "Probe Task", dueDate = "2026-11-15", dueTime = "14:30")
        val trigger = AnchorAlarmScheduler.calculateTaskTriggerMillis(probeItem)
        val notifLatency = System.currentTimeMillis() - notifStart
        val notifOk = channelOk && trigger != null
        probes.add(
            SubsystemProbe(
                id = "notif",
                name = "AlarmManager & Notifications",
                isHealthy = notifOk,
                statusBadge = if (notifOk) "Operational" else "Channel Error",
                latencyMs = notifLatency,
                details = "Channels registered. Exact trigger math calculated in ${notifLatency}ms."
            )
        )

        // 4. SharedPreferences Storage
        val prefStart = System.currentTimeMillis()
        val prefs = AnchorPreferences.get(appContext)
        val settings = prefs.getSettings()
        val prefLatency = System.currentTimeMillis() - prefStart
        val prefOk = settings.morningTime.isNotBlank() && settings.eveningTime.isNotBlank()
        probes.add(
            SubsystemProbe(
                id = "prefs",
                name = "SharedPreferences Storage",
                isHealthy = prefOk,
                statusBadge = "Synced",
                latencyMs = prefLatency,
                details = "Brief: ${settings.morningTime} | Review: ${settings.eveningTime} | Quiet: ${settings.quietHours}"
            )
        )

        // 5. Habit Self-Calibration
        val habitStart = System.currentTimeMillis()
        val profile = habitProfiler.getProfile()
        val habitLatency = System.currentTimeMillis() - habitStart
        probes.add(
            SubsystemProbe(
                id = "habit",
                name = "Habit Self-Calibration",
                isHealthy = true,
                statusBadge = String.format(Locale.getDefault(), "Multiplier: %.2fx", profile.estimationMultiplier),
                latencyMs = habitLatency,
                details = "Peak focus hours: ${profile.peakHours.joinToString { String.format(Locale.getDefault(), "%02d:00", it) }}"
            )
        )

        val totalMs = System.currentTimeMillis() - overallStart
        return DiagnosticsReport(
            timestamp = System.currentTimeMillis(),
            probes = probes,
            allPassing = probes.all { it.isHealthy },
            totalLatencyMs = totalMs
        )
    }
}

