package com.brunoafk.calendardnd.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import com.brunoafk.calendardnd.system.alarms.EngineRunner
import com.brunoafk.calendardnd.util.PermissionUtils
import com.brunoafk.calendardnd.util.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sin
import kotlin.random.Random

// Joyful color palette
private val JoyfulPink = Color(0xFFFF6B9D)
private val JoyfulPurple = Color(0xFFC56CF0)
private val JoyfulBlue = Color(0xFF74B9FF)
private val JoyfulCyan = Color(0xFF00D2D3)
private val JoyfulGreen = Color(0xFF55EFC4)
private val JoyfulYellow = Color(0xFFFECA57)
private val JoyfulOrange = Color(0xFFFF9F43)
private val JoyfulRed = Color(0xFFFF6B6B)

private val GradientColors = listOf(
    JoyfulPink,
    JoyfulPurple,
    JoyfulBlue,
    JoyfulCyan,
    JoyfulGreen
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun StatusScreenV5(
    onNavigateBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectTheme: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val settingsStore = remember { SettingsStore(context) }
    val runtimeStateStore = remember { RuntimeStateStore(context) }
    val calendarRepository = remember { CalendarRepository(context) }
    val dndController = remember { DndController(context) }

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
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isRefreshing by remember { mutableStateOf(false) }
    var themeDebugMode by remember { mutableStateOf(ThemeDebugMode.OFF) }

    fun refresh() {
        scope.launch {
            hasCalendarPermission = PermissionUtils.hasCalendarPermission(context)
            hasPolicyAccess = dndController.hasPolicyAccess()

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

    // Data collection effects (condensed)
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

    DisposableEffect(Unit) {
        val job = scope.launch { settingsStore.themeDebugMode.collectLatest { themeDebugMode = it } }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        // Colorful animated background
        ColorfulBackground(isDndActive = isDndActive)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Calendar DND",
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                JoyfulButton(
                    onClick = onOpenSettings,
                    colors = listOf(JoyfulPurple, JoyfulPink)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main status card
            JoyfulStatusCard(
                isDndActive = isDndActive,
                isPaused = !automationEnabled && !missingPermissions,
                isError = missingPermissions,
                activeWindow = activeWindow,
                nowMs = nowMs,
                dndMode = dndMode
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Meeting info card
            if (isDndActive && activeWindow != null) {
                CurrentMeetingCard(
                    title = activeWindow?.events?.firstOrNull()?.title?.takeIf { it.isNotBlank() } ?: "Meeting",
                    timeRange = "${TimeUtils.formatTime(context, activeWindow!!.begin)} - ${TimeUtils.formatTime(context, activeWindow!!.end)}",
                    remainingMs = (activeWindow!!.end - nowMs).coerceAtLeast(0L)
                )
            } else if (nextInstance != null) {
                NextMeetingCardJoyful(
                    title = nextInstance!!.title.takeIf { it.isNotBlank() } ?: "Meeting",
                    timeRange = "${TimeUtils.formatTime(context, nextInstance!!.begin)} - ${TimeUtils.formatTime(context, nextInstance!!.end)}",
                    startsIn = TimeUtils.formatDuration((nextInstance!!.begin - nowMs).coerceAtLeast(0L))
                )
            } else if (!missingPermissions && automationEnabled) {
                EmptyStateCard()
            }

            Spacer(modifier = Modifier.weight(1f))

            // Automation toggle
            JoyfulToggle(
                enabled = automationEnabled,
                onToggle = { enabled ->
                    scope.launch {
                        settingsStore.setAutomationEnabled(enabled)
                        if (enabled) {
                            withContext(Dispatchers.IO) {
                                EngineRunner.runEngine(context, Trigger.MANUAL)
                            }
                        }
                        refresh()
                    }
                }
            )

            if (themeDebugMode == ThemeDebugMode.THEME_SELECTOR) {
                Spacer(modifier = Modifier.height(12.dp))
                ThemeSelectorBottomBar(onSelectTheme = onSelectTheme)
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
private fun ColorfulBackground(isDndActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")

    val offset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset1"
    )

    val offset2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset2"
    )

    val offset3 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset3"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (isDndActive) {
                        listOf(
                            Color(0xFF2D1B4E),
                            Color(0xFF1A1A2E)
                        )
                    } else {
                        listOf(
                            Color(0xFF1A1A2E),
                            Color(0xFF16213E)
                        )
                    }
                )
            )
    ) {
        val maxW = maxWidth
        val maxH = maxHeight

        // Colorful blobs
        ColorfulBlob(
            color = if (isDndActive) JoyfulPurple else JoyfulPink,
            size = 200.dp,
            modifier = Modifier
                .offset(
                    x = maxW * (-0.1f + offset1 * 0.3f),
                    y = maxH * (0.1f + offset2 * 0.1f)
                )
        )

        ColorfulBlob(
            color = if (isDndActive) JoyfulBlue else JoyfulCyan,
            size = 180.dp,
            modifier = Modifier
                .offset(
                    x = maxW * (0.6f + offset2 * 0.2f),
                    y = maxH * (0.05f + offset1 * 0.15f)
                )
        )

        ColorfulBlob(
            color = if (isDndActive) JoyfulPink else JoyfulPurple,
            size = 160.dp,
            modifier = Modifier
                .offset(
                    x = maxW * (0.7f + offset3 * 0.2f),
                    y = maxH * (0.5f + offset1 * 0.1f)
                )
        )

        ColorfulBlob(
            color = if (isDndActive) JoyfulCyan else JoyfulGreen,
            size = 140.dp,
            modifier = Modifier
                .offset(
                    x = maxW * (-0.05f + offset1 * 0.15f),
                    y = maxH * (0.6f + offset3 * 0.15f)
                )
        )
    }
}

@Composable
private fun ColorfulBlob(
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .blur(60.dp)
            .background(color.copy(alpha = 0.4f), CircleShape)
    )
}

@Composable
private fun JoyfulButton(
    onClick: () -> Unit,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(colors))
            .clickable(onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun JoyfulStatusCard(
    isDndActive: Boolean,
    isPaused: Boolean,
    isError: Boolean,
    activeWindow: MeetingWindow?,
    nowMs: Long,
    dndMode: DndMode
) {
    val infiniteTransition = rememberInfiniteTransition(label = "status")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wobble"
    )

    val (gradientColors, icon, statusText, subtitleText) = when {
        isError -> StatusData(
            colors = listOf(JoyfulOrange, JoyfulRed),
            icon = Icons.Rounded.Warning,
            status = "Oops!",
            subtitle = "Needs some setup love"
        )
        isDndActive -> StatusData(
            colors = listOf(JoyfulPurple, JoyfulPink),
            icon = Icons.AutoMirrored.Rounded.VolumeOff,
            status = "Shhh...",
            subtitle = "Focus mode activated"
        )
        isPaused -> StatusData(
            colors = listOf(Color(0xFF636E72), Color(0xFF2D3436)),
            icon = Icons.Rounded.MusicNote,
            status = "Taking a break",
            subtitle = "Automation is paused"
        )
        else -> StatusData(
            colors = listOf(JoyfulCyan, JoyfulGreen),
            icon = Icons.Rounded.Celebration,
            status = "All good!",
            subtitle = "Ready to help you focus"
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (isDndActive) pulseScale else 1f),
        shape = RoundedCornerShape(32.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(gradientColors),
                    RoundedCornerShape(32.dp)
                )
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .rotate(if (!isDndActive && !isError && !isPaused) rotation else 0f)
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = icon,
                        transitionSpec = {
                            (scaleIn(spring(stiffness = Spring.StiffnessLow)) + fadeIn()) togetherWith
                                (scaleOut() + fadeOut())
                        },
                        label = "icon"
                    ) { targetIcon ->
                        Icon(
                            imageVector = targetIcon,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Status text
                AnimatedContent(
                    targetState = statusText,
                    transitionSpec = {
                        fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                    },
                    label = "status"
                ) { text ->
                    Text(
                        text = text,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = subtitleText,
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )

                // Time remaining for active meeting
                if (isDndActive && activeWindow != null) {
                    Spacer(modifier = Modifier.height(24.dp))

                    val remaining = (activeWindow.end - nowMs).coerceAtLeast(0L)
                    val minutes = remaining / 60000
                    val seconds = (remaining % 60000) / 1000

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TimeChip(value = minutes.toInt(), label = "min")
                        Text(
                            text = ":",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        TimeChip(value = seconds.toInt(), label = "sec")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress dots
                    val progress = if (activeWindow.end > activeWindow.begin) {
                        ((nowMs - activeWindow.begin).toFloat() / (activeWindow.end - activeWindow.begin)).coerceIn(0f, 1f)
                    } else 0f

                    ProgressDots(progress = progress)
                }
            }
        }
    }
}

private data class StatusData(
    val colors: List<Color>,
    val icon: ImageVector,
    val status: String,
    val subtitle: String
)

@Composable
private fun TimeChip(
    value: Int,
    label: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.2f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = String.format("%02d", value),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ProgressDots(progress: Float) {
    val dotCount = 10
    val filledDots = (progress * dotCount).toInt()

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(dotCount) { index ->
            val isFilled = index < filledDots
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        if (isFilled) Color.White else Color.White.copy(alpha = 0.3f),
                        CircleShape
                    )
            )
        }
    }
}

