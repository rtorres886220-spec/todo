package com.example.data

import android.content.Context
import com.example.ai.GeminiService
import com.example.ai.ParsedCapture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AnchorRepository(
    private val dao: AnchorDao,
    private val geminiService: GeminiService,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    val allInbox: Flow<List<AnchorItem>> = dao.getInboxItems()
    val allToday: Flow<List<AnchorItem>> = dao.getTodayItems()
    val allUpcoming: Flow<List<AnchorItem>> = dao.getUpcomingItems()
    val allNotes: Flow<List<AnchorItem>> = dao.getNotes()
    val allDone: Flow<List<AnchorItem>> = dao.getDoneItems()
    val habitualSnoozed: Flow<List<AnchorItem>> = dao.getHabitualSnoozedItems(3)

    /**
     * Zero-friction instant capture:
     * 1. Immediately saves initial item to Room DB so UI updates in 0 milliseconds!
     * 2. In background, invokes Gemini to parse, split, and enrich.
     * 3. Updates the item silently and marks aiSorted = true.
     */
    suspend fun captureInstant(rawText: String, sharedUri: String? = null): Long {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty() && sharedUri == null) return -1L

        val initialTitle = if (trimmed.isNotEmpty()) trimmed else "Shared Attachment"
        val placeholder = AnchorItem(
            title = initialTitle,
            rawInput = trimmed,
            type = if (trimmed.startsWith("note:", ignoreCase = true)) AnchorItem.TYPE_NOTE else AnchorItem.TYPE_TASK,
            status = AnchorItem.STATUS_INBOX,
            aiSorted = false,
            sharedUri = sharedUri
        )

        val initialId = dao.insert(placeholder)

        // Asynchronous AI enrichment in background
        externalScope.launch {
            try {
                val parsedList = geminiService.parseCapture(trimmed)
                if (parsedList.isNotEmpty()) {
                    if (parsedList.size == 1) {
                        val parsed = parsedList[0]
                        val updated = placeholder.copy(
                            id = initialId,
                            title = parsed.title,
                            type = parsed.type,
                            dueDate = parsed.dueDate,
                            dueTime = parsed.dueTime,
                            priority = parsed.priority,
                            tags = parsed.tags.joinToString(","),
                            estimatedMinutes = parsed.estimatedMinutes,
                            aiSorted = true
                        )
                        dao.update(updated)
                    } else {
                        // Multiple items detected in single capture: update first, insert rest
                        val first = parsedList[0]
                        dao.update(
                            placeholder.copy(
                                id = initialId,
                                title = first.title,
                                type = first.type,
                                dueDate = first.dueDate,
                                dueTime = first.dueTime,
                                priority = first.priority,
                                tags = first.tags.joinToString(","),
                                estimatedMinutes = first.estimatedMinutes,
                                aiSorted = true
                            )
                        )
                        for (i in 1 until parsedList.size) {
                            val extra = parsedList[i]
                            dao.insert(
                                AnchorItem(
                                    title = extra.title,
                                    rawInput = trimmed,
                                    type = extra.type,
                                    dueDate = extra.dueDate,
                                    dueTime = extra.dueTime,
                                    priority = extra.priority,
                                    tags = extra.tags.joinToString(","),
                                    estimatedMinutes = extra.estimatedMinutes,
                                    status = AnchorItem.STATUS_INBOX,
                                    aiSorted = true
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // If anything fails in background, item remains safe in Room
            }
        }

        return initialId
    }

    suspend fun moveToToday(item: AnchorItem) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        dao.update(
            item.copy(
                status = AnchorItem.STATUS_TODAY,
                dueDate = item.dueDate ?: todayStr,
                isPinnedToToday = true
            )
        )
    }

    suspend fun moveToInbox(item: AnchorItem) {
        dao.update(
            item.copy(
                status = AnchorItem.STATUS_INBOX,
                isPinnedToToday = false
            )
        )
    }

    suspend fun markCompleted(item: AnchorItem) {
        dao.update(
            item.copy(
                status = AnchorItem.STATUS_DONE,
                completedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun uncomplete(item: AnchorItem) {
        dao.update(
            item.copy(
                status = AnchorItem.STATUS_INBOX,
                completedAt = null
            )
        )
    }

    suspend fun snooze(item: AnchorItem, hours: Int = 24) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.HOUR_OF_DAY, hours)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        val newSnoozeCount = item.snoozeCount + 1
        var breakdownSteps = item.aiBreakdownSteps

        // If snoozed 3 or more times, proactively trigger Gemini Snooze Coach!
        if (newSnoozeCount >= 3 && breakdownSteps == null) {
            externalScope.launch {
                try {
                    val breakdown = geminiService.breakdownSnoozedTask(item.title, newSnoozeCount)
                    dao.update(
                        item.copy(
                            snoozeCount = newSnoozeCount,
                            snoozeUntil = cal.timeInMillis,
                            dueDate = dateFormat.format(cal.time),
                            dueTime = timeFormat.format(cal.time),
                            status = AnchorItem.STATUS_INBOX,
                            isPinnedToToday = false,
                            aiBreakdownSteps = breakdown.subtasks.joinToString("\n")
                        )
                    )
                } catch (e: Exception) {
                    // ignore
                }
            }
        }

        dao.update(
            item.copy(
                snoozeCount = newSnoozeCount,
                snoozeUntil = cal.timeInMillis,
                dueDate = dateFormat.format(cal.time),
                dueTime = timeFormat.format(cal.time),
                status = AnchorItem.STATUS_INBOX,
                isPinnedToToday = false
            )
        )
    }

    suspend fun delete(item: AnchorItem) {
        dao.delete(item)
    }

    suspend fun updateItem(item: AnchorItem) {
        dao.update(item)
    }

    suspend fun getItem(id: Long): AnchorItem? {
        return dao.getItemById(id)
    }

    suspend fun getOverdueItems(): List<AnchorItem> {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return dao.getOverdueItems(todayStr)
    }

    suspend fun getActiveTasksSync(): List<AnchorItem> {
        return dao.getAllActiveTasksSync()
    }
}
