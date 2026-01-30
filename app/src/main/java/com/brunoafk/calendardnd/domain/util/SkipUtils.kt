package com.brunoafk.calendardnd.domain.util

import com.brunoafk.calendardnd.domain.model.EventInstance
import com.brunoafk.calendardnd.domain.model.SkippedEventState

/**
 * Utility functions for skip event logic.
 * Centralized to ensure consistent behavior across UI, Engine, and tests.
 */
object SkipUtils {

    /**
     * Checks if the given event matches the currently skipped event.
     *
     * @param event The event to check
     * @param skippedState The current skip state
     * @param nowMs Current time in milliseconds (for expiration check)
     * @return true if this event is currently skipped
     */
    fun isSkippedEvent(
        event: EventInstance?,
        skippedState: SkippedEventState,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        if (event == null) return false
        if (!skippedState.isActive) return false
        if (event.eventId != skippedState.eventId) return false
        if (event.begin != skippedState.beginMs) return false
        // Skip expired if we're past the event end time
        if (skippedState.endMs > 0L && nowMs >= skippedState.endMs) return false
        return true
    }

    /**
     * Overload that accepts individual skip parameters.
     * Useful when you don't have a SkippedEventState object.
     */
    fun isSkippedEvent(
        event: EventInstance?,
        skippedEventId: Long,
        skippedEventBeginMs: Long,
        skippedEventEndMs: Long,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        return isSkippedEvent(
            event,
            SkippedEventState(skippedEventId, skippedEventBeginMs, skippedEventEndMs),
            nowMs
        )
    }

    /**
     * Filters out the skipped event from a list of instances.
     *
     * @param instances List of event instances
     * @param skippedState The current skip state
     * @param nowMs Current time in milliseconds
     * @return List with the skipped event removed
     */
    fun filterOutSkipped(
        instances: List<EventInstance>,
        skippedState: SkippedEventState,
        nowMs: Long = System.currentTimeMillis()
    ): List<EventInstance> {
        if (!skippedState.isActive) return instances
        return instances.filterNot { isSkippedEvent(it, skippedState, nowMs) }
    }

    /**
     * Overload that accepts individual skip parameters.
     */
    fun filterOutSkipped(
        instances: List<EventInstance>,
        skippedEventId: Long,
        skippedEventBeginMs: Long,
        skippedEventEndMs: Long,
        nowMs: Long = System.currentTimeMillis()
    ): List<EventInstance> {
        return filterOutSkipped(
            instances,
            SkippedEventState(skippedEventId, skippedEventBeginMs, skippedEventEndMs),
            nowMs
        )
    }

    /**
     * Checks if the skip state should be cleared (event has ended).
     *
     * @param skippedState The current skip state
     * @param nowMs Current time in milliseconds
     * @return true if the skip should be cleared
     */
    fun shouldClearSkip(
        skippedState: SkippedEventState,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        if (!skippedState.isActive) return false
        return skippedState.endMs > 0L && nowMs >= skippedState.endMs
    }
}
