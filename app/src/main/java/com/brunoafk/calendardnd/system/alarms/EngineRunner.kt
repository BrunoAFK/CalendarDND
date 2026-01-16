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
import com.brunoafk.calendardnd.system.workers.Workers
import com.brunoafk.calendardnd.util.AndroidTimeFormatter
import com.brunoafk.calendardnd.util.AnalyticsTracker
import com.brunoafk.calendardnd.util.DebugLogger
import com.brunoafk.calendardnd.util.EngineConstants.PRE_DND_NOTIFICATION_LEAD_MS
import com.brunoafk.calendardnd.util.EngineConstants.PRE_DND_NOTIFICATION_MIN_DELAY_MS
import com.brunoafk.calendardnd.util.PermissionUtils
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

            // Gather input
            val input = EngineInput(
                trigger = trigger,
                now = now,
                automationEnabled = settingsSnapshot.automationEnabled,
                selectedCalendarIds = settingsSnapshot.selectedCalendarIds,
                busyOnly = settingsSnapshot.busyOnly,
                ignoreAllDay = settingsSnapshot.ignoreAllDay,
                minEventMinutes = settingsSnapshot.minEventMinutes,
                dndMode = settingsSnapshot.dndMode,
                dndStartOffsetMinutes = settingsSnapshot.dndStartOffsetMinutes,
                preDndNotificationEnabled = settingsSnapshot.preDndNotificationEnabled,
                dndSetByApp = runtimeSnapshot.dndSetByApp,
                activeWindowEndMs = runtimeSnapshot.activeWindowEndMs,
                userSuppressedUntilMs = runtimeSnapshot.userSuppressedUntilMs,
                manualDndUntilMs = runtimeSnapshot.manualDndUntilMs,
                lastKnownDndFilter = runtimeSnapshot.lastKnownDndFilter,
                hasCalendarPermission = hasCalendarPermission(context),
                hasPolicyAccess = dndController.hasPolicyAccess(),
                hasExactAlarms = alarmScheduler.canScheduleExactAlarms(),
                systemDndIsOn = dndController.isDndOn(),
                currentSystemFilter = normalizedFilter
            )

            val includeDetailedLogs = settingsStore.debugLogIncludeDetails.first()

            // Run engine
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

            if (dryRun) {
                return
            }

            // Apply decisions
            val decision = output.decision

            // DND changes
            if (decision.shouldEnableDnd) {
                val beforeFilter = dndController.getCurrentFilterName()
                val success = dndController.enableDnd(input.dndMode)
                val afterFilter = dndController.getCurrentFilterName()
                debugLogStore.appendLog(
                    DebugLogLevel.INFO,
                    "DND Enable: mode=${input.dndMode.name} | before=$beforeFilter | after=$afterFilter | success=$success"
                )
                if (success) {
                    runtimeStateStore.setLastKnownDndFilter(input.dndMode.filterValue)
                }
            } else if (decision.shouldDisableDnd) {
                val beforeFilter = dndController.getCurrentFilterName()
                val success = dndController.disableDnd()
                val afterFilter = dndController.getCurrentFilterName()
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
                    MeetingOverrunNotificationHelper.showOverrunNotification(context)
                }
            }

            // State updates
            decision.setDndSetByApp?.let { runtimeStateStore.setDndSetByApp(it) }
            decision.setUserSuppressedUntil?.let { runtimeStateStore.setUserSuppressedUntilMs(it) }
            decision.setActiveWindowEnd?.let { runtimeStateStore.setActiveWindowEndMs(it) }
            decision.setManualDndUntilMs?.let { runtimeStateStore.setManualDndUntilMs(it) }

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
                    val notifyAtMs = nextDndStartMs - PRE_DND_NOTIFICATION_LEAD_MS
                    if (notifyAtMs > System.currentTimeMillis() + PRE_DND_NOTIFICATION_MIN_DELAY_MS) {
                        alarmScheduler.schedulePreDndNotificationAlarm(
                            triggerAtMs = notifyAtMs,
                            meetingTitle = output.nextInstance?.title,
                            dndWindowEndMs = output.dndWindowEndMs
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

            // Ensure sanity worker if automation enabled
            if (input.automationEnabled) {
                Workers.ensureSanityWorker(context)
            } else {
                Workers.cancelAllWork(context)
            }

            // Log
        } catch (e: Exception) {
            e.printStackTrace()
            debugLogStore.appendLog(DebugLogLevel.ERROR, "ERROR: ${e.message}")
        }
    }

    private fun hasCalendarPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

}
