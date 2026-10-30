package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.EveningReview
import com.example.ai.GeminiService
import com.example.ai.MorningBrief
import com.example.ai.SnoozeBreakdown
import com.example.data.AnchorDatabase
import com.example.data.AnchorItem
import com.example.data.AnchorRepository
import com.example.data.AnchorPreferences
import com.example.notification.AnchorAlarmScheduler
import com.example.notification.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AnchorTab(val label: String) {
    INBOX("Inbox"),
    TODAY("Today"),
    UPCOMING("Upcoming"),
    NOTES("Notes"),
    DONE("Done")
}

data class AnchorUiState(
    val activeTab: AnchorTab = AnchorTab.INBOX,
    val clipboardCandidate: String? = null,
    val undoItem: AnchorItem? = null,
    val undoMessage: String? = null,
    val editingItem: AnchorItem? = null,
    val isMorningBriefOpen: Boolean = false,
    val morningBrief: MorningBrief? = null,
    val isEveningReviewOpen: Boolean = false,
    val eveningReview: EveningReview? = null,
    val isSnoozeCoachOpen: Boolean = false,
    val snoozeCoachItem: AnchorItem? = null,
    val snoozeBreakdown: SnoozeBreakdown? = null,
    val isSettingsOpen: Boolean = false,
    val isGeneratingAi: Boolean = false,
    val morningTime: String = "08:00",
    val eveningTime: String = "20:00",
    val quietHours: String = "22:00 - 07:00",
    val escalationIntensity: String = "Standard",
    val deadAppHours: Int = 6
)

class AnchorViewModel(application: Application) : AndroidViewModel(application) {

    private val app: Application = application
    private val db = AnchorDatabase.getDatabase(application)
    private val geminiService = GeminiService()
    private val repository = AnchorRepository(db.anchorDao(), geminiService, viewModelScope)

    private val _uiState = MutableStateFlow(AnchorUiState())
    val uiState: StateFlow<AnchorUiState> = _uiState.asStateFlow()

    val inboxItems = repository.allInbox.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val todayItems = repository.allToday.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val upcomingItems = repository.allUpcoming.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val notes = repository.allNotes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val doneItems = repository.allDone.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val habitualSnoozed = repository.habitualSnoozed.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _overdueItems = MutableStateFlow<List<AnchorItem>>(emptyList())
    val overdueItems: StateFlow<List<AnchorItem>> = _overdueItems.asStateFlow()

    init {
        val prefs = AnchorPreferences.get(app)
        _uiState.update {
            it.copy(
                morningTime = prefs.morningBriefTime,
                eveningTime = prefs.eveningReviewTime,
                quietHours = prefs.quietHours,
                escalationIntensity = prefs.escalationIntensity,
                deadAppHours = prefs.deadAppHours
            )
        }
        AnchorAlarmScheduler.scheduleDailyBriefs(app, prefs.morningBriefTime, prefs.eveningReviewTime)
        refreshOverdue()
        updateLastActive()
    }

    fun setTab(tab: AnchorTab) {
        _uiState.update { it.copy(activeTab = tab) }
        refreshOverdue()
    }

    fun refreshOverdue() {
        viewModelScope.launch {
            _overdueItems.value = repository.getOverdueItems()
        }
    }

    /**
     * Zero-friction capture: instant local Room save, background Gemini sort.
     */
    fun capture(text: String, sharedUri: String? = null) {
        if (text.isBlank() && sharedUri == null) return
        viewModelScope.launch {
            val id = repository.captureInstant(text, sharedUri)
            val created = repository.getItem(id)
            if (created != null && created.dueDate != null) {
                AnchorAlarmScheduler.scheduleTaskReminder(app, created)
            }
            refreshOverdue()
        }
    }

    fun moveToToday(item: AnchorItem) {
        viewModelScope.launch {
            repository.moveToToday(item)
            val updated = repository.getItem(item.id)
            if (updated != null && updated.dueDate != null) {
                AnchorAlarmScheduler.scheduleTaskReminder(app, updated)
            }
            showUndo(item, "Moved to Today")
        }
    }

    fun moveToInbox(item: AnchorItem) {
        viewModelScope.launch {
            repository.moveToInbox(item)
            showUndo(item, "Moved to Inbox")
        }
    }

    fun completeTask(item: AnchorItem) {
        viewModelScope.launch {
            AnchorAlarmScheduler.cancelTaskReminder(app, item.id)
            repository.markCompleted(item)
            refreshOverdue()
            showUndo(item, "Task completed!")
        }
    }

