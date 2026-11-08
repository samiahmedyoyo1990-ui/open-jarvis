package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.DevWorkspaceRepoEntity
import com.example.data.local.entity.InstalledSkillEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.RoutineEntity
import com.example.data.local.entity.SkillAuditLogEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.UsageLogEntity
import com.example.data.local.entity.UserMemoryEntity
import com.example.data.local.entity.WorkspaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: Long): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchNotes(query: String): Flow<List<NoteEntity>>
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, timestamp DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("UPDATE tasks SET isCompleted = :completed WHERE id = :id")
    suspend fun setTaskCompleted(id: Long, completed: Boolean)
}

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY timestamp ASC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentConversations(limit: Int = 50): Flow<List<ConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ConversationEntity): Long

    @Query("DELETE FROM conversations")
    suspend fun clearAllConversations()

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversationById(id: Long)
}

@Dao
interface UserMemoryDao {
    @Query("SELECT * FROM user_memory ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<UserMemoryEntity>>

    @Query("SELECT * FROM user_memory WHERE memoryKey = :key LIMIT 1")
    suspend fun getMemoryByKey(key: String): UserMemoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: UserMemoryEntity): Long

    @Delete
    suspend fun deleteMemory(memory: UserMemoryEntity)

    @Query("DELETE FROM user_memory")
    suspend fun clearAllMemories()
}

@Dao
interface WorkspaceDao {
    @Query("SELECT * FROM workspaces ORDER BY createdAt ASC")
    fun getAllWorkspaces(): Flow<List<WorkspaceEntity>>

    @Query("SELECT * FROM workspaces WHERE slug = :slug LIMIT 1")
    suspend fun getWorkspaceBySlug(slug: String): WorkspaceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkspace(workspace: WorkspaceEntity): Long

    @Update
    suspend fun updateWorkspace(workspace: WorkspaceEntity)

    @Delete
    suspend fun deleteWorkspace(workspace: WorkspaceEntity)

    @Query("DELETE FROM workspaces WHERE id = :id")
    suspend fun deleteWorkspaceById(id: Long)
}

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines ORDER BY createdAt DESC")
    fun getAllRoutines(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines WHERE isEnabled = 1")
    fun getEnabledRoutines(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines WHERE id = :id LIMIT 1")
    suspend fun getRoutineById(id: Long): RoutineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineEntity): Long

    @Update
    suspend fun updateRoutine(routine: RoutineEntity)

    @Delete
    suspend fun deleteRoutine(routine: RoutineEntity)

    @Query("UPDATE routines SET isEnabled = :enabled WHERE id = :id")
    suspend fun setRoutineEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE routines SET lastExecutedAt = :time WHERE id = :id")
    suspend fun updateLastExecuted(id: Long, time: Long)

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun deleteRoutineById(id: Long)
}

@Dao
interface UsageLogDao {
    @Query("SELECT * FROM usage_logs ORDER BY timestamp DESC")
    fun getAllUsageLogs(): Flow<List<UsageLogEntity>>

    @Query("SELECT * FROM usage_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentUsageLogs(limit: Int = 100): Flow<List<UsageLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageLog(log: UsageLogEntity): Long

    @Query("SELECT SUM(totalTokens) FROM usage_logs")
    suspend fun getTotalTokensUsed(): Long?

    @Query("SELECT SUM(estimatedCostUsd) FROM usage_logs")
    suspend fun getTotalEstimatedCost(): Double?

    @Query("DELETE FROM usage_logs")
    suspend fun clearUsageLogs()
}

@Dao
interface InstalledSkillDao {
    @Query("SELECT * FROM installed_skills ORDER BY installedAt DESC")
    fun getAllInstalledSkills(): Flow<List<InstalledSkillEntity>>

    @Query("SELECT * FROM installed_skills WHERE isEnabled = 1")
    fun getEnabledInstalledSkills(): Flow<List<InstalledSkillEntity>>

    @Query("SELECT * FROM installed_skills WHERE id = :id LIMIT 1")
    suspend fun getInstalledSkillById(id: Long): InstalledSkillEntity?

    @Query("SELECT * FROM installed_skills WHERE skillId = :skillId LIMIT 1")
    suspend fun getInstalledSkillByIdentifier(skillId: String): InstalledSkillEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstalledSkill(skill: InstalledSkillEntity): Long

    @Update
    suspend fun updateInstalledSkill(skill: InstalledSkillEntity)

    @Delete
    suspend fun deleteInstalledSkill(skill: InstalledSkillEntity)

    @Query("DELETE FROM installed_skills WHERE id = :id")
    suspend fun deleteInstalledSkillById(id: Long)

    @Query("UPDATE installed_skills SET isEnabled = :enabled WHERE id = :id")
    suspend fun setSkillEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE installed_skills SET grantedPermissionsJson = :permissionsJson WHERE id = :id")
    suspend fun updateSkillPermissions(id: Long, permissionsJson: String)
}

@Dao
interface DevWorkspaceRepoDao {
    @Query("SELECT * FROM dev_workspace_repos ORDER BY importedAt DESC")
    fun getAllDevRepos(): Flow<List<DevWorkspaceRepoEntity>>

    @Query("SELECT * FROM dev_workspace_repos WHERE id = :id LIMIT 1")
    suspend fun getDevRepoById(id: Long): DevWorkspaceRepoEntity?

    @Query("SELECT * FROM dev_workspace_repos WHERE url = :url LIMIT 1")
    suspend fun getDevRepoByUrl(url: String): DevWorkspaceRepoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevRepo(repo: DevWorkspaceRepoEntity): Long

    @Update
    suspend fun updateDevRepo(repo: DevWorkspaceRepoEntity)

    @Delete
    suspend fun deleteDevRepo(repo: DevWorkspaceRepoEntity)

    @Query("DELETE FROM dev_workspace_repos WHERE id = :id")
    suspend fun deleteDevRepoById(id: Long)
}

@Dao
interface SkillAuditLogDao {
    @Query("SELECT * FROM skill_audit_logs ORDER BY scannedAt DESC LIMIT :limit")
    fun getRecentAuditLogs(limit: Int = 50): Flow<List<SkillAuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: SkillAuditLogEntity): Long

    @Query("DELETE FROM skill_audit_logs")
    suspend fun clearAuditLogs()
}
