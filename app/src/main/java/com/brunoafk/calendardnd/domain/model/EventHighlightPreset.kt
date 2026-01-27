package com.brunoafk.calendardnd.domain.model

enum class EventHighlightPreset {
    PRESET_1,
    PRESET_2,
    PRESET_3,
    PRESET_4,
    PRESET_5,
    PRESET_6,
    PRESET_7,
    PRESET_8,
    PRESET_9,
    PRESET_10,
    PRESET_11,
    PRESET_12,
    PRESET_13;

    companion object {
        fun fromString(raw: String?): EventHighlightPreset {
            return runCatching { valueOf(raw ?: "") }.getOrDefault(PRESET_1)
        }
    }
}
