package com.brunoafk.calendardnd.ui.screens

import android.util.Log
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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.pointerInput
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
import com.brunoafk.calendardnd.domain.model.EventHighlightPreset
import com.brunoafk.calendardnd.domain.model.KeywordMatchMode
import com.brunoafk.calendardnd.domain.model.MeetingWindow
import com.brunoafk.calendardnd.domain.model.Trigger
import com.brunoafk.calendardnd.domain.model.OneTimeAction
import com.brunoafk.calendardnd.domain.model.OneTimeActionType
import com.brunoafk.calendardnd.domain.model.SkippedEventState
import com.brunoafk.calendardnd.domain.util.SkipUtils
import com.brunoafk.calendardnd.domain.model.ThemeDebugMode
import com.brunoafk.calendardnd.domain.planning.MeetingWindowResolver
import com.brunoafk.calendardnd.system.alarms.AlarmScheduler
import com.brunoafk.calendardnd.system.alarms.EngineRunner
import com.brunoafk.calendardnd.system.update.ManualUpdateManager
import com.brunoafk.calendardnd.util.PermissionUtils
import com.brunoafk.calendardnd.util.TimeUtils
import com.brunoafk.calendardnd.ui.theme.LocalIsDarkTheme
import com.brunoafk.calendardnd.ui.theme.eventHighlightColors
import com.brunoafk.calendardnd.ui.theme.surfaceColorAtElevation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

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
    var activeWindowRaw by remember { mutableStateOf<MeetingWindow?>(null) }
    var nextInstance by remember { mutableStateOf<EventInstance?>(null) }
    var nextOverlappingEvents by remember { mutableStateOf<List<EventInstance>>(emptyList()) }
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
    var eventHighlightPreset by remember { mutableStateOf(EventHighlightPreset.PRESET_1) }
    var eventHighlightBarHidden by remember { mutableStateOf(false) }
    var userSuppressedUntilMs by remember { mutableLongStateOf(0L) }
    var userSuppressedFromMs by remember { mutableLongStateOf(0L) }
    var manualEventStartMs by remember { mutableLongStateOf(0L) }
    var manualEventEndMs by remember { mutableLongStateOf(0L) }
    var skippedEventId by remember { mutableLongStateOf(0L) }
    var skippedEventBeginMs by remember { mutableLongStateOf(0L) }
    var skippedEventEndMs by remember { mutableLongStateOf(0L) }
    var oneTimeActionConfirmation by remember { mutableStateOf(true) }
    var pendingOneTimeDialog by remember { mutableStateOf<OneTimeDialog?>(null) }
    var showActiveEventsDialog by remember { mutableStateOf(false) }
    var showNextEventsDialog by remember { mutableStateOf(false) }

    // --- Debug tracking ---
    var statusDebugPanelEnabled by remember { mutableStateOf(false) }
    var debugRefreshCount by remember { mutableStateOf(0) }
    var debugLastRefreshMs by remember { mutableLongStateOf(0L) }
    var debugLastRefreshSource by remember { mutableStateOf("none") }
    var debugSmartTimerDelayMs by remember { mutableLongStateOf(0L) }
    var debugSmartTimerTarget by remember { mutableLongStateOf(0L) }
    var debugShowPanel by remember { mutableStateOf(false) }
    val isDarkThemeForHighlight = LocalIsDarkTheme.current
    val highlightPalette = eventHighlightColors(eventHighlightPreset)
    val eventHighlightColor = if (isDarkThemeForHighlight) highlightPalette.dark else highlightPalette.light

    fun buildManualWindow(instance: EventInstance): Pair<Long, Long> {
        val offsetMs = dndStartOffsetMinutes * 60_000L
        val rawStart = instance.begin + offsetMs
        val start = if (rawStart >= instance.end) instance.begin else rawStart
        return start to instance.end
    }

    fun isSkippedEvent(event: EventInstance?): Boolean {
        return SkipUtils.isSkippedEvent(event, skippedEventId, skippedEventBeginMs, skippedEventEndMs)
    }

    fun determineActiveAction(event: EventInstance?): OneTimeActionType? {
        if (event == null) return null
        val now = System.currentTimeMillis()

        // Check if skip is active for this event (suppression covers the event time)
        if (isSkippedEvent(event)) {
            return OneTimeActionType.SKIP
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

    fun refresh(source: String = "unknown") {
        scope.launch {
            Log.d("StatusV10", "=== refresh() STARTED === source=$source")
            debugRefreshCount++
            debugLastRefreshMs = System.currentTimeMillis()
            debugLastRefreshSource = source
            hasCalendarPermission = PermissionUtils.hasCalendarPermission(context)
            hasPolicyAccess = dndController.hasPolicyAccess()
            canScheduleExactAlarms = alarmScheduler.canScheduleExactAlarms()

            // Read fresh runtime state to ensure UI is synchronized after engine runs.
            // This fixes the race condition where DataStore flows haven't emitted yet.
            val runtimeSnapshot = withContext(Dispatchers.IO) {
                runtimeStateStore.getSnapshot()
            }
            dndSetByApp = runtimeSnapshot.dndSetByApp
            userSuppressedUntilMs = runtimeSnapshot.userSuppressedUntilMs
            userSuppressedFromMs = runtimeSnapshot.userSuppressedFromMs
            manualEventStartMs = runtimeSnapshot.manualEventStartMs
            manualEventEndMs = runtimeSnapshot.manualEventEndMs
            skippedEventId = runtimeSnapshot.skippedEventId
            skippedEventBeginMs = runtimeSnapshot.skippedEventBeginMs
            skippedEventEndMs = runtimeSnapshot.skippedEventEndMs

            Log.d("StatusV10", "refresh() state: dndSetByApp=$dndSetByApp, skipId=$skippedEventId, skipBegin=$skippedEventBeginMs, skipEnd=$skippedEventEndMs")

            if (!hasCalendarPermission) {
                activeWindow = null
                activeWindowRaw = null
                nextInstance = null
                Log.d("StatusV10", "refresh() ABORTED - no calendar permission")
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
                Log.d("StatusV10", "refresh() activeInstances=${activeInstances.size}: ${activeInstances.map { "${it.title}(id=${it.eventId},${it.begin}-${it.end})" }}")

                val windowRaw = MeetingWindowResolver.findActiveWindow(activeInstances, now)
                Log.d("StatusV10", "refresh() windowRaw: events=${windowRaw?.events?.size}, begin=${windowRaw?.begin}, end=${windowRaw?.end}")

                val filteredInstances = activeInstances.filterNot { inst ->
                    val skipped = isSkippedEvent(inst)
                    if (skipped) Log.d("StatusV10", "refresh() FILTERING OUT: ${inst.title}(id=${inst.eventId})")
                    skipped
                }
                Log.d("StatusV10", "refresh() filteredInstances=${filteredInstances.size}: ${filteredInstances.map { it.title }}")

                val window = MeetingWindowResolver.findActiveWindow(filteredInstances, now)
                Log.d("StatusV10", "refresh() activeWindow: events=${window?.events?.size}, begin=${window?.begin}, end=${window?.end}")

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
                Log.d("StatusV10", "refresh() nextInstance: ${next?.title}(begin=${next?.begin})")

                val nextOverlap = if (next != null) {
                    calendarRepository.getInstancesInRange(
                        beginMs = next.begin,
                        endMs = next.end,
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
                } else {
                    emptyList()
                }

                data class RefreshResult(
                    val window: MeetingWindow?,
                    val windowRaw: MeetingWindow?,
                    val next: EventInstance?,
                    val nextOverlap: List<EventInstance>
                )
                RefreshResult(window, windowRaw, next, nextOverlap)
            }
            activeWindow = snapshot.window
            activeWindowRaw = snapshot.windowRaw
            nextInstance = snapshot.next
            nextOverlappingEvents = snapshot.nextOverlap
            Log.d("StatusV10", "=== refresh() DONE === activeWindow=${activeWindow != null}, activeWindowRaw=${activeWindowRaw != null}, nextInstance=${nextInstance != null}")
        }
    }

    fun applyOneTimeAction(action: OneTimeAction) {
        scope.launch {
            Log.d("StatusV10", ">>> applyOneTimeAction: $action")
            when (action) {
                is OneTimeAction.EnableForEvent -> {
                    runtimeStateStore.setManualEvent(action.startMs, action.endMs)
                    runtimeStateStore.clearUserSuppression()
                    runtimeStateStore.clearSkippedEvent()
                }
                is OneTimeAction.SkipEvent -> {
                    Log.d("StatusV10", ">>> SKIP: eventId=${action.event.eventId}, begin=${action.event.begin}, end=${action.event.end}")
                    runtimeStateStore.clearUserSuppression()
                    runtimeStateStore.setSkippedEvent(
                        action.event.eventId,
                        action.event.begin,
                        action.event.end
                    )
                }
            }
            Log.d("StatusV10", ">>> Running engine...")
            withContext(Dispatchers.IO) {
                EngineRunner.runEngine(context, Trigger.MANUAL)
            }
            Log.d("StatusV10", ">>> Engine done, calling refresh()...")
            refresh("applyOneTimeAction")
        }
    }

    fun clearOneTimeAction() {
        scope.launch {
            runtimeStateStore.clearAllOneTimeActions()
            withContext(Dispatchers.IO) {
                EngineRunner.runEngine(context, Trigger.MANUAL)
            }
            refresh("clearOneTimeAction")
        }
    }

    // Track if app is in foreground
    var isResumed by remember { mutableStateOf(false) }

    // Lifecycle & data collection effects
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    isResumed = true
                    refresh("ON_RESUME")
                }
                Lifecycle.Event.ON_PAUSE -> {
                    isResumed = false
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Smart auto-refresh: continuously recalculates when next state change is expected
    // Uses a while loop so it keeps firing even when keys don't change
    LaunchedEffect(isResumed) {
        while (isResumed) {
            val now = System.currentTimeMillis()
            val offsetMs = dndStartOffsetMinutes * 60_000L

            Log.d("StatusV10", "[SmartRefresh] calculating: activeWindow.end=${activeWindow?.end}, nextInstance.begin=${nextInstance?.begin}, offset=${offsetMs}")

            // Calculate next relevant timestamp
            val nextBoundary = listOfNotNull(
                activeWindow?.end,
                nextInstance?.let { (it.begin + offsetMs).coerceAtLeast(it.begin) }
            )
                .filter { it > now }
                .minOrNull()

            val delayMs = if (nextBoundary != null && nextBoundary - now <= 60_000L) {
                (nextBoundary - now + 1_000L).coerceAtLeast(1_000L)
            } else {
                30_000L
            }

            debugSmartTimerDelayMs = delayMs
            debugSmartTimerTarget = System.currentTimeMillis() + delayMs
            Log.d("StatusV10", "[SmartRefresh] nextBoundary=$nextBoundary, delayMs=$delayMs (${delayMs/1000}s)")
            delay(delayMs)
            Log.d("StatusV10", "[SmartRefresh] FIRING refresh() now")
            if (isResumed) refresh("SmartTimer")
        }
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
        val job = scope.launch { settingsStore.debugEventHighlightPreset.collectLatest { eventHighlightPreset = it } }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch { settingsStore.debugEventHighlightBarHidden.collectLatest { eventHighlightBarHidden = it } }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch { settingsStore.statusDebugPanelEnabled.collectLatest { statusDebugPanelEnabled = it } }
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
        val job = scope.launch { runtimeStateStore.skippedEventId.collectLatest { skippedEventId = it } }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch { runtimeStateStore.skippedEventBeginMs.collectLatest { skippedEventBeginMs = it } }
        onDispose { job.cancel() }
    }

    DisposableEffect(Unit) {
        val job = scope.launch { runtimeStateStore.skippedEventEndMs.collectLatest { skippedEventEndMs = it } }
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
    val rawActiveEvents = activeWindowRaw?.events.orEmpty()
    val specialRuleEvent = rawActiveEvents.firstOrNull { determineActiveAction(it) != null }
    val hasSpecialRuleActiveEvent = specialRuleEvent != null
    val activePrimaryEvent = if (isDndActive) {
        activeWindow?.events?.firstOrNull()
    } else {
        specialRuleEvent
    }
    val activeOverlapCount = if (isDndActive) {
        activeWindow?.events?.size ?: 0
    } else {
        rawActiveEvents.size
    }
    val activeActionType = determineActiveAction(activePrimaryEvent)
    val activeActionData = activePrimaryEvent?.let { instance ->
        val (startMs, endMs) = buildManualWindow(instance)
        if (automationEnabled) {
            OneTimeAction.SkipEvent(instance, startMs, endMs)
        } else {
            OneTimeAction.EnableForEvent(instance, startMs, endMs)
        }
    }
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
                refresh("PullToRefresh")
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
        // Resolve strings at composable level to ensure correct locale context
        val (titleText, bodyText, confirmText) = when (dialog) {
            is OneTimeDialog.Set -> when (dialog.action) {
                is OneTimeAction.EnableForEvent -> Triple(
                    stringResource(R.string.next_event_enable_confirm_title),
                    stringResource(R.string.next_event_enable_confirm_body),
                    stringResource(R.string.next_event_enable_confirm_action)
                )
                is OneTimeAction.SkipEvent -> Triple(
                    stringResource(R.string.next_event_skip_confirm_title),
                    stringResource(R.string.next_event_skip_confirm_body),
                    stringResource(R.string.next_event_skip_confirm_action)
                )
            }
            is OneTimeDialog.Clear -> when (dialog.activeType) {
                OneTimeActionType.ENABLE -> Triple(
                    stringResource(R.string.next_event_clear_enable_title),
                    stringResource(R.string.next_event_clear_enable_body),
                    stringResource(R.string.next_event_clear_enable_action)
                )
                OneTimeActionType.SKIP -> Triple(
                    stringResource(R.string.next_event_clear_skip_title),
                    stringResource(R.string.next_event_clear_skip_body),
                    stringResource(R.string.next_event_clear_skip_action)
                )
            }
        }
        val cancelText = stringResource(R.string.cancel)

        AlertDialog(
            onDismissRequest = { pendingOneTimeDialog = null },
            title = { Text(text = titleText) },
            text = { Text(text = bodyText) },
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
                    Text(text = confirmText)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingOneTimeDialog = null }) {
                    Text(text = cancelText)
                }
            }
        )
    }

    if (showActiveEventsDialog && activeWindowRaw?.events?.isNotEmpty() == true) {
        AlertDialog(
            onDismissRequest = { showActiveEventsDialog = false },
            title = { Text(stringResource(R.string.active_meetings_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    activeWindowRaw!!.events.forEach { event ->
                        val title = event.title.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.untitled_meeting)
                        val timeLabel = "${TimeUtils.formatTime(context, event.begin)} – ${TimeUtils.formatTime(context, event.end)}"
                        val eventActionType = determineActiveAction(event)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showActiveEventsDialog = false
                                    StatusScreenIntents.openCalendarEvent(context, event.eventId, event.begin)
                                }
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = timeLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val actionLabel = when {
                                    eventActionType == OneTimeActionType.SKIP -> stringResource(R.string.next_event_clear_skip_action)
                                    eventActionType == OneTimeActionType.ENABLE -> stringResource(R.string.next_event_clear_enable_action)
                                    automationEnabled -> stringResource(R.string.next_event_skip_dnd)
                                    else -> stringResource(R.string.next_event_enable_dnd)
                                }
                                TextButton(onClick = {
                                    showActiveEventsDialog = false
                                    if (eventActionType != null) {
                                        clearOneTimeAction()
                                    } else {
                                        val (startMs, endMs) = buildManualWindow(event)
                                        val action = if (automationEnabled) {
                                            OneTimeAction.SkipEvent(event, startMs, endMs)
                                        } else {
                                            OneTimeAction.EnableForEvent(event, startMs, endMs)
                                        }
                                        if (oneTimeActionConfirmation) {
                                            pendingOneTimeDialog = OneTimeDialog.Set(action)
                                        } else {
                                            applyOneTimeAction(action)
                                        }
                                    }
                                }) {
                                    Text(actionLabel)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showActiveEventsDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    if (showNextEventsDialog && nextOverlappingEvents.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showNextEventsDialog = false },
            title = { Text(stringResource(R.string.upcoming_meetings_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    nextOverlappingEvents.forEach { event ->
                        val title = event.title.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.untitled_meeting)
                        val timeLabel = "${TimeUtils.formatTime(context, event.begin)} – ${TimeUtils.formatTime(context, event.end)}"
                        val eventActionType = determineActiveAction(event)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showNextEventsDialog = false
                                    StatusScreenIntents.openCalendarEvent(context, event.eventId, event.begin)
                                }
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = timeLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val actionLabel = when {
                                    eventActionType == OneTimeActionType.SKIP -> stringResource(R.string.next_event_clear_skip_action)
                                    eventActionType == OneTimeActionType.ENABLE -> stringResource(R.string.next_event_clear_enable_action)
                                    automationEnabled -> stringResource(R.string.next_event_skip_dnd)
                                    else -> stringResource(R.string.next_event_enable_dnd)
                                }
                                TextButton(onClick = {
                                    showNextEventsDialog = false
                                    if (eventActionType != null) {
                                        clearOneTimeAction()
                                    } else {
                                        val (startMs, endMs) = buildManualWindow(event)
                                        val action = if (automationEnabled) {
                                            OneTimeAction.SkipEvent(event, startMs, endMs)
                                        } else {
                                            OneTimeAction.EnableForEvent(event, startMs, endMs)
                                        }
                                        if (oneTimeActionConfirmation) {
                                            pendingOneTimeDialog = OneTimeDialog.Set(action)
                                        } else {
                                            applyOneTimeAction(action)
                                        }
                                    }
                                }) {
                                    Text(actionLabel)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNextEventsDialog = false }) {
                    Text(stringResource(R.string.close))
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
                color = MaterialTheme.colorScheme.onSurface,
                modifier = if (statusDebugPanelEnabled) Modifier.clickable { debugShowPanel = !debugShowPanel } else Modifier
            )

            // --- DEBUG PANEL (tap app name to toggle) ---
            if (statusDebugPanelEnabled && debugShowPanel) {
                val debugNow = nowMs
                val debugTimerIn = if (debugSmartTimerTarget > debugNow) {
                    "${(debugSmartTimerTarget - debugNow) / 1000}s"
                } else "fired"
                val debugLastAgo = if (debugLastRefreshMs > 0) {
                    "${(debugNow - debugLastRefreshMs) / 1000}s ago"
                } else "never"
                val debugWindowInfo = activeWindow?.let {
                    "events=${it.events.size}, ${it.events.map { e -> e.title }}"
                } ?: "null"
                val debugRawInfo = activeWindowRaw?.let {
                    "events=${it.events.size}, ${it.events.map { e -> e.title }}"
                } ?: "null"
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    color = Color(0xCC000000),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("DEBUG REFRESH PANEL", color = Color.Yellow, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text("refreshCount: $debugRefreshCount", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        Text("lastRefresh: $debugLastAgo ($debugLastRefreshSource)", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        Text("smartTimer: $debugTimerIn (delay=${debugSmartTimerDelayMs/1000}s)", color = Color.Cyan, style = MaterialTheme.typography.bodySmall)
                        Text("isResumed: $isResumed", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("STATE:", color = Color.Yellow, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text("dndSetByApp: $dndSetByApp", color = if (dndSetByApp) Color.Green else Color.Red, style = MaterialTheme.typography.bodySmall)
                        Text("isDndActive: $isDndActive", color = if (isDndActive) Color.Green else Color.Red, style = MaterialTheme.typography.bodySmall)
                        Text("automationEnabled: $automationEnabled", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("WINDOWS:", color = Color.Yellow, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text("activeWindow: $debugWindowInfo", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        Text("activeWindowRaw: $debugRawInfo", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        Text("nextInstance: ${nextInstance?.title ?: "null"}", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("SKIP:", color = Color.Yellow, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text("skipId=$skippedEventId, begin=$skippedEventBeginMs, end=$skippedEventEndMs", color = if (skippedEventId > 0) Color.Magenta else Color.Gray, style = MaterialTheme.typography.bodySmall)
                        Text("activePrimaryEvent: ${activePrimaryEvent?.title ?: "null"}", color = Color.White, style = MaterialTheme.typography.bodySmall)
                        Text("activeActionType: $activeActionType", color = Color.White, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

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
                            val overlapCount = activeOverlapCount
                            when {
                                meetingTitle != null && overlapCount > 1 ->
                                    stringResource(R.string.status_in_meeting, meetingTitle) +
                                        " " + stringResource(R.string.active_meeting_overlap_chip, overlapCount)
                                meetingTitle != null ->
                                    stringResource(R.string.status_in_meeting, meetingTitle)
                                else ->
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
                        overflow = TextOverflow.Ellipsis,
                        modifier = if (isDndActive && activeOverlapCount > 1) {
                            Modifier.clickable { showActiveEventsDialog = true }
                        } else {
                            Modifier
                        }
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
                            highlightColor = eventHighlightColor,
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

            // Active meeting card when DND is not active (skipped or paused)
            AnimatedVisibility(
                visible = !isDndActive && activePrimaryEvent != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                val activeEvent = activePrimaryEvent ?: return@AnimatedVisibility
                val overlapCount = activeOverlapCount.coerceAtLeast(1)
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ActiveMeetingCardV10(
                        title = activeEvent.title.takeIf { it.isNotBlank() } ?: stringResource(R.string.untitled_meeting),
                        startTime = TimeUtils.formatTime(context, activeEvent.begin),
                        endTime = TimeUtils.formatTime(context, activeEvent.end),
                        overlapCount = overlapCount,
                        highlightCard = activeActionType != null,
                        highlightColor = eventHighlightColor,
                        onClick = {
                            if (overlapCount > 1) {
                                showActiveEventsDialog = true
                            } else {
                                StatusScreenIntents.openCalendarEvent(context, activeEvent.eventId, activeEvent.begin)
                            }
                        }
                    )
                    ActionStrip(
                        activeActionType = activeActionType,
                        automationEnabled = automationEnabled,
                        enabled = nextActionEnabled,
                        activeHighlightColor = eventHighlightColor,
                        onSetAction = {
                            val action = activeActionData ?: return@ActionStrip
                            if (oneTimeActionConfirmation) {
                                pendingOneTimeDialog = OneTimeDialog.Set(action)
                            } else {
                                applyOneTimeAction(action)
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

            // Next meeting card (when not in DND)
            AnimatedVisibility(
                visible = !isDndActive && nextInstance != null && !hasSpecialRuleActiveEvent,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                    nextInstance?.let { next ->
                        val activeActionType = determineActiveAction(next)
                        val nextOverlapCount = nextOverlappingEvents.size.coerceAtLeast(1)
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        NextMeetingCardV10(
                            title = next.title.takeIf { it.isNotBlank() } ?: stringResource(R.string.untitled_meeting),
                            startTime = TimeUtils.formatTime(context, next.begin),
                            endTime = TimeUtils.formatTime(context, next.end),
                            startsIn = TimeUtils.formatDuration((next.begin - nowMs).coerceAtLeast(0L)),
                            overlapCount = nextOverlapCount,
                            highlightCard = activeActionType != null,
                            highlightColor = eventHighlightColor,
                            hasActionStrip = true,
                            onClick = {
                                if (nextOverlapCount > 1) {
                                    showNextEventsDialog = true
                                } else {
                                    StatusScreenIntents.openCalendarEvent(context, next.eventId, next.begin)
                                }
                            }
                        )
                        ActionStrip(
                            activeActionType = activeActionType,
                            automationEnabled = automationEnabled,
                            enabled = nextActionEnabled,
                            activeHighlightColor = eventHighlightColor,
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

            var suppressSettingsClick by remember { mutableStateOf(false) }

            // Settings button
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = {
                        if (suppressSettingsClick) {
                            suppressSettingsClick = false
                        } else {
                            onOpenSettings(false)
                        }
                    })
                    .pointerInput(eventHighlightBarHidden) {
                        awaitEachGesture {
                            awaitFirstDown()
                            val up = withTimeoutOrNull(2000L) {
                                waitForUpOrCancellation()
                            }
                            if (up == null) {
                                suppressSettingsClick = true
                                scope.launch {
                                    settingsStore.setDebugEventHighlightBarHidden(!eventHighlightBarHidden)
                                }
                                waitForUpOrCancellation()
                            }
                        }
                    },
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

            if (themeDebugMode == ThemeDebugMode.EVENT_CARD_COLORS && !eventHighlightBarHidden) {
                EventColorSelectorBottomBar(
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
    highlightColor: Color,
    onCardClick: () -> Unit,
    onSetAction: () -> Unit,
    onClearAction: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
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
                activeHighlightColor = highlightColor,
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
    overlapCount: Int = 1,
    highlightCard: Boolean,
    highlightColor: Color,
    hasActionStrip: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
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

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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
                    if (overlapCount > 1) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = chipBackground
                        ) {
                            Text(
                                text = stringResource(R.string.active_meeting_overlap_chip, overlapCount),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = chipText
                            )
                        }
                    }
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

@Composable
private fun ActiveMeetingCardV10(
    title: String,
    startTime: String,
    endTime: String,
    overlapCount: Int,
    highlightCard: Boolean,
    highlightColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
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
            bottomStart = 0.dp,
            bottomEnd = 0.dp
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
                        text = stringResource(R.string.current_meeting).uppercase(),
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

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = chipBackground
                    ) {
                        Text(
                            text = stringResource(R.string.time_until_now),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = chipText
                        )
                    }
                    if (overlapCount > 1) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = chipBackground
                        ) {
                            Text(
                                text = stringResource(R.string.active_meeting_overlap_chip, overlapCount),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = chipText
                            )
                        }
                    }
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
    activeHighlightColor: Color,
    onSetAction: () -> Unit,
    onClearAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = LocalIsDarkTheme.current
    val isActive = activeActionType != null

    // Neutral gray when active, blue as default
    val backgroundColor = if (isActive) {
        if (isDarkTheme) Color(0xFF3A3A3A) else Color(0xFFE8E8E8)
    } else {
        activeHighlightColor
    }

    val contentColor = if (isActive) {
        if (isDarkTheme) Color.White.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.8f)
    } else {
        Color.Black.copy(alpha = 0.85f)
    }

    val onClick = when {
        !enabled -> null
        activeActionType != null -> onClearAction
        else -> onSetAction
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
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
                .padding(horizontal = 16.dp),
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
