# CalendarDND - Technical Code Analysis

This document provides an in-depth analysis of the CalendarDND codebase, derived from reading the actual source code implementation.

---

## 1. Architecture Overview

CalendarDND uses a **layered, trigger-driven architecture** with a centralized decision engine:

```
UI Layer (Jetpack Compose)
        ↓
Intent/Event Layer (Alarms, Workers, System Events)
        ↓
Engine Runner (EngineRunner.kt) - Orchestration Point
        ↓
Automation Engine (Core Decision Logic)
        ↓
Data Access Layer
├── Calendar Data (CalendarRepository)
├── DND Control (DndController)
├── Preferences (SettingsStore)
└── Runtime State (RuntimeStateStore)
```

**Key Design Principle**: All DND decisions flow through a single `AutomationEngine.run()` function regardless of trigger source (alarm, worker, app startup, etc.). This ensures consistent decision-making.

---

## 2. Complete Flow from App Startup to DND Automation

### 2.1 App Initialization (App.kt → MainActivity.kt)

1. **App.kt** - Application singleton initialization:
   - Reads user's Crashlytics/Analytics opt-in from SettingsStore
   - Configures Firebase based on BuildConfig flags and user preferences
   - Single Application-level setup

2. **MainActivity.kt** - First screen display:
   - Creates SettingsStore and reads `preferredLanguageTag`
   - Sets up localized context with appropriate language
   - Provides LocalActivityResultRegistryOwner for permission handling
   - Renders theme and launches `AppNavigation()`

### 2.2 Navigation Flow (AppNavigation.kt)

**Start Route**: `AppRoutes.STARTUP`

**Onboarding Path** (first-time users):
```
STARTUP → LANGUAGE_ONBOARDING → INTRO → ONBOARDING → CALENDAR_SCOPE
  → (if specific) CALENDAR_PICKER_ONBOARDING → PRIVACY → STATUS
```

**Standard Path** (returning users):
```
STARTUP → STATUS (main app screen)
```

**Key Navigation Features**:
- `DestinationWrapper` wraps all screens to manage interactive state
- `lockedRoutes` set prevents navigation during screen transitions
- `navInteractionGate` modifier gates user interactions during animation
- Debug overlay available when `debugOverlayEnabled` and build flag set

**Available Screens**:
- STATUS: Main dashboard showing automation state
- SETTINGS: User preferences and calendar configuration
- DND_MODE: Select Priority/Total Silence mode
- CALENDAR_PICKER: Select which calendars to monitor
- DEBUG_LOGS: View engine execution logs
- HELP, LANGUAGE_SETTINGS, UPDATE screens, etc.

---

## 3. Detailed Automation Engine Logic

### 3.1 Engine Entry Point (AutomationEngine.run())

The engine receives an `EngineInput` object containing all context and returns an `EngineOutput` with decisions and scheduling.

**Input Parameters**:
```kotlin
data class EngineInput(
    val trigger: Trigger,          // What triggered this run (ALARM, WORKER_SANITY, etc.)
    val now: Long,                 // Current time in ms

    // Settings (from SettingsStore - user preferences)
    val automationEnabled: Boolean,
    val selectedCalendarIds: Set<String>,
    val busyOnly: Boolean,
    val ignoreAllDay: Boolean,
    val minEventMinutes: Int,
    val dndMode: DndMode,
    val dndStartOffsetMinutes: Int,
    val preDndNotificationEnabled: Boolean,

    // Runtime State (from RuntimeStateStore - tracking current state)
    val dndSetByApp: Boolean,           // Does app own current DND?
    val activeWindowEndMs: Long,        // When does current meeting end?
    val userSuppressedUntilMs: Long,    // User manually overrode - suppress until
    val manualDndUntilMs: Long,         // User set manual DND - active until

    // System State (from DndController and AlarmScheduler)
    val hasCalendarPermission: Boolean,
    val hasPolicyAccess: Boolean,       // Can app change DND?
    val hasExactAlarms: Boolean,        // Can app schedule exact alarms?
    val systemDndIsOn: Boolean,
    val currentSystemFilter: Int        // Current DND mode value
)
```

### 3.2 Engine Decision Rules (Five Sequential Rules)

**RULE 1: Automation OFF**
```
IF !automationEnabled THEN
    - shouldDisableDnd = true (only if we own it)
    - Clear dndSetByApp flag
    - Return (no DND changes)
```

**RULE 2: Missing Permissions**
```
IF !hasCalendarPermission OR !hasPolicyAccess THEN
    - Send SETUP_REQUIRED notification
    - Don't change DND
    - Skip to scheduling
```

**RULE 3: Active DND Window + NOT Suppressed**
```
IF dndWindow.isActive AND !isSuppressed THEN
    - shouldEnableDnd = true (if not already on)
    - setDndSetByApp = true
    - Track active window end time
    - Clear manual DND if expired
    - Send DEGRADED_MODE notification if no exact alarms
```

**RULE 4: Active DND Window but IS Suppressed**
```
IF dndWindow.isActive AND isSuppressed THEN
    - Don't enable/disable DND
    - If user override detected: set userSuppressedUntil to window end
    - Track active window end for reference
    - Clear manual DND if expired
```

**RULE 5: No Active DND Window**
```
IF !dndWindow.isActive THEN
    - shouldDisableDnd = true (only if we own it)
    - setDndSetByApp = false
    - Check for meeting overrun (meeting ended < 2 min ago, gap to next > 5 min)
    - Send MEETING_OVERRUN notification if detected
    - Clear manual DND if expired
```

### 3.3 DND Window Resolution (resolveDndWindow())

Determines what time range DND should be active:

