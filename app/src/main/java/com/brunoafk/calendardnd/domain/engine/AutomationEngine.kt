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

        val isSuppressed = now < input.userSuppressedUntilMs || userOverrideDetected

        // Make the decision
        val decision = makeDecision(input, dndWindow, isSuppressed, userOverrideDetected, nextInstance)

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
        val decision = Decision(
            shouldEnableDnd = false,
            shouldDisableDnd = input.dndSetByApp, // Turn off only if we own it
            setDndSetByApp = if (input.dndSetByApp) false else null,
            setUserSuppressedUntil = null,
            setActiveWindowEnd = 0L,
            setManualDndUntilMs = 0L,
            notificationNeeded = NotificationNeeded.NONE
        )

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
            setActiveWindowEnd = null,
            setManualDndUntilMs = 0L,
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
        nextInstance: com.brunoafk.calendardnd.domain.model.EventInstance?
    ): Decision {
        val shouldClearManual = input.manualDndUntilMs > 0L && input.manualDndUntilMs <= input.now
        val manualClearValue = if (shouldClearManual) 0L else null

        // RULE 3: Active DND window and NOT suppressed
        if (dndWindow.isActive && !isSuppressed) {
            val meetingOverrun = input.postMeetingNotificationEnabled &&
                detectMeetingOverrun(input.now, input.activeWindowEndMs, nextInstance, input.postMeetingNotificationOffsetMinutes)
            return Decision(
                shouldEnableDnd = !input.systemDndIsOn, // Turn on if not already on
                shouldDisableDnd = false,
                setDndSetByApp = if (!input.systemDndIsOn) true else null,
                setUserSuppressedUntil = null,
                setActiveWindowEnd = dndWindow.endMs,
                setManualDndUntilMs = manualClearValue,
                notificationNeeded = when {
                    meetingOverrun -> NotificationNeeded.MEETING_OVERRUN
                    !input.hasExactAlarms -> NotificationNeeded.DEGRADED_MODE
                    else -> NotificationNeeded.NONE
                }
            )
        }

        // RULE 4: Active DND window but suppressed (user override)
        if (dndWindow.isActive && isSuppressed) {
            val meetingOverrun = input.postMeetingNotificationEnabled &&
                detectMeetingOverrun(input.now, input.activeWindowEndMs, nextInstance, input.postMeetingNotificationOffsetMinutes)
            val newSuppressedUntil = if (userOverrideDetected) dndWindow.endMs else null

            return Decision(
                shouldEnableDnd = false,
                shouldDisableDnd = false,
                setDndSetByApp = if (userOverrideDetected) false else null,
                setUserSuppressedUntil = newSuppressedUntil,
                setActiveWindowEnd = dndWindow.endMs,
                setManualDndUntilMs = manualClearValue,
                notificationNeeded = if (meetingOverrun) {
                    NotificationNeeded.MEETING_OVERRUN
                } else {
                    NotificationNeeded.NONE
                }
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
        val notificationForOverrun = if (meetingOverrun) {
            NotificationNeeded.MEETING_OVERRUN
        } else if (!input.hasExactAlarms) {
            NotificationNeeded.DEGRADED_MODE
        } else {
            NotificationNeeded.NONE
        }

        return Decision(
            shouldEnableDnd = false,
            shouldDisableDnd = input.dndSetByApp, // Turn off only if we own it
            setDndSetByApp = if (input.dndSetByApp) false else null,
            setUserSuppressedUntil = null,
            setActiveWindowEnd = if (keepActiveWindowEnd) input.activeWindowEndMs else 0L,
            setManualDndUntilMs = manualClearValue,
            notificationNeeded = notificationForOverrun
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
