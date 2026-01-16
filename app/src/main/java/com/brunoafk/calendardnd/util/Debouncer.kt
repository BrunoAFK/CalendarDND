package com.brunoafk.calendardnd.util

import kotlinx.coroutines.*

/**
 * Debounces rapid consecutive calls, only executing the last one after a delay
 */
class Debouncer(private val delayMs: Long) {
    private var debounceJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun debounce(action: suspend () -> Unit) {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(delayMs)
            action()
        }
    }

    fun cancel() {
        debounceJob?.cancel()
    }
}