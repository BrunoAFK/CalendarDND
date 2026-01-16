package com.brunoafk.calendardnd.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.brunoafk.calendardnd.ui.theme.surfaceColorAtElevation

@Composable
fun PermissionStatusCard(
    permissions: List<PermissionStatus>,
    onFixPermission: (PermissionStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = surfaceColorAtElevation(1.dp)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.permissions_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            permissions.forEachIndexed { index, permission ->
                PermissionStatusRow(
                    permission = permission,
                    onFix = { onFixPermission(permission) }
                )
                if (index != permissions.lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(
    permission: PermissionStatus,
    onFix: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (permission.state) {
                PermissionState.GRANTED -> Icons.Default.CheckCircle
                PermissionState.DENIED -> Icons.Default.Cancel
                PermissionState.LIMITED -> Icons.Default.Warning
            },
            contentDescription = null,
            tint = when (permission.state) {
                PermissionState.GRANTED -> MaterialTheme.colorScheme.primary
                PermissionState.DENIED -> MaterialTheme.colorScheme.error
                PermissionState.LIMITED -> MaterialTheme.colorScheme.tertiary
            },
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = permission.name,
                style = MaterialTheme.typography.bodyMedium
            )
            if (permission.state != PermissionState.GRANTED) {
                Text(
                    text = permission.statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (permission.state != PermissionState.GRANTED) {
            TextButton(
                onClick = onFix,
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.fix),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

data class PermissionStatus(
    val type: PermissionType,
    val name: String,
    val state: PermissionState,
    val statusMessage: String
)

enum class PermissionState {
    GRANTED,
    DENIED,
    LIMITED
}

enum class PermissionType {
    CALENDAR,
    DND_POLICY,
    EXACT_ALARMS,
    NOTIFICATIONS
}
