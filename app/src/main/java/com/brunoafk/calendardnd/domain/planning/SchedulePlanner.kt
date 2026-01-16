package com.brunoafk.calendardnd.domain.planning

import com.brunoafk.calendardnd.util.EngineConstants.GUARD_MIN_DELAY_MS
import com.brunoafk.calendardnd.util.EngineConstants.GUARD_OFFSET_MS
import com.brunoafk.calendardnd.util.EngineConstants.NEAR_TERM_THRESHOLD_MS

/**
 * Plans the next boundary alarm and guard jobs
 */
object SchedulePlanner {

    data class SchedulePlan(
        val nextBoundaryMs: Long?, // null means no boundary to schedule
        val needsNearTermGuards: Boolean, // true if exact alarms unavailable and boundary is soon
        val guardBeforeMs: Long?, // guard job before boundary
        val guardAfterMs: Long? // guard job after boundary
    )

    /**
     * Determine the next scheduling plan
     */
    fun planNextSchedule(
        now: Long,
        dndWindowStartMs: Long?,
        dndWindowEndMs: Long?,
        dndWindowActive: Boolean,
        hasExactAlarms: Boolean
    ): SchedulePlan {
        // Determine the next boundary
        val nextBoundary = when {
            dndWindowActive -> dndWindowEndMs // End of current DND window
            dndWindowStartMs != null && dndWindowStartMs > now -> dndWindowStartMs // Next DND start
            else -> null // No upcoming boundary
        }

        // If no boundary, nothing to schedule
        if (nextBoundary == null) {
            return SchedulePlan(
                nextBoundaryMs = null,
                needsNearTermGuards = false,
                guardBeforeMs = null,
                guardAfterMs = null
            )
        }

        // Check if boundary is within 60 minutes
        val timeUntilBoundary = nextBoundary - now
        val isNearTerm = timeUntilBoundary in 1..NEAR_TERM_THRESHOLD_MS

        // If we don't have exact alarms and boundary is near-term, use guards
        val needsGuards = !hasExactAlarms && isNearTerm

        val guardBefore = if (needsGuards) {
            // Schedule guard 2 minutes before, but at least 10 seconds from now
            maxOf(now + GUARD_MIN_DELAY_MS, nextBoundary - GUARD_OFFSET_MS)
        } else null

        val guardAfter = if (needsGuards) {
            // Schedule guard 2 minutes after boundary
            nextBoundary + GUARD_OFFSET_MS
        } else null

        return SchedulePlan(
            nextBoundaryMs = nextBoundary,
            needsNearTermGuards = needsGuards,
            guardBeforeMs = guardBefore,
            guardAfterMs = guardAfter
        )
    }
}
