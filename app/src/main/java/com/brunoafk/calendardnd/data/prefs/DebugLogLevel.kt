package com.brunoafk.calendardnd.data.prefs

enum class DebugLogLevel(val displayName: String) {
    INFO("Info"),
    WARNING("Warning"),
    ERROR("Errors");

    companion object {
        fun fromString(value: String?): DebugLogLevel {
            return values().firstOrNull { it.name == value } ?: WARNING
        }

        fun allows(level: DebugLogLevel, minimum: DebugLogLevel): Boolean {
            return when (minimum) {
                INFO -> true
                WARNING -> level == WARNING || level == ERROR
                ERROR -> level == ERROR
            }
        }
    }
}
