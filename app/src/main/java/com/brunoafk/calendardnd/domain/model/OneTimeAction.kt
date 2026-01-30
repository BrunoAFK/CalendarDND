package com.brunoafk.calendardnd.domain.model

/**
 * Represents a one-time action that can be applied to an event.
 * Used when the user wants to skip DND for a specific event or
 * enable DND for an event when automation is off.
 */
sealed class OneTimeAction {
    /**
     * Enable DND for a specific event window when automation is OFF.
     */
    data class EnableForEvent(
        val event: EventInstance,
        val startMs: Long,
        val endMs: Long
    ) : OneTimeAction()

    /**
     * Skip DND for a specific event when automation is ON.
     */
    data class SkipEvent(
        val event: EventInstance,
        val startMs: Long,
        val endMs: Long
    ) : OneTimeAction()
}

/**
 * Type of one-time action currently active for an event.
 */
enum class OneTimeActionType {
    SKIP,
    ENABLE
}

/**
 * Represents the state needed to identify a skipped event.
 */
data class SkippedEventState(
    val eventId: Long,
    val beginMs: Long,
    val endMs: Long
) {
    companion object {
        val NONE = SkippedEventState(0L, 0L, 0L)
    }

    val isActive: Boolean
        get() = eventId > 0L && beginMs > 0L
}
