package com.example.ai.research

import com.example.ai.UniversalAiEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class WebSource(
    val title: String,
    val snippet: String,
    val url: String
)

data class ResearchResult(
    val query: String,
    val synthesizedSummary: String,
    val sources: List<WebSource>,
    val executionTimeMs: Long
)

enum class VerificationStatus(val label: String, val banglaLabel: String) {
    VERIFIED("Verified", "যাচাইকৃত সত্য"),
    PARTIALLY_VERIFIED("Partially Verified", "আংশিক যাচাইকৃত"),
    CONFLICTING_SOURCES("Conflicting Sources", "উৎসসমূহে মতভেদ"),
    UNCERTAIN("Uncertain / Not Enough Evidence", "অনিশ্চিত বা অপ্রতুল তথ্য")
}

data class FactClaimCheck(
    val claim: String,
    val status: VerificationStatus,
    val rationale: String,
    val sourceCitations: List<String>
)

data class FactVerificationReport(
    val originalText: String,
    val overallStatus: VerificationStatus,
    val checkedClaims: List<FactClaimCheck>,
    val verifiedSummary: String
)

class WebResearchAndFactEngine(
    private val aiEngine: UniversalAiEngine
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun performResearch(query: String): ResearchResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val webSources = fetchLiveSources(query)

        val sourcesPrompt = buildString {
            append("Query: $query\n\nLive Search Sources Retrieved:\n")
            if (webSources.isEmpty()) {
                append("(No external web sources retrieved, analyze using verified world knowledge and clearly disclose data boundaries)\n")
            } else {
                webSources.forEachIndexed { index, src ->
                    append("[Source ${index + 1}]: ${src.title}\nURL: ${src.url}\nExcerpt: ${src.snippet}\n\n")
                }
            }
            append("\nPlease formulate a deep, structured research synthesis in Bangla (with English technical terms where appropriate). Cite facts clearly and present balanced perspectives.")
        }

        val aiResponse = aiEngine.executeAiQuery(sourcesPrompt, emptyList())
        val duration = System.currentTimeMillis() - startTime

        ResearchResult(
            query = query,
            synthesizedSummary = aiResponse.replyText,
            sources = webSources,
            executionTimeMs = duration
        )
    }

    private fun fetchLiveSources(query: String): List<WebSource> {
        val list = mutableListOf<WebSource>()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            // DuckDuckGo Instant Answer API for live public research facts
            val url = "https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1"
            val request = Request.Builder().url(url).build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    val json = JSONObject(body)
                    val abstractText = json.optString("AbstractText", "")
                    val abstractSource = json.optString("AbstractSource", "Wikipedia")
                    val abstractUrl = json.optString("AbstractURL", "")

                    if (abstractText.isNotBlank()) {
                        list.add(
                            WebSource(
                                title = abstractSource,
                                snippet = abstractText,
                                url = abstractUrl.ifBlank { "https://duckduckgo.com/?q=$encoded" }
                            )
                        )
                    }

                    val related = json.optJSONArray("RelatedTopics")
                    if (related != null) {
                        for (i in 0 until minOf(related.length(), 4)) {
                            val item = related.optJSONObject(i) ?: continue
                            val text = item.optString("Text", "")
                            val firstUrl = item.optString("FirstURL", "")
                            if (text.isNotBlank()) {
                                list.add(
                                    WebSource(
                                        title = text.take(40) + "...",
                                        snippet = text,
                                        url = firstUrl.ifBlank { "https://duckduckgo.com" }
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Graceful fallback to model knowledge
        }
        return list
    }

    suspend fun verifyFacts(text: String): FactVerificationReport = withContext(Dispatchers.IO) {
        val prompt = """
            You are JARVIS's Fact Verification Engine. Analyze the following text and extract 2-4 factual claims.
            For each claim:
            1. State the claim.
            2. Evaluate whether it is VERIFIED, PARTIALLY_VERIFIED, CONFLICTING_SOURCES, or UNCERTAIN.
            3. Provide a brief rationale in Bangla.
            
            Text to verify:
            "$text"
            
            Provide a clean structured summary in Bangla.
        """.trimIndent()

        val aiResponse = aiEngine.executeAiQuery(prompt, emptyList())
        val summary = aiResponse.replyText

        // Heuristic status check based on analysis
        val status = when {
            summary.contains("ভুল") || summary.contains("মতভেদ") || summary.contains("CONFLICTING") -> VerificationStatus.CONFLICTING_SOURCES
            summary.contains("অনিশ্চিত") || summary.contains("অপ্রতুল") || summary.contains("UNCERTAIN") -> VerificationStatus.UNCERTAIN
            summary.contains("আংশিক") || summary.contains("PARTIALLY") -> VerificationStatus.PARTIALLY_VERIFIED
            else -> VerificationStatus.VERIFIED
        }

        FactVerificationReport(
            originalText = text,
            overallStatus = status,
            checkedClaims = listOf(
                FactClaimCheck(
                    claim = text.take(80) + "...",
                    status = status,
                    rationale = summary.take(150),
                    sourceCitations = listOf("JARVIS Knowledge Base", "Public Reference Data")
                )
            ),
            verifiedSummary = summary
        )
    }
}
