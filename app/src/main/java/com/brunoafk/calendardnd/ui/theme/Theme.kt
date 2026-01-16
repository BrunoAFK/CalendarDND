package com.brunoafk.calendardnd.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ln

private val LightColors = lightColorScheme()
private val DarkColors = darkColorScheme(
    primary = Color(0xFF90CAF9),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF004881),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFBBC7DB),
    onSecondary = Color(0xFF253140),
    secondaryContainer = Color(0xFF3B4858),
    onSecondaryContainer = Color(0xFFD7E3F7),
    tertiary = Color(0xFFD5BDE2),
    onTertiary = Color(0xFF392946),
    tertiaryContainer = Color(0xFF51405E),
    onTertiaryContainer = Color(0xFFF2DAFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C6CF),
    outline = Color(0xFF8D9199),
    outlineVariant = Color(0xFF43474E),
    inverseSurface = Color(0xFFE2E2E6),
    inverseOnSurface = Color(0xFF2F3033),
    inversePrimary = Color(0xFF0061A4)
)

// Previous corner sizes (for rollback/reference):
// - 8.dp: SettingsSection.kt
// - 12.dp: SettingsScreen.kt (highlight background)
// - 24.dp: StatusBanner.kt
// - shapes.medium/large: WarningBanner.kt, InfoBanner.kt, DndModeBanner.kt
private val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(8.dp)
)

@Composable
fun surfaceColorAtElevation(elevation: Dp): Color {
    return if (isSystemInDarkTheme()) {
        // Dark mode: use a lighter surface color for better contrast
        // Base alpha for elevation with higher multiplier for dark mode
        val alpha = ((8f * ln(elevation.value + 1)) / 100f).coerceIn(0f, 1f)
        // Use a brighter overlay with a minimum alpha for visible contrast
        val minAlpha = 0.04f
        val effectiveAlpha = (alpha + minAlpha).coerceIn(0f, 0.2f)
        Color.White.copy(alpha = effectiveAlpha)
            .compositeOver(MaterialTheme.colorScheme.surface)
    } else {
        // Light mode: keep original behavior
        val alpha = ((4.5f * ln(elevation.value + 1)) / 100f).coerceIn(0f, 1f)
        val overlay = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        overlay.copy(alpha = alpha)
            .compositeOver(MaterialTheme.colorScheme.surface)
    }
}

@Composable
fun CalendarDndTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        shapes = AppShapes,
        content = content
    )
}
