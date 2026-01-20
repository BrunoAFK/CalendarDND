package com.brunoafk.calendardnd.domain.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromString(value: String?): ThemeMode {
            return when (value) {
                "LIGHT" -> LIGHT
                "DARK" -> DARK
                else -> SYSTEM
            }
        }
    }
}
