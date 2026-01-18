package com.brunoafk.calendardnd.ui

import android.content.Intent
import android.os.Bundle
import android.service.quicksettings.TileService
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import com.brunoafk.calendardnd.BuildConfig
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.ui.navigation.AppNavigation
import com.brunoafk.calendardnd.ui.theme.CalendarDndTheme
import com.brunoafk.calendardnd.data.prefs.SettingsStore
import com.brunoafk.calendardnd.system.notifications.UpdateNotificationHelper
import com.brunoafk.calendardnd.system.update.ManualUpdateManager
import com.brunoafk.calendardnd.ui.components.ManualUpdatePrompt
import android.content.res.Configuration
import android.os.LocaleList
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import java.util.Locale
import com.brunoafk.calendardnd.util.ExternalLinkPolicy

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_OPEN_UPDATES = "open_updates"
        const val EXTRA_EXTERNAL_URL = "external_url"
        private const val UPDATES_DEEPLINK_HOST = "updates"
        private const val ABOUT_DEEPLINK_HOST = "about"
        private const val SETTINGS_DEEPLINK_HOST = "settings"
        private const val KEYWORDS_DEEPLINK_HOST = "keywords"
        private const val HELP_DEEPLINK_HOST = "help"
    }

    private val tileHintState = mutableStateOf(false)
    private val openUpdatesState = mutableStateOf(false)
    private val openAboutState = mutableStateOf(false)
    private val openSettingsState = mutableStateOf(false)
    private val openKeywordsState = mutableStateOf(false)
    private val openHelpState = mutableStateOf(false)
    private val pendingExternalUrlState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tileHintState.value = isTilePreferencesIntent(intent)
        openUpdatesState.value = shouldOpenUpdates(intent)
        openAboutState.value = shouldOpenAbout(intent)
        openSettingsState.value = shouldOpenSettings(intent)
        openKeywordsState.value = shouldOpenKeywords(intent)
        openHelpState.value = shouldOpenHelp(intent)
        extractExternalUrl(intent)?.let { pendingExternalUrlState.value = it }

        setContent {
            val baseContext = LocalContext.current
            val settingsStore = remember { SettingsStore(baseContext) }
            val preferredTag by settingsStore.preferredLanguageTag.collectAsState(initial = "")
            val supportedFallback = remember { resolveSupportedLanguage(Locale.getDefault().language) }
            val effectiveTag = if (preferredTag.isBlank()) supportedFallback else preferredTag
            val localizedContext = remember(baseContext, effectiveTag) {
                val config = Configuration(baseContext.resources.configuration)
                config.setLocales(LocaleList.forLanguageTags(effectiveTag))
                baseContext.createConfigurationContext(config)
            }
            var updatePrompt by remember { mutableStateOf<ManualUpdateManager.UpdatePrompt?>(null) }
            var updateStatus by remember { mutableStateOf<ManualUpdateManager.UpdateStatus?>(null) }
            var signatureStatus by remember {
                mutableStateOf(ManualUpdateManager.SignatureStatus(isAllowed = true, isPinned = true))
            }
            val openUpdates by openUpdatesState
            val openAbout by openAboutState
            val openSettings by openSettingsState
            val openKeywords by openKeywordsState
            val openHelp by openHelpState
            val pendingExternalUrl by pendingExternalUrlState

            LaunchedEffect(Unit) {
                if (BuildConfig.MANUAL_UPDATE_ENABLED) {
                    signatureStatus = ManualUpdateManager.getSignatureStatus(baseContext)
                    val updateResult = ManualUpdateManager.checkForUpdates(baseContext)
                    updatePrompt = updateResult.prompt
                    updateStatus = updateResult.status

                    updateResult.status?.let { status ->
                        val lastNotified = settingsStore.getLastNotificationUpdateVersion()
                        if (status.info.versionName != lastNotified) {
                            UpdateNotificationHelper.showUpdateNotification(baseContext, status.info)
                            settingsStore.setLastNotificationUpdateVersion(status.info.versionName)
                        }
                    }
                } else {
                    updatePrompt = null
                    updateStatus = null
                }
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalActivityResultRegistryOwner provides this
            ) {
                CalendarDndTheme {
                    val showTileHint by tileHintState
                    val view = LocalView.current
                    val useDarkIcons = !isSystemInDarkTheme()
                    val backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.background
                    @Suppress("DEPRECATION")
                    SideEffect {
                        WindowCompat.getInsetsController(window, view).apply {
                            isAppearanceLightStatusBars = useDarkIcons
                            isAppearanceLightNavigationBars = useDarkIcons
                        }
                        window.statusBarColor = backgroundColor.toArgb()
                        window.navigationBarColor = backgroundColor.toArgb()
                    }
                    Box(modifier = Modifier.fillMaxSize()) {
                        AppNavigation(
                            showTileHint = showTileHint,
                            onTileHintConsumed = { tileHintState.value = false },
                            updateStatus = updateStatus,
                            signatureStatus = signatureStatus,
                            openUpdates = openUpdates,
                            onOpenUpdatesConsumed = { openUpdatesState.value = false },
                            openAbout = openAbout,
                            onOpenAboutConsumed = { openAboutState.value = false },
                            openSettings = openSettings,
                            onOpenSettingsConsumed = { openSettingsState.value = false },
                            openKeywords = openKeywords,
                            onOpenKeywordsConsumed = { openKeywordsState.value = false },
                            openHelp = openHelp,
                            onOpenHelpConsumed = { openHelpState.value = false }
                        )
                    }
                    updatePrompt?.let { prompt ->
                        ManualUpdatePrompt(
                            prompt = prompt,
                            onDismiss = { updatePrompt = null }
                        )
                    }
                    pendingExternalUrl?.let { url ->
                        val hostLabel = remember(url) {
                            runCatching { Uri.parse(url) }.getOrNull()?.host ?: url
                        }
                        AlertDialog(
                            onDismissRequest = { pendingExternalUrlState.value = null },
                            title = { Text(stringResource(R.string.external_link_prompt_title)) },
                            text = {
                                Text(
                                    stringResource(
                                        R.string.external_link_prompt_message,
                                        hostLabel
                                    )
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        openExternalUrl(localizedContext, url)
                                        pendingExternalUrlState.value = null
                                    }
                                ) {
                                    Text(stringResource(R.string.external_link_open))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { pendingExternalUrlState.value = null }) {
                                    Text(stringResource(R.string.external_link_cancel))
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        tileHintState.value = isTilePreferencesIntent(intent)
        openUpdatesState.value = shouldOpenUpdates(intent)
        openAboutState.value = shouldOpenAbout(intent)
        openSettingsState.value = shouldOpenSettings(intent)
        openKeywordsState.value = shouldOpenKeywords(intent)
        openHelpState.value = shouldOpenHelp(intent)
        extractExternalUrl(intent)?.let { pendingExternalUrlState.value = it }
    }

    private fun isTilePreferencesIntent(intent: Intent?): Boolean {
        return intent?.action == TileService.ACTION_QS_TILE_PREFERENCES
    }

    private fun shouldOpenUpdates(intent: Intent?): Boolean {
        if (!BuildConfig.MANUAL_UPDATE_ENABLED) {
            return false
        }
        if (intent?.getBooleanExtra(EXTRA_OPEN_UPDATES, false) == true) {
            return true
        }
        val data = intent?.data ?: return false
        return data.scheme == "calendardnd" && data.host == UPDATES_DEEPLINK_HOST
    }

    private fun shouldOpenAbout(intent: Intent?): Boolean {
        val data = intent?.data ?: return false
        return data.scheme == "calendardnd" && data.host == ABOUT_DEEPLINK_HOST
    }

    private fun shouldOpenSettings(intent: Intent?): Boolean {
        val data = intent?.data ?: return false
        return data.scheme == "calendardnd" && data.host == SETTINGS_DEEPLINK_HOST
    }

    private fun shouldOpenKeywords(intent: Intent?): Boolean {
        val data = intent?.data ?: return false
        return data.scheme == "calendardnd" && data.host == KEYWORDS_DEEPLINK_HOST
    }

    private fun shouldOpenHelp(intent: Intent?): Boolean {
        val data = intent?.data ?: return false
        return data.scheme == "calendardnd" && data.host == HELP_DEEPLINK_HOST
    }

    private fun extractExternalUrl(intent: Intent?): String? {
        val rawUrl = intent?.getStringExtra(EXTRA_EXTERNAL_URL)?.trim().orEmpty()
        if (rawUrl.isBlank()) {
            return null
        }
        val uri = runCatching { Uri.parse(rawUrl) }.getOrNull() ?: return null
        if (ExternalLinkPolicy.isInternal(uri) || ExternalLinkPolicy.isAllowlistedExternal(uri)) {
            return null
        }
        if (uri.scheme != "https") {
            return null
        }
        return uri.toString()
    }

    private fun openExternalUrl(context: android.content.Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    private fun resolveSupportedLanguage(language: String): String {
        return when (language) {
            "en", "zh", "hr", "de", "it", "ko" -> language
            else -> "en"
        }
    }
}
