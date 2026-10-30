package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.ai.GeminiService
import com.example.data.AnchorDatabase
import com.example.data.AnchorPreferences
import com.example.data.AnchorRepository
import com.example.notification.AnchorAlarmScheduler
import com.example.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AnchorNotificationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DONE = "com.example.ACTION_DONE"
        const val ACTION_SNOOZE = "com.example.ACTION_SNOOZE"
        const val ACTION_TASK_REMINDER = "com.example.ACTION_TASK_REMINDER"
        const val ACTION_MORNING_BRIEF = "com.example.ACTION_MORNING_BRIEF"
        const val ACTION_EVENING_REVIEW = "com.example.ACTION_EVENING_REVIEW"

        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)

        val db = AnchorDatabase.getDatabase(context)
        val repo = AnchorRepository(db.anchorDao(), GeminiService())

        when (action) {
            ACTION_DONE -> {
                if (taskId != -1L) {
                    NotificationManagerCompat.from(context).cancel(taskId.toInt())
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val item = repo.getItem(taskId)
                            if (item != null) {
                                repo.markCompleted(item)
                            }
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
            ACTION_SNOOZE -> {
                if (taskId != -1L) {
                    NotificationManagerCompat.from(context).cancel(taskId.toInt())
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val item = repo.getItem(taskId)
                            if (item != null) {
                                repo.snooze(item, hours = 1)
                            }
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
            ACTION_TASK_REMINDER -> {
                if (taskId != -1L) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val item = repo.getItem(taskId)
                            if (item != null && item.status != "DONE") {
                                val escalation = if (item.snoozeCount >= 3) 2 else if (item.snoozeCount > 0) 1 else 0
                                NotificationHelper.showTaskReminder(context, item.id, item.title, escalationLevel = escalation)
                            }
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
            ACTION_MORNING_BRIEF -> {
                NotificationHelper.showDailyBriefNotification(
                    context,
                    isMorning = true,
                    headline = "Ready to start your day? Tap to review your top 3 core anchors."
                )
                // Reschedule for next day
                val prefs = AnchorPreferences(context).getSettings()
                AnchorAlarmScheduler.scheduleDailyBriefs(context, prefs.morningTime, prefs.eveningTime)
            }
            ACTION_EVENING_REVIEW -> {
                NotificationHelper.showDailyBriefNotification(
                    context,
                    isMorning = false,
                    headline = "Time to wind down. Review what got done and clear your mind."
                )
                // Reschedule for next day
                val prefs = AnchorPreferences(context).getSettings()
                AnchorAlarmScheduler.scheduleDailyBriefs(context, prefs.morningTime, prefs.eveningTime)
            }
        }
    }
}
