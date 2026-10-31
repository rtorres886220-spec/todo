package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.AnchorDatabase
import com.example.data.AnchorPreferences
import com.example.notification.AnchorAlarmScheduler
import com.example.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            NotificationHelper.createChannels(context)

            val prefs = AnchorPreferences.get(context).getSettings()
            AnchorAlarmScheduler.scheduleDailyBriefs(context, prefs.morningTime, prefs.eveningTime)

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AnchorDatabase.getDatabase(context)
                    val activeTasks = db.anchorDao().getAllActiveTasksSync()
                    var scheduledCount = 0
                    for (task in activeTasks) {
                        // Restore all active tasks with future deadlines, including date-only tasks (which use 09:00 default)
                        if (task.dueDate != null || task.snoozeUntil != null) {
                            AnchorAlarmScheduler.scheduleTaskReminder(context, task)
                            scheduledCount++
                        }
                    }
                    Log.d(TAG, "Restored $scheduledCount task reminders following device boot.")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore task alarms on boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
