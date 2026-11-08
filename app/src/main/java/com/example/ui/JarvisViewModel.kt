package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.actions.ExecutionResult
import com.example.actions.JarvisAction
import com.example.actions.JarvisActionEngine
import com.example.ai.AiBrainManager
import com.example.data.local.JarvisDatabase
import com.example.data.local.entity.ConversationEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.preferences.JarvisPreferences
import com.example.data.repository.JarvisRepository
import com.example.notifications.ReminderNotificationHelper
import com.example.ui.components.AssistantState
import com.example.voice.JarvisWakeWordService
import com.example.voice.SpeechRecognizerManager
import com.example.voice.SpeechState
import com.example.voice.TextToSpeechManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.data.theme.ThemeConfig
import com.example.data.theme.ThemeManager
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import android.net.Uri
import com.example.ai.agents.AgentRole
import com.example.ai.agents.MultiAgentEngine
import com.example.ai.agents.MultiAgentSession
import com.example.ai.analytics.UsageAnalyticsEngine
import com.example.ai.analytics.UsageDashboardData
import com.example.ai.modes.AnswerMode
import com.example.ai.model.AiModelInfo
import com.example.ai.model.AiProviderConfig
import com.example.ai.model.ConnectionTestResult
import com.example.ai.research.FactVerificationReport
import com.example.ai.research.ResearchResult
import com.example.ai.research.WebResearchAndFactEngine
import com.example.ai.skills.JarvisSkill
import com.example.ai.skills.SkillsEngine
import com.example.ai.vision.DocumentAnalysisResult
import com.example.ai.vision.VisionAnalysisResult
import com.example.ai.vision.VisionAndDocumentEngine
import com.example.automation.RoutinesEngine
import com.example.data.local.entity.RoutineEntity
import com.example.data.local.entity.WorkspaceEntity
import com.example.diagnostic.DiagnosticEngine
import com.example.diagnostic.DiagnosticItem
import com.example.diagnostic.DiagnosticReport
import com.example.sync.SyncDeviceProfile
import com.example.sync.SyncEngine
import com.example.sync.SyncResult
import com.example.github.*
import com.example.data.local.entity.InstalledSkillEntity
import com.example.data.local.entity.DevWorkspaceRepoEntity
import com.example.data.local.entity.SkillAuditLogEntity
import kotlinx.coroutines.flow.first
import kotlin.math.sin

enum class DashboardOverlay {
    NONE,
    NOTES,
    TASKS,
    SETTINGS,
    PERSONALIZATION,
    ABOUT,
    GITHUB_SKILLS
}