```kotlin
fun resolveDndWindow(
    now: Long,
    activeWindow: MeetingWindow?,    // Currently active meetings
    nextInstance: EventInstance?,    // Next upcoming meeting
    offsetMinutes: Int,              // User-set offset before meeting starts
    manualUntilMs: Long              // User manually set DND until this time
): DndWindow

// Priority 1: Manual DND (user explicitly set)
if (manualUntilMs > now) {
    return DndWindow(startMs=now, endMs=manualUntilMs, isActive=true, ...)
}

// Priority 2: Use active or next meeting
val meetingStart = activeWindow?.begin ?: nextInstance?.begin
val meetingEnd = activeWindow?.end ?: nextInstance?.end

if (meeting exists) {
    dndStartMs = meetingStart + (offset * 60_000)

    // Validate: DND start shouldn't be after meeting end
    if (dndStartMs >= meetingEnd) return NO_WINDOW

    isActive = (now in dndStartMs until meetingEnd)
    nextStart = if (!isActive && dndStartMs > now) dndStartMs else null
}
```

### 3.4 User Override Detection (detectUserOverride())

```kotlin
fun detectUserOverride(input: EngineInput, dndWindowActive: Boolean): Boolean {
    // True if:
    // 1. App thinks it owns DND (dndSetByApp = true)
    // 2. DND window should be active
    // 3. But system DND is actually OFF
    return input.dndSetByApp && dndWindowActive && !input.systemDndIsOn
}
```

**Scenario**: User manually turned off DND during a meeting. App detects this and suppresses automation until the meeting ends, respecting user intent.

### 3.5 Meeting Overrun Detection (detectMeetingOverrun())

```kotlin
fun detectMeetingOverrun(
    now: Long,
    activeWindowEnd: Long,
    nextInstance: EventInstance?
): Boolean {
    val justEnded = (now > activeWindowEnd) &&
                    ((now - activeWindowEnd) < 2 minutes)

    val gapToNext = nextInstance?.begin?.let { it - now } ?: INFINITY
    val hasLargeGap = gapToNext > 5 minutes

    return justEnded && hasLargeGap && activeWindowEnd > 0
}
```

**Purpose**: Detect when a meeting just ended and there's a long gap before the next one. This triggers the "extend DND?" notification.

### 3.6 Suppression Logic

**Three Suppression Types**:

1. **userSuppressedUntilMs**: Set when user override detected (Rule 4)
   - Blocks automation from re-enabling DND until window ends

2. **manualDndUntilMs**: Set when user manually enables DND from UI
   - Takes priority over calendar-based decisions
   - Cleared automatically when expiration time reached

3. **Active Window Tracking** (activeWindowEndMs):
   - Stored to detect overrun conditions
   - Used to calculate next scheduling boundary

---

## 4. Calendar Event Querying and Filtering

### 4.1 Calendar Data Access (CalendarRepository.kt)

**Key Query Methods**:

1. **getActiveInstances()** - Events happening RIGHT NOW
   ```kotlin
   // Query 6-hour window around now (3 hours past, 3 hours future)
   // This captures all ongoing events
   val windowStart = now - 6 hours
   val windowEnd = now + 6 hours

   // Return: instances where begin <= now < end
   return allInstances.filter { it.begin <= now && now < it.end }
   ```

2. **getNextInstance()** - Next upcoming event
   ```kotlin
   // Query 7 days ahead
   val allInstances = queryInstances(now, now + 7 days)

   // Return: first instance where begin > now
   return allInstances
       .filter { it.begin > now }
       .minByOrNull { it.begin }  // Earliest next event
   ```

3. **getInstancesInRange()** - All events in time window
   ```kotlin
   // Used for meeting window merging
   return allInstances.filter { isRelevantInstance(...) }
   ```

### 4.2 Filtering Logic (isRelevantInstance())

All events are filtered through four criteria:

```kotlin
fun isRelevantInstance(
    instance: EventInstance,
    selectedCalendarIds: Set<String>,    // User selected calendars
    busyOnly: Boolean,                   // Filter to BUSY events only?
    ignoreAllDay: Boolean,               // Skip all-day events?
    minEventMinutes: Int                 // Minimum duration (e.g., 10 min)
): Boolean {
    // Filter 1: Calendar Selection
    if (selectedCalendarIds.isNotEmpty() &&
        !selectedCalendarIds.contains(instance.calendarId)) {
        return false
    }

    // Filter 2: All-day Filter
    if (ignoreAllDay && instance.allDay) {
        return false
    }

    // Filter 3: Busy-only Filter
    if (busyOnly && instance.availability != AVAILABILITY_BUSY) {
        return false
    }

    // Filter 4: Minimum Duration
    if (instance.durationMinutes < minEventMinutes) {
        return false
    }

    return true
}
```

**Default Settings**:
- busyOnly = true (ignore "Free" events)
- ignoreAllDay = true (skip all-day events)
- minEventMinutes = 10
- selectedCalendarIds = empty set (all calendars)

### 4.3 Android Calendar Content Provider Query (CalendarQueries.kt)

Raw queries to `CalendarContract.Instances`:

```kotlin
fun queryInstances(context: Context, beginMs: Long, endMs: Long): List<EventInstance> {
    val uri = CalendarContract.Instances.CONTENT_URI
        .buildUpon()
        .appendId(beginMs)
        .appendId(endMs)
        .build()

    val projection = arrayOf(
        _ID, CALENDAR_ID, TITLE, BEGIN, END, ALL_DAY, AVAILABILITY
    )

    // Query with BEGIN ASC sorting for efficiency
    val cursor = contentResolver.query(uri, projection, null, null, "BEGIN ASC")

    // Build EventInstance objects from cursor
    return cursor.map { EventInstance(...) }
}
```

**Error Handling**: Silent failure with `e.printStackTrace()` - returns empty list if permission denied or query fails.

