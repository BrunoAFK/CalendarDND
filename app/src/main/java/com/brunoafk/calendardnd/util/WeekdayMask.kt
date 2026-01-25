package com.brunoafk.calendardnd.util

import java.time.DayOfWeek

object WeekdayMask {
    const val ALL_DAYS_MASK: Int = (1 shl 7) - 1

    fun dayToMask(day: DayOfWeek): Int {
        return 1 shl (day.value - 1)
    }

    fun isDayAllowed(mask: Int, day: DayOfWeek): Boolean {
        return (mask and dayToMask(day)) != 0
    }
}
