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
        skipRecurring: Boolean,
        minEventMinutes: Int,
        requireLocation: Boolean,
        requireTitleKeyword: Boolean,
        titleKeyword: String,
        titleKeywordMatchMode: com.brunoafk.calendardnd.domain.model.KeywordMatchMode,
        titleKeywordCaseSensitive: Boolean,
        titleKeywordMatchAll: Boolean,
        titleKeywordExclude: Boolean
    ): List<EventInstance>

    suspend fun getNextInstance(
        now: Long,
        selectedCalendarIds: Set<String>,
        busyOnly: Boolean,
        ignoreAllDay: Boolean,
        skipRecurring: Boolean,
        minEventMinutes: Int,
        requireLocation: Boolean,
        requireTitleKeyword: Boolean,
        titleKeyword: String,
        titleKeywordMatchMode: com.brunoafk.calendardnd.domain.model.KeywordMatchMode,
        titleKeywordCaseSensitive: Boolean,
        titleKeywordMatchAll: Boolean,
        titleKeywordExclude: Boolean
    ): EventInstance?

    suspend fun getInstancesInRange(
        beginMs: Long,
        endMs: Long,
        selectedCalendarIds: Set<String>,
        busyOnly: Boolean,
        ignoreAllDay: Boolean,
        skipRecurring: Boolean,
        minEventMinutes: Int,
        requireLocation: Boolean,
        requireTitleKeyword: Boolean,
        titleKeyword: String,
        titleKeywordMatchMode: com.brunoafk.calendardnd.domain.model.KeywordMatchMode,
        titleKeywordCaseSensitive: Boolean,
        titleKeywordMatchAll: Boolean,
        titleKeywordExclude: Boolean
    ): List<EventInstance>

    suspend fun getCalendars(): List<CalendarInfo>
}
