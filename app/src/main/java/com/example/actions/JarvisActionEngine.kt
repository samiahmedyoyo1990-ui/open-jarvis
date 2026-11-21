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
        val text = userPrompt.lowercase(Locale.ROOT).trim()
        if (text.isBlank()) return null

        // Only execute app launches for explicit commands; mentioning an app in a question
        // must never trigger an unexpected side effect.
        val launchCommand = listOf("open ", "launch ", "start ", "চালু কর", "খোলো", "ওপেন কর")
            .any { text.startsWith(it) || text.contains(" $it") }
        if (launchCommand) {
            when {
                text.contains("youtube") || text.contains("ইউটিউব") -> return Pair(
                    JarvisAction.OpenApp("YouTube", "com.google.android.youtube", "https://www.youtube.com"),
                    "অবশ্যই, ইউটিউব ওপেন করছি।"
                )
                text.contains("chrome") || text.contains("browser") || text.contains("ব্রাউজার") -> return Pair(
                    JarvisAction.OpenApp("Google Chrome", "com.android.chrome", "https://www.google.com"),
                    "ব্রাউজার ওপেন করছি।"
                )
                text.contains("whatsapp") || text.contains("হোয়াটসঅ্যাপ") || text.contains("হোয়াটসঅ্যাপ") -> return Pair(
                    JarvisAction.OpenApp("WhatsApp", "com.whatsapp"),
                    "হোয়াটসঅ্যাপ ওপেন করছি।"
                )
                text.contains("camera") || text.contains("ক্যামেরা") -> return Pair(
                    JarvisAction.OpenApp("Camera", null), "ক্যামেরা চালু করছি।"
                )
                text.contains("calculator") || text.contains("ক্যালকুলেটর") -> return Pair(
                    JarvisAction.OpenApp("Calculator", "com.google.android.calculator"), "ক্যালকুলেটর ওপেন করছি।"
                )
                text.contains("clock") || text.contains("alarm") || text.contains("ঘড়ি") || text.contains("অ্যালার্ম") -> return Pair(
                    JarvisAction.OpenApp("Clock", "com.google.android.deskclock"), "ঘড়ি এবং অ্যালার্ম স্ক্রিন ওপেন করছি।"
                )
            }
        }

        if (text.contains("wifi") || text.contains("ওয়াইফাই")) {
            return Pair(JarvisAction.OpenSettings("wifi"), "ওয়াইফাই সেটিংস ওপেন করছি।")
        }
        if (text.contains("bluetooth") || text.contains("ব্লুটুথ")) {
            return Pair(JarvisAction.OpenSettings("bluetooth"), "ব্লুটুথ সেটিংস ওপেন করছি।")
        }
        if (text.contains("setting") || text.contains("সেটিংস")) {
            return Pair(JarvisAction.OpenSettings("general"), "সিস্টেম সেটিংস ওপেন করছি।")
        }
        if (text.contains("dialer") || text.contains("ডায়ালার") || text.contains("কল কর") || text.contains("ফোন কর")) {
            return Pair(JarvisAction.OpenDialer(null), "ফোন ডায়ালার ওপেন করছি।")
        }
        if (text.contains("battery") || text.contains("ব্যাটারি") || text.contains("device info") || text.contains("ডিভাইস")) {
            return Pair(JarvisAction.GetDeviceInfo, "আপনার ডিভাইসের বর্তমান তথ্য তুলে ধরছি।")
        }
        if (text.startsWith("search ") || text.startsWith("খোঁজো ") || text.startsWith("সার্চ ")) {
            val query = text.removePrefix("search ").removePrefix("খোঁজো ").removePrefix("সার্চ ").trim()
            if (query.isNotBlank()) return Pair(JarvisAction.WebSearch(query), "\"$query\" লিখে গুগলে সার্চ করছি।")
        }

        val expression = text
            .removePrefix("calculate ")
            .removePrefix("ক্যালকুলেট ")
            .removePrefix("হিসাব কর ")
            .trim()
        // Restrict the parser to arithmetic characters and whitespace. This prevents a
        // conversational sentence containing numbers from being interpreted as a command.
        if (expression.isNotBlank() && expression.length <= 120 &&
            expression.any { it in "+-*/()" } && expression.all { it.isDigit() || it in ".,+-*/() \t" }) {
            val normalized = expression.replace(',', '.')
            val parseAttempt = runCatching { ArithmeticParser(normalized).parse() }
            val result = parseAttempt.getOrNull()
            if (result != null && result.isFinite()) {
                val clean = if (result % 1.0 == 0.0 && result >= Long.MIN_VALUE && result <= Long.MAX_VALUE) {
                    result.toLong().toString()
                } else {
                    "%.6f".format(Locale.US, result).trimEnd('0').trimEnd('.')
                }
                return Pair(JarvisAction.Calculation(normalized, clean), "$normalized এর ফলাফল হলো $clean")
            }
            if (parseAttempt.exceptionOrNull()?.message == "Division by zero") {
                return Pair(JarvisAction.Calculation(normalized, "Undefined"), "শূন্য দিয়ে ভাগ করা সম্ভব নয়।")
            }
        }
        return null
    }

    /** Small recursive-descent parser: parentheses, unary signs and operator precedence. */
    private class ArithmeticParser(private val source: String) {
        private var position = 0

        fun parse(): Double {
            val value = parseExpression()
            skipSpaces()
            require(position == source.length) { "Unexpected character" }
            return value
        }

        private fun parseExpression(): Double {
            var value = parseTerm()
            while (true) {
                skipSpaces()
                value = when {
                    consume('+') -> value + parseTerm()
                    consume('-') -> value - parseTerm()
                    else -> return value
                }
            }
        }

        private fun parseTerm(): Double {
            var value = parseUnary()
            while (true) {
                skipSpaces()
                value = when {
                    consume('*') -> value * parseUnary()
                    consume('/') -> {
                        val divisor = parseUnary()
                        require(divisor != 0.0) { "Division by zero" }
                        value / divisor
                    }
                    else -> return value
                }
            }
        }

        private fun parseUnary(): Double {
            skipSpaces()
            return when {
                consume('+') -> parseUnary()
                consume('-') -> -parseUnary()
                consume('(') -> parseExpression().also {
                    skipSpaces()
                    require(consume(')')) { "Missing closing parenthesis" }
                }
                else -> parseNumber()
            }
        }

        private fun parseNumber(): Double {
            skipSpaces()
            val start = position
            var decimalSeen = false
            while (position < source.length) {
                val char = source[position]
                if (char.isDigit()) position++
                else if (char == '.' && !decimalSeen) { decimalSeen = true; position++ }
                else break
            }
            require(position > start) { "Number expected" }
            return source.substring(start, position).toDouble()
        }

        private fun consume(char: Char): Boolean {
            if (position < source.length && source[position] == char) { position++; return true }
            return false
        }

        private fun skipSpaces() { while (position < source.length && source[position].isWhitespace()) position++ }
    }

}
