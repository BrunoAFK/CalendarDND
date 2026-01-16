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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.app.NotificationManager
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.data.dnd.DndController
import com.brunoafk.calendardnd.data.prefs.SettingsStore
import com.brunoafk.calendardnd.domain.model.DndMode
import com.brunoafk.calendardnd.system.notifications.DndNotificationHelper
import com.brunoafk.calendardnd.ui.components.OneUiTopAppBar
import com.brunoafk.calendardnd.ui.components.SettingsDivider
import com.brunoafk.calendardnd.ui.components.SettingsNavigationRow
import com.brunoafk.calendardnd.ui.components.SettingsSection
import com.brunoafk.calendardnd.ui.components.SettingsSwitchRow
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import android.widget.Toast
import com.brunoafk.calendardnd.domain.model.Trigger
import com.brunoafk.calendardnd.system.alarms.EngineRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugToolsScreen(
    onNavigateBack: () -> Unit,
    onOpenLanguage: (String) -> Unit,
    onOpenDebugLogs: () -> Unit = {},
    onOpenLogSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsStore = remember { SettingsStore(context) }
    val dndController = remember { DndController(context) }
    val debugOverlayEnabled by settingsStore.debugOverlayEnabled.collectAsState(
        initial = false
    )
    val totalSilenceDialogEnabled by settingsStore.totalSilenceDialogEnabled.collectAsState(
        initial = true
    )
    val testNotificationTitle = stringResource(R.string.debug_tools_test_notification_title)
    val testNotificationSubtitle = stringResource(R.string.debug_tools_test_notification_subtitle)
    val dndPreviewSubtitle = stringResource(
        R.string.debug_tools_dnd_preview_subtitle,
        DND_PREVIEW_SECONDS
    )
    val languages = listOf(
        "en" to stringResource(R.string.language_english),
        "zh" to stringResource(R.string.language_chinese),
        "hr" to stringResource(R.string.language_croatian),
        "de" to stringResource(R.string.language_german),
        "it" to stringResource(R.string.language_italian),
        "ko" to stringResource(R.string.language_korean)
    )

    fun previewDnd(mode: DndMode) {
        scope.launch {
            if (!dndController.hasPolicyAccess()) {
                dndController.openPolicyAccessSettings()
                return@launch
            }

            val previousFilter = dndController.getCurrentFilter()
            dndController.enableDnd(mode)
            delay(DND_PREVIEW_MS)

            val restoreFilter = if (previousFilter == NotificationManager.INTERRUPTION_FILTER_UNKNOWN) {
                NotificationManager.INTERRUPTION_FILTER_ALL
            } else {
                previousFilter
            }
            dndController.setFilterValue(restoreFilter)
        }
    }

    Scaffold(
        topBar = {
            OneUiTopAppBar(
                onNavigateBack = onNavigateBack,
                title = stringResource(R.string.debug_tools_title)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.debug_tools_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                SettingsSection(title = stringResource(R.string.debug_tools_logs_section_title)) {
                    SettingsNavigationRow(
                        title = stringResource(R.string.debug_logs),
                        subtitle = stringResource(R.string.debug_tools_logs_subtitle),
                        onClick = onOpenDebugLogs
                    )
                    SettingsDivider()
                    SettingsNavigationRow(
                        title = stringResource(R.string.debug_log_settings_title),
                        subtitle = stringResource(R.string.debug_log_settings_subtitle),
                        onClick = onOpenLogSettings
                    )
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.debug_tools_preview_title)) {
                    Column {
                        SettingsNavigationRow(
                            title = testNotificationTitle,
                            subtitle = testNotificationSubtitle,
                            onClick = {
                                DndNotificationHelper.showPreDndNotification(
                                    context = context,
                                    meetingTitle = testNotificationTitle,
                                    dndWindowEndMs = System.currentTimeMillis() + 30 * 60 * 1000
                                )
                            }
                        )
                        SettingsDivider()
                        SettingsNavigationRow(
                            title = stringResource(R.string.debug_tools_dnd_preview_priority_title),
                            subtitle = dndPreviewSubtitle,
                            onClick = { previewDnd(DndMode.PRIORITY) }
                        )
                        SettingsDivider()
                        SettingsNavigationRow(
                            title = stringResource(R.string.debug_tools_dnd_preview_total_title),
                            subtitle = dndPreviewSubtitle,
                            onClick = { previewDnd(DndMode.TOTAL_SILENCE) }
                        )
                    }
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.debug_tools_engine_section_title)) {
                    Column {
                        SettingsNavigationRow(
                            title = stringResource(R.string.debug_tools_dry_run_title),
                            subtitle = stringResource(R.string.debug_tools_dry_run_subtitle),
                            onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        EngineRunner.runEngine(context, Trigger.MANUAL, dryRun = true)
                                    }
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.debug_tools_dry_run_toast),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.debug_tools_maintenance_title)) {
                    Column {
                        SettingsNavigationRow(
                            title = stringResource(R.string.debug_tools_restore_banners_title),
                            subtitle = stringResource(R.string.debug_tools_restore_banners_subtitle),
                            onClick = {
                                scope.launch {
                                    settingsStore.setDndModeBannerDismissed(false)
                                    settingsStore.setRefreshBannerDismissed(false)
                                }
                            }
                        )
                        SettingsDivider()
                        SettingsNavigationRow(
                            title = stringResource(R.string.debug_tools_reset_total_silence_title),
                            subtitle = stringResource(R.string.debug_tools_reset_total_silence_subtitle),
                            onClick = {
                                scope.launch {
                                    settingsStore.setTotalSilenceConfirmed(false)
                                }
                            }
                        )
                    }
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.debug_tools_app_control_title)) {
                    Column {
                        SettingsSwitchRow(
                            title = stringResource(R.string.debug_tools_total_silence_dialog_title),
                            subtitle = stringResource(R.string.debug_tools_total_silence_dialog_subtitle),
                            checked = totalSilenceDialogEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    settingsStore.setTotalSilenceDialogEnabled(enabled)
                                }
                            }
                        )
                        SettingsDivider()
                        SettingsSwitchRow(
                            title = stringResource(R.string.debug_tools_nav_overlay_title),
                            subtitle = stringResource(R.string.debug_tools_nav_overlay_subtitle),
                            checked = debugOverlayEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    settingsStore.setDebugOverlayEnabled(enabled)
                                }
                            }
                        )
                    }
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.debug_tools_languages_title)) {
                    Column {
                        languages.forEachIndexed { index, (tag, label) ->
                            SettingsNavigationRow(
                                title = label,
                                subtitle = tag,
                                onClick = { onOpenLanguage(tag) }
                            )
                            if (index < languages.lastIndex) {
                                SettingsDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val DND_PREVIEW_SECONDS = 10
private const val DND_PREVIEW_MS = DND_PREVIEW_SECONDS * 1000L
