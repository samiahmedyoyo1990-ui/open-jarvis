package com.example.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * System-Wide Background Wake Word Service for JARVIS.
 * Listens for "Hey JARVIS", "হেই জার্ভিস", "JARVIS", etc.
 * Uses an Android Foreground Service with transparent, user-controlled ongoing notification.
 */
class JarvisWakeWordService : Service() {

    private val TAG = "JarvisWakeWordService"
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isListeningLoopActive = false
    private var consecutiveErrors = 0

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "JarvisWakeWordService created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            Log.d(TAG, "Stop action received in onStartCommand")
            stopSelf()
            return START_NOT_STICKY
        }

        // Start Foreground with persistent notification
        val notification = buildForegroundNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    )
                } else {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    )
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            _isServiceRunning.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground service", e)
            stopSelf()
            return START_NOT_STICKY
        }

        startWakeWordLoop()
        return START_STICKY
    }

    private fun buildForegroundNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_WAKE_WORD_TRIGGERED, true)
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, JarvisWakeWordService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("JARVIS ব্যাকগ্রাউন্ডে সক্রিয়")
            .setContentText("'Hey JARVIS' বা 'হেই জার্ভিস' বলুন")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(pendingOpenApp)
            .addAction(R.drawable.ic_launcher_foreground, "বন্ধ করুন", pendingStop)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "JARVIS ব্যাকগ্রাউন্ড লিসেনার",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "হ্যান্ডস-ফ্রি 'Hey JARVIS' অ্যাক্টিভেশনের জন্য ব্যবহৃত সার্ভিস"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun startWakeWordLoop() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e(TAG, "SpeechRecognizer is not available on this device")
            stopSelf()
            return
        }

        isListeningLoopActive = true
        initRecognizerAndListen()
    }

    private fun initRecognizerAndListen() {
        if (!isListeningLoopActive) return

        mainHandler.post {
            try {
                destroyRecognizer()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            consecutiveErrors = 0
                        }

                        override fun onBeginningOfSpeech() {}

                        override fun onRmsChanged(rmsdB: Float) {}

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {}

                        override fun onError(error: Int) {
                            Log.d(TAG, "Wake word recognizer error code: $error")
                            consecutiveErrors++
                            scheduleRestart(if (consecutiveErrors > 3) 2000L else 500L)
                        }

                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            checkWakeWordMatches(matches)
                            scheduleRestart(300L)
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (checkWakeWordMatches(partial)) {
                                destroyRecognizer()
                                scheduleRestart(1500L)
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("bn-BD", "en-US", "bn-IN"))
                }

                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error in initRecognizerAndListen", e)
                scheduleRestart(1500L)
            }
        }
    }

    private fun checkWakeWordMatches(matches: ArrayList<String>?): Boolean {
        if (matches == null || matches.isEmpty()) return false

        for (phrase in matches) {
            val clean = phrase.lowercase(Locale.ROOT).trim()
            Log.d(TAG, "Inspecting audio segment: $clean")

            if (isWakeWordMatch(clean)) {
                Log.i(TAG, "WAKE WORD TRIGGERED: $clean")
                triggerJarvisActivation()
                return true
            }
        }
        return false
    }

    private fun isWakeWordMatch(text: String): Boolean {
        val targets = listOf(
            "hey jarvis",
            "jarvis",
            "হেই জার্ভিস",
            "হে জার্ভিস",
            "জার্ভিস",
            "জারভিস",
            "হাই জার্ভিস",
            "hi jarvis",
            "ok jarvis",
            "hello jarvis",
            "হ্যালো জার্ভিস"
        )
        return targets.any { text.contains(it) }
    }

    private fun triggerJarvisActivation() {
        // 1. Dual Haptic Pulse
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createWaveform(longArrayOf(0, 70, 80, 110), -1)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(200)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibrator error", e)
        }

        // 2. Broadcast to running MainActivity / ViewModel
        val broadcastIntent = Intent(ACTION_WAKE_WORD_DETECTED).apply {
            setPackage(packageName)
        }
        sendBroadcast(broadcastIntent)

        // 3. Bring MainActivity to Foreground
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_WAKE_WORD_TRIGGERED, true)
        }
        startActivity(launchIntent)
    }

    private fun scheduleRestart(delayMs: Long) {
        if (!isListeningLoopActive) return
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            if (isListeningLoopActive) {
                initRecognizerAndListen()
            }
        }, delayMs)
    }

    private fun destroyRecognizer() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.w(TAG, "Error destroying speechRecognizer", e)
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "JarvisWakeWordService destroying")
        isListeningLoopActive = false
        mainHandler.removeCallbacksAndMessages(null)
        destroyRecognizer()
        _isServiceRunning.value = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "jarvis_wake_word_channel"
        const val NOTIFICATION_ID = 4040
        const val ACTION_STOP_SERVICE = "com.example.voice.STOP_WAKE_WORD_SERVICE"
        const val ACTION_WAKE_WORD_DETECTED = "com.example.voice.WAKE_WORD_DETECTED"
        const val EXTRA_WAKE_WORD_TRIGGERED = "extra_wake_word_triggered"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        fun start(context: Context) {
            try {
                val intent = Intent(context, JarvisWakeWordService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e("JarvisWakeWordService", "Failed to start wake word service", e)
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, JarvisWakeWordService::class.java)
                context.stopService(intent)
                _isServiceRunning.value = false
            } catch (e: Exception) {
                Log.e("JarvisWakeWordService", "Failed to stop wake word service", e)
            }
        }
    }
}
