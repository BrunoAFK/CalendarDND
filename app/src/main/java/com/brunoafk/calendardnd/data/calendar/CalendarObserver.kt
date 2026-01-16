package com.brunoafk.calendardnd.data.calendar

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import com.brunoafk.calendardnd.util.Debouncer
import com.brunoafk.calendardnd.util.EngineConstants.CALENDAR_OBSERVER_DEBOUNCE_MS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CalendarObserver(
    private val context: Context,
    private val onCalendarChanged: suspend () -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val debouncer = Debouncer(CALENDAR_OBSERVER_DEBOUNCE_MS)

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            // Debounce rapid changes
            debouncer.debounce {
                scope.launch {
                    onCalendarChanged()
                }
            }
        }
    }

    private var isRegistered = false

    /**
     * Start observing calendar changes
     */
    fun register() {
        if (isRegistered) return

        try {
            // Observe both Events and Instances
            context.contentResolver.registerContentObserver(
                CalendarContract.Events.CONTENT_URI,
                true,
                observer
            )
            context.contentResolver.registerContentObserver(
                CalendarContract.Instances.CONTENT_URI,
                true,
                observer
            )
            isRegistered = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Stop observing calendar changes
     */
    fun unregister() {
        if (!isRegistered) return

        try {
            context.contentResolver.unregisterContentObserver(observer)
            isRegistered = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
