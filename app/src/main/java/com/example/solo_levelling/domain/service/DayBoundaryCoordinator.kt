package com.example.solo_levelling.domain.service

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.core.time.AppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.UserConfigEntity
import com.example.solo_levelling.work.DayBoundaryWorker
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Owns calendar-boundary orchestration: timezone sync, catch-up, and next-local-midnight scheduling.
 * Domain rules live in [DayBoundaryService]; WorkManager stays a thin adapter via [DayBoundaryWorker].
 */
class DayBoundaryCoordinator(
    private val db: JsonDatabase,
    private val clock: AppClock,
    private val dayBoundary: DayBoundaryService,
    private val questGeneration: QuestGenerationService,
) {
    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE

    suspend fun syncTimezoneFromDevice(): String {
        val deviceZone = ZoneId.systemDefault().id
        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID) ?: return deviceZone
        if (profile.timezone != deviceZone) {
            db.playerDao().upsertProfile(profile.copy(timezone = deviceZone))
        }
        return deviceZone
    }

    suspend fun ensureCatchUpAndSchedule(context: Context) {
        val timezone = syncTimezoneFromDevice()
        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
        if (profile?.onboardingDone != true) {
            cancelLegacyPeriodic(context)
            return
        }
        runBoundaryIfNeeded(timezone)
        scheduleNextMidnight(context, timezone)
    }

    /** Called by [DayBoundaryWorker] when the one-time midnight work fires. */
    suspend fun onBoundaryFired(context: Context) {
        val timezone = syncTimezoneFromDevice()
        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
        if (profile?.onboardingDone != true) {
            scheduleNextMidnight(context, timezone)
            return
        }
        runBoundaryIfNeeded(timezone)
        scheduleNextMidnight(context, timezone)
    }

    /**
     * Runs domain boundary once per local calendar day (idempotent via last-boundary config).
     * Safe for late WorkManager execution and duplicate/startup catch-up.
     */
    suspend fun runBoundaryIfNeeded(timezone: String = ZoneId.systemDefault().id): Boolean {
        val zone = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneId.systemDefault())
        val today = clock.today(zone)
        val todayStr = today.format(dateFmt)
        val last = db.configDao().get(KEY_LAST_DAY_BOUNDARY)?.value
        if (last == todayStr) return false

        dayBoundary.runDailyBoundary(timezone)
        questGeneration.generateForToday(timezone)
        db.configDao().upsert(UserConfigEntity(key = KEY_LAST_DAY_BOUNDARY, value = todayStr))
        return true
    }

    fun scheduleNextMidnight(context: Context, timezone: String = ZoneId.systemDefault().id) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(LEGACY_PERIODIC)
        val zone = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneId.systemDefault())
        val delayMs = clock.millisUntilNextLocalMidnight(zone)
        val request = OneTimeWorkRequestBuilder<DayBoundaryWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()
        wm.enqueueUniqueWork(
            UNIQUE_MIDNIGHT,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancelLegacyPeriodic(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(LEGACY_PERIODIC)
    }

    companion object {
        const val KEY_LAST_DAY_BOUNDARY = "last_day_boundary_date"
        const val UNIQUE_MIDNIGHT = "day_boundary_midnight"
        private const val LEGACY_PERIODIC = "day_boundary"
    }
}
