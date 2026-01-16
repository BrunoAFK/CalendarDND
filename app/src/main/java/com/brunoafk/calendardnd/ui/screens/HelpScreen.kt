package com.brunoafk.calendardnd.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.ui.components.OneUiTopAppBar
import com.brunoafk.calendardnd.ui.theme.surfaceColorAtElevation
import com.brunoafk.calendardnd.util.AnalyticsTracker

data class HelpItem(
    val id: String,
    val title: String,
    val summary: String,
    val details: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    var searchQuery by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        AnalyticsTracker.logScreenView(context, "help")
    }

    val allItems = listOf(
        HelpItem(
            id = "automation",
            title = stringResource(R.string.help_item_automation_title),
            summary = stringResource(R.string.help_item_automation_summary),
            details = stringResource(R.string.help_item_automation_details)
        ),
        HelpItem(
            id = "permissions",
            title = stringResource(R.string.help_item_permissions_title),
            summary = stringResource(R.string.help_item_permissions_summary),
            details = stringResource(R.string.help_item_permissions_details)
        ),
        HelpItem(
            id = "language",
            title = stringResource(R.string.help_item_language_title),
            summary = stringResource(R.string.help_item_language_summary),
            details = stringResource(R.string.help_item_language_details)
        ),
        HelpItem(
            id = "privacy",
            title = stringResource(R.string.help_item_privacy_title),
            summary = stringResource(R.string.help_item_privacy_summary),
            details = stringResource(R.string.help_item_privacy_details)
        ),
        HelpItem(
            id = "updates",
            title = stringResource(R.string.help_item_updates_title),
            summary = stringResource(R.string.help_item_updates_summary),
            details = stringResource(R.string.help_item_updates_details)
        ),
        HelpItem(
            id = "exact_alarms",
            title = stringResource(R.string.help_item_exact_alarms_title),
            summary = stringResource(R.string.help_item_exact_alarms_summary),
            details = stringResource(R.string.help_item_exact_alarms_details)
        ),
        HelpItem(
            id = "battery",
            title = stringResource(R.string.help_item_battery_title),
            summary = stringResource(R.string.help_item_battery_summary),
            details = stringResource(R.string.help_item_battery_details)
        ),
        HelpItem(
            id = "busy_only",
            title = stringResource(R.string.help_item_busy_only_title),
            summary = stringResource(R.string.help_item_busy_only_summary),
            details = stringResource(R.string.help_item_busy_only_details)
        ),
        HelpItem(
            id = "ignore_all_day",
            title = stringResource(R.string.help_item_ignore_all_day_title),
            summary = stringResource(R.string.help_item_ignore_all_day_summary),
            details = stringResource(R.string.help_item_ignore_all_day_details)
        ),
        HelpItem(
            id = "min_duration",
            title = stringResource(R.string.help_item_min_duration_title),
            summary = stringResource(R.string.help_item_min_duration_summary),
            details = stringResource(R.string.help_item_min_duration_details)
        ),
        HelpItem(
            id = "dnd_mode",
            title = stringResource(R.string.help_item_dnd_mode_title),
            summary = stringResource(R.string.help_item_dnd_mode_summary),
            details = stringResource(R.string.help_item_dnd_mode_details)
        ),
        HelpItem(
            id = "dnd_timing",
            title = stringResource(R.string.help_item_dnd_timing_title),
            summary = stringResource(R.string.help_item_dnd_timing_summary),
            details = stringResource(R.string.help_item_dnd_timing_details)
        ),
        HelpItem(
            id = "pre_dnd_notification",
            title = stringResource(R.string.help_item_pre_dnd_notification_title),
            summary = stringResource(R.string.help_item_pre_dnd_notification_summary),
            details = stringResource(R.string.help_item_pre_dnd_notification_details)
        ),
        HelpItem(
            id = "tile",
            title = stringResource(R.string.help_item_tile_title),
            summary = stringResource(R.string.help_item_tile_summary),
            details = stringResource(R.string.help_item_tile_details)
        ),
        HelpItem(
            id = "debug_logs",
            title = stringResource(R.string.help_item_debug_logs_title),
            summary = stringResource(R.string.help_item_debug_logs_summary),
            details = stringResource(R.string.help_item_debug_logs_details)
        ),
        HelpItem(
            id = "pull_refresh",
            title = stringResource(R.string.help_item_pull_refresh_title),
            summary = stringResource(R.string.help_item_pull_refresh_summary),
            details = stringResource(R.string.help_item_pull_refresh_details)
        )
    )

    val filteredItems by remember(searchQuery) {
        derivedStateOf {
            if (searchQuery.isBlank()) {
                allItems
            } else {
                val query = searchQuery.lowercase()
                allItems.filter { item ->
                    item.title.lowercase().contains(query) ||
                    item.summary.lowercase().contains(query) ||
                    item.details.lowercase().contains(query)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            OneUiTopAppBar(
                onNavigateBack = onNavigateBack,
                title = stringResource(R.string.help_title)
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
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.help_search_placeholder)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.help_search_clear)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { keyboardController?.hide() }
                    )
                )
            }

            if (filteredItems.isEmpty() && searchQuery.isNotBlank()) {
                item {
                    Text(
                        text = stringResource(R.string.help_search_no_results),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }

            items(filteredItems) { item ->
                val isExpanded = expanded[item.id] ?: false
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = surfaceColorAtElevation(1.dp)
                    ),
                    onClick = { expanded[item.id] = !isExpanded }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = item.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = expandVertically(animationSpec = tween(200)),
                            exit = shrinkVertically(animationSpec = tween(200))
                        ) {
                            Text(
                                text = item.details,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
