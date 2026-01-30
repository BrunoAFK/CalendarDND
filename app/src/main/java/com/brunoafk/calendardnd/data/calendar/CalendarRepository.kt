package com.brunoafk.calendardnd.data.calendar

import android.content.Context
import android.provider.CalendarContract
import com.brunoafk.calendardnd.domain.model.EventInstance
import com.brunoafk.calendardnd.domain.model.KeywordMatchMode
import com.brunoafk.calendardnd.util.EngineConstants.ACTIVE_INSTANCES_WINDOW_MS
import com.brunoafk.calendardnd.util.EngineConstants.NEXT_INSTANCE_LOOKAHEAD_MS
import com.brunoafk.calendardnd.util.WeekdayMask
import java.time.Instant
import java.time.ZoneId

class CalendarRepository(private val context: Context) : ICalendarRepository {

    /**
     * Get instances that are currently active (begin <= now < end)
     * Queries a 6-hour window around now for safety
     */
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
        val windowStart = now - ACTIVE_INSTANCES_WINDOW_MS
        val windowEnd = now + ACTIVE_INSTANCES_WINDOW_MS

        val allInstances = CalendarQueries.queryInstances(
            context,
            windowStart,
            windowEnd
        )

        return allInstances.filter { instance ->
            // Must be active right now
            instance.begin <= now && now < instance.end &&
                    // Apply filters
                    isRelevantInstance(
                        instance,
                        selectedCalendarIds,
                        busyOnly,
                        ignoreAllDay,
                        skipRecurring,
                        selectedDaysEnabled,
                        selectedDaysMask,
                        minEventMinutes,
                        requireLocation,
                        requireTitleKeyword,
                        titleKeyword,
                        titleKeywordMatchMode,
                        titleKeywordCaseSensitive,
                        titleKeywordMatchAll,
                        titleKeywordExclude
                    )
        }
    }

    /**
     * Get the next upcoming instance after now
     * Queries up to 7 days ahead
     */
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
        val windowEnd = now + NEXT_INSTANCE_LOOKAHEAD_MS

        val allInstances = CalendarQueries.queryInstances(
            context,
            now,
            windowEnd
        )

        return allInstances
            .filter { instance ->
                // Must start in the future
                instance.begin > now &&
                        // Apply filters
                    isRelevantInstance(
                        instance,
                        selectedCalendarIds,
                        busyOnly,
                        ignoreAllDay,
                        skipRecurring,
                        selectedDaysEnabled,
                        selectedDaysMask,
                        minEventMinutes,
                        requireLocation,
                        requireTitleKeyword,
                            titleKeyword,
                            titleKeywordMatchMode,
                            titleKeywordCaseSensitive,
                            titleKeywordMatchAll,
                            titleKeywordExclude
                        )
            }
            .sortedWith(compareBy<EventInstance> { it.begin }.thenByDescending { it.end - it.begin })
            .firstOrNull()
    }

    /**
     * Get all instances within a specific time range
     * Used for merging windows
     */
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
        val allInstances = CalendarQueries.queryInstances(
            context,
            beginMs,
            endMs
        )

        return allInstances.filter { instance ->
            isRelevantInstance(
                instance,
                selectedCalendarIds,
                busyOnly,
                ignoreAllDay,
                skipRecurring,
                selectedDaysEnabled,
                selectedDaysMask,
                minEventMinutes,
                requireLocation,
                requireTitleKeyword,
                titleKeyword,
                titleKeywordMatchMode,
                titleKeywordCaseSensitive,
                titleKeywordMatchAll,
                titleKeywordExclude
            )
        }
    }

    /**
     * Get list of all available calendars
     */
    override suspend fun getCalendars(): List<CalendarInfo> {
        return CalendarQueries.queryCalendars(context)
    }

    /**
     * Check if an instance is relevant based on user settings
     */
    private fun isRelevantInstance(
        instance: EventInstance,
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
    ): Boolean {
        // Calendar filter (empty selection means "all calendars")
        if (selectedCalendarIds.isNotEmpty() &&
            !selectedCalendarIds.contains(instance.calendarId.toString())) {
            return false
        }

        // All-day filter
        if (ignoreAllDay && instance.allDay) {
            return false
        }

        // Busy-only filter
        if (busyOnly && instance.availability != CalendarContract.Events.AVAILABILITY_BUSY) {
            return false
        }

        // Recurring filter
        if (skipRecurring && instance.isRecurring) {
            return false
        }

        if (selectedDaysEnabled) {
            val startDay = Instant.ofEpochMilli(instance.begin)
                .atZone(ZoneId.systemDefault())
                .dayOfWeek
            if (!WeekdayMask.isDayAllowed(selectedDaysMask, startDay)) {
                return false
            }
        }

        // Minimum duration filter
        if (instance.durationMinutes < minEventMinutes) {
            return false
        }

        if (requireLocation && instance.location.isBlank()) {
            return false
        }

        if (!matchesTitleKeyword(
                instance.title,
                requireTitleKeyword,
                titleKeyword,
                titleKeywordMatchMode,
                titleKeywordCaseSensitive,
                titleKeywordMatchAll,
                titleKeywordExclude
            )
        ) {
            return false
        }

        return true
    }

    private fun matchesTitleKeyword(
        title: String,
        requireTitleKeyword: Boolean,
        titleKeyword: String,
        matchMode: KeywordMatchMode,
        caseSensitive: Boolean,
        matchAll: Boolean,
        excludeMatches: Boolean
    ): Boolean {
        if (!requireTitleKeyword) {
            return true
        }
        val pattern = titleKeyword.trim()
        if (pattern.isBlank()) {
            return false
        }

        return com.brunoafk.calendardnd.data.calendar.matchesTitleKeyword(
            title,
            requireTitleKeyword,
            pattern,
            matchMode,
            caseSensitive,
            matchAll,
            excludeMatches
        )
    }
}

