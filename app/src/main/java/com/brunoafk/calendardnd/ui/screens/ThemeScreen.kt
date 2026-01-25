package com.brunoafk.calendardnd.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.data.prefs.SettingsStore
import com.brunoafk.calendardnd.domain.model.ThemeMode
import com.brunoafk.calendardnd.ui.components.SettingsDivider
import com.brunoafk.calendardnd.ui.components.SettingsSection
import com.brunoafk.calendardnd.ui.components.SettingsSwitchRow
import com.brunoafk.calendardnd.ui.components.OneUiTopAppBar
import kotlinx.coroutines.launch

data class ThemeOption(
    val mode: ThemeMode,
    val label: String,
    val subtitle: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore(context) }
    val themeMode by settingsStore.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val legacyThemeEnabled by settingsStore.legacyThemeEnabled.collectAsState(initial = false)
    val scope = rememberCoroutineScope()

    val options = listOf(
        ThemeOption(
            ThemeMode.SYSTEM,
            stringResource(R.string.theme_option_system),
            stringResource(R.string.theme_option_system_subtitle)
        ),
        ThemeOption(
            ThemeMode.LIGHT,
            stringResource(R.string.theme_option_light),
            stringResource(R.string.theme_option_light_subtitle)
        ),
        ThemeOption(
            ThemeMode.DARK,
            stringResource(R.string.theme_option_dark),
            stringResource(R.string.theme_option_dark_subtitle)
        )
    )

    Scaffold(
        topBar = {
            OneUiTopAppBar(
                onNavigateBack = onNavigateBack,
                title = stringResource(R.string.theme_title)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.theme_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SettingsSection(title = stringResource(R.string.theme_colors_section_title)) {
                        Column {
                            options.forEachIndexed { index, option ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            scope.launch {
                                                settingsStore.setThemeMode(option.mode)
                                            }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = option.label,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = option.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    RadioButton(
                                        selected = themeMode == option.mode,
                                        onClick = {
                                            scope.launch {
                                                settingsStore.setThemeMode(option.mode)
                                            }
                                        }
                                    )
                                }
                                if (index < options.lastIndex) {
                                    SettingsDivider()
                                }
                            }
                        }
                    }
                }

                item {
                    SettingsSection(title = stringResource(R.string.theme_section_title)) {
                        Column {
                            SettingsSwitchRow(
                                title = stringResource(R.string.legacy_theme_title),
                                subtitle = stringResource(R.string.legacy_theme_subtitle),
                                checked = legacyThemeEnabled,
                                onCheckedChange = { enabled ->
                                    scope.launch {
                                        settingsStore.setLegacyThemeEnabled(enabled)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
