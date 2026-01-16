# Calendar DND - Project Completion Checklist

Use this checklist to verify all components are properly implemented and working.

## 📁 File Structure

### Core Package Files
- [ ] `App.kt` - Application class
- [ ] `AndroidManifest.xml` - All receivers, permissions, services declared

### UI Layer
- [ ] `ui/MainActivity.kt`
- [ ] `ui/navigation/AppNavigation.kt`
- [ ] `ui/theme/Theme.kt`
- [ ] `ui/screens/OnboardingScreen.kt`
- [ ] `ui/screens/StatusScreen.kt`
- [ ] `ui/screens/SettingsScreen.kt`
- [ ] `ui/screens/CalendarPickerScreen.kt`
- [ ] `ui/screens/DebugLogScreen.kt`
- [ ] `ui/tiles/AutomationTileService.kt`

### Domain Layer
- [ ] `domain/model/EventInstance.kt`
- [ ] `domain/model/MeetingWindow.kt`
- [ ] `domain/model/DndMode.kt`
- [ ] `domain/model/Trigger.kt`
- [ ] `domain/engine/AutomationEngine.kt`
- [ ] `domain/engine/EngineInput.kt`
- [ ] `domain/engine/EngineOutput.kt`
- [ ] `domain/planning/MeetingWindowResolver.kt`
- [ ] `domain/planning/SchedulePlanner.kt`

### Data Layer
- [ ] `data/calendar/CalendarRepository.kt`
- [ ] `data/calendar/CalendarQueries.kt`
- [ ] `data/calendar/CalendarObserver.kt`
- [ ] `data/dnd/DndController.kt`
- [ ] `data/prefs/SettingsStore.kt`
- [ ] `data/prefs/RuntimeStateStore.kt`
- [ ] `data/prefs/DebugLogStore.kt`

### System Layer
- [ ] `system/alarms/AlarmScheduler.kt`
- [ ] `system/alarms/AlarmReceiver.kt`
- [ ] `system/alarms/AlarmActions.kt`
- [ ] `system/alarms/EngineRunner.kt`
- [ ] `system/workers/SanityWorker.kt`
- [ ] `system/workers/NearTermGuardWorker.kt`
- [ ] `system/workers/Workers.kt`
- [ ] `system/receivers/BootReceiver.kt`
- [ ] `system/receivers/TimeChangeReceiver.kt`
- [ ] `system/receivers/TimezoneChangeReceiver.kt`

### Utilities
- [ ] `util/Debouncer.kt`
- [ ] `util/TimeUtils.kt`
- [ ] `util/PermissionUtils.kt`

### Resources
- [ ] `res/values/strings.xml`
- [ ] `res/drawable/ic_tile.xml`
- [ ] `res/xml/backup_rules.xml`
- [ ] `res/xml/data_extraction_rules.xml`

### Build Files
- [ ] `build.gradle.kts` (app module)
- [ ] `build.gradle.kts` (project level)
- [ ] `gradle.properties`

### Documentation
- [ ] `README.md`
- [ ] `USER_GUIDE.md`
- [ ] `DEVELOPER_DOCUMENTATION.md`
- [ ] `ACCEPTANCE_TESTS.md`
- [ ] `SETUP_GUIDE.md`
- [ ] `PROJECT_CHECKLIST.md` (this file)

---

## 🔧 Build & Compile

- [ ] Project syncs successfully in Android Studio
- [ ] No Gradle errors
- [ ] No import errors
- [ ] No resource errors
- [ ] Debug build compiles: `./gradlew assembleDebug`
- [ ] Release build compiles: `./gradlew assembleRelease`

---

## 📱 Installation & Launch

- [ ] App installs on Samsung device
- [ ] App launches without crashes
- [ ] No startup errors in logcat
- [ ] Navigation works (can move between screens)
- [ ] App icon displays correctly
- [ ] App appears in app drawer

---

## 🔐 Permissions & Setup

### Required Permissions
- [ ] Calendar permission requested
- [ ] Calendar permission granted
- [ ] DND policy access requested
- [ ] DND policy access granted
- [ ] Exact alarms permission requested (Android 12+)
- [ ] Exact alarms permission granted
- [ ] Notification permission (Android 13+)

