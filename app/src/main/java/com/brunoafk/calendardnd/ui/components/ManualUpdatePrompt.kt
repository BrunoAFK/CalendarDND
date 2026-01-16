package com.brunoafk.calendardnd.ui.components

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.brunoafk.calendardnd.R
import com.brunoafk.calendardnd.system.update.ManualUpdateManager
import kotlinx.coroutines.launch

@Composable
fun ManualUpdatePrompt(
    prompt: ManualUpdateManager.UpdatePrompt,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
                    scope.launch {
                        val apkFile = if (info.sha256.isNullOrBlank()) {
                            Toast.makeText(
                                context,
                                R.string.update_download_missing_hash,
                                Toast.LENGTH_LONG
                            ).show()
                            null
                        } else {
                            ManualUpdateManager.downloadAndVerifyApk(context, info)
                        }
                        if (apkFile == null) {
                            if (!info.sha256.isNullOrBlank()) {
                                Toast.makeText(
                                    context,
                                    R.string.update_download_failed,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            return@launch
                        }
                        try {
                            context.startActivity(
                                ManualUpdateManager.createInstallIntent(context, apkFile)
                            )
                        } catch (_: ActivityNotFoundException) {
                            Toast.makeText(
                                context,
                                R.string.update_open_failed,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        onDismiss()
                    }
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
