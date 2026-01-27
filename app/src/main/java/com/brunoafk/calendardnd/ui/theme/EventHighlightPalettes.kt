package com.brunoafk.calendardnd.ui.theme

import androidx.compose.ui.graphics.Color
import com.brunoafk.calendardnd.domain.model.EventHighlightPreset

data class EventHighlightColors(
    val light: Color,
    val dark: Color,
    val lightHex: String,
    val darkHex: String
)

fun eventHighlightColors(preset: EventHighlightPreset): EventHighlightColors {
    return when (preset) {
        EventHighlightPreset.PRESET_1 -> EventHighlightColors(
            light = Color(0xFFB5E5FD),
            dark = Color(0xFF5CA0C5),
            lightHex = "#B5E5FD",
            darkHex = "#5CA0C5"
        )
        EventHighlightPreset.PRESET_2 -> EventHighlightColors(
            light = Color(0xFFFDD1B5),
            dark = Color(0xFFC5865C),
            lightHex = "#FDD1B5",
            darkHex = "#C5865C"
        )
        EventHighlightPreset.PRESET_3 -> EventHighlightColors(
            light = Color(0xFFB5CCFD),
            dark = Color(0xFF5C7EC5),
            lightHex = "#B5CCFD",
            darkHex = "#5C7EC5"
        )
        EventHighlightPreset.PRESET_4 -> EventHighlightColors(
            light = Color(0xFFD4B5FD),
            dark = Color(0xFF815CC5),
            lightHex = "#D4B5FD",
            darkHex = "#815CC5"
        )
        EventHighlightPreset.PRESET_5 -> EventHighlightColors(
            light = Color(0xFFEBFDB5),
            dark = Color(0xFFA4C55C),
            lightHex = "#EBFDB5",
            darkHex = "#A4C55C"
        )
        EventHighlightPreset.PRESET_6 -> EventHighlightColors(
            light = Color(0xFFFFC107),
            dark = Color(0xFFB38B00),
            lightHex = "#FFC107",
            darkHex = "#B38B00"
        )
        EventHighlightPreset.PRESET_7 -> EventHighlightColors(
            light = Color(0xFFFDEEB5),
            dark = Color(0xFFC5B15C),
            lightHex = "#FDEEB5",
            darkHex = "#C5B15C"
        )
        EventHighlightPreset.PRESET_8 -> EventHighlightColors(
            light = Color(0xFFB2DFDB),
            dark = Color(0xFF4A6B68),
            lightHex = "#B2DFDB",
            darkHex = "#4A6B68"
        )
        EventHighlightPreset.PRESET_9 -> EventHighlightColors(
            light = Color(0xFFB3C5E8),
            dark = Color(0xFF4A5A7A),
            lightHex = "#B3C5E8",
            darkHex = "#4A5A7A"
        )
        EventHighlightPreset.PRESET_10 -> EventHighlightColors(
            light = Color(0xFFB2DFDB),
            dark = Color(0xFF4A6B68),
            lightHex = "#B2DFDB",
            darkHex = "#4A6B68"
        )
        EventHighlightPreset.PRESET_11 -> EventHighlightColors(
            light = Color(0xFFD1C4E9),
            dark = Color(0xFF5E527A),
            lightHex = "#D1C4E9",
            darkHex = "#5E527A"
        )
        EventHighlightPreset.PRESET_12 -> EventHighlightColors(
            light = Color(0xFFF5F5F7),
            dark = Color(0xFF404248),
            lightHex = "#F5F5F7",
            darkHex = "#404248"
        )
        EventHighlightPreset.PRESET_13 -> EventHighlightColors(
            light = Color(0xFFCDDEFF),
            dark = Color(0xFF65789E),
            lightHex = "#CDDEFF",
            darkHex = "#65789E"
        )
    }
}
