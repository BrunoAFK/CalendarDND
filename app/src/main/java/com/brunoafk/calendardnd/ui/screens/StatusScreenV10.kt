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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.data.calendar.CalendarRepository
import com.brunoafk.calendardnd.data.dnd.DndController
import com.brunoafk.calendardnd.data.prefs.RuntimeStateStore
import com.brunoafk.calendardnd.data.prefs.SettingsStore
import com.brunoafk.calendardnd.domain.model.DndMode
import com.brunoafk.calendardnd.domain.model.EventInstance
import com.brunoafk.calendardnd.domain.model.KeywordMatchMode
import com.brunoafk.calendardnd.domain.model.MeetingWindow
import com.brunoafk.calendardnd.domain.model.Trigger
import com.brunoafk.calendardnd.domain.model.ThemeDebugMode
import com.brunoafk.calendardnd.domain.planning.MeetingWindowResolver
import com.brunoafk.calendardnd.system.alarms.AlarmScheduler
import com.brunoafk.calendardnd.system.alarms.EngineRunner
import com.brunoafk.calendardnd.system.update.ManualUpdateManager
import com.brunoafk.calendardnd.util.PermissionUtils
import com.brunoafk.calendardnd.util.TimeUtils
import com.brunoafk.calendardnd.ui.theme.LocalIsDarkTheme
import com.brunoafk.calendardnd.ui.theme.surfaceColorAtElevation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * V10 with:
 * - App name header and main status area
 * - Next event shown alongside active meeting
 * - Gradient background based on state
 * - Bottom row split into Event Filters and Settings
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun StatusScreenV10(
    onNavigateBack: () -> Unit,
    showTileHint: Boolean,
    onTileHintDismissed: () -> Unit,
    updateStatus: ManualUpdateManager.UpdateStatus?,
    signatureStatus: ManualUpdateManager.SignatureStatus,
    onOpenUpdates: () -> Unit,
    onOpenSettings: (Boolean) -> Unit,
    onOpenSetup: () -> Unit,
    onOpenDndMode: () -> Unit,
    onOpenFilters: () -> Unit = { onOpenSettings(false) }, // Default to settings if not provided
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
    var dndStartOffsetMinutes by remember { mutableStateOf(0) }
    var canScheduleExactAlarms by remember { mutableStateOf(alarmScheduler.canScheduleExactAlarms()) }
    var onboardingCompleted by remember { mutableStateOf(false) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var themeDebugMode by remember { mutableStateOf(ThemeDebugMode.OFF) }
    var userSuppressedUntilMs by remember { mutableLongStateOf(0L) }
    var userSuppressedFromMs by remember { mutableLongStateOf(0L) }
    var manualEventStartMs by remember { mutableLongStateOf(0L) }
    var manualEventEndMs by remember { mutableLongStateOf(0L) }
    var oneTimeActionConfirmation by remember { mutableStateOf(true) }
    var pendingOneTimeDialog by remember { mutableStateOf<OneTimeDialog?>(null) }

    fun buildManualWindow(instance: EventInstance): Pair<Long, Long> {
        val offsetMs = dndStartOffsetMinutes * 60_000L
        val rawStart = instance.begin + offsetMs
        val start = if (rawStart >= instance.end) instance.begin else rawStart
        return start to instance.end
    }

    fun determineActiveAction(event: EventInstance?): OneTimeActionType? {
        if (event == null) return null
        val now = System.currentTimeMillis()

        // Check if skip is active for this event (suppression covers the event time)
        if (userSuppressedUntilMs > 0 && now < userSuppressedUntilMs) {
            val (startMs, endMs) = buildManualWindow(event)
            if (userSuppressedFromMs <= startMs && userSuppressedUntilMs >= endMs) {
                return OneTimeActionType.SKIP
            }
        }

        // Check if enable is active for this event (manual event covers the event time)
        if (manualEventEndMs > 0 && now < manualEventEndMs) {
            val (startMs, endMs) = buildManualWindow(event)
            if (manualEventStartMs <= startMs && manualEventEndMs >= endMs) {
                return OneTimeActionType.ENABLE
            }
        }

        return null
    }

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

    fun applyOneTimeAction(action: OneTimeAction) {
        scope.launch {
            when (action) {
                is OneTimeAction.EnableForEvent -> {
                    runtimeStateStore.setManualEventStartMs(action.startMs)
                    runtimeStateStore.setManualEventEndMs(action.endMs)
                    runtimeStateStore.setUserSuppressedFromMs(0L)
                    runtimeStateStore.setUserSuppressedUntilMs(0L)
                }
                is OneTimeAction.SkipEvent -> {
                    runtimeStateStore.setUserSuppressedFromMs(action.startMs)
                    runtimeStateStore.setUserSuppressedUntilMs(action.endMs)
                    runtimeStateStore.setSkippedEventBeginMs(action.event.begin)
                    runtimeStateStore.setNotifiedNewEventBeforeSkip(false)
                }
            }
            withContext(Dispatchers.IO) {
                EngineRunner.runEngine(context, Trigger.MANUAL)
            }
        }
    }

    fun clearOneTimeAction() {
        scope.launch {
            runtimeStateStore.setManualEventStartMs(0L)
            runtimeStateStore.setManualEventEndMs(0L)
            runtimeStateStore.setUserSuppressedFromMs(0L)
            runtimeStateStore.setUserSuppressedUntilMs(0L)
            runtimeStateStore.setSkippedEventBeginMs(0L)
            runtimeStateStore.setNotifiedNewEventBeforeSkip(false)
            withContext(Dispatchers.IO) {
                EngineRunner.runEngine(context, Trigger.MANUAL)
            }
        }
    }

    // Lifecycle & data collection effects
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
        val job = scope.launch { settingsStore.dndStartOffsetMinutes.collectLatest { dndStartOffsetMinutes = it; refresh() } }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch { settingsStore.oneTimeActionConfirmation.collectLatest { oneTimeActionConfirmation = it } }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch { settingsStore.onboardingCompleted.collectLatest { onboardingCompleted = it } }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch { settingsStore.themeDebugMode.collectLatest { themeDebugMode = it } }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch { runtimeStateStore.dndSetByApp.collectLatest { dndSetByApp = it } }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch { runtimeStateStore.userSuppressedUntilMs.collectLatest { userSuppressedUntilMs = it } }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch { runtimeStateStore.userSuppressedFromMs.collectLatest { userSuppressedFromMs = it } }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch { runtimeStateStore.manualEventStartMs.collectLatest { manualEventStartMs = it } }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch { runtimeStateStore.manualEventEndMs.collectLatest { manualEventEndMs = it } }
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
    val nextActionData = nextInstance?.let { instance ->
        val (startMs, endMs) = buildManualWindow(instance)
        if (automationEnabled) {
            OneTimeAction.SkipEvent(instance, startMs, endMs)
        } else {
            OneTimeAction.EnableForEvent(instance, startMs, endMs)
        }
    }
    val nextActionLabel = if (automationEnabled) {
        stringResource(R.string.next_event_skip_dnd)
    } else {
        stringResource(R.string.next_event_enable_dnd)
    }
    val nextActionEnabled = hasCalendarPermission && hasPolicyAccess

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
                settingsStore.setRefreshBannerDismissed(true)
                withContext(Dispatchers.IO) {
                    EngineRunner.runEngine(context, Trigger.MANUAL)
                }
                refresh()
                delay(300L)
                isRefreshing = false
            }
        }
    )

    val isDarkTheme = LocalIsDarkTheme.current
    // Background gradient colors based on state, tuned for contrast in light/dark
    val gradientColors = when {
        missingPermissions -> listOf(
            if (isDarkTheme) Color(0xFFFF4444) else Color(0xFFFF3535),
            MaterialTheme.colorScheme.surface
        )
        isDndActive -> listOf(
            if (isDarkTheme) Color(0xFFFF6E40) else Color(0xFFFF5722),
            MaterialTheme.colorScheme.surface
        )
        !automationEnabled -> listOf(
            if (isDarkTheme) Color(0xFF3A3D40) else Color(0xFFE0E0E0),
            MaterialTheme.colorScheme.surface
        )
        else -> listOf(
            if (isDarkTheme) Color(0xFF5B6B8C) else Color(0xFFC5CAE9),
            MaterialTheme.colorScheme.surface
        )
    }
    val gradientStartColor = gradientColors.first()
    val gradientEndColor = gradientColors.last()

    pendingOneTimeDialog?.let { dialog ->
        val (titleRes, bodyRes, confirmRes) = when (dialog) {
            is OneTimeDialog.Set -> when (dialog.action) {
                is OneTimeAction.EnableForEvent -> Triple(
                    R.string.next_event_enable_confirm_title,
                    R.string.next_event_enable_confirm_body,
                    R.string.next_event_enable_confirm_action
                )
                is OneTimeAction.SkipEvent -> Triple(
                    R.string.next_event_skip_confirm_title,
                    R.string.next_event_skip_confirm_body,
                    R.string.next_event_skip_confirm_action
                )
            }
            is OneTimeDialog.Clear -> when (dialog.activeType) {
                OneTimeActionType.ENABLE -> Triple(
                    R.string.next_event_clear_enable_title,
                    R.string.next_event_clear_enable_body,
                    R.string.next_event_clear_enable_action
                )
                OneTimeActionType.SKIP -> Triple(
                    R.string.next_event_clear_skip_title,
                    R.string.next_event_clear_skip_body,
                    R.string.next_event_clear_skip_action
                )
            }
        }

        AlertDialog(
            onDismissRequest = { pendingOneTimeDialog = null },
            title = { Text(text = stringResource(titleRes)) },
            text = { Text(text = stringResource(bodyRes)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingOneTimeDialog = null
                        when (dialog) {
                            is OneTimeDialog.Set -> applyOneTimeAction(dialog.action)
                            is OneTimeDialog.Clear -> clearOneTimeAction()
                        }
                    }
                ) {
                    Text(text = stringResource(confirmRes))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingOneTimeDialog = null }) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = gradientColors,
                    startY = 0f,
                    endY = 800f
                )
            )
            .pullRefresh(pullRefreshState)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Top bar - App name
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            StatusBannerBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
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

            Spacer(modifier = Modifier.height(24.dp))

            // Main status area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = activeWindow?.events?.firstOrNull() != null) {
                        val event = activeWindow?.events?.firstOrNull() ?: return@clickable
                        StatusScreenIntents.openCalendarEvent(context, event.eventId, event.begin)
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large status text
                AnimatedContent(
                    targetState = when {
                        missingPermissions -> stringResource(R.string.status_setup_required)
                        isDndActive -> stringResource(R.string.status_dnd_active)
                        !automationEnabled -> stringResource(R.string.status_automation_paused)
                        else -> stringResource(R.string.status_automation_ready)
                    },
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
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

                // Subtitle - context based on state
                val dndModeLabel = if (dndMode == DndMode.PRIORITY) {
                    stringResource(R.string.priority_mode_title)
                } else {
                    stringResource(R.string.total_silence_title)
                }
                AnimatedContent(
                    targetState = when {
                        missingPermissions -> stringResource(R.string.status_missing_permissions_summary)
                        isDndActive -> {
                            val meetingTitle = activeWindow?.events?.firstOrNull()?.title?.takeIf { it.isNotBlank() }
                            if (meetingTitle != null) {
                                stringResource(R.string.status_in_meeting, meetingTitle)
                            } else {
                                stringResource(R.string.status_dnd_active_summary, TimeUtils.formatDuration(remainingMs))
                            }
                        }
                        !automationEnabled -> stringResource(R.string.status_automation_paused_summary)
                        else -> stringResource(R.string.status_automation_ready_summary, dndModeLabel)
                    },
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                    label = "subtitle"
                ) { subtitle ->
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Time display for active meeting
                AnimatedVisibility(
                    visible = isDndActive,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val hours = remainingMs / 3600000
                        val minutes = (remainingMs % 3600000) / 60000
                        val seconds = (remainingMs % 60000) / 1000

                        Row(verticalAlignment = Alignment.Bottom) {
                            if (hours > 0) {
                                TimeUnitV10(
                                    value = hours.toInt(),
                                    label = stringResource(R.string.time_unit_hours_short)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            TimeUnitV10(
                                value = minutes.toInt(),
                                label = stringResource(R.string.time_unit_minutes_short)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            TimeUnitV10(
                                value = seconds.toInt(),
                                label = stringResource(R.string.time_unit_seconds_short),
                                isSeconds = true
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        ProgressBarV10(
                            progress = meetingProgress,
                            progressColor = gradientStartColor,
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
            }

            // Next meeting card when current is active (outside clickable area)
            AnimatedVisibility(
                visible = isDndActive && nextInstance != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    nextInstance?.let { next ->
                        val compactActiveActionType = determineActiveAction(next)
                        val compactActionData = run {
                            val (startMs, endMs) = buildManualWindow(next)
                            if (automationEnabled) {
                                OneTimeAction.SkipEvent(next, startMs, endMs)
                            } else {
                                OneTimeAction.EnableForEvent(next, startMs, endMs)
                            }
                        }
                        UpNextCompactCard(
                            title = next.title.takeIf { it.isNotBlank() }
                                ?: stringResource(R.string.untitled_meeting),
                            time = TimeUtils.formatTime(context, next.begin),
                            startsIn = TimeUtils.formatDuration((next.begin - nowMs).coerceAtLeast(0L)),
                            activeActionType = compactActiveActionType,
                            automationEnabled = automationEnabled,
                            actionEnabled = nextActionEnabled,
                            onCardClick = {
                                StatusScreenIntents.openCalendarEvent(
                                    context,
                                    next.eventId,
                                    next.begin
                                )
                            },
                            onSetAction = {
                                if (oneTimeActionConfirmation) {
                                    pendingOneTimeDialog = OneTimeDialog.Set(compactActionData)
                                } else {
                                    applyOneTimeAction(compactActionData)
                                }
                            },
                            onClearAction = {
                                compactActiveActionType?.let { type ->
                                    if (oneTimeActionConfirmation) {
                                        pendingOneTimeDialog = OneTimeDialog.Clear(type)
                                    } else {
                                        clearOneTimeAction()
                                    }
                                }
                            }
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
                        val activeActionType = determineActiveAction(next)
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        NextMeetingCardV10(
                            title = next.title.takeIf { it.isNotBlank() } ?: stringResource(R.string.untitled_meeting),
                            startTime = TimeUtils.formatTime(context, next.begin),
                            endTime = TimeUtils.formatTime(context, next.end),
                            startsIn = TimeUtils.formatDuration((next.begin - nowMs).coerceAtLeast(0L)),
                            highlightCard = activeActionType != null,
                            hasActionStrip = true,
                            onClick = { StatusScreenIntents.openCalendarEvent(context, next.eventId, next.begin) }
                        )
                        ActionStrip(
                            activeActionType = activeActionType,
                            automationEnabled = automationEnabled,
                            enabled = nextActionEnabled,
                            onSetAction = {
                                nextActionData?.let { action ->
                                    if (oneTimeActionConfirmation) {
                                        pendingOneTimeDialog = OneTimeDialog.Set(action)
                                    } else {
                                        applyOneTimeAction(action)
                                    }
                                }
                            },
                            onClearAction = {
                                activeActionType?.let { type ->
                                    if (oneTimeActionConfirmation) {
                                        pendingOneTimeDialog = OneTimeDialog.Clear(type)
                                    } else {
                                        clearOneTimeAction()
                                    }
                                }
                            }
                        )
                    }
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Event,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = stringResource(R.string.no_upcoming_meetings),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.no_meetings_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { StatusScreenIntents.openCalendarApp(context) }) {
                        Text(text = stringResource(R.string.open_calendar))
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom section - Automation toggle
            val calendarSummary = if (selectedCalendarIds.isEmpty()) {
                stringResource(R.string.automation_summary_calendars_all)
            } else {
                stringResource(R.string.automation_summary_calendars_selected, selectedCalendarIds.size)
            }
            val activeFilters = listOf(
                busyOnly,
                ignoreAllDay,
                skipRecurring,
                selectedDaysEnabled,
                requireLocation,
                requireTitleKeyword
            ).count { it }
            val filtersSummary = if (activeFilters == 0) {
                stringResource(R.string.automation_summary_filters_none)
            } else {
                stringResource(R.string.automation_summary_filters_count, activeFilters)
            }
            val automationSummary = stringResource(
                R.string.automation_summary_format,
                calendarSummary,
                filtersSummary
            )
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
                            text = stringResource(R.string.automation),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = automationSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = automationEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                settingsStore.setAutomationEnabled(enabled)
                                // Always run engine to apply changes immediately
                                withContext(Dispatchers.IO) {
                                    EngineRunner.runEngine(context, Trigger.MANUAL)
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

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom action row - Event Filters and Settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Event Filters button
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenFilters),
                    shape = RoundedCornerShape(12.dp),
                    color = surfaceColorAtElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.event_filters),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Settings button
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = { onOpenSettings(false) }),
                    shape = RoundedCornerShape(12.dp),
                    color = surfaceColorAtElevation(1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.settings),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (themeDebugMode == ThemeDebugMode.GRADIENT_OVERVIEW) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = surfaceColorAtElevation(1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.theme_debug_mode_gradient),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Start: ${gradientStartColor.toHexString()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "End: ${gradientEndColor.toHexString()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Surface: ${MaterialTheme.colorScheme.surface.toHexString()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (themeDebugMode == ThemeDebugMode.THEME_SELECTOR) {
                ThemeSelectorBottomBar(
                    onSelectTheme = onSelectTheme,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun TimeUnitV10(
    value: Int,
    label: String,
    isSeconds: Boolean = false
) {
    Row(verticalAlignment = Alignment.Bottom) {
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
private fun ProgressBarV10(
    progress: Float,
    progressColor: Color,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300),
        label = "progress"
    )

    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

    Canvas(modifier = modifier.height(6.dp)) {
        val cornerRadius = CornerRadius(3.dp.toPx())

        drawRoundRect(
            color = trackColor,
            cornerRadius = cornerRadius,
            size = size
        )

        drawRoundRect(
            color = progressColor,
            cornerRadius = cornerRadius,
            size = Size(size.width * animatedProgress, size.height)
        )
    }
}

@Composable
private fun UpNextCompactCard(
    title: String,
    time: String,
    startsIn: String,
    activeActionType: OneTimeActionType?,
    automationEnabled: Boolean,
    actionEnabled: Boolean,
    onCardClick: () -> Unit,
    onSetAction: () -> Unit,
    onClearAction: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isDarkTheme = LocalIsDarkTheme.current

    // Yellow/gold color when action is active (same as NextMeetingCardV10)
    val highlightColor = if (isDarkTheme) Color(0xFFB38B00) else Color(0xFFFFC107)
    val isHighlighted = activeActionType != null

    val cardBackgroundColor = if (isHighlighted) {
        highlightColor.copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f)
    }

    val primaryTextColor = if (isHighlighted) Color.Black else MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = if (isHighlighted) Color.Black.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
    val iconTint = if (isHighlighted) Color.Black else MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .animateContentSize()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCardClick),
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (expanded) 0.dp else 12.dp,
                bottomEnd = if (expanded) 0.dp else 12.dp
            ),
            color = cardBackgroundColor
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Main content area
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = iconTint
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.next_meeting),
                            style = MaterialTheme.typography.labelSmall,
                            color = iconTint
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = primaryTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = stringResource(R.string.time_until_format, startsIn),
                        style = MaterialTheme.typography.labelMedium,
                        color = secondaryTextColor
                    )
                }

                // Expand/collapse arrow button - intercepts its own clicks
                Surface(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .clickable(onClick = { expanded = !expanded }),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isHighlighted) {
                        Color.Black.copy(alpha = 0.1f)
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    }
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        tint = iconTint
                    )
                }
            }
        }

        // Expandable action strip
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            ActionStrip(
                activeActionType = activeActionType,
                automationEnabled = automationEnabled,
                enabled = actionEnabled,
                onSetAction = onSetAction,
                onClearAction = onClearAction,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun NextMeetingCardV10(
    title: String,
    startTime: String,
    endTime: String,
    startsIn: String,
    highlightCard: Boolean,
    hasActionStrip: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val isDarkTheme = LocalIsDarkTheme.current
    val highlightColor = if (isDarkTheme) Color(0xFFB38B00) else Color(0xFFFFC107)
    val primaryText = if (highlightCard) Color.Black else MaterialTheme.colorScheme.onSurface
    val secondaryText = if (highlightCard) Color.Black.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
    val chipBackground = if (highlightCard) Color.Black.copy(alpha = 0.08f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    val chipText = if (highlightCard) Color.Black else MaterialTheme.colorScheme.primary

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomStart = if (hasActionStrip) 0.dp else 16.dp,
            bottomEnd = if (hasActionStrip) 0.dp else 16.dp
        ),
        color = if (highlightCard) highlightColor else surfaceColorAtElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.next_meeting).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (highlightCard) Color.Black else MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = primaryText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = chipBackground
                ) {
                    Text(
                        text = stringResource(R.string.time_until_format, startsIn),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = chipText
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "$startTime – $endTime",
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryText
            )
        }
    }
}

