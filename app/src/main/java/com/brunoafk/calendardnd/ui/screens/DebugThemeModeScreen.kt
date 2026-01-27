package com.brunoafk.calendardnd.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.data.prefs.SettingsStore
import com.brunoafk.calendardnd.domain.model.ThemeDebugMode
import com.brunoafk.calendardnd.ui.components.OneUiTopAppBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugThemeModeScreen(
    onNavigateBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsStore = remember { SettingsStore(context) }
    val scope = rememberCoroutineScope()
    val currentMode by settingsStore.themeDebugMode.collectAsState(initial = ThemeDebugMode.OFF)

    val options = listOf(
        ThemeDebugMode.OFF to Pair(
            stringResource(R.string.theme_debug_mode_off),
            stringResource(R.string.theme_debug_mode_off_subtitle)
        ),
        ThemeDebugMode.GRADIENT_OVERVIEW to Pair(
            stringResource(R.string.theme_debug_mode_gradient),
            stringResource(R.string.theme_debug_mode_gradient_subtitle)
        ),
        ThemeDebugMode.THEME_SELECTOR to Pair(
            stringResource(R.string.theme_debug_mode_selector),
            stringResource(R.string.theme_debug_mode_selector_subtitle)
        ),
        ThemeDebugMode.EVENT_CARD_COLORS to Pair(
            stringResource(R.string.theme_debug_mode_event_colors),
            stringResource(R.string.theme_debug_mode_event_colors_subtitle)
        )
    )

    Scaffold(
        topBar = {
            OneUiTopAppBar(
                onNavigateBack = onNavigateBack,
                title = stringResource(R.string.theme_debug_mode_title)
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
                    text = stringResource(R.string.theme_debug_mode_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(options.size) { index ->
                val (mode, labels) = options[index]
                val (title, subtitle) = labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch { settingsStore.setThemeDebugMode(mode) }
                        }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    RadioButton(
                        selected = currentMode == mode,
                        onClick = {
                            scope.launch { settingsStore.setThemeDebugMode(mode) }
                        }
                    )
                }
            }
        }
    }
}
