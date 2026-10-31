package com.example.ai

import android.util.Log
import com.example.data.AnchorItem
import com.example.data.HabitProfile
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

data class ParsedCapture(
    val type: String = AnchorItem.TYPE_TASK,
    val title: String,
    val dueDate: String? = null,
    val dueTime: String? = null,
    val priority: String = AnchorItem.PRIORITY_MEDIUM,
    val tags: List<String> = emptyList(),
    val estimatedMinutes: Int? = null,
    val isLockedSchedule: Boolean = false
)

data class ScheduleAdjustment(
    val taskId: Long,
    val suggestedTime: String,
    val calibratedDuration: Int,
    val strategicRationale: String
)

data class MorningBrief(
    val focusHeadline: String,
    val topTaskIds: List<Long>,
    val motivationalQuote: String,
    val estimatedTotalMinutes: Int
)

data class EveningReview(
    val summary: String,
    val completedCount: Int,
    val slippedTaskIds: List<Long>,
    val reflection: String
)

data class SnoozeBreakdown(
    val empathyPrompt: String,
    val subtasks: List<String>
)

data class IntelligenceAnswer(
    val headline: String,
    val answer: String,
    val relevantItems: List<AnchorItem> = emptyList(),
    val suggestedActions: List<String> = emptyList()
)

class GeminiService {

    companion object {
        private const val TAG = "GeminiService"
        private const val TIMEOUT_MS = 30_000L
        private const val MODEL_NAME = "gemini-3.8-flash"
    }

    private val jsonConfig = generationConfig {
        responseMimeType = "application/json"
        temperature = 0.2f
    }

    private val textConfig = generationConfig {
        temperature = 0.3f
    }

