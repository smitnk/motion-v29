# V29 Build-Ready Pass

- Rebased from V28.
- Removed an unsafe AdvancedToolsPanel argument injection so the existing panel API remains intact.
- Added a GitHub Actions workflow using JDK 17 + Gradle 8.7.
- Workflow generates the Gradle wrapper and assembles `:app:assembleDebug`.
- APK is uploaded as a GitHub Actions artifact.

Important: this environment does not contain the Android SDK/Gradle toolchain needed for a genuine local APK compile, so this ZIP is build-ready but the APK itself is not claimed as locally verified.