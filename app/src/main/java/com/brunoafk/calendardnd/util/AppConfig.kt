package com.brunoafk.calendardnd.util

import com.brunoafk.calendardnd.BuildConfig
import com.brunoafk.calendardnd.domain.model.TelemetryLevel

object AppConfig {
    val crashlyticsEnabled: Boolean = BuildConfig.CRASHLYTICS_ENABLED
    val analyticsEnabled: Boolean = BuildConfig.ANALYTICS_ENABLED
    val telemetryDefaultEnabled: Boolean = BuildConfig.TELEMETRY_DEFAULT_ENABLED
    val telemetryDefaultLevel: TelemetryLevel =
        TelemetryLevel.fromString(BuildConfig.TELEMETRY_DEFAULT_LEVEL)
    val umamiBaseUrl: String = BuildConfig.UMAMI_BASE_URL
    val umamiWebsiteId: String = BuildConfig.UMAMI_WEBSITE_ID
}
