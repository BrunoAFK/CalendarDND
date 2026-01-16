package com.brunoafk.calendardnd.util

import android.content.Context
import android.text.format.DateFormat
import com.brunoafk.calendardnd.domain.engine.TimeFormatter
import java.util.Date

class AndroidTimeFormatter(private val context: Context) : TimeFormatter {
    override fun formatTime(millis: Long): String {
        val formatter = DateFormat.getTimeFormat(context)
        return formatter.format(Date(millis))
    }
}
