# JARVIS — Complete Project Repair, Security Hardening & Verification Plan

A full-stack engineering repair of the existing JARVIS Android assistant (Kotlin, Jetpack Compose, Room, Multi-Provider AI) to resolve confirmed functional bugs, eliminate critical backup/skill/notification security vulnerabilities, repair Gradle Wrapper and CI builds, align documentation with verified code, and deliver an installable debug APK backed by automated regression tests.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> Because the clarifying questions were skipped, this plan proceeds with the recommended engineering defaults below while preserving JARVIS's existing UI, `com.example` package structure, `com.aistudio.jarvis.vqxpk` application ID, and AI Studio signing setup.

- **Confirmed Approach — Build & CI**: Generate a complete, verifiable Gradle Wrapper (`gradlew`, `gradle-wrapper.jar`) and upgrade the GitHub Actions CI workflow with Android SDK provisioning, license acceptance, lint, unit tests, and debug APK artifacts, while preserving AI Studio's `debugConfig` and environment-based `release` signing blocks.
- **Confirmed Approach — Wake-Word Listener**: Harden the foreground microphone listener with explicit runtime permission guards, exponential backoff, duplicate-trigger suppression, and Android 10+ background-launch-safe notification actions, while honestly documenting hardware DSP limitations vs. standard `SpeechRecognizer`.
- **Confirmed Approach — Database & Backup Compatibility**: Implement explicit Room schema migrations without data loss and upgrade backup encryption to PBKDF2-HMAC-SHA256 + AES-256-GCM while keeping a backward-compatible read-only decryptor for legacy v1 backups.
- **Confirmed Approach — Feature Scope vs. Legacy Docs**: Implement all safe in-scope Android capabilities (including bundled skill JSON loading, prompt sanitization, response validation, risky-action confirmation, and usage pattern insights) and cleanly document external/OS-restricted legacy items (Accessibility auto-tap, Termux socket bridge, local GGUF inference, MCP daemon) rather than shipping fake stubs.

---

## 1. Overview & Core Concept

- **What It Does**: Stabilizes and secures every core subsystem of the JARVIS personal AI assistant—voice interaction (Bangla & English STT/TTS), scheduled task reminders, automated routines, multi-provider BYOK AI routing, web research & fact verification, GitHub skill sandboxing, encrypted backups, and opt-in notification intelligence.
- **Target User**: Personal Android user (`Android 7.0+` / `minSdk 24` up to `Android 16` / `targetSdk 36`, including Android 10 compatibility) interacting in Bangla, English, or Romanized Bangla via voice or text.
- **Key Value**: Transforms JARVIS from a build-passing prototype with silent runtime defects (unscheduled reminders, empty routine queries, static-IV backups, unapproved skill prompts, OTP leaks) into a verifiable, security-hardened, testable Android application.

---

## 2. User Experience & Visual Design

> [!NOTE]
> Per project rules, the existing cyberpunk HUD visual identity, theme presets, and Compose navigation flows are preserved rather than redesigned. UI changes are strictly targeted to surfacing real errors, permission controls, and security approvals.

- **Key User Flows Preserved & Repaired**:
  - **Voice & Action Loop**: Tapping the Arc Orb or microphone triggers runtime `RECORD_AUDIO` checks, live RMS amplitude waveforms, interruption-safe Bangla/English TTS, and confirmation prompts before sensitive device actions (e.g., dialing, external launches) triggered from natural language.
  - **Scheduled Tasks & Routines**: Creating a timed task schedules a real system alarm/work notification (never firing prematurely on creation) and synchronizes completion/cancellation with Room; enabling a routine persists across restarts and triggers deterministically without duplicate execution.
  - **Skill Security & Approval Gate**: Inspecting a GitHub skill displays a manifest & risk breakdown with explicit per-permission toggles (least privilege, default unchecked) before installation; installed tool definitions and bundled skill JSON assets load directly into the sandboxed runtime.
  - **Notification Privacy Controls**: Notification Intelligence is opt-in by default, features an editable protected-app blocklist, automatically masks OTP/2FA codes, and requires explicit user consent before sending redacted summaries to an AI provider.
