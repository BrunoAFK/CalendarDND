package com.brunoafk.calendardnd.system.workers

import android.content.Context
import androidx.work.*
import com.brunoafk.calendardnd.util.EngineConstants.SANITY_WORKER_INTERVAL_MINUTES
import java.util.concurrent.TimeUnit

/**
 * WorkManager helper for scheduling workers
 */
object Workers {

    private const val SANITY_WORK_NAME = "sanity_worker"
    private const val GUARD_WORK_TAG = "near_term_guard"

    /**
     * Ensure the periodic sanity worker is scheduled
     */
    fun ensureSanityWorker(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<SanityWorker>(
            SANITY_WORKER_INTERVAL_MINUTES, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SANITY_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    /**
     * Schedule a one-time near-term guard worker
     * @param targetMs when to run
     * @param isBefore true if this is a "before" guard, false for "after"
     */
    fun scheduleNearTermGuard(context: Context, targetMs: Long, isBefore: Boolean) {
        val now = System.currentTimeMillis()
        val delayMs = maxOf(0, targetMs - now)

        val workRequest = OneTimeWorkRequestBuilder<NearTermGuardWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .addTag(GUARD_WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }

    /**
     * Cancel all near-term guard workers
     */
    fun cancelGuardWorkers(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(GUARD_WORK_TAG)
    }

    /**
     * Cancel all work (when automation is turned off)
     */
    fun cancelAllWork(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(SANITY_WORK_NAME)
        cancelGuardWorkers(context)
    }
}
