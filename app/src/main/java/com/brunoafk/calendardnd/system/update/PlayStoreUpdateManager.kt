package com.brunoafk.calendardnd.system.update

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest

interface PlayStoreUpdateManager {
    fun checkForPlayStoreUpdate(
        activity: Activity,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    )
    fun resumeIfUpdateInProgress(
        activity: Activity,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    )
    fun handleUpdateFlowResult(
        resultCode: Int,
        activity: Activity,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    )
}
