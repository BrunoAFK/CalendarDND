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
    showTopSpacer: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (showTopSpacer) {
            Spacer(modifier = Modifier.height(56.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall
        )
        Spacer(modifier = Modifier.height(28.dp))
    }
}
