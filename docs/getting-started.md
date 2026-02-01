# Getting Started

Development environment setup for Calendar DND.

## Prerequisites

- **Android Studio**: Ladybug or newer
- **JDK**: 17 or higher
- **Android SDK**: API 26-36

## Clone and Build

```bash
# Clone the repository
git clone https://github.com/BrunoAFK/CalendarDND.git
cd CalendarDND

# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

## Project Structure

```
CalendarDND/
├── app/
│   ├── src/
│   │   ├── main/           # Shared code
│   │   ├── play/           # Play Store flavor
│   │   └── manual/         # Manual distribution flavor
│   └── build.gradle.kts
├── docs/                   # Documentation (you are here)
├── scripts/                # Build scripts
├── CLAUDE.md               # AI assistant instructions
└── README.md               # User-facing readme
```

## Source Code Layout

```
com.brunoafk.calendardnd/
├── App.kt                  # Application class
├── ui/                     # UI layer
│   ├── MainActivity.kt
│   ├── navigation/         # Navigation setup
│   ├── screens/            # All screens
│   ├── components/         # Reusable components
│   └── theme/              # Material theme
├── domain/                 # Business logic (pure Kotlin)
│   ├── engine/             # AutomationEngine
│   ├── planning/           # Meeting resolvers
│   └── model/              # Data classes
├── data/                   # Data access
│   ├── calendar/           # Calendar queries
│   ├── dnd/                # DND control
│   └── prefs/              # DataStore preferences
├── system/                 # Android system integration
│   ├── alarms/             # Alarm scheduling
│   ├── workers/            # WorkManager jobs
│   ├── receivers/          # Broadcast receivers
│   └── notifications/      # Notification helpers
└── util/                   # Utilities
```

## Running the App

### Debug Build

```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/play/debug/app-play-debug.apk`

### With Debug Tools

Debug tools are enabled by default in debug builds. To enable in release:

```bash
./gradlew assemblePlayRelease -PdebugToolsEnabled=true
```

## Testing on Device

1. Enable Developer Options on your Android device
2. Enable USB Debugging
3. Connect via USB
4. Run:

```bash
./gradlew installDebug
```

## Required Permissions

The app needs these permissions to function. Grant them during testing:

1. **Calendar Access**: Required to read events
2. **Notification Policy Access**: Required to control DND (Settings > Apps > Special access)
3. **Exact Alarms**: Required for precise scheduling (Android 12+)
4. **Notifications**: Required for pre-meeting warnings (Android 13+)

## IDE Setup

### Android Studio

1. Open Android Studio
2. File > Open > Select `CalendarDND` folder
3. Wait for Gradle sync
4. Select `app` configuration
5. Click Run

### Recommended Plugins

- Kotlin
- Compose Multiplatform IDE Support

## Troubleshooting

### Gradle Sync Fails

```bash
./gradlew clean
./gradlew --refresh-dependencies
```

### JDK Issues

Ensure Android Studio uses JDK 17:
- File > Settings > Build > Gradle > Gradle JDK

### Device Not Detected

```bash
adb devices
adb kill-server
adb start-server
```

## Next Steps

- [Architecture](architecture.md) - Understand the codebase
- [Testing](testing.md) - Run and write tests
- [Configuration](configuration.md) - Build flavors and toggles
