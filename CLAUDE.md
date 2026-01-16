# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew assembleDebug                # Debug build
./gradlew assemblePlayRelease          # Play Store release
./gradlew assembleAltstoreRelease      # AltStore release
./gradlew assembleManualRelease        # Manual distribution release
./gradlew test                         # JVM unit tests
./gradlew connectedAndroidTest         # Instrumented tests (requires device)
./gradlew lint                         # Static analysis
```

Build toggles (pass via `-P` or add to `gradle.properties`):
- `crashlyticsEnabled=false` - Disable Firebase Crashlytics
- `analyticsEnabled=false` - Disable Firebase Analytics
- `debugToolsEnabled=true` - Enable debug tools in release

## Architecture

Calendar DND uses clean architecture with four layers:

```
UI Layer (Compose)
    ↓
Domain Layer (Pure Kotlin)
    ↓
Data Layer (Repositories + DataStore)
    ↓
System Layer (Alarms, Workers, Receivers)
```

### Key Components

- **AutomationEngine** (`domain/engine/AutomationEngine.kt`): Pure Kotlin decision-making logic. Can be unit tested without Android dependencies. Takes `EngineInput`, returns `EngineOutput`.

- **MeetingWindowResolver** (`domain/planning/MeetingWindowResolver.kt`): Merges overlapping/touching calendar events into single windows to prevent DND flickering between back-to-back meetings.

- **EngineRunner** (`system/alarms/EngineRunner.kt`): Centralized entry point for all background execution. Called by AlarmReceiver, Workers, and BroadcastReceivers.

- **SettingsStore** (`data/prefs/SettingsStore.kt`): User preferences (persisted).

- **RuntimeStateStore** (`data/prefs/RuntimeStateStore.kt`): App runtime state including `dndSetByApp` ownership flag (ephemeral).

### Background Scheduling Strategy

1. **Primary**: Exact alarms at meeting boundaries (`setExactAndAllowWhileIdle`)
2. **Fallback**: WorkManager near-term guards (when exact alarms unavailable)
3. **Safety net**: 15-minute periodic SanityWorker

### DND Ownership Model

The app tracks whether it "owns" the current DND state via `RuntimeStateStore.dndSetByApp`. If user manually changes DND during a meeting, the app detects this (dndSetByApp=true but system DND differs) and sets `userSuppressedUntilMs` to stop interfering until meeting ends.

## Build Flavors

Three distribution variants (`distribution` dimension):
- `play` - Google Play Store
- `altstore` - AltStore
- `manual` - Direct download with built-in update checker (checks `update.json` from GitHub Releases)

## Testing

Unit tests live in `app/src/test/java/`. The `AutomationEngine` and `MeetingWindowResolver` are pure Kotlin and designed for testability.

Run a single test:
```bash
./gradlew test --tests "com.brunoafk.calendardnd.domain.planning.SchedulePlannerTest"
```

## Samsung-Specific Notes

This app is optimized for Samsung devices. Key considerations:
- Always wrap DND operations in try-catch (Samsung can throw even with permission)
- Multiple scheduling strategies compensate for aggressive battery management
- Guide users to disable battery optimization for reliability
