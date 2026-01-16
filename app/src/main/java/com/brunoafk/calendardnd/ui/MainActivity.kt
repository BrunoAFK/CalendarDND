package com.brunoafk.calendardnd.ui

import android.content.Intent
import android.os.Bundle
import android.service.quicksettings.TileService
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.core.view.WindowCompat
import com.brunoafk.calendardnd.ui.navigation.AppNavigation
import com.brunoafk.calendardnd.ui.theme.CalendarDndTheme
import com.brunoafk.calendardnd.data.prefs.SettingsStore
import com.brunoafk.calendardnd.system.update.ManualUpdateManager
import com.brunoafk.calendardnd.ui.components.ManualUpdatePrompt
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val tileHintState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tileHintState.value = isTilePreferencesIntent(intent)

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

            LaunchedEffect(Unit) {
                updatePrompt = ManualUpdateManager.checkForUpdate(baseContext)
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
                    AppNavigation(
                        showTileHint = showTileHint,
                        onTileHintConsumed = { tileHintState.value = false }
                    )
                    updatePrompt?.let { prompt ->
                        ManualUpdatePrompt(
                            prompt = prompt,
                            onDismiss = { updatePrompt = null }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        tileHintState.value = isTilePreferencesIntent(intent)
    }

    private fun isTilePreferencesIntent(intent: Intent?): Boolean {
        return intent?.action == TileService.ACTION_QS_TILE_PREFERENCES
    }

    private fun resolveSupportedLanguage(language: String): String {
        return when (language) {
            "en", "zh", "hr", "de", "it", "ko" -> language
            else -> "en"
        }
    }
}
