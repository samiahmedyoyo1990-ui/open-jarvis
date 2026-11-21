package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.ConversationDao
import com.example.data.local.dao.DevWorkspaceRepoDao
import com.example.data.local.dao.InstalledSkillDao
import com.example.data.local.dao.NoteDao
import com.example.data.local.dao.RoutineDao
import com.example.data.local.dao.SkillAuditLogDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.dao.UsageLogDao
import com.example.data.local.dao.UserMemoryDao
import com.example.data.local.dao.WorkspaceDao
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

@Database(
    entities = [
        NoteEntity::class,
        TaskEntity::class,
        ConversationEntity::class,
        UserMemoryEntity::class,
        WorkspaceEntity::class,
        RoutineEntity::class,
        UsageLogEntity::class,
        InstalledSkillEntity::class,
        DevWorkspaceRepoEntity::class,
        SkillAuditLogEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class JarvisDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun taskDao(): TaskDao
    abstract fun conversationDao(): ConversationDao
    abstract fun userMemoryDao(): UserMemoryDao
    abstract fun workspaceDao(): WorkspaceDao
    abstract fun routineDao(): RoutineDao
    abstract fun usageLogDao(): UsageLogDao
    abstract fun installedSkillDao(): InstalledSkillDao
    abstract fun devWorkspaceRepoDao(): DevWorkspaceRepoDao
    abstract fun skillAuditLogDao(): SkillAuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: JarvisDatabase? = null

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `workspaces` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `slug` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `systemInstructions` TEXT NOT NULL,
                        `colorHex` TEXT NOT NULL,
                        `iconName` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `routines` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `triggerType` TEXT NOT NULL,
                        `triggerValue` TEXT NOT NULL,
                        `actionType` TEXT NOT NULL,
                        `actionPayload` TEXT NOT NULL,
                        `isEnabled` INTEGER NOT NULL,
                        `lastExecutedAt` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `usage_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `providerId` TEXT NOT NULL,
                        `modelId` TEXT NOT NULL,
                        `promptTokens` INTEGER NOT NULL,
                        `completionTokens` INTEGER NOT NULL,
                        `totalTokens` INTEGER NOT NULL,
                        `latencyMs` INTEGER NOT NULL,
                        `estimatedCostUsd` REAL NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `installed_skills` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `skillId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `banglaName` TEXT NOT NULL,
                        `author` TEXT NOT NULL,
                        `version` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `skillType` TEXT NOT NULL,
                        `repoUrl` TEXT NOT NULL,
                        `repoBranch` TEXT NOT NULL,
                        `commitSha` TEXT NOT NULL,
                        `license` TEXT NOT NULL,
                        `installedAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `isEnabled` INTEGER NOT NULL,
                        `grantedPermissionsJson` TEXT NOT NULL,
                        `manifestJson` TEXT NOT NULL,
                        `systemPromptSnippet` TEXT NOT NULL,
                        `toolsJson` TEXT NOT NULL,
                        `integrityHash` TEXT NOT NULL,
                        `previousVersionBackupJson` TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `dev_workspace_repos` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `repoName` TEXT NOT NULL,
                        `owner` TEXT NOT NULL,
                        `url` TEXT NOT NULL,
                        `defaultBranch` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `language` TEXT NOT NULL,
                        `stars` INTEGER NOT NULL,
                        `readmeContent` TEXT NOT NULL,
                        `fileTreeJson` TEXT NOT NULL,
                        `importedAt` INTEGER NOT NULL,
                        `aiAnalysisNotes` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `skill_audit_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `repoUrl` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `securityScore` INTEGER NOT NULL,
                        `findingsJson` TEXT NOT NULL,
                        `scannedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): JarvisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JarvisDatabase::class.java,
                    "jarvis_memory.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

