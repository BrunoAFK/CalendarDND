package com.brunoafk.calendardnd.domain.engine

import com.brunoafk.calendardnd.data.calendar.ICalendarRepository
import com.brunoafk.calendardnd.domain.model.MeetingWindow
import java.security.MessageDigest
import com.brunoafk.calendardnd.domain.planning.MeetingWindowResolver
import com.brunoafk.calendardnd.domain.planning.SchedulePlanner
import com.brunoafk.calendardnd.domain.planning.SchedulePlanner.SchedulePlan
import com.brunoafk.calendardnd.util.EngineConstants.ACTIVE_INSTANCES_WINDOW_MS
import com.brunoafk.calendardnd.util.EngineConstants.MEETING_GAP_THRESHOLD_MS
import com.brunoafk.calendardnd.util.EngineConstants.MEETING_OVERRUN_THRESHOLD_MS

/**
 * Core automation engine - makes all DND decisions
 * This is the brain of the app
 */
class AutomationEngine(
    private val calendarRepository: ICalendarRepository,
    private val timeFormatter: TimeFormatter = DefaultTimeFormatter()
) {

    /**
     * Main entry point - run the engine with given input
     */
    suspend fun run(input: EngineInput): EngineOutput {
        val now = input.now

        // RULE 1: If automation is OFF
        if (!input.automationEnabled) {
            return handleAutomationOff(input)
        }

        // RULE 2: If missing required permissions
        if (!input.hasCalendarPermission || !input.hasPolicyAccess) {
            return handleMissingPermissions(input)
        }

        // Get calendar instances around now to resolve merged active windows.
        val instancesAroundNow = calendarRepository.getInstancesInRange(
            beginMs = now - ACTIVE_INSTANCES_WINDOW_MS,
            endMs = now + ACTIVE_INSTANCES_WINDOW_MS,
            selectedCalendarIds = input.selectedCalendarIds,
            busyOnly = input.busyOnly,
            ignoreAllDay = input.ignoreAllDay,
            skipRecurring = input.skipRecurring,
            selectedDaysEnabled = input.selectedDaysEnabled,
            selectedDaysMask = input.selectedDaysMask,
            minEventMinutes = input.minEventMinutes,
            requireLocation = input.requireLocation,
            requireTitleKeyword = input.requireTitleKeyword,
            titleKeyword = input.titleKeyword,
            titleKeywordMatchMode = input.titleKeywordMatchMode,
            titleKeywordCaseSensitive = input.titleKeywordCaseSensitive,
            titleKeywordMatchAll = input.titleKeywordMatchAll,
            titleKeywordExclude = input.titleKeywordExclude
        )

        val activeWindow = MeetingWindowResolver.findActiveWindow(instancesAroundNow, now)

        val nextInstance = calendarRepository.getNextInstance(
            now,
            input.selectedCalendarIds,
            input.busyOnly,
            input.ignoreAllDay,
            input.skipRecurring,
            input.selectedDaysEnabled,
            input.selectedDaysMask,
            input.minEventMinutes,
            input.requireLocation,
            input.requireTitleKeyword,
            input.titleKeyword,
            input.titleKeywordMatchMode,
            input.titleKeywordCaseSensitive,
            input.titleKeywordMatchAll,
            input.titleKeywordExclude
        )

        val dndWindow = resolveDndWindow(
            now = now,
            activeWindow = activeWindow,
            nextInstance = nextInstance,
            offsetMinutes = input.dndStartOffsetMinutes,
            manualUntilMs = input.manualDndUntilMs
        )

        // Check for user override (user manually turned off DND during DND window)
        val userOverrideDetected = detectUserOverride(input, dndWindow.isActive)

        val suppressionActive = input.userSuppressedUntilMs > 0L &&
            now >= input.userSuppressedFromMs &&
            now < input.userSuppressedUntilMs
        val isSuppressed = suppressionActive || userOverrideDetected

        // Check for new event before skipped event
        val newEventBeforeSkipped = detectNewEventBeforeSkipped(input, nextInstance)

        // Make the decision
        val decision = makeDecision(input, dndWindow, isSuppressed, userOverrideDetected, nextInstance, newEventBeforeSkipped)

        // Plan next schedule
        val schedulePlan = SchedulePlanner.planNextSchedule(
            now,
            dndWindow.startMs,
            dndWindow.endMs,
            dndWindow.isActive,
            input.hasExactAlarms
        )

        val logMessage = buildLogMessage(
            input,
            activeWindow,
            nextInstance,
            dndWindow,
            decision,
            schedulePlan
        )

        return EngineOutput(
            decision = decision,
            activeWindow = activeWindow,
            nextInstance = nextInstance,
            schedulePlan = schedulePlan,
            nextDndStartMs = dndWindow.nextStartMs,
            dndWindowEndMs = dndWindow.endMs,
            logMessage = logMessage,
            userOverrideDetected = userOverrideDetected
        )
    }

    private fun handleAutomationOff(input: EngineInput): EngineOutput {
        val now = input.now
        val manualStart = input.manualEventStartMs
        val manualEnd = input.manualEventEndMs
        val hasManualEvent = manualStart > 0L && manualEnd > 0L && manualEnd > now
        val shouldClearManualEvent = manualStart > 0L && manualEnd > 0L && manualEnd <= now

        if (hasManualEvent) {
            val isActive = now in manualStart until manualEnd
            val dndWindow = DndWindow(
                startMs = manualStart,
                endMs = manualEnd,
                isActive = isActive,
                nextStartMs = if (!isActive && manualStart > now) manualStart else null
            )
            val schedulePlan = SchedulePlanner.planNextSchedule(
                now,
                dndWindow.startMs,
                dndWindow.endMs,
                dndWindow.isActive,
                input.hasExactAlarms
            )
            val shouldEnable = dndWindow.isActive && !input.systemDndIsOn
            val shouldDisable = !dndWindow.isActive && input.dndSetByApp
            val baseDecision = Decision(
                shouldEnableDnd = shouldEnable,
                shouldDisableDnd = shouldDisable,
                setDndSetByApp = when {
                    shouldEnable -> true
                    shouldDisable -> false
                    else -> null
                },
                setUserSuppressedUntil = null,
                setUserSuppressedFromMs = null,
                setActiveWindowEnd = if (dndWindow.isActive) dndWindow.endMs else 0L,
                setManualDndUntilMs = 0L,
                setManualEventStartMs = null,
                setManualEventEndMs = null,
                setSkippedEventBeginMs = null,
                setNotifiedNewEventBeforeSkip = null,
                notificationNeeded = NotificationNeeded.NONE
            )
            val decision = applySuppressionClear(baseDecision, input)

            return EngineOutput(
                decision = decision,
                activeWindow = null,
                nextInstance = null,
                schedulePlan = schedulePlan,
                nextDndStartMs = dndWindow.nextStartMs,
                dndWindowEndMs = dndWindow.endMs,
                logMessage = "Automation OFF - manual event window scheduled",
                userOverrideDetected = false
            )
        }

        val baseDecision = Decision(
            shouldEnableDnd = false,
            shouldDisableDnd = input.dndSetByApp, // Turn off only if we own it
            setDndSetByApp = if (input.dndSetByApp) false else null,
            setUserSuppressedUntil = null,
            setUserSuppressedFromMs = null,
            setActiveWindowEnd = 0L,
            setManualDndUntilMs = 0L,
            setManualEventStartMs = if (shouldClearManualEvent) 0L else null,
            setManualEventEndMs = if (shouldClearManualEvent) 0L else null,
            setSkippedEventBeginMs = null,
            setNotifiedNewEventBeforeSkip = null,
            notificationNeeded = NotificationNeeded.NONE
        )
        val decision = applySuppressionClear(baseDecision, input)

        return EngineOutput(
            decision = decision,
            activeWindow = null,
            nextInstance = null,
            schedulePlan = null,
            nextDndStartMs = null,
            dndWindowEndMs = null,
            logMessage = "Automation OFF - clearing state",
            userOverrideDetected = false
        )
    }

    private fun handleMissingPermissions(input: EngineInput): EngineOutput {
        val decision = Decision(
            shouldEnableDnd = false,
            shouldDisableDnd = false,
            setDndSetByApp = null,
            setUserSuppressedUntil = null,
            setUserSuppressedFromMs = null,
            setActiveWindowEnd = null,
            setManualDndUntilMs = 0L,
            setManualEventStartMs = null,
            setManualEventEndMs = null,
            setSkippedEventBeginMs = null,
            setNotifiedNewEventBeforeSkip = null,
            notificationNeeded = NotificationNeeded.SETUP_REQUIRED
        )

        return EngineOutput(
            decision = decision,
            activeWindow = null,
            nextInstance = null,
            schedulePlan = null,
            nextDndStartMs = null,
            dndWindowEndMs = null,
            logMessage = "Missing permissions - setup required",
            userOverrideDetected = false
        )
    }

    private fun detectUserOverride(input: EngineInput, dndWindowActive: Boolean): Boolean {
        if (!input.dndSetByApp || !dndWindowActive) {
            return false
        }

        if (!input.systemDndIsOn) {
            return true
        }

        val expectedFilter = input.dndMode.filterValue
        val lastKnownFilter = input.lastKnownDndFilter
        val currentFilter = input.currentSystemFilter

        if (lastKnownFilter >= 0 && currentFilter >= 0 &&
            lastKnownFilter == expectedFilter && currentFilter != expectedFilter
        ) {
            return true
        }

        return false
    }

    private fun detectNewEventBeforeSkipped(
        input: EngineInput,
        nextInstance: com.brunoafk.calendardnd.domain.model.EventInstance?
    ): Boolean {
        // Only detect if automation is ON and there's an active suppression (skip)
        if (!input.automationEnabled) return false
        if (input.userSuppressedUntilMs <= input.now) return false
        if (input.skippedEventBeginMs <= 0) return false
        if (input.notifiedNewEventBeforeSkip) return false
        if (nextInstance == null) return false

        // Check if the next event starts before the skipped event
        return nextInstance.begin < input.skippedEventBeginMs && nextInstance.begin > input.now
    }

    private fun detectMeetingOverrun(
        now: Long,
        activeWindowEnd: Long,
        nextInstance: com.brunoafk.calendardnd.domain.model.EventInstance?,
        offsetMinutes: Int
    ): Boolean {
        val triggerAt = activeWindowEnd + offsetMinutes.toLong() * 60 * 1000L
        val withinWindow = now >= triggerAt && (now - triggerAt) < MEETING_OVERRUN_THRESHOLD_MS

        // Next meeting should be more than 5 minutes away (or no next meeting)
        val gapToNext = nextInstance?.begin?.let { it - now } ?: Long.MAX_VALUE
        val hasLargeGap = gapToNext > MEETING_GAP_THRESHOLD_MS

        return withinWindow && hasLargeGap && activeWindowEnd > 0
    }

    private fun makeDecision(
        input: EngineInput,
        dndWindow: DndWindow,
        isSuppressed: Boolean,
        userOverrideDetected: Boolean,
        nextInstance: com.brunoafk.calendardnd.domain.model.EventInstance?,
        newEventBeforeSkipped: Boolean
    ): Decision {
        val shouldClearManual = input.manualDndUntilMs > 0L && input.manualDndUntilMs <= input.now
        val manualClearValue = if (shouldClearManual) 0L else null
        val shouldClearSuppression = input.userSuppressedUntilMs > 0L && input.userSuppressedUntilMs <= input.now
        val suppressionClearValue = if (shouldClearSuppression) 0L else null

        // RULE 3: Active DND window and NOT suppressed
        if (dndWindow.isActive && !isSuppressed) {
            val meetingOverrun = input.postMeetingNotificationEnabled &&
                detectMeetingOverrun(input.now, input.activeWindowEndMs, nextInstance, input.postMeetingNotificationOffsetMinutes)
            return applySuppressionClear(
                Decision(
                shouldEnableDnd = !input.systemDndIsOn, // Turn on if not already on
                shouldDisableDnd = false,
                setDndSetByApp = if (!input.systemDndIsOn) true else null,
                setUserSuppressedUntil = null,
                setUserSuppressedFromMs = null,
                setActiveWindowEnd = dndWindow.endMs,
                setManualDndUntilMs = manualClearValue,
                setManualEventStartMs = null,
                setManualEventEndMs = null,
                setSkippedEventBeginMs = null,
                setNotifiedNewEventBeforeSkip = null,
                notificationNeeded = when {
                    meetingOverrun -> NotificationNeeded.MEETING_OVERRUN
                    !input.hasExactAlarms -> NotificationNeeded.DEGRADED_MODE
                    else -> NotificationNeeded.NONE
                }
            ),
                input
            )
        }

        // RULE 4: Active DND window but suppressed (user override)
        if (dndWindow.isActive && isSuppressed) {
            val meetingOverrun = input.postMeetingNotificationEnabled &&
                detectMeetingOverrun(input.now, input.activeWindowEndMs, nextInstance, input.postMeetingNotificationOffsetMinutes)
            val newSuppressedUntil = if (userOverrideDetected) dndWindow.endMs else null

            return applySuppressionClear(
                Decision(
                shouldEnableDnd = false,
                shouldDisableDnd = false,
                setDndSetByApp = if (userOverrideDetected) false else null,
                setUserSuppressedUntil = newSuppressedUntil,
                setUserSuppressedFromMs = if (userOverrideDetected) input.now else null,
                setActiveWindowEnd = dndWindow.endMs,
                setManualDndUntilMs = manualClearValue,
                setManualEventStartMs = null,
                setManualEventEndMs = null,
                setSkippedEventBeginMs = null,
                setNotifiedNewEventBeforeSkip = null,
                notificationNeeded = if (meetingOverrun) {
                    NotificationNeeded.MEETING_OVERRUN
                } else {
                    NotificationNeeded.NONE
                }
            ),
                input
            )
        }

        // RULE 5: No active DND window
        // Check for meeting overrun (meeting just ended, might want to extend)
        val meetingOverrun = input.postMeetingNotificationEnabled &&
            detectMeetingOverrun(input.now, input.activeWindowEndMs, nextInstance, input.postMeetingNotificationOffsetMinutes)
        val keepActiveWindowEnd = input.postMeetingNotificationEnabled &&
            input.postMeetingNotificationOffsetMinutes > 0 &&
            input.activeWindowEndMs > 0 &&
            input.now <= input.activeWindowEndMs +
            input.postMeetingNotificationOffsetMinutes.toLong() * 60 * 1000L +
            MEETING_OVERRUN_THRESHOLD_MS

        // Determine notification needed - prioritize new event before skipped
        val notificationNeeded = when {
            newEventBeforeSkipped -> NotificationNeeded.NEW_EVENT_BEFORE_SKIPPED
            meetingOverrun -> NotificationNeeded.MEETING_OVERRUN
            !input.hasExactAlarms -> NotificationNeeded.DEGRADED_MODE
            else -> NotificationNeeded.NONE
        }

        return applySuppressionClear(
            Decision(
            shouldEnableDnd = false,
            shouldDisableDnd = input.dndSetByApp, // Turn off only if we own it
            setDndSetByApp = if (input.dndSetByApp) false else null,
            setUserSuppressedUntil = suppressionClearValue,
            setUserSuppressedFromMs = suppressionClearValue,
            setActiveWindowEnd = if (keepActiveWindowEnd) input.activeWindowEndMs else 0L,
            setManualDndUntilMs = manualClearValue,
            setManualEventStartMs = null,
            setManualEventEndMs = null,
            setSkippedEventBeginMs = null,
            setNotifiedNewEventBeforeSkip = if (newEventBeforeSkipped) true else null,
            notificationNeeded = notificationNeeded
        ),
            input
        )
    }

    private fun applySuppressionClear(decision: Decision, input: EngineInput): Decision {
        val shouldClearSuppression = input.userSuppressedUntilMs > 0L && input.userSuppressedUntilMs <= input.now
        if (!shouldClearSuppression) {
            return decision
        }
        return decision.copy(
            setUserSuppressedUntil = 0L,
            setUserSuppressedFromMs = 0L,
            setSkippedEventBeginMs = 0L,
            setNotifiedNewEventBeforeSkip = false
        )
    }

    private fun buildLogMessage(
        input: EngineInput,
        activeWindow: MeetingWindow?,
        nextInstance: com.brunoafk.calendardnd.domain.model.EventInstance?,
        dndWindow: DndWindow,
        decision: Decision,
        schedulePlan: SchedulePlan?
    ): String {
        val parts = mutableListOf<String>()

        parts.add("Trigger: ${input.trigger}")

        if (activeWindow != null) {
            parts.add("Active: ${activeWindow.events.size} event(s), ends ${formatTimestamp(activeWindow.end)}")
        } else {
            parts.add("Active: none")
        }

        if (nextInstance != null) {
            parts.add("Next: ${redactTitle(nextInstance.title)} at ${formatTimestamp(nextInstance.begin)}")
        }

        if (dndWindow.startMs != null && dndWindow.endMs != null) {
            val startLabel = if (dndWindow.isActive) "DND active" else "DND starts"
            parts.add("$startLabel: ${formatTimestamp(dndWindow.startMs)} → ${formatTimestamp(dndWindow.endMs)}")
        }

        when {
            decision.shouldEnableDnd -> parts.add("Action: Enable DND (${input.dndMode.name})")
            decision.shouldDisableDnd -> parts.add("Action: Disable DND")
            else -> parts.add("Action: No change")
        }

        if (schedulePlan?.nextBoundaryMs != null) {
            parts.add("Next boundary: ${formatTimestamp(schedulePlan.nextBoundaryMs)}")
        }

        return parts.joinToString(" | ")
    }

    private fun formatTimestamp(ms: Long): String {
        return timeFormatter.formatTime(ms)
    }

    private fun redactTitle(title: String): String {
        if (title.isBlank()) {
            return "(no title)"
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(title.toByteArray(Charsets.UTF_8))
        val hash = digest.joinToString("") { "%02x".format(it) }.take(8)
        return "event#$hash"
    }

    private fun resolveDndWindow(
        now: Long,
        activeWindow: MeetingWindow?,
        nextInstance: com.brunoafk.calendardnd.domain.model.EventInstance?,
        offsetMinutes: Int,
        manualUntilMs: Long
    ): DndWindow {
        if (manualUntilMs > now) {
            return DndWindow(
                startMs = now,
                endMs = manualUntilMs,
                isActive = true,
                nextStartMs = null
            )
        }

        val offsetMs = offsetMinutes * 60_000L

        val meetingStartMs = activeWindow?.begin ?: nextInstance?.begin
        val meetingEndMs = activeWindow?.end ?: nextInstance?.end

        if (meetingStartMs == null || meetingEndMs == null) {
            return DndWindow(null, null, false, null)
        }

        val dndStartMs = meetingStartMs + offsetMs
        if (dndStartMs >= meetingEndMs) {
            return DndWindow(null, null, false, null)
        }

        val active = now in dndStartMs until meetingEndMs
        val nextStart = if (!active && dndStartMs > now) dndStartMs else null

        return DndWindow(
            startMs = dndStartMs,
            endMs = meetingEndMs,
            isActive = active,
            nextStartMs = nextStart
        )
    }

    private data class DndWindow(
        val startMs: Long?,
        val endMs: Long?,
        val isActive: Boolean,
        val nextStartMs: Long?
    )
}
