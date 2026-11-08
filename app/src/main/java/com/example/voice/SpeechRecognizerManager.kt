package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed interface SpeechState {
    data object Idle : SpeechState
    data object Listening : SpeechState
    data class PartialResult(val text: String) : SpeechState
    data class FinalResult(val text: String) : SpeechState
    data class Error(val message: String) : SpeechState
}

class SpeechRecognizerManager(private val context: Context) {

    private val TAG = "SpeechRecognizerManager"
    private var speechRecognizer: SpeechRecognizer? = null

    private val _speechState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    val isAvailable: Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening(languageCode: String = "bn-BD") {
        if (!isAvailable) {
            _speechState.value = SpeechState.Error("এই ডিভাইসে ভয়েস রিকগনিশন সার্ভিস উপলভ্য নয়।")
            return
        }

        stopListening()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener())
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

                val selectedLocale = when (languageCode) {
                    "en" -> Locale.US.toLanguageTag()
                    else -> "bn-BD"
                }
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, selectedLocale)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, selectedLocale)
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("bn-BD", "en-US", "bn-IN"))
            }

            speechRecognizer?.startListening(intent)
            _speechState.value = SpeechState.Listening
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition", e)
            _speechState.value = SpeechState.Error("ভয়েস ইনপুট শুরু করা যায়নি: ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping speech recognition", e)
        }
        _audioAmplitude.value = 0f
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _speechState.value = SpeechState.Listening
        }

        override fun onBeginningOfSpeech() {
            _speechState.value = SpeechState.Listening
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Normalizing rmsdB (typically -2 to 10) to 0.0f..1.0f
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1.0f)
            _audioAmplitude.value = normalized
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _audioAmplitude.value = 0f
        }

        override fun onError(error: Int) {
            _audioAmplitude.value = 0f
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "অডিও রেকর্ডিংয়ে ত্রুটি হয়েছে।"
                SpeechRecognizer.ERROR_CLIENT -> "ভয়েস ক্লায়েন্ট প্রস্তুত নয়।"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "মাইক্রোফোন ব্যবহারের অনুমতি প্রয়োজন।"
                SpeechRecognizer.ERROR_NETWORK -> "ইন্টারনেট সংযোগে সমস্যা দেখা দিয়েছে।"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "ইন্টারনেট টাইমআউট হয়েছে।"
                SpeechRecognizer.ERROR_NO_MATCH -> "কিছু শুনতে পাইনি। অনুগ্রহ করে আবার বলুন।"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "ভয়েস ইঞ্জিন ব্যস্ত রয়েছে।"
                SpeechRecognizer.ERROR_SERVER -> "সার্ভারে ত্রুটি হয়েছে।"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "কোনো কথা শোনা যায়নি।"
                else -> "ভয়েস রিকগনিশনে সমস্যা হয়েছে ($error)।"
            }
            _speechState.value = SpeechState.Error(errorMessage)
        }

        override fun onResults(results: Bundle?) {
            _audioAmplitude.value = 0f
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val recognizedText = matches?.firstOrNull().orEmpty().trim()
            if (recognizedText.isNotBlank()) {
                _speechState.value = SpeechState.FinalResult(recognizedText)
            } else {
                _speechState.value = SpeechState.Error("কথা সনাক্ত করা যায়নি।")
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partialText = matches?.firstOrNull().orEmpty().trim()
            if (partialText.isNotBlank()) {
                _speechState.value = SpeechState.PartialResult(partialText)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun resetState() {
        _speechState.value = SpeechState.Idle
        _audioAmplitude.value = 0f
    }
}
