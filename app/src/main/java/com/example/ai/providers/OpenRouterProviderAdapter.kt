package com.example.ai.providers

import android.util.Log
import com.example.ai.AiAssistantResponse
import com.example.ai.JarvisPromptConstants
import com.example.ai.model.AiModelInfo
import com.example.ai.model.AiProviderConfig
import com.example.ai.model.AiProviderType
import com.example.ai.model.ConnectionTestResult
import com.example.data.local.entity.ConversationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class OpenRouterProviderAdapter(
    override val config: AiProviderConfig
) : UniversalAiProvider {

    private val TAG = "OpenRouterAdapter"
    override val id: String = config.id
    override val type: AiProviderType = AiProviderType.OPEN_ROUTER

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    override suspend fun testConnection(apiKey: String): ConnectionTestResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext ConnectionTestResult(
                isSuccess = false,
                statusCode = 400,
                message = "OpenRouter API Key প্রদান করা হয়নি।"
            )
        }

        val startTime = System.currentTimeMillis()
        val baseUrl = config.baseUrl.trimEnd('/')
        val url = "$baseUrl/models"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("HTTP-Referer", "https://github.com/samiahmedyoyo1990-ui/open-jarvis")
            .addHeader("X-Title", "JARVIS AI Assistant")
            .get()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                val body = response.body?.string().orEmpty()

                if (response.isSuccessful) {
                    val count = try {
                        val json = JSONObject(body)
                        json.optJSONArray("data")?.length() ?: 0
                    } catch (e: Exception) {
                        0
                    }
                    ConnectionTestResult(
                        isSuccess = true,
                        statusCode = response.code,
                        message = "OpenRouter কানেকশন সফল! ($latency ms, $count মডেল উপলভ্য)",
                        latencyMs = latency,
                        discoveredModelCount = count
                    )
                } else {
                    val errorMsg = parseErrorMessage(response.code, body)
                    ConnectionTestResult(
                        isSuccess = false,
                        statusCode = response.code,
                        message = errorMsg,
                        latencyMs = latency
                    )
                }
            }
        } catch (e: IOException) {
            ConnectionTestResult(
                isSuccess = false,
                statusCode = 0,
                message = "নেটওয়ার্ক কানেকশন ফেইল্ড: ${e.localizedMessage ?: "টাইমআউট বা ইন্টারনেট ত্রুটি"}"
            )
        } catch (e: Exception) {
            ConnectionTestResult(
                isSuccess = false,
                statusCode = 0,
                message = "অনাকাঙ্ক্ষিত ত্রুটি: ${e.localizedMessage}"
            )
        }
    }

    override suspend fun fetchModels(apiKey: String): List<AiModelInfo> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext defaultModels()

        val baseUrl = config.baseUrl.trimEnd('/')
        val url = "$baseUrl/models"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("HTTP-Referer", "https://github.com/samiahmedyoyo1990-ui/open-jarvis")
            .addHeader("X-Title", "JARVIS AI Assistant")
            .get()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext defaultModels()

                val body = response.body?.string().orEmpty()
                val json = JSONObject(body)
                val dataArray = json.optJSONArray("data") ?: return@withContext defaultModels()

                val list = mutableListOf<AiModelInfo>()
                for (i in 0 until dataArray.length()) {
                    val item = dataArray.getJSONObject(i)
                    val modelId = item.optString("id")
                    val name = item.optString("name", modelId)
                    val description = item.optString("description", "")
                    val contextLength = item.optInt("context_length", 32768)
                    val pricingObj = item.optJSONObject("pricing")
                    val isFree = pricingObj?.optDouble("prompt", 1.0) == 0.0

                    if (modelId.isNotBlank()) {
                        list.add(
                            AiModelInfo(
                                id = modelId,
                                name = name,
                                description = description,
                                contextLength = contextLength,
                                isFree = isFree
                            )
                        )
                    }
                }

                if (list.isNotEmpty()) list else defaultModels()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch remote OpenRouter models, falling back to defaults", e)
            defaultModels()
        }
    }

    override suspend fun generateResponse(
        prompt: String,
        history: List<ConversationEntity>,
        userName: String,
        apiKey: String
    ): AiAssistantResponse = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext AiAssistantResponse(
                replyText = "OpenRouter API Key কনফিগার করা নেই। অনুগ্রহ করে সেটিংস -> 'AI Provider Manager' এ গিয়ে আপনার নিজস্ব OpenRouter API Key যুক্ত করুন।"
            )
        }

        val baseUrl = config.baseUrl.trimEnd('/')
        val url = "$baseUrl/chat/completions"

        val payload = JSONObject().apply {
            put("model", config.selectedModel.trim())

            val messagesArray = JSONArray()

            // System prompt
            messagesArray.put(JSONObject().apply {
                put("role", "system")
                put("content", JarvisPromptConstants.SYSTEM_INSTRUCTION)
            })

            // Multi-turn history
            history.takeLast(6).forEach { msg ->
                messagesArray.put(JSONObject().apply {
                    put("role", if (msg.isUser) "user" else "assistant")
                    put("content", msg.text)
                })
            }

            // Current prompt
            messagesArray.put(JSONObject().apply {
                put("role", "user")
                put("content", "User ($userName): $prompt")
            })

            put("messages", messagesArray)
            put("temperature", config.temperature.toDouble())
        }

        val requestBody = payload.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("HTTP-Referer", "https://github.com/samiahmedyoyo1990-ui/open-jarvis")
            .addHeader("X-Title", "JARVIS Personal Assistant")
            .post(requestBody)
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    val errorText = parseErrorMessage(response.code, body)
                    return@withContext AiAssistantResponse(replyText = errorText)
                }

                val json = JSONObject(body)
                val choices = json.optJSONArray("choices")
                if (choices == null || choices.length() == 0) {
                    return@withContext AiAssistantResponse(replyText = "OpenRouter থেকে কোনো উত্তর পাওয়া যায়নি।")
                }

                val firstChoice = choices.getJSONObject(0)
                val text = firstChoice.optJSONObject("message")?.optString("content")?.trim().orEmpty()

                if (text.isBlank()) {
                    return@withContext AiAssistantResponse(replyText = "উত্তরটি ফাঁকা এসেছে। অনুগ্রহ করে প্রশ্নটি পুনরায় করুন।")
                }

                JarvisPromptConstants.parseResponseAndAction(text)
            }
        } catch (e: IOException) {
            AiAssistantResponse(replyText = "নেটওয়ার্ক টাইমআউট বা ইন্টারনেট সংযোগ নেই: ${e.localizedMessage}")
        } catch (e: Exception) {
            AiAssistantResponse(replyText = "OpenRouter প্রসেসিং ত্রুটি: ${e.localizedMessage}")
        }
    }

    private fun parseErrorMessage(statusCode: Int, body: String): String {
        val serverMessage = try {
            val json = JSONObject(body)
            json.optJSONObject("error")?.optString("message")
        } catch (e: Exception) {
            null
        }

        val details = if (!serverMessage.isNullOrBlank()) " ($serverMessage)" else ""

        return when (statusCode) {
            400 -> "ভুল রিকোয়েস্ট বা প্যারামিটার$details (HTTP 400)"
            401 -> "OpenRouter API Key সঠিক নয়$details (HTTP 401 Unauthorized)"
            402 -> "OpenRouter একাউন্টে ব্যালেন্স বা ক্রেডিট অপর্যাপ্ত$details (HTTP 402 Payment Required)"
            403 -> "অ্যাক্সেস অনুমোদিত নয়$details (HTTP 403 Forbidden)"
            404 -> "মডেল '${config.selectedModel}' পাওয়া যায়নি$details (HTTP 404)"
            429 -> "রেট লিমিট অতিক্রম করেছে$details (HTTP 429 Too Many Requests)"
            500, 502, 503 -> "OpenRouter সার্ভার সাময়িকভাবে ডাউন$details (HTTP $statusCode)"
            else -> "OpenRouter ত্রুটি: HTTP $statusCode $details"
        }
    }

    private fun defaultModels(): List<AiModelInfo> {
        return listOf(
            AiModelInfo("deepseek/deepseek-chat", "DeepSeek V3 (Recommended)", "অত্যন্ত বুদ্ধিমান ও সাশ্রয়ী চ্যাট মডেল", 64000),
            AiModelInfo("deepseek/deepseek-r1", "DeepSeek R1 (Reasoning)", "উচ্চতর লজিক ও যুক্তিনির্ভর রিজনিং মডেল", 64000),
            AiModelInfo("meta-llama/llama-3.3-70b-instruct", "Llama 3.3 70B Instruct", "মেটার শক্তিশালী ওপেন ওয়েটস ফ্ল্যাগশিপ মডেল", 131072),
            AiModelInfo("anthropic/claude-3.5-sonnet", "Claude 3.5 Sonnet", "অসাধারণ কোডিং ও স্বাভাবিক কথোপকথন", 200000),
            AiModelInfo("openai/gpt-4o", "OpenAI GPT-4o", "অত্যাধুনিক অমনি মডেল", 128000),
            AiModelInfo("google/gemini-2.0-flash-exp:free", "Gemini 2.0 Flash (Free)", "OpenRouter ফ্রি টিয়ার ফ্ল্যাশ মডেল", 1048576, isFree = true)
        )
    }
}
