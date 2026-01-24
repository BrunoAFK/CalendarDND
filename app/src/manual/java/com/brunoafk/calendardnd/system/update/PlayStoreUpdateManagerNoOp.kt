package com.brunoafk.calendardnd.system.update

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
class PlayStoreUpdateManagerNoOp : PlayStoreUpdateManager {
    override fun checkForPlayStoreUpdate(
        activity: Activity,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        // No-op
    }

    override fun resumeIfUpdateInProgress(
        activity: Activity,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        // No-op
    }
}
