package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val category: String = "সাধারণ"
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val isCompleted: Boolean = false,
    val dueDate: Long? = null,
    val priority: String = "Normal", // Low, Normal, High
    val reminderSet: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionExecuted: String? = null
)

@Entity(tableName = "user_memory")
data class UserMemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val memoryKey: String,
    val memoryValue: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "workspaces")
data class WorkspaceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val slug: String,
    val name: String,
    val description: String,
    val systemInstructions: String,
    val colorHex: String = "#00D9FF",
    val iconName: String = "Work",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val triggerType: String, // VOICE, TIME, APP_LAUNCH
    val triggerValue: String, // e.g. "Good morning", "08:00", "YouTube"
    val actionType: String, // SPEAK, OPEN_APP, WEATHER_BRIEF, SYSTEM_TOGGLE
    val actionPayload: String,
    val isEnabled: Boolean = true,
    val lastExecutedAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "usage_logs")
data class UsageLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val providerId: String,
    val modelId: String,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val latencyMs: Long,
    val estimatedCostUsd: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "installed_skills")
data class InstalledSkillEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val skillId: String,
    val name: String,
    val banglaName: String,
    val author: String,
    val version: String,
    val description: String,
    val category: String,
    val skillType: String = "SKILL_RUNTIME", // SKILL_RUNTIME, AGENT_INSTRUCTION, PROMPT_PACK, DEV_SOURCE
    val repoUrl: String,
    val repoBranch: String = "main",
    val commitSha: String = "",
    val license: String = "Unknown",
    val installedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isEnabled: Boolean = true,
    val grantedPermissionsJson: String = "[]",
    val manifestJson: String = "{}",
    val systemPromptSnippet: String = "",
    val toolsJson: String = "[]",
    val integrityHash: String = "",
    val previousVersionBackupJson: String? = null
)

@Entity(tableName = "dev_workspace_repos")
data class DevWorkspaceRepoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val repoName: String,
    val owner: String,
    val url: String,
    val defaultBranch: String = "main",
    val description: String = "",
    val language: String = "",
    val stars: Int = 0,
    val readmeContent: String = "",
    val fileTreeJson: String = "[]",
    val importedAt: Long = System.currentTimeMillis(),
    val aiAnalysisNotes: String = ""
)

@Entity(tableName = "skill_audit_logs")
data class SkillAuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val repoUrl: String,
    val status: String,
    val securityScore: Int,
    val findingsJson: String,
    val scannedAt: Long = System.currentTimeMillis()
)
