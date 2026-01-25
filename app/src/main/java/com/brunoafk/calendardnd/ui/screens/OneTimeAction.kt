package com.brunoafk.calendardnd.ui.screens

import com.brunoafk.calendardnd.domain.model.EventInstance

sealed class OneTimeAction {
    data class EnableForEvent(val event: EventInstance, val startMs: Long, val endMs: Long) : OneTimeAction()
    data class SkipEvent(val event: EventInstance, val startMs: Long, val endMs: Long) : OneTimeAction()
}
