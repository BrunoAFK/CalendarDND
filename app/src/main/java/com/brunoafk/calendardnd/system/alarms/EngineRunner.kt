package com.brunoafk.calendardnd.system.alarms

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.brunoafk.calendardnd.data.calendar.CalendarRepository
import com.brunoafk.calendardnd.data.dnd.DndController
import com.brunoafk.calendardnd.data.prefs.DebugLogLevel
import com.brunoafk.calendardnd.data.prefs.DebugLogStore
import com.brunoafk.calendardnd.data.prefs.RuntimeStateStore
import com.brunoafk.calendardnd.data.prefs.SettingsStore
import com.brunoafk.calendardnd.domain.engine.AutomationEngine
import com.brunoafk.calendardnd.domain.engine.EngineInput
import com.brunoafk.calendardnd.domain.model.Trigger
import com.brunoafk.calendardnd.system.notifications.MeetingOverrunNotificationHelper
import com.brunoafk.calendardnd.system.notifications.NewEventBeforeSkippedNotificationHelper
import com.brunoafk.calendardnd.system.workers.Workers
import com.brunoafk.calendardnd.util.AndroidTimeFormatter
import com.brunoafk.calendardnd.util.AnalyticsTracker
import com.brunoafk.calendardnd.util.AppConfig
import com.brunoafk.calendardnd.util.DebugLogger
import com.brunoafk.calendardnd.util.EngineConstants.PRE_DND_NOTIFICATION_MIN_DELAY_MS
import com.brunoafk.calendardnd.util.PermissionUtils
import com.brunoafk.calendardnd.util.PerformanceTrace
import com.brunoafk.calendardnd.util.PerformanceTracer
import kotlinx.coroutines.flow.first

/**
 * Centralized engine runner used by all background components
 */
object EngineRunner {

    private const val TAG = "EngineRunner"

