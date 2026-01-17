# Architecture

Calendar DND uses clean architecture with four distinct layers.

## Layer Overview

```
┌─────────────────────────────────────┐
│         UI Layer (Compose)          │
│  Screens, Components, Navigation    │
└──────────────────┬──────────────────┘
                   │
┌──────────────────▼──────────────────┐
│      Domain Layer (Pure Kotlin)     │
│  AutomationEngine, Resolvers        │
└──────────────────┬──────────────────┘
                   │
┌──────────────────▼──────────────────┐
│    Data Layer (Repositories)        │
│  Calendar, DND, Preferences         │
└──────────────────┬──────────────────┘
                   │
┌──────────────────▼──────────────────┐
│   System Layer (Android Platform)   │
│  Alarms, Workers, Receivers         │
└─────────────────────────────────────┘
```

## Core Components

### AutomationEngine

**Location**: `domain/engine/AutomationEngine.kt`

The decision-making brain of the app. Pure Kotlin with no Android dependencies.

**Input/Output**:
```kotlin
fun run(input: EngineInput): EngineOutput
```

**Decision Flow**:
1. If automation disabled → disable DND (if app owns it)
2. If permissions missing → send notification, no DND changes
3. If in active DND window and not suppressed → enable DND
4. If in active DND window but suppressed → respect user override
5. If no active window → disable DND (if app owns it)

**Key Features**:
- Deterministic (same input = same output)
- Unit testable without Android
- Logs all decisions for debugging

### MeetingWindowResolver

**Location**: `domain/planning/MeetingWindowResolver.kt`

Merges overlapping or back-to-back meetings into single DND windows.

**Example**:
```
Meeting A: 10:00-10:30
Meeting B: 10:30-11:00  (touches A)
Meeting C: 10:45-11:15  (overlaps B)
───────────────────────────────────
Result:    10:00-11:15  (single window)
```

This prevents DND flickering between consecutive meetings.

### EngineRunner

**Location**: `system/alarms/EngineRunner.kt`

Central orchestration point. All triggers flow through here.

**Flow**:
1. Gather input (settings, state, permissions, system DND)
2. Run `AutomationEngine.run(input)`
3. Apply DND changes via `DndController`
4. Update runtime state
5. Schedule next alarm/worker
6. Schedule pre-DND notification (if enabled)
7. Log execution

**Triggers**:
- `AlarmReceiver` - Exact alarms at meeting boundaries
- `SanityWorker` - Periodic 15-minute checks
- `NearTermGuardWorker` - Near-boundary guards
- `BootReceiver` - Device restart
- `TimeChangeReceiver` - Time/timezone changes
- `CalendarObserver` - Calendar changes (debounced)
- UI - Manual "Run Now" button

### DndController

**Location**: `data/dnd/DndController.kt`

Interface to Android's DND system.

```kotlin
fun isDndActive(): Boolean
fun enableDnd(mode: DndMode)
fun disableDnd()
fun hasPermission(): Boolean
```

All operations wrapped in try-catch for Samsung compatibility.

## Data Management

### SettingsStore

**Location**: `data/prefs/SettingsStore.kt`

Persistent user preferences via DataStore.

| Setting | Type | Purpose |
|---------|------|---------|
| `automationEnabled` | Boolean | Global on/off |
| `selectedCalendarIds` | Set<Long> | Calendars to monitor |
| `busyOnly` | Boolean | Only BUSY events |
| `ignoreAllDay` | Boolean | Skip all-day events |
| `minEventMinutes` | Int | Minimum duration |
| `dndMode` | Enum | PRIORITY or SILENCE |
| `dndStartOffsetMinutes` | Int | Start offset |
| `preDndNotificationEnabled` | Boolean | 5-min warnings |

### RuntimeStateStore

**Location**: `data/prefs/RuntimeStateStore.kt`

Transient automation state via DataStore.

| State | Type | Purpose |
|-------|------|---------|
| `dndSetByApp` | Boolean | App owns DND? |
| `activeWindowEndMs` | Long | Current meeting end |
| `userSuppressedUntilMs` | Long | User override until |
| `lastEngineRunMs` | Long | Last execution time |

### CalendarRepository

**Location**: `data/calendar/CalendarRepository.kt`

Calendar data access via ContentResolver.

```kotlin
fun getActiveInstances(): List<CalendarInstance>
fun getNextInstance(): CalendarInstance?
fun getInstancesInRange(start: Long, end: Long): List<CalendarInstance>
```

Applies all filters (calendar selection, busy-only, all-day, duration, keywords).

## Background Scheduling

Three-tier strategy for reliability:

### Tier 1: Exact Alarms (Primary)

```kotlin
AlarmManager.setExactAndAllowWhileIdle(
    AlarmManager.RTC_WAKEUP,
    boundaryTimeMs,
    pendingIntent
)
```

Most reliable. Requires `SCHEDULE_EXACT_ALARM` permission on Android 12+.

### Tier 2: WorkManager Guards (Fallback)

When exact alarms unavailable, schedules WorkManager jobs:
- 2 minutes before boundary
- 2 minutes after boundary

For boundaries within 60 minutes.

### Tier 3: SanityWorker (Safety Net)

Periodic 15-minute checks via WorkManager. Always active when automation enabled.

## User Override Detection

The app respects manual DND changes:

1. App enables DND, sets `dndSetByApp = true`
2. User manually disables DND during meeting
3. Next engine run detects: `dndSetByApp == true` but system DND is off
4. App sets `userSuppressedUntilMs = meetingEnd`
5. App stops interfering until meeting ends

## Navigation

**Location**: `ui/navigation/AppNavigation.kt`

Single-activity architecture with Compose Navigation.

**Routes**:
```
STARTUP → (onboarding flow) → STATUS
STATUS → SETTINGS, DEBUG_LOGS, UPDATES
SETTINGS → sub-screens
```

Features:
- Animated transitions (220ms slide)
- Route locking (prevents double-navigation)
- Interaction gating during transitions

## Testing Strategy

The architecture enables testing at each layer:

| Layer | Testing Approach |
|-------|-----------------|
| Domain | Pure unit tests (no Android) |
| Data | Unit tests with mocked repos |
| System | Instrumented tests |
| UI | Compose UI tests |

See [Testing](testing.md) for commands.

## Key Design Decisions

### Pure Domain Layer

`AutomationEngine` and `MeetingWindowResolver` have zero Android dependencies. Benefits:
- Fast unit tests
- Easy to reason about
- Portable logic

### Immutable Data Classes

All input/output objects are immutable:
```kotlin
data class EngineInput(...)
data class EngineOutput(...)
data class Decision(...)
```

### Repository Pattern

`ICalendarRepository` interface allows mocking for tests.

### Flow-Based State

DataStore with Kotlin Flow for reactive updates:
```kotlin
val automationEnabled: Flow<Boolean>
```

### Defensive Error Handling

All system calls wrapped in try-catch (Samsung throws even with permissions).
