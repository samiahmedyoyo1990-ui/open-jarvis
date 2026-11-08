package com.example.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.actions.JarvisAction
import com.example.actions.JarvisActionEngine
import com.example.data.local.entity.ConversationEntity
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

data class AiAssistantResponse(
    val replyText: String,
    val action: JarvisAction = JarvisAction.None
)

interface AiProvider {
    val name: String
    suspend fun processQuery(
        prompt: String,
        recentHistory: List<ConversationEntity>,
        userName: String
    ): AiAssistantResponse
}

class FirebaseGeminiProvider : AiProvider {

    override val name: String = "Google Gemini 3.8 Flash (Firebase AI)"
    private val TAG = "FirebaseGeminiProvider"

    // Model initialized lazily to avoid class construction overhead or pre-AppCheck issues
    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    """
                    You are JARVIS (Just A Rather Very Intelligent System), a cutting-edge personal AI assistant operating on Android.
                    Your personality is intelligent, polite, loyal, sharp, futuristic, and helpful.
                    
                    CRITICAL COMMUNICATION GUIDELINES:
                    1. The user communicates in Bangla (বাংলা), Romanized Bangla (Banglish like 'amar jonno ekta note save koro'), or English.
                    2. By default, ALWAYS reply in clear, natural, respectful, and standard Bangla (বাংলা).
                    3. Address the user with respect (e.g., 'স্যার' or 'বস' or their specified name).
                    4. Keep replies crisp, spoken-friendly, and to the point.
                    5. Avoid repeating questions unnecessarily.
                    
                    ANDROID ACTIONS ENGINE INTEGRATION:
                    If the user's intent is to perform a phone or device task, include an action tag at the very end of your response in one of these exact formats:
                    - Open YouTube: <<ACTION:OPEN_APP:YouTube:com.google.android.youtube>>
                    - Open Chrome/Browser: <<ACTION:OPEN_APP:Browser:com.android.chrome>>
                    - Open WhatsApp: <<ACTION:OPEN_APP:WhatsApp:com.whatsapp>>
                    - Open Camera: <<ACTION:OPEN_APP:Camera>>
                    - Open Calculator: <<ACTION:OPEN_APP:Calculator:com.google.android.calculator>>
                    - Open Settings: <<ACTION:OPEN_SETTINGS:general>> (or wifi, bluetooth, display, sound)
                    - Open Phone Dialer: <<ACTION:OPEN_DIALER>>
                    - Save Note: <<ACTION:SAVE_NOTE:NoteTitle:NoteContent>>
                    - Add Task: <<ACTION:ADD_TASK:TaskTitle>>
                    - Search Web: <<ACTION:WEB_SEARCH:SearchQuery>>
                    - Device Status: <<ACTION:DEVICE_INFO>>
                    
                    Example:
                    User: "Jarvis, YouTube open koro"
                    JARVIS: "অবশ্যই স্যার, ইউটিউব ওপেন করছি। <<ACTION:OPEN_APP:YouTube:com.google.android.youtube>>"
                    
                    User: "Amar jonno ekta note likho bazar er list"
                    JARVIS: "ঠিক আছে, নোটটি তৈরি করে রেখেছি। <<ACTION:SAVE_NOTE:বাজারের তালিকা:বাজারের প্রয়োজনীয় জিনিসপত্র>>"
                    
                    If the query is a conversational or informational question, answer intelligently in Bangla without an action tag.
                    """.trimIndent()
                )
            }
        )
    }

    override suspend fun processQuery(
        prompt: String,
        recentHistory: List<ConversationEntity>,
        userName: String
    ): AiAssistantResponse {
        val historyContent: List<Content> = recentHistory.takeLast(6).map { item: ConversationEntity ->
            content(if (item.isUser) "user" else "model") {
                text(item.text)
            }
        }

        // Start chat with multi-turn history
        val chat = model.startChat(history = historyContent)
        val rawResponse = withTimeout(60_000L) {
            chat.sendMessage("User ($userName): $prompt")
        }

        val fullText = rawResponse.text?.trim().orEmpty()
        return parseResponseAndAction(fullText)
    }

    private fun parseResponseAndAction(rawText: String): AiAssistantResponse {
        val actionRegex = Regex("""<<ACTION:(.+?)>>""")
        val match = actionRegex.find(rawText)

        if (match == null) {
            return AiAssistantResponse(replyText = rawText, action = JarvisAction.None)
        }

        val actionPayload = match.groupValues[1]
        val cleanReply = rawText.replace(match.value, "").trim()

        val parts = actionPayload.split(":")
        val actionType = parts.getOrNull(0)?.uppercase() ?: ""

        val action: JarvisAction = when (actionType) {
            "OPEN_APP" -> {
                val appName = parts.getOrNull(1) ?: "App"
                val pkg = parts.getOrNull(2)
                JarvisAction.OpenApp(appName, pkg)
            }
            "OPEN_SETTINGS" -> {
                val settingType = parts.getOrNull(1) ?: "general"
                JarvisAction.OpenSettings(settingType)
            }
            "OPEN_DIALER" -> JarvisAction.OpenDialer(parts.getOrNull(1))
            "WEB_SEARCH" -> {
                val query = parts.drop(1).joinToString(":")
                JarvisAction.WebSearch(query)
            }
            "SAVE_NOTE" -> {
                val title = parts.getOrNull(1) ?: "নোট"
                val content = parts.drop(2).joinToString(":").ifBlank { title }
                JarvisAction.SaveNote(title, content)
            }
            "ADD_TASK" -> {
                val title = parts.drop(1).joinToString(":").ifBlank { "নতুন কাজ" }
                JarvisAction.AddTask(title)
            }
            "DEVICE_INFO" -> JarvisAction.GetDeviceInfo
            else -> JarvisAction.None
        }

        return AiAssistantResponse(replyText = cleanReply, action = action)
    }
}

