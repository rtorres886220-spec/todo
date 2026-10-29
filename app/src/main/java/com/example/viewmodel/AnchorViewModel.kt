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
            repository.captureInstant(text, sharedUri)
            refreshOverdue()
        }
    }

    fun moveToToday(item: AnchorItem) {
        viewModelScope.launch {
            repository.moveToToday(item)
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
            repository.markCompleted(item)
            refreshOverdue()
            showUndo(item, "Task completed!")
        }
    }

    fun uncompleteTask(item: AnchorItem) {
        viewModelScope.launch {
            repository.uncomplete(item)
            showUndo(item, "Restored to Inbox")
        }
    }

    fun snoozeTask(item: AnchorItem, hours: Int = 24) {
        viewModelScope.launch {
            repository.snooze(item, hours)
            refreshOverdue()
            showUndo(item, "Snoozed for ${if (hours == 24) "1 day" else "$hours hours"}")
        }
    }

    fun deleteTask(item: AnchorItem) {
        viewModelScope.launch {
            repository.delete(item)
            refreshOverdue()
            showUndo(item, "Item deleted")
        }
    }

    fun updateItem(item: AnchorItem) {
        viewModelScope.launch {
            repository.updateItem(item)
            refreshOverdue()
            _uiState.update { it.copy(editingItem = null) }
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
            val done = db.anchorDao().getAllItems() // We can also use done list
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
