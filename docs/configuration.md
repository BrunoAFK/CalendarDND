# Configuration

Build flavors, feature toggles, and project settings.

## Build Flavors

The app has two distribution flavors in the `distribution` dimension:

### Play Store (`play`)

For Google Play distribution.

```bash
./gradlew assemblePlayDebug
./gradlew assemblePlayRelease
```

**Features**:
- Firebase Crashlytics enabled
- Firebase Analytics enabled
- Standard update mechanism (Play Store)

### Manual (`manual`)

For direct APK distribution.

```bash
./gradlew assembleManualDebug
./gradlew assembleManualRelease
```

**Features**:
- Built-in update checker
- Checks `update.json` from GitHub Releases
- Notifies users of new versions
- In-app update download

## Build Toggles

Pass via `-P` flag or add to `gradle.properties`:

### Firebase Toggles

```bash
# Disable Crashlytics
./gradlew assemblePlayRelease -PcrashlyticsEnabled=false

# Disable Analytics
./gradlew assemblePlayRelease -PanalyticsEnabled=false

# Disable both
./gradlew assemblePlayRelease -PcrashlyticsEnabled=false -PanalyticsEnabled=false
```

### Debug Tools Toggle

```bash
# Enable debug tools in release build
./gradlew assemblePlayRelease -PdebugToolsEnabled=true
```

Debug tools include:
- Debug logs screen
- Run engine manually
- Debug overlay

## gradle.properties

Default values:

```properties
# Enable/disable Firebase services
crashlyticsEnabled=true
analyticsEnabled=true

# Enable/disable debug tools in release
debugToolsEnabled=false
```

Override for local development by creating `local.properties`:

```properties
# local.properties (not committed to git)
crashlyticsEnabled=false
analyticsEnabled=false
debugToolsEnabled=true
```

## Build Types

### Debug

- Debuggable
- Debug signing
- Debug tools enabled
- Faster builds (no minification)

### Release

- Not debuggable
- Release signing
- Minified (R8)
- Optimized

## Signing

### Debug

Uses Android's default debug keystore (auto-generated).

### Release

Requires signing configuration. Add to `local.properties`:

```properties
RELEASE_STORE_FILE=/path/to/keystore.jks
RELEASE_STORE_PASSWORD=your_password
RELEASE_KEY_ALIAS=your_alias
RELEASE_KEY_PASSWORD=your_key_password
```

## SDK Versions

| Setting | Value |
|---------|-------|
| minSdk | 26 (Android 8.0) |
| targetSdk | 36 (Android 15) |
| compileSdk | 36 |

## Dependencies

Key dependencies (see `app/build.gradle.kts` for versions):

| Category | Library |
|----------|---------|
| UI | Jetpack Compose, Material 3 |
| Navigation | Compose Navigation |
| Preferences | DataStore |
| Background | WorkManager |
| Analytics | Firebase Analytics (play only) |
| Crashes | Firebase Crashlytics (play only) |

## ProGuard / R8

Release builds use R8 for:
- Code shrinking
- Obfuscation
- Optimization

Rules in `app/proguard-rules.pro`.

## Build Scripts

Helper scripts in `scripts/`:

```bash
# Build Play Store release
./scripts/build-play.sh

# Build Manual release
./scripts/build-manual.sh
```

## Version Management

Version defined in `app/build.gradle.kts`:

```kotlin
versionCode = 11900    // Increment for each release
versionName = "1.19"   // Semantic version
```

Version code format: `MAJOR * 10000 + MINOR * 100 + PATCH`

## Manual Update System

For `manual` flavor, updates are checked via:

1. App fetches `update.json` from GitHub Releases
2. Compares version codes
3. Shows notification if update available
4. User can download APK from within app

`update.json` format:
```json
{
  "versionCode": 11900,
  "versionName": "1.19",
  "downloadUrl": "https://github.com/.../app-manual-release.apk",
  "releaseNotes": "Bug fixes and improvements"
}
```

## Environment Variables

For CI/CD:

```bash
RELEASE_STORE_FILE      # Path to keystore
RELEASE_STORE_PASSWORD  # Keystore password
RELEASE_KEY_ALIAS       # Key alias
RELEASE_KEY_PASSWORD    # Key password
```

## Continuous Integration

Recommended CI checks:

```bash
./gradlew lint          # Static analysis
./gradlew test          # Unit tests
./gradlew assembleDebug # Build verification
```
