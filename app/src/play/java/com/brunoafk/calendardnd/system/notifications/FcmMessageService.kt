package com.brunoafk.calendardnd.system.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.brunoafk.calendardnd.R

class FcmMessageService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val title = data["title"] ?: message.notification?.title ?: getString(R.string.app_name)
        val body = data["body"] ?: message.notification?.body.orEmpty()
        if (title.isBlank() && body.isBlank()) {
            return
        }
        val actionUrl = data["action_url"] ?: data["action"]
        val actionLabel = data["action_label"]
        val openUrl = data["open_url"]
        FcmNotificationHelper.show(
            context = this,
            title = title,
            body = body,
            openUrl = openUrl,
            actionLabel = actionLabel,
            actionUrl = actionUrl
        )
    }
}
