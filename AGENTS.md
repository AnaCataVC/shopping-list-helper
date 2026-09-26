# AGENTS.md — Development & Architecture Directives

## 1. Project Overview & Architecture
`shopping-list-helper` (Por Comprar) is a lightweight, offline-first native Android application written in Kotlin.
It uses Jetpack Compose (Material 3) for the entire UI, Room 2.6.1 for local persistence, and no external DI or navigation framework.

- **Stack:**
  - Language: Kotlin 2.0.0
  - UI: Jetpack Compose with Material Design 3 (BOM 2024.06.00)
  - Database: AndroidX Room 2.6.1 with KSP schema tracking (`app/schemas/`)
  - Target SDK: 34 / Min SDK: 26
  - Toolchain: Gradle 8.9, AGP 8.5.2, JDK 17+ (Android Studio JBR recommended)

- **Source Layout:**
  - `app/src/main/java/com/anacatavc/shoppinglist/data/`: Room entities (`Category`, `Item`), DAO (`ShoppingDao`), Database configuration (`AppDatabase`), and JSON backup serialization (`Backup.kt`).
  - `app/src/main/java/com/anacatavc/shoppinglist/ui/`: Compose UI components (`MainActivity.kt`, `PendingScreen.kt`, `ShopScreen.kt`, `CategoriesScreen.kt`, `ItemDialog.kt`, `ItemRow.kt`, `Theme.kt`).
  - `app/schemas/`: Exported Room schemas for schema tracking and future migrations.
  - `docs/`: Technical specifications (`architecture.md`, `testing.md`).

---

## 2. Invariants & Development Directives

### 2.1. Offline-First & No Server Dependency
- All application data resides purely in the local SQLite database (`shopping.db`).
- Never introduce network calls, cloud synchronization dependencies, or background push notifications.
- Backup and restore strictly rely on Android's Storage Access Framework (`ACTION_CREATE_DOCUMENT` / `ACTION_OPEN_DOCUMENT`).

### 2.2. Due Dates & Recurrence Calculation
- Recurring purchase intervals are evaluated on-demand (`Item.isDue(now)`) when screens render.
- Do not introduce background `WorkManager` jobs or `AlarmManager` timers just to recalculate due states.
- When recurrence is toggled off on an item, `nextDueAt` must be set to `null` so the item becomes due immediately.

### 2.3. Data Integrity & Cascades
- Foreign key constraint between `Item` and `Category` is configured as `ForeignKey.RESTRICT`.
- Deleting a category that holds active or completed items requires reassigning its items to another category within a single database transaction (`ShoppingDao.moveItemsAndDeleteCategory`).

### 2.4. Compose UI State & Rotation
- All editable dialog states and user input fields must use `rememberSaveable` rather than transient `remember` to prevent loss of user data during device rotation or configuration changes.
- Themes (`SYSTEM`, `LIGHT`, `DARK`) are stored in `SharedPreferences` and applied dynamically via `AppTheme`. System bar icon colors must stay synchronized with the active theme style.

### 2.5. Localization
- Every user-facing string must reside in `res/values/strings.xml` (English, default) and `res/values-es/strings.xml` (Spanish).
- Seed categories are generated in the device language upon initial database creation.

---

## 3. Build & Test Commands

Always verify that `JAVA_HOME` points to a JDK 17+ (such as Android Studio's bundled JBR) and `ANDROID_HOME` is exported:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"

# JVM Unit Tests
.\gradlew testDebugUnitTest

# Assemble Debug APK
.\gradlew assembleDebug

# Instrumented UI & Room tests (requires active emulator / device)
.\gradlew connectedDebugAndroidTest
```
