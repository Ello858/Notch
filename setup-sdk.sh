#!/usr/bin/env bash
# One-time Android SDK setup for CLI-only builds (no Android Studio).
set -euo pipefail

ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export ANDROID_HOME
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

echo "SDK root: $ANDROID_HOME"

sdkmanager --sdk_root="$ANDROID_HOME" \
  "platform-tools" \
  "platforms;android-35" \
  "build-tools;35.0.0"

yes | sdkmanager --sdk_root="$ANDROID_HOME" --licenses

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
if [ ! -f "$PROJECT_DIR/local.properties" ]; then
  echo "sdk.dir=$ANDROID_HOME" > "$PROJECT_DIR/local.properties"
  echo "Created local.properties"
fi

echo "Done. Build with: ./gradlew assembleDebug"
