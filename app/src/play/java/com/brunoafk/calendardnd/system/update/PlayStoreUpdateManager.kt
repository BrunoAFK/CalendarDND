package com.brunoafk.calendardnd.system.update

import android.app.Activity
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

class PlayStoreUpdateManagerImpl : PlayStoreUpdateManager {
    private var lastUpdateType: Int? = null
    private var activeUpdateManager: com.google.android.play.core.appupdate.AppUpdateManager? = null
    private var flexibleListener: InstallStateUpdatedListener? = null

    override fun checkForPlayStoreUpdate(
        activity: Activity,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        val appUpdateManager = AppUpdateManagerFactory.create(activity)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                val updateType = resolveUpdateType(appUpdateInfo)
                if (updateType == null) {
                    Log.w(TAG, "PLAY_UPDATE: No supported update type")
                    return@addOnSuccessListener
                }
                startUpdateFlow(appUpdateManager, appUpdateInfo, launcher, updateType)
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
                startUpdateFlow(appUpdateManager, appUpdateInfo, launcher, AppUpdateType.IMMEDIATE)
                return@addOnSuccessListener
            }
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                Log.d(TAG, "PLAY_UPDATE: Completing downloaded flexible update")
                appUpdateManager.completeUpdate()
            }
        }.addOnFailureListener { error ->
            Log.w(TAG, "PLAY_UPDATE: Unable to resume update", error)
        }
    }

    override fun handleUpdateFlowResult(
        resultCode: Int,
        activity: Activity,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        if (resultCode == Activity.RESULT_OK) {
            return
        }
        if (lastUpdateType == AppUpdateType.IMMEDIATE) {
            Log.w(TAG, "PLAY_UPDATE: Immediate update canceled, retrying")
            checkForPlayStoreUpdate(activity, launcher)
        } else {
            Log.w(TAG, "PLAY_UPDATE: Update flow canceled")
        }
    }

    private fun startUpdateFlow(
        appUpdateManager: com.google.android.play.core.appupdate.AppUpdateManager,
        appUpdateInfo: com.google.android.play.core.appupdate.AppUpdateInfo,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
        updateType: Int
    ) {
        lastUpdateType = updateType
        if (updateType == AppUpdateType.FLEXIBLE) {
            registerFlexibleListener(appUpdateManager)
        }
        val options = AppUpdateOptions.newBuilder(updateType).build()
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

    private fun resolveUpdateType(
        appUpdateInfo: com.google.android.play.core.appupdate.AppUpdateInfo
    ): Int? {
        val updatePriority = appUpdateInfo.updatePriority()
        val stalenessDays = appUpdateInfo.clientVersionStalenessDays() ?: 0
        val wantsImmediate = updatePriority >= IMMEDIATE_PRIORITY
            || stalenessDays >= IMMEDIATE_STALENESS_DAYS
        val immediateAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
        val flexibleAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
        return when {
            wantsImmediate && immediateAllowed -> AppUpdateType.IMMEDIATE
            flexibleAllowed -> AppUpdateType.FLEXIBLE
            immediateAllowed -> AppUpdateType.IMMEDIATE
            else -> null
        }
    }

    private fun registerFlexibleListener(
        appUpdateManager: com.google.android.play.core.appupdate.AppUpdateManager
    ) {
        if (activeUpdateManager != null && flexibleListener != null) {
            activeUpdateManager?.unregisterListener(flexibleListener!!)
        }
        activeUpdateManager = appUpdateManager
        lateinit var listener: InstallStateUpdatedListener
        listener = InstallStateUpdatedListener { state ->
            when (state.installStatus()) {
                InstallStatus.DOWNLOADED -> {
                    Log.d(TAG, "PLAY_UPDATE: Flexible update downloaded, completing")
                    appUpdateManager.completeUpdate()
                }
                InstallStatus.INSTALLED,
                InstallStatus.FAILED,
                InstallStatus.CANCELED -> {
                    Log.d(TAG, "PLAY_UPDATE: Flexible update finished with status ${state.installStatus()}")
                    appUpdateManager.unregisterListener(listener)
                    activeUpdateManager = null
                    flexibleListener = null
                }
                else -> Unit
            }
        }
        flexibleListener = listener
        appUpdateManager.registerListener(listener)
    }

    private companion object {
        private const val TAG = "PlayStoreUpdate"
        private const val IMMEDIATE_PRIORITY = 4
        private const val IMMEDIATE_STALENESS_DAYS = 14
    }
}
