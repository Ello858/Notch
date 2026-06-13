# NotchDroid — CLI Build Guide

No Android Studio required. Build and deploy entirely from the terminal.

## Prerequisites

- **JDK 17** (required — JDK 26 will fail with `What went wrong: 26.0.1`)
- Android SDK command-line tools
- `platform-tools` (for `adb`)

## One-time SDK setup

```bash
export ANDROID_HOME=~/Android/Sdk
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

sdkmanager --sdk_root="$ANDROID_HOME" \
  "platform-tools" \
  "platforms;android-35" \
  "build-tools;35.0.0"

yes | sdkmanager --sdk_root="$ANDROID_HOME" --licenses
```

## Configure SDK path

```bash
cp local.properties.example local.properties
# Edit sdk.dir in local.properties to point at your SDK
```

Or set `ANDROID_HOME` — Gradle resolves it automatically.

## Build

```bash
export GRADLE_USER_HOME=~/.gradle
./gradlew-java17 assembleDebug
# or, if JDK 17 is your default:
# export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
# ./gradlew assembleDebug
```

First run downloads Gradle 8.9 and Android dependencies — requires network access.

> **JDK note:** If you see `What went wrong: 26.0.1`, your system Java is too new.
> Use `./gradlew-java17` or `export JAVA_HOME=/usr/lib/jvm/java-17-openjdk` before building.

## Install on device

```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Debug logs

```bash
adb logcat -s NotchDroid NotchOverlay MediaSession
```

## Permissions (granted via in-app onboarding)

1. **Display over other apps** — overlay window
2. **Notification access** — read active media sessions
3. **Notifications** (Android 13+) — foreground service notification
