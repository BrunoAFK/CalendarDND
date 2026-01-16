package com.brunoafk.calendardnd.data.prefs

enum class DebugLogLevel(val displayName: String) {
    ALL("All"),
    INFO("Info"),
    WARNING("Warning"),
    ERROR("Error");

    companion object {
        fun fromString(value: String?): DebugLogLevel {
            return values().firstOrNull { it.name == value } ?: ALL
        }
    }
}
