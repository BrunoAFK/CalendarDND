package com.brunoafk.calendardnd.data.prefs

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.brunoafk.calendardnd.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.debugLogDataStore: DataStore<Preferences> by preferencesDataStore(name = "debug_log")

class DebugLogStore(private val context: Context) {

    private val dataStore = context.debugLogDataStore

    companion object {
        private val LOG_ENTRIES = stringPreferencesKey("log_entries")
        private const val MAX_LINES = 500
        private const val MAX_LOG_SIZE_BYTES = 500_000
        private const val DELIMITER = "|||"
    }

    val logEntries: Flow<List<DebugLogEntry>> = dataStore.data.map { prefs ->
        val combined = prefs[LOG_ENTRIES] ?: ""
        if (combined.isEmpty()) {
            emptyList()
        } else {
            combined.split(DELIMITER).mapNotNull { raw ->
                raw.split("::", limit = 2).let { parts ->
                    if (parts.size == 2) {
                        val level = DebugLogLevel.fromString(parts[0])
                        DebugLogEntry(level, parts[1])
                    } else {
                        DebugLogEntry(DebugLogLevel.INFO, raw)
                    }
                }
            }
        }
    }

    suspend fun appendLog(level: DebugLogLevel, message: String) {
        val settingsStore = SettingsStore(context)
        val debugEnabled = settingsStore.debugToolsUnlocked.first()
        if (!debugEnabled) {
            return
        }
        val captureLevel = settingsStore.logLevelCapture.first()
        if (!DebugLogLevel.allows(level, captureLevel)) {
            return
        }
        dataStore.edit { prefs ->
            val existing = prefs[LOG_ENTRIES] ?: ""
            val entries = if (existing.isEmpty()) {
                emptyList()
            } else {
                existing.split(DELIMITER)
            }.toMutableList()

            val storedEntry = "${level.name}::$message"
            entries.add(0, storedEntry)

            val trimmedLines = if (entries.size > MAX_LINES) {
                entries.take(MAX_LINES)
            } else {
                entries
            }

            var trimmedLog = trimmedLines.joinToString(DELIMITER)
            if (trimmedLog.length > MAX_LOG_SIZE_BYTES) {
                val mutableEntries = trimmedLines.toMutableList()
                while (mutableEntries.isNotEmpty() && trimmedLog.length > MAX_LOG_SIZE_BYTES) {
                    mutableEntries.removeAt(mutableEntries.lastIndex)
                    trimmedLog = mutableEntries.joinToString(DELIMITER)
                }
            }

            prefs[LOG_ENTRIES] = trimmedLog
        }
    }

    suspend fun clearLogs() {
        dataStore.edit { prefs ->
            prefs.remove(LOG_ENTRIES)
        }
    }

    suspend fun getLogsAsString(): String {
        val settingsStore = SettingsStore(context)
        val settingsSnapshot = settingsStore.getSnapshot()
        val preferredLanguageTag = settingsStore.preferredLanguageTag.first()
        val analyticsOptIn = settingsStore.analyticsOptIn.first()
        val crashlyticsOptIn = settingsStore.crashlyticsOptIn.first()
        val logLevelFilter = settingsStore.logLevelFilter.first()
        val logLevelCapture = settingsStore.logLevelCapture.first()
        val includeDetails = settingsStore.debugLogIncludeDetails.first()

        val languageLabel = if (preferredLanguageTag.isBlank()) "system" else preferredLanguageTag
        val calendarScope = if (settingsSnapshot.selectedCalendarIds.isEmpty()) {
            "all"
        } else {
            settingsSnapshot.selectedCalendarIds.size.toString()
        }

        val header = buildString {
            appendLine("App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Language: $languageLabel")
            appendLine("Automation: ${settingsSnapshot.automationEnabled}")
            appendLine("Calendar scope: $calendarScope")
            appendLine("Busy only: ${settingsSnapshot.busyOnly}")
            appendLine("Ignore all-day: ${settingsSnapshot.ignoreAllDay}")
            appendLine("Min duration: ${settingsSnapshot.minEventMinutes}")
            appendLine("Require location: ${settingsSnapshot.requireLocation}")
            appendLine("DND mode: ${settingsSnapshot.dndMode.name}")
            appendLine("DND offset: ${settingsSnapshot.dndStartOffsetMinutes}")
            appendLine("Pre-DND notification: ${settingsSnapshot.preDndNotificationEnabled}")
            appendLine("Pre-DND timing: ${settingsSnapshot.preDndNotificationLeadMinutes}m")
            appendLine("Post-meeting notification: ${settingsSnapshot.postMeetingNotificationEnabled}")
            appendLine("Post-meeting timing: ${settingsSnapshot.postMeetingNotificationOffsetMinutes}m")
            appendLine("Vibration cool-down: ${settingsSnapshot.vibrationCooldownEnabled} (${settingsSnapshot.vibrationCooldownMinutes}m)")
            appendLine("Title filter: ${settingsSnapshot.requireTitleKeyword}")
            if (settingsSnapshot.requireTitleKeyword) {
                appendLine("Title filter mode: ${settingsSnapshot.titleKeywordMatchMode.name}")
                appendLine("Title filter case sensitive: ${settingsSnapshot.titleKeywordCaseSensitive}")
                appendLine("Title filter match all: ${settingsSnapshot.titleKeywordMatchAll}")
                appendLine("Title filter exclude: ${settingsSnapshot.titleKeywordExclude}")
                appendLine("Title filter pattern: [redacted]")
            }
            appendLine("Analytics opt-in: $analyticsOptIn")
            appendLine("Crashlytics opt-in: $crashlyticsOptIn")
            appendLine("Log capture: ${logLevelCapture.displayName}")
            appendLine("Log filter: ${logLevelFilter.displayName}")
            appendLine("Detailed logs: $includeDetails")
            appendLine("---")
        }

        val entries = logEntries.first()
        val logsText = entries.joinToString("\n") { "[${it.level.name}] ${it.message}" }
        return header + logsText
    }

