package com.brunoafk.calendardnd

import android.app.Application
import com.brunoafk.calendardnd.data.prefs.SettingsStore
import com.brunoafk.calendardnd.util.AppConfig
import com.brunoafk.calendardnd.util.TelemetryController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        val settingsStore = SettingsStore(this)
        val crashlyticsOptIn = runBlocking { settingsStore.crashlyticsOptIn.first() }
        val analyticsOptIn = runBlocking { settingsStore.analyticsOptIn.first() }

        TelemetryController.setCrashlyticsEnabled(AppConfig.crashlyticsEnabled && crashlyticsOptIn)
        TelemetryController.setPerformanceEnabled(AppConfig.crashlyticsEnabled && crashlyticsOptIn)
        TelemetryController.setAnalyticsEnabled(this, AppConfig.analyticsEnabled && analyticsOptIn)
        TelemetryController.subscribeToUpdatesTopic()
    }

    companion object {
        lateinit var instance: App
            private set
    }
}
