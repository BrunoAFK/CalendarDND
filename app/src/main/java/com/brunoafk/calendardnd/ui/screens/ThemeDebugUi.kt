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
import com.brunoafk.calendardnd.ui.navigation.AppRoutes
import kotlinx.coroutines.launch

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
            TextButton(onClick = { showDialog = true }) {
                Text(text = stringResource(R.string.theme_debug_selector_action))
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
