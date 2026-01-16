package com.brunoafk.calendardnd.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.net.Uri
import android.content.ContextWrapper
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.data.dnd.DndController
import com.brunoafk.calendardnd.data.prefs.SettingsStore
import com.brunoafk.calendardnd.system.alarms.AlarmScheduler
import com.brunoafk.calendardnd.ui.components.PermissionCard
import com.brunoafk.calendardnd.ui.components.PrimaryActionButton
import com.brunoafk.calendardnd.util.AnalyticsTracker
import com.brunoafk.calendardnd.util.PermissionUtils
import androidx.compose.ui.graphics.Brush
import androidx.core.app.ActivityCompat

/**
 * Consolidated state for all permissions required by the app
 */
private data class PermissionsState(
    val hasCalendar: Boolean = false,
    val hasDndPolicy: Boolean = false,
    val hasExactAlarms: Boolean = false,
    val hasNotifications: Boolean = false,
    val batteryOptimized: Boolean = true
) {
    /** Required permissions for core functionality */
    val hasRequiredPermissions: Boolean
        get() = hasCalendar && hasDndPolicy

    /** All permissions granted */
    val hasAllPermissions: Boolean
        get() = hasRequiredPermissions && hasExactAlarms && hasNotifications && !batteryOptimized
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val isCompactHeight = configuration.screenHeightDp < 700
    val contentSpacing = if (isCompactHeight) 12.dp else 16.dp
    val cardPadding = if (isCompactHeight) PaddingValues(12.dp) else PaddingValues(16.dp)
    val bottomBarHeight = if (isCompactHeight) 104.dp else 120.dp
    val bottomBarBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface.copy(alpha = 0f),
            MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            MaterialTheme.colorScheme.surface
        )
    )

    // System controllers - consider injecting these via DI
    val dndController = remember { DndController(context) }
    val alarmScheduler = remember { AlarmScheduler(context) }
    val settingsStore = remember { SettingsStore(context) }

    // Consolidated permission state
    var permissionsState by remember {
        mutableStateOf(createPermissionsState(context, dndController, alarmScheduler))
    }

    val preDndNotificationUserSet by settingsStore.preDndNotificationUserSet
        .collectAsState(initial = false)

    // Track if we've already auto-enabled notifications to prevent duplicate calls
    var hasAutoEnabledNotifications by remember { mutableStateOf(false) }
    var requestedCalendarPermission by remember { mutableStateOf(false) }
    var calendarPermissionPermanentlyDenied by remember { mutableStateOf(false) }

    // Permission launchers
    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionsState = permissionsState.copy(hasCalendar = granted)
        if (!granted && requestedCalendarPermission) {
            val activity = context.findActivity()
            val showRationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(
                    it,
                    android.Manifest.permission.READ_CALENDAR
                )
            } ?: true
            calendarPermissionPermanentlyDenied = !showRationale
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionsState = permissionsState.copy(hasNotifications = granted)
    }

    // Track screen view
    LaunchedEffect(Unit) {
        AnalyticsTracker.logScreenView(context, "onboarding")
    }

    // Auto-enable pre-DND notifications when permission is granted
    LaunchedEffect(permissionsState.hasNotifications, preDndNotificationUserSet) {
        if (permissionsState.hasNotifications &&
            !preDndNotificationUserSet &&
            !hasAutoEnabledNotifications) {
            settingsStore.setPreDndNotificationEnabled(true)
            settingsStore.setPreDndNotificationUserSet(true)
            hasAutoEnabledNotifications = true
        }
    }

    // Refresh permissions when returning to screen
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionsState = createPermissionsState(context, dndController, alarmScheduler)
                if (!permissionsState.hasCalendar && requestedCalendarPermission) {
                    val activity = context.findActivity()
                    val showRationale = activity?.let {
                        ActivityCompat.shouldShowRequestPermissionRationale(
                            it,
                            android.Manifest.permission.READ_CALENDAR
                        )
                    } ?: true
                    calendarPermissionPermanentlyDenied = !showRationale
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.onboarding_title)) })
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = bottomBarHeight) // Space for sticky action bar
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(contentSpacing)) {
                    Text(
                        text = stringResource(
                            if (isCompactHeight) {
                                R.string.onboarding_description_short
                            } else {
                                R.string.onboarding_description
                            }
                        )
                    )

                    // REQUIRED PERMISSIONS
                    PermissionCard(
                        title = stringResource(R.string.calendar_permission_title),
                        description = buildString {
                            append(
                                stringResource(
                                    if (isCompactHeight) {
                                        R.string.calendar_permission_description_short
                                    } else {
                                        R.string.calendar_permission_description
                                    }
                                )
                            )
                            append(" ")
                            append(stringResource(R.string.calendar_permission_rationale))
                        },
                        isGranted = permissionsState.hasCalendar,
                        actionLabel = stringResource(
                            if (calendarPermissionPermanentlyDenied) {
                                R.string.open_app_settings
                            } else {
                                R.string.grant_permission
                            }
                        ),
                        onAction = {
                            if (calendarPermissionPermanentlyDenied) {
                                openAppSettings(context)
                            } else {
                                requestedCalendarPermission = true
                                calendarPermissionLauncher.launch(
                                    android.Manifest.permission.READ_CALENDAR
                                )
                            }
                        },
                        icon = Icons.Default.CalendarMonth,
                        isRequired = true,
                        contentPadding = cardPadding,
                        requiredLabel = stringResource(R.string.permission_required_label),
                        whyLabel = stringResource(R.string.why_we_need_this)
                    )

                    PermissionCard(
                        title = stringResource(R.string.dnd_permission_title),
                        description = buildString {
                            append(
                                stringResource(
                                    if (isCompactHeight) {
                                        R.string.dnd_permission_description_short
                                    } else {
                                        R.string.dnd_permission_description
                                    }
                                )
                            )
                            append(" ")
                            append(stringResource(R.string.dnd_permission_rationale))
                        },
                        isGranted = permissionsState.hasDndPolicy,
                        actionLabel = stringResource(R.string.open_settings),
                        onAction = { dndController.openPolicyAccessSettings() },
                        icon = Icons.Default.DoNotDisturb,
                        isRequired = true,
                        contentPadding = cardPadding,
                        requiredLabel = stringResource(R.string.permission_required_label),
                        whyLabel = stringResource(R.string.why_we_need_this)
                    )

                    PermissionCard(
                        title = stringResource(R.string.notification_permission_title),
                        description = buildString {
                            append(
                                stringResource(
                                    if (isCompactHeight) {
                                        R.string.notification_permission_description_short
                                    } else {
                                        R.string.notification_permission_description
                                    }
                                )
                            )
                            append(" ")
                            append(stringResource(R.string.notification_permission_rationale))
                        },
                        isGranted = permissionsState.hasNotifications,
                        actionLabel = stringResource(R.string.grant_permission),
                        onAction = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(
                                    android.Manifest.permission.POST_NOTIFICATIONS
                                )
                            }
                        },
                        icon = Icons.Default.Notifications,
                        isRequired = false,
                        contentPadding = cardPadding,
                        requiredLabel = stringResource(R.string.permission_required_label),
                        whyLabel = stringResource(R.string.why_we_need_this)
                    )

                    PermissionCard(
                        title = stringResource(R.string.battery_title),
                        description = buildString {
                            append(
                                stringResource(
                                    if (isCompactHeight) {
                                        R.string.battery_description_short
                                    } else {
                                        R.string.battery_description
                                    }
                                )
                            )
                            append(" ")
                            append(stringResource(R.string.battery_rationale))
                        },
                        isGranted = !permissionsState.batteryOptimized,
                        actionLabel = stringResource(R.string.open_settings),
                        onAction = { openBatterySettings(context) },
                        icon = Icons.Default.BatterySaver,
                        isRequired = false,
                        contentPadding = cardPadding,
                        requiredLabel = stringResource(R.string.permission_required_label),
                        whyLabel = stringResource(R.string.why_we_need_this)
                    )

                    PermissionCard(
                        title = stringResource(R.string.exact_alarm_title),
                        description = buildString {
                            append(
                                stringResource(
                                    if (isCompactHeight) {
                                        R.string.exact_alarm_description_short
                                    } else {
                                        R.string.exact_alarm_description
                                    }
                                )
                            )
                            append(" ")
                            append(stringResource(R.string.exact_alarm_rationale))
                        },
                        isGranted = permissionsState.hasExactAlarms,
                        actionLabel = stringResource(R.string.open_settings),
                        onAction = { alarmScheduler.openExactAlarmSettings() },
                        icon = Icons.Default.Alarm,
                        isRequired = false,
                        contentPadding = cardPadding,
                        requiredLabel = stringResource(R.string.permission_required_label),
                        whyLabel = stringResource(R.string.why_we_need_this)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(bottomBarBrush)
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 16.dp)
            ) {
                if (!permissionsState.hasRequiredPermissions) {
                    Text(
                        text = stringResource(R.string.permissions_helper_text),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                PrimaryActionButton(
                    label = stringResource(R.string.continue_button),
                    onClick = onContinue,
                    enabled = permissionsState.hasRequiredPermissions
                )
            }
        }
    }

}

/**
 * Creates the current permissions state by checking all permissions
 */
private fun createPermissionsState(
    context: Context,
    dndController: DndController,
    alarmScheduler: AlarmScheduler
): PermissionsState {
    return PermissionsState(
        hasCalendar = PermissionUtils.hasCalendarPermission(context),
        hasDndPolicy = dndController.hasPolicyAccess(),
        hasExactAlarms = alarmScheduler.canScheduleExactAlarms(),
        hasNotifications = PermissionUtils.hasNotificationPermission(context),
        batteryOptimized = !isIgnoringBatteryOptimizations(context)
    )
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        return true
    }
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

private fun openBatterySettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        return
    }
    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}

private fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    ).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}

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
