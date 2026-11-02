# Project notes

- **No Android SDK on this machine.** The user does not have the Android SDK installed (Termux environment). Do not suggest, run, or assume `sdkmanager`, `adb`, `avdmanager`, Android Studio, or any `ANDROID_HOME`/`ANDROID_SDK_ROOT` based builds. Do not attempt `./gradlew assembleDebug`-style builds that require the SDK — they will fail. Prefer approaches that work without the SDK (static code review, plain JVM/Java tooling, manual APK inspection, etc.).
