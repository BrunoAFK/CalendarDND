# Calendar DND

Automatic Do Not Disturb mode for Android based on your calendar events.

## Overview

Calendar DND silences your phone during calendar meetings automatically. No more interruptions during important calls or presentations!

### Key Features

- ⚡ **Automatic**: DND turns on/off based on calendar events
- 🧠 **Smart**: Merges back-to-back meetings (no interruptions)
- 👤 **Respectful**: Honors manual DND changes during meetings
- 🔋 **Efficient**: Battery-friendly background scheduling
- 🛡️ **Private**: 100% local, optional analytics
- 📱 **Samsung Optimized**: Specifically tested and optimized for Samsung devices
- 🌍 **Multi-Language**: Supports English, German, Croatian, Italian, Korean
- 🔔 **Pre-Warnings**: Optional 5-minute notifications before DND activates
- ⚙️ **Flexible**: Configurable DND start offset (early/delayed)
- 📦 **Multiple Distributions**: Play Store, AltStore, or manual updates

## Quick Start

1. Install the app
2. Enable automation
3. Grant calendar and DND permissions
4. Done! Your phone will now auto-silence during meetings

## Requirements

- Android 8.0 (API 26) or higher
- Calendar app with synced events
- Notification Policy Access permission

## Screenshots

[Add screenshots here when available]

## Documentation

- **[User Guide](USER_GUIDE.md)** - Complete guide for end users
- **[Developer Documentation](DEVELOPER_DOCUMENTATION.md)** - Architecture and development guide
- **[Acceptance Tests](ACCEPTANCE_TESTS.md)** - Test scenarios and expected behavior

## Installation

### From Source

1. Clone this repository
```bash
git clone https://github.com/yourusername/calendar-dnd.git
cd calendar-dnd
```

2. Open in Android Studio

3. Build and run
```bash
./gradlew assembleDebug
```

### From Release

[Link to releases when available]

## Project Structure

```
com.brunoafk.calendardnd/
├── ui/                   # Jetpack Compose UI
├── domain/              # Business logic (engine)
├── data/                # Repositories and data access
├── system/              # Alarms, workers, receivers
└── util/                # Helper utilities
```

See [Developer Documentation](DEVELOPER_DOCUMENTATION.md) for detailed architecture.

## Technology Stack

- **Language**: Kotlin (JVM 11)
- **UI**: Jetpack Compose + Material 3
- **Navigation**: Accompanist Navigation Animation
- **Storage**: DataStore (Preferences)
- **Background**: AlarmManager + WorkManager
- **Calendar**: CalendarContract API
- **DND**: NotificationManager API
- **Analytics**: Firebase Crashlytics + Analytics (optional)
- **Localization**: 5 languages (en, de, hr, it, ko)

## Key Components

- **AutomationEngine**: Pure Kotlin decision-making logic with DND offset support
- **MeetingWindowResolver**: Merges overlapping calendar events
- **AlarmScheduler**: Exact alarm management + pre-DND notifications
- **EngineRunner**: Centralized background execution with analytics
- **ManualUpdateManager**: Built-in update checker (manual flavor)
- **DebugTapLogger**: Navigation debugging and interaction gating

## Permissions Required

- `READ_CALENDAR` - Read calendar events
- `RECEIVE_BOOT_COMPLETED` - Restart on device boot
- `POST_NOTIFICATIONS` - Show notifications (Android 13+)
- `SCHEDULE_EXACT_ALARM` - Precise timing (Android 12+)

### Special Permissions

- **Notification Policy Access** - Control Do Not Disturb (granted via Settings)

## Samsung-Specific Optimizations

This app is specifically designed to work reliably on Samsung devices:

- Multiple scheduling strategies (exact alarms + WorkManager fallbacks)
- Battery optimization detection and guidance
- Samsung DND API edge case handling
- Tested on Galaxy S25

