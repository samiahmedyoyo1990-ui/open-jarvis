package com.example.data.repository

import com.example.data.local.JarvisDatabase
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.UserMemoryEntity
import com.example.data.preferences.JarvisPreferences
import kotlinx.coroutines.flow.Flow

class JarvisRepository(
    private val database: JarvisDatabase,
    val preferences: JarvisPreferences
) {
    // Notes
    val allNotes: Flow<List<NoteEntity>> = database.noteDao().getAllNotes()

    suspend fun saveNote(title: String, content: String, category: String = "সাধারণ"): Long {
        val note = NoteEntity(title = title.trim(), content = content.trim(), category = category)
        return database.noteDao().insertNote(note)
    }

    suspend fun updateNote(note: NoteEntity) = database.noteDao().updateNote(note)

    suspend fun deleteNote(note: NoteEntity) = database.noteDao().deleteNote(note)

    suspend fun deleteNoteById(id: Long) = database.noteDao().deleteNoteById(id)

    fun searchNotes(query: String): Flow<List<NoteEntity>> = database.noteDao().searchNotes(query)

    // Tasks
    val allTasks: Flow<List<TaskEntity>> = database.taskDao().getAllTasks()

    suspend fun addTask(
        title: String,
        dueDate: Long? = null,
        priority: String = "Normal",
        reminderSet: Boolean = false
    ): Long {
        val task = TaskEntity(
            title = title.trim(),
            dueDate = dueDate,
            priority = priority,
            reminderSet = reminderSet
        )
        return database.taskDao().insertTask(task)
    }

    suspend fun setTaskCompleted(id: Long, completed: Boolean) {
        database.taskDao().setTaskCompleted(id, completed)
    }

    suspend fun deleteTask(task: TaskEntity) = database.taskDao().deleteTask(task)

    suspend fun deleteTaskById(id: Long) = database.taskDao().deleteTaskById(id)

    // Conversations
    val recentConversations: Flow<List<ConversationEntity>> =
        database.conversationDao().getRecentConversations(100)

    suspend fun addConversationMessage(
        isUser: Boolean,
        text: String,
        actionExecuted: String? = null
    ): Long {
        val message = ConversationEntity(
            isUser = isUser,
            text = text,
            actionExecuted = actionExecuted
        )
        return database.conversationDao().insertMessage(message)
    }

    suspend fun clearConversationHistory() {
        database.conversationDao().clearAllConversations()
    }

    // Memories
    val allMemories: Flow<List<UserMemoryEntity>> = database.userMemoryDao().getAllMemories()

    suspend fun saveMemory(key: String, value: String): Long {
        return database.userMemoryDao().insertMemory(
            UserMemoryEntity(memoryKey = key, memoryValue = value)
        )
    }

    suspend fun deleteMemory(memory: UserMemoryEntity) {
        database.userMemoryDao().deleteMemory(memory)
    }

    suspend fun clearAllMemories() {
        database.userMemoryDao().clearAllMemories()
    }

    // Workspaces (System 18)
    val allWorkspaces: Flow<List<com.example.data.local.entity.WorkspaceEntity>> =
        database.workspaceDao().getAllWorkspaces()

    suspend fun initDefaultWorkspacesIfEmpty() {
        val defaults = listOf(
            com.example.data.local.entity.WorkspaceEntity(
                slug = "personal",
                name = "Personal",
                description = "ব্যক্তিগত আলোচনা, দৈনন্দিন পরিকল্পনা ও নোটস",
                systemInstructions = "You are in the Personal Workspace. Assist warmly with daily life, personal planning, and reflections.",
                colorHex = "#00D9FF",
                iconName = "Person"
            ),
            com.example.data.local.entity.WorkspaceEntity(
                slug = "business",
                name = "Business & Strategy",
                description = "ব্যবসায়িক পরিকল্পনা, ইমেইল ড্রাফট ও অর্থনৈতিক বিশ্লেষণ",
                systemInstructions = "You are in the Business Workspace. Focus on professional communication, business analysis, and ROI.",
                colorHex = "#8B5CF6",
                iconName = "Business"
            ),
            com.example.data.local.entity.WorkspaceEntity(
                slug = "coding",
                name = "Coding & Dev",
                description = "অ্যান্ড্রয়েড আর্কিটেকচার, কোটলিন, পাইথন ও অ্যালগরিদম",
                systemInstructions = "You are in the Coding Workspace. Provide clean, production-grade code, debugging tips, and architectural patterns.",
                colorHex = "#00E676",
                iconName = "Code"
            ),
            com.example.data.local.entity.WorkspaceEntity(
                slug = "study",
                name = "Study & Research",
                description = "শিক্ষা, বৈজ্ঞানিক গবেষণা ও জটিল বিষয় ব্যাখ্যা",
                systemInstructions = "You are in the Study Workspace. Explain concepts intuitively, provide academic summaries, and quiz the user.",
                colorHex = "#FFB300",
                iconName = "School"
            ),
            com.example.data.local.entity.WorkspaceEntity(
                slug = "creative",
                name = "Creative Writing",
                description = "গল্প, বাংলা কবিতা, ভিডিও স্ক্রিপ্ট ও সৃজনশীল আইডিয়া",
                systemInstructions = "You are in the Creative Workspace. Use imaginative metaphors, vivid imagery, and engaging tone.",
                colorHex = "#EC4899",
                iconName = "Brush"
            )
        )
        for (ws in defaults) {
            val existing = database.workspaceDao().getWorkspaceBySlug(ws.slug)
            if (existing == null) {
                database.workspaceDao().insertWorkspace(ws)
            }
        }
    }

    suspend fun saveWorkspace(workspace: com.example.data.local.entity.WorkspaceEntity): Long {
        return database.workspaceDao().insertWorkspace(workspace)
    }

    suspend fun deleteWorkspaceById(id: Long) {
        database.workspaceDao().deleteWorkspaceById(id)
    }

    // Routines (System 07)
    val allRoutines: Flow<List<com.example.data.local.entity.RoutineEntity>> =
        database.routineDao().getAllRoutines()

    val enabledRoutines: Flow<List<com.example.data.local.entity.RoutineEntity>> =
        database.routineDao().getEnabledRoutines()

    suspend fun initDefaultRoutinesIfEmpty() {
        val defaults = listOf(
            com.example.data.local.entity.RoutineEntity(
                name = "Good Morning Routine",
                triggerType = "VOICE",
                triggerValue = "শুভ সকাল",
                actionType = "SPEAK",
                actionPayload = "শুভ সকাল স্যার! আজকের দিনের আবহাওয়া ও গুরুত্বপূর্ণ কাজসমূহ প্রস্তুত।"
            ),
            com.example.data.local.entity.RoutineEntity(
                name = "Work Mode",
                triggerType = "VOICE",
                triggerValue = "কাজে বসছি",
                actionType = "OPEN_APP",
                actionPayload = "Chrome"
            ),
            com.example.data.local.entity.RoutineEntity(
                name = "Study Mode",
                triggerType = "VOICE",
                triggerValue = "পড়াশোনা শুরু",
                actionType = "SPEAK",
                actionPayload = "স্টাডি মোড সক্রিয়। ডিস্টার্ব না করে মনোযোগ দিয়ে পড়ুন।"
            ),
            com.example.data.local.entity.RoutineEntity(
                name = "Night Mode Routine",
                triggerType = "VOICE",
                triggerValue = "শুভ রাত্রি",
                actionType = "SPEAK",
                actionPayload = "শুভ রাত্রি স্যার! আজকের কাজগুলো সেভ করা হয়েছে। বিশ্রাম নিন।"
            )
        )
        // Check if empty
        for (r in defaults) {
            database.routineDao().insertRoutine(r)
        }
    }

    suspend fun saveRoutine(routine: com.example.data.local.entity.RoutineEntity): Long {
        return database.routineDao().insertRoutine(routine)
    }

    suspend fun toggleRoutine(id: Long, enabled: Boolean) {
        database.routineDao().setRoutineEnabled(id, enabled)
    }

    suspend fun deleteRoutineById(id: Long) {
        database.routineDao().deleteRoutineById(id)
    }

    suspend fun markRoutineExecuted(id: Long) {
        database.routineDao().updateLastExecuted(id, System.currentTimeMillis())
    }

    // Usage Logs (System 21)
    val allUsageLogs: Flow<List<com.example.data.local.entity.UsageLogEntity>> =
        database.usageLogDao().getAllUsageLogs()

    suspend fun recordUsage(
        providerId: String,
        modelId: String,
        promptTokens: Int,
        completionTokens: Int,
        latencyMs: Long,
        estimatedCostUsd: Double
    ): Long {
        val log = com.example.data.local.entity.UsageLogEntity(
            providerId = providerId,
            modelId = modelId,
            promptTokens = promptTokens,
            completionTokens = completionTokens,
            totalTokens = promptTokens + completionTokens,
            latencyMs = latencyMs,
            estimatedCostUsd = estimatedCostUsd
        )
        return database.usageLogDao().insertUsageLog(log)
    }

    suspend fun clearUsageLogs() {
        database.usageLogDao().clearUsageLogs()
    }

    // Installed Skills (Universal GitHub & Skill Installer)
    val allInstalledSkills: Flow<List<com.example.data.local.entity.InstalledSkillEntity>> =
        database.installedSkillDao().getAllInstalledSkills()

    val enabledInstalledSkills: Flow<List<com.example.data.local.entity.InstalledSkillEntity>> =
        database.installedSkillDao().getEnabledInstalledSkills()

    suspend fun getInstalledSkillById(id: Long): com.example.data.local.entity.InstalledSkillEntity? {
        return database.installedSkillDao().getInstalledSkillById(id)
    }

    suspend fun getInstalledSkillByIdentifier(skillId: String): com.example.data.local.entity.InstalledSkillEntity? {
        return database.installedSkillDao().getInstalledSkillByIdentifier(skillId)
    }

    suspend fun saveInstalledSkill(skill: com.example.data.local.entity.InstalledSkillEntity): Long {
        return database.installedSkillDao().insertInstalledSkill(skill)
    }

    suspend fun updateInstalledSkill(skill: com.example.data.local.entity.InstalledSkillEntity) {
        database.installedSkillDao().updateInstalledSkill(skill)
    }

    suspend fun deleteInstalledSkill(id: Long) {
        database.installedSkillDao().deleteInstalledSkillById(id)
    }

    suspend fun setSkillEnabled(id: Long, enabled: Boolean) {
        database.installedSkillDao().setSkillEnabled(id, enabled)
    }

    suspend fun updateSkillPermissions(id: Long, permissionsJson: String) {
        database.installedSkillDao().updateSkillPermissions(id, permissionsJson)
    }

    // Developer Workspace (Source Repositories)
    val allDevRepos: Flow<List<com.example.data.local.entity.DevWorkspaceRepoEntity>> =
        database.devWorkspaceRepoDao().getAllDevRepos()

    suspend fun getDevRepoById(id: Long): com.example.data.local.entity.DevWorkspaceRepoEntity? {
        return database.devWorkspaceRepoDao().getDevRepoById(id)
    }

    suspend fun getDevRepoByUrl(url: String): com.example.data.local.entity.DevWorkspaceRepoEntity? {
        return database.devWorkspaceRepoDao().getDevRepoByUrl(url)
    }

    suspend fun saveDevRepo(repo: com.example.data.local.entity.DevWorkspaceRepoEntity): Long {
        return database.devWorkspaceRepoDao().insertDevRepo(repo)
    }

    suspend fun updateDevRepo(repo: com.example.data.local.entity.DevWorkspaceRepoEntity) {
        database.devWorkspaceRepoDao().updateDevRepo(repo)
    }

    suspend fun deleteDevRepo(id: Long) {
        database.devWorkspaceRepoDao().deleteDevRepoById(id)
    }

    // Audit Logs
    val recentAuditLogs: Flow<List<com.example.data.local.entity.SkillAuditLogEntity>> =
        database.skillAuditLogDao().getRecentAuditLogs(50)

    suspend fun recordAuditLog(log: com.example.data.local.entity.SkillAuditLogEntity): Long {
        return database.skillAuditLogDao().insertAuditLog(log)
    }
}