private fun Color.toHexString(): String {
    val argb = toArgb()
    return String.format(
        "#%02X%02X%02X",
        (argb shr 16) and 0xFF,
        (argb shr 8) and 0xFF,
        argb and 0xFF
    )
}

enum class OneTimeActionType {
    SKIP,
    ENABLE
}

/**
 * Action strip shown below the next meeting card.
 * - When no action is set: shows prompt to skip/enable DND (tap to set)
 * - When action is set: shows status (tap to clear)
 */
@Composable
private fun ActionStrip(
    activeActionType: OneTimeActionType?,
    automationEnabled: Boolean,
    enabled: Boolean,
    onSetAction: () -> Unit,
    onClearAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = LocalIsDarkTheme.current
    val isActive = activeActionType != null

    // Yellow when no action is set, gray when action is active
    val backgroundColor = if (isActive) {
        if (isDarkTheme) Color(0xFF3A3A3A) else Color(0xFFE8E8E8)
    } else {
        if (isDarkTheme) Color(0xFFB38B00) else Color(0xFFFFC107)
    }

    val contentColor = if (isActive) {
        if (isDarkTheme) Color.White.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.8f)
    } else {
        Color.Black.copy(alpha = 0.9f)
    }

    val onClick = when {
        !enabled -> null
        activeActionType != null -> onClearAction
        else -> onSetAction
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(
            topStart = 0.dp,
            topEnd = 0.dp,
            bottomStart = 16.dp,
            bottomEnd = 16.dp
        ),
        color = backgroundColor.copy(alpha = if (activeActionType != null) 0.85f else 1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = 12.dp
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (activeActionType != null) Icons.Rounded.Check else Icons.Rounded.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = contentColor.copy(alpha = if (enabled || activeActionType != null) 0.8f else 0.4f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(
                        when {
                            activeActionType == OneTimeActionType.SKIP -> R.string.action_status_dnd_skipped_tap
                            activeActionType == OneTimeActionType.ENABLE -> R.string.action_status_dnd_enabled_tap
                            automationEnabled -> R.string.action_strip_tap_to_skip
                            else -> R.string.action_strip_tap_to_enable
                        }
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor.copy(alpha = if (enabled || activeActionType != null) 1f else 0.5f)
                )
            }
        }
    }
}
