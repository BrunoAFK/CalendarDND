package com.brunoafk.calendardnd.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.ui.components.OneUiTopAppBar
import com.brunoafk.calendardnd.ui.components.SettingsNavigationRow
import com.brunoafk.calendardnd.ui.components.SettingsSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val latestReleaseUrl = stringResource(R.string.github_latest_release_url)
    val releasesUrl = stringResource(R.string.github_releases_url)

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
            contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SettingsSection(title = stringResource(R.string.whats_new_latest_title)) {
                    Text(
                        text = stringResource(R.string.whats_new_latest_body),
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
                    Text(
                        text = stringResource(R.string.whats_new_features_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            item {
                SettingsSection(title = stringResource(R.string.whats_new_history_title)) {
                    SettingsNavigationRow(
                        title = stringResource(R.string.whats_new_history_action),
                        subtitle = stringResource(R.string.whats_new_history_subtitle),
                        onClick = { openUrl(releasesUrl) }
                    )
                }
            }
        }
    }
}
