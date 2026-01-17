# Calendar DND - Project Completion Checklist

Use this checklist to verify all components are properly implemented and working.

## 📁 File Structure

### Core Package Files
- [ ] `App.kt` - Application class
- [ ] `AndroidManifest.xml` - All receivers, permissions, services declared

### UI Layer
- [ ] `ui/MainActivity.kt`
- [ ] `ui/navigation/AppNavigation.kt` (Accompanist animated navigation)
- [ ] `ui/theme/Theme.kt`
- [ ] `ui/screens/StartupScreen.kt`
- [ ] `ui/screens/IntroScreen.kt`
- [ ] `ui/screens/LanguageScreen.kt`
- [ ] `ui/screens/OnboardingScreen.kt`
- [ ] `ui/screens/CalendarScopeScreen.kt`
- [ ] `ui/screens/CalendarPickerScreen.kt`
- [ ] `ui/screens/PrivacyScreen.kt`
- [ ] `ui/screens/StatusScreen.kt`
- [ ] `ui/screens/SettingsScreen.kt`
- [ ] `ui/screens/EventKeywordFilterScreen.kt` (experimental)
- [ ] `ui/screens/DebugLogScreen.kt`
- [ ] `ui/screens/DebugToolsScreen.kt`
- [ ] `ui/screens/DebugLanguageScreen.kt`
- [ ] `ui/screens/DebugSplashPreviewScreen.kt`
- [ ] `ui/screens/UpdateScreen.kt`
- [ ] `ui/screens/HelpScreen.kt`
- [ ] `ui/components/PrimaryActionButton.kt`
- [ ] `ui/components/StepIndicator.kt`
- [ ] `ui/components/PermissionCard.kt`
- [ ] `ui/components/OneUiHeader.kt`
- [ ] `ui/components/OneUiTopAppBar.kt`
- [ ] `ui/components/StatusCard.kt`
- [ ] `ui/components/ManualUpdatePrompt.kt`
- [ ] `ui/tiles/AutomationTileService.kt`

### Domain Layer
- [ ] `domain/model/EventInstance.kt`
- [ ] `domain/model/MeetingWindow.kt`
- [ ] `domain/model/DndMode.kt`
- [ ] `domain/model/Trigger.kt`
- [ ] `domain/engine/AutomationEngine.kt`
- [ ] `domain/engine/EngineInput.kt`
- [ ] `domain/engine/EngineOutput.kt`
- [ ] `domain/engine/TimeFormatter.kt`
- [ ] `domain/planning/MeetingWindowResolver.kt`
- [ ] `domain/planning/SchedulePlanner.kt`

### Data Layer
- [ ] `data/calendar/CalendarRepository.kt`
- [ ] `data/calendar/CalendarQueries.kt`
- [ ] `data/calendar/CalendarObserver.kt`
- [ ] `data/calendar/CalendarInfo.kt`
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
- [ ] `system/receivers/EnableDndNowReceiver.kt`
- [ ] `system/notifications/DndNotificationHelper.kt`
- [ ] `system/notifications/UpdateNotificationHelper.kt`
- [ ] `system/update/ManualUpdateManager.kt`

### Utilities
- [ ] `util/AnalyticsTracker.kt`
- [ ] `util/AndroidTimeFormatter.kt`
- [ ] `util/Debouncer.kt`
- [ ] `util/TimeUtils.kt`
- [ ] `util/PermissionUtils.kt`
- [ ] `util/LocaleUtils.kt`
- [ ] `util/AppConfig.kt`
- [ ] `util/DebugTapLogger.kt`

### Resources
- [ ] `res/values/strings.xml`
- [ ] `res/values-de/strings.xml` (German)
- [ ] `res/values-hr/strings.xml` (Croatian)
- [ ] `res/values-it/strings.xml` (Italian)
- [ ] `res/values-ko/strings.xml` (Korean)
- [ ] `res/drawable/ic_tile.xml`
- [ ] `res/drawable/ic_intro_logo.xml`
- [ ] `res/xml/backup_rules.xml`
- [ ] `res/xml/data_extraction_rules.xml`

