package com.brunoafk.calendardnd.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.BuildConfig
import com.brunoafk.calendardnd.system.update.ManualUpdateManager
import com.brunoafk.calendardnd.ui.components.OneUiTopAppBar
import com.brunoafk.calendardnd.ui.components.SettingsNavigationRow
import com.brunoafk.calendardnd.ui.components.SettingsSection
import com.brunoafk.calendardnd.ui.components.MarkdownText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewScreen(
    onNavigateBack: () -> Unit,
    onNavigateToUpdates: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latestReleaseUrl = stringResource(R.string.github_latest_release_url)
    var metadata by remember { mutableStateOf<ManualUpdateManager.UpdateMetadata?>(null) }
    val listState = rememberLazyListState()
    val buildVersionName = BuildConfig.VERSION_NAME.removeSuffix("-debug")

    LaunchedEffect(Unit) {
        metadata = ManualUpdateManager.fetchReleaseNotesMetadata()
    }

    val releases = metadata?.releases.orEmpty()
    val latestRelease = releases.firstOrNull()
    val currentRelease = releases.firstOrNull { it.versionName == buildVersionName }
    val highlightsText = currentRelease?.releaseNotes

    fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.unable_to_open_link),
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    Scaffold(
        topBar = {
            OneUiTopAppBar(
                onNavigateBack = onNavigateBack,
                title = stringResource(R.string.whats_new_title)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SettingsSection(title = stringResource(R.string.whats_new_latest_title)) {
                    Text(
                        text = latestRelease?.let {
                            context.getString(R.string.update_dialog_body, it.versionName)
                        } ?: stringResource(R.string.whats_new_latest_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                    SettingsNavigationRow(
                        title = stringResource(R.string.whats_new_latest_action),
                        subtitle = stringResource(R.string.whats_new_latest_subtitle),
                        onClick = { openUrl(latestReleaseUrl) }
                    )
                }
            }
            item {
                SettingsSection(title = stringResource(R.string.whats_new_features_title)) {
                    MarkdownText(
                        text = highlightsText ?: stringResource(R.string.whats_new_features_body),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            item {
                SettingsSection(title = stringResource(R.string.whats_new_history_title)) {
                    SettingsNavigationRow(
                        title = stringResource(R.string.whats_new_history_action),
                        subtitle = stringResource(R.string.whats_new_history_subtitle),
                        onClick = onNavigateToUpdates
                    )
                }
            }
        }
    }
}
