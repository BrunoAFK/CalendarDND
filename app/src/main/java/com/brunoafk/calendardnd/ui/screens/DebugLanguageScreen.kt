package com.brunoafk.calendardnd.ui.screens

import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.ui.components.OneUiTopAppBar

data class DebugStringEntry(
    val key: String,
    val value: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugLanguageScreen(
    languageTag: String,
    onNavigateBack: () -> Unit
) {
    val baseContext = LocalContext.current
    val languageLabel = when (languageTag) {
        "en" -> stringResource(R.string.language_english)
        "de" -> stringResource(R.string.language_german)
        "hr" -> stringResource(R.string.language_croatian)
        "it" -> stringResource(R.string.language_italian)
        "ko" -> stringResource(R.string.language_korean)
        else -> languageTag
    }

    val stringIds = remember {
        R.string::class.java.fields
            .mapNotNull { field ->
                val id = field.getInt(null)
                val name = field.name
                name to id
            }
            .sortedBy { it.first }
    }

    val entries = remember(baseContext, languageTag) {
        val config = Configuration(baseContext.resources.configuration)
        config.setLocales(LocaleList.forLanguageTags(languageTag))
        val localizedContext = baseContext.createConfigurationContext(config)
        stringIds.map { (name, id) ->
            DebugStringEntry(
                key = name,
                value = localizedContext.resources.getString(id)
            )
        }
    }

    Scaffold(
        topBar = {
            OneUiTopAppBar(
                onNavigateBack = onNavigateBack,
                title = stringResource(R.string.debug_language_title, languageLabel)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(entries) { entry ->
                Column {
                    Text(
                        text = entry.key,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = entry.value,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}
