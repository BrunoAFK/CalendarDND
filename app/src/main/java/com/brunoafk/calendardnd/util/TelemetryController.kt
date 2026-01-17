package com.brunoafk.calendardnd.util

import android.content.Context
import android.os.Bundle
import com.brunoafk.calendardnd.BuildConfig

object TelemetryController {

    fun setAnalyticsEnabled(context: Context, enabled: Boolean) {
        if (!BuildConfig.FIREBASE_ENABLED || !AppConfig.analyticsEnabled) {
            return
        }
        runCatching {
            val analyticsClass = Class.forName("com.google.firebase.analytics.FirebaseAnalytics")
            val instance = analyticsClass
                .getMethod("getInstance", Context::class.java)
                .invoke(null, context)
            analyticsClass
                .getMethod("setAnalyticsCollectionEnabled", Boolean::class.javaPrimitiveType)
                .invoke(instance, enabled)
        }
    }

    fun setCrashlyticsEnabled(enabled: Boolean) {
        if (!BuildConfig.FIREBASE_ENABLED || !AppConfig.crashlyticsEnabled) {
            return
        }
        runCatching {
            val crashlyticsClass = Class.forName("com.google.firebase.crashlytics.FirebaseCrashlytics")
            val instance = crashlyticsClass
                .getMethod("getInstance")
                .invoke(null)
            crashlyticsClass
                .getMethod("setCrashlyticsCollectionEnabled", Boolean::class.javaPrimitiveType)
                .invoke(instance, enabled)
        }
    }

    fun setPerformanceEnabled(enabled: Boolean) {
        if (!BuildConfig.FIREBASE_ENABLED || !AppConfig.crashlyticsEnabled) {
            return
        }
        runCatching {
            val perfClass = Class.forName("com.google.firebase.perf.FirebasePerformance")
            val instance = perfClass
                .getMethod("getInstance")
                .invoke(null)
            val setter = perfClass.getMethod(
                "setPerformanceCollectionEnabled",
                Boolean::class.javaPrimitiveType
            )
            setter.invoke(instance, enabled)
        }
    }

    fun subscribeToUpdatesTopic() {
        if (!BuildConfig.FIREBASE_ENABLED) {
            return
        }
        runCatching {
            val messagingClass = Class.forName("com.google.firebase.messaging.FirebaseMessaging")
            val instance = messagingClass
                .getMethod("getInstance")
                .invoke(null)
            messagingClass
                .getMethod("subscribeToTopic", String::class.java)
                .invoke(instance, "updates")
        }
    }

    fun logEvent(context: Context, name: String, params: Bundle?) {
        if (!BuildConfig.FIREBASE_ENABLED || !AppConfig.analyticsEnabled) {
            return
        }
        runCatching {
            val analyticsClass = Class.forName("com.google.firebase.analytics.FirebaseAnalytics")
            val instance = analyticsClass
                .getMethod("getInstance", Context::class.java)
                .invoke(null, context)
            analyticsClass
                .getMethod("logEvent", String::class.java, Bundle::class.java)
                .invoke(instance, name, params)
        }
    }
}
