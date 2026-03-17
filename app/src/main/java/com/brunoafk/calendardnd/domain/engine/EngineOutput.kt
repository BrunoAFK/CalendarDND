package com.brunoafk.calendardnd.domain.engine

import com.brunoafk.calendardnd.domain.model.EventInstance
import com.brunoafk.calendardnd.domain.model.MeetingWindow
import com.brunoafk.calendardnd.domain.planning.SchedulePlanner

/**
 * Output from the automation engine
 */
data class EngineOutput(
    val decision: Decision,
    val activeWindow: MeetingWindow?,
    val nextInstance: EventInstance?,
    val schedulePlan: SchedulePlanner.SchedulePlan?,
    val nextDndStartMs: Long?,
    val dndWindowEndMs: Long?,
    val logMessage: String,
    val userOverrideDetected: Boolean
)

data class Decision(
    val shouldEnableDnd: Boolean, // true = turn ON, false = turn OFF/no change
    val shouldDisableDnd: Boolean, // true = turn OFF
    val shouldEnableVibrate: Boolean, // true = switch ringer to vibrate
    val shouldDisableVibrate: Boolean, // true = restore pre-event ringer mode
    val setDndSetByApp: Boolean?, // new value for ownership flag, null = no change
    val setRingerSetByApp: Boolean?, // new value for ringer ownership flag, null = no change
    val setUserSuppressedUntil: Long?, // new suppression timestamp, null = no change
    val setUserSuppressedFromMs: Long?, // new suppression start timestamp, null = no change
    val setActiveWindowEnd: Long?, // new active window end, null = no change
    val setManualDndUntilMs: Long?, // new manual override end, null = no change
    val setManualEventStartMs: Long?, // new manual event start, null = no change
    val setManualEventEndMs: Long?, // new manual event end, null = no change
    val setLastKnownDndFilter: Int?, // new last known DND filter, null = no change
    val setLastKnownRingerMode: Int?, // new last known ringer mode, null = no change
    val setSavedEventRingerMode: Int?, // saved pre-event ringer mode, null = no change
    val setSkippedEventId: Long?, // new skipped event id, null = no change
    val setSkippedEventBeginMs: Long?, // new skipped event begin, null = no change
    val setSkippedEventEndMs: Long?, // new skipped event end, null = no change
    val setNotifiedNewEventBeforeSkip: Boolean?, // set notification flag, null = no change
    val notificationNeeded: NotificationNeeded
)

enum class NotificationNeeded {
    NONE,
    SETUP_REQUIRED,
    DEGRADED_MODE,
    MEETING_OVERRUN,
    NEW_EVENT_BEFORE_SKIPPED
}
