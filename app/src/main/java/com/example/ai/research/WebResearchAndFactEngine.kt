package com.example.ai.research

import com.example.ai.UniversalAiEngine
import kotlinx.coroutines.CancellationException
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
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")

        // 1. DuckDuckGo Instant Answer API
        try {
            val url = "https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "JARVIS-Android-Research/1.0")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    val json = JSONObject(body)
                    val abstractText = json.optString("AbstractText", "")
                    val abstractSource = json.optString("AbstractSource", "DuckDuckGo")
                    val abstractUrl = json.optString("AbstractURL", "")

                    if (abstractText.isNotBlank() && abstractUrl.isNotBlank()) {
                        list.add(
                            WebSource(
                                title = abstractSource,
                                snippet = abstractText,
                                url = abstractUrl
                            )
                        )
                    }

                    val related = json.optJSONArray("RelatedTopics")
                    if (related != null) {
                        for (i in 0 until minOf(related.length(), 4)) {
                            val item = related.optJSONObject(i) ?: continue
                            val text = item.optString("Text", "")
                            val firstUrl = item.optString("FirstURL", "")
                            if (text.isNotBlank() && firstUrl.isNotBlank()) {
                                list.add(
                                    WebSource(
                                        title = text.take(48),
                                        snippet = text,
                                        url = firstUrl
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Continue to next source
        }

        // 2. Wikipedia MediaWiki Search API
        try {
            val wikiUrl = "https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encoded&format=json&utf8=1&srlimit=3"
            val request = Request.Builder()
                .url(wikiUrl)
                .header("User-Agent", "JARVIS-Android-Research/1.0")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    val searchArr = JSONObject(body)
                        .optJSONObject("query")
                        ?.optJSONArray("search")
                    if (searchArr != null) {
                        for (i in 0 until searchArr.length()) {
                            val item = searchArr.optJSONObject(i) ?: continue
                            val title = item.optString("title", "")
                            val rawSnippet = item.optString("snippet", "")
                                .replace(Regex("<[^>]+>"), " ")
                                .replace("&quot;", "\"")
                                .replace("&amp;", "&")
                                .replace(Regex("\\s+"), " ")
                                .trim()
                            if (title.isNotBlank() && rawSnippet.isNotBlank()) {
                                val slug = URLEncoder.encode(title.replace(" ", "_"), "UTF-8")
                                list.add(
                                    WebSource(
                                        title = "Wikipedia: $title",
                                        snippet = rawSnippet,
                                        url = "https://en.wikipedia.org/wiki/$slug"
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Graceful fallback if offline
        }

        return deduplicateSources(list)
    }

    companion object {
        fun deduplicateSources(sources: List<WebSource>): List<WebSource> {
            val seenUrls = LinkedHashSet<String>()
            val result = mutableListOf<WebSource>()
            for (src in sources) {
                val normalizedUrl = src.url.trim().trimEnd('/').lowercase()
                if (normalizedUrl.isNotBlank() && seenUrls.add(normalizedUrl)) {
                    result.add(src)
                }
            }
            return result
        }

        fun parseVerificationStatus(
            summary: String,
            hasRetrievedSources: Boolean
        ): VerificationStatus {
            return when {
                summary.contains("CONFLICTING", ignoreCase = true) ||
                    summary.contains("মতভেদ") ||
                    summary.contains("মিথ্যা") ||
                    summary.contains("ভুল") -> VerificationStatus.CONFLICTING_SOURCES
                summary.contains("UNCERTAIN", ignoreCase = true) ||
                    summary.contains("অনিশ্চিত") ||
                    summary.contains("অপ্রতুল") -> VerificationStatus.UNCERTAIN
                summary.contains("PARTIALLY", ignoreCase = true) ||
                    summary.contains("আংশিক") -> VerificationStatus.PARTIALLY_VERIFIED
                hasRetrievedSources && (
                    summary.contains("VERIFIED", ignoreCase = true) ||
                        summary.contains("যাচাইকৃত সত্য") ||
                        summary.contains("সঠিক")
                    ) -> VerificationStatus.VERIFIED
                else -> VerificationStatus.UNCERTAIN
            }
        }
    }

    suspend fun verifyFacts(text: String): FactVerificationReport = withContext(Dispatchers.IO) {
        val retrievedSources = fetchLiveSources(text.take(180))
        val prompt = buildString {
            append("You are JARVIS's Fact Verification Engine. Analyze the following text and extract 2-4 factual claims.\n")
            if (retrievedSources.isNotEmpty()) {
                append("\nRetrieved Live Evidence:\n")
                retrievedSources.forEachIndexed { idx, src ->
                    append("[${idx + 1}] ${src.title} (${src.url}): ${src.snippet}\n")
                }
            } else {
                append("\n(No live web sources were retrieved for this claim; explicitly mark uncertain if external verification is required.)\n")
            }
            append("\nFor each claim:\n")
            append("1. State the claim.\n")
            append("2. Evaluate whether it is VERIFIED, PARTIALLY_VERIFIED, CONFLICTING_SOURCES, or UNCERTAIN.\n")
            append("3. Provide a brief rationale in Bangla.\n\n")
            append("Text to verify:\n\"$text\"\n\n")
            append("Begin with the overall tag (VERIFIED / PARTIALLY_VERIFIED / CONFLICTING_SOURCES / UNCERTAIN) and provide a clean structured summary in Bangla.")
        }

        val aiResponse = aiEngine.executeAiQuery(prompt, emptyList())
        val summary = aiResponse.replyText

        val status = parseVerificationStatus(summary, hasRetrievedSources = retrievedSources.isNotEmpty())
        val realCitations = retrievedSources.map { it.url }

        FactVerificationReport(
            originalText = text,
            overallStatus = status,
            checkedClaims = listOf(
                FactClaimCheck(
                    claim = text.take(80) + if (text.length > 80) "..." else "",
                    status = status,
                    rationale = summary.take(200),
                    sourceCitations = realCitations
                )
            ),
            verifiedSummary = summary
        )
    }
}
