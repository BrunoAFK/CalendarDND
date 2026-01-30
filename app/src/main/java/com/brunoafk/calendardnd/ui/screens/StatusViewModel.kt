package com.brunoafk.calendardnd.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.brunoafk.calendardnd.data.calendar.CalendarRepository
import com.brunoafk.calendardnd.data.dnd.DndController
import com.brunoafk.calendardnd.data.prefs.RuntimeStateStore
import com.brunoafk.calendardnd.data.prefs.SettingsStore
import com.brunoafk.calendardnd.domain.model.DndMode
import com.brunoafk.calendardnd.domain.model.EventHighlightPreset
import com.brunoafk.calendardnd.domain.model.EventInstance
import com.brunoafk.calendardnd.domain.model.KeywordMatchMode
import com.brunoafk.calendardnd.domain.model.MeetingWindow
import com.brunoafk.calendardnd.domain.model.OneTimeAction
import com.brunoafk.calendardnd.domain.model.OneTimeActionType
import com.brunoafk.calendardnd.domain.model.ThemeDebugMode
import com.brunoafk.calendardnd.domain.model.Trigger
import com.brunoafk.calendardnd.domain.planning.MeetingWindowResolver
import com.brunoafk.calendardnd.domain.util.SkipUtils
import com.brunoafk.calendardnd.system.alarms.AlarmScheduler
import com.brunoafk.calendardnd.system.alarms.EngineRunner
import com.brunoafk.calendardnd.util.PermissionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Unified state for status screens - eliminates duplication between StatusScreen and StatusScreenV10.
 *
 * Benefits:
 * - Single source of truth for UI state
 * - Testable business logic (no Android dependencies in state calculations)
 * - Automatic UI updates when any source flow emits
 * - No manual refresh() calls needed for flow-based updates
 */
data class StatusUiState(
    // Settings - Core
    val automationEnabled: Boolean = false,
    val dndMode: DndMode = DndMode.PRIORITY,
    val dndStartOffsetMinutes: Int = 0,
    val oneTimeActionConfirmation: Boolean = true,
    val onboardingCompleted: Boolean = false,

    // Settings - Filters
    val selectedCalendarIds: Set<String> = emptySet(),
    val busyOnly: Boolean = true,
    val ignoreAllDay: Boolean = true,
    val skipRecurring: Boolean = false,
    val selectedDaysEnabled: Boolean = false,
    val selectedDaysMask: Int = 0x7F,
    val minEventMinutes: Int = 10,
    val requireLocation: Boolean = false,
    val requireTitleKeyword: Boolean = false,
    val titleKeyword: String = "",
    val titleKeywordMatchMode: KeywordMatchMode = KeywordMatchMode.KEYWORDS,
    val titleKeywordCaseSensitive: Boolean = false,
    val titleKeywordMatchAll: Boolean = false,
    val titleKeywordExclude: Boolean = false,

    // Settings - Debug/Theme
    val themeDebugMode: ThemeDebugMode = ThemeDebugMode.OFF,
    val eventHighlightPreset: EventHighlightPreset = EventHighlightPreset.PRESET_1,
    val eventHighlightBarHidden: Boolean = false,

    // Runtime state
    val dndSetByApp: Boolean = false,
    val userSuppressedUntilMs: Long = 0L,
    val userSuppressedFromMs: Long = 0L,
    val manualEventStartMs: Long = 0L,
    val manualEventEndMs: Long = 0L,
    val skippedEventId: Long = 0L,
    val skippedEventBeginMs: Long = 0L,
    val skippedEventEndMs: Long = 0L,

    // Permissions
    val hasCalendarPermission: Boolean = false,
    val hasPolicyAccess: Boolean = false,
    val canScheduleExactAlarms: Boolean = false,

    // Calendar data
    val activeWindow: MeetingWindow? = null,
    val activeWindowRaw: MeetingWindow? = null,
    val nextInstance: EventInstance? = null,
    val nextOverlappingEvents: List<EventInstance> = emptyList(),

    // UI state
    val isRefreshing: Boolean = false
) {
    val missingPermissions: Boolean
        get() = !hasCalendarPermission || !hasPolicyAccess

    val isDndActive: Boolean
        get() = dndSetByApp && activeWindow != null

    val activeFilterCount: Int
        get() = listOf(
            busyOnly,
            ignoreAllDay,
            skipRecurring,
            selectedDaysEnabled,
            requireLocation,
            requireTitleKeyword
        ).count { it }

    fun isSkippedEvent(event: EventInstance?): Boolean {
        return SkipUtils.isSkippedEvent(event, skippedEventId, skippedEventBeginMs, skippedEventEndMs)
    }

    fun buildManualWindow(event: EventInstance): Pair<Long, Long> {
        val offsetMs = dndStartOffsetMinutes * 60_000L
        val rawStart = event.begin + offsetMs
        val start = if (rawStart >= event.end) event.begin else rawStart
        return start to event.end
    }

    fun determineActiveAction(event: EventInstance?): OneTimeActionType? {
        if (event == null) return null
        val now = System.currentTimeMillis()

        if (isSkippedEvent(event)) {
            return OneTimeActionType.SKIP
        }

        if (manualEventEndMs > 0 && now < manualEventEndMs) {
            val (startMs, endMs) = buildManualWindow(event)
            if (manualEventStartMs <= startMs && manualEventEndMs >= endMs) {
                return OneTimeActionType.ENABLE
            }
        }

        return null
    }
}

