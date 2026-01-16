package com.brunoafk.calendardnd.system.notifications

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.ui.MainActivity
import com.brunoafk.calendardnd.system.receivers.EnableDndNowReceiver
import com.brunoafk.calendardnd.util.AnalyticsTracker
import com.brunoafk.calendardnd.util.LocaleUtils
import com.brunoafk.calendardnd.util.PermissionUtils

object DndNotificationHelper {

    private const val CHANNEL_ID = "dnd_reminders"
    private const val CHANNEL_NAME = "DND reminders"
    private const val NOTIFICATION_ID = 2001

    @SuppressLint("MissingPermission")
    fun showPreDndNotification(
        context: Context,
        meetingTitle: String?,
        dndWindowEndMs: Long?
    ) {
        if (!PermissionUtils.hasNotificationPermission(context)) {
            return
        }

        ensureChannel(context)

        val stringsContext = LocaleUtils.localizedContext(context)
        val title = stringsContext.getString(R.string.pre_dnd_notification_title)
        val text = if (meetingTitle.isNullOrBlank()) {
            stringsContext.getString(R.string.pre_dnd_notification_body_generic)
        } else {
            stringsContext.getString(R.string.pre_dnd_notification_body, meetingTitle)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.setContentIntent(openPendingIntent)
        
        if (dndWindowEndMs != null) {
            val actionIntent = Intent(context, EnableDndNowReceiver::class.java).apply {
                action = EnableDndNowReceiver.ACTION_ENABLE_DND_NOW
                putExtra(EnableDndNowReceiver.EXTRA_DND_WINDOW_END_MS, dndWindowEndMs)
            }
            val actionPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                actionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                0,
                stringsContext.getString(R.string.pre_dnd_notification_action),
                actionPendingIntent
            )
        }

        val notification = builder.build()

        AnalyticsTracker.logPreDndNotificationShown(context)

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Permission was revoked between the check and notify call.
        }
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
