package com.brunoafk.calendardnd.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.data.prefs.SettingsStore
import com.brunoafk.calendardnd.ui.components.OneUiTopAppBar
import com.brunoafk.calendardnd.ui.components.SettingsHelpText
import com.brunoafk.calendardnd.ui.components.SettingsSection
import com.brunoafk.calendardnd.ui.components.SettingsSwitchRow
import com.brunoafk.calendardnd.util.AnalyticsTracker
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationAdvancedScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsStore = remember { SettingsStore(context) }

    val preDndNotificationEnabled by settingsStore.preDndNotificationEnabled.collectAsState(initial = false)
    val preDndNotificationLeadMinutes by settingsStore.preDndNotificationLeadMinutes.collectAsState(initial = 5)
    val postMeetingNotificationEnabled by settingsStore.postMeetingNotificationEnabled.collectAsState(initial = true)
    val postMeetingNotificationOffsetMinutes by settingsStore.postMeetingNotificationOffsetMinutes.collectAsState(
        initial = 0
    )
    val postMeetingNotificationSilent by settingsStore.postMeetingNotificationSilent.collectAsState(initial = true)
    val listState = rememberLazyListState()

    val preDndTimingValue = stringResource(
        R.string.pre_dnd_notification_timing_value,
        preDndNotificationLeadMinutes
    )
    val postMeetingTimingValue = when {
        postMeetingNotificationOffsetMinutes == 0 -> stringResource(
            R.string.post_meeting_notification_timing_at_end
        )
        postMeetingNotificationOffsetMinutes < 0 -> stringResource(
            R.string.post_meeting_notification_timing_before,
            -postMeetingNotificationOffsetMinutes
        )
        else -> stringResource(
            R.string.post_meeting_notification_timing_after,
            postMeetingNotificationOffsetMinutes
        )
    }

    Scaffold(
        topBar = {
            OneUiTopAppBar(
                onNavigateBack = onNavigateBack,
                title = stringResource(R.string.notifications_advanced_title)
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
                SettingsSection(title = stringResource(R.string.notifications_title)) {
                    Column(modifier = Modifier.padding(16.dp).padding(bottom = 8.dp)) {
                        val labelColor = if (preDndNotificationEnabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        }
                        Text(
                            stringResource(R.string.pre_dnd_notification_timing_title),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            preDndTimingValue,
                            style = MaterialTheme.typography.bodySmall,
                            color = labelColor
                        )
                        Slider(
                            value = preDndNotificationLeadMinutes.toFloat(),
                            onValueChange = { value ->
                                scope.launch {
                                    settingsStore.setPreDndNotificationLeadMinutes(value.toInt())
                                }
                            },
                            onValueChangeFinished = {
                                AnalyticsTracker.logSettingsChanged(
                                    context,
                                    "pre_dnd_notification_minutes",
                                    preDndNotificationLeadMinutes.toString()
                                )
                            },
                            valueRange = 0f..10f,
                            steps = 9,
                            enabled = preDndNotificationEnabled
                        )
                        Text(
                            preDndTimingValue,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        SettingsHelpText(
                            text = stringResource(R.string.pre_dnd_notification_timing_help),
                            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                        )
                    }

                    Column(modifier = Modifier.padding(16.dp).padding(bottom = 8.dp)) {
                        val labelColor = if (postMeetingNotificationEnabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        }
                        Text(
                            stringResource(R.string.post_meeting_notification_timing_title),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            postMeetingTimingValue,
                            style = MaterialTheme.typography.bodySmall,
                            color = labelColor
                        )
                        Slider(
                            value = postMeetingNotificationOffsetMinutes.toFloat(),
                            onValueChange = { value ->
                                scope.launch {
                                    settingsStore.setPostMeetingNotificationOffsetMinutes(value.toInt())
                                }
                            },
                            onValueChangeFinished = {
                                AnalyticsTracker.logSettingsChanged(
                                    context,
                                    "post_meeting_notification_minutes",
                                    postMeetingNotificationOffsetMinutes.toString()
                                )
                            },
                            valueRange = -10f..10f,
                            steps = 19,
                            enabled = postMeetingNotificationEnabled
                        )
                        Text(
                            postMeetingTimingValue,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        SettingsHelpText(
                            text = stringResource(R.string.post_meeting_notification_timing_help),
                            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                        )
                    }

                    SettingsSwitchRow(
                        title = stringResource(R.string.post_meeting_notification_silent_title),
                        subtitle = stringResource(R.string.post_meeting_notification_silent_subtitle),
                        checked = postMeetingNotificationSilent,
                        enabled = postMeetingNotificationEnabled,
                        onCheckedChange = { silent ->
                            scope.launch {
                                settingsStore.setPostMeetingNotificationSilent(silent)
                                AnalyticsTracker.logSettingsChanged(
                                    context,
                                    "post_meeting_notification_silent",
                                    silent.toString()
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}
