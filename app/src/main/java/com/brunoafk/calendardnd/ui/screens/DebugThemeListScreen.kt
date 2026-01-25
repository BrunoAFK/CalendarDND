package com.brunoafk.calendardnd.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.data.prefs.SettingsStore
import com.brunoafk.calendardnd.ui.components.OneUiTopAppBar
import com.brunoafk.calendardnd.ui.components.SettingsDivider
import com.brunoafk.calendardnd.ui.components.SettingsNavigationRow
import com.brunoafk.calendardnd.ui.components.SettingsSection
import com.brunoafk.calendardnd.ui.navigation.AppRoutes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugThemeListScreen(
    onNavigateBack: () -> Unit,
    onSelectTheme: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
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

    Scaffold(
        topBar = {
            OneUiTopAppBar(
                onNavigateBack = onNavigateBack,
                title = stringResource(R.string.debug_tools_themes_list_title)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.debug_tools_themes_list_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                SettingsSection(title = stringResource(R.string.theme_list_current_title)) {
                    Column {
                        SettingsNavigationRow(
                            title = currentThemeTitle,
                            subtitle = stringResource(R.string.theme_list_current_subtitle),
                            onClick = { onSelectTheme(AppRoutes.STATUS) }
                        )
                    }
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.theme_list_official_title)) {
                    Column {
                        SettingsNavigationRow(
                            title = stringResource(R.string.theme_list_main_title),
                            subtitle = if (!legacyThemeEnabled) {
                                stringResource(R.string.theme_list_current_subtitle)
                            } else {
                                stringResource(R.string.theme_list_main_subtitle)
                            },
                            onClick = {
                                scope.launch { settingsStore.setLegacyThemeEnabled(false) }
                                onSelectTheme(AppRoutes.STATUS)
                            }
                        )
                        SettingsDivider()
                        SettingsNavigationRow(
                            title = stringResource(R.string.theme_list_legacy_title),
                            subtitle = if (legacyThemeEnabled) {
                                stringResource(R.string.theme_list_current_subtitle)
                            } else {
                                stringResource(R.string.theme_list_legacy_subtitle)
                            },
                            onClick = {
                                scope.launch { settingsStore.setLegacyThemeEnabled(true) }
                                onSelectTheme(AppRoutes.STATUS)
                            }
                        )
                    }
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.theme_list_other_title)) {
                    Column {
                        otherThemes.forEachIndexed { index, item ->
                            SettingsNavigationRow(
                                title = item.title,
                                subtitle = item.subtitle,
                                onClick = { onSelectTheme(item.route) }
                            )
                            if (index < otherThemes.lastIndex) {
                                SettingsDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}
