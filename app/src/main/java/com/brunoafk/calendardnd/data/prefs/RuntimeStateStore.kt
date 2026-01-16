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
        private val MANUAL_DND_UNTIL_MS = longPreferencesKey("manual_dnd_until_ms")
        private val LAST_PLANNED_BOUNDARY_MS = longPreferencesKey("last_planned_boundary_ms")
        private val LAST_ENGINE_RUN_MS = longPreferencesKey("last_engine_run_ms")
        private val LAST_KNOWN_DND_FILTER = intPreferencesKey("last_known_dnd_filter")
    }

    data class RuntimeStateSnapshot(
        val dndSetByApp: Boolean,
        val activeWindowEndMs: Long,
        val userSuppressedUntilMs: Long,
        val manualDndUntilMs: Long,
        val lastKnownDndFilter: Int
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

    val manualDndUntilMs: Flow<Long> = dataStore.data.map { prefs ->
        prefs[MANUAL_DND_UNTIL_MS] ?: 0L
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

    suspend fun getSnapshot(): RuntimeStateSnapshot {
        val prefs = dataStore.data.first()
        return RuntimeStateSnapshot(
            dndSetByApp = prefs[DND_SET_BY_APP] ?: false,
            activeWindowEndMs = prefs[ACTIVE_WINDOW_END_MS] ?: 0L,
            userSuppressedUntilMs = prefs[USER_SUPPRESSED_UNTIL_MS] ?: 0L,
            manualDndUntilMs = prefs[MANUAL_DND_UNTIL_MS] ?: 0L,
            lastKnownDndFilter = prefs[LAST_KNOWN_DND_FILTER] ?: -1
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

    suspend fun setManualDndUntilMs(value: Long) {
        dataStore.edit { prefs ->
            prefs[MANUAL_DND_UNTIL_MS] = value
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

    suspend fun getDndSetByAppValue(): Boolean {
        return dndSetByApp.first()
    }

    suspend fun getActiveWindowEndMsValue(): Long {
        return activeWindowEndMs.first()
    }

    suspend fun getUserSuppressedUntilMsValue(): Long {
        return userSuppressedUntilMs.first()
    }

    suspend fun getManualDndUntilMsValue(): Long {
        return manualDndUntilMs.first()
    }

    suspend fun clearAll() {
        dataStore.edit { prefs ->
            prefs[DND_SET_BY_APP] = false
            prefs[ACTIVE_WINDOW_END_MS] = 0L
            prefs[USER_SUPPRESSED_UNTIL_MS] = 0L
            prefs[MANUAL_DND_UNTIL_MS] = 0L
            prefs[LAST_PLANNED_BOUNDARY_MS] = 0L
            prefs[LAST_KNOWN_DND_FILTER] = -1
        }
    }
}
