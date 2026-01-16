# Calendar DND

Automatic Do Not Disturb for Android based on your calendar events.

## What It Does

Calendar DND silences your phone during calendar meetings automatically. No more interruptions during important calls or presentations.

When a meeting starts, DND turns on. When it ends, DND turns off. Simple.

## Features

- ⚡ **Automatic** - DND activates based on your calendar events
- 🧠 **Smart Merging** - Back-to-back meetings stay silent (no interruptions between)
- 🙌 **Respects You** - If you manually change DND during a meeting, the app stops interfering
- 🔋 **Battery Friendly** - Minimal background activity
- 🔒 **Private** - All data stays on your device
- 🌍 **Multi-Language** - English, German, Croatian, Italian, Korean, Chinese
- ⏰ **Flexible Timing** - Start DND before or after meeting time
- 🔔 **Pre-Warnings** - Optional notifications before DND activates
- 📱 **Samsung Optimized** - Tested and optimized for Samsung devices

## Screenshots

<p align="center">
  <img src="docs/screenshots/status.png" width="200" alt="Status Screen"/>
  <img src="docs/screenshots/settings.png" width="200" alt="Settings Screen"/>
  <img src="docs/screenshots/onboarding.png" width="200" alt="Onboarding Screen"/>
</p>

> Screenshots coming soon

## How to Use

1. **Install** the app
2. **Enable** automation on the main screen
3. **Grant** calendar and DND permissions when prompted
4. **Done** - Your phone will now auto-silence during meetings

## Configuration

Once set up, you can customize:

- **Calendars** - Choose which calendars to monitor
- **Event Filters** - Only busy events, ignore all-day events, minimum duration
- **DND Mode** - Priority Only or Total Silence
- **Timing** - Start DND before or after the meeting time
- **Notifications** - Get a 5-minute warning before DND activates

All settings are in the app's Settings screen.

## Requirements

- Android 8.0 or higher
- Calendar app with synced events
- DND Policy Access permission (the app will guide you)

## Permissions Explained

| Permission | Why It's Needed |
|------------|-----------------|
| Calendar | Read your events to know when meetings happen |
| DND Policy Access | Control Do Not Disturb mode |
| Notifications | Show pre-meeting warnings (optional) |
| Exact Alarms | Precise timing for meeting boundaries |

## Privacy

- ✅ All calendar data stays on your device
- ✅ No accounts required
- ✅ No ads
- ✅ Optional analytics (you choose)
- ✅ Open source

## Troubleshooting

**DND not working?**
1. Check that automation is enabled (main screen toggle)
2. Verify permissions are granted (Settings > Permissions)
3. Make sure your event matches your filters (busy status, duration, etc.)
4. On Samsung: Disable battery optimization for the app

**Still having issues?**

Check the in-app debug logs (Settings > Debug Tools > Debug Logs) or [open an issue](https://github.com/BrunoAFK/CalendarDND/issues).

## Samsung Users

This app is optimized for Samsung devices. For best results:
- Disable battery optimization for Calendar DND
- Allow exact alarms in app settings

The app will guide you through these steps if needed.

## Support

- **Issues & Bugs**: [GitHub Issues](https://github.com/BrunoAFK/CalendarDND/issues)
- **In-App Help**: Settings > Help

## For Developers

Technical documentation, architecture details, and contribution guidelines are available in the [Developer Documentation](docs/DEVELOPER_DOCUMENTATION.md).

## License

[Apache License 2.0](LICENSE)

## Author

Vibe-coded by Bruno Pavelja ([@BrunoAFK](https://github.com/BrunoAFK)) 

---

⭐ **Star this repo if Calendar DND helps you focus!**
