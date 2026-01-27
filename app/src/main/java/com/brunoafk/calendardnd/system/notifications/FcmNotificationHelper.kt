package com.brunoafk.calendardnd.system.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.ui.MainActivity
import com.brunoafk.calendardnd.util.PermissionUtils

object FcmNotificationHelper {

    private const val CHANNEL_ID = "fcm_general"

    fun show(
        context: Context,
        title: String,
        body: String,
        openUrl: String? = null,
        actionLabel: String? = null,
        actionUrl: String? = null
    ) {
        if (!PermissionUtils.hasNotificationPermission(context)) {
            return
        }

        ensureChannel(context)

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            buildOpenIntent(context, openUrl),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (!actionLabel.isNullOrBlank() && !actionUrl.isNullOrBlank()) {
            val actionIntent = PendingIntent.getActivity(
                context,
                1,
                Intent(Intent.ACTION_VIEW, Uri.parse(actionUrl)),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(R.drawable.ic_notification, actionLabel, actionIntent)
        }

        val notificationId = (System.currentTimeMillis() and 0xFFFFFFF).toInt()
        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Permission can be revoked at runtime even after the pre-check.
        }
    }

    private fun buildOpenIntent(context: Context, openUrl: String?): Intent {
        return if (!openUrl.isNullOrBlank()) {
            Intent(Intent.ACTION_VIEW, Uri.parse(openUrl))
        } else {
            Intent(context, MainActivity::class.java)
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.fcm_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.fcm_channel_description)
        }
        manager.createNotificationChannel(channel)
    }
}
