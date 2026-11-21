package com.example.ai.providers

import android.util.Log
import com.example.ai.AiAssistantResponse
import com.example.ai.JarvisPromptConstants
import com.example.ai.model.AiEndpointSecurity
import com.example.ai.model.AiModelInfo
import com.example.ai.model.AiProviderConfig
import com.example.ai.model.AiProviderType
import com.example.ai.model.ConnectionTestResult
import com.example.data.local.entity.ConversationEntity
import kotlinx.coroutines.CancellationException
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

class OpenAiCompatibleProviderAdapter(
    override val config: AiProviderConfig
) : UniversalAiProvider {

    private val TAG = "OpenAiCompatAdapter"
    override val id: String = config.id
    override val type: AiProviderType = AiProviderType.CUSTOM_OPENAI

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private fun normalizedBaseUrl(): String {
        return config.baseUrl.trim().ifBlank { AiProviderType.CUSTOM_OPENAI.defaultBaseUrl }.trimEnd('/')
    }

    override suspend fun testConnection(apiKey: String): ConnectionTestResult = withContext(Dispatchers.IO) {
        val baseUrl = normalizedBaseUrl()
        if (!AiEndpointSecurity.isValidEndpointUrl(baseUrl)) {
            return@withContext ConnectionTestResult(
                isSuccess = false,
                statusCode = 400,
                message = "নিরাপত্তার জন্য শুধুমাত্র HTTPS অথবা লোকাল নেটওয়ার্ক (localhost/LAN HTTP) Endpoint অনুমোদিত।"
            )
        }

        val startTime = System.currentTimeMillis()
        val requestBuilder = Request.Builder()
            .url("$baseUrl/models")
            .get()
        if (apiKey.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $apiKey")
        }

        try {
            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val count = try {
                        JSONObject(body).optJSONArray("data")?.length() ?: 0
                    } catch (_: Exception) {
                        0
                    }
                    ConnectionTestResult(
                        isSuccess = true,
                        statusCode = response.code,
                        message = "কাস্টম OpenAI এন্ডপয়েন্ট সংযুক্ত! ($latency ms, $count মডেল)",
                        latencyMs = latency,
                        discoveredModelCount = count
                    )
                } else {
                    ConnectionTestResult(
                        isSuccess = false,
                        statusCode = response.code,
                        message = parseErrorMessage(response.code, body, apiKey),
                        latencyMs = latency
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            ConnectionTestResult(
                isSuccess = false,
                statusCode = 0,
                message = "নেটওয়ার্ক কানেকশন ফেইল্ড: ${e.localizedMessage ?: "টাইমআউট বা হোস্ট অপ্রাপ্য"}"
            )
        } catch (e: Exception) {
            ConnectionTestResult(
                isSuccess = false,
                statusCode = 0,
                message = "কানেকশন ত্রুটি: ${AiEndpointSecurity.redactSecret(e.localizedMessage.orEmpty(), apiKey)}"
            )
        }
    }

    override suspend fun fetchModels(apiKey: String): List<AiModelInfo> = withContext(Dispatchers.IO) {
        val baseUrl = normalizedBaseUrl()
        if (!AiEndpointSecurity.isValidEndpointUrl(baseUrl)) return@withContext defaultModels()

        val requestBuilder = Request.Builder()
            .url("$baseUrl/models")
            .get()
        if (apiKey.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $apiKey")
        }

        try {
            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) return@withContext defaultModels()
                val body = response.body?.string().orEmpty()
                val dataArray = JSONObject(body).optJSONArray("data") ?: return@withContext defaultModels()
                val list = mutableListOf<AiModelInfo>()
                for (i in 0 until dataArray.length()) {
                    val item = dataArray.getJSONObject(i)
                    val modelId = item.optString("id")
                    if (modelId.isNotBlank()) {
                        list.add(
                            AiModelInfo(
                                id = modelId,
                                name = modelId,
                                description = item.optString("owned_by", "Custom Endpoint"),
                                contextLength = 128000
                            )
                        )
                    }
                }
                if (list.isNotEmpty()) list else defaultModels()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch models from custom endpoint", e)
            defaultModels()
        }
    }

    override suspend fun generateResponse(
        prompt: String,
        history: List<ConversationEntity>,
        userName: String,
        apiKey: String
    ): AiAssistantResponse {
        return sendChatCompletion(
            userContent = "User ($userName): $prompt",
            history = history,
            apiKey = apiKey
        )
    }

    override suspend fun generateMultimodalResponse(
        prompt: String,
        imageBase64: String,
        mimeType: String,
        history: List<ConversationEntity>,
        userName: String,
        apiKey: String
    ): AiAssistantResponse {
        val parts = JSONArray().apply {
            put(JSONObject().apply {
                put("type", "text")
                put("text", "User ($userName): $prompt")
            })
            put(JSONObject().apply {
                put("type", "image_url")
                put("image_url", JSONObject().apply {
                    put("url", "data:$mimeType;base64,$imageBase64")
                })
            })
        }
        return sendChatCompletion(
            userContent = parts,
            history = history,
            apiKey = apiKey
        )
    }

    private suspend fun sendChatCompletion(
        userContent: Any,
        history: List<ConversationEntity>,
        apiKey: String
    ): AiAssistantResponse = withContext(Dispatchers.IO) {
        val baseUrl = normalizedBaseUrl()
        if (!AiEndpointSecurity.isValidEndpointUrl(baseUrl)) {
            return@withContext AiAssistantResponse(
                replyText = "অবৈধ বা অনিরাপদ API Base URL। শুধুমাত্র HTTPS বা লোকাল নেটওয়ার্ক ঠিকানা ব্যবহার করুন।",
                isError = true
            )
        }

        val model = config.selectedModel.trim().ifBlank { AiProviderType.CUSTOM_OPENAI.defaultModel }
        val payload = JSONObject().apply {
            put("model", model)
            val messagesArray = JSONArray()
            messagesArray.put(JSONObject().apply {
                put("role", "system")
                put("content", JarvisPromptConstants.SYSTEM_INSTRUCTION)
            })
            history.takeLast(6).forEach { msg ->
                messagesArray.put(JSONObject().apply {
                    put("role", if (msg.isUser) "user" else "assistant")
                    put("content", msg.text)
                })
            }
            messagesArray.put(JSONObject().apply {
                put("role", "user")
                put("content", userContent)
            })
            put("messages", messagesArray)
            put("temperature", config.temperature.toDouble())
        }

        val requestBuilder = Request.Builder()
            .url("$baseUrl/chat/completions")
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
        if (apiKey.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $apiKey")
        }

        try {
            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext AiAssistantResponse(
                        replyText = parseErrorMessage(response.code, body, apiKey),
                        isError = true,
                        isRetryableError = AiEndpointSecurity.isRetryableStatusCode(response.code)
                    )
                }

                val choices = JSONObject(body).optJSONArray("choices")
                if (choices == null || choices.length() == 0) {
                    return@withContext AiAssistantResponse(
                        replyText = "কাস্টম এন্ডপয়েন্ট থেকে কোনো উত্তর পাওয়া যায়নি।",
                        isError = true,
                        isRetryableError = true
                    )
                }

                val text = choices.getJSONObject(0)
                    .optJSONObject("message")
                    ?.optString("content")
                    ?.trim()
                    .orEmpty()
                if (text.isBlank()) {
                    return@withContext AiAssistantResponse(
                        replyText = "উত্তরটি ফাঁকা এসেছে। অনুগ্রহ করে প্রশ্নটি পুনরায় করুন।",
                        isError = true
                    )
                }
                JarvisPromptConstants.parseResponseAndAction(text)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            AiAssistantResponse(
                replyText = "নেটওয়ার্ক টাইমআউট বা সংযোগ ব্যর্থ: ${e.localizedMessage}",
                isError = true,
                isRetryableError = true
            )
        } catch (e: Exception) {
            AiAssistantResponse(
                replyText = "কাস্টম প্রোভাইডার ত্রুটি: ${AiEndpointSecurity.redactSecret(e.localizedMessage.orEmpty(), apiKey)}",
                isError = true
            )
        }
    }

    private fun parseErrorMessage(statusCode: Int, body: String, apiKey: String): String {
        val rawServerMsg = try {
            JSONObject(body).optJSONObject("error")?.optString("message")
        } catch (_: Exception) {
            null
        }
        val cleanMsg = rawServerMsg?.let { AiEndpointSecurity.redactSecret(it, apiKey) }
        val details = if (!cleanMsg.isNullOrBlank()) " ($cleanMsg)" else ""
        return when (statusCode) {
            401, 403 -> "API Key সঠিক নয় বা অনুমতি নেই$details (HTTP $statusCode)"
            404 -> "মডেল '${config.selectedModel}' বা এন্ডপয়েন্ট পাওয়া যায়নি$details (HTTP 404)"
            429 -> "রেট লিমিট বা কোটা অতিক্রম করেছে$details (HTTP 429)"
            in 500..599 -> "সার্ভার সাময়িকভাবে অনুপলব্ধ$details (HTTP $statusCode)"
            else -> "প্রোভাইডার ত্রুটি: HTTP $statusCode$details"
        }
    }

    private fun defaultModels(): List<AiModelInfo> {
        return listOf(
            AiModelInfo("gpt-4o-mini", "GPT-4o Mini", "দ্রুত ও সাশ্রয়ী চ্যাট/ভিশন মডেল", 128000),
            AiModelInfo("gpt-4o", "GPT-4o", "উন্নত অমনি মাল্টিমোডাল মডেল", 128000),
            AiModelInfo("llama-3.3-70b-versatile", "Llama 3.3 70B (Groq)", "দ্রুত ওপেন-ওয়েটস মডেল", 128000),
            AiModelInfo("qwen2.5:14b", "Qwen 2.5 14B (Ollama Local)", "লোকাল সার্ভার মডেল", 32768, isFree = true)
        )
    }
}
