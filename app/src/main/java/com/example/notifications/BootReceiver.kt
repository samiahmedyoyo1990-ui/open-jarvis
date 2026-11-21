package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.JarvisDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Restores pending alarms after reboot or an application update. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val tasks = JarvisDatabase.getInstance(appContext)
                    .taskDao()
                    .getPendingScheduledTasksSnapshot()
                val now = System.currentTimeMillis()
                tasks.forEach { task ->
                    val dueAt = task.dueDate ?: return@forEach
                    if (dueAt > now) {
                        ReminderScheduler.scheduleReminder(
                            context = appContext,
                            taskId = task.id,
                            title = task.title,
                            triggerAtMillis = dueAt,
                            priority = task.priority,
                            nowMillis = now
                        )
                    }
                }
            } catch (error: Exception) {
                Log.e("JARVIS.BootReceiver", "Failed to restore pending reminders", error)
            } finally {
                pendingResult.finish()
                scope.cancel()
            }
        }
    }
}
