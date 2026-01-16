package com.brunoafk.calendardnd.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.brunoafk.calendardnd.BuildConfig
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.data.prefs.SettingsStore
import com.brunoafk.calendardnd.ui.components.OneUiTopAppBar
import com.brunoafk.calendardnd.ui.components.SettingsDivider
import com.brunoafk.calendardnd.ui.components.SettingsNavigationRow
import com.brunoafk.calendardnd.ui.components.SettingsSection
import com.brunoafk.calendardnd.ui.components.SettingsSwitchRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    onNavigateToWhatsNew: () -> Unit
) {
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore(context) }
    val scope = rememberCoroutineScope()
    val debugToolsUnlocked by settingsStore.debugToolsUnlocked.collectAsState(initial = false)
    val versionName = BuildConfig.VERSION_NAME
    val githubUrl = stringResource(R.string.github_repo_url)
    val issuesUrl = stringResource(R.string.github_issues_url)
    val releasesUrl = stringResource(R.string.github_releases_url)
    val versionReleaseUrl = "$releasesUrl/tag/v$versionName"

    fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.unable_to_open_link),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Scaffold(
        topBar = {
            OneUiTopAppBar(
                onNavigateBack = onNavigateBack,
                title = stringResource(R.string.about_title)
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
                Text(
                    text = stringResource(R.string.about_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                SettingsSection(title = stringResource(R.string.about_section_title)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = rememberRipple(),
                                onClick = { openUrl(versionReleaseUrl) }
                            )
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.about_version_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.app_version_name_only, versionName),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.about_version_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    SettingsDivider()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = rememberRipple(),
                                onClick = { openUrl(githubUrl) }
                            )
                            .padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.about_author_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.about_author_value),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = githubUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            item {
                SettingsSection(title = stringResource(R.string.about_report_title)) {
                    Column {
                        Text(
                            text = stringResource(R.string.about_report_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                        SettingsNavigationRow(
                            title = stringResource(R.string.github_repo_title),
                            subtitle = stringResource(R.string.github_repo_subtitle),
                            onClick = { openUrl(issuesUrl) }
                        )
                    }
                }
            }
            item {
                SettingsSection(title = stringResource(R.string.about_whats_new_title)) {
                    SettingsNavigationRow(
                        title = stringResource(R.string.about_whats_new_action),
                        subtitle = stringResource(R.string.about_whats_new_subtitle),
                        onClick = onNavigateToWhatsNew
                    )
                }
            }
            item {
                SettingsSection(title = stringResource(R.string.about_features_title)) {
                    SettingsSwitchRow(
                        title = stringResource(R.string.about_features_debug_title),
                        subtitle = stringResource(R.string.about_features_debug_subtitle),
                        checked = debugToolsUnlocked,
                        onCheckedChange = { enabled ->
                            scope.launch(Dispatchers.IO) {
                                settingsStore.setDebugToolsUnlocked(enabled)
                            }
                        }
                    )
                }
            }
        }
    }
}
