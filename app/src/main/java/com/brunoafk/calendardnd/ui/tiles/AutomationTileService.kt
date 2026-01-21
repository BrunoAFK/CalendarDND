package com.brunoafk.calendardnd.ui.tiles

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.data.calendar.CalendarRepository
import com.brunoafk.calendardnd.data.dnd.DndController
import com.brunoafk.calendardnd.data.prefs.RuntimeStateStore
import com.brunoafk.calendardnd.data.prefs.SettingsStore
import com.brunoafk.calendardnd.domain.model.Trigger
import com.brunoafk.calendardnd.system.alarms.EngineRunner
import com.brunoafk.calendardnd.ui.MainActivity
import com.brunoafk.calendardnd.util.AnalyticsTracker
import com.brunoafk.calendardnd.util.PermissionUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AutomationTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()

        scope.launch {
            val settingsStore = SettingsStore(applicationContext)
            val dndController = DndController(applicationContext)

            val currentlyEnabled = settingsStore.automationEnabled.first()
            val hasCalendarPermission = PermissionUtils.hasCalendarPermission(applicationContext)
            val hasPolicyAccess = dndController.hasPolicyAccess()

            if (currentlyEnabled) {
                // Turn OFF
                settingsStore.setAutomationEnabled(false)
                EngineRunner.runEngine(applicationContext, Trigger.TILE_TOGGLE)
                AnalyticsTracker.logAutomationToggle(applicationContext, false, "tile")
            } else {
                // Try to turn ON
                if (hasCalendarPermission && hasPolicyAccess) {
                    settingsStore.setAutomationEnabled(true)
                    EngineRunner.runEngine(applicationContext, Trigger.TILE_TOGGLE)
                    AnalyticsTracker.logAutomationToggle(applicationContext, true, "tile")
                } else {
                    // Missing permissions - open app
                    openApp()
                }
            }

            updateTileState()
        }
    }

    private fun updateTileState() {
        scope.launch {
            val settingsStore = SettingsStore(applicationContext)
            val runtimeStateStore = RuntimeStateStore(applicationContext)

            val automationEnabled = settingsStore.automationEnabled.first()
            val dndSetByApp = runtimeStateStore.dndSetByApp.first()
            val activeWindowEndMs = runtimeStateStore.activeWindowEndMs.first()

            val tileInfo = buildTileInfo(
                automationEnabled = automationEnabled,
                dndSetByApp = dndSetByApp,
                activeWindowEndMs = activeWindowEndMs
            )

            withContext(Dispatchers.Main) {
                qsTile?.apply {
                    state = if (automationEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                    label = getString(R.string.app_name)
                    setSubtitleCompat(tileInfo.subtitle)
                    contentDescription = tileInfo.contentDescription
                    icon = Icon.createWithResource(
                        applicationContext,
                        if (dndSetByApp) R.drawable.ic_dnd_on else R.drawable.ic_dnd_off
                    )
                    updateTile()
                }
            }
        }
    }

    private fun Tile.setSubtitleCompat(value: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            subtitle = value
        }
    }

    private suspend fun buildTileInfo(
        automationEnabled: Boolean,
        dndSetByApp: Boolean,
        activeWindowEndMs: Long
    ): TileInfo {
        if (!automationEnabled) {
            return TileInfo(
                subtitle = getString(R.string.tile_disabled),
                contentDescription = getString(R.string.tile_disabled_desc)
            )
        }

        if (!dndSetByApp || activeWindowEndMs <= 0) {
            val nextMeeting = getNextMeetingInfo()
            return if (nextMeeting != null) {
                TileInfo(
                    subtitle = getString(R.string.tile_next_prefix, nextMeeting.timeUntil),
                    contentDescription = getString(
                        R.string.tile_next_desc,
                        nextMeeting.timeUntil,
                        nextMeeting.title
                    )
                )
            } else {
                TileInfo(
                    subtitle = getString(R.string.tile_enabled),
                    contentDescription = getString(R.string.tile_enabled_desc)
                )
            }
        }

        val now = System.currentTimeMillis()
        val remainingMs = activeWindowEndMs - now
        val remainingMinutes = (remainingMs / 60000).toInt()

        val remainingText = when {
            remainingMinutes < 1 -> getString(R.string.tile_less_than_minute)
            remainingMinutes < 60 -> getString(R.string.tile_minutes, remainingMinutes)
            else -> {
                val hours = remainingMinutes / 60
                val mins = remainingMinutes % 60
                if (mins > 0) {
                    getString(R.string.tile_hours_minutes, hours, mins)
                } else {
                    getString(R.string.tile_hours, hours)
                }
            }
        }

        val currentMeetingTitle = getCurrentMeetingTitle()

        return TileInfo(
            subtitle = getString(R.string.tile_dnd_prefix, remainingText),
            contentDescription = if (currentMeetingTitle != null) {
                getString(R.string.tile_dnd_desc_with_title, remainingText, currentMeetingTitle)
            } else {
                getString(R.string.tile_dnd_desc, remainingText)
            }
        )
    }

    private suspend fun getNextMeetingInfo(): NextMeetingInfo? {
        return try {
            val settingsStore = SettingsStore(applicationContext)
            val settings = settingsStore.getSnapshot()
            val calendarRepository = CalendarRepository(applicationContext)

            val nextInstance = calendarRepository.getNextInstance(
                now = System.currentTimeMillis(),
                selectedCalendarIds = settings.selectedCalendarIds,
                busyOnly = settings.busyOnly,
                ignoreAllDay = settings.ignoreAllDay,
                minEventMinutes = settings.minEventMinutes,
                requireLocation = settings.requireLocation,
                requireTitleKeyword = settings.requireTitleKeyword,
                titleKeyword = settings.titleKeyword,
                titleKeywordMatchMode = settings.titleKeywordMatchMode,
                titleKeywordCaseSensitive = settings.titleKeywordCaseSensitive,
                titleKeywordMatchAll = settings.titleKeywordMatchAll,
                titleKeywordExclude = settings.titleKeywordExclude
            )

            nextInstance?.let {
                val minutesUntil = ((it.begin - System.currentTimeMillis()) / 60000).toInt()
                val timeUntil = when {
                    minutesUntil < 1 -> getString(R.string.tile_less_than_minute)
                    minutesUntil < 60 -> getString(R.string.tile_minutes, minutesUntil)
                    minutesUntil < 1440 -> getString(R.string.tile_hours, minutesUntil / 60)
                    else -> getString(R.string.tile_days, minutesUntil / 1440)
                }
                NextMeetingInfo(
                    title = it.title.ifBlank { getString(R.string.tile_meeting_fallback) },
                    timeUntil = timeUntil
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun getCurrentMeetingTitle(): String? {
        return try {
            val settingsStore = SettingsStore(applicationContext)
            val settings = settingsStore.getSnapshot()
            val calendarRepository = CalendarRepository(applicationContext)

            val activeInstances = calendarRepository.getActiveInstances(
                now = System.currentTimeMillis(),
                selectedCalendarIds = settings.selectedCalendarIds,
                busyOnly = settings.busyOnly,
                ignoreAllDay = settings.ignoreAllDay,
                minEventMinutes = settings.minEventMinutes,
                requireLocation = settings.requireLocation,
                requireTitleKeyword = settings.requireTitleKeyword,
                titleKeyword = settings.titleKeyword,
                titleKeywordMatchMode = settings.titleKeywordMatchMode,
                titleKeywordCaseSensitive = settings.titleKeywordCaseSensitive,
                titleKeywordMatchAll = settings.titleKeywordMatchAll,
                titleKeywordExclude = settings.titleKeywordExclude
            )

            activeInstances.firstOrNull()?.title?.ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openApp() {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private data class TileInfo(
        val subtitle: String,
        val contentDescription: String
    )

    private data class NextMeetingInfo(
        val title: String,
        val timeUntil: String
    )
}
