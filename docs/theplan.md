# The Plan: Targeted Improvements

This document outlines concrete tasks and implementation details for the next set of improvements.

---

## 1) Centralize Engine/Scheduler Constants

### Goal
Remove magic numbers across engine and scheduling code to avoid drift and improve maintainability.

### Tasks
- Create a dedicated constants object.
- Replace hardcoded timing values in engine, scheduling, calendar queries, and observers.
- Ensure all constants are in milliseconds unless explicitly documented otherwise.

### Implementation Details
- Add `app/src/main/java/com/brunoafk/calendardnd/util/EngineConstants.kt` with:
  - Scheduling constants (guard offsets, near-term thresholds, sanity interval).
  - Notification constants (pre-DND lead and min delay).
  - Meeting detection constants (overrun thresholds).
  - Calendar query windows (active/next lookahead).
  - Debounce constants (calendar observer).
- Replace values in:
  - `app/src/main/java/com/brunoafk/calendardnd/domain/engine/AutomationEngine.kt`
  - `app/src/main/java/com/brunoafk/calendardnd/domain/planning/SchedulePlanner.kt`
  - `app/src/main/java/com/brunoafk/calendardnd/data/calendar/CalendarRepository.kt`
  - `app/src/main/java/com/brunoafk/calendardnd/system/alarms/EngineRunner.kt`
  - `app/src/main/java/com/brunoafk/calendardnd/system/workers/Workers.kt`
  - `app/src/main/java/com/brunoafk/calendardnd/data/calendar/CalendarObserver.kt`
- Add or update unit tests for any logic depending on thresholds (if tests exist).

### Notes
This task is largely specified in `docs/TASKS.md` and should follow that spec.

---

## 2) Tighten User Override Detection Edge Cases

### Goal
Avoid incorrect override detection when system DND changes while the app "owns" DND, and prevent false suppression states.

### Tasks
- Audit `detectUserOverride()` and ownership logic for edge cases:
  - System DND toggled manually while app owns it.
  - System DND toggled by other apps.
  - DND policy access revoked mid-session.
  - Filter mismatch (system filter differs from app target).
- Add explicit comparisons between expected DND mode and current system mode.
- Ensure suppression timers are set only when the app's desired state conflicts with system state.

### Implementation Details
- Locate `detectUserOverride()` in the engine (likely `domain/engine/AutomationEngine.kt`).
- Update detection criteria:
  - Require `dndSetByApp == true`
  - Require active DND window
  - Require system DND state not matching expected state
  - If system DND is off but app expects on, treat as manual override only if
    `hasPolicyAccess == true` and `dndSetByApp == true` (avoid false positives)
  - If system DND is on but filter mode differs from expected, treat as override
    only if user likely changed it (track last app-set mode if available)
- Consider recording the last DND mode set by the app (in `RuntimeStateStore`)
  so comparisons are stable across runs.
- Add tests around:
  - Manual DND disable during active window.
  - Manual DND mode change (Priority → Total silence) during active window.
  - App loses policy access while DND is on.

### Notes
If runtime state does not already track the last app-set DND filter, add a field
in `RuntimeStateStore` and persist it when the app sets DND.

---

## 3) "Test Run" Button (Dry Run) in Debug Menu

### Goal
Provide a debug-only action that simulates the next engine decision without
changing system DND or scheduling alarms.

### Tasks
- Add a debug menu action labeled "Test Run (Dry)".
- Run the engine with the current input but in dry-run mode:
  - No calls to DND controller.
  - No schedule changes (alarms/workers/notifications).
  - Log output should clearly state it is a dry run.
- Display results in debug logs (or a toast/snackbar).

### Implementation Details
- Add a flag to `EngineRunner` or engine invocation to indicate dry run.
- Introduce a "no-op" DND controller/scheduler in dry-run mode or guard
  the side-effecting calls with `if (!dryRun)`.
- UI:
  - Add button to `DebugToolsScreen` or equivalent debug menu screen.
  - Trigger a background execution using same pipeline as "Run Engine Now"
    but pass `dryRun = true`.
- Logging:
  - Append "DRY RUN" to log entries.
  - Include decision summary (enable/disable, next boundary).

### Notes
Keep this feature behind the existing debug tools flag to avoid production use.

---

## 4) Quick Settings Tile: Long-Press Behavior

### Goal
Long-pressing the Quick Settings tile should open the Status screen and provide
a hint about the automation toggle state.

### Tasks
- Implement `onStartListening()` or `onClick()` override to provide a long-press
  intent target.
- Ensure long-press opens the app to the Status screen.
- Add a hint message/banner when launched from tile long-press.

### Implementation Details
- Update `app/src/main/java/com/brunoafk/calendardnd/ui/tiles/AutomationTileService.kt`:
  - Use `TileService.startActivityAndCollapse()` with an intent for Status route.
  - Add an extra flag: `EXTRA_LAUNCHED_FROM_TILE = true`.
- In `MainActivity` or `StatusScreen`, detect the extra and show a hint:
  - Example: "Automation is OFF. Toggle it to enable DND automation."
- Ensure the hint is one-time per launch to avoid repeated banners.

### Notes
Android handles long-press by launching the activity defined in the tile intent.
Use that hook to open the Status screen directly.

---

## 5) Shareable Debug Log Summary (Redacted)

### Goal
Allow users to share a debug summary that includes redacted device info and a
settings header, suitable for support and issue reporting.

### Tasks
- Add a "Share Debug Summary" action in Debug Logs screen.
- Build a structured header:
  - App version (name/code)
  - Android version
  - Device manufacturer/model (redacted or generalized)
  - Language
  - Key settings (automation, filters, DND mode, offset, pre-DND notification)
- Redact device identifiers:
  - Use a sanitized model name (e.g., "Samsung Galaxy Sxx" or "Samsung Device")
  - Avoid serial/IMEI/Android ID.
- Append recent debug logs below the header.
- Use `ACTION_SEND` with `text/plain`.

### Implementation Details
- Update `app/src/main/java/com/brunoafk/calendardnd/ui/screens/DebugLogScreen.kt`:
  - Add new action button next to "Copy Logs".
  - Reuse existing log header generation but redact device info.
- If headers are created in `DebugLogStore`, add a new method for redacted header:
  - `buildRedactedHeader(context)` or similar.
- Implement redaction helper:
  - If model/manufacturer known, map to generic buckets (e.g., "Samsung Device").
  - Keep Android API level only (no build fingerprint).

### Notes
Keep the share payload short and safe for issue reports. Avoid PII.

---

## Suggested Order
1) Centralize constants (low risk, unlocks cleanup).
2) Tighten override detection (logic correctness).
3) Debug dry-run action (safe debug tooling).
4) Tile long-press behavior (UX polish).
5) Shareable debug summary (support tooling).
