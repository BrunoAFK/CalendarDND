# Calendar DND - Acceptance Test Suite

This document contains comprehensive test scenarios for validating Calendar DND functionality, especially on Samsung devices.

## Test Environment Setup

### Required Test Device
- Samsung Galaxy S25 (or similar Samsung device)
- Android 14 or higher
- Calendar app with test events
- Device NOT connected to debugger (for realistic background behavior)

### Pre-Test Configuration
1. Install Calendar DND
2. Complete all permissions (Calendar, DND Policy, Exact Alarms)
3. Disable battery optimization for Calendar DND
4. Enable automation toggle
5. Set to default settings:
    - Busy events only: ON
    - Ignore all-day: ON
    - Min duration: 10 minutes
    - DND mode: Priority Only
    - All calendars selected

---

## Core Functional Tests

### Test 1: Active Meeting Triggers DND

**Objective**: Verify DND turns on during a meeting

**Steps**:
1. Create a calendar event starting 2 minutes from now, 15 minutes duration
2. Mark event as "Busy"
3. Wait for event to start
4. Check DND status

**Expected Result**:
- DND turns on within 30 seconds of meeting start
- Status screen shows "Current Meeting"
- Debug log shows "Action: Enable DND"
- Quick Settings shows DND enabled
- System DND indicator appears in status bar

**Actual Result**: _______________

**Pass/Fail**: _______________

---

### Test 2: DND Turns Off After Meeting Ends

**Objective**: Verify DND disables when meeting ends

**Setup**: Continue from Test 1 or create new 5-minute meeting

**Steps**:
1. Wait for meeting to end
2. Observe DND status

**Expected Result**:
- DND turns off within 30 seconds of meeting end
- Status screen shows "Current Meeting: None"
- Debug log shows "Action: Disable DND"
- System DND indicator disappears

**Actual Result**: _______________

**Pass/Fail**: _______________

---

### Test 3: Back-to-Back Meetings (No Flicker)

**Objective**: Verify DND stays on between consecutive meetings

**Steps**:
1. Create Meeting A: Starts now + 2min, ends now + 17min
2. Create Meeting B: Starts now + 17min, ends now + 32min (exactly when A ends)
3. Both marked "Busy"
4. Wait for Meeting A to start
5. Observe during transition from A to B
6. Wait for Meeting B to end

**Expected Result**:
- DND turns on at Meeting A start
- DND **stays on** continuously during both meetings
- No DND toggle between meetings
- DND turns off only after Meeting B ends
- Debug log shows single merged window ending at B's end time

**Actual Result**: _______________

**Pass/Fail**: _______________

---

### Test 4: Overlapping Meetings

**Objective**: Verify DND handles overlapping events correctly

**Steps**:
1. Create Meeting A: 10:00 - 10:30
2. Create Meeting B: 10:15 - 10:45 (overlaps A)
3. Both marked "Busy"
4. Observe at 10:00, 10:30, and 10:45

**Expected Result**:
- DND on at 10:00
- DND stays on through 10:30 (A ends but B continues)
- DND off after 10:45
- Status screen shows merged window

**Actual Result**: _______________

**Pass/Fail**: _______________

---

## Filter Tests

### Test 5: All-Day Events Ignored

**Objective**: Verify all-day events don't trigger DND (default setting)

**Steps**:
1. Create all-day event for today
2. Mark as "Busy"
3. Observe DND status throughout the day

**Expected Result**:
- DND remains off
- Event not shown in "Current Meeting"
- Debug log does not list all-day event

**Actual Result**: _______________

**Pass/Fail**: _______________

---

### Test 6: Free Events Ignored (Busy-Only Mode)

**Objective**: Verify "Free" events don't trigger when Busy-Only enabled

**Steps**:
1. Ensure "Busy events only" is ON
2. Create event starting now + 2min
3. Set availability to "Free" (not Busy)
4. Wait for event time

**Expected Result**:
- DND remains off
- Event not shown in "Current Meeting"
- Debug log shows event filtered

**Actual Result**: _______________

**Pass/Fail**: _______________

---

### Test 7: Short Events Ignored

**Objective**: Verify events shorter than minimum duration are ignored

**Steps**:
1. Set minimum duration to 10 minutes
2. Create 5-minute meeting starting now + 2min
3. Mark as "Busy"
4. Wait for event time

