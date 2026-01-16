# CalendarDND - Implementation Tasks

This document contains detailed implementation plans for upcoming improvements.

---

## Task Overview

| # | Task | Category | Priority | Estimated Effort |
|---|------|----------|----------|------------------|
| 1 | Centralize Constants | Small | High | 1-2 hours |
| 2 | Specific Exceptions | Small | High | 2-3 hours |
| 3 | Query Timeout | Small | Medium | 1-2 hours |
| 4 | Enhanced Debug Logs | Small | Medium | 2-3 hours |
| 5 | Cache DataStore Reads | Small | Medium | 2-3 hours |
| 6 | Haptic Feedback | QoL | Low | 1-2 hours |
| 7 | Enhanced Quick Settings Tile | QoL | Medium | 3-4 hours |
| 8 | Permission Explanations | UX | High | 2-3 hours |
| 9 | Clearer Status Indicator | UX | High | 3-4 hours |
| 10 | Settings Grouping | UX | Medium | 2-3 hours |
| 11 | Inline Setting Explanations | UX | Medium | 1-2 hours |
| 12 | Permission Status Display | UX | High | 2-3 hours |
| 13 | Dark Mode Fixes | UX | Medium | 3-4 hours |
| 14 | Friendlier Language | UX | High | 2-3 hours |
| 15 | Error State Improvements | UX | High | 3-4 hours |
| 16 | Degraded Mode Warning Banner | UX | High | 2-3 hours |
| 17 | Next Meeting Preview Card | UX | High | 3-4 hours |
| 18 | Empty State Design | UX | Medium | 2-3 hours |

---

## Task 1: Centralize Constants

### Problem
Magic numbers are scattered across multiple files:
- `2 minutes` - guard offset, overrun threshold
- `5 minutes` - pre-DND notification lead, meeting gap threshold
- `10 seconds` - minimum guard delay
- `15 minutes` - sanity worker interval
- `60 minutes` - near-term boundary threshold
- `6 hours` - active instances query window
- `7 days` - next instance query window

This makes maintenance difficult and increases risk of inconsistencies.

### Solution
Create a centralized constants file and replace all hardcoded values.

### Files to Create

**`app/src/main/java/com/brunoafk/calendardnd/util/EngineConstants.kt`**

```kotlin
package com.brunoafk.calendardnd.util

/**
 * Centralized constants for the automation engine and scheduling.
 * All time values are in milliseconds unless otherwise noted.
 */
object EngineConstants {

    // ═══════════════════════════════════════════════════════════════
    // SCHEDULING CONSTANTS
    // ═══════════════════════════════════════════════════════════════

    /** Interval for the periodic sanity worker (in minutes) */
    const val SANITY_WORKER_INTERVAL_MINUTES = 15L

    /** Threshold for considering a boundary "near-term" (eligible for guards) */
    const val NEAR_TERM_THRESHOLD_MS = 60 * 60 * 1000L  // 60 minutes

    /** Offset before/after boundary for WorkManager guards */
    const val GUARD_OFFSET_MS = 2 * 60 * 1000L  // 2 minutes

    /** Minimum delay before scheduling a guard (to avoid immediate firing) */
    const val GUARD_MIN_DELAY_MS = 10 * 1000L  // 10 seconds

    // ═══════════════════════════════════════════════════════════════
    // NOTIFICATION CONSTANTS
    // ═══════════════════════════════════════════════════════════════

    /** How far before DND starts to show pre-DND notification */
    const val PRE_DND_NOTIFICATION_LEAD_MS = 5 * 60 * 1000L  // 5 minutes

    /** Minimum delay before showing pre-DND notification */
    const val PRE_DND_NOTIFICATION_MIN_DELAY_MS = 5 * 1000L  // 5 seconds

    // ═══════════════════════════════════════════════════════════════
    // MEETING DETECTION CONSTANTS
    // ═══════════════════════════════════════════════════════════════

    /** Time window after meeting end to detect "just ended" for overrun */
    const val MEETING_OVERRUN_THRESHOLD_MS = 2 * 60 * 1000L  // 2 minutes

    /** Minimum gap to next meeting to trigger overrun notification */
    const val MEETING_GAP_THRESHOLD_MS = 5 * 60 * 1000L  // 5 minutes

    // ═══════════════════════════════════════════════════════════════
    // CALENDAR QUERY CONSTANTS
    // ═══════════════════════════════════════════════════════════════

    /** Window around 'now' for querying active instances (±3 hours = 6 hour total) */
    const val ACTIVE_INSTANCES_WINDOW_MS = 6 * 60 * 60 * 1000L  // 6 hours

    /** How far ahead to look for next upcoming instance */
    const val NEXT_INSTANCE_LOOKAHEAD_MS = 7 * 24 * 60 * 60 * 1000L  // 7 days

    /** Timeout for calendar ContentProvider queries */
    const val CALENDAR_QUERY_TIMEOUT_MS = 5000L  // 5 seconds

    // ═══════════════════════════════════════════════════════════════
    // DEBOUNCE CONSTANTS
    // ═══════════════════════════════════════════════════════════════

    /** Debounce delay for calendar change observer */
    const val CALENDAR_OBSERVER_DEBOUNCE_MS = 10 * 1000L  // 10 seconds
}
```

### Files to Modify

1. **`domain/engine/AutomationEngine.kt`**
   - Replace overrun detection magic numbers
   ```kotlin
   // Before
   val justEnded = (now - activeWindowEndMs) < 2 * 60 * 1000L
   val hasLargeGap = gapToNext > 5 * 60 * 1000L

   // After
   import com.brunoafk.calendardnd.util.EngineConstants.MEETING_OVERRUN_THRESHOLD_MS
   import com.brunoafk.calendardnd.util.EngineConstants.MEETING_GAP_THRESHOLD_MS

   val justEnded = (now - activeWindowEndMs) < MEETING_OVERRUN_THRESHOLD_MS
   val hasLargeGap = gapToNext > MEETING_GAP_THRESHOLD_MS
   ```

2. **`domain/planning/SchedulePlanner.kt`**
   - Replace near-term threshold and guard offsets
   ```kotlin
   // Before
   val isNearTerm = timeUntilBoundary in 1..60 * 60 * 1000L
   val guardBefore = maxOf(now + 10_000, nextBoundary - 2 * 60 * 1000L)

   // After
   import com.brunoafk.calendardnd.util.EngineConstants.*

   val isNearTerm = timeUntilBoundary in 1..NEAR_TERM_THRESHOLD_MS
   val guardBefore = maxOf(now + GUARD_MIN_DELAY_MS, nextBoundary - GUARD_OFFSET_MS)
   ```

3. **`data/calendar/CalendarRepository.kt`**
   - Replace query window constants
   ```kotlin
   // Before
   val windowStart = now - 6 * 60 * 60 * 1000L
   val windowEnd = now + 7 * 24 * 60 * 60 * 1000L

   // After
   import com.brunoafk.calendardnd.util.EngineConstants.*

   val windowStart = now - ACTIVE_INSTANCES_WINDOW_MS / 2
   val windowEnd = now + NEXT_INSTANCE_LOOKAHEAD_MS
   ```

4. **`system/alarms/EngineRunner.kt`**
   - Replace pre-DND notification constants
   ```kotlin
   // Before
   val notifyAtMs = dndStartMs - 5 * 60 * 1000L
   if (notifyAtMs > now + 5000)

   // After
   import com.brunoafk.calendardnd.util.EngineConstants.*

   val notifyAtMs = dndStartMs - PRE_DND_NOTIFICATION_LEAD_MS
   if (notifyAtMs > now + PRE_DND_NOTIFICATION_MIN_DELAY_MS)
   ```

5. **`system/workers/Workers.kt`**
   - Replace sanity worker interval
   ```kotlin
   // Before
   PeriodicWorkRequestBuilder<SanityWorker>(15, TimeUnit.MINUTES)

   // After
   import com.brunoafk.calendardnd.util.EngineConstants.SANITY_WORKER_INTERVAL_MINUTES

   PeriodicWorkRequestBuilder<SanityWorker>(SANITY_WORKER_INTERVAL_MINUTES, TimeUnit.MINUTES)
   ```

6. **`data/calendar/CalendarObserver.kt`**
   - Replace debounce constant
   ```kotlin
   // Before
   debouncer.debounce(10_000L) { ... }

   // After
   import com.brunoafk.calendardnd.util.EngineConstants.CALENDAR_OBSERVER_DEBOUNCE_MS

   debouncer.debounce(CALENDAR_OBSERVER_DEBOUNCE_MS) { ... }
   ```

### Implementation Checklist

- [ ] Create `util/EngineConstants.kt` with all constants
- [ ] Update `AutomationEngine.kt` - meeting overrun detection
- [ ] Update `SchedulePlanner.kt` - near-term threshold, guard offsets
- [ ] Update `CalendarRepository.kt` - query windows
- [ ] Update `EngineRunner.kt` - pre-DND notification timing
- [ ] Update `Workers.kt` - sanity worker interval
- [ ] Update `CalendarObserver.kt` - debounce delay
- [ ] Search codebase for any remaining magic numbers
- [ ] Run `./gradlew test` to verify no regressions
- [ ] Test on device: verify DND timing still works correctly

### Testing Notes

After implementation, verify:
1. Pre-DND notification arrives ~5 minutes before meeting
2. DND enables/disables at correct meeting boundaries
3. Sanity worker runs every 15 minutes (check debug logs)
4. Back-to-back meetings merge correctly (no flicker)

---

## Task 2: Specific Exceptions

### Problem
All exceptions are caught as generic `Exception` with only `e.printStackTrace()`:
- No differentiation between recoverable and fatal errors
- No analytics tracking of specific failure types
- Difficult to debug Samsung-specific issues
- Silent failures with no user feedback

### Solution
Implement typed exception handling with proper logging and optional analytics.

### Files to Create

**`app/src/main/java/com/brunoafk/calendardnd/util/ExceptionHandler.kt`**

```kotlin
package com.brunoafk.calendardnd.util

import android.util.Log

/**
 * Centralized exception handling utilities.
 */
object ExceptionHandler {

    private const val TAG = "CalendarDND"

    /**
     * Handle exceptions from DND operations.
     * Returns true if the operation should be retried.
     */
    fun handleDndException(e: Exception, operation: String): Boolean {
        return when (e) {
            is SecurityException -> {
                // Samsung quirk: throws even with permission granted
                Log.w(TAG, "SecurityException during $operation - possible Samsung quirk", e)
                AnalyticsTracker.logException("dnd_security", operation)
                false // Don't retry, will work on next engine run
            }
            is IllegalStateException -> {
                // System service unavailable (rare)
                Log.e(TAG, "IllegalStateException during $operation - system service issue", e)
                AnalyticsTracker.logException("dnd_illegal_state", operation)
                false
            }
            is IllegalArgumentException -> {
                // Invalid filter value (shouldn't happen)
                Log.e(TAG, "IllegalArgumentException during $operation - invalid argument", e)
                AnalyticsTracker.logException("dnd_illegal_arg", operation)
                false
            }
            else -> {
                Log.e(TAG, "Unexpected exception during $operation", e)
                AnalyticsTracker.logException("dnd_unknown", operation)
                false
            }
        }
    }

    /**
     * Handle exceptions from calendar operations.
     * Returns true if the operation should be retried.
     */
    fun handleCalendarException(e: Exception, operation: String): Boolean {
        return when (e) {
            is SecurityException -> {
                // Permission revoked
                Log.w(TAG, "SecurityException during $operation - permission revoked?", e)
                AnalyticsTracker.logException("calendar_security", operation)
                false
            }
            is IllegalArgumentException -> {
                // Invalid URI or projection
                Log.e(TAG, "IllegalArgumentException during $operation", e)
                AnalyticsTracker.logException("calendar_illegal_arg", operation)
                false
            }
            is android.database.CursorIndexOutOfBoundsException -> {
                // Cursor access issue
                Log.e(TAG, "CursorIndexOutOfBoundsException during $operation", e)
                AnalyticsTracker.logException("calendar_cursor", operation)
                false
            }
            else -> {
                Log.e(TAG, "Unexpected exception during $operation", e)
                AnalyticsTracker.logException("calendar_unknown", operation)
                false
            }
        }
    }

    /**
     * Handle exceptions from alarm operations.
     */
    fun handleAlarmException(e: Exception, operation: String): Boolean {
        return when (e) {
            is SecurityException -> {
                // SCHEDULE_EXACT_ALARM permission revoked
                Log.w(TAG, "SecurityException during $operation - exact alarm permission revoked?", e)
                AnalyticsTracker.logException("alarm_security", operation)
                false
            }
            is IllegalStateException -> {
                // AlarmManager not available
                Log.e(TAG, "IllegalStateException during $operation", e)
                AnalyticsTracker.logException("alarm_illegal_state", operation)
                false
            }
            else -> {
                Log.e(TAG, "Unexpected exception during $operation", e)
                AnalyticsTracker.logException("alarm_unknown", operation)
                false
            }
        }
    }
}
```