- **Visual Identity & Theme**: Maintains JARVIS's existing dark HUD surface palette (`#070A12` background, `#0D1422` elevated cards, `#00D9FF` cyan arc primary, `#8B5CF6` violet accents, and Material 3 dynamic/preset themes via `Theme.kt`), ensuring 48dp touch targets, edge-to-edge `WindowInsets`, and clear loading/error banners so failed AI, TTS, or backup operations never hang silently.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1 — True Time-Based Reminders (`AlarmManager` + `WorkManager` + Boot Recovery)**
  - *Chosen Approach*: Schedule exact/inexact alarms paired with a `BroadcastReceiver` and `RECEIVE_BOOT_COMPLETED` rescheduler backed by Room, replacing immediate `notify()` calls on task creation.
  - *Why*: Survives app kills and device reboots while handling Android 13+ notification permissions and Android 12+ exact-alarm fallback cleanly.
  - *Alternatives Considered*: Coroutine `delay()` inside ViewModel (fails immediately when the app closes).

- **Decision 2 — Authenticated Backup Encryption (`AES-256-GCM` + `PBKDF2-HMAC-SHA256`)**
  - *Chosen Approach*: Version 2 encrypted envelope containing random salt, iteration metadata, random 12-byte GCM nonce, and authenticated ciphertext, wrapped in an atomic Room transaction; retains legacy v1 CBC fallback decryptor for recovery.
  - *Why*: Eliminates the confirmed static-IV (`0x42`) and unsalted SHA-256 vulnerability while preserving existing user backups.
  - *Alternatives Considered*: Hard-breaking legacy format (would lock users out of prior backups).

- **Decision 3 — Zero-Trust GitHub Skill Sandboxing**
  - *Chosen Approach*: Validate HTTPS GitHub URLs and manifest schemas, require explicit user opt-in for allowlisted permissions, store tools properly in `toolsJson`, load bundled asset skills via `AssetManager`, and wrap imported skill prompts in untrusted-context delimiters separate from trusted system instructions.
  - *Why*: Prevents untrusted repositories from injecting system overrides or calling unapproved tools.
  - *Alternatives Considered*: Prompt concatenation only (confirmed vulnerable to instruction hijacking).

- **Decision 4 — Strict AI Provider Error & Fallback Discipline**
  - *Chosen Approach*: Classify HTTP `400/401/402/403/404/408/429/5xx`, timeouts, and malformed payloads explicitly; never silently retry invalid keys or silently fall back to another paid provider unless the user explicitly enables Backup Provider fallback; redact API keys from all logs and error strings.
  - *Why*: Prevents infinite retry loops, surprise billing, and credential leakage in logs.

- **Decision 5 — Fact & Research Transparency**
  - *Chosen Approach*: Stop fabricating static citations (`"JARVIS Knowledge Base"`, `"Public Reference Data"`) or defaulting failed/empty checks to `VERIFIED`; cross-check retrieved DuckDuckGo + Wikipedia search extracts, deduplicate real source URLs, and return `UNCERTAIN` when evidence is absent.

---

## 4. Technical Architecture & Data Strategy

### Confirmed Audit Summary (Pre-Repair Baseline)

| Area | Verified Finding Classification |
|---|---|
| Gradle Wrapper & CI | **Confirmed Bug**: `gradlew` & `gradle-wrapper.jar` missing; `.github/workflows/android.yml` relies on system `gradle` and skips SDK setup/lint. |
| `RoutinesEngine` | **Confirmed Bug**: `databaseRoutines()` returns `emptyList()`, leaving all routines inert. |
| Reminder System | **Confirmed Bug**: `addTask()` fires notification immediately instead of scheduling for `dueDate`; no boot recovery. |
| `TextToSpeechManager` | **Confirmed Bug**: Missing timeout/utterance-ID guard and language/engine error state recovery. |
| `JarvisWakeWordService` | **Confirmed Bug + Design Limit**: Missing mic permission check before `startForeground`, background `startActivity` violates BAL rules, restart loop drains battery. |
| `MainActivity` & Sharing | **Incomplete Implementation**: `ACTION_SEND_MULTIPLE` declared in Manifest but unhandled; missing URI permission & `resolveActivity` guards. |
| `JarvisActionEngine` | **Confirmed Bug**: Loose `.contains()` triggers false app launches on normal sentences; division by zero yields `NaN`. |
| Room & `SyncEngine` | **Confirmed Vulnerability + Bug**: Static IV (`0x42`), missing PBKDF2/GCM tag, destructive migration, dropped `dueDate` and `routines` on restore, non-transactional import. |
| AI Engine & Research | **Confirmed Bug**: Broad fallback masks `401/402/404/408` errors; deprecated default model IDs in adapter; `WebResearchAndFactEngine` fabricates citations and defaults to `VERIFIED`. |
| GitHub Skills Security | **Confirmed Vulnerability + Bug**: `toolsJson` saved as `"[]"`, untrusted prompt appended directly to system prompt, bundled `assets/skills/*.json` disconnected, weak permission gating. |
| Notification Privacy | **Confirmed Vulnerability**: Captures all apps without opt-in, lacks OTP/sensitive-app blocklist, sends raw notification bodies to AI. |
| Legacy Docs vs. Code | **Integration/Doc Drift**: Old README advertises Termux bridge, Accessibility auto-tap, Floating Overlay, MCP, and Local GGUF that belong to removed legacy architecture. |