    suspend fun runEngine(context: Context, trigger: Trigger, dryRun: Boolean = false) {
        val startTime = System.currentTimeMillis()
        val settingsStore = SettingsStore(context)
        val runtimeStateStore = RuntimeStateStore(context)
        val debugLogStore = DebugLogStore(context)
        val dndController = DndController(context)
        val calendarRepository = CalendarRepository(context)
        val alarmScheduler = AlarmScheduler(context)
        val engine = AutomationEngine(calendarRepository, AndroidTimeFormatter(context))

        try {
            val settingsSnapshot = settingsStore.getSnapshot()
            val runtimeSnapshot = runtimeStateStore.getSnapshot()
            val now = System.currentTimeMillis()
            val currentFilter = dndController.getCurrentFilter()
            val normalizedFilter = if (currentFilter == NotificationManager.INTERRUPTION_FILTER_UNKNOWN) {
                -1
            } else {
                currentFilter
            }
            val perfEnabled = AppConfig.crashlyticsEnabled && settingsSnapshot.crashlyticsOptIn
            val hasNotifications = PermissionUtils.hasNotificationPermission(context)

            // Gather input
            val input = EngineInput(
                trigger = trigger,
                now = now,
                automationEnabled = settingsSnapshot.automationEnabled,
                selectedCalendarIds = settingsSnapshot.selectedCalendarIds,
                busyOnly = settingsSnapshot.busyOnly,
                ignoreAllDay = settingsSnapshot.ignoreAllDay,
                skipRecurring = settingsSnapshot.skipRecurring,
                selectedDaysEnabled = settingsSnapshot.selectedDaysEnabled,
                selectedDaysMask = settingsSnapshot.selectedDaysMask,
                minEventMinutes = settingsSnapshot.minEventMinutes,
                requireLocation = settingsSnapshot.requireLocation,
                dndMode = settingsSnapshot.dndMode,
                dndStartOffsetMinutes = settingsSnapshot.dndStartOffsetMinutes,
                preDndNotificationEnabled = settingsSnapshot.preDndNotificationEnabled && hasNotifications,
                preDndNotificationLeadMinutes = settingsSnapshot.preDndNotificationLeadMinutes,
                requireTitleKeyword = settingsSnapshot.requireTitleKeyword,
                titleKeyword = settingsSnapshot.titleKeyword,
                titleKeywordMatchMode = settingsSnapshot.titleKeywordMatchMode,
                titleKeywordCaseSensitive = settingsSnapshot.titleKeywordCaseSensitive,
                titleKeywordMatchAll = settingsSnapshot.titleKeywordMatchAll,
                titleKeywordExclude = settingsSnapshot.titleKeywordExclude,
                postMeetingNotificationEnabled = settingsSnapshot.postMeetingNotificationEnabled && hasNotifications,
                postMeetingNotificationOffsetMinutes = settingsSnapshot.postMeetingNotificationOffsetMinutes,
                dndSetByApp = runtimeSnapshot.dndSetByApp,
                activeWindowEndMs = runtimeSnapshot.activeWindowEndMs,
                userSuppressedUntilMs = runtimeSnapshot.userSuppressedUntilMs,
                userSuppressedFromMs = runtimeSnapshot.userSuppressedFromMs,
                manualDndUntilMs = runtimeSnapshot.manualDndUntilMs,
                manualEventStartMs = runtimeSnapshot.manualEventStartMs,
                manualEventEndMs = runtimeSnapshot.manualEventEndMs,
                lastKnownDndFilter = runtimeSnapshot.lastKnownDndFilter,
                skippedEventBeginMs = runtimeSnapshot.skippedEventBeginMs,
                notifiedNewEventBeforeSkip = runtimeSnapshot.notifiedNewEventBeforeSkip,
                hasCalendarPermission = hasCalendarPermission(context),
                hasPolicyAccess = dndController.hasPolicyAccess(),
                hasExactAlarms = alarmScheduler.canScheduleExactAlarms(),
                systemDndIsOn = dndController.isDndOn(),
                currentSystemFilter = normalizedFilter
            )

            val includeDetailedLogs = settingsStore.debugLogIncludeDetails.first()

            // Run engine
            val engineTrace = startEngineTraceIfEnabled(perfEnabled, input)
            val output = engine.run(input)
            val executionTime = System.currentTimeMillis() - startTime

            val detailedLog = DebugLogger.buildEngineLog(
                context = context,
                trigger = trigger,
                input = input,
                output = output,
                executionTimeMs = executionTime
            )
            val baseLogMessage = if (includeDetailedLogs) {
                detailedLog
            } else {
                DebugLogger.buildCompactLog(
                    context = context,
                    trigger = trigger,
                    input = input,
                    output = output
                )
            }
            val logMessage = if (dryRun) {
                "DRY RUN\n$baseLogMessage"
            } else {
                baseLogMessage
            }
            debugLogStore.appendLog(DebugLogLevel.INFO, logMessage)

            val compactLog = DebugLogger.buildCompactLog(
                context = context,
                trigger = trigger,
                input = input,
                output = output
            )
            Log.d(TAG, if (dryRun) "DRY RUN | $compactLog" else compactLog)

            finishEngineTrace(engineTrace, output)

            if (dryRun) {
                return
            }

            // Apply decisions
            val decision = output.decision

            // DND changes
            if (decision.shouldEnableDnd) {
                val dndTrace = startDndTraceIfEnabled(perfEnabled, "dnd_enable", input)
                val beforeFilter = dndController.getCurrentFilterName()
                val success = dndController.enableDnd(input.dndMode)
                val afterFilter = dndController.getCurrentFilterName()
                finishDndTrace(dndTrace, success)
                debugLogStore.appendLog(
                    DebugLogLevel.INFO,
                    "DND Enable: mode=${input.dndMode.name} | before=$beforeFilter | after=$afterFilter | success=$success"
                )
                if (success) {
                    runtimeStateStore.setLastKnownDndFilter(input.dndMode.filterValue)
                }
            } else if (decision.shouldDisableDnd) {
                val dndTrace = startDndTraceIfEnabled(perfEnabled, "dnd_disable", input)
                val beforeFilter = dndController.getCurrentFilterName()
                val success = dndController.disableDnd()
                val afterFilter = dndController.getCurrentFilterName()
                finishDndTrace(dndTrace, success)
                debugLogStore.appendLog(
                    DebugLogLevel.INFO,
                    "DND Disable: before=$beforeFilter | after=$afterFilter | success=$success"
                )
                if (success) {
                    runtimeStateStore.setLastKnownDndFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                }
            }

            val action = when {
                decision.shouldEnableDnd -> "enable_dnd"
                decision.shouldDisableDnd -> "disable_dnd"
                else -> "no_change"
            }
            AnalyticsTracker.logEngineRun(
                context = context,
                trigger = input.trigger.name,
                action = action,
                exactAlarms = input.hasExactAlarms
            )

            // Check for meeting overrun notification
            if (decision.notificationNeeded == com.brunoafk.calendardnd.domain.engine.NotificationNeeded.MEETING_OVERRUN) {
                if (PermissionUtils.hasNotificationPermission(context)) {
                    MeetingOverrunNotificationHelper.showOverrunNotification(
                        context,
                        silent = settingsSnapshot.postMeetingNotificationSilent
                    )
                }
            }

            // Check for new event before skipped notification
            if (decision.notificationNeeded == com.brunoafk.calendardnd.domain.engine.NotificationNeeded.NEW_EVENT_BEFORE_SKIPPED) {
                if (PermissionUtils.hasNotificationPermission(context)) {
                    NewEventBeforeSkippedNotificationHelper.showNotification(
                        context,
                        eventTitle = output.nextInstance?.title
                    )
                }
            }

            // State updates
            decision.setDndSetByApp?.let { runtimeStateStore.setDndSetByApp(it) }
            decision.setUserSuppressedUntil?.let { runtimeStateStore.setUserSuppressedUntilMs(it) }
            decision.setUserSuppressedFromMs?.let { runtimeStateStore.setUserSuppressedFromMs(it) }
            decision.setActiveWindowEnd?.let { runtimeStateStore.setActiveWindowEndMs(it) }
            decision.setManualDndUntilMs?.let { runtimeStateStore.setManualDndUntilMs(it) }
            decision.setManualEventStartMs?.let { runtimeStateStore.setManualEventStartMs(it) }
            decision.setManualEventEndMs?.let { runtimeStateStore.setManualEventEndMs(it) }
            decision.setSkippedEventBeginMs?.let { runtimeStateStore.setSkippedEventBeginMs(it) }
            decision.setNotifiedNewEventBeforeSkip?.let { runtimeStateStore.setNotifiedNewEventBeforeSkip(it) }

            // Update last run time
            runtimeStateStore.setLastEngineRunMs(System.currentTimeMillis())

            // Scheduling
            val schedulePlan = output.schedulePlan
            if (schedulePlan != null) {
                if (schedulePlan.nextBoundaryMs != null) {
                    if (input.hasExactAlarms) {
                        // Schedule exact alarm
                        alarmScheduler.scheduleBoundaryAlarm(schedulePlan.nextBoundaryMs)
                        runtimeStateStore.setLastPlannedBoundaryMs(schedulePlan.nextBoundaryMs)
                    }

                    // Schedule near-term guards if needed
                    if (schedulePlan.needsNearTermGuards) {
                        schedulePlan.guardBeforeMs?.let {
                            Workers.scheduleNearTermGuard(context, it, true)
                        }
                        schedulePlan.guardAfterMs?.let {
                            Workers.scheduleNearTermGuard(context, it, false)
                        }
                    }
                } else {
                    // No boundary - cancel alarms
                    alarmScheduler.cancelBoundaryAlarm()
                    runtimeStateStore.setLastPlannedBoundaryMs(0L)
                }
            } else {
                // No plan - cancel everything
                alarmScheduler.cancelBoundaryAlarm()
                runtimeStateStore.setLastPlannedBoundaryMs(0L)
            }

            // Pre-DND notification scheduling (5 minutes before DND starts)
            if (input.automationEnabled &&
                input.preDndNotificationEnabled &&
                PermissionUtils.hasNotificationPermission(context)
            ) {
                val nextDndStartMs = output.nextDndStartMs
                if (nextDndStartMs != null) {
                    val leadMs = input.preDndNotificationLeadMinutes
                        .coerceIn(0, 10)
                        .toLong() * 60 * 1000L
                    val notifyAtMs = nextDndStartMs - leadMs
                    if (notifyAtMs > System.currentTimeMillis() + PRE_DND_NOTIFICATION_MIN_DELAY_MS) {
                        alarmScheduler.schedulePreDndNotificationAlarm(
                            triggerAtMs = notifyAtMs,
                            meetingTitle = output.nextInstance?.title,
                            dndWindowEndMs = output.dndWindowEndMs,
                            dndWindowStartMs = output.nextDndStartMs
                        )
                    } else {
                        alarmScheduler.cancelPreDndNotificationAlarm()
                    }
                } else {
                    alarmScheduler.cancelPreDndNotificationAlarm()
                }
            } else {
                alarmScheduler.cancelPreDndNotificationAlarm()
            }

            // Post-meeting notification check scheduling
            val postMeetingOffsetMinutes = settingsSnapshot.postMeetingNotificationOffsetMinutes
            val postMeetingEnabled = input.automationEnabled &&
                settingsSnapshot.postMeetingNotificationEnabled
            val postMeetingWindowEndMs = when {
                output.dndWindowEndMs != null && output.dndWindowEndMs > 0L -> output.dndWindowEndMs
                input.activeWindowEndMs > 0L -> input.activeWindowEndMs
                else -> null
            }

            if (postMeetingEnabled && postMeetingWindowEndMs != null) {
                val triggerAtMs = postMeetingWindowEndMs +
                    postMeetingOffsetMinutes.toLong() * 60 * 1000L
                if (triggerAtMs > System.currentTimeMillis() + PRE_DND_NOTIFICATION_MIN_DELAY_MS) {
                    alarmScheduler.schedulePostMeetingCheckAlarm(triggerAtMs)
                    debugLogStore.appendLog(
                        DebugLogLevel.INFO,
                        "Post-meeting check scheduled at ${AndroidTimeFormatter(context).formatTime(triggerAtMs)} " +
                            "(offset=${postMeetingOffsetMinutes}m)"
                    )
                } else {
                    alarmScheduler.cancelPostMeetingCheckAlarm()
                    debugLogStore.appendLog(
                        DebugLogLevel.INFO,
                        "Post-meeting check canceled (too soon for offset=${postMeetingOffsetMinutes}m)"
                    )
                }
            } else {
                alarmScheduler.cancelPostMeetingCheckAlarm()
                debugLogStore.appendLog(
                    DebugLogLevel.INFO,
                    "Post-meeting check canceled (enabled=$postMeetingEnabled, windowEnd=$postMeetingWindowEndMs)"
                )
            }

            // Ensure sanity worker if automation enabled
            if (input.automationEnabled) {
                Workers.ensureSanityWorker(context)
            } else {
                Workers.cancelAllWork(context)
            }

            // Log
        } catch (e: Exception) {
            Log.e(TAG, "Engine run failed", e)
            debugLogStore.appendLog(DebugLogLevel.ERROR, "ERROR: ${e.message}")
        }
    }