**Update `util/AnalyticsTracker.kt`** (add method):

```kotlin
/**
 * Log an exception event for tracking failure patterns.
 */
fun logException(type: String, operation: String) {
    if (!isEnabled) return

    firebaseAnalytics.logEvent("exception") {
        param("exception_type", type)
        param("operation", operation)
        param("device_manufacturer", Build.MANUFACTURER)
        param("device_model", Build.MODEL)
        param("android_version", Build.VERSION.SDK_INT.toLong())
    }
}
```

### Files to Modify

1. **`data/dnd/DndController.kt`**

```kotlin
package com.brunoafk.calendardnd.data.dnd

import com.brunoafk.calendardnd.util.ExceptionHandler

class DndController(private val context: Context) {

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val TAG = "DndController"
    }

    fun enableDnd(mode: DndMode): Boolean {
        if (!hasPolicyAccess()) {
            Log.w(TAG, "enableDnd called without policy access")
            return false
        }

        return try {
            notificationManager.setInterruptionFilter(mode.filterValue)
            Log.d(TAG, "DND enabled with mode: $mode")
            true
        } catch (e: SecurityException) {
            ExceptionHandler.handleDndException(e, "enableDnd")
            false
        } catch (e: IllegalStateException) {
            ExceptionHandler.handleDndException(e, "enableDnd")
            false
        } catch (e: Exception) {
            ExceptionHandler.handleDndException(e, "enableDnd")
            false
        }
    }

    fun disableDnd(): Boolean {
        if (!hasPolicyAccess()) {
            Log.w(TAG, "disableDnd called without policy access")
            return false
        }

        return try {
            notificationManager.setInterruptionFilter(
                NotificationManager.INTERRUPTION_FILTER_ALL
            )
            Log.d(TAG, "DND disabled")
            true
        } catch (e: SecurityException) {
            ExceptionHandler.handleDndException(e, "disableDnd")
            false
        } catch (e: IllegalStateException) {
            ExceptionHandler.handleDndException(e, "disableDnd")
            false
        } catch (e: Exception) {
            ExceptionHandler.handleDndException(e, "disableDnd")
            false
        }
    }

    fun getCurrentFilter(): Int {
        return try {
            notificationManager.currentInterruptionFilter
        } catch (e: Exception) {
            ExceptionHandler.handleDndException(e, "getCurrentFilter")
            NotificationManager.INTERRUPTION_FILTER_UNKNOWN
        }
    }
}
```

2. **`data/calendar/CalendarQueries.kt`**

```kotlin
fun queryInstances(
    context: Context,
    beginMs: Long,
    endMs: Long
): List<EventInstance> {
    return try {
        // Existing query logic...
        val uri = CalendarContract.Instances.CONTENT_URI
            .buildUpon()
            .appendPath(beginMs.toString())
            .appendPath(endMs.toString())
            .build()

        val cursor = context.contentResolver.query(
            uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC"
        )

        cursor?.use {
            // Map to EventInstance list
        } ?: emptyList()

    } catch (e: SecurityException) {
        ExceptionHandler.handleCalendarException(e, "queryInstances")
        emptyList()
    } catch (e: IllegalArgumentException) {
        ExceptionHandler.handleCalendarException(e, "queryInstances")
        emptyList()
    } catch (e: Exception) {
        ExceptionHandler.handleCalendarException(e, "queryInstances")
        emptyList()
    }
}
```

3. **`system/alarms/AlarmScheduler.kt`**

```kotlin
fun scheduleBoundaryAlarm(boundaryMs: Long): Boolean {
    if (!canScheduleExactAlarms()) {
        Log.w(TAG, "Cannot schedule exact alarms - permission not granted")
        return false
    }

    return try {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmActions.ACTION_BOUNDARY
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_BOUNDARY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            boundaryMs,
            pendingIntent
        )

        Log.d(TAG, "Scheduled boundary alarm for ${formatTime(boundaryMs)}")
        true

    } catch (e: SecurityException) {
        ExceptionHandler.handleAlarmException(e, "scheduleBoundaryAlarm")
        false
    } catch (e: IllegalStateException) {
        ExceptionHandler.handleAlarmException(e, "scheduleBoundaryAlarm")
        false
    } catch (e: Exception) {
        ExceptionHandler.handleAlarmException(e, "scheduleBoundaryAlarm")
        false
    }
}
```

### Implementation Checklist

- [ ] Create `util/ExceptionHandler.kt`
- [ ] Add `logException()` method to `AnalyticsTracker.kt`
- [ ] Update `DndController.kt` with typed exception handling
- [ ] Update `CalendarQueries.kt` with typed exception handling
- [ ] Update `AlarmScheduler.kt` with typed exception handling
- [ ] Update `CalendarRepository.kt` if it has direct try-catch blocks
- [ ] Search for remaining `catch (e: Exception)` and evaluate each
- [ ] Run `./gradlew test` to verify no regressions
- [ ] Test exception scenarios (revoke permissions, etc.)

### Testing Notes

To test exception handling:
1. Revoke calendar permission while app running → should log SecurityException
2. Revoke DND policy access → should handle gracefully
3. Check Firebase Analytics for exception events (if enabled)
4. Review logcat for proper exception categorization

---

## Task 3: Query Timeout

### Problem
Calendar ContentProvider queries have no timeout:
- If system is under load, query could hang indefinitely
- EngineRunner blocks on query completion
- Could cause ANR (Application Not Responding) in worst case
- BroadcastReceiver has 10-second limit before system kills it

### Solution
Wrap calendar queries in coroutine timeout.

### Files to Modify

1. **`data/calendar/CalendarRepository.kt`**

```kotlin
package com.brunoafk.calendardnd.data.calendar

import com.brunoafk.calendardnd.util.EngineConstants.CALENDAR_QUERY_TIMEOUT_MS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class CalendarRepository(
    private val context: Context,
    private val calendarQueries: CalendarQueries = CalendarQueries()
) : ICalendarRepository {

    companion object {
        private const val TAG = "CalendarRepository"
    }

    /**
     * Get currently active calendar instances with timeout protection.
     * Returns empty list if query times out.
     */
    override suspend fun getActiveInstances(
        now: Long,
        selectedCalendarIds: Set<String>,
        busyOnly: Boolean,
        ignoreAllDay: Boolean,
        minEventMinutes: Int
    ): List<EventInstance> {
        return executeWithTimeout("getActiveInstances") {
            val windowStart = now - ACTIVE_INSTANCES_WINDOW_MS / 2
            val windowEnd = now + ACTIVE_INSTANCES_WINDOW_MS / 2

            calendarQueries.queryInstances(context, windowStart, windowEnd)
                .filter { instance ->
                    isRelevantInstance(instance, selectedCalendarIds, busyOnly, ignoreAllDay, minEventMinutes)
                }
                .filter { instance ->
                    // Active: begin <= now < end
                    instance.begin <= now && now < instance.end
                }
        } ?: emptyList()
    }

    /**
     * Get next upcoming calendar instance with timeout protection.
     * Returns null if query times out or no upcoming events.
     */
    override suspend fun getNextInstance(
        now: Long,
        selectedCalendarIds: Set<String>,
        busyOnly: Boolean,
        ignoreAllDay: Boolean,
        minEventMinutes: Int
    ): EventInstance? {
        return executeWithTimeout("getNextInstance") {
            val windowEnd = now + NEXT_INSTANCE_LOOKAHEAD_MS

            calendarQueries.queryInstances(context, now, windowEnd)
                .filter { instance ->
                    isRelevantInstance(instance, selectedCalendarIds, busyOnly, ignoreAllDay, minEventMinutes)
                }
                .filter { instance ->
                    instance.begin > now  // Future events only
                }
                .minByOrNull { it.begin }  // Earliest first
        }
    }

    /**
     * Get all instances in a time range with timeout protection.
     */
    override suspend fun getInstancesInRange(
        startMs: Long,
        endMs: Long,
        selectedCalendarIds: Set<String>,
        busyOnly: Boolean,
        ignoreAllDay: Boolean,
        minEventMinutes: Int
    ): List<EventInstance> {
        return executeWithTimeout("getInstancesInRange") {
            calendarQueries.queryInstances(context, startMs, endMs)
                .filter { instance ->
                    isRelevantInstance(instance, selectedCalendarIds, busyOnly, ignoreAllDay, minEventMinutes)
                }
        } ?: emptyList()
    }

    /**
     * Execute a calendar operation with timeout protection.
     * Runs on IO dispatcher to avoid blocking main thread.
     */
    private suspend fun <T> executeWithTimeout(
        operationName: String,
        block: suspend () -> T
    ): T? {
        return try {
            withTimeoutOrNull(CALENDAR_QUERY_TIMEOUT_MS) {
                withContext(Dispatchers.IO) {
                    block()
                }
            }.also { result ->
                if (result == null) {
                    Log.w(TAG, "$operationName timed out after ${CALENDAR_QUERY_TIMEOUT_MS}ms")
                    AnalyticsTracker.logException("calendar_timeout", operationName)
                }
            }
        } catch (e: Exception) {
            ExceptionHandler.handleCalendarException(e, operationName)
            null
        }
    }

    // ... rest of existing code (isRelevantInstance, etc.)
}
```

2. **`data/calendar/ICalendarRepository.kt`** (update interface to be suspend)

```kotlin
package com.brunoafk.calendardnd.data.calendar

interface ICalendarRepository {

    suspend fun getActiveInstances(
        now: Long,
        selectedCalendarIds: Set<String>,
        busyOnly: Boolean,
        ignoreAllDay: Boolean,
        minEventMinutes: Int
    ): List<EventInstance>

    suspend fun getNextInstance(
        now: Long,
        selectedCalendarIds: Set<String>,
        busyOnly: Boolean,
        ignoreAllDay: Boolean,
        minEventMinutes: Int
    ): EventInstance?

    suspend fun getInstancesInRange(
        startMs: Long,
        endMs: Long,
        selectedCalendarIds: Set<String>,
        busyOnly: Boolean,
        ignoreAllDay: Boolean,
        minEventMinutes: Int
    ): List<EventInstance>

    fun getCalendars(): List<CalendarInfo>
}
```

3. **`domain/engine/AutomationEngine.kt`** (ensure it handles null/empty results)

```kotlin
// The engine already handles empty lists gracefully, but add explicit null checks
suspend fun run(input: EngineInput): EngineOutput {
    // ... existing code

    val activeInstances = calendarRepository.getActiveInstances(
        input.now,
        input.selectedCalendarIds,
        input.busyOnly,
        input.ignoreAllDay,
        input.minEventMinutes
    )

    // activeInstances will be empty list on timeout - this is handled correctly
    // by existing logic (no active window = don't enable DND)

    // ... rest of engine logic
}
```

### Implementation Checklist

- [ ] Add `CALENDAR_QUERY_TIMEOUT_MS` to `EngineConstants.kt` (done in Task 1)
- [ ] Update `ICalendarRepository.kt` interface methods to be `suspend`
- [ ] Update `CalendarRepository.kt` with `executeWithTimeout()` wrapper
- [ ] Ensure `AutomationEngine` handles empty/null results gracefully
- [ ] Update any callers of CalendarRepository that aren't already in coroutine context
- [ ] Add timeout analytics tracking
- [ ] Run `./gradlew test` - update tests for suspend functions
- [ ] Test with slow device / heavy load scenarios

