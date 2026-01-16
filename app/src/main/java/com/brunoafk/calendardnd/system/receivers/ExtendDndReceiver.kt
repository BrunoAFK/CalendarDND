package com.brunoafk.calendardnd.system.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.brunoafk.calendardnd.data.prefs.RuntimeStateStore
import com.brunoafk.calendardnd.domain.model.Trigger
import com.brunoafk.calendardnd.system.alarms.EngineRunner
import com.brunoafk.calendardnd.system.notifications.MeetingOverrunNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles DND extension actions from notifications.
 */
class ExtendDndReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_EXTEND_DND -> handleExtend(context, intent)
            ACTION_STOP_DND -> handleStop(context)
        }
    }

    private fun handleExtend(context: Context, intent: Intent) {
        val minutes = intent.getIntExtra(EXTRA_MINUTES, 15)
        val extendUntilMs = System.currentTimeMillis() + (minutes * 60 * 1000L)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val runtimeStateStore = RuntimeStateStore(context)
                runtimeStateStore.setManualDndUntilMs(extendUntilMs)

                // Cancel the overrun notification
                MeetingOverrunNotificationHelper.cancel(context)

                // Trigger engine run to apply the extension
                EngineRunner.runEngine(context, Trigger.MANUAL)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleStop(context: Context) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val runtimeStateStore = RuntimeStateStore(context)
                runtimeStateStore.setManualDndUntilMs(0L)

                // Cancel any overrun notification
                MeetingOverrunNotificationHelper.cancel(context)

                // Trigger engine run to disable DND
                EngineRunner.runEngine(context, Trigger.MANUAL)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_EXTEND_DND = "com.brunoafk.calendardnd.EXTEND_DND"
        const val ACTION_STOP_DND = "com.brunoafk.calendardnd.STOP_DND"
        const val EXTRA_MINUTES = "MINUTES"
    }
}
