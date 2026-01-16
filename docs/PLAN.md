Alright. I’ll lock this down as a concrete build spec using **Jetpack Compose + DataStore (Preferences)**, with an engine you can unit test, and Android components wired for Samsung reliability.

# Calendar DND Automation App

Technical design and implementation specification (MVP + edge-case coverage)

---

## 0) Tech stack

* Language: **Kotlin**
* UI: **Jetpack Compose**
* Storage: **Preferences DataStore**
* Background:

    * **AlarmManager** for exact boundary wakeups (preferred)
    * **WorkManager** periodic sanity check (15 min)
    * **WorkManager** one-time “near-term guard” jobs (when exact alarms not available)
* Calendar access: `CalendarContract.Instances`
* DND control: `NotificationManager.setInterruptionFilter`
* Toggle: **Quick Settings TileService**
* Logging: local ring-buffer log (DataStore string set or small file)

---

## 1) App behavior summary

When automation is ON:

1. Detect whether there is an **active meeting window** (merged from overlapping/touching instances).
2. If active and not suppressed: ensure DND is ON until window end.
3. If not active: ensure DND is OFF only if the app previously turned it ON.
4. Schedule the next wake:

    * Next relevant start OR current window end
5. React quickly when calendar changes:

    * ContentObserver triggers engine run + reschedule
6. Safety nets:

    * Periodic sanity worker (15 minutes)
    * Near-term guard one-time work around upcoming start/end if exact alarms not available

When automation is OFF:

* Turn DND OFF only if app owns it, clear state, cancel alarms/works, stop observer.

---

## 2) Package structure (final)

```
com.example.calendardnd/

App.kt

ui/
  MainActivity.kt
  navigation/AppNav.kt
  screens/
    OnboardingScreen.kt
    StatusScreen.kt
    SettingsScreen.kt
    CalendarPickerScreen.kt
    DebugLogScreen.kt
  components/
    PermissionCard.kt
    StatusCard.kt

ui/tiles/
  AutomationTileService.kt

domain/
  engine/
    AutomationEngine.kt
    EngineInput.kt
    EngineOutput.kt
    Decision.kt
  planning/
    MeetingWindowResolver.kt
    SchedulePlanner.kt
  model/
    EventInstance.kt
    MeetingWindow.kt
    DndMode.kt

data/
  calendar/
    CalendarRepository.kt
    CalendarQueries.kt
    CalendarObserver.kt
    CalendarIdsRepository.kt
  dnd/
    DndController.kt
  prefs/
    SettingsStore.kt
    RuntimeStateStore.kt
    DebugLogStore.kt

system/
  alarms/
    AlarmScheduler.kt
    AlarmReceiver.kt
    AlarmActions.kt
  workers/
    SanityWorker.kt
    NearTermGuardWorker.kt
    Workers.kt
  receivers/
    BootReceiver.kt
    TimeChangeReceiver.kt
    TimezoneChangeReceiver.kt

util/
  Debouncer.kt
  TimeUtils.kt
  PermissionUtils.kt
```

---

## 3) Manifest spec

### Permissions

Required:

* `android.permission.READ_CALENDAR`

Recommended:

* `android.permission.RECEIVE_BOOT_COMPLETED`
* `android.permission.POST_NOTIFICATIONS` (Android 13+)
* `android.permission.SCHEDULE_EXACT_ALARM` (Android 12+ reliability)

### Components (manifest outline)

* `MainActivity` (exported=true)
* `AlarmReceiver` (exported=false)
* `BootReceiver` (exported=false) listens `BOOT_COMPLETED`
* `TimeChangeReceiver` (exported=false) listens `ACTION_TIME_CHANGED`
* `TimezoneChangeReceiver` (exported=false) listens `ACTION_TIMEZONE_CHANGED`
* `AutomationTileService` (exported=true, permission `BIND_QUICK_SETTINGS_TILE`)
* WorkManager default config

Notes:

* You do NOT declare notification policy access in manifest; user grants it in Settings.

---

## 4) DataStore keys

### SettingsStore (user-configurable)

* `automationEnabled: Boolean` default false
* `selectedCalendarIds: Set<String>` default empty (empty means “all calendars”)
* `busyOnly: Boolean` default true
* `ignoreAllDay: Boolean` default true
* `minEventMinutes: Int` default 10
* `dndMode: String` values: `"PRIORITY" | "TOTAL_SILENCE"` default `"PRIORITY"`
* `requireTitleKeyword: Boolean` default false (MVP optional)
* `titleKeyword: String` default "" (MVP optional)

### RuntimeStateStore (app-owned state)

* `dndSetByApp: Boolean` default false
* `activeWindowEndMs: Long` default 0 (0 means none)
* `userSuppressedUntilMs: Long` default 0 (0 means none)
* `lastPlannedBoundaryMs: Long` default 0
* `lastEngineRunMs: Long` default 0
* `lastKnownDndFilter: Int` default -1 (optional)

### DebugLogStore

