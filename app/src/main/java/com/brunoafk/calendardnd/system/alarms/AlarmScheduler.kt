package com.brunoafk.calendardnd.system.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.brunoafk.calendardnd.util.ExceptionHandler

class AlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    companion object {
        private const val REQUEST_CODE_BOUNDARY = 1001
        private const val REQUEST_CODE_PRE_DND_NOTIFICATION = 1002
        private const val REQUEST_CODE_POST_MEETING_CHECK = 1003
        private const val REQUEST_CODE_RESTORE_RINGER = 1004
        const val EXTRA_MEETING_TITLE = "extra_meeting_title"
        const val EXTRA_DND_WINDOW_END_MS = "extra_dnd_window_end_ms"
        const val EXTRA_DND_WINDOW_START_MS = "extra_dnd_window_start_ms"
    }

    /**
     * Check if exact alarms are available
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true // Always available before Android 12
        }
    }

    /**
     * Schedule an exact alarm for the next boundary
     */
    fun scheduleBoundaryAlarm(boundaryMs: Long): Boolean {
        if (!canScheduleExactAlarms()) {
            return false
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmActions.ACTION_BOUNDARY
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_BOUNDARY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                boundaryMs,
                pendingIntent
            )
            true
        } catch (e: Exception) {
            ExceptionHandler.handleAlarmException(e, "scheduleBoundaryAlarm")
            false
        }
    }

    /**
     * Cancel the boundary alarm
     */
    fun cancelBoundaryAlarm() {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmActions.ACTION_BOUNDARY
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_BOUNDARY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        } catch (e: Exception) {
            ExceptionHandler.handleAlarmException(e, "cancelBoundaryAlarm")
        }
    }

    /**
     * Schedule a notification shortly before DND will start
     */
    fun schedulePreDndNotificationAlarm(
        triggerAtMs: Long,
        meetingTitle: String?,
        dndWindowEndMs: Long?,
        dndWindowStartMs: Long?
    ): Boolean {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmActions.ACTION_PRE_DND_NOTIFICATION
            putExtra(EXTRA_MEETING_TITLE, meetingTitle)
            if (dndWindowEndMs != null) {
                putExtra(EXTRA_DND_WINDOW_END_MS, dndWindowEndMs)
            }
            if (dndWindowStartMs != null) {
                putExtra(EXTRA_DND_WINDOW_START_MS, dndWindowStartMs)
            }
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_PRE_DND_NOTIFICATION,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return try {
            if (canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    pendingIntent
                )
            } else {
                val windowMs = 60_000L
                alarmManager.setWindow(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    windowMs,
                    pendingIntent
                )
            }
            true
        } catch (e: Exception) {
            ExceptionHandler.handleAlarmException(e, "schedulePreDndNotificationAlarm")
            false
        }
    }

    /**
     * Cancel the pre-DND notification alarm
     */
    fun cancelPreDndNotificationAlarm() {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmActions.ACTION_PRE_DND_NOTIFICATION
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_PRE_DND_NOTIFICATION,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        } catch (e: Exception) {
            ExceptionHandler.handleAlarmException(e, "cancelPreDndNotificationAlarm")
        }
    }

    /**
     * Schedule a post-meeting check to run the engine around the end offset.
     */
    fun schedulePostMeetingCheckAlarm(triggerAtMs: Long): Boolean {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmActions.ACTION_POST_MEETING_CHECK
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_POST_MEETING_CHECK,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return try {
            if (canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    pendingIntent
                )
            } else {
                val windowMs = 60_000L
                alarmManager.setWindow(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    windowMs,
                    pendingIntent
                )
            }
            true
        } catch (e: Exception) {
            ExceptionHandler.handleAlarmException(e, "schedulePostMeetingCheckAlarm")
            false
        }
    }

    /**
     * Cancel the post-meeting check alarm
     */
    fun cancelPostMeetingCheckAlarm() {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmActions.ACTION_POST_MEETING_CHECK
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_POST_MEETING_CHECK,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        } catch (e: Exception) {
            ExceptionHandler.handleAlarmException(e, "cancelPostMeetingCheckAlarm")
        }
    }

    /**
     * Schedule an alarm to restore the ringer mode after vibration cool-down.
     */
    fun scheduleRestoreRingerAlarm(triggerAtMs: Long): Boolean {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmActions.ACTION_RESTORE_RINGER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_RESTORE_RINGER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return try {
            if (canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    pendingIntent
                )
            } else {
                val windowMs = 60_000L
                alarmManager.setWindow(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMs,
                    windowMs,
                    pendingIntent
                )
            }
            true
        } catch (e: Exception) {
            ExceptionHandler.handleAlarmException(e, "scheduleRestoreRingerAlarm")
            false
        }
    }

    /**
     * Cancel the restore ringer alarm.
     */
    fun cancelRestoreRingerAlarm() {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmActions.ACTION_RESTORE_RINGER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_RESTORE_RINGER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        } catch (e: Exception) {
            ExceptionHandler.handleAlarmException(e, "cancelRestoreRingerAlarm")
        }
    }

    /**
     * Open exact alarm settings (for Android 12+)
     */
    fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
