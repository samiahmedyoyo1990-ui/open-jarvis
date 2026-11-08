package com.example.ai.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android Keystore-backed AES-256 GCM Secure Storage for BYOK API Keys.
 * Encrypts API keys before storing in private app preferences.
 * At rest, credentials are protected by the hardware-backed keystore.
 */
class SecureApiKeyStorage(private val context: Context) {

    private val TAG = "SecureApiKeyStorage"
    private val PREFS_NAME = "jarvis_secure_keystore_vault"
    private val KEY_ALIAS = "jarvis_byok_master_key"
    private val ANDROID_KEYSTORE = "AndroidKeyStore"
    private val TRANSFORMATION = "AES/GCM/NoPadding"
    private val GCM_IV_LENGTH = 12
    private val GCM_TAG_LENGTH = 128

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        ensureMasterKey()
    }

    private fun ensureMasterKey() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .build()

                keyGenerator.init(keyGenParameterSpec)
                keyGenerator.generateKey()
                Log.d(TAG, "Generated new Android Keystore AES-256 key")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error ensuring keystore master key", e)
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    fun saveApiKey(providerId: String, apiKey: String) {
        if (apiKey.isBlank()) {
            removeApiKey(providerId)
            return
        }

        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val cipherText = cipher.doFinal(apiKey.toByteArray(Charsets.UTF_8))

            // Combine IV + ciphertext
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)

            val base64Payload = Base64.encodeToString(combined, Base64.NO_WRAP)
            prefs.edit().putString("key_$providerId", base64Payload).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encrypt API key for provider: $providerId", e)
        }
    }

    fun getApiKey(providerId: String): String? {
        val base64Payload = prefs.getString("key_$providerId", null) ?: return null
        return try {
            val combined = Base64.decode(base64Payload, Base64.NO_WRAP)
            if (combined.size < GCM_IV_LENGTH) return null

            val iv = ByteArray(GCM_IV_LENGTH)
            val cipherText = ByteArray(combined.size - GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
            System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

            val decryptedBytes = cipher.doFinal(cipherText)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt API key for provider: $providerId", e)
            null
        }
    }

    fun hasApiKey(providerId: String): Boolean {
        val key = getApiKey(providerId)
        return !key.isNullOrBlank()
    }

    fun removeApiKey(providerId: String) {
        prefs.edit().remove("key_$providerId").apply()
    }

    companion object {
        fun maskKey(key: String): String {
            val clean = key.trim()
            if (clean.length <= 8) return "••••••••"
            val prefix = clean.take(4)
            val suffix = clean.takeLast(4)
            return "$prefix••••••••$suffix"
        }
    }
}