### Onboarding Flow
- [ ] Onboarding screen shows on first launch
- [ ] Permission cards display correctly
- [ ] "Grant Permission" buttons work
- [ ] Permission requests open correct system settings
- [ ] Can complete setup and navigate to status screen
- [ ] Missing permissions show warnings

### Battery Optimization
- [ ] Battery optimization guidance shows
- [ ] "Open Battery Settings" button works
- [ ] Can disable battery optimization
- [ ] Degraded mode warning when optimization enabled

---

## ⚙️ Core Functionality

### Automation Toggle
- [ ] Toggle switches on/off
- [ ] State persists across app restart
- [ ] Enabling runs engine
- [ ] Disabling clears DND (if app owns it)

### Calendar Reading
- [ ] App reads calendar events
- [ ] Active meetings display on status screen
- [ ] Next meeting displays on status screen
- [ ] Calendar selection works
- [ ] Filter settings apply correctly

### DND Control
- [ ] Can check DND policy access
- [ ] Can enable DND (Priority mode)
- [ ] Can enable DND (Total Silence mode)
- [ ] Can disable DND
- [ ] DND state shows on status screen
- [ ] System DND indicator appears in status bar

### Engine Execution
- [ ] "Run Engine Now" button works
- [ ] Engine executes without errors
- [ ] Decisions logged to debug log
- [ ] State updates in DataStore
- [ ] DND changes apply

---

## 🎯 Meeting Scenarios

### Basic Meeting Flow
- [ ] DND turns on at meeting start
- [ ] Status shows current meeting
- [ ] DND turns off at meeting end
- [ ] Timing accurate (within 30 seconds)

### Back-to-Back Meetings
- [ ] DND stays on between consecutive meetings
- [ ] No flicker/toggle between meetings
- [ ] Single merged window detected
- [ ] DND turns off after last meeting ends

### Overlapping Meetings
- [ ] DND stays on for longest overlap
- [ ] Merged window shows correct end time
- [ ] All events included in window

### Event Filters
- [ ] All-day events ignored (when setting enabled)
- [ ] Free events ignored (when Busy-only enabled)
- [ ] Short events ignored (below minimum duration)
- [ ] Selected calendars only (when specific selection)

---

## 📅 Background Execution

### Exact Alarms
- [ ] Alarms scheduled for boundaries
- [ ] Alarms fire when screen off
- [ ] Alarms fire in doze mode
- [ ] AlarmReceiver executes engine
- [ ] Logs show "Trigger: ALARM"

### WorkManager
- [ ] Sanity worker scheduled every 15 minutes
- [ ] Sanity worker executes successfully
- [ ] Near-term guards scheduled when needed
- [ ] Guards execute near boundaries
- [ ] Workers survive force-stop (within 15 min)

### Content Observer
- [ ] Registers when automation enabled
- [ ] Fires when calendar changes
- [ ] Debounces rapid changes (10 seconds)
- [ ] Triggers engine execution
- [ ] Logs show "Trigger: CALENDAR_CHANGE"

### System Events
- [ ] Boot receiver fires on device restart
- [ ] Time change receiver fires on time change
- [ ] Timezone change receiver fires on timezone change
- [ ] All receivers trigger engine

---

## 👤 User Override Handling

- [ ] Manual DND not affected when automation off
- [ ] User disabling DND during meeting detected
- [ ] Suppression set to meeting end time
- [ ] App stops interfering until meeting ends
- [ ] Debug log shows "user override detected"
- [ ] Normal behavior resumes after meeting

---

## 📊 State Management

### Settings (SettingsStore)
- [ ] All settings persist
- [ ] Settings survive app restart
- [ ] Settings backed up (if Android backup enabled)
- [ ] Changes immediately apply

### Runtime State (RuntimeStateStore)
- [ ] dndSetByApp tracks ownership
- [ ] activeWindowEndMs updates
- [ ] userSuppressedUntilMs tracks suppression
- [ ] lastPlannedBoundaryMs shows next wake
- [ ] State clears when automation disabled

### Debug Logs (DebugLogStore)
- [ ] Logs append on each engine run
- [ ] Logs limited to 200 lines
- [ ] Logs show in DebugLogScreen
- [ ] Can clear logs
- [ ] Timestamps show correctly

