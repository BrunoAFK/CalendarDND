package com.brunoafk.calendardnd.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.brunoafk.calendardnd.R

@Composable
fun ErrorCard(
    error: AppError,
    onPrimaryAction: () -> Unit,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(error.titleRes),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(error.messageRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    )
                }
                onDismiss?.let {
                    IconButton(onClick = it) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.dismiss),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onPrimaryAction) {
                    Text(
                        text = stringResource(error.actionLabelRes),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

sealed class AppError(
    @StringRes val titleRes: Int,
    @StringRes val messageRes: Int,
    @StringRes val actionLabelRes: Int
) {
    data object CalendarPermissionDenied : AppError(
        R.string.error_calendar_permission_title,
        R.string.error_calendar_permission_message,
        R.string.error_action_open_settings
    )

    data object DndPermissionDenied : AppError(
        R.string.error_dnd_permission_title,
        R.string.error_dnd_permission_message,
        R.string.error_action_open_settings
    )

    data object CalendarQueryFailed : AppError(
        R.string.error_calendar_query_title,
        R.string.error_calendar_query_message,
        R.string.error_action_retry
    )

    data object DndChangeFailed : AppError(
        R.string.error_dnd_change_title,
        R.string.error_dnd_change_message,
        R.string.error_action_try_again
    )

    data object NoCalendarsFound : AppError(
        R.string.error_no_calendars_title,
        R.string.error_no_calendars_message,
        R.string.error_action_open_calendar
    )

    data object NetworkError : AppError(
        R.string.error_network_title,
        R.string.error_network_message,
        R.string.error_action_retry
    )
}
