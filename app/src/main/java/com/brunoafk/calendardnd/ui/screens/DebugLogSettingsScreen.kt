package com.brunoafk.calendardnd.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.data.prefs.DebugLogLevel
import com.brunoafk.calendardnd.data.prefs.SettingsStore
import com.brunoafk.calendardnd.ui.components.OneUiTopAppBar
import com.brunoafk.calendardnd.ui.components.SettingsSection
import com.brunoafk.calendardnd.ui.components.SettingsSwitchRow
import com.brunoafk.calendardnd.util.AnalyticsTracker
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugLogSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsStore = remember { SettingsStore(context) }
    val logLevelCapture by settingsStore.logLevelCapture.collectAsState(initial = DebugLogLevel.WARNING)
    val includeDetails by settingsStore.debugLogIncludeDetails.collectAsState(initial = false)
    var levelMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        AnalyticsTracker.logScreenView(context, "debug_log_settings")
    }

    Scaffold(
        topBar = {
            OneUiTopAppBar(
                onNavigateBack = onNavigateBack,
                title = stringResource(R.string.debug_log_settings_title)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSection(title = stringResource(R.string.debug_log_settings_title)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.debug_log_settings_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.padding(top = 12.dp))
                    ExposedDropdownMenuBox(
                        expanded = levelMenuExpanded,
                        onExpandedChange = { levelMenuExpanded = !levelMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = logLevelCapture.displayName,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            label = { Text(stringResource(R.string.debug_log_settings_level_label)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = levelMenuExpanded
                                )
                            }
                        )
                        ExposedDropdownMenu(
                            expanded = levelMenuExpanded,
                            onDismissRequest = { levelMenuExpanded = false }
                        ) {
                            listOf(
                                DebugLogLevel.INFO,
                                DebugLogLevel.WARNING,
                                DebugLogLevel.ERROR
                            ).forEach { level ->
                                DropdownMenuItem(
                                    onClick = {
                                        scope.launch {
                                            settingsStore.setLogLevelCapture(level)
                                        }
                                        levelMenuExpanded = false
                                    },
                                    text = { Text(level.displayName) }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.padding(top = 12.dp))
                    SettingsSwitchRow(
                        title = stringResource(R.string.debug_log_settings_include_details_title),
                        subtitle = stringResource(R.string.debug_log_settings_include_details_subtitle),
                        checked = includeDetails,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                settingsStore.setDebugLogIncludeDetails(enabled)
                            }
                        }
                    )
                }
            }
        }
    }
}
