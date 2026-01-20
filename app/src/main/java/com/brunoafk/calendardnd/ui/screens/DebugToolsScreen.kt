package com.brunoafk.calendardnd.ui.screens

import android.app.NotificationManager
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.brunoafk.calendardnd.BuildConfig
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.data.dnd.DndController
import com.brunoafk.calendardnd.data.prefs.DebugLogLevel
import com.brunoafk.calendardnd.data.prefs.DebugLogStore
import com.brunoafk.calendardnd.data.prefs.SettingsStore
import com.brunoafk.calendardnd.domain.model.DndMode
import com.brunoafk.calendardnd.domain.model.Trigger
import com.brunoafk.calendardnd.system.alarms.EngineRunner
import com.brunoafk.calendardnd.system.notifications.DndNotificationHelper
import com.brunoafk.calendardnd.system.update.ManualUpdateManager
import com.brunoafk.calendardnd.ui.components.OneUiTopAppBar
import com.brunoafk.calendardnd.ui.components.SettingsDivider
import com.brunoafk.calendardnd.ui.components.SettingsNavigationRow
import com.brunoafk.calendardnd.ui.components.SettingsSection
import com.brunoafk.calendardnd.ui.components.SettingsSwitchRow
import com.brunoafk.calendardnd.ui.components.PersistentWarningBanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugToolsScreen(
    onNavigateBack: () -> Unit,
    onOpenLanguage: (String) -> Unit,
    onOpenDebugLogs: () -> Unit = {},
    onOpenLogSettings: () -> Unit = {},
    signatureStatus: ManualUpdateManager.SignatureStatus = ManualUpdateManager.SignatureStatus(
        isAllowed = true,
        isPinned = true
    )
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsStore = remember { SettingsStore(context) }
    val dndController = remember { DndController(context) }
    val debugOverlayEnabled by settingsStore.debugOverlayEnabled.collectAsState(
        initial = false
    )
    val testerTelemetryEnabled by settingsStore.testerTelemetryEnabled.collectAsState(
        initial = com.brunoafk.calendardnd.util.AppConfig.testerTelemetryDefault
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
        "en" to stringResource(R.string.language_english).ifBlank { "English" },
        "zh" to stringResource(R.string.language_chinese).ifBlank { "中文" },
        "hr" to stringResource(R.string.language_croatian).ifBlank { "Hrvatski" },
        "de" to stringResource(R.string.language_german).ifBlank { "Deutsch" },
        "it" to stringResource(R.string.language_italian).ifBlank { "Italiano" },
        "tr" to stringResource(R.string.language_turkish).ifBlank { "Türkçe" },
        "ko" to stringResource(R.string.language_korean).ifBlank { "한국어" }
    )
    val listState = rememberLazyListState()

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
            state = listState,
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
                SettingsSection(title = stringResource(R.string.debug_tools_telemetry_section_title)) {
                    SettingsSwitchRow(
                        title = stringResource(R.string.debug_tools_tester_telemetry_title),
                        subtitle = stringResource(R.string.debug_tools_tester_telemetry_subtitle),
                        checked = testerTelemetryEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                settingsStore.setTesterTelemetryEnabled(enabled)
                            }
                        }
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
                                val startMs = System.currentTimeMillis() + 5 * 60 * 1000
                                val endMs = startMs + 30 * 60 * 1000
                                DndNotificationHelper.showPreDndNotification(
                                    context = context,
                                    meetingTitle = testNotificationTitle,
                                    dndWindowEndMs = endMs,
                                    dndWindowStartMs = startMs
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

            if (BuildConfig.MANUAL_UPDATE_ENABLED) {
                item {
                    SettingsSection(title = stringResource(R.string.debug_tools_updates_title)) {
                        Column {
                            if (BuildConfig.DEBUG && !signatureStatus.isAllowed) {
                                val messageRes = if (signatureStatus.isPinned) {
                                    R.string.signature_warning_message
                                } else {
                                    R.string.signature_warning_unconfigured
                                }
                                val message = stringResource(messageRes) + " " +
                                    stringResource(R.string.signature_warning_debug_note)
                                PersistentWarningBanner(
                                    title = stringResource(R.string.signature_warning_title),
                                    message = message
                                )
                                SettingsDivider()
                            }
                            SettingsNavigationRow(
                                title = stringResource(R.string.debug_tools_force_update_title),
                                subtitle = stringResource(R.string.debug_tools_force_update_subtitle),
                                onClick = {
                                    scope.launch {
                                        val debugLogStore = DebugLogStore(context)
                                        debugLogStore.appendLog(
                                            DebugLogLevel.INFO,
                                            "UPDATE_DEBUG: Force update requested."
                                        )
                                        val metadata = ManualUpdateManager.fetchUpdateMetadata(context)
                                        val latest = metadata?.releases?.firstOrNull()
                                        if (latest == null) {
                                            debugLogStore.appendLog(
                                                DebugLogLevel.WARNING,
                                                "UPDATE_DEBUG: No update metadata available."
                                            )
                                            Toast.makeText(
                                                context,
                                                R.string.update_screen_no_data,
                                                Toast.LENGTH_LONG
                                            ).show()
                                            return@launch
                                        }
                                        val apkFile = if (latest.sha256.isNullOrBlank()) {
                                            debugLogStore.appendLog(
                                                DebugLogLevel.WARNING,
                                                "UPDATE_DEBUG: Missing SHA-256 for ${latest.versionName}"
                                            )
                                            Toast.makeText(
                                                context,
                                                R.string.update_download_missing_hash,
                                                Toast.LENGTH_LONG
                                            ).show()
                                            null
                                        } else {
                                            ManualUpdateManager.downloadAndVerifyApk(context, latest)
                                        }
                                        if (apkFile == null) {
                                            debugLogStore.appendLog(
                                                DebugLogLevel.ERROR,
                                                "UPDATE_DEBUG: Download or verification failed for ${latest.versionName}"
                                            )
                                            if (!latest.sha256.isNullOrBlank()) {
                                                Toast.makeText(
                                                    context,
                                                    R.string.update_download_failed,
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                            return@launch
                                        }
                                        try {
                                            if (!ManualUpdateManager.canRequestPackageInstalls(context)) {
                                                debugLogStore.appendLog(
                                                    DebugLogLevel.WARNING,
                                                    "UPDATE_DEBUG: Install permission missing."
                                                )
                                                ManualUpdateManager.createInstallPermissionIntent(context)?.let {
                                                    context.startActivity(it)
                                                } ?: Toast.makeText(
                                                    context,
                                                    R.string.update_install_permission_required,
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                return@launch
                                            }
                                            val installIntent =
                                                ManualUpdateManager.createInstallIntent(context, apkFile)
                                            if (installIntent.resolveActivity(context.packageManager) == null) {
                                                debugLogStore.appendLog(
                                                    DebugLogLevel.ERROR,
                                                    "UPDATE_DEBUG: Installer activity not found"
                                                )
                                                Toast.makeText(
                                                    context,
                                                    R.string.update_open_failed,
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                return@launch
                                            }
                                            debugLogStore.appendLog(
                                                DebugLogLevel.INFO,
                                                "UPDATE_DEBUG: Launch installer ${apkFile.absolutePath}"
                                            )
                                            context.startActivity(
                                                installIntent
                                            )
                                        } catch (_: android.content.ActivityNotFoundException) {
                                            debugLogStore.appendLog(
                                                DebugLogLevel.ERROR,
                                                "UPDATE_DEBUG: Installer activity not found"
                                            )
                                            Toast.makeText(
                                                context,
                                                R.string.update_open_failed,
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            )
                        }
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
