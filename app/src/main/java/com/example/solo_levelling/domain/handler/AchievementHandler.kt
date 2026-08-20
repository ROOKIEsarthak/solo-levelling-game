package com.example.solo_levelling.domain.handler

import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.core.event.DomainEvent
import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.AppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.PlayerAchievementEntity
import com.example.solo_levelling.domain.model.QuestStatus
import com.example.solo_levelling.domain.service.ModuleScope
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
        val modules = progression.currentModules()
        val streak = db.playerDao().getStreak(SystemDefaults.PLAYER_ID)
        val questsCompleted = countActiveModuleCompletedQuests(modules)
        val dsaCount = if (modules.career) db.moduleDao().countDsaSolved() else 0
        val bossCleared = db.moduleDao().countBossCleared()
        val perfectWeek = isPerfectWeek(profile.timezone, modules)

        for (def in defs) {
            if (def.key in unlocked) continue
            if (!ModuleScope.allowsAchievement(def.criteriaType, modules, def.key)) continue
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
                    metadataJson = """{"module":"GLOBAL"}""",
                    applyDailyCap = true,
                    modules = modules,
                )
            }
            eventBus.publish(DomainEvent.AchievementUnlocked(def.key, now))
        }
    }

    private suspend fun countActiveModuleCompletedQuests(
        modules: com.example.solo_levelling.domain.service.EnabledModules,
    ): Int {
        // Approximate: count completed instances whose templates are allowed
        val allCompleted = db.questDao().getInstancesInRange("1970-01-01", "9999-12-31")
            .filter { it.status == QuestStatus.COMPLETED.name }
        return allCompleted.count { instance ->
            val tags = db.questDao().getTemplateById(instance.templateId)?.priorityTags.orEmpty()
            ModuleScope.allowsQuestTemplate(tags, modules)
        }
    }

    private suspend fun isPerfectWeek(
        timezone: String,
        modules: com.example.solo_levelling.domain.service.EnabledModules,
    ): Boolean {
        val zone = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneId.systemDefault())
        val today = clock.today(zone)
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).format(dateFmt)
        val weekEnd = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusDays(6).format(dateFmt)
        val instances = db.questDao().getInstancesInRange(weekStart, weekEnd)
            .filter { instance ->
                val tags = db.questDao().getTemplateById(instance.templateId)?.priorityTags.orEmpty()
                ModuleScope.allowsQuestTemplate(tags, modules)
            }
        if (instances.isEmpty()) return false
        val completed = instances.count { it.status == QuestStatus.COMPLETED.name }
        val incomplete = instances.count {
            it.status != QuestStatus.COMPLETED.name && it.status != QuestStatus.MISSED.name
        }
        return completed == instances.size && incomplete == 0
    }
}
