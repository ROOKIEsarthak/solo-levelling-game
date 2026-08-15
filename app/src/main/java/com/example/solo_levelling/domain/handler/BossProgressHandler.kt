package com.example.solo_levelling.domain.handler

import com.example.solo_levelling.core.event.DomainEvent
import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.domain.logic.BossProgressLogic
import com.example.solo_levelling.domain.model.AttributeCode
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
                if (event is DomainEvent.QuestCompleted) {
                    onQuestCompleted(event)
                }
            }
        }
    }

    private suspend fun onQuestCompleted(event: DomainEvent.QuestCompleted) {
        val boss = db.moduleDao().getActiveBoss() ?: return
        val template = db.questDao().getTemplateById(event.templateId) ?: return
        val bossQuests = db.moduleDao().getBossQuests(boss.id)
        val matching = bossQuests.firstOrNull { it.templateKey == template.key && !it.completed }
            ?: return

        db.moduleDao().updateBossQuest(matching.copy(completed = true))

        val updatedQuests = db.moduleDao().getBossQuests(boss.id)
        val progress = BossProgressLogic.weightedProgress(
            updatedQuests.map { BossProgressLogic.QuestWeight(it.completed, it.weight) },
        )
        val newValue = BossProgressLogic.bossCurrentValue(progress, boss.targetValue)
        val cleared = BossProgressLogic.isCleared(newValue, boss.targetValue)

        db.moduleDao().updateBoss(
            boss.copy(
                currentValue = newValue.coerceAtMost(boss.targetValue),
                status = if (cleared) "CLEARED" else "ACTIVE",
            ),
        )
        eventBus.publish(DomainEvent.BossProgressUpdated(boss.id, progress))
        if (cleared) {
            progression.award(
                sourceType = "BOSS",
                sourceId = "boss_${boss.id}",
                amount = boss.xpReward,
                attrs = mapOf(AttributeCode.DISC to (boss.xpReward / 2).coerceAtLeast(1)),
                applyDailyCap = true,
            )
            eventBus.publish(DomainEvent.BossCompleted(boss.id, boss.xpReward))
        }
    }
}
