package com.example.solo_levelling.domain.handler

import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.core.event.DomainEvent
import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.AppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.PlayerAchievementEntity
import com.example.solo_levelling.domain.service.ProgressionService
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class AchievementHandler(
    private val db: JsonDatabase,
    private val eventBus: EventBus,
    private val clock: AppClock,
    private val progression: ProgressionService,
    private val scope: CoroutineScope,
) {
    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE

    fun start() {
        scope.launch {
            eventBus.events.collect { event ->
                when (event) {
                    is DomainEvent.QuestCompleted,
                    is DomainEvent.StreakUpdated,
                    is DomainEvent.LevelUp,
                    is DomainEvent.BossProgressUpdated,
                    -> evaluate()
                    else -> Unit
                }
            }
        }
    }

    suspend fun evaluate() {
        val defs = db.achievementDao().getDefs()
        val unlocked = db.achievementDao().getUnlocked().map { it.achievementKey }.toSet()
        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID) ?: return
        val streak = db.playerDao().getStreak(SystemDefaults.PLAYER_ID)
        val questsCompleted = db.questDao().countCompletedAll()
        val dsaCount = db.moduleDao().countDsaSolved()
        val bossCleared = db.moduleDao().countBossCleared()
        val perfectWeek = isPerfectWeek(profile.timezone)

        for (def in defs) {
            if (def.key in unlocked) continue
            val met = when (def.criteriaType) {
                "QUESTS_COMPLETED" -> questsCompleted >= def.criteriaValue
                "STREAK" -> (streak?.best ?: 0) >= def.criteriaValue || (streak?.current ?: 0) >= def.criteriaValue
                "LEVEL" -> profile.level >= def.criteriaValue
                "DSA_SOLVED" -> dsaCount >= def.criteriaValue
                "BOSS_CLEARED" -> bossCleared >= def.criteriaValue
                "PERFECT_WEEK" -> perfectWeek
                else -> false
            }
            if (!met) continue
            val now = clock.nowEpochMs()
            db.achievementDao().unlock(PlayerAchievementEntity(def.key, now))
            if (def.rewardXp > 0) {
                progression.award(
                    sourceType = "ACHIEVEMENT",
                    sourceId = "ACH_${def.key}",
                    amount = def.rewardXp,
                    applyDailyCap = true,
                )
            }
            eventBus.publish(DomainEvent.AchievementUnlocked(def.key, now))
        }
    }

    private suspend fun isPerfectWeek(timezone: String): Boolean {
        val zone = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneId.systemDefault())
        val today = clock.today(zone)
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).format(dateFmt)
        val weekEnd = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusDays(6).format(dateFmt)
        val total = db.questDao().countTotalInRange(weekStart, weekEnd)
        if (total == 0) return false
        val completed = db.questDao().countCompletedInRange(weekStart, weekEnd)
        val incomplete = db.questDao().countIncompleteInRange(weekStart, weekEnd)
        return completed == total && incomplete == 0
    }
}
