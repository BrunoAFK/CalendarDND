package com.brunoafk.calendardnd.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

enum class StatusBannerKind {
    Disabled,
    Enabled,
    DndActive,
    MissingPermissions
}

data class StatusBannerState(
    val kind: StatusBannerKind,
    val statusText: String,
    val contextText: String? = null,
    val secondaryText: String? = null
)

@Composable
fun StatusBanner(
    state: StatusBannerState,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val (backgroundColor, statusColor, icon) = when (state.kind) {
        StatusBannerKind.Disabled -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.outline,
            Icons.Default.PauseCircle
        )
        StatusBannerKind.Enabled -> Triple(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            MaterialTheme.colorScheme.primary,
            Icons.Default.CheckCircle
        )
        StatusBannerKind.DndActive -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.tertiary,
            Icons.Default.DoNotDisturbOn
        )
        StatusBannerKind.MissingPermissions -> Triple(
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
            MaterialTheme.colorScheme.error,
            Icons.Default.Warning
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(backgroundColor)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(20.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = statusColor
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = state.statusText,
            style = MaterialTheme.typography.titleLarge,
            color = statusColor
        )

        state.contextText?.let { context ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = context,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        state.secondaryText?.let { secondary ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = secondary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
