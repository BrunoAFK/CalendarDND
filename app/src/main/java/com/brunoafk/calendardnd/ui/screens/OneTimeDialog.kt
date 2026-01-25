package com.brunoafk.calendardnd.ui.screens

sealed class OneTimeDialog {
    data class Set(val action: OneTimeAction) : OneTimeDialog()
    data class Clear(val activeType: OneTimeActionType) : OneTimeDialog()
}
