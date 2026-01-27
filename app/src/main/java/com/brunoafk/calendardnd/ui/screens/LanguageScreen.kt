package com.brunoafk.calendardnd.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.background
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.data.prefs.SettingsStore
import com.brunoafk.calendardnd.ui.components.PrimaryActionButton
import com.brunoafk.calendardnd.ui.components.OneUiTopAppBar
import com.brunoafk.calendardnd.ui.theme.surfaceColorAtElevation
import com.brunoafk.calendardnd.util.TelemetryController
import kotlinx.coroutines.launch

data class LanguageOption(
    val tag: String,
    val label: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(
    onNavigateBack: () -> Unit,
    onContinue: () -> Unit,
    showBack: Boolean = true,
    showContinue: Boolean = true,
    showDescription: Boolean = true
) {
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore(context) }
    val selectedTag by settingsStore.preferredLanguageTag.collectAsState(initial = "")
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val options = listOf(
        LanguageOption("en", stringResource(R.string.language_english).ifBlank { "English" }),
        LanguageOption("zh", stringResource(R.string.language_chinese).ifBlank { "中文" }),
        LanguageOption("hr", stringResource(R.string.language_croatian).ifBlank { "Hrvatski" }),
        LanguageOption("de", stringResource(R.string.language_german).ifBlank { "Deutsch" }),
        LanguageOption("it", stringResource(R.string.language_italian).ifBlank { "Italiano" }),
        LanguageOption("tr", stringResource(R.string.language_turkish).ifBlank { "Türkçe" }),
        LanguageOption("ko", stringResource(R.string.language_korean).ifBlank { "한국어" })
    )
    val continueLabel = stringResource(R.string.continue_button).ifBlank { "Continue" }
    var bottomBarHeightPx by remember { mutableStateOf(0) }
    val bottomBarHeight = with(LocalDensity.current) {
        val measured = bottomBarHeightPx.toDp()
        if (measured.value > 0f) measured else 88.dp
    }
    val bottomBarBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface.copy(alpha = 0f),
            MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            MaterialTheme.colorScheme.surface
        )
    )
    val contentBottomPadding = if (showContinue) bottomBarHeight else 0.dp

    Scaffold(
        topBar = {
            OneUiTopAppBar(
                onNavigateBack = if (showBack) onNavigateBack else null,
                title = stringResource(R.string.language_title)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = contentBottomPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (showDescription) {
                    val textBlockHeight = with(LocalDensity.current) {
                        MaterialTheme.typography.bodyMedium.lineHeight.toDp() * 3
                    }
                    Box(modifier = Modifier.height(textBlockHeight)) {
                        Text(
                            text = stringResource(R.string.language_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        state = listState,
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(options) { option ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = surfaceColorAtElevation(1.dp)
                                ),
                                onClick = {
                                    val tag = option.tag
                                    scope.launch {
                                        TelemetryController.updateLanguageTopic(selectedTag, tag)
                                        settingsStore.setPreferredLanguageTag(tag)
                                    }
                                }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = option.label,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        RadioButton(
                                            selected = selectedTag == option.tag,
                                            onClick = {
                                                val tag = option.tag
                                                scope.launch {
                                                    settingsStore.setPreferredLanguageTag(tag)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
            }

            if (showContinue) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(bottomBarBrush)
                        .onSizeChanged { bottomBarHeightPx = it.height }
                        .padding(top = 18.dp, bottom = 14.dp)
                ) {
                    PrimaryActionButton(
                        label = continueLabel,
                        onClick = onContinue
                    )
                }
            }
        }
    }
}
