package com.brunoafk.calendardnd.domain.model

/**
 * DND mode setting
 */
enum class DndMode(val filterValue: Int) {
    PRIORITY(android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY),
    TOTAL_SILENCE(android.app.NotificationManager.INTERRUPTION_FILTER_NONE);

    companion object {
        fun fromString(value: String): DndMode {
            return when (value) {
                "TOTAL_SILENCE" -> TOTAL_SILENCE
                else -> PRIORITY
            }
        }
    }
}