### 4.4 Meeting Window Resolution (MeetingWindowResolver.kt)

**Purpose**: Merge overlapping/touching events into consolidated windows

**Algorithm**:

```kotlin
fun findActiveWindow(instances: List<EventInstance>, now: Long): MeetingWindow? {
    // 1. Filter to currently active events (begin <= now < end)
    val activeInstances = instances.filter { it.begin <= now && now < it.end }
    if (activeInstances.isEmpty()) return null

    // 2. Start with min/max times
    var mergedBegin = activeInstances.minOf { it.begin }
    var mergedEnd = activeInstances.maxOf { it.end }

    // 3. Iteratively expand window if other events overlap/touch
    var changed = true
    while (changed) {
        changed = false
        for (instance in instances) {
            // Check overlap: instance.begin <= mergedEnd && instance.end >= mergedBegin
            if (instance.begin <= mergedEnd && instance.end >= mergedBegin) {
                val newBegin = minOf(mergedBegin, instance.begin)
                val newEnd = maxOf(mergedEnd, instance.end)

                if (newBegin != mergedBegin || newEnd != mergedEnd) {
                    mergedBegin = newBegin
                    mergedEnd = newEnd
                    changed = true  // Found expansion, loop again
                }
            }
        }
    }

    // 4. Return merged window with all overlapping events
    return MeetingWindow(
        begin = mergedBegin,
        end = mergedEnd,
        events = instances.filter { it overlaps mergedWindow }
    )
}
```

**Example**: Three meetings: 9-10, 9:30-11, 10:30-11:30
- Start: begin=9, end=10
- Loop 1: 9-10:30 (9:30-11 overlaps)
- Loop 2: 9-11:30 (10:30-11:30 overlaps)
- Result: Single window 9-11:30 with all 3 events

---

## 5. Alarm and Worker Scheduling Logic

### 5.1 Scheduling Planner (SchedulePlanner.kt)

Determines what to schedule after engine runs:

```kotlin
fun planNextSchedule(
    now: Long,
    dndWindowStartMs: Long?,          // Next DND start time
    dndWindowEndMs: Long?,            // When DND should end
    dndWindowActive: Boolean,         // Is DND currently active?
    hasExactAlarms: Boolean           // Can app schedule exact alarms?
): SchedulePlan {

    // Determine next boundary (when something changes)
    val nextBoundary = when {
        dndWindowActive -> dndWindowEndMs         // DND currently on → schedule its END
        dndWindowStartMs != null && dndWindowStartMs > now -> dndWindowStartMs  // Schedule next START
        else -> null  // No upcoming boundary
    }

    if (nextBoundary == null) {
        return SchedulePlan(nextBoundaryMs=null, needsGuards=false, ...)
    }

    // Check if boundary is "near-term" (within 60 minutes)
    val timeUntilBoundary = nextBoundary - now
    val isNearTerm = timeUntilBoundary in 1ms..60minutes

    // If no exact alarms and boundary is soon: use WorkManager guards
    val needsGuards = !hasExactAlarms && isNearTerm

    val guardBefore = if (needsGuards) {
        // Schedule guard 2 min before boundary, but at least 10 sec from now
        maxOf(now + 10_000, nextBoundary - 2.minutes)
    } else null

    val guardAfter = if (needsGuards) {
        // Schedule guard 2 min after boundary
        nextBoundary + 2.minutes
    } else null

    return SchedulePlan(
        nextBoundaryMs = nextBoundary,
        needsNearTermGuards = needsGuards,
        guardBeforeMs = guardBefore,
        guardAfterMs = guardAfter
    )
}
```

**Scheduling Strategy**:
- **With Exact Alarms** (Android 12+ with permission): Schedule precise alarm at boundary
- **Without Exact Alarms**: Use WorkManager guards 2 min before/after boundary to catch state changes
- **Far-away Boundaries** (>60 min): Only periodic SanityWorker runs

### 5.2 Alarm Scheduling (AlarmScheduler.kt)

```kotlin
fun canScheduleExactAlarms(): Boolean {
    return if (Build.VERSION.SDK_INT >= Android 12) {
        alarmManager.canScheduleExactAlarms()  // Check permission
    } else {
        true  // Always available pre-Android 12
    }
}

fun scheduleBoundaryAlarm(boundaryMs: Long): Boolean {
    val intent = Intent(context, AlarmReceiver::class.java)
        .apply { action = ACTION_BOUNDARY }

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE_BOUNDARY,
        intent,
        FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE
    )

    return try {
        alarmManager.setExactAndAllowWhileIdle(
            RTC_WAKEUP,      // Wake device if sleeping
            boundaryMs,      // Exact trigger time
            pendingIntent
        )
        true
    } catch (e: Exception) {
        e.printStackTrace()  // Samsung devices may throw even with permission
        false
    }
}

fun schedulePreDndNotificationAlarm(
    triggerAtMs: Long,
    meetingTitle: String?,
    dndWindowEndMs: Long?
): Boolean {
    // Similar to boundary alarm, but fires ACTION_PRE_DND_NOTIFICATION
    // Triggered 5 minutes before DND start (from EngineRunner)
}
```

### 5.3 Alarm Receiver (AlarmReceiver.kt)

BroadcastReceiver that triggers engine runs:

```kotlin
class AlarmReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_BOUNDARY -> {
                val pendingResult = goAsync()  // Allow async work
                scope.launch {
                    try {
                        EngineRunner.runEngine(context, Trigger.ALARM)
                    } finally {
                        pendingResult.finish()  // Signal completion
                    }
                }
            }
            ACTION_PRE_DND_NOTIFICATION -> {
                val title = intent.getStringExtra(EXTRA_MEETING_TITLE)
                val endMs = intent.getLongExtra(EXTRA_DND_WINDOW_END_MS, 0L)

                val pendingResult = goAsync()
                scope.launch {
                    try {
                        DndNotificationHelper.showPreDndNotification(context, title, endMs)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
```

