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

            val prompt = """
                Task: $userPrompt
                
                Please describe the visual elements clearly in Bangla (with English technical terms where appropriate). If text is visible, transcribe it accurately.
            """.trimIndent()

            val response = aiEngine.executeMultimodalQuery(
                prompt = prompt,
                imageBase64 = base64,
                mimeType = "image/jpeg",
                history = emptyList()
            )
            VisionAnalysisResult(
                description = response.replyText,
                extractedText = if (response.replyText.contains("টেক্সট") || response.replyText.contains("লেখা")) "Text detected" else ""
            )
        }

    private fun encodeImageToBase64(uri: Uri): String? {
        var bitmap: Bitmap? = null
        var scaledBitmap: Bitmap? = null
        return try {
            val maxDim = 1024
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            } ?: return null

            var sampleSize = 1
            if (bounds.outWidth > maxDim || bounds.outHeight > maxDim) {
                val halfW = bounds.outWidth / 2
                val halfH = bounds.outHeight / 2
                while ((halfW / sampleSize) >= maxDim && (halfH / sampleSize) >= maxDim) {
                    sampleSize *= 2
                }
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return null

            val scale = minOf(1f, maxDim.toFloat() / maxOf(bitmap.width, bitmap.height))
            scaledBitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    maxOf(1, (bitmap.width * scale).toInt()),
                    maxOf(1, (bitmap.height * scale).toInt()),
                    true
                )
            } else {
                bitmap
            }

            ByteArrayOutputStream().use { outputStream ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
            }
        } catch (_: Exception) {
            null
        } finally {
            if (scaledBitmap != null && scaledBitmap !== bitmap && !scaledBitmap.isRecycled) {
                scaledBitmap.recycle()
            }
            if (bitmap != null && !bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }
}
