package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/** Schedules durable task reminders using AlarmManager. */
object ReminderScheduler {
    const val EXTRA_TASK_ID = "com.example.notifications.extra.TASK_ID"
    const val EXTRA_TITLE = "com.example.notifications.extra.TITLE"
    const val EXTRA_PRIORITY = "com.example.notifications.extra.PRIORITY"
    private const val ACTION_REMINDER = "com.example.notifications.action.TASK_REMINDER"

    fun scheduleReminder(
        context: Context,
        taskId: Long,
        title: String,
        triggerAtMillis: Long,
        priority: String = "Normal",
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (taskId <= 0L || title.isBlank() || triggerAtMillis <= nowMillis) return false
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        val pendingIntent = reminderPendingIntent(context, taskId, title, priority) ?: return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Avoid the special exact-alarm permission; reminders may be delivered slightly late.
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
            true
        } catch (_: SecurityException) {
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                true
            } catch (_: RuntimeException) {
                false
            }
        } catch (_: RuntimeException) {
            false
        }
    }

    fun cancelReminder(context: Context, taskId: Long) {
        if (taskId <= 0L) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply { action = ACTION_REMINDER }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode(taskId),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun reminderPendingIntent(
        context: Context,
        taskId: Long,
        title: String,
        priority: String
    ): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_PRIORITY, priority)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(taskId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun requestCode(taskId: Long): Int = (taskId xor (taskId ushr 32)).toInt()
}
