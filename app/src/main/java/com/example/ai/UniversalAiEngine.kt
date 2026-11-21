package com.example.ai

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.ai.model.AiEndpointSecurity
import com.example.ai.model.AiModelInfo
import com.example.ai.model.AiProviderConfig
import com.example.ai.model.AiProviderType
import com.example.ai.model.ConnectionTestResult
import com.example.ai.providers.FirebaseAiProviderAdapter
import com.example.ai.providers.GeminiDirectProviderAdapter
import com.example.ai.providers.OpenAiCompatibleProviderAdapter
import com.example.ai.providers.OpenRouterProviderAdapter
import com.example.ai.providers.UniversalAiProvider
import com.example.ai.security.SecureApiKeyStorage
import com.example.data.local.entity.ConversationEntity
import com.example.data.preferences.dataStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class UniversalAiEngine(private val context: Context) {

    private val TAG = "UniversalAiEngine"
    val secureStorage = SecureApiKeyStorage(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    // DataStore preference keys
    private val KEY_ACTIVE_PROVIDER_ID = stringPreferencesKey("ai_active_provider_id")
    private val KEY_BACKUP_PROVIDER_ID = stringPreferencesKey("ai_backup_provider_id")
    private val KEY_BACKUP_ENABLED = booleanPreferencesKey("ai_backup_enabled")
    private val KEY_CONFIGS_JSON = stringPreferencesKey("ai_configs_json")

    // State flows
    private val _allConfigs = MutableStateFlow<List<AiProviderConfig>>(AiProviderConfig.defaultConfigs())
    val allConfigs: StateFlow<List<AiProviderConfig>> = _allConfigs.asStateFlow()

    private val _activeConfig = MutableStateFlow(
        AiProviderConfig.defaultConfigs().firstOrNull { it.id == "provider_gemini" }
            ?: AiProviderConfig.defaultConfigs().first()
    )
    val activeConfig: StateFlow<AiProviderConfig> = _activeConfig.asStateFlow()

    private val _backupConfig = MutableStateFlow<AiProviderConfig?>(
        AiProviderConfig.defaultConfigs().firstOrNull { it.id == "provider_openrouter" }
    )
    val backupConfig: StateFlow<AiProviderConfig?> = _backupConfig.asStateFlow()

    private val _isBackupEnabled = MutableStateFlow(false)
    val isBackupEnabled: StateFlow<Boolean> = _isBackupEnabled.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        scope.launch {
            context.dataStore.data.collect { prefs ->
                val configsJson = prefs[KEY_CONFIGS_JSON]
                if (!configsJson.isNullOrBlank()) {
                    _allConfigs.value = parseConfigsJson(configsJson)
                }

                val activeId = prefs[KEY_ACTIVE_PROVIDER_ID] ?: "provider_gemini"
                val active = _allConfigs.value.firstOrNull { it.id == activeId }
                    ?: _allConfigs.value.firstOrNull()
                if (active != null) {
                    _activeConfig.value = active
                }

                val backupId = prefs[KEY_BACKUP_PROVIDER_ID]
                val backup = _allConfigs.value.firstOrNull { it.id == backupId }
                _backupConfig.value = backup

                _isBackupEnabled.value = prefs[KEY_BACKUP_ENABLED] ?: false
            }
        }
    }

    fun setActiveProvider(providerId: String) {
        val target = _allConfigs.value.firstOrNull { it.id == providerId } ?: return
        _activeConfig.value = target
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[KEY_ACTIVE_PROVIDER_ID] = providerId
            }
        }
    }

    fun setBackupProvider(providerId: String?) {
        val target = _allConfigs.value.firstOrNull { it.id == providerId }
        _backupConfig.value = target
        scope.launch {
            context.dataStore.edit { prefs ->
                if (providerId != null) {
                    prefs[KEY_BACKUP_PROVIDER_ID] = providerId
                } else {
                    prefs.remove(KEY_BACKUP_PROVIDER_ID)
                }
            }
        }
    }

    fun setBackupEnabled(enabled: Boolean) {
        _isBackupEnabled.value = enabled
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[KEY_BACKUP_ENABLED] = enabled
            }
        }
    }

    fun saveProviderConfig(config: AiProviderConfig, apiKey: String?) {
        if (apiKey != null) {
            secureStorage.saveApiKey(config.id, apiKey)
        }

        val updatedList = _allConfigs.value.filter { it.id != config.id } + config
        _allConfigs.value = updatedList

        if (_activeConfig.value.id == config.id) {
            _activeConfig.value = config
        }
        if (_backupConfig.value?.id == config.id) {
            _backupConfig.value = config
        }

        persistConfigs(updatedList)
    }

    fun deleteProviderConfig(providerId: String) {
        secureStorage.removeApiKey(providerId)
        val updatedList = _allConfigs.value.filter { it.id != providerId }
        _allConfigs.value = updatedList

        if (_activeConfig.value.id == providerId) {
            val fallback = updatedList.firstOrNull() ?: AiProviderConfig.defaultConfigs().first()
            _activeConfig.value = fallback
            scope.launch {
                context.dataStore.edit { prefs ->
                    prefs[KEY_ACTIVE_PROVIDER_ID] = fallback.id
                }
            }
        }

        if (_backupConfig.value?.id == providerId) {
            _backupConfig.value = null
            scope.launch {
                context.dataStore.edit { prefs ->
                    prefs.remove(KEY_BACKUP_PROVIDER_ID)
                }
            }
        }

        persistConfigs(updatedList)
    }

    suspend fun testProviderConnection(providerId: String, apiKeyOverride: String? = null): ConnectionTestResult {
        val config = _allConfigs.value.firstOrNull { it.id == providerId }
            ?: return ConnectionTestResult(false, 404, "প্রোভাইডার কনফিগারেশন পাওয়া যায়নি।")

        val key = apiKeyOverride ?: secureStorage.getApiKey(providerId).orEmpty()
        val adapter = createAdapter(config)
        val result = adapter.testConnection(key)

        // Update config status
        val updatedConfig = config.copy(
            lastTestedTime = System.currentTimeMillis(),
            lastConnectionSuccess = result.isSuccess,
            lastStatusMessage = result.message
        )
        saveProviderConfig(updatedConfig, null)

        return result
    }

    suspend fun fetchModelsForProvider(providerId: String, apiKeyOverride: String? = null): List<AiModelInfo> {
        val config = _allConfigs.value.firstOrNull { it.id == providerId } ?: return emptyList()
        return fetchModelsForConfig(config, apiKeyOverride)
    }

    suspend fun fetchModelsForConfig(config: AiProviderConfig, apiKeyOverride: String? = null): List<AiModelInfo> {
        val key = apiKeyOverride ?: secureStorage.getApiKey(config.id).orEmpty()
        val adapter = createAdapter(config)
        return adapter.fetchModels(key)
    }

    private fun requiresApiKey(config: AiProviderConfig): Boolean {
        return when (config.type) {
            AiProviderType.FIREBASE_AI -> false
            AiProviderType.GOOGLE_GEMINI, AiProviderType.OPEN_ROUTER -> true
            AiProviderType.CUSTOM_OPENAI -> {
                val base = config.baseUrl.trim().lowercase()
                // Local/LAN servers (e.g., Ollama / LM Studio on http://localhost or RFC1918) often run without a key
                !base.startsWith("http://")
            }
        }
    }

    suspend fun processQuery(
        prompt: String,
        history: List<ConversationEntity>,
        userName: String
    ): AiAssistantResponse {
        return executeWithFailover(
            prompt = prompt,
            history = history,
            userName = userName
        ) { adapter, key ->
            adapter.generateResponse(prompt, history, userName, key)
        }
    }

    suspend fun executeMultimodalQuery(
        prompt: String,
        imageBase64: String,
        mimeType: String = "image/jpeg",
        history: List<ConversationEntity> = emptyList(),
        userName: String = "User"
    ): AiAssistantResponse {
        return executeWithFailover(
            prompt = prompt,
            history = history,
            userName = userName
        ) { adapter, key ->
            adapter.generateMultimodalResponse(prompt, imageBase64, mimeType, history, userName, key)
        }
    }

    private suspend fun executeWithFailover(
        prompt: String,
        history: List<ConversationEntity>,
        userName: String,
        callBlock: suspend (UniversalAiProvider, String) -> AiAssistantResponse
    ): AiAssistantResponse {
        val active = _activeConfig.value
        val activeKey = secureStorage.getApiKey(active.id).orEmpty()

        // Explicit BYOK validation: do not silently leak user prompts to Firebase when user selected a BYOK provider
        if (requiresApiKey(active) && activeKey.isBlank()) {
            return AiAssistantResponse(
                replyText = "${active.name} এর API Key কনফিগার করা নেই। অনুগ্রহ করে সেটিংস -> 'AI Providers' এ গিয়ে আপনার API Key যুক্ত করুন অথবা Firebase AI প্রোভাইডার নির্বাচন করুন।",
                isError = true
            )
        }

        if (active.type != AiProviderType.FIREBASE_AI &&
            !AiEndpointSecurity.isValidEndpointUrl(active.baseUrl.ifBlank { active.type.defaultBaseUrl })
        ) {
            return AiAssistantResponse(
                replyText = "${active.name} এর Endpoint URL সঠিক বা নিরাপদ নয়।",
                isError = true
            )
        }

        val primaryAdapter = createAdapter(active)
        try {
            val response = callBlock(primaryAdapter, activeKey)
            val shouldFailover = (response.isError && response.isRetryableError) || isErrorResponse(response.replyText)
            if (shouldFailover && _isBackupEnabled.value) {
                val backupResp = tryBackupProvider(active.id, callBlock)
                if (backupResp != null) return backupResp
            }
            return response
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Primary provider error: ${e.message}", e)
            if (_isBackupEnabled.value) {
                val backupResp = tryBackupProvider(active.id, callBlock)
                if (backupResp != null) return backupResp
            }
            return AiAssistantResponse(
                replyText = "AI প্রসেসিং ত্রুটি (${active.name}): ${AiEndpointSecurity.redactSecret(e.localizedMessage ?: "অজানা সমস্যা", activeKey)}",
                isError = true
            )
        }
    }

    private suspend fun tryBackupProvider(
        activeId: String,
        callBlock: suspend (UniversalAiProvider, String) -> AiAssistantResponse
    ): AiAssistantResponse? {
        val backup = _backupConfig.value ?: return null
        if (backup.id == activeId) return null
        val backupKey = secureStorage.getApiKey(backup.id).orEmpty()
        if (requiresApiKey(backup) && backupKey.isBlank()) return null

        return try {
            val backupAdapter = createAdapter(backup)
            val backupResp = callBlock(backupAdapter, backupKey)
            if (backupResp.isError) null else backupResp.copy(
                replyText = "${backupResp.replyText}\n\n[ব্যাকআপ ইঞ্জিন: ${backup.name}]"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Backup provider ${backup.name} also failed: ${e.message}")
            null
        }
    }

    private fun isErrorResponse(reply: String): Boolean {
        val errorKeywords = listOf("HTTP 401", "HTTP 403", "HTTP 429", "Quota Exceeded", "রেট লিমিট", "সার্ভার সাময়িকভাবে")
        return errorKeywords.any { reply.contains(it) }
    }

    private fun createAdapter(config: AiProviderConfig): UniversalAiProvider {
        return when (config.type) {
            AiProviderType.GOOGLE_GEMINI -> GeminiDirectProviderAdapter(config)
            AiProviderType.OPEN_ROUTER -> OpenRouterProviderAdapter(config)
            AiProviderType.CUSTOM_OPENAI -> OpenAiCompatibleProviderAdapter(config)
            AiProviderType.FIREBASE_AI -> FirebaseAiProviderAdapter(config)
        }
    }

    private fun persistConfigs(list: List<AiProviderConfig>) {
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[KEY_CONFIGS_JSON] = AiProviderConfig.listToJson(list)
            }
        }
    }

    private fun parseConfigsJson(jsonStr: String): List<AiProviderConfig> {
        return AiProviderConfig.listFromJson(jsonStr)
    }

    suspend fun executeAiQuery(
        prompt: String,
        history: List<ConversationEntity> = emptyList(),
        userName: String = "User"
    ): AiAssistantResponse = processQuery(prompt, history, userName)
}
