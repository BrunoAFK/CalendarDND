# Firebase Toggles

This project supports build-time toggles for Firebase Crashlytics and Analytics. You can enable or
disable each product globally (persistent) or per build.

## What is controlled

- Crashlytics collection on/off
- Analytics collection on/off

These are controlled via build-time properties that generate `BuildConfig` flags.

## Persistent toggle (recommended)

Add the following to `gradle.properties`:

```
crashlyticsEnabled=false
analyticsEnabled=false
```

Set either value to `true` to enable. This applies to all builds on your machine.

## One-off build toggle

Override on the command line:

```
./gradlew assembleDebug -PcrashlyticsEnabled=false -PanalyticsEnabled=false
./gradlew assembleDebug -PcrashlyticsEnabled=true  -PanalyticsEnabled=true
```

You can mix them if you want only one enabled.

## Where the flags live

The flags are generated as:

- `BuildConfig.CRASHLYTICS_ENABLED`
- `BuildConfig.ANALYTICS_ENABLED`

They are exposed to the app via:

- `AppConfig.crashlyticsEnabled`
- `AppConfig.analyticsEnabled`

## Runtime behavior

On app startup (`App.kt`):

- Crashlytics collection is enabled/disabled using:
  `FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(...)`
- Analytics collection is enabled/disabled using:
  `FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(...)`

## Troubleshooting

If toggles do not seem to apply:

1. Ensure the build you installed was built after changing `gradle.properties`.
2. Clean build artifacts:
   ```
   ./gradlew clean
   ./gradlew assembleDebug
   ```
3. Confirm the `BuildConfig` values (optional):
   - Search `BuildConfig.CRASHLYTICS_ENABLED` in generated sources.

## Files involved

- `build.gradle.kts`
- `app/build.gradle.kts`
- `app/src/main/java/com/brunoafk/calendardnd/App.kt`
- `app/src/main/java/com/brunoafk/calendardnd/util/AppConfig.kt`

