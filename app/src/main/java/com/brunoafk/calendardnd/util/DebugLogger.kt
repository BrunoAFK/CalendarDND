package com.brunoafk.calendardnd.util

import android.content.Context
import com.brunoafk.calendardnd.domain.engine.Decision
import com.brunoafk.calendardnd.domain.engine.EngineInput
import com.brunoafk.calendardnd.domain.engine.EngineOutput
import com.brunoafk.calendardnd.domain.model.Trigger
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLogger {

    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun buildEngineLog(
        context: Context,
        trigger: Trigger,
        input: EngineInput,
        output: EngineOutput,
        executionTimeMs: Long
    ): String {
        val decision = output.decision
        val action = formatDecisionAction(decision)
        val activeWindow = output.activeWindow
        val activeWindowSummary = activeWindow?.let {
            val range = formatTimeRange(it.begin, it.end)
            "window $range (${it.events.size} events)"
        }
        val nextEvent = output.nextInstance
        val nextSummary = if (nextEvent != null) {
            "${redactTitle(nextEvent.title)} at ${formatDateTime(nextEvent.begin)}"
        } else {
            "none"
        }

        return buildString {
            appendLine("============================================================")
            appendLine("Engine run ${dateTimeFormat.format(Date(input.now))} (${executionTimeMs}ms)")
            appendLine("------------------------------------------------------------")
            appendLine("Inputs")
            appendLine("- Trigger: $trigger")
            appendLine("- Automation: ${if (input.automationEnabled) "on" else "off"}")
            appendLine("- DND mode: ${input.dndMode}")
            appendLine("- Calendars: ${formatCalendarIds(input.selectedCalendarIds)}")
            appendLine("- Active window: ${activeWindowSummary ?: "none"}")
            appendLine("- Next event: $nextSummary")
            appendLine()
            appendLine("Decision")
            appendLine("- Action: $action")
            decision.setDndSetByApp?.let { appendLine("- App owns DND: $it") }
            decision.setUserSuppressedUntil?.let { appendLine("- Suppress until: ${formatTime(it)}") }
            decision.setManualDndUntilMs?.let { appendLine("- Manual DND until: ${formatTime(it)}") }
            if (decision.notificationNeeded != null) {
                appendLine("- Notification: ${decision.notificationNeeded}")
            }
            if (output.userOverrideDetected) {
                appendLine("- User override detected")
            }
            appendLine()
            appendLine("Schedule")
            val plan = output.schedulePlan
            if (plan?.nextBoundaryMs != null) {
                appendLine("- Next boundary: ${formatDateTime(plan.nextBoundaryMs)} (guards=${plan.needsNearTermGuards})")
                plan.guardBeforeMs?.let { appendLine("- Guard before: ${formatTime(it)}") }
                plan.guardAfterMs?.let { appendLine("- Guard after: ${formatTime(it)}") }
            } else {
                appendLine("- Next boundary: none")
            }

            appendLine("============================================================")
        }
    }

    fun buildCompactLog(
        context: Context,
        trigger: Trigger,
        input: EngineInput,
        output: EngineOutput
    ): String {
        val action = formatDecisionAction(output.decision)
        val activeCount = output.activeWindow?.events?.size ?: 0
        val nextIn = output.nextInstance?.let {
            val minutes = (it.begin - input.now) / 60000
            if (minutes < 60) "${minutes}min" else "${minutes / 60}h"
        } ?: "none"

        return "[${timeFormat.format(Date(input.now))}] " +
            "$trigger $action | " +
            "auto=${if (input.automationEnabled) "on" else "off"} | " +
            "active=$activeCount | " +
            "next=$nextIn"
    }

    private fun formatTime(ms: Long?): String {
        if (ms == null || ms <= 0) return "none"
        return timeFormat.format(Date(ms))
    }

    private fun formatDateTime(ms: Long?): String {
        if (ms == null || ms <= 0) return "none"
        return dateTimeFormat.format(Date(ms))
    }

    private fun formatTimeRange(startMs: Long, endMs: Long): String {
        return "${timeFormat.format(Date(startMs))} - ${timeFormat.format(Date(endMs))}"
    }

    private fun formatCalendarIds(ids: Set<String>): String {
        return if (ids.isEmpty()) "all" else "${ids.size} selected"
    }

    private fun formatDecisionAction(decision: Decision): String {
        return when {
            decision.shouldEnableDnd -> "ENABLE DND"
            decision.shouldDisableDnd -> "DISABLE DND"
            else -> "NO CHANGE"
        }
    }

    private fun redactTitle(title: String): String {
        if (title.isBlank()) {
            return "(no title)"
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(title.toByteArray(Charsets.UTF_8))
        val hash = digest.joinToString("") { "%02x".format(it) }.take(8)
        return "event#$hash"
    }
}
