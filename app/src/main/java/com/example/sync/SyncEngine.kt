package com.example.sync

import android.content.Context
import android.util.Base64
import androidx.room.withTransaction
import com.example.data.local.JarvisDatabase
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.RoutineEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.UserMemoryEntity
import com.example.data.local.entity.WorkspaceEntity
import com.example.notifications.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
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
    private val secureRandom = SecureRandom()

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
        require(passkey.isNotBlank()) { "Passkey must not be blank" }

        val notes = database.noteDao().getAllNotes().first()
        val tasks = database.taskDao().getAllTasks().first()
        val memories = database.userMemoryDao().getAllMemories().first()
        val workspaces = database.workspaceDao().getAllWorkspaces().first()
        val routines = database.routineDao().getAllRoutines().first()

        val root = JSONObject().apply {
            put("version", 2)
            put("timestamp", System.currentTimeMillis())
            put("deviceId", getDeviceProfile().deviceId)

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

            val tasksArr = JSONArray()
            tasks.forEach {
                tasksArr.put(JSONObject().apply {
                    put("title", it.title)
                    put("isCompleted", it.isCompleted)
                    put("priority", it.priority)
                    put("dueDate", it.dueDate ?: 0L)
                    put("reminderSet", it.reminderSet)
                    put("timestamp", it.timestamp)
                })
            }
            put("tasks", tasksArr)

            val memArr = JSONArray()
            memories.forEach {
                memArr.put(JSONObject().apply {
                    put("key", it.memoryKey)
                    put("value", it.memoryValue)
                    put("timestamp", it.timestamp)
                })
            }
            put("memories", memArr)

            val wsArr = JSONArray()
            workspaces.forEach {
                wsArr.put(JSONObject().apply {
                    put("slug", it.slug)
                    put("name", it.name)
                    put("description", it.description)
                    put("systemInstructions", it.systemInstructions)
                    put("colorHex", it.colorHex)
                    put("iconName", it.iconName)
                })
            }
            put("workspaces", wsArr)

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
            if (passkey.isBlank()) {
                return@withContext SyncResult(false, "ডিক্রিপ্ট করার জন্য পাসকি প্রদান করুন।")
            }
            if (encryptedPayload.isBlank()) {
                return@withContext SyncResult(false, "ব্যাকআপ ডেটা খালি রয়েছে।")
            }
            try {
                val plainJson = decryptString(encryptedPayload.trim(), passkey)
                    ?: return@withContext SyncResult(false, "ডিক্রিপশন ব্যর্থ হয়েছে। পাসকি সঠিক নয় বা ফাইল ক্ষতিগ্রস্ত।")

                val root = JSONObject(plainJson)
                var restoredCount = 0
                val tasksToSchedule = mutableListOf<Triple<Long, TaskEntity, Long>>()

                database.withTransaction {
                    // Existing snapshots for deduplication
                    val existingNotes = database.noteDao().getAllNotes().first()
                        .map { "${it.title}|${it.content}" }.toMutableSet()
                    val existingTasks = database.taskDao().getAllTasks().first()
                        .map { "${it.title}|${it.dueDate ?: 0L}" }.toMutableSet()
                    val existingMemoryKeys = database.userMemoryDao().getAllMemories().first()
                        .associateBy { it.memoryKey }.toMutableMap()
                    val existingWorkspaceSlugs = database.workspaceDao().getAllWorkspaces().first()
                        .map { it.slug }.toMutableSet()
                    val existingRoutines = database.routineDao().getAllRoutines().first()
                        .map { "${it.name}|${it.triggerValue}" }.toMutableSet()

                    // Notes
                    val notesArr = root.optJSONArray("notes")
                    if (notesArr != null) {
                        for (i in 0 until notesArr.length()) {
                            val obj = notesArr.getJSONObject(i)
                            val title = obj.optString("title", "")
                            val content = obj.optString("content", "")
                            val key = "$title|$content"
                            if (existingNotes.add(key)) {
                                database.noteDao().insertNote(
                                    NoteEntity(
                                        title = title,
                                        content = content,
                                        category = obj.optString("category", "সাধারণ"),
                                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                                    )
                                )
                                restoredCount++
                            }
                        }
                    }

                    // Tasks (preserves dueDate, reminderSet, timestamp & reschedules active future reminders)
                    val tasksArr = root.optJSONArray("tasks")
                    if (tasksArr != null) {
                        for (i in 0 until tasksArr.length()) {
                            val obj = tasksArr.getJSONObject(i)
                            val title = obj.optString("title", "")
                            val rawDue = obj.optLong("dueDate", 0L)
                            val dueDate = if (rawDue > 0L) rawDue else null
                            val dedupKey = "$title|${dueDate ?: 0L}"
                            if (existingTasks.add(dedupKey)) {
                                val entity = TaskEntity(
                                    title = title,
                                    isCompleted = obj.optBoolean("isCompleted", false),
                                    dueDate = dueDate,
                                    priority = obj.optString("priority", "Normal"),
                                    reminderSet = obj.optBoolean("reminderSet", dueDate != null),
                                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                                )
                                val insertedId = database.taskDao().insertTask(entity)
                                if (!entity.isCompleted && entity.reminderSet && dueDate != null && dueDate > System.currentTimeMillis()) {
                                    tasksToSchedule.add(Triple(insertedId, entity, dueDate))
                                }
                                restoredCount++
                            }
                        }
                    }

                    // Memories
                    val memArr = root.optJSONArray("memories")
                    if (memArr != null) {
                        for (i in 0 until memArr.length()) {
                            val obj = memArr.getJSONObject(i)
                            val memKey = obj.optString("key", "")
                            val memVal = obj.optString("value", "")
                            if (memKey.isBlank()) continue
                            val existing = existingMemoryKeys[memKey]
                            if (existing == null || existing.memoryValue != memVal) {
                                val entry = UserMemoryEntity(
                                    memoryKey = memKey,
                                    memoryValue = memVal,
                                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                                )
                                database.userMemoryDao().insertMemory(entry)
                                existingMemoryKeys[memKey] = entry
                                restoredCount++
                            }
                        }
                    }

                    // Workspaces
                    val wsArr = root.optJSONArray("workspaces")
                    if (wsArr != null) {
                        for (i in 0 until wsArr.length()) {
                            val obj = wsArr.getJSONObject(i)
                            val slug = obj.optString("slug", "ws_$i")
                            if (existingWorkspaceSlugs.add(slug)) {
                                database.workspaceDao().insertWorkspace(
                                    WorkspaceEntity(
                                        slug = slug,
                                        name = obj.optString("name", "Workspace"),
                                        description = obj.optString("description", ""),
                                        systemInstructions = obj.optString("systemInstructions", ""),
                                        colorHex = obj.optString("colorHex", "#00D9FF"),
                                        iconName = obj.optString("iconName", "Work")
                                    )
                                )
                                restoredCount++
                            }
                        }
                    }

                    // Routines
                    val rArr = root.optJSONArray("routines")
                    if (rArr != null) {
                        for (i in 0 until rArr.length()) {
                            val obj = rArr.getJSONObject(i)
                            val name = obj.optString("name", "Routine $i")
                            val triggerVal = obj.optString("triggerValue", "")
                            val key = "$name|$triggerVal"
                            if (existingRoutines.add(key)) {
                                database.routineDao().insertRoutine(
                                    RoutineEntity(
                                        name = name,
                                        triggerType = obj.optString("triggerType", "VOICE"),
                                        triggerValue = triggerVal,
                                        actionType = obj.optString("actionType", "SPEAK"),
                                        actionPayload = obj.optString("actionPayload", ""),
                                        isEnabled = obj.optBoolean("isEnabled", true)
                                    )
                                )
                                restoredCount++
                            }
                        }
                    }
                }

                // Schedule pending future reminders outside the DB transaction
                tasksToSchedule.forEach { (taskId, entity, dueDate) ->
                    ReminderScheduler.scheduleReminder(
                        context = context,
                        taskId = taskId,
                        title = entity.title,
                        priority = entity.priority,
                        triggerAtMillis = dueDate
                    )
                }

                prefs.edit().putLong("last_sync_time", System.currentTimeMillis()).apply()

                SyncResult(
                    isSuccess = true,
                    message = "সফলভাবে $restoredCount টি নতুন ডেটা রিস্টোর সম্পন্ন হয়েছে।",
                    itemsRestoredCount = restoredCount
                )
            } catch (e: Exception) {
                SyncResult(false, "রিস্টোর ত্রুটি: ${e.localizedMessage}")
            }
        }

    internal fun encryptString(plain: String, passkey: String): String {
        require(passkey.isNotBlank()) { "Passkey must not be blank" }
        val salt = ByteArray(SALT_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(GCM_IV_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
        val key = derivePbkdf2Key(passkey, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val cipherBytes = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))

        val combined = ByteArray(salt.size + iv.size + cipherBytes.size)
        System.arraycopy(salt, 0, combined, 0, salt.size)
        System.arraycopy(iv, 0, combined, salt.size, iv.size)
        System.arraycopy(cipherBytes, 0, combined, salt.size + iv.size, cipherBytes.size)

        return V2_PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    internal fun decryptString(encryptedPayload: String, passkey: String): String? {
        if (passkey.isBlank() || encryptedPayload.isBlank()) return null
        return try {
            if (encryptedPayload.startsWith(V2_PREFIX)) {
                val encoded = encryptedPayload.removePrefix(V2_PREFIX)
                val combined = Base64.decode(encoded, Base64.NO_WRAP)
                if (combined.size <= SALT_LENGTH_BYTES + GCM_IV_LENGTH_BYTES) return null

                val salt = combined.copyOfRange(0, SALT_LENGTH_BYTES)
                val iv = combined.copyOfRange(SALT_LENGTH_BYTES, SALT_LENGTH_BYTES + GCM_IV_LENGTH_BYTES)
                val cipherBytes = combined.copyOfRange(SALT_LENGTH_BYTES + GCM_IV_LENGTH_BYTES, combined.size)

                val key = derivePbkdf2Key(passkey, salt)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
                val decrypted = cipher.doFinal(cipherBytes)
                String(decrypted, Charsets.UTF_8)
            } else {
                decryptLegacyV1(encryptedPayload, passkey)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun decryptLegacyV1(encryptedBase64: String, passkey: String): String? {
        return try {
            val keyBytes = MessageDigest.getInstance("SHA-256").digest(passkey.toByteArray(Charsets.UTF_8))
            val secretKey = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            val iv = ByteArray(16) { 0x42 }
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
            val decrypted = cipher.doFinal(Base64.decode(encryptedBase64, Base64.NO_WRAP))
            val text = String(decrypted, Charsets.UTF_8)
            // Sanity check JSON structure to reduce false-positive legacy CBC padding decodes
            if (text.trimStart().startsWith("{")) text else null
        } catch (_: Exception) {
            null
        }
    }

    private fun derivePbkdf2Key(passkey: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passkey.toCharArray(), salt, PBKDF2_ITERATIONS, AES_KEY_LENGTH_BITS)
        val encoded = factory.generateSecret(spec).encoded
        return SecretKeySpec(encoded, "AES")
    }

    companion object {
        const val V2_PREFIX = "JARVIS_SYNC_V2:"
        private const val SALT_LENGTH_BYTES = 16
        private const val GCM_IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val PBKDF2_ITERATIONS = 100_000
        private const val AES_KEY_LENGTH_BITS = 256
    }
}

