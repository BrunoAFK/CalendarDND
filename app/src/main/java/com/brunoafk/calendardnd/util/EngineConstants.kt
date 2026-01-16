package com.brunoafk.calendardnd.util

/**
 * Centralized constants for the automation engine and scheduling.
 * All time values are in milliseconds unless otherwise noted.
 */
object EngineConstants {

    // Scheduling
    const val SANITY_WORKER_INTERVAL_MINUTES = 15L
    const val NEAR_TERM_THRESHOLD_MS = 60 * 60 * 1000L
    const val GUARD_OFFSET_MS = 2 * 60 * 1000L
    const val GUARD_MIN_DELAY_MS = 10 * 1000L

    // Notifications
    const val PRE_DND_NOTIFICATION_LEAD_MS = 5 * 60 * 1000L
    const val PRE_DND_NOTIFICATION_MIN_DELAY_MS = 5 * 1000L

    // Meeting detection
    const val MEETING_OVERRUN_THRESHOLD_MS = 2 * 60 * 1000L
    const val MEETING_GAP_THRESHOLD_MS = 5 * 60 * 1000L

    // Calendar queries
    const val ACTIVE_INSTANCES_WINDOW_MS = 6 * 60 * 60 * 1000L
    const val NEXT_INSTANCE_LOOKAHEAD_MS = 7 * 24 * 60 * 60 * 1000L
    const val CALENDAR_QUERY_TIMEOUT_MS = 5 * 1000L

    // Debounce
    const val CALENDAR_OBSERVER_DEBOUNCE_MS = 10 * 1000L
}
