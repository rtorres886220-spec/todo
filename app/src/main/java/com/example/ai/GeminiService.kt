package com.example.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.AnchorItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class ParsedCapture(
    val type: String = AnchorItem.TYPE_TASK,
    val title: String,
    val dueDate: String? = null,
    val dueTime: String? = null,
    val priority: String = AnchorItem.PRIORITY_MEDIUM,
    val tags: List<String> = emptyList(),
    val estimatedMinutes: Int? = null
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

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val apiKey: String
        get() = BuildConfig.GEMINI_API_KEY

    private val hasApiKey: Boolean
        get() = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY" && apiKey != "your_api_key_here"

    /**
     * Parses a zero-friction text capture into one or multiple structured items.
     * Uses Gemini structured JSON when available, falls back to local heuristic NLP parser.
     */
    suspend fun parseCapture(rawInput: String): List<ParsedCapture> = withContext(Dispatchers.IO) {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) return@withContext emptyList()

        if (hasApiKey) {
            try {
                val geminiResult = callGeminiForParsing(trimmed)
                if (geminiResult.isNotEmpty()) {
                    return@withContext geminiResult
                }
            } catch (e: Exception) {
                Log.e("GeminiService", "Gemini parsing failed, falling back to heuristic", e)
            }
        }

        // Resilient fallback heuristic parser
        return@withContext parseWithLocalHeuristics(trimmed)
    }

    private fun callGeminiForParsing(text: String): List<ParsedCapture> {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentDay = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())

        val systemPrompt = """
            You are the high-speed intelligence engine for 'Anchor', a zero-friction capture to-do and notes app.
            Today is $currentDay, $todayStr.
            The user just typed or spoke: "$text"
            
            Instructions:
            1. If the input contains multiple distinct tasks or notes (e.g. combined with 'and', commas, or line breaks), split them into separate items.
            2. For each item determine:
               - "type": "task" or "note" (thoughts, ideas, snippets are notes; actions are tasks)
               - "title": concise, action-oriented title with filler words removed
               - "dueDate": ISO format "YYYY-MM-DD" or null if not specified (resolve relative words like 'today', 'tomorrow', 'next friday', 'this weekend')
               - "dueTime": "HH:mm" in 24h format (e.g., '14:30', '09:00') or null if not specified
               - "priority": "high", "medium", "low", or "none"
               - "tags": array of 1-3 lowercase relevant tags (e.g. ["work", "errands", "health", "ideas"])
               - "estimatedMinutes": estimated duration in minutes (integer like 15, 30, 60) or null
            
            Respond ONLY with a valid JSON array of objects. No markdown formatting, no code block backticks.
        """.trimIndent()

        val jsonResponse = callGeminiApi(systemPrompt) ?: return emptyList()
        val cleanedJson = cleanJsonResponse(jsonResponse)
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
            results.add(
                ParsedCapture(
                    type = obj.optString("type", AnchorItem.TYPE_TASK),
                    title = obj.optString("title", text),
                    dueDate = if (obj.has("dueDate") && !obj.isNull("dueDate")) obj.getString("dueDate") else null,
                    dueTime = if (obj.has("dueTime") && !obj.isNull("dueTime")) obj.getString("dueTime") else null,
                    priority = obj.optString("priority", AnchorItem.PRIORITY_MEDIUM),
                    tags = tags,
                    estimatedMinutes = if (obj.has("estimatedMinutes") && !obj.isNull("estimatedMinutes")) obj.getInt("estimatedMinutes") else null
                )
            )
        }
        return results
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

        if (hasApiKey) {
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

                val response = callGeminiApi(prompt)
                if (response != null) {
                    val json = JSONObject(cleanJsonResponse(response))
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
            } catch (e: Exception) {
                Log.e("GeminiService", "Morning brief AI generation error", e)
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

        if (hasApiKey) {
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

                val response = callGeminiApi(prompt)
                if (response != null) {
                    val json = JSONObject(cleanJsonResponse(response))
                    return@withContext EveningReview(
                        summary = json.optString("summary", "You accomplished $completedCount anchors today!"),
                        completedCount = completedCount,
                        slippedTaskIds = slippedIds,
                        reflection = json.optString("reflection", "Sleep with a clear mind. Tomorrow is a fresh canvas.")
                    )
                }
            } catch (e: Exception) {
                Log.e("GeminiService", "Evening review AI error", e)
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
        if (hasApiKey) {
            try {
                val prompt = """
                    The user has postponed/snoozed this task $snoozeCount times: "$taskTitle"
                    Procrastination usually happens when a task is too vague, overwhelming, or emotionally charged.
                    
                    Provide:
                    1. "empathyPrompt": A warm, non-judgmental question asking why they feel stuck (e.g. "Is this missing clear info or does it just feel daunting?")
                    2. "subtasks": An array of 2 to 3 microscopic, friction-free 5-minute first steps that will get them started immediately.
                    
                    Respond strictly in JSON:
                    {
                      "empathyPrompt": "string",
                      "subtasks": ["step 1", "step 2", "step 3"]
                    }
                """.trimIndent()

                val response = callGeminiApi(prompt)
                if (response != null) {
                    val json = JSONObject(cleanJsonResponse(response))
                    val subtasksArray = json.optJSONArray("subtasks")
                    val subtasks = mutableListOf<String>()
                    if (subtasksArray != null) {
                        for (i in 0 until subtasksArray.length()) {
                            subtasks.add(subtasksArray.getString(i))
                        }
                    }
                    return@withContext SnoozeBreakdown(
                        empathyPrompt = json.optString("empathyPrompt", "You've snoozed this $snoozeCount times. Let's make it frictionless:"),
                        subtasks = if (subtasks.isNotEmpty()) subtasks else listOf(
                            "Open relevant window/document for 2 minutes",
                            "Draft the very first sentence or note",
                            "Decide next action"
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("GeminiService", "Snooze breakdown error", e)
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

    private fun callGeminiApi(prompt: String): String? {
        val models = listOf("gemini-2.5-flash", "gemini-1.5-flash", "gemini-3.8-flash")
        for (model in models) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val requestJson = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", prompt)
                                })
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("temperature", 0.2)
                        put("responseMimeType", "application/json")
                    })
                }

                val body = requestJson.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: return null
                    val root = JSONObject(responseBody)
                    val candidates = root.optJSONArray("candidates") ?: return null
                    if (candidates.length() > 0) {
                        val firstCandidate = candidates.getJSONObject(0)
                        val content = firstCandidate.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return parts.getJSONObject(0).optString("text")
                        }
                    }
                } else {
                    Log.w("GeminiService", "Gemini API call returned status ${response.code} for $model: ${response.message}")
                }
            } catch (e: Exception) {
                Log.e("GeminiService", "Error calling model $model", e)
            }
        }
        return null
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
        // Check if there are multiple statements separated by 'and also', 'and then', newlines, or bullets
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
        val hashtagPattern = Pattern.compile("#(\\w+)")
        val matcher = hashtagPattern.matcher(workingText)
        while (matcher.find()) {
            matcher.group(1)?.let { tags.add(it.lowercase(Locale.getDefault())) }
        }
        workingText = workingText.replace(Regex("#\\w+"), "").trim()

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
                title = workingText,
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

        // Clean up title
        val cleanTitle = workingText.replace(Regex("\\s+"), " ").trim()

        return ParsedCapture(
            type = AnchorItem.TYPE_TASK,
            title = if (cleanTitle.isNotBlank()) cleanTitle else raw,
            dueDate = dueDate,
            dueTime = dueTime,
            priority = priority,
            tags = tags.distinct(),
            estimatedMinutes = estimatedMinutes ?: 25
        )
    }
}
