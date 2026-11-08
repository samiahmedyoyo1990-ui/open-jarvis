package com.example.github

import android.content.Context
import com.example.actions.JarvisAction
import com.example.actions.JarvisActionEngine
import com.example.ai.UniversalAiEngine
import com.example.ai.research.WebResearchAndFactEngine
import com.example.data.local.entity.InstalledSkillEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray

data class ToolExecutionResult(
    val toolName: String,
    val success: Boolean,
    val output: String,
    val permissionDenied: Boolean = false
)

class SecureSkillRuntime(
    private val context: Context,
    private val aiEngine: UniversalAiEngine,
    private val actionEngine: JarvisActionEngine,
    private val researchEngine: WebResearchAndFactEngine
) {

    /**
     * Builds the aggregated system prompt additions from all active installed skills.
     */
    fun buildActiveSkillsPromptContext(activeSkills: List<InstalledSkillEntity>): String {
        if (activeSkills.isEmpty()) return ""

        return buildString {
            append("\n[ACTIVE INSTALLED SKILLS & AGENT CAPABILITIES]:\n")
            for (skill in activeSkills) {
                append("--- Skill: ${skill.name} (v${skill.version}) ---\n")
                if (skill.systemPromptSnippet.isNotBlank()) {
                    append(skill.systemPromptSnippet.take(1500))
                    append("\n")
                }
            }
            append("---------------------------------------------------\n")
        }
    }

    /**
     * Executes a registered declarative tool with strict permission gates and a 10s execution timeout.
     */
    suspend fun executeSkillTool(
        skill: InstalledSkillEntity,
        toolName: String,
        arguments: String
    ): ToolExecutionResult = withContext(Dispatchers.IO) {
        val grantedPerms = parseGrantedPermissions(skill.grantedPermissionsJson)

        // Safety timeout: 10,000 milliseconds
        val result = withTimeoutOrNull(10000L) {
            when (toolName) {
                "tool_web_search" -> {
                    if (!grantedPerms.contains("NETWORK")) {
                        return@withTimeoutOrNull ToolExecutionResult(
                            toolName, false, "অনুমতি প্রত্যাখ্যান: এই স্কিলের জন্য NETWORK অনুমতি প্রয়োজন।", true
                        )
                    }
                    val res = researchEngine.performResearch(arguments)
                    ToolExecutionResult(toolName, true, res.synthesizedSummary)
                }

                "tool_calculate" -> {
                    val res = actionEngine.parseOfflineIntent(arguments)
                    val out = (res?.first as? JarvisAction.Calculation)?.result ?: "গণনা সম্পন্ন করা যায়নি"
                    ToolExecutionResult(toolName, true, out)
                }

                "tool_device_action" -> {
                    if (!grantedPerms.contains("DEVICE_ACTIONS")) {
                        return@withTimeoutOrNull ToolExecutionResult(
                            toolName, false, "অনুমতি প্রত্যাখ্যান: ডিভাইস নিয়ন্ত্রণের জন্য DEVICE_ACTIONS অনুমতি আবশ্যক।", true
                        )
                    }
                    val actionRes = actionEngine.executeAction(JarvisAction.OpenApp(arguments, null))
                    ToolExecutionResult(toolName, actionRes.success, actionRes.feedbackMessage)
                }

                "tool_summarize" -> {
                    val prompt = "Please provide a concise Bengali summary of the following text:\n\"\"\"\n$arguments\n\"\"\""
                    val aiRes = aiEngine.executeAiQuery(prompt)
                    ToolExecutionResult(toolName, true, aiRes.replyText)
                }

                "tool_fact_check" -> {
                    val claimRes = researchEngine.verifyFacts(arguments)
                    ToolExecutionResult(toolName, true, "ফলাফল: ${claimRes.overallStatus.banglaLabel}\n${claimRes.verifiedSummary}")
                }

                else -> {
                    ToolExecutionResult(toolName, false, "অজ্ঞাত টুল হ্যান্ডলার: $toolName")
                }
            }
        }

        result ?: ToolExecutionResult(toolName, false, "টুল এক্সিকিউশন টাইমআউট হয়েছে (১০ সেকেন্ড অতিক্রম)।")
    }

    private fun parseGrantedPermissions(json: String): List<String> {
        val list = mutableListOf<String>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
        } catch (_: Exception) {}
        return list
    }

    companion object {
        fun enrichPromptWithActiveSkills(basePrompt: String, activeSkills: List<InstalledSkillEntity>): String {
            if (activeSkills.isEmpty()) return basePrompt
            return buildString {
                append(basePrompt)
                append("\n\n[ACTIVE INSTALLED SKILLS & AGENT CAPABILITIES]:\n")
                for (skill in activeSkills) {
                    append("--- Skill: ${skill.name} (v${skill.version}) ---\n")
                    if (skill.systemPromptSnippet.isNotBlank()) {
                        append(skill.systemPromptSnippet.take(1500))
                        append("\n")
                    }
                }
                append("---------------------------------------------------\n")
            }
        }
    }
}
