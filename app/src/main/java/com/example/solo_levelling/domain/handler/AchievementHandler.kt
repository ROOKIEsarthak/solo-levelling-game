package com.example.solo_levelling.domain.handler

import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.core.event.DomainEvent
import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.AppClock
import com.example.solo_levelling.data.db.AppDatabase
import com.example.solo_levelling.data.db.entity.PlayerAchievementEntity
import com.example.solo_levelling.data.db.entity.XpLedgerEntryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class AchievementHandler(
    private val db: AppDatabase,
    private val eventBus: EventBus,
    private val clock: AppClock,
    private val scope: CoroutineScope,
) {
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

        for (def in defs) {
            if (def.key in unlocked) continue
            val met = when (def.criteriaType) {
                "QUESTS_COMPLETED" -> questsCompleted >= def.criteriaValue
                "STREAK" -> (streak?.best ?: 0) >= def.criteriaValue || (streak?.current ?: 0) >= def.criteriaValue
                "LEVEL" -> profile.level >= def.criteriaValue
                "DSA_SOLVED" -> dsaCount >= def.criteriaValue
                "BOSS_CLEARED" -> bossCleared >= def.criteriaValue
                else -> false
            }
            if (!met) continue
            val now = clock.nowEpochMs()
            db.achievementDao().unlock(PlayerAchievementEntity(def.key, now))
            if (def.rewardXp > 0) {
                val sourceId = "ACH_${def.key}"
                if (db.xpDao().findBySource("ACHIEVEMENT", sourceId) == null) {
                    val latest = db.playerDao().getProfile(SystemDefaults.PLAYER_ID) ?: profile
                    val newTotal = latest.totalXp + def.rewardXp
                    val newLevel = SystemDefaults.levelFromTotalXp(newTotal)
                    val newRank = SystemDefaults.rankForLevel(newLevel)
                    db.xpDao().insertLedger(
                        XpLedgerEntryEntity(
                            amount = def.rewardXp,
                            sourceType = "ACHIEVEMENT",
                            sourceId = sourceId,
                            createdAtEpochMs = now,
                        ),
                    )
                    db.playerDao().upsertProfile(
                        latest.copy(totalXp = newTotal, level = newLevel, rank = newRank),
                    )
                }
            }
            eventBus.publish(DomainEvent.AchievementUnlocked(def.key, now))
        }
    }
}
