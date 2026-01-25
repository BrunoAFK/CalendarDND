package com.brunoafk.calendardnd.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.brunoafk.calendardnd.BuildConfig
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.data.dnd.DndController
import com.brunoafk.calendardnd.data.prefs.SettingsStore
import com.brunoafk.calendardnd.domain.model.DndMode
import com.brunoafk.calendardnd.domain.model.MeetingWindow
import com.brunoafk.calendardnd.system.alarms.AlarmScheduler
import com.brunoafk.calendardnd.system.update.ManualUpdateManager
import com.brunoafk.calendardnd.ui.components.AppError
import com.brunoafk.calendardnd.ui.components.DndModeBanner
import com.brunoafk.calendardnd.ui.components.InfoBanner
import com.brunoafk.calendardnd.ui.components.PersistentWarningBanner
import com.brunoafk.calendardnd.ui.components.StatusBanner
import com.brunoafk.calendardnd.ui.components.StatusBannerKind
import com.brunoafk.calendardnd.ui.components.StatusBannerState
import com.brunoafk.calendardnd.ui.components.WarningBanner
import com.brunoafk.calendardnd.ui.components.ErrorCard
import com.brunoafk.calendardnd.ui.theme.surfaceColorAtElevation
import com.brunoafk.calendardnd.util.TimeUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun StatusBannerBlock(
    modifier: Modifier = Modifier,
    showTileHint: Boolean,
    onTileHintDismissed: () -> Unit,
    updateStatus: ManualUpdateManager.UpdateStatus?,
    signatureStatus: ManualUpdateManager.SignatureStatus,
    showStatusBanner: Boolean = true,
    useModernDesign: Boolean = false,
    automationEnabled: Boolean,
    hasCalendarPermission: Boolean,
    hasPolicyAccess: Boolean,
    dndSetByApp: Boolean,
    activeWindow: MeetingWindow?,
    dndMode: DndMode,
    canScheduleExactAlarms: Boolean,
    onboardingCompleted: Boolean,
    onOpenUpdates: () -> Unit,
    onOpenSettings: (Boolean) -> Unit,
    onOpenSetup: () -> Unit,
    onOpenDndMode: () -> Unit,
    settingsStore: SettingsStore,
    alarmScheduler: AlarmScheduler,
    dndController: DndController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var warningDismissed by rememberSaveable { mutableStateOf(false) }
    var permissionErrorDismissed by remember { mutableStateOf(false) }
    var dndBannerDismissed by remember { mutableStateOf(false) }
    var refreshBannerDismissed by remember { mutableStateOf(false) }
    var tileHintVisible by remember { mutableStateOf(false) }
    var lastSeenUpdateVersion by remember { mutableStateOf("") }

    LaunchedEffect(showTileHint) {
        tileHintVisible = showTileHint
    }

    LaunchedEffect(Unit) {
        settingsStore.dndModeBannerDismissed.collectLatest { dismissed ->
            dndBannerDismissed = dismissed
        }
    }

    LaunchedEffect(Unit) {
        settingsStore.refreshBannerDismissed.collectLatest { dismissed ->
            refreshBannerDismissed = dismissed
        }
    }

    LaunchedEffect(Unit) {
        settingsStore.lastSeenUpdateVersion.collectLatest { value ->
            lastSeenUpdateVersion = value ?: ""
        }
    }

    fun formatTimeUntil(targetMs: Long): String {
        val remaining = targetMs - System.currentTimeMillis()
        return if (remaining <= 0) {
            TimeUtils.formatDuration(0)
        } else {
            TimeUtils.formatDuration(remaining)
        }
    }

    val statusBannerState = run {
        val missingPermissions = listOf(!hasCalendarPermission, !hasPolicyAccess).count { it }
        val dndModeLabel = if (dndMode == DndMode.PRIORITY) {
            stringResource(R.string.priority_mode_title)
        } else {
            stringResource(R.string.total_silence_title)
        }
        when {
            missingPermissions > 0 -> StatusBannerState(
                kind = StatusBannerKind.MissingPermissions,
                statusText = stringResource(R.string.status_setup_required),
                contextText = stringResource(R.string.status_missing_permissions_summary)
            )
            !automationEnabled -> StatusBannerState(
                kind = StatusBannerKind.Disabled,
                statusText = stringResource(R.string.status_automation_paused),
                contextText = stringResource(R.string.status_automation_paused_summary)
            )
            dndSetByApp && activeWindow != null -> StatusBannerState(
                kind = StatusBannerKind.DndActive,
                statusText = stringResource(R.string.status_dnd_active),
                contextText = stringResource(
                    R.string.status_dnd_active_summary,
                    formatTimeUntil(activeWindow.end)
                )
            )
            else -> StatusBannerState(
                kind = StatusBannerKind.Enabled,
                statusText = stringResource(R.string.status_automation_ready),
                contextText = stringResource(R.string.status_automation_ready_summary, dndModeLabel)
            )
        }
    }

    val currentError = when {
        !hasCalendarPermission -> AppError.CalendarPermissionDenied
        !hasPolicyAccess -> AppError.DndPermissionDenied
        else -> null
    }

    LaunchedEffect(currentError) {
        permissionErrorDismissed = false
    }

    val showWarningBanner = !canScheduleExactAlarms && !warningDismissed
    val showDndBanner = automationEnabled && hasPolicyAccess && !dndBannerDismissed && !showWarningBanner
    val showRefreshBanner = !showWarningBanner && !showDndBanner && !refreshBannerDismissed

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!signatureStatus.isAllowed &&
            !(BuildConfig.DEBUG && BuildConfig.MANUAL_UPDATE_ENABLED)
        ) {
            val messageRes = if (signatureStatus.isPinned) {
                R.string.signature_warning_message
            } else {
                R.string.signature_warning_unconfigured
            }
            PersistentWarningBanner(
                title = stringResource(R.string.signature_warning_title),
                message = stringResource(messageRes)
            )
        }

        if (tileHintVisible) {
            val tileHintMessage = if (automationEnabled) {
                stringResource(R.string.tile_hint_enabled_message)
            } else {
                stringResource(R.string.tile_hint_disabled_message)
            }
            if (useModernDesign) {
                ModernInfoBanner(
                    title = stringResource(R.string.tile_hint_title),
                    message = tileHintMessage,
                    onDismiss = {
                        tileHintVisible = false
                        onTileHintDismissed()
                    }
                )
            } else {
                InfoBanner(
                    title = stringResource(R.string.tile_hint_title),
                    message = tileHintMessage,
                    onDismiss = {
                        tileHintVisible = false
                        onTileHintDismissed()
                    }
                )
            }
        }

        val updateVersion = updateStatus?.info?.versionName
        if (!updateVersion.isNullOrBlank() && updateVersion != lastSeenUpdateVersion) {
            if (useModernDesign) {
                ModernInfoBanner(
                    title = stringResource(R.string.update_banner_title),
                    message = stringResource(R.string.update_banner_message, updateVersion),
                    backgroundColor = Color(0xFFE8F5E9), // Light green for updates
                    contentColor = Color(0xFF2E7D32), // Green
                    onDismiss = {
                        scope.launch { settingsStore.setLastSeenUpdateVersion(updateVersion) }
                    },
                    onClick = {
                        scope.launch { settingsStore.setLastSeenUpdateVersion(updateVersion) }
                        onOpenUpdates()
                    }
                )
            } else {
                InfoBanner(
                    title = stringResource(R.string.update_banner_title),
                    message = stringResource(R.string.update_banner_message, updateVersion),
                    onDismiss = {
                        scope.launch { settingsStore.setLastSeenUpdateVersion(updateVersion) }
                    },
                    onClick = {
                        scope.launch { settingsStore.setLastSeenUpdateVersion(updateVersion) }
                        onOpenUpdates()
                    }
                )
            }
        } else if (showDndBanner) {
            val dndModeLabel = stringResource(
                if (dndMode == DndMode.PRIORITY) {
                    R.string.priority_mode_title
                } else {
                    R.string.total_silence_title
                }
            )
            if (useModernDesign) {
                ModernInfoBanner(
                    title = stringResource(R.string.dnd_mode_banner_title),
                    message = stringResource(R.string.dnd_mode_banner_message, dndModeLabel),
                    backgroundColor = Color(0xFFF3E5F5), // Light purple
                    contentColor = Color(0xFF7B1FA2), // Purple
                    onClick = {
                        scope.launch { settingsStore.setDndModeBannerDismissed(true) }
                        onOpenDndMode()
                    },
                    onDismiss = {
                        scope.launch { settingsStore.setDndModeBannerDismissed(true) }
                    }
                )
            } else {
                DndModeBanner(
                    title = stringResource(R.string.dnd_mode_banner_title),
                    message = stringResource(R.string.dnd_mode_banner_message, dndModeLabel),
                    onClick = {
                        scope.launch { settingsStore.setDndModeBannerDismissed(true) }
                        onOpenDndMode()
                    },
                    onDismiss = {
                        scope.launch { settingsStore.setDndModeBannerDismissed(true) }
                    }
                )
            }
        } else if (showRefreshBanner) {
            if (useModernDesign) {
                ModernInfoBanner(
                    title = stringResource(R.string.refresh_banner_title),
                    message = stringResource(R.string.refresh_banner_message),
                    onDismiss = { scope.launch { settingsStore.setRefreshBannerDismissed(true) } }
                )
            } else {
                InfoBanner(
                    title = stringResource(R.string.refresh_banner_title),
                    message = stringResource(R.string.refresh_banner_message),
                    onDismiss = { scope.launch { settingsStore.setRefreshBannerDismissed(true) } }
                )
            }
        }

        if (showStatusBanner) {
            StatusBanner(
                state = statusBannerState,
                modifier = Modifier.padding(top = 4.dp),
                onClick = if (!automationEnabled) {
                    { onOpenSettings(true) }
                } else {
                    null
                }
            )
        }

        if (showWarningBanner) {
            if (useModernDesign) {
                ModernWarningBanner(
                    message = stringResource(R.string.warning_degraded_mode),
                    actionLabel = stringResource(R.string.fix_now),
                    onAction = { alarmScheduler.openExactAlarmSettings() },
                    onDismiss = { warningDismissed = true }
                )
            } else {
                WarningBanner(
                    message = stringResource(R.string.warning_degraded_mode),
                    actionLabel = stringResource(R.string.fix_now),
                    onAction = { alarmScheduler.openExactAlarmSettings() },
                    onDismiss = { warningDismissed = true }
                )
            }
        }

        if (currentError != null && !permissionErrorDismissed) {
            ErrorCard(
                error = currentError,
                onPrimaryAction = {
                    when (currentError) {
                        AppError.CalendarPermissionDenied -> StatusScreenIntents.openAppSettings(context)
                        AppError.DndPermissionDenied -> dndController.openPolicyAccessSettings()
                        AppError.CalendarQueryFailed -> Unit
                        AppError.DndChangeFailed -> Unit
                        AppError.NoCalendarsFound -> StatusScreenIntents.openCalendarApp(context)
                        AppError.NetworkError -> Unit
                    }
                },
                onDismiss = { permissionErrorDismissed = true }
            )
        } else if (!hasCalendarPermission || !hasPolicyAccess) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = surfaceColorAtElevation(1.dp)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.setup_required),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (!onboardingCompleted) {
                        Button(
                            onClick = onOpenSetup,
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            Text(stringResource(R.string.complete_setup))
                        }
                    }
                }
            }
        }
    }
}

