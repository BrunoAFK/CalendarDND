package com.brunoafk.calendardnd.data.calendar

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import com.brunoafk.calendardnd.domain.model.EventInstance
import com.brunoafk.calendardnd.util.EngineConstants.CALENDAR_QUERY_TIMEOUT_MS
import com.brunoafk.calendardnd.util.ExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

object CalendarQueries {

    suspend fun queryInstances(
        context: Context,
        beginMs: Long,
        endMs: Long
    ): List<EventInstance> {
        return withContext(Dispatchers.IO) {
            withTimeoutOrNull(CALENDAR_QUERY_TIMEOUT_MS) {
                val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
                ContentUris.appendId(builder, beginMs)
                ContentUris.appendId(builder, endMs)
                val uri = builder.build()

                val projection = arrayOf(
                    CalendarContract.Instances._ID,
                    CalendarContract.Instances.EVENT_ID,
                    CalendarContract.Instances.CALENDAR_ID,
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.EVENT_LOCATION,
                    CalendarContract.Instances.BEGIN,
                    CalendarContract.Instances.END,
                    CalendarContract.Instances.ALL_DAY,
                    CalendarContract.Instances.AVAILABILITY
                )

                val results = mutableListOf<EventInstance>()

                try {
                    context.contentResolver.query(
                        uri,
                        projection,
                        null,
                        null,
                        "${CalendarContract.Instances.BEGIN} ASC"
                    )?.use { cursor ->
                        val idIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances._ID)
                        val eventIdIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
                        val calendarIdIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_ID)
                        val titleIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
                        val locationIdx = cursor.getColumnIndexOrThrow(
                            CalendarContract.Instances.EVENT_LOCATION
                        )
                        val beginIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
                        val endIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
                        val allDayIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
                        val availabilityIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.AVAILABILITY)

                        while (cursor.moveToNext()) {
                            results.add(
                                EventInstance(
                                    id = cursor.getLong(idIdx),
                                    eventId = cursor.getLong(eventIdIdx),
                                    calendarId = cursor.getLong(calendarIdIdx),
                                    title = cursor.getString(titleIdx) ?: "",
                                    location = cursor.getString(locationIdx) ?: "",
                                    begin = cursor.getLong(beginIdx),
                                    end = cursor.getLong(endIdx),
                                    allDay = cursor.getInt(allDayIdx) == 1,
                                    availability = cursor.getInt(availabilityIdx)
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    ExceptionHandler.handleCalendarException(e, "queryInstances")
                }

                results
            } ?: emptyList()
        }
    }

    suspend fun queryCalendars(context: Context): List<CalendarInfo> {
        return withContext(Dispatchers.IO) {
            withTimeoutOrNull(CALENDAR_QUERY_TIMEOUT_MS) {
                val projection = arrayOf(
                    CalendarContract.Calendars._ID,
                    CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                    CalendarContract.Calendars.ACCOUNT_NAME,
                    CalendarContract.Calendars.CALENDAR_COLOR
                )

                val results = mutableListOf<CalendarInfo>()

                try {
                    context.contentResolver.query(
                        CalendarContract.Calendars.CONTENT_URI,
                        projection,
                        "${CalendarContract.Calendars.VISIBLE} = 1",
                        null,
                        CalendarContract.Calendars.CALENDAR_DISPLAY_NAME + " ASC"
                    )?.use { cursor ->
                        val idIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
                        val nameIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                        val accountIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
                        val colorIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_COLOR)

                        while (cursor.moveToNext()) {
                            results.add(
                                CalendarInfo(
                                    id = cursor.getLong(idIdx),
                                    displayName = cursor.getString(nameIdx) ?: "",
                                    accountName = cursor.getString(accountIdx) ?: "",
                                    color = cursor.getInt(colorIdx)
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    ExceptionHandler.handleCalendarException(e, "queryCalendars")
                }

                results
            } ?: emptyList()
        }
    }
}
