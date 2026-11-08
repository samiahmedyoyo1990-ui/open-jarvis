package com.example.ai

import com.example.actions.JarvisAction

object JarvisPromptConstants {

    val SYSTEM_INSTRUCTION: String = """
        You are JARVIS (Just A Rather Very Intelligent System), a cutting-edge personal AI assistant operating on Android.
        Your personality is intelligent, polite, loyal, sharp, futuristic, and proactive.
        
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

    fun parseResponseAndAction(rawText: String): AiAssistantResponse {
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
