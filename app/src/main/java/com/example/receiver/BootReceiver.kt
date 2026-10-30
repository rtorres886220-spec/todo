package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AnchorDatabase
import com.example.data.AnchorPreferences
import com.example.notification.AnchorAlarmScheduler
import com.example.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            NotificationHelper.createChannels(context)

            val prefs = AnchorPreferences(context).getSettings()
            AnchorAlarmScheduler.scheduleDailyBriefs(context, prefs.morningTime, prefs.eveningTime)

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AnchorDatabase.getDatabase(context)
                    val activeTasks = db.anchorDao().getAllActiveTasksSync()
                    for (task in activeTasks) {
                        if (task.dueTime != null || task.snoozeUntil != null) {
                            AnchorAlarmScheduler.scheduleTaskReminder(context, task)
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
