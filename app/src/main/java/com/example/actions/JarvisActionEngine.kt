package com.example.actions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface JarvisAction {
    data class OpenApp(val appName: String, val packageName: String?, val fallbackUrl: String? = null) : JarvisAction
    data class OpenSettings(val settingType: String) : JarvisAction
    data class WebSearch(val query: String) : JarvisAction
    data class SaveNote(val title: String, val content: String) : JarvisAction
    data class AddTask(val title: String, val priority: String = "Normal") : JarvisAction
    data class OpenDialer(val phoneNumber: String? = null) : JarvisAction
    data class ShareContent(val text: String) : JarvisAction
    data object GetDeviceInfo : JarvisAction
    data class Calculation(val expression: String, val result: String) : JarvisAction
    data object None : JarvisAction
}

data class ExecutionResult(
    val success: Boolean,
    val feedbackMessage: String,
    val actionType: String
)

class JarvisActionEngine(private val context: Context) {

    private val TAG = "JarvisActionEngine"

    fun executeAction(action: JarvisAction): ExecutionResult {
        return try {
            when (action) {
                is JarvisAction.OpenApp -> openApplication(action)
                is JarvisAction.OpenSettings -> openSettingsScreen(action.settingType)
                is JarvisAction.WebSearch -> searchWeb(action.query)
                is JarvisAction.OpenDialer -> openPhoneDialer(action.phoneNumber)
                is JarvisAction.ShareContent -> shareText(action.text)
                is JarvisAction.GetDeviceInfo -> retrieveDeviceInfo()
                is JarvisAction.Calculation -> ExecutionResult(
                    success = true,
                    feedbackMessage = "${action.expression} এর ফলাফল হলো ${action.result}",
                    actionType = "Calculation"
                )
                is JarvisAction.SaveNote -> ExecutionResult(
                    success = true,
                    feedbackMessage = "নোটটি সফলভাবে সংরক্ষিত হয়েছে।",
                    actionType = "SaveNote"
                )
                is JarvisAction.AddTask -> ExecutionResult(
                    success = true,
                    feedbackMessage = "কাজটি আপনার টাস্ক লিস্টে যোগ করা হয়েছে।",
                    actionType = "AddTask"
                )
                is JarvisAction.None -> ExecutionResult(
                    success = true,
                    feedbackMessage = "",
                    actionType = "None"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing action $action", e)
            ExecutionResult(
                success = false,
                feedbackMessage = "দুঃখিত, অ্যাকশন সম্পন্ন করার সময় একটি সমস্যা হয়েছে: ${e.localizedMessage}",
                actionType = "Error"
            )
        }
    }

    private fun openApplication(action: JarvisAction.OpenApp): ExecutionResult {
        val pm = context.packageManager

        // Special handling for Camera
        if (action.appName.equals("Camera", ignoreCase = true) || action.appName.contains("ক্যামেরা")) {
            val cameraIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (cameraIntent.resolveActivity(pm) != null) {
                context.startActivity(cameraIntent)
                return ExecutionResult(true, "ক্যামেরা ওপেন করা হয়েছে।", "OpenApp:Camera")
            }
        }

        // Special handling for Clock / Alarm
        if (action.appName.equals("Clock", ignoreCase = true) || action.appName.contains("ঘড়ি") || action.appName.contains("অ্যালার্ম")) {
            val clockIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (clockIntent.resolveActivity(pm) != null) {
                context.startActivity(clockIntent)
                return ExecutionResult(true, "ঘড়ি/অ্যালার্ম ওপেন করা হয়েছে।", "OpenApp:Clock")
            }
        }

        // Try direct launch by package name
        action.packageName?.let { pkg ->
            val launchIntent = pm.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return ExecutionResult(true, "${action.appName} ওপেন করা হচ্ছে...", "OpenApp:$pkg")
            }
        }

        // If package not found, attempt fallback URL (e.g. YouTube web)
        if (!action.fallbackUrl.isNullOrBlank()) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(action.fallbackUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            return ExecutionResult(true, "${action.appName} অ্যাপ ডিভাইসে নেই, ব্রাউজারে ওপেন করা হচ্ছে।", "OpenApp:FallbackWeb")
        }

        return ExecutionResult(
            success = false,
            feedbackMessage = "দুঃখিত, ${action.appName} অ্যাপটি আপনার ডিভাইসে ইনস্টল করা নেই।",
            actionType = "AppNotFound"
        )
    }

    private fun openSettingsScreen(settingType: String): ExecutionResult {
        val intent = when (settingType.lowercase()) {
            "wifi", "ওয়াইফাই" -> Intent(Settings.ACTION_WIFI_SETTINGS)
            "bluetooth", "ব্লুটুথ" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            "display", "ডিসপ্লে" -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
            "sound", "সাউন্ড" -> Intent(Settings.ACTION_SOUND_SETTINGS)
            "apps", "অ্যাপস" -> Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
            else -> Intent(Settings.ACTION_SETTINGS)
        }.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
        return ExecutionResult(true, "সিস্টেম সেটিংস খোলা হয়েছে।", "OpenSettings:$settingType")
    }

    private fun searchWeb(query: String): ExecutionResult {
        val cleanQuery = query.trim()
        val searchUri = Uri.parse("https://www.google.com/search?q=" + Uri.encode(cleanQuery))
        val intent = Intent(Intent.ACTION_VIEW, searchUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return ExecutionResult(true, "গুগলে \"$cleanQuery\" সার্চ করা হচ্ছে...", "WebSearch")
    }

    private fun openPhoneDialer(phoneNumber: String?): ExecutionResult {
        val uri = if (phoneNumber.isNullOrBlank()) {
            Uri.parse("tel:")
        } else {
            Uri.parse("tel:${phoneNumber.trim()}")
        }
        val intent = Intent(Intent.ACTION_DIAL, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return ExecutionResult(true, "ফোন ডায়ালার ওপেন করা হয়েছে।", "OpenDialer")
    }

    private fun shareText(text: String): ExecutionResult {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "শেয়ার করুন").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        return ExecutionResult(true, "শেয়ার ডায়লগ ওপেন করা হয়েছে।", "ShareContent")
    }

    fun retrieveDeviceInfo(): ExecutionResult {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryLevel = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1

        val model = Build.MODEL
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val androidVersion = Build.VERSION.RELEASE

        val stat = StatFs(Environment.getDataDirectory().path)
        val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
        val availableGb = availableBytes / (1024 * 1024 * 1024)

        val dateFormat = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale("bn", "BD"))
        val currentFormattedTime = dateFormat.format(Date())

        val infoString = """
            ডিভাইস: $manufacturer $model
            অ্যান্ড্রয়েড সংস্করণ: $androidVersion
            ব্যাটারি চার্জ: ${if (batteryLevel >= 0) "$batteryLevel%" else "অজানা"}
            ফ্রি স্টোরেজ: প্রায় $availableGb GB
            বর্তমান সময় ও তারিখ: $currentFormattedTime
        """.trimIndent()

        return ExecutionResult(
            success = true,
            feedbackMessage = infoString,
            actionType = "DeviceInfo"
        )
    }

    /**
     * Local Offline Intent Classifier for instantaneous response when offline
     */
    fun parseOfflineIntent(userPrompt: String): Pair<JarvisAction, String>? {
        val text = userPrompt.lowercase().trim()

        // YouTube
        if (text.contains("youtube") || text.contains("ইউটিউব")) {
            return Pair(
                JarvisAction.OpenApp("YouTube", "com.google.android.youtube", "https://www.youtube.com"),
                "অবশ্যই, ইউটিউব ওপেন করছি।"
            )
        }

        // Chrome / Browser
        if (text.contains("chrome") || text.contains("browser") || text.contains("ব্রাউজার") || text.contains("ইন্টারনেট")) {
            return Pair(
                JarvisAction.OpenApp("Google Chrome", "com.android.chrome", "https://www.google.com"),
                "ব্রাউজার ওপেন করছি।"
            )
        }

        // WhatsApp
        if (text.contains("whatsapp") || text.contains("হোয়াটসঅ্যাপ") || text.contains("হোয়াটসঅ্যাপ")) {
            return Pair(
                JarvisAction.OpenApp("WhatsApp", "com.whatsapp"),
                "হোয়াটসঅ্যাপ ওপেন করছি।"
            )
        }

        // Camera
        if (text.contains("camera") || text.contains("ক্যামেরা") || text.contains("ছবি তোল")) {
            return Pair(
                JarvisAction.OpenApp("Camera", null),
                "ক্যামেরা চালু করছি।"
            )
        }

        // Calculator
        if (text.contains("calculator") || text.contains("ক্যালকুলেটর") || text.contains("হিসাব কর")) {
            return Pair(
                JarvisAction.OpenApp("Calculator", "com.google.android.calculator"),
                "ক্যালকুলেটর ওপেন করছি।"
            )
        }

        // Clock / Alarm
        if (text.contains("alarm") || text.contains("clock") || text.contains("অ্যালার্ম") || text.contains("ঘড়ি")) {
            return Pair(
                JarvisAction.OpenApp("Clock", "com.google.android.deskclock"),
                "ঘড়ি এবং অ্যালার্ম স্ক্রিন ওপেন করছি।"
            )
        }

        // Settings
        if (text.contains("wifi") || text.contains("ওয়াইফাই")) {
            return Pair(JarvisAction.OpenSettings("wifi"), "ওয়াইফাই সেটিংস ওপেন করছি।")
        }
        if (text.contains("bluetooth") || text.contains("ব্লুটুথ")) {
            return Pair(JarvisAction.OpenSettings("bluetooth"), "ব্লুটুথ সেটিংস ওপেন করছি।")
        }
        if (text.contains("setting") || text.contains("সেটিংস")) {
            return Pair(JarvisAction.OpenSettings("general"), "সিস্টেম সেটিংস ওপেন করছি।")
        }

        // Phone Dialer
        if (text.contains("dialer") || text.contains("ডায়ালার") || text.contains("কল কর") || text.contains("ফোন কর")) {
            return Pair(JarvisAction.OpenDialer(null), "ফোন ডায়ালার ওপেন করছি।")
        }

        // Device Info / Battery
        if (text.contains("battery") || text.contains("ব্যাটারি") || text.contains("device info") || text.contains("ডিভাইস")) {
            return Pair(JarvisAction.GetDeviceInfo, "আপনার ডিভাইসের বর্তমান তথ্য তুলে ধরছি।")
        }

        // Google Search
        if (text.startsWith("search ") || text.startsWith("খোঁজো ") || text.startsWith("সার্চ ")) {
            val query = text.removePrefix("search ").removePrefix("খোঁজো ").removePrefix("সার্চ ")
            return Pair(JarvisAction.WebSearch(query), "\"$query\" লিখে গুগলে সার্চ করছি।")
        }

        // Simple arithmetic offline evaluation
        val calcMatch = Regex("""(\d+(?:\.\d+)?)\s*([\+\-\*\/])\s*(\d+(?:\.\d+)?)""").find(text)
        if (calcMatch != null) {
            val (n1Str, op, n2Str) = calcMatch.destructured
            val n1 = n1Str.toDoubleOrNull()
            val n2 = n2Str.toDoubleOrNull()
            if (n1 != null && n2 != null) {
                val res = when (op) {
                    "+" -> n1 + n2
                    "-" -> n1 - n2
                    "*" -> n1 * n2
                    "/" -> if (n2 != 0.0) n1 / n2 else Double.NaN
                    else -> null
                }
                if (res != null) {
                    val cleanRes = if (res % 1.0 == 0.0) res.toLong().toString() else "%.2f".format(res)
                    return Pair(
                        JarvisAction.Calculation("$n1 $op $n2", cleanRes),
                        "$n1 $op $n2 এর ফলাফল হলো $cleanRes"
                    )
                }
            }
        }

        return null
    }
}
