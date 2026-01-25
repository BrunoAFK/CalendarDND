package com.brunoafk.calendardnd.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.brunoafk.calendardnd.ui.components.OneUiHeader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.BuildConfig
import com.brunoafk.calendardnd.data.calendar.CalendarRepository
import com.brunoafk.calendardnd.data.dnd.DndController
import com.brunoafk.calendardnd.data.prefs.RuntimeStateStore
import com.brunoafk.calendardnd.data.prefs.SettingsStore
import com.brunoafk.calendardnd.domain.model.DndMode
import com.brunoafk.calendardnd.domain.model.EventInstance
import com.brunoafk.calendardnd.domain.model.MeetingWindow
import com.brunoafk.calendardnd.domain.model.KeywordMatchMode
import com.brunoafk.calendardnd.domain.model.ThemeDebugMode
import com.brunoafk.calendardnd.domain.model.Trigger
import com.brunoafk.calendardnd.domain.planning.MeetingWindowResolver
import com.brunoafk.calendardnd.system.alarms.AlarmScheduler
import com.brunoafk.calendardnd.system.alarms.EngineRunner
import com.brunoafk.calendardnd.system.update.ManualUpdateManager
import com.brunoafk.calendardnd.ui.components.AppError
import com.brunoafk.calendardnd.ui.components.DndModeBanner
import com.brunoafk.calendardnd.ui.components.InfoBanner
import com.brunoafk.calendardnd.ui.components.EmptyStates
import com.brunoafk.calendardnd.ui.components.ErrorCard
import com.brunoafk.calendardnd.ui.components.EventOverviewCard
import com.brunoafk.calendardnd.ui.components.EventOverviewState
import com.brunoafk.calendardnd.ui.components.EventSummary
import com.brunoafk.calendardnd.ui.components.PersistentWarningBanner
import com.brunoafk.calendardnd.ui.components.StatusBanner
import com.brunoafk.calendardnd.ui.components.StatusBannerKind
import com.brunoafk.calendardnd.ui.components.StatusBannerState
import com.brunoafk.calendardnd.ui.components.WarningBanner
import com.brunoafk.calendardnd.util.AnalyticsTracker
import com.brunoafk.calendardnd.util.PermissionUtils
import com.brunoafk.calendardnd.util.TimeUtils
import com.brunoafk.calendardnd.ui.theme.surfaceColorAtElevation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun StatusScreen(
    showTileHint: Boolean,
    onTileHintDismissed: () -> Unit,
    updateStatus: com.brunoafk.calendardnd.system.update.ManualUpdateManager.UpdateStatus?,
    signatureStatus: ManualUpdateManager.SignatureStatus,
    onOpenUpdates: () -> Unit,
    onOpenSettings: (Boolean) -> Unit,
    onOpenDebugLogs: () -> Unit,
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
    var onboardingCompleted by remember { mutableStateOf(false) }
    var dndMode by remember { mutableStateOf(DndMode.PRIORITY) }
    var dndStartOffsetMinutes by remember { mutableStateOf(0) }
    var oneTimeActionConfirmation by remember { mutableStateOf(true) }
    var canScheduleExactAlarms by remember {
        mutableStateOf(alarmScheduler.canScheduleExactAlarms())
    }
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    var warningDismissed by rememberSaveable { mutableStateOf(false) }
    var permissionErrorDismissed by remember { mutableStateOf(false) }
    var dndBannerDismissed by remember { mutableStateOf(false) }
    var refreshBannerDismissed by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var themeDebugMode by remember { mutableStateOf(ThemeDebugMode.OFF) }
    var tileHintVisible by remember { mutableStateOf(false) }
    var lastSeenUpdateVersion by remember { mutableStateOf("") }
    var userSuppressedUntilMs by remember { mutableStateOf(0L) }
    var userSuppressedFromMs by remember { mutableStateOf(0L) }
    var manualEventStartMs by remember { mutableStateOf(0L) }
    var manualEventEndMs by remember { mutableStateOf(0L) }
    var pendingOneTimeDialog by remember { mutableStateOf<OneTimeDialog?>(null) }

    LaunchedEffect(showTileHint) {
        tileHintVisible = showTileHint
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

    fun buildManualWindow(instance: EventInstance): Pair<Long, Long> {
        val offsetMs = dndStartOffsetMinutes * 60_000L
        val rawStart = instance.begin + offsetMs
        val start = if (rawStart >= instance.end) instance.begin else rawStart
        return start to instance.end
    }

    fun determineActiveAction(event: EventInstance?): OneTimeActionType? {
        if (event == null) return null
        val now = System.currentTimeMillis()
        val (startMs, endMs) = buildManualWindow(event)

        if (userSuppressedUntilMs > 0 && now < userSuppressedUntilMs) {
            if (userSuppressedFromMs <= startMs && userSuppressedUntilMs >= endMs) {
                return OneTimeActionType.SKIP
            }
        }

        if (manualEventEndMs > 0 && now < manualEventEndMs) {
            if (manualEventStartMs <= startMs && manualEventEndMs >= endMs) {
                return OneTimeActionType.ENABLE
            }
        }

        return null
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
            withContext(Dispatchers.IO) {
                EngineRunner.runEngine(context, Trigger.MANUAL)
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                AnalyticsTracker.logScreenView(context, "status")
                refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(Unit) {
        val job = scope.launch {
            settingsStore.automationEnabled.collectLatest { enabled ->
                automationEnabled = enabled
            }
        }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch {
            settingsStore.dndMode.collectLatest { value ->
                dndMode = value
                refresh()
            }
        }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch {
            settingsStore.dndStartOffsetMinutes.collectLatest { value ->
                dndStartOffsetMinutes = value
                refresh()
            }
        }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch {
            settingsStore.onboardingCompleted.collectLatest { value ->
                onboardingCompleted = value
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
        val job = scope.launch {
            settingsStore.oneTimeActionConfirmation.collectLatest { value ->
                oneTimeActionConfirmation = value
            }
        }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch {
            runtimeStateStore.dndSetByApp.collectLatest { value ->
                dndSetByApp = value
            }
        }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch {
            runtimeStateStore.userSuppressedUntilMs.collectLatest { userSuppressedUntilMs = it }
        }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch {
            runtimeStateStore.userSuppressedFromMs.collectLatest { userSuppressedFromMs = it }
        }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch {
            runtimeStateStore.manualEventStartMs.collectLatest { manualEventStartMs = it }
        }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch {
            runtimeStateStore.manualEventEndMs.collectLatest { manualEventEndMs = it }
        }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch {
            settingsStore.selectedCalendarIds.collectLatest { ids ->
                selectedCalendarIds = ids
                refresh()
            }
        }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch {
            settingsStore.busyOnly.collectLatest { value ->
                busyOnly = value
                refresh()
            }
        }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch {
            settingsStore.ignoreAllDay.collectLatest { value ->
                ignoreAllDay = value
                refresh()
            }
        }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch {
            settingsStore.skipRecurring.collectLatest { value ->
                skipRecurring = value
                refresh()
            }
        }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch {
            settingsStore.selectedDaysMask.collectLatest { value ->
                selectedDaysMask = value
                refresh()
            }
        }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch {
            settingsStore.selectedDaysEnabled.collectLatest { value ->
                selectedDaysEnabled = value
                refresh()
            }
        }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch {
            settingsStore.dndModeBannerDismissed.collectLatest { dismissed ->
                dndBannerDismissed = dismissed
            }
        }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch {
            settingsStore.refreshBannerDismissed.collectLatest { dismissed ->
                refreshBannerDismissed = dismissed
            }
        }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch {
            settingsStore.lastSeenUpdateVersion.collectLatest { value ->
                lastSeenUpdateVersion = value ?: ""
            }
        }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch {
            settingsStore.minEventMinutes.collectLatest { value ->
                minEventMinutes = value
                refresh()
            }
        }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch {
            settingsStore.requireLocation.collectLatest { value ->
                requireLocation = value
                refresh()
            }
        }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch {
            settingsStore.requireTitleKeyword.collectLatest { value ->
                requireTitleKeyword = value
                refresh()
            }
        }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch {
            settingsStore.titleKeyword.collectLatest { value ->
                titleKeyword = value
                refresh()
            }
        }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch {
            settingsStore.titleKeywordMatchMode.collectLatest { value ->
                titleKeywordMatchMode = value
                refresh()
            }
        }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch {
            settingsStore.titleKeywordCaseSensitive.collectLatest { value ->
                titleKeywordCaseSensitive = value
                refresh()
            }
        }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch {
            settingsStore.titleKeywordMatchAll.collectLatest { value ->
                titleKeywordMatchAll = value
                refresh()
            }
        }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch {
            settingsStore.titleKeywordExclude.collectLatest { value ->
                titleKeywordExclude = value
                refresh()
            }
        }
        onDispose { job.cancel() }
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
        val currentWindow = activeWindow
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
            dndSetByApp && currentWindow != null -> StatusBannerState(
                kind = StatusBannerKind.DndActive,
                statusText = stringResource(R.string.status_dnd_active),
                contextText = stringResource(
                    R.string.status_dnd_active_summary,
                    formatTimeUntil(currentWindow.end)
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
    val eventOverview = run {
        val currentSummary = activeWindow?.let { window ->
            val endRemaining = (window.end - nowMs).coerceAtLeast(0L)
            EventSummary(
                title = window.events.firstOrNull()?.title?.ifBlank { null }
                    ?: stringResource(R.string.untitled_meeting),
                timeRange = "${TimeUtils.formatTime(context, window.begin)} - " +
                    TimeUtils.formatTime(context, window.end),
                statusLine = stringResource(
                    R.string.event_overview_ends_in_format,
                    TimeUtils.formatDuration(endRemaining)
                )
            )
        }
        val nextSummary = nextInstance?.let { instance ->
            val startsIn = (instance.begin - nowMs).coerceAtLeast(0L)
            EventSummary(
                title = instance.title.ifBlank { stringResource(R.string.untitled_meeting) },
                timeRange = "${TimeUtils.formatTime(context, instance.begin)} - " +
                    TimeUtils.formatTime(context, instance.end),
                statusLine = stringResource(
                    R.string.event_overview_starts_in_format,
                    TimeUtils.formatDuration(startsIn)
                )
            )
        }
        EventOverviewState(current = currentSummary, next = nextSummary)
    }
    val nextActionData = nextInstance?.let { instance ->
        val (startMs, endMs) = buildManualWindow(instance)
        if (automationEnabled) {
            OneTimeAction.SkipEvent(instance, startMs, endMs)
        } else {
            OneTimeAction.EnableForEvent(instance, startMs, endMs)
        }
    }
    val activeActionType = determineActiveAction(nextInstance)
    val nextActionLabel = when (activeActionType) {
        OneTimeActionType.SKIP -> stringResource(R.string.action_status_dnd_skipped_tap)
        OneTimeActionType.ENABLE -> stringResource(R.string.action_status_dnd_enabled_tap)
        null -> if (automationEnabled) {
            stringResource(R.string.action_strip_tap_to_skip)
        } else {
            stringResource(R.string.action_strip_tap_to_enable)
        }
    }
    val nextActionEnabled = hasCalendarPermission && hasPolicyAccess

    LaunchedEffect(currentError) {
        permissionErrorDismissed = false
    }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            kotlinx.coroutines.delay(60_000L)
        }
    }

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

    Scaffold(
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onOpenSettings(false) },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(stringResource(R.string.settings))
                }

                if (themeDebugMode == ThemeDebugMode.THEME_SELECTOR) {
                    ThemeSelectorBottomBar(onSelectTheme = onSelectTheme)
                }
            }
        }
    ) { padding ->
        val showWarningBanner = !canScheduleExactAlarms && !warningDismissed
        val showDndBanner = automationEnabled && hasPolicyAccess && !dndBannerDismissed && !showWarningBanner
        val showRefreshBanner = !showWarningBanner && !showDndBanner && !refreshBannerDismissed

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
                    kotlinx.coroutines.delay(300L)
                    isRefreshing = false
                }
            }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .pullRefresh(pullRefreshState)
        ) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OneUiHeader(
                    title = stringResource(R.string.app_name),
                    showTopSpacer = false,
                    compact = true,
                    modifier = Modifier.padding(top = 0.dp)
                )
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
                    InfoBanner(
                        title = stringResource(R.string.tile_hint_title),
                        message = tileHintMessage,
                        onDismiss = {
                            tileHintVisible = false
                            onTileHintDismissed()
                        }
                    )
                }
                val updateVersion = updateStatus?.info?.versionName
                if (!updateVersion.isNullOrBlank() && updateVersion != lastSeenUpdateVersion) {
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
                } else if (showDndBanner) {
                    val dndModeLabel = stringResource(
                        if (dndMode == DndMode.PRIORITY) {
                            R.string.priority_mode_title
                        } else {
                            R.string.total_silence_title
                        }
                    )
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
                } else if (showRefreshBanner) {
                    InfoBanner(
                        title = stringResource(R.string.refresh_banner_title),
                        message = stringResource(R.string.refresh_banner_message),
                        onDismiss = { scope.launch { settingsStore.setRefreshBannerDismissed(true) } }
                    )
                }
                StatusBanner(
                    state = statusBannerState,
                    modifier = Modifier.padding(top = 4.dp),
                    onClick = if (!automationEnabled) {
                        { onOpenSettings(true) }
                    } else {
                        null
                    }
                )
                if (showWarningBanner) {
                    WarningBanner(
                        message = stringResource(R.string.warning_degraded_mode),
                        actionLabel = stringResource(R.string.fix_now),
                        onAction = { alarmScheduler.openExactAlarmSettings() },
                        onDismiss = { warningDismissed = true }
                    )
                }
                if (currentError != null && !permissionErrorDismissed) {
                    ErrorCard(
                        error = currentError,
                        onPrimaryAction = {
                            when (currentError) {
                                AppError.CalendarPermissionDenied -> StatusScreenIntents.openAppSettings(context)
                                AppError.DndPermissionDenied -> dndController.openPolicyAccessSettings()
                                AppError.CalendarQueryFailed -> refresh()
                                AppError.DndChangeFailed -> refresh()
                                AppError.NoCalendarsFound -> StatusScreenIntents.openCalendarApp(context)
                                AppError.NetworkError -> refresh()
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

                if (eventOverview.current != null || eventOverview.next != null) {
                    EventOverviewCard(
                        state = eventOverview,
                        onCurrentClick = activeWindow?.events?.firstOrNull()?.let { current ->
                            { StatusScreenIntents.openCalendarEvent(context, current.eventId, current.begin) }
                        },
                        onNextClick = nextInstance?.let { next ->
                            { StatusScreenIntents.openCalendarEvent(context, next.eventId, next.begin) }
                        },
                        highlightNext = activeActionType != null,
                        actionStripActive = activeActionType != null,
                        nextActionLabel = nextActionData?.let { nextActionLabel },
                        nextActionEnabled = nextActionEnabled,
                        onNextAction = if (nextInstance != null) {
                            {
                                if (activeActionType == null) {
                                    nextActionData?.let { action ->
                                        if (oneTimeActionConfirmation) {
                                            pendingOneTimeDialog = OneTimeDialog.Set(action)
                                        } else {
                                            applyOneTimeAction(action)
                                        }
                                    }
                                } else {
                                    if (oneTimeActionConfirmation) {
                                        pendingOneTimeDialog = OneTimeDialog.Clear(activeActionType)
                                    } else {
                                        clearOneTimeAction()
                                    }
                                }
                            }
                        } else {
                            null
                        }
                    )
                } else {
                    EmptyStates.NoMeetings(onOpenCalendar = { StatusScreenIntents.openCalendarApp(context) })
                }

            }

            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )
        }
    }
}
