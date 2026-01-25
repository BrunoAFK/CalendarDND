package com.brunoafk.calendardnd.util

import android.content.Context
import android.os.Bundle
import android.os.Build
import com.brunoafk.calendardnd.App
import com.brunoafk.calendardnd.util.TelemetryController

object AnalyticsTracker {

    fun logEngineRun(
        context: Context,
        trigger: String,
        action: String,
        exactAlarms: Boolean
    ) {
        logEvent(
            context,
            "engine_run",
            Bundle().apply {
                putString("trigger", trigger)
                putString("action", action)
                putBoolean("exact_alarms", exactAlarms)
            }
        )
    }

    fun logAutomationToggle(context: Context, enabled: Boolean, source: String) {
        logEvent(
            context,
            "automation_toggle",
            Bundle().apply {
                putBoolean("enabled", enabled)
                putString("source", source)
            }
        )
    }

    fun logSettingsChanged(context: Context, name: String, value: String) {
        logEvent(
            context,
            "settings_changed",
            Bundle().apply {
                putString("name", name)
                putString("value", value)
            }
        )
    }

    fun logScreenView(context: Context, screen: String) {
        logEvent(
            context,
            "screen_view",
            Bundle().apply {
                putString("screen_name", screen)
                putString("screen_class", "MainActivity")
            }
        )
    }

    fun logPreDndNotificationShown(context: Context) {
        logEvent(context, "pre_dnd_notification_shown", null)
    }

    fun logPreDndEnableNow(context: Context) {
        logEvent(context, "pre_dnd_enable_now", null)
    }

    fun logException(type: String, operation: String) {
        if (!AppConfig.analyticsEnabled) {
            return
        }

        val params = Bundle().apply {
            putString("exception_type", type)
            putString("operation", operation)
            putString("device_manufacturer", Build.MANUFACTURER)
            putString("device_model", Build.MODEL)
            putLong("android_version", Build.VERSION.SDK_INT.toLong())
        }

        try {
            TelemetryController.logEvent(App.instance, "exception", params)
        } catch (_: Exception) {
            // Ignore analytics failures to avoid impacting app behavior.
        }
    }

    private fun logEvent(context: Context, name: String, params: Bundle?) {
        if (!AppConfig.analyticsEnabled) {
            return
        }

        try {
            TelemetryController.logEvent(context, name, params)
        } catch (_: Exception) {
            // Ignore analytics failures to avoid impacting app behavior.
        }
    }
}
