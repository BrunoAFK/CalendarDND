package com.brunoafk.calendardnd.util

import android.content.Context
import android.text.format.DateFormat
import java.util.Date

object TimeUtils {

    fun formatTime(context: Context, millis: Long): String {
        val formatter = DateFormat.getTimeFormat(context)
        return formatter.format(Date(millis))
    }

    fun formatDateTime(context: Context, millis: Long): String {
        val date = Date(millis)
        val dateFormat = DateFormat.getDateFormat(context)
        val timeFormat = DateFormat.getTimeFormat(context)
        return "${dateFormat.format(date)} ${timeFormat.format(date)}"
    }

    fun formatFullDateTime(context: Context, millis: Long): String {
        val date = Date(millis)
        val dateFormat = DateFormat.getLongDateFormat(context)
        val timeFormat = DateFormat.getTimeFormat(context)
        return "${dateFormat.format(date)} ${timeFormat.format(date)}"
    }

    fun formatDuration(durationMs: Long): String {
        val minutes = durationMs / 60_000L
        val hours = minutes / 60
        val days = hours / 24
        val hoursRemainder = hours % 24
        val mins = minutes % 60

        return when {
            days > 0 && hoursRemainder > 0 -> "${days}d ${hoursRemainder}h"
            days > 0 -> "${days}d"
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            else -> "${mins}m"
        }
    }

    fun formatRelativeTime(millis: Long): String {
        val now = System.currentTimeMillis()
        val diff = millis - now

        return when {
            diff < 0 -> "in the past"
            diff < 60_000 -> "in ${diff / 1000}s"
            diff < 3_600_000 -> "in ${diff / 60_000}m"
            diff < 86_400_000 -> "in ${diff / 3_600_000}h"
            else -> "in ${diff / 86_400_000}d"
        }
    }
}
