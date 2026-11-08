package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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

        fun getInstance(context: Context): JarvisDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JarvisDatabase::class.java,
                    "jarvis_memory.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
