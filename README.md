# Por Comprar — Shopping List Helper

Native Android app to keep track of things you need to buy, and to build the shopping list for a
given store type when you go shopping ("voy a comprar al supermercado").

## Features

- **Pending items** with category, urgency (high / medium / low), optional quantity and note.
- **Recurring purchases**: an item can repeat every N days. Buying it hides it until it is due again.
- **"Voy a comprar"**: pick a category and get its due items sorted by urgency. Tick them one by one
  or mark the whole list as bought.
- **Editable categories** (name and emoji). Deleting a category that still has items requires moving
  those items to another category first.
- **JSON backup export** through the system file picker (no storage permissions needed).
- **Light / dark theme** (Material Design 3), following the phone setting by default or forced from
  the menu (⋮ → Tema / Theme).
- **English and Spanish**, following the phone language. On Android 13+ the app language can also
  be changed on its own in system settings → Apps → Por Comprar → Language.
- Everything is stored locally on the device; there is no account or server.

## Build

Requirements: Android SDK (platform 34) and JDK 17+. The JDK bundled with Android Studio works.

```powershell
$env:JAVA_HOME = "<path to JDK 17+>"   # e.g. Android Studio's jbr folder
.\gradlew assembleDebug
.\gradlew installDebug                 # with a device or emulator connected
```

Or open the folder in Android Studio and run the `app` configuration.

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Backup

Menu (⋮) → *Exportar respaldo JSON*, then choose where to save the file (Drive, Downloads, etc.).
The format is documented in [docs/architecture.md](docs/architecture.md#backup-format).
There is no import yet.

## Tests

```powershell
.\gradlew testDebugUnitTest            # JVM unit tests
.\gradlew connectedDebugAndroidTest    # database + UI tests, needs a device/emulator (wipes app data)
```

Plan and manual checklist: [docs/testing.md](docs/testing.md).

## Project layout

See [docs/architecture.md](docs/architecture.md).