* `logLines: List<String>` stored as single string with delimiter or as a file

    * Keep last 200 lines

---

## 5) Calendar logic spec

### Filters

An instance is relevant if:

* Calendar matches selected set (unless selection empty)
* If `busyOnly`: `Instances.AVAILABILITY == Events.AVAILABILITY_BUSY`
* If `ignoreAllDay`: `Instances.ALL_DAY == 0`
* Duration >= `minEventMinutes`

### Query windows

* Active lookup: query Instances within `[now-6h, now+6h]` and filter by `begin <= now < end`
* Next lookup: query within `[now, now+7d]` and take min by begin

### Meeting window merging

Input: list of relevant instances that intersect “now” or are near boundary planning
Output:

* If active: merge all instances where `begin <= now < end` plus any that overlap/touch that merged window.
  Definition touch: `next.begin <= current.end` (includes back-to-back)

This prevents DND flicker.

---

## 6) DND control spec (Samsung-safe)

### Requirements

* User must grant Notification Policy Access:

    * Deep link: `Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS`
* Check access:

    * `NotificationManager.isNotificationPolicyAccessGranted`

### Apply modes

* PRIORITY:

    * `INTERRUPTION_FILTER_PRIORITY`
* TOTAL_SILENCE:

    * Prefer `INTERRUPTION_FILTER_NONE` (note: alarms blocked)
    * If you want “no sound but alarms ok”, that’s not truly “total silence”; keep MVP simple and document it.

### Ownership rules

* Only disable DND if `dndSetByApp == true`.
* If user enables DND manually while app is OFF: app must not touch it.

### User override suppression (recommended MVP+)

If during an active meeting:

* app believes it owns DND, but system DND is OFF
* then:

    * set `userSuppressedUntilMs = activeWindowEndMs`
    * set `dndSetByApp=false`
    * do not re-enable DND until suppression expires

This stops “fighting the user”.

---

## 7) Scheduling spec

### Primary scheduling: exact boundary alarm

Use AlarmManager:

* Single scheduled alarm for the **next boundary**.
  Boundary selection:
* If active meeting: boundary = activeWindowEnd
* Else: boundary = nextMeetingBegin
* If none: schedule nothing (but periodic worker remains if automation ON)

Schedule method:

* `setExactAndAllowWhileIdle(RTC_WAKEUP, boundaryMs, pendingIntent)`

Permission handling:

* If exact alarms are not allowed:

    * don’t request repeatedly
    * show UI prompt + “degraded mode” badge

### Fallback scheduling: WorkManager near-term guards

When exact alarms not available and there’s an upcoming boundary within 60 minutes:

* enqueue OneTimeWork at:

    * `boundary - 2min` (clamped to now+10s)
    * `boundary + 2min`
      Also keep periodic 15 min sanity.

### Safety net: periodic sanity worker

* PeriodicWorkRequest every 15 minutes
* Constraints: none or “battery not low” optional (I’d avoid constraints for reliability)
* Work does:

    * run engine
    * fix state
    * reschedule boundary

### React to calendar changes: ContentObserver

* Observe `CalendarContract.Events.CONTENT_URI` and `CalendarContract.Instances.CONTENT_URI`
* On change:

    * Debounce 10 seconds
    * Trigger engine run + reschedule
      Implementation:
* Use a process singleton `CalendarObserver` that registers when automation enabled.
* If process dies, periodic worker and next alarm bring it back.

### Receivers

* BootReceiver: run engine, reschedule, ensure periodic worker enqueued
* TimeChangeReceiver / TimezoneChangeReceiver:

    * run engine, reschedule (events shift)

---

## 8) Engine decision table

Inputs:

* A = automationEnabled
* Pcal = calendar permission granted
* Pdnd = policy access granted
* Active = active meeting exists
* Supp = now < userSuppressedUntil
* Own = dndSetByApp
* SysDnd = current system DND is ON/OFF

Outputs:

* DndDesired: ON/OFF/NOCHANGE
* OwnDesired: true/false
* NextBoundary: timestamp or none
* Notify: setup-needed / degraded / none

Rules:

1. If A == false:

* If Own == true: set DND OFF, Own=false
* Cancel alarms + near-term workers
* Optionally stop periodic worker (or keep off)
* NextBoundary = none

2. If A == true and (Pcal == false or Pdnd == false):

* Don’t attempt DND changes
* Own=false (optional: keep Own but safer to clear)
* Schedule periodic worker only
* Notify “Setup required”
* NextBoundary = none (or still compute calendar if only Pdnd missing)

3. If A == true and Active == true and Supp == false:

* DND should be ON
* If SysDnd == OFF: set ON, Own=true
* If SysDnd == ON: if Own==false, keep Own as-is (do not assume ownership)
* NextBoundary = activeWindowEnd

4. If A == true and Active == true and Supp == true:

* Do not force ON
* Own=false
* NextBoundary = userSuppressedUntil (or activeWindowEnd)

