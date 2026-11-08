package com.example.ai.providers

import com.example.ai.AiAssistantResponse
import com.example.ai.JarvisPromptConstants
import com.example.ai.model.AiModelInfo
import com.example.ai.model.AiProviderConfig
import com.example.ai.model.AiProviderType
import com.example.ai.model.ConnectionTestResult
import com.example.data.local.entity.ConversationEntity
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class FirebaseAiProviderAdapter(
    override val config: AiProviderConfig = AiProviderConfig(
        id = "provider_firebase_builtin",
        type = AiProviderType.FIREBASE_AI,
        name = "Firebase AI Logic (Default)",
        baseUrl = "",
        selectedModel = "gemini-3.8-flash"
    )
) : UniversalAiProvider {

    override val id: String = config.id
    override val type: AiProviderType = AiProviderType.FIREBASE_AI

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = config.selectedModel.ifBlank { "gemini-3.8-flash" },
            systemInstruction = content {
                text(JarvisPromptConstants.SYSTEM_INSTRUCTION)
            }
        )
    }

    override suspend fun testConnection(apiKey: String): ConnectionTestResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val response = withTimeout(15_000L) {
                model.generateContent("Ping test. Reply with 'OK'.")
            }
            val latency = System.currentTimeMillis() - startTime
            val text = response.text?.trim().orEmpty()
            if (text.isNotBlank()) {
                ConnectionTestResult(
                    isSuccess = true,
                    statusCode = 200,
                    message = "Firebase AI Logic সফলভাবে সংযুক্ত! ($latency ms)",
                    latencyMs = latency,
                    discoveredModelCount = 1
                )
            } else {
                ConnectionTestResult(
                    isSuccess = false,
                    statusCode = 500,
                    message = "Firebase AI রেসপন্স পাওয়া যায়নি।"
                )
            }
        } catch (e: Exception) {
            ConnectionTestResult(
                isSuccess = false,
                statusCode = 0,
                message = "Firebase AI সংযোগ ত্রুটি: ${e.localizedMessage}"
            )
        }
    }

    override suspend fun fetchModels(apiKey: String): List<AiModelInfo> {
        return listOf(
            AiModelInfo(
                id = "gemini-3.8-flash",
                name = "Gemini 3.8 Flash (Server-Side Firebase AI)",
                description = "ডিফল্ট ক্লাউড প্রভিশনড সার্ভার-সাইড মডেল",
                contextLength = 1048576,
                isFree = true
            )
        )
    }

    override suspend fun generateResponse(
        prompt: String,
        history: List<ConversationEntity>,
        userName: String,
        apiKey: String
    ): AiAssistantResponse = withContext(Dispatchers.IO) {
        val historyContent: List<Content> = history.takeLast(6).map { item: ConversationEntity ->
            content(if (item.isUser) "user" else "model") {
                text(item.text)
            }
        }

        val chat = model.startChat(history = historyContent)
        val rawResponse = withTimeout(45_000L) {
            chat.sendMessage("User ($userName): $prompt")
        }

        val fullText = rawResponse.text?.trim().orEmpty()
        JarvisPromptConstants.parseResponseAndAction(fullText)
    }
}
