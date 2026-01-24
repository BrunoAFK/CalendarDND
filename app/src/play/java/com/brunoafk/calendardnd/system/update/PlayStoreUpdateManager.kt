package com.brunoafk.calendardnd.system.update

import android.app.Activity
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

class PlayStoreUpdateManagerImpl : PlayStoreUpdateManager {

    override fun checkForPlayStoreUpdate(
        activity: Activity,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        val appUpdateManager = AppUpdateManagerFactory.create(activity)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                startImmediateUpdate(appUpdateManager, appUpdateInfo, launcher)
            }
        }.addOnFailureListener { error ->
            Log.w(TAG, "PLAY_UPDATE: Unable to check update availability", error)
        }
    }

    override fun resumeIfUpdateInProgress(
        activity: Activity,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        val appUpdateManager = AppUpdateManagerFactory.create(activity)
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                startImmediateUpdate(appUpdateManager, appUpdateInfo, launcher)
            }
        }.addOnFailureListener { error ->
            Log.w(TAG, "PLAY_UPDATE: Unable to resume update", error)
        }
    }

    private fun startImmediateUpdate(
        appUpdateManager: com.google.android.play.core.appupdate.AppUpdateManager,
        appUpdateInfo: com.google.android.play.core.appupdate.AppUpdateInfo,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        val options = AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
        val started = appUpdateManager.startUpdateFlowForResult(
            appUpdateInfo,
            launcher,
            options
        )
        if (!started) {
            Log.w(TAG, "PLAY_UPDATE: Update flow not started")
        } else {
            Log.d(TAG, "PLAY_UPDATE: Update flow started")
        }
    }

    private companion object {
        private const val TAG = "PlayStoreUpdate"
    }
}
