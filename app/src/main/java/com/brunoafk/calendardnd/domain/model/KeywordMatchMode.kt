package com.brunoafk.calendardnd.domain.model

enum class KeywordMatchMode {
    KEYWORDS,
    WHOLE_WORD,
    STARTS_WITH,
    ENDS_WITH,
    EXACT,
    REGEX;

    companion object {
        fun fromString(value: String?): KeywordMatchMode {
            return when (value?.uppercase()) {
                "WHOLE_WORD" -> WHOLE_WORD
                "STARTS_WITH" -> STARTS_WITH
                "ENDS_WITH" -> ENDS_WITH
                "EXACT" -> EXACT
                "REGEX" -> REGEX
                else -> KEYWORDS
            }
        }
    }
}
