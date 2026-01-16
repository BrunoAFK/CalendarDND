# Calendar DND - Developer Setup Guide

Quick start guide for developers to get Calendar DND running.

## Prerequisites

- **Android Studio**: Hedgehog (2023.1.1) or newer
- **JDK**: 17 or higher
- **Android SDK**: API 26 (Android 8.0) minimum, API 34 (Android 14) target
- **Test Device**: Samsung device recommended (Galaxy S25 or similar)

---

## Project Setup

### 1. Clone Repository

```bash
git clone https://github.com/yourusername/calendar-dnd.git
cd calendar-dnd
```

### 2. Open in Android Studio

1. Open Android Studio
2. Select "Open an Existing Project"
3. Navigate to cloned directory
4. Click "Open"
5. Wait for Gradle sync to complete

### 3. Project Structure

The project should have this structure:

```
calendar-dnd/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/brunoafk/calendardnd/
│   │       │   ├── App.kt
│   │       │   ├── ui/
│   │       │   ├── domain/
│   │       │   ├── data/
│   │       │   ├── system/
│   │       │   └── util/
│   │       ├── res/
│   │       │   ├── values/
│   │       │   │   └── strings.xml
│   │       │   ├── xml/
│   │       │   │   ├── backup_rules.xml
│   │       │   │   └── data_extraction_rules.xml
│   │       │   └── drawable/
│   │       │       └── ic_tile.xml
│   │       └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

### 4. Create Missing Resource Files

If not already present, create these files:

#### `app/src/main/res/values/strings.xml`

See the `strings.xml` artifact for complete content.

#### `app/src/main/res/drawable/ic_tile.xml`

See the `ic_tile.xml` artifact for complete content.

#### `app/src/main/res/xml/backup_rules.xml`

See the `backup_rules.xml` artifact for complete content.

#### `app/src/main/res/xml/data_extraction_rules.xml`

See the `data_extraction_rules.xml` artifact for complete content.

---

## Build Configuration

### `build.gradle.kts` (Project Level)

```kotlin
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
}
```

### `build.gradle.kts` (App Level)

Use the provided artifact. Key dependencies:

- Jetpack Compose (BOM 2024.02.00)
- DataStore Preferences
- WorkManager
- Navigation Compose
- Material 3

### `gradle.properties`

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.enableJetifier=false
kotlin.code.style=official
```

---

## Running the App

### Connect Device

1. Enable Developer Options on Samsung device:
    - Settings → About phone → Tap "Build number" 7 times
2. Enable USB Debugging:
    - Settings → Developer options → USB debugging → ON
3. Connect via USB
4. Allow USB debugging when prompted

### Build and Run

**Via Android Studio**:
1. Click ▶️ Run button
2. Select your device
3. Wait for build and install

**Via Command Line**:
```bash
# Debug build
./gradlew assembleDebug
./gradlew installDebug

# Release build
./gradlew assembleRelease
```

### First Launch Setup

1. App opens to onboarding screen
2. Grant calendar permission when prompted
3. Grant DND policy access:
    - Tap "Grant Permission"
    - Find "Calendar DND" in list
    - Toggle ON
    - Return to app
4. Grant exact alarms (if prompted):
    - Tap "Grant Permission"
    - Enable for Calendar DND
5. Disable battery optimization (Samsung):
    - Settings → Battery → Background usage limits
    - Remove Calendar DND from sleeping apps

---

## Development Workflow

### Running Tests

**Unit Tests**:
```bash
./gradlew test
```

**Instrumentation Tests** (requires connected device):
```bash
./gradlew connectedAndroidTest
```

### Debugging

**View Logs**:
```bash
adb logcat | grep "CalendarDND"
```

**In-App Logs**:
- Open app → Tap 📄 icon → View debug logs

**Engine Testing**:
- Status screen → "Run Engine Now" button
- Forces immediate engine execution
- Check logs for decision output

### Code Style

**Format Code**:
- Android Studio: Code → Reformat Code (Ctrl+Alt+L / Cmd+Option+L)

**Kotlin Style**:
- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Max line length: 120 characters

---

## Common Issues

### Gradle Sync Failed

**Solution**:
1. File → Invalidate Caches → Invalidate and Restart
2. Delete `.gradle` and `.idea` folders
3. Re-open project

### Compose Not Found

**Solution**:
- Verify Compose Compiler version matches Kotlin version
- Current: Kotlin 1.9.0 → Compose Compiler 1.5.1

### DataStore Import Errors

**Solution**:
```kotlin
// Correct imports
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
```

### App Crashes on Launch

**Check**:
1. Manifest has all required permissions
2. All receivers/services declared
3. App class name matches manifest
4. No missing resource files

---

## Testing on Samsung Device

### Required Setup for Testing

1. **Calendar Events**:
    - Add Google/Samsung account
    - Create test calendar
    - Add test events

2. **Permissions**:
    - Calendar access
    - DND policy access
    - Exact alarms
    - Notification permission (Android 13+)

3. **Battery Settings**:
    - Disable battery optimization
    - Remove from sleeping apps
    - Background data allowed

