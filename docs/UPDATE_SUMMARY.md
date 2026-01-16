# Update Summary

This document summarizes the recent updates related to onboarding, analytics, languages, and debugging.

## Onboarding Flow

The onboarding flow is now multi-step:

1. Language selection
2. Intro screen
3. Permissions
4. Calendar scope selection (all vs specific)
5. Optional calendar picker (if “specific”)

Notes:
- If the device language is supported (en/de/hr/it/ko), it is preselected.
- Language changes apply immediately in the app.
- Onboarding completion is saved to avoid repeating the flow.

Relevant files:
- `app/src/main/java/com/brunoafk/calendardnd/ui/navigation/AppNavigation.kt`
- `app/src/main/java/com/brunoafk/calendardnd/ui/screens/StartupScreen.kt`
- `app/src/main/java/com/brunoafk/calendardnd/ui/screens/IntroScreen.kt`
- `app/src/main/java/com/brunoafk/calendardnd/ui/screens/LanguageScreen.kt`
- `app/src/main/java/com/brunoafk/calendardnd/ui/screens/OnboardingScreen.kt`
- `app/src/main/java/com/brunoafk/calendardnd/ui/screens/CalendarScopeScreen.kt`
- `app/src/main/java/com/brunoafk/calendardnd/ui/screens/CalendarPickerScreen.kt`

## Analytics Tracking

Analytics events are tracked in a privacy-safe way (no event titles or calendar IDs). Events include:
- Screen views (onboarding, status, settings, calendar picker, debug logs, help)
- Automation toggles (tile + UI)
- Settings changes
- Engine runs (trigger + action)
- Pre-DND notification shown and “Enable now” action

Analytics is controlled via build-time toggles:
- `analyticsEnabled` (gradle property)
- `AppConfig.analyticsEnabled`

Relevant files:
- `app/src/main/java/com/brunoafk/calendardnd/util/AnalyticsTracker.kt`
- `app/src/main/java/com/brunoafk/calendardnd/util/AppConfig.kt`
- `app/src/main/java/com/brunoafk/calendardnd/App.kt`
- `app/src/main/java/com/brunoafk/calendardnd/system/alarms/EngineRunner.kt`
- `app/src/main/java/com/brunoafk/calendardnd/ui/tiles/AutomationTileService.kt`
- `app/src/main/java/com/brunoafk/calendardnd/ui/screens/SettingsScreen.kt`

## Languages and Localization

All UI strings (including Help content) are stored in `strings.xml` and translated into:
- German (`values-de`)
- Croatian (`values-hr`)
- Italian (`values-it`)
- Korean (`values-ko`)

Language selection is stored in settings and applied at app startup.

Relevant files:
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-de/strings.xml`
- `app/src/main/res/values-hr/strings.xml`
- `app/src/main/res/values-it/strings.xml`
- `app/src/main/res/values-ko/strings.xml`
- `app/src/main/java/com/brunoafk/calendardnd/data/prefs/SettingsStore.kt`
- `app/src/main/java/com/brunoafk/calendardnd/App.kt`

## Debugging and Logs

Debug logs now include a header when copied:
- App version (name + code)
- Language
- Key settings (automation, calendar scope, filters, DND mode, offset, pre-DND notification)

There is a new “Copy Logs” action in the Debug Logs screen.

Relevant files:
- `app/src/main/java/com/brunoafk/calendardnd/ui/screens/DebugLogScreen.kt`
- `app/src/main/java/com/brunoafk/calendardnd/data/prefs/DebugLogStore.kt`

