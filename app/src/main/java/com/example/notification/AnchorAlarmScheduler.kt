package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.AnchorItem
import com.example.receiver.AnchorNotificationReceiver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object AnchorAlarmScheduler {

    fun scheduleTaskReminder(context: Context, item: AnchorItem) {
        val targetMillis = calculateTaskTriggerMillis(item) ?: return
        if (targetMillis <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AnchorNotificationReceiver::class.java).apply {
            action = AnchorNotificationReceiver.ACTION_TASK_REMINDER
            putExtra(AnchorNotificationReceiver.EXTRA_TASK_ID, item.id)
            putExtra(AnchorNotificationReceiver.EXTRA_TASK_TITLE, item.title)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarm(alarmManager, targetMillis, pendingIntent)
    }

    fun cancelTaskReminder(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AnchorNotificationReceiver::class.java).apply {
            action = AnchorNotificationReceiver.ACTION_TASK_REMINDER
            putExtra(AnchorNotificationReceiver.EXTRA_TASK_ID, taskId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun scheduleDailyBriefs(context: Context, morningTime: String, eveningTime: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // Morning Brief
        val morningMillis = calculateNextDailyMillis(morningTime)
        val morningIntent = Intent(context, AnchorNotificationReceiver::class.java).apply {
            action = AnchorNotificationReceiver.ACTION_MORNING_BRIEF
        }
        val morningPending = PendingIntent.getBroadcast(
            context,
            88801,
            morningIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        scheduleAlarm(alarmManager, morningMillis, morningPending)

        // Evening Review
        val eveningMillis = calculateNextDailyMillis(eveningTime)
        val eveningIntent = Intent(context, AnchorNotificationReceiver::class.java).apply {
            action = AnchorNotificationReceiver.ACTION_EVENING_REVIEW
        }
        val eveningPending = PendingIntent.getBroadcast(
            context,
            88802,
            eveningIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        scheduleAlarm(alarmManager, eveningMillis, eveningPending)
    }

    private fun scheduleAlarm(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun calculateTaskTriggerMillis(item: AnchorItem): Long? {
        if (item.snoozeUntil != null && item.snoozeUntil > System.currentTimeMillis()) {
            return item.snoozeUntil
        }
        val dueDate = item.dueDate ?: return null
        val dueTime = item.dueTime ?: "09:00"

        return try {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val date = format.parse("$dueDate $dueTime")
            date?.time
        } catch (_: Exception) {
            null
        }
    }

    private fun calculateNextDailyMillis(timeStr: String): Long {
        val parts = timeStr.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }
}