**Pattern**: Uses `goAsync()` to allow coroutine work to complete before BroadcastReceiver finishes.

### 5.4 WorkManager Guards (SanityWorker, NearTermGuardWorker)

**SanityWorker** - Periodic safety net:
```kotlin
class SanityWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            EngineRunner.runEngine(applicationContext, Trigger.WORKER_SANITY)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

// Scheduled every 15 minutes in Workers.ensureSanityWorker()
fun ensureSanityWorker(context: Context) {
    val workRequest = PeriodicWorkRequestBuilder<SanityWorker>(
        15, TimeUnit.MINUTES
    ).build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "sanity_worker",
        ExistingPeriodicWorkPolicy.KEEP,
        workRequest
    )
}
```

**NearTermGuardWorker** - Near-boundary safety checks:
```kotlin
// One-time workers scheduled 2 min before/after boundary
fun scheduleNearTermGuard(context: Context, targetMs: Long, isBefore: Boolean) {
    val delayMs = maxOf(0, targetMs - System.currentTimeMillis())

    val workRequest = OneTimeWorkRequestBuilder<NearTermGuardWorker>()
        .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
        .addTag("near_term_guard")
        .build()

    WorkManager.getInstance(context).enqueue(workRequest)
}
```

### 5.5 Engine Runner Orchestration (EngineRunner.kt)

Central orchestrator called by all triggers:

```kotlin
suspend fun runEngine(context: Context, trigger: Trigger) {
    val settingsStore = SettingsStore(context)
    val runtimeStateStore = RuntimeStateStore(context)
    val dndController = DndController(context)
    val calendarRepository = CalendarRepository(context)

    // Gather input from all sources
    val input = EngineInput(
        trigger = trigger,
        now = System.currentTimeMillis(),
        automationEnabled = settingsStore.automationEnabled.first(),
        selectedCalendarIds = settingsStore.selectedCalendarIds.first(),
        busyOnly = settingsStore.busyOnly.first(),
        ignoreAllDay = settingsStore.ignoreAllDay.first(),
        minEventMinutes = settingsStore.minEventMinutes.first(),
        dndMode = settingsStore.dndMode.first(),
        dndStartOffsetMinutes = settingsStore.dndStartOffsetMinutes.first(),
        preDndNotificationEnabled = settingsStore.preDndNotificationEnabled.first(),
        dndSetByApp = runtimeStateStore.dndSetByApp.first(),
        activeWindowEndMs = runtimeStateStore.activeWindowEndMs.first(),
        userSuppressedUntilMs = runtimeStateStore.userSuppressedUntilMs.first(),
        manualDndUntilMs = runtimeStateStore.manualDndUntilMs.first(),
        hasCalendarPermission = hasCalendarPermission(context),
        hasPolicyAccess = dndController.hasPolicyAccess(),
        hasExactAlarms = alarmScheduler.canScheduleExactAlarms(),
        systemDndIsOn = dndController.isDndOn(),
        currentSystemFilter = dndController.getCurrentFilter()
    )

    // 1. RUN ENGINE
    val output = engine.run(input)
    val decision = output.decision

    // 2. APPLY DND CHANGES
    if (decision.shouldEnableDnd) {
        dndController.enableDnd(input.dndMode)  // Set to PRIORITY or TOTAL_SILENCE
    } else if (decision.shouldDisableDnd) {
        dndController.disableDnd()  // Set to INTERRUPTION_FILTER_ALL
    }

    // 3. UPDATE RUNTIME STATE
    decision.setDndSetByApp?.let { runtimeStateStore.setDndSetByApp(it) }
    decision.setUserSuppressedUntil?.let { runtimeStateStore.setUserSuppressedUntilMs(it) }
    decision.setActiveWindowEnd?.let { runtimeStateStore.setActiveWindowEndMs(it) }
    decision.setManualDndUntilMs?.let { runtimeStateStore.setManualDndUntilMs(it) }

    // 4. SCHEDULE NEXT BOUNDARIES
    val schedulePlan = output.schedulePlan
    if (schedulePlan?.nextBoundaryMs != null) {
        if (input.hasExactAlarms) {
            alarmScheduler.scheduleBoundaryAlarm(schedulePlan.nextBoundaryMs)
        }
        if (schedulePlan.needsNearTermGuards) {
            schedulePlan.guardBeforeMs?.let { Workers.scheduleNearTermGuard(context, it, true) }
            schedulePlan.guardAfterMs?.let { Workers.scheduleNearTermGuard(context, it, false) }
        }
    } else {
        alarmScheduler.cancelBoundaryAlarm()
    }

    // 5. SCHEDULE PRE-DND NOTIFICATION (5 min before DND)
    if (input.preDndNotificationEnabled && output.nextDndStartMs != null) {
        val notifyAtMs = output.nextDndStartMs - 5.minutes
        if (notifyAtMs > now + 5 seconds) {
            alarmScheduler.schedulePreDndNotificationAlarm(notifyAtMs, output.nextInstance?.title, output.dndWindowEndMs)
        }
    }

    // 6. ENSURE SANITY WORKER
    if (input.automationEnabled) {
        Workers.ensureSanityWorker(context)
    } else {
        Workers.cancelAllWork(context)
    }

    // 7. LOGGING & ANALYTICS
    debugLogStore.appendLog(output.logMessage)
    AnalyticsTracker.logEngineRun(context, trigger, action, exactAlarms)
}
```

---

## 6. State Management

### 6.1 SettingsStore (User Preferences)

