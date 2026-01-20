package com.brunoafk.calendardnd.util

import com.brunoafk.calendardnd.BuildConfig

object AppConfig {
    val crashlyticsEnabled: Boolean = BuildConfig.CRASHLYTICS_ENABLED
    val analyticsEnabled: Boolean = BuildConfig.ANALYTICS_ENABLED
    val testerTelemetryDefault: Boolean = BuildConfig.TESTER_TELEMETRY_DEFAULT
    val umamiBaseUrl: String = BuildConfig.UMAMI_BASE_URL
    val umamiWebsiteId: String = BuildConfig.UMAMI_WEBSITE_ID
}
