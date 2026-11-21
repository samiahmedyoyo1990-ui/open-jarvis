# JARVIS — GitHub Actions Cloud Build

This project can be built in GitHub Actions without installing Android Studio, an Android SDK, or a large IDE on the phone.

## What the workflow does

The workflow at `.github/workflows/android.yml`:

1. Runs on pushes, pull requests, or manual `workflow_dispatch`.
2. Uses a GitHub-hosted Ubuntu 24.04 runner and JDK 17.
3. Verifies `ANDROID_HOME`, accepts the Android SDK licenses, and ensures SDK Platform 36.1 and Build Tools 36.0.0 are installed.
4. Provisions Gradle 9.3.1 through the official `gradle/actions/setup-gradle` action. This deliberately does not depend on a `gradle-wrapper.jar` being present in this exported project.
5. Runs `:app:testDebugUnitTest`, `:app:lintDebug`, and `:app:assembleDebug`.
6. Uploads the debug APK when one was produced, plus test/lint reports, even if an earlier check failed.

## Put the project in GitHub

The contents of this ZIP are arranged with the Android project at the archive root. Upload/extract the contents into the **root of the GitHub repository**, preserving hidden files and folders such as `.github/workflows/android.yml`.

Do not upload the outer folder as a nested directory. The repository root should directly contain `settings.gradle.kts`, `build.gradle.kts`, `app/`, `gradle/`, and `.github/`.

Do not commit `local.properties`, API keys, `.env`, private signing keystores, or passwords. The workflow builds the debug APK and does not require a release signing key.

## Run the cloud build

1. Commit and push the project files to GitHub.
2. Open the repository's **Actions** tab.
3. Choose **JARVIS Android Cloud Build**.
4. Select **Run workflow** if starting manually, then select the default branch and run it.
5. Open the run to inspect the Unit Tests, Lint, and Debug APK steps.
6. Download `JARVIS-debug-APK` from the run's Artifacts section if the APK step produced it. Download `JARVIS-build-reports` to inspect test/lint results.

A successful Gradle configuration alone is not a successful APK build. Treat the build as complete only when `:app:assembleDebug` succeeds and the APK artifact is present. Unit-test and lint failures must also be reviewed before calling the project release-ready.

## Muse workflow

Muse can make the same repository changes if it has permission to edit and push to the connected GitHub repository. Give it this ZIP and ask it to:

- extract the project contents at the repository root (not in a nested folder);
- preserve `.github/workflows/android.yml` and all hidden files;
- commit and push the changes;
- start or inspect the **JARVIS Android Cloud Build** workflow;
- fix errors based on the actual Actions logs, rerun the workflow, and report the exact run URL, test results, lint result, APK artifact name, and any remaining failures.

Do not let it report success unless the workflow logs show the APK task succeeded and the APK artifact was uploaded.