**Persistent DataStore** - Survives app restart, manually edited by user:

```kotlin
val automationEnabled: Flow<Boolean>              // Default: true
val selectedCalendarIds: Flow<Set<String>>        // Default: empty (all calendars)
val busyOnly: Flow<Boolean>                       // Default: true
val ignoreAllDay: Flow<Boolean>                   // Default: true
val minEventMinutes: Flow<Int>                    // Default: 10
val dndMode: Flow<DndMode>                        // Default: PRIORITY
val dndStartOffsetMinutes: Flow<Int>              // Default: 0
val preDndNotificationEnabled: Flow<Boolean>      // Default: false
val preferredLanguageTag: Flow<String>            // Default: "" (system language)
val onboardingCompleted: Flow<Boolean>            // Default: false
val requireTitleKeyword: Flow<Boolean>            // Default: false
val titleKeyword: Flow<String>                    // Default: ""
val debugOverlayEnabled: Flow<Boolean>            // Default: false
val totalSilenceConfirmed: Flow<Boolean>          // Default: false
```

**Update Pattern**:
```kotlin
suspend fun setAutomationEnabled(enabled: Boolean) {
    dataStore.edit { prefs ->
        prefs[AUTOMATION_ENABLED] = enabled
    }
}
```

### 6.2 RuntimeStateStore (Automation State)

**Transient DataStore** - Tracks current automation execution state:

```kotlin
val dndSetByApp: Flow<Boolean>              // Does app own current DND?
val activeWindowEndMs: Flow<Long>           // When does current meeting end?
val userSuppressedUntilMs: Flow<Long>       // User override - suppress until when?
val manualDndUntilMs: Flow<Long>            // User manual DND - active until?
val lastPlannedBoundaryMs: Flow<Long>       // For debugging
val lastEngineRunMs: Flow<Long>             // For analytics
val lastKnownDndFilter: Flow<Int>           // For Samsung workarounds
```

**Lifecycle**:
- Set by EngineRunner after each engine run
- Read by EngineRunner at start of next engine run
- Cleared when automation disabled or app uninstalled

### 6.3 State Interaction Pattern

```
User Changes Settings (SettingsStore)
    ↓
Android System broadcasts BOOT/TIME_CHANGE/CALENDAR_CHANGE (or SanityWorker runs)
    ↓
EngineRunner reads SettingsStore + RuntimeStateStore
    ↓
EngineRunner.runEngine() calls engine.run(input)
    ↓
Engine makes decision based on current state
    ↓
EngineRunner updates RuntimeStateStore with new state
    ↓
EngineRunner schedules next alarm/worker
    ↓
Next trigger fires → repeat
```

**Key Invariant**: RuntimeStateStore should always reflect the app's understanding of:
- Who owns the current DND (to avoid fighting user)
- What window we're tracking (to detect overrun)
- What suppressions are active (to respect user intent)

---

## 7. DND Control Implementation

### 7.1 DndController (Direct System Interaction)

```kotlin
class DndController(context: Context) {
    private val notificationManager = getSystemService(NotificationManager::class)

    fun hasPolicyAccess(): Boolean {
        return notificationManager.isNotificationPolicyAccessGranted
    }

    fun getCurrentFilter(): Int {
        return notificationManager.currentInterruptionFilter
    }

    fun isDndOn(): Boolean {
        val filter = getCurrentFilter()
        return filter != INTERRUPTION_FILTER_ALL &&   // Not "Off"
               filter != INTERRUPTION_FILTER_UNKNOWN
    }

    fun enableDnd(mode: DndMode): Boolean {
        if (!hasPolicyAccess()) return false
        try {
            notificationManager.setInterruptionFilter(mode.filterValue)
            return true
        } catch (e: Exception) {
            // Samsung devices may throw even with permission granted
            e.printStackTrace()
            return false
        }
    }

    fun disableDnd(): Boolean {
        if (!hasPolicyAccess()) return false
        try {
            notificationManager.setInterruptionFilter(INTERRUPTION_FILTER_ALL)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
```

### 7.2 DND Modes (DndMode.kt)

```kotlin
enum class DndMode(val filterValue: Int) {
    PRIORITY(INTERRUPTION_FILTER_PRIORITY),      // Allow contacts only
    TOTAL_SILENCE(INTERRUPTION_FILTER_NONE)      // No notifications at all
}
```

### 7.3 Samsung Quirks Handling

- **Issue**: Samsung devices sometimes throw `SecurityException` even with Notification Policy Access
- **Solution**: Wrapped in try-catch, returns false if exception caught
- **Fallback**: Periodically SanityWorker runs to detect and recover

### 7.4 Permission Model

**Required Permissions**:
1. `READ_CALENDAR` - To query calendar events
   - Checked at runtime before engine runs
   - If missing: engine returns SETUP_REQUIRED notification

2. **Notification Policy Access** (System Setting, not manifest)
   - Not a normal manifest permission
   - Checked with `isNotificationPolicyAccessGranted`
   - If missing: engine returns SETUP_REQUIRED notification
   - User must manually grant in settings

---

## 8. Edge Cases and Special Scenarios

### 8.1 User Override Detection and Handling

**Scenario**: DND is scheduled to turn on at 2:00 PM. User manually turns it off at 1:50 PM before meeting starts.

**Logic Flow**:
1. App thinks: "DND should be active, and we own it" (dndSetByApp=true)
2. Engine checks: "Is DND window active?" → Yes
3. Engine checks: "Is system DND on?" → No
4. Engine calls `detectUserOverride()` → Returns TRUE
5. RULE 4 applies: "Active window but suppressed"
6. Engine sets `userSuppressedUntilMs = windowEnd`
7. DND stays OFF until meeting ends
8. Engine logs override in debug logs

