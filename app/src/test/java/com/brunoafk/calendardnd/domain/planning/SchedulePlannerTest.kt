package com.brunoafk.calendardnd.domain.planning

import com.brunoafk.calendardnd.util.EngineConstants.GUARD_MIN_DELAY_MS
import com.brunoafk.calendardnd.util.EngineConstants.GUARD_OFFSET_MS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SchedulePlannerTest {

    @Test
    fun `near-term boundary without exact alarms schedules guards`() {
        val now = 1_000L
        val boundary = 10_000L

        val plan = SchedulePlanner.planNextSchedule(
            now = now,
            dndWindowStartMs = boundary,
            dndWindowEndMs = null,
            dndWindowActive = false,
            hasExactAlarms = false
        )

        val expectedGuardBefore = maxOf(now + GUARD_MIN_DELAY_MS, boundary - GUARD_OFFSET_MS)

        assertTrue(plan.needsNearTermGuards)
        assertEquals(boundary, plan.nextBoundaryMs)
        assertEquals(expectedGuardBefore, plan.guardBeforeMs)
        assertEquals(boundary + GUARD_OFFSET_MS, plan.guardAfterMs)
    }
}