class AiBrainManager(
    private val context: Context,
    private val actionEngine: JarvisActionEngine
) {
    private val TAG = "AiBrainManager"
    val universalAiEngine = UniversalAiEngine(context)

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun askJarvis(
        userPrompt: String,
        recentHistory: List<ConversationEntity>,
        userName: String
    ): AiAssistantResponse {
        // 1. Check if user is offline
        if (!isOnline()) {
            val offlineMatch = actionEngine.parseOfflineIntent(userPrompt)
            return if (offlineMatch != null) {
                AiAssistantResponse(
                    replyText = "${offlineMatch.second} (অফলাইন মোড)",
                    action = offlineMatch.first
                )
            } else {
                AiAssistantResponse(
                    replyText = "বর্তমানে কোনো ইন্টারনেট সংযোগ নেই। অফলাইন মোডে আমি অ্যাপ ওপেন, নোট তৈরি এবং সাধারণ হিসাব করতে পারি।",
                    action = JarvisAction.None
                )
            }
        }

        // 2. Online processing with selected Universal AI Provider (BYOK)
        return try {
            universalAiEngine.processQuery(userPrompt, recentHistory, userName)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI processing timed out", e)
            val offlineFallback = actionEngine.parseOfflineIntent(userPrompt)
            if (offlineFallback != null) {
                AiAssistantResponse(
                    replyText = "অনলাইন রেসপন্স টাইমআউট হয়েছে, তাই অফলাইন কমান্ড হিসেবে সম্পন্ন করছি: ${offlineFallback.second}",
                    action = offlineFallback.first
                )
            } else {
                AiAssistantResponse(
                    replyText = "অনুরোধটি সম্পন্ন করতে বেশি সময় লেগেছে। অনুগ্রহ করে আবার চেষ্টা করুন।",
                    action = JarvisAction.None
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI inference failed: ${e.message}", e)
            // Attempt offline fallback if local intent matched
            val offlineFallback = actionEngine.parseOfflineIntent(userPrompt)
            if (offlineFallback != null) {
                AiAssistantResponse(
                    replyText = "${offlineFallback.second} (অফলাইন সমাধান)",
                    action = offlineFallback.first
                )
            } else {
                AiAssistantResponse(
                    replyText = "দুঃখিত, সংযোগে ত্রুটি হয়েছে: ${e.localizedMessage ?: "অজানা সমস্যা"}। অফলাইন কমান্ড দিয়ে চেষ্টা করুন।",
                    action = JarvisAction.None
                )
            }
        }
    }
}
