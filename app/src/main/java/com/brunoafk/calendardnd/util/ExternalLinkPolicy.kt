package com.brunoafk.calendardnd.util

import android.net.Uri

object ExternalLinkPolicy {

    private val allowlistedHosts = setOf(
        "calendar-dnd.app",
        "github.com",
        "githubusercontent.com",
        "pavelja.com",
        "pavelja.me",
        "afk.place"
    )

    fun isInternal(uri: Uri): Boolean {
        return uri.scheme == "calendardnd"
    }

    fun isAllowlistedExternal(uri: Uri): Boolean {
        if (uri.scheme != "https") {
            return false
        }
        val host = uri.host?.lowercase() ?: return false
        return allowlistedHosts.any { host == it || host.endsWith(".$it") }
    }
}
