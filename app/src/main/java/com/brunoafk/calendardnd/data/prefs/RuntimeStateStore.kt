package com.brunoafk.calendardnd.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.brunoafk.calendardnd.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.runtimeStateDataStore: DataStore<Preferences> by preferencesDataStore(name = "runtime_state")

class RuntimeStateStore(private val context: Context) {

    private val dataStore = context.runtimeStateDataStore

    companion object {
        private val DND_SET_BY_APP = booleanPreferencesKey("dnd_set_by_app")
        private val ACTIVE_WINDOW_END_MS = longPreferencesKey("active_window_end_ms")
        private val USER_SUPPRESSED_UNTIL_MS = longPreferencesKey("user_suppressed_until_ms")
        private val USER_SUPPRESSED_FROM_MS = longPreferencesKey("user_suppressed_from_ms")
        private val MANUAL_DND_UNTIL_MS = longPreferencesKey("manual_dnd_until_ms")
        private val MANUAL_EVENT_START_MS = longPreferencesKey("manual_event_start_ms")
        private val MANUAL_EVENT_END_MS = longPreferencesKey("manual_event_end_ms")
        private val LAST_PLANNED_BOUNDARY_MS = longPreferencesKey("last_planned_boundary_ms")
        private val LAST_ENGINE_RUN_MS = longPreferencesKey("last_engine_run_ms")
        private val LAST_KNOWN_DND_FILTER = intPreferencesKey("last_known_dnd_filter")
        private val SKIPPED_EVENT_ID = longPreferencesKey("skipped_event_id")
        private val SKIPPED_EVENT_BEGIN_MS = longPreferencesKey("skipped_event_begin_ms")
        private val SKIPPED_EVENT_END_MS = longPreferencesKey("skipped_event_end_ms")
        private val NOTIFIED_NEW_EVENT_BEFORE_SKIP = booleanPreferencesKey("notified_new_event_before_skip")
        private val SAVED_RINGER_MODE = intPreferencesKey("saved_ringer_mode")
    }

    data class RuntimeStateSnapshot(
        val dndSetByApp: Boolean,
        val activeWindowEndMs: Long,
        val userSuppressedUntilMs: Long,
        val userSuppressedFromMs: Long,
        val manualDndUntilMs: Long,
        val manualEventStartMs: Long,
        val manualEventEndMs: Long,
        val lastKnownDndFilter: Int,
        val skippedEventId: Long,
        val skippedEventBeginMs: Long,
        val skippedEventEndMs: Long,
        val notifiedNewEventBeforeSkip: Boolean,
        val savedRingerMode: Int
    )

