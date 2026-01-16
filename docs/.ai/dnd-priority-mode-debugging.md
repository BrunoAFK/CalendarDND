# DND Priority Mode Debugging Issue

**Date Created:** 2026-01-14
**Status:** Under Investigation
**Priority:** Medium

---

## Problem Summary

User reports that when using **Priority Only** DND mode, calls and Signal messages from his wife are getting through during calendar events, even though:
- No priority contacts are configured in Android system settings
- Repeat callers is disabled
- Only alarms are allowed in system DND settings

## Current Configuration

### App Settings
```
DND mode: PRIORITY
DND offset: 0 minutes
Automation: Enabled
Calendar scope: All calendars
Busy only: true
Ignore all-day: true
Min duration: 10 minutes
```

### Android System DND Settings (User Confirmed)
- **Calls:** No one
- **Messages:** No one
- **Repeat callers:** OFF
- **Alarms:** Allowed (only exception)
- **No starred/favorite contacts configured**

## What We Know (From Debug Logs)

### App is Working Correctly ✅
```
14/01/2026 09:20: System Check: DND=Off | App owns=false | Trigger=ALARM
14/01/2026 09:20: DND Enable: mode=PRIORITY | before=Off | after=Priority Only | success=true
14/01/2026 09:20: Trigger: ALARM | Active: 1 event(s), ends 09:30 |
                  DND active: 09:20 → 09:30 | Action: Enable DND (PRIORITY)
```

**Confirmed:**
- App successfully sets DND to "Priority Only" mode
- Android confirms the filter changed from "Off" to "Priority Only"
- DND window is active during calendar events (09:20 → 09:30)

## What's Getting Through

**Source of interruptions:**
1. **Phone calls** (standard phone app)
2. **Signal app messages** (making noise)

Both are getting through despite Priority Only mode being active and no priority exceptions configured.

## Theories to Investigate

### 1. Timing Issue (Most Likely)
- **Hypothesis:** Wife calls AFTER the meeting ends (after 09:30 in example)
- **Next Step:** User needs to verify exact timing:
  - Check what time meeting ends in calendar
  - Check what time wife actually calls
  - Look for "Action: Disable DND" in logs before the call

### 2. Signal App Bypassing DND
- **Hypothesis:** Signal might have its own notification priority settings
- **Investigation Needed:**
  - Check Signal app notification settings
  - Check if Signal is marked as "priority app" in Android
  - Signal might use a different notification channel that bypasses DND

### 3. OEM DND Customization
- **Unknown:** User hasn't confirmed phone manufacturer yet
- **Potential Issue:** Samsung/Xiaomi/OnePlus sometimes have custom DND implementations
- **Next Step:** Ask user for exact phone model

### 4. Android Bug/Inconsistency
- **Possibility:** Android INTERRUPTION_FILTER_PRIORITY might not be working as expected
- **Evidence Needed:** Confirm DND is actually active when wife calls (check notification bar icon)

### 5. Hidden System Settings
- **Locations to check:**
  - Do Not Disturb → **Apps** section (some apps can break through)
  - Do Not Disturb → **Custom DND rules** (might override app's DND)
  - Do Not Disturb → **Conversations** (separate from Calls/Messages)
  - Per-app notification settings (Signal, Phone app)

## Code Locations

### Where DND Mode is Set
- `DndController.kt:61-62` - `enableDnd(mode: DndMode)` function
- `EngineRunner.kt:69` - Called with `input.dndMode`
- Uses: `NotificationManager.setInterruptionFilter(mode.filterValue)`
- `DndMode.PRIORITY` maps to `INTERRUPTION_FILTER_PRIORITY`

### Enhanced Logging Added (2026-01-14)
- `EngineRunner.kt:62-64` - System state check before engine runs
- `EngineRunner.kt:68-82` - DND enable/disable logging with before/after states
- `AutomationEngine.kt:255` - Shows DND mode in main log message

### Debug Logs Screen
- `StatusScreen.kt:208-210` - Bug icon in top-right corner
- `DebugToolsScreen.kt:92-112` - New "Debug Logs" card added
- `DebugLogScreen.kt` - Full log viewer with copy/clear functions

## Recommended Next Steps

### For User
1. **Timing Test:**
   - Create test calendar event (5 min from now, 10 min duration)
   - Wait for DND to activate
   - **While DND icon is visible in notification bar**, have wife call
   - Check Debug Logs immediately after
   - Confirm: Was DND still active or had it disabled?

2. **Provide Info:**
   - Exact phone model and Android version
   - Screenshot of Signal app notification settings
   - Screenshot of Android DND settings → Apps (if that section exists)

3. **Try Total Silence Mode:**
   - Settings → DND Mode → Total Silence
   - This uses `INTERRUPTION_FILTER_NONE` which blocks EVERYTHING
   - If wife still gets through with Total Silence, something is seriously wrong

### For Development
1. **Add more detailed logging:**
   - Log when DND is disabled (we only log enable currently)
   - Add notification channel priority to logs
   - Log exact interruption filter value after setting

2. **Create DND verification tool:**
   - Add test button in Debug Tools
   - Shows current Android DND state in real-time
   - Lists all notification channels and their priorities
   - Shows which apps have DND bypass permissions

3. **Consider workaround:**
   - If PRIORITY mode is unreliable, recommend Total Silence by default
   - Add warning in app: "Priority mode allows some interruptions based on your system settings"

## Related Documentation

- Android DND API: https://developer.android.com/reference/android/app/NotificationManager#setInterruptionFilter(int)
- `INTERRUPTION_FILTER_PRIORITY`: Allow interruptions based on notification policy
- `INTERRUPTION_FILTER_NONE`: Suppress all interruptions (Total Silence)

## Test Results

### Test 1: App DND Activation (2026-01-14) ✅
- **Result:** SUCCESS
- **Evidence:** Logs show `after=Priority Only | success=true`
- **Conclusion:** App correctly sets Android DND mode

### Test 2: Actual Call During Active DND
- **Status:** PENDING
- **Required:** User needs to perform timing test
- **Goal:** Confirm DND is actually active when wife calls

---

## Notes

- User confirmed NO priority contacts in system settings
- User confirmed repeat callers is OFF
- Only alarms should be allowed through
- But phone calls and Signal messages ARE getting through
- This violates expected Android DND behavior for PRIORITY mode

**Critical Question:** Is wife calling during active DND window, or after meeting ends?

If during active DND → Android system issue or Signal bypassing DND
If after meeting ends → App working correctly, just timing confusion
