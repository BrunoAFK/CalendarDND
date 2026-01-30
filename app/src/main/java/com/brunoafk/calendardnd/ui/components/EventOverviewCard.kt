package com.brunoafk.calendardnd.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
    val next: EventSummary?,
    val afterNext: EventSummary? = null
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
    highlightCurrent: Boolean = false,
    highlightNext: Boolean = false,
    actionStripActive: Boolean = false,
    highlightColor: androidx.compose.ui.graphics.Color,
    currentActionLabel: String? = null,
    currentActionEnabled: Boolean = true,
    onCurrentAction: (() -> Unit)? = null,
    currentActionStripActive: Boolean = false,
    currentOverlapCount: Int = 0,
    nextOverlapCount: Int = 0,
    onAfterNextClick: (() -> Unit)? = null
) {
    val isDarkTheme = LocalIsDarkTheme.current
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
                        .background(if (highlightCurrent) highlightColor else androidx.compose.ui.graphics.Color.Transparent)
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.event_overview_now_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (highlightCurrent) highlightText else MaterialTheme.colorScheme.primary
                        )
                        if (currentOverlapCount > 1) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = stringResource(R.string.active_meeting_overlap_chip, currentOverlapCount),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                    Text(
                        text = current.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (highlightCurrent) highlightText else MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = current.timeRange,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (highlightCurrent) highlightSubText else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = current.statusLine,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (highlightCurrent) highlightSubText else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (currentActionLabel != null && onCurrentAction != null) {
                    val stripColor = if (currentActionStripActive) {
                        if (isDarkTheme) androidx.compose.ui.graphics.Color(0xFF3A3A3A) else androidx.compose.ui.graphics.Color(0xFFE8E8E8)
                    } else {
                        highlightColor
                    }
                    val stripTextColor = if (currentActionStripActive) {
                        if (isDarkTheme) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f)
                        else androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.8f)
                    } else {
                        androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.85f)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .requiredHeight(64.dp)
                            .background(stripColor)
                            .then(if (currentActionEnabled) Modifier.clickable(onClick = onCurrentAction) else Modifier)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = currentActionLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = stripTextColor.copy(alpha = if (currentActionEnabled) 1f else 0.5f)
                        )
                    }
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.event_overview_next_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (highlightNext) highlightText else MaterialTheme.colorScheme.primary
                        )
                        if (nextOverlapCount > 1) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = stringResource(R.string.active_meeting_overlap_chip, nextOverlapCount),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
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
                        highlightColor
                    }
                    val stripTextColor = if (actionStripActive) {
                        if (isDarkTheme) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f)
                        else androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.8f)
                    } else {
                        androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.85f)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .requiredHeight(64.dp)
                            .background(stripColor)
                            .then(if (nextActionEnabled) Modifier.clickable(onClick = onNextAction) else Modifier)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = nextActionLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = stripTextColor.copy(alpha = if (nextActionEnabled) 1f else 0.5f)
                        )
                    }
                }
            }

            state.afterNext?.let { afterNext ->
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (onAfterNextClick != null) {
                                Modifier.clickable(onClick = onAfterNextClick)
                            } else {
                                Modifier
                            }
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.event_overview_next_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = afterNext.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = afterNext.timeRange,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = afterNext.statusLine,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
