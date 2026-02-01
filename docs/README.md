# Developer Documentation

Technical documentation for Calendar DND contributors and developers.

## Quick Links

| Document | Description |
|----------|-------------|
| [Getting Started](getting-started.md) | Development environment setup |
| [Architecture](architecture.md) | System design and code structure |
| [Features](features.md) | Detailed feature documentation |
| [Configuration](configuration.md) | Build flavors, toggles, and settings |
| [Testing](testing.md) | Testing strategy and commands |
| [Privacy Policy](privacy-policy.md) | User privacy policy |

## Project Overview

Calendar DND is an Android app that automatically controls Do Not Disturb mode based on calendar events. It's built with:

- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 36 (Android 15)
- **Architecture**: Clean Architecture (4 layers)

## Quick Commands

```bash
# Debug build
./gradlew assembleDebug

# Run tests
./gradlew test

# Lint check
./gradlew lint

# Release builds
./gradlew assemblePlayRelease      # Play Store
./gradlew assembleManualRelease    # Manual distribution
```

## Architecture at a Glance

```
UI Layer (Compose)
    │
Domain Layer (Pure Kotlin)
    │
Data Layer (Repositories)
    │
System Layer (Alarms, Workers)
```

See [Architecture](architecture.md) for details.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests: `./gradlew test`
5. Run lint: `./gradlew lint`
6. Submit a pull request

## Archive

Previous documentation versions are available in the [archive](archive/) folder.
