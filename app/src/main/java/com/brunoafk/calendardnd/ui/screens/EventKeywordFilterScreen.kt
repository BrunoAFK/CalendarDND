package com.brunoafk.calendardnd.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.data.prefs.SettingsStore
import com.brunoafk.calendardnd.domain.model.KeywordMatchMode
import com.brunoafk.calendardnd.ui.components.OneUiTopAppBar
import com.brunoafk.calendardnd.ui.components.SettingsDivider
import com.brunoafk.calendardnd.ui.components.SettingsSection
import com.brunoafk.calendardnd.ui.components.SettingsSwitchRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventKeywordFilterScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsStore = remember { SettingsStore(context) }
    val requireTitleKeyword by settingsStore.requireTitleKeyword.collectAsState(initial = false)
    val titleKeyword by settingsStore.titleKeyword.collectAsState(initial = "")
    val titleKeywordMatchMode by settingsStore.titleKeywordMatchMode.collectAsState(
        initial = KeywordMatchMode.KEYWORDS
    )
    val titleKeywordCaseSensitive by settingsStore.titleKeywordCaseSensitive.collectAsState(initial = false)
    val titleKeywordMatchAll by settingsStore.titleKeywordMatchAll.collectAsState(initial = false)
    val titleKeywordExclude by settingsStore.titleKeywordExclude.collectAsState(initial = false)
    var keywordInput by remember { mutableStateOf(titleKeyword) }
    var modeMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(titleKeyword) {
        if (titleKeyword != keywordInput) {
            keywordInput = titleKeyword
        }
    }

    val regexInvalid = remember(titleKeywordMatchMode, keywordInput) {
        titleKeywordMatchMode == KeywordMatchMode.REGEX &&
            keywordInput.isNotBlank() &&
            runCatching { Regex(keywordInput) }.isFailure
    }

    val inputLabel = when (titleKeywordMatchMode) {
        KeywordMatchMode.KEYWORDS -> stringResource(R.string.event_keyword_filter_keywords_label)
        KeywordMatchMode.WHOLE_WORD -> stringResource(R.string.event_keyword_filter_whole_word_label)
        KeywordMatchMode.STARTS_WITH -> stringResource(R.string.event_keyword_filter_starts_with_label)
        KeywordMatchMode.ENDS_WITH -> stringResource(R.string.event_keyword_filter_ends_with_label)
        KeywordMatchMode.EXACT -> stringResource(R.string.event_keyword_filter_exact_label)
        KeywordMatchMode.REGEX -> stringResource(R.string.event_keyword_filter_regex_label)
    }
    val inputHelp = when (titleKeywordMatchMode) {
        KeywordMatchMode.KEYWORDS -> stringResource(R.string.event_keyword_filter_keywords_help)
        KeywordMatchMode.WHOLE_WORD -> stringResource(R.string.event_keyword_filter_whole_word_help)
        KeywordMatchMode.STARTS_WITH -> stringResource(R.string.event_keyword_filter_starts_with_help)
        KeywordMatchMode.ENDS_WITH -> stringResource(R.string.event_keyword_filter_ends_with_help)
        KeywordMatchMode.EXACT -> stringResource(R.string.event_keyword_filter_exact_help)
        KeywordMatchMode.REGEX -> stringResource(R.string.event_keyword_filter_regex_help)
    }
    val inputExamples = when (titleKeywordMatchMode) {
        KeywordMatchMode.KEYWORDS -> stringResource(R.string.event_keyword_filter_keywords_examples)
        KeywordMatchMode.WHOLE_WORD -> stringResource(R.string.event_keyword_filter_whole_word_examples)
        KeywordMatchMode.STARTS_WITH -> stringResource(R.string.event_keyword_filter_starts_with_examples)
        KeywordMatchMode.ENDS_WITH -> stringResource(R.string.event_keyword_filter_ends_with_examples)
        KeywordMatchMode.EXACT -> stringResource(R.string.event_keyword_filter_exact_examples)
        KeywordMatchMode.REGEX -> stringResource(R.string.event_keyword_filter_regex_examples)
    }
    val showCaseSensitive = titleKeywordMatchMode != KeywordMatchMode.REGEX
    val showMatchAll = titleKeywordMatchMode == KeywordMatchMode.KEYWORDS ||
        titleKeywordMatchMode == KeywordMatchMode.WHOLE_WORD

    Scaffold(
        topBar = {
            OneUiTopAppBar(
                onNavigateBack = onNavigateBack,
                title = stringResource(R.string.event_keyword_filter_title)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSection(title = stringResource(R.string.event_keyword_filter_title)) {
                Column {
                    SettingsSwitchRow(
                        title = stringResource(R.string.event_keyword_filter_toggle_title),
                        subtitle = stringResource(R.string.event_keyword_filter_toggle_subtitle),
                        checked = requireTitleKeyword,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                settingsStore.setRequireTitleKeyword(enabled)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.padding(top = 12.dp))
                    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                        ExposedDropdownMenuBox(
                            expanded = modeMenuExpanded,
                            onExpandedChange = { modeMenuExpanded = !modeMenuExpanded }
                        ) {
                            OutlinedTextField(
                                value = when (titleKeywordMatchMode) {
                                    KeywordMatchMode.KEYWORDS ->
                                        stringResource(R.string.event_keyword_filter_mode_keywords)
                                    KeywordMatchMode.WHOLE_WORD ->
                                        stringResource(R.string.event_keyword_filter_mode_whole_word)
                                    KeywordMatchMode.STARTS_WITH ->
                                        stringResource(R.string.event_keyword_filter_mode_starts_with)
                                    KeywordMatchMode.ENDS_WITH ->
                                        stringResource(R.string.event_keyword_filter_mode_ends_with)
                                    KeywordMatchMode.EXACT ->
                                        stringResource(R.string.event_keyword_filter_mode_exact)
                                    KeywordMatchMode.REGEX ->
                                        stringResource(R.string.event_keyword_filter_mode_regex)
                                },
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                label = { Text(stringResource(R.string.event_keyword_filter_mode_label)) },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = modeMenuExpanded
                                    )
                                }
                            )
                            ExposedDropdownMenu(
                                expanded = modeMenuExpanded,
                                onDismissRequest = { modeMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    onClick = {
                                        scope.launch {
                                            settingsStore.setTitleKeywordMatchMode(KeywordMatchMode.KEYWORDS)
                                        }
                                        modeMenuExpanded = false
                                    },
                                    text = { Text(stringResource(R.string.event_keyword_filter_mode_keywords)) }
                                )
                                DropdownMenuItem(
                                    onClick = {
                                        scope.launch {
                                            settingsStore.setTitleKeywordMatchMode(KeywordMatchMode.WHOLE_WORD)
                                        }
                                        modeMenuExpanded = false
                                    },
                                    text = { Text(stringResource(R.string.event_keyword_filter_mode_whole_word)) }
                                )
                                DropdownMenuItem(
                                    onClick = {
                                        scope.launch {
                                            settingsStore.setTitleKeywordMatchMode(KeywordMatchMode.STARTS_WITH)
                                        }
                                        modeMenuExpanded = false
                                    },
                                    text = { Text(stringResource(R.string.event_keyword_filter_mode_starts_with)) }
                                )
                                DropdownMenuItem(
                                    onClick = {
                                        scope.launch {
                                            settingsStore.setTitleKeywordMatchMode(KeywordMatchMode.ENDS_WITH)
                                        }
                                        modeMenuExpanded = false
                                    },
                                    text = { Text(stringResource(R.string.event_keyword_filter_mode_ends_with)) }
                                )
                                DropdownMenuItem(
                                    onClick = {
                                        scope.launch {
                                            settingsStore.setTitleKeywordMatchMode(KeywordMatchMode.EXACT)
                                        }
                                        modeMenuExpanded = false
                                    },
                                    text = { Text(stringResource(R.string.event_keyword_filter_mode_exact)) }
                                )
                                DropdownMenuItem(
                                    onClick = {
                                        scope.launch {
                                            settingsStore.setTitleKeywordMatchMode(KeywordMatchMode.REGEX)
                                        }
                                        modeMenuExpanded = false
                                    },
                                    text = { Text(stringResource(R.string.event_keyword_filter_mode_regex)) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.padding(top = 12.dp))
                        OutlinedTextField(
                            value = keywordInput,
                            onValueChange = { value ->
                                keywordInput = value
                                scope.launch {
                                    settingsStore.setTitleKeyword(value)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(inputLabel) }
                        )
                        Spacer(modifier = Modifier.padding(top = 8.dp))
                        Text(
                            text = inputHelp,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.padding(top = 6.dp))
                        Text(
                            text = inputExamples,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (regexInvalid) {
                            Spacer(modifier = Modifier.padding(top = 8.dp))
                            Text(
                                text = stringResource(R.string.event_keyword_filter_regex_invalid),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    SettingsDivider()
                    if (showCaseSensitive) {
                        SettingsSwitchRow(
                            title = stringResource(R.string.event_keyword_filter_case_sensitive_title),
                            subtitle = stringResource(R.string.event_keyword_filter_case_sensitive_subtitle),
                            checked = titleKeywordCaseSensitive,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    settingsStore.setTitleKeywordCaseSensitive(enabled)
                                }
                            }
                        )
                        SettingsDivider()
                    }
                    if (showMatchAll) {
                        SettingsSwitchRow(
                            title = stringResource(R.string.event_keyword_filter_match_all_title),
                            subtitle = stringResource(R.string.event_keyword_filter_match_all_subtitle),
                            checked = titleKeywordMatchAll,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    settingsStore.setTitleKeywordMatchAll(enabled)
                                }
                            }
                        )
                        SettingsDivider()
                    }
                    SettingsSwitchRow(
                        title = stringResource(R.string.event_keyword_filter_exclude_title),
                        subtitle = stringResource(R.string.event_keyword_filter_exclude_subtitle),
                        checked = titleKeywordExclude,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                settingsStore.setTitleKeywordExclude(enabled)
                            }
                        }
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = stringResource(R.string.event_keyword_filter_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}