**Next Run**: When engine runs again, checks `isSuppressed = now < userSuppressedUntilMs` → True, continues respecting user.

### 8.2 Meeting Overrun Notification

**Scenario**: 2-hour meeting ends at 4:00 PM. No meetings until 5:30 PM.

**Detection**:
```
now = 4:01 PM (1 min after meeting ended)
activeWindowEnd = 4:00 PM
nextInstance.begin = 5:30 PM

justEnded = (4:01 - 4:00) < 2 min → TRUE
hasLargeGap = (5:30 - 4:01) > 5 min → TRUE
detectMeetingOverrun() → TRUE
```

**Action**: Send MEETING_OVERRUN notification offering to extend DND.

### 8.3 Exact Alarm Unavailability (Android 12+ Degraded Mode)

**Scenario**: User denies "Schedule exact alarms" permission or it's revoked.

**App Response**:
1. `canScheduleExactAlarms()` returns FALSE
2. SchedulePlanner detects: boundary within 60 min? → Schedule WorkManager guards
3. Guards run 2 min before/after boundary to catch state changes
4. Engine sends DEGRADED_MODE notification every run
5. ≥ 15-min SanityWorker gap → Risk of missing boundary

**User Experience**: App works but less precise, notifications warn of degraded mode.

### 8.4 Manual DND (User-Set via UI)

**Scenario**: User manually enables DND from a UI button for 1 hour.

**Setting**: `manualDndUntilMs = now + 1 hour`

**Resolution**:
- `resolveDndWindow()` checks: `if (manualUntilMs > now) return DndWindow(now, manualUntilMs, isActive=true)`
- Overrides calendar-based decisions
- Engine clears when `manualClearValue = if (shouldClearManual) 0L else null`

### 8.5 Missing Permissions at Runtime

**Scenario**: Calendar permission revoked or app installed without granting.

**Engine Response** (RULE 2):
```
if (!input.hasCalendarPermission || !input.hasPolicyAccess) {
    notificationNeeded = SETUP_REQUIRED
    return (no DND changes, no scheduling)
}
```

**Result**: User sees notification linking to permission settings. Automation disabled until both permissions granted.

### 8.6 No Upcoming Meetings

**Scenario**: User's calendars are empty or no relevant events for 7 days.

**Engine Response**:
- `nextInstance = null`
- `activeWindow = null`
- `resolveDndWindow()` → returns `DndWindow(null, null, false, null)`
- RULE 5 applies: "No active window"
- `shouldDisableDnd = true` (if we own it)
- `nextBoundaryMs = null`
- All alarms/workers canceled

**Result**: DND turned off if app owns it, no scheduled wakeups.

### 8.7 Overlapping and Back-to-Back Meetings

**Scenario**:
- Meeting A: 2:00-3:00 PM
- Meeting B: 3:00-4:00 PM
- Meeting C: 2:30-3:30 PM

**MeetingWindowResolver.findActiveWindow()** at 2:45 PM:
1. Filter active: [A (2:00-3:00), C (2:30-3:30)]
2. Initial window: begin=2:00, end=3:30
3. Check B (3:00-4:00): Overlaps? (3:00 <= 3:30 && 4:00 >= 2:00) → YES
4. Expand: begin=2:00, end=4:00
5. Return: Single window 2:00-4:00 with [A, C, B]

**Result**: Single contiguous DND block from 2:00-4:00 covering all meetings.

### 8.8 All-Day Events Handling

**Scenario**: User has all-day event "Vacation" on calendar.

**Setting**: `ignoreAllDay = true` (default)

**Filter Logic**:
```
if (ignoreAllDay && instance.allDay) {
    return false  // Skip this event
}
```

**Result**: Vacation event doesn't trigger DND (unless user changes setting).

### 8.9 Minimum Event Duration Filter

**Scenario**: Calendar has many 5-minute check-in events.

**Setting**: `minEventMinutes = 10` (default)

**Filter Logic**:
```
if (instance.durationMinutes < minEventMinutes) {
    return false
}
```

**Result**: Only events ≥10 minutes trigger DND.

### 8.10 Calendar Selection Filter

**Scenario**: User has Work, Personal, and Family calendars, only wants Work + Personal.

**Setting**: `selectedCalendarIds = {"work_id", "personal_id"}`

**Filter Logic**:
```
if (selectedCalendarIds.isNotEmpty() &&
    !selectedCalendarIds.contains(instance.calendarId)) {
    return false
}
```

**Result**: Family calendar events ignored.

### 8.11 Busy-Only Filter

**Scenario**: Coworker marks meeting as "Free" time.

**Setting**: `busyOnly = true` (default)

**Filter Logic**:
```
if (busyOnly && instance.availability != AVAILABILITY_BUSY) {
    return false
}
```

**Result**: "Free" time event doesn't trigger DND.

---

## 9. UI Navigation and Screens

### 9.1 Navigation Structure

**Route Hierarchy**:
```
STARTUP (check if onboarded)
├─→ [if not onboarded] LANGUAGE_ONBOARDING
│   └─→ INTRO
│       └─→ ONBOARDING (request permissions)
│           └─→ CALENDAR_SCOPE (all vs specific)
│               ├─→ [if specific] CALENDAR_PICKER_ONBOARDING
│               └─→ PRIVACY (privacy policy)
│                   └─→ STATUS (main app)
│
└─→ [if onboarded] STATUS (main app)
    ├─→ SETTINGS
    │   ├─→ CALENDAR_PICKER
    │   ├─→ DND_MODE
    │   ├─→ LANGUAGE_SETTINGS
    │   ├─→ DEBUG_TOOLS
    │   │   ├─→ DEBUG_LANGUAGE/{tag}
    │   │   └─→ DEBUG_SPLASH
    │   ├─→ HELP
    │   └─→ UPDATES
    ├─→ DEBUG_LOGS
    └─→ ONBOARDING (re-setup)
```

