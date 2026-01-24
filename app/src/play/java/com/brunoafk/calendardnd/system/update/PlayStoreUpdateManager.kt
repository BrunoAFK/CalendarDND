package com.brunoafk.calendardnd.system.update

import android.app.Activity
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

class PlayStoreUpdateManagerImpl : PlayStoreUpdateManager {

    private val UPDATE_REQUEST_CODE = 123

    override fun checkForPlayStoreUpdate(activity: Activity) {
        val appUpdateManager = AppUpdateManagerFactory.create(activity)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.IMMEDIATE,
                    activity,
                    UPDATE_REQUEST_CODE
                )
            }
        }
    }

    override fun handleActivityResult(requestCode: Int, resultCode: Int, activity: Activity) {
        if (requestCode == UPDATE_REQUEST_CODE) {
            if (resultCode != Activity.RESULT_OK) {
                // If the update is cancelled or fails,
                // you can request to start the update again.
                checkForPlayStoreUpdate(activity)
            }
        }
    }
}
