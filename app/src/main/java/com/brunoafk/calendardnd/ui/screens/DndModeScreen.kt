package com.brunoafk.calendardnd.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.brunoafk.calendardnd.data.dnd.DndController
import com.brunoafk.calendardnd.data.prefs.SettingsStore
import com.brunoafk.calendardnd.domain.model.DndMode
import com.brunoafk.calendardnd.ui.components.OneUiTopAppBar
import com.brunoafk.calendardnd.util.AnalyticsTracker
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DndModeScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsStore = remember { SettingsStore(context) }
    val dndController = remember { DndController(context) }
    val dndMode by settingsStore.dndMode.collectAsState(initial = DndMode.PRIORITY)
    val totalSilenceConfirmed by settingsStore.totalSilenceConfirmed.collectAsState(initial = false)
    val totalSilenceDialogEnabled by settingsStore.totalSilenceDialogEnabled.collectAsState(initial = true)
    val totalSilenceConfirmedEffective = !totalSilenceDialogEnabled ||
        totalSilenceConfirmed ||
        dndMode == DndMode.TOTAL_SILENCE
    var showTotalSilenceDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        AnalyticsTracker.logScreenView(context, "dnd_mode")
    }

    Scaffold(
        topBar = {
            OneUiTopAppBar(
                onNavigateBack = onNavigateBack,
                title = stringResource(R.string.dnd_mode)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 16.dp)),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.dnd_mode_screen_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = dndMode == DndMode.PRIORITY,
                        onClick = {
                            scope.launch {
                                settingsStore.setDndMode(DndMode.PRIORITY)
                                AnalyticsTracker.logSettingsChanged(
                                    context,
                                    "dnd_mode",
                                    DndMode.PRIORITY.name
                                )
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = stringResource(R.string.priority_mode_title))
                    }
                    SegmentedButton(
                        selected = dndMode == DndMode.TOTAL_SILENCE,
                        onClick = {
                            if (totalSilenceConfirmedEffective) {
                                scope.launch {
                                    settingsStore.setDndMode(DndMode.TOTAL_SILENCE)
                                    AnalyticsTracker.logSettingsChanged(
                                        context,
                                        "dnd_mode",
                                        DndMode.TOTAL_SILENCE.name
                                    )
                                }
                            } else {
                                showTotalSilenceDialog = true
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(text = stringResource(R.string.total_silence_title))
                    }
                }

                if (dndMode == DndMode.PRIORITY) {
                    Text(
                        text = stringResource(R.string.priority_mode_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.priority_mode_details),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = stringResource(R.string.total_silence_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.total_silence_details),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = stringResource(R.string.dnd_mode_manage_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(onClick = { dndController.openZenModeSettings() }) {
                    Text(stringResource(R.string.dnd_total_silence_manage_button))
                }
            }
        }
    }

    if (showTotalSilenceDialog) {
        AlertDialog(
            onDismissRequest = { showTotalSilenceDialog = false },
            title = {
                Text(
                    stringResource(R.string.dnd_total_silence_confirm_title)
                )
            },
            text = {
                Text(
                    stringResource(R.string.dnd_total_silence_confirm_body)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showTotalSilenceDialog = false
                        scope.launch {
                            settingsStore.setTotalSilenceConfirmed(true)
                            settingsStore.setDndMode(DndMode.TOTAL_SILENCE)
                            AnalyticsTracker.logSettingsChanged(
                                context,
                                "dnd_mode",
                                DndMode.TOTAL_SILENCE.name
                            )
                        }
                    }
                ) {
                    Text(
                        stringResource(R.string.dnd_total_silence_confirm_button)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showTotalSilenceDialog = false }) {
                    Text(
                        stringResource(R.string.dnd_total_silence_cancel_button)
                    )
                }
            }
        )
    }
}