**Expected Result**:
- DND remains off
- Event not shown in "Current Meeting"
- Debug log shows event filtered (too short)

**Actual Result**: _______________

**Pass/Fail**: _______________

---

## Permission Tests

### Test 8: Missing Calendar Permission

**Objective**: Verify graceful handling when calendar permission denied

**Steps**:
1. Revoke calendar permission: Settings → Apps → Calendar DND → Permissions → Calendar → Deny
2. Return to app
3. Attempt to enable automation

**Expected Result**:
- App shows setup required warning
- Onboarding screen appears
- No crashes
- Debug log shows "Missing permissions"
- Quick toggle opens app instead of enabling

**Actual Result**: _______________

**Pass/Fail**: _______________

---

### Test 9: Missing DND Policy Access

**Objective**: Verify graceful handling when DND permission denied

**Steps**:
1. Revoke DND permission: Settings → Apps → Special access → Do Not Disturb → Calendar DND → Deny
2. Create active meeting
3. Observe behavior

**Expected Result**:
- App shows setup required warning
- DND not changed
- No crashes
- Debug log shows "Missing permissions"

**Actual Result**: _______________

**Pass/Fail**: _______________

---

## Ownership & User Override Tests

### Test 10: User Manual DND Not Affected

**Objective**: Verify app doesn't disable DND user manually enabled

**Steps**:
1. Disable automation in Calendar DND
2. Manually enable DND via Quick Settings
3. Wait 5 minutes
4. Check DND status

**Expected Result**:
- DND remains on
- App does not turn it off
- Debug log shows automation off, no changes made

**Actual Result**: _______________

**Pass/Fail**: _______________

---

### Test 11: User Override During Meeting

**Objective**: Verify app respects user disabling DND mid-meeting

**Steps**:
1. Create 20-minute meeting starting now + 2min
2. Wait for meeting to start and DND to enable
3. Manually disable DND via Quick Settings
4. Wait 2 minutes
5. Observe DND status

**Expected Result**:
- DND initially turns on (app owns it)
- After user disables: DND stays off
- App does not re-enable DND for remainder of meeting
- Debug log shows "user override detected"
- After meeting ends, normal behavior resumes

**Actual Result**: _______________

**Pass/Fail**: _______________

---

## Scheduling Reliability Tests

### Test 12: Screen Off - Meeting Start (Exact Alarms)

**Objective**: Verify alarm fires when screen off and device idle

**Prerequisites**: Exact alarms permission granted

