package com.example.solo_levelling.domain.service

import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.core.event.DomainEvent
import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.AppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.domain.logic.DayBoundaryLogic
import com.example.solo_levelling.domain.model.QuestStatus
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class DayBoundaryService(
    private val db: JsonDatabase,
    private val eventBus: EventBus,
    private val clock: AppClock,
) {
    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE

    suspend fun runDailyBoundary(timezone: String = "Asia/Kolkata") {
        val zone = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneId.systemDefault())
        val today = clock.today(zone)
        val yesterday = today.minusDays(1)
        markMissedQuestsForYesterday(yesterday)
        applyStreakDecayIfMissed(today)
    }

    suspend fun markMissedQuestsForYesterday(yesterday: LocalDate) {
        val yesterdayStr = yesterday.format(dateFmt)
        val todayStr = yesterday.plusDays(1).format(dateFmt)
        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
        val modules = ModuleFlags.resolve(
            onboardingDone = profile?.onboardingDone == true,
            career = db.configDao().get(ModuleFlags.KEY_CAREER)?.value,
            workout = db.configDao().get(ModuleFlags.KEY_WORKOUT)?.value,
            diet = db.configDao().get(ModuleFlags.KEY_DIET)?.value,
        )
        val tagsById = db.questDao().getActiveTemplates().associate { it.id to it.priorityTags }
        val instances = db.questDao().getInstancesBeforeDate(todayStr)
            .filter { it.scheduledDate == yesterdayStr }
            .filter { ModuleScope.allowsQuestTemplate(tagsById[it.templateId].orEmpty(), modules) }

        val events = mutableListOf<DomainEvent>()
        for (instance in instances) {
            db.questDao().updateInstance(instance.copy(status = QuestStatus.MISSED.name))
            events += DomainEvent.QuestMissed(instance.id, instance.scheduledDate)
        }
        events.forEach { eventBus.publish(it) }
    }

    suspend fun applyStreakDecayIfMissed(today: LocalDate) {
        val streak = db.playerDao().getStreak(SystemDefaults.PLAYER_ID) ?: return
        val lastDateStr = streak.lastCompletedDate ?: return
        val lastDate = LocalDate.parse(lastDateStr, dateFmt)
        val graceDays = db.configDao().get("STREAK_GRACE_DAYS")?.value?.toIntOrNull()
            ?: SystemDefaults.STREAK_GRACE_DAYS

        if (!DayBoundaryLogic.shouldResetStreak(lastDate, today, graceDays)) return

        val previous = streak.current
        db.playerDao().upsertStreak(streak.copy(current = 0))
        eventBus.publish(DomainEvent.StreakUpdated(0, streak.best))
        if (previous > 0) {
            eventBus.publish(DomainEvent.StreakBroken(previous, streak.best))
        }
    }
}
