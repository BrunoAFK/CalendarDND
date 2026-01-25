package com.brunoafk.calendardnd.util

import android.content.Context
import com.brunoafk.calendardnd.BuildConfig
import com.brunoafk.calendardnd.data.prefs.SettingsStore
import com.brunoafk.calendardnd.domain.model.TelemetryLevel
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import android.os.Build
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

object UmamiTelemetry {

    private const val INSTALL_EVENT = "app_install"
    private const val DAILY_EVENT = "app_active_daily"

    suspend fun trackAppOpenIfEnabled(context: Context) {
        val settingsStore = SettingsStore(context)
        if (!settingsStore.telemetryEnabled.first()) {
            return
        }
        val telemetryLevel = settingsStore.telemetryLevel.first()
        val appLanguage = if (telemetryLevel == TelemetryLevel.DETAILED) {
            settingsStore.getPreferredLanguageTag().ifBlank { "system" }
        } else {
            null
        }
        val analyticsEnabled = settingsStore.analyticsOptIn.first()
        val crashlyticsEnabled = settingsStore.crashlyticsOptIn.first()
        val baseUrl = AppConfig.umamiBaseUrl.trim().trimEnd('/')
        val websiteId = AppConfig.umamiWebsiteId.trim()
        if (baseUrl.isBlank() || websiteId.isBlank()) {
            return
        }

        val installId = settingsStore.getOrCreateInstallId()

        if (!settingsStore.getInstallPingSent()) {
            if (sendEvent(
                    baseUrl,
                    websiteId,
                    installId,
                    telemetryLevel,
                    appLanguage,
                    analyticsEnabled,
                    crashlyticsEnabled,
                    INSTALL_EVENT
                )
            ) {
                settingsStore.setInstallPingSent(true)
            }
        }

        val today = LocalDate.now(ZoneId.systemDefault()).toString()
        val lastDaily = settingsStore.getLastDailyPingDate()
        if (lastDaily != today) {
            if (sendEvent(
                    baseUrl,
                    websiteId,
                    installId,
                    telemetryLevel,
                    appLanguage,
                    analyticsEnabled,
                    crashlyticsEnabled,
                    DAILY_EVENT
                )
            ) {
                settingsStore.setLastDailyPingDate(today)
            }
        }
    }

    private fun sendEvent(
        baseUrl: String,
        websiteId: String,
        installId: String,
        telemetryLevel: TelemetryLevel,
        appLanguage: String?,
        analyticsEnabled: Boolean,
        crashlyticsEnabled: Boolean,
        eventName: String
    ): Boolean {
        val deviceLanguage = Locale.getDefault().toLanguageTag()
        val dataPayload = JSONObject()
            .put("install_id", installId)
            .put("app_version", BuildConfig.VERSION_NAME)
            .put("device_language", deviceLanguage)
            .put("flavor", BuildConfig.FLAVOR)
            .put("analytics_opt_in", analyticsEnabled)
            .put("crashlytics_opt_in", crashlyticsEnabled)

        if (telemetryLevel == TelemetryLevel.DETAILED) {
            dataPayload
                .put("app_language", appLanguage ?: "system")
                .put("os_version", Build.VERSION.RELEASE ?: "")
                .put("sdk_int", Build.VERSION.SDK_INT)
                .put("manufacturer", Build.MANUFACTURER ?: "")
                .put("model", Build.MODEL ?: "")
        }

        val payload = JSONObject()
            .put("website", websiteId)
            .put("hostname", "app")
            .put("language", deviceLanguage)
            .put("referrer", "")
            .put("screen", "0x0")
            .put("title", "Calendar DND")
            .put("url", "/$eventName")
            .put("name", eventName)
            .put("data", dataPayload)

        val body = JSONObject()
            .put("type", "event")
            .put("payload", payload)

        val url = URL("$baseUrl/api/send")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 6000
            readTimeout = 6000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }

        return try {
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(body.toString())
            }
            connection.inputStream.use { it.readBytes() }
            true
        } catch (_: Exception) {
            false
        } finally {
            connection.disconnect()
        }
    }
}
