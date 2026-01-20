package com.brunoafk.calendardnd.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.brunoafk.calendardnd.ui.components.OneUiTopAppBar
import com.brunoafk.calendardnd.ui.theme.surfaceColorAtElevation
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
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

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
                state = listState,
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(options) { option ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = surfaceColorAtElevation(1.dp)
                        ),
                        onClick = {
                            scope.launch {
                                settingsStore.setThemeMode(option.mode)
                            }
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                RadioButton(
                                    selected = themeMode == option.mode,
                                    onClick = {
                                        scope.launch {
                                            settingsStore.setThemeMode(option.mode)
                                        }
                                    }
                                )
                            }
                            Text(
                                text = option.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
