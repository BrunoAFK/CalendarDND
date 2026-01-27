package com.brunoafk.calendardnd.data.prefs

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.brunoafk.calendardnd.BuildConfig
import com.brunoafk.calendardnd.data.prefs.DebugLogFilterLevel
import com.brunoafk.calendardnd.data.prefs.DebugLogLevel
import com.brunoafk.calendardnd.domain.model.DndMode
import com.brunoafk.calendardnd.domain.model.KeywordMatchMode
import com.brunoafk.calendardnd.domain.model.TelemetryLevel
import com.brunoafk.calendardnd.domain.model.ThemeMode
import com.brunoafk.calendardnd.domain.model.ThemeDebugMode
import com.brunoafk.calendardnd.domain.model.EventHighlightPreset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {

    private val dataStore = context.settingsDataStore
    private val debugLogStore by lazy { DebugLogStore(context) }

    companion object {
        private val AUTOMATION_ENABLED = booleanPreferencesKey("automation_enabled")
        private val SELECTED_CALENDAR_IDS = stringSetPreferencesKey("selected_calendar_ids")
        private val BUSY_ONLY = booleanPreferencesKey("busy_only")
        private val IGNORE_ALL_DAY = booleanPreferencesKey("ignore_all_day")
        private val SKIP_RECURRING = booleanPreferencesKey("skip_recurring")
        private val SELECTED_DAYS_MASK = intPreferencesKey("selected_days_mask")
        private val SELECTED_DAYS_ENABLED = booleanPreferencesKey("selected_days_enabled")
        private val MIN_EVENT_MINUTES = intPreferencesKey("min_event_minutes")
        private val REQUIRE_LOCATION = booleanPreferencesKey("require_location")
        private val DND_MODE = stringPreferencesKey("dnd_mode")
        private val DND_START_OFFSET_MINUTES = intPreferencesKey("dnd_start_offset_minutes")
        private val PRE_DND_NOTIFICATION_ENABLED = booleanPreferencesKey("pre_dnd_notification_enabled")
        private val PRE_DND_NOTIFICATION_USER_SET = booleanPreferencesKey("pre_dnd_notification_user_set")
        private val PRE_DND_NOTIFICATION_LEAD_MINUTES = intPreferencesKey("pre_dnd_notification_lead_minutes")
        private val PREFERRED_LANGUAGE_TAG = stringPreferencesKey("preferred_language_tag")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val LEGACY_THEME_ENABLED = booleanPreferencesKey("legacy_theme_enabled")
        private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val ANALYTICS_OPT_IN = booleanPreferencesKey("analytics_opt_in")
        private val CRASHLYTICS_OPT_IN = booleanPreferencesKey("crashlytics_opt_in")
        private val REQUIRE_TITLE_KEYWORD = booleanPreferencesKey("require_title_keyword")
        private val TITLE_KEYWORD = stringPreferencesKey("title_keyword")
        private val TITLE_KEYWORD_MATCH_MODE = stringPreferencesKey("title_keyword_match_mode")
        private val TITLE_KEYWORD_CASE_SENSITIVE = booleanPreferencesKey("title_keyword_case_sensitive")
        private val TITLE_KEYWORD_MATCH_ALL = booleanPreferencesKey("title_keyword_match_all")
        private val TITLE_KEYWORD_EXCLUDE = booleanPreferencesKey("title_keyword_exclude")
        private val LAST_IN_APP_UPDATE_VERSION = stringPreferencesKey("last_in_app_update_version")
        private val LAST_NOTIFICATION_UPDATE_VERSION = stringPreferencesKey("last_notification_update_version")
        private val LAST_SEEN_UPDATE_VERSION = stringPreferencesKey("last_seen_update_version")
        private val DEBUG_OVERLAY_ENABLED = booleanPreferencesKey("debug_overlay_enabled")
        private val TOTAL_SILENCE_CONFIRMED = booleanPreferencesKey("total_silence_confirmed")
        private val TOTAL_SILENCE_DIALOG_ENABLED = booleanPreferencesKey("total_silence_dialog_enabled")
        private val DEBUG_TOOLS_UNLOCKED = booleanPreferencesKey("debug_tools_unlocked")
        private val DND_MODE_BANNER_DISMISSED = booleanPreferencesKey("dnd_mode_banner_dismissed")
        private val REFRESH_BANNER_DISMISSED = booleanPreferencesKey("refresh_banner_dismissed")
        private val LOG_LEVEL_FILTER = stringPreferencesKey("log_level_filter")
        private val LOG_LEVEL_CAPTURE = stringPreferencesKey("log_level_capture")
        private val DEBUG_LOG_INCLUDE_DETAILS = booleanPreferencesKey("debug_log_include_details")
        private val THEME_DEBUG_MODE = stringPreferencesKey("theme_debug_mode")
        private val DEBUG_EVENT_HIGHLIGHT_PRESET = stringPreferencesKey("debug_event_highlight_preset")
        private val DEBUG_EVENT_HIGHLIGHT_HISTORY = stringPreferencesKey("debug_event_highlight_history")
        private val DEBUG_EVENT_HIGHLIGHT_FAVORITES = stringPreferencesKey("debug_event_highlight_favorites")
        private val DEBUG_EVENT_HIGHLIGHT_BAR_HIDDEN = booleanPreferencesKey("debug_event_highlight_bar_hidden")
        private val SIGNING_CERT_FINGERPRINT = stringPreferencesKey("signing_cert_fingerprint")
        private val POST_MEETING_NOTIFICATION_ENABLED = booleanPreferencesKey("post_meeting_notification_enabled")
        private val POST_MEETING_NOTIFICATION_OFFSET_MINUTES =
            intPreferencesKey("post_meeting_notification_offset_minutes")
        private val POST_MEETING_NOTIFICATION_SILENT = booleanPreferencesKey("post_meeting_notification_silent")
        private val TELEMETRY_ENABLED = booleanPreferencesKey("telemetry_enabled")
        private val TELEMETRY_LEVEL = stringPreferencesKey("telemetry_level")
        private val TESTER_TELEMETRY_ENABLED = booleanPreferencesKey("tester_telemetry_enabled")
        private val INSTALL_ID = stringPreferencesKey("install_id")
        private val INSTALL_PING_SENT = booleanPreferencesKey("install_ping_sent")
        private val LAST_DAILY_PING_DATE = stringPreferencesKey("last_daily_ping_date")
        private val FIRST_OPEN_MS = longPreferencesKey("first_open_ms")
        private val APP_OPEN_COUNT = intPreferencesKey("app_open_count")
        private val REVIEW_PROMPT_SHOWN = booleanPreferencesKey("review_prompt_shown")
        private val MODERN_BANNER_DESIGN = booleanPreferencesKey("modern_banner_design")
        private val ONE_TIME_ACTION_CONFIRMATION = booleanPreferencesKey("one_time_action_confirmation")
    }

    data class SettingsSnapshot(
        val automationEnabled: Boolean,
        val selectedCalendarIds: Set<String>,
        val busyOnly: Boolean,
        val ignoreAllDay: Boolean,
        val skipRecurring: Boolean,
        val selectedDaysEnabled: Boolean,
        val selectedDaysMask: Int,
        val minEventMinutes: Int,
        val requireLocation: Boolean,
        val dndMode: DndMode,
        val dndStartOffsetMinutes: Int,
        val preDndNotificationEnabled: Boolean,
        val preDndNotificationLeadMinutes: Int,
        val requireTitleKeyword: Boolean,
        val titleKeyword: String,
        val titleKeywordMatchMode: KeywordMatchMode,
        val titleKeywordCaseSensitive: Boolean,
        val titleKeywordMatchAll: Boolean,
        val titleKeywordExclude: Boolean,
        val crashlyticsOptIn: Boolean,
        val postMeetingNotificationEnabled: Boolean,
        val postMeetingNotificationOffsetMinutes: Int,
        val postMeetingNotificationSilent: Boolean
    )

    data class ReviewPromptState(
        val firstOpenMs: Long,
        val appOpenCount: Int,
        val promptShown: Boolean
    )

    val automationEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[AUTOMATION_ENABLED] ?: true
    }

    val oneTimeActionConfirmation: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[ONE_TIME_ACTION_CONFIRMATION] ?: true
    }

    val selectedCalendarIds: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[SELECTED_CALENDAR_IDS] ?: emptySet()
    }

    val busyOnly: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[BUSY_ONLY] ?: true
    }

    val ignoreAllDay: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[IGNORE_ALL_DAY] ?: true
    }

    val skipRecurring: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[SKIP_RECURRING] ?: false
    }

    val selectedDaysMask: Flow<Int> = dataStore.data.map { prefs ->
        prefs[SELECTED_DAYS_MASK] ?: com.brunoafk.calendardnd.util.WeekdayMask.ALL_DAYS_MASK
    }

    val selectedDaysEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[SELECTED_DAYS_ENABLED] ?: false
    }

    val minEventMinutes: Flow<Int> = dataStore.data.map { prefs ->
        prefs[MIN_EVENT_MINUTES] ?: 10
    }

    val requireLocation: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[REQUIRE_LOCATION] ?: false
    }

    val dndMode: Flow<DndMode> = dataStore.data.map { prefs ->
        DndMode.fromString(prefs[DND_MODE] ?: "PRIORITY")
    }

    val dndStartOffsetMinutes: Flow<Int> = dataStore.data.map { prefs ->
        prefs[DND_START_OFFSET_MINUTES] ?: 0
    }

    val preDndNotificationEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PRE_DND_NOTIFICATION_ENABLED] ?: false
    }

    val preDndNotificationUserSet: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[PRE_DND_NOTIFICATION_USER_SET] ?: false
    }

    val preDndNotificationLeadMinutes: Flow<Int> = dataStore.data.map { prefs ->
        prefs[PRE_DND_NOTIFICATION_LEAD_MINUTES] ?: 5
    }

    val preferredLanguageTag: Flow<String> = dataStore.data.map { prefs ->
        prefs[PREFERRED_LANGUAGE_TAG] ?: ""
    }

    val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        ThemeMode.fromString(prefs[THEME_MODE])
    }

    val legacyThemeEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[LEGACY_THEME_ENABLED] ?: (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R)
    }

    val onboardingCompleted: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[ONBOARDING_COMPLETED] ?: false
    }

    val analyticsOptIn: Flow<Boolean> = dataStore.data.map { prefs ->
        val defaultEnabled = BuildConfig.FLAVOR == "play"
        prefs[ANALYTICS_OPT_IN] ?: defaultEnabled
    }

    val crashlyticsOptIn: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[CRASHLYTICS_OPT_IN] ?: true
    }

    val requireTitleKeyword: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[REQUIRE_TITLE_KEYWORD] ?: false
    }

    val titleKeyword: Flow<String> = dataStore.data.map { prefs ->
        prefs[TITLE_KEYWORD] ?: ""
    }

    val titleKeywordMatchMode: Flow<KeywordMatchMode> = dataStore.data.map { prefs ->
        KeywordMatchMode.fromString(prefs[TITLE_KEYWORD_MATCH_MODE])
    }

    val titleKeywordCaseSensitive: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[TITLE_KEYWORD_CASE_SENSITIVE] ?: false
    }

    val titleKeywordMatchAll: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[TITLE_KEYWORD_MATCH_ALL] ?: false
    }

    val titleKeywordExclude: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[TITLE_KEYWORD_EXCLUDE] ?: false
    }

    val debugOverlayEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[DEBUG_OVERLAY_ENABLED] ?: false
    }

    val totalSilenceConfirmed: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[TOTAL_SILENCE_CONFIRMED] ?: false
    }

    val totalSilenceDialogEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[TOTAL_SILENCE_DIALOG_ENABLED] ?: false
    }

    val debugToolsUnlocked: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[DEBUG_TOOLS_UNLOCKED] ?: false
    }

    val dndModeBannerDismissed: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[DND_MODE_BANNER_DISMISSED] ?: false
    }

    val refreshBannerDismissed: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[REFRESH_BANNER_DISMISSED] ?: false
    }

    val logLevelFilter: Flow<DebugLogFilterLevel> = dataStore.data.map { prefs ->
        DebugLogFilterLevel.fromString(prefs[LOG_LEVEL_FILTER])
    }

    val logLevelCapture: Flow<DebugLogLevel> = dataStore.data.map { prefs ->
        DebugLogLevel.fromString(prefs[LOG_LEVEL_CAPTURE])
    }

    val debugLogIncludeDetails: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[DEBUG_LOG_INCLUDE_DETAILS] ?: false
    }
    val themeDebugMode: Flow<ThemeDebugMode> = dataStore.data.map { prefs ->
        val raw = prefs[THEME_DEBUG_MODE] ?: ThemeDebugMode.OFF.name
        runCatching { ThemeDebugMode.valueOf(raw) }.getOrDefault(ThemeDebugMode.OFF)
    }

    val debugEventHighlightPreset: Flow<EventHighlightPreset> = dataStore.data.map { prefs ->
        EventHighlightPreset.fromString(prefs[DEBUG_EVENT_HIGHLIGHT_PRESET])
    }

    val debugEventHighlightHistory: Flow<List<EventHighlightPreset>> = dataStore.data.map { prefs ->
        val raw = prefs[DEBUG_EVENT_HIGHLIGHT_HISTORY].orEmpty()
        if (raw.isBlank()) {
            emptyList()
        } else {
            raw.split(",")
                .mapNotNull { name -> runCatching { EventHighlightPreset.valueOf(name) }.getOrNull() }
        }
    }

    val debugEventHighlightFavorites: Flow<List<EventHighlightPreset>> = dataStore.data.map { prefs ->
        val raw = prefs[DEBUG_EVENT_HIGHLIGHT_FAVORITES].orEmpty()
        if (raw.isBlank()) {
            emptyList()
        } else {
            raw.split(",")
                .mapNotNull { name -> runCatching { EventHighlightPreset.valueOf(name) }.getOrNull() }
        }
    }

    val debugEventHighlightBarHidden: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[DEBUG_EVENT_HIGHLIGHT_BAR_HIDDEN] ?: false
    }

    val postMeetingNotificationEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[POST_MEETING_NOTIFICATION_ENABLED] ?: false
    }

    val postMeetingNotificationOffsetMinutes: Flow<Int> = dataStore.data.map { prefs ->
        prefs[POST_MEETING_NOTIFICATION_OFFSET_MINUTES] ?: 0
    }

    val postMeetingNotificationSilent: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[POST_MEETING_NOTIFICATION_SILENT] ?: true
    }

    val telemetryEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        when {
            prefs.contains(TELEMETRY_ENABLED) ->
                prefs[TELEMETRY_ENABLED] ?: com.brunoafk.calendardnd.util.AppConfig.telemetryDefaultEnabled
            prefs.contains(TESTER_TELEMETRY_ENABLED) ->
                prefs[TESTER_TELEMETRY_ENABLED] ?: com.brunoafk.calendardnd.util.AppConfig.telemetryDefaultEnabled
            else -> com.brunoafk.calendardnd.util.AppConfig.telemetryDefaultEnabled
        }
    }

    val telemetryLevel: Flow<TelemetryLevel> = dataStore.data.map { prefs ->
        if (prefs.contains(TELEMETRY_LEVEL)) {
            TelemetryLevel.fromString(prefs[TELEMETRY_LEVEL])
        } else {
            com.brunoafk.calendardnd.util.AppConfig.telemetryDefaultLevel
        }
    }

    val lastSeenUpdateVersion: Flow<String?> = dataStore.data.map { prefs ->
        prefs[LAST_SEEN_UPDATE_VERSION]
    }

    suspend fun getSnapshot(): SettingsSnapshot {
        val prefs = dataStore.data.first()
        return SettingsSnapshot(
            automationEnabled = prefs[AUTOMATION_ENABLED] ?: true,
            selectedCalendarIds = prefs[SELECTED_CALENDAR_IDS] ?: emptySet(),
            busyOnly = prefs[BUSY_ONLY] ?: true,
            ignoreAllDay = prefs[IGNORE_ALL_DAY] ?: true,
            skipRecurring = prefs[SKIP_RECURRING] ?: false,
            selectedDaysEnabled = prefs[SELECTED_DAYS_ENABLED] ?: false,
            selectedDaysMask = prefs[SELECTED_DAYS_MASK]
                ?: com.brunoafk.calendardnd.util.WeekdayMask.ALL_DAYS_MASK,
            minEventMinutes = prefs[MIN_EVENT_MINUTES] ?: 10,
            requireLocation = prefs[REQUIRE_LOCATION] ?: false,
            dndMode = DndMode.fromString(prefs[DND_MODE] ?: "PRIORITY"),
            dndStartOffsetMinutes = prefs[DND_START_OFFSET_MINUTES] ?: 0,
            preDndNotificationEnabled = prefs[PRE_DND_NOTIFICATION_ENABLED] ?: false,
            preDndNotificationLeadMinutes = prefs[PRE_DND_NOTIFICATION_LEAD_MINUTES] ?: 5,
            requireTitleKeyword = prefs[REQUIRE_TITLE_KEYWORD] ?: false,
            titleKeyword = prefs[TITLE_KEYWORD] ?: "",
            titleKeywordMatchMode = KeywordMatchMode.fromString(prefs[TITLE_KEYWORD_MATCH_MODE]),
            titleKeywordCaseSensitive = prefs[TITLE_KEYWORD_CASE_SENSITIVE] ?: false,
            titleKeywordMatchAll = prefs[TITLE_KEYWORD_MATCH_ALL] ?: false,
            titleKeywordExclude = prefs[TITLE_KEYWORD_EXCLUDE] ?: false,
            crashlyticsOptIn = prefs[CRASHLYTICS_OPT_IN] ?: true,
            postMeetingNotificationEnabled = prefs[POST_MEETING_NOTIFICATION_ENABLED] ?: false,
            postMeetingNotificationOffsetMinutes = prefs[POST_MEETING_NOTIFICATION_OFFSET_MINUTES] ?: 0,
            postMeetingNotificationSilent = prefs[POST_MEETING_NOTIFICATION_SILENT] ?: true
        )
    }

    suspend fun getSigningCertFingerprint(): String? {
        return dataStore.data.first()[SIGNING_CERT_FINGERPRINT]
    }

    suspend fun setSigningCertFingerprint(value: String) {
        dataStore.edit { prefs ->
            prefs[SIGNING_CERT_FINGERPRINT] = value
        }
    }

    suspend fun setAutomationEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[AUTOMATION_ENABLED] = enabled
        }
        logSettingChange("Automation", if (enabled) "enabled" else "disabled")
    }

    suspend fun setOneTimeActionConfirmation(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[ONE_TIME_ACTION_CONFIRMATION] = enabled
        }
        logSettingChange("One-time DND confirmation", enabled.toString())
    }

    suspend fun setSelectedCalendarIds(ids: Set<String>) {
        dataStore.edit { prefs ->
            prefs[SELECTED_CALENDAR_IDS] = ids
        }
    }

    suspend fun setBusyOnly(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[BUSY_ONLY] = enabled
        }
        logSettingChange("Busy only", enabled.toString())
    }

    suspend fun setIgnoreAllDay(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[IGNORE_ALL_DAY] = enabled
        }
        logSettingChange("Ignore all-day", enabled.toString())
    }

    suspend fun setSkipRecurring(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[SKIP_RECURRING] = enabled
        }
        logSettingChange("Skip recurring", enabled.toString())
    }

    suspend fun setSelectedDaysEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[SELECTED_DAYS_ENABLED] = enabled
        }
        logSettingChange("Selected days enabled", enabled.toString())
    }

    suspend fun setSelectedDaysMask(mask: Int) {
        dataStore.edit { prefs ->
            prefs[SELECTED_DAYS_MASK] = mask
        }
        logSettingChange("Selected days mask", mask.toString())
    }

    suspend fun setMinEventMinutes(minutes: Int) {
        dataStore.edit { prefs ->
            prefs[MIN_EVENT_MINUTES] = minutes
        }
        logSettingChange("Min event minutes", minutes.toString())
    }

    suspend fun setRequireLocation(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[REQUIRE_LOCATION] = enabled
        }
        logSettingChange("Require location", enabled.toString())
    }

    suspend fun setRequireTitleKeyword(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[REQUIRE_TITLE_KEYWORD] = enabled
        }
        logSettingChange("Title keyword filter", enabled.toString())
    }

    suspend fun setTitleKeyword(keyword: String) {
        dataStore.edit { prefs ->
            prefs[TITLE_KEYWORD] = keyword
        }
        logSettingChange("Title keyword", keyword)
    }

    suspend fun setTitleKeywordMatchMode(mode: KeywordMatchMode) {
        dataStore.edit { prefs ->
            prefs[TITLE_KEYWORD_MATCH_MODE] = mode.name
        }
        logSettingChange("Title keyword match", mode.name)
    }

    suspend fun setTitleKeywordCaseSensitive(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[TITLE_KEYWORD_CASE_SENSITIVE] = enabled
        }
        logSettingChange("Title keyword case sensitive", enabled.toString())
    }

    suspend fun setTitleKeywordMatchAll(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[TITLE_KEYWORD_MATCH_ALL] = enabled
        }
        logSettingChange("Title keyword match all", enabled.toString())
    }

    suspend fun setTitleKeywordExclude(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[TITLE_KEYWORD_EXCLUDE] = enabled
        }
        logSettingChange("Title keyword exclude", enabled.toString())
    }

    suspend fun setDndMode(mode: DndMode) {
        dataStore.edit { prefs ->
            prefs[DND_MODE] = mode.name
        }
        logSettingChange("DND mode", mode.name)
    }

    suspend fun setDndStartOffsetMinutes(minutes: Int) {
        dataStore.edit { prefs ->
            prefs[DND_START_OFFSET_MINUTES] = minutes
        }
        logSettingChange("DND offset minutes", minutes.toString())
    }

    suspend fun setPreDndNotificationEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[PRE_DND_NOTIFICATION_ENABLED] = enabled
        }
        logSettingChange("Pre-DND notification", if (enabled) "enabled" else "disabled")
    }

    suspend fun setPreDndNotificationUserSet(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[PRE_DND_NOTIFICATION_USER_SET] = value
        }
    }

    suspend fun setPreDndNotificationLeadMinutes(minutes: Int) {
        dataStore.edit { prefs ->
            prefs[PRE_DND_NOTIFICATION_LEAD_MINUTES] = minutes
        }
        logSettingChange("Pre-DND notification timing", "${minutes}m")
    }

    suspend fun setPostMeetingNotificationEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[POST_MEETING_NOTIFICATION_ENABLED] = enabled
        }
        logSettingChange("Post-meeting notification", if (enabled) "enabled" else "disabled")
    }

    suspend fun setPostMeetingNotificationOffsetMinutes(minutes: Int) {
        dataStore.edit { prefs ->
            prefs[POST_MEETING_NOTIFICATION_OFFSET_MINUTES] = minutes
        }
        logSettingChange("Post-meeting notification timing", "${minutes}m")
    }

    suspend fun setPostMeetingNotificationSilent(silent: Boolean) {
        dataStore.edit { prefs ->
            prefs[POST_MEETING_NOTIFICATION_SILENT] = silent
        }
        logSettingChange("Post-meeting notification silent", silent.toString())
    }

    suspend fun setTelemetryEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[TELEMETRY_ENABLED] = enabled
        }
        logSettingChange("Telemetry", if (enabled) "enabled" else "disabled")
    }

    suspend fun setTelemetryLevel(level: TelemetryLevel) {
        dataStore.edit { prefs ->
            prefs[TELEMETRY_LEVEL] = level.name
        }
        logSettingChange("Telemetry level", level.name)
    }

    suspend fun getOrCreateInstallId(): String {
        val prefs = dataStore.data.first()
        val existing = prefs[INSTALL_ID]
        if (!existing.isNullOrBlank()) {
            return existing
        }
        val generated = java.util.UUID.randomUUID().toString()
        dataStore.edit { it[INSTALL_ID] = generated }
        return generated
    }

    suspend fun getInstallPingSent(): Boolean {
        return dataStore.data.first()[INSTALL_PING_SENT] ?: false
    }

    suspend fun setInstallPingSent(sent: Boolean) {
        dataStore.edit { it[INSTALL_PING_SENT] = sent }
    }

    suspend fun getLastDailyPingDate(): String? {
        return dataStore.data.first()[LAST_DAILY_PING_DATE]
    }

    suspend fun setLastDailyPingDate(date: String) {
        dataStore.edit { it[LAST_DAILY_PING_DATE] = date }
    }

    suspend fun recordAppOpen(nowMs: Long): ReviewPromptState {
        val prefs = dataStore.data.first()
        val firstOpenMs = prefs[FIRST_OPEN_MS] ?: nowMs
        val appOpenCount = (prefs[APP_OPEN_COUNT] ?: 0) + 1
        val promptShown = prefs[REVIEW_PROMPT_SHOWN] ?: false
        dataStore.edit {
            it[FIRST_OPEN_MS] = firstOpenMs
            it[APP_OPEN_COUNT] = appOpenCount
        }
        return ReviewPromptState(
            firstOpenMs = firstOpenMs,
            appOpenCount = appOpenCount,
            promptShown = promptShown
        )
    }

    suspend fun setReviewPromptShown(shown: Boolean) {
        dataStore.edit { it[REVIEW_PROMPT_SHOWN] = shown }
    }

    val modernBannerDesign: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[MODERN_BANNER_DESIGN] ?: false
    }

    suspend fun setModernBannerDesign(enabled: Boolean) {
        dataStore.edit { it[MODERN_BANNER_DESIGN] = enabled }
    }

    suspend fun getPreferredLanguageTag(): String {
        return dataStore.data.first()[PREFERRED_LANGUAGE_TAG].orEmpty()
    }

    suspend fun setPreferredLanguageTag(tag: String) {
        dataStore.edit { prefs ->
            prefs[PREFERRED_LANGUAGE_TAG] = tag
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[THEME_MODE] = mode.name
        }
        logSettingChange("Theme mode", mode.name)
    }

    suspend fun setLegacyThemeEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[LEGACY_THEME_ENABLED] = enabled
        }
        logSettingChange("Legacy theme", enabled.toString())
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setAnalyticsOptIn(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[ANALYTICS_OPT_IN] = enabled
        }
    }

    suspend fun setCrashlyticsOptIn(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[CRASHLYTICS_OPT_IN] = enabled
        }
    }

    suspend fun getLastInAppUpdateVersion(): String? {
        return dataStore.data.first()[LAST_IN_APP_UPDATE_VERSION]
    }

    suspend fun setLastInAppUpdateVersion(versionName: String) {
        dataStore.edit { prefs ->
            prefs[LAST_IN_APP_UPDATE_VERSION] = versionName
        }
    }

    suspend fun getLastNotificationUpdateVersion(): String? {
        return dataStore.data.first()[LAST_NOTIFICATION_UPDATE_VERSION]
    }

    suspend fun setLastNotificationUpdateVersion(versionName: String) {
        dataStore.edit { prefs ->
            prefs[LAST_NOTIFICATION_UPDATE_VERSION] = versionName
        }
    }

    suspend fun setLastSeenUpdateVersion(versionName: String) {
        dataStore.edit { prefs ->
            prefs[LAST_SEEN_UPDATE_VERSION] = versionName
        }
    }

    suspend fun setDebugOverlayEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[DEBUG_OVERLAY_ENABLED] = enabled
        }
        logSettingChange("Debug overlay", if (enabled) "enabled" else "disabled")
    }

    suspend fun setTotalSilenceConfirmed(confirmed: Boolean) {
        dataStore.edit { prefs ->
            prefs[TOTAL_SILENCE_CONFIRMED] = confirmed
        }
    }

    suspend fun setTotalSilenceDialogEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[TOTAL_SILENCE_DIALOG_ENABLED] = enabled
        }
        logSettingChange("Total silence dialog", if (enabled) "enabled" else "disabled")
    }

    suspend fun setDebugToolsUnlocked(unlocked: Boolean) {
        dataStore.edit { prefs ->
            prefs[DEBUG_TOOLS_UNLOCKED] = unlocked
        }
    }

    suspend fun setDndModeBannerDismissed(dismissed: Boolean) {
        dataStore.edit { prefs ->
            prefs[DND_MODE_BANNER_DISMISSED] = dismissed
        }
    }

    suspend fun setRefreshBannerDismissed(dismissed: Boolean) {
        dataStore.edit { prefs ->
            prefs[REFRESH_BANNER_DISMISSED] = dismissed
        }
    }

    suspend fun setLogLevelFilter(level: DebugLogFilterLevel) {
        dataStore.edit { prefs ->
            prefs[LOG_LEVEL_FILTER] = level.name
        }
        logSettingChange("Log level filter", level.displayName)
    }

    suspend fun setLogLevelCapture(level: DebugLogLevel) {
        dataStore.edit { prefs ->
            prefs[LOG_LEVEL_CAPTURE] = level.name
        }
        logSettingChange("Log level capture", level.displayName)
    }

    suspend fun setDebugLogIncludeDetails(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[DEBUG_LOG_INCLUDE_DETAILS] = enabled
        }
        logSettingChange("Log include details", if (enabled) "enabled" else "disabled")
    }
    suspend fun setThemeDebugMode(mode: ThemeDebugMode) {
        dataStore.edit { prefs ->
            prefs[THEME_DEBUG_MODE] = mode.name
        }
        logSettingChange("Theme debug mode", mode.name.lowercase())
    }

    suspend fun setDebugEventHighlightPreset(preset: EventHighlightPreset) {
        dataStore.edit { prefs ->
            prefs[DEBUG_EVENT_HIGHLIGHT_PRESET] = preset.name
        }
        logSettingChange("Debug event highlight preset", preset.name.lowercase())
    }

    suspend fun setDebugEventHighlightHistory(presets: List<EventHighlightPreset>) {
        dataStore.edit { prefs ->
            prefs[DEBUG_EVENT_HIGHLIGHT_HISTORY] = presets.joinToString(",") { it.name }
        }
        logSettingChange("Debug event highlight history", presets.joinToString(",") { it.name.lowercase() })
    }

    suspend fun setDebugEventHighlightFavorites(presets: List<EventHighlightPreset>) {
        dataStore.edit { prefs ->
            prefs[DEBUG_EVENT_HIGHLIGHT_FAVORITES] = presets.joinToString(",") { it.name }
        }
        logSettingChange("Debug event highlight favorites", presets.joinToString(",") { it.name.lowercase() })
    }

    suspend fun setDebugEventHighlightBarHidden(hidden: Boolean) {
        dataStore.edit { prefs ->
            prefs[DEBUG_EVENT_HIGHLIGHT_BAR_HIDDEN] = hidden
        }
        logSettingChange("Debug event highlight bar", if (hidden) "hidden" else "shown")
    }

    private suspend fun logSettingChange(label: String, value: String) {
        debugLogStore.appendLog(DebugLogLevel.INFO, "SETTING: $label -> $value")
    }
}