**Steps**:
1. Create meeting starting 10 minutes from now
2. Turn off screen
3. Leave device idle (don't touch)
4. Wait for meeting start time
5. Check DND status (turn on screen after start time)

**Expected Result**:
- DND is already on when screen turns back on
- Turned on within 30 seconds of meeting start
- Debug log shows "Trigger: ALARM"

**Actual Result**: _______________

**Pass/Fail**: _______________

---

### Test 13: Exact Alarms Denied - Degraded Mode

**Objective**: Verify near-term guards work when exact alarms unavailable

**Steps**:
1. Revoke exact alarm permission: Settings → Apps → Calendar DND → Alarms & reminders → Deny
2. Create meeting starting 30 minutes from now
3. Observe behavior

**Expected Result**:
- App shows degraded mode notification
- Meeting still handled (via near-term guards + periodic worker)
- May have ±2 minute timing variance
- Debug log shows "needsNearTermGuards: true"

**Actual Result**: _______________

**Pass/Fail**: _______________

---

### Test 14: Calendar Change Detection

**Objective**: Verify app reacts to calendar modifications

**Steps**:
1. With automation enabled and screen on
2. Add new meeting starting 30 minutes from now via calendar app
3. Return to Calendar DND
4. Check status screen

**Expected Result**:
- "Next Meeting" updates within 15 seconds
- New boundary scheduled
- Debug log shows "Trigger: CALENDAR_CHANGE"

**Actual Result**: _______________

**Pass/Fail**: _______________

---

### Test 15: Periodic Worker Safety Net

**Objective**: Verify periodic worker catches missed events

**Setup**: Simulate missed alarm

**Steps**:
1. Create meeting starting now + 5 minutes
2. Force-stop the app: Settings → Apps → Calendar DND → Force Stop
3. Wait for meeting to start
4. Wait up to 15 minutes
5. Check DND status

**Expected Result**:
- DND eventually turns on (within 15 minutes of meeting start)
- Debug log shows "Trigger: WORKER_SANITY"
- Not as precise as alarm, but functional

**Actual Result**: _______________

**Pass/Fail**: _______________

---

## System Event Tests

### Test 16: Reboot Handling

**Objective**: Verify automation continues after device restart

**Steps**:
1. Enable automation
2. Create meeting starting 20 minutes from now
3. Reboot device
4. After boot, check app status
5. Wait for meeting start

**Expected Result**:
- Automation still enabled after reboot
- Meeting still scheduled
- DND turns on at meeting start
- Debug log shows "Trigger: BOOT"

**Actual Result**: _______________

**Pass/Fail**: _______________

---

### Test 17: Time Zone Change

**Objective**: Verify boundaries recalculate on timezone change

**Steps**:
1. Create meeting at 2:00 PM (local time), 1 hour duration
2. Change timezone: Settings → System → Date & time → Select time zone
3. Choose timezone 2 hours different
4. Check Calendar DND status

**Expected Result**:
- Next meeting time updates to reflect new timezone
- Boundaries recalculated
- Debug log shows "Trigger: TIME_CHANGE"

**Actual Result**: _______________

**Pass/Fail**: _______________

---

## Battery Management Tests

### Test 18: Battery Optimization Enabled (Degraded Behavior)

**Objective**: Document behavior when battery optimization enabled

**Steps**:
1. Enable battery optimization: Settings → Apps → Calendar DND → Battery → Optimized
2. Turn off screen
3. Create meeting starting 10 minutes from now
4. Wait for meeting start

**Expected Result**:
- DND may turn on with delay (up to 15 minutes)
- App shows degraded mode notification
- Still functional but less precise
- Document actual delay observed: _____ minutes

**Actual Result**: _______________

**Pass/Fail**: _______________

---

### Test 19: Battery Optimization Disabled (Full Performance)

**Objective**: Verify optimal behavior with battery optimization disabled

**Steps**:
1. Disable battery optimization: Settings → Apps → Calendar DND → Battery → Unrestricted
2. Remove from sleeping apps if present
3. Turn off screen
4. Create meeting starting 10 minutes from now
5. Wait for meeting start

**Expected Result**:
- DND turns on within 30 seconds of meeting start
- Precise timing
- No degraded mode warnings

**Actual Result**: _______________

**Pass/Fail**: _______________

---

## Edge Cases

### Test 20: Three Overlapping Meetings

**Objective**: Verify complex overlap handling

**Steps**:
1. Create Meeting A: 10:00 - 10:30
2. Create Meeting B: 10:15 - 10:45
3. Create Meeting C: 10:20 - 11:00
4. All marked "Busy"

**Expected Result**:
- DND on at 10:00
- DND stays on until 11:00 (end of C)
- Single merged window shown

**Actual Result**: _______________

**Pass/Fail**: _______________

---

### Test 21: Meeting Deleted While Active

**Objective**: Verify behavior when active meeting deleted

**Steps**:
1. Create meeting starting now, 30 minutes
2. Wait for DND to enable
3. Delete the meeting from calendar
4. Tap "Run Engine Now" in app

**Expected Result**:
- DND turns off when engine runs
- Status shows no current meeting
- Debug log shows no active window

**Actual Result**: _______________

**Pass/Fail**: _______________

---

### Test 22: Meeting Extended While Active

**Objective**: Verify updated end time detected

**Steps**:
1. Create meeting starting now, ending now + 15min
2. Wait for DND to enable
3. Edit meeting to end now + 30min
4. Tap "Run Engine Now" or wait for observer

**Expected Result**:
- Next boundary updates to new end time
- DND stays on longer
- Debug log shows updated end time

**Actual Result**: _______________

**Pass/Fail**: _______________

---

## Test Summary

**Total Tests**: 22  
**Passed**: _____  
**Failed**: _____  
**Blocked**: _____

### Critical Failures (Must Fix)
- List any test failures here

### Known Issues (Acceptable)
- Document expected limitations

### Device-Specific Notes
- Record any Samsung-specific observations

---

## Test Sign-Off

**Tester Name**: _______________  
**Date**: _______________  
**Device**: _______________  
**Android Version**: _______________  
**App Version**: _______________

**Overall Result**: PASS / FAIL / CONDITIONAL

**Comments**:
_______________________________________________
_______________________________________________
_______________________________________________