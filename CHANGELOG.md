# JARVIS — Changelog

## v1.0 (Current)

### Core Assistant & UI
- Jetpack Compose Material 3 interface with Navigation Compose backstack routes and Dashboard overlays (`MainDashboardScreen`, `ChatScreen`, `VoiceHudScreen`, `NotesScreen`, `TasksScreen`, `SettingsScreen`, `PersonalizationStudioScreen`, `AboutScreen`, `PrivacyScreen`, `GitHubSkillsScreen`, `AiProviderManagerScreen`, `AiAgentsScreen`, `WorkspacesScreen`, `SkillsMarketplaceScreen`, `RoutinesScreen`, `DiagnosticScreen`, `AiUsageDashboardScreen`, `SyncManagerScreen`, `NotificationIntelligenceScreen`, `ResearchAndFactScreen`, `HistoryScreen`).
- Share-to-JARVIS handling for both `ACTION_SEND` and `ACTION_SEND_MULTIPLE`.
- Custom `android:icon` / `roundIcon` and edge-to-edge WindowInsets support.

### Voice, Actions & Automation
- Android `SpeechRecognizer` (`bn-BD`, `bn-IN`, `en-US`) and `TextToSpeech` with uninitialized fallback callbacks and utterance stall watchdog.
- Background `"Hey JARVIS"` wake-word foreground service gated behind runtime `RECORD_AUDIO` permission with 3-second detection cooldown and zero raw-transcript logging.
- `JarvisActionEngine` with `resolveActivity` checks across intents, question-aware offline intent filtering, and recursive-descent arithmetic expression evaluation with divide-by-zero handling.
- `RoutinesEngine` backed by Room snapshots with per-routine execution cooldown.
- `ReminderScheduler` + `ReminderReceiver` + `BootReceiver` using `canScheduleExactAlarms()` guard and `setAndAllowWhileIdle` fallback (canceling alarms on task completion/deletion).

### Universal AI Engine (BYOK) & Intelligence
- `GeminiDirectProviderAdapter` using `x-goog-api-key` headers (no URL query parameters).
- `OpenRouterProviderAdapter`, `FirebaseAiProviderAdapter`, and `OpenAiCompatibleProviderAdapter` (`CUSTOM_OPENAI`) with HTTPS / local-LAN endpoint validation and secret redaction.
- Real multimodal Base64 image support (`generateMultimodalResponse` / `executeMultimodalQuery`) with proper stream closing and bitmap recycling.
- `WebResearchAndFactEngine` combining DuckDuckGo Instant Answer and Wikipedia MediaWiki APIs with deduplicated citations and conservative `UNCERTAIN` fallback.

### Persistence, Encrypted Sync & Privacy
- `JarvisDatabase` v3 with explicit non-destructive `MIGRATION_1_2` and `MIGRATION_2_3`.
- `SyncEngine` v2 encryption (`JARVIS_SYNC_V2:` envelope using `PBKDF2WithHmacSHA256` + `AES/GCM/NoPadding`, random salt & IV), legacy v1 read support, and transactional deduplicated restore.
- Opt-in `JarvisNotificationListenerService` with default-off capture/AI consent flags, bank/MFS/password-manager/SMS package blocklist, and OTP/PIN regex redaction.
- Hardened GitHub Skill Inspector (`GitHubApiClient`, `SkillSecurityInspector`, `GitHubSkillInstaller`, `SecureSkillRuntime`) enforcing `NETWORK` / `DEVICE_ACTIONS` / `STORAGE_READ` allowlist, opt-in permissions, untrusted context delimiters, real `toolsJson` storage, and rollback snapshots.
