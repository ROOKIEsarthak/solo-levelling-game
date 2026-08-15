package com.example.solo_levelling.domain.service

import androidx.room.withTransaction
import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.core.event.DomainEvent
import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.AppClock
import com.example.solo_levelling.data.db.AppDatabase
import com.example.solo_levelling.data.db.entity.AttributeStatEntity
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.XpLedgerEntryEntity
import com.example.solo_levelling.domain.model.QuestStatus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class QuestCompletionService(
    private val db: AppDatabase,
    private val eventBus: EventBus,
    private val clock: AppClock,
) {
    private val mutex = Mutex()

    sealed class Result {
        data class Completed(
            val instanceId: Long,
            val xp: Int,
            val newTotalXp: Int,
            val newLevel: Int,
            val newRank: String,
        ) : Result()

        data object AlreadyCompleted : Result()
        data object NotFound : Result()
        data object InvalidStatus : Result()
        data object DailyCapReached : Result()
    }

    suspend fun complete(instanceId: Long): Result = mutex.withLock {
        val questDao = db.questDao()
        val xpDao = db.xpDao()
        val playerDao = db.playerDao()

        val instance = questDao.getInstance(instanceId) ?: return Result.NotFound
        if (instance.status == QuestStatus.COMPLETED.name) return Result.AlreadyCompleted
        if (instance.status != QuestStatus.AVAILABLE.name && instance.status != QuestStatus.IN_PROGRESS.name) {
            return Result.InvalidStatus
        }

        val sourceType = "QUEST_INSTANCE"
        val sourceId = instanceId.toString()
        if (xpDao.findBySource(sourceType, sourceId) != null) return Result.AlreadyCompleted

        val profile = playerDao.getProfile(SystemDefaults.PLAYER_ID)
            ?: PlayerProfileEntity(createdAtEpochMs = clock.nowEpochMs())

        val dayStart = startOfDayMs(profile.timezone)
        val dayEnd = dayStart + 24L * 60 * 60 * 1000
        val earnedToday = xpDao.sumXpBetween(dayStart, dayEnd)
        if (earnedToday >= SystemDefaults.DAILY_XP_CAP) return Result.DailyCapReached

        val xpToAward = minOf(instance.baseXp, SystemDefaults.DAILY_XP_CAP - earnedToday)
        if (xpToAward <= 0) return Result.DailyCapReached

        val now = clock.nowEpochMs()
        val oldLevel = profile.level
        val oldRank = profile.rank
        val newTotal = profile.totalXp + xpToAward
        val newLevel = SystemDefaults.levelFromTotalXp(newTotal)
        val newRank = SystemDefaults.rankForLevel(newLevel)

        val events = mutableListOf<DomainEvent>()

        db.withTransaction {
            questDao.updateInstance(
                instance.copy(
                    status = QuestStatus.COMPLETED.name,
                    completedAtEpochMs = now,
                ),
            )
            val ledgerId = xpDao.insertLedger(
                XpLedgerEntryEntity(
                    amount = xpToAward,
                    sourceType = sourceType,
                    sourceId = sourceId,
                    metadataJson = """{"title":"${instance.title.replace("\"", "'")}"}""",
                    createdAtEpochMs = now,
                ),
            )
            playerDao.upsertProfile(
                profile.copy(totalXp = newTotal, level = newLevel, rank = newRank),
            )

            val deltas = AttributeRewardsParser.parse(instance.attributeRewardsJson)
            val attrs = playerDao.getAttributes().associateBy { it.code }
            for (delta in deltas) {
                val existing = attrs[delta.code.name] ?: AttributeStatEntity(code = delta.code.name)
                playerDao.upsertAttribute(
                    existing.copy(
                        currentValue = existing.currentValue + delta.amount,
                        lifetimeXp = existing.lifetimeXp + delta.amount,
                    ),
                )
            }

            events += DomainEvent.QuestCompleted(
                instanceId = instance.id,
                templateId = instance.templateId,
                xp = xpToAward,
                attributeRewardsJson = instance.attributeRewardsJson,
                completedAtEpochMs = now,
            )
            events += DomainEvent.XpAwarded(
                ledgerId = ledgerId,
                amount = xpToAward,
                sourceType = sourceType,
                sourceId = sourceId,
                totalXpAfter = newTotal,
            )
            if (deltas.isNotEmpty()) {
                events += DomainEvent.AttributesProgressed(AttributeRewardsParser.toJson(deltas))
            }
            if (newLevel > oldLevel) events += DomainEvent.LevelUp(oldLevel, newLevel)
            if (newRank != oldRank) events += DomainEvent.RankUp(oldRank, newRank)
        }

        events.forEach { eventBus.publish(it) }

        Result.Completed(
            instanceId = instanceId,
            xp = xpToAward,
            newTotalXp = newTotal,
            newLevel = newLevel,
            newRank = newRank,
        )
    }

    suspend fun undo(instanceId: Long): Boolean = mutex.withLock {
        val questDao = db.questDao()
        val xpDao = db.xpDao()
        val playerDao = db.playerDao()
        val instance = questDao.getInstance(instanceId) ?: return false
        if (instance.status != QuestStatus.COMPLETED.name) return false
        val completedAt = instance.completedAtEpochMs ?: return false
        val undoWindowMs = SystemDefaults.QUEST_UNDO_MINUTES * 60_000L
        if (clock.nowEpochMs() - completedAt > undoWindowMs) return false

        val sourceType = "QUEST_INSTANCE"
        val sourceId = instanceId.toString()
        val original = xpDao.findBySource(sourceType, sourceId) ?: return false
        val reverseSourceId = "UNDO_$instanceId"
        if (xpDao.findBySource("QUEST_UNDO", reverseSourceId) != null) return false

        val profile = playerDao.getProfile(SystemDefaults.PLAYER_ID) ?: return false
        val now = clock.nowEpochMs()
        val newTotal = (profile.totalXp - original.amount).coerceAtLeast(0)
        val newLevel = SystemDefaults.levelFromTotalXp(newTotal)
        val newRank = SystemDefaults.rankForLevel(newLevel)
        val events = mutableListOf<DomainEvent>()

        db.withTransaction {
            questDao.updateInstance(
                instance.copy(status = QuestStatus.AVAILABLE.name, completedAtEpochMs = null),
            )
            val ledgerId = xpDao.insertLedger(
                XpLedgerEntryEntity(
                    amount = -original.amount,
                    sourceType = "QUEST_UNDO",
                    sourceId = reverseSourceId,
                    metadataJson = """{"originalLedgerId":${original.id}}""",
                    createdAtEpochMs = now,
                ),
            )
            playerDao.upsertProfile(profile.copy(totalXp = newTotal, level = newLevel, rank = newRank))
            val deltas = AttributeRewardsParser.parse(instance.attributeRewardsJson)
            val attrs = playerDao.getAttributes().associateBy { it.code }
            for (delta in deltas) {
                val existing = attrs[delta.code.name] ?: continue
                playerDao.upsertAttribute(
                    existing.copy(currentValue = (existing.currentValue - delta.amount).coerceAtLeast(0)),
                )
            }
            events += DomainEvent.QuestUndone(instanceId, original.amount, now)
            events += DomainEvent.XpReversed(
                ledgerId = ledgerId,
                amount = -original.amount,
                sourceType = "QUEST_UNDO",
                sourceId = reverseSourceId,
                totalXpAfter = newTotal,
            )
        }
        events.forEach { eventBus.publish(it) }
        true
    }

    private fun startOfDayMs(timezone: String): Long {
        val zone = runCatching { java.time.ZoneId.of(timezone) }.getOrDefault(java.time.ZoneId.systemDefault())
        return clock.today(zone).atStartOfDay(zone).toInstant().toEpochMilli()
    }
}
