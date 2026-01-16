package com.brunoafk.calendardnd.data.calendar

import com.brunoafk.calendardnd.domain.model.EventInstance

/**
 * Interface for calendar data access
 * Allows for testing AutomationEngine with mock implementations
 */
interface ICalendarRepository {
    suspend fun getActiveInstances(
        now: Long,
        selectedCalendarIds: Set<String>,
        busyOnly: Boolean,
        ignoreAllDay: Boolean,
        minEventMinutes: Int
    ): List<EventInstance>

    suspend fun getNextInstance(
        now: Long,
        selectedCalendarIds: Set<String>,
        busyOnly: Boolean,
        ignoreAllDay: Boolean,
        minEventMinutes: Int
    ): EventInstance?

    suspend fun getCalendars(): List<CalendarInfo>
}