### Testing Notes

To test timeout handling:
1. Normal operation should complete well under 5 seconds
2. Simulate slow query by adding artificial delay in test
3. Verify empty list returned on timeout (not crash)
4. Check analytics for timeout events
5. Verify DND doesn't activate incorrectly when query times out

---

## Task 4: Enhanced Debug Logs

### Problem
Current debug logs lack context for troubleshooting:
- No device/system state information
- No battery level (affects scheduling reliability)
- No indication of which calendars were checked
- Timestamp format not always clear
- Hard to correlate logs with actual behavior

### Solution
Create a structured debug log format with rich context.

### Files to Create

**`app/src/main/java/com/brunoafk/calendardnd/util/DebugLogger.kt`**

```kotlin
package com.brunoafk.calendardnd.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import java.text.SimpleDateFormat
import java.util.*

/**
 * Creates structured, context-rich debug log entries.
 */
object DebugLogger {

    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    /**
     * Build a comprehensive engine run log entry.
     */
    fun buildEngineLog(
        context: Context,
        trigger: Trigger,
        input: EngineInput,
        output: EngineOutput,
        executionTimeMs: Long
    ): String {
        return buildString {
            appendLine("╔════════════════════════════════════════════════════════════")
            appendLine("║ ENGINE RUN - ${dateTimeFormat.format(Date(input.now))}")
            appendLine("╠════════════════════════════════════════════════════════════")

            // Trigger & Timing
            appendLine("║ TRIGGER")
            appendLine("║   Source: $trigger")
            appendLine("║   Execution time: ${executionTimeMs}ms")
            appendLine("║")

            // System State
            appendLine("║ SYSTEM STATE")
            appendLine("║   Battery: ${getBatteryInfo(context)}")
            appendLine("║   Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("║   Android: ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})")
            appendLine("║")

            // Permissions
            appendLine("║ PERMISSIONS")
            appendLine("║   Calendar: ${if (input.hasCalendarPermission) "✓" else "✗"}")
            appendLine("║   DND Policy: ${if (input.hasPolicyAccess) "✓" else "✗"}")
            appendLine("║   Exact Alarms: ${if (input.hasExactAlarms) "✓" else "✗"}")
            appendLine("║")

            // Settings
            appendLine("║ SETTINGS")
            appendLine("║   Automation: ${if (input.automationEnabled) "ON" else "OFF"}")
            appendLine("║   DND Mode: ${input.dndMode}")
            appendLine("║   Offset: ${input.dndStartOffsetMinutes} min")
            appendLine("║   Calendars: ${formatCalendarIds(input.selectedCalendarIds)}")
            appendLine("║   Filters: busy=${input.busyOnly}, noAllDay=${input.ignoreAllDay}, min=${input.minEventMinutes}min")
            appendLine("║")

            // Current State
            appendLine("║ CURRENT STATE")
            appendLine("║   System DND: ${if (input.systemDndIsOn) "ON (${input.currentSystemFilter})" else "OFF"}")
            appendLine("║   App owns DND: ${input.dndSetByApp}")
            appendLine("║   Suppressed until: ${formatTime(input.userSuppressedUntilMs)}")
            appendLine("║   Manual DND until: ${formatTime(input.manualDndUntilMs)}")
            appendLine("║   Active window end: ${formatTime(input.activeWindowEndMs)}")
            appendLine("║")

            // Calendar Events
            appendLine("║ CALENDAR EVENTS")
            val activeWindow = output.activeWindow
            if (activeWindow != null) {
                appendLine("║   Active Window: ${formatTimeRange(activeWindow.begin, activeWindow.end)}")
                appendLine("║   Events in window: ${activeWindow.events.size}")
                activeWindow.events.take(3).forEach { event ->
                    appendLine("║     • ${event.title ?: "(no title)"}")
                    appendLine("║       ${formatTimeRange(event.begin, event.end)}")
                }
                if (activeWindow.events.size > 3) {
                    appendLine("║     ... and ${activeWindow.events.size - 3} more")
                }
            } else {
                appendLine("║   Active Window: none")
            }

            val nextEvent = output.nextInstance
            if (nextEvent != null) {
                appendLine("║   Next Event: ${nextEvent.title ?: "(no title)"}")
                appendLine("║     Starts: ${formatDateTime(nextEvent.begin)}")
            } else {
                appendLine("║   Next Event: none in next 7 days")
            }
            appendLine("║")

            // Decision
            appendLine("║ DECISION")
            val decision = output.decision
            appendLine("║   Action: ${formatDecisionAction(decision)}")
            if (decision.setDndSetByApp != null) {
                appendLine("║   Set ownership: ${decision.setDndSetByApp}")
            }
            if (decision.setUserSuppressedUntil != null) {
                appendLine("║   Suppress until: ${formatTime(decision.setUserSuppressedUntil)}")
            }
            if (output.userOverrideDetected) {
                appendLine("║   ⚠️ User override detected!")
            }
            appendLine("║")

            // Scheduling
            appendLine("║ SCHEDULING")
            val plan = output.schedulePlan
            if (plan?.nextBoundaryMs != null) {
                appendLine("║   Next boundary: ${formatDateTime(plan.nextBoundaryMs)}")
                appendLine("║   Using guards: ${plan.needsNearTermGuards}")
                if (plan.needsNearTermGuards) {
                    appendLine("║   Guard before: ${formatTime(plan.guardBeforeMs)}")
                    appendLine("║   Guard after: ${formatTime(plan.guardAfterMs)}")
                }
            } else {
                appendLine("║   Next boundary: none scheduled")
            }

            appendLine("╚════════════════════════════════════════════════════════════")
        }
    }

    /**
     * Build a compact single-line log for quick scanning.
     */
    fun buildCompactLog(
        trigger: Trigger,
        input: EngineInput,
        output: EngineOutput
    ): String {
        val action = formatDecisionAction(output.decision)
        val activeCount = output.activeWindow?.events?.size ?: 0
        val nextIn = output.nextInstance?.let {
            val minutes = (it.begin - input.now) / 60000
            if (minutes < 60) "${minutes}min" else "${minutes / 60}h"
        } ?: "none"

        return "[${timeFormat.format(Date(input.now))}] " +
               "$trigger → $action | " +
               "Active: $activeCount | " +
               "Next: $nextIn | " +
               "Battery: ${getBatteryPercent(input.context)}%"
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPER FUNCTIONS
    // ═══════════════════════════════════════════════════════════════

    private fun getBatteryInfo(context: Context): String {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) (level * 100 / scale) else -1

        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                         status == BatteryManager.BATTERY_STATUS_FULL

        return "$percent%${if (isCharging) " (charging)" else ""}"
    }

    private fun getBatteryPercent(context: Context): Int {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) (level * 100 / scale) else -1
    }

    private fun formatTime(ms: Long?): String {
        if (ms == null || ms <= 0) return "none"
        return timeFormat.format(Date(ms))
    }

    private fun formatDateTime(ms: Long?): String {
        if (ms == null || ms <= 0) return "none"
        return dateTimeFormat.format(Date(ms))
    }

    private fun formatTimeRange(startMs: Long, endMs: Long): String {
        return "${timeFormat.format(Date(startMs))} - ${timeFormat.format(Date(endMs))}"
    }

    private fun formatCalendarIds(ids: Set<String>): String {
        return if (ids.isEmpty()) "all" else "${ids.size} selected"
    }

    private fun formatDecisionAction(decision: EngineDecision): String {
        return when {
            decision.shouldEnableDnd -> "ENABLE DND"
            decision.shouldDisableDnd -> "DISABLE DND"
            else -> "NO CHANGE"
        }
    }
}
```

### Files to Modify

1. **`system/alarms/EngineRunner.kt`**

```kotlin
suspend fun runEngine(context: Context, trigger: Trigger) {
    val startTime = System.currentTimeMillis()

    // ... gather input, run engine ...

    val executionTime = System.currentTimeMillis() - startTime

    // Enhanced logging
    val detailedLog = DebugLogger.buildEngineLog(
        context, trigger, input, output, executionTime
    )
    val compactLog = DebugLogger.buildCompactLog(trigger, input, output)

    // Store detailed log
    debugLogStore.appendLog(detailedLog)

    // Logcat gets compact version
    Log.d(TAG, compactLog)

    // ... rest of engine runner
}
```

2. **`data/prefs/DebugLogStore.kt`** (increase log capacity)

```kotlin
companion object {
    private const val MAX_LOG_LINES = 500  // Increased from 200
    private const val MAX_LOG_SIZE_BYTES = 500_000  // ~500KB limit
}

suspend fun appendLog(entry: String) {
    dataStore.edit { prefs ->
        val currentLog = prefs[LOG_KEY] ?: ""
        val newLog = "$entry\n$currentLog"

        // Trim if too large
        val trimmedLog = if (newLog.length > MAX_LOG_SIZE_BYTES) {
            newLog.take(MAX_LOG_SIZE_BYTES)
                .substringBeforeLast("\n╔")  // Keep complete entries
        } else {
            newLog
        }

        prefs[LOG_KEY] = trimmedLog
    }
}
```

### Implementation Checklist

- [ ] Create `util/DebugLogger.kt` with structured log builder
- [ ] Update `EngineRunner.kt` to use new logger
- [ ] Update `DebugLogStore.kt` to handle larger logs
- [ ] Add execution time tracking to engine runs
- [ ] Add battery info helper
- [ ] Update `DebugLogScreen.kt` to handle new format (scrollable, searchable)
- [ ] Run `./gradlew test`
- [ ] Test on device and verify log readability

### Testing Notes

Verify logs include:
1. Clear timestamps and trigger source
2. Battery level and charging state
3. All permission states
4. Active window details with event titles
5. Decision action clearly stated
6. Next boundary scheduling info

---

## Task 5: Cache DataStore Reads

### Problem
`EngineRunner` makes multiple blocking `.first()` calls on every run:
- Each `.first()` call reads from disk
- Multiple reads on every engine execution (potentially every 15 minutes)
- Unnecessary I/O when values rarely change
- Could cause slight delays in time-sensitive operations

### Solution
Batch read all settings/state in single operations using snapshot data classes.

### Files to Modify

1. **`data/prefs/SettingsStore.kt`** (add snapshot method)

```kotlin
package com.brunoafk.calendardnd.data.prefs

/**
 * Immutable snapshot of all user settings.
 */
data class SettingsSnapshot(
    val automationEnabled: Boolean,
    val selectedCalendarIds: Set<String>,
    val busyOnly: Boolean,
    val ignoreAllDay: Boolean,
    val minEventMinutes: Int,
    val dndMode: DndMode,
    val dndStartOffsetMinutes: Int,
    val preDndNotificationEnabled: Boolean,
    val requireTitleKeyword: Boolean,
    val titleKeyword: String,
    val hapticFeedbackEnabled: Boolean  // For Task 6
)

class SettingsStore(private val context: Context) {

    // ... existing Flow properties ...

    /**
     * Get all settings as a single snapshot.
     * More efficient than multiple .first() calls.
     */
    suspend fun getSnapshot(): SettingsSnapshot {
        return dataStore.data.first().let { prefs ->
            SettingsSnapshot(
                automationEnabled = prefs[AUTOMATION_ENABLED] ?: true,
                selectedCalendarIds = prefs[SELECTED_CALENDAR_IDS]
                    ?.split(",")
                    ?.filter { it.isNotBlank() }
                    ?.toSet()
                    ?: emptySet(),
                busyOnly = prefs[BUSY_ONLY] ?: true,
                ignoreAllDay = prefs[IGNORE_ALL_DAY] ?: true,
                minEventMinutes = prefs[MIN_EVENT_MINUTES] ?: 10,
                dndMode = DndMode.fromValue(prefs[DND_MODE] ?: DndMode.PRIORITY.filterValue),
                dndStartOffsetMinutes = prefs[DND_START_OFFSET_MINUTES] ?: 0,
                preDndNotificationEnabled = prefs[PRE_DND_NOTIFICATION_ENABLED] ?: false,
                requireTitleKeyword = prefs[REQUIRE_TITLE_KEYWORD] ?: false,
                titleKeyword = prefs[TITLE_KEYWORD] ?: "",
                hapticFeedbackEnabled = prefs[HAPTIC_FEEDBACK_ENABLED] ?: true
            )
        }
    }
}
```