internal fun matchesTitleKeyword(
    title: String,
    requireTitleKeyword: Boolean,
    titleKeyword: String,
    matchMode: KeywordMatchMode,
    caseSensitive: Boolean,
    matchAll: Boolean,
    excludeMatches: Boolean
): Boolean {
    if (!requireTitleKeyword) {
        return true
    }
    val pattern = titleKeyword.trim()
    if (pattern.isBlank()) {
        return false
    }

    val normalizedTitle = if (caseSensitive) title else title.lowercase()
    val effectiveMatchAll = matchAll && (
        matchMode == KeywordMatchMode.KEYWORDS || matchMode == KeywordMatchMode.WHOLE_WORD
    )
    val keywords = pattern
        .split(",", "\n")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    val matches = when (matchMode) {
        KeywordMatchMode.KEYWORDS -> {
            if (keywords.isEmpty()) {
                false
            } else {
                val keywordMatches = keywords.map { keyword ->
                    val normalizedKeyword = if (caseSensitive) keyword else keyword.lowercase()
                    normalizedTitle.contains(normalizedKeyword)
                }
                if (effectiveMatchAll) keywordMatches.all { it } else keywordMatches.any { it }
            }
        }
        KeywordMatchMode.WHOLE_WORD -> {
            if (keywords.isEmpty()) {
                false
            } else {
                val keywordMatches = keywords.map { keyword ->
                    val regex = Regex(
                        "\\b${Regex.escape(keyword)}\\b",
                        if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
                    )
                    regex.containsMatchIn(title)
                }
                if (effectiveMatchAll) keywordMatches.all { it } else keywordMatches.any { it }
            }
        }
        KeywordMatchMode.STARTS_WITH -> {
            if (keywords.isEmpty()) {
                false
            } else {
                val keywordMatches = keywords.map { keyword ->
                    val normalizedKeyword = if (caseSensitive) keyword else keyword.lowercase()
                    normalizedTitle.startsWith(normalizedKeyword)
                }
                keywordMatches.any { it }
            }
        }
        KeywordMatchMode.ENDS_WITH -> {
            if (keywords.isEmpty()) {
                false
            } else {
                val keywordMatches = keywords.map { keyword ->
                    val normalizedKeyword = if (caseSensitive) keyword else keyword.lowercase()
                    normalizedTitle.endsWith(normalizedKeyword)
                }
                keywordMatches.any { it }
            }
        }
        KeywordMatchMode.EXACT -> {
            if (keywords.isEmpty()) {
                false
            } else {
                val keywordMatches = keywords.map { keyword ->
                    val normalizedKeyword = if (caseSensitive) keyword else keyword.lowercase()
                    normalizedTitle == normalizedKeyword
                }
                keywordMatches.any { it }
            }
        }
        KeywordMatchMode.REGEX -> {
            val regex = runCatching { Regex(pattern) }.getOrNull() ?: return false
            regex.containsMatchIn(title)
        }
    }
    return if (excludeMatches) !matches else matches
}
