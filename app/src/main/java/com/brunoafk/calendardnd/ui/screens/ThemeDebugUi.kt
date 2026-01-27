package com.brunoafk.calendardnd.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.data.prefs.SettingsStore
import com.brunoafk.calendardnd.domain.model.EventHighlightPreset
import com.brunoafk.calendardnd.domain.model.ThemeMode
import com.brunoafk.calendardnd.ui.navigation.AppRoutes
import com.brunoafk.calendardnd.ui.theme.eventHighlightColors
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder

@Composable
fun ThemeSelectorBottomBar(
    onSelectTheme: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore(context) }
    val scope = rememberCoroutineScope()
    val legacyThemeEnabled by settingsStore.legacyThemeEnabled.collectAsState(initial = false)
    val themeMode by settingsStore.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val items = themeDebugItems()
    val otherThemes = items.filter { it.route != AppRoutes.STATUS }
    val currentThemeTitle = if (legacyThemeEnabled) {
        stringResource(R.string.theme_list_legacy_title)
    } else {
        stringResource(R.string.theme_list_main_title)
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                CompositionLocalProvider(LocalContext provides context) {
                    Text(text = stringResource(R.string.theme_debug_selector_title))
                }
            },
            text = {
                CompositionLocalProvider(LocalContext provides context) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            ThemeDialogSectionTitle(text = stringResource(R.string.theme_list_official_title))
                        }
                        item {
                            ThemeDialogRow(
                                title = stringResource(R.string.theme_list_main_title),
                                subtitle = if (!legacyThemeEnabled) {
                                    stringResource(R.string.theme_list_current_subtitle)
                                } else {
                                    stringResource(R.string.theme_list_main_subtitle)
                                },
                                onClick = {
                                    scope.launch { settingsStore.setLegacyThemeEnabled(false) }
                                    showDialog = false
                                    onSelectTheme(AppRoutes.STATUS)
                                }
                            )
                        }
                        item {
                            ThemeDialogRow(
                                title = stringResource(R.string.theme_list_legacy_title),
                                subtitle = if (legacyThemeEnabled) {
                                    stringResource(R.string.theme_list_current_subtitle)
                                } else {
                                    stringResource(R.string.theme_list_legacy_subtitle)
                                },
                                onClick = {
                                    scope.launch { settingsStore.setLegacyThemeEnabled(true) }
                                    showDialog = false
                                    onSelectTheme(AppRoutes.STATUS)
                                }
                            )
                        }

                        item {
                            ThemeDialogSectionTitle(text = stringResource(R.string.theme_list_other_title))
                        }
                        items(otherThemes.size) { index ->
                            val item = otherThemes[index]
                            ThemeDialogRow(
                                title = item.title,
                                subtitle = item.subtitle,
                                onClick = {
                                    showDialog = false
                                    onSelectTheme(item.route)
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                CompositionLocalProvider(LocalContext provides context) {
                    TextButton(onClick = { showDialog = false }) {
                        Text(text = stringResource(R.string.close))
                    }
                }
            }
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.theme_debug_selector_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(R.string.theme_debug_selector_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = {
                        val nextMode = if (themeMode == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
                        scope.launch { settingsStore.setThemeMode(nextMode) }
                    }
                ) {
                    Icon(
                        imageVector = if (themeMode == ThemeMode.DARK) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { showDialog = true }) {
                    Text(text = stringResource(R.string.theme_debug_selector_action))
                }
            }
        }
    }
}

