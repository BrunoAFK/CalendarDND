package com.brunoafk.calendardnd.system.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.brunoafk.calendardnd.domain.model.Trigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AlarmActions.ACTION_BOUNDARY -> {
                // Use goAsync for long-running work
                val pendingResult = goAsync()

                scope.launch {
                    try {
                        // Run the engine
                        EngineRunner.runEngine(context, Trigger.ALARM)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            AlarmActions.ACTION_PRE_DND_NOTIFICATION -> {
                val pendingResult = goAsync()
                val meetingTitle = intent.getStringExtra(AlarmScheduler.EXTRA_MEETING_TITLE)
                val dndWindowEndMs = if (intent.hasExtra(AlarmScheduler.EXTRA_DND_WINDOW_END_MS)) {
                    intent.getLongExtra(AlarmScheduler.EXTRA_DND_WINDOW_END_MS, 0L)
                } else {
                    null
                }
                val dndWindowStartMs = if (intent.hasExtra(AlarmScheduler.EXTRA_DND_WINDOW_START_MS)) {
                    intent.getLongExtra(AlarmScheduler.EXTRA_DND_WINDOW_START_MS, 0L)
                } else {
                    null
                }

                scope.launch {
                    try {
                        com.brunoafk.calendardnd.system.notifications.DndNotificationHelper
                            .showPreDndNotification(context, meetingTitle, dndWindowEndMs, dndWindowStartMs)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            AlarmActions.ACTION_POST_MEETING_CHECK -> {
                val pendingResult = goAsync()

                scope.launch {
                    try {
                        EngineRunner.runEngine(context, Trigger.ALARM)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