2. **`data/prefs/RuntimeStateStore.kt`** (add snapshot method)

```kotlin
package com.brunoafk.calendardnd.data.prefs

/**
 * Immutable snapshot of runtime state.
 */
data class RuntimeStateSnapshot(
    val dndSetByApp: Boolean,
    val activeWindowEndMs: Long,
    val userSuppressedUntilMs: Long,
    val manualDndUntilMs: Long,
    val lastPlannedBoundaryMs: Long,
    val lastEngineRunMs: Long,
    val lastKnownDndFilter: Int
)

class RuntimeStateStore(private val context: Context) {

    // ... existing Flow properties ...

    /**
     * Get all runtime state as a single snapshot.
     * More efficient than multiple .first() calls.
     */
    suspend fun getSnapshot(): RuntimeStateSnapshot {
        return dataStore.data.first().let { prefs ->
            RuntimeStateSnapshot(
                dndSetByApp = prefs[DND_SET_BY_APP] ?: false,
                activeWindowEndMs = prefs[ACTIVE_WINDOW_END_MS] ?: 0L,
                userSuppressedUntilMs = prefs[USER_SUPPRESSED_UNTIL_MS] ?: 0L,
                manualDndUntilMs = prefs[MANUAL_DND_UNTIL_MS] ?: 0L,
                lastPlannedBoundaryMs = prefs[LAST_PLANNED_BOUNDARY_MS] ?: 0L,
                lastEngineRunMs = prefs[LAST_ENGINE_RUN_MS] ?: 0L,
                lastKnownDndFilter = prefs[LAST_KNOWN_DND_FILTER]
                    ?: NotificationManager.INTERRUPTION_FILTER_UNKNOWN
            )
        }
    }
}
```

3. **`system/alarms/EngineRunner.kt`** (use snapshots)

```kotlin
suspend fun runEngine(context: Context, trigger: Trigger) {
    val startTime = System.currentTimeMillis()

    val settingsStore = SettingsStore(context)
    val runtimeStateStore = RuntimeStateStore(context)
    val dndController = DndController(context)
    val alarmScheduler = AlarmScheduler(context)

    // Single read for all settings
    val settings = settingsStore.getSnapshot()

    // Single read for all runtime state
    val runtimeState = runtimeStateStore.getSnapshot()

    // Build input from snapshots (much faster than individual .first() calls)
    val input = EngineInput(
        trigger = trigger,
        now = startTime,

        // From settings snapshot
        automationEnabled = settings.automationEnabled,
        selectedCalendarIds = settings.selectedCalendarIds,
        busyOnly = settings.busyOnly,
        ignoreAllDay = settings.ignoreAllDay,
        minEventMinutes = settings.minEventMinutes,
        dndMode = settings.dndMode,
        dndStartOffsetMinutes = settings.dndStartOffsetMinutes,
        preDndNotificationEnabled = settings.preDndNotificationEnabled,

        // From runtime state snapshot
        dndSetByApp = runtimeState.dndSetByApp,
        activeWindowEndMs = runtimeState.activeWindowEndMs,
        userSuppressedUntilMs = runtimeState.userSuppressedUntilMs,
        manualDndUntilMs = runtimeState.manualDndUntilMs,

        // System state (still need individual calls)
        hasCalendarPermission = hasCalendarPermission(context),
        hasPolicyAccess = dndController.hasPolicyAccess(),
        hasExactAlarms = alarmScheduler.canScheduleExactAlarms(),
        systemDndIsOn = dndController.isDndOn(),
        currentSystemFilter = dndController.getCurrentFilter()
    )

    // ... rest of engine runner
}
```

### Implementation Checklist

- [ ] Create `SettingsSnapshot` data class in `SettingsStore.kt`
- [ ] Add `getSnapshot()` method to `SettingsStore`
- [ ] Create `RuntimeStateSnapshot` data class in `RuntimeStateStore.kt`
- [ ] Add `getSnapshot()` method to `RuntimeStateStore`
- [ ] Update `EngineRunner.kt` to use snapshots
- [ ] Update any tests that mock individual Flow reads
- [ ] Run `./gradlew test`
- [ ] Benchmark before/after (optional but nice to have)

### Testing Notes

Performance improvement may be small but adds up:
- Before: ~10 separate DataStore reads per engine run
- After: 2 DataStore reads per engine run

Verify:
1. All settings correctly read in snapshot
2. All runtime state correctly read in snapshot
3. Engine behavior unchanged after refactor

---

## Task 6: Haptic Feedback

### Problem
Users have no tactile feedback when DND changes:
- DND silently enables/disables
- User might not notice state change
- No confirmation that automation is working

### Solution
Add optional vibration when DND state changes.

### Files to Modify

1. **`data/prefs/SettingsStore.kt`** (add setting)

```kotlin
// Add preference key
private val HAPTIC_FEEDBACK_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")

// Add Flow property
val hapticFeedbackEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
    prefs[HAPTIC_FEEDBACK_ENABLED] ?: true  // Default: enabled
}

// Add setter
suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
    dataStore.edit { prefs ->
        prefs[HAPTIC_FEEDBACK_ENABLED] = enabled
    }
}

// Add to SettingsSnapshot
data class SettingsSnapshot(
    // ... existing fields
    val hapticFeedbackEnabled: Boolean
)

// Update getSnapshot()
suspend fun getSnapshot(): SettingsSnapshot {
    return dataStore.data.first().let { prefs ->
        SettingsSnapshot(
            // ... existing fields
            hapticFeedbackEnabled = prefs[HAPTIC_FEEDBACK_ENABLED] ?: true
        )
    }
}
```

2. **Create `util/HapticHelper.kt`**

```kotlin
package com.brunoafk.calendardnd.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.getSystemService

/**
 * Helper for providing haptic feedback on DND state changes.
 */
object HapticHelper {

    /**
     * Vibration pattern for DND enabling (two short pulses)
     */
    fun vibrateDndEnabled(context: Context) {
        vibrate(context, longArrayOf(0, 50, 50, 50))  // Two pulses
    }

    /**
     * Vibration pattern for DND disabling (single short pulse)
     */
    fun vibrateDndDisabled(context: Context) {
        vibrate(context, longArrayOf(0, 30))  // Single short pulse
    }

    private fun vibrate(context: Context, pattern: LongArray) {
        val vibrator = getVibrator(context) ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Modern vibration with amplitude control
            val amplitudes = pattern.map { if (it == 0L) 0 else VibrationEffect.DEFAULT_AMPLITUDE }.toIntArray()
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
        } else {
            // Legacy vibration
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService<VibratorManager>()?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService<Vibrator>()
        }
    }
}
```

3. **`system/alarms/EngineRunner.kt`** (add haptic feedback)

```kotlin
import com.brunoafk.calendardnd.util.HapticHelper

suspend fun runEngine(context: Context, trigger: Trigger) {
    // ... existing code to get settings snapshot ...

    val output = engine.run(input)
    val decision = output.decision

    // Apply DND changes with haptic feedback
    if (decision.shouldEnableDnd) {
        val success = dndController.enableDnd(settings.dndMode)
        if (success && settings.hapticFeedbackEnabled) {
            HapticHelper.vibrateDndEnabled(context)
        }
    } else if (decision.shouldDisableDnd) {
        val success = dndController.disableDnd()
        if (success && settings.hapticFeedbackEnabled) {
            HapticHelper.vibrateDndDisabled(context)
        }
    }

    // ... rest of engine runner
}
```

4. **`ui/screens/SettingsScreen.kt`** (add UI toggle)

```kotlin
// In the settings list, add:
item {
    SwitchPreference(
        title = stringResource(R.string.settings_haptic_feedback),
        subtitle = stringResource(R.string.settings_haptic_feedback_desc),
        checked = hapticFeedbackEnabled,
        onCheckedChange = {
            scope.launch {
                settingsStore.setHapticFeedbackEnabled(it)
            }
        }
    )
}
```

5. **`res/values/strings.xml`** (add strings)

```xml
<string name="settings_haptic_feedback">Haptic feedback</string>
<string name="settings_haptic_feedback_desc">Vibrate when DND turns on/off</string>
```

6. **Add translations** to `values-de/strings.xml`, `values-hr/strings.xml`, etc.

### Implementation Checklist

- [ ] Add `HAPTIC_FEEDBACK_ENABLED` preference to `SettingsStore.kt`
- [ ] Add to `SettingsSnapshot` data class
- [ ] Create `util/HapticHelper.kt`
- [ ] Update `EngineRunner.kt` to call haptic helper
- [ ] Add toggle UI in `SettingsScreen.kt`
- [ ] Add string resources (all 5 languages)
- [ ] Test vibration patterns on real device
- [ ] Verify no vibration when setting disabled
- [ ] Run `./gradlew test`

### Testing Notes

Test on real device:
1. Enable haptic feedback → DND enables → feel two pulses
2. DND disables → feel single pulse
3. Disable haptic feedback → no vibration
4. Test during silent mode (should still vibrate)

---

## Task 7: Enhanced Quick Settings Tile

### Problem
Current tile only shows on/off status:
- No information about current meeting
- No indication of when DND will end
- User has to open app to see details

### Solution
Show meeting info and time remaining in tile subtitle.

### Files to Modify

1. **`ui/tiles/AutomationTileService.kt`**

