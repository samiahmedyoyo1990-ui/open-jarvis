package com.example.sync

import android.content.Context
import android.util.Base64
import com.example.data.local.JarvisDatabase
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.RoutineEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.UserMemoryEntity
import com.example.data.local.entity.WorkspaceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

data class SyncDeviceProfile(
    val deviceId: String,
    val deviceName: String,
    val lastSyncTime: Long = System.currentTimeMillis()
)

data class SyncResult(
    val isSuccess: Boolean,
    val message: String,
    val itemsRestoredCount: Int = 0
)

class SyncEngine(
    private val context: Context,
    private val database: JarvisDatabase
) {
    private val prefs = context.getSharedPreferences("jarvis_sync_prefs", Context.MODE_PRIVATE)

    fun getDeviceProfile(): SyncDeviceProfile {
        var id = prefs.getString("sync_device_id", null)
        if (id == null) {
            id = "jarvis_dev_${UUID.randomUUID().toString().take(8)}"
            prefs.edit().putString("sync_device_id", id).apply()
        }
        val name = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
        val lastSync = prefs.getLong("last_sync_time", 0L)
        return SyncDeviceProfile(id, name, lastSync)
    }

    suspend fun generateEncryptedBackup(passkey: String): String = withContext(Dispatchers.IO) {
        val notes = database.noteDao().getAllNotes().first()
        val tasks = database.taskDao().getAllTasks().first()
        val memories = database.userMemoryDao().getAllMemories().first()
        val workspaces = database.workspaceDao().getAllWorkspaces().first()
        val routines = database.routineDao().getAllRoutines().first()

        val root = JSONObject().apply {
            put("version", 1)
            put("timestamp", System.currentTimeMillis())
            put("deviceId", getDeviceProfile().deviceId)

            // Notes
            val notesArr = JSONArray()
            notes.forEach {
                notesArr.put(JSONObject().apply {
                    put("title", it.title)
                    put("content", it.content)
                    put("category", it.category)
                    put("timestamp", it.timestamp)
                })
            }
            put("notes", notesArr)

            // Tasks
            val tasksArr = JSONArray()
            tasks.forEach {
                tasksArr.put(JSONObject().apply {
                    put("title", it.title)
                    put("isCompleted", it.isCompleted)
                    put("priority", it.priority)
                    put("dueDate", it.dueDate ?: 0L)
                })
            }
            put("tasks", tasksArr)

            // Memories
            val memArr = JSONArray()
            memories.forEach {
                memArr.put(JSONObject().apply {
                    put("key", it.memoryKey)
                    put("value", it.memoryValue)
                })
            }
            put("memories", memArr)

            // Workspaces
            val wsArr = JSONArray()
            workspaces.forEach {
                wsArr.put(JSONObject().apply {
                    put("slug", it.slug)
                    put("name", it.name)
                    put("description", it.description)
                    put("systemInstructions", it.systemInstructions)
                    put("colorHex", it.colorHex)
                })
            }
            put("workspaces", wsArr)

            // Routines
            val rArr = JSONArray()
            routines.forEach {
                rArr.put(JSONObject().apply {
                    put("name", it.name)
                    put("triggerType", it.triggerType)
                    put("triggerValue", it.triggerValue)
                    put("actionType", it.actionType)
                    put("actionPayload", it.actionPayload)
                    put("isEnabled", it.isEnabled)
                })
            }
            put("routines", rArr)
        }

        val plainJson = root.toString()
        encryptString(plainJson, passkey)
    }

    suspend fun restoreEncryptedBackup(encryptedPayload: String, passkey: String): SyncResult =
        withContext(Dispatchers.IO) {
            try {
                val plainJson = decryptString(encryptedPayload, passkey)
                    ?: return@withContext SyncResult(false, "ডিক্রিপশন ব্যর্থ হয়েছে। পাসকি সঠিক নয় বা ফাইল ক্ষতিগ্রস্ত।")

                val root = JSONObject(plainJson)
                var restoredCount = 0

                // Notes
                val notesArr = root.optJSONArray("notes")
                if (notesArr != null) {
                    for (i in 0 until notesArr.length()) {
                        val obj = notesArr.getJSONObject(i)
                        database.noteDao().insertNote(
                            NoteEntity(
                                title = obj.optString("title", ""),
                                content = obj.optString("content", ""),
                                category = obj.optString("category", "সাধারণ")
                            )
                        )
                        restoredCount++
                    }
                }

                // Tasks
                val tasksArr = root.optJSONArray("tasks")
                if (tasksArr != null) {
                    for (i in 0 until tasksArr.length()) {
                        val obj = tasksArr.getJSONObject(i)
                        database.taskDao().insertTask(
                            TaskEntity(
                                title = obj.optString("title", ""),
                                isCompleted = obj.optBoolean("isCompleted", false),
                                priority = obj.optString("priority", "Normal")
                            )
                        )
                        restoredCount++
                    }
                }

                // Memories
                val memArr = root.optJSONArray("memories")
                if (memArr != null) {
                    for (i in 0 until memArr.length()) {
                        val obj = memArr.getJSONObject(i)
                        database.userMemoryDao().insertMemory(
                            UserMemoryEntity(
                                memoryKey = obj.optString("key", ""),
                                memoryValue = obj.optString("value", "")
                            )
                        )
                        restoredCount++
                    }
                }

                // Workspaces
                val wsArr = root.optJSONArray("workspaces")
                if (wsArr != null) {
                    for (i in 0 until wsArr.length()) {
                        val obj = wsArr.getJSONObject(i)
                        database.workspaceDao().insertWorkspace(
                            WorkspaceEntity(
                                slug = obj.optString("slug", "ws_$i"),
                                name = obj.optString("name", "Workspace"),
                                description = obj.optString("description", ""),
                                systemInstructions = obj.optString("systemInstructions", ""),
                                colorHex = obj.optString("colorHex", "#00D9FF")
                            )
                        )
                        restoredCount++
                    }
                }

                prefs.edit().putLong("last_sync_time", System.currentTimeMillis()).apply()

                SyncResult(
                    isSuccess = true,
                    message = "সফলভাবে $restoredCount টি ডেটা রিস্টোর সম্পন্ন হয়েছে।",
                    itemsRestoredCount = restoredCount
                )
            } catch (e: Exception) {
                SyncResult(false, "রিস্টোর ত্রুটি: ${e.localizedMessage}")
            }
        }

    internal fun encryptString(plain: String, passkey: String): String {
        val keyBytes = MessageDigest.getInstance("SHA-256").digest(passkey.toByteArray(Charsets.UTF_8))
        val secretKey = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val iv = ByteArray(16) { 0x42 } // Deterministic or prepended IV
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    internal fun decryptString(encryptedBase64: String, passkey: String): String? {
        return try {
            val keyBytes = MessageDigest.getInstance("SHA-256").digest(passkey.toByteArray(Charsets.UTF_8))
            val secretKey = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val iv = ByteArray(16) { 0x42 }
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
            val decrypted = cipher.doFinal(Base64.decode(encryptedBase64, Base64.NO_WRAP))
            String(decrypted, Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }
}