---

## 🎨 UI Components

### Status Screen
- [ ] Automation toggle works
- [ ] Current meeting displays
- [ ] Next meeting displays
- [ ] DND status shows
- [ ] "Run Engine Now" executes
- [ ] Navigation icons work

### Settings Screen
- [ ] Calendar selection navigation works
- [ ] Busy-only toggle works
- [ ] Ignore all-day toggle works
- [ ] Min duration slider works
- [ ] DND mode selection works
- [ ] Back navigation works

### Calendar Picker
- [ ] Calendars list loads
- [ ] Can select "All calendars"
- [ ] Can select specific calendars
- [ ] Selection saves
- [ ] Checkboxes reflect state

### Debug Log Screen
- [ ] Logs display (newest first)
- [ ] Can scroll through logs
- [ ] Clear logs button works
- [ ] "No logs" message when empty

### Quick Settings Tile
- [ ] Tile appears in Quick Settings
- [ ] Tile shows correct state
- [ ] Tap toggles automation
- [ ] Tap opens app when setup needed
- [ ] Tile updates when state changes

---

## 🧪 Edge Cases

- [ ] App handles missing calendars gracefully
- [ ] App handles calendar permission revoked mid-operation
- [ ] App handles DND permission revoked mid-operation
- [ ] Meeting deleted during active window handled
- [ ] Meeting extended during active window updated
- [ ] Multiple rapid calendar changes debounced
- [ ] Force-stop recovery (via periodic worker)
- [ ] Very long meetings (24+ hours) handled
- [ ] Events with no end time ignored

---

## 🔋 Samsung-Specific Tests

### Battery Management
- [ ] Works with battery optimization enabled (degraded)
- [ ] Works with battery optimization disabled (full)
- [ ] Degraded mode notification shows
- [ ] Can detect sleeping apps restriction
- [ ] Guidance to disable optimization works

### DND Control
- [ ] Samsung DND modes work (Priority/None)
- [ ] No crashes on Samsung DND API calls
- [ ] Try-catch handles Samsung exceptions
- [ ] One UI DND settings compatible

### Exact Alarms
- [ ] Detects when permission missing
- [ ] Falls back to near-term guards
- [ ] Settings link works

---

## 📝 Logging & Debugging

- [ ] Debug logs show all engine runs
- [ ] Triggers logged correctly
- [ ] Decisions logged clearly
- [ ] Errors logged with details
- [ ] Logcat shows no errors during normal operation
- [ ] Can export logs (optional feature)

---

## ✅ Final Verification

### Pre-Release Checklist
- [ ] All 22 acceptance tests pass
- [ ] No crashes in normal operation
- [ ] No ANR (Application Not Responding)
- [ ] No memory leaks detected
- [ ] Battery usage <1% per day
- [ ] Works on multiple Samsung models tested
- [ ] Documentation complete and accurate

### User Experience
- [ ] Setup flow is clear
- [ ] Error messages are helpful
- [ ] UI is responsive
- [ ] No confusing behavior
- [ ] Works as advertised

### Code Quality
- [ ] No compiler warnings
- [ ] No lint errors (or all suppressed with reason)
- [ ] Code follows Kotlin conventions
- [ ] All public APIs documented
- [ ] Unit tests pass (if implemented)

---

## 🚀 Ready for Release?

**All items checked?**

If YES:
- [ ] Build release APK
- [ ] Test release build on clean device
- [ ] Prepare store listing
- [ ] Submit to Play Store (or distribute)

If NO:
- Review failed items
- Fix issues
- Re-test
- Return to this checklist

---

## 📋 Issue Tracking Template

For any unchecked items, create issues:

**Issue Template:**
```
Title: [Component] Brief description
Priority: High/Medium/Low
Category: Bug/Feature/Documentation

Description:
- What's not working/missing
- Steps to reproduce (if bug)
- Expected behavior
- Actual behavior

Environment:
- Device: 
- Android version:
- App version:

Acceptance Criteria:
- [ ] Item works as specified
- [ ] Tests pass
- [ ] Documentation updated
```

---

**Last Updated**: [Add date when you complete checklist]  
**Completed By**: [Your name]  
**Overall Status**: ☐ In Progress  ☐ Complete  ☐ Blocked