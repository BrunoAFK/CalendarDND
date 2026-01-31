package com.brunoafk.calendardnd.util

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PermissionUtilsInstrumentedTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun notificationPermission_isGrantedOnPreTiramisuDevices() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            assertTrue(PermissionUtils.hasNotificationPermission(context))
        }
    }
}
