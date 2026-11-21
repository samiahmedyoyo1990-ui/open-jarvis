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

class GeminiDirectProviderAdapter(
    override val config: AiProviderConfig
) : UniversalAiProvider {

    private val TAG = "GeminiDirectAdapter"
    override val id: String = config.id
    override val type: AiProviderType = AiProviderType.GOOGLE_GEMINI

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    override suspend fun testConnection(apiKey: String): ConnectionTestResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext ConnectionTestResult(
                isSuccess = false,
                statusCode = 400,
                message = "API Key প্রদান করা হয়নি।"
            )
        }

        val startTime = System.currentTimeMillis()
        val baseUrl = config.baseUrl.trimEnd('/')
        if (!AiEndpointSecurity.isValidEndpointUrl(baseUrl)) {
            return@withContext ConnectionTestResult(false, 400, "অবৈধ Gemini Base URL।")
        }
        val url = "$baseUrl/v1beta/models"

        val request = Request.Builder()
            .url(url)
            .addHeader("x-goog-api-key", apiKey)
            .get()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                val body = response.body?.string().orEmpty()

                if (response.isSuccessful) {
                    val count = try {
                        val json = JSONObject(body)
                        json.optJSONArray("models")?.length() ?: 0
                    } catch (e: Exception) {
                        0
                    }
                    ConnectionTestResult(
                        isSuccess = true,
                        statusCode = response.code,
                        message = "Google Gemini কানেকশন সফল! ($latency ms, $count মডেল পাওয়া গেছে)",
                        latencyMs = latency,
                        discoveredModelCount = count
                    )
                } else {
                    val errorMsg = parseErrorMessage(response.code, body, apiKey)
                    ConnectionTestResult(
                        isSuccess = false,
                        statusCode = response.code,
                        message = errorMsg,
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
                message = "নেটওয়ার্ক কানেকশন ফেইল্ড: ${e.localizedMessage ?: "টাইমআউট বা ইন্টারনেট ত্রুটি"}"
            )
        } catch (e: Exception) {
            ConnectionTestResult(
                isSuccess = false,
                statusCode = 0,
                message = "অনাকাঙ্ক্ষিত ত্রুটি: ${AiEndpointSecurity.redactSecret(e.localizedMessage.orEmpty(), apiKey)}"
            )
        }
    }

    override suspend fun fetchModels(apiKey: String): List<AiModelInfo> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext defaultModels()

        val baseUrl = config.baseUrl.trimEnd('/')
        if (!AiEndpointSecurity.isValidEndpointUrl(baseUrl)) return@withContext defaultModels()
        val url = "$baseUrl/v1beta/models"

        val request = Request.Builder()
            .url(url)
            .addHeader("x-goog-api-key", apiKey)
            .get()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext defaultModels()

                val body = response.body?.string().orEmpty()
                val json = JSONObject(body)
                val modelsArray = json.optJSONArray("models") ?: return@withContext defaultModels()

                val list = mutableListOf<AiModelInfo>()
                for (i in 0 until modelsArray.length()) {
                    val modelObj = modelsArray.getJSONObject(i)
                    val rawName = modelObj.optString("name")
                    val cleanId = rawName.replace("models/", "")
                    val supportedMethods = modelObj.optJSONArray("supportedGenerationMethods")

                    var canGenerate = false
                    if (supportedMethods != null) {
                        for (m in 0 until supportedMethods.length()) {
                            if (supportedMethods.getString(m) == "generateContent") {
                                canGenerate = true
                                break
                            }
                        }
                    } else {
                        canGenerate = cleanId.contains("gemini")
                    }

                    if (canGenerate && !cleanId.contains("embedding") && !cleanId.contains("aqa")) {
                        list.add(
                            AiModelInfo(
                                id = cleanId,
                                name = modelObj.optString("displayName", cleanId),
                                description = modelObj.optString("description", ""),
                                contextLength = modelObj.optInt("inputTokenLimit", 1048576)
                            )
                        )
                    }
                }

                if (list.isNotEmpty()) list else defaultModels()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch remote Gemini models, falling back to defaults", e)
            defaultModels()
        }
    }

    override suspend fun generateResponse(
        prompt: String,
        history: List<ConversationEntity>,
        userName: String,
        apiKey: String
    ): AiAssistantResponse {
        val currentParts = JSONArray().apply {
            put(JSONObject().apply { put("text", "User ($userName): $prompt") })
        }
        return sendGenerateContent(currentParts, history, apiKey)
    }

    override suspend fun generateMultimodalResponse(
        prompt: String,
        imageBase64: String,
        mimeType: String,
        history: List<ConversationEntity>,
        userName: String,
        apiKey: String
    ): AiAssistantResponse {
        val currentParts = JSONArray().apply {
            put(JSONObject().apply { put("text", "User ($userName): $prompt") })
            put(JSONObject().apply {
                put("inline_data", JSONObject().apply {
                    put("mime_type", mimeType)
                    put("data", imageBase64)
                })
            })
        }
        return sendGenerateContent(currentParts, history, apiKey)
    }

    private suspend fun sendGenerateContent(
        currentParts: JSONArray,
        history: List<ConversationEntity>,
        apiKey: String
    ): AiAssistantResponse = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext AiAssistantResponse(
                replyText = "Google Gemini API Key কনফিগার করা নেই। অনুগ্রহ করে সেটিংস -> 'AI Provider Manager' এ গিয়ে আপনার নিজস্ব Gemini API Key প্রদান করুন।",
                isError = true
            )
        }

        val baseUrl = config.baseUrl.trimEnd('/')
        if (!AiEndpointSecurity.isValidEndpointUrl(baseUrl)) {
            return@withContext AiAssistantResponse(
                replyText = "অবৈধ Gemini Base URL।",
                isError = true
            )
        }

        val model = config.selectedModel.trim().replace("models/", "")
        val url = "$baseUrl/v1beta/models/$model:generateContent"

        val payload = JSONObject().apply {
            put("system_instruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", JarvisPromptConstants.SYSTEM_INSTRUCTION) })
                })
            })

            val contentsArray = JSONArray()
            history.takeLast(6).forEach { msg ->
                contentsArray.put(JSONObject().apply {
                    put("role", if (msg.isUser) "user" else "model")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", msg.text) })
                    })
                })
            }

            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", currentParts)
            })

            put("contents", contentsArray)
            put("generationConfig", JSONObject().apply {
                put("temperature", config.temperature.toDouble())
            })
        }

        val requestBody = payload.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(url)
            .addHeader("x-goog-api-key", apiKey)
            .post(requestBody)
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    val errorText = parseErrorMessage(response.code, body, apiKey)
                    return@withContext AiAssistantResponse(
                        replyText = errorText,
                        isError = true,
                        isRetryableError = AiEndpointSecurity.isRetryableStatusCode(response.code)
                    )
                }

                val json = JSONObject(body)
                val candidates = json.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return@withContext AiAssistantResponse(
                        replyText = "মডেল থেকে কোনো উত্তর পাওয়া যায়নি।",
                        isError = true,
                        isRetryableError = true
                    )
                }

                val firstCandidate = candidates.getJSONObject(0)
                val parts = firstCandidate.optJSONObject("content")?.optJSONArray("parts")
                val text = parts?.optJSONObject(0)?.optString("text")?.trim().orEmpty()

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
                replyText = "নেটওয়ার্ক টাইমআউট বা ইন্টারনেট সংযোগ নেই: ${e.localizedMessage}",
                isError = true,
                isRetryableError = true
            )
        } catch (e: Exception) {
            AiAssistantResponse(
                replyText = "Gemini API প্রসেসিং ত্রুটি: ${AiEndpointSecurity.redactSecret(e.localizedMessage.orEmpty(), apiKey)}",
                isError = true
            )
        }
    }

    private fun parseErrorMessage(statusCode: Int, body: String, apiKey: String): String {
        val rawServerMessage = try {
            val json = JSONObject(body)
            json.optJSONObject("error")?.optString("message")
        } catch (e: Exception) {
            null
        }
        val serverMessage = rawServerMessage?.let { AiEndpointSecurity.redactSecret(it, apiKey) }
        val details = if (!serverMessage.isNullOrBlank()) " ($serverMessage)" else ""

        return when (statusCode) {
            400 -> "ভুল রিকোয়েস্ট বা ইনভ্যালিড মডেল প্যারামিটার$details (HTTP 400)"
            401, 403 -> "Gemini API Key সঠিক নয় অথবা পারমিশন নেই$details (HTTP $statusCode)"
            404 -> "মডেল '${config.selectedModel}' পাওয়া যায়নি$details (HTTP 404)"
            429 -> "কোটা শেষ বা রেট লিমিট অতিক্রম করেছে$details (HTTP 429 Quota Exceeded)"
            500, 503 -> "Google Gemini সার্ভার সাময়িকভাবে ডাউন$details (HTTP $statusCode)"
            else -> "Google Gemini ত্রুটি: HTTP $statusCode $details"
        }
    }

    private fun defaultModels(): List<AiModelInfo> {
        return listOf(
            AiModelInfo("gemini-2.5-flash", "Gemini 2.5 Flash (Recommended)", "অতি দ্রুত ও স্মার্ট মাল্টিমোডাল মডেল", 1048576, isFree = true),
            AiModelInfo("gemini-2.5-pro", "Gemini 2.5 Pro", "উচ্চ ক্ষমতার বিশ্লেষণ ও জটিল কোডিং মডেল", 1048576),
            AiModelInfo("gemini-2.5-flash-lite", "Gemini 2.5 Flash Lite", "অত্যন্ত হালকা ও সাশ্রয়ী দ্রুত মডেল", 1048576, isFree = true)
        )
    }
}