    suspend fun getRedactedShareSummary(): String {
        val settingsStore = SettingsStore(context)
        val settingsSnapshot = settingsStore.getSnapshot()
        val preferredLanguageTag = settingsStore.preferredLanguageTag.first()
        val analyticsOptIn = settingsStore.analyticsOptIn.first()
        val crashlyticsOptIn = settingsStore.crashlyticsOptIn.first()
        val logLevelFilter = settingsStore.logLevelFilter.first()
        val logLevelCapture = settingsStore.logLevelCapture.first()
        val includeDetails = settingsStore.debugLogIncludeDetails.first()

        val languageLabel = if (preferredLanguageTag.isBlank()) "system" else preferredLanguageTag
        val calendarScope = if (settingsSnapshot.selectedCalendarIds.isEmpty()) {
            "all"
        } else {
            settingsSnapshot.selectedCalendarIds.size.toString()
        }

        val header = buildString {
            appendLine("App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Device: ${buildRedactedDeviceLabel()}")
            appendLine("Language: $languageLabel")
            appendLine("Automation: ${settingsSnapshot.automationEnabled}")
            appendLine("Calendar scope: $calendarScope")
            appendLine("Busy only: ${settingsSnapshot.busyOnly}")
            appendLine("Ignore all-day: ${settingsSnapshot.ignoreAllDay}")
            appendLine("Min duration: ${settingsSnapshot.minEventMinutes}")
            appendLine("Require location: ${settingsSnapshot.requireLocation}")
            appendLine("DND mode: ${settingsSnapshot.dndMode.name}")
            appendLine("DND offset: ${settingsSnapshot.dndStartOffsetMinutes}")
            appendLine("Pre-DND notification: ${settingsSnapshot.preDndNotificationEnabled}")
            appendLine("Pre-DND timing: ${settingsSnapshot.preDndNotificationLeadMinutes}m")
            appendLine("Post-meeting notification: ${settingsSnapshot.postMeetingNotificationEnabled}")
            appendLine("Post-meeting timing: ${settingsSnapshot.postMeetingNotificationOffsetMinutes}m")
            appendLine("Vibration cool-down: ${settingsSnapshot.vibrationCooldownEnabled} (${settingsSnapshot.vibrationCooldownMinutes}m)")
            appendLine("Title filter: ${settingsSnapshot.requireTitleKeyword}")
            if (settingsSnapshot.requireTitleKeyword) {
                appendLine("Title filter mode: ${settingsSnapshot.titleKeywordMatchMode.name}")
                appendLine("Title filter case sensitive: ${settingsSnapshot.titleKeywordCaseSensitive}")
                appendLine("Title filter match all: ${settingsSnapshot.titleKeywordMatchAll}")
                appendLine("Title filter exclude: ${settingsSnapshot.titleKeywordExclude}")
                appendLine("Title filter pattern: ${settingsSnapshot.titleKeyword}")
            }
            appendLine("Analytics opt-in: $analyticsOptIn")
            appendLine("Crashlytics opt-in: $crashlyticsOptIn")
            appendLine("Log capture: ${logLevelCapture.displayName}")
            appendLine("Log filter: ${logLevelFilter.displayName}")
            appendLine("Detailed logs: $includeDetails")
            appendLine("---")
        }

        val entries = logEntries.first()
        val logsText = entries.joinToString("\n") { "[${it.level.name}] ${it.message}" }
        return header + logsText
    }

    private fun buildRedactedDeviceLabel(): String {
        val manufacturer = Build.MANUFACTURER?.trim().orEmpty()
        if (manufacturer.isBlank()) {
            return "Unknown device"
        }
        val label = manufacturer.replaceFirstChar { it.uppercase() }
        return "$label device"
    }
}

data class DebugLogEntry(
    val level: DebugLogLevel,
    val message: String
)
