# Privacy Policy for Calendar DND

Last updated: 2026-01-31

## Summary

Calendar DND is a calendar-based Do Not Disturb automation app. It does not sell your data. Some distributions use Firebase services for analytics, crash reporting, performance monitoring, and push notifications.

## Data We Access and Use

### Calendar Access
- **What**: Event titles, times, availability, and calendar identifiers.
- **Why**: To decide when to enable/disable Do Not Disturb.
- **Where stored**: Processed locally on-device; never sent to external servers.

### Device Permissions
- **Do Not Disturb access**: Used to toggle DND during meetings.
- **Notifications**: Used for pre-DND reminders and update notices.
- **Exact alarms**: Used for precise scheduling at meeting boundaries.

## Distributions and Firebase

### Play Store and GitHub (Manual) Distributions

These distributions include Firebase services:

| Service | Purpose | User Control |
|---------|---------|--------------|
| Firebase Analytics | Usage events (screen views, settings changes) | Opt-in (off by default) |
| Firebase Crashlytics | Crash reports with device info | Opt-out toggle in app |
| Firebase Performance | App performance traces | Opt-out toggle in app (bundled with Crashlytics) |
| Firebase Cloud Messaging | Push notifications for updates | Always enabled |

**Defaults:**
- Analytics: **disabled** by default
- Crash & performance reports: **enabled** by default

You can change these settings anytime in the app's Privacy screen.

### Telemetry (Usage Pings)

Some builds send anonymous usage pings to understand usage and decide what to improve. This is not user tracking and does not include calendar data.

**Basic:** install ID, app version, device language, build flavor, daily active ping.  
**Detailed (closed testing only):** Basic + app language, OS version, manufacturer, model.

Opt out: open About and enable Debug Tools, then go to Settings > Debug Tools and disable Telemetry.

## Update Checks (GitHub Distribution)

The GitHub (manual) distribution can check for updates by downloading a small JSON file from GitHub. If an update is available, the app downloads the APK and verifies its cryptographic signature and SHA-256 checksum before prompting for installation.

## Debug Logs (Optional)

Debug logs are stored locally and can be shared manually by the user. They may include:
- App settings and timing information
- Calendar event titles (hashed for privacy)
- Keyword filter patterns (if user chooses to share)

Debug logs are excluded from device backups.

## Data Sharing

We do not sell your data. Data may be shared with:
- **Google Firebase** (Play Store and GitHub builds only) for analytics, crash reporting, performance monitoring, and push notifications.

## Data Retention

### Local Data
App data remains on your device until you uninstall the app or clear app data.

### Firebase Data
Data sent to Firebase is retained according to Google's policies:
- **Analytics**: Up to 14 months
- **Crashlytics**: 90 days
- **Performance Monitoring**: 90 days

For details, see [Firebase Data Privacy](https://firebase.google.com/support/privacy).

## Your Choices

- **Analytics**: Opt-in via Privacy settings (disabled by default)
- **Crash & Performance Reports**: Opt-out via Privacy settings
- **Permissions**: Revoke calendar, notifications, or DND access in Android settings
- **Local Data**: Uninstall the app or clear app data to remove all local data
- **Telemetry**: Open About and enable Debug Tools, then go to Settings > Debug Tools and disable Telemetry

## Data Subject Rights (Firebase)

If you have opted into Firebase services and wish to exercise data subject rights (access, deletion, etc.), these requests should be directed to Google as the data processor. See [Google's Privacy Policy](https://policies.google.com/privacy) and [Firebase Data Processing Terms](https://firebase.google.com/terms/data-processing-terms) for more information.

Calendar DND does not maintain user accounts or collect personal identifiers directly. The app uses Firebase's anonymous identifiers for analytics and crash reporting.

## Contact

If you have questions about this privacy policy, contact: apps@pavelja.me
