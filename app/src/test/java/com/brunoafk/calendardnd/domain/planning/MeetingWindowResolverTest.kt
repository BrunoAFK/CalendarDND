package com.brunoafk.calendardnd.domain.planning

import com.brunoafk.calendardnd.domain.model.EventInstance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MeetingWindowResolverTest {

    @Test
    fun `findActiveWindow merges touching and overlapping events`() {
        val now = 1500L
        val instances = listOf(
            EventInstance(
                id = 1L,
                eventId = 1L,
                calendarId = 1L,
                title = "A",
                location = "",
                begin = 1000L,
                end = 2000L,
                allDay = false,
                availability = 0
            ),
            EventInstance(
                id = 2L,
                eventId = 2L,
                calendarId = 1L,
                title = "B",
                location = "",
                begin = 2000L,
                end = 3000L,
                allDay = false,
                availability = 0
            ),
            EventInstance(
                id = 3L,
                eventId = 3L,
                calendarId = 1L,
                title = "C",
                location = "",
                begin = 2500L,
                end = 3500L,
                allDay = false,
                availability = 0
            )
        )

        val window = MeetingWindowResolver.findActiveWindow(instances, now)

        assertNotNull(window)
        assertEquals(1000L, window?.begin)
        assertEquals(3500L, window?.end)
        assertEquals(3, window?.events?.size)
    }

    @Test
    fun `mergeIntoWindows splits gaps and merges touches`() {
        val instances = listOf(
            EventInstance(
                id = 1L,
                eventId = 1L,
                calendarId = 1L,
                title = "A",
                location = "",
                begin = 1000L,
                end = 1100L,
                allDay = false,
                availability = 0
            ),
            EventInstance(
                id = 2L,
                eventId = 2L,
                calendarId = 1L,
                title = "B",
                location = "",
                begin = 1200L,
                end = 1300L,
                allDay = false,
                availability = 0
            ),
            EventInstance(
                id = 3L,
                eventId = 3L,
                calendarId = 1L,
                title = "C",
                location = "",
                begin = 1300L,
                end = 1400L,
                allDay = false,
                availability = 0
            ),
            EventInstance(
                id = 4L,
                eventId = 4L,
                calendarId = 1L,
                title = "D",
                location = "",
                begin = 1500L,
                end = 1600L,
                allDay = false,
                availability = 0
            )
        )

        val windows = MeetingWindowResolver.mergeIntoWindows(instances)

        assertEquals(3, windows.size)
        assertEquals(1000L, windows[0].begin)
        assertEquals(1100L, windows[0].end)
        assertEquals(1200L, windows[1].begin)
        assertEquals(1400L, windows[1].end)
        assertEquals(1500L, windows[2].begin)
        assertEquals(1600L, windows[2].end)
    }
}
