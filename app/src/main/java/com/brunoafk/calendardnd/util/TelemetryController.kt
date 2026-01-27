package com.brunoafk.calendardnd.util

import android.content.Context
import android.os.Bundle
import java.util.Locale
import com.brunoafk.calendardnd.BuildConfig

object TelemetryController {

    private val VERSION_GATES = listOf(
        11200,
        11300,
        11400,
        11500,
        11600
    )

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

    fun subscribeToFcmTopics(languageTag: String) {
        if (!BuildConfig.FIREBASE_ENABLED) {
            return
        }
        runCatching {
            val messagingClass = Class.forName("com.google.firebase.messaging.FirebaseMessaging")
            val instance = messagingClass
                .getMethod("getInstance")
                .invoke(null)
            val subscribe = messagingClass.getMethod("subscribeToTopic", String::class.java)
            val topics = mutableSetOf(
                "all",
                "updates",
                "flavor_${BuildConfig.FLAVOR}",
                "v_${BuildConfig.VERSION_CODE}",
                languageTopic(languageTag)
            )
            topics += if (BuildConfig.DEBUG) "build_debug" else "build_release"
            VERSION_GATES.filter { BuildConfig.VERSION_CODE >= it }
                .forEach { gate ->
                    topics += "v_ge_${gate}"
                }
            topics.forEach { topic ->
                subscribe.invoke(instance, topic)
            }
        }
    }

    fun updateLanguageTopic(previousTag: String, currentTag: String) {
        if (!BuildConfig.FIREBASE_ENABLED) {
            return
        }
        val previous = languageTopic(previousTag)
        val current = languageTopic(currentTag)
        if (previous == current) {
            return
        }
        runCatching {
            val messagingClass = Class.forName("com.google.firebase.messaging.FirebaseMessaging")
            val instance = messagingClass
                .getMethod("getInstance")
                .invoke(null)
            val subscribe = messagingClass.getMethod("subscribeToTopic", String::class.java)
            val unsubscribe = messagingClass.getMethod("unsubscribeFromTopic", String::class.java)
            if (previous.isNotBlank()) {
                unsubscribe.invoke(instance, previous)
            }
            if (current.isNotBlank()) {
                subscribe.invoke(instance, current)
            }
        }
    }

    private fun languageTopic(tag: String): String {
        val normalized = tag.ifBlank { Locale.getDefault().language }
            .lowercase(Locale.ROOT)
            .replace('-', '_')
        return "lang_${normalized}"
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
