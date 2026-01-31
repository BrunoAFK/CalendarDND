package com.brunoafk.calendardnd.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.data.dnd.DndController
import com.brunoafk.calendardnd.system.alarms.AlarmScheduler
import com.brunoafk.calendardnd.ui.components.OneUiTopAppBar
import com.brunoafk.calendardnd.ui.components.PermissionCard
import com.brunoafk.calendardnd.util.PermissionUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val dndController = remember { DndController(context) }
    val alarmScheduler = remember { AlarmScheduler(context) }

    var hasCalendarPermission by remember {
        mutableStateOf(PermissionUtils.hasCalendarPermission(context))
    }
    var hasPolicyAccess by remember { mutableStateOf(dndController.hasPolicyAccess()) }
    var hasExactAlarms by remember { mutableStateOf(alarmScheduler.canScheduleExactAlarms()) }
    var hasNotificationPermission by remember {
        mutableStateOf(PermissionUtils.hasNotificationPermission(context))
    }
    var batteryOptimized by remember {
        mutableStateOf(isBatteryOptimized(context))
    }

    fun refreshPermissions() {
        hasCalendarPermission = PermissionUtils.hasCalendarPermission(context)
        hasPolicyAccess = dndController.hasPolicyAccess()
        hasExactAlarms = alarmScheduler.canScheduleExactAlarms()
        hasNotificationPermission = PermissionUtils.hasNotificationPermission(context)
        batteryOptimized = isBatteryOptimized(context)
    }

    LaunchedEffect(Unit) {
        refreshPermissions()
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
                title = stringResource(R.string.permissions_title)
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
                    text = stringResource(R.string.permissions_subtitle),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                PermissionCard(
                    title = stringResource(R.string.calendar_permission_title),
                    description = buildString {
                        append(stringResource(R.string.calendar_permission_description))
                        append(" ")
                        append(stringResource(R.string.calendar_permission_rationale))
                    },
                    isGranted = hasCalendarPermission,
                    actionLabel = stringResource(R.string.open_settings),
                    onAction = { openAppSettings(context) },
                    icon = Icons.Default.CalendarMonth,
                    isRequired = true,
                    requiredLabel = stringResource(R.string.permission_required_label),
                    whyLabel = stringResource(R.string.why_we_need_this)
                )
            }
            item {
                PermissionCard(
                    title = stringResource(R.string.dnd_permission_title),
                    description = buildString {
                        append(stringResource(R.string.dnd_permission_description))
                        append(" ")
                        append(stringResource(R.string.dnd_permission_rationale))
                    },
                    isGranted = hasPolicyAccess,
                    actionLabel = stringResource(R.string.open_settings),
                    onAction = { dndController.openPolicyAccessSettings() },
                    icon = Icons.Default.DoNotDisturb,
                    isRequired = true,
                    requiredLabel = stringResource(R.string.permission_required_label),
                    whyLabel = stringResource(R.string.why_we_need_this)
                )
            }
            item {
                PermissionCard(
                    title = stringResource(R.string.notification_permission_title),
                    description = buildString {
                        append(stringResource(R.string.notification_permission_description))
                        append(" ")
                        append(stringResource(R.string.notification_permission_rationale))
                    },
                    isGranted = hasNotificationPermission,
                    actionLabel = stringResource(R.string.open_settings),
                    onAction = { openNotificationSettings(context) },
                    icon = Icons.Default.Notifications,
                    isRequired = false,
                    requiredLabel = stringResource(R.string.permission_required_label),
                    whyLabel = stringResource(R.string.why_we_need_this)
                )
            }
            item {
                PermissionCard(
                    title = stringResource(R.string.battery_title),
                    description = buildString {
                        append(stringResource(R.string.battery_description))
                        append(" ")
                        append(stringResource(R.string.battery_rationale))
                    },
                    isGranted = !batteryOptimized,
                    actionLabel = stringResource(R.string.open_settings),
                    onAction = { openBatterySettings(context) },
                    icon = Icons.Default.BatterySaver,
                    isRequired = false,
                    requiredLabel = stringResource(R.string.permission_required_label),
                    whyLabel = stringResource(R.string.why_we_need_this)
                )
            }
            item {
                PermissionCard(
                    title = stringResource(R.string.exact_alarm_title),
                    description = buildString {
                        append(stringResource(R.string.exact_alarm_description))
                        append(" ")
                        append(stringResource(R.string.exact_alarm_rationale))
                    },
                    isGranted = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                        true
                    } else {
                        hasExactAlarms
                    },
                    actionLabel = stringResource(R.string.open_settings),
                    onAction = { alarmScheduler.openExactAlarmSettings() },
                    icon = Icons.Default.Alarm,
                    isRequired = false,
                    requiredLabel = stringResource(R.string.permission_required_label),
                    whyLabel = stringResource(R.string.why_we_need_this)
                )
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

private fun openNotificationSettings(context: android.content.Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}

private fun openBatterySettings(context: android.content.Context) {
    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}

private fun isBatteryOptimized(context: android.content.Context): Boolean {
    val powerManager =
        context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
    return !powerManager.isIgnoringBatteryOptimizations(context.packageName)
}
