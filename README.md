# Shopping List Helper (Ayudante de Compras)

[![Platform](https://img.shields.io/badge/Platform-Android%20(API%2026%2B)-3DDC84?style=flat&logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-7F52FF?style=flat&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.06.00-4285F4?style=flat&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Material Design 3](https://img.shields.io/badge/Material%20Design-3-7B5FD9?style=flat)](https://m3.material.io/)
[![Room](https://img.shields.io/badge/Room-2.6.1-4285F4?style=flat)](https://developer.android.com/training/data-storage/room)

Product page / Página del producto: [shopping-list-helper.ana-catalina.com](https://shopping-list-helper.ana-catalina.com)

[English](#english) • [Español](#español)

---

## English

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

## Español

Aplicación nativa para Android, offline-first y ligera, diseñada para gestionar compras recurrentes y armar la lista de compras por tipo de tienda (*"Voy a comprar al supermercado"*).

### Características

- **Ítems Pendientes:** Organización por categorías con 3 niveles de urgencia (Alta, Media, Baja), cantidades y notas opcionales.
- **Compras Recurrentes:** Repetición automática cada $N$ días. Al marcar un ítem como comprado, se oculta hasta la fecha en que vuelve a vencer.
- **Modo "Voy a comprar":** Filtra por tienda/categoría mostrando solo los ítems vencidos ordenados por urgencia. Permite marcar uno a uno o la lista completa con un solo toque.
- **Categorías Editables:** Nombre y emoji personalizables. Protección de integridad referencial: para eliminar una categoría con ítems asociados, el usuario debe reubicarlos en otra categoría primero.
- **Respaldo JSON Local:** Exporta el estado completo de la base de datos mediante el Storage Access Framework de Android (`ACTION_CREATE_DOCUMENT`), sin solicitar permisos intrusivos de almacenamiento.
- **Tema Claro y Oscuro:** Paleta personalizada con Material Design 3, configurable para seguir el sistema o fijarse manualmente.
- **Completamente Bilingüe:** Interfaz disponible en Español e Inglés, compatible con la selección por aplicación de Android 13+.
- **Privacidad Total:** Sin servidores, analíticas ni cuentas de usuario; todo se almacena localmente en el dispositivo.

### Pila Tecnológica y Arquitectura

- **Lenguaje:** Kotlin 2.0.0
- **Interfaz de Usuario:** Jetpack Compose con Material 3
- **Base de Datos:** Room 2.6.1 (SQLite) con `Flow` reactivo y seguimiento de esquemas con KSP
- **Target SDK:** 34 | **Min SDK:** 26

Documentación técnica detallada disponible en [docs/architecture.md](docs/architecture.md).

### Compilación e Instalación

Requisitos: Android SDK (API 34) y JDK 17+ (o el JBR incluido en Android Studio).

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"

# Compilar APK de depuración
.\gradlew assembleDebug

# Instalar en dispositivo o emulador conectado
.\gradlew installDebug
```

El APK compilado se genera en `app/build/outputs/apk/debug/app-debug.apk`.

### Pruebas Automatizadas

El proyecto incluye 3 capas de pruebas (unitarias, base de datos y flujos de UI):

```powershell
# Pruebas unitarias en JVM (reglas de cálculo y serialización JSON)
.\gradlew testDebugUnitTest

# Pruebas instrumentadas en emulador o dispositivo (Room DAO y flujos Compose)
.\gradlew connectedDebugAndroidTest
```

Plan de pruebas y lista de verificación manual: [docs/testing.md](docs/testing.md).

### Aprendizajes Técnicos y Decisiones Arquitectónicas

1. **Flujo Reactivo sin Sobrecarga:** La arquitectura centraliza la observación de `Flow` en `MainActivity` hacia componentes Composable puros, evitando la complejidad de librerías externas de navegación o inyección de dependencias para una app autocontenida.
2. **Cálculo Determinista sin Procesos en Segundo Plano:** El estado de vencimiento se calcula en tiempo real al renderizar (`Item.isDue(now)`), eliminando servicios en segundo plano (`WorkManager`) y optimizando el consumo de batería.
3. **Transaccionalidad en Integridad Referencial:** La reubicación y eliminación de categorías se ejecuta en una única `@Transaction` de Room, asegurando que la restricción de clave foránea (`RESTRICT`) jamás deje datos huérfanos.
4. **Persistencia de Formularios ante Rotación:** La captura de datos en diálogos emplea `rememberSaveable`, garantizando la retención de datos del usuario durante cambios de orientación sin necesidad de ViewModels adicionales.
