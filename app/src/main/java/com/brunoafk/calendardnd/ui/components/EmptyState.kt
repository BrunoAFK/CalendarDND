package com.brunoafk.calendardnd.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.brunoafk.calendardnd.R

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    action: EmptyStateAction? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .size(80.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    CircleShape
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.padding(top = 24.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.padding(top = 8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        action?.let {
            Spacer(modifier = Modifier.padding(top = 24.dp))
            OutlinedButton(onClick = it.onClick) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = it.icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(it.label)
                }
            }
        }
    }
}

data class EmptyStateAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

object EmptyStates {
    @Composable
    fun NoMeetings(onOpenCalendar: () -> Unit, modifier: Modifier = Modifier) {
        EmptyState(
            icon = Icons.Default.EventAvailable,
            title = stringResource(R.string.empty_no_meetings_title),
            message = stringResource(R.string.empty_no_meetings_message),
            action = EmptyStateAction(
                label = stringResource(R.string.open_calendar),
            icon = Icons.AutoMirrored.Filled.OpenInNew,
                onClick = onOpenCalendar
            ),
            modifier = modifier
        )
    }

    @Composable
    fun NoCalendars(onAddAccount: () -> Unit, modifier: Modifier = Modifier) {
        EmptyState(
            icon = Icons.Default.CalendarMonth,
            title = stringResource(R.string.empty_no_calendars_title),
            message = stringResource(R.string.empty_no_calendars_message),
            action = EmptyStateAction(
                label = stringResource(R.string.add_account),
                icon = Icons.Default.PersonAdd,
                onClick = onAddAccount
            ),
            modifier = modifier
        )
    }

    @Composable
    fun NoLogs(modifier: Modifier = Modifier) {
        EmptyState(
            icon = Icons.AutoMirrored.Filled.Article,
            title = stringResource(R.string.empty_no_logs_title),
            message = stringResource(R.string.empty_no_logs_message),
            modifier = modifier
        )
    }

    @Composable
    fun AutomationDisabled(onEnable: () -> Unit, modifier: Modifier = Modifier) {
        EmptyState(
            icon = Icons.Default.PauseCircle,
            title = stringResource(R.string.empty_disabled_title),
            message = stringResource(R.string.empty_disabled_message),
            action = EmptyStateAction(
                label = stringResource(R.string.enable_now),
                icon = Icons.Default.PlayArrow,
                onClick = onEnable
            ),
            modifier = modifier
        )
    }
}