5. If A == true and Active == false:

* If Own==true: set DND OFF, Own=false
* NextBoundary = nextMeetingBegin (if any)

User override detector (during sanity runs):

* If Active == true and Own == true and SysDnd == OFF:

    * set Supp = activeWindowEnd
    * Own=false

---

## 9) UI spec (Compose)

### Navigation

* If automation enabled but missing permissions → start at Onboarding
* Else → Status

### OnboardingScreen

Cards:

1. Calendar permission
2. Notification policy access
3. Exact alarms (optional, only shown if needed)
4. Battery help (optional)

Actions:

* Request calendar permission (runtime)
* Open DND policy settings
* Open exact alarm settings (if applicable)
* Open battery optimization settings

### StatusScreen

Display:

* Automation ON/OFF toggle
* “Current meeting” (title list, ends at)
* “Next meeting”
* DND state: system filter, app ownership, suppression
* Boundary schedule time
  Buttons:
* Run now (debug)
* Open logs
* Settings

### SettingsScreen

* Busy-only toggle
* Ignore all-day toggle
* Minimum minutes slider
* DND mode selector
* Calendar picker link
* Battery help link

### CalendarPickerScreen

* List calendars with checkbox
* “All calendars” option (empty selection)
* Save

### DebugLogScreen

* Scrollable log
* Export log (share intent) MVP optional
* Clear log

### Quick Settings Tile

States:

* OFF
* ON
* NEEDS SETUP (automation on but missing permissions)
  Tap behavior:
* If OFF → attempt enable; if missing permissions open app
* If ON → disable
* If NEEDS SETUP → open app onboarding

---

## 10) Implementation steps (ordered for lowest risk)

### Step 1: DND controller POC

* Implement `DndController`:

    * `hasPolicyAccess()`
    * `getCurrentFilter()`
    * `setFilter(mode)`
* Verify on your S25 it actually changes behavior.

### Step 2: Calendar queries POC

* Implement `CalendarRepository`:

    * `getActiveInstances(now)`
    * `getNextInstance(now)`
* Validate busy/all-day filters.

### Step 3: Engine core + status UI

* Implement engine as pure Kotlin with injected interfaces:

    * CalendarRepository
    * DndController
    * Scheduler
    * Stores
* Build status screen showing computed state.

### Step 4: Alarm scheduling

* Implement AlarmReceiver calling engine
* Schedule next boundary, verify in doze

### Step 5: WorkManager safety nets

* Periodic sanity worker
* Near-term guard worker (only in degraded mode)

### Step 6: ContentObserver + debounce

* Register when automation enabled
* On change, run engine

### Step 7: Tile + onboarding + notifications

* Setup required notification
* Degraded mode notification
* Tile states

---

## 11) Acceptance tests (Samsung-focused)

### Core functional

1. Create a meeting active now → DND turns on within 30s, stays until end
2. Meeting ends → DND turns off if app owns it
3. Back-to-back meetings (A ends exactly when B starts) → no DND flicker
4. Overlapping meetings → DND stays on until the latest end

### Filters

5. All-day event → does not trigger
6. “Free” event (availability free) → does not trigger when Busy-only is on
7. Event shorter than min minutes → does not trigger

### Permission flows

8. Automation ON without calendar permission → shows setup notification, no crashes
9. Automation ON without policy access → shows setup notification, no DND changes

### Ownership and user override

10. User manually turns ON DND while automation OFF → app does not turn it off
11. App turns ON DND for meeting, user turns it OFF mid-meeting → app respects suppression until meeting end

### Scheduling reliability

12. Screen off, phone idle: boundary alarm fires at meeting start/end (exact alarms enabled)
13. Exact alarms denied: meeting starts within 60 min → near-term guards trigger close to boundary
14. Add a meeting starting in 30 min while app running → observer triggers reschedule quickly
15. Add meeting while phone idle and sync delayed → periodic worker catches it within 15 min (documented)

### System events

16. Reboot → automation continues (alarms/work re-enqueued)
17. Change timezone → recompute, correct boundaries

### Battery management

18. Put app into “sleeping apps” intentionally → confirm degraded behavior is visible (log shows missed/alarm drift)
19. Remove from sleeping apps → returns to normal

---

## 12) Concrete class responsibilities (quick reference)

* `AutomationEngine`: single entry `run(trigger: Trigger)`
* `CalendarRepository`: reads Instances, lists calendars
* `MeetingWindowResolver`: merges overlaps/touches
* `SchedulePlanner`: picks next boundary + decides guard jobs
* `DndController`: reads/sets interruption filter
* `AlarmScheduler`: schedule/cancel boundary alarms
* `SanityWorker`: calls engine
* `NearTermGuardWorker`: calls engine
* `CalendarObserver`: registers observer and invokes engine
* `SettingsStore` / `RuntimeStateStore`: DataStore wrappers
* `DebugLogStore`: append + trim logs
* `AutomationTileService`: toggles automation, opens app if setup needed

---