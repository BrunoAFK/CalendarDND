package com.brunoafk.calendardnd.domain.model

enum class TelemetryLevel {
    BASIC,
    DETAILED;

    companion object {
        fun fromString(value: String?): TelemetryLevel {
            if (value.isNullOrBlank()) {
                return BASIC
            }
            return values().firstOrNull { it.name.equals(value, ignoreCase = true) } ?: BASIC
        }
    }
}
