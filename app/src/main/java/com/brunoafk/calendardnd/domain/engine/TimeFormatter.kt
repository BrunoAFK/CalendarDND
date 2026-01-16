package com.brunoafk.calendardnd.domain.engine

import java.text.DateFormat
import java.util.Date
import java.util.Locale

interface TimeFormatter {
    fun formatTime(millis: Long): String
}

class DefaultTimeFormatter : TimeFormatter {
    private val timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault())

    override fun formatTime(millis: Long): String {
        return timeFormat.format(Date(millis))
    }
}
