package com.brunoafk.calendardnd.domain.engine

import com.brunoafk.calendardnd.domain.model.DndMode
import com.brunoafk.calendardnd.domain.model.KeywordMatchMode
import com.brunoafk.calendardnd.domain.model.Trigger

/**
 * Input to the automation engine
 */
data class EngineInput(
    val trigger: Trigger,
    val now: Long,

    // Settings
    val automationEnabled: Boolean,
    val selectedCalendarIds: Set<String>,
    val busyOnly: Boolean,
    val ignoreAllDay: Boolean,
    val skipRecurring: Boolean,
    val selectedDaysEnabled: Boolean,
    val selectedDaysMask: Int,
    val minEventMinutes: Int,
    val requireLocation: Boolean,
    val dndMode: DndMode,
    val dndStartOffsetMinutes: Int,
    val preDndNotificationEnabled: Boolean,
    val preDndNotificationLeadMinutes: Int,
    val requireTitleKeyword: Boolean,
    val titleKeyword: String,
    val titleKeywordMatchMode: KeywordMatchMode,
    val titleKeywordCaseSensitive: Boolean,
    val titleKeywordMatchAll: Boolean,
    val titleKeywordExclude: Boolean,
    val postMeetingNotificationEnabled: Boolean,
    val postMeetingNotificationOffsetMinutes: Int,

    // Runtime state
    val dndSetByApp: Boolean,
    val activeWindowEndMs: Long,
    val userSuppressedUntilMs: Long,
    val userSuppressedFromMs: Long,
    val manualDndUntilMs: Long,
    val manualEventStartMs: Long,
    val manualEventEndMs: Long,
    val lastKnownDndFilter: Int,
    val skippedEventBeginMs: Long,
    val notifiedNewEventBeforeSkip: Boolean,

    // System state
    val hasCalendarPermission: Boolean,
    val hasPolicyAccess: Boolean,
    val hasExactAlarms: Boolean,
    val systemDndIsOn: Boolean,
    val currentSystemFilter: Int
)
