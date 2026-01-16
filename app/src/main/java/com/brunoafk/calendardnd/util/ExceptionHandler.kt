package com.brunoafk.calendardnd.util

import android.util.Log

/**
 * Centralized exception handling utilities.
 */
object ExceptionHandler {

    private const val TAG = "CalendarDND"

    fun handleDndException(e: Exception, operation: String): Boolean {
        return when (e) {
            is SecurityException -> {
                Log.w(TAG, "SecurityException during $operation - possible OEM quirk", e)
                AnalyticsTracker.logException("dnd_security", operation)
                false
            }
            is IllegalStateException -> {
                Log.e(TAG, "IllegalStateException during $operation", e)
                AnalyticsTracker.logException("dnd_illegal_state", operation)
                false
            }
            is IllegalArgumentException -> {
                Log.e(TAG, "IllegalArgumentException during $operation", e)
                AnalyticsTracker.logException("dnd_illegal_arg", operation)
                false
            }
            else -> {
                Log.e(TAG, "Unexpected exception during $operation", e)
                AnalyticsTracker.logException("dnd_unknown", operation)
                false
            }
        }
    }

    fun handleCalendarException(e: Exception, operation: String): Boolean {
        return when (e) {
            is SecurityException -> {
                Log.w(TAG, "SecurityException during $operation - permission revoked?", e)
                AnalyticsTracker.logException("calendar_security", operation)
                false
            }
            is IllegalArgumentException -> {
                Log.e(TAG, "IllegalArgumentException during $operation", e)
                AnalyticsTracker.logException("calendar_illegal_arg", operation)
                false
            }
            is android.database.CursorIndexOutOfBoundsException -> {
                Log.e(TAG, "CursorIndexOutOfBoundsException during $operation", e)
                AnalyticsTracker.logException("calendar_cursor", operation)
                false
            }
            else -> {
                Log.e(TAG, "Unexpected exception during $operation", e)
                AnalyticsTracker.logException("calendar_unknown", operation)
                false
            }
        }
    }

    fun handleAlarmException(e: Exception, operation: String): Boolean {
        return when (e) {
            is SecurityException -> {
                Log.w(TAG, "SecurityException during $operation - exact alarm permission revoked?", e)
                AnalyticsTracker.logException("alarm_security", operation)
                false
            }
            is IllegalStateException -> {
                Log.e(TAG, "IllegalStateException during $operation", e)
                AnalyticsTracker.logException("alarm_illegal_state", operation)
                false
            }
            else -> {
                Log.e(TAG, "Unexpected exception during $operation", e)
                AnalyticsTracker.logException("alarm_unknown", operation)
                false
            }
        }
    }
}