    val dndSetByApp: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[DND_SET_BY_APP] ?: false
    }

    val activeWindowEndMs: Flow<Long> = dataStore.data.map { prefs ->
        prefs[ACTIVE_WINDOW_END_MS] ?: 0L
    }

    val userSuppressedUntilMs: Flow<Long> = dataStore.data.map { prefs ->
        prefs[USER_SUPPRESSED_UNTIL_MS] ?: 0L
    }

    val userSuppressedFromMs: Flow<Long> = dataStore.data.map { prefs ->
        prefs[USER_SUPPRESSED_FROM_MS] ?: 0L
    }

    val manualDndUntilMs: Flow<Long> = dataStore.data.map { prefs ->
        prefs[MANUAL_DND_UNTIL_MS] ?: 0L
    }

    val manualEventStartMs: Flow<Long> = dataStore.data.map { prefs ->
        prefs[MANUAL_EVENT_START_MS] ?: 0L
    }

    val manualEventEndMs: Flow<Long> = dataStore.data.map { prefs ->
        prefs[MANUAL_EVENT_END_MS] ?: 0L
    }

    val lastPlannedBoundaryMs: Flow<Long> = dataStore.data.map { prefs ->
        prefs[LAST_PLANNED_BOUNDARY_MS] ?: 0L
    }

    val lastEngineRunMs: Flow<Long> = dataStore.data.map { prefs ->
        prefs[LAST_ENGINE_RUN_MS] ?: 0L
    }

    val lastKnownDndFilter: Flow<Int> = dataStore.data.map { prefs ->
        prefs[LAST_KNOWN_DND_FILTER] ?: -1
    }

    val skippedEventBeginMs: Flow<Long> = dataStore.data.map { prefs ->
        prefs[SKIPPED_EVENT_BEGIN_MS] ?: 0L
    }

    val skippedEventId: Flow<Long> = dataStore.data.map { prefs ->
        prefs[SKIPPED_EVENT_ID] ?: 0L
    }

    val skippedEventEndMs: Flow<Long> = dataStore.data.map { prefs ->
        prefs[SKIPPED_EVENT_END_MS] ?: 0L
    }

    val notifiedNewEventBeforeSkip: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[NOTIFIED_NEW_EVENT_BEFORE_SKIP] ?: false
    }

    val savedRingerMode: Flow<Int> = dataStore.data.map { prefs ->
        prefs[SAVED_RINGER_MODE] ?: -1
    }

    suspend fun getSnapshot(): RuntimeStateSnapshot {
        val prefs = dataStore.data.first()
        return RuntimeStateSnapshot(
            dndSetByApp = prefs[DND_SET_BY_APP] ?: false,
            activeWindowEndMs = prefs[ACTIVE_WINDOW_END_MS] ?: 0L,
            userSuppressedUntilMs = prefs[USER_SUPPRESSED_UNTIL_MS] ?: 0L,
            userSuppressedFromMs = prefs[USER_SUPPRESSED_FROM_MS] ?: 0L,
            manualDndUntilMs = prefs[MANUAL_DND_UNTIL_MS] ?: 0L,
            manualEventStartMs = prefs[MANUAL_EVENT_START_MS] ?: 0L,
            manualEventEndMs = prefs[MANUAL_EVENT_END_MS] ?: 0L,
            lastKnownDndFilter = prefs[LAST_KNOWN_DND_FILTER] ?: -1,
            skippedEventId = prefs[SKIPPED_EVENT_ID] ?: 0L,
            skippedEventBeginMs = prefs[SKIPPED_EVENT_BEGIN_MS] ?: 0L,
            skippedEventEndMs = prefs[SKIPPED_EVENT_END_MS] ?: 0L,
            notifiedNewEventBeforeSkip = prefs[NOTIFIED_NEW_EVENT_BEFORE_SKIP] ?: false,
            savedRingerMode = prefs[SAVED_RINGER_MODE] ?: -1
        )
    }

    suspend fun setDndSetByApp(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[DND_SET_BY_APP] = value
        }
    }

    suspend fun setActiveWindowEndMs(value: Long) {
        dataStore.edit { prefs ->
            prefs[ACTIVE_WINDOW_END_MS] = value
        }
    }

    suspend fun setUserSuppressedUntilMs(value: Long) {
        dataStore.edit { prefs ->
            prefs[USER_SUPPRESSED_UNTIL_MS] = value
        }
    }

    suspend fun setUserSuppressedFromMs(value: Long) {
        dataStore.edit { prefs ->
            prefs[USER_SUPPRESSED_FROM_MS] = value
        }
    }

    suspend fun setManualDndUntilMs(value: Long) {
        dataStore.edit { prefs ->
            prefs[MANUAL_DND_UNTIL_MS] = value
        }
    }

    suspend fun setManualEventStartMs(value: Long) {
        dataStore.edit { prefs ->
            prefs[MANUAL_EVENT_START_MS] = value
        }
    }

    suspend fun setManualEventEndMs(value: Long) {
        dataStore.edit { prefs ->
            prefs[MANUAL_EVENT_END_MS] = value
        }
    }

    suspend fun setLastPlannedBoundaryMs(value: Long) {
        dataStore.edit { prefs ->
            prefs[LAST_PLANNED_BOUNDARY_MS] = value
        }
    }

    suspend fun setLastEngineRunMs(value: Long) {
        dataStore.edit { prefs ->
            prefs[LAST_ENGINE_RUN_MS] = value
        }
    }

    suspend fun setLastKnownDndFilter(value: Int) {
        dataStore.edit { prefs ->
            prefs[LAST_KNOWN_DND_FILTER] = value
        }
    }

    suspend fun setSkippedEventBeginMs(value: Long) {
        dataStore.edit { prefs ->
            prefs[SKIPPED_EVENT_BEGIN_MS] = value
        }
    }

    suspend fun setSkippedEventId(value: Long) {
        dataStore.edit { prefs ->
            prefs[SKIPPED_EVENT_ID] = value
        }
    }

    suspend fun setSkippedEventEndMs(value: Long) {
        dataStore.edit { prefs ->
            prefs[SKIPPED_EVENT_END_MS] = value
        }
    }

    suspend fun setNotifiedNewEventBeforeSkip(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[NOTIFIED_NEW_EVENT_BEFORE_SKIP] = value
        }
    }

    suspend fun setSavedRingerMode(value: Int) {
        dataStore.edit { prefs ->
            prefs[SAVED_RINGER_MODE] = value
        }
    }

    suspend fun clearSavedRingerMode() {
        dataStore.edit { prefs ->
            prefs[SAVED_RINGER_MODE] = -1
        }
    }

    /**
     * Atomically sets all skipped event fields in a single transaction.
     * This prevents intermediate states where some fields are set but others aren't.
     */
    suspend fun setSkippedEvent(eventId: Long, beginMs: Long, endMs: Long) {
        dataStore.edit { prefs ->
            prefs[SKIPPED_EVENT_ID] = eventId
            prefs[SKIPPED_EVENT_BEGIN_MS] = beginMs
            prefs[SKIPPED_EVENT_END_MS] = endMs
            prefs[NOTIFIED_NEW_EVENT_BEFORE_SKIP] = false
        }
    }

    /**
     * Atomically clears all skipped event fields in a single transaction.
     */
    suspend fun clearSkippedEvent() {
        dataStore.edit { prefs ->
            prefs[SKIPPED_EVENT_ID] = 0L
            prefs[SKIPPED_EVENT_BEGIN_MS] = 0L
            prefs[SKIPPED_EVENT_END_MS] = 0L
            prefs[NOTIFIED_NEW_EVENT_BEFORE_SKIP] = false
        }
    }

    /**
     * Atomically sets manual event window in a single transaction.
     */
    suspend fun setManualEvent(startMs: Long, endMs: Long) {
        dataStore.edit { prefs ->
            prefs[MANUAL_EVENT_START_MS] = startMs
            prefs[MANUAL_EVENT_END_MS] = endMs
        }
    }

    /**
     * Atomically clears manual event window in a single transaction.
     */
    suspend fun clearManualEvent() {
        dataStore.edit { prefs ->
            prefs[MANUAL_EVENT_START_MS] = 0L
            prefs[MANUAL_EVENT_END_MS] = 0L
        }
    }

    /**
     * Atomically clears user suppression in a single transaction.
     */
    suspend fun clearUserSuppression() {
        dataStore.edit { prefs ->
            prefs[USER_SUPPRESSED_FROM_MS] = 0L
            prefs[USER_SUPPRESSED_UNTIL_MS] = 0L
        }
    }

    /**
     * Atomically clears all one-time action state (skip, manual event, suppression).
     */
    suspend fun clearAllOneTimeActions() {
        dataStore.edit { prefs ->
            prefs[SKIPPED_EVENT_ID] = 0L
            prefs[SKIPPED_EVENT_BEGIN_MS] = 0L
            prefs[SKIPPED_EVENT_END_MS] = 0L
            prefs[NOTIFIED_NEW_EVENT_BEFORE_SKIP] = false
            prefs[MANUAL_EVENT_START_MS] = 0L
            prefs[MANUAL_EVENT_END_MS] = 0L
            prefs[USER_SUPPRESSED_FROM_MS] = 0L
            prefs[USER_SUPPRESSED_UNTIL_MS] = 0L
        }
    }

    suspend fun getDndSetByAppValue(): Boolean {
        return dndSetByApp.first()
    }

    suspend fun getActiveWindowEndMsValue(): Long {
        return activeWindowEndMs.first()
    }

    suspend fun getUserSuppressedUntilMsValue(): Long {
        return userSuppressedUntilMs.first()
    }

    suspend fun getUserSuppressedFromMsValue(): Long {
        return userSuppressedFromMs.first()
    }

    suspend fun getManualDndUntilMsValue(): Long {
        return manualDndUntilMs.first()
    }

    suspend fun clearAll() {
        dataStore.edit { prefs ->
            prefs[DND_SET_BY_APP] = false
            prefs[ACTIVE_WINDOW_END_MS] = 0L
            prefs[USER_SUPPRESSED_UNTIL_MS] = 0L
            prefs[USER_SUPPRESSED_FROM_MS] = 0L
            prefs[MANUAL_DND_UNTIL_MS] = 0L
            prefs[MANUAL_EVENT_START_MS] = 0L
            prefs[MANUAL_EVENT_END_MS] = 0L
            prefs[LAST_PLANNED_BOUNDARY_MS] = 0L
            prefs[LAST_KNOWN_DND_FILTER] = -1
            prefs[SKIPPED_EVENT_ID] = 0L
            prefs[SKIPPED_EVENT_BEGIN_MS] = 0L
            prefs[SKIPPED_EVENT_END_MS] = 0L
            prefs[NOTIFIED_NEW_EVENT_BEFORE_SKIP] = false
            prefs[SAVED_RINGER_MODE] = -1
        }
    }
}
