package com.brunoafk.calendardnd.system.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.brunoafk.calendardnd.data.dnd.DndController
import com.brunoafk.calendardnd.data.prefs.RuntimeStateStore
import com.brunoafk.calendardnd.data.prefs.SettingsStore
import com.brunoafk.calendardnd.system.alarms.AlarmScheduler
import com.brunoafk.calendardnd.system.workers.Workers
import com.brunoafk.calendardnd.util.AnalyticsTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class EnableDndNowReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ENABLE_DND_NOW) {
            return
        }

        val pendingResult = goAsync()
        val endMs = intent.getLongExtra(EXTRA_DND_WINDOW_END_MS, 0L)

        scope.launch {
            try {
                if (endMs <= System.currentTimeMillis()) {
                    return@launch
                }

                val settingsStore = SettingsStore(context)
                val runtimeStateStore = RuntimeStateStore(context)
                val dndController = DndController(context)
                val alarmScheduler = AlarmScheduler(context)

                if (!settingsStore.automationEnabled.first()) {
                    return@launch
                }

                if (!dndController.hasPolicyAccess()) {
                    return@launch
                }

                dndController.enableDnd(settingsStore.dndMode.first())

                runtimeStateStore.setDndSetByApp(true)
                runtimeStateStore.setActiveWindowEndMs(endMs)
                runtimeStateStore.setManualDndUntilMs(endMs)
                runtimeStateStore.setUserSuppressedUntilMs(0L)

                alarmScheduler.scheduleBoundaryAlarm(endMs)
                Workers.ensureSanityWorker(context)

                AnalyticsTracker.logPreDndEnableNow(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_ENABLE_DND_NOW = "com.brunoafk.calendardnd.action.ENABLE_DND_NOW"
        const val EXTRA_DND_WINDOW_END_MS = "extra_dnd_window_end_ms"
    }
}
