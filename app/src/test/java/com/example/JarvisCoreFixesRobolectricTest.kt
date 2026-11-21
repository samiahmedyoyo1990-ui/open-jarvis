package com.example

import android.app.AlarmManager
import android.content.Context
import android.util.Base64
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.actions.JarvisAction
import com.example.actions.JarvisActionEngine
import com.example.automation.RoutinesEngine
import com.example.data.local.JarvisDatabase
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.RoutineEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.UserMemoryEntity
import com.example.data.local.entity.WorkspaceEntity
import com.example.data.preferences.JarvisPreferences
import com.example.data.repository.JarvisRepository
import com.example.notifications.ReminderScheduler
import com.example.sync.SyncEngine
import com.example.voice.JarvisWakeWordService
import com.example.voice.TextToSpeechManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class JarvisCoreFixesRobolectricTest {

    private lateinit var context: Context
    private lateinit var inMemoryDb: JarvisDatabase
    private lateinit var repository: JarvisRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        inMemoryDb = Room.inMemoryDatabaseBuilder(context, JarvisDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = JarvisRepository(inMemoryDb, JarvisPreferences(context))
    }

    @After
    fun tearDown() {
        inMemoryDb.close()
    }

    @Test
    fun roomMigration_v1ToV3_preservesExistingUserNotesAndTasks() = runTest {
        val dbName = "jarvis_migration_test_${System.currentTimeMillis()}.db"
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()

        // Create authentic Version 1 schema using SQLite directly
        val helperConfig = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `notes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `category` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)"
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `isCompleted` INTEGER NOT NULL, `dueDate` INTEGER, `priority` TEXT NOT NULL, `reminderSet` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL)"
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `conversations` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `isUser` INTEGER NOT NULL, `text` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `actionExecuted` TEXT)"
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `user_memory` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `memoryKey` TEXT NOT NULL, `memoryValue` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)"
                    )
                    db.execSQL(
                        "INSERT INTO `notes` (`title`, `content`, `category`, `timestamp`) VALUES ('Legacy Note', 'Keep me safe', 'সাধারণ', 1700000000000)"
                    )
                    db.execSQL(
                        "INSERT INTO `tasks` (`title`, `isCompleted`, `dueDate`, `priority`, `reminderSet`, `timestamp`) VALUES ('Legacy Task', 0, 1900000000000, 'High', 1, 1700000000000)"
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        FrameworkSQLiteOpenHelperFactory().create(helperConfig).use { helper ->
            helper.writableDatabase.close()
        }

        // Open via Room v3 with MIGRATION_1_2 and MIGRATION_2_3 (no destructive migration allowed)
        val migratedDb = Room.databaseBuilder(context, JarvisDatabase::class.java, dbName)
            .addMigrations(JarvisDatabase.MIGRATION_1_2, JarvisDatabase.MIGRATION_2_3)
            .fallbackToDestructiveMigration(false)
            .allowMainThreadQueries()
            .build()

        try {
            val notes = migratedDb.noteDao().getAllNotes().first()
            assertEquals(1, notes.size)
            assertEquals("Legacy Note", notes.first().title)
            assertEquals("Keep me safe", notes.first().content)

            val tasks = migratedDb.taskDao().getAllTasks().first()
            assertEquals(1, tasks.size)
            assertEquals("Legacy Task", tasks.first().title)
            assertEquals(1900000000000L, tasks.first().dueDate)
            assertTrue(tasks.first().reminderSet)

            // Verify new v2/v3 tables work properly
            migratedDb.workspaceDao().insertWorkspace(
                WorkspaceEntity(slug = "dev", name = "Dev", description = "Test", systemInstructions = "Be concise")
            )
            assertEquals(1, migratedDb.workspaceDao().getAllWorkspaces().first().size)
        } finally {
            migratedDb.close()
            File(dbFile.path).delete()
        }
    }

    @Test
    fun syncEngine_v2EncryptionRoundtrip_preservesFieldsAndDeduplicates() = runTest {
        val futureDue = System.currentTimeMillis() + 3_600_000L
        inMemoryDb.noteDao().insertNote(NoteEntity(title = "Meeting", content = "Roadmap sync", category = "Work"))
        inMemoryDb.taskDao().insertTask(
            TaskEntity(title = "Call Client", isCompleted = false, dueDate = futureDue, priority = "High", reminderSet = true)
        )
        inMemoryDb.userMemoryDao().insertMemory(UserMemoryEntity(memoryKey = "city", memoryValue = "Dhaka"))
        inMemoryDb.workspaceDao().insertWorkspace(
            WorkspaceEntity(slug = "engineering", name = "Engineering", description = "Android team", systemInstructions = "Kotlin")
        )
        inMemoryDb.routineDao().insertRoutine(
            RoutineEntity(
                name = "Focus Mode",
                triggerType = "VOICE",
                triggerValue = "focus time",
                actionType = "SPEAK",
                actionPayload = "Entering focus mode",
                isEnabled = true
            )
        )

        val syncEngine = SyncEngine(context, inMemoryDb)
        val encrypted1 = syncEngine.generateEncryptedBackup("StrongPass#2026")
        val encrypted2 = syncEngine.generateEncryptedBackup("StrongPass#2026")

        assertTrue(encrypted1.startsWith(SyncEngine.V2_PREFIX))
        // Random salt + random IV ensures ciphertexts are never identical
        assertNotEquals(encrypted1, encrypted2)

        // Wrong passkey must fail cleanly
        val wrongResult = syncEngine.restoreEncryptedBackup(encrypted1, "WrongPass")
        assertFalse(wrongResult.isSuccess)

        // Restore into a fresh DB
        val targetDb = Room.inMemoryDatabaseBuilder(context, JarvisDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val targetSync = SyncEngine(context, targetDb)
            val restoreResult = targetSync.restoreEncryptedBackup(encrypted1, "StrongPass#2026")
            assertTrue(restoreResult.isSuccess)
            assertEquals(5, restoreResult.itemsRestoredCount)

            val restoredTasks = targetDb.taskDao().getAllTasks().first()
            assertEquals(1, restoredTasks.size)
            assertEquals(futureDue, restoredTasks.first().dueDate)
            assertTrue(restoredTasks.first().reminderSet)

            val restoredRoutines = targetDb.routineDao().getAllRoutines().first()
            assertEquals(1, restoredRoutines.size)
            assertEquals("focus time", restoredRoutines.first().triggerValue)

            // Re-importing the exact same backup must deduplicate
            val secondRestore = targetSync.restoreEncryptedBackup(encrypted1, "StrongPass#2026")
            assertTrue(secondRestore.isSuccess)
            assertEquals(0, secondRestore.itemsRestoredCount)
        } finally {
            targetDb.close()
        }
    }

    @Test
    fun syncEngine_legacyV1Payload_stillDecryptsCleanly() {
        val syncEngine = SyncEngine(context, inMemoryDb)
        val passkey = "legacyPass"
        val plainJson = """{"version":1,"notes":[],"tasks":[]}"""

        val keyBytes = MessageDigest.getInstance("SHA-256").digest(passkey.toByteArray(Charsets.UTF_8))
        val secretKey = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(ByteArray(16) { 0x42 }))
        val legacyEncoded = Base64.encodeToString(cipher.doFinal(plainJson.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)

        assertEquals(plainJson, syncEngine.decryptString(legacyEncoded, passkey))
        assertNull(syncEngine.decryptString(legacyEncoded, "wrongKey"))
    }

    @Test
    fun routinesEngine_triggersAndRespectsCooldown() = runTest {
        repository.saveRoutine(
            RoutineEntity(
                name = "Standup Greeting",
                triggerType = "VOICE",
                triggerValue = "start standup",
                actionType = "SPEAK",
                actionPayload = "স্ট্যান্ডআপ শুরু হচ্ছে।",
                isEnabled = true
            )
        )
        val engine = RoutinesEngine(repository, JarvisActionEngine(context))

        val firstHit = engine.checkAndExecuteTrigger("Jarvis start standup please")
        assertNotNull(firstHit)
        assertEquals("Standup Greeting", firstHit?.routineName)
        assertEquals("স্ট্যান্ডআপ শুরু হচ্ছে।", firstHit?.spokenFeedback)

        // Immediate repeat must be blocked by cooldown window
        val cooldownHit = engine.checkAndExecuteTrigger("start standup")
        assertNull(cooldownHit)
    }

    @Test
    fun reminderScheduler_validatesFutureTimestampAndCancelsAlarm() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = shadowOf(alarmManager)

        val now = System.currentTimeMillis()
        assertFalse(ReminderScheduler.scheduleReminder(context, 42L, "Past Task", now - 5_000L, nowMillis = now))
        assertFalse(ReminderScheduler.scheduleReminder(context, -1L, "Invalid ID", now + 60_000L, nowMillis = now))

        val scheduled = ReminderScheduler.scheduleReminder(
            context = context,
            taskId = 42L,
            title = "Pay internet bill",
            triggerAtMillis = now + 120_000L,
            nowMillis = now
        )
        assertTrue(scheduled)
        assertTrue(shadowAlarmManager.scheduledAlarms.isNotEmpty())

        ReminderScheduler.cancelReminder(context, 42L)
        assertTrue(shadowAlarmManager.scheduledAlarms.isEmpty())
    }

    @Test
    fun textToSpeechManager_uninitializedSpeakInvokesCallbackImmediately() {
        val ttsManager = TextToSpeechManager(context)
        var doneInvoked = false
        ttsManager.setLanguageForCode("en")
        ttsManager.speak("Testing fallback callback") {
            doneInvoked = true
        }
        assertTrue(doneInvoked)
        assertFalse(ttsManager.isSpeaking.value)
        ttsManager.shutdown()
    }

    @Test
    fun jarvisActionEngine_offlineClassifierAndArithmeticWorkSafely() {
        val actionEngine = JarvisActionEngine(context)

        // Conversational questions must NOT hijack app launchers
        assertNull(actionEngine.parseOfflineIntent("how does youtube recommendation work?"))
        assertNull(actionEngine.parseOfflineIntent("whatsapp কিভাবে এনক্রিপশন করে?"))

        // Explicit commands work
        val openWa = actionEngine.parseOfflineIntent("open whatsapp")
        assertNotNull(openWa)
        assertTrue(openWa?.first is JarvisAction.OpenApp)

        val battery = actionEngine.parseOfflineIntent("check battery status")
        assertNotNull(battery)
        assertTrue(battery?.first is JarvisAction.GetDeviceInfo)

        // Precedence & parentheses arithmetic evaluator
        val math = actionEngine.parseOfflineIntent("calculate (10 + 5) * 2 - 4 / 2")
        assertNotNull(math)
        val calcAction = math?.first as? JarvisAction.Calculation
        assertEquals("28", calcAction?.result)

        // Divide by zero handled safely
        val divZero = actionEngine.parseOfflineIntent("15 / 0")
        assertNotNull(divZero)
        assertTrue(divZero?.second?.contains("ভাগ করা সম্ভব নয়") == true)

        val nestedDivisionByZero = actionEngine.parseOfflineIntent("calculate 1 / (2 - 2)")
        assertTrue(nestedDivisionByZero?.second?.contains("ভাগ করা সম্ভব নয়") == true)
        assertNull(actionEngine.parseOfflineIntent("calculate 2 +"))
        assertNull(actionEngine.parseOfflineIntent("tell me why youtube is popular"))
    }

    @Test
    fun wakeWordService_matchesBengaliBanglishAndEnglishPhrases() {
        assertTrue(JarvisWakeWordService.isWakeWordMatch("Hey Jarvis open settings"))
        assertTrue(JarvisWakeWordService.isWakeWordMatch("ওহে জার্ভিস সময় কত"))
        assertTrue(JarvisWakeWordService.isWakeWordMatch("hi jarvis"))
        assertFalse(JarvisWakeWordService.isWakeWordMatch("open youtube without wake word"))
    }
}