    private fun hasCalendarPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startEngineTraceIfEnabled(enabled: Boolean, input: EngineInput): PerformanceTrace? {
        if (!enabled) {
            return null
        }
        return PerformanceTracer.newTrace("engine_run")?.apply {
            putAttribute("trigger", input.trigger.name)
            putAttribute("exact_alarms", input.hasExactAlarms.toString())
            putAttribute("automation_enabled", input.automationEnabled.toString())
            putAttribute("calendar_scope", if (input.selectedCalendarIds.isEmpty()) "all" else "specific")
            putAttribute("policy_access", input.hasPolicyAccess.toString())
            start()
        }
    }

    private fun finishEngineTrace(
        trace: PerformanceTrace?,
        output: com.brunoafk.calendardnd.domain.engine.EngineOutput
    ) {
        if (trace == null) {
            return
        }
        val decision = output.decision
        trace.incrementMetric("dnd_enable", if (decision.shouldEnableDnd) 1 else 0)
        trace.incrementMetric("dnd_disable", if (decision.shouldDisableDnd) 1 else 0)
        trace.incrementMetric("no_change", if (!decision.shouldEnableDnd && !decision.shouldDisableDnd) 1 else 0)
        trace.incrementMetric(
            "missing_permissions",
            if (decision.notificationNeeded == com.brunoafk.calendardnd.domain.engine.NotificationNeeded.SETUP_REQUIRED) 1 else 0
        )
        trace.incrementMetric("user_override", if (output.userOverrideDetected) 1 else 0)
        trace.incrementMetric(
            "meeting_overrun",
            if (decision.notificationNeeded == com.brunoafk.calendardnd.domain.engine.NotificationNeeded.MEETING_OVERRUN) 1 else 0
        )
        trace.stop()
    }

    private fun startDndTraceIfEnabled(
        enabled: Boolean,
        traceName: String,
        input: EngineInput
    ): PerformanceTrace? {
        if (!enabled) {
            return null
        }
        return PerformanceTracer.newTrace(traceName)?.apply {
            putAttribute("mode", input.dndMode.name)
            putAttribute("policy_access", input.hasPolicyAccess.toString())
            start()
        }
    }

    private fun finishDndTrace(trace: PerformanceTrace?, success: Boolean) {
        if (trace == null) {
            return
        }
        trace.incrementMetric("success", if (success) 1 else 0)
        trace.stop()
    }

}
