package com.brunoafk.calendardnd.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.data.calendar.CalendarInfo
import com.brunoafk.calendardnd.data.calendar.CalendarRepository
import com.brunoafk.calendardnd.data.prefs.SettingsStore
import com.brunoafk.calendardnd.ui.components.EmptyStates
import com.brunoafk.calendardnd.ui.components.OneUiTopAppBar
import com.brunoafk.calendardnd.ui.components.PrimaryActionButton
import com.brunoafk.calendardnd.ui.theme.surfaceColorAtElevation
import com.brunoafk.calendardnd.util.AnalyticsTracker
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarPickerScreen(
    onNavigateBack: () -> Unit,
    onDone: (() -> Unit)? = null,
    isActive: Boolean = true
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsStore = remember { SettingsStore(context) }
    val calendarRepository = remember { CalendarRepository(context) }

    var calendars by remember { mutableStateOf<List<CalendarInfo>>(emptyList()) }
    val selectedIds by settingsStore.selectedCalendarIds.collectAsState(initial = emptySet())
    val canDone = selectedIds.isNotEmpty()
    val interactionEnabled = isActive

    LaunchedEffect(Unit) {
        AnalyticsTracker.logScreenView(context, "calendar_picker")
        calendars = calendarRepository.getCalendars()
    }

    val handleNavigateBack = {
        onNavigateBack()
    }

    BackHandler {
        handleNavigateBack()
    }

    Scaffold(
        topBar = {
            OneUiTopAppBar(
                onNavigateBack = handleNavigateBack,
                title = stringResource(R.string.select_calendars)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 0.dp,
                    end = 16.dp,
                    bottom = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (calendars.isEmpty()) {
                    item {
                        EmptyStates.NoCalendars(onAddAccount = { openAddAccount(context) })
                    }
                } else {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = surfaceColorAtElevation(1.dp)
                            ),
                            enabled = interactionEnabled,
                            onClick = {
                                scope.launch {
                                    settingsStore.setSelectedCalendarIds(emptySet())
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    stringResource(R.string.all_calendars),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                RadioButton(
                                    selected = selectedIds.isEmpty(),
                                    enabled = interactionEnabled,
                                    onClick = {
                                        scope.launch {
                                            settingsStore.setSelectedCalendarIds(emptySet())
                                        }
                                    }
                                )
                            }
                        }
                    }
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                    items(calendars) { calendar ->
                        val isSelected = selectedIds.contains(calendar.id.toString())

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = surfaceColorAtElevation(1.dp)
                            ),
                            enabled = interactionEnabled,
                            onClick = {
                                scope.launch {
                                    val newSelection = if (isSelected) {
                                        selectedIds - calendar.id.toString()
                                    } else {
                                        selectedIds + calendar.id.toString()
                                    }
                                    settingsStore.setSelectedCalendarIds(newSelection)
                                }
                            }
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
                                        calendar.displayName,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        calendar.accountName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Checkbox(
                                    checked = isSelected,
                                    enabled = interactionEnabled,
                                    onCheckedChange = { checked ->
                                        scope.launch {
                                            val newSelection = if (checked) {
                                                selectedIds + calendar.id.toString()
                                            } else {
                                                selectedIds - calendar.id.toString()
                                            }
                                            settingsStore.setSelectedCalendarIds(newSelection)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (onDone != null) {
                PrimaryActionButton(
                    label = stringResource(R.string.done),
                    onClick = onDone,
                    enabled = canDone,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )
            }
        }
    }
}

private fun openAddAccount(context: android.content.Context) {
    val intent = android.content.Intent(android.provider.Settings.ACTION_ADD_ACCOUNT).apply {
        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}
