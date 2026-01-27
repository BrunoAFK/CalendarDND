package com.brunoafk.calendardnd.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.brunoafk.calendardnd.data.calendar.CalendarRepository
import com.brunoafk.calendardnd.data.dnd.DndController
import com.brunoafk.calendardnd.data.prefs.RuntimeStateStore
import com.brunoafk.calendardnd.data.prefs.SettingsStore
import com.brunoafk.calendardnd.domain.model.DndMode
import com.brunoafk.calendardnd.domain.model.EventInstance
import com.brunoafk.calendardnd.domain.model.KeywordMatchMode
import com.brunoafk.calendardnd.domain.model.MeetingWindow
import com.brunoafk.calendardnd.domain.model.ThemeDebugMode
import com.brunoafk.calendardnd.domain.model.Trigger
import com.brunoafk.calendardnd.domain.planning.MeetingWindowResolver
import com.brunoafk.calendardnd.system.alarms.AlarmScheduler
import com.brunoafk.calendardnd.system.alarms.EngineRunner
import com.brunoafk.calendardnd.system.update.ManualUpdateManager
import com.brunoafk.calendardnd.util.PermissionUtils
import com.brunoafk.calendardnd.util.TimeUtils
import com.brunoafk.calendardnd.ui.theme.surfaceColorAtElevation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun StatusScreenV4(
    onNavigateBack: () -> Unit,
    showTileHint: Boolean,
    onTileHintDismissed: () -> Unit,
    updateStatus: ManualUpdateManager.UpdateStatus?,
    signatureStatus: ManualUpdateManager.SignatureStatus,
    onOpenUpdates: () -> Unit,
    onOpenSettings: (Boolean) -> Unit,
    onOpenSetup: () -> Unit,
    onOpenDndMode: () -> Unit,
    onSelectTheme: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val settingsStore = remember { SettingsStore(context) }
    val runtimeStateStore = remember { RuntimeStateStore(context) }
    val calendarRepository = remember { CalendarRepository(context) }
    val dndController = remember { DndController(context) }
    val alarmScheduler = remember { AlarmScheduler(context) }

    var automationEnabled by remember { mutableStateOf(false) }
    var activeWindow by remember { mutableStateOf<MeetingWindow?>(null) }
    var nextInstance by remember { mutableStateOf<EventInstance?>(null) }
    var hasCalendarPermission by remember {
        mutableStateOf(PermissionUtils.hasCalendarPermission(context))
    }
    var hasPolicyAccess by remember { mutableStateOf(dndController.hasPolicyAccess()) }
    var dndSetByApp by remember { mutableStateOf(false) }
    var selectedCalendarIds by remember { mutableStateOf(emptySet<String>()) }
    var busyOnly by remember { mutableStateOf(true) }
    var ignoreAllDay by remember { mutableStateOf(true) }
    var skipRecurring by remember { mutableStateOf(false) }
    var selectedDaysMask by remember { mutableStateOf(com.brunoafk.calendardnd.util.WeekdayMask.ALL_DAYS_MASK) }
    var selectedDaysEnabled by remember { mutableStateOf(false) }
    var minEventMinutes by remember { mutableStateOf(10) }
    var requireLocation by remember { mutableStateOf(false) }
    var requireTitleKeyword by remember { mutableStateOf(false) }
    var titleKeyword by remember { mutableStateOf("") }
    var titleKeywordMatchMode by remember { mutableStateOf(KeywordMatchMode.KEYWORDS) }
    var titleKeywordCaseSensitive by remember { mutableStateOf(false) }
    var titleKeywordMatchAll by remember { mutableStateOf(false) }
    var titleKeywordExclude by remember { mutableStateOf(false) }
    var dndMode by remember { mutableStateOf(DndMode.PRIORITY) }
    var canScheduleExactAlarms by remember { mutableStateOf(alarmScheduler.canScheduleExactAlarms()) }
    var onboardingCompleted by remember { mutableStateOf(false) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var themeDebugMode by remember { mutableStateOf(ThemeDebugMode.OFF) }

    fun refresh() {
        scope.launch {
            hasCalendarPermission = PermissionUtils.hasCalendarPermission(context)
            hasPolicyAccess = dndController.hasPolicyAccess()
            canScheduleExactAlarms = alarmScheduler.canScheduleExactAlarms()

            if (!hasCalendarPermission) {
                activeWindow = null
                nextInstance = null
                return@launch
            }

            val snapshot = withContext(Dispatchers.IO) {
                val now = System.currentTimeMillis()
                val activeInstances = calendarRepository.getActiveInstances(
                    now = now,
                    selectedCalendarIds = selectedCalendarIds,
                    busyOnly = busyOnly,
                    ignoreAllDay = ignoreAllDay,
                    skipRecurring = skipRecurring,
                    selectedDaysEnabled = selectedDaysEnabled,
                    selectedDaysMask = selectedDaysMask,
                    minEventMinutes = minEventMinutes,
                    requireLocation = requireLocation,
                    requireTitleKeyword = requireTitleKeyword,
                    titleKeyword = titleKeyword,
                    titleKeywordMatchMode = titleKeywordMatchMode,
                    titleKeywordCaseSensitive = titleKeywordCaseSensitive,
                    titleKeywordMatchAll = titleKeywordMatchAll,
                    titleKeywordExclude = titleKeywordExclude
                )
                val window = MeetingWindowResolver.findActiveWindow(activeInstances, now)
                val next = calendarRepository.getNextInstance(
                    now = now,
                    selectedCalendarIds = selectedCalendarIds,
                    busyOnly = busyOnly,
                    ignoreAllDay = ignoreAllDay,
                    skipRecurring = skipRecurring,
                    selectedDaysEnabled = selectedDaysEnabled,
                    selectedDaysMask = selectedDaysMask,
                    minEventMinutes = minEventMinutes,
                    requireLocation = requireLocation,
                    requireTitleKeyword = requireTitleKeyword,
                    titleKeyword = titleKeyword,
                    titleKeywordMatchMode = titleKeywordMatchMode,
                    titleKeywordCaseSensitive = titleKeywordCaseSensitive,
                    titleKeywordMatchAll = titleKeywordMatchAll,
                    titleKeywordExclude = titleKeywordExclude
                )
                window to next
            }
            activeWindow = snapshot.first
            nextInstance = snapshot.second
        }
    }

    // All the DisposableEffects for data collection (same as other versions)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) { refresh() }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(Unit) {
        val job = scope.launch { settingsStore.automationEnabled.collectLatest { automationEnabled = it } }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch { settingsStore.dndMode.collectLatest { dndMode = it; refresh() } }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch {
            settingsStore.onboardingCompleted.collectLatest { completed ->
                onboardingCompleted = completed
            }
        }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch {
            settingsStore.themeDebugMode.collectLatest { mode ->
                themeDebugMode = mode
            }
        }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch { runtimeStateStore.dndSetByApp.collectLatest { dndSetByApp = it } }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch { settingsStore.selectedCalendarIds.collectLatest { selectedCalendarIds = it; refresh() } }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch { settingsStore.busyOnly.collectLatest { busyOnly = it; refresh() } }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch { settingsStore.ignoreAllDay.collectLatest { ignoreAllDay = it; refresh() } }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch { settingsStore.skipRecurring.collectLatest { skipRecurring = it; refresh() } }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch { settingsStore.selectedDaysMask.collectLatest { selectedDaysMask = it; refresh() } }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch { settingsStore.selectedDaysEnabled.collectLatest { selectedDaysEnabled = it; refresh() } }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch { settingsStore.minEventMinutes.collectLatest { minEventMinutes = it; refresh() } }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch { settingsStore.requireLocation.collectLatest { requireLocation = it; refresh() } }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch { settingsStore.requireTitleKeyword.collectLatest { requireTitleKeyword = it; refresh() } }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch { settingsStore.titleKeyword.collectLatest { titleKeyword = it; refresh() } }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch { settingsStore.titleKeywordMatchMode.collectLatest { titleKeywordMatchMode = it; refresh() } }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch { settingsStore.titleKeywordCaseSensitive.collectLatest { titleKeywordCaseSensitive = it; refresh() } }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch { settingsStore.titleKeywordMatchAll.collectLatest { titleKeywordMatchAll = it; refresh() } }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch { settingsStore.titleKeywordExclude.collectLatest { titleKeywordExclude = it; refresh() } }
        onDispose { job.cancel() }
    }

    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val missingPermissions = !hasCalendarPermission || !hasPolicyAccess
    val isDndActive = dndSetByApp && activeWindow != null

    // Progress calculation
    var meetingProgress by remember { mutableFloatStateOf(0f) }
    var remainingMs by remember { mutableLongStateOf(0L) }
    if (isDndActive && activeWindow != null) {
        val duration = (activeWindow!!.end - activeWindow!!.begin).toFloat()
        val elapsed = (nowMs - activeWindow!!.begin).toFloat()
        meetingProgress = (elapsed / duration).coerceIn(0f, 1f)
        remainingMs = (activeWindow!!.end - nowMs).coerceAtLeast(0L)
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                withContext(Dispatchers.IO) {
                    EngineRunner.runEngine(context, Trigger.MANUAL)
                }
                refresh()
                delay(300L)
                isRefreshing = false
            }
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .pullRefresh(pullRefreshState)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status pill
                StatusPill(
                    isActive = isDndActive,
                    isPaused = !automationEnabled && !missingPermissions,
                    isError = missingPermissions
                )

                IconButton(onClick = { onOpenSettings(false) }) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            StatusBannerBlock(
                showTileHint = showTileHint,
                onTileHintDismissed = onTileHintDismissed,
                updateStatus = updateStatus,
                signatureStatus = signatureStatus,
                showStatusBanner = false,
                automationEnabled = automationEnabled,
                hasCalendarPermission = hasCalendarPermission,
                hasPolicyAccess = hasPolicyAccess,
                dndSetByApp = dndSetByApp,
                activeWindow = activeWindow,
                dndMode = dndMode,
                canScheduleExactAlarms = canScheduleExactAlarms,
                onboardingCompleted = onboardingCompleted,
                onOpenUpdates = onOpenUpdates,
                onOpenSettings = onOpenSettings,
                onOpenSetup = onOpenSetup,
                onOpenDndMode = onOpenDndMode,
                settingsStore = settingsStore,
                alarmScheduler = alarmScheduler,
                dndController = dndController
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Main status area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = activeWindow?.events?.firstOrNull() != null
                    ) {
                        val event = activeWindow?.events?.firstOrNull() ?: return@clickable
                        StatusScreenIntents.openCalendarEvent(context, event.eventId, event.begin)
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large status text
                AnimatedContent(
                    targetState = when {
                        missingPermissions -> "Setup Required"
                        isDndActive -> "Do Not Disturb"
                        !automationEnabled -> "Paused"
                        else -> "Standing By"
                    },
                    transitionSpec = {
                        fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                    },
                    label = "status"
                ) { status ->
                    Text(
                        text = status,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle
                AnimatedContent(
                    targetState = when {
                        missingPermissions -> "Grant permissions to continue"
                        isDndActive -> {
                            val title = activeWindow?.events?.firstOrNull()?.title?.takeIf { it.isNotBlank() }
                            title ?: "In a meeting"
                        }
                        !automationEnabled -> "Enable automation below"
                        nextInstance != null -> "Next: ${nextInstance!!.title.takeIf { it.isNotBlank() } ?: "Meeting"}"
                        else -> "No upcoming meetings"
                    },
                    transitionSpec = {
                        fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                    },
                    label = "subtitle"
                ) { subtitle ->
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Time display (for active meeting)
                AnimatedVisibility(
                    visible = isDndActive,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Large countdown
                        val hours = remainingMs / 3600000
                        val minutes = (remainingMs % 3600000) / 60000
                        val seconds = (remainingMs % 60000) / 1000

                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            if (hours > 0) {
                                TimeUnit(value = hours.toInt(), label = "h")
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            TimeUnit(value = minutes.toInt(), label = "m")
                            Spacer(modifier = Modifier.width(4.dp))
                            TimeUnit(value = seconds.toInt(), label = "s", isSeconds = true)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Progress bar
                        ProgressBar(
                            progress = meetingProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = TimeUtils.formatTime(context, activeWindow?.begin ?: 0L),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = TimeUtils.formatTime(context, activeWindow?.end ?: 0L),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Next meeting card (when not in DND)
                AnimatedVisibility(
                    visible = !isDndActive && nextInstance != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    nextInstance?.let { next ->
                        NextMeetingCard(
                            title = next.title.takeIf { it.isNotBlank() } ?: "Untitled Meeting",
                            startTime = TimeUtils.formatTime(context, next.begin),
                            endTime = TimeUtils.formatTime(context, next.end),
                            startsIn = TimeUtils.formatDuration((next.begin - nowMs).coerceAtLeast(0L)),
                            modifier = Modifier.padding(horizontal = 16.dp),
                            onClick = { StatusScreenIntents.openCalendarEvent(context, next.eventId, next.begin) }
                        )
                    }
                }

                // Empty state
                AnimatedVisibility(
                    visible = !isDndActive && nextInstance == null && !missingPermissions && automationEnabled,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Event,
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .alpha(0.4f),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Your schedule is clear",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom section - Automation toggle
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = surfaceColorAtElevation(2.dp),
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Automation",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (automationEnabled) "Active" else "Disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = automationEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                settingsStore.setAutomationEnabled(enabled)
                                if (enabled) {
                                    withContext(Dispatchers.IO) {
                                        EngineRunner.runEngine(context, Trigger.MANUAL)
                                    }
                                }
                                refresh()
                            }
                        },
                        thumbContent = {
                            Icon(
                                imageVector = if (automationEnabled) Icons.Rounded.Check else Icons.Rounded.Close,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                            checkedIconColor = MaterialTheme.colorScheme.onPrimary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            uncheckedIconColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }

            // DND Mode indicator
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = { onOpenSettings(false) })
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DND Mode",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (dndMode == DndMode.PRIORITY) "Priority Only" else "Total Silence",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (themeDebugMode == ThemeDebugMode.THEME_SELECTOR) {
                Spacer(modifier = Modifier.height(12.dp))
                ThemeSelectorBottomBar(onSelectTheme = onSelectTheme)
            }
            if (themeDebugMode == ThemeDebugMode.EVENT_CARD_COLORS) {
                Spacer(modifier = Modifier.height(12.dp))
                EventColorSelectorBottomBar()
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun StatusPill(
    isActive: Boolean,
    isPaused: Boolean,
    isError: Boolean
) {
    val backgroundColor = when {
        isError -> MaterialTheme.colorScheme.errorContainer
        isActive -> MaterialTheme.colorScheme.tertiaryContainer
        isPaused -> surfaceColorAtElevation(3.dp)
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    val contentColor = when {
        isError -> MaterialTheme.colorScheme.onErrorContainer
        isActive -> MaterialTheme.colorScheme.onTertiaryContainer
        isPaused -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    val text = when {
        isError -> "Setup needed"
        isActive -> "DND Active"
        isPaused -> "Paused"
        else -> "Ready"
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.FiberManualRecord,
                contentDescription = null,
                modifier = Modifier.size(8.dp),
                tint = contentColor
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

@Composable
private fun TimeUnit(
    value: Int,
    label: String,
    isSeconds: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = if (isSeconds) String.format("%02d", value) else value.toString(),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = if (isSeconds) 40.sp else 56.sp,
                fontWeight = FontWeight.Light
            ),
            color = if (isSeconds) {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }
}

@Composable
private fun ProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300),
        label = "progress"
    )

    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val progressColor = MaterialTheme.colorScheme.tertiary

    Canvas(
        modifier = modifier.height(6.dp)
    ) {
        val cornerRadius = CornerRadius(3.dp.toPx())

        // Track
        drawRoundRect(
            color = trackColor,
            cornerRadius = cornerRadius,
            size = size
        )

        // Progress
        drawRoundRect(
            color = progressColor,
            cornerRadius = cornerRadius,
            size = Size(size.width * animatedProgress, size.height)
        )
    }
}

@Composable
private fun NextMeetingCard(
    title: String,
    startTime: String,
    endTime: String,
    startsIn: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        color = surfaceColorAtElevation(1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "NEXT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "in $startsIn",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "$startTime – $endTime",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