### Build Files
- [ ] `build.gradle.kts` (app module with 3 flavors)
- [ ] `build.gradle.kts` (project level)
- [ ] `gradle.properties`
- [ ] `gradle/libs.versions.toml`
- [ ] `google-services.json` (Firebase)

### Scripts
- [ ] `scripts/release.sh` (Manual release automation)

### Documentation
- [ ] `docs/README.md`
- [ ] `docs/DEVELOPER_DOCUMENTATION.md`
- [ ] `docs/ACCEPTANCE_TESTS.md`
- [ ] `docs/SETUP_GUIDE.md`
- [ ] `docs/PROJECT_CHECKLIST.md` (this file)
- [ ] `docs/PLAN.md`
- [ ] `docs/FIREBASE_TOGGLES.md`
- [ ] `docs/manual-updates.md`
- [ ] `docs/IMPLEMENTATION_SUMMARY.md`
- [ ] `docs/UPDATE_SUMMARY.md`
- [ ] `AGENTS.md`

---

## 🔧 Build & Compile

- [ ] Project syncs successfully in Android Studio
- [ ] No Gradle errors
- [ ] No import errors
- [ ] No resource errors
- [ ] Debug build compiles: `./gradlew assembleDebug`
- [ ] Play flavor release: `./gradlew assemblePlayRelease`
- [ ] AltStore flavor release: `./gradlew assembleAltstoreRelease`
- [ ] Manual flavor release: `./gradlew assembleManualRelease`
- [ ] Firebase toggles work (crashlytics/analytics)
- [ ] Debug tools can be enabled in release builds

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

### Multi-Language Support
- [ ] Language screen shows all 5 languages
- [ ] Language selection persists
- [ ] Can switch between languages
- [ ] All screens display in selected language
- [ ] Debug language preview works for all languages
- [ ] English (default)
- [ ] German (de)
- [ ] Croatian (hr)
- [ ] Italian (it)
- [ ] Korean (ko)

### Pre-DND Notifications
- [ ] 5-minute warning notification shows
- [ ] Notification displays meeting title
- [ ] Notification shows meeting end time
- [ ] "Enable DND Now" action works
- [ ] Notification respects permission (Android 13+)
- [ ] Can be disabled in settings

### DND Start Offset
- [ ] Offset setting available in settings
- [ ] Can set positive offset (delay DND)
- [ ] Can set negative offset (early DND)
- [ ] Default is 0 (no offset)
- [ ] DND activates at offset time
- [ ] Offset respects meeting boundaries

### Debug Tools
- [ ] Debug Tools screen accessible from Settings
- [ ] Debug overlay toggle works
- [ ] Navigation overlay shows current route
- [ ] Tap logging works when enabled
- [ ] Language preview opens correct language
- [ ] Splash preview displays correctly
- [ ] Test notification button works
- [ ] App version displays correctly

### Manual Update System (manual flavor only)
- [ ] Update checker runs in background
- [ ] Update notification shows for major/minor versions
- [ ] Patch updates are silent
- [ ] Update screen shows release notes
- [ ] Download button works
- [ ] Update metadata pulls from GitHub
- [ ] Fallback URLs work if primary fails

### Firebase Integration
- [ ] Crashlytics opt-in works
- [ ] Analytics opt-in works
- [ ] Crash reports sent when opted in
- [ ] Analytics events tracked when opted in
- [ ] Can opt-out via settings
- [ ] Build-time toggles work (gradle.properties)

### Animated Navigation
- [ ] Smooth slide transitions between screens
- [ ] No jank during animations
- [ ] Route locking prevents double-navigation
- [ ] Back button works during animations
- [ ] Interaction gating blocks taps during transitions
- [ ] Animation duration ~220ms

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
