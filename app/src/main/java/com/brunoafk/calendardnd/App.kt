package com.brunoafk.calendardnd

import android.app.Application
import com.brunoafk.calendardnd.data.prefs.SettingsStore
import com.brunoafk.calendardnd.util.AppConfig
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this

        val settingsStore = SettingsStore(this)
        val crashlyticsOptIn = runBlocking { settingsStore.crashlyticsOptIn.first() }
        val analyticsOptIn = runBlocking { settingsStore.analyticsOptIn.first() }

        FirebaseCrashlytics.getInstance()
            .setCrashlyticsCollectionEnabled(AppConfig.crashlyticsEnabled && crashlyticsOptIn)

        FirebaseAnalytics.getInstance(this)
            .setAnalyticsCollectionEnabled(AppConfig.analyticsEnabled && analyticsOptIn)
    }

    companion object {
        lateinit var instance: App
            private set
    }
}