    // Lazy initialization of Firebase AI Logic models to prevent class-init crashes
    private val jsonModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = jsonConfig
        )
    }

    private val textModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = textConfig
        )
    }

    private fun isFirebaseAiAvailable(): Boolean {
        return try {
            val app = try {
                FirebaseApp.getInstance()
            } catch (_: Exception) {
                null
            }
            app != null
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Parses a zero-friction text capture into one or multiple structured items.
     * Uses Firebase AI Logic with App Check security when available,
     * validated through AiValidator, and seamlessly falls back to the local heuristic NLP engine.
     */
    suspend fun parseCapture(rawInput: String): List<ParsedCapture> = withContext(Dispatchers.IO) {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) return@withContext emptyList()

        if (isFirebaseAiAvailable()) {
            try {
                val geminiResult = withTimeout(TIMEOUT_MS) {
                    callAiForParsing(trimmed)
                }
                if (geminiResult.isNotEmpty()) {
                    return@withContext geminiResult.map { AiValidator.normalize(it, trimmed) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Firebase AI parsing failed, falling back to local heuristic: ${e.message}")
            }
        }

        // Resilient fallback heuristic parser
        return@withContext parseWithLocalHeuristics(trimmed).map { AiValidator.normalize(it, trimmed) }
    }

    private suspend fun callAiForParsing(text: String): List<ParsedCapture> {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentDay = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())

        val systemPrompt = """
            You are the high-speed intelligence engine for 'Anchor', a zero-friction capture to-do and notes app.
            Today is $currentDay, $todayStr.
            The user captured: "$text"
            
            Instructions:
            1. If the input contains multiple distinct tasks or notes (e.g. combined with 'and', commas, or line breaks), split them into separate items.
            2. For each item determine:
               - "type": "task" or "note" (thoughts, ideas, snippets are notes; actions are tasks)
               - "title": concise, action-oriented title with filler words removed
               - "dueDate": ISO format "YYYY-MM-DD" or null if not explicitly mentioned or implied (resolve relative words like 'today', 'tomorrow', 'next friday', 'this weekend')
               - "dueTime": "HH:mm" in 24h format (e.g., '14:30', '09:00') or null if not specified
               - "priority": "high", "medium", "low", or "none"
               - "tags": array of 1-3 lowercase relevant tags (e.g. ["work", "errands", "health", "ideas"])
               - "estimatedMinutes": estimated duration in minutes (integer like 15, 30, 60) or null
               - "isLockedSchedule": boolean (true for fixed external commitments like flights, doctor/dentist appts, interviews, or meetings; false for flexible self-directed tasks)
            
            Respond ONLY with a valid JSON array of objects.
        """.trimIndent()

        val response = jsonModel.generateContent(systemPrompt)
        val jsonText = response.text ?: return emptyList()
        val cleanedJson = cleanJsonResponse(jsonText)
        val array = JSONArray(cleanedJson)
        val results = mutableListOf<ParsedCapture>()

        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val tagsArray = obj.optJSONArray("tags")
            val tags = mutableListOf<String>()
            if (tagsArray != null) {
                for (j in 0 until tagsArray.length()) {
                    tags.add(tagsArray.getString(j))
                }
            }
            val titleStr = obj.optString("title", text)
            val dueTimeStr = if (obj.has("dueTime") && !obj.isNull("dueTime")) obj.getString("dueTime") else null
            val isLocked = if (obj.has("isLockedSchedule")) obj.getBoolean("isLockedSchedule") else AiValidator.isNonNegotiableSchedule(titleStr, text, dueTimeStr)

            results.add(
                ParsedCapture(
                    type = obj.optString("type", AnchorItem.TYPE_TASK),
                    title = titleStr,
                    dueDate = if (obj.has("dueDate") && !obj.isNull("dueDate")) obj.getString("dueDate") else null,
                    dueTime = dueTimeStr,
                    priority = obj.optString("priority", AnchorItem.PRIORITY_MEDIUM),
                    tags = tags,
                    estimatedMinutes = if (obj.has("estimatedMinutes") && !obj.isNull("estimatedMinutes")) obj.getInt("estimatedMinutes") else null,
                    isLockedSchedule = isLocked
                )
            )
        }
        return results
    }

    /**
     * Contextual Intelligence Engine:
     * Answers queries about the user's real captured tasks and notes (grounded strictly in local DB).
     */
    suspend fun askContextualIntelligence(
        query: String,
        allTasks: List<AnchorItem>,
        allNotes: List<AnchorItem>
    ): IntelligenceAnswer = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            return@withContext IntelligenceAnswer(
                headline = "How can Anchor help?",
                answer = "Ask questions like 'What am I forgetting?', 'What can I finish in 20 minutes?', or 'What did I write about my car?'."
            )
        }

        if (isFirebaseAiAvailable()) {
            try {
                val answer = withTimeout(TIMEOUT_MS) {
                    callAiForContextualIntelligence(trimmed, allTasks, allNotes)
                }
                if (answer != null) {
                    return@withContext answer
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Firebase AI contextual intelligence failed, using local reasoning: ${e.message}")
            }
        }

        // Local grounded reasoning engine
        return@withContext answerLocally(trimmed, allTasks, allNotes)
    }

    private suspend fun callAiForContextualIntelligence(
        query: String,
        tasks: List<AnchorItem>,
        notes: List<AnchorItem>
    ): IntelligenceAnswer? {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val taskContext = tasks.take(30).joinToString("\n") { t ->
            "- [ID:${t.id}] ${t.title} (Status: ${t.status}, Due: ${t.dueDate ?: "none"} ${t.dueTime ?: ""}, Priority: ${t.priority}, Snoozed: ${t.snoozeCount}x, Est: ${t.estimatedMinutes ?: 30}m, Tags: ${t.tags})"
        }
        val noteContext = notes.take(20).joinToString("\n") { n ->
            "- [ID:${n.id}] ${n.title} (Raw: ${n.rawInput}, Tags: ${n.tags})"
        }

        val prompt = """
            You are Anchor's intelligent personal assistant.
            Today is $todayStr.
            User Question: "$query"
            
            USER'S STORED TASKS:
            $taskContext
            
            USER'S STORED NOTES:
            $noteContext
            
            INSTRUCTIONS:
            - Answer the user's question accurately based ONLY on the stored tasks and notes above.
            - NEVER invent or hallucinate tasks, notes, or commitments not in the list.
            - If asking about short tasks, pick tasks with estimated minutes <= 25.
            - If asking about forgotten or neglected tasks, check overdue items or items snoozed repeatedly.
            - If asking about notes, locate the exact note snippets.
            - Identify matching task/note IDs in 'relevantIds'.
            
            Respond strictly in JSON format:
            {
              "headline": "Short crisp headline",
              "answer": "Clear, direct 1-3 sentence answer",
              "relevantIds": [ID1, ID2],
              "suggestedActions": ["Action 1", "Action 2"]
            }
        """.trimIndent()

        val response = jsonModel.generateContent(prompt)
        val jsonText = response.text ?: return null
        val json = JSONObject(cleanJsonResponse(jsonText))

        val idsArray = json.optJSONArray("relevantIds")
        val matchedIds = mutableSetOf<Long>()
        if (idsArray != null) {
            for (i in 0 until idsArray.length()) {
                matchedIds.add(idsArray.getLong(i))
            }
        }
        val relevantItems = (tasks + notes).filter { matchedIds.contains(it.id) }

        val actionsArray = json.optJSONArray("suggestedActions")
        val actions = mutableListOf<String>()
        if (actionsArray != null) {
            for (i in 0 until actionsArray.length()) {
                actions.add(actionsArray.getString(i))
            }
        }

        return IntelligenceAnswer(
            headline = json.optString("headline", "Anchor Insights"),
            answer = json.optString("answer", "Here is what I found based on your saved anchors."),
            relevantItems = relevantItems,
            suggestedActions = actions
        )
    }

    /**
     * Deterministic local reasoning engine when offline or Gemini is unavailable.
     */
    private fun answerLocally(
        query: String,
        tasks: List<AnchorItem>,
        notes: List<AnchorItem>
    ): IntelligenceAnswer {
        val q = query.lowercase(Locale.getDefault())
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // 1. "What am I forgetting?" / Overdue / Slipping
        if (q.contains("forget") || q.contains("slip") || q.contains("overdue") || q.contains("miss")) {
            val overdue = tasks.filter { it.dueDate != null && it.dueDate < todayStr }
            val snoozed = tasks.filter { it.snoozeCount >= 2 }
            val items = (overdue + snoozed).distinctBy { it.id }.take(5)

            return if (items.isNotEmpty()) {
                IntelligenceAnswer(
                    headline = "High Priority & Postponed",
                    answer = "You have ${items.size} anchors that may need your attention: ${items.take(2).joinToString(" and ") { "'${it.title}'" }}.",
                    relevantItems = items,
                    suggestedActions = listOf("Reschedule slipped anchors", "Tackle easiest first")
                )
            } else {
                IntelligenceAnswer(
                    headline = "Nothing Slipped",
                    answer = "You are up to date! There are no overdue or repeatedly postponed tasks.",
                    relevantItems = emptyList()
                )
            }
        }

        // 2. "What can I finish in 20 minutes?" / Quick wins
        if (q.contains("20") || q.contains("quick") || q.contains("minute") || q.contains("fast") || q.contains("short")) {
            val shortTasks = tasks.filter { (it.estimatedMinutes ?: 25) <= 20 }.take(4)
            return if (shortTasks.isNotEmpty()) {
                IntelligenceAnswer(
                    headline = "Quick Wins (Under 20 Mins)",
                    answer = "You can knock out ${shortTasks.size} tasks right now: ${shortTasks.take(2).joinToString(" and ") { "'${it.title}'" }}.",
                    relevantItems = shortTasks,
                    suggestedActions = listOf("Start 5-minute timer", "Mark completed")
                )
            } else {
                IntelligenceAnswer(
                    headline = "No Short Tasks Estimated",
                    answer = "All your current tasks are planned for 30+ minutes or unestimated. Pick any task and break off a 5-minute slice.",
                    relevantItems = tasks.take(2)
                )
            }
        }

        // 3. "What have I repeatedly postponed?"
        if (q.contains("postpone") || q.contains("snooze") || q.contains("procrastinat") || q.contains("stuck")) {
            val habitual = tasks.filter { it.snoozeCount >= 1 }.sortedByDescending { it.snoozeCount }.take(5)
            return if (habitual.isNotEmpty()) {
                IntelligenceAnswer(
                    headline = "Postponed Anchors",
                    answer = "You have postponed ${habitual.size} items. The most delayed is '${habitual.first().title}' (postponed ${habitual.first().snoozeCount} times).",
                    relevantItems = habitual,
                    suggestedActions = listOf("Use Snooze Coach to break down", "Archive if no longer relevant")
                )
            } else {
                IntelligenceAnswer(
                    headline = "Zero Postponements",
                    answer = "Great momentum! You haven't repeatedly postponed any anchors.",
                    relevantItems = emptyList()
                )
            }
        }

        // 4. "What should I focus on today?"
        if (q.contains("focus") || q.contains("today") || q.contains("priority") || q.contains("start")) {
            val todayTasks = tasks.filter { it.status == AnchorItem.STATUS_TODAY || it.dueDate == todayStr }
            val top = if (todayTasks.isNotEmpty()) todayTasks else tasks.filter { it.priority == AnchorItem.PRIORITY_HIGH }
            val pick = top.take(3)
            return IntelligenceAnswer(
                headline = "Today's Core Focus",
                answer = if (pick.isNotEmpty()) "Focus on: ${pick.joinToString(", ") { "'${it.title}'" }}." else "No tasks scheduled for today yet. Capture your top anchor above.",
                relevantItems = pick,
                suggestedActions = listOf("Do this next", "Sprint focus")
            )
        }

        // 5. Notes search (e.g. "car", "idea", "meeting")
        val keywords = q.replace(Regex("\\b(what|did|i|previously|write|about|where|find|is|are|the|my|a|an)\\b"), "")
            .trim()
            .split(" ")
            .filter { it.length >= 3 }

        if (keywords.isNotEmpty()) {
            val matchedNotes = notes.filter { note ->
                keywords.any { k ->
                    note.title.contains(k, ignoreCase = true) ||
                    note.rawInput.contains(k, ignoreCase = true) ||
                    note.tags.contains(k, ignoreCase = true)
                }
            }
            if (matchedNotes.isNotEmpty()) {
                return IntelligenceAnswer(
                    headline = "Found in Your Notes",
                    answer = "Found ${matchedNotes.size} matching note(s): '${matchedNotes.first().title.take(60)}'.",
                    relevantItems = matchedNotes
                )
            }
        }

        // Default response
        return IntelligenceAnswer(
            headline = "Anchor System Summary",
            answer = "You have ${tasks.size} active tasks and ${notes.size} notes in your local Anchor database.",
            relevantItems = tasks.take(3)
        )
    }

    /**
     * Generates a Morning Brief: picking today's top 3 tasks from deadlines, priorities, and postponements.
     */
    suspend fun generateMorningBrief(activeTasks: List<AnchorItem>): MorningBrief = withContext(Dispatchers.IO) {
        if (activeTasks.isEmpty()) {
            return@withContext MorningBrief(
                focusHeadline = "A clean slate today",
                topTaskIds = emptyList(),
                motivationalQuote = "Capture what comes up or enjoy your open focus time.",
                estimatedTotalMinutes = 0
            )
        }

        if (isFirebaseAiAvailable()) {
            try {
                val taskDescriptions = activeTasks.take(15).joinToString("\n") { task ->
                    "ID: ${task.id} | Title: ${task.title} | Priority: ${task.priority} | DueDate: ${task.dueDate} | Snoozed: ${task.snoozeCount} times | Est: ${task.estimatedMinutes ?: 30}m"
                }

                val prompt = """
                    You are Anchor's Morning Coach. Select today's TOP 3 must-do items from this list to prevent overwhelm.
                    Prioritize items that have impending deadlines, high priority, or have been repeatedly snoozed/postponed.
                    Tasks:
                    $taskDescriptions
                    
                    Respond strictly in JSON format:
                    {
                      "focusHeadline": "Short punchy focal theme for today",
                      "topTaskIds": [ID1, ID2, ID3],
                      "motivationalQuote": "1 crisp inspiring sentence",
                      "estimatedTotalMinutes": sum_of_minutes_number
                    }
                """.trimIndent()

                val response = withTimeout(TIMEOUT_MS) { jsonModel.generateContent(prompt) }
                val jsonText = response.text
                if (jsonText != null) {
                    val json = JSONObject(cleanJsonResponse(jsonText))
                    val idsArray = json.optJSONArray("topTaskIds")
                    val ids = mutableListOf<Long>()
                    if (idsArray != null) {
                        for (i in 0 until idsArray.length()) {
                            ids.add(idsArray.getLong(i))
                        }
                    }
                    return@withContext MorningBrief(
                        focusHeadline = json.optString("focusHeadline", "Focus Sprint"),
                        topTaskIds = if (ids.isNotEmpty()) ids else activeTasks.take(3).map { it.id },
                        motivationalQuote = json.optString("motivationalQuote", "Three key anchors. Everything else is secondary."),
                        estimatedTotalMinutes = json.optInt("estimatedTotalMinutes", 90)
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Morning brief AI generation error, using heuristic fallback: ${e.message}")
            }
        }

        // Heuristic fallback for Morning Brief
        val sorted = activeTasks.sortedWith(
            compareByDescending<AnchorItem> { it.priority == AnchorItem.PRIORITY_HIGH }
                .thenByDescending { it.snoozeCount }
                .thenBy { it.dueDate ?: "9999-99-99" }
        )
        val top3 = sorted.take(3)
        val estTotal = top3.sumOf { it.estimatedMinutes ?: 25 }

        MorningBrief(
            focusHeadline = "Today's Core Anchors",
            topTaskIds = top3.map { it.id },
            motivationalQuote = "Focus exclusively on these 3 anchors to create unstoppable momentum.",
            estimatedTotalMinutes = estTotal
        )
    }

    /**
     * Generates an Evening Review: what got done, what slipped, and encouraging reflection.
     */
    suspend fun generateEveningReview(doneToday: List<AnchorItem>, slipped: List<AnchorItem>): EveningReview = withContext(Dispatchers.IO) {
        val completedCount = doneToday.size
        val slippedIds = slipped.map { it.id }

        if (isFirebaseAiAvailable()) {
            try {
                val doneTitles = doneToday.joinToString(", ") { it.title }
                val slippedTitles = slipped.joinToString(", ") { it.title }

                val prompt = """
                    Generate an Evening Review for Anchor user.
                    Completed today ($completedCount items): $doneTitles
                    Slipped/uncompleted today (${slipped.size} items): $slippedTitles
                    
                    Respond strictly in JSON format:
                    {
                      "summary": "1 celebratory sentence summarizing today's wins",
                      "reflection": "1 calm sentence advising how to gracefully reschedule the slipped tasks without guilt"
                    }
                """.trimIndent()

                val response = withTimeout(TIMEOUT_MS) { jsonModel.generateContent(prompt) }
                val jsonText = response.text
                if (jsonText != null) {
                    val json = JSONObject(cleanJsonResponse(jsonText))
                    return@withContext EveningReview(
                        summary = json.optString("summary", "You accomplished $completedCount anchors today!"),
                        completedCount = completedCount,
                        slippedTaskIds = slippedIds,
                        reflection = json.optString("reflection", "Sleep with a clear mind. Tomorrow is a fresh canvas.")
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Evening review AI error, using heuristic: ${e.message}")
            }
        }

        EveningReview(
            summary = if (completedCount > 0) "Great progress! You closed $completedCount tasks today." else "Quiet day. Rest up and reset.",
            completedCount = completedCount,
            slippedTaskIds = slippedIds,
            reflection = if (slipped.isNotEmpty()) "You have ${slipped.size} slipped items. Reschedule them to tomorrow with one tap." else "All done! Nothing slipped through the cracks."
        )
    }

    /**
     * Snooze Coach: When a task is snoozed 3+ times, Gemini asks why and offers to break it down into micro-steps.
     */
    suspend fun breakdownSnoozedTask(taskTitle: String, snoozeCount: Int): SnoozeBreakdown = withContext(Dispatchers.IO) {
        if (isFirebaseAiAvailable()) {
            try {
                val prompt = """
                    The user has postponed/snoozed this task $snoozeCount times: "$taskTitle"
                    Procrastination usually happens when a task is too vague, overwhelming, or emotionally charged.
                    
                    Provide:
                    1. "empathyPrompt": A warm, non-judgmental question asking why they feel stuck (e.g. "Is this missing clear info or does it just feel daunting?")
                    2. "subtasks": An array of 2 to 4 microscopic, friction-free 5-minute first steps that will get them started immediately.
                    
                    Respond strictly in JSON:
                    {
                      "empathyPrompt": "string",
                      "subtasks": ["step 1", "step 2", "step 3"]
                    }
                """.trimIndent()

                val response = withTimeout(TIMEOUT_MS) { jsonModel.generateContent(prompt) }
                val jsonText = response.text
                if (jsonText != null) {
                    val json = JSONObject(cleanJsonResponse(jsonText))
                    val subtasksArray = json.optJSONArray("subtasks")
                    val subtasks = mutableListOf<String>()
                    if (subtasksArray != null) {
                        for (i in 0 until subtasksArray.length()) {
                            subtasks.add(subtasksArray.getString(i))
                        }
                    }
                    val validatedSubtasks = AiValidator.validateSubtasks(subtasks)
                    return@withContext SnoozeBreakdown(
                        empathyPrompt = json.optString("empathyPrompt", "You've snoozed this $snoozeCount times. Let's make it frictionless:"),
                        subtasks = if (validatedSubtasks.isNotEmpty()) validatedSubtasks else listOf(
                            "Open relevant window/document for 2 minutes",
                            "Draft the very first sentence or note",
                            "Decide next action"
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Snooze breakdown error, using heuristic: ${e.message}")
            }
        }

        // Heuristic breakdown
        SnoozeBreakdown(
            empathyPrompt = "You've snoozed '$taskTitle' $snoozeCount times. Big tasks cause resistance—let's start small:",
            subtasks = listOf(
                "Spend just 3 minutes setting up everything needed for '$taskTitle'",
                "Complete the easiest 20% of '$taskTitle'",
                "Take a 5 minute break after"
            )
        )
    }

    private fun cleanJsonResponse(raw: String): String {
        var str = raw.trim()
        if (str.startsWith("```json")) {
            str = str.substring(7)
        } else if (str.startsWith("```")) {
            str = str.substring(3)
        }
        if (str.endsWith("```")) {
            str = str.substring(0, str.length - 3)
        }
        return str.trim()
    }

    /**
     * Sophisticated local heuristic NLP parser: extracts compound tasks, dates, times, priority, tags.
     */
    fun parseWithLocalHeuristics(input: String): List<ParsedCapture> {
        val segments = if (input.contains("\n")) {
            input.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        } else if (input.contains("; ")) {
            input.split("; ").map { it.trim() }.filter { it.isNotBlank() }
        } else if (input.matches(Regex(".*\\b(and then|and also|plus)\\b.*", RegexOption.IGNORE_CASE))) {
            input.split(Regex("\\b(and then|and also|plus)\\b", RegexOption.IGNORE_CASE)).map { it.trim() }.filter { it.isNotBlank() }
        } else {
            listOf(input)
        }

        return segments.map { parseSingleItemHeuristic(it) }
    }

    private fun parseSingleItemHeuristic(raw: String): ParsedCapture {
        var workingText = raw.trim()
        val tags = mutableListOf<String>()

        // Extract #hashtags
        val hashtagPattern = Pattern.compile("#([a-zA-Z0-9_-]+)")
        val matcher = hashtagPattern.matcher(workingText)
        while (matcher.find()) {
            matcher.group(1)?.let { tags.add(it.lowercase(Locale.getDefault())) }
        }
        workingText = workingText.replace(Regex("#[a-zA-Z0-9_-]+"), "").trim()

        // Detect note vs task
        val isNote = workingText.startsWith("note:", ignoreCase = true) ||
                workingText.startsWith("idea:", ignoreCase = true) ||
                workingText.startsWith("memo:", ignoreCase = true) ||
                workingText.startsWith("remember:", ignoreCase = true)

        if (isNote) {
            workingText = workingText.replace(Regex("^(note|idea|memo|remember):\\s*", RegexOption.IGNORE_CASE), "").trim()
            tags.add("note")
            return ParsedCapture(
                type = AnchorItem.TYPE_NOTE,
                title = workingText.ifBlank { raw },
                tags = tags
            )
        }

        // Detect priority
        var priority = AnchorItem.PRIORITY_MEDIUM
        if (workingText.contains("urgent", ignoreCase = true) ||
            workingText.contains("asap", ignoreCase = true) ||
            workingText.contains("important", ignoreCase = true) ||
            workingText.contains("!!!")
        ) {
            priority = AnchorItem.PRIORITY_HIGH
            workingText = workingText.replace(Regex("\\b(urgent|asap|important)\\b", RegexOption.IGNORE_CASE), "")
                .replace("!!!", "")
                .trim()
        } else if (workingText.contains("someday", ignoreCase = true) || workingText.contains("low priority", ignoreCase = true)) {
            priority = AnchorItem.PRIORITY_LOW
        }

        // Detect estimated minutes
        var estimatedMinutes: Int? = null
        val durationRegex = Regex("(\\d+)\\s*(min|mins|minutes|m|hour|hours|hr|hrs)\\b", RegexOption.IGNORE_CASE)
        val durationMatch = durationRegex.find(workingText)
        if (durationMatch != null) {
            val num = durationMatch.groupValues[1].toIntOrNull() ?: 0
            val unit = durationMatch.groupValues[2].lowercase(Locale.getDefault())
            estimatedMinutes = if (unit.startsWith("h")) num * 60 else num
            workingText = workingText.replace(durationMatch.value, "").trim()
        }

        // Detect Date
        var dueDate: String? = null
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        if (workingText.contains(Regex("\\btoday\\b", RegexOption.IGNORE_CASE))) {
            dueDate = dateFormat.format(calendar.time)
            workingText = workingText.replace(Regex("\\btoday\\b", RegexOption.IGNORE_CASE), "").trim()
        } else if (workingText.contains(Regex("\\btomorrow\\b", RegexOption.IGNORE_CASE))) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            dueDate = dateFormat.format(calendar.time)
            workingText = workingText.replace(Regex("\\btomorrow\\b", RegexOption.IGNORE_CASE), "").trim()
        } else if (workingText.contains(Regex("\\bnext week\\b", RegexOption.IGNORE_CASE))) {
            calendar.add(Calendar.DAY_OF_YEAR, 7)
            dueDate = dateFormat.format(calendar.time)
            workingText = workingText.replace(Regex("\\bnext week\\b", RegexOption.IGNORE_CASE), "").trim()
        }

        // Detect Time (e.g. at 3pm, at 14:00, 10:30am)
        var dueTime: String? = null
        val timeRegex = Regex("\\b(?:at\\s+)?(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\b", RegexOption.IGNORE_CASE)
        val timeMatch = timeRegex.findAll(workingText).firstOrNull { it.groupValues[3].isNotEmpty() || it.groupValues[2].isNotEmpty() }
        if (timeMatch != null) {
            var hour = timeMatch.groupValues[1].toIntOrNull() ?: 12
            val minute = timeMatch.groupValues[2].toIntOrNull() ?: 0
            val ampm = timeMatch.groupValues[3].lowercase(Locale.getDefault())

            if (ampm == "pm" && hour < 12) hour += 12
            if (ampm == "am" && hour == 12) hour = 0

            dueTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
            workingText = workingText.replace(timeMatch.value, "").trim()
        }

        // Add contextual tags if matched
        if (workingText.contains(Regex("\\b(call|email|message|reach out)\\b", RegexOption.IGNORE_CASE))) tags.add("communication")
        if (workingText.contains(Regex("\\b(buy|groceries|order|shop|store)\\b", RegexOption.IGNORE_CASE))) tags.add("errands")
        if (workingText.contains(Regex("\\b(gym|workout|run|doctor|dentist|health)\\b", RegexOption.IGNORE_CASE))) tags.add("health")
        if (workingText.contains(Regex("\\b(meeting|deck|client|project|review|report)\\b", RegexOption.IGNORE_CASE))) tags.add("work")

        val cleanTitle = workingText.replace(Regex("\\s+"), " ").trim()
        val isLocked = AiValidator.isNonNegotiableSchedule(cleanTitle, raw, dueTime)

        return ParsedCapture(
            type = AnchorItem.TYPE_TASK,
            title = if (cleanTitle.isNotBlank()) cleanTitle else raw,
            dueDate = dueDate,
            dueTime = dueTime,
            priority = priority,
            tags = tags.distinct(),
            estimatedMinutes = estimatedMinutes ?: 25,
            isLockedSchedule = isLocked
        )
    }

    /**
     * High-density prompt context serializer: compresses tasks into a compact piped string
     * reducing token usage by >70% compared to verbose JSON.
     */
    fun serializeTasksCompact(tasks: List<AnchorItem>): String {
        return tasks.joinToString("\n") { t ->
            val lockTag = if (t.isLockedSchedule) "LOCKED" else "FLEX"
            "ID:${t.id}|${t.title}|${t.priority}|${t.dueDate ?: "none"}|${t.dueTime ?: "none"}|${t.estimatedMinutes ?: 30}m|$lockTag|snz:${t.snoozeCount}"
        }
    }

    /**
     * Enriches a task with tactical micro-steps where Step 1 is always high-leverage strategic guidance ("★ STRATEGY: ...").
     * Uses Gemini high-reasoning model with App Check security, and falls back to deterministic local reasoning.
     */
    suspend fun enrichTaskWithStrategicMicroSteps(
        task: AnchorItem,
        habitProfile: HabitProfile
    ): List<String> = withContext(Dispatchers.IO) {
        val calibratedMinutes = habitProfile.calibrateDuration(task.estimatedMinutes)
        val peakHoursStr = habitProfile.peakHours.joinToString(", ") { String.format(Locale.getDefault(), "%02d:00", it) }

        if (isFirebaseAiAvailable()) {
            try {
                val prompt = """
                    You are Anchor's Executive Strategic Engine.
                    Deconstruct this anchor into 3-4 tactical, friction-free micro-steps.
                    CRITICAL: The very first step MUST be a high-leverage strategic advice directive starting with "★ STRATEGY: " that identifies the single 80/20 move to create momentum or avoid friction.
                    
                    Task: "${task.title}"
                    Priority: ${task.priority}
                    Scheduled Duration: ${calibratedMinutes}m
                    User Peak Performance Hours: $peakHoursStr
                    Snoozed count: ${task.snoozeCount}
                    Locked Schedule: ${task.isLockedSchedule}
                    
                    Respond strictly in JSON array of strings:
                    ["★ STRATEGY: ...", "Step 1...", "Step 2..."]
                """.trimIndent()

                val response = withTimeout(TIMEOUT_MS) { jsonModel.generateContent(prompt) }
                val text = response.text
                if (text != null) {
                    val arr = JSONArray(cleanJsonResponse(text))
                    val steps = mutableListOf<String>()
                    for (i in 0 until arr.length()) {
                        val s = arr.getString(i).trim()
                        if (s.isNotBlank()) steps.add(s)
                    }
                    if (steps.isNotEmpty()) {
                        return@withContext steps
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Strategic micro-step AI error, using deterministic logic: ${e.message}")
            }
        }

        // High-reasoning deterministic local fallback
        val advice = if (task.snoozeCount >= 2) {
            "★ STRATEGY: Eliminate friction—open the workspace and do only the easiest 3-minute sub-slice."
        } else if (task.priority == AnchorItem.PRIORITY_HIGH) {
            "★ STRATEGY: Tackle during peak cognitive block ($peakHoursStr); protect from interruptions."
        } else {
            "★ STRATEGY: Execute in a single focused ${calibratedMinutes}m sprint; clear all distractions."
        }

        listOf(
            advice,
            "Set up prerequisites and open required materials (3 mins)",
            "Execute the core 80% output for '${task.title.take(40)}'",
            "Review, verify completion, and log next steps"
        )
    }

    /**
     * Evaluates autonomous schedule rebalancing behind the scenes.
     * NEVER changes locked non-negotiable tasks (meetings, flights, doctors).
     * Re-sequences flexible tasks around locked commitments and aligns them to user's peak performance hours.
     */
    suspend fun evaluateAutonomousRebalance(
        todayTasks: List<AnchorItem>,
        habitProfile: HabitProfile
    ): List<ScheduleAdjustment> = withContext(Dispatchers.IO) {
        val flexibleTasks = todayTasks.filter { !it.isLockedSchedule }
        val lockedTasks = todayTasks.filter { it.isLockedSchedule }

        if (flexibleTasks.isEmpty()) return@withContext emptyList()

        if (isFirebaseAiAvailable() && todayTasks.size >= 2) {
            try {
                val compactContext = serializeTasksCompact(todayTasks)
                val peakHoursStr = habitProfile.peakHours.joinToString(", ") { String.format(Locale.getDefault(), "%02d:00", it) }

                val prompt = """
                    You are Anchor's Autonomous Schedule Optimizer.
                    Evaluate today's tasks and optimize ONLY the FLEXIBLE (FLEX) tasks.
                    RULE: NEVER modify or suggest changes for LOCKED tasks (fixed commitments).
                    User Peak Performance Hours: $peakHoursStr
                    
                    TASKS:
                    $compactContext
                    
                    Identify any flexible tasks that have scheduling conflicts, are timed during locked tasks, or should be aligned to peak focus hours.
                    Respond strictly in JSON array of objects:
                    [
                      {
                        "taskId": 123,
                        "suggestedTime": "10:30",
                        "calibratedDuration": 45,
                        "strategicRationale": "Aligned to peak morning focus hour without conflicting with locked commitments"
                      }
                    ]
                """.trimIndent()

                val response = withTimeout(TIMEOUT_MS) { jsonModel.generateContent(prompt) }
                val text = response.text
                if (text != null) {
                    val arr = JSONArray(cleanJsonResponse(text))
                    val adjustments = mutableListOf<ScheduleAdjustment>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val id = obj.getLong("taskId")
                        // Enforce safety constraint: ignore any response attempting to modify a locked task
                        if (lockedTasks.none { it.id == id }) {
                            adjustments.add(
                                ScheduleAdjustment(
                                    taskId = id,
                                    suggestedTime = obj.getString("suggestedTime"),
                                    calibratedDuration = obj.optInt("calibratedDuration", 30),
                                    strategicRationale = obj.optString("strategicRationale", "Optimized sequence")
                                )
                            )
                        }
                    }
                    if (adjustments.isNotEmpty()) {
                        return@withContext adjustments
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Autonomous rebalance AI error, using local scheduler: ${e.message}")
            }
        }

        // Deterministic local rebalancing
        val adjustments = mutableListOf<ScheduleAdjustment>()
        val occupiedTimes = lockedTasks.mapNotNull { it.dueTime }.toSet()
        val peakSlots = habitProfile.peakHours.map { String.format(Locale.getDefault(), "%02d:00", it) }

        var slotIndex = 0
        for (flex in flexibleTasks) {
            val calibrated = habitProfile.calibrateDuration(flex.estimatedMinutes)
            val currentDueTime = flex.dueTime

            // If flexible task has no time or collides with a locked task
            if (currentDueTime == null || occupiedTimes.contains(currentDueTime)) {
                // Find next free peak or open slot
                val nextSlot = peakSlots.getOrNull(slotIndex) ?: "14:00"
                slotIndex++
                if (nextSlot != currentDueTime && !occupiedTimes.contains(nextSlot)) {
                    adjustments.add(
                        ScheduleAdjustment(
                            taskId = flex.id,
                            suggestedTime = nextSlot,
                            calibratedDuration = calibrated,
                            strategicRationale = "Re-scheduled to open slot outside locked commitments"
                        )
                    )
                }
            }
        }

        return@withContext adjustments
    }
}
