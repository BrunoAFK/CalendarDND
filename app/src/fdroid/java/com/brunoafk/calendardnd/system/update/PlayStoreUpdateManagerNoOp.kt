package com.brunoafk.calendardnd.system.update

import android.app.Activity
class PlayStoreUpdateManagerNoOp : PlayStoreUpdateManager {
    override fun checkForPlayStoreUpdate(activity: Activity) {
        // No-op
    }

    override fun handleActivityResult(requestCode: Int, resultCode: Int, activity: Activity) {
        // No-op
    }
}