object StatusScreenIntents {
    fun openAppSettings(context: android.content.Context) {
        val intent = android.content.Intent(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android.net.Uri.fromParts("package", context.packageName, null)
        ).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openCalendarApp(context: android.content.Context) {
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            addCategory(android.content.Intent.CATEGORY_APP_CALENDAR)
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openCalendarEvent(context: android.content.Context, eventId: Long, beginMs: Long) {
        val uri = android.content.ContentUris.withAppendedId(
            android.provider.CalendarContract.Events.CONTENT_URI,
            eventId
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            data = uri
            putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginMs)
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}

/**
 * Modern banner design - minimal, clean, and consistent with V4Improved design language
 */
@Composable
fun ModernInfoBanner(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Info,
    backgroundColor: Color = Color(0xFFE3F2FD), // Light blue
    contentColor: Color = Color(0xFF1565C0), // Blue
    onDismiss: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
            if (onDismiss != null) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(18.dp),
                        tint = contentColor.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun ModernWarningBanner(
    message: String,
    actionLabel: String,
    modifier: Modifier = Modifier,
    onAction: () -> Unit,
    onDismiss: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFFF3E0) // Light orange
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color(0xFFE65100) // Orange
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE65100),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFE65100)
            ) {
                Text(
                    text = actionLabel,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
            if (onDismiss != null) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Dismiss",
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFFE65100).copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
