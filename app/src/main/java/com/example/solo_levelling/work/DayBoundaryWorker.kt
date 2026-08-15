package com.example.solo_levelling.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.solo_levelling.appContainer
import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.core.event.DomainEvent
import java.util.concurrent.TimeUnit

class DayBoundaryWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val container = applicationContext.appContainer
        val profile = container.db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
        if (profile?.onboardingDone != true) return Result.success()

        container.dayBoundary.runDailyBoundary(profile.timezone)
        container.questGeneration.generateForToday(profile.timezone)

        val today = container.clock.today(
            runCatching { java.time.ZoneId.of(profile.timezone) }
                .getOrDefault(java.time.ZoneId.systemDefault()),
        )
        val dateStr = today.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
        val count = container.db.questDao().getInstancesForDate(dateStr).size
        container.eventBus.publish(DomainEvent.DailyQuestsReady(dateStr, count))

        return Result.success()
    }

    companion object {
        private const val UNIQUE = "day_boundary"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DayBoundaryWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
