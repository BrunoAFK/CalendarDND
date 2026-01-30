package com.brunoafk.calendardnd.data.dnd

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import com.brunoafk.calendardnd.domain.model.DndMode
import com.brunoafk.calendardnd.util.ExceptionHandler

class DndController(private val context: Context) {

    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val audioManager: AudioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    /**
     * Check if the app has Notification Policy Access permission
     */
    fun hasPolicyAccess(): Boolean {
        return notificationManager.isNotificationPolicyAccessGranted
    }

    /**
     * Get current system DND filter
     * Returns one of INTERRUPTION_FILTER_* constants
     */
    fun getCurrentFilter(): Int {
        return try {
            notificationManager.currentInterruptionFilter
        } catch (e: Exception) {
            ExceptionHandler.handleDndException(e, "getCurrentFilter")
            NotificationManager.INTERRUPTION_FILTER_UNKNOWN
        }
    }

    /**
     * Check if system DND is currently ON (any mode)
     */
    fun isDndOn(): Boolean {
        val filter = getCurrentFilter()
        return filter != NotificationManager.INTERRUPTION_FILTER_ALL &&
                filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
    }

    /**
     * Set DND mode
     * Requires hasPolicyAccess() to return true
     */
    fun setFilter(mode: DndMode): Boolean {
        if (!hasPolicyAccess()) {
            return false
        }

        return try {
            notificationManager.setInterruptionFilter(mode.filterValue)
            true
        } catch (e: Exception) {
            ExceptionHandler.handleDndException(e, "setFilter")
            false
        }
    }

    /**
     * Turn DND on with specified mode
     */
    fun enableDnd(mode: DndMode): Boolean {
        return setFilter(mode)
    }

    /**
     * Restore a specific interruption filter value.
     */
    fun setFilterValue(filterValue: Int): Boolean {
        if (!hasPolicyAccess()) {
            return false
        }

        return try {
            notificationManager.setInterruptionFilter(filterValue)
            true
        } catch (e: Exception) {
            ExceptionHandler.handleDndException(e, "setFilterValue")
            false
        }
    }

    /**
     * Turn DND off (set to ALL notifications)
     */
    fun disableDnd(): Boolean {
        if (!hasPolicyAccess()) {
            return false
        }

        return try {
            notificationManager.setInterruptionFilter(
                NotificationManager.INTERRUPTION_FILTER_ALL
            )
            true
        } catch (e: Exception) {
            ExceptionHandler.handleDndException(e, "disableDnd")
            false
        }
    }

    /**
     * Open system settings for Notification Policy Access
     */
    fun openPolicyAccessSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Open system settings for Priority interruptions.
     */
    fun openPrioritySettings() {
        val intent = Intent(Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            openPolicyAccessSettings()
        }
    }

    /**
     * Open system DND settings.
     */
    fun openZenModeSettings() {
        val intent = Intent("android.settings.ZEN_MODE_SETTINGS").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            openPolicyAccessSettings()
        }
    }

    /**
     * Get human-readable name of current DND state
     */
    fun getCurrentFilterName(): String {
        return when (getCurrentFilter()) {
            NotificationManager.INTERRUPTION_FILTER_ALL -> "Off"
            NotificationManager.INTERRUPTION_FILTER_PRIORITY -> "Priority Only"
            NotificationManager.INTERRUPTION_FILTER_NONE -> "Total Silence"
            NotificationManager.INTERRUPTION_FILTER_ALARMS -> "Alarms Only"
            else -> "Unknown"
        }
    }

    /**
     * Get current ringer mode (NORMAL, VIBRATE, or SILENT).
     * Returns -1 on failure.
     */
    fun getCurrentRingerMode(): Int {
        return try {
            audioManager.ringerMode
        } catch (e: Exception) {
            ExceptionHandler.handleDndException(e, "getCurrentRingerMode")
            -1
        }
    }

    /**
     * Set ringer mode to vibrate only.
     */
    fun setRingerModeVibrate(): Boolean {
        return try {
            audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            true
        } catch (e: Exception) {
            ExceptionHandler.handleDndException(e, "setRingerModeVibrate")
            false
        }
    }

    /**
     * Restore a previously saved ringer mode.
     */
    fun restoreRingerMode(mode: Int): Boolean {
        return try {
            audioManager.ringerMode = mode
            true
        } catch (e: Exception) {
            ExceptionHandler.handleDndException(e, "restoreRingerMode")
            false
        }
    }
}
