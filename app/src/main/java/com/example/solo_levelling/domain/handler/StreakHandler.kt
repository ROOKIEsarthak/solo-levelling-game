package com.example.solo_levelling.domain.handler

import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.core.event.DomainEvent
import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.AppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.QuestInstanceEntity
import com.example.solo_levelling.data.db.entity.StreakStateEntity
import com.example.solo_levelling.domain.logic.StreakLogic
import com.example.solo_levelling.domain.model.QuestStatus
import com.example.solo_levelling.domain.service.ModuleFlags
import com.example.solo_levelling.domain.service.ModuleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

class StreakHandler(
    private val db: JsonDatabase,
    private val eventBus: EventBus,
    private val clock: AppClock,
    private val scope: CoroutineScope,
) {
    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE

    fun start() {
        scope.launch {
            eventBus.events.collect { event ->
                when (event) {
                    is DomainEvent.QuestCompleted -> onQuestCompleted()
                    is DomainEvent.QuestUndone -> onQuestUndone()
                    else -> Unit
                }
            }
        }
    }

    private suspend fun onQuestCompleted() {
        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID) ?: return
        val zone = runCatching { ZoneId.of(profile.timezone) }.getOrDefault(ZoneId.systemDefault())
        val today = clock.today(zone)
        val todayStr = today.format(dateFmt)

        val modules = ModuleFlags.resolve(
            onboardingDone = profile.onboardingDone,
            career = db.configDao().get(ModuleFlags.KEY_CAREER)?.value,
            workout = db.configDao().get(ModuleFlags.KEY_WORKOUT)?.value,
            diet = db.configDao().get(ModuleFlags.KEY_DIET)?.value,
        )
        val completedTodayAllowed = db.questDao().getInstancesForDate(todayStr)
            .filter { it.status == QuestStatus.COMPLETED.name }
            .any { instance ->
                val tags = db.questDao().getTemplateById(instance.templateId)?.priorityTags.orEmpty()
                ModuleScope.allowsQuestTemplate(tags, modules)
            }
        if (!completedTodayAllowed) return

        val streak = db.playerDao().getStreak(SystemDefaults.PLAYER_ID) ?: StreakStateEntity()

        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).format(dateFmt)
        var recoveryUsed = streak.recoveryUsedThisWeek
        if (streak.weekStartDate != weekStart) {
            recoveryUsed = 0
        }

        val last = streak.lastCompletedDate?.let { LocalDate.parse(it, dateFmt) }
        if (StreakLogic.isStreakBroken(last, today)) {
            maybeSpawnRecovery(todayStr, recoveryUsed)
            recoveryUsed = (db.playerDao().getStreak(SystemDefaults.PLAYER_ID)?.recoveryUsedThisWeek) ?: recoveryUsed
        }

        val newCurrent = StreakLogic.computeNewStreak(streak.current, last, today)
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

    private suspend fun onQuestUndone() {
        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID) ?: return
        val zone = runCatching { ZoneId.of(profile.timezone) }.getOrDefault(ZoneId.systemDefault())
        val today = clock.today(zone)
        val todayStr = today.format(dateFmt)
        val modules = ModuleFlags.resolve(
            onboardingDone = profile.onboardingDone,
            career = db.configDao().get(ModuleFlags.KEY_CAREER)?.value,
            workout = db.configDao().get(ModuleFlags.KEY_WORKOUT)?.value,
            diet = db.configDao().get(ModuleFlags.KEY_DIET)?.value,
        )
        val completedToday = db.questDao().getInstancesForDate(todayStr)
            .filter { it.status == QuestStatus.COMPLETED.name }
            .any { instance ->
                val tags = db.questDao().getTemplateById(instance.templateId)?.priorityTags.orEmpty()
                ModuleScope.allowsQuestTemplate(tags, modules)
            }
        if (completedToday) return

        val streak = db.playerDao().getStreak(SystemDefaults.PLAYER_ID) ?: return
        if (streak.lastCompletedDate != todayStr) return

        val newCurrent = (streak.current - 1).coerceAtLeast(0)
        val newLast = if (newCurrent == 0) null else today.minusDays(1).format(dateFmt)
        db.playerDao().upsertStreak(
            streak.copy(
                current = newCurrent,
                lastCompletedDate = newLast,
            ),
        )
        eventBus.publish(DomainEvent.StreakUpdated(newCurrent, streak.best))
    }

    private suspend fun maybeSpawnRecovery(todayStr: String, recoveryUsed: Int) {
        if (recoveryUsed >= SystemDefaults.WEEKLY_RECOVERY_LIMIT) return
        val template = db.questDao().getTemplateByKey("recovery")
        val instanceId = db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = template?.id ?: -1L,
                scheduledDate = todayStr,
                status = QuestStatus.AVAILABLE.name,
                title = template?.title ?: "Recovery quest",
                type = "RECOVERY",
                baseXp = template?.baseXp ?: 15,
                attributeRewardsJson = template?.attributeRewardsJson ?: """{"DISC":15}""",
                verificationType = template?.verificationType ?: "MANUAL",
                verificationTarget = template?.verificationTarget ?: 0f,
                verificationUnit = template?.verificationUnit ?: "",
            ),
        )
        if (instanceId > 0) {
            eventBus.publish(DomainEvent.RecoveryQuestAvailable(instanceId, todayStr))
        }
        val streak = db.playerDao().getStreak(SystemDefaults.PLAYER_ID) ?: StreakStateEntity()
        db.playerDao().upsertStreak(streak.copy(recoveryUsedThisWeek = recoveryUsed + 1))
    }
}
