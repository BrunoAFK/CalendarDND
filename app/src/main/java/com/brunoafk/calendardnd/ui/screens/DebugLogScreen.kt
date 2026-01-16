package com.brunoafk.calendardnd.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.widget.Toast
import com.brunoafk.calendardnd.data.prefs.DebugLogEntry
import com.brunoafk.calendardnd.data.prefs.DebugLogLevel
import com.brunoafk.calendardnd.data.prefs.DebugLogStore
import com.brunoafk.calendardnd.data.prefs.SettingsStore
import com.brunoafk.calendardnd.ui.components.EmptyStates
import com.brunoafk.calendardnd.ui.components.OneUiTopAppBar
import com.brunoafk.calendardnd.ui.theme.surfaceColorAtElevation
import com.brunoafk.calendardnd.util.AnalyticsTracker
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugLogScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val debugLogStore = remember { DebugLogStore(context) }
    val settingsStore = remember { SettingsStore(context) }

    val logEntries by debugLogStore.logEntries.collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var searchExpanded by remember { mutableStateOf(false) }
    val logLevelFilter by settingsStore.logLevelFilter.collectAsState(initial = DebugLogLevel.ALL)
    val filteredByLevel = remember(logEntries, logLevelFilter) {
        if (logLevelFilter == DebugLogLevel.ALL) {
            logEntries
        } else {
            logEntries.filter { it.level == logLevelFilter }
        }
    }
    val filteredBySearch = remember(filteredByLevel, searchQuery) {
        if (searchQuery.isBlank()) {
            filteredByLevel
        } else {
            filteredByLevel.filter { it.message.contains(searchQuery, ignoreCase = true) }
        }
    }
    val logSummaries = remember(filteredBySearch) {
        filteredBySearch.map { it.toLogSummary() }
    }

    LaunchedEffect(Unit) {
        AnalyticsTracker.logScreenView(context, "debug_logs")
    }

    Scaffold(
        topBar = {
            OneUiTopAppBar(
                onNavigateBack = onNavigateBack,
                title = stringResource(com.brunoafk.calendardnd.R.string.debug_logs),
                actions = {
                    if (searchExpanded) {
                        IconButton(
                            onClick = {
                                searchQuery = ""
                                searchExpanded = false
                            }
                        ) {
                            Icon(Icons.Default.Close, stringResource(com.brunoafk.calendardnd.R.string.close_search))
                        }
                    } else {
                        IconButton(
                            onClick = { searchExpanded = true }
                        ) {
                            Icon(Icons.Default.Search, stringResource(com.brunoafk.calendardnd.R.string.search_logs))
                        }
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                val logs = debugLogStore.getLogsAsString()
                                clipboardManager.setText(AnnotatedString(logs))
                                Toast.makeText(
                                    context,
                                    context.getString(com.brunoafk.calendardnd.R.string.logs_copied),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, stringResource(com.brunoafk.calendardnd.R.string.copy_logs))
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                val summary = debugLogStore.getRedactedShareSummary()
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, summary)
                                }
                                val chooser = Intent.createChooser(
                                    sendIntent,
                                    context.getString(com.brunoafk.calendardnd.R.string.debug_logs_share)
                                )
                                context.startActivity(chooser)
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Share,
                            stringResource(com.brunoafk.calendardnd.R.string.debug_logs_share)
                        )
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                debugLogStore.clearLogs()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Delete, stringResource(com.brunoafk.calendardnd.R.string.clear_logs))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                if (searchExpanded) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(stringResource(com.brunoafk.calendardnd.R.string.search_logs))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
            if (logSummaries.isEmpty()) {
                item {
                    EmptyStates.NoLogs()
                }
            } else {
                items(logSummaries, key = { it.id }) { log ->
                    var expanded by rememberSaveable(log.id) { mutableStateOf(false) }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = surfaceColorAtElevation(1.dp)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = log.header,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            val infoChunks = listOfNotNull(
                                log.trigger?.let { "Source: $it" },
                                log.action?.let { "Action: $it" },
                                log.activeWindow?.let { "Active window: $it" },
                                log.nextBoundary?.let { "Next boundary: $it" }
                            )
                            if (infoChunks.isNotEmpty()) {
                                Text(
                                    text = infoChunks.joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (expanded) {
                                Text(
                                    text = log.details,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(
                                onClick = { expanded = !expanded }
                            ) {
                                val labelRes = if (expanded) {
                                    com.brunoafk.calendardnd.R.string.debug_logs_hide_details
                                } else {
                                    com.brunoafk.calendardnd.R.string.debug_logs_show_details
                                }
                                Text(text = stringResource(labelRes))
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class LogSummary(
    val id: String,
    val level: DebugLogLevel,
    val header: String,
    val trigger: String?,
    val action: String?,
    val activeWindow: String?,
    val nextBoundary: String?,
    val details: String
)

private fun DebugLogEntry.toLogSummary(): LogSummary {
    val lines = message
        .lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }

    val baseHeader = lines.firstOrNull { it.startsWith("ENGINE RUN -") }
        ?: lines.firstOrNull()
        ?: message

    val header = "${level.displayName} • $baseHeader"
    val triggerLine = lines.firstOrNull { it.startsWith("Source:") }?.substringAfter(":")?.trim()
    val actionLine = lines.firstOrNull { it.startsWith("Action:") }?.substringAfter(":")?.trim()
    val activeWindowLine = lines.firstOrNull {
        it.startsWith("Active Window:") || it.startsWith("Active window:")
    }?.substringAfter(":")?.trim()
    val nextBoundaryLine = lines.firstOrNull { it.startsWith("Next boundary:") }?.substringAfter(":")?.trim()

    val idSource = buildString {
        append(header)
        append(triggerLine ?: "")
        append(actionLine ?: "")
        append(activeWindowLine ?: "")
        append(nextBoundaryLine ?: "")
        append(this@toLogSummary.hashCode())
    }

    val detailsText = lines.joinToString("\n")

    return LogSummary(
        id = idSource.hashCode().toString(),
        level = level,
        header = header,
        trigger = triggerLine,
        action = actionLine,
        activeWindow = activeWindowLine,
        nextBoundary = nextBoundaryLine,
        details = detailsText
    )
}
