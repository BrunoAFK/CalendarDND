package com.brunoafk.calendardnd.util

import com.brunoafk.calendardnd.BuildConfig

object PerformanceTracer {

    fun newTrace(name: String): PerformanceTrace? {
        if (!BuildConfig.FIREBASE_ENABLED || !AppConfig.crashlyticsEnabled) {
            return null
        }
        val perfClass = runCatching {
            Class.forName("com.google.firebase.perf.FirebasePerformance")
        }.getOrNull() ?: return null
        val instance = runCatching {
            perfClass.getMethod("getInstance").invoke(null)
        }.getOrNull() ?: return null
        val trace = runCatching {
            perfClass.getMethod("newTrace", String::class.java).invoke(instance, name)
        }.getOrNull() ?: return null
        return PerformanceTrace(trace)
    }
}

class PerformanceTrace(private val delegate: Any) {

    fun putAttribute(key: String, value: String) {
        runCatching {
            delegate.javaClass
                .getMethod("putAttribute", String::class.java, String::class.java)
                .invoke(delegate, key, value)
        }
    }

    fun incrementMetric(name: String, value: Long) {
        runCatching {
            delegate.javaClass
                .getMethod("incrementMetric", String::class.java, Long::class.javaPrimitiveType)
                .invoke(delegate, name, value)
        }
    }

    fun start() {
        runCatching {
            delegate.javaClass.getMethod("start").invoke(delegate)
        }
    }

    fun stop() {
        runCatching {
            delegate.javaClass.getMethod("stop").invoke(delegate)
        }
    }
}
