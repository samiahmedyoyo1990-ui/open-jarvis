#!/usr/bin/env sh
# Lightweight Gradle bootstrap for environments where the Gradle Wrapper JAR
# was omitted from the exported project. Downloads the pinned official Gradle
# distribution and verifies it against the published SHA-256 checksum.
set -eu

GRADLE_VERSION="9.3.1"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
INSTALL_DIR="$GRADLE_USER_HOME/jarvis-bootstrap/gradle-$GRADLE_VERSION"
GRADLE_BIN="$INSTALL_DIR/bin/gradle"

if [ ! -x "$GRADLE_BIN" ]; then
  command -v curl >/dev/null 2>&1 || { echo "ERROR: curl is required. Install it with: pkg install curl" >&2; exit 1; }
  command -v unzip >/dev/null 2>&1 || { echo "ERROR: unzip is required. Install it with: pkg install unzip" >&2; exit 1; }
  command -v sha256sum >/dev/null 2>&1 || { echo "ERROR: sha256sum is required." >&2; exit 1; }
  mkdir -p "$GRADLE_USER_HOME/jarvis-bootstrap"
  TMP_DIR="$GRADLE_USER_HOME/jarvis-bootstrap/tmp-$GRADLE_VERSION"
  ZIP_FILE="$TMP_DIR/gradle-$GRADLE_VERSION-bin.zip"
  mkdir -p "$TMP_DIR"
  BASE_URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
  echo "Downloading Gradle $GRADLE_VERSION..."
  curl -fL --retry 3 --connect-timeout 20 "$BASE_URL" -o "$ZIP_FILE"
  curl -fL --retry 3 --connect-timeout 20 "$BASE_URL.sha256" -o "$ZIP_FILE.sha256"
  EXPECTED="$(tr -d '[:space:]' < "$ZIP_FILE.sha256")"
  ACTUAL="$(sha256sum "$ZIP_FILE" | awk '{print $1}')"
  [ "$EXPECTED" = "$ACTUAL" ] || { echo "ERROR: Gradle distribution checksum mismatch." >&2; rm -f "$ZIP_FILE"; exit 1; }
  unzip -q -o "$ZIP_FILE" -d "$GRADLE_USER_HOME/jarvis-bootstrap"
  rm -rf "$TMP_DIR"
fi
exec "$GRADLE_BIN" "$@"
