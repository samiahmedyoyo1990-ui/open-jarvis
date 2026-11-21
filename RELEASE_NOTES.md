# JARVIS v1.0 Release Notes

> Personal AI Assistant for Android with Bangla, Romanized Bangla, and English voice & text interaction.

## Highlights

- **Universal AI Engine (BYOK):** Configure Google Gemini, OpenRouter, Custom OpenAI-compatible APIs (OpenAI, Groq, local Ollama / LM Studio), or built-in Firebase AI Logic with automatic failover.
- **Voice & Wake-Word Pipeline:** Push-to-talk speech recognition (`bn-BD`, `bn-IN`, `en-US`), Bengali/English TTS, and optional background `"Hey JARVIS"` wake-word listener.
- **Database & Reminders:** Local Room v3 persistence with schema migrations, task due-date reminders (`AlarmManager`), and boot rescheduling.
- **Encrypted Sync & Backup:** PBKDF2 + AES-256-GCM passphrase-encrypted backup export and deduplicated transactional restore.
- **GitHub Skill Inspector:** Repository URL validation, security audit scanning, permission allowlist enforcement, opt-in permission grants, update permission escalation checks, and 1-click rollback.
- **Opt-In Notification Intelligence:** Sensitive financial/MFS/OTP/authenticator apps are blocked automatically, OTP codes are redacted, and AI summarization requires explicit consent.

## Requirements

- Android 7.0+ (API 24+ minSdk, compiled & targeted against Android 16 / SDK 36)
- Firebase configuration (`google-services.json`) for built-in Firebase AI Logic, or any BYOK provider key configured in Settings → AI Providers.

## Notes

- Physical device features (live OEM TTS voices, microphone wake-word hardware behavior, and Play-signed release APKs) should be validated on a target Android device.
