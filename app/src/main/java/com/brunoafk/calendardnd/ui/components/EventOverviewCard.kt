package com.brunoafk.calendardnd.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.ui.theme.LocalIsDarkTheme
import com.brunoafk.calendardnd.ui.theme.surfaceColorAtElevation

data class EventSummary(
    val title: String,
    val timeRange: String,
    val statusLine: String
)

data class EventOverviewState(
    val current: EventSummary?,
    val next: EventSummary?
)

@Composable
fun EventOverviewCard(
    state: EventOverviewState,
    modifier: Modifier = Modifier,
    onCurrentClick: (() -> Unit)? = null,
    onNextClick: (() -> Unit)? = null,
    nextActionLabel: String? = null,
    nextActionEnabled: Boolean = true,
    onNextAction: (() -> Unit)? = null,
    highlightNext: Boolean = false,
    actionStripActive: Boolean = false
) {
    val isDarkTheme = LocalIsDarkTheme.current
    val highlightColor = if (isDarkTheme) androidx.compose.ui.graphics.Color(0xFFB38B00) else androidx.compose.ui.graphics.Color(0xFFFFC107)
    val highlightText = androidx.compose.ui.graphics.Color.Black
    val highlightSubText = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = surfaceColorAtElevation(1.dp)
        )
    ) {
        Column {
            state.current?.let { current ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (onCurrentClick != null) {
                                Modifier.clickable(onClick = onCurrentClick)
                            } else {
                                Modifier
                            }
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.event_overview_now_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = current.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = current.timeRange,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = current.statusLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state.current != null && state.next != null) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

            state.next?.let { next ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (onNextClick != null) {
                                Modifier.clickable(onClick = onNextClick)
                            } else {
                                Modifier
                            }
                        )
                        .background(if (highlightNext) highlightColor else androidx.compose.ui.graphics.Color.Transparent)
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.event_overview_next_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (highlightNext) highlightText else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = next.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (highlightNext) highlightText else MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = next.timeRange,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (highlightNext) highlightSubText else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = next.statusLine,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (highlightNext) highlightSubText else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (nextActionLabel != null && onNextAction != null) {
                    val stripColor = if (actionStripActive) {
                        if (isDarkTheme) androidx.compose.ui.graphics.Color(0xFF3A3A3A) else androidx.compose.ui.graphics.Color(0xFFE8E8E8)
                    } else {
                        if (isDarkTheme) androidx.compose.ui.graphics.Color(0xFFB38B00) else androidx.compose.ui.graphics.Color(0xFFFFC107)
                    }
                    val stripTextColor = if (actionStripActive) {
                        if (isDarkTheme) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f)
                        else androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.8f)
                    } else {
                        androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.9f)
                    }

                    androidx.compose.material3.Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (nextActionEnabled) Modifier.clickable(onClick = onNextAction) else Modifier),
                        color = stripColor
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = nextActionLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = stripTextColor.copy(alpha = if (nextActionEnabled) 1f else 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}
