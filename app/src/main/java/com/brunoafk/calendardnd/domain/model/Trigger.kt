package com.brunoafk.calendardnd.domain.model

/**
 * Trigger source for engine runs
 */
enum class Trigger {
    ALARM,
    WORKER_SANITY,
    WORKER_GUARD,
    CALENDAR_CHANGE,
    BOOT,
    TIME_CHANGE,
    USER_TOGGLE,
    TILE_TOGGLE,
    MANUAL
}
