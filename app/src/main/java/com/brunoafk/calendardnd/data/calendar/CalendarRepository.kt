package com.brunoafk.calendardnd.data.calendar

import android.content.Context
import android.provider.CalendarContract
import com.brunoafk.calendardnd.domain.model.EventInstance
import com.brunoafk.calendardnd.util.EngineConstants.ACTIVE_INSTANCES_WINDOW_MS
import com.brunoafk.calendardnd.util.EngineConstants.NEXT_INSTANCE_LOOKAHEAD_MS

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
        minEventMinutes: Int
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
                    isRelevantInstance(instance, selectedCalendarIds, busyOnly, ignoreAllDay, minEventMinutes)
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
        minEventMinutes: Int
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
                        isRelevantInstance(instance, selectedCalendarIds, busyOnly, ignoreAllDay, minEventMinutes)
            }
            .minByOrNull { it.begin }
    }

    /**
     * Get all instances within a specific time range
     * Used for merging windows
     */
    suspend fun getInstancesInRange(
        beginMs: Long,
        endMs: Long,
        selectedCalendarIds: Set<String>,
        busyOnly: Boolean,
        ignoreAllDay: Boolean,
        minEventMinutes: Int
    ): List<EventInstance> {
        val allInstances = CalendarQueries.queryInstances(
            context,
            beginMs,
            endMs
        )

        return allInstances.filter { instance ->
            isRelevantInstance(instance, selectedCalendarIds, busyOnly, ignoreAllDay, minEventMinutes)
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
        minEventMinutes: Int
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

        // Minimum duration filter
        if (instance.durationMinutes < minEventMinutes) {
            return false
        }

        return true
    }
}
