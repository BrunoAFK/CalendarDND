package com.brunoafk.calendardnd.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OneUiHeader(
    title: String,
    modifier: Modifier = Modifier,
    showTopSpacer: Boolean = true,
    compact: Boolean = false
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (showTopSpacer) {
            Spacer(modifier = Modifier.height(if (compact) 24.dp else 56.dp))
        }
        Text(
            text = title,
            style = if (compact) {
                MaterialTheme.typography.titleLarge
            } else {
                MaterialTheme.typography.displaySmall
            }
        )
        Spacer(modifier = Modifier.height(if (compact) 12.dp else 28.dp))
    }
}
