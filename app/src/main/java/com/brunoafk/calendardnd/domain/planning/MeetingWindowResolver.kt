package com.brunoafk.calendardnd.domain.planning

import com.brunoafk.calendardnd.domain.model.EventInstance
import com.brunoafk.calendardnd.domain.model.MeetingWindow

/**
 * Resolves overlapping and touching events into merged meeting windows
 */
object MeetingWindowResolver {

    /**
     * Find active meeting window at the given time
     * Returns null if no active meetings
     */
    fun findActiveWindow(instances: List<EventInstance>, now: Long): MeetingWindow? {
        // Filter to only active instances
        val activeInstances = instances.filter { it.begin <= now && now < it.end }

        if (activeInstances.isEmpty()) {
            return null
        }

        // Start with the first active instance
        var mergedBegin = activeInstances.minOf { it.begin }
        var mergedEnd = activeInstances.maxOf { it.end }

        // Keep merging until no more overlaps/touches are found
        var changed = true
        while (changed) {
            changed = false

            for (instance in instances) {
                // Check if this instance overlaps or touches the current window
                if (instance.begin <= mergedEnd && instance.end >= mergedBegin) {
                    val newBegin = minOf(mergedBegin, instance.begin)
                    val newEnd = maxOf(mergedEnd, instance.end)

                    if (newBegin != mergedBegin || newEnd != mergedEnd) {
                        mergedBegin = newBegin
                        mergedEnd = newEnd
                        changed = true
                    }
                }
            }
        }

        // Collect all instances that are part of this window
        val windowInstances = instances.filter { instance ->
            instance.begin < mergedEnd && instance.end > mergedBegin
        }

        return MeetingWindow(
            begin = mergedBegin,
            end = mergedEnd,
            events = windowInstances
        )
    }

    /**
     * Merge overlapping and touching instances into consolidated windows
     * Used for more general analysis
     */
    fun mergeIntoWindows(instances: List<EventInstance>): List<MeetingWindow> {
        if (instances.isEmpty()) return emptyList()

        // Sort by begin time
        val sorted = instances.sortedBy { it.begin }
        val windows = mutableListOf<MeetingWindow>()

        var currentBegin = sorted[0].begin
        var currentEnd = sorted[0].end
        var currentEvents = mutableListOf(sorted[0])

        for (i in 1 until sorted.size) {
            val instance = sorted[i]

            // Check if this instance touches or overlaps the current window
            // Touch condition: instance.begin <= currentEnd (includes back-to-back)
            if (instance.begin <= currentEnd) {
                // Merge into current window
                currentEnd = maxOf(currentEnd, instance.end)
                currentEvents.add(instance)
            } else {
                // Create a new window from accumulated events
                windows.add(
                    MeetingWindow(
                        begin = currentBegin,
                        end = currentEnd,
                        events = currentEvents.toList()
                    )
                )

                // Start a new window
                currentBegin = instance.begin
                currentEnd = instance.end
                currentEvents = mutableListOf(instance)
            }
        }

        // Add the last window
        windows.add(
            MeetingWindow(
                begin = currentBegin,
                end = currentEnd,
                events = currentEvents.toList()
            )
        )

        return windows
    }
}