### System Architecture Diagram

```
┌───────────────────────────────────────────────────────────────────────────┐
│                         JARVIS Presentation Layer                         │
│  MainActivity (Share Intent + App Check) ──► Jetpack Compose Screens      │
│  Voice HUD │ Dashboard │ Tasks/Notes │ GitHub Skills │ Notifications │ AI │
└───────────────────────────────┬───────────────────────────────────────────┘
                                │ ViewModel & StateFlows
┌───────────────────────────────▼───────────────────────────────────────────┐
│                      JARVIS Core Orchestration Layer                      │
│  ┌─────────────────────┐  ┌─────────────────────┐  ┌───────────────────┐  │
│  │ Intent & Safety Gate│  │ Universal AI Engine │  │ Skill & Asset Mgr │  │
│  │ • Structured Parser │  │ • Gemini / Firebase │  │ • Allowlist Perms │  │
│  │ • Risky Action Gate │  │ • OpenRouter/Custom │  │ • Untrusted Wrap  │  │
│  │ • Prompt Sanitizer  │  │ • Explicit Fallbacks│  │ • Tool Sandbox    │  │
│  └──────────┬──────────┘  └──────────┬──────────┘  └─────────┬─────────┘  │
└─────────────┼────────────────────────┼───────────────────────┼────────────┘
              │                        │                       │
┌─────────────▼────────────────────────▼───────────────────────▼────────────┐
│                   Services, Schedulers & Persistence                      │
│  • TTS / STT Managers & Permission-Guarded Foreground Wake Listener       │
│  • AlarmManager + BroadcastReceiver + Boot Recovery (Scheduled Reminders) │
│  • Opt-In Notification Listener (Protected Blocklist + OTP Redaction)     │
│  • Room DB (Explicit Migrations v1→v2→v3→v4 + Transactional SyncEngine)   │
│  • Android Keystore AES-256-GCM Vault & PBKDF2-GCM Backup Envelope v2     │
└───────────────────────────────────────────────────────────────────────────┘
```

### Repair & Verification Strategy

1. **Build System & CI Repair**: Restore valid Gradle Wrapper binaries/scripts, keep `build.gradle.kts` signing configs intact while making release signing optional when env secrets are absent, and update `.github/workflows/android.yml` to install Android SDK platforms/build-tools, run wrapper validation, unit tests, lint, debug APK build, and artifact upload.
2. **Core Bug Fixes**: Wire `RoutinesEngine` to Room DAO flows/snapshots with cooldown dedup; build `ReminderScheduler` + `ReminderReceiver` + `BootReceiver` with Room sync; harden `TextToSpeechManager` utterance tracking & timeouts; refactor `JarvisWakeWordService` with permission checks, BAL-safe notifications, and battery-safe backoff; implement `ACTION_SEND_MULTIPLE` image handling and `resolveActivity` guards; upgrade `JarvisActionEngine` intent parsing, confirmation checks, and math evaluator.
3. **Database, Backup & Security Hardening**: Add explicit Room migrations and transactional restore; upgrade `SyncEngine` to PBKDF2 + AES-256-GCM (preserving legacy v1 decryption); enforce HTTP status discipline, response validation, and key redaction across AI adapters; replace fabricated citations in `WebResearchAndFactEngine`; enforce least-privilege skill permissions, prompt isolation, bundled JSON skill loading, and opt-in OTP-filtered Notification Intelligence.
4. **Regression Testing, Docs Sync & APK Delivery**: Expand JVM + Robolectric test suites across build, database migrations, AES-GCM backup/restore, TTS/voice state machines, AI error fallback, research validation, skill security, and notification filtering; update project documentation to match real verified capabilities; verify with unit tests, Android lint, and `assembleDebug` APK verification (without pushing to GitHub unless explicitly authorized).
