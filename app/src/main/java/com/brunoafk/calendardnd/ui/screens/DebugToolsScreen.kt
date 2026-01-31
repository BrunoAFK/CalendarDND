package com.brunoafk.calendardnd.ui.screens

import android.app.NotificationManager
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.brunoafk.calendardnd.BuildConfig
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.data.dnd.DndController
import com.brunoafk.calendardnd.data.prefs.DebugLogLevel
import com.brunoafk.calendardnd.data.prefs.DebugLogStore
import com.brunoafk.calendardnd.data.prefs.RuntimeStateStore
import com.brunoafk.calendardnd.data.prefs.SettingsStore
import com.brunoafk.calendardnd.domain.model.DndMode
import com.brunoafk.calendardnd.domain.model.Trigger
import com.brunoafk.calendardnd.system.alarms.EngineRunner
import com.brunoafk.calendardnd.system.notifications.DndNotificationHelper
import com.brunoafk.calendardnd.system.update.ManualUpdateManager
import com.brunoafk.calendardnd.ui.components.OneUiTopAppBar
import com.brunoafk.calendardnd.ui.components.SettingsDivider
import com.brunoafk.calendardnd.ui.components.SettingsInfoRow
import com.brunoafk.calendardnd.ui.components.SettingsNavigationRow
import com.brunoafk.calendardnd.ui.components.SettingsSection
import com.brunoafk.calendardnd.ui.components.SettingsSwitchRow
import com.brunoafk.calendardnd.ui.components.PersistentWarningBanner
import com.brunoafk.calendardnd.util.TimeUtils
import com.google.android.play.core.review.ReviewManagerFactory
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
    onOpenTelemetryLevel: () -> Unit = {},
    onOpenThemeList: () -> Unit = {},
    onOpenThemeDebugging: () -> Unit = {},
    signatureStatus: ManualUpdateManager.SignatureStatus = ManualUpdateManager.SignatureStatus(
        isAllowed = true,
        isPinned = true
    )
) {
    val context = LocalContext.current
    val viewContext = LocalView.current.context
    val activityOwner = LocalActivityResultRegistryOwner.current
    val scope = rememberCoroutineScope()
    val settingsStore = remember { SettingsStore(context) }
    val runtimeStateStore = remember { RuntimeStateStore(context) }
    val dndController = remember { DndController(context) }
    val debugOverlayEnabled by settingsStore.debugOverlayEnabled.collectAsState(
        initial = false
    )
    val statusDebugPanelEnabled by settingsStore.statusDebugPanelEnabled.collectAsState(
        initial = false
    )
    val telemetryEnabled by settingsStore.telemetryEnabled.collectAsState(
        initial = com.brunoafk.calendardnd.util.AppConfig.telemetryDefaultEnabled
    )
    val telemetryLevel by settingsStore.telemetryLevel.collectAsState(
        initial = com.brunoafk.calendardnd.util.AppConfig.telemetryDefaultLevel
    )
    val oneTimeActionConfirmation by settingsStore.oneTimeActionConfirmation.collectAsState(
        initial = true
    )
    val totalSilenceDialogEnabled by settingsStore.totalSilenceDialogEnabled.collectAsState(
        initial = true
    )
    val lastPlannedBoundaryMs by runtimeStateStore.lastPlannedBoundaryMs.collectAsState(
        initial = 0L
    )
    val testNotificationTitle = stringResource(R.string.debug_tools_test_notification_title)
    val testNotificationSubtitle = stringResource(R.string.debug_tools_test_notification_subtitle)
    val dndPreviewSubtitle = stringResource(
        R.string.debug_tools_dnd_preview_subtitle,
        DND_PREVIEW_SECONDS
    )
    val telemetryLevelLabel = when (telemetryLevel) {
        com.brunoafk.calendardnd.domain.model.TelemetryLevel.BASIC ->
            stringResource(R.string.debug_tools_telemetry_level_basic_title)
        com.brunoafk.calendardnd.domain.model.TelemetryLevel.DETAILED ->
            stringResource(R.string.debug_tools_telemetry_level_detailed_title)
    }
    val nextCheckLabel = if (lastPlannedBoundaryMs > 0L) {
        TimeUtils.formatDateTime(context, lastPlannedBoundaryMs)
    } else {
        stringResource(R.string.debug_tools_next_check_unset)
    }
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
    val reviewPromptState by produceState<SettingsStore.ReviewPromptState?>(initialValue = null, settingsStore) {
        value = withContext(Dispatchers.IO) {
            settingsStore.getReviewPromptState()
        }
    }
    val reviewStateSubtitle = reviewPromptState?.let { state ->
        val dayMs = 24L * 60 * 60 * 1000
        val daysSinceFirstOpen = if (state.firstOpenMs > 0L) {
            ((System.currentTimeMillis() - state.firstOpenMs) / dayMs).coerceAtLeast(0)
        } else {
            -1
        }
        val daysLabel = if (daysSinceFirstOpen >= 0) "${daysSinceFirstOpen}d" else "—"
        val lastPromptLabel = if (state.lastPromptMs > 0L) {
            TimeUtils.formatDateTime(context, state.lastPromptMs)
        } else {
            "—"
        }
        val currentMajorVersion = parseMajorVersion(BuildConfig.VERSION_NAME)
        val cooldownMs = 60L * 24 * 60 * 60 * 1000
        val cooldownElapsed = state.lastPromptMs <= 0L ||
            System.currentTimeMillis() - state.lastPromptMs >= cooldownMs
        val eligible = !BuildConfig.DEBUG &&
            BuildConfig.FLAVOR == "play" &&
            state.appOpenCount >= 5 &&
            daysSinceFirstOpen >= 3 &&
            (cooldownElapsed || state.lastPromptMajorVersion != currentMajorVersion)
        stringResource(
            R.string.debug_tools_reviews_state_subtitle,
            lastPromptLabel,
            state.appOpenCount,
            daysLabel,
            if (eligible) stringResource(R.string.yes) else stringResource(R.string.no)
        )
    } ?: "—"

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
                        checked = telemetryEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                settingsStore.setTelemetryEnabled(enabled)
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsNavigationRow(
                        title = stringResource(R.string.debug_tools_telemetry_level_title),
                        subtitle = stringResource(
                            R.string.debug_tools_telemetry_level_current,
                            telemetryLevelLabel
                        ),
                        onClick = onOpenTelemetryLevel
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
                        SettingsInfoRow(
                            title = stringResource(R.string.debug_tools_next_check_title),
                            subtitle = nextCheckLabel
                        )
                        SettingsDivider()
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

            if (BuildConfig.FLAVOR == "play") {
                item {
                    SettingsSection(title = stringResource(R.string.debug_tools_reviews_title)) {
                        SettingsNavigationRow(
                            title = stringResource(R.string.debug_tools_reviews_force_title),
                            subtitle = stringResource(R.string.debug_tools_reviews_force_subtitle),
                            onClick = {
                                scope.launch {
                                    val activity = (activityOwner as? Activity)
                                        ?: viewContext.findActivity()
                                    if (activity == null) {
                                        Toast.makeText(
                                            context,
                                            R.string.debug_tools_reviews_no_activity,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@launch
                                    }
                                    val settingsStore = SettingsStore(context)
                                    withContext(Dispatchers.IO) {
                                        settingsStore.resetReviewPromptEligibility(System.currentTimeMillis())
                                    }
                                    val manager = ReviewManagerFactory.create(activity)
                                    manager.requestReviewFlow().addOnCompleteListener { requestTask ->
                                        if (!requestTask.isSuccessful) {
                                            return@addOnCompleteListener
                                        }
                                        val reviewInfo = requestTask.result
                                        manager.launchReviewFlow(activity, reviewInfo)
                                            .addOnCompleteListener {
                                                scope.launch(Dispatchers.IO) {
                                                    if (it.isSuccessful) {
                                                        settingsStore.markReviewPromptLaunched(
                                                            System.currentTimeMillis(),
                                                            parseMajorVersion(BuildConfig.VERSION_NAME)
                                                        )
                                                    }
                                                }
                                            }
                                    }
                                }
                            }
                        )
                        SettingsDivider()
                        SettingsInfoRow(
                            title = stringResource(R.string.debug_tools_reviews_state_title),
                            subtitle = reviewStateSubtitle
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
                            title = stringResource(R.string.one_time_action_confirmation_title),
                            subtitle = stringResource(R.string.one_time_action_confirmation_subtitle),
                            checked = oneTimeActionConfirmation,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    settingsStore.setOneTimeActionConfirmation(enabled)
                                }
                            }
                        )
                        SettingsDivider()
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
                        SettingsDivider()
                        SettingsSwitchRow(
                            title = stringResource(R.string.debug_tools_status_debug_panel_title),
                            subtitle = stringResource(R.string.debug_tools_status_debug_panel_subtitle),
                            checked = statusDebugPanelEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    settingsStore.setStatusDebugPanelEnabled(enabled)
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

            item {
                SettingsSection(title = stringResource(R.string.debug_tools_themes_title)) {
                    Column {
                        SettingsNavigationRow(
                            title = stringResource(R.string.debug_tools_themes_list_title),
                            subtitle = stringResource(R.string.debug_tools_themes_list_subtitle),
                            onClick = onOpenThemeList
                        )
                        SettingsDivider()
                        SettingsNavigationRow(
                            title = stringResource(R.string.theme_debug_mode_title),
                            subtitle = stringResource(R.string.theme_debug_mode_description),
                            onClick = onOpenThemeDebugging
                        )
                    }
                }
            }
        }
    }
}

private fun parseMajorVersion(versionName: String): Int {
    val trimmed = versionName.trim()
    val primary = trimmed.substringBefore(".")
    return primary.toIntOrNull() ?: trimmed.toIntOrNull() ?: 0
}

private const val DND_PREVIEW_SECONDS = 10
private const val DND_PREVIEW_MS = DND_PREVIEW_SECONDS * 1000L

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) {
            return current
        }
        current = current.baseContext
    }
    return null
}