@Composable
private fun CurrentMeetingCard(
    title: String,
    timeRange: String,
    remainingMs: Long
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Brush.linearGradient(listOf(JoyfulPurple, JoyfulPink)),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicOff,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = timeRange,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun NextMeetingCardJoyful(
    title: String,
    timeRange: String,
    startsIn: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Brush.linearGradient(listOf(JoyfulCyan, JoyfulGreen)),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = timeRange,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = JoyfulGreen.copy(alpha = 0.3f)
            ) {
                Text(
                    text = "in $startsIn",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = JoyfulGreen
                )
            }
        }
    }
}

@Composable
private fun EmptyStateCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.EventBusy,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No meetings today!",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.8f)
            )
            Text(
                text = "Enjoy your free time",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun JoyfulToggle(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (enabled) 1.02f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "toggleScale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedScale)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onToggle(!enabled) },
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (enabled) {
                        Brush.linearGradient(listOf(JoyfulCyan, JoyfulGreen))
                    } else {
                        Brush.linearGradient(listOf(Color(0xFF4A4A4A), Color(0xFF3A3A3A)))
                    },
                    RoundedCornerShape(24.dp)
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Auto-silence",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = if (enabled) "Tap to pause" else "Tap to enable",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = enabled,
                        transitionSpec = {
                            (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut())
                        },
                        label = "toggleIcon"
                    ) { isEnabled ->
                        Icon(
                            imageVector = if (isEnabled) Icons.Rounded.NotificationsOff else Icons.Rounded.Notifications,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}
