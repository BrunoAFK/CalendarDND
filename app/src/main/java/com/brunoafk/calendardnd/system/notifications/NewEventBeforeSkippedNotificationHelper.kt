package com.brunoafk.calendardnd.system.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.ui.MainActivity
import com.brunoafk.calendardnd.util.PermissionUtils

/**
 * Shows notification when a new event appears before a user-skipped event.
 * This is informational only - no action buttons needed.
 */
object NewEventBeforeSkippedNotificationHelper {

    private const val CHANNEL_ID = "new_event_info"
    private const val NOTIFICATION_ID = 1003

    fun showNotification(context: Context, eventTitle: String?) {
        if (!PermissionUtils.hasNotificationPermission(context)) {
            return
        }

        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = context.getString(R.string.new_event_before_skipped_title)
        val body = if (eventTitle.isNullOrBlank()) {
            context.getString(R.string.new_event_before_skipped_body_generic)
        } else {
            context.getString(R.string.new_event_before_skipped_body, eventTitle)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.new_event_before_skipped_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.new_event_before_skipped_channel_description)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun cancel(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
