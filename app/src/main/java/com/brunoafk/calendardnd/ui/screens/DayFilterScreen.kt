package com.brunoafk.calendardnd.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.data.prefs.SettingsStore
import com.brunoafk.calendardnd.ui.components.OneUiTopAppBar
import com.brunoafk.calendardnd.ui.components.SettingsDivider
import com.brunoafk.calendardnd.ui.components.SettingsSection
import com.brunoafk.calendardnd.ui.components.SettingsSwitchRow
import com.brunoafk.calendardnd.util.AnalyticsTracker
import com.brunoafk.calendardnd.util.WeekdayMask
import com.brunoafk.calendardnd.system.alarms.EngineRunner
import com.brunoafk.calendardnd.domain.model.Trigger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayFilterScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsStore = remember { SettingsStore(context) }
    val listState = rememberLazyListState()

    val selectedDaysEnabled by settingsStore.selectedDaysEnabled.collectAsState(initial = false)
    val selectedDaysMask by settingsStore.selectedDaysMask.collectAsState(
        initial = WeekdayMask.ALL_DAYS_MASK
    )

    val locale = remember(context) {
        context.resources.configuration.locales[0] ?: Locale.getDefault()
    }
    val dayLabels = remember(locale) {
        val firstDay = WeekFields.of(Locale.getDefault()).firstDayOfWeek
        val orderedDays = (0..6).map { offset -> firstDay.plus(offset.toLong()) }
        orderedDays.map { day ->
            day to day.getDisplayName(TextStyle.FULL, locale)
        }
    }

    Scaffold(
        topBar = {
            OneUiTopAppBar(
                onNavigateBack = onNavigateBack,
                title = stringResource(R.string.day_filter_title)
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
                SettingsSection(title = stringResource(R.string.day_filter_section_title)) {
                    SettingsSwitchRow(
                        title = stringResource(R.string.day_filter_enable_title),
                        subtitle = stringResource(R.string.day_filter_enable_description),
                        checked = selectedDaysEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                settingsStore.setSelectedDaysEnabled(enabled)
                                withContext(Dispatchers.IO) {
                                    EngineRunner.runEngine(context, Trigger.MANUAL)
                                }
                                AnalyticsTracker.logSettingsChanged(
                                    context,
                                    "selected_days_enabled",
                                    enabled.toString()
                                )
                            }
                        }
                    )
                }
            }

            item {
                SettingsSection(title = stringResource(R.string.day_filter_days_title)) {
                    Text(
                        text = stringResource(R.string.day_filter_days_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp)
                    )
                    dayLabels.forEachIndexed { index, (day, label) ->
                        val mask = WeekdayMask.dayToMask(day)
                        val selected = (selectedDaysMask and mask) != 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Checkbox(
                                checked = selected,
                                enabled = selectedDaysEnabled,
                                onCheckedChange = { checked ->
                                    if (!selectedDaysEnabled) {
                                        return@Checkbox
                                    }
                                    if (!checked && selectedDaysMask == mask) {
                                        return@Checkbox
                                    }
                                    val newMask = if (checked) {
                                        selectedDaysMask or mask
                                    } else {
                                        selectedDaysMask and mask.inv()
                                    }
                                    scope.launch {
                                        settingsStore.setSelectedDaysMask(newMask)
                                        withContext(Dispatchers.IO) {
                                            EngineRunner.runEngine(context, Trigger.MANUAL)
                                        }
                                        AnalyticsTracker.logSettingsChanged(
                                            context,
                                            "selected_days_mask",
                                            newMask.toString()
                                        )
                                    }
                                }
                            )
                        }
                        if (index < dayLabels.lastIndex) {
                            SettingsDivider()
                        }
                    }
                }
            }
        }
    }
}