    fun uncompleteTask(item: AnchorItem) {
        viewModelScope.launch {
            repository.uncomplete(item)
            if (item.dueDate != null) {
                AnchorAlarmScheduler.scheduleTaskReminder(app, item)
            }
            showUndo(item, "Restored to Inbox")
        }
    }

    fun snoozeTask(item: AnchorItem, hours: Int = 24) {
        viewModelScope.launch {
            AnchorAlarmScheduler.cancelTaskReminder(app, item.id)
            repository.snooze(item, hours)
            val updated = repository.getItem(item.id)
            if (updated != null && updated.dueDate != null) {
                AnchorAlarmScheduler.scheduleTaskReminder(app, updated)
            }
            refreshOverdue()
            showUndo(item, "Snoozed for ${if (hours == 24) "1 day" else "$hours hours"}")
        }
    }

    fun deleteTask(item: AnchorItem) {
        viewModelScope.launch {
            AnchorAlarmScheduler.cancelTaskReminder(app, item.id)
            repository.delete(item)
            refreshOverdue()
            showUndo(item, "Item deleted")
        }
    }

    fun updateItem(item: AnchorItem) {
        viewModelScope.launch {
            repository.updateItem(item)
            if (item.status != AnchorItem.STATUS_DONE && item.dueDate != null) {
                AnchorAlarmScheduler.scheduleTaskReminder(app, item)
            } else {
                AnchorAlarmScheduler.cancelTaskReminder(app, item.id)
            }
            refreshOverdue()
            _uiState.update { it.copy(editingItem = null) }
        }
    }

    fun toggleSubtask(item: AnchorItem, stepIndex: Int, completed: Boolean, onUpdated: (AnchorItem) -> Unit) {
        viewModelScope.launch {
            val updated = repository.updateSubtask(item, stepIndex, completed)
            onUpdated(updated)
        }
    }

    fun addSubtask(item: AnchorItem, stepText: String, onUpdated: (AnchorItem) -> Unit) {
        viewModelScope.launch {
            val updated = repository.addSubtask(item, stepText)
            onUpdated(updated)
        }
    }

    fun startEditing(item: AnchorItem) {
        _uiState.update { it.copy(editingItem = item) }
    }

    fun dismissEditing() {
        _uiState.update { it.copy(editingItem = null) }
    }

    private fun showUndo(item: AnchorItem, message: String) {
        _uiState.update { it.copy(undoItem = item, undoMessage = message) }
    }

    fun undo() {
        val item = _uiState.value.undoItem ?: return
        viewModelScope.launch {
            repository.updateItem(item)
            refreshOverdue()
            clearUndo()
        }
    }

    fun clearUndo() {
        _uiState.update { it.copy(undoItem = null, undoMessage = null) }
    }

    // Clipboard detection
    fun onClipboardDetected(text: String) {
        if (text.isNotBlank() && _uiState.value.clipboardCandidate != text) {
            _uiState.update { it.copy(clipboardCandidate = text) }
        }
    }

    fun acceptClipboard() {
        val text = _uiState.value.clipboardCandidate ?: return
        capture(text)
        _uiState.update { it.copy(clipboardCandidate = null) }
    }

    fun dismissClipboard() {
        _uiState.update { it.copy(clipboardCandidate = null) }
    }

    // Morning Brief
    fun openMorningBrief() {
        _uiState.update { it.copy(isMorningBriefOpen = true, isGeneratingAi = true) }
        viewModelScope.launch {
            val activeTasks = repository.getActiveTasksSync()
            val brief = geminiService.generateMorningBrief(activeTasks)
            _uiState.update { it.copy(morningBrief = brief, isGeneratingAi = false) }
        }
    }

    fun dismissMorningBrief() {
        _uiState.update { it.copy(isMorningBriefOpen = false) }
    }

    fun acceptMorningBriefTop3(taskIds: List<Long>) {
        viewModelScope.launch {
            taskIds.forEach { id ->
                val item = repository.getItem(id)
                if (item != null) {
                    repository.moveToToday(item)
                }
            }
            _uiState.update { it.copy(isMorningBriefOpen = false) }
            setTab(AnchorTab.TODAY)
        }
    }

    // Evening Review
    fun openEveningReview() {
        _uiState.update { it.copy(isEveningReviewOpen = true, isGeneratingAi = true) }
        viewModelScope.launch {
            val startOfDay = getStartOfDayTimestamp()
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val active = repository.getActiveTasksSync()
            val slipped = active.filter { it.status == AnchorItem.STATUS_TODAY || it.dueDate == todayStr }
            val doneToday = doneItems.value.filter { (it.completedAt ?: 0L) >= startOfDay }

            val review = geminiService.generateEveningReview(doneToday, slipped)
            _uiState.update { it.copy(eveningReview = review, isGeneratingAi = false) }
        }
    }

