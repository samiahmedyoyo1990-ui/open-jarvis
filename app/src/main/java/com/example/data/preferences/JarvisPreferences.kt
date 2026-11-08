package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "jarvis_settings")

class JarvisPreferences(private val context: Context) {

    companion object {
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_PREFERRED_LANG = stringPreferencesKey("preferred_lang") // "bn", "en", "auto"
        val KEY_SPEECH_RATE = floatPreferencesKey("speech_rate")
        val KEY_SPEECH_PITCH = floatPreferencesKey("speech_pitch")
        val KEY_AUTO_SPEAK = booleanPreferencesKey("auto_speak")
        val KEY_HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_CONTINUOUS_MODE = booleanPreferencesKey("continuous_mode")
        val KEY_ANIMATION_ENABLED = booleanPreferencesKey("animation_enabled")
        val KEY_ANIMATION_INTENSITY = floatPreferencesKey("animation_intensity")
        val KEY_REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val KEY_WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        val KEY_WAKE_WORD_SENSITIVITY = floatPreferencesKey("wake_word_sensitivity")
    }

    val userNameFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_NAME] ?: "বস"
    }

    val preferredLanguageFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_PREFERRED_LANG] ?: "bn"
    }

    val speechRateFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_SPEECH_RATE] ?: 1.0f
    }

    val speechPitchFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_SPEECH_PITCH] ?: 1.0f
    }

    val autoSpeakFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_SPEAK] ?: true
    }

    val hapticEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_HAPTIC_ENABLED] ?: true
    }

    val onboardingCompletedFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] ?: false
    }

    val continuousModeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_CONTINUOUS_MODE] ?: false
    }

    val animationEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ANIMATION_ENABLED] ?: true
    }

    val animationIntensityFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_ANIMATION_INTENSITY] ?: 1.0f
    }

    val reducedMotionFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_REDUCED_MOTION] ?: false
    }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_USER_NAME] = name
        }
    }

    suspend fun setPreferredLanguage(lang: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PREFERRED_LANG] = lang
        }
    }

    suspend fun setSpeechRate(rate: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SPEECH_RATE] = rate
        }
    }

    suspend fun setSpeechPitch(pitch: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SPEECH_PITCH] = pitch
        }
    }

    suspend fun setAutoSpeak(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_SPEAK] = enabled
        }
    }

    suspend fun setHapticEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_HAPTIC_ENABLED] = enabled
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setContinuousMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CONTINUOUS_MODE] = enabled
        }
    }

    suspend fun setAnimationEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ANIMATION_ENABLED] = enabled
        }
    }

    suspend fun setAnimationIntensity(intensity: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ANIMATION_INTENSITY] = intensity
        }
    }

    val wakeWordEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_WAKE_WORD_ENABLED] ?: false
    }

    val wakeWordSensitivityFlow: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_WAKE_WORD_SENSITIVITY] ?: 0.75f
    }

    suspend fun setReducedMotion(reduced: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_REDUCED_MOTION] = reduced
        }
    }

    suspend fun setWakeWordEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_WAKE_WORD_ENABLED] = enabled
        }
    }

    suspend fun setWakeWordSensitivity(sensitivity: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_WAKE_WORD_SENSITIVITY] = sensitivity
        }
    }
}