```kotlin
package com.brunoafk.calendardnd.ui.tiles

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.data.prefs.RuntimeStateStore
import com.brunoafk.calendardnd.data.prefs.SettingsStore
import com.brunoafk.calendardnd.data.calendar.CalendarRepository
import com.brunoafk.calendardnd.system.alarms.EngineRunner
import com.brunoafk.calendardnd.domain.model.Trigger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class AutomationTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "AutomationTileService"
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            val settingsStore = SettingsStore(applicationContext)
            val currentState = settingsStore.automationEnabled.first()
            settingsStore.setAutomationEnabled(!currentState)

            // Trigger engine run to apply change
            EngineRunner.runEngine(applicationContext, Trigger.TILE_TOGGLE)

            // Update tile UI
            withContext(Dispatchers.Main) {
                updateTileState()
            }
        }
    }

    private fun updateTileState() {
        scope.launch {
            val settingsStore = SettingsStore(applicationContext)
            val runtimeStateStore = RuntimeStateStore(applicationContext)

            val automationEnabled = settingsStore.automationEnabled.first()
            val dndSetByApp = runtimeStateStore.dndSetByApp.first()
            val activeWindowEndMs = runtimeStateStore.activeWindowEndMs.first()

            val tileInfo = buildTileInfo(
                automationEnabled = automationEnabled,
                dndSetByApp = dndSetByApp,
                activeWindowEndMs = activeWindowEndMs
            )

            withContext(Dispatchers.Main) {
                qsTile?.apply {
                    state = if (automationEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                    label = getString(R.string.app_name)

                    // Subtitle (Android 10+)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        subtitle = tileInfo.subtitle
                    }

                    // Content description for accessibility
                    contentDescription = tileInfo.contentDescription

                    // Icon based on state
                    icon = Icon.createWithResource(
                        applicationContext,
                        if (dndSetByApp) R.drawable.ic_dnd_on else R.drawable.ic_dnd_off
                    )

                    updateTile()
                }
            }
        }
    }

    private suspend fun buildTileInfo(
        automationEnabled: Boolean,
        dndSetByApp: Boolean,
        activeWindowEndMs: Long
    ): TileInfo {
        if (!automationEnabled) {
            return TileInfo(
                subtitle = getString(R.string.tile_disabled),
                contentDescription = getString(R.string.tile_disabled_desc)
            )
        }

        if (!dndSetByApp || activeWindowEndMs <= 0) {
            // Try to get next meeting info
            val nextMeeting = getNextMeetingInfo()
            return if (nextMeeting != null) {
                TileInfo(
                    subtitle = "Next: ${nextMeeting.timeUntil}",
                    contentDescription = "DND will activate in ${nextMeeting.timeUntil} for ${nextMeeting.title}"
                )
            } else {
                TileInfo(
                    subtitle = getString(R.string.tile_enabled),
                    contentDescription = getString(R.string.tile_enabled_desc)
                )
            }
        }

        // DND is active
        val now = System.currentTimeMillis()
        val remainingMs = activeWindowEndMs - now
        val remainingMinutes = (remainingMs / 60000).toInt()

        val remainingText = when {
            remainingMinutes < 1 -> "< 1 min"
            remainingMinutes < 60 -> "$remainingMinutes min"
            else -> {
                val hours = remainingMinutes / 60
                val mins = remainingMinutes % 60
                if (mins > 0) "${hours}h ${mins}m" else "${hours}h"
            }
        }

        // Try to get current meeting title
        val currentMeetingTitle = getCurrentMeetingTitle()

        return TileInfo(
            subtitle = "DND: $remainingText",
            contentDescription = buildString {
                append("Do Not Disturb active for $remainingText")
                if (currentMeetingTitle != null) {
                    append(" during $currentMeetingTitle")
                }
            }
        )
    }

    private suspend fun getNextMeetingInfo(): NextMeetingInfo? {
        return try {
            val settingsStore = SettingsStore(applicationContext)
            val settings = settingsStore.getSnapshot()

            val calendarRepository = CalendarRepository(applicationContext)
            val nextInstance = calendarRepository.getNextInstance(
                now = System.currentTimeMillis(),
                selectedCalendarIds = settings.selectedCalendarIds,
                busyOnly = settings.busyOnly,
                ignoreAllDay = settings.ignoreAllDay,
                minEventMinutes = settings.minEventMinutes
            )

            nextInstance?.let {
                val minutesUntil = ((it.begin - System.currentTimeMillis()) / 60000).toInt()
                val timeUntil = when {
                    minutesUntil < 1 -> "< 1 min"
                    minutesUntil < 60 -> "$minutesUntil min"
                    minutesUntil < 1440 -> "${minutesUntil / 60}h"
                    else -> "${minutesUntil / 1440}d"
                }
                NextMeetingInfo(
                    title = it.title ?: "Meeting",
                    timeUntil = timeUntil
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun getCurrentMeetingTitle(): String? {
        return try {
            val settingsStore = SettingsStore(applicationContext)
            val settings = settingsStore.getSnapshot()

            val calendarRepository = CalendarRepository(applicationContext)
            val activeInstances = calendarRepository.getActiveInstances(
                now = System.currentTimeMillis(),
                selectedCalendarIds = settings.selectedCalendarIds,
                busyOnly = settings.busyOnly,
                ignoreAllDay = settings.ignoreAllDay,
                minEventMinutes = settings.minEventMinutes
            )

            activeInstances.firstOrNull()?.title
        } catch (e: Exception) {
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private data class TileInfo(
        val subtitle: String,
        val contentDescription: String
    )

    private data class NextMeetingInfo(
        val title: String,
        val timeUntil: String
    )
}
```

2. **`res/values/strings.xml`** (add strings)

```xml
<string name="tile_disabled">Disabled</string>
<string name="tile_disabled_desc">Calendar DND automation is disabled</string>
<string name="tile_enabled">Enabled</string>
<string name="tile_enabled_desc">Calendar DND automation is enabled</string>
```

3. **Create tile icons** (if not existing)

```
res/drawable/ic_dnd_on.xml   - DND active icon (filled bell with slash)
res/drawable/ic_dnd_off.xml  - DND inactive icon (outline bell)
```

4. **Add translations** to all language files

### Implementation Checklist

- [ ] Update `AutomationTileService.kt` with enhanced state display
- [ ] Add `TileInfo` and `NextMeetingInfo` data classes
- [ ] Add `buildTileInfo()` method with time calculations
- [ ] Add `getNextMeetingInfo()` method
- [ ] Add `getCurrentMeetingTitle()` method
- [ ] Add string resources (all 5 languages)
- [ ] Create/verify tile icons exist
- [ ] Test tile shows correct states:
  - [ ] Automation disabled
  - [ ] Automation enabled, no upcoming meeting
  - [ ] Automation enabled, meeting in X min/hours
  - [ ] DND active with time remaining
- [ ] Test accessibility (content descriptions)
- [ ] Run `./gradlew test`

### Testing Notes

Test all tile states:
1. **Disabled**: Shows "Disabled"
2. **Enabled, no meetings**: Shows "Enabled" or "Next: 2h"
3. **Enabled, meeting soon**: Shows "Next: 15 min"
4. **DND active**: Shows "DND: 45 min" with countdown
5. Tap to toggle → state changes correctly
6. Verify subtitle updates when tile becomes visible

---

## Implementation Order

Recommended order based on dependencies and impact:

1. **Task 1: Centralize Constants** - Foundation for other tasks
2. **Task 2: Specific Exceptions** - Improves debugging for all tasks
3. **Task 5: Cache DataStore Reads** - Required by Task 7 (tile uses snapshots)
4. **Task 3: Query Timeout** - Independent, improves reliability
5. **Task 4: Enhanced Debug Logs** - Uses constants and exception handling
6. **Task 6: Haptic Feedback** - Independent, quick win
7. **Task 7: Enhanced Tile** - Uses cached reads, most complex

---

## Notes

- All tasks should include unit tests where applicable
- Run full test suite after each task: `./gradlew test`
- Test on real Samsung device for Samsung-specific behavior
- Update `CHANGELOG.md` or release notes after completing tasks

---

# UX Improvement Tasks

---

## Task 8: Permission Explanations

### Problem
Permission cards request access but don't fully explain WHY the permission is needed:
- Users hesitant to grant permissions they don't understand
- Generic "permission required" messages don't build trust
- No visual connection between permission and benefit

### Solution
Add benefit-focused copy and contextual illustrations to permission request screens.

### Files to Modify

1. **`ui/screens/OnboardingScreen.kt`**

```kotlin
@Composable
fun PermissionExplanationCard(
    permissionType: PermissionType,
    isGranted: Boolean,
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Icon representing the permission benefit
            Icon(
                imageVector = permissionType.icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Title - what we're asking for
            Text(
                text = stringResource(permissionType.titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Explanation - WHY we need it (benefit-focused)
            Text(
                text = stringResource(permissionType.explanationRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isGranted) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.permission_granted),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.grant_permission))
                }
            }
        }
    }
}

enum class PermissionType(
    val icon: ImageVector,
    @StringRes val titleRes: Int,
    @StringRes val explanationRes: Int
) {
    CALENDAR(
        icon = Icons.Default.CalendarMonth,
        titleRes = R.string.permission_calendar_title,
        explanationRes = R.string.permission_calendar_explanation
    ),
    DND_POLICY(
        icon = Icons.Default.DoNotDisturb,
        titleRes = R.string.permission_dnd_title,
        explanationRes = R.string.permission_dnd_explanation
    ),
    NOTIFICATIONS(
        icon = Icons.Default.Notifications,
        titleRes = R.string.permission_notifications_title,
        explanationRes = R.string.permission_notifications_explanation
    ),
    EXACT_ALARMS(
        icon = Icons.Default.Alarm,
        titleRes = R.string.permission_alarms_title,
        explanationRes = R.string.permission_alarms_explanation
    )
}
```

2. **`res/values/strings.xml`** (add benefit-focused strings)

```xml
<!-- Permission explanations - benefit focused -->
<string name="permission_calendar_title">Calendar Access</string>
<string name="permission_calendar_explanation">We read your calendar to know when your meetings start and end. This lets us automatically silence your phone so you\'re never interrupted during important calls.</string>

<string name="permission_dnd_title">Do Not Disturb Control</string>
<string name="permission_dnd_explanation">This lets us turn on Do Not Disturb when meetings start and turn it off when they end. You stay in control—we only change DND during your scheduled events.</string>

<string name="permission_notifications_title">Notifications</string>
<string name="permission_notifications_explanation">We\'ll send you a heads-up 5 minutes before DND activates, so you\'re never caught off guard. You can disable this anytime.</string>

<string name="permission_alarms_title">Precise Timing</string>
<string name="permission_alarms_explanation">This ensures DND turns on and off at exactly the right moment—not a minute early or late. Without this, timing may vary by up to 15 minutes.</string>

<string name="permission_granted">Permission granted</string>
<string name="grant_permission">Grant Permission</string>
```

3. **Add translations** to all language files (de, hr, it, ko)

### Implementation Checklist

- [ ] Create `PermissionType` enum with icons and string resources
- [ ] Create `PermissionExplanationCard` composable
- [ ] Update `OnboardingScreen.kt` to use new permission cards
- [ ] Add all string resources in `strings.xml`
- [ ] Add translations for all 5 languages
- [ ] Test permission flow end-to-end
- [ ] Verify explanations are clear and benefit-focused
- [ ] Run `./gradlew test`

### Testing Notes

1. Fresh install → onboarding shows clear explanations
2. Each permission card explains the BENEFIT, not just requirement
3. Granted permissions show checkmark, not button
4. Language switching shows translated explanations

---

## Task 9: Clearer Status Indicator

### Problem
Current status is presented in the same card style as clickable elements:
- Users think status cards are clickable when they're not
- No visual hierarchy—everything looks the same
- Hard to quickly scan current state
- Status should be immediately obvious within 1 second

### Solution
Create a distinct, non-clickable status display at the top of StatusScreen with clear visual differentiation.

### Design Approach

```
┌─────────────────────────────────────────┐
│                                         │
│     🟢  AUTO-SILENCE ACTIVE            │  ← Large, colored status
│                                         │
│     Currently in: Team Standup          │  ← Context info
│     DND ends in 23 minutes              │
│                                         │
└─────────────────────────────────────────┘
         ↑ Non-clickable, distinct style

┌─────────────────────────────────────────┐
│  ⚙️  Settings                      →   │  ← Clickable card (has arrow)
└─────────────────────────────────────────┘
```

### Files to Create

**`ui/components/StatusBanner.kt`**

```kotlin
package com.brunoafk.calendardnd.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.brunoafk.calendardnd.R

/**
 * Current automation status - NOT clickable.
 * Visually distinct from action cards.
 */
@Composable
fun StatusBanner(
    state: AutomationState,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, statusColor, statusIcon) = when (state) {
        is AutomationState.Disabled -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.outline,
            "⏸️"
        )
        is AutomationState.Enabled -> Triple(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.primary,
            "✓"
        )
        is AutomationState.DndActive -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.tertiary,
            "🔕"
        )
        is AutomationState.MissingPermissions -> Triple(
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.error,
            "⚠️"
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Status indicator - large and clear
            Text(
                text = statusIcon,
                style = MaterialTheme.typography.displaySmall
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Main status text
            Text(
                text = state.statusText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = statusColor
            )

            // Context info (meeting name, time remaining, etc.)
            state.contextText?.let { context ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = context,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Secondary info (when DND ends, next meeting, etc.)
            state.secondaryText?.let { secondary ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

sealed class AutomationState(
    val statusText: String,
    val contextText: String? = null,
    val secondaryText: String? = null
) {
    class Disabled : AutomationState(
        statusText = "Auto-Silence Paused"
    )

    class Enabled(
        nextMeetingIn: String? = null
    ) : AutomationState(
        statusText = "Auto-Silence Ready",
        contextText = nextMeetingIn?.let { "Next meeting in $it" },
        secondaryText = "DND will activate automatically"
    )

    class DndActive(
        meetingTitle: String?,
        endsIn: String
    ) : AutomationState(
        statusText = "Do Not Disturb Active",
        contextText = meetingTitle?.let { "In: $it" },
        secondaryText = "Ends in $endsIn"
    )

    class MissingPermissions(
        missingCount: Int
    ) : AutomationState(
        statusText = "Setup Required",
        contextText = "$missingCount permission${if (missingCount > 1) "s" else ""} needed",
        secondaryText = "Tap below to complete setup"
    )
}
```

### Files to Modify

1. **`ui/screens/StatusScreen.kt`**

