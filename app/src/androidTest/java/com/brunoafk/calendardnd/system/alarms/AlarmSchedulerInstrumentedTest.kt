package com.brunoafk.calendardnd.system.alarms

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmSchedulerInstrumentedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun scheduleBoundaryAlarm_respectsExactAlarmCapability() {
        val scheduler = AlarmScheduler(context)
        val triggerAtMs = System.currentTimeMillis() + 60_000L
        val canExact = scheduler.canScheduleExactAlarms()

        val scheduled = scheduler.scheduleBoundaryAlarm(triggerAtMs)

        if (canExact) {
            assertTrue(scheduled)
        } else {
            assertFalse(scheduled)
        }

        scheduler.cancelBoundaryAlarm()
    }

    @Test
    fun schedulePreDndNotificationAlarm_succeeds_with_or_without_exact_alarms() {
        val scheduler = AlarmScheduler(context)
        val triggerAtMs = System.currentTimeMillis() + 120_000L

        val scheduled = scheduler.schedulePreDndNotificationAlarm(
            triggerAtMs = triggerAtMs,
            meetingTitle = "Test Meeting",
            dndWindowEndMs = triggerAtMs + 30_000L,
            dndWindowStartMs = triggerAtMs - 300_000L
        )

        assertTrue(scheduled)
        scheduler.cancelPreDndNotificationAlarm()
    }

    @Test
    fun schedulePostMeetingCheckAlarm_succeeds_with_or_without_exact_alarms() {
        val scheduler = AlarmScheduler(context)
        val triggerAtMs = System.currentTimeMillis() + 180_000L

        val scheduled = scheduler.schedulePostMeetingCheckAlarm(triggerAtMs)

        assertTrue(scheduled)
        scheduler.cancelPostMeetingCheckAlarm()
    }

    @Test
    fun scheduleRestoreRingerAlarm_succeeds_with_or_without_exact_alarms() {
        val scheduler = AlarmScheduler(context)
        val triggerAtMs = System.currentTimeMillis() + 240_000L

        val scheduled = scheduler.scheduleRestoreRingerAlarm(triggerAtMs)

        assertTrue(scheduled)
        scheduler.cancelRestoreRingerAlarm()
    }
}
