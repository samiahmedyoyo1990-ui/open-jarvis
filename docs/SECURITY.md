# Security — JARVIS Android

## Security Architecture

JARVIS is designed with local-first data storage and defense-in-depth protections for API keys, backups, external AI skills, and notifications.

---

## Key Controls

### 1. API Key Storage (`SecureApiKeyStorage`)
- User BYOK API keys and optional GitHub PAT tokens are encrypted at rest using Android Keystore (`AES/GCM/NoPadding`, 256-bit key in `jarvis_byok_master_key`).
- Gemini direct API requests send the key via the `x-goog-api-key` HTTP header rather than URL query parameters (`?key=`), preventing URL/proxy leakage.
- Error messages pass through `AiEndpointSecurity.redactSecret(...)` before surfacing to logs or UI.

### 2. Endpoint Validation (`AiEndpointSecurity`)
- Public external endpoints require `https://` without embedded `user:pass@` URL credentials.
- Unencrypted `http://` endpoints are permitted only for local/LAN hosts (`localhost`, `127.0.0.1`, `::1`, `10.0.2.2`, RFC 1918 private subnets) such as Ollama or LM Studio.

### 3. Encrypted Backup & Sync (`SyncEngine`)
- Exported backups use the `JARVIS_SYNC_V2:` envelope powered by `PBKDF2WithHmacSHA256` (100,000 iterations, random 16-byte salt) and `AES/GCM/NoPadding` (random 12-byte IV, 128-bit authentication tag).
- Legacy v1 AES-CBC payloads remain readable for backward-compatible restores.
- Restores execute inside `database.withTransaction` with deduplication and never rely on a hardcoded default passkey in the UI.

### 4. GitHub Skill Inspector & Runtime (`SkillSecurityInspector` / `SecureSkillRuntime`)
- `GitHubApiClient.parseUrl` rejects credential-smuggling `@` URLs, non-GitHub hosts, whitespace, and `..` path traversal.
- `SkillSecurityInspector` scans file paths, manifests, tool definitions, and README text for binaries (`.dex`, `.apk`, `.so`, `.jar`, `.exe`, `.sh`, etc.), path traversal, embedded credentials, and prompt-injection patterns.
- Only `NETWORK`, `DEVICE_ACTIONS`, and `STORAGE_READ` permissions are allowlisted; disallowed permissions cause `AuditStatus.BLOCKED`.
- `GitHubSkillsScreen` defaults requested skill permissions to **opt-in (unchecked)**, `GitHubSkillInstaller` strips newly escalated permissions on updates, and `SecureSkillRuntime` wraps active snippets in `<untrusted_skill_context>` delimiters while stripping embedded `<<ACTION:...>>` tags.

### 5. Notification Privacy (`JarvisNotificationListenerService`)
- Notification capture and AI summarization are **disabled by default** via DataStore preferences (`notification_capture_enabled`, `notification_ai_summary_consent`).
- Banking, MFS (bKash, Nagad, Rocket, Upay, etc.), authenticator, password manager, wallet, and SMS/messaging packages are filtered out automatically.
- OTP/PIN/verification codes matching numeric OTP contexts are redacted with `[REDACTED_SECRET]` / `[REDACTED_CODE]`.
