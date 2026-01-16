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
import com.brunoafk.calendardnd.data.prefs.DebugLogLevel
import com.brunoafk.calendardnd.data.prefs.DebugLogStore
import com.brunoafk.calendardnd.system.update.ManualUpdateManager
import kotlinx.coroutines.launch

@Composable
fun ManualUpdatePrompt(
    prompt: ManualUpdateManager.UpdatePrompt,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val debugLogStore = DebugLogStore(context)
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
                        debugLogStore.appendLog(
                            DebugLogLevel.INFO,
                            "UPDATE_UI: Prompt download for ${info.versionName}"
                        )
                        val apkFile = if (info.sha256.isNullOrBlank()) {
                            debugLogStore.appendLog(
                                DebugLogLevel.WARNING,
                                "UPDATE_UI: Missing SHA-256 for ${info.versionName}"
                            )
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
                            debugLogStore.appendLog(
                                DebugLogLevel.ERROR,
                                "UPDATE_UI: Download or verification failed for ${info.versionName}"
                            )
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
                            if (!ManualUpdateManager.canRequestPackageInstalls(context)) {
                                debugLogStore.appendLog(
                                    DebugLogLevel.WARNING,
                                    "UPDATE_UI: Install permission missing."
                                )
                                ManualUpdateManager.createInstallPermissionIntent(context)?.let {
                                    context.startActivity(it)
                                } ?: Toast.makeText(
                                    context,
                                    R.string.update_install_permission_required,
                                    Toast.LENGTH_LONG
                                ).show()
                                return@launch
                            }
                            val installIntent =
                                ManualUpdateManager.createInstallIntent(context, apkFile)
                            if (installIntent.resolveActivity(context.packageManager) == null) {
                                debugLogStore.appendLog(
                                    DebugLogLevel.ERROR,
                                    "UPDATE_UI: Installer activity not found"
                                )
                                Toast.makeText(
                                    context,
                                    R.string.update_open_failed,
                                    Toast.LENGTH_LONG
                                ).show()
                                return@launch
                            }
                            debugLogStore.appendLog(
                                DebugLogLevel.INFO,
                                "UPDATE_UI: Launch installer ${apkFile.absolutePath}"
                            )
                            context.startActivity(
                                installIntent
                            )
                        } catch (_: ActivityNotFoundException) {
                            debugLogStore.appendLog(
                                DebugLogLevel.ERROR,
                                "UPDATE_UI: Installer activity not found"
                            )
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
