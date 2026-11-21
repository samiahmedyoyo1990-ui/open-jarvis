# JARVIS AI Studio Project — Repair Report

## Scope
Source ZIP: `ga.i jarvis-ai-assistant.zip` (145 files in the original archive).
This repair preserves the existing app architecture, package name, application ID, provider configuration, and user-facing contracts where possible.

## Changes made

1. **Restored task reminder implementation**
   - Implemented `ReminderScheduler` with input validation, stable PendingIntent identity, scheduling, and cancellation.
   - Uses `setAndAllowWhileIdle` on Android 6+ rather than requiring the special exact-alarm permission; reminders may be delayed by Android battery policies.
   - Implemented `ReminderReceiver` to create the channel and display the reminder safely.
   - Implemented `BootReceiver` to re-schedule pending future reminders after reboot or app replacement.

2. **Fixed offline command parsing**
   - App launches now require an explicit launch phrase; merely asking a question about YouTube or WhatsApp no longer launches the app.
   - Replaced the first-operator arithmetic regex with a precedence-aware parser supporting parentheses and unary signs.
   - Division by zero returns a safe Bangla response rather than `NaN`.
   - Added regression assertions for nested division by zero and malformed expressions.

3. **Fixed wake-word test/API mismatch and permission handling**
   - Exposed `isWakeWordMatch` as a companion function, matching the existing unit test call site.
   - The listener now refuses to start without microphone permission and stops if permission is revoked.

4. **Fixed Android share handling**
   - `ACTION_SEND_MULTIPLE` now passes the selected shared URIs to the ViewModel.
   - The ViewModel analyzes up to 10 shared image URIs and reports per-file failures rather than silently ignoring all but text.

5. **Fixed debug signing**
   - Removed the custom debug signing config that referenced a missing `debug.keystore`.
   - Debug builds now use Gradle/Android's automatically generated default debug signing key.
   - Release signing still intentionally requires the owner's private release keystore and environment secrets.

6. **Hardened backup behavior**
   - Excluded `jarvis_secure_keystore_vault.xml` from cloud backup and device transfer because Android Keystore keys are device-bound and the encrypted API-key blobs cannot safely be restored without their key.

7. **Fixed package visibility for app launching**
   - Added launcher/camera/alarm intent queries so `getLaunchIntentForPackage` and `resolveActivity` can see compatible apps under Android 11+ package visibility rules.

8. **Aligned KSP with AGP 9 guidance**
   - Updated KSP from 2.3.5 to 2.3.6. Android's AGP 9 upgrade guidance recommends KSP 2.3.6 or newer.
   - Existing AGP 9.1.1 / Gradle 9.3.1 / Kotlin Gradle plugin 2.2.10 / JDK 17 versions were retained; these match the AGP 9.1.1 compatibility table.

9. **Repaired Gradle launch path**
   - The original `gradlew`, `gradlew.bat`, and `gradle-wrapper.jar` were zero-byte files.
   - Replaced the Unix launcher with a Gradle 9.3.1 bootstrap that verifies the official distribution SHA-256 before execution.
   - Added a Windows fallback that uses an installed Gradle executable and gives an explicit setup error otherwise.

## Cloud build preparation (added after the initial repair)

- Added `.github/workflows/android.yml` for GitHub Actions. It runs on push, pull request, and manual dispatch.
- The workflow uses a GitHub-hosted Ubuntu 24.04 runner, JDK 17, the Android SDK platform `android-36.1`, Build Tools `36.0.0`, and Gradle `9.3.1` provisioned by the official Gradle Actions setup action.
- The workflow executes `:app:testDebugUnitTest`, `:app:lintDebug`, and `:app:assembleDebug`, and uploads available APK and test/lint report artifacts.
- Enabled `android.useAndroidX=true` in `gradle.properties` because the app uses AndroidX dependencies and this property was absent from the exported project.
- Added `CLOUD_BUILD_GUIDE.md` with repository-root placement and workflow run instructions.

This GitHub Actions workflow has been prepared but **has not been run**: it must first be committed and pushed to a GitHub repository. Its first run is the first full CI verification of the cloud build configuration.

## Validation performed in this environment

- XML resource and manifest parsing: **PASS**
- JSON parsing for project JSON files: **PASS**
- `sh -n gradlew`: **PASS**
- Kotlin parser/syntax scan for the changed action engine: no Kotlin syntax diagnostics were emitted; full compilation was not possible because Android/Gradle dependencies are not present.
- Zero-byte file scan: **PASS** (no zero-byte project files remain).
- `./gradlew --version`: **BLOCKED** — the execution environment cannot resolve `services.gradle.org` (`Could not resolve host`). The Gradle distribution could not be downloaded.

## Not yet verified

- `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug` were **not run** because this environment has no Android SDK/Gradle distribution and outbound DNS is unavailable.
- No APK was produced here. A successful local build, test suite, cloud workflow run, and on-device behavior are therefore not claimed.
- Real-device behavior still needs testing for microphone foreground-service restrictions, vendor battery optimization, notification permission, and exact reminder delivery timing.
- Third-party API availability, Firebase project configuration, and provider-specific model access require live credentials/network and were not tested.

## Build and verify after extracting

For a phone-friendly cloud build, follow [CLOUD_BUILD_GUIDE.md](CLOUD_BUILD_GUIDE.md).


```bash
pkg install curl unzip coreutils openjdk-17 -y
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
```

Expected debug APK path after a successful build:
`app/build/outputs/apk/debug/app-debug.apk`

## Compatibility references

- Android Gradle Plugin 9.1.1 compatibility: https://developer.android.com/build/releases/agp-9-1-0-release-notes
- AGP 9 upgrade guidance (KSP 2.3.6+): https://github.com/android/skills/blob/main/build-system/agp/agp-9-upgrade/SKILL.md