@Composable
fun EventColorSelectorBottomBar(
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore(context) }
    val scope = rememberCoroutineScope()
    val themeMode by settingsStore.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val currentPreset by settingsStore.debugEventHighlightPreset.collectAsState(initial = EventHighlightPreset.PRESET_1)
    val favoritePresets by settingsStore.debugEventHighlightFavorites.collectAsState(initial = emptyList())
    val presets = remember {
        listOf(
            EventHighlightPreset.PRESET_13,
            EventHighlightPreset.PRESET_8,
            EventHighlightPreset.PRESET_9,
            EventHighlightPreset.PRESET_10,
            EventHighlightPreset.PRESET_11,
            EventHighlightPreset.PRESET_12,
            EventHighlightPreset.PRESET_1,
            EventHighlightPreset.PRESET_2,
            EventHighlightPreset.PRESET_3,
            EventHighlightPreset.PRESET_4,
            EventHighlightPreset.PRESET_5,
            EventHighlightPreset.PRESET_6,
            EventHighlightPreset.PRESET_7
        )
    }
    val favoriteItems = remember(currentPreset, favoritePresets) {
        favoritePresets.filter { it != currentPreset }
    }
    val otherItems = remember(currentPreset, favoriteItems, presets) {
        presets.filter { it != currentPreset && !favoriteItems.contains(it) }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                CompositionLocalProvider(LocalContext provides context) {
                    Text(text = stringResource(R.string.event_color_selector_dialog_title))
                }
            },
            text = {
                CompositionLocalProvider(LocalContext provides context) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            ThemeDialogSectionTitle(text = stringResource(R.string.event_color_section_current))
                        }
                        item {
                            val colors = eventHighlightColors(currentPreset)
                            EventColorDialogRow(
                                title = stringResource(R.string.event_color_preset_title, presets.indexOf(currentPreset) + 1),
                                subtitle = stringResource(
                                    R.string.event_color_preset_subtitle,
                                    colors.lightHex,
                                    colors.darkHex
                                ),
                                lightColor = colors.light,
                                darkColor = colors.dark,
                                isFavorite = favoritePresets.contains(currentPreset),
                                selected = true,
                                onClick = { showDialog = false },
                                onToggleFavorite = {
                                    val updated = if (favoritePresets.contains(currentPreset)) {
                                        favoritePresets.filter { it != currentPreset }
                                    } else {
                                        favoritePresets + currentPreset
                                    }
                                    scope.launch { settingsStore.setDebugEventHighlightFavorites(updated.distinct()) }
                                }
                            )
                        }

                        if (favoriteItems.isNotEmpty()) {
                            item {
                                ThemeDialogSectionTitle(text = stringResource(R.string.event_color_section_favorites))
                            }
                            items(favoriteItems.size) { index ->
                                val preset = favoriteItems[index]
                                val colors = eventHighlightColors(preset)
                                EventColorDialogRow(
                                    title = stringResource(
                                        R.string.event_color_preset_title,
                                        presets.indexOf(preset) + 1
                                    ),
                                    subtitle = stringResource(
                                        R.string.event_color_preset_subtitle,
                                        colors.lightHex,
                                        colors.darkHex
                                    ),
                                    lightColor = colors.light,
                                    darkColor = colors.dark,
                                    isFavorite = true,
                                    selected = false,
                                    onClick = {
                                        scope.launch {
                                            settingsStore.setDebugEventHighlightPreset(preset)
                                        }
                                        showDialog = false
                                    },
                                    onToggleFavorite = {
                                        val updated = favoritePresets.filter { it != preset }
                                        scope.launch { settingsStore.setDebugEventHighlightFavorites(updated) }
                                    }
                                )
                            }
                        }

                        item {
                            ThemeDialogSectionTitle(text = stringResource(R.string.event_color_section_all))
                        }
                        items(otherItems.size) { index ->
                            val preset = otherItems[index]
                            val colors = eventHighlightColors(preset)
                            EventColorDialogRow(
                                title = stringResource(
                                    R.string.event_color_preset_title,
                                    presets.indexOf(preset) + 1
                                ),
                                subtitle = stringResource(
                                    R.string.event_color_preset_subtitle,
                                    colors.lightHex,
                                    colors.darkHex
                                ),
                                lightColor = colors.light,
                                darkColor = colors.dark,
                                isFavorite = favoritePresets.contains(preset),
                                selected = preset == currentPreset,
                                onClick = {
                                    scope.launch {
                                        settingsStore.setDebugEventHighlightPreset(preset)
                                    }
                                    showDialog = false
                                },
                                onToggleFavorite = {
                                    val updated = if (favoritePresets.contains(preset)) {
                                        favoritePresets.filter { it != preset }
                                    } else {
                                        favoritePresets + preset
                                    }
                                    scope.launch { settingsStore.setDebugEventHighlightFavorites(updated.distinct()) }
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                CompositionLocalProvider(LocalContext provides context) {
                    TextButton(onClick = { showDialog = false }) {
                        Text(text = stringResource(R.string.close))
                    }
                }
            }
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.event_color_selector_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = {
                        val nextMode = if (themeMode == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
                        scope.launch { settingsStore.setThemeMode(nextMode) }
                    }
                ) {
                    Icon(
                        imageVector = if (themeMode == ThemeMode.DARK) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { showDialog = true }) {
                    Text(text = stringResource(R.string.event_color_selector_action))
                }
            }
        }
    }
}

@Composable
private fun ThemeDialogSectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun ThemeDialogRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EventColorDialogRow(
    title: String,
    subtitle: String,
    lightColor: androidx.compose.ui.graphics.Color,
    darkColor: androidx.compose.ui.graphics.Color,
    isFavorite: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                            contentDescription = null,
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        color = lightColor,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.width(24.dp).heightIn(min = 24.dp)
                    ) {}
                    Surface(
                        color = darkColor,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.width(24.dp).heightIn(min = 24.dp)
                    ) {}
                }
            }
        }
    }
}
