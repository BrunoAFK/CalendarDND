# Developer Documentation

Technical documentation for Calendar DND.

## Table of Contents

1. [Getting Started](#getting-started)
2. [Architecture](#architecture)
3. [Project Structure](#project-structure)
4. [Core Components](#core-components)
5. [Background Execution](#background-execution)
6. [Data Flow](#data-flow)
7. [UI Components](#ui-components)
8. [Testing](#testing)
9. [Build Configuration](#build-configuration)
11. [Contributing](#contributing)

---

## Getting Started

### Prerequisites

- Android Studio (latest stable)
- JDK 11+
- Android SDK 26+ (minSdk)

### Setup

```bash
git clone https://github.com/BrunoAFK/CalendarDND.git
cd CalendarDND
```

Open in Android Studio and sync Gradle.

### Build Commands

```bash
./gradlew assembleDebug                # Debug build
./gradlew assemblePlayRelease          # Play Store release
./gradlew assembleAltstoreRelease      # AltStore release
./gradlew assembleManualRelease        # Manual distribution release
./gradlew test                         # Unit tests
./gradlew connectedAndroidTest         # Instrumented tests
./gradlew lint                         # Static analysis
```

### Build Toggles

Pass via `-P` or add to `gradle.properties`:

```properties
crashlyticsEnabled=false     # Disable Firebase Crashlytics
analyticsEnabled=false       # Disable Firebase Analytics
debugToolsEnabled=true       # Enable debug tools in release
```

---

## Architecture

Calendar DND uses clean architecture with four layers:

```
┌─────────────────────────────────────┐
│         UI Layer (Compose)          │
│  Screens, Components, Navigation    │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│      Domain Layer (Pure Kotlin)     │
│  AutomationEngine, Resolvers        │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│         Data Layer                  │
│  Repositories, DataStore, DND       │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│        System Layer                 │
│  Alarms, Workers, Receivers         │
└─────────────────────────────────────┘
```

### Design Principles

1. **Pure Engine Logic** - `AutomationEngine` is pure Kotlin, unit testable without Android
2. **Single Source of Truth** - All state lives in DataStore
3. **Ownership Tracking** - App tracks whether it owns current DND state
4. **User Override Respect** - Manual DND changes stop app interference
5. **Defensive Scheduling** - Multiple strategies ensure reliability

---

## Project Structure

```
com.brunoafk.calendardnd/
├── App.kt                              # Application class

├── ui/                                 # UI Layer
│   ├── MainActivity.kt
│   ├── navigation/
│   │   └── AppNavigation.kt            # Navigation with transitions
│   ├── screens/
│   │   ├── StatusScreen.kt             # Main dashboard
│   │   ├── SettingsScreen.kt           # User settings
│   │   ├── EventKeywordFilterScreen.kt # Keyword filter settings (experimental)
│   │   ├── OnboardingScreen.kt         # Permission setup
│   │   ├── AboutScreen.kt              # App info
│   │   ├── DebugToolsScreen.kt         # Debug utilities
│   │   ├── DebugLogScreen.kt           # Log viewer
│   │   ├── DebugLogSettingsScreen.kt   # Log level config
│   │   └── ...                         # Other screens
│   ├── components/
│   │   ├── InfoBanner.kt               # Dismissable info banner
│   │   ├── DndModeBanner.kt            # DND mode info banner
│   │   ├── SettingsSection.kt          # Settings UI components
│   │   ├── EventOverviewCard.kt        # Event display card
│   │   └── ...                         # Other components
│   ├── tiles/
│   │   └── AutomationTileService.kt    # Quick Settings tile
│   └── theme/
│       └── Theme.kt                    # Material 3 theming

├── domain/                             # Domain Layer
│   ├── engine/
│   │   ├── AutomationEngine.kt         # Core decision logic
│   │   ├── EngineInput.kt              # Input data class
│   │   ├── EngineOutput.kt             # Output data class
│   │   └── TimeFormatter.kt            # Time formatting interface
│   ├── planning/
│   │   ├── MeetingWindowResolver.kt    # Merges overlapping events
│   │   └── SchedulePlanner.kt          # Plans next boundary
│   └── model/
│       ├── EventInstance.kt            # Calendar event model
│       ├── MeetingWindow.kt            # Merged window model
│       ├── DndMode.kt                  # DND mode enum
│       └── Trigger.kt                  # Execution trigger enum

├── data/                               # Data Layer
│   ├── calendar/
│   │   ├── CalendarRepository.kt       # Calendar data access
│   │   ├── CalendarQueries.kt          # ContentProvider queries
│   │   ├── CalendarObserver.kt         # Calendar change detection
│   │   └── CalendarInfo.kt             # Calendar metadata
│   ├── dnd/
│   │   └── DndController.kt            # System DND control
│   └── prefs/
│       ├── SettingsStore.kt            # User preferences
│       ├── RuntimeStateStore.kt        # Runtime state
│       ├── DebugLogStore.kt            # Debug log storage
│       └── DebugLogLevel.kt            # Log level enum

├── system/                             # System Layer
│   ├── alarms/
│   │   ├── AlarmScheduler.kt           # Exact alarm scheduling
│   │   ├── AlarmReceiver.kt            # Alarm broadcast receiver
│   │   ├── AlarmActions.kt             # Action constants
│   │   └── EngineRunner.kt             # Centralized execution
│   ├── workers/
│   │   ├── SanityWorker.kt             # Periodic safety check
│   │   ├── NearTermGuardWorker.kt      # Fallback scheduling
│   │   └── Workers.kt                  # WorkManager helpers
│   ├── receivers/
│   │   ├── BootReceiver.kt             # Device boot handler
│   │   ├── TimeChangeReceiver.kt       # Time/timezone handlers
│   │   ├── EnableDndNowReceiver.kt     # Notification action
│   │   └── ExtendDndReceiver.kt        # Extend DND action
│   ├── notifications/
│   │   ├── DndNotificationHelper.kt    # Pre-DND notifications
│   │   └── UpdateNotificationHelper.kt # Update notifications
│   └── update/
│       └── ManualUpdateManager.kt      # Built-in update checker

└── util/                               # Utilities
    ├── AnalyticsTracker.kt             # Firebase analytics
    ├── DebugLogger.kt                  # Debug logging
    ├── LocaleUtils.kt                  # Language utilities
    ├── PermissionUtils.kt              # Permission checking
    ├── TimeUtils.kt                    # Time utilities
    └── ...                             # Other utilities
```

---

## Core Components

### AutomationEngine

**Location**: `domain/engine/AutomationEngine.kt`

Pure Kotlin decision logic. Takes `EngineInput`, returns `EngineOutput`.

```kotlin
suspend fun run(input: EngineInput): EngineOutput
```

**Decision Rules**:

| Condition | Action | Notes |
|-----------|--------|-------|
| Automation OFF | Disable DND if owned | Cleanup state |
| Missing permissions | No change | Show notification |
| Active meeting | Enable DND | Set ownership |
| User suppressed | No change | Respect override |
| No meeting | Disable DND if owned | Schedule next |

### MeetingWindowResolver

**Location**: `domain/planning/MeetingWindowResolver.kt`

Merges overlapping/touching events into single windows to prevent DND flickering.

```
Meeting A: 10:00-10:30
Meeting B: 10:30-11:00  (touches)
Meeting C: 10:45-11:15  (overlaps)

Result: Single window 10:00-11:15
```

### EngineRunner

**Location**: `system/alarms/EngineRunner.kt`

Centralized entry point for all background execution:

1. Gathers inputs (settings, state, permissions)
2. Runs engine
3. Applies DND changes
4. Updates state
5. Schedules next boundary
6. Logs results

**Called by**: AlarmReceiver, Workers, BroadcastReceivers, UI

### DndController

**Location**: `data/dnd/DndController.kt`

Samsung-safe DND control with error handling:

```kotlin
fun hasPolicyAccess(): Boolean
fun enableDnd(mode: DndMode): Boolean
fun disableDnd(): Boolean
```

Always wraps operations in try-catch for Samsung compatibility.

---

## Background Execution

### Scheduling Hierarchy

**Priority 1: Exact Alarms**
- Most reliable
- Uses `setExactAndAllowWhileIdle()`
- Requires `SCHEDULE_EXACT_ALARM` permission

**Priority 2: Near-Term Guards**
- Fallback when exact alarms unavailable
- Two WorkManager jobs before/after boundary
- For boundaries within 60 minutes

**Priority 3: Periodic Worker**
- Runs every 15 minutes
- Catches missed boundaries
- Always active when automation enabled

### Wake Scenarios

| Trigger | Method | Timing |
|---------|--------|--------|
| Meeting boundary | Exact alarm | Precise |
| No exact alarms | Near-term guards | ±2 min |
| Calendar changed | ContentObserver | +10s debounce |
| Phone rebooted | BootReceiver | Immediate |
| Time changed | TimeChangeReceiver | Immediate |
| Fallback | SanityWorker | Every 15 min |

---

## Data Flow

### Engine Execution

```
Trigger (Alarm/Worker/UI)
         ↓
  EngineRunner.runEngine()
         ↓
  Gather EngineInput
    - Settings (DataStore)
    - Runtime state
    - Permissions
    - Current DND
         ↓
  AutomationEngine.run(input)
         ↓
  EngineOutput (decision + plan)
         ↓
  Apply Changes
    - Enable/disable DND
    - Update ownership
    - Schedule alarms
    - Enqueue workers
         ↓
  Log to DebugLogStore
```

### State Management

**SettingsStore** (persisted):
- User preferences
- Modified by Settings UI
- Experimental keyword filter: `requireTitleKeyword`, `titleKeyword`, `titleKeywordMatchMode`

**RuntimeStateStore** (ephemeral):
- `dndSetByApp` - Ownership flag
- `activeWindowEndMs` - Current meeting end
- `userSuppressedUntilMs` - Override expiry
- Modified only by engine

---

## UI Components

### Banners

**InfoBanner** / **DndModeBanner** - Dismissable banners with:
- Swipe to dismiss (animate off screen)
- Fade out on button tap
- Spring animation snap-back
- Modern card design with icon

### Settings Components

**SettingsSection** - Card container with title
**SettingsNavigationRow** - Clickable row with chevron
**SettingsSwitchRow** - Row with toggle switch
**SettingsDivider** - Section divider

---

## Testing

### Unit Tests

`AutomationEngine` and `MeetingWindowResolver` are pure Kotlin:

```kotlin
@Test
fun `merges touching events`() {
    val events = listOf(
        EventInstance(begin = 1000, end = 2000),
        EventInstance(begin = 2000, end = 3000)
    )
    val window = MeetingWindowResolver.findActiveWindow(events, 1500)
    assertEquals(3000, window?.end)
}
```

### Run Tests

```bash
./gradlew test                                    # All tests
./gradlew test --tests "*.AutomationEngineTest"   # Specific test
```

---

## Build Configuration

### Version Info

```kotlin
versionCode = 11900
versionName = "1.19"
minSdk = 26
targetSdk = 36
```

### Build Flavors

Three distribution variants:

| Flavor | Description |
|--------|-------------|
| `play` | Google Play Store (Firebase enabled) |
| `fdroid` | F-Droid (no Firebase/FCM, no analytics UI) |
| `manual` | Direct download + built-in updater (Firebase enabled) |

Tip: For local Android Studio runs, use `playDebug` to keep Firebase enabled.

### Store Build Scripts

Helper scripts to build release APKs:

```bash
./scripts/build-play.sh
./scripts/build-fdroid.sh
./scripts/build-manual.sh
```

For manual builds, you can pass the signing cert pin:

```bash
MANUAL_SIGNER_SHA256=... ./scripts/build-manual.sh
```

### Supported Languages

- English (en)
- German (de)
- Croatian (hr)
- Italian (it)
- Korean (ko)
- Chinese (zh)

---

## FCM Broadcast Messages (Topic)

Calendar DND subscribes devices to the `updates` topic at startup for Firebase-enabled
flavors (`play`, `manual`). Use the local script to send broadcast messages with an
optional deep link.

### Prerequisites

- Firebase service account JSON (example: `firebase-admin.json`)
- `jq` and `openssl` installed

### .env Setup (repo root)

```bash
FCM_PROJECT_ID=calendar-dnd
GOOGLE_APPLICATION_CREDENTIALS=./firebase-admin.json
FCM_TOPIC=updates
```

### Send a Message

```bash
./scripts/message.sh -t="Update available" -m="Check out v1.8" -a="calendardnd://updates"
```

### Deep Links

- `calendardnd://updates` opens the in-app Updates screen.

### Debugging

```bash
DEBUG=1 ./scripts/message.sh -t="Update available" -m="Check out v1.8"
```

This prints the JSON file path, size, and a small preview to help validate the
service account file.

---

## Samsung Considerations

Samsung-specific notes are no longer applicable since the Samsung flavor was removed.
- Always check permission before operations
- Wrap all DND calls in try-catch
- Handle failures gracefully

### Exact Alarms

Android 12+ requires explicit permission:
- Check `canScheduleExactAlarms()` before scheduling
- Fallback to near-term guards when unavailable

---

## Contributing

### Code Style

- Follow Kotlin official style guide
- Max line length: 120 characters
- Use Android Studio formatting

### Pull Request Process

1. Create feature branch
2. Add tests for new functionality
3. Update documentation
4. Test on real device (Samsung preferred)
5. Submit PR with description

### Adding New Features

**New Screen**:
1. Create composable in `ui/screens/`
2. Add route to `AppNavigation.kt`
3. Add navigation call

**New Setting**:
1. Add to `SettingsStore`
2. Add UI in `SettingsScreen`
3. Update `EngineInput` if needed

**New Language**:
1. Create `res/values-{lang}/strings.xml`
2. Add to `LanguageScreen` list
3. Test all screens in new language

---

## License

MIT License - see [LICENSE](../LICENSE)

---

## Support

- **Issues**: [GitHub Issues](https://github.com/BrunoAFK/CalendarDND/issues)
- **Debug Logs**: Available in-app (Settings > Debug Tools)