    fun dismissEveningReview() {
        _uiState.update { it.copy(isEveningReviewOpen = false) }
    }

    fun rescheduleSlippedTasks(taskIds: List<Long>, hours: Int = 24) {
        viewModelScope.launch {
            taskIds.forEach { id ->
                val item = repository.getItem(id)
                if (item != null) {
                    repository.snooze(item, hours)
                }
            }
            dismissEveningReview()
            refreshOverdue()
        }
    }

    // Snooze Coach
    fun openSnoozeCoach(item: AnchorItem) {
        _uiState.update { it.copy(isSnoozeCoachOpen = true, snoozeCoachItem = item, isGeneratingAi = true) }
        viewModelScope.launch {
            val breakdown = geminiService.breakdownSnoozedTask(item.title, item.snoozeCount)
            _uiState.update { it.copy(snoozeBreakdown = breakdown, isGeneratingAi = false) }
        }
    }

    fun dismissSnoozeCoach() {
        _uiState.update { it.copy(isSnoozeCoachOpen = false, snoozeCoachItem = null, snoozeBreakdown = null) }
    }

    fun applySnoozeSubtasks(originalItem: AnchorItem, subtasks: List<String>) {
        viewModelScope.launch {
            // Convert micro-steps into separate actionable subtasks
            subtasks.forEachIndexed { index, stepTitle ->
                repository.captureInstant(
                    rawText = stepTitle
                )
            }
            // Mark original task snoozed with notes or completed
            repository.delete(originalItem)
            dismissSnoozeCoach()
            refreshOverdue()
            showUndo(originalItem, "Broken into ${subtasks.size} friction-free steps!")
        }
    }

    // Settings
    fun openSettings() {
        _uiState.update { it.copy(isSettingsOpen = true) }
    }

    fun dismissSettings() {
        _uiState.update { it.copy(isSettingsOpen = false) }
    }

    fun updateSettings(morning: String, evening: String, quiet: String, intensity: String, deadAppH: Int) {
        val prefs = AnchorPreferences.get(app)
        prefs.morningBriefTime = morning
        prefs.eveningReviewTime = evening
        prefs.quietHours = quiet
        prefs.escalationIntensity = intensity
        prefs.deadAppHours = deadAppH
        AnchorAlarmScheduler.scheduleDailyBriefs(app, morning, evening)

        _uiState.update {
            it.copy(
                morningTime = morning,
                eveningTime = evening,
                quietHours = quiet,
                escalationIntensity = intensity,
                deadAppHours = deadAppH,
                isSettingsOpen = false
            )
        }
    }

