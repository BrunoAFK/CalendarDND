package com.brunoafk.calendardnd.util

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import com.brunoafk.calendardnd.data.prefs.SettingsStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

object LocaleUtils {
    fun localizedContext(context: Context): Context {
        val settingsStore = SettingsStore(context)
        val tag = runBlocking { settingsStore.preferredLanguageTag.first() }
        if (tag.isBlank()) {
            return context
        }
        val config = Configuration(context.resources.configuration)
        config.setLocales(LocaleList.forLanguageTags(tag))
        return context.createConfigurationContext(config)
    }
}
