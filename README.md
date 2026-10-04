<p align="center">
  <img src="icon.png" alt="shopping-list-helper Logo" width="120" />
</p>

# Shopping List Helper

[English](README.md) | [Español](README.es.md)

[![Platform](https://img.shields.io/badge/Platform-Android%20(API%2026%2B)-3DDC84?style=flat&logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-7F52FF?style=flat&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.06.00-4285F4?style=flat&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Material Design 3](https://img.shields.io/badge/Material%20Design-3-7B5FD9?style=flat)](https://m3.material.io/)
[![Room](https://img.shields.io/badge/Room-2.6.1-4285F4?style=flat)](https://developer.android.com/training/data-storage/room)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

Product page: [shopping-list-helper.ana-catalina.com](https://shopping-list-helper.ana-catalina.com)

---

A native, offline-first Android application to manage recurring household purchases and plan shopping trips by store type (*"Going shopping to the supermarket"*).

### Features

- **Pending Items:** Categorized shopping items with 3 urgency tiers (High, Medium, Low), optional notes, and custom quantity fields.
- **Recurring Purchases:** Automated recurrence rule (every $N$ days). Marking an item as bought hides it until its next calculated due date.
- **"Going Shopping" Mode:** Select a specific store or category to get only its due items sorted by urgency. Check off items individually or mark the whole store list in one tap.
- **Category Management:** Custom category names and emoji icons. Relational integrity prevents orphaned items by requiring a target destination before deleting any populated category.
- **Local JSON Backup Export:** Exports full application state using Android's Storage Access Framework (`ACTION_CREATE_DOCUMENT`), requiring zero runtime storage permissions.
- **Theme Adaptation:** Dual Material Design 3 palette (light and dark) supporting system default or manual user selection.
- **Bilingual Interface:** Full localization in English and Spanish, respecting device locale and Android 13+ per-app language settings.
- **100% Privacy & Offline:** No accounts, telemetry, or server communication; all data is kept locally on device.

### Tech Stack & Architecture

- **Language:** Kotlin 2.0.0
- **UI Toolkit:** Jetpack Compose with Material 3 (single-activity architecture, no external navigation/DI bloat)
- **Local Persistence:** AndroidX Room 2.6.1 (SQLite) with reactive Kotlin `Flow`s and KSP schema tracking
- **Target SDK:** 34 | **Min SDK:** 26

For comprehensive technical specifications, review [docs/architecture.md](docs/architecture.md).

### Build & Run

Requirements: Android SDK (API 34) and JDK 17+ (such as the JBR bundled with Android Studio).

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"

# Compile debug APK
.\gradlew assembleDebug

# Install on a connected device / emulator
.\gradlew installDebug
```

The compiled APK will be output to `app/build/outputs/apk/debug/app-debug.apk`.

### Automated Testing

The project maintains a 3-layer test hierarchy (unit, database, and Compose UI flows):

```powershell
# JVM unit tests (due date rules, JSON backup serialization)
.\gradlew testDebugUnitTest

# Instrumented tests on connected emulator or device (Room DAO and Compose UI flows)
.\gradlew connectedDebugAndroidTest
```

Detailed test strategy and manual QA checklist: [docs/testing.md](docs/testing.md).

### Key Learnings & Architectural Decisions

1. **Lightweight Reactive Flow Pipeline:** Rather than introducing heavy DI frameworks (Hilt, Koin) or navigation libraries for a focused utility, `MainActivity` observes Room `Flow` streams directly and propagates state to pure composable screens.
2. **On-Demand Due Calculations:** Avoided background battery-draining services or `WorkManager` workers. Due dates are evaluated deterministically at UI render time (`Item.isDue(now)`), preserving device battery.
3. **Atomic Relational Operations:** Using Room `@Transaction` to atomically move items across foreign-key-restricted categories prior to deletion eliminates database integrity corruption.
4. **Resilient Compose State:** Form inputs utilize `rememberSaveable` to withstand configuration changes and screen rotations without relying on heavyweight ViewModel singletons for transient dialogs.

---

---

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.

