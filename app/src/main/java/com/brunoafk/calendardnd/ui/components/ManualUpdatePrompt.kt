package com.brunoafk.calendardnd.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.system.update.ManualUpdateManager

@Composable
fun ManualUpdatePrompt(
    prompt: ManualUpdateManager.UpdatePrompt,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val info = prompt.info
    val title = stringResource(R.string.update_dialog_title)
    val message = stringResource(R.string.update_dialog_body, info.versionName)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            val notes = info.releaseNotes
            if (notes.isNullOrBlank()) {
                Text(message)
            } else {
                Text("$message\n\n$notes")
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    openUpdateUrl(context, info.apkUrl)
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.update_action_download))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.update_action_later))
            }
        }
    )
}

private fun openUpdateUrl(context: android.content.Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val chooser = Intent.createChooser(intent, context.getString(R.string.update_action_download))
    try {
        context.startActivity(chooser)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.update_open_failed, Toast.LENGTH_LONG).show()
    }
}
