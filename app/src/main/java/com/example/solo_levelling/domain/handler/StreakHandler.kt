package com.example.solo_levelling.domain.handler

import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.core.event.DomainEvent
import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.AppClock
import com.example.solo_levelling.data.db.AppDatabase
import com.example.solo_levelling.data.db.entity.QuestInstanceEntity
import com.example.solo_levelling.data.db.entity.StreakStateEntity
import com.example.solo_levelling.domain.model.QuestStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

class StreakHandler(
    private val db: AppDatabase,
    private val eventBus: EventBus,
    private val clock: AppClock,
    private val scope: CoroutineScope,
) {
    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE

    fun start() {
        scope.launch {
            eventBus.events.collect { event ->
                if (event is DomainEvent.QuestCompleted) {
                    onQuestCompleted()
                }
            }
        }
    }

    private suspend fun onQuestCompleted() {
        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID) ?: return
        val zone = runCatching { ZoneId.of(profile.timezone) }.getOrDefault(ZoneId.systemDefault())
        val today = clock.today(zone)
        val todayStr = today.format(dateFmt)
        val streak = db.playerDao().getStreak(SystemDefaults.PLAYER_ID) ?: StreakStateEntity()

        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).format(dateFmt)
        var recoveryUsed = streak.recoveryUsedThisWeek
        if (streak.weekStartDate != weekStart) {
            recoveryUsed = 0
        }

        val last = streak.lastCompletedDate?.let { LocalDate.parse(it, dateFmt) }
        val broken = last != null && last != today && last != today.minusDays(1)
        if (broken) {
            maybeSpawnRecovery(todayStr, recoveryUsed)
            recoveryUsed = (db.playerDao().getStreak(SystemDefaults.PLAYER_ID)?.recoveryUsedThisWeek) ?: recoveryUsed
        }

        val newCurrent = when {
            last == null -> 1
            last == today -> streak.current
            last == today.minusDays(1) -> streak.current + 1
            else -> 1
        }
        val newBest = maxOf(streak.best, newCurrent)
        db.playerDao().upsertStreak(
            StreakStateEntity(
                current = newCurrent,
                best = newBest,
                lastCompletedDate = todayStr,
                recoveryUsedThisWeek = recoveryUsed,
                weekStartDate = weekStart,
            ),
        )
        eventBus.publish(DomainEvent.StreakUpdated(newCurrent, newBest))
    }

    private suspend fun maybeSpawnRecovery(todayStr: String, recoveryUsed: Int) {
        if (recoveryUsed >= SystemDefaults.WEEKLY_RECOVERY_LIMIT) return
        val template = db.questDao().getTemplateByKey("recovery")
        db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = template?.id ?: -1L,
                scheduledDate = todayStr,
                status = QuestStatus.AVAILABLE.name,
                title = template?.title ?: "Recovery quest",
                type = "RECOVERY",
                baseXp = template?.baseXp ?: 15,
                attributeRewardsJson = template?.attributeRewardsJson ?: """{"DISC":15}""",
            ),
        )
        val streak = db.playerDao().getStreak(SystemDefaults.PLAYER_ID) ?: StreakStateEntity()
        db.playerDao().upsertStreak(streak.copy(recoveryUsedThisWeek = recoveryUsed + 1))
    }
}
