package com.brunoafk.calendardnd.domain.util

import com.brunoafk.calendardnd.domain.model.EventInstance
import com.brunoafk.calendardnd.domain.model.SkippedEventState
import org.junit.Assert.*
import org.junit.Test

class SkipUtilsTest {

    private fun createEvent(
        id: Long = 1L,
        eventId: Long = 123L,
        begin: Long = 1000L,
        end: Long = 2000L
    ) = EventInstance(
        id = id,
        eventId = eventId,
        calendarId = 1L,
        title = "Test Event",
        location = "",
        begin = begin,
        end = end,
        allDay = false,
        availability = 0,
        isRecurring = false
    )

    @Test
    fun `isSkippedEvent returns false for null event`() {
        val state = SkippedEventState(123L, 1000L, 2000L)
        assertFalse(SkipUtils.isSkippedEvent(null, state))
    }

    @Test
    fun `isSkippedEvent returns false when no skip is active`() {
        val event = createEvent()
        val state = SkippedEventState.NONE
        assertFalse(SkipUtils.isSkippedEvent(event, state))
    }

    @Test
    fun `isSkippedEvent returns false when eventId is zero`() {
        val event = createEvent()
        val state = SkippedEventState(0L, 1000L, 2000L)
        assertFalse(SkipUtils.isSkippedEvent(event, state))
    }

    @Test
    fun `isSkippedEvent returns false when beginMs is zero`() {
        val event = createEvent()
        val state = SkippedEventState(123L, 0L, 2000L)
        assertFalse(SkipUtils.isSkippedEvent(event, state))
    }

    @Test
    fun `isSkippedEvent returns true for matching event`() {
        val event = createEvent(eventId = 123L, begin = 1000L, end = 2000L)
        val state = SkippedEventState(123L, 1000L, 2000L)
        assertTrue(SkipUtils.isSkippedEvent(event, state, nowMs = 1500L))
    }

    @Test
    fun `isSkippedEvent returns false when eventId does not match`() {
        val event = createEvent(eventId = 456L, begin = 1000L, end = 2000L)
        val state = SkippedEventState(123L, 1000L, 2000L)
        assertFalse(SkipUtils.isSkippedEvent(event, state))
    }

    @Test
    fun `isSkippedEvent returns false when begin does not match`() {
        val event = createEvent(eventId = 123L, begin = 1100L, end = 2000L)
        val state = SkippedEventState(123L, 1000L, 2000L)
        assertFalse(SkipUtils.isSkippedEvent(event, state))
    }

    @Test
    fun `isSkippedEvent returns false when event has ended`() {
        val event = createEvent(eventId = 123L, begin = 1000L, end = 2000L)
        val state = SkippedEventState(123L, 1000L, 2000L)
        // Now is past the event end time
        assertFalse(SkipUtils.isSkippedEvent(event, state, nowMs = 2500L))
    }

    @Test
    fun `isSkippedEvent returns true when endMs is zero`() {
        // endMs of 0 means no expiration check
        val event = createEvent(eventId = 123L, begin = 1000L, end = 2000L)
        val state = SkippedEventState(123L, 1000L, 0L)
        assertTrue(SkipUtils.isSkippedEvent(event, state, nowMs = 5000L))
    }

    @Test
    fun `filterOutSkipped removes skipped event from list`() {
        val skippedEvent = createEvent(id = 1L, eventId = 123L, begin = 1000L, end = 2000L)
        val otherEvent = createEvent(id = 2L, eventId = 456L, begin = 1500L, end = 3000L)
        val events = listOf(skippedEvent, otherEvent)
        val state = SkippedEventState(123L, 1000L, 2000L)

        val filtered = SkipUtils.filterOutSkipped(events, state, nowMs = 1500L)

        assertEquals(1, filtered.size)
        assertEquals(456L, filtered[0].eventId)
    }

    @Test
    fun `filterOutSkipped returns all events when no skip is active`() {
        val event1 = createEvent(id = 1L, eventId = 123L)
        val event2 = createEvent(id = 2L, eventId = 456L)
        val events = listOf(event1, event2)
        val state = SkippedEventState.NONE

        val filtered = SkipUtils.filterOutSkipped(events, state)

        assertEquals(2, filtered.size)
    }

    @Test
    fun `shouldClearSkip returns true when event has ended`() {
        val state = SkippedEventState(123L, 1000L, 2000L)
        assertTrue(SkipUtils.shouldClearSkip(state, nowMs = 2500L))
    }

    @Test
    fun `shouldClearSkip returns false when event is ongoing`() {
        val state = SkippedEventState(123L, 1000L, 2000L)
        assertFalse(SkipUtils.shouldClearSkip(state, nowMs = 1500L))
    }

    @Test
    fun `shouldClearSkip returns false when no skip is active`() {
        val state = SkippedEventState.NONE
        assertFalse(SkipUtils.shouldClearSkip(state, nowMs = 5000L))
    }

    @Test
    fun `overloaded isSkippedEvent with individual params works correctly`() {
        val event = createEvent(eventId = 123L, begin = 1000L, end = 2000L)
        assertTrue(SkipUtils.isSkippedEvent(event, 123L, 1000L, 2000L, nowMs = 1500L))
        assertFalse(SkipUtils.isSkippedEvent(event, 456L, 1000L, 2000L, nowMs = 1500L))
    }
}
