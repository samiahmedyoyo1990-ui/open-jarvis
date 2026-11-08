package com.example.ai.analytics

import com.example.data.local.entity.UsageLogEntity
import com.example.data.repository.JarvisRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class ProviderUsageSummary(
    val providerId: String,
    val requestCount: Int,
    val totalTokens: Long,
    val totalCostUsd: Double
)

data class UsageDashboardData(
    val totalRequests: Int,
    val totalTokens: Long,
    val totalEstimatedCostUsd: Double,
    val averageLatencyMs: Long,
    val providerSummaries: List<ProviderUsageSummary>
)

class UsageAnalyticsEngine(
    private val repository: JarvisRepository
) {
    val usageLogs: Flow<List<UsageLogEntity>> = repository.allUsageLogs

    val dashboardData: Flow<UsageDashboardData> = usageLogs.map { logs ->
        if (logs.isEmpty()) {
            UsageDashboardData(0, 0L, 0.0, 0L, emptyList())
        } else {
            val totalReq = logs.size
            val totalTok = logs.sumOf { it.totalTokens.toLong() }
            val totalCost = logs.sumOf { it.estimatedCostUsd }
            val avgLatency = logs.map { it.latencyMs }.average().toLong()

            val byProvider = logs.groupBy { it.providerId }.map { (provider, group) ->
                ProviderUsageSummary(
                    providerId = provider,
                    requestCount = group.size,
                    totalTokens = group.sumOf { it.totalTokens.toLong() },
                    totalCostUsd = group.sumOf { it.estimatedCostUsd }
                )
            }

            UsageDashboardData(
                totalRequests = totalReq,
                totalTokens = totalTok,
                totalEstimatedCostUsd = totalCost,
                averageLatencyMs = avgLatency,
                providerSummaries = byProvider
            )
        }
    }

    suspend fun recordRequest(
        providerId: String,
        modelId: String,
        promptLengthChars: Int,
        responseLengthChars: Int,
        latencyMs: Long
    ) {
        // Approximate token heuristic: ~4 chars per token
        val promptTokens = maxOf(1, promptLengthChars / 4)
        val completionTokens = maxOf(1, responseLengthChars / 4)

        // Calculate approximate cost based on provider/model
        val costUsd = when {
            providerId.contains("firebase", ignoreCase = true) -> 0.0 // Free default
            providerId.contains("gemini", ignoreCase = true) -> {
                (promptTokens * 0.000000075) + (completionTokens * 0.0000003)
            }
            providerId.contains("openrouter", ignoreCase = true) -> {
                (promptTokens * 0.00000014) + (completionTokens * 0.00000028)
            }
            else -> (promptTokens + completionTokens) * 0.0000002
        }

        repository.recordUsage(
            providerId = providerId,
            modelId = modelId,
            promptTokens = promptTokens,
            completionTokens = completionTokens,
            latencyMs = latencyMs,
            estimatedCostUsd = costUsd
        )
    }

    suspend fun clearHistory() {
        repository.clearUsageLogs()
    }
}