/**
 * ViewModel for Status screens that combines all state sources into a single reactive flow.
 *
 * This eliminates:
 * - 20+ individual DisposableEffect blocks
 * - Manual refresh() calls after every action
 * - Race conditions between DataStore emissions and UI reads
 * - Code duplication between StatusScreen and StatusScreenV10
 */
class StatusViewModel(
    private val context: Context,
    private val settingsStore: SettingsStore,
    private val runtimeStateStore: RuntimeStateStore,
    private val calendarRepository: CalendarRepository,
    private val dndController: DndController,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val _calendarData = MutableStateFlow(CalendarData())
    private val _isRefreshing = MutableStateFlow(false)
    private val _permissions = MutableStateFlow(PermissionState())

    /**
     * Combined UI state that automatically updates when ANY source flow emits.
     * Replaces 26+ individual DisposableEffect blocks with a single reactive stream.
     */
    val uiState: StateFlow<StatusUiState> = combine(
        // Core settings (5 flows)
        combine(
            settingsStore.automationEnabled,
            settingsStore.dndMode,
            settingsStore.dndStartOffsetMinutes,
            settingsStore.oneTimeActionConfirmation,
            settingsStore.onboardingCompleted,
        ) { automation, mode, offset, confirmation, onboarding ->
            CoreSettings(automation, mode, offset, confirmation, onboarding)
        },
        // Filter settings part 1 (5 flows)
        combine(
            settingsStore.selectedCalendarIds,
            settingsStore.busyOnly,
            settingsStore.ignoreAllDay,
            settingsStore.skipRecurring,
            settingsStore.selectedDaysEnabled,
        ) { calendarIds, busyOnly, ignoreAllDay, skipRecurring, daysEnabled ->
            FilterSettings1(calendarIds, busyOnly, ignoreAllDay, skipRecurring, daysEnabled)
        },
        // Filter settings part 2 (5 flows)
        combine(
            settingsStore.selectedDaysMask,
            settingsStore.minEventMinutes,
            settingsStore.requireLocation,
            settingsStore.requireTitleKeyword,
            settingsStore.titleKeyword,
        ) { daysMask, minMinutes, requireLoc, requireKeyword, keyword ->
            FilterSettings2(daysMask, minMinutes, requireLoc, requireKeyword, keyword)
        },
        // Filter settings part 3 (4 flows)
        combine(
            settingsStore.titleKeywordMatchMode,
            settingsStore.titleKeywordCaseSensitive,
            settingsStore.titleKeywordMatchAll,
            settingsStore.titleKeywordExclude,
        ) { matchMode, caseSensitive, matchAll, exclude ->
            FilterSettings3(matchMode, caseSensitive, matchAll, exclude)
        },
        // Debug/theme settings (3 flows)
        combine(
            settingsStore.themeDebugMode,
            settingsStore.debugEventHighlightPreset,
            settingsStore.debugEventHighlightBarHidden,
        ) { themeDebug, highlightPreset, barHidden ->
            DebugSettings(themeDebug, highlightPreset, barHidden)
        },
    ) { core, filter1, filter2, filter3, debug ->
        AllSettings(core, filter1, filter2, filter3, debug)
    }.combine(
        // Runtime state part 1 (5 flows)
        combine(
            runtimeStateStore.dndSetByApp,
            runtimeStateStore.userSuppressedUntilMs,
            runtimeStateStore.userSuppressedFromMs,
            runtimeStateStore.manualEventStartMs,
            runtimeStateStore.manualEventEndMs,
        ) { dndSetByApp, suppressedUntil, suppressedFrom, manualStart, manualEnd ->
            RuntimeState1(dndSetByApp, suppressedUntil, suppressedFrom, manualStart, manualEnd)
        }
    ) { settings, runtime1 ->
        Pair(settings, runtime1)
    }.combine(
        // Runtime state part 2 (3 flows)
        combine(
            runtimeStateStore.skippedEventId,
            runtimeStateStore.skippedEventBeginMs,
            runtimeStateStore.skippedEventEndMs,
        ) { skippedId, skippedBegin, skippedEnd ->
            RuntimeState2(skippedId, skippedBegin, skippedEnd)
        }
    ) { (settings, runtime1), runtime2 ->
        Triple(settings, runtime1, runtime2)
    }.combine(_calendarData) { (settings, runtime1, runtime2), calendar ->
        CombinedData(settings, runtime1, runtime2, calendar)
    }.combine(_permissions) { combined, permissions ->
        Pair(combined, permissions)
    }.combine(_isRefreshing) { (combined, permissions), isRefreshing ->
        buildUiState(combined, permissions, isRefreshing)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatusUiState()
    )

    init {
        refreshPermissions()
        refreshCalendarData()
    }

    fun refresh() {
        refreshPermissions()
        refreshCalendarData()
    }

    fun refreshWithPullToRefresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            refreshPermissions()
            refreshCalendarData()
            _isRefreshing.value = false
        }
    }

    fun runEngineAndRefresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            withContext(Dispatchers.IO) {
                EngineRunner.runEngine(context, Trigger.MANUAL)
            }
            refreshPermissions()
            refreshCalendarData()
            _isRefreshing.value = false
        }
    }

    fun applyOneTimeAction(action: OneTimeAction) {
        viewModelScope.launch {
            when (action) {
                is OneTimeAction.EnableForEvent -> {
                    runtimeStateStore.setManualEvent(action.startMs, action.endMs)
                    runtimeStateStore.clearUserSuppression()
                    runtimeStateStore.clearSkippedEvent()
                }
                is OneTimeAction.SkipEvent -> {
                    runtimeStateStore.clearUserSuppression()
                    runtimeStateStore.setSkippedEvent(
                        action.event.eventId,
                        action.event.begin,
                        action.event.end
                    )
                }
            }
            withContext(Dispatchers.IO) {
                EngineRunner.runEngine(context, Trigger.MANUAL)
            }
            refreshCalendarData()
        }
    }

    fun clearOneTimeAction() {
        viewModelScope.launch {
            runtimeStateStore.clearAllOneTimeActions()
            withContext(Dispatchers.IO) {
                EngineRunner.runEngine(context, Trigger.MANUAL)
            }
            refreshCalendarData()
        }
    }

    fun setAutomationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setAutomationEnabled(enabled)
            withContext(Dispatchers.IO) {
                EngineRunner.runEngine(context, Trigger.MANUAL)
            }
            refreshCalendarData()
        }
    }

    private fun refreshPermissions() {
        _permissions.value = PermissionState(
            hasCalendar = PermissionUtils.hasCalendarPermission(context),
            hasPolicyAccess = dndController.hasPolicyAccess(),
            canScheduleExactAlarms = alarmScheduler.canScheduleExactAlarms()
        )
    }

    private fun refreshCalendarData() {
        viewModelScope.launch {
            val state = uiState.value
            if (!_permissions.value.hasCalendar) {
                _calendarData.value = CalendarData()
                return@launch
            }

            val data = withContext(Dispatchers.IO) {
                val now = System.currentTimeMillis()
                val runtimeSnapshot = runtimeStateStore.getSnapshot()

                val activeInstances = calendarRepository.getActiveInstances(
                    now = now,
                    selectedCalendarIds = state.selectedCalendarIds,
                    busyOnly = state.busyOnly,
                    ignoreAllDay = state.ignoreAllDay,
                    skipRecurring = state.skipRecurring,
                    selectedDaysEnabled = state.selectedDaysEnabled,
                    selectedDaysMask = state.selectedDaysMask,
                    minEventMinutes = state.minEventMinutes,
                    requireLocation = state.requireLocation,
                    requireTitleKeyword = state.requireTitleKeyword,
                    titleKeyword = state.titleKeyword,
                    titleKeywordMatchMode = state.titleKeywordMatchMode,
                    titleKeywordCaseSensitive = state.titleKeywordCaseSensitive,
                    titleKeywordMatchAll = state.titleKeywordMatchAll,
                    titleKeywordExclude = state.titleKeywordExclude
                )

                val windowRaw = MeetingWindowResolver.findActiveWindow(activeInstances, now)

                val filteredInstances = SkipUtils.filterOutSkipped(
                    activeInstances,
                    runtimeSnapshot.skippedEventId,
                    runtimeSnapshot.skippedEventBeginMs,
                    runtimeSnapshot.skippedEventEndMs,
                    now
                )
                val window = MeetingWindowResolver.findActiveWindow(filteredInstances, now)

                val next = calendarRepository.getNextInstance(
                    now = now,
                    selectedCalendarIds = state.selectedCalendarIds,
                    busyOnly = state.busyOnly,
                    ignoreAllDay = state.ignoreAllDay,
                    skipRecurring = state.skipRecurring,
                    selectedDaysEnabled = state.selectedDaysEnabled,
                    selectedDaysMask = state.selectedDaysMask,
                    minEventMinutes = state.minEventMinutes,
                    requireLocation = state.requireLocation,
                    requireTitleKeyword = state.requireTitleKeyword,
                    titleKeyword = state.titleKeyword,
                    titleKeywordMatchMode = state.titleKeywordMatchMode,
                    titleKeywordCaseSensitive = state.titleKeywordCaseSensitive,
                    titleKeywordMatchAll = state.titleKeywordMatchAll,
                    titleKeywordExclude = state.titleKeywordExclude
                )

                // Find events overlapping with the next instance
                val nextOverlapping = if (next != null) {
                    calendarRepository.getInstancesInRange(
                        beginMs = next.begin,
                        endMs = next.end,
                        selectedCalendarIds = state.selectedCalendarIds,
                        busyOnly = state.busyOnly,
                        ignoreAllDay = state.ignoreAllDay,
                        skipRecurring = state.skipRecurring,
                        selectedDaysEnabled = state.selectedDaysEnabled,
                        selectedDaysMask = state.selectedDaysMask,
                        minEventMinutes = state.minEventMinutes,
                        requireLocation = state.requireLocation,
                        requireTitleKeyword = state.requireTitleKeyword,
                        titleKeyword = state.titleKeyword,
                        titleKeywordMatchMode = state.titleKeywordMatchMode,
                        titleKeywordCaseSensitive = state.titleKeywordCaseSensitive,
                        titleKeywordMatchAll = state.titleKeywordMatchAll,
                        titleKeywordExclude = state.titleKeywordExclude
                    )
                } else {
                    emptyList()
                }

                CalendarData(window, windowRaw, next, nextOverlapping)
            }

            _calendarData.value = data
        }
    }

    private fun buildUiState(
        combined: CombinedData,
        permissions: PermissionState,
        isRefreshing: Boolean
    ): StatusUiState {
        val core = combined.settings.core
        val filter1 = combined.settings.filter1
        val filter2 = combined.settings.filter2
        val filter3 = combined.settings.filter3
        val debug = combined.settings.debug
        val runtime1 = combined.runtime1
        val runtime2 = combined.runtime2
        val calendar = combined.calendar

        return StatusUiState(
            // Core settings
            automationEnabled = core.automationEnabled,
            dndMode = core.dndMode,
            dndStartOffsetMinutes = core.dndStartOffsetMinutes,
            oneTimeActionConfirmation = core.oneTimeActionConfirmation,
            onboardingCompleted = core.onboardingCompleted,

            // Filter settings
            selectedCalendarIds = filter1.selectedCalendarIds,
            busyOnly = filter1.busyOnly,
            ignoreAllDay = filter1.ignoreAllDay,
            skipRecurring = filter1.skipRecurring,
            selectedDaysEnabled = filter1.selectedDaysEnabled,
            selectedDaysMask = filter2.selectedDaysMask,
            minEventMinutes = filter2.minEventMinutes,
            requireLocation = filter2.requireLocation,
            requireTitleKeyword = filter2.requireTitleKeyword,
            titleKeyword = filter2.titleKeyword,
            titleKeywordMatchMode = filter3.titleKeywordMatchMode,
            titleKeywordCaseSensitive = filter3.titleKeywordCaseSensitive,
            titleKeywordMatchAll = filter3.titleKeywordMatchAll,
            titleKeywordExclude = filter3.titleKeywordExclude,

            // Debug settings
            themeDebugMode = debug.themeDebugMode,
            eventHighlightPreset = debug.eventHighlightPreset,
            eventHighlightBarHidden = debug.eventHighlightBarHidden,

            // Runtime state
            dndSetByApp = runtime1.dndSetByApp,
            userSuppressedUntilMs = runtime1.userSuppressedUntilMs,
            userSuppressedFromMs = runtime1.userSuppressedFromMs,
            manualEventStartMs = runtime1.manualEventStartMs,
            manualEventEndMs = runtime1.manualEventEndMs,
            skippedEventId = runtime2.skippedEventId,
            skippedEventBeginMs = runtime2.skippedEventBeginMs,
            skippedEventEndMs = runtime2.skippedEventEndMs,

            // Permissions
            hasCalendarPermission = permissions.hasCalendar,
            hasPolicyAccess = permissions.hasPolicyAccess,
            canScheduleExactAlarms = permissions.canScheduleExactAlarms,

            // Calendar data
            activeWindow = calendar.activeWindow,
            activeWindowRaw = calendar.activeWindowRaw,
            nextInstance = calendar.nextInstance,
            nextOverlappingEvents = calendar.nextOverlappingEvents,

            // UI state
            isRefreshing = isRefreshing
        )
    }

    // Internal data classes for combining flows
    private data class CoreSettings(
        val automationEnabled: Boolean,
        val dndMode: DndMode,
        val dndStartOffsetMinutes: Int,
        val oneTimeActionConfirmation: Boolean,
        val onboardingCompleted: Boolean
    )

    private data class FilterSettings1(
        val selectedCalendarIds: Set<String>,
        val busyOnly: Boolean,
        val ignoreAllDay: Boolean,
        val skipRecurring: Boolean,
        val selectedDaysEnabled: Boolean
    )

    private data class FilterSettings2(
        val selectedDaysMask: Int,
        val minEventMinutes: Int,
        val requireLocation: Boolean,
        val requireTitleKeyword: Boolean,
        val titleKeyword: String
    )

    private data class FilterSettings3(
        val titleKeywordMatchMode: KeywordMatchMode,
        val titleKeywordCaseSensitive: Boolean,
        val titleKeywordMatchAll: Boolean,
        val titleKeywordExclude: Boolean
    )

    private data class DebugSettings(
        val themeDebugMode: ThemeDebugMode,
        val eventHighlightPreset: EventHighlightPreset,
        val eventHighlightBarHidden: Boolean
    )

    private data class AllSettings(
        val core: CoreSettings,
        val filter1: FilterSettings1,
        val filter2: FilterSettings2,
        val filter3: FilterSettings3,
        val debug: DebugSettings
    )

    private data class RuntimeState1(
        val dndSetByApp: Boolean,
        val userSuppressedUntilMs: Long,
        val userSuppressedFromMs: Long,
        val manualEventStartMs: Long,
        val manualEventEndMs: Long
    )

    private data class RuntimeState2(
        val skippedEventId: Long,
        val skippedEventBeginMs: Long,
        val skippedEventEndMs: Long
    )

    private data class CalendarData(
        val activeWindow: MeetingWindow? = null,
        val activeWindowRaw: MeetingWindow? = null,
        val nextInstance: EventInstance? = null,
        val nextOverlappingEvents: List<EventInstance> = emptyList()
    )

    private data class PermissionState(
        val hasCalendar: Boolean = false,
        val hasPolicyAccess: Boolean = false,
        val canScheduleExactAlarms: Boolean = false
    )

    private data class CombinedData(
        val settings: AllSettings,
        val runtime1: RuntimeState1,
        val runtime2: RuntimeState2,
        val calendar: CalendarData
    )

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StatusViewModel(
                context = context.applicationContext,
                settingsStore = SettingsStore(context),
                runtimeStateStore = RuntimeStateStore(context),
                calendarRepository = CalendarRepository(context),
                dndController = DndController(context),
                alarmScheduler = AlarmScheduler(context)
            ) as T
        }
    }
}