    fun loadSampleData() {
        viewModelScope.launch {
            val cal = java.util.Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = dateFormat.format(cal.time)

            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = dateFormat.format(cal.time)

            cal.add(java.util.Calendar.DAY_OF_YEAR, 2)
            val tomorrowStr = dateFormat.format(cal.time)

            cal.add(java.util.Calendar.DAY_OF_YEAR, 4)
            val nextWeekStr = dateFormat.format(cal.time)

            val now = System.currentTimeMillis()

            val sampleItems = listOf(
                // 1. Overdue task (Rose tint - overdue only)
                AnchorItem(
                    title = "Renew enterprise security & domain SSL certificates",
                    rawInput = "Renew enterprise security & domain SSL certificates yesterday at 5pm #security #personal",
                    type = AnchorItem.TYPE_TASK,
                    status = AnchorItem.STATUS_TODAY,
                    dueDate = yesterdayStr,
                    dueTime = "17:00",
                    priority = AnchorItem.PRIORITY_HIGH,
                    tags = "security,personal",
                    estimatedMinutes = 45,
                    aiSorted = true,
                    todayOrder = 99
                ),
                // 2. "Do this next" Hero card task with due time (Slate tint)
                AnchorItem(
                    title = "Quarterly systems architecture review & client sync",
                    rawInput = "Quarterly systems architecture review & client sync today at 2:30pm 45m #meeting #work",
                    type = AnchorItem.TYPE_TASK,
                    status = AnchorItem.STATUS_TODAY,
                    dueDate = todayStr,
                    dueTime = "14:30",
                    priority = AnchorItem.PRIORITY_HIGH,
                    tags = "meeting,work",
                    estimatedMinutes = 45,
                    aiSorted = true,
                    todayOrder = 0
                ),
                // 3. Today bento item (Sage tint)
                AnchorItem(
                    title = "Team wellness sprint retrospective & stretch",
                    rawInput = "Team wellness sprint retrospective & stretch today at 4pm #wellness #health",
                    type = AnchorItem.TYPE_TASK,
                    status = AnchorItem.STATUS_TODAY,
                    dueDate = todayStr,
                    dueTime = "16:00",
                    priority = AnchorItem.PRIORITY_MEDIUM,
                    tags = "wellness,health",
                    estimatedMinutes = 30,
                    aiSorted = true,
                    todayOrder = 1
                ),
                // 4. Today bento item (Apricot tint)
                AnchorItem(
                    title = "Order studio acoustic baffles & hardware",
                    rawInput = "Order studio acoustic baffles & hardware today at 6pm #order #studio",
                    type = AnchorItem.TYPE_TASK,
                    status = AnchorItem.STATUS_TODAY,
                    dueDate = todayStr,
                    dueTime = "18:00",
                    priority = AnchorItem.PRIORITY_LOW,
                    tags = "order,studio",
                    estimatedMinutes = 20,
                    aiSorted = true,
                    todayOrder = 2
                ),
                // 5. Today bento item (Orchid tint)
                AnchorItem(
                    title = "Design team celebration dinner reservations",
                    rawInput = "Design team celebration dinner reservations today at 8pm #celebration #dinner",
                    type = AnchorItem.TYPE_TASK,
                    status = AnchorItem.STATUS_TODAY,
                    dueDate = todayStr,
                    dueTime = "20:00",
                    priority = AnchorItem.PRIORITY_MEDIUM,
                    tags = "celebration,dinner",
                    estimatedMinutes = 15,
                    aiSorted = true,
                    todayOrder = 3
                ),
                // 6. Inbox task (Sage tint)
                AnchorItem(
                    title = "Schedule annual preventive ophthalmologist checkup",
                    rawInput = "Schedule annual preventive ophthalmologist checkup #health",
                    type = AnchorItem.TYPE_TASK,
                    status = AnchorItem.STATUS_INBOX,
                    tags = "health,wellness",
                    aiSorted = true
                ),
                // 7. Inbox task (Apricot tint)
                AnchorItem(
                    title = "Curate references for generative typography lecture",
                    rawInput = "Curate references for generative typography lecture #learn #course",
                    type = AnchorItem.TYPE_TASK,
                    status = AnchorItem.STATUS_INBOX,
                    tags = "learn,course",
                    aiSorted = true
                ),
                // 8. Upcoming task (Slate tint)
                AnchorItem(
                    title = "Keynote rehearsal with European engineering leads",
                    rawInput = "Keynote rehearsal with European engineering leads tomorrow at 10am #meeting #doc",
                    type = AnchorItem.TYPE_TASK,
                    status = AnchorItem.STATUS_INBOX,
                    dueDate = tomorrowStr,
                    dueTime = "10:00",
                    priority = AnchorItem.PRIORITY_HIGH,
                    tags = "meeting,doc",
                    estimatedMinutes = 60,
                    aiSorted = true
                ),
                // 9. Note (Orchid tint)
                AnchorItem(
                    title = "Design premise: Large rounded forms layered like satin and dark glass. Restraint and pixel precision over decoration.",
                    rawInput = "note: Design premise: Large rounded forms layered like satin and dark glass. Restraint and pixel precision over decoration. #idea #philosophy",
                    type = AnchorItem.TYPE_NOTE,
                    status = AnchorItem.STATUS_INBOX,
                    tags = "idea,philosophy",
                    aiSorted = true
                ),
                // 10. Done item (Rose tint)
                AnchorItem(
                    title = "Deploy production v3.2 build release candidate",
                    rawInput = "Deploy production v3.2 build release candidate #release #personal",
                    type = AnchorItem.TYPE_TASK,
                    status = AnchorItem.STATUS_DONE,
                    completedAt = now - 3600000L * 2,
                    tags = "release,personal",
                    aiSorted = true
                )
            )

            db.anchorDao().insertAll(sampleItems)
            refreshOverdue()
            _uiState.update { it.copy(isSettingsOpen = false) }
        }
    }

    fun triggerTestNotification(context: Context) {
        val sampleTask = todayItems.value.firstOrNull() ?: inboxItems.value.firstOrNull()
        if (sampleTask != null) {
            NotificationHelper.showTaskReminder(context, sampleTask.id, sampleTask.title, escalationLevel = 1)
        } else {
            NotificationHelper.showDeadAppCheckIn(context, hoursInactive = _uiState.value.deadAppHours)
        }
    }

    private fun updateLastActive() {
        val prefs = getApplication<Application>().getSharedPreferences("anchor_prefs", Context.MODE_PRIVATE)
        prefs.edit().putLong("last_active_time", System.currentTimeMillis()).apply()
    }

    private fun getStartOfDayTimestamp(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
