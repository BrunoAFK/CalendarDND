package com.brunoafk.calendardnd.domain.model

/**
 * Represents a merged window of overlapping/touching meetings
 */
data class MeetingWindow(
    val begin: Long,
    val end: Long,
    val events: List<EventInstance>
) {
    fun isActive(now: Long): Boolean = begin <= now && now < end

    fun overlapsOrTouches(other: EventInstance): Boolean {
        return other.begin <= this.end && other.end >= this.begin
    }
}