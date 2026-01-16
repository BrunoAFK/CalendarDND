package com.brunoafk.calendardnd.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SwipeDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.brunoafk.calendardnd.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Composable
fun InfoBanner(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.SwipeDown,
    autoDismissMs: Long = 0L
) {
    val scope = rememberCoroutineScope()
    val screenWidth = with(LocalDensity.current) {
        LocalConfiguration.current.screenWidthDp.dp.toPx()
    }
    val dismissThreshold = screenWidth * 0.4f

    var isDismissed by remember { mutableStateOf(false) }
    val offsetX = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }
    val scale = remember { Animatable(1f) }

    // Entry animation
    LaunchedEffect(Unit) {
        scale.snapTo(0.95f)
        alpha.snapTo(0f)
        launch { scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
        launch { alpha.animateTo(1f, tween(300)) }
    }

    // Auto dismiss
    LaunchedEffect(autoDismissMs) {
        if (autoDismissMs > 0) {
            delay(autoDismissMs)
            if (!isDismissed) {
                isDismissed = true
                launch { alpha.animateTo(0f, tween(300)) }
                launch { scale.animateTo(0.95f, tween(300)) }
                delay(300)
                onDismiss()
            }
        }
    }

    fun dismiss(swipeDirection: Int = 0) {
        if (isDismissed) return
        isDismissed = true
        scope.launch {
            if (swipeDirection != 0) {
                // Swipe off screen
                launch {
                    offsetX.animateTo(
                        targetValue = if (swipeDirection > 0) screenWidth else -screenWidth,
                        animationSpec = tween(250)
                    )
                }
                launch { alpha.animateTo(0f, tween(250)) }
            } else {
                // Fade out
                launch { alpha.animateTo(0f, tween(300)) }
                launch { scale.animateTo(0.95f, tween(300)) }
            }
            delay(300)
            onDismiss()
        }
    }

    val draggableState = rememberDraggableState { delta ->
        scope.launch {
            offsetX.snapTo(offsetX.value + delta)
        }
    }

    val swipeProgress = (offsetX.value.absoluteValue / screenWidth).coerceIn(0f, 1f)
    val swipeAlpha = (1f - swipeProgress * 0.3f)

    if (!isDismissed || alpha.value > 0f) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .scale(scale.value)
                .alpha(alpha.value * swipeAlpha)
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = { velocity ->
                        if (offsetX.value.absoluteValue > dismissThreshold || velocity.absoluteValue > 1000f) {
                            dismiss(if (offsetX.value >= 0) 1 else -1)
                        } else {
                            scope.launch {
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            }
                        }
                    }
                ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                IconButton(
                    onClick = { dismiss() },
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.dismiss),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
