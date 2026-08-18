package com.example.solo_levelling.domain.service

import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.core.event.DomainEvent
import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.AppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.XpLedgerEntryEntity
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
        data object ModuleDisabled : Result()
    }

    suspend fun complete(instanceId: Long): Result {
        val pending = mutex.withLock {
            val questDao = db.questDao()

            val instance = questDao.getInstance(instanceId) ?: return@withLock Pending(Result.NotFound, emptyList())
            if (instance.status == QuestStatus.COMPLETED.name) {
                return@withLock Pending(Result.AlreadyCompleted, emptyList())
            }
            if (instance.status != QuestStatus.AVAILABLE.name && instance.status != QuestStatus.IN_PROGRESS.name) {
                return@withLock Pending(Result.InvalidStatus, emptyList())
            }

            val template = questDao.getTemplateById(instance.templateId)
            val modules = progression.currentModules()
            val priorityTags = template?.priorityTags.orEmpty()
            if (!ModuleScope.allowsQuestTemplate(priorityTags, modules)) {
                return@withLock Pending(Result.ModuleDisabled, emptyList())
            }

            val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
                ?: return@withLock Pending(Result.NotFound, emptyList())

            val moduleId = ModuleScope.moduleForPriorityTags(priorityTags)
            var xpAmount = instance.baseXp
            if (moduleId == ModuleId.WORKOUT) {
                xpAmount = (xpAmount * workoutProgressionScale()).toInt().coerceAtLeast(0)
            }

            val now = clock.nowEpochMs()
            val sourceType = "QUEST_INSTANCE"
            val priorAwards = db.xpDao().getAllLedger().count {
                it.sourceType == "QUEST_INSTANCE" &&
                    (it.sourceId == instanceId.toString() || it.sourceId.startsWith("${instanceId}_"))
            }
            val sourceId = "${instanceId}_${now}_$priorAwards"
            val events = mutableListOf<DomainEvent>()
            var awardResult: ProgressionService.AwardResult? = null
            val metadata =
                """{"title":"${instance.title.replace("\"", "'")}","module":"${moduleId.name}"}"""

            db.withTransaction {
                val deltas = AttributeRewardsParser.parse(instance.attributeRewardsJson)
                val attrs = deltas.associate { it.code to it.amount }
                awardResult = progression.awardWithinTransaction(
                    sourceType = sourceType,
                    sourceId = sourceId,
                    amount = xpAmount,
                    attrs = attrs,
                    metadataJson = metadata,
                    applyDailyCap = true,
                    events = events,
                    modules = modules,
                )
                if (awardResult is ProgressionService.AwardResult.Success) {
                    questDao.updateInstance(
                        instance.copy(
                            status = QuestStatus.COMPLETED.name,
                            completedAtEpochMs = now,
                        ),
                    )
                }
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
                    Pending(
                        Result.Completed(
                            instanceId = instanceId,
                            xp = result.awarded,
                            newTotalXp = result.newTotal,
                            newLevel = result.newLevel,
                            newRank = result.newRank,
                        ),
                        events.toList(),
                    )
                }
                ProgressionService.AwardResult.AlreadyAwarded -> Pending(Result.AlreadyCompleted, emptyList())
                ProgressionService.AwardResult.CapReached -> Pending(Result.DailyCapReached, emptyList())
                ProgressionService.AwardResult.ModuleDisabled -> Pending(Result.ModuleDisabled, emptyList())
                ProgressionService.AwardResult.NoProfile -> Pending(Result.NotFound, emptyList())
                null -> Pending(Result.NotFound, emptyList())
            }
        }
        pending.events.forEach { eventBus.publish(it) }
        return pending.result
    }

    suspend fun undo(instanceId: Long): Boolean {
        val pending = mutex.withLock {
            val questDao = db.questDao()
            val instance = questDao.getInstance(instanceId) ?: return@withLock PendingUndo(false, emptyList())
            if (instance.status != QuestStatus.COMPLETED.name) return@withLock PendingUndo(false, emptyList())
            val completedAt = instance.completedAtEpochMs ?: return@withLock PendingUndo(false, emptyList())
            val undoWindowMs = SystemDefaults.QUEST_UNDO_MINUTES * 60_000L
            if (clock.nowEpochMs() - completedAt > undoWindowMs) return@withLock PendingUndo(false, emptyList())

            val award = findUnrevertedQuestAward(instanceId) ?: return@withLock PendingUndo(false, emptyList())

            val deltas = AttributeRewardsParser.parse(instance.attributeRewardsJson)
            val attrs = deltas.associate { it.code to it.amount }
            val events = mutableListOf<DomainEvent>()
            var ok = false

            db.withTransaction {
                questDao.updateInstance(
                    instance.copy(status = QuestStatus.AVAILABLE.name, completedAtEpochMs = null),
                )
                ok = progression.reverseWithinTransaction(
                    originalSourceType = award.sourceType,
                    originalSourceId = award.sourceId,
                    reverseSourceType = "QUEST_UNDO",
                    reverseSourceId = "UNDO_${award.id}",
                    events = events,
                    attrs = attrs,
                )
            }

            if (ok) {
                events += DomainEvent.QuestUndone(instanceId, instance.baseXp, clock.nowEpochMs())
                PendingUndo(true, events.toList())
            } else {
                PendingUndo(false, emptyList())
            }
        }
        pending.events.forEach { eventBus.publish(it) }
        return pending.ok
    }

    private suspend fun workoutProgressionScale(): Float {
        val raw = db.configDao().get(WorkoutSplitChangeLogic.KEY_SCALE)?.value?.toFloatOrNull()
        return raw?.coerceIn(0.1f, 1f) ?: 1f
    }

    private suspend fun findUnrevertedQuestAward(instanceId: Long): XpLedgerEntryEntity? {
        val ledger = db.xpDao().getAllLedger()
        val idStr = instanceId.toString()
        val prefix = "${instanceId}_"
        val awards = ledger.filter {
            it.sourceType == "QUEST_INSTANCE" &&
                it.amount > 0 &&
                (it.sourceId == idStr || it.sourceId.startsWith(prefix))
        }
        val reversedIds = ledger
            .filter { it.sourceType == "QUEST_UNDO" && it.amount < 0 }
            .mapNotNull { parseOriginalLedgerId(it.metadataJson) }
            .toSet()
        return awards.filter { it.id !in reversedIds }.maxByOrNull { it.createdAtEpochMs }
    }

    private fun parseOriginalLedgerId(metadataJson: String): Long? =
        Regex(""""originalLedgerId"\s*:\s*(\d+)""")
            .find(metadataJson)
            ?.groupValues
            ?.get(1)
            ?.toLongOrNull()

    private data class Pending(val result: Result, val events: List<DomainEvent>)
    private data class PendingUndo(val ok: Boolean, val events: List<DomainEvent>)
}
