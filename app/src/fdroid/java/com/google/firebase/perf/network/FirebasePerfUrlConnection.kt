package com.google.firebase.perf.network

import java.net.HttpURLConnection
import java.net.URLConnection
import javax.net.ssl.HttpsURLConnection

/**
 * F-Droid stub to satisfy Firebase Performance instrumentation.
 */
object FirebasePerfUrlConnection {
    @JvmStatic
    fun instrument(connection: URLConnection): URLConnection = connection

    @JvmStatic
    fun instrument(connection: HttpURLConnection): HttpURLConnection = connection

    @JvmStatic
    fun instrument(connection: HttpsURLConnection): HttpsURLConnection = connection
}
