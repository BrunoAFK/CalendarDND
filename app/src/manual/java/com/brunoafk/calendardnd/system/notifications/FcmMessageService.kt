package com.brunoafk.calendardnd.system.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.ui.MainActivity
import com.brunoafk.calendardnd.util.ExternalLinkPolicy
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FcmMessageService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title
            ?: message.data["title"]
            ?: getString(R.string.fcm_default_title)
        val body = message.notification?.body
            ?: message.data["body"]
            ?: ""
        val action = message.data["action"]?.trim().orEmpty()

        showNotification(title, body, action)
    }

    private fun showNotification(title: String, body: String, action: String) {
        createNotificationChannel()

        val actionUri = action.takeIf { it.isNotBlank() }?.let {
            runCatching { Uri.parse(it) }.getOrNull()
        }
        val intent = when {
            actionUri == null -> Intent(this, MainActivity::class.java)
            ExternalLinkPolicy.isInternal(actionUri) -> Intent(Intent.ACTION_VIEW, actionUri)
            ExternalLinkPolicy.isAllowlistedExternal(actionUri) -> Intent(Intent.ACTION_VIEW, actionUri)
            actionUri.scheme == "https" -> {
                Intent(this, MainActivity::class.java).apply {
                    putExtra(MainActivity.EXTRA_EXTERNAL_URL, actionUri.toString())
                }
            }
            else -> Intent(this, MainActivity::class.java)
        }.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.fcm_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.fcm_channel_description)
        }

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "updates"
        private const val NOTIFICATION_ID = 2001
    }
}
