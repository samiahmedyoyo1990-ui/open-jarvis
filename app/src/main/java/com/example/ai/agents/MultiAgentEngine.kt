package com.example.ai.agents

import com.example.ai.UniversalAiEngine
import com.example.data.local.entity.ConversationEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AgentRole(
    val id: String,
    val title: String,
    val banglaTitle: String,
    val description: String,
    val systemPrompt: String
) {
    PLANNING(
        id = "planning",
        title = "Planning Agent",
        banglaTitle = "প্ল্যানিং এজেন্ট",
        description = "জটিল সমস্যাকে ধাপে ধাপে বাস্তবায়নযোগ্য পরিকল্পনায় রূপান্তর করে।",
        systemPrompt = "You are JARVIS's Planning Agent. Deconstruct the user's objective into clear, logical, numbered sub-tasks."
    ),
    RESEARCH(
        id = "research",
        title = "Research Agent",
        banglaTitle = "রিসার্চ এজেন্ট",
        description = "প্রয়োজনীয় তথ্যসূত্র, প্রাসঙ্গিক জ্ঞান ও প্রমাণাদি অনুসন্ধান করে।",
        systemPrompt = "You are JARVIS's Research Agent. Retrieve, analyze, and synthesize the essential facts, data, and context required for the user's inquiry."
    ),
    ANALYSIS(
        id = "analysis",
        title = "Analysis Agent",
        banglaTitle = "অ্যানালাইসিস এজেন্ট",
        description = "সুবিধা-অসুবিধা, সম্ভাব্য ঝুঁকি ও যৌক্তিক ফলাফল বিশ্লেষণ করে।",
        systemPrompt = "You are JARVIS's Analysis Agent. Critically evaluate strengths, trade-offs, edge-cases, and strategic implications."
    ),
    CODING(
        id = "coding",
        title = "Coding Agent",
        banglaTitle = "কোডিং এজেন্ট",
        description = "উন্নত কোড তৈরি, বাগ শনাক্তকরণ ও পারফরম্যান্স অপটিমাইজেশন করে।",
        systemPrompt = "You are JARVIS's Coding Agent. Generate elegant, clean, type-safe, production-ready code with explanatory architecture."
    ),
    CREATIVE(
        id = "creative",
        title = "Creative Agent",
        banglaTitle = "ক্রিয়েটিভ এজেন্ট",
        description = "সৃজনশীল ভাবনা, সুন্দর ভাষাভঙ্গী ও অনুপ্রেরণাদায়ী উপস্থাপনা।",
        systemPrompt = "You are JARVIS's Creative Agent. Bring imaginative flair, vivid storytelling, and compelling narratives."
    ),
    VERIFICATION(
        id = "verification",
        title = "Verification Agent",
        banglaTitle = "ভেরিফিকেশন এজেন্ট",
        description = "তথ্যগত অসঙ্গতি পরীক্ষা ও দাবির যথার্থতা যাচাই করে।",
        systemPrompt = "You are JARVIS's Verification Agent. Cross-examine claims, check consistency, and ensure factual integrity."
    ),
    REVIEWER(
        id = "reviewer",
        title = "Reviewer Agent",
        banglaTitle = "রিভিউয়ার এজেন্ট",
        description = "চূড়ান্ত সমন্বয় সাধন করে সহজবোধ্য ও কার্যকর উত্তর প্রস্তুত করে।",
        systemPrompt = "You are JARVIS's Reviewer Agent. Review outputs from sibling agents and synthesize a unified, articulate, polished master answer in natural Bangla/English."
    )
}

data class AgentStepResult(
    val role: AgentRole,
    val output: String,
    val status: String = "Completed",
    val latencyMs: Long = 0L
)

data class MultiAgentSession(
    val query: String,
    val activeAgents: List<AgentRole>,
    val steps: List<AgentStepResult> = emptyList(),
    val finalSynthesis: String = "",
    val isRunning: Boolean = false
)

class MultiAgentEngine(private val aiEngine: UniversalAiEngine) {

    private val _currentSession = MutableStateFlow<MultiAgentSession?>(null)
    val currentSession: StateFlow<MultiAgentSession?> = _currentSession.asStateFlow()

    suspend fun executeCollaboration(
        query: String,
        selectedRoles: List<AgentRole> = listOf(AgentRole.PLANNING, AgentRole.RESEARCH, AgentRole.REVIEWER)
    ): MultiAgentSession {
        val initialSession = MultiAgentSession(
            query = query,
            activeAgents = selectedRoles,
            isRunning = true
        )
        _currentSession.value = initialSession

        val completedSteps = mutableListOf<AgentStepResult>()
        var cumulativeContext = "User Query: $query\n\n"

        for (role in selectedRoles) {
            val startTime = System.currentTimeMillis()
            val prompt = buildString {
                append(role.systemPrompt)
                append("\n\nContext accumulated so far:\n")
                append(cumulativeContext)
                append("\nPerform your specialized duty as ${role.title} for the user request.")
            }

            val response = aiEngine.executeAiQuery(prompt, emptyList())
            val duration = System.currentTimeMillis() - startTime
            val result = AgentStepResult(
                role = role,
                output = response.replyText,
                latencyMs = duration
            )
            completedSteps.add(result)
            cumulativeContext += "--- [${role.title} Output] ---\n${response.replyText}\n\n"

            _currentSession.value = initialSession.copy(
                steps = completedSteps.toList()
            )
        }

        // Final Master Synthesis if reviewer wasn't last
        val finalAnswer = if (selectedRoles.lastOrNull() == AgentRole.REVIEWER) {
            completedSteps.last().output
        } else {
            val synthesisPrompt = buildString {
                append("You are JARVIS Orchestrator. Synthesize the findings of your specialized agents into a unified, polite, and cohesive response in Bangla (with English technical terms where appropriate):\n\n")
                append(cumulativeContext)
            }
            aiEngine.executeAiQuery(synthesisPrompt, emptyList()).replyText
        }

        val finishedSession = MultiAgentSession(
            query = query,
            activeAgents = selectedRoles,
            steps = completedSteps,
            finalSynthesis = finalAnswer,
            isRunning = false
        )
        _currentSession.value = finishedSession
        return finishedSession
    }

    fun clearSession() {
        _currentSession.value = null
    }
}
