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
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class DailyQuestWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val container = applicationContext.appContainer
        val profile = container.db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
        if (profile?.onboardingDone == true) {
            container.questGeneration.generateForToday(profile.timezone)
            val zone = runCatching { ZoneId.of(profile.timezone) }
                .getOrDefault(ZoneId.systemDefault())
            val today = container.clock.today(zone)
            val dateStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val count = container.db.questDao().getInstancesForDate(dateStr).size
            container.eventBus.publish(DomainEvent.DailyQuestsReady(dateStr, count))
        }
        return Result.success()
    }

    companion object {
        private const val UNIQUE = "daily_quest_generation"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailyQuestWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