See [Samsung Tips](USER_GUIDE.md#samsung-specific-tips) for setup recommendations.

## How It Works

```
Calendar Event Detected
         ↓
Meeting Window Calculated
         ↓
Engine Makes Decision
         ↓
DND Enabled/Disabled
         ↓
Next Boundary Scheduled
```

The app uses a sophisticated scheduling system:
1. **Primary**: Exact alarms at meeting boundaries
2. **Fallback**: WorkManager near-term guards
3. **Safety Net**: 15-minute periodic checks

See [Architecture Overview](DEVELOPER_DOCUMENTATION.md#architecture-overview) for details.

## Configuration

All settings accessible via in-app UI:

- **Language Selection**: Choose from 5 supported languages
- **Calendar Selection**: Choose which calendars to monitor
- **Event Filters**: Busy-only, ignore all-day, minimum duration
- **DND Mode**: Priority Only vs Total Silence
- **DND Start Offset**: Start DND before/after meeting time (-10 to +10 minutes)
- **Pre-DND Notifications**: Optional 5-minute warning before DND
- **Quick Settings Tile**: One-tap toggle
- **Privacy Controls**: Opt-in/out of Crashlytics and Analytics
- **Debug Tools**: Language preview, notification testing, debug overlay

## Testing

### Unit Tests
```bash
./gradlew test
```

### Instrumentation Tests
```bash
./gradlew connectedAndroidTest
```

### Manual Testing

See [ACCEPTANCE_TESTS.md](ACCEPTANCE_TESTS.md) for comprehensive test scenarios.

## Troubleshooting

### DND Not Working?

1. Check permissions (Calendar + DND Policy Access)
2. Verify automation is enabled
3. Check event matches filters (busy, duration, calendar)
4. Review debug logs in app
5. Ensure battery optimization disabled (Samsung)

See [Troubleshooting Guide](USER_GUIDE.md#troubleshooting) for more.

## Contributing

Contributions welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Follow Kotlin coding conventions
5. Test on real Samsung device
6. Submit pull request

See [Developer Documentation](DEVELOPER_DOCUMENTATION.md#contributing) for guidelines.

## Build Flavors

Calendar DND is available in three distribution variants:

### 1. Play Store (`play` flavor)
- Distributed via Google Play Store
- Auto-updates via Play Store
- Standard configuration

### 2. AltStore (`altstore` flavor)
- Distributed via AltStore
- Standard configuration

### 3. Manual (`manual` flavor)
- Direct download / GitHub Releases
- Built-in update checker
- Shows update prompts for new versions
- Supports semantic versioning
- Configuration: See `docs/manual-updates.md`

**Building specific flavors:**
```bash
./gradlew assemblePlayRelease
./gradlew assembleAltstoreRelease
./gradlew assembleManualRelease
```

## Known Limitations

- Requires calendar events to have both start and end times
- DND changes may delay up to 15 minutes if exact alarms unavailable
- Cannot override system-level DND restrictions
- Requires active calendar sync (won't work offline without pre-synced events)
- Language changes require app restart

## Roadmap

### Completed (v1.0.0)
- ✅ Multi-language support (5 languages)
- ✅ Pre-DND notifications
- ✅ DND start offset configuration
- ✅ Manual update system
- ✅ Firebase analytics integration
- ✅ Debug tools and overlay
- ✅ Animated navigation

### Planned Features
- [ ] Custom notification filter rules
- [ ] Location-based exceptions
- [ ] Keyword-based event filtering
- [ ] Statistics and usage insights
- [ ] Multiple profile support
- [ ] Additional language support

### Under Consideration
- [ ] Tasker integration
- [ ] Smartwatch companion
- [ ] Auto-reply SMS during meetings

## Performance

- **Battery**: <1% per day typical usage
- **Memory**: ~20MB resident
- **Storage**: <5MB app size
- **Network**: No network usage (100% local)

## Privacy

Calendar DND respects your privacy:

- ✅ All calendar data stored locally
- ✅ Optional analytics (Firebase Crashlytics & Analytics)
- ✅ Explicit opt-in required for telemetry
- ✅ No ads
- ✅ Minimal network access (only for manual updates if enabled)
- ✅ Open source (auditable)
- ✅ Analytics can be disabled at build time (see `docs/FIREBASE_TOGGLES.md`)

## License

[Specify your license here - e.g., MIT, Apache 2.0, GPL]

## Support

- **Issues**: [GitHub Issues](https://github.com/yourusername/calendar-dnd/issues)
- **Documentation**: See docs folder
- **Debug Logs**: Available in-app (📄 icon)

## Acknowledgments

Built with:
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Accompanist Navigation Animation](https://google.github.io/accompanist/navigation-animation/)
- [DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- [Firebase](https://firebase.google.com/) (Crashlytics, Analytics)
- [Material 3](https://m3.material.io/)

Inspired by the need for distraction-free meetings.

## Authors

- Your Name (@yourusername)

## Version History

- **1.0.0** (2025-01-11) - Initial release

---

**Star ⭐ this repo if Calendar DND helps you focus!**
