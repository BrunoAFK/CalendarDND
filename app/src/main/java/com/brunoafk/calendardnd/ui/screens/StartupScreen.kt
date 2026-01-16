package com.brunoafk.calendardnd.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.brunoafk.calendardnd.data.dnd.DndController
import com.brunoafk.calendardnd.data.prefs.SettingsStore
import com.brunoafk.calendardnd.util.PermissionUtils
import kotlinx.coroutines.flow.first
import java.util.Locale

@Composable
fun StartupScreen(
    onGoIntro: () -> Unit,
    onGoPermissions: () -> Unit,
    onGoStatus: () -> Unit
) {
    val context = LocalContext.current
    val settingsStore = SettingsStore(context)
    val dndController = DndController(context)

    LaunchedEffect(Unit) {
        val onboardingCompleted = settingsStore.onboardingCompleted.first()
        val preferredTag = settingsStore.preferredLanguageTag.first()
        if (preferredTag.isBlank()) {
            val supported = setOf("en", "de", "hr", "it", "ko")
            val deviceLang = Locale.getDefault().language
            val initialTag = if (supported.contains(deviceLang)) deviceLang else "en"
            settingsStore.setPreferredLanguageTag(initialTag)
        }
        if (!onboardingCompleted) {
            onGoIntro()
            return@LaunchedEffect
        }

        val hasCalendarPermission = PermissionUtils.hasCalendarPermission(context)
        val hasPolicyAccess = dndController.hasPolicyAccess()
        if (!hasCalendarPermission || !hasPolicyAccess) {
            onGoPermissions()
        } else {
            onGoStatus()
        }
    }
}