sealed interface InspectionUiState {
    data object Idle : InspectionUiState
    data object Inspecting : InspectionUiState
    data class Success(val report: SecurityAuditReport) : InspectionUiState
    data class Error(val message: String) : InspectionUiState
}

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "JarvisViewModel"
    private val context = application.applicationContext

    // Dependencies
    private val database = JarvisDatabase.getInstance(context)
    val preferences = JarvisPreferences(context)
    val themeManager = ThemeManager(context)
    val repository = JarvisRepository(database, preferences)
    val actionEngine = JarvisActionEngine(context)
    val aiBrain = AiBrainManager(context, actionEngine)
    val speechManager = SpeechRecognizerManager(context)
    val ttsManager = TextToSpeechManager(context)

    // Theme Streams
    val currentTheme: StateFlow<ThemeConfig> = themeManager.currentTheme
    val customProfiles: StateFlow<List<ThemeConfig>> = themeManager.customProfiles

    fun applyTheme(theme: ThemeConfig) {
        themeManager.applyTheme(theme)
    }

    fun saveCustomProfile(profile: ThemeConfig) {
        themeManager.saveCustomProfile(profile)
    }

    fun deleteProfile(id: String) {
        themeManager.deleteProfile(id)
    }

    fun resetTheme() {
        themeManager.resetToDefault()
    }

    // Universal AI Engine & BYOK Streams
    val universalAiEngine get() = aiBrain.universalAiEngine
    val allAiConfigs: StateFlow<List<AiProviderConfig>> = universalAiEngine.allConfigs
    val activeAiConfig: StateFlow<AiProviderConfig> = universalAiEngine.activeConfig
    val backupAiConfig: StateFlow<AiProviderConfig?> = universalAiEngine.backupConfig
    val isAiBackupEnabled: StateFlow<Boolean> = universalAiEngine.isBackupEnabled

    fun setActiveAiProvider(providerId: String) = universalAiEngine.setActiveProvider(providerId)
    fun setBackupAiProvider(providerId: String?) = universalAiEngine.setBackupProvider(providerId)
    fun setAiBackupEnabled(enabled: Boolean) = universalAiEngine.setBackupEnabled(enabled)
    fun saveAiProviderConfig(config: AiProviderConfig, apiKey: String?) = universalAiEngine.saveProviderConfig(config, apiKey)
    fun deleteAiProviderConfig(providerId: String) = universalAiEngine.deleteProviderConfig(providerId)
    suspend fun testAiConnection(providerId: String, apiKeyOverride: String? = null): ConnectionTestResult =
        universalAiEngine.testProviderConnection(providerId, apiKeyOverride)
    suspend fun fetchModelsForProvider(providerId: String, apiKeyOverride: String? = null): List<AiModelInfo> =
        universalAiEngine.fetchModelsForProvider(providerId, apiKeyOverride)
    fun getSavedApiKey(providerId: String): String? = universalAiEngine.secureStorage.getApiKey(providerId)
    fun hasSavedApiKey(providerId: String): Boolean = universalAiEngine.secureStorage.hasApiKey(providerId)

    // 23-System Core Engines
    val multiAgentEngine = MultiAgentEngine(universalAiEngine)
    val skillsEngine = SkillsEngine(context)
    val webResearchAndFactEngine = WebResearchAndFactEngine(universalAiEngine)
    val visionAndDocumentEngine = VisionAndDocumentEngine(context, universalAiEngine)
    val routinesEngine = RoutinesEngine(repository, actionEngine)
    val diagnosticEngine = DiagnosticEngine(context, universalAiEngine, database)
    val usageAnalyticsEngine = UsageAnalyticsEngine(repository)
    val syncEngine = SyncEngine(context, database)

    // System 23: Answer Modes
    private val _currentAnswerMode = MutableStateFlow(AnswerMode.QUICK)
    val currentAnswerMode: StateFlow<AnswerMode> = _currentAnswerMode.asStateFlow()
    fun setAnswerMode(mode: AnswerMode) { _currentAnswerMode.value = mode }

    // System 18: Project Workspaces
    val workspaces: StateFlow<List<WorkspaceEntity>> = repository.allWorkspaces.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    private val _currentWorkspaceSlug = MutableStateFlow("personal")
    val currentWorkspaceSlug: StateFlow<String> = _currentWorkspaceSlug.asStateFlow()
    fun selectWorkspace(slug: String) { _currentWorkspaceSlug.value = slug }
    fun saveWorkspace(ws: WorkspaceEntity) = viewModelScope.launch { repository.saveWorkspace(ws) }
    fun deleteWorkspace(id: Long) = viewModelScope.launch { repository.deleteWorkspaceById(id) }

    // System 07: Routines
    val routines: StateFlow<List<RoutineEntity>> = repository.allRoutines.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    fun toggleRoutine(id: Long, enabled: Boolean) = viewModelScope.launch { repository.toggleRoutine(id, enabled) }
    fun saveRoutine(routine: RoutineEntity) = viewModelScope.launch { repository.saveRoutine(routine) }
    fun deleteRoutine(id: Long) = viewModelScope.launch { repository.deleteRoutineById(id) }

    // System 21: Usage Logs
    val usageDashboardData: StateFlow<UsageDashboardData> = usageAnalyticsEngine.dashboardData.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UsageDashboardData(0, 0L, 0.0, 0L, emptyList())
    )
    fun clearUsageLogs() = viewModelScope.launch { usageAnalyticsEngine.clearHistory() }

    // System 19: Diagnostics
    val diagnosticReport: StateFlow<DiagnosticReport?> = diagnosticEngine.report
    fun runDiagnostics() = viewModelScope.launch { diagnosticEngine.runFullDiagnostics() }
    suspend fun performSafeRepair(item: DiagnosticItem): String = diagnosticEngine.performSafeRepair(item)
    fun exportDiagnosticReport(): String = diagnosticEngine.exportRedactedReport()

    // System 17: Share-to-JARVIS Integration
    private val _incomingSharedText = MutableStateFlow<String?>(null)
    val incomingSharedText: StateFlow<String?> = _incomingSharedText.asStateFlow()
    fun handleSharedContent(sharedText: String?, sharedUri: Uri?) {
        if (!sharedText.isNullOrBlank()) {
            _incomingSharedText.value = sharedText
            processUserQuery("Shared to JARVIS: $sharedText")
        } else if (sharedUri != null) {
            viewModelScope.launch {
                val res = visionAndDocumentEngine.analyzeImage(sharedUri)
                _lastAiResponse.value = res.description
            }
        }
    }
    fun clearSharedContent() { _incomingSharedText.value = null }

    // Universal GitHub & Skill Installer
    val gitHubApiClient = GitHubApiClient(
        tokenProvider = { universalAiEngine.secureStorage.getApiKey("github_pat") }
    )
    val gitHubSkillInstaller = GitHubSkillInstaller(context, gitHubApiClient, repository)
    val secureSkillRuntime = SecureSkillRuntime(
        context = context,
        aiEngine = universalAiEngine,
        actionEngine = actionEngine,
        researchEngine = webResearchAndFactEngine
    )

    val installedSkills: StateFlow<List<InstalledSkillEntity>> = repository.allInstalledSkills.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val devWorkspaceRepos: StateFlow<List<DevWorkspaceRepoEntity>> = repository.allDevRepos.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recentAuditLogs: StateFlow<List<SkillAuditLogEntity>> = repository.recentAuditLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _gitHubSearchResults = MutableStateFlow<List<GitHubRepoItem>>(emptyList())
    val gitHubSearchResults: StateFlow<List<GitHubRepoItem>> = _gitHubSearchResults.asStateFlow()

    private val _isSearchingGitHub = MutableStateFlow(false)
    val isSearchingGitHub: StateFlow<Boolean> = _isSearchingGitHub.asStateFlow()

    private val _inspectionState = MutableStateFlow<InspectionUiState>(InspectionUiState.Idle)
    val inspectionState: StateFlow<InspectionUiState> = _inspectionState.asStateFlow()

    private val _installerStatusMessage = MutableStateFlow<String?>(null)
    val installerStatusMessage: StateFlow<String?> = _installerStatusMessage.asStateFlow()

    val featuredGitHubSkills: List<GitHubRepoItem> = gitHubSkillInstaller.getFeaturedSkills()

    fun searchGitHub(query: String) {
        viewModelScope.launch {
            _isSearchingGitHub.value = true
            try {
                val results = gitHubApiClient.searchRepositories(query)
                _gitHubSearchResults.value = results
            } catch (e: Exception) {
                _gitHubSearchResults.value = emptyList()
            } finally {
                _isSearchingGitHub.value = false
            }
        }
    }

    fun inspectGitHubRepository(url: String, branch: String? = null) {
        viewModelScope.launch {
            _inspectionState.value = InspectionUiState.Inspecting
            try {
                val report = gitHubSkillInstaller.inspectRepositoryUrl(url, branch)
                _inspectionState.value = InspectionUiState.Success(report)
            } catch (e: Exception) {
                _inspectionState.value = InspectionUiState.Error("পরিদর্শন ব্যর্থ হয়েছে: ${e.localizedMessage}")
            }
        }
    }

    fun clearInspection() {
        _inspectionState.value = InspectionUiState.Idle
    }

    fun installSkillFromInspection(report: SecurityAuditReport, approvedPermissions: List<String>) {
        viewModelScope.launch {
            val id = gitHubSkillInstaller.installSkill(report, approvedPermissions)
            if (id > 0) {
                _installerStatusMessage.value = "স্কিল '${report.skillManifest?.name ?: "Skill"}' সফলভাবে ইনস্টল করা হয়েছে।"
                _lastActionFeedback.value = "স্কিল ইনস্টল সম্পন্ন।"
            } else {
                _installerStatusMessage.value = "স্কিল ইনস্টলেশন ব্যর্থ হয়েছে।"
            }
            clearInspection()
        }
    }

    fun importDevRepoFromInspection(report: SecurityAuditReport) {
        viewModelScope.launch {
            val id = gitHubSkillInstaller.importToDeveloperWorkspace(report)
            if (id > 0) {
                _installerStatusMessage.value = "সোর্স কোড সফলভাবে ডেভেলপার ওয়ার্কস্পেসে ইমপোর্ট হয়েছে।"
                _lastActionFeedback.value = "ডেভেলপার ওয়ার্কস্পেসে ইমপোর্ট সম্পন্ন।"
            }
            clearInspection()
        }
    }

    fun toggleInstalledSkill(id: Long, enabled: Boolean) {
        viewModelScope.launch { repository.setSkillEnabled(id, enabled) }
    }

    fun deleteInstalledSkill(id: Long) {
        viewModelScope.launch {
            repository.deleteInstalledSkill(id)
            _lastActionFeedback.value = "স্কিল আনইনস্টল করা হয়েছে।"
        }
    }

    fun deleteDevRepo(id: Long) {
        viewModelScope.launch {
            repository.deleteDevRepo(id)
            _lastActionFeedback.value = "ওয়ার্কস্পেস রিপোজিটরি মুছে ফেলা হয়েছে।"
        }
    }

    fun updateInstalledSkill(skill: InstalledSkillEntity) {
        viewModelScope.launch {
            val ok = gitHubSkillInstaller.updateSkill(skill)
            if (ok) {
                _lastActionFeedback.value = "'${skill.name}' সফলভাবে আপডেট হয়েছে।"
            } else {
                _lastActionFeedback.value = "আপডেট ব্যর্থ হয়েছে।"
            }
        }
    }

    fun rollbackInstalledSkill(skill: InstalledSkillEntity) {
        viewModelScope.launch {
            val ok = gitHubSkillInstaller.rollbackSkill(skill)
            if (ok) {
                _lastActionFeedback.value = "'${skill.name}' পূর্ববর্তী সংস্করণে রোলব্যাক করা হয়েছে।"
            } else {
                _lastActionFeedback.value = "রোলব্যাক ব্যর্থ হয়েছে।"
            }
        }
    }

    fun saveGitHubToken(token: String) {
        universalAiEngine.secureStorage.saveApiKey("github_pat", token)
        _lastActionFeedback.value = "GitHub অ্যাক্সেস টোকেন সংরক্ষিত।"
    }

    fun getGitHubToken(): String? {
        return universalAiEngine.secureStorage.getApiKey("github_pat")
    }

    // UI States
    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _lastAiResponse = MutableStateFlow("হ্যালো! আমি JARVIS। আপনাকে কীভাবে সাহায্য করতে পারি?")
    val lastAiResponse: StateFlow<String> = _lastAiResponse.asStateFlow()

    private val _lastActionFeedback = MutableStateFlow<String?>(null)
    val lastActionFeedback: StateFlow<String?> = _lastActionFeedback.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _lastUserQuery = MutableStateFlow("")
    val lastUserQuery: StateFlow<String> = _lastUserQuery.asStateFlow()

    private val _isMicrophoneAvailable = MutableStateFlow(speechManager.isAvailable)
    val isMicrophoneAvailable: StateFlow<Boolean> = _isMicrophoneAvailable.asStateFlow()

    private val _activeOverlay = MutableStateFlow(DashboardOverlay.NONE)
    val activeOverlay: StateFlow<DashboardOverlay> = _activeOverlay.asStateFlow()

    // Combined audio amplitude (reacts to mic when listening; reacts to synthetic vocal oscillation when speaking)
    private val _visualizerAmplitude = MutableStateFlow(0f)
    val visualizerAmplitude: StateFlow<Float> = _visualizerAmplitude.asStateFlow()

    // Jobs
    private var speakingAmplitudeJob: Job? = null
    private var continuousModeJob: Job? = null

    // Preferences & Settings Streams
    val isContinuousMode: StateFlow<Boolean> = preferences.continuousModeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val animationEnabled: StateFlow<Boolean> = preferences.animationEnabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val animationIntensity: StateFlow<Float> = preferences.animationIntensityFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 1.0f
    )

    val reducedMotion: StateFlow<Boolean> = preferences.reducedMotionFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val notes: StateFlow<List<NoteEntity>> = repository.allNotes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val tasks: StateFlow<List<TaskEntity>> = repository.allTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val conversations: StateFlow<List<ConversationEntity>> = repository.recentConversations.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val userName: StateFlow<String> = preferences.userNameFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "বস"
    )

    val preferredLanguage: StateFlow<String> = preferences.preferredLanguageFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "bn"
    )

    val speechRate: StateFlow<Float> = preferences.speechRateFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 1.0f
    )

    val speechPitch: StateFlow<Float> = preferences.speechPitchFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 1.0f
    )

    val autoSpeak: StateFlow<Boolean> = preferences.autoSpeakFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    // Background Wake Word Streams & Controls
    val isWakeWordServiceRunning: StateFlow<Boolean> = JarvisWakeWordService.isServiceRunning

    val isWakeWordEnabled: StateFlow<Boolean> = preferences.wakeWordEnabledFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun toggleWakeWord(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setWakeWordEnabled(enabled)
            if (enabled) {
                JarvisWakeWordService.start(context)
            } else {
                JarvisWakeWordService.stop(context)
            }
        }
    }

    fun handleWakeWordTriggered() {
        Log.d(TAG, "Wake word trigger received in ViewModel")
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(100)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibrator error on wake word trigger", e)
        }
        startListening()
    }

    init {
        ReminderNotificationHelper.createNotificationChannel(context)
        observeSpeechState()
        observeSpeechAmplitude()
        viewModelScope.launch {
            repository.initDefaultWorkspacesIfEmpty()
            repository.initDefaultRoutinesIfEmpty()
        }
    }

    private fun observeSpeechState() {
        viewModelScope.launch {
            speechManager.speechState.collect { state ->
                when (state) {
                    is SpeechState.Idle -> {
                        if (_assistantState.value == AssistantState.LISTENING) {
                            _assistantState.value = AssistantState.IDLE
                        }
                    }
                    is SpeechState.Listening -> {
                        _assistantState.value = AssistantState.LISTENING
                        _errorMessage.value = null
                    }
                    is SpeechState.PartialResult -> {
                        _liveTranscript.value = state.text
                    }
                    is SpeechState.FinalResult -> {
                        _liveTranscript.value = state.text
                        processUserQuery(state.text)
                    }
                    is SpeechState.Error -> {
                        _assistantState.value = AssistantState.ERROR
                        _errorMessage.value = state.message
                        _lastActionFeedback.value = state.message
                    }
                }
            }
        }
    }

    private fun observeSpeechAmplitude() {
        viewModelScope.launch {
            speechManager.audioAmplitude.collect { amp ->
                if (_assistantState.value == AssistantState.LISTENING) {
                    _visualizerAmplitude.value = amp
                }
            }
        }
    }

    fun startListening() {
        triggerHaptic()
        // If JARVIS was speaking, interrupt immediately
        stopSpeaking()
        continuousModeJob?.cancel()

        _liveTranscript.value = ""
        _errorMessage.value = null
        _lastActionFeedback.value = null

        val lang = preferredLanguage.value
        speechManager.startListening(lang)
    }

    fun stopListening() {
        triggerHaptic()
        speechManager.stopListening()
        continuousModeJob?.cancel()
        if (_assistantState.value == AssistantState.LISTENING) {
            _assistantState.value = AssistantState.IDLE
        }
    }

    fun toggleListening() {
        if (_assistantState.value == AssistantState.LISTENING) {
            stopListening()
        } else if (_assistantState.value == AssistantState.SPEAKING) {
            // Instant interrupt when user taps microphone while speaking
            startListening()
        } else {
            startListening()
        }
    }

    fun processUserQuery(query: String) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return

        _lastUserQuery.value = cleanQuery
        _liveTranscript.value = cleanQuery
        _assistantState.value = AssistantState.THINKING
        _errorMessage.value = null

        viewModelScope.launch {
            // Save user message to memory
            repository.addConversationMessage(isUser = true, text = cleanQuery)

            // 1. Check Automation & Routines (System 07)
            val routineTrigger = routinesEngine.checkAndExecuteTrigger(cleanQuery)
            if (routineTrigger != null) {
                _lastAiResponse.value = routineTrigger.spokenFeedback
                _lastActionFeedback.value = routineTrigger.executedAction
                repository.addConversationMessage(
                    isUser = false,
                    text = routineTrigger.spokenFeedback,
                    actionExecuted = routineTrigger.executedAction
                )
                if (autoSpeak.value) {
                    speakResponse(routineTrigger.spokenFeedback)
                } else {
                    _assistantState.value = AssistantState.IDLE
                    handleContinuousListeningContinuation()
                }
                return@launch
            }

            // 1b. Check Natural Language GitHub & Skill Installer Commands
            val ghUrl = Regex("(https?://github\\.com/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+)").find(cleanQuery)?.value
            if (ghUrl != null && (cleanQuery.contains("install", ignoreCase = true) || cleanQuery.contains("ইন্সটল", ignoreCase = true) || cleanQuery.contains("ইমপোর্ট", ignoreCase = true))) {
                _lastActionFeedback.value = "গিটহাব রিপোজিটরি পরীক্ষা করা হচ্ছে..."
                val report = gitHubSkillInstaller.inspectRepositoryUrl(ghUrl)
                val reply = if (report.isInstallable) {
                    val id = gitHubSkillInstaller.installSkill(report, report.requestedPermissions)
                    "✅ স্যার, '${report.skillManifest?.name}' স্কিলটি সফলভাবে ইন্সটল ও সক্রিয় করা হয়েছে। এটি এখন ব্যবহারের জন্য প্রস্তুত।"
                } else if (report.isDeveloperSource) {
                    gitHubSkillInstaller.importToDeveloperWorkspace(report)
                    "📁 এটি একটি ডেভেলপার সোর্স কোড রিপোজিটরি। এটি ডেভেলপার ওয়ার্কস্পেসে সংরক্ষিত করা হয়েছে।"
                } else {
                    "⚠️ রিপোজিটরিটি সরাসরি ইন্সটলযোগ্য স্কিল হিসেবে সমর্থিত নয় (${report.status.banglaLabel})।"
                }
                _lastAiResponse.value = reply
                repository.addConversationMessage(isUser = false, text = reply)
                if (autoSpeak.value) speakResponse(reply) else _assistantState.value = AssistantState.IDLE
                return@launch
            }

            if ((cleanQuery.contains("find", ignoreCase = true) && cleanQuery.contains("skill", ignoreCase = true)) ||
                cleanQuery.contains("স্কিল খুঁজে", ignoreCase = true)) {
                val q = cleanQuery.replace("find", "").replace("skill", "").replace("ai", "").replace("স্কিল", "").replace("খুঁজে বের করো", "").trim()
                val searchQ = if (q.isNotBlank()) "$q skill" else "ai assistant skill"
                val found = gitHubSkillInstaller.apiClient.searchRepositories(searchQ)
                _gitHubSearchResults.value = found
                val summary = if (found.isNotEmpty()) {
                    "গিটহাবে ${found.size} টি স্কিল পাওয়া গেছে:\n" +
                    found.take(3).joinToString("\n") { "• ${it.fullName} (⭐ ${it.stars}): ${it.description.take(50)}" } +
                    "\n\n'GitHub & Skills' স্ক্রিনে গিয়ে বিস্তারিত দেখতে পারেন।"
                } else {
                    "গিটহাবে কোনো স্কিল পাওয়া যায়নি।"
                }
                _lastAiResponse.value = summary
                repository.addConversationMessage(isUser = false, text = summary)
                if (autoSpeak.value) speakResponse(summary) else _assistantState.value = AssistantState.IDLE
                return@launch
            }

            if (cleanQuery.contains("show installed skills", ignoreCase = true) ||
                cleanQuery.contains("ইন্সটল করা স্কিল", ignoreCase = true)) {
                val skills = repository.allInstalledSkills.first()
                val reply = if (skills.isEmpty()) {
                    "বর্তমানে কোনো বাহ্যিক গিটহাব স্কিল ইন্সটল করা নেই। আপনি 'GitHub & Skills' থেকে নতুন স্কিল যোগ করতে পারেন।"
                } else {
                    "আপনার ডিভাইসে ${skills.size} টি স্কিল ইন্সটল রয়েছে:\n" +
                    skills.joinToString("\n") { "• ${it.name} (v${it.version}) - ${if (it.isEnabled) "সক্রিয়" else "নিষ্ক্রিয়"}" }
                }
                _lastAiResponse.value = reply
                repository.addConversationMessage(isUser = false, text = reply)
                if (autoSpeak.value) speakResponse(reply) else _assistantState.value = AssistantState.IDLE
                return@launch
            }

            // 2. Query Universal AI Engine with Answer Mode (System 23) + Active Installed Skills
            val mode = currentAnswerMode.value
            val activeSkills = repository.enabledInstalledSkills.first()
            val skillsContext = secureSkillRuntime.buildActiveSkillsPromptContext(activeSkills)

            val basePrompt = if (mode != AnswerMode.QUICK) {
                "$cleanQuery\n\n[${mode.systemPromptSuffix}]"
            } else {
                cleanQuery
            }

            val enrichedQuery = if (skillsContext.isNotBlank()) {
                "$skillsContext\n$basePrompt"
            } else {
                basePrompt
            }

            val startTime = System.currentTimeMillis()
            val history = conversations.value
            val response = aiBrain.askJarvis(
                userPrompt = enrichedQuery,
                recentHistory = history,
                userName = userName.value
            )
            val duration = System.currentTimeMillis() - startTime

            // 3. Record Usage & Cost Analytics (System 21)
            val active = activeAiConfig.value
            usageAnalyticsEngine.recordRequest(
                providerId = active.id,
                modelId = active.selectedModel,
                promptLengthChars = cleanQuery.length,
                responseLengthChars = response.replyText.length,
                latencyMs = duration
            )

            _lastAiResponse.value = response.replyText

            // Action execution step
            var actionDescription: String? = null
            if (response.action !is JarvisAction.None) {
                _assistantState.value = AssistantState.EXECUTING

                when (val action = response.action) {
                    is JarvisAction.SaveNote -> {
                        repository.saveNote(action.title, action.content)
                        actionDescription = "নোট সংরক্ষিত: ${action.title}"
                    }
                    is JarvisAction.AddTask -> {
                        repository.addTask(action.title, priority = action.priority)
                        actionDescription = "টাস্ক তৈরি: ${action.title}"
                    }
                    else -> {}
                }

                val execResult: ExecutionResult = actionEngine.executeAction(response.action)
                _lastActionFeedback.value = execResult.feedbackMessage
                if (actionDescription == null) {
                    actionDescription = execResult.actionType
                }
                delay(300) // Brief execution pulse for fluid UX
            } else {
                _lastActionFeedback.value = null
            }

            // Record JARVIS response to local memory
            repository.addConversationMessage(
                isUser = false,
                text = response.replyText,
                actionExecuted = actionDescription
            )

            // Voice synthesis
            if (autoSpeak.value) {
                speakResponse(response.replyText)
            } else {
                _assistantState.value = AssistantState.IDLE
                handleContinuousListeningContinuation()
            }
        }
    }

    fun retryLastQuery() {
        val last = _lastUserQuery.value
        if (last.isNotBlank()) {
            processUserQuery(last)
        } else {
            startListening()
        }
    }

    fun speakResponse(text: String) {
        _assistantState.value = AssistantState.SPEAKING
        startSpeakingAmplitudeSimulation()

        ttsManager.speak(
            text = text,
            speechRate = speechRate.value,
            speechPitch = speechPitch.value,
            onDone = {
                stopSpeakingAmplitudeSimulation()
                _assistantState.value = AssistantState.IDLE
                handleContinuousListeningContinuation()
            }
        )
    }

    private fun handleContinuousListeningContinuation() {
        if (isContinuousMode.value) {
            continuousModeJob?.cancel()
            continuousModeJob = viewModelScope.launch {
                delay(650) // Natural conversational turn delay
                if (_assistantState.value == AssistantState.IDLE) {
                    startListening()
                }
            }
        }
    }

    fun stopSpeaking() {
        stopSpeakingAmplitudeSimulation()
        ttsManager.stop()
        continuousModeJob?.cancel()
        if (_assistantState.value == AssistantState.SPEAKING) {
            _assistantState.value = AssistantState.IDLE
        }
    }

    private fun startSpeakingAmplitudeSimulation() {
        speakingAmplitudeJob?.cancel()
        speakingAmplitudeJob = viewModelScope.launch {
            var step = 0f
            while (isActive && _assistantState.value == AssistantState.SPEAKING) {
                val wave1 = (sin(step.toDouble()) * 0.35 + 0.55).toFloat()
                val wave2 = (sin((step * 2.3).toDouble()) * 0.15).toFloat()
                _visualizerAmplitude.value = (wave1 + wave2).coerceIn(0.2f, 0.95f)
                step += 0.3f
                delay(40)
            }
            _visualizerAmplitude.value = 0f
        }
    }

    private fun stopSpeakingAmplitudeSimulation() {
        speakingAmplitudeJob?.cancel()
        speakingAmplitudeJob = null
        _visualizerAmplitude.value = 0f
    }

    fun toggleContinuousMode() {
        val current = isContinuousMode.value
        viewModelScope.launch {
            preferences.setContinuousMode(!current)
        }
    }

    fun setOverlay(overlay: DashboardOverlay) {
        _activeOverlay.value = overlay
    }

    fun closeOverlay() {
        _activeOverlay.value = DashboardOverlay.NONE
    }

    // Notes
    fun saveNote(title: String, content: String, category: String = "সাধারণ") {
        viewModelScope.launch {
            repository.saveNote(title, content, category)
            _lastActionFeedback.value = "নোট সংরক্ষিত হয়েছে: $title"
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
            _lastActionFeedback.value = "নোটটি মুছে ফেলা হয়েছে।"
        }
    }

    // Tasks
    fun addTask(title: String, priority: String = "Normal", reminderMinutes: Int? = null) {
        viewModelScope.launch {
            val dueDate = if (reminderMinutes != null && reminderMinutes > 0) {
                System.currentTimeMillis() + (reminderMinutes * 60 * 1000L)
            } else null

            val taskId = repository.addTask(title, dueDate = dueDate, priority = priority, reminderSet = reminderMinutes != null)
            _lastActionFeedback.value = "টাস্ক যোগ করা হয়েছে: $title"

            if (dueDate != null) {
                ReminderNotificationHelper.showReminderNotification(
                    context = context,
                    notificationId = taskId.toInt(),
                    title = "রিমাইন্ডার সেট করা হয়েছে",
                    content = "$title - নির্ধারিত সময়ে আপনাকে মনে করিয়ে দেওয়া হবে।"
                )
            }
        }
    }

    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.setTaskCompleted(task.id, !task.isCompleted)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
            _lastActionFeedback.value = "টাস্ক মুছে ফেলা হয়েছে।"
        }
    }

    fun clearConversations() {
        viewModelScope.launch {
            repository.clearConversationHistory()
            _lastActionFeedback.value = "কথোপকথনের ইতিহাস মুছে ফেলা হয়েছে।"
        }
    }

    fun updateUserName(name: String) {
        viewModelScope.launch {
            preferences.setUserName(name)
        }
    }

    fun updateLanguage(lang: String) {
        viewModelScope.launch {
            preferences.setPreferredLanguage(lang)
        }
    }

    fun updateSpeechRate(rate: Float) {
        viewModelScope.launch {
            preferences.setSpeechRate(rate)
        }
    }

    fun updateSpeechPitch(pitch: Float) {
        viewModelScope.launch {
            preferences.setSpeechPitch(pitch)
        }
    }

    fun updateAutoSpeak(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setAutoSpeak(enabled)
        }
    }

    fun updateAnimationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setAnimationEnabled(enabled)
        }
    }

    fun updateAnimationIntensity(intensity: Float) {
        viewModelScope.launch {
            preferences.setAnimationIntensity(intensity)
        }
    }

    fun updateReducedMotion(reduced: Boolean) {
        viewModelScope.launch {
            preferences.setReducedMotion(reduced)
        }
    }

    private fun triggerHaptic() {
        try {
            val vibrator = context.getSystemService(Application.VIBRATOR_SERVICE) as? Vibrator
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(35)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Haptic feedback unavailable", e)
        }
    }

    fun analyzeDocument(uri: Uri, fileName: String, query: String? = null) {
        viewModelScope.launch {
            _assistantState.value = AssistantState.THINKING
            _lastUserQuery.value = "📄 $fileName"
            repository.addConversationMessage(isUser = true, text = "ডকুমেন্ট বিশ্লেষণ: $fileName")
            try {
                val res = visionAndDocumentEngine.analyzeDocument(uri, fileName, query)
                val reply = "📄 **${res.fileName}** (${res.characterCount} অক্ষর)\n\n${res.summary}"
                _lastAiResponse.value = reply
                repository.addConversationMessage(isUser = false, text = reply)
            } catch (e: Exception) {
                val errorMsg = "ডকুমেন্ট বিশ্লেষণ ব্যর্থ হয়েছে: ${e.localizedMessage}"
                _lastAiResponse.value = errorMsg
                repository.addConversationMessage(isUser = false, text = errorMsg)
            } finally {
                _assistantState.value = AssistantState.IDLE
            }
        }
    }

    fun analyzeImageAttachment(uri: Uri, prompt: String = "এই ছবিটি বিশ্লেষণ করুন") {
        viewModelScope.launch {
            _assistantState.value = AssistantState.THINKING
            _lastUserQuery.value = "🖼️ ছবি বিশ্লেষণ"
            repository.addConversationMessage(isUser = true, text = "ছবি পাঠানো হয়েছে: $prompt")
            try {
                val res = visionAndDocumentEngine.analyzeImage(uri, prompt)
                val reply = res.description
                _lastAiResponse.value = reply
                repository.addConversationMessage(isUser = false, text = reply)
            } catch (e: Exception) {
                val errorMsg = "ছবি বিশ্লেষণ ব্যর্থ হয়েছে: ${e.localizedMessage}"
                _lastAiResponse.value = errorMsg
                repository.addConversationMessage(isUser = false, text = errorMsg)
            } finally {
                _assistantState.value = AssistantState.IDLE
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        speakingAmplitudeJob?.cancel()
        continuousModeJob?.cancel()
        speechManager.stopListening()
        ttsManager.shutdown()
    }
}
