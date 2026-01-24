package com.brunoafk.calendardnd.system.update

import android.app.Activity

interface PlayStoreUpdateManager {
    fun checkForPlayStoreUpdate(activity: Activity)
    fun handleActivityResult(requestCode: Int, resultCode: Int, activity: Activity)
}
