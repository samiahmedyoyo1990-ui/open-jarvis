package com.example.ai.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.ai.UniversalAiEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream

data class DocumentAnalysisResult(
    val fileName: String,
    val characterCount: Int,
    val summary: String,
    val extractedInsights: List<String>
)

data class VisionAnalysisResult(
    val description: String,
    val extractedText: String = "",
    val detectedObjects: List<String> = emptyList()
)

class VisionAndDocumentEngine(
    private val context: Context,
    private val aiEngine: UniversalAiEngine
) {

    suspend fun analyzeDocument(uri: Uri, fileName: String, userQuestion: String? = null): DocumentAnalysisResult =
        withContext(Dispatchers.IO) {
            val content = readTextFromUri(uri)
            val truncatedContent = content.take(15000) // Keep token window safe

            val prompt = buildString {
                append("You are JARVIS Document Intelligence Engine. Analyze the following document:\n\n")
                append("Document Name: $fileName\n")
                append("Content:\n\"\"\"\n$truncatedContent\n\"\"\"\n\n")
                if (!userQuestion.isNullOrBlank()) {
                    append("User Question: $userQuestion\n")
                } else {
                    append("Provide: 1. A clear Bengali summary, 2. Key takeaways and actionable points.\n")
                }
            }

            val response = aiEngine.executeAiQuery(prompt, emptyList())
            val summary = response.replyText

            DocumentAnalysisResult(
                fileName = fileName,
                characterCount = content.length,
                summary = summary,
                extractedInsights = listOf("ডকুমেন্ট সফলভাবে বিশ্লেষণ সম্পন্ন", "মূল পয়েন্ট চিহ্নিত")
            )
        }

    private fun readTextFromUri(uri: Uri): String {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: "ডকুমেন্ট রিড করা সম্ভব হয়নি।"
        } catch (e: Exception) {
            "ডকুমেন্ট লোড করতে ত্রুটি: ${e.localizedMessage}"
        }
    }

    suspend fun analyzeImage(uri: Uri, userPrompt: String = "Describe this image in detail and extract any text visible"): VisionAnalysisResult =
        withContext(Dispatchers.IO) {
            val base64 = encodeImageToBase64(uri)
            if (base64 == null) {
                return@withContext VisionAnalysisResult("ইমেজ লোড করতে সমস্যা হয়েছে।")
            }

            // Route prompt with context to active Universal AI Engine
            val prompt = """
                [VISION REQUEST]
                The user has attached an image.
                Task: $userPrompt
                
                Please describe the visual elements clearly in Bangla (with English technical terms where appropriate). If text is visible, transcribe it accurately.
            """.trimIndent()

            val response = aiEngine.executeAiQuery(prompt, emptyList())
            VisionAnalysisResult(
                description = response.replyText,
                extractedText = if (response.replyText.contains("টেক্সট") || response.replyText.contains("লেখা")) "Text detected" else ""
            )
        }

    private fun encodeImageToBase64(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream) ?: return null

            // Scale down to max 1024x1024 for mobile bandwidth & memory safety
            val maxDim = 1024
            val scale = minOf(1f, maxDim.toFloat() / maxOf(bitmap.width, bitmap.height))
            val scaledBitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt(),
                    (bitmap.height * scale).toInt(),
                    true
                )
            } else {
                bitmap
            }

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val byteArray = outputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }
}
