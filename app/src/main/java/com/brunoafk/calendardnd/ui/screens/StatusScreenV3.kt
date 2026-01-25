package com.brunoafk.calendardnd.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.DoNotDisturb
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun StatusScreenV3(
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

    // Lifecycle & data collection effects (same as V2)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
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
        val job = scope.launch {
            runtimeStateStore.dndSetByApp.collectLatest { value ->
                dndSetByApp = value
            }
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

    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val missingPermissions = !hasCalendarPermission || !hasPolicyAccess
    val isDndActive = dndSetByApp && activeWindow != null

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

    // Calculate progress for active meeting
    var meetingProgress by remember { mutableFloatStateOf(0f) }
    if (isDndActive && activeWindow != null) {
        val duration = (activeWindow!!.end - activeWindow!!.begin).toFloat()
        val elapsed = (nowMs - activeWindow!!.begin).toFloat()
        meetingProgress = (elapsed / duration).coerceIn(0f, 1f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceContainerLowest
                    )
                )
            )
            .pullRefresh(pullRefreshState)
    ) {
        // Ambient background orbs
        AmbientBackground(
            isDndActive = isDndActive,
            isError = missingPermissions,
            isPaused = !automationEnabled && !missingPermissions
        )

        val selectorBottomPadding = if (themeDebugMode == ThemeDebugMode.THEME_SELECTOR) 96.dp else 0.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 16.dp + selectorBottomPadding
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top row: Title + Settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Calendar\nDND",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    lineHeight = 36.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                BentoCard(
                    modifier = Modifier.size(56.dp),
                    onClick = { onOpenSettings(false) }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

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

            // Bento Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left column - Status hero
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Main status card (large)
                    BentoCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.5f),
                        backgroundColor = when {
                            missingPermissions -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                            isDndActive -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f)
                            !automationEnabled -> MaterialTheme.colorScheme.surfaceContainerHigh
                            else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            // Animated ring for active state
                            if (isDndActive) {
                                AnimatedProgressRing(
                                    progress = meetingProgress,
                                    modifier = Modifier.size(140.dp)
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val statusIcon = when {
                                    missingPermissions -> Icons.Rounded.Warning
                                    isDndActive -> Icons.Rounded.DoNotDisturb
                                    !automationEnabled -> Icons.Rounded.Pause
                                    else -> Icons.Rounded.Notifications
                                }

                                Icon(
                                    imageVector = statusIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = when {
                                        missingPermissions -> MaterialTheme.colorScheme.error
                                        isDndActive -> MaterialTheme.colorScheme.tertiary
                                        !automationEnabled -> MaterialTheme.colorScheme.onSurfaceVariant
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                AnimatedContent(
                                    targetState = when {
                                        missingPermissions -> "Setup\nNeeded"
                                        isDndActive -> "Silent\nMode"
                                        !automationEnabled -> "Paused"
                                        else -> "Ready"
                                    },
                                    transitionSpec = {
                                        fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                                    },
                                    label = "status"
                                ) { status ->
                                    Text(
                                        text = status,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            missingPermissions -> MaterialTheme.colorScheme.onErrorContainer
                                            isDndActive -> MaterialTheme.colorScheme.onTertiaryContainer
                                            !automationEnabled -> MaterialTheme.colorScheme.onSurfaceVariant
                                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                                        }
                                    )
                                }

                                if (isDndActive && activeWindow != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val remaining = (activeWindow!!.end - nowMs).coerceAtLeast(0L)
                                    Text(
                                        text = TimeUtils.formatDuration(remaining),
                                        style = MaterialTheme.typography.displaySmall,
                                        fontWeight = FontWeight.Light,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }

                    // DND Mode indicator
                    BentoCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.5f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (dndMode == DndMode.PRIORITY) {
                                        Icons.Rounded.Notifications
                                    } else {
                                        Icons.Rounded.NotificationsOff
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = "Mode",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (dndMode == DndMode.PRIORITY) "Priority" else "Silence",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                // Right column - Info cards
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Automation toggle
                    BentoCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.6f),
                        onClick = {
                            scope.launch {
                                val newValue = !automationEnabled
                                settingsStore.setAutomationEnabled(newValue)
                                if (newValue) {
                                    withContext(Dispatchers.IO) {
                                        EngineRunner.runEngine(context, Trigger.MANUAL)
                                    }
                                }
                                refresh()
                            }
                        },
                        backgroundColor = if (automationEnabled) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "Auto",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = if (automationEnabled) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (automationEnabled) {
                                                Icons.Rounded.PlayArrow
                                            } else {
                                                Icons.Rounded.Pause
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = if (automationEnabled) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                }
                            }

                            Text(
                                text = if (automationEnabled) "ON" else "OFF",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Black,
                                color = if (automationEnabled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }

                    // Current/Next meeting card
                    val meetingEvent = activeWindow?.events?.firstOrNull() ?: nextInstance
                    BentoCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        onClick = meetingEvent?.let {
                            { StatusScreenIntents.openCalendarEvent(context, it.eventId, it.begin) }
                        }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (activeWindow != null) "NOW" else "NEXT",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeWindow != null) {
                                        MaterialTheme.colorScheme.tertiary
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    }
                                )
                            }

                            val meetingToShow = meetingEvent

                            if (meetingToShow != null) {
                                Column {
                                    Text(
                                        text = meetingToShow.title.takeIf { it.isNotBlank() } ?: "Untitled",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${TimeUtils.formatTime(context, meetingToShow.begin)} - ${TimeUtils.formatTime(context, meetingToShow.end)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (activeWindow == null && nextInstance != null) {
                                    val startsIn = (nextInstance!!.begin - nowMs).coerceAtLeast(0L)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    ) {
                                        Text(
                                            text = "in ${TimeUtils.formatDuration(startsIn)}",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            } else {
                                Column {
                                    Text(
                                        text = "All clear",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "No meetings",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }

                    // Quick action
                    BentoCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.4f),
                        onClick = { onOpenSettings(false) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Settings",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                }
            }
        }

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        if (themeDebugMode == ThemeDebugMode.THEME_SELECTOR) {
            ThemeSelectorBottomBar(
                onSelectTheme = onSelectTheme,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun BentoCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .then(
                if (onClick != null) Modifier.clip(RoundedCornerShape(24.dp)).clickable(onClick = onClick)
                else Modifier
            ),
        shape = RoundedCornerShape(24.dp),
        color = backgroundColor,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
private fun AnimatedProgressRing(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(500),
        label = "progress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "ring")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val primaryColor = MaterialTheme.colorScheme.tertiary
    val trackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)

    Canvas(modifier = modifier.rotate(rotation - 90f)) {
        val strokeWidth = 6.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2

        // Track
        drawCircle(
            color = trackColor,
            radius = radius,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Progress arc
        drawArc(
            color = primaryColor,
            startAngle = 0f,
            sweepAngle = animatedProgress * 360f,
            useCenter = false,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun AmbientBackground(
    isDndActive: Boolean,
    isError: Boolean,
    isPaused: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient")

    val offset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset1"
    )

    val offset2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset2"
    )

    val orbColor = when {
        isError -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
        isDndActive -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
        isPaused -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxW = maxWidth
        val maxH = maxHeight

        // Orb 1
        Box(
            modifier = Modifier
                .offset(
                    x = maxW * 0.6f + (maxW * 0.2f * offset1),
                    y = maxH * 0.1f + (maxH * 0.1f * offset2)
                )
                .size(200.dp)
                .blur(60.dp)
                .alpha(0.8f)
                .background(orbColor, CircleShape)
        )

        // Orb 2
        Box(
            modifier = Modifier
                .offset(
                    x = maxW * (-0.1f) + (maxW * 0.15f * offset2),
                    y = maxH * 0.5f + (maxH * 0.15f * offset1)
                )
                .size(180.dp)
                .blur(50.dp)
                .alpha(0.6f)
                .background(orbColor, CircleShape)
        )
    }
}
