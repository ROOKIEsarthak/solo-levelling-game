package com.example.solo_levelling.domain.service

import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.core.event.DomainEvent
import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.AppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.domain.model.QuestStatus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class QuestCompletionService(
    private val db: JsonDatabase,
    private val eventBus: EventBus,
    private val clock: AppClock,
    private val progression: ProgressionService,
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

        val instance = questDao.getInstance(instanceId) ?: return Result.NotFound
        if (instance.status == QuestStatus.COMPLETED.name) return Result.AlreadyCompleted
        if (instance.status != QuestStatus.AVAILABLE.name && instance.status != QuestStatus.IN_PROGRESS.name) {
            return Result.InvalidStatus
        }

        val sourceType = "QUEST_INSTANCE"
        val sourceId = instanceId.toString()
        if (db.xpDao().findBySource(sourceType, sourceId) != null) return Result.AlreadyCompleted

        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
            ?: return Result.NotFound

        val dayStart = startOfDayMs(profile.timezone)
        val dayEnd = dayStart + 24L * 60 * 60 * 1000
        val earnedToday = db.xpDao().sumXpBetween(dayStart, dayEnd)
        if (earnedToday >= SystemDefaults.DAILY_XP_CAP) return Result.DailyCapReached
        val xpAvailable = minOf(instance.baseXp, SystemDefaults.DAILY_XP_CAP - earnedToday)
        if (xpAvailable <= 0) return Result.DailyCapReached

        val now = clock.nowEpochMs()
        val events = mutableListOf<DomainEvent>()
        var awardResult: ProgressionService.AwardResult? = null

        db.withTransaction {
            questDao.updateInstance(
                instance.copy(
                    status = QuestStatus.COMPLETED.name,
                    completedAtEpochMs = now,
                ),
            )
            val deltas = AttributeRewardsParser.parse(instance.attributeRewardsJson)
            val attrs = deltas.associate { it.code to it.amount }
            awardResult = progression.awardWithinTransaction(
                sourceType = sourceType,
                sourceId = sourceId,
                amount = instance.baseXp,
                attrs = attrs,
                metadataJson = """{"title":"${instance.title.replace("\"", "'")}"}""",
                applyDailyCap = true,
                events = events,
            )
        }

        when (val result = awardResult) {
            is ProgressionService.AwardResult.Success -> {
                events += DomainEvent.QuestCompleted(
                    instanceId = instance.id,
                    templateId = instance.templateId,
                    xp = result.awarded,
                    attributeRewardsJson = instance.attributeRewardsJson,
                    completedAtEpochMs = now,
                )
                events.forEach { eventBus.publish(it) }
                Result.Completed(
                    instanceId = instanceId,
                    xp = result.awarded,
                    newTotalXp = result.newTotal,
                    newLevel = result.newLevel,
                    newRank = result.newRank,
                )
            }
            ProgressionService.AwardResult.AlreadyAwarded -> Result.AlreadyCompleted
            ProgressionService.AwardResult.CapReached -> Result.DailyCapReached
            ProgressionService.AwardResult.NoProfile -> Result.NotFound
            null -> Result.NotFound
        }
    }

    suspend fun undo(instanceId: Long): Boolean = mutex.withLock {
        val questDao = db.questDao()
        val instance = questDao.getInstance(instanceId) ?: return false
        if (instance.status != QuestStatus.COMPLETED.name) return false
        val completedAt = instance.completedAtEpochMs ?: return false
        val undoWindowMs = SystemDefaults.QUEST_UNDO_MINUTES * 60_000L
        if (clock.nowEpochMs() - completedAt > undoWindowMs) return false

        val sourceType = "QUEST_INSTANCE"
        val sourceId = instanceId.toString()
        if (db.xpDao().findBySource(sourceType, sourceId) == null) return false

        val reverseSourceId = "UNDO_$instanceId"
        val deltas = AttributeRewardsParser.parse(instance.attributeRewardsJson)
        val attrs = deltas.associate { it.code to it.amount }
        val events = mutableListOf<DomainEvent>()
        var ok = false

        db.withTransaction {
            questDao.updateInstance(
                instance.copy(status = QuestStatus.AVAILABLE.name, completedAtEpochMs = null),
            )
            ok = progression.reverseWithinTransaction(
                originalSourceType = sourceType,
                originalSourceId = sourceId,
                reverseSourceType = "QUEST_UNDO",
                reverseSourceId = reverseSourceId,
                events = events,
                attrs = attrs,
            )
        }

        if (ok) {
            events += DomainEvent.QuestUndone(instanceId, instance.baseXp, clock.nowEpochMs())
            events.forEach { eventBus.publish(it) }
        }
        ok
    }

    private fun startOfDayMs(timezone: String): Long {
        val zone = runCatching { java.time.ZoneId.of(timezone) }.getOrDefault(java.time.ZoneId.systemDefault())
        return clock.today(zone).atStartOfDay(zone).toInstant().toEpochMilli()
    }
}
