package com.brunoafk.calendardnd.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import com.brunoafk.calendardnd.ui.components.OneUiTopAppBar
import com.brunoafk.calendardnd.ui.theme.surfaceColorAtElevation
import com.brunoafk.calendardnd.ui.components.PrimaryActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.data.prefs.SettingsStore
import com.brunoafk.calendardnd.util.AppConfig
import com.brunoafk.calendardnd.BuildConfig
import com.brunoafk.calendardnd.util.TelemetryController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    onNavigateBack: () -> Unit,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore(context) }
    val scope = rememberCoroutineScope()
    val analyticsOptIn by settingsStore.analyticsOptIn.collectAsState(initial = false)
    val crashlyticsOptIn by settingsStore.crashlyticsOptIn.collectAsState(initial = true)

    Scaffold(
        topBar = {
            OneUiTopAppBar(
                onNavigateBack = onNavigateBack,
                title = stringResource(R.string.privacy_title)
            )
        }
    ) { padding ->
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = bottomBarHeight),
                verticalArrangement = Arrangement.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.privacy_change_later),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.privacy_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.privacy_defaults_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (BuildConfig.FIREBASE_ENABLED) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = surfaceColorAtElevation(1.dp)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.analytics_opt_in_title),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = stringResource(R.string.analytics_opt_in_description),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = analyticsOptIn,
                                    onCheckedChange = { enabled ->
                                        scope.launch {
                                            settingsStore.setAnalyticsOptIn(enabled)
                                            TelemetryController.setAnalyticsEnabled(
                                                context,
                                                AppConfig.analyticsEnabled && enabled
                                            )
                                        }
                                    }
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = surfaceColorAtElevation(1.dp)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.crashlytics_opt_in_title),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = stringResource(R.string.crashlytics_opt_in_description),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = crashlyticsOptIn,
                                    onCheckedChange = { enabled ->
                                        scope.launch {
                                            settingsStore.setCrashlyticsOptIn(enabled)
                                            TelemetryController.setCrashlyticsEnabled(
                                                AppConfig.crashlyticsEnabled && enabled
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(bottomBarBrush)
                    .onSizeChanged { bottomBarHeightPx = it.height }
                    .padding(top = 18.dp, bottom = 14.dp)
            ) {
                PrimaryActionButton(
                    label = stringResource(R.string.continue_button),
                    onClick = onContinue
                )
            }
        }
    }
}
