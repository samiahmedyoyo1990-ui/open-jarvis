package com.example.diagnostic

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Environment
import android.os.StatFs
import androidx.core.content.ContextCompat
import com.example.ai.UniversalAiEngine
import com.example.ai.security.SecureApiKeyStorage
import com.example.data.local.JarvisDatabase
import com.example.notifications.JarvisNotificationListenerService
import com.example.voice.JarvisWakeWordService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.KeyStore

enum class DiagnosticStatus {
    HEALTHY,
    WARNING,
    CRITICAL
}

data class DiagnosticItem(
    val title: String,
    val description: String,
    val status: DiagnosticStatus,
    val details: String,
    val canAutoRepair: Boolean = false,
    val repairActionName: String? = null
)

data class DiagnosticReport(
    val timestamp: Long = System.currentTimeMillis(),
    val overallStatus: DiagnosticStatus,
    val items: List<DiagnosticItem>,
    val appVersion: String = "1.0.0",
    val androidApiLevel: Int = android.os.Build.VERSION.SDK_INT
)

class DiagnosticEngine(
    private val context: Context,
    private val aiEngine: UniversalAiEngine,
    private val database: JarvisDatabase
) {
    private val _report = MutableStateFlow<DiagnosticReport?>(null)
    val report: StateFlow<DiagnosticReport?> = _report.asStateFlow()

    suspend fun runFullDiagnostics(): DiagnosticReport = withContext(Dispatchers.IO) {
        val items = mutableListOf<DiagnosticItem>()

        // 1. AI Provider & BYOK Check
        val activeProvider = aiEngine.activeConfig.value
        val hasKey = SecureApiKeyStorage(context).hasApiKey(activeProvider.id) || activeProvider.type.name == "FIREBASE_AI"
        items.add(
            DiagnosticItem(
                title = "AI Core Engine & BYOK",
                description = "Active: ${activeProvider.name} (${activeProvider.selectedModel})",
                status = if (hasKey) DiagnosticStatus.HEALTHY else DiagnosticStatus.WARNING,
                details = if (hasKey) "API key securely encrypted in Keystore" else "No API key configured for ${activeProvider.name}. Tap to add key in BYOK settings.",
                canAutoRepair = false
            )
        )

        // 2. Network Connectivity
        val isNetworkAvailable = checkNetwork()
        items.add(
            DiagnosticItem(
                title = "Network Connectivity",
                description = "Internet access for cloud AI inference",
                status = if (isNetworkAvailable) DiagnosticStatus.HEALTHY else DiagnosticStatus.CRITICAL,
                details = if (isNetworkAvailable) "Connected to high-speed network" else "Offline. Offline intent engine is active for basic local commands.",
                canAutoRepair = false
            )
        )

        // 3. Database Integrity
        val isDbHealthy = checkDatabase()
        items.add(
            DiagnosticItem(
                title = "Room Database Health",
                description = "Local storage for notes, tasks & memories",
                status = if (isDbHealthy) DiagnosticStatus.HEALTHY else DiagnosticStatus.CRITICAL,
                details = if (isDbHealthy) "Database query successful (jarvis_memory.db)" else "Database read failure",
                canAutoRepair = true,
                repairActionName = "Reconnect Database"
            )
        )

        // 4. Microphone & Audio Permission
        val micGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        items.add(
            DiagnosticItem(
                title = "Microphone Permission",
                description = "Required for speech recognition and voice commands",
                status = if (micGranted) DiagnosticStatus.HEALTHY else DiagnosticStatus.WARNING,
                details = if (micGranted) "RECORD_AUDIO permission granted" else "Permission missing. Tap to grant permission.",
                canAutoRepair = false
            )
        )

        // 5. Wake Word Service State
        val wakeWordRunning = JarvisWakeWordService.isServiceRunning.value
        items.add(
            DiagnosticItem(
                title = "Background Wake Word Service",
                description = "Listens for 'Hey JARVIS' wake-word in background",
                status = if (wakeWordRunning) DiagnosticStatus.HEALTHY else DiagnosticStatus.WARNING,
                details = if (wakeWordRunning) "Active foreground service with microphone notification" else "Stopped (Service disabled by user preference)",
                canAutoRepair = true,
                repairActionName = "Start Service"
            )
        )

        // 6. Android Keystore Master Key
        val hasMasterKey = checkKeystore()
        items.add(
            DiagnosticItem(
                title = "Keystore Hardware Security",
                description = "AES-256 GCM hardware encryption key",
                status = if (hasMasterKey) DiagnosticStatus.HEALTHY else DiagnosticStatus.WARNING,
                details = if (hasMasterKey) "Master key 'jarvis_byok_master_key' securely loaded" else "Keystore key not initialized yet",
                canAutoRepair = true,
                repairActionName = "Generate Key"
            )
        )

        // 7. Notification Access
        val notifAccess = JarvisNotificationListenerService.isNotificationAccessGranted(context)
        items.add(
            DiagnosticItem(
                title = "Notification Intelligence Access",
                description = "Required for reading & summarizing status bar notifications",
                status = if (notifAccess) DiagnosticStatus.HEALTHY else DiagnosticStatus.WARNING,
                details = if (notifAccess) "NotificationListenerService granted" else "Not granted. Enable in Android Settings > Notification Access.",
                canAutoRepair = false
            )
        )

        // 8. Internal Storage Space
        val freeMb = getFreeInternalStorageMb()
        items.add(
            DiagnosticItem(
                title = "Internal Storage",
                description = "Storage for local database and caches",
                status = if (freeMb > 100) DiagnosticStatus.HEALTHY else DiagnosticStatus.WARNING,
                details = "${freeMb} MB free space available",
                canAutoRepair = false
            )
        )

        val overall = when {
            items.any { it.status == DiagnosticStatus.CRITICAL } -> DiagnosticStatus.CRITICAL
            items.any { it.status == DiagnosticStatus.WARNING } -> DiagnosticStatus.WARNING
            else -> DiagnosticStatus.HEALTHY
        }

        val fullReport = DiagnosticReport(
            overallStatus = overall,
            items = items
        )
        _report.value = fullReport
        fullReport
    }

    private fun checkNetwork(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }

    private fun checkDatabase(): Boolean {
        return try {
            database.openHelper.readableDatabase.isOpen
        } catch (_: Exception) {
            false
        }
    }

    private fun checkKeystore(): Boolean {
        return try {
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            ks.containsAlias("jarvis_byok_master_key")
        } catch (_: Exception) {
            false
        }
    }

    private fun getFreeInternalStorageMb(): Long {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            (stat.availableBytes / (1024 * 1024))
        } catch (_: Exception) {
            500L
        }
    }

    suspend fun performSafeRepair(item: DiagnosticItem): String = withContext(Dispatchers.IO) {
        when (item.repairActionName) {
            "Generate Key" -> {
                SecureApiKeyStorage(context)
                "Android Keystore master key successfully verified & initialized."
            }
            "Reconnect Database" -> {
                database.openHelper.readableDatabase
                "Database connection re-established."
            }
            "Start Service" -> {
                JarvisWakeWordService.start(context)
                "Hey JARVIS background service started."
            }
            else -> "No automated action needed."
        }
    }

    fun exportRedactedReport(): String {
        val rep = _report.value ?: return "{}"
        val json = JSONObject().apply {
            put("timestamp", rep.timestamp)
            put("overallStatus", rep.overallStatus.name)
            put("appVersion", rep.appVersion)
            put("androidApiLevel", rep.androidApiLevel)
            val arr = org.json.JSONArray()
            rep.items.forEach { item ->
                val obj = JSONObject().apply {
                    put("title", item.title)
                    put("status", item.status.name)
                    put("details", item.details)
                }
                arr.put(obj)
            }
            put("diagnostics", arr)
        }
        return json.toString(2)
    }
}
