# JARVIS (open-jarvis)

**Voice-first Personal AI Assistant for Android — built with Kotlin, Jetpack Compose, Room, Firebase AI Logic, and BYOK Universal AI Engine.**

[![Android](https://img.shields.io/badge/Android-7.0%2B%20(API%2024%2B)-brightgreen)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-blue)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)
[![GitHub](https://img.shields.io/badge/GitHub-samiahmedyoyo1990--ui%2Fopen--jarvis-00D9FF)](https://github.com/samiahmedyoyo1990-ui/open-jarvis)

---


## GitHub Actions Cloud Build

You can run the Android tests, lint, and debug APK build in GitHub Actions without installing Android SDK or a large IDE on your phone. See [CLOUD_BUILD_GUIDE.md](CLOUD_BUILD_GUIDE.md). The workflow is `.github/workflows/android.yml`.

## Overview

JARVIS is a modular Android personal AI assistant designed for **Bangla (বাংলা)**, **Romanized Bangla (Banglish)**, and **English** voice and text interaction. It combines multi-provider cloud/local AI routing, offline device intent actions, Room database persistence with tested migrations, opt-in notification intelligence, encrypted local backups, and a security-audited GitHub Skill Inspector.

---

## Implemented Capabilities

| System | Description | Status |
|---|---|---|
| **Universal AI Engine (BYOK)** | Supports Google Gemini (header auth `x-goog-api-key`), OpenRouter, Custom / OpenAI-Compatible endpoints (OpenAI, Groq, Ollama/LM Studio on LAN/localhost), and built-in Firebase AI Logic with optional backup failover. | Implemented |
| **Voice Pipeline (STT + TTS)** | Android `SpeechRecognizer` (`bn-BD` / `bn-IN` / `en-US`) and `TextToSpeech` with runtime `RECORD_AUDIO` checks, utterance watchdog completion, and optional background `"Hey JARVIS"` wake-word service. | Implemented |
| **Device Action Engine & Calculator** | Explicit offline intent routing (Open App, System Settings, Dialer, Web Search, Share, Device Info) guarded by `resolveActivity` plus recursive-descent arithmetic evaluator (`+ - * / % ( )`). | Implemented |
| **Multimodal Vision & Docs** | Analyzes text documents and downscaled JPEG images (`executeMultimodalQuery`) across Gemini, OpenRouter, Custom OpenAI, and Firebase AI Logic. | Implemented |
| **Live Web Research & Fact Check** | Queries DuckDuckGo Instant Answer and Wikipedia MediaWiki APIs, deduplicates real citations, and conservatively defaults unverified claims to `UNCERTAIN`. | Implemented |
| **Multi-Agent & Answer Modes** | 7 agent roles (`Planning`, `Research`, `Analysis`, `Coding`, `Creative`, `Verification`, `Reviewer`) and 6 answer modes (`Quick`, `Coding`, `Research`, `Creative`, `Deep Think`, `Precision`). | Implemented |
| **Room Persistence & Reminders** | Version 3 schema (`notes`, `tasks`, `conversation_history`, `user_memory`, `workspaces`, `routines`, `usage_logs`, `installed_skills`, `dev_workspace_repos`, `skill_audit_logs`) with non-destructive `MIGRATION_1_2` & `MIGRATION_2_3`, `AlarmManager` reminders, and `BootReceiver` rescheduling. | Implemented |
| **Encrypted Sync & Backup** | PBKDF2-HMAC-SHA256 + AES-256-GCM (`JARVIS_SYNC_V2:`) encrypted JSON export/import with transactional deduplicated restore and legacy v1 read support. | Implemented |
| **GitHub Skill Inspector** | Audits GitHub repositories for path traversal, suspicious binaries, embedded credentials, prompt-injection markers, and enforces a strict permission allowlist (`NETWORK`, `DEVICE_ACTIONS`, `STORAGE_READ`) with opt-in approval and update rollback. | Implemented |
| **Notification Intelligence** | Strictly opt-in `NotificationListenerService` with default-off capture & AI summary toggles, sensitive financial/MFS/OTP/authenticator package blocklist, and OTP/PIN regex redaction. | Implemented |
| **Personalization & Diagnostics** | 8 curated cyberpunk/minimal themes + custom theme editor, plus in-app diagnostics and redacted health report export. | Implemented |

---

## Project Architecture

```text
app/src/main/java/com/example/
├── MainActivity.kt                  # Edge-to-edge host activity & share intent handler
├── actions/
│   └── JarvisActionEngine.kt        # Offline intent router, math evaluator, safe Android intents
├── ai/
│   ├── AiBrain.kt                   # Prompt constants, action tag parser, response models
│   ├── UniversalAiEngine.kt         # Active/backup provider orchestration & multimodal routing
│   ├── providers/                   # GeminiDirect, OpenRouter, OpenAiCompatible, FirebaseAi adapters
│   ├── agents/MultiAgentEngine.kt   # 7-agent collaboration orchestrator
│   ├── research/                    # DuckDuckGo + Wikipedia web research & fact checking
│   ├── security/SecureApiKeyStorage.kt # Android Keystore AES-256-GCM vault
│   ├── skills/SkillsEngine.kt       # Core skills + assets/skills/*.json workflow loader
│   └── vision/VisionAndDocumentEngine.kt
├── automation/RoutinesEngine.kt     # DB-backed routine matcher with cooldown protection
├── data/
│   ├── local/JarvisDatabase.kt      # Room v3 DB with MIGRATION_1_2 & MIGRATION_2_3
│   ├── preferences/JarvisPreferences.kt
│   └── repository/JarvisRepository.kt
├── diagnostic/DiagnosticEngine.kt   # System health & permission verification
├── github/                          # GitHubApiClient, SkillManifestParser, SecurityInspector, Installer, Runtime
├── notifications/                   # ReminderScheduler, ReminderReceiver, BootReceiver, NotificationListener
├── sync/SyncEngine.kt               # PBKDF2 + AES-GCM v2 backup & transactional restore
├── voice/                           # SpeechRecognizerManager, TextToSpeechManager, JarvisWakeWordService
└── ui/
    ├── JarvisViewModel.kt
    ├── navigation/AppNavigation.kt
    └── screens/                     # Jetpack Compose Material 3 screens & Dashboard overlays
```

---

## Build & Verification

### Local Build Commands

The exported project originally contained an empty Gradle Wrapper JAR and empty launcher scripts.
The included `gradlew` is therefore a small bootstrap launcher: on first run it downloads the pinned
Gradle 9.3.1 distribution from the official Gradle distribution host, verifies its published SHA-256,
and caches it under `~/.gradle/jarvis-bootstrap/`. An internet connection, Java 17+, `curl`, `unzip`,
and `sha256sum` are required for that first run. On Termux, install the command-line tools with
`pkg install curl unzip coreutils -y`.

```bash
# Run unit & Robolectric tests
./gradlew :app:testDebugUnitTest

# Run Android Lint
./gradlew :app:lintDebug

# Build debug APK
./gradlew :app:assembleDebug
```

> Configure user API keys directly inside the app's **AI Providers (BYOK)** screen (stored in hardware-backed Android Keystore), or configure `google-services.json` for Firebase AI Logic. Never commit `*.keystore`, `*.jks`, `.env`, or API tokens.

---

## Repository & License

- **Repository:** https://github.com/samiahmedyoyo1990-ui/open-jarvis
- **Application ID:** `com.aistudio.jarvis.vqxpk`
- **License:** MIT
