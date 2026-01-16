package com.brunoafk.calendardnd.util

import android.util.Log
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
fun Modifier.debugTapLog(
    route: String,
    isInteractive: Boolean? = null,
    enabled: Boolean = false
): Modifier {
    if (!enabled) {
        return this
    }

    return pointerInput(route, isInteractive) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.any { it.changedToDownIgnoreConsumed() }) {
                    val interactiveStr = if (isInteractive != null) " interactive=$isInteractive" else ""
                    Log.d("NavTapDebug", "Tap on route=$route$interactiveStr")
                }
            }
        }
    }
}

fun Modifier.navInteractionGate(isInteractive: Boolean, debugLoggingEnabled: Boolean = false): Modifier {
    return this
        .semantics {
            if (!isInteractive) {
                disabled()
            }
        }
        .pointerInput(isInteractive) {
            if (!isInteractive) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (debugLoggingEnabled && event.changes.any { it.changedToDownIgnoreConsumed() }) {
                            Log.d("NavTapDebug", "  → CONSUMING (gate blocked)")
                        }
                        // Consume to block
                        event.changes.forEach { it.consume() }
                    }
                }
            }
        }
}
