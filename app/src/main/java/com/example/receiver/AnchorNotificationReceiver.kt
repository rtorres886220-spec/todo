package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.ai.GeminiService
import com.example.data.AnchorDatabase
import com.example.data.AnchorRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AnchorNotificationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DONE = "com.example.ACTION_DONE"
        const val ACTION_SNOOZE = "com.example.ACTION_SNOOZE"
        const val EXTRA_TASK_ID = "extra_task_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return

        // Dismiss the notification
        NotificationManagerCompat.from(context).cancel(taskId.toInt())

        val db = AnchorDatabase.getDatabase(context)
        val repo = AnchorRepository(db.anchorDao(), GeminiService())

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val item = repo.getItem(taskId)
                if (item != null) {
                    when (intent.action) {
                        ACTION_DONE -> {
                            repo.markCompleted(item)
                        }
                        ACTION_SNOOZE -> {
                            // Snooze by 10 minutes or 1 hour
                            repo.snooze(item, hours = 1)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
