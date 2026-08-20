package com.example.solo_levelling.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.solo_levelling.appContainer

/** Thin WorkManager adapter; domain + scheduling live in DayBoundaryCoordinator. */
class DayBoundaryWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        applicationContext.appContainer.dayBoundaryCoordinator.onBoundaryFired(applicationContext)
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            // Scheduling is owned by DayBoundaryCoordinator; kept for MainActivity call-site compat.
            // Actual enqueue happens after catch-up in ensureCatchUpAndSchedule.
        }
    }
}
