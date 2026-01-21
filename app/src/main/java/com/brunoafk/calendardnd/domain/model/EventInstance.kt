package com.brunoafk.calendardnd.domain.model

/**
 * Represents a single calendar event instance
 */
data class EventInstance(
    val id: Long,
    val eventId: Long,
    val calendarId: Long,
    val title: String,
    val location: String,
    val begin: Long,
    val end: Long,
    val allDay: Boolean,
    val availability: Int
) {
    val durationMinutes: Long
        get() = (end - begin) / 60_000L
}
