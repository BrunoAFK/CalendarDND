package com.brunoafk.calendardnd.system.notifications

import android.annotation.SuppressLint
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
import com.brunoafk.calendardnd.system.update.ManualUpdateManager
import com.brunoafk.calendardnd.util.PermissionUtils

object UpdateNotificationHelper {

    private const val CHANNEL_ID = "app_updates"
    private const val CHANNEL_NAME = "App updates"
    private const val NOTIFICATION_ID = 3001

    @SuppressLint("MissingPermission")
    fun showUpdateNotification(context: Context, info: ManualUpdateManager.ReleaseInfo) {
        if (!PermissionUtils.hasNotificationPermission(context)) {
            return
        }

        ensureChannel(context)

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.apkUrl))
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(R.string.update_notification_title)
        val text = context.getString(R.string.update_notification_body, info.versionName)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)
    }
}