### 9.2 Key Navigation Features

**DestinationWrapper**:
- Wraps each screen to manage state transitions
- `lockedRoutes` set tracks routes during navigation
- Prevents interactions during animation transitions
- `onSystemBack` handler for device back button

**Navigation Locking Pattern**:
```kotlin
lockedRoutes.value = lockedRoutes.value + route  // Lock route during transition
navController.navigate(nextRoute)                 // Navigate away
// Route unlocked when DestinationWrapper disposed or screen becomes current again
```

**Debug Overlay**:
- Shows current route, elapsed time since last nav event
- Collapsible overlay in top-left
- Only visible if `debugOverlayEnabled` and DEBUG or DEBUG_TOOLS_ENABLED

### 9.3 Language Localization

**Mechanism**:
```kotlin
// MainActivity.kt
val preferredTag = settingsStore.preferredLanguageTag.first()
val effectiveTag = if (preferredTag.isBlank())
    resolveSupportedLanguage(Locale.getDefault().language)
else
    preferredTag

val localizedContext = baseContext.createConfigurationContext(
    Configuration(baseContext.resources.configuration).apply {
        setLocales(LocaleList.forLanguageTags(effectiveTag))
    }
)

CompositionLocalProvider(LocalContext provides localizedContext) {
    AppNavigation()
}
```

**Supported Languages**: en, de, hr, it, ko

---

## 10. Execution Flow Examples

### Example 1: User Opens App for First Time

```
1. App.onCreate()
   - Initialize Firebase based on opt-in settings

2. MainActivity.onCreate()
   - Read preferred language (empty)
   - Use system language, fallback to "en"
   - Render AppNavigation()

3. AppNavigation starts at STARTUP
   - Check onboardingCompleted from SettingsStore
   - FALSE → Navigate to LANGUAGE_ONBOARDING

4. LANGUAGE_ONBOARDING
   - User selects language
   - setPreferredLanguageTag stored
   - Navigate to INTRO

5. INTRO
   - Explain what app does
   - Navigate to ONBOARDING

6. ONBOARDING
   - Request READ_CALENDAR permission
   - Request Notification Policy Access (manual settings link)
   - Navigate to CALENDAR_SCOPE

7. CALENDAR_SCOPE
   - Choose: All calendars vs Specific calendars
   - If specific: CALENDAR_PICKER_ONBOARDING
   - setSelectedCalendarIds stored
   - Navigate to PRIVACY

8. PRIVACY
   - Show privacy policy
   - User confirms
   - setOnboardingCompleted(true)
   - Navigate to STATUS

9. STATUS
   - Main dashboard
   - EngineRunner.runEngine() called (some trigger)
   - Automation starts
```

### Example 2: Meeting About to Start - Exact Alarms Available

```
Current Time: 1:55 PM
Meeting starts: 2:00 PM
offset: 0 minutes

Trigger: WORKER_SANITY (15-min periodic check)
or previous boundary alarm scheduled 1:55

EngineRunner.runEngine(context, Trigger.ALARM)
├─ Gather input from stores
├─ Engine.run(input)
│  ├─ Check automation ON ✓
│  ├─ Check permissions ✓
│  ├─ getActiveInstances(1:55) → []
│  ├─ getNextInstance(1:55) → Meeting 2:00-3:00
│  ├─ resolveDndWindow()
│  │  dndStart = 2:00 + 0min = 2:00
│  │  dndEnd = 3:00
│  │  isActive = (1:55 in [2:00, 3:00)) → FALSE
│  │  nextStart = 2:00
│  ├─ detectUserOverride() → FALSE
│  ├─ isSuppressed = FALSE
│  ├─ RULE 5 applies: "No active window"
│  └─ Decision:
│     - shouldEnableDnd = false
│     - shouldDisableDnd = false (we don't own it)
│     - setActiveWindowEnd = null
│
├─ SchedulePlanner.planNextSchedule()
│  ├─ nextBoundary = 2:00 (DND start)
│  ├─ timeUntilBoundary = 5 minutes
│  ├─ isNearTerm = true (within 60 min)
│  ├─ hasExactAlarms = true
│  ├─ needsGuards = false (have exact alarms)
│  └─ SchedulePlan(
│     nextBoundaryMs = 2:00 PM,
│     guardBeforeMs = null,
│     guardAfterMs = null
│     )
│
├─ Apply decisions: no DND change
├─ Schedule exact alarm for 2:00 PM
│  alarmScheduler.scheduleBoundaryAlarm(2:00 PM timestamp)
│  → AlarmManager.setExactAndAllowWhileIdle()
│
└─ EngineRunner finishes, logs output
```

