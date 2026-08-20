package com.example.solo_levelling.domain.handler

import com.example.solo_levelling.core.event.DomainEvent
import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.BossEntity
import com.example.solo_levelling.data.db.entity.BossQuestEntity
import com.example.solo_levelling.domain.logic.BossProgressLogic
import com.example.solo_levelling.domain.model.AttributeCode
import com.example.solo_levelling.domain.service.ModuleScope
import com.example.solo_levelling.domain.service.ProgressionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class BossProgressHandler(
    private val db: JsonDatabase,
    private val eventBus: EventBus,
    private val progression: ProgressionService,
    private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch {
            eventBus.events.collect { event ->
                when (event) {
                    is DomainEvent.QuestCompleted -> Unit // PostQuestCompletionCoordinator
                    is DomainEvent.QuestUndone -> Unit // PostQuestCompletionCoordinator
                    else -> Unit
                }
            }
        }
    }

    /** Canonical quest-driven boss progress. Idempotent for already-completed boss quest flags. */
    suspend fun applyQuestCompleted(templateId: Long) {
        val boss = db.moduleDao().getActiveBoss() ?: return
        val template = db.questDao().getTemplateById(templateId) ?: return
        val modules = progression.currentModules()
        if (!ModuleScope.allowsQuestTemplate(template.priorityTags, modules)) return
        val bossQuests = db.moduleDao().getBossQuests(boss.id)
        val matching = bossQuests.firstOrNull { it.templateKey == template.key && !it.completed }
            ?: return

        db.moduleDao().updateBossQuest(matching.copy(completed = true))
        recalculateAndPersist(boss)
    }

    suspend fun applyQuestUndone(instanceId: Long) {
        val instance = db.questDao().getInstance(instanceId) ?: return
        val template = db.questDao().getTemplateById(instance.templateId) ?: return
        val modules = progression.currentModules()
        if (!ModuleScope.allowsQuestTemplate(template.priorityTags, modules)) return

        var matchedBoss: BossEntity? = null
        var matching: BossQuestEntity? = null
        for (candidate in db.moduleDao().getBosses()) {
            val quest = db.moduleDao().getBossQuests(candidate.id)
                .firstOrNull { it.templateKey == template.key && it.completed }
            if (quest != null) {
                matchedBoss = candidate
                matching = quest
                break
            }
        }
        val boss = matchedBoss ?: return
        val bossQuest = matching ?: return

        val wasCleared = boss.status == "CLEARED"
        db.moduleDao().updateBossQuest(bossQuest.copy(completed = false))
        val updated = recalculateAndPersist(boss)

        if (wasCleared && updated.status != "CLEARED") {
            progression.reverse(
                originalSourceType = "BOSS",
                originalSourceId = "boss_${boss.id}",
                reverseSourceType = "BOSS_UNDO",
                reverseSourceId = "UNDO_BOSS_${boss.id}",
            )
        }
    }

    private suspend fun recalculateAndPersist(boss: BossEntity): BossEntity {
        val updatedQuests = db.moduleDao().getBossQuests(boss.id)
        val progress = BossProgressLogic.weightedProgress(
            updatedQuests.map { BossProgressLogic.QuestWeight(it.completed, it.weight) },
        )
        val newValue = BossProgressLogic.bossCurrentValue(progress, boss.targetValue)
        val cleared = BossProgressLogic.isCleared(newValue, boss.targetValue)
        val updated = boss.copy(
            currentValue = newValue.coerceAtMost(boss.targetValue),
            status = if (cleared) "CLEARED" else "ACTIVE",
        )
        db.moduleDao().updateBoss(updated)
        eventBus.publish(DomainEvent.BossProgressUpdated(boss.id, progress))
        if (cleared && boss.status != "CLEARED") {
            progression.award(
                sourceType = "BOSS",
                sourceId = "boss_${boss.id}",
                amount = boss.xpReward,
                attrs = mapOf(AttributeCode.DISC to (boss.xpReward / 2).coerceAtLeast(1)),
                applyDailyCap = true,
            )
            eventBus.publish(DomainEvent.BossCompleted(boss.id, boss.xpReward))
        }
        return updated
    }
}
