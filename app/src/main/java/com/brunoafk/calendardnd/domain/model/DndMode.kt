package com.brunoafk.calendardnd.domain.model

import com.brunoafk.calendardnd.R

/**
 * DND mode setting
 */
enum class DndMode(val filterValue: Int) {
    VIBRATE(-1),
    PRIORITY(android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY),
    TOTAL_SILENCE(android.app.NotificationManager.INTERRUPTION_FILTER_NONE);

    val usesDndFilter: Boolean
        get() = this != VIBRATE

    val titleResId: Int
        get() = when (this) {
            VIBRATE -> R.string.vibrate_mode_title
            PRIORITY -> R.string.priority_mode_title
            TOTAL_SILENCE -> R.string.total_silence_title
        }

    val descriptionResId: Int
        get() = when (this) {
            VIBRATE -> R.string.vibrate_mode_description
            PRIORITY -> R.string.priority_mode_description
            TOTAL_SILENCE -> R.string.total_silence_description
        }

    val detailsResId: Int
        get() = when (this) {
            VIBRATE -> R.string.vibrate_mode_details
            PRIORITY -> R.string.priority_mode_details
            TOTAL_SILENCE -> R.string.total_silence_details
        }

    companion object {
        fun fromString(value: String): DndMode {
            return when (value) {
                "VIBRATE" -> VIBRATE
                "TOTAL_SILENCE" -> TOTAL_SILENCE
                else -> PRIORITY
            }
        }
    }
}
