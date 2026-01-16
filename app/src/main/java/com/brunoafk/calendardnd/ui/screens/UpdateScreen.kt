package com.brunoafk.calendardnd.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.brunoafk.calendardnd.BuildConfig
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.system.update.ManualUpdateManager
import com.brunoafk.calendardnd.ui.components.OneUiTopAppBar
import com.brunoafk.calendardnd.ui.theme.surfaceColorAtElevation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var metadata by remember { mutableStateOf<ManualUpdateManager.UpdateMetadata?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        metadata = ManualUpdateManager.fetchUpdateMetadata()
        isLoading = false
    }

    val releases = metadata?.releases.orEmpty()
    val latest = releases.firstOrNull()

    Scaffold(
        topBar = {
            OneUiTopAppBar(
                onNavigateBack = onNavigateBack,
                title = stringResource(R.string.update_screen_title)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.update_screen_current_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!BuildConfig.MANUAL_UPDATE_ENABLED) {
                item {
                    Text(
                        text = stringResource(R.string.update_screen_unavailable),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                return@LazyColumn
            }

            if (isLoading) {
                item {
                    Text(
                        text = stringResource(R.string.update_screen_loading),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                return@LazyColumn
            }

            if (latest == null) {
                item {
                    Text(
                        text = stringResource(R.string.update_screen_no_data),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                return@LazyColumn
            }

            item {
                Text(
                    text = stringResource(R.string.update_screen_latest_title),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = surfaceColorAtElevation(1.dp)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = latest.versionName,
                            style = MaterialTheme.typography.titleMedium
                        )
                        latest.releaseNotes?.let { notes ->
                            Text(
                                text = notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(latest.apkUrl))
                                context.startActivity(intent)
                            }
                        ) {
                            Text(stringResource(R.string.update_action_download))
                        }
                    }
                }
            }

            if (releases.size > 1) {
                item {
                    Text(
                        text = stringResource(R.string.update_screen_history_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            items(releases.drop(1)) { release ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = surfaceColorAtElevation(1.dp)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = release.versionName,
                            style = MaterialTheme.typography.titleMedium
                        )
                        release.releaseNotes?.let { notes ->
                            Text(
                                text = notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
