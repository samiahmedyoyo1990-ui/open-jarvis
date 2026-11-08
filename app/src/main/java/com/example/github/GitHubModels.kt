package com.example.github

import org.json.JSONArray
import org.json.JSONObject

enum class CompatibilityType(val label: String, val banglaLabel: String) {
    JARVIS_SKILL("JARVIS Native Skill", "জার্ভিস নেটিভ স্কিল"),
    AGENT_INSTRUCTION("AI Agent Instructions", "এআই এজেন্ট নির্দেশিকা"),
    PROMPT_PACK("Prompt Pack", "প্রম্পট প্যাক"),
    CONFIG_RESOURCE("Config & Resource Pack", "কনফিগারেশন ও রিসোর্স"),
    DEV_SOURCE_PROJECT("Developer Source Code", "ডেভেলপার সোর্স কোড"),
    UNSUPPORTED("Unsupported Package", "অসমর্থিত প্যাকেজ"),
    BLOCKED("Blocked / Malicious", "নিরাপত্তা ঝুঁকিপূর্ণ (ব্লকড)")
}

enum class AuditStatus(val label: String, val banglaLabel: String) {
    COMPATIBLE("Compatible", "সামঞ্জস্যপূর্ণ"),
    SAFE_TO_REVIEW("Safe to Review", "পর্যালোচনাযোগ্য"),
    NEEDS_PERMISSION("Needs Permission", "অনুমতি আবশ্যক"),
    WARNING("Security Warning", "সতর্কতা"),
    UNSUPPORTED("Unsupported", "অসমর্থিত"),
    BLOCKED("Blocked / Unsafe", "ব্লকড / অনিরাপদ")
}

data class GitHubRepoItem(
    val id: Long,
    val name: String,
    val fullName: String,
    val owner: String,
    val description: String,
    val stars: Int,
    val forks: Int,
    val language: String,
    val license: String,
    val defaultBranch: String,
    val htmlUrl: String,
    val topics: List<String> = emptyList(),
    val updatedAt: String = ""
)

data class GitHubContentItem(
    val name: String,
    val path: String,
    val sha: String,
    val size: Long,
    val type: String, // "file" or "dir"
    val downloadUrl: String? = null,
    val contentBase64: String? = null
)

data class GitHubBranchItem(
    val name: String,
    val commitSha: String
)

data class GitHubReleaseItem(
    val tagName: String,
    val name: String,
    val body: String,
    val publishedAt: String,
    val htmlUrl: String
)

data class SecurityFinding(
    val level: String, // "INFO", "WARNING", "CRITICAL"
    val message: String,
    val affectedItem: String? = null
)

data class SecurityAuditReport(
    val repoUrl: String,
    val repoFullName: String,
    val branch: String,
    val commitSha: String,
    val compatibilityType: CompatibilityType,
    val status: AuditStatus,
    val integritySha256: String,
    val detectedManifestName: String?,
    val requestedPermissions: List<String>,
    val findings: List<SecurityFinding>,
    val skillManifest: SkillManifest?,
    val isInstallable: Boolean,
    val isDeveloperSource: Boolean,
    val rawReadme: String = ""
)

data class SkillManifest(
    val id: String,
    val name: String,
    val banglaName: String = "",
    val version: String = "1.0.0",
    val author: String = "",
    val description: String = "",
    val category: String = "General",
    val type: String = "SKILL_RUNTIME",
    val systemPrompt: String = "",
    val allowedPermissions: List<String> = emptyList(),
    val tools: List<SkillToolDefinition> = emptyList(),
    val sourceRepoUrl: String = "",
    val integrityHash: String = ""
) {
    fun toJsonString(): String {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("banglaName", banglaName)
            put("version", version)
            put("author", author)
            put("description", description)
            put("category", category)
            put("type", type)
            put("systemPrompt", systemPrompt)
            put("permissions", JSONArray(allowedPermissions))
            val toolsArray = JSONArray()
            tools.forEach { toolsArray.put(it.toJson()) }
            put("tools", toolsArray)
            put("sourceRepoUrl", sourceRepoUrl)
            put("integrityHash", integrityHash)
        }.toString()
    }

    companion object {
        fun fromJson(jsonStr: String): SkillManifest? {
            return try {
                val obj = JSONObject(jsonStr)
                val permissionsList = mutableListOf<String>()
                val permsArray = obj.optJSONArray("permissions")
                if (permsArray != null) {
                    for (i in 0 until permsArray.length()) {
                        permissionsList.add(permsArray.getString(i))
                    }
                }
                val toolsList = mutableListOf<SkillToolDefinition>()
                val toolsArray = obj.optJSONArray("tools")
                if (toolsArray != null) {
                    for (i in 0 until toolsArray.length()) {
                        SkillToolDefinition.fromJson(toolsArray.getJSONObject(i))?.let { toolsList.add(it) }
                    }
                }

                SkillManifest(
                    id = obj.optString("id", "skill_${System.currentTimeMillis()}"),
                    name = obj.optString("name", "Custom Skill"),
                    banglaName = obj.optString("banglaName", ""),
                    version = obj.optString("version", "1.0.0"),
                    author = obj.optString("author", "Community"),
                    description = obj.optString("description", ""),
                    category = obj.optString("category", "General"),
                    type = obj.optString("type", "SKILL_RUNTIME"),
                    systemPrompt = obj.optString("systemPrompt", ""),
                    allowedPermissions = permissionsList,
                    tools = toolsList,
                    sourceRepoUrl = obj.optString("sourceRepoUrl", ""),
                    integrityHash = obj.optString("integrityHash", "")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

data class SkillToolDefinition(
    val name: String,
    val description: String,
    val handler: String, // e.g. "tool_web_search", "tool_calculate", "tool_summarize", "tool_device_action"
    val requiredPermission: String? = null
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("name", name)
            put("description", description)
            put("handler", handler)
            put("requiredPermission", requiredPermission)
        }
    }

    companion object {
        fun fromJson(obj: JSONObject): SkillToolDefinition? {
            val name = obj.optString("name", "")
            if (name.isBlank()) return null
            return SkillToolDefinition(
                name = name,
                description = obj.optString("description", ""),
                handler = obj.optString("handler", "tool_summarize"),
                requiredPermission = obj.optString("requiredPermission").takeIf { it.isNotBlank() && it != "null" }
            )
        }
    }
}