```kotlin
@Composable
fun StatusScreen(
    navController: NavController,
    settingsStore: SettingsStore,
    runtimeStateStore: RuntimeStateStore
) {
    val automationEnabled by settingsStore.automationEnabled.collectAsState(initial = true)
    val dndSetByApp by runtimeStateStore.dndSetByApp.collectAsState(initial = false)
    val activeWindowEndMs by runtimeStateStore.activeWindowEndMs.collectAsState(initial = 0L)

    // Determine current state
    val automationState = remember(automationEnabled, dndSetByApp, activeWindowEndMs) {
        when {
            !hasRequiredPermissions() -> AutomationState.MissingPermissions(
                missingCount = countMissingPermissions()
            )
            !automationEnabled -> AutomationState.Disabled()
            dndSetByApp && activeWindowEndMs > 0 -> {
                val remaining = formatTimeRemaining(activeWindowEndMs)
                AutomationState.DndActive(
                    meetingTitle = currentMeetingTitle,
                    endsIn = remaining
                )
            }
            else -> AutomationState.Enabled(
                nextMeetingIn = nextMeetingTimeUntil
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Non-clickable status banner at top
        StatusBanner(
            state = automationState,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Clickable action cards below (with arrows/chevrons)
        ActionCard(
            icon = Icons.Default.Settings,
            title = stringResource(R.string.settings),
            onClick = { navController.navigate(AppRoutes.SETTINGS) },
            showChevron = true  // Visual indicator it's clickable
        )

        Spacer(modifier = Modifier.height(12.dp))

        ActionCard(
            icon = Icons.Default.Article,
            title = stringResource(R.string.debug_logs),
            onClick = { navController.navigate(AppRoutes.DEBUG_LOGS) },
            showChevron = true
        )

        // ... more action cards
    }
}
```

2. **`ui/components/ActionCard.kt`** (ensure clickable cards look different)

```kotlin
@Composable
fun ActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    showChevron: Boolean = true,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Chevron indicates clickability
            if (showChevron) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
```

### Implementation Checklist

- [ ] Create `ui/components/StatusBanner.kt`
- [ ] Create `AutomationState` sealed class with all states
- [ ] Update `StatusScreen.kt` to use StatusBanner
- [ ] Create/update `ActionCard.kt` with chevron indicator
- [ ] Ensure visual distinction between status (not clickable) and actions (clickable)
- [ ] Add proper colors for each state (disabled, enabled, active, error)
- [ ] Test all 4 states display correctly
- [ ] Verify status banner is NOT clickable
- [ ] Verify action cards ARE clickable with visual feedback
- [ ] Run `./gradlew test`

### Testing Notes

1. **Visual hierarchy**: Status banner should stand out as informational, not actionable
2. **State transitions**: Animate smoothly between states
3. **Clickability**: Users should NOT try to tap the status banner
4. **Action cards**: Should have clear tap feedback and chevron
5. **Quick scan**: User should know app state within 1 second of opening

---

## Task 10: Settings Grouping

### Problem
Settings may appear as a flat list without clear organization:
- Hard to find specific settings
- No logical grouping of related options
- Overwhelming when many settings are visible

### Solution
Group related settings with section headers and visual separation.

### Design Structure

```
AUTOMATION
├─ Enable auto-silence        [ON]
├─ DND mode              [Priority]
└─ Start offset              [0 min]

CALENDAR FILTERS
├─ Calendars              [3 of 5]
├─ Busy events only          [ON]
├─ Skip all-day events       [ON]
└─ Minimum duration        [10 min]

NOTIFICATIONS
├─ Pre-DND warning           [ON]
└─ Haptic feedback           [ON]

ABOUT
├─ Help
├─ Debug tools
└─ Version 1.1.0
```

### Files to Create

**`ui/components/SettingsSection.kt`**

```kotlin
package com.brunoafk.calendardnd.ui.components

@Composable
fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier) {
        // Section header
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(
                start = 16.dp,
                top = 24.dp,
                bottom = 8.dp
            )
        )

        // Section content card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
fun SettingsNavigationRow(
    title: String,
    subtitle: String? = null,
    value: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        value?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}
```

### Files to Modify

**`ui/screens/SettingsScreen.kt`**

```kotlin
@Composable
fun SettingsScreen(
    navController: NavController,
    settingsStore: SettingsStore
) {
    val automationEnabled by settingsStore.automationEnabled.collectAsState(initial = true)
    val dndMode by settingsStore.dndMode.collectAsState(initial = DndMode.PRIORITY)
    val busyOnly by settingsStore.busyOnly.collectAsState(initial = true)
    // ... other state

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // ═══════════════════════════════════════════════════════════
        // AUTOMATION SECTION
        // ═══════════════════════════════════════════════════════════
        item {
            SettingsSection(title = stringResource(R.string.settings_section_automation)) {
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_automation),
                    subtitle = stringResource(R.string.settings_automation_desc),
                    checked = automationEnabled,
                    onCheckedChange = { /* toggle */ }
                )

                SettingsDivider()

                SettingsNavigationRow(
                    title = stringResource(R.string.settings_dnd_mode),
                    value = dndMode.displayName,
                    onClick = { navController.navigate(AppRoutes.DND_MODE) }
                )

                SettingsDivider()

                SettingsNavigationRow(
                    title = stringResource(R.string.settings_start_offset),
                    value = formatOffset(offsetMinutes),
                    onClick = { /* show offset picker */ }
                )
            }
        }

        // ═══════════════════════════════════════════════════════════
        // CALENDAR FILTERS SECTION
        // ═══════════════════════════════════════════════════════════
        item {
            SettingsSection(title = stringResource(R.string.settings_section_filters)) {
                SettingsNavigationRow(
                    title = stringResource(R.string.settings_calendars),
                    value = "${selectedCount} of ${totalCount}",
                    onClick = { navController.navigate(AppRoutes.CALENDAR_PICKER) }
                )

                SettingsDivider()

                SettingsSwitchRow(
                    title = stringResource(R.string.settings_busy_only),
                    subtitle = stringResource(R.string.settings_busy_only_desc),
                    checked = busyOnly,
                    onCheckedChange = { /* toggle */ }
                )

                SettingsDivider()

                SettingsSwitchRow(
                    title = stringResource(R.string.settings_ignore_all_day),
                    checked = ignoreAllDay,
                    onCheckedChange = { /* toggle */ }
                )

                SettingsDivider()

                SettingsNavigationRow(
                    title = stringResource(R.string.settings_min_duration),
                    value = "$minEventMinutes min",
                    onClick = { /* show duration picker */ }
                )
            }
        }

        // ═══════════════════════════════════════════════════════════
        // NOTIFICATIONS SECTION
        // ═══════════════════════════════════════════════════════════
        item {
            SettingsSection(title = stringResource(R.string.settings_section_notifications)) {
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_pre_dnd_notification),
                    subtitle = stringResource(R.string.settings_pre_dnd_notification_desc),
                    checked = preDndEnabled,
                    onCheckedChange = { /* toggle */ }
                )

                SettingsDivider()

                SettingsSwitchRow(
                    title = stringResource(R.string.settings_haptic_feedback),
                    subtitle = stringResource(R.string.settings_haptic_feedback_desc),
                    checked = hapticEnabled,
                    onCheckedChange = { /* toggle */ }
                )
            }
        }

        // ═══════════════════════════════════════════════════════════
        // ABOUT SECTION
        // ═══════════════════════════════════════════════════════════
        item {
            SettingsSection(title = stringResource(R.string.settings_section_about)) {
                SettingsNavigationRow(
                    title = stringResource(R.string.settings_help),
                    onClick = { navController.navigate(AppRoutes.HELP) }
                )

                SettingsDivider()

                SettingsNavigationRow(
                    title = stringResource(R.string.settings_language),
                    value = currentLanguageName,
                    onClick = { navController.navigate(AppRoutes.LANGUAGE_SETTINGS) }
                )

                SettingsDivider()

                SettingsNavigationRow(
                    title = stringResource(R.string.settings_debug_tools),
                    onClick = { navController.navigate(AppRoutes.DEBUG_TOOLS) }
                )

                SettingsDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_version),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = BuildConfig.VERSION_NAME,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
```

### String Resources

```xml
<!-- Settings section headers -->
<string name="settings_section_automation">Automation</string>
<string name="settings_section_filters">Calendar Filters</string>
<string name="settings_section_notifications">Notifications</string>
<string name="settings_section_about">About</string>
```

### Implementation Checklist

- [ ] Create `ui/components/SettingsSection.kt`
- [ ] Create `SettingsSwitchRow` composable
- [ ] Create `SettingsNavigationRow` composable
- [ ] Create `SettingsDivider` composable
- [ ] Refactor `SettingsScreen.kt` to use sections
- [ ] Add section header strings (all 5 languages)
- [ ] Ensure consistent spacing and visual rhythm
- [ ] Test scrolling behavior with sections
- [ ] Verify all settings still function correctly
- [ ] Run `./gradlew test`

### Testing Notes

1. Settings should be easy to scan and find
2. Related settings grouped logically
3. Section headers stand out but don't dominate
4. Smooth scrolling through all sections
5. Visual consistency across light/dark modes

---

## Task 11: Inline Setting Explanations

### Problem
Some settings are confusing without explanation:
- "Start offset" - what does positive/negative mean?
- "Busy events only" - what's a busy vs free event?
- Users need to guess or navigate to help

### Solution
Add subtle help text below complex settings, shown inline.

### Files to Modify

**`ui/components/SettingsSection.kt`** (update existing components)

```kotlin
@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String? = null,
    helpText: String? = null,  // NEW: inline explanation
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }

        // Inline help text
        helpText?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SettingsValueRow(
    title: String,
    value: String,
    helpText: String? = null,  // Dynamic help based on value
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Dynamic help text
        helpText?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

**Usage in `SettingsScreen.kt`**:

```kotlin
// Start offset with dynamic explanation
val offsetHelpText = when {
    offsetMinutes > 0 -> "DND will start $offsetMinutes minutes after your meeting begins"
    offsetMinutes < 0 -> "DND will start ${-offsetMinutes} minutes before your meeting begins"
    else -> "DND will start exactly when your meeting begins"
}

SettingsValueRow(
    title = stringResource(R.string.settings_start_offset),
    value = formatOffset(offsetMinutes),
    helpText = offsetHelpText,
    onClick = { /* show picker */ }
)

// Busy only with static explanation
SettingsSwitchRow(
    title = stringResource(R.string.settings_busy_only),
    checked = busyOnly,
    helpText = if (!busyOnly) stringResource(R.string.settings_busy_only_help) else null,
    onCheckedChange = { /* toggle */ }
)
```

**String resources**:

```xml
<string name="settings_busy_only_help">Events marked as "Free" or "Tentative" will also trigger DND</string>
<string name="settings_min_duration_help">Shorter meetings won\'t trigger DND. Useful to ignore quick check-ins.</string>
```

### Implementation Checklist

- [ ] Update `SettingsSwitchRow` to support `helpText` parameter
- [ ] Update `SettingsValueRow` to support dynamic `helpText`
- [ ] Add help text for "Start offset" (dynamic based on value)
- [ ] Add help text for "Busy events only"
- [ ] Add help text for "Minimum duration"
- [ ] Add help strings (all 5 languages)
- [ ] Style help text to be subtle but readable
- [ ] Test that help text doesn't clutter UI
- [ ] Run `./gradlew test`

### Testing Notes

1. Help text should be subtle, not dominant
2. Dynamic help (offset) updates when value changes
3. Some settings show help only in certain states
4. Help text visible in both light/dark modes
5. Text is readable but clearly secondary

---

## Task 12: Permission Status Display

### Problem
Users can't easily see which permissions are granted:
- No way to diagnose issues without deep diving
- Permission problems cause automation to fail silently
- Users don't know what to fix

### Solution
Add a permission status section in Settings with quick-fix links.

### Files to Create

**`ui/components/PermissionStatusCard.kt`**

```kotlin
package com.brunoafk.calendardnd.ui.components