### Creating Test Scenarios

**Quick Test Event**:
```kotlin
// Via Calendar app:
Title: "Test Meeting"
Start: Now + 2 minutes
End: Now + 12 minutes
Calendar: Any
Busy/Free: Busy
```

**Back-to-Back Test**:
```kotlin
// Event A:
Start: Now + 2 minutes
End: Now + 17 minutes

// Event B:
Start: Now + 17 minutes (exactly when A ends)
End: Now + 32 minutes
```

---

## Project Configuration Files

### File Locations

| File | Purpose | Location |
|------|---------|----------|
| `AndroidManifest.xml` | App manifest | `app/src/main/` |
| `strings.xml` | String resources | `app/src/main/res/values/` |
| `ic_tile.xml` | Tile icon | `app/src/main/res/drawable/` |
| `backup_rules.xml` | Backup config | `app/src/main/res/xml/` |
| `data_extraction_rules.xml` | Data extraction | `app/src/main/res/xml/` |
| `build.gradle.kts` | Build config | `app/` |

All file contents are provided in the artifacts.

---

## Architecture Quick Reference

```
UI Layer (Compose)
    ↓
Domain Layer (Engine - Pure Kotlin)
    ↓
Data Layer (Repositories, DataStore)
    ↓
System Layer (Alarms, Workers, Receivers)
```

**Key Classes to Understand**:

1. **AutomationEngine**: Decision-making brain
2. **EngineRunner**: Executes engine from background
3. **CalendarRepository**: Calendar data access
4. **DndController**: DND system control
5. **AlarmScheduler**: Exact alarm management

---

## Debugging Tips

### Enable Verbose Logging

Add to `EngineRunner`:
```kotlin
// At start of runEngine()
Log.d("EngineRunner", "Starting engine run, trigger: $trigger")
```

### View DataStore Contents

```bash
adb shell run-as com.brunoafk.calendardnd
cd files/datastore
cat settings.preferences_pb
```

### Force Engine Run

```kotlin
// In any Activity/Fragment:
lifecycleScope.launch {
    EngineRunner.runEngine(context, Trigger.MANUAL)
}
```

### Check Scheduled Alarms

```bash
adb shell dumpsys alarm | grep "com.brunoafk.calendardnd"
```

### Check WorkManager Jobs

```bash
adb shell dumpsys jobscheduler | grep "com.brunoafk.calendardnd"
```

---

## Performance Profiling

### CPU Profiler
1. Android Studio → View → Tool Windows → Profiler
2. Click + → Calendar DND
3. Run engine manually
4. Check for performance bottlenecks

### Memory Profiler
1. Track memory allocation during engine runs
2. Check for leaks in observers/coroutines

### Battery Profiler
1. Run app for 24 hours
2. Settings → Battery → Battery usage → Calendar DND
3. Should be <1% daily

---

## CI/CD Setup (Optional)

### GitHub Actions Example

```yaml
name: Android CI

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 17
        uses: actions/setup-java@v2
        with:
          java-version: '17'
      - name: Build with Gradle
        run: ./gradlew build
      - name: Run tests
        run: ./gradlew test
```

---

## Release Build

### Signing Configuration

1. Create keystore:
```bash
keytool -genkey -v -keystore calendar-dnd.keystore -alias calendar-dnd -keyalg RSA -keysize 2048 -validity 10000
```

2. Add to `app/build.gradle.kts`:
```kotlin
signingConfigs {
    create("release") {
        storeFile = file("path/to/calendar-dnd.keystore")
        storePassword = "your_password"
        keyAlias = "calendar-dnd"
        keyPassword = "your_password"
    }
}
```

3. Build:
```bash
./gradlew assembleRelease
```

### ProGuard (Optional)

Update `build.gradle.kts`:
```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

---

## Documentation

All documentation files are included in the repository:

- **[README.md](README.md)** - Project overview
- **[USER_GUIDE.md](USER_GUIDE.md)** - End user documentation
- **[DEVELOPER_DOCUMENTATION.md](DEVELOPER_DOCUMENTATION.md)** - Detailed architecture
- **[ACCEPTANCE_TESTS.md](ACCEPTANCE_TESTS.md)** - Test scenarios
- **[SETUP_GUIDE.md](SETUP_GUIDE.md)** - This file

---

## Getting Help

### Resources
- Android Developer Docs: https://developer.android.com
- Jetpack Compose: https://developer.android.com/jetpack/compose
- Kotlin Docs: https://kotlinlang.org/docs

### Community
- Stack Overflow: Tag `calendar-dnd`
- GitHub Issues: For bug reports

---

## Quick Start Checklist

- [ ] Clone repository
- [ ] Open in Android Studio
- [ ] Sync Gradle
- [ ] Create resource files (strings.xml, etc.)
- [ ] Connect Samsung device
- [ ] Build and run
- [ ] Grant all permissions
- [ ] Disable battery optimization
- [ ] Create test calendar event
- [ ] Verify DND triggers

---

**You're all set! Start coding! 🚀**