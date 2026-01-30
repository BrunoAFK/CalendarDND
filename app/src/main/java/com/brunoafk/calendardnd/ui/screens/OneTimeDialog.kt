package com.brunoafk.calendardnd.ui.screens

import com.brunoafk.calendardnd.domain.model.OneTimeAction
import com.brunoafk.calendardnd.domain.model.OneTimeActionType

sealed class OneTimeDialog {
    data class Set(val action: OneTimeAction) : OneTimeDialog()
    data class Clear(val activeType: OneTimeActionType) : OneTimeDialog()
}
