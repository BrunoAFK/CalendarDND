package com.brunoafk.calendardnd.data.prefs

enum class DebugLogFilterLevel(val displayName: String) {
    ALL("All"),
    INFO("Info"),
    WARNING("Warning"),
    ERROR("Errors");

    companion object {
        fun fromString(value: String?): DebugLogFilterLevel {
            return values().firstOrNull { it.name == value } ?: ALL
        }
    }
}
