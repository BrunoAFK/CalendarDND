# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Summary

Calendar DND is an Android app that automatically manages Do Not Disturb mode based on calendar events. Built with Kotlin/Jetpack Compose following Clean Architecture.

## Build Commands

```bash
./gradlew assembleDebug          # Debug APK
./gradlew assemblePlayRelease    # Play Store release
./gradlew assembleManualRelease  # Manual distribution
./gradlew installDebug           # Install on device
./gradlew test                   # Unit tests
./gradlew :app:testPlayDebugUnitTest --tests "ClassName"  # Single test class
./gradlew connectedAndroidTest   # Instrumented tests
./gradlew lint                   # Code quality
./gradlew compilePlayDebugKotlin # Quick compile check
```

## Architecture

Four-layer Clean Architecture:

```
UI Layer (Compose) → Domain Layer (Pure Kotlin) → Data Layer → System Layer
```

- **UI** (`ui/`): Compose screens, navigation, components
- **Domain** (`domain/`): Pure Kotlin business logic - no Android dependencies
- **Data** (`data/`): Repositories for calendar, DND, preferences (DataStore)
- **System** (`system/`): AlarmManager, WorkManager, BroadcastReceivers

### Core Flow

All background triggers → `EngineRunner.runEngine()` → `AutomationEngine.run(EngineInput)` → `EngineOutput` → apply DND changes → schedule next alarm

### Key Files

| File | Purpose |
|------|---------|
| `domain/engine/AutomationEngine.kt` | Decision-making brain (pure Kotlin, deterministic) |
| `domain/engine/EngineInput.kt` | All inputs: settings, permissions, current state |
| `system/alarms/EngineRunner.kt` | Orchestration - gathers input, runs engine, applies output |
| `domain/planning/MeetingWindowResolver.kt` | Merges overlapping/consecutive meetings |
| `data/calendar/CalendarRepository.kt` | Calendar queries via ContentResolver |
| `data/prefs/SettingsStore.kt` | Persistent user preferences |
| `data/prefs/RuntimeStateStore.kt` | Transient automation state |
| `data/dnd/DndController.kt` | Android DND system interface |
| `domain/util/SkipUtils.kt` | Per-event skip logic utilities |
| `domain/model/OneTimeAction.kt` | Skip/Enable action models |

### Background Scheduling (3 tiers)

1. **Exact Alarms** (primary): `AlarmManager.setExactAndAllowWhileIdle()` at meeting boundaries
2. **WorkManager Guards** (fallback): 2 min before/after boundaries when exact alarms unavailable
3. **SanityWorker** (safety net): Periodic 15-minute checks

### User Override Detection

App tracks `dndSetByApp` in RuntimeStateStore. If user manually disables DND during a meeting, the app sets `userSuppressedUntilMs` and stops interfering until meeting ends.

### Per-Event Skip/Enable

Users can skip or enable DND for individual events. Tracked via `skippedEventId`, `skippedEventBeginMs`, `skippedEventEndMs` in RuntimeStateStore. Use `SkipUtils` for skip logic and `RuntimeStateStore.setSkippedEvent()`/`clearSkippedEvent()` for atomic updates.

## Product Flavors

| Flavor | Firebase | Updates | Notes |
|--------|----------|---------|-------|
| `play` | Full | Play Store | Google Play distribution |
| `manual` | Full | In-app/GitHub | Direct APK |

Flavor-specific code in `src/play/`, `src/manual/`.

## Code Style

- Kotlin with JVM target 11, 4-space indentation
- Packages: `lowercase`, Classes: `PascalCase`, Functions/vars: `camelCase`, Constants: `UPPER_SNAKE_CASE`
- Domain layer must remain pure Kotlin (no Android imports)
- All DND/system operations wrapped in try-catch (Samsung compatibility)

## Testing

Domain layer is pure Kotlin and easily unit tested. Run `./gradlew test` before marking tasks complete.

Key tests:
- `AutomationEngineTest.kt` - Decision logic including skip behavior
- `MeetingWindowResolverTest.kt` - Meeting merging logic
- `SkipUtilsTest.kt` - Per-event skip utilities

## SDK Requirements

- minSdk: 26 (Android 8.0)
- targetSdk/compileSdk: 36

## Detailed Documentation

See `docs/` folder:
- `architecture.md` - Full system design
- `getting-started.md` - Development setup
- `configuration.md` - Build toggles and signing
- `testing.md` - Test strategy
