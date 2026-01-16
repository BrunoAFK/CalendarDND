package com.brunoafk.calendardnd.system.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.system.receivers.ExtendDndReceiver

/**
 * Shows notification when a meeting runs over scheduled time
 * Offers quick extension options (+5m, +15m, +30m)
 */
object MeetingOverrunNotificationHelper {

    private const val CHANNEL_ID = "meeting_overrun"
    private const val NOTIFICATION_ID = 1002

    fun showOverrunNotification(context: Context) {
        createNotificationChannel(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile_automation)
            .setContentTitle(context.getString(R.string.meeting_overrun_title))
            .setContentText(context.getString(R.string.meeting_overrun_message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(createExtendAction(context, 5))
            .addAction(createExtendAction(context, 15))
            .addAction(createExtendAction(context, 30))
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

    private fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.meeting_overrun_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.meeting_overrun_channel_description)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun cancel(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
