package com.brunoafk.calendardnd.domain.engine

import com.brunoafk.calendardnd.data.calendar.CalendarInfo
import com.brunoafk.calendardnd.data.calendar.ICalendarRepository
import com.brunoafk.calendardnd.domain.model.DndMode
import com.brunoafk.calendardnd.domain.model.EventInstance
import com.brunoafk.calendardnd.domain.model.KeywordMatchMode
import com.brunoafk.calendardnd.domain.model.Trigger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AutomationEngineTest {

    private lateinit var engine: AutomationEngine
    private lateinit var mockCalendarRepository: MockCalendarRepository
    private lateinit var mockTimeFormatter: MockTimeFormatter

    @Before
    fun setup() {
        mockCalendarRepository = MockCalendarRepository()
        mockTimeFormatter = MockTimeFormatter()
        engine = AutomationEngine(mockCalendarRepository, mockTimeFormatter)
    }

    // ============================================
    // RULE 1: Automation OFF
    // ============================================

    @Test
    fun `automation OFF with app owning DND should disable DND`() = runBlocking {
        val input = createEngineInput(
            automationEnabled = false,
            dndSetByApp = true,
            systemDndIsOn = true
        )

        val output = engine.run(input)

        assertFalse("Should disable DND", output.decision.shouldEnableDnd)
        assertTrue("Should disable DND", output.decision.shouldDisableDnd)
        assertEquals("Should clear ownership", false, output.decision.setDndSetByApp)
        assertNull("Should have no schedule", output.schedulePlan)
    }

    @Test
    fun `automation OFF without app owning DND should not touch DND`() = runBlocking {
        val input = createEngineInput(
            automationEnabled = false,
            dndSetByApp = false,
            systemDndIsOn = true
        )

        val output = engine.run(input)

        assertFalse("Should not enable DND", output.decision.shouldEnableDnd)
        assertFalse("Should not disable DND", output.decision.shouldDisableDnd)
    }

    // ============================================
    // RULE 2: Missing Permissions
    // ============================================

    @Test
    fun `missing calendar permission should not enable DND`() = runBlocking {
        val input = createEngineInput(
            automationEnabled = true,
            hasCalendarPermission = false,
            hasPolicyAccess = true
        )

        val output = engine.run(input)

        assertFalse("Should not enable DND", output.decision.shouldEnableDnd)
        assertFalse("Should not disable DND", output.decision.shouldDisableDnd)
        assertEquals("Should notify setup required",
            NotificationNeeded.SETUP_REQUIRED,
            output.decision.notificationNeeded)
    }

    @Test
    fun `missing policy access should not enable DND`() = runBlocking {
        val input = createEngineInput(
            automationEnabled = true,
            hasCalendarPermission = true,
            hasPolicyAccess = false
        )

        val output = engine.run(input)

        assertFalse("Should not enable DND", output.decision.shouldEnableDnd)
        assertFalse("Should not disable DND", output.decision.shouldDisableDnd)
        assertEquals("Should notify setup required",
            NotificationNeeded.SETUP_REQUIRED,
            output.decision.notificationNeeded)
    }

    // ============================================
    // RULE 3: Active Meeting, Not Suppressed
    // ============================================

    @Test
    fun `active meeting should enable DND`() = runBlocking {
        val now = 1000L
        val meetingStart = 900L
        val meetingEnd = 1200L

        mockCalendarRepository.activeInstances = listOf(
            EventInstance(
                id = 1L,
                eventId = 1L,
                calendarId = 1L,
                title = "Test Meeting",
                begin = meetingStart,
                end = meetingEnd,
                allDay = false,
                availability = 0
            )
        )

        val input = createEngineInput(
            now = now,
            automationEnabled = true,
            hasCalendarPermission = true,
            hasPolicyAccess = true,
            systemDndIsOn = false,
            dndSetByApp = false
        )

        val output = engine.run(input)

        assertTrue("Should enable DND", output.decision.shouldEnableDnd)
        assertFalse("Should not disable DND", output.decision.shouldDisableDnd)
        assertEquals("Should set ownership", true, output.decision.setDndSetByApp)
        assertEquals("Should set active window end", meetingEnd, output.decision.setActiveWindowEnd)
    }

    @Test
    fun `active meeting with DND already on should not re-enable`() = runBlocking {
        val now = 1000L
        val meetingStart = 900L
        val meetingEnd = 1200L

        mockCalendarRepository.activeInstances = listOf(
            EventInstance(
                id = 1L,
                eventId = 1L,
                calendarId = 1L,
                title = "Test Meeting",
                begin = meetingStart,
                end = meetingEnd,
                allDay = false,
                availability = 0
            )
        )

        val input = createEngineInput(
            now = now,
            automationEnabled = true,
            hasCalendarPermission = true,
            hasPolicyAccess = true,
            systemDndIsOn = true,
            dndSetByApp = true
        )

        val output = engine.run(input)

        assertFalse("Should not re-enable DND", output.decision.shouldEnableDnd)
        assertFalse("Should not disable DND", output.decision.shouldDisableDnd)
        assertNull("Should not change ownership", output.decision.setDndSetByApp)
    }

    // ============================================
    // RULE 4: Active Meeting, User Suppressed
    // ============================================

    @Test
    fun `active meeting with user override should not enable DND`() = runBlocking {
        val now = 1000L
        val meetingStart = 900L
        val meetingEnd = 1200L

        mockCalendarRepository.activeInstances = listOf(
            EventInstance(
                id = 1L,
                eventId = 1L,
                calendarId = 1L,
                title = "Test Meeting",
                begin = meetingStart,
                end = meetingEnd,
                allDay = false,
                availability = 0
            )
        )

        val input = createEngineInput(
            now = now,
            automationEnabled = true,
            hasCalendarPermission = true,
            hasPolicyAccess = true,
            systemDndIsOn = false,
            dndSetByApp = true, // App thought it owned DND
            userSuppressedUntilMs = meetingEnd
        )

        val output = engine.run(input)

        assertFalse("Should not enable DND", output.decision.shouldEnableDnd)
        assertFalse("Should not disable DND", output.decision.shouldDisableDnd)
    }

    @Test
    fun `user manually disables DND during meeting should trigger suppression`() = runBlocking {
        val now = 1000L
        val meetingStart = 900L
        val meetingEnd = 1200L

        mockCalendarRepository.activeInstances = listOf(
            EventInstance(
                id = 1L,
                eventId = 1L,
                calendarId = 1L,
                title = "Test Meeting",
                begin = meetingStart,
                end = meetingEnd,
                allDay = false,
                availability = 0
            )
        )

        val input = createEngineInput(
            now = now,
            automationEnabled = true,
            hasCalendarPermission = true,
            hasPolicyAccess = true,
            systemDndIsOn = false, // User turned it off
            dndSetByApp = true, // But app thought it owned DND
            userSuppressedUntilMs = 0 // Not yet suppressed
        )

        val output = engine.run(input)

        assertFalse("Should not re-enable DND", output.decision.shouldEnableDnd)
        assertEquals("Should set suppression", meetingEnd, output.decision.setUserSuppressedUntil)
        assertEquals("Should clear ownership", false, output.decision.setDndSetByApp)
    }

    @Test
    fun `user changes DND mode during meeting should trigger suppression`() = runBlocking {
        val now = 1000L
        val meetingStart = 900L
        val meetingEnd = 1200L

        mockCalendarRepository.activeInstances = listOf(
            EventInstance(
                id = 1L,
                eventId = 1L,
                calendarId = 1L,
                title = "Test Meeting",
                begin = meetingStart,
                end = meetingEnd,
                allDay = false,
                availability = 0
            )
        )

        val input = createEngineInput(
            now = now,
            automationEnabled = true,
            hasCalendarPermission = true,
            hasPolicyAccess = true,
            systemDndIsOn = true,
            dndSetByApp = true,
            dndMode = DndMode.PRIORITY,
            lastKnownDndFilter = DndMode.PRIORITY.filterValue,
            currentSystemFilter = DndMode.TOTAL_SILENCE.filterValue
        )

        val output = engine.run(input)

        assertFalse("Should not re-enable DND", output.decision.shouldEnableDnd)
        assertEquals("Should set suppression", meetingEnd, output.decision.setUserSuppressedUntil)
        assertEquals("Should clear ownership", false, output.decision.setDndSetByApp)
    }

    // ============================================
    // RULE 5: No Active Meeting
    // ============================================

    @Test
    fun `no active meeting with app owning DND should disable DND`() = runBlocking {
        mockCalendarRepository.activeInstances = emptyList()

        val input = createEngineInput(
            automationEnabled = true,
            hasCalendarPermission = true,
            hasPolicyAccess = true,
            systemDndIsOn = true,
            dndSetByApp = true
        )

        val output = engine.run(input)

        assertFalse("Should not enable DND", output.decision.shouldEnableDnd)
        assertTrue("Should disable DND", output.decision.shouldDisableDnd)
        assertEquals("Should clear ownership", false, output.decision.setDndSetByApp)
        assertEquals("Should clear active window", 0L, output.decision.setActiveWindowEnd)
    }

    @Test
    fun `no active meeting without app owning DND should not disable DND`() = runBlocking {
        mockCalendarRepository.activeInstances = emptyList()

        val input = createEngineInput(
            automationEnabled = true,
            hasCalendarPermission = true,
            hasPolicyAccess = true,
            systemDndIsOn = true,
            dndSetByApp = false // User enabled DND manually
        )

        val output = engine.run(input)

        assertFalse("Should not enable DND", output.decision.shouldEnableDnd)
        assertFalse("Should not disable DND (not owned)", output.decision.shouldDisableDnd)
    }

    // ============================================
    // DND Start Offset Tests
    // ============================================

    @Test
    fun `positive offset should delay DND start`() = runBlocking {
        val now = 900L
        val meetingStart = 1000L
        val meetingEnd = 1200L
        val offset = 5 // 5 minutes = 300,000ms
        val expectedDndStart = meetingStart + (offset * 60_000L) // 1300L

        mockCalendarRepository.activeInstances = emptyList()
        mockCalendarRepository.nextInstance = EventInstance(
            id = 1L,
                eventId = 1L,
                calendarId = 1L,
            title = "Future Meeting",
            begin = meetingStart,
            end = meetingEnd,
            allDay = false,
            availability = 0
        )

        val input = createEngineInput(
            now = now,
            automationEnabled = true,
            hasCalendarPermission = true,
            hasPolicyAccess = true,
            dndStartOffsetMinutes = offset
        )

        val output = engine.run(input)

        // DND should not be active yet (now < expectedDndStart)
        assertFalse("Should not enable DND yet", output.decision.shouldEnableDnd)
    }

    @Test
    fun `negative offset should advance DND start`() = runBlocking {
        val now = 900L
        val meetingStart = 1000L
        val meetingEnd = 1200L
        val offset = -5 // -5 minutes = start 5 min early
        val expectedDndStart = meetingStart + (offset * 60_000L) // 700L

        mockCalendarRepository.activeInstances = emptyList()
        mockCalendarRepository.nextInstance = EventInstance(
            id = 1L,
                eventId = 1L,
                calendarId = 1L,
            title = "Future Meeting",
            begin = meetingStart,
            end = meetingEnd,
            allDay = false,
            availability = 0
        )

        val input = createEngineInput(
            now = now,
            automationEnabled = true,
            hasCalendarPermission = true,
            hasPolicyAccess = true,
            dndStartOffsetMinutes = offset
        )

        val output = engine.run(input)

        // DND should already be active (now > expectedDndStart)
        assertTrue("Should enable DND early", output.decision.shouldEnableDnd)
    }

    @Test
    fun `offset that pushes start past end should not activate DND`() = runBlocking {
        val now = 1000L
        val meetingStart = 1000L
        val meetingEnd = 1100L
        val offset = 10 // 10 minutes = 600,000ms, pushes start to 1600L (past 1100L end)

        mockCalendarRepository.activeInstances = listOf(
            EventInstance(
                id = 1L,
                eventId = 1L,
                calendarId = 1L,
                title = "Short Meeting",
                begin = meetingStart,
                end = meetingEnd,
                allDay = false,
                availability = 0
            )
        )

        val input = createEngineInput(
            now = now,
            automationEnabled = true,
            hasCalendarPermission = true,
            hasPolicyAccess = true,
            dndStartOffsetMinutes = offset
        )

        val output = engine.run(input)

        // DND should not activate because offset makes it invalid
        assertFalse("Should not enable DND", output.decision.shouldEnableDnd)
    }

    // ============================================
    // Manual DND Mode Tests
    // ============================================

    @Test
    fun `active manual DND should take precedence over calendar`() = runBlocking {
        val now = 1000L
        val manualUntil = 2000L

        mockCalendarRepository.activeInstances = emptyList() // No calendar events

        val input = createEngineInput(
            now = now,
            automationEnabled = true,
            hasCalendarPermission = true,
            hasPolicyAccess = true,
            manualDndUntilMs = manualUntil,
            systemDndIsOn = false
        )

        val output = engine.run(input)

        assertTrue("Should enable DND for manual mode", output.decision.shouldEnableDnd)
        assertEquals("DND window should end at manual time", manualUntil, output.dndWindowEndMs)
    }

    @Test
    fun `expired manual DND should be cleared`() = runBlocking {
        val now = 2000L
        val manualUntil = 1500L // Expired

        mockCalendarRepository.activeInstances = emptyList()

        val input = createEngineInput(
            now = now,
            automationEnabled = true,
            hasCalendarPermission = true,
            hasPolicyAccess = true,
            manualDndUntilMs = manualUntil,
            systemDndIsOn = true,
            dndSetByApp = true
        )

        val output = engine.run(input)

        assertEquals("Should clear expired manual DND", 0L, output.decision.setManualDndUntilMs)
        assertTrue("Should disable DND", output.decision.shouldDisableDnd)
    }

    // ============================================
    // Helper Methods
    // ============================================

    private fun createEngineInput(
        now: Long = System.currentTimeMillis(),
        automationEnabled: Boolean = true,
        selectedCalendarIds: Set<String> = emptySet(),
        busyOnly: Boolean = true,
        ignoreAllDay: Boolean = true,
        minEventMinutes: Int = 10,
        dndMode: DndMode = DndMode.PRIORITY,
        dndStartOffsetMinutes: Int = 0,
        preDndNotificationEnabled: Boolean = false,
        requireTitleKeyword: Boolean = false,
        titleKeyword: String = "",
        titleKeywordMatchMode: KeywordMatchMode = KeywordMatchMode.KEYWORDS,
        dndSetByApp: Boolean = false,
        activeWindowEndMs: Long = 0,
        userSuppressedUntilMs: Long = 0,
        manualDndUntilMs: Long = 0,
        lastKnownDndFilter: Int = -1,
        hasCalendarPermission: Boolean = true,
        hasPolicyAccess: Boolean = true,
        hasExactAlarms: Boolean = true,
        systemDndIsOn: Boolean = false,
        currentSystemFilter: Int = 1
    ): EngineInput {
        return EngineInput(
            trigger = Trigger.MANUAL,
            now = now,
            automationEnabled = automationEnabled,
            selectedCalendarIds = selectedCalendarIds,
            busyOnly = busyOnly,
            ignoreAllDay = ignoreAllDay,
            minEventMinutes = minEventMinutes,
            dndMode = dndMode,
            dndStartOffsetMinutes = dndStartOffsetMinutes,
            preDndNotificationEnabled = preDndNotificationEnabled,
            requireTitleKeyword = requireTitleKeyword,
            titleKeyword = titleKeyword,
            titleKeywordMatchMode = titleKeywordMatchMode,
            dndSetByApp = dndSetByApp,
            activeWindowEndMs = activeWindowEndMs,
            userSuppressedUntilMs = userSuppressedUntilMs,
            manualDndUntilMs = manualDndUntilMs,
            lastKnownDndFilter = lastKnownDndFilter,
            hasCalendarPermission = hasCalendarPermission,
            hasPolicyAccess = hasPolicyAccess,
            hasExactAlarms = hasExactAlarms,
            systemDndIsOn = systemDndIsOn,
            currentSystemFilter = currentSystemFilter
        )
    }

    // ============================================
    // Mock Classes
    // ============================================

    private class MockCalendarRepository : ICalendarRepository {
        var activeInstances: List<EventInstance> = emptyList()
        var nextInstance: EventInstance? = null

        override suspend fun getActiveInstances(
            now: Long,
            selectedCalendarIds: Set<String>,
            busyOnly: Boolean,
            ignoreAllDay: Boolean,
            minEventMinutes: Int,
            requireTitleKeyword: Boolean,
            titleKeyword: String,
            titleKeywordMatchMode: KeywordMatchMode
        ): List<EventInstance> {
            return activeInstances
        }

        override suspend fun getNextInstance(
            now: Long,
            selectedCalendarIds: Set<String>,
            busyOnly: Boolean,
            ignoreAllDay: Boolean,
            minEventMinutes: Int,
            requireTitleKeyword: Boolean,
            titleKeyword: String,
            titleKeywordMatchMode: KeywordMatchMode
        ): EventInstance? {
            return nextInstance
        }

        override suspend fun getCalendars(): List<CalendarInfo> {
            return emptyList()
        }
    }

    private class MockTimeFormatter : TimeFormatter {
        override fun formatTime(timestampMs: Long): String {
            return timestampMs.toString()
        }
    }
}
