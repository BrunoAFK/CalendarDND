package com.brunoafk.calendardnd.system.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.brunoafk.calendardnd.domain.model.Trigger
import com.brunoafk.calendardnd.system.alarms.EngineRunner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {

            val pendingResult = goAsync()

            scope.launch {
                try {
                    EngineRunner.runEngine(context, Trigger.BOOT)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}