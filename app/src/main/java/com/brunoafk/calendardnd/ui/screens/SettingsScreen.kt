package com.brunoafk.calendardnd.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import kotlinx.coroutines.delay
import com.brunoafk.calendardnd.BuildConfig
import com.brunoafk.calendardnd.data.prefs.SettingsStore
import com.brunoafk.calendardnd.domain.model.DndMode
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.brunoafk.calendardnd.util.AnalyticsTracker
import com.brunoafk.calendardnd.util.AppConfig
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.brunoafk.calendardnd.data.dnd.DndController
import com.brunoafk.calendardnd.domain.model.Trigger
import com.brunoafk.calendardnd.system.alarms.EngineRunner
import com.brunoafk.calendardnd.ui.components.AppError
import com.brunoafk.calendardnd.ui.components.ErrorCard
import com.brunoafk.calendardnd.ui.components.OneUiTopAppBar
import com.brunoafk.calendardnd.ui.components.SettingsDivider
import com.brunoafk.calendardnd.ui.components.SettingsHelpText
import com.brunoafk.calendardnd.ui.components.SettingsNavigationRow
import com.brunoafk.calendardnd.ui.components.SettingsSection
import com.brunoafk.calendardnd.ui.components.SettingsSwitchRow
import com.brunoafk.calendardnd.util.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCalendarPicker: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToDebugTools: () -> Unit,
    onNavigateToUpdates: () -> Unit,
    onNavigateToDndMode: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    highlightAutomation: Boolean,
    showUpdatesMenu: Boolean
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val settingsStore = remember { SettingsStore(context) }
    val dndController = remember { DndController(context) }

    val busyOnly by settingsStore.busyOnly.collectAsState(initial = true)
    val ignoreAllDay by settingsStore.ignoreAllDay.collectAsState(initial = true)
    val minEventMinutes by settingsStore.minEventMinutes.collectAsState(initial = 10)
    val dndMode by settingsStore.dndMode.collectAsState(initial = DndMode.PRIORITY)
    val dndStartOffsetMinutes by settingsStore.dndStartOffsetMinutes.collectAsState(initial = 0)
    val preDndNotificationEnabled by settingsStore.preDndNotificationEnabled.collectAsState(initial = false)
    val selectedCalendarIds by settingsStore.selectedCalendarIds.collectAsState(initial = emptySet())
    val analyticsOptIn by settingsStore.analyticsOptIn.collectAsState(initial = false)
    val crashlyticsOptIn by settingsStore.crashlyticsOptIn.collectAsState(initial = true)
    val automationEnabled by settingsStore.automationEnabled.collectAsState(initial = false)
    val debugToolsUnlocked by settingsStore.debugToolsUnlocked.collectAsState(initial = false)
    var hasCalendarPermission by remember {
        mutableStateOf(PermissionUtils.hasCalendarPermission(context))
    }
    var hasPolicyAccess by remember { mutableStateOf(dndController.hasPolicyAccess()) }
    val dndOffsetLabel = when {
        dndStartOffsetMinutes == 0 -> stringResource(
            com.brunoafk.calendardnd.R.string.dnd_timing_on_time
        )
        dndStartOffsetMinutes < 0 -> stringResource(
            com.brunoafk.calendardnd.R.string.dnd_timing_before_meeting,
            -dndStartOffsetMinutes
        )
        else -> stringResource(
            com.brunoafk.calendardnd.R.string.dnd_timing_after_meeting,
            dndStartOffsetMinutes
        )
    }
    val dndOffsetValue = when {
        dndStartOffsetMinutes == 0 -> stringResource(
            com.brunoafk.calendardnd.R.string.dnd_timing_value_zero
        )
        dndStartOffsetMinutes < 0 -> stringResource(
            com.brunoafk.calendardnd.R.string.dnd_timing_value_before,
            -dndStartOffsetMinutes
        )
        else -> stringResource(
            com.brunoafk.calendardnd.R.string.dnd_timing_value_after,
            dndStartOffsetMinutes
        )
    }
    val dndOffsetHelpText = when {
        dndStartOffsetMinutes == 0 -> stringResource(
            com.brunoafk.calendardnd.R.string.dnd_timing_help_on_time
        )
        dndStartOffsetMinutes < 0 -> stringResource(
            com.brunoafk.calendardnd.R.string.dnd_timing_help_before,
            -dndStartOffsetMinutes
        )
        else -> stringResource(
            com.brunoafk.calendardnd.R.string.dnd_timing_help_after,
            dndStartOffsetMinutes
        )
    }

    LaunchedEffect(Unit) {
        AnalyticsTracker.logScreenView(context, "settings")
    }

    val showDebugTools = debugToolsUnlocked
    var permissionErrorDismissed by remember { mutableStateOf(false) }
    val highlightAlpha = remember { Animatable(0f) }
    var highlightPlayed by rememberSaveable { mutableStateOf(false) }
    var isCheckingDnd by remember { mutableStateOf(false) }

    fun refreshPermissions() {
        hasCalendarPermission = PermissionUtils.hasCalendarPermission(context)
        hasPolicyAccess = dndController.hasPolicyAccess()
    }

    val currentError = when {
        !hasCalendarPermission -> AppError.CalendarPermissionDenied
        !hasPolicyAccess -> AppError.DndPermissionDenied
        else -> null
    }

    LaunchedEffect(currentError) {
        permissionErrorDismissed = false
    }

    LaunchedEffect(Unit) {
        refreshPermissions()
    }
    LaunchedEffect(highlightAutomation) {
        if (highlightAutomation && !highlightPlayed) {
            highlightPlayed = true
            repeat(2) { index ->
                highlightAlpha.snapTo(1f)
                highlightAlpha.animateTo(0f, animationSpec = tween(durationMillis = 700))
                if (index == 0) {
                    kotlinx.coroutines.delay(120L)
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            OneUiTopAppBar(
                onNavigateBack = onNavigateBack,
                title = stringResource(com.brunoafk.calendardnd.R.string.settings)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                if (currentError != null && !permissionErrorDismissed) {
                    ErrorCard(
                        error = currentError,
                        onPrimaryAction = {
                            when (currentError) {
                                AppError.CalendarPermissionDenied -> openAppSettings(context)
                                AppError.DndPermissionDenied -> dndController.openPolicyAccessSettings()
                                AppError.CalendarQueryFailed -> {}
                                AppError.DndChangeFailed -> {}
                                AppError.NoCalendarsFound -> openCalendarApp(context)
                                AppError.NetworkError -> {}
                            }
                        },
                        onDismiss = { permissionErrorDismissed = true }
                    )
                }
            }

            item {
                SettingsSection(title = stringResource(com.brunoafk.calendardnd.R.string.settings_general)) {
                    SettingsSwitchRow(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(
                                    alpha = 0.14f * highlightAlpha.value
                                ),
                                MaterialTheme.shapes.small
                            ),
                        title = stringResource(com.brunoafk.calendardnd.R.string.automation),
                        subtitle = stringResource(com.brunoafk.calendardnd.R.string.automation_description),
                        checked = automationEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                settingsStore.setAutomationEnabled(enabled)
                                EngineRunner.runEngine(context, Trigger.USER_TOGGLE)
                                AnalyticsTracker.logAutomationToggle(
                                    context,
                                    enabled,
                                    "settings_toggle"
                                )
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsNavigationRow(
                        title = stringResource(com.brunoafk.calendardnd.R.string.selected_calendars),
                        value = if (selectedCalendarIds.isEmpty()) {
                            stringResource(com.brunoafk.calendardnd.R.string.all_calendars)
                        } else {
                            stringResource(
                                com.brunoafk.calendardnd.R.string.calendars_selected,
                                selectedCalendarIds.size
                            )
                        },
                        onClick = onNavigateToCalendarPicker
                    )
                    SettingsDivider()
                    SettingsNavigationRow(
                        title = stringResource(com.brunoafk.calendardnd.R.string.language_title),
                        subtitle = stringResource(com.brunoafk.calendardnd.R.string.language_description),
                        onClick = onNavigateToLanguage
                    )
                    SettingsDivider()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = {
                                if (!isCheckingDnd) {
                                    isCheckingDnd = true
                                    scope.launch(Dispatchers.IO) {
                                        EngineRunner.runEngine(context, Trigger.MANUAL)
                                        delay(500L)
                                        isCheckingDnd = false
                                        scope.launch(Dispatchers.Main) {
                                            Toast.makeText(
                                                context,
                                                context.getString(com.brunoafk.calendardnd.R.string.check_dnd_complete),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isCheckingDnd
                        ) {
                            if (isCheckingDnd) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(stringResource(com.brunoafk.calendardnd.R.string.run_engine_now))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(com.brunoafk.calendardnd.R.string.manual_check_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            item {
                SettingsSection(title = stringResource(com.brunoafk.calendardnd.R.string.event_filters)) {
                    SettingsSwitchRow(
                        title = stringResource(com.brunoafk.calendardnd.R.string.busy_only_title),
                        subtitle = stringResource(com.brunoafk.calendardnd.R.string.busy_only_description),
                        helpText = if (!busyOnly) {
                            stringResource(com.brunoafk.calendardnd.R.string.settings_busy_only_help)
                        } else {
                            null
                        },
                        checked = busyOnly,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                settingsStore.setBusyOnly(enabled)
                                AnalyticsTracker.logSettingsChanged(
                                    context,
                                    "busy_only",
                                    enabled.toString()
                                )
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        title = stringResource(com.brunoafk.calendardnd.R.string.ignore_allday_title),
                        subtitle = stringResource(com.brunoafk.calendardnd.R.string.ignore_allday_description),
                        checked = ignoreAllDay,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                settingsStore.setIgnoreAllDay(enabled)
                                AnalyticsTracker.logSettingsChanged(
                                    context,
                                    "ignore_all_day",
                                    enabled.toString()
                                )
                            }
                        }
                    )
                    SettingsDivider()
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(com.brunoafk.calendardnd.R.string.min_duration_title),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            stringResource(
                                com.brunoafk.calendardnd.R.string.min_duration_description,
                                minEventMinutes
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = minEventMinutes.toFloat(),
                            onValueChange = { value ->
                                scope.launch {
                                    settingsStore.setMinEventMinutes(value.toInt())
                                }
                            },
                            onValueChangeFinished = {
                                AnalyticsTracker.logSettingsChanged(
                                    context,
                                    "min_event_minutes",
                                    minEventMinutes.toString()
                                )
                            },
                            valueRange = 5f..60f,
                            steps = 10
                        )
                        Text(
                            stringResource(
                                com.brunoafk.calendardnd.R.string.min_duration_value,
                                minEventMinutes
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        SettingsHelpText(
                            text = stringResource(
                                com.brunoafk.calendardnd.R.string.settings_min_duration_help
                            ),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            item {
                SettingsSection(title = stringResource(com.brunoafk.calendardnd.R.string.dnd_mode)) {
                    SettingsNavigationRow(
                        title = stringResource(com.brunoafk.calendardnd.R.string.dnd_mode),
                        subtitle = stringResource(com.brunoafk.calendardnd.R.string.help_item_dnd_mode_summary),
                        value = if (dndMode == DndMode.PRIORITY) {
                            stringResource(com.brunoafk.calendardnd.R.string.priority_mode_title)
                        } else {
                            stringResource(com.brunoafk.calendardnd.R.string.total_silence_title)
                        },
                        onClick = onNavigateToDndMode
                    )
                    SettingsDivider()
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(com.brunoafk.calendardnd.R.string.dnd_timing_start_title),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            dndOffsetLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = dndStartOffsetMinutes.toFloat(),
                            onValueChange = { value ->
                                scope.launch {
                                    settingsStore.setDndStartOffsetMinutes(value.toInt())
                                }
                            },
                            onValueChangeFinished = {
                                AnalyticsTracker.logSettingsChanged(
                                    context,
                                    "dnd_start_offset_minutes",
                                    dndStartOffsetMinutes.toString()
                                )
                            },
                            valueRange = -30f..30f,
                            steps = 11
                        )
                        Text(
                            dndOffsetValue,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        SettingsHelpText(
                            text = dndOffsetHelpText,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            item {
                SettingsSection(title = stringResource(com.brunoafk.calendardnd.R.string.notifications_title)) {
                    SettingsSwitchRow(
                        title = stringResource(com.brunoafk.calendardnd.R.string.pre_dnd_notification_setting_title),
                        subtitle = stringResource(com.brunoafk.calendardnd.R.string.pre_dnd_notification_setting),
                        checked = preDndNotificationEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                settingsStore.setPreDndNotificationEnabled(enabled)
                                settingsStore.setPreDndNotificationUserSet(true)
                                AnalyticsTracker.logSettingsChanged(
                                    context,
                                    "pre_dnd_notification",
                                    enabled.toString()
                                )
                            }
                        }
                    )
                }
            }

            item {
                SettingsSection(title = stringResource(com.brunoafk.calendardnd.R.string.privacy_title)) {
                    SettingsNavigationRow(
                        title = stringResource(com.brunoafk.calendardnd.R.string.permissions_title),
                        subtitle = stringResource(com.brunoafk.calendardnd.R.string.permissions_subtitle),
                        onClick = onNavigateToPermissions
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        title = stringResource(com.brunoafk.calendardnd.R.string.analytics_opt_in_title),
                        subtitle = stringResource(com.brunoafk.calendardnd.R.string.analytics_opt_in_description),
                        checked = analyticsOptIn,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                settingsStore.setAnalyticsOptIn(enabled)
                                if (AppConfig.analyticsEnabled) {
                                    FirebaseAnalytics.getInstance(context)
                                        .setAnalyticsCollectionEnabled(enabled)
                                }
                                AnalyticsTracker.logSettingsChanged(
                                    context,
                                    "analytics_opt_in",
                                    enabled.toString()
                                )
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        title = stringResource(com.brunoafk.calendardnd.R.string.crashlytics_opt_in_title),
                        subtitle = stringResource(com.brunoafk.calendardnd.R.string.crashlytics_opt_in_description),
                        checked = crashlyticsOptIn,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                settingsStore.setCrashlyticsOptIn(enabled)
                                FirebaseCrashlytics.getInstance()
                                    .setCrashlyticsCollectionEnabled(
                                        AppConfig.crashlyticsEnabled && enabled
                                    )
                                AnalyticsTracker.logSettingsChanged(
                                    context,
                                    "crashlytics_opt_in",
                                    enabled.toString()
                                )
                            }
                        }
                    )
                }
            }

            item {
                SettingsSection(title = stringResource(com.brunoafk.calendardnd.R.string.help_title)) {
                    if (BuildConfig.MANUAL_UPDATE_ENABLED && showUpdatesMenu) {
                        SettingsNavigationRow(
                            title = stringResource(com.brunoafk.calendardnd.R.string.update_menu_title),
                            subtitle = stringResource(com.brunoafk.calendardnd.R.string.update_menu_subtitle),
                            onClick = onNavigateToUpdates
                        )
                        SettingsDivider()
                    }
                    SettingsNavigationRow(
                        title = stringResource(com.brunoafk.calendardnd.R.string.help_about_title),
                        subtitle = stringResource(com.brunoafk.calendardnd.R.string.help_about_subtitle),
                        onClick = onNavigateToAbout
                    )
                    SettingsDivider()
                    SettingsNavigationRow(
                        title = stringResource(com.brunoafk.calendardnd.R.string.help_feature_guide_title),
                        subtitle = stringResource(com.brunoafk.calendardnd.R.string.help_feature_guide_subtitle),
                        onClick = onNavigateToHelp
                    )
                    if (showDebugTools) {
                        SettingsDivider()
                        SettingsNavigationRow(
                            title = stringResource(com.brunoafk.calendardnd.R.string.debug_tools_open_title),
                            subtitle = stringResource(com.brunoafk.calendardnd.R.string.debug_tools_open_subtitle),
                            onClick = onNavigateToDebugTools
                        )
                    }
                }
            }

        }
    }
}

private fun openAppSettings(context: android.content.Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    ).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}

private fun openCalendarApp(context: android.content.Context) {
    val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_APP_CALENDAR)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}
