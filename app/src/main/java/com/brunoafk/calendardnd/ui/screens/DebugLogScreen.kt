package com.brunoafk.calendardnd.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontFamily
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
    val levelCounts = remember(logEntries) {
        val counts = mutableMapOf(
            DebugLogLevel.INFO to 0,
            DebugLogLevel.WARNING to 0,
            DebugLogLevel.ERROR to 0
        )
        logEntries.forEach { entry ->
            counts[entry.level] = (counts[entry.level] ?: 0) + 1
        }
        counts
    }
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
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
                LogLevelFilters(
                    current = logLevelFilter,
                    totals = levelCounts,
                    onSelect = { level ->
                        scope.launch {
                            settingsStore.setLogLevelFilter(level)
                        }
                    }
                )
            }
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
                itemsIndexed(logSummaries, key = { index, log -> "${log.id}-$index" }) { index, log ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LogHeaderRow(log)
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
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 0.dp
                        ) {
                            Text(
                                text = log.details,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        Divider(color = MaterialTheme.colorScheme.outlineVariant)
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
    val timestamp: String?,
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
    val timestamp = lines.firstOrNull { it.startsWith("[") && it.contains("]") }
        ?.substringAfter("[")
        ?.substringBefore("]")

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
        timestamp = timestamp,
        trigger = triggerLine,
        action = actionLine,
        activeWindow = activeWindowLine,
        nextBoundary = nextBoundaryLine,
        details = detailsText
    )
}

@Composable
private fun LogLevelFilters(
    current: DebugLogLevel,
    totals: Map<DebugLogLevel, Int>,
    onSelect: (DebugLogLevel) -> Unit
) {
    val allCount = totals.values.sum()
    val levels = listOf(
        DebugLogLevel.ALL to allCount,
        DebugLogLevel.INFO to (totals[DebugLogLevel.INFO] ?: 0),
        DebugLogLevel.WARNING to (totals[DebugLogLevel.WARNING] ?: 0),
        DebugLogLevel.ERROR to (totals[DebugLogLevel.ERROR] ?: 0)
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(levels.size) { index ->
            val (level, count) = levels[index]
            FilterChip(
                selected = current == level,
                onClick = { onSelect(level) },
                label = { Text("${level.displayName} ($count)") }
            )
        }
    }
}

@Composable
private fun LogHeaderRow(log: LogSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LevelPill(level = log.level)
        Text(
            text = log.header,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
    if (log.timestamp != null) {
        Text(
            text = "Time: ${log.timestamp}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LevelPill(level: DebugLogLevel) {
    val (bg, fg) = when (level) {
        DebugLogLevel.ERROR -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        DebugLogLevel.WARNING -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        DebugLogLevel.INFO -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        DebugLogLevel.ALL -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = bg
    ) {
        Text(
            text = level.displayName,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
