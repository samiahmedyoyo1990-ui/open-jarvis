package com.example.notifications

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class JarvisNotificationItem(
    val key: String,
    val packageName: String,
    val appTitle: String,
    val title: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPriority: Boolean = false
)

class JarvisNotificationListenerService : NotificationListenerService() {

    private val TAG = "JarvisNotification"

    companion object {
        private val _notifications = MutableStateFlow<List<JarvisNotificationItem>>(emptyList())
        val notifications: StateFlow<List<JarvisNotificationItem>> = _notifications.asStateFlow()

        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

        fun isNotificationAccessGranted(context: Context): Boolean {
            val enabledListeners = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            val myPackage = context.packageName
            return enabledListeners?.contains(myPackage) == true
        }

        fun clearNotifications() {
            _notifications.value = emptyList()
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isServiceConnected.value = true
        Log.d(TAG, "Jarvis Notification Listener Connected")
        readActiveNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isServiceConnected.value = false
        Log.d(TAG, "Jarvis Notification Listener Disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        processNotification(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (sbn == null) return
        _notifications.value = _notifications.value.filter { it.key != sbn.key }
    }

    private fun readActiveNotifications() {
        try {
            val active = activeNotifications ?: return
            val items = active.mapNotNull { extractNotificationItem(it) }
            _notifications.value = items.take(30)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading active notifications", e)
        }
    }

    private fun processNotification(sbn: StatusBarNotification) {
        val item = extractNotificationItem(sbn) ?: return
        val current = _notifications.value.filter { it.key != sbn.key }
        _notifications.value = (listOf(item) + current).take(30)
    }

    private fun extractNotificationItem(sbn: StatusBarNotification): JarvisNotificationItem? {
        val notification = sbn.notification ?: return null
        val extras = notification.extras ?: return null

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()

        if (title.isBlank() && text.isBlank()) return null
        // Skip ongoing persistent notifications
        if (sbn.isOngoing) return null

        val pm = packageManager
        val appName = try {
            val appInfo = pm.getApplicationInfo(sbn.packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            sbn.packageName
        }

        val isVip = sbn.packageName.contains("whatsapp") ||
                sbn.packageName.contains("messaging") ||
                sbn.packageName.contains("gm") ||
                title.contains("urgent", ignoreCase = true) ||
                title.contains("জরুরি")

        return JarvisNotificationItem(
            key = sbn.key,
            packageName = sbn.packageName,
            appTitle = appName,
            title = title,
            text = text,
            timestamp = sbn.postTime,
            isPriority = isVip
        )
    }
}
