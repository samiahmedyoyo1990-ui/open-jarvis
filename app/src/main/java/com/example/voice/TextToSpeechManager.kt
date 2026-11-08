package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

class TextToSpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

    private val TAG = "TextToSpeechManager"
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _ttsError = MutableStateFlow<String?>(null)
    val ttsError: StateFlow<String?> = _ttsError.asStateFlow()

    private var currentSpeakingCallback: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            setupVoice()
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    currentSpeakingCallback?.invoke()
                    currentSpeakingCallback = null
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    currentSpeakingCallback = null
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                    currentSpeakingCallback = null
                    Log.w(TAG, "TTS utterance error code: $errorCode")
                }
            })
        } else {
            isInitialized = false
            _ttsError.value = "টেক্সট-টু-স্পিচ ইঞ্জিন শুরু করা যায়নি।"
            Log.e(TAG, "TextToSpeech initialization failed with status $status")
        }
    }

    private fun setupVoice() {
        val ttsInstance = tts ?: return
        // Try Bengali (Bangladesh), then Bengali (India), then Bengali generic, then fallback to English
        val bnBD = Locale("bn", "BD")
        val bnIN = Locale("bn", "IN")
        val bn = Locale("bn")

        val result = when {
            ttsInstance.isLanguageAvailable(bnBD) >= TextToSpeech.LANG_AVAILABLE -> ttsInstance.setLanguage(bnBD)
            ttsInstance.isLanguageAvailable(bnIN) >= TextToSpeech.LANG_AVAILABLE -> ttsInstance.setLanguage(bnIN)
            ttsInstance.isLanguageAvailable(bn) >= TextToSpeech.LANG_AVAILABLE -> ttsInstance.setLanguage(bn)
            else -> {
                Log.w(TAG, "Bengali TTS not available on device. Falling back to default locale.")
                ttsInstance.setLanguage(Locale.getDefault())
            }
        }

        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Selected TTS language data missing or not supported.")
        }
    }

    fun speak(
        text: String,
        speechRate: Float = 1.0f,
        speechPitch: Float = 1.0f,
        onDone: (() -> Unit)? = null
    ) {
        if (!isInitialized || tts == null) {
            Log.w(TAG, "TTS not initialized yet")
            return
        }

        stop()
        currentSpeakingCallback = onDone

        tts?.setSpeechRate(speechRate.coerceIn(0.5f, 2.0f))
        tts?.setPitch(speechPitch.coerceIn(0.5f, 2.0f))

        val utteranceId = UUID.randomUUID().toString()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        try {
            if (tts?.isSpeaking == true) {
                tts?.stop()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        }
        _isSpeaking.value = false
        currentSpeakingCallback = null
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