@Composable
fun PermissionStatusCard(
    permissions: List<PermissionStatus>,
    onFixPermission: (PermissionStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.permissions_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

            permissions.forEach { permission ->
                PermissionStatusRow(
                    permission = permission,
                    onFix = { onFixPermission(permission) }
                )

                if (permission != permissions.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(
    permission: PermissionStatus,
    onFix: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status icon
        Icon(
            imageVector = when (permission.state) {
                PermissionState.GRANTED -> Icons.Default.CheckCircle
                PermissionState.DENIED -> Icons.Default.Cancel
                PermissionState.LIMITED -> Icons.Default.Warning
            },
            contentDescription = null,
            tint = when (permission.state) {
                PermissionState.GRANTED -> MaterialTheme.colorScheme.primary
                PermissionState.DENIED -> MaterialTheme.colorScheme.error
                PermissionState.LIMITED -> MaterialTheme.colorScheme.tertiary
            },
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Permission name
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = permission.name,
                style = MaterialTheme.typography.bodyMedium
            )
            if (permission.state != PermissionState.GRANTED) {
                Text(
                    text = permission.statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Fix button (if not granted)
        if (permission.state != PermissionState.GRANTED) {
            TextButton(
                onClick = onFix,
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.fix),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

data class PermissionStatus(
    val type: PermissionType,
    val name: String,
    val state: PermissionState,
    val statusMessage: String
)

enum class PermissionState {
    GRANTED,
    DENIED,
    LIMITED  // For exact alarms - partial functionality
}
```

**Usage in `SettingsScreen.kt`**:

```kotlin
// In Settings, add permission status section
item {
    SettingsSection(title = stringResource(R.string.settings_section_permissions)) {
        val permissions = remember {
            listOf(
                PermissionStatus(
                    type = PermissionType.CALENDAR,
                    name = "Calendar",
                    state = if (hasCalendarPermission) PermissionState.GRANTED else PermissionState.DENIED,
                    statusMessage = "Required to read events"
                ),
                PermissionStatus(
                    type = PermissionType.DND_POLICY,
                    name = "DND Control",
                    state = if (hasPolicyAccess) PermissionState.GRANTED else PermissionState.DENIED,
                    statusMessage = "Required to change DND"
                ),
                PermissionStatus(
                    type = PermissionType.EXACT_ALARMS,
                    name = "Precise Timing",
                    state = when {
                        Build.VERSION.SDK_INT < 31 -> PermissionState.GRANTED
                        canScheduleExactAlarms -> PermissionState.GRANTED
                        else -> PermissionState.LIMITED
                    },
                    statusMessage = "DND timing may vary ±15 min"
                ),
                PermissionStatus(
                    type = PermissionType.NOTIFICATIONS,
                    name = "Notifications",
                    state = if (hasNotificationPermission) PermissionState.GRANTED else PermissionState.DENIED,
                    statusMessage = "Required for pre-DND alerts"
                )
            )
        }

        permissions.forEach { permission ->
            PermissionStatusRow(
                permission = permission,
                onFix = { handlePermissionFix(permission.type) }
            )
            if (permission != permissions.last()) {
                SettingsDivider()
            }
        }
    }
}
```

### Implementation Checklist

- [ ] Create `PermissionStatus` data class
- [ ] Create `PermissionState` enum
- [ ] Create `PermissionStatusRow` composable
- [ ] Add permission section to `SettingsScreen.kt`
- [ ] Implement `handlePermissionFix()` to open correct settings
- [ ] Add string resources (all 5 languages)
- [ ] Test all permission states display correctly
- [ ] Test "Fix" buttons open correct system settings
- [ ] Run `./gradlew test`

### Testing Notes

1. All four permissions shown with correct state
2. "Fix" button opens correct settings screen
3. Limited state (exact alarms) shows warning, not error
4. States update when returning from settings
5. Clean visual with clear status indicators

---

## Task 13: Dark Mode Fixes

### Problem
Dark mode may have visual issues:
- Insufficient contrast in some areas
- Elevation shadows not visible
- Some icons hard to see
- Colors not optimized for dark backgrounds

### Solution
Audit and fix dark mode colors, contrast, and elevation.

### Files to Modify

1. **`ui/theme/Theme.kt`**

```kotlin
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),           // Lighter blue for dark mode
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF004881),
    onPrimaryContainer = Color(0xFFD1E4FF),

    secondary = Color(0xFFBBC7DB),
    onSecondary = Color(0xFF253140),
    secondaryContainer = Color(0xFF3B4858),
    onSecondaryContainer = Color(0xFFD7E3F7),

    tertiary = Color(0xFFD5BDE2),           // For DND active state
    onTertiary = Color(0xFF392946),
    tertiaryContainer = Color(0xFF51405E),
    onTertiaryContainer = Color(0xFFF2DAFF),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color(0xFF1A1C1E),         // True dark, not pure black
    onBackground = Color(0xFFE2E2E6),

    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE2E2E6),

    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C6CF),

    outline = Color(0xFF8D9199),
    outlineVariant = Color(0xFF43474E),

    // Ensure sufficient contrast
    inverseSurface = Color(0xFFE2E2E6),
    inverseOnSurface = Color(0xFF2F3033),
    inversePrimary = Color(0xFF0061A4)
)

// Add surface tones for elevation
@Composable
fun surfaceColorAtElevation(elevation: Dp): Color {
    return if (isSystemInDarkTheme()) {
        // In dark mode, higher elevation = lighter surface
        val alpha = ((4.5f * ln(elevation.value + 1)) / 100f).coerceIn(0f, 1f)
        MaterialTheme.colorScheme.primary.copy(alpha = alpha)
            .compositeOver(MaterialTheme.colorScheme.surface)
    } else {
        MaterialTheme.colorScheme.surface
    }
}
```

2. **`res/values-night/colors.xml`** (create if doesn't exist)

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Ensure sufficient contrast for dark mode -->
    <color name="status_enabled">#90CAF9</color>
    <color name="status_disabled">#8D9199</color>
    <color name="status_active">#D5BDE2</color>
    <color name="status_error">#FFB4AB</color>

    <!-- Text colors with proper contrast -->
    <color name="text_primary">#E2E2E6</color>
    <color name="text_secondary">#C3C6CF</color>
    <color name="text_hint">#8D9199</color>
</resources>
```

3. **Audit all composables for hardcoded colors**

```kotlin
// BAD - hardcoded color
Text(color = Color.Gray)

// GOOD - theme-aware color
Text(color = MaterialTheme.colorScheme.onSurfaceVariant)
```

4. **Fix elevation in dark mode**

```kotlin
// Cards should use tonal elevation in dark mode
Card(
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
    )
)
```

### Contrast Audit Checklist

Check these elements meet WCAG AA (4.5:1 for text, 3:1 for UI):

- [ ] Primary text on background
- [ ] Secondary text on background
- [ ] Primary text on cards/surfaces
- [ ] Button text on button background
- [ ] Icon visibility on all backgrounds
- [ ] Status indicator colors (green/yellow/red)
- [ ] Switch track and thumb contrast
- [ ] Divider visibility
- [ ] Help text readability

### Implementation Checklist

- [ ] Audit `Theme.kt` dark color scheme
- [ ] Create/update `values-night/colors.xml`
- [ ] Search for hardcoded `Color()` values in composables
- [ ] Update cards to use tonal elevation
- [ ] Test all screens in dark mode
- [ ] Verify contrast ratios meet WCAG AA
- [ ] Test StatusBanner in all states (dark mode)
- [ ] Test Settings sections (dark mode)
- [ ] Run `./gradlew test`

### Testing Notes

1. Toggle between light/dark mode on each screen
2. Check outdoors in bright light (dark mode)
3. Check in dim room (light mode)
4. Verify all text is readable
5. Verify all icons are visible
6. Check elevation shadows visible in dark mode

---

## Task 14: Friendlier Language

### Problem
Technical terms used throughout app:
- "Automation" - not user-friendly
- "Policy access" - confusing
- "Exact alarms" - too technical
- "Interruption filter" - meaningless to users

### Solution
Replace technical terms with user-friendly alternatives.

### Language Mapping

| Technical Term | Friendly Alternative |
|----------------|---------------------|
| Automation enabled | Auto-silence is on |
| Automation disabled | Auto-silence is paused |
| Policy access | DND control |
| Exact alarms | Precise timing |
| Interruption filter | DND mode |
| Sanity worker | (don't expose to users) |
| Engine run | (don't expose to users) |
| Boundary alarm | (don't expose to users) |
| Calendar instance | Calendar event |
| Active window | Current meeting block |

### Files to Modify

**`res/values/strings.xml`**

```xml
<!-- Main Status -->
<string name="status_enabled">Auto-Silence Ready</string>
<string name="status_disabled">Auto-Silence Paused</string>
<string name="status_active">Do Not Disturb Active</string>
<string name="status_setup_required">Setup Needed</string>

<!-- Settings -->
<string name="settings_automation">Auto-silence</string>
<string name="settings_automation_desc">Automatically silence during meetings</string>
<string name="settings_automation_on">Your phone will silence during calendar events</string>
<string name="settings_automation_off">Auto-silence is turned off</string>

<!-- Permissions (user-facing) -->
<string name="permission_calendar">Calendar access</string>
<string name="permission_dnd">DND control</string>
<string name="permission_timing">Precise timing</string>
<string name="permission_notifications">Notifications</string>

<!-- Permission explanations -->
<string name="permission_dnd_needed">We need permission to turn DND on and off</string>
<string name="permission_timing_limited">Timing may be less precise without this permission</string>

<!-- DND Modes -->
<string name="dnd_mode_priority">Priority Only</string>
<string name="dnd_mode_priority_desc">Calls from favorites and repeat callers allowed</string>
<string name="dnd_mode_silence">Total Silence</string>
<string name="dnd_mode_silence_desc">All notifications blocked</string>

<!-- Notifications -->
<string name="notification_dnd_starting">DND starting soon</string>
<string name="notification_dnd_starting_body">Your phone will silence in 5 minutes for %s</string>
<string name="notification_meeting_ended">Meeting ended</string>
<string name="notification_meeting_ended_body">Extend quiet time?</string>

<!-- Errors (user-friendly) -->
<string name="error_calendar_access">Can\'t read your calendar</string>
<string name="error_calendar_access_fix">Check that calendar permission is granted</string>
<string name="error_dnd_control">Can\'t change Do Not Disturb</string>
<string name="error_dnd_control_fix">Grant DND control permission in settings</string>

<!-- Debug logs (keep technical but clearer) -->
<string name="debug_trigger_alarm">Scheduled check</string>
<string name="debug_trigger_worker">Background check</string>
<string name="debug_trigger_boot">Device restarted</string>
<string name="debug_trigger_manual">Manual refresh</string>
```

### Implementation Checklist

- [ ] Audit all user-facing strings in `strings.xml`
- [ ] Replace technical terms with friendly alternatives
- [ ] Update all 5 language files with translations
- [ ] Update notification text
- [ ] Update error messages
- [ ] Update debug log labels (user-visible ones)
- [ ] Review onboarding flow text
- [ ] Review settings screen text
- [ ] Test full app flow with new language
- [ ] Run `./gradlew test`

### Testing Notes

1. Read all text as if you've never used the app
2. Would a non-technical user understand?
3. Are error messages actionable?
4. Do notifications make sense out of context?
5. Is the tone friendly but not childish?

---

## Task 15: Error State Improvements

### Problem
Errors show generic messages:
- "Something went wrong" - unhelpful
- No indication of what to do
- No retry options
- Silent failures confuse users

### Solution
Specific, actionable error messages with recovery options.

### Files to Create

**`ui/components/ErrorCard.kt`**

```kotlin
package com.brunoafk.calendardnd.ui.components

@Composable
fun ErrorCard(
    error: AppError,
    onPrimaryAction: () -> Unit,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = error.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = error.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    )
                }

                onDismiss?.let {
                    IconButton(onClick = it) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.dismiss),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onPrimaryAction) {
                    Text(
                        text = error.actionLabel,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

sealed class AppError(
    val title: String,
    val message: String,
    val actionLabel: String
) {
    class CalendarPermissionDenied : AppError(
        title = "Can't read calendar",
        message = "Grant calendar permission to see your meetings",
        actionLabel = "Open Settings"
    )

    class DndPermissionDenied : AppError(
        title = "Can't control DND",
        message = "Grant Do Not Disturb access to enable auto-silence",
        actionLabel = "Open Settings"
    )

    class CalendarQueryFailed : AppError(
        title = "Couldn't load calendar",
        message = "There was a problem reading your events. Try again.",
        actionLabel = "Retry"
    )

    class DndChangeFailed : AppError(
        title = "Couldn't change DND",
        message = "Something prevented DND from changing. This sometimes happens on Samsung devices.",
        actionLabel = "Try Again"
    )

    class NoCalendarsFound : AppError(
        title = "No calendars found",
        message = "Make sure you have at least one calendar synced to your device",
        actionLabel = "Open Calendar"
    )

    class NetworkError : AppError(
        title = "Connection problem",
        message = "Couldn't check for updates. Check your internet connection.",
        actionLabel = "Retry"
    )
}
```

**Usage in screens:**

```kotlin
// In StatusScreen
var currentError by remember { mutableStateOf<AppError?>(null) }

currentError?.let { error ->
    ErrorCard(
        error = error,
        onPrimaryAction = {
            when (error) {
                is AppError.CalendarPermissionDenied -> openAppSettings()
                is AppError.DndPermissionDenied -> openDndSettings()
                is AppError.CalendarQueryFailed -> refreshCalendar()
                // ... handle other errors
            }
        },
        onDismiss = { currentError = null }
    )
}
```

### Implementation Checklist

- [ ] Create `AppError` sealed class with all error types
- [ ] Create `ErrorCard` composable
- [ ] Integrate error handling in `StatusScreen`
- [ ] Integrate error handling in `SettingsScreen`
- [ ] Add error strings (all 5 languages)
- [ ] Implement action handlers for each error type
- [ ] Test all error scenarios
- [ ] Verify error cards are dismissible
- [ ] Run `./gradlew test`

### Testing Notes

1. Each error type shows appropriate message
2. Action buttons work correctly
3. Errors can be dismissed
4. Error styling matches app theme
5. Samsung-specific error message is helpful

---

## Task 16: Degraded Mode Warning Banner

### Problem
When exact alarms aren't available, user may not know:
- Timing could be off by up to 15 minutes
- No clear indication of degraded state
- User might think app is broken

### Solution
Persistent but dismissible banner when running in degraded mode.

### Files to Create

**`ui/components/WarningBanner.kt`**

```kotlin
package com.brunoafk.calendardnd.ui.components

@Composable
fun WarningBanner(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f)
            )

            TextButton(
                onClick = onAction,
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.dismiss),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
```

**Usage in `StatusScreen.kt`:**

```kotlin
@Composable
fun StatusScreen(...) {
    val canScheduleExactAlarms = remember { alarmScheduler.canScheduleExactAlarms() }
    var warningDismissed by rememberSaveable { mutableStateOf(false) }

    Column {
        // Show warning if in degraded mode and not dismissed
        if (!canScheduleExactAlarms && !warningDismissed) {
            WarningBanner(
                message = stringResource(R.string.warning_degraded_mode),
                actionLabel = stringResource(R.string.fix_now),
                onAction = { openAlarmSettings() },
                onDismiss = { warningDismissed = true },
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Rest of status screen...
    }
}
```

**String resources:**

```xml
<string name="warning_degraded_mode">DND timing may be less precise. Grant alarm permission for exact timing.</string>
<string name="fix_now">Fix Now</string>
```

### Implementation Checklist

- [ ] Create `WarningBanner` composable
- [ ] Add banner to `StatusScreen` for degraded mode
- [ ] Implement dismiss persistence (survives rotation, not app restart)
- [ ] Add action to open alarm permission settings
- [ ] Add string resources (all 5 languages)
- [ ] Test banner appears when exact alarms unavailable
- [ ] Test "Fix Now" opens correct settings
- [ ] Test dismiss works correctly
- [ ] Run `./gradlew test`

### Testing Notes

1. Banner appears when exact alarm permission denied
2. Banner doesn't appear when permission granted
3. Dismiss hides banner (until next app launch)
4. "Fix Now" navigates to correct settings screen
5. Banner styling matches app theme

---

## Task 17: Next Meeting Preview Card

### Problem
Users can't see what's coming next without opening calendar:
- No visibility into upcoming DND windows
- Have to trust app is working correctly
- No countdown to next meeting

### Solution
Dedicated card on StatusScreen showing next meeting with countdown.

### Files to Create

**`ui/components/NextMeetingCard.kt`**

```kotlin
package com.brunoafk.calendardnd.ui.components

@Composable
fun NextMeetingCard(
    meeting: NextMeetingInfo?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        if (meeting != null) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.next_meeting),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Meeting title
                Text(
                    text = meeting.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Time info
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Countdown
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = meeting.timeUntil,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Time range
                    Text(
                        text = meeting.timeRange,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // DND info
                Text(
                    text = stringResource(R.string.dnd_will_activate, meeting.dndStartTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // No upcoming meetings
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.EventAvailable,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.no_upcoming_meetings),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = stringResource(R.string.no_meetings_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

data class NextMeetingInfo(
    val title: String,
    val timeUntil: String,      // "in 23 min", "in 2 hours"
    val timeRange: String,      // "2:00 PM - 3:00 PM"
    val dndStartTime: String    // "2:00 PM"
)
```

**String resources:**

```xml
<string name="next_meeting">Next Meeting</string>
<string name="dnd_will_activate">DND will activate at %s</string>
<string name="no_upcoming_meetings">All clear!</string>
<string name="no_meetings_hint">No meetings in the next 7 days</string>
```

### Implementation Checklist

- [ ] Create `NextMeetingCard` composable
- [ ] Create `NextMeetingInfo` data class
- [ ] Add card to `StatusScreen` below status banner
- [ ] Implement countdown logic (updates every minute)
- [ ] Handle null state (no meetings)
- [ ] Add string resources (all 5 languages)
- [ ] Test countdown updates correctly
- [ ] Test empty state displays correctly
- [ ] Run `./gradlew test`

### Testing Notes

1. Card shows next meeting title and time
2. Countdown updates in real-time (or near real-time)
3. Empty state is friendly, not alarming
4. Time range respects system 12/24h format
5. Long meeting titles truncate properly

---

## Task 18: Empty State Design

### Problem
When there's nothing to show, screens may look broken:
- Empty calendar = blank space
- No meetings = user wonders if app works
- Missing opportunity to reassure user

### Solution
Friendly, informative empty states with helpful guidance.

### Files to Create

**`ui/components/EmptyState.kt`**

```kotlin
package com.brunoafk.calendardnd.ui.components

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    action: EmptyStateAction? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon with subtle background
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        action?.let {
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(onClick = it.onClick) {
                Icon(
                    imageVector = it.icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(it.label)
            }
        }
    }
}

data class EmptyStateAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

// Predefined empty states
object EmptyStates {
    @Composable
    fun NoMeetings(onOpenCalendar: () -> Unit) {
        EmptyState(
            icon = Icons.Default.EventAvailable,
            title = stringResource(R.string.empty_no_meetings_title),
            message = stringResource(R.string.empty_no_meetings_message),
            action = EmptyStateAction(
                label = stringResource(R.string.open_calendar),
                icon = Icons.Default.OpenInNew,
                onClick = onOpenCalendar
            )
        )
    }

    @Composable
    fun NoCalendars(onAddAccount: () -> Unit) {
        EmptyState(
            icon = Icons.Default.CalendarMonth,
            title = stringResource(R.string.empty_no_calendars_title),
            message = stringResource(R.string.empty_no_calendars_message),
            action = EmptyStateAction(
                label = stringResource(R.string.add_account),
                icon = Icons.Default.PersonAdd,
                onClick = onAddAccount
            )
        )
    }

    @Composable
    fun NoLogs() {
        EmptyState(
            icon = Icons.Default.Article,
            title = stringResource(R.string.empty_no_logs_title),
            message = stringResource(R.string.empty_no_logs_message)
        )
    }

    @Composable
    fun AutomationDisabled(onEnable: () -> Unit) {
        EmptyState(
            icon = Icons.Default.PauseCircle,
            title = stringResource(R.string.empty_disabled_title),
            message = stringResource(R.string.empty_disabled_message),
            action = EmptyStateAction(
                label = stringResource(R.string.enable_now),
                icon = Icons.Default.PlayArrow,
                onClick = onEnable
            )
        )
    }
}
```

**String resources:**

```xml
<!-- Empty states -->
<string name="empty_no_meetings_title">All Clear!</string>
<string name="empty_no_meetings_message">No meetings scheduled for the next 7 days. DND will activate automatically when you add events.</string>
<string name="open_calendar">Open Calendar</string>

<string name="empty_no_calendars_title">No Calendars Found</string>
<string name="empty_no_calendars_message">Add a Google, Outlook, or other calendar account to get started.</string>
<string name="add_account">Add Account</string>

<string name="empty_no_logs_title">No Activity Yet</string>
<string name="empty_no_logs_message">Logs will appear here as DND turns on and off.</string>

<string name="empty_disabled_title">Auto-Silence Paused</string>
<string name="empty_disabled_message">Enable auto-silence to automatically turn on DND during your calendar events.</string>
<string name="enable_now">Enable Now</string>
```

### Implementation Checklist

- [ ] Create `EmptyState` composable
- [ ] Create `EmptyStateAction` data class
- [ ] Create predefined empty states in `EmptyStates` object
- [ ] Use in `StatusScreen` (no meetings)
- [ ] Use in `CalendarPickerScreen` (no calendars)
- [ ] Use in `DebugLogScreen` (no logs)
- [ ] Add string resources (all 5 languages)
- [ ] Test all empty states display correctly
- [ ] Test action buttons work
- [ ] Run `./gradlew test`

### Testing Notes

1. Empty states are friendly, not alarming
2. Icon and colors match app theme
3. Action buttons work correctly
4. Messages are helpful and guide next steps
5. Empty states look good in both light/dark modes

---

## UX Implementation Order

Recommended order based on user impact and dependencies:

### Phase 1: Core UX (High Impact)
1. **Task 9: Clearer Status Indicator** - Most visible change
2. **Task 14: Friendlier Language** - Improves all screens
3. **Task 16: Degraded Mode Banner** - Critical user feedback

### Phase 2: Information Architecture
4. **Task 10: Settings Grouping** - Organizes complexity
5. **Task 17: Next Meeting Card** - Key information
6. **Task 18: Empty State Design** - Polishes edges

### Phase 3: Refinements
7. **Task 8: Permission Explanations** - Improves onboarding
8. **Task 11: Inline Explanations** - Reduces confusion
9. **Task 12: Permission Status** - Aids troubleshooting

### Phase 4: Polish
10. **Task 13: Dark Mode Fixes** - Visual polish
11. **Task 15: Error State Improvements** - Edge case handling

---

## Updated Implementation Order (All Tasks)

Complete recommended order including both technical and UX tasks:

1. **Task 1: Centralize Constants** - Foundation
2. **Task 2: Specific Exceptions** - Debugging foundation
3. **Task 5: Cache DataStore Reads** - Performance foundation
4. **Task 9: Clearer Status Indicator** - Highest UX impact
5. **Task 14: Friendlier Language** - Affects all text
6. **Task 10: Settings Grouping** - Organize settings
7. **Task 16: Degraded Mode Banner** - User feedback
8. **Task 3: Query Timeout** - Reliability
9. **Task 4: Enhanced Debug Logs** - Debugging
10. **Task 17: Next Meeting Card** - Key feature
11. **Task 18: Empty State Design** - Polish
12. **Task 8: Permission Explanations** - Onboarding
13. **Task 11: Inline Explanations** - Clarity
14. **Task 12: Permission Status** - Troubleshooting
15. **Task 6: Haptic Feedback** - Quick win
16. **Task 7: Enhanced Tile** - Uses cached reads
17. **Task 13: Dark Mode Fixes** - Visual polish
18. **Task 15: Error State Improvements** - Edge cases
