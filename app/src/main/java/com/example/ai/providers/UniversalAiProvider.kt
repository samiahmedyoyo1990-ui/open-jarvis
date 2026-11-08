package com.example.ai.providers

import com.example.ai.AiAssistantResponse
import com.example.ai.model.AiModelInfo
import com.example.ai.model.AiProviderConfig
import com.example.ai.model.AiProviderType
import com.example.ai.model.ConnectionTestResult
import com.example.data.local.entity.ConversationEntity

interface UniversalAiProvider {
    val id: String
    val type: AiProviderType
    val config: AiProviderConfig

    suspend fun testConnection(apiKey: String): ConnectionTestResult

    suspend fun fetchModels(apiKey: String): List<AiModelInfo>

    suspend fun generateResponse(
        prompt: String,
        history: List<ConversationEntity>,
        userName: String,
        apiKey: String
    ): AiAssistantResponse
}
