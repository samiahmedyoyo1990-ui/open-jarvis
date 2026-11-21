package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val taskId = intent.getLongExtra(ReminderScheduler.EXTRA_TASK_ID, -1L)
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE)?.takeIf { it.isNotBlank() }
            ?: return
        if (taskId <= 0L) return
        try {
            ReminderNotificationHelper.createNotificationChannel(context)
            val priority = intent.getStringExtra(ReminderScheduler.EXTRA_PRIORITY).orEmpty()
            val message = if (priority.equals("High", ignoreCase = true)) {
                "জরুরি কাজের রিমাইন্ডার"
            } else {
                "আপনার নির্ধারিত কাজের সময় হয়েছে।"
            }
            ReminderNotificationHelper.showReminderNotification(
                context = context,
                notificationId = (taskId xor (taskId ushr 32)).toInt(),
                title = title,
                content = message
            )
        } catch (error: RuntimeException) {
            Log.e("JARVIS.Reminder", "Unable to show reminder for task $taskId", error)
        }
    }
}
