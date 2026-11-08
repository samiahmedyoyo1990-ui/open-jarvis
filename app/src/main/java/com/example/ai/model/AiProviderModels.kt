package com.example.ai.model

import org.json.JSONArray
import org.json.JSONObject

enum class AiProviderType(
    val displayName: String,
    val defaultBaseUrl: String,
    val defaultModel: String,
    val description: String
) {
    GOOGLE_GEMINI(
        displayName = "Google Gemini",
        defaultBaseUrl = "https://generativelanguage.googleapis.com",
        defaultModel = "gemini-2.5-flash",
        description = "Google's state-of-the-art multimodal model with high speed and precision."
    ),
    OPEN_ROUTER(
        displayName = "OpenRouter",
        defaultBaseUrl = "https://openrouter.ai/api/v1",
        defaultModel = "deepseek/deepseek-chat",
        description = "Unified gateway providing access to DeepSeek, Claude, Llama 3, GPT-4o, and hundreds of models."
    ),
    FIREBASE_AI(
        displayName = "Firebase AI Logic (Built-in)",
        defaultBaseUrl = "",
        defaultModel = "gemini-3.8-flash",
        description = "Zero-config server-side Gemini 3.8 Flash integrated with Firebase App Check."
    )
}

data class AiModelInfo(
    val id: String,
    val name: String,
    val description: String = "",
    val contextLength: Int = 0,
    val isFree: Boolean = false
)

data class ConnectionTestResult(
    val isSuccess: Boolean,
    val statusCode: Int = 200,
    val message: String,
    val latencyMs: Long = 0L,
    val discoveredModelCount: Int = 0
)

data class AiProviderConfig(
    val id: String,
    val type: AiProviderType,
    val name: String,
    val baseUrl: String = type.defaultBaseUrl,
    val selectedModel: String = type.defaultModel,
    val isEnabled: Boolean = true,
    val temperature: Float = 0.7f,
    val lastTestedTime: Long = 0L,
    val lastConnectionSuccess: Boolean = false,
    val lastStatusMessage: String = "Not Tested"
) {
    val isConfigured: Boolean
        get() = type == AiProviderType.FIREBASE_AI || lastConnectionSuccess

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("type", type.name)
            put("name", name)
            put("baseUrl", baseUrl)
            put("selectedModel", selectedModel)
            put("isEnabled", isEnabled)
            put("temperature", temperature.toDouble())
            put("lastTestedTime", lastTestedTime)
            put("lastConnectionSuccess", lastConnectionSuccess)
            put("lastStatusMessage", lastStatusMessage)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): AiProviderConfig {
            val typeStr = json.optString("type", AiProviderType.GOOGLE_GEMINI.name)
            val type = try {
                AiProviderType.valueOf(typeStr)
            } catch (e: Exception) {
                AiProviderType.GOOGLE_GEMINI
            }

            return AiProviderConfig(
                id = json.optString("id", "provider_${System.currentTimeMillis()}"),
                type = type,
                name = json.optString("name", type.displayName),
                baseUrl = json.optString("baseUrl", type.defaultBaseUrl),
                selectedModel = json.optString("selectedModel", type.defaultModel),
                isEnabled = json.optBoolean("isEnabled", true),
                temperature = json.optDouble("temperature", 0.7).toFloat(),
                lastTestedTime = json.optLong("lastTestedTime", 0L),
                lastConnectionSuccess = json.optBoolean("lastConnectionSuccess", false),
                lastStatusMessage = json.optString("lastStatusMessage", "Not Tested")
            )
        }

        fun defaultConfigs(): List<AiProviderConfig> {
            return listOf(
                AiProviderConfig(
                    id = "provider_gemini",
                    type = AiProviderType.GOOGLE_GEMINI,
                    name = "Google Gemini (BYOK)",
                    baseUrl = AiProviderType.GOOGLE_GEMINI.defaultBaseUrl,
                    selectedModel = "gemini-2.5-flash"
                ),
                AiProviderConfig(
                    id = "provider_openrouter",
                    type = AiProviderType.OPEN_ROUTER,
                    name = "OpenRouter (BYOK)",
                    baseUrl = AiProviderType.OPEN_ROUTER.defaultBaseUrl,
                    selectedModel = "deepseek/deepseek-chat"
                ),
                AiProviderConfig(
                    id = "provider_firebase_builtin",
                    type = AiProviderType.FIREBASE_AI,
                    name = "Firebase AI Logic (Default)",
                    baseUrl = "",
                    selectedModel = "gemini-3.8-flash"
                )
            )
        }

        fun listToJson(list: List<AiProviderConfig>): String {
            val array = JSONArray()
            list.forEach { array.put(it.toJson()) }
            return array.toString()
        }

        fun listFromJson(jsonStr: String): List<AiProviderConfig> {
            val list = mutableListOf<AiProviderConfig>()
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(fromJson(obj))
                }
            } catch (e: Exception) {
                // Return defaults on error
            }
            return if (list.isNotEmpty()) list else defaultConfigs()
        }
    }
}
