package com.brunoafk.calendardnd.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.data.prefs.SettingsStore
import com.brunoafk.calendardnd.ui.components.OneUiTopAppBar
import com.brunoafk.calendardnd.ui.components.SettingsDivider
import com.brunoafk.calendardnd.ui.components.SettingsInfoRow
import com.brunoafk.calendardnd.ui.components.SettingsNavigationRow
import com.brunoafk.calendardnd.ui.components.SettingsSection
import com.brunoafk.calendardnd.ui.components.SettingsSwitchRow
import com.brunoafk.calendardnd.util.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun DebugReviewScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsStore = remember { SettingsStore(context) }
    val reviewState by settingsStore.reviewPromptState.collectAsState(
        initial = SettingsStore.ReviewPromptState(
            firstOpenMs = 0L,
            appOpenCount = 0,
            promptShown = false,
            lastPromptMs = 0L,
            lastPromptMajorVersion = 0,
            launchAttempts = 0
        )
    )
    var editField by remember { mutableStateOf<ReviewEditField?>(null) }
    var editValue by remember { mutableStateOf("") }

    fun openEditor(field: ReviewEditField, currentValue: String) {
        editField = field
        editValue = currentValue
    }

    fun formattedTimestamp(value: Long): String {
        if (value <= 0L) {
            return "—"
        }
        return TimeUtils.formatDateTime(context, value)
    }

    Scaffold(
        topBar = {
            OneUiTopAppBar(
                onNavigateBack = onNavigateBack,
                title = stringResource(R.string.debug_review_title)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSection(title = stringResource(R.string.debug_review_state_title)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.debug_review_state_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.padding(top = 12.dp))
                    SettingsInfoRow(
                        title = stringResource(R.string.debug_review_first_open_title),
                        subtitle = stringResource(
                            R.string.debug_review_first_open_value,
                            formattedTimestamp(reviewState.firstOpenMs),
                            reviewState.firstOpenMs
                        )
                    )
                    SettingsDivider()
                    SettingsInfoRow(
                        title = stringResource(R.string.debug_review_open_count_title),
                        subtitle = reviewState.appOpenCount.toString()
                    )
                    SettingsDivider()
                    SettingsInfoRow(
                        title = stringResource(R.string.debug_review_last_prompt_title),
                        subtitle = stringResource(
                            R.string.debug_review_last_prompt_value,
                            formattedTimestamp(reviewState.lastPromptMs),
                            reviewState.lastPromptMs
                        )
                    )
                    SettingsDivider()
                    SettingsInfoRow(
                        title = stringResource(R.string.debug_review_last_major_title),
                        subtitle = reviewState.lastPromptMajorVersion.toString()
                    )
                    SettingsDivider()
                    SettingsInfoRow(
                        title = stringResource(R.string.debug_review_launch_attempts_title),
                        subtitle = reviewState.launchAttempts.toString()
                    )
                    SettingsDivider()
                    SettingsInfoRow(
                        title = stringResource(R.string.debug_review_prompt_shown_title),
                        subtitle = if (reviewState.promptShown) {
                            stringResource(R.string.yes)
                        } else {
                            stringResource(R.string.no)
                        }
                    )
                }
            }

            SettingsSection(title = stringResource(R.string.debug_review_edit_title)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsNavigationRow(
                        title = stringResource(R.string.debug_review_first_open_set_title),
                        subtitle = stringResource(R.string.debug_review_first_open_set_subtitle),
                        onClick = { openEditor(ReviewEditField.FIRST_OPEN, reviewState.firstOpenMs.toString()) }
                    )
                    SettingsDivider()
                    SettingsNavigationRow(
                        title = stringResource(R.string.debug_review_first_open_now_title),
                        subtitle = stringResource(R.string.debug_review_first_open_now_subtitle),
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                settingsStore.setReviewPromptFirstOpenMs(System.currentTimeMillis())
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsNavigationRow(
                        title = stringResource(R.string.debug_review_first_open_clear_title),
                        subtitle = stringResource(R.string.debug_review_first_open_clear_subtitle),
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                settingsStore.setReviewPromptFirstOpenMs(0L)
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsNavigationRow(
                        title = stringResource(R.string.debug_review_open_count_set_title),
                        subtitle = stringResource(R.string.debug_review_open_count_set_subtitle),
                        onClick = { openEditor(ReviewEditField.OPEN_COUNT, reviewState.appOpenCount.toString()) }
                    )
                    SettingsDivider()
                    SettingsNavigationRow(
                        title = stringResource(R.string.debug_review_open_count_min_title),
                        subtitle = stringResource(R.string.debug_review_open_count_min_subtitle),
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                settingsStore.setReviewPromptAppOpenCount(5)
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsNavigationRow(
                        title = stringResource(R.string.debug_review_open_count_clear_title),
                        subtitle = stringResource(R.string.debug_review_open_count_clear_subtitle),
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                settingsStore.setReviewPromptAppOpenCount(0)
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsNavigationRow(
                        title = stringResource(R.string.debug_review_last_prompt_set_title),
                        subtitle = stringResource(R.string.debug_review_last_prompt_set_subtitle),
                        onClick = { openEditor(ReviewEditField.LAST_PROMPT_MS, reviewState.lastPromptMs.toString()) }
                    )
                    SettingsDivider()
                    SettingsNavigationRow(
                        title = stringResource(R.string.debug_review_last_prompt_now_title),
                        subtitle = stringResource(R.string.debug_review_last_prompt_now_subtitle),
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                settingsStore.setReviewPromptLastPromptMs(System.currentTimeMillis())
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsNavigationRow(
                        title = stringResource(R.string.debug_review_last_prompt_clear_title),
                        subtitle = stringResource(R.string.debug_review_last_prompt_clear_subtitle),
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                settingsStore.setReviewPromptLastPromptMs(0L)
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsNavigationRow(
                        title = stringResource(R.string.debug_review_last_major_set_title),
                        subtitle = stringResource(R.string.debug_review_last_major_set_subtitle),
                        onClick = {
                            openEditor(
                                ReviewEditField.LAST_MAJOR_VERSION,
                                reviewState.lastPromptMajorVersion.toString()
                            )
                        }
                    )
                    SettingsDivider()
                    SettingsNavigationRow(
                        title = stringResource(R.string.debug_review_last_major_clear_title),
                        subtitle = stringResource(R.string.debug_review_last_major_clear_subtitle),
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                settingsStore.setReviewPromptLastMajorVersion(0)
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsNavigationRow(
                        title = stringResource(R.string.debug_review_launch_attempts_set_title),
                        subtitle = stringResource(R.string.debug_review_launch_attempts_set_subtitle),
                        onClick = {
                            openEditor(
                                ReviewEditField.LAUNCH_ATTEMPTS,
                                reviewState.launchAttempts.toString()
                            )
                        }
                    )
                    SettingsDivider()
                    SettingsNavigationRow(
                        title = stringResource(R.string.debug_review_launch_attempts_clear_title),
                        subtitle = stringResource(R.string.debug_review_launch_attempts_clear_subtitle),
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                settingsStore.setReviewPromptLaunchAttempts(0)
                            }
                        }
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        title = stringResource(R.string.debug_review_prompt_shown_toggle_title),
                        subtitle = stringResource(R.string.debug_review_prompt_shown_toggle_subtitle),
                        checked = reviewState.promptShown,
                        onCheckedChange = { enabled ->
                            scope.launch(Dispatchers.IO) {
                                settingsStore.setReviewPromptShown(enabled)
                            }
                        }
                    )
                }
            }
        }
    }

    editField?.let { field ->
        AlertDialog(
            onDismissRequest = { editField = null },
            title = { Text(stringResource(field.titleRes)) },
            text = {
                OutlinedTextField(
                    value = editValue,
                    onValueChange = { editValue = it },
                    label = { Text(stringResource(field.inputLabelRes)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val success = when (field) {
                            ReviewEditField.FIRST_OPEN -> editValue.toLongOrNull()?.let {
                                scope.launch(Dispatchers.IO) {
                                    settingsStore.setReviewPromptFirstOpenMs(it)
                                }
                                true
                            } ?: false
                            ReviewEditField.OPEN_COUNT -> editValue.toIntOrNull()?.let {
                                scope.launch(Dispatchers.IO) {
                                    settingsStore.setReviewPromptAppOpenCount(it)
                                }
                                true
                            } ?: false
                            ReviewEditField.LAST_PROMPT_MS -> editValue.toLongOrNull()?.let {
                                scope.launch(Dispatchers.IO) {
                                    settingsStore.setReviewPromptLastPromptMs(it)
                                }
                                true
                            } ?: false
                            ReviewEditField.LAST_MAJOR_VERSION -> editValue.toIntOrNull()?.let {
                                scope.launch(Dispatchers.IO) {
                                    settingsStore.setReviewPromptLastMajorVersion(it)
                                }
                                true
                            } ?: false
                            ReviewEditField.LAUNCH_ATTEMPTS -> editValue.toIntOrNull()?.let {
                                scope.launch(Dispatchers.IO) {
                                    settingsStore.setReviewPromptLaunchAttempts(it)
                                }
                                true
                            } ?: false
                        }
                        if (!success) {
                            Toast.makeText(
                                context,
                                R.string.debug_review_invalid_value,
                                Toast.LENGTH_SHORT
                            ).show()
                            return@TextButton
                        }
                        editField = null
                    }
                ) {
                    Text(stringResource(R.string.debug_review_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { editField = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

private enum class ReviewEditField(
    val titleRes: Int,
    val inputLabelRes: Int
) {
    FIRST_OPEN(
        R.string.debug_review_first_open_set_title,
        R.string.debug_review_first_open_input_label
    ),
    OPEN_COUNT(
        R.string.debug_review_open_count_set_title,
        R.string.debug_review_open_count_input_label
    ),
    LAST_PROMPT_MS(
        R.string.debug_review_last_prompt_set_title,
        R.string.debug_review_last_prompt_input_label
    ),
    LAST_MAJOR_VERSION(
        R.string.debug_review_last_major_set_title,
        R.string.debug_review_last_major_input_label
    ),
    LAUNCH_ATTEMPTS(
        R.string.debug_review_launch_attempts_set_title,
        R.string.debug_review_launch_attempts_input_label
    )
}
