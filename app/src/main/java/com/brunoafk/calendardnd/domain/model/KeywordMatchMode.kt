package com.brunoafk.calendardnd.domain.model

enum class KeywordMatchMode {
    KEYWORDS,
    REGEX;

    companion object {
        fun fromString(value: String?): KeywordMatchMode {
            return when (value?.uppercase()) {
                "REGEX" -> REGEX
                else -> KEYWORDS
            }
        }
    }
}