**At 2:00 PM**:
```
AlarmManager fires → AlarmReceiver.onReceive()
├─ Action = ACTION_BOUNDARY
├─ goAsync() → EngineRunner.runEngine(context, Trigger.ALARM)
│  ├─ Gather input from stores
│  ├─ Engine.run(input)
│  │  ├─ getActiveInstances(2:00) → [Meeting 2:00-3:00]
│  │  ├─ getNextInstance(2:00) → [Meeting 2:00-3:00]
│  │  ├─ resolveDndWindow()
│  │  │  dndStart = 2:00 + 0min = 2:00
│  │  │  dndEnd = 3:00
│  │  │  isActive = (2:00 in [2:00, 3:00)) → TRUE ✓
│  │  ├─ isSuppressed = FALSE
│  │  ├─ RULE 3 applies: "Active window, not suppressed"
│  │  └─ Decision:
│  │     - shouldEnableDnd = true ✓
│  │     - setDndSetByApp = true
│  │     - setActiveWindowEnd = 3:00
│  │
│  ├─ SchedulePlanner.planNextSchedule()
│  │  ├─ dndWindowActive = true
│  │  ├─ nextBoundary = 3:00 (end of window)
│  │  └─ SchedulePlan(nextBoundaryMs=3:00, ...)
│  │
│  ├─ Apply DND: dndController.enableDnd(DndMode.PRIORITY)
│  │  → NotificationManager.setInterruptionFilter(PRIORITY)
│  │  → DND turned ON ✓
│  │
│  ├─ Update state:
│  │  runtimeStateStore.setDndSetByApp(true)
│  │  runtimeStateStore.setActiveWindowEndMs(3:00)
│  │
│  ├─ Schedule next boundary at 3:00 PM
│  │  alarmScheduler.scheduleBoundaryAlarm(3:00 PM timestamp)
│  │
│  └─ Log: "Trigger: ALARM | Active: 1 event, ends 3:00 PM | Action: Enable DND (PRIORITY)"
│
└─ BroadcastReceiver.finish()
```

### Example 3: User Manually Turns Off DND During Meeting

```
Time: 2:15 PM (during meeting)
DND should be: ON (meeting until 3:00)
App thinks: dndSetByApp=true, DND is ON

User action: Settings → Turn off DND manually

Next trigger: SanityWorker (15-min check) or other event

EngineRunner.runEngine(context, Trigger.WORKER_SANITY)
├─ Gather input:
│  ├─ systemDndIsOn = false (user turned it off)
│  ├─ dndSetByApp = true (app thinks it owns it)
│  ├─ userSuppressedUntilMs = 0 (not suppressed yet)
│
├─ Engine.run(input)
│  ├─ getActiveInstances(2:15) → [Meeting]
│  ├─ resolveDndWindow() → isActive = true
│  ├─ detectUserOverride()
│  │  return (dndSetByApp=true && isActive=true && !systemDndIsOn=true)
│  │  → TRUE ✓ USER OVERRIDE DETECTED
│  │
│  ├─ isSuppressed = (2:15 < 0) || userOverrideDetected=true → TRUE
│  ├─ RULE 4 applies: "Active window but suppressed"
│  ├─ Decision:
│  │  - shouldEnableDnd = false (respect user)
│  │  - setDndSetByApp = false (we don't own it anymore)
│  │  - setUserSuppressedUntil = 3:00 (suppress until meeting end)
│  │
│  └─ Log: "Trigger: WORKER_SANITY | Active: 1 event | User override detected | Action: No change"
│
└─ Update state:
   runtimeStateStore.setDndSetByApp(false)
   runtimeStateStore.setUserSuppressedUntilMs(3:00)
```

**At 3:00 PM** (meeting ends):
```
Engine runs, detects no active meetings
- userSuppressedUntilMs (3:00) = now (3:00) → no longer suppressed
- RULE 5: "No active window"
- shouldDisableDnd = false (we don't own it anymore)
- Just track state, no action needed
```

---

## 11. Code Quality and Design Patterns

### 11.1 Design Patterns Used

1. **Repository Pattern** (CalendarRepository, ICalendarRepository)
   - Abstracts calendar data access
   - Allows testing with mocks
   - Single source of calendar queries

2. **Observer Pattern** (Flow-based)
   - SettingsStore and RuntimeStateStore use DataStore with Flow
   - UI observes state changes automatically
   - No manual updates needed

3. **Strategy Pattern** (DndMode enum)
   - Multiple DND modes encapsulated in enum
   - Easy to add new modes

4. **Trigger-Driven Architecture**
   - Same engine runs from multiple sources (alarm, worker, app, system broadcast)
   - Trigger type included for logging/analytics
   - Centralized decision logic

5. **State Machine** (implicit in Engine rules)
   - Five sequential rules define all state transitions
   - Deterministic given input

### 11.2 Error Handling

**Patterns**:
- Null-coalescing: `instance.title ?: ""` for string safety
- Try-catch for system API calls (alarm, DND changes, calendar query)
- `Result.retry()` for WorkManager failures
- Log and continue on exceptions
- Silent failures with `e.printStackTrace()`

### 11.3 Performance Considerations

1. **Calendar Query Optimization**:
   - 6-hour window for active queries (not all events)
   - 7-day window for next event (reasonable lookhead)
   - ORDER BY BEGIN ASC for efficient iteration
   - Filtering done in-memory, not in query

2. **State Access**:
   - `.first()` calls block on DataStore reads
   - Could be optimized with better caching
   - But realistically EngineRunner runs infrequently

3. **Alarm Management**:
   - `setExactAndAllowWhileIdle()` over `setAndAllowWhileIdle()`
   - Ensures precise timing if permission available
   - Degrades gracefully to WorkManager guards

### 11.4 Testing Considerations

- **ICalendarRepository interface** allows mock implementation
- **AutomationEngine is dependency-injected** with calendar provider
- **EngineInput/EngineOutput** are immutable data classes

---

## 12. Summary

CalendarDND is a sophisticated Android automation app that:

1. **Integrates** calendar events, DND system, alarms, WorkManager, and user preferences
2. **Executes** DND decisions through a single AutomationEngine triggered by multiple sources
3. **Manages** state across three persistent stores (SettingsStore, RuntimeStateStore, system DND state)
4. **Handles** edge cases (user override, missing permissions, Samsung quirks, overlapping meetings)
5. **Schedules** work with hybrid approach (exact alarms + WorkManager guards)
6. **Provides** intuitive UI with comprehensive onboarding and settings

The architecture is **event-driven** with **deterministic decision logic**, allowing the app to reliably automate DND based on calendar events while respecting user intent and system constraints.
