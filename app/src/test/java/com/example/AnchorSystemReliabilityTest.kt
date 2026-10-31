package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.ai.AiValidator
import com.example.ai.GeminiService
import com.example.ai.ParsedCapture
import com.example.data.AnchorDatabase
import com.example.data.AnchorItem
import com.example.data.AnchorRepository
import com.example.data.HabitMetric
import com.example.data.HabitProfiler
import com.example.notification.AnchorAlarmScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AnchorSystemReliabilityTest {

    private lateinit var context: Context
    private lateinit var database: AnchorDatabase
    private lateinit var repository: AnchorRepository
    private lateinit var geminiService: GeminiService

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AnchorDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        geminiService = GeminiService()
        repository = AnchorRepository(database.anchorDao(), geminiService, context, habitDao = database.habitDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    // 1. Strict AI Validation tests
    @Test
    fun `ai validator rejects invalid dates and times`() {
        assertNull(AiValidator.validateDate("2026-02-31")) // Invalid date
        assertNull(AiValidator.validateDate("not-a-date"))
        assertNull(AiValidator.validateDate("2026/10/20")) // Wrong format
        assertEquals("2026-10-30", AiValidator.validateDate("2026-10-30"))

        assertNull(AiValidator.validateTime("25:00")) // Invalid hour
        assertNull(AiValidator.validateTime("14:65")) // Invalid minute
        assertNull(AiValidator.validateTime("3pm")) // Non-24h format
        assertEquals("14:30", AiValidator.validateTime("14:30"))
        assertEquals("09:00", AiValidator.validateTime("9:00"))
    }

    @Test
    fun `ai validator normalizes priorities and tags`() {
        assertEquals(AnchorItem.PRIORITY_HIGH, AiValidator.validatePriority("HIGH"))
        assertEquals(AnchorItem.PRIORITY_MEDIUM, AiValidator.validatePriority("invalid_priority"))
        assertEquals(AnchorItem.PRIORITY_LOW, AiValidator.validatePriority("low"))

        val dirtyTags = listOf("  #URGENT  ", "work!!", "a", "this_is_a_very_long_tag_that_exceeds_length")
        val cleanTags = AiValidator.validateTags(dirtyTags)
        assertTrue(cleanTags.contains("urgent"))
        assertTrue(cleanTags.contains("work"))
        assertFalse(cleanTags.contains("a")) // Too short
    }

    @Test
    fun `ai validator normalizes duration bounds`() {
        assertNull(AiValidator.validateEstimatedMinutes(0))
        assertNull(AiValidator.validateEstimatedMinutes(-10))
        assertNull(AiValidator.validateEstimatedMinutes(5000)) // Beyond 24 hours
        assertEquals(45, AiValidator.validateEstimatedMinutes(45))
    }

    // 2. Capture and immediate persistence
    @Test
    fun `captureInstant saves immediately before AI finishes`() = runBlocking {
        val id = repository.captureInstant("Schedule dentist appointment tomorrow at 10am")
        assertTrue(id > 0)

        val item = repository.getItem(id)
        assertNotNull(item)
        assertEquals("Schedule dentist appointment tomorrow at 10am", item?.title)
        assertEquals(AnchorItem.STATUS_INBOX, item?.status)
    }

    // 3. Reminder trigger calculation
    @Test
    fun `reminder trigger calculates default 9am for date-only tasks`() {
        val dateOnlyTask = AnchorItem(
            id = 101,
            title = "File taxes",
            dueDate = "2026-11-15",
            dueTime = null
        )
        val trigger = AnchorAlarmScheduler.calculateTaskTriggerMillis(dateOnlyTask)
        assertNotNull(trigger)

        val cal = Calendar.getInstance().apply { timeInMillis = trigger!! }
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(10, cal.get(Calendar.MONTH)) // November is index 10 (0-based)
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(9, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
    }

    @Test
    fun `reminder trigger uses exact time when specified`() {
        val taskWithTime = AnchorItem(
            id = 102,
            title = "Board meeting",
            dueDate = "2026-11-15",
            dueTime = "14:30"
        )
        val trigger = AnchorAlarmScheduler.calculateTaskTriggerMillis(taskWithTime)
        assertNotNull(trigger)

        val cal = Calendar.getInstance().apply { timeInMillis = trigger!! }
        assertEquals(14, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, cal.get(Calendar.MINUTE))
    }

    @Test
    fun `completed task yields null reminder trigger`() {
        val completedTask = AnchorItem(
            id = 103,
            title = "Completed item",
            status = AnchorItem.STATUS_DONE,
            dueDate = "2026-11-15",
            dueTime = "14:30"
        )
        val trigger = AnchorAlarmScheduler.calculateTaskTriggerMillis(completedTask)
        assertNull(trigger)
    }

    // 4. Safe parent-child task breakdown & duplicate prevention
    @Test
    fun `applyBreakdownSubtasks preserves parent task and assigns parentId to children`() = runBlocking {
        val parentTask = AnchorItem(
            title = "Prepare keynote presentation",
            rawInput = "Prepare keynote presentation",
            priority = AnchorItem.PRIORITY_HIGH,
            tags = "work,conference"
        )
        val parentId = database.anchorDao().insert(parentTask)
        val insertedParent = database.anchorDao().getItemById(parentId)!!

        val subtasks = listOf(
            "Outline key message and 3 takeaways",
            "Collect 4 product screenshots",
            "Rehearse speaking pace for 10 minutes"
        )

        val updatedParent = repository.applyBreakdownSubtasks(insertedParent, subtasks)

        // Verify parent is NOT deleted
        val parentAfter = database.anchorDao().getItemById(parentId)
        assertNotNull(parentAfter)
        assertEquals("Prepare keynote presentation", parentAfter?.title)
        assertTrue(parentAfter?.aiBreakdownSteps?.contains("Outline key message") == true)

        // Verify subtask children exist with parentId
        val children = database.anchorDao().getSubtasksSync(parentId)
        assertEquals(3, children.size)
        children.forEach { child ->
            assertEquals(parentId, child.parentId)
            assertEquals("work,conference", child.tags)
            assertEquals(AnchorItem.PRIORITY_HIGH, child.priority)
        }

        // Test duplicate prevention: applying a breakdown again should replace previous subtasks, not duplicate
        val newSubtasks = listOf("Updated step 1", "Updated step 2")
        repository.applyBreakdownSubtasks(parentAfter!!, newSubtasks)

        val childrenAfterSecondCall = database.anchorDao().getSubtasksSync(parentId)
        assertEquals(2, childrenAfterSecondCall.size)
        assertEquals("Updated step 1", childrenAfterSecondCall[0].title)
    }

    // 5. Query separation: Upcoming vs Today vs Overdue
    @Test
    fun `upcoming query only returns strictly future tasks`() = runBlocking {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val pastStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)

        cal.add(Calendar.DAY_OF_YEAR, 2)
        val futureStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)

        val pastTask = AnchorItem(title = "Past overdue task", dueDate = pastStr, status = AnchorItem.STATUS_INBOX)
        val todayTask = AnchorItem(title = "Today task", dueDate = todayStr, status = AnchorItem.STATUS_TODAY)
        val futureTask = AnchorItem(title = "Future task", dueDate = futureStr, status = AnchorItem.STATUS_INBOX)

        database.anchorDao().insertAll(listOf(pastTask, todayTask, futureTask))

        val upcomingList = database.anchorDao().getUpcomingItems(todayStr).first()

        assertEquals(1, upcomingList.size)
        assertEquals("Future task", upcomingList[0].title)
    }

    // 6. Contextual Intelligence grounded reasoning
    @Test
    fun `contextual intelligence answers what am I forgetting with real overdue items`() = runBlocking {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -2)
        val overdueStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)

        val overdueItem = AnchorItem(
            id = 1,
            title = "Renew domain name",
            dueDate = overdueStr,
            status = AnchorItem.STATUS_INBOX
        )
        val normalItem = AnchorItem(
            id = 2,
            title = "Read a book",
            status = AnchorItem.STATUS_INBOX
        )

        val answer = geminiService.askContextualIntelligence(
            query = "What am I forgetting?",
            allTasks = listOf(overdueItem, normalItem),
            allNotes = emptyList()
        )

        assertTrue(answer.relevantItems.any { it.title == "Renew domain name" })
    }

    @Test
    fun `contextual intelligence answers 20 min query with short tasks`() = runBlocking {
        val shortTask = AnchorItem(id = 10, title = "Reply to quick email", estimatedMinutes = 15)
        val longTask = AnchorItem(id = 11, title = "Rebuild server cluster", estimatedMinutes = 180)

        val answer = geminiService.askContextualIntelligence(
            query = "What can I finish in 20 minutes?",
            allTasks = listOf(shortTask, longTask),
            allNotes = emptyList()
        )

        assertTrue(answer.relevantItems.any { it.title == "Reply to quick email" })
        assertFalse(answer.relevantItems.any { it.title == "Rebuild server cluster" })
    }

    // 7. Behind-The-Scenes Task Mutability (Non-Negotiable vs Flexible)
    @Test
    fun `task mutability detects locked external commitments vs flexible internal tasks`() {
        // Non-negotiable commitments
        assertTrue(AiValidator.isNonNegotiableSchedule("Flight to NYC", "Flight to NYC at 3pm", "15:00"))
        assertTrue(AiValidator.isNonNegotiableSchedule("Doctor appointment with Dr Smith", "Doctor appointment", "10:30"))
        assertTrue(AiValidator.isNonNegotiableSchedule("Job interview with Google", "Interview tomorrow at 2pm", "14:00"))

        // Flexible self-directed tasks
        assertFalse(AiValidator.isNonNegotiableSchedule("Draft quarterly report", "Draft report tomorrow", "11:00"))
        assertFalse(AiValidator.isNonNegotiableSchedule("Clean the garage", "Clean garage this weekend", null))
        assertFalse(AiValidator.isNonNegotiableSchedule("Review pull requests", "Review PRs", null))
    }

    // 8. Habit Profiler & Duration Self-Calibration
    @Test
    fun `habit profiler calculates estimation multiplier and auto corrects duration`() = runBlocking {
        val habitDao = database.habitDao()

        // Log 3 tasks where user estimated 30 mins but actually took 45 mins (ratio = 1.5)
        habitDao.insert(HabitMetric(itemId = 1, actionType = "complete", hourOfDay = 10, dayOfWeek = 2, estimatedMinutes = 30, actualMinutes = 45))
        habitDao.insert(HabitMetric(itemId = 2, actionType = "complete", hourOfDay = 10, dayOfWeek = 3, estimatedMinutes = 30, actualMinutes = 45))
        habitDao.insert(HabitMetric(itemId = 3, actionType = "complete", hourOfDay = 14, dayOfWeek = 4, estimatedMinutes = 30, actualMinutes = 45))

        val profiler = HabitProfiler(habitDao)
        val profile = profiler.getProfile()

        // Verify multiplier is ~1.5
        assertEquals(1.5f, profile.estimationMultiplier, 0.05f)

        // Verify self-correction calibration: 40 mins initial becomes 60 mins
        val calibrated = profile.calibrateDuration(40)
        assertEquals(60, calibrated)
    }

    // 9. High-Density Token-Guarded Context Serializer
    @Test
    fun `compact prompt serialization produces token efficient representations`() {
        val lockedTask = AnchorItem(id = 1, title = "Flight to SFO", priority = AnchorItem.PRIORITY_HIGH, dueDate = "2026-11-01", dueTime = "08:30", estimatedMinutes = 120, isLockedSchedule = true)
        val flexTask = AnchorItem(id = 2, title = "Write newsletter", priority = AnchorItem.PRIORITY_MEDIUM, dueDate = "2026-11-01", dueTime = "14:00", estimatedMinutes = 45, isLockedSchedule = false)

        val compact = geminiService.serializeTasksCompact(listOf(lockedTask, flexTask))

        assertTrue(compact.contains("ID:1|Flight to SFO|high|2026-11-01|08:30|120m|LOCKED|snz:0"))
        assertTrue(compact.contains("ID:2|Write newsletter|medium|2026-11-01|14:00|45m|FLEX|snz:0"))
    }

    // 10. Tactical Strategic Micro-Step Generation
    @Test
    fun `strategic micro-step enrichment injects high leverage guidance as step one`() = runBlocking {
        val highPrioTask = AnchorItem(id = 5, title = "Complete pitch deck", priority = AnchorItem.PRIORITY_HIGH, estimatedMinutes = 60)
        val profile = HabitProfiler(database.habitDao()).getProfile()

        val steps = geminiService.enrichTaskWithStrategicMicroSteps(highPrioTask, profile)

        assertTrue(steps.isNotEmpty())
        assertTrue("Step 1 must contain strategic guidance marker", steps[0].startsWith("★ STRATEGY:"))
    }

    // 11. Autonomous Schedule Rebalance Never Modifies Locked Commitments
    @Test
    fun `autonomous rebalance never touches locked tasks and reschedules flexible ones`() = runBlocking {
        val lockedMeeting = AnchorItem(id = 100, title = "Client Meeting", dueTime = "14:00", isLockedSchedule = true, estimatedMinutes = 60)
        val conflictingFlex = AnchorItem(id = 101, title = "Organize desk", dueTime = "14:00", isLockedSchedule = false, estimatedMinutes = 30)

        val profile = HabitProfiler(database.habitDao()).getProfile()
        val adjustments = geminiService.evaluateAutonomousRebalance(listOf(lockedMeeting, conflictingFlex), profile)

        // Must NOT suggest any change to locked task ID 100
        assertFalse("Locked task must never be adjusted", adjustments.any { it.taskId == 100L })

        // Should adjust the conflicting flexible task ID 101
        assertTrue("Conflicting flexible task should be adjusted", adjustments.any { it.taskId == 101L })
    }
}
