# Features

Detailed documentation of Calendar DND features.

## Core Automation

### Automatic DND Control

The app monitors your calendar and automatically:
- **Enables DND** when a meeting starts
- **Disables DND** when a meeting ends

No manual intervention required after initial setup.

### Smart Meeting Merging

Back-to-back or overlapping meetings are merged into a single DND window:

```
Without merging:
  Meeting 1     Meeting 2
  [DND ON]      [DND ON]
        ↑ DND OFF ↑
        (interruption)

With merging:
  Meeting 1     Meeting 2
  [────── DND ON ──────]
        (no interruption)
```

This prevents your phone from making noise between consecutive meetings.

### User Override Respect

If you manually change DND during a meeting, the app detects this and stops interfering:

1. You're in a meeting, DND is on
2. You manually disable DND (maybe expecting an important call)
3. App detects the override
4. App stops managing DND until meeting ends
5. After meeting, normal automation resumes

## Event Filtering

### Calendar Selection

Choose which calendars trigger DND:
- **All Calendars**: Monitor everything
- **Specific Calendars**: Only selected calendars

Useful if you have personal and work calendars and only want DND for work meetings.

### Busy-Only Filter

Only trigger DND for events marked as "Busy" in your calendar. Events marked "Free" or "Tentative" are ignored.

### All-Day Event Filter

Ignore all-day events (like birthdays, holidays). These typically don't require silence.

### Minimum Duration

Set a minimum event length. Short events (like 5-minute reminders) can be ignored.

### Keyword Filter (Experimental)

Only trigger DND for events with specific words in the title.

**Match Modes**:
- **Contains**: Title contains the keyword
- **Starts With**: Title starts with keyword
- **Exact Match**: Title equals keyword exactly
- **Regex**: Regular expression matching

**Example**: Set keyword to "Meeting" to only silence events with "Meeting" in the title.

## DND Modes

### Priority Only

Allows priority interruptions through:
- Starred contacts
- Repeat callers
- Alarms
- Media sounds

Configure priority settings in Android's DND settings.

### Total Silence

Complete silence. No sounds, vibrations, or visual interruptions.

## Timing Options

### Start Offset

Adjust when DND activates relative to meeting start:

| Setting | Behavior |
|---------|----------|
| -5 minutes | DND starts 5 min before meeting |
| 0 minutes | DND starts exactly at meeting time |
| +5 minutes | DND starts 5 min after meeting |

Negative offset useful if you need quiet time to prepare.

### Pre-Meeting Notifications

Get a notification 5 minutes before DND activates. Gives you a heads-up to wrap up calls or reply to messages.

## Manual DND

Set DND manually with a timer:
- 15 minutes
- 30 minutes
- 1 hour
- 2 hours
- Until next meeting ends

Useful when you need silence outside of scheduled meetings.

## Meeting Overrun

When a meeting ends but you're still in it:

1. Meeting ends (per calendar)
2. No immediate next meeting (>5 min gap)
3. App shows notification: "Meeting ended - extend DND?"
4. Tap to extend DND

## Localization

Supported languages:
- English
- German (Deutsch)
- Croatian (Hrvatski)
- Italian (Italiano)
- Korean (한국어)
- Chinese (中文)

Language can be changed in Settings without restarting the app.

## Debug Tools

Available in Settings > Debug Tools:

### Debug Logs

View all engine execution logs:
- Timestamp
- Decision made
- Events considered
- State changes

Useful for troubleshooting "why didn't DND activate?"

### Run Engine Now

Manually trigger the automation engine. Useful for testing without waiting for a meeting.

### Debug Overlay

Shows current automation state on screen:
- Current route
- DND status
- Next boundary

## Notifications

### Pre-DND Warning

5 minutes before meeting, shows notification with:
- Meeting title
- Time until DND activates

Tap to dismiss or snooze.

### Missing Permissions

If permissions are revoked, the app notifies you and provides a quick link to fix.

### Meeting Overrun

After meeting ends, option to extend DND if you're still busy.

## Quick Settings Tile

Toggle automation on/off from Quick Settings without opening the app.

## Battery Optimization

The app uses minimal resources:
- No constant background service
- Only wakes at meeting boundaries
- Efficient alarm scheduling

For best reliability, disable Android's battery optimization for the app (Settings guides you through this).

## Samsung Optimization

Special handling for Samsung devices:
- Defensive error handling around DND calls
- Multiple scheduling strategies
- Guide for battery optimization settings

## Privacy

- All calendar data processed on-device
- No data sent to servers
- No account required
- Event titles hashed in debug logs (privacy protection)
- Optional analytics (explicit opt-in)
