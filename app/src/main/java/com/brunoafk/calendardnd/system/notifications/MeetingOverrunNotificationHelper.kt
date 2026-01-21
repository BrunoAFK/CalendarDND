package com.brunoafk.calendardnd.system.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.system.receivers.ExtendDndReceiver
import com.brunoafk.calendardnd.util.PermissionUtils

/**
 * Shows notification when a meeting runs over scheduled time
 * Offers quick extension options (+5m, +15m, +30m)
 */
object MeetingOverrunNotificationHelper {

    private const val CHANNEL_ID = "meeting_overrun"
    private const val CHANNEL_ID_SILENT = "meeting_overrun_silent"
    private const val NOTIFICATION_ID = 1002

    fun showOverrunNotification(context: Context, silent: Boolean) {
        if (!PermissionUtils.hasNotificationPermission(context)) {
            return
        }

        val channelId = if (silent) CHANNEL_ID_SILENT else CHANNEL_ID
        createNotificationChannel(context, silent)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.meeting_overrun_title))
            .setContentText(context.getString(R.string.meeting_overrun_message))
            .setPriority(if (silent) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(createExtendAction(context, 5))
            .addAction(createExtendAction(context, 15))
            .addAction(createExtendAction(context, 30))
            .addAction(createRestoreAction(context))
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createExtendAction(context: Context, minutes: Int): NotificationCompat.Action {
        val intent = Intent(context, ExtendDndReceiver::class.java).apply {
            action = ExtendDndReceiver.ACTION_EXTEND_DND
            putExtra(ExtendDndReceiver.EXTRA_MINUTES, minutes)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            minutes, // unique request code
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Action.Builder(
            0, // no icon
            context.getString(R.string.meeting_overrun_extend_format, minutes),
            pendingIntent
        ).build()
    }

    private fun createRestoreAction(context: Context): NotificationCompat.Action {
        val intent = Intent(context, ExtendDndReceiver::class.java).apply {
            action = ExtendDndReceiver.ACTION_STOP_DND
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Action.Builder(
            R.drawable.ic_refresh,
            context.getString(R.string.meeting_overrun_restore),
            pendingIntent
        ).build()
    }

    private fun createNotificationChannel(context: Context, silent: Boolean) {
        val channelId = if (silent) CHANNEL_ID_SILENT else CHANNEL_ID
        val channelNameRes = if (silent) {
            R.string.meeting_overrun_channel_name_silent
        } else {
            R.string.meeting_overrun_channel_name
        }
        val importance = if (silent) {
            NotificationManager.IMPORTANCE_LOW
        } else {
            NotificationManager.IMPORTANCE_HIGH
        }
        val channel = NotificationChannel(
            channelId,
            context.getString(channelNameRes),
            importance
        ).apply {
            description = context.getString(R.string.meeting_overrun_channel_description)
            if (silent) {
                setSound(null, null)
                enableVibration(true)
            }
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun cancel(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
