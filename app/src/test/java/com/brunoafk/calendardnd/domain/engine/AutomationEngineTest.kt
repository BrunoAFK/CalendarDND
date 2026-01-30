package com.brunoafk.calendardnd.domain.engine

import com.brunoafk.calendardnd.data.calendar.CalendarInfo
import com.brunoafk.calendardnd.data.calendar.ICalendarRepository
import com.brunoafk.calendardnd.domain.model.DndMode
import com.brunoafk.calendardnd.domain.model.EventInstance
import com.brunoafk.calendardnd.domain.model.KeywordMatchMode
import com.brunoafk.calendardnd.domain.model.Trigger
import com.brunoafk.calendardnd.util.WeekdayMask
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AutomationEngineTest {

    /** Must match the 1-minute end buffer in AutomationEngine.resolveDndWindow */
    private val DND_END_BUFFER_MS = 60_000L

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
                location = "",
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
        assertEquals("Should set active window end", meetingEnd + DND_END_BUFFER_MS, output.decision.setActiveWindowEnd)
    }

    @Test
    fun `active meeting without exact alarms should mark degraded mode`() = runBlocking {
        val now = 1000L
        val meetingStart = 900L
        val meetingEnd = 1200L

        mockCalendarRepository.activeInstances = listOf(
            EventInstance(
                id = 1L,
                eventId = 1L,
                calendarId = 1L,
                title = "Test Meeting",
                location = "",
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
            hasExactAlarms = false,
            systemDndIsOn = false
        )

        val output = engine.run(input)

        assertEquals("Should notify degraded mode",
            NotificationNeeded.DEGRADED_MODE,
            output.decision.notificationNeeded)
    }

    @Test
    fun `active meeting merges with touching future event`() = runBlocking {
        val now = 1500L
        val meetingStart = 1000L
        val meetingEnd = 2000L
        val nextStart = 2000L
        val nextEnd = 3000L

        mockCalendarRepository.instancesInRange = listOf(
            EventInstance(
                id = 1L,
                eventId = 1L,
                calendarId = 1L,
                title = "Current Meeting",
                location = "",
                begin = meetingStart,
                end = meetingEnd,
                allDay = false,
                availability = 0
            ),
            EventInstance(
                id = 2L,
                eventId = 2L,
                calendarId = 1L,
                title = "Next Meeting",
                location = "",
                begin = nextStart,
                end = nextEnd,
                allDay = false,
                availability = 0
            )
        )
        mockCalendarRepository.nextInstance = EventInstance(
            id = 2L,
            eventId = 2L,
            calendarId = 1L,
            title = "Next Meeting",
            location = "",
            begin = nextStart,
            end = nextEnd,
            allDay = false,
            availability = 0
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
        assertEquals("Should extend active window to merged end", nextEnd + DND_END_BUFFER_MS, output.decision.setActiveWindowEnd)
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
                location = "",
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
                location = "",
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
                location = "",
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
        assertEquals("Should set suppression", meetingEnd + DND_END_BUFFER_MS, output.decision.setUserSuppressedUntil)
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
                location = "",
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
        assertEquals("Should set suppression", meetingEnd + DND_END_BUFFER_MS, output.decision.setUserSuppressedUntil)
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
    fun `no active meeting without exact alarms should mark degraded mode`() = runBlocking {
        mockCalendarRepository.activeInstances = emptyList()

        val input = createEngineInput(
            automationEnabled = true,
            hasCalendarPermission = true,
            hasPolicyAccess = true,
            hasExactAlarms = false,
            systemDndIsOn = false,
            dndSetByApp = false
        )

        val output = engine.run(input)

        assertEquals("Should notify degraded mode",
            NotificationNeeded.DEGRADED_MODE,
            output.decision.notificationNeeded)
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
            location = "",
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
            location = "",
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
                location = "",
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
    // Skip Event Tests
    // ============================================

    @Test
    fun `skipped event should not trigger DND`() = runBlocking {
        val now = 1000L
        val eventStart = 500L
        val eventEnd = 2000L
        val eventId = 123L

        val event = EventInstance(
            id = 1L,
            eventId = eventId,
            calendarId = 1L,
            title = "Skipped Meeting",
            location = "",
            begin = eventStart,
            end = eventEnd,
            allDay = false,
            availability = 0,
            isRecurring = false
        )

        mockCalendarRepository.instancesInRange = listOf(event)

        val input = createEngineInput(
            now = now,
            automationEnabled = true,
            hasCalendarPermission = true,
            hasPolicyAccess = true,
            skippedEventId = eventId,
            skippedEventBeginMs = eventStart,
            skippedEventEndMs = eventEnd
        )

        val output = engine.run(input)

        assertFalse("Should NOT enable DND for skipped event", output.decision.shouldEnableDnd)
        assertNull("Active window should be null", output.activeWindow)
    }

    @Test
    fun `skipped event with overlapping event should still trigger DND for other event`() = runBlocking {
        val now = 1000L
        val eventAId = 123L
        val eventAStart = 500L
        val eventAEnd = 2000L

        val eventBId = 456L
        val eventBStart = 800L
        val eventBEnd = 3000L

        val eventA = EventInstance(
            id = 1L,
            eventId = eventAId,
            calendarId = 1L,
            title = "Skipped Meeting",
            location = "",
            begin = eventAStart,
            end = eventAEnd,
            allDay = false,
            availability = 0,
            isRecurring = false
        )

        val eventB = EventInstance(
            id = 2L,
            eventId = eventBId,
            calendarId = 1L,
            title = "Active Meeting",
            location = "",
            begin = eventBStart,
            end = eventBEnd,
            allDay = false,
            availability = 0,
            isRecurring = false
        )

        mockCalendarRepository.instancesInRange = listOf(eventA, eventB)

        val input = createEngineInput(
            now = now,
            automationEnabled = true,
            hasCalendarPermission = true,
            hasPolicyAccess = true,
            skippedEventId = eventAId,
            skippedEventBeginMs = eventAStart,
            skippedEventEndMs = eventAEnd
        )

        val output = engine.run(input)

        assertTrue("Should enable DND for non-skipped overlapping event", output.decision.shouldEnableDnd)
        assertNotNull("Active window should exist", output.activeWindow)
        assertEquals("Active window should contain only event B", 1, output.activeWindow?.events?.size)
        assertEquals("Active event should be event B", eventBId, output.activeWindow?.events?.firstOrNull()?.eventId)
    }

    @Test
    fun `skip should auto-clear when event ends`() = runBlocking {
        val eventEnd = 1000L
        val now = 1500L // After event ended
        val eventId = 123L
        val eventStart = 500L

        mockCalendarRepository.instancesInRange = emptyList()

        val input = createEngineInput(
            now = now,
            automationEnabled = true,
            hasCalendarPermission = true,
            hasPolicyAccess = true,
            skippedEventId = eventId,
            skippedEventBeginMs = eventStart,
            skippedEventEndMs = eventEnd
        )

        val output = engine.run(input)

        assertEquals("Should clear skipped event ID", 0L, output.decision.setSkippedEventId)
        assertEquals("Should clear skipped event begin", 0L, output.decision.setSkippedEventBeginMs)
        assertEquals("Should clear skipped event end", 0L, output.decision.setSkippedEventEndMs)
    }

    @Test
    fun `skip should not match event with different begin time`() = runBlocking {
        val now = 1000L
        val eventId = 123L
        val originalEventStart = 500L
        val modifiedEventStart = 600L // Calendar event was modified
        val eventEnd = 2000L

        val modifiedEvent = EventInstance(
            id = 1L,
            eventId = eventId,
            calendarId = 1L,
            title = "Modified Meeting",
            location = "",
            begin = modifiedEventStart,
            end = eventEnd,
            allDay = false,
            availability = 0,
            isRecurring = false
        )

        mockCalendarRepository.instancesInRange = listOf(modifiedEvent)

        val input = createEngineInput(
            now = now,
            automationEnabled = true,
            hasCalendarPermission = true,
            hasPolicyAccess = true,
            skippedEventId = eventId,
            skippedEventBeginMs = originalEventStart, // Original start time
            skippedEventEndMs = eventEnd
        )

        val output = engine.run(input)

        assertTrue("Should enable DND since event begin time changed", output.decision.shouldEnableDnd)
        assertNotNull("Active window should exist for modified event", output.activeWindow)
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
        skipRecurring: Boolean = false,
        selectedDaysEnabled: Boolean = false,
        selectedDaysMask: Int = WeekdayMask.ALL_DAYS_MASK,
        minEventMinutes: Int = 10,
        requireLocation: Boolean = false,
        dndMode: DndMode = DndMode.PRIORITY,
        dndStartOffsetMinutes: Int = 0,
        preDndNotificationEnabled: Boolean = false,
        requireTitleKeyword: Boolean = false,
        titleKeyword: String = "",
        titleKeywordMatchMode: KeywordMatchMode = KeywordMatchMode.KEYWORDS,
        titleKeywordCaseSensitive: Boolean = false,
        titleKeywordMatchAll: Boolean = false,
        titleKeywordExclude: Boolean = false,
        dndSetByApp: Boolean = false,
        activeWindowEndMs: Long = 0,
        userSuppressedUntilMs: Long = 0,
        userSuppressedFromMs: Long = 0,
        manualDndUntilMs: Long = 0,
        manualEventStartMs: Long = 0,
        manualEventEndMs: Long = 0,
        lastKnownDndFilter: Int = -1,
        hasCalendarPermission: Boolean = true,
        hasPolicyAccess: Boolean = true,
        hasExactAlarms: Boolean = true,
        systemDndIsOn: Boolean = false,
        currentSystemFilter: Int = 1,
        preDndNotificationLeadMinutes: Int = 5,
        postMeetingNotificationEnabled: Boolean = true,
        postMeetingNotificationOffsetMinutes: Int = 0,
        skippedEventId: Long = 0,
        skippedEventBeginMs: Long = 0,
        skippedEventEndMs: Long = 0,
        notifiedNewEventBeforeSkip: Boolean = false
    ): EngineInput {
        return EngineInput(
            trigger = Trigger.MANUAL,
            now = now,
            automationEnabled = automationEnabled,
            selectedCalendarIds = selectedCalendarIds,
            busyOnly = busyOnly,
            ignoreAllDay = ignoreAllDay,
            skipRecurring = skipRecurring,
            selectedDaysEnabled = selectedDaysEnabled,
            selectedDaysMask = selectedDaysMask,
            minEventMinutes = minEventMinutes,
            requireLocation = requireLocation,
            dndMode = dndMode,
            dndStartOffsetMinutes = dndStartOffsetMinutes,
            preDndNotificationEnabled = preDndNotificationEnabled,
            preDndNotificationLeadMinutes = preDndNotificationLeadMinutes,
            requireTitleKeyword = requireTitleKeyword,
            titleKeyword = titleKeyword,
            titleKeywordMatchMode = titleKeywordMatchMode,
            titleKeywordCaseSensitive = titleKeywordCaseSensitive,
            titleKeywordMatchAll = titleKeywordMatchAll,
            titleKeywordExclude = titleKeywordExclude,
            postMeetingNotificationEnabled = postMeetingNotificationEnabled,
            postMeetingNotificationOffsetMinutes = postMeetingNotificationOffsetMinutes,
            dndSetByApp = dndSetByApp,
            activeWindowEndMs = activeWindowEndMs,
            userSuppressedUntilMs = userSuppressedUntilMs,
            userSuppressedFromMs = userSuppressedFromMs,
            manualDndUntilMs = manualDndUntilMs,
            manualEventStartMs = manualEventStartMs,
            manualEventEndMs = manualEventEndMs,
            lastKnownDndFilter = lastKnownDndFilter,
            skippedEventId = skippedEventId,
            hasCalendarPermission = hasCalendarPermission,
            hasPolicyAccess = hasPolicyAccess,
            hasExactAlarms = hasExactAlarms,
            systemDndIsOn = systemDndIsOn,
            currentSystemFilter = currentSystemFilter,
            skippedEventBeginMs = skippedEventBeginMs,
            skippedEventEndMs = skippedEventEndMs,
            notifiedNewEventBeforeSkip = notifiedNewEventBeforeSkip
        )
    }

    // ============================================
    // Mock Classes
    // ============================================

    private class MockCalendarRepository : ICalendarRepository {
        var activeInstances: List<EventInstance> = emptyList()
        var nextInstance: EventInstance? = null
        var instancesInRange: List<EventInstance> = emptyList()

        override suspend fun getActiveInstances(
            now: Long,
            selectedCalendarIds: Set<String>,
            busyOnly: Boolean,
            ignoreAllDay: Boolean,
            skipRecurring: Boolean,
            selectedDaysEnabled: Boolean,
            selectedDaysMask: Int,
            minEventMinutes: Int,
            requireLocation: Boolean,
            requireTitleKeyword: Boolean,
            titleKeyword: String,
            titleKeywordMatchMode: KeywordMatchMode,
            titleKeywordCaseSensitive: Boolean,
            titleKeywordMatchAll: Boolean,
            titleKeywordExclude: Boolean
        ): List<EventInstance> {
            return activeInstances
        }

        override suspend fun getNextInstance(
            now: Long,
            selectedCalendarIds: Set<String>,
            busyOnly: Boolean,
            ignoreAllDay: Boolean,
            skipRecurring: Boolean,
            selectedDaysEnabled: Boolean,
            selectedDaysMask: Int,
            minEventMinutes: Int,
            requireLocation: Boolean,
            requireTitleKeyword: Boolean,
            titleKeyword: String,
            titleKeywordMatchMode: KeywordMatchMode,
            titleKeywordCaseSensitive: Boolean,
            titleKeywordMatchAll: Boolean,
            titleKeywordExclude: Boolean
        ): EventInstance? {
            return nextInstance
        }

        override suspend fun getInstancesInRange(
            beginMs: Long,
            endMs: Long,
            selectedCalendarIds: Set<String>,
            busyOnly: Boolean,
            ignoreAllDay: Boolean,
            skipRecurring: Boolean,
            selectedDaysEnabled: Boolean,
            selectedDaysMask: Int,
            minEventMinutes: Int,
            requireLocation: Boolean,
            requireTitleKeyword: Boolean,
            titleKeyword: String,
            titleKeywordMatchMode: KeywordMatchMode,
            titleKeywordCaseSensitive: Boolean,
            titleKeywordMatchAll: Boolean,
            titleKeywordExclude: Boolean
        ): List<EventInstance> {
            if (instancesInRange.isNotEmpty()) {
                return instancesInRange
            }
            val result = activeInstances.toMutableList()
            nextInstance?.let { instance ->
                if (instance.begin in beginMs..endMs && result.none { it.id == instance.id }) {
                    result.add(instance)
                }
            }
            return result
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
