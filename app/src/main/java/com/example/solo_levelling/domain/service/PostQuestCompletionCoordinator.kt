package com.example.solo_levelling.domain.service

import com.example.solo_levelling.domain.handler.AchievementHandler
import com.example.solo_levelling.domain.handler.BossProgressHandler
import com.example.solo_levelling.domain.handler.StreakHandler

/**
 * Orchestrates critical post-quest derived state after the core quest XP/status commit.
 * Optional effects (notifications, analytics) remain on EventBus.
 */
class PostQuestCompletionCoordinator(
    private val streakHandler: StreakHandler,
    private val bossProgressHandler: BossProgressHandler,
    private val achievementHandler: AchievementHandler,
    private val questGeneration: QuestGenerationService,
    private val season: SeasonService,
) {
    data class CompleteContext(
        val instanceId: Long,
        val templateId: Long,
        val xpAwarded: Int,
    )

    suspend fun afterComplete(ctx: CompleteContext) {
        streakHandler.applyQuestCompleted()
        bossProgressHandler.applyQuestCompleted(ctx.templateId)
        questGeneration.unlockDependentsAfterComplete(ctx.instanceId)
        if (ctx.xpAwarded != 0) {
            season.addSeasonXp(ctx.xpAwarded)
        }
        achievementHandler.evaluate()
    }

    suspend fun afterUndo(instanceId: Long, xpReversed: Int) {
        streakHandler.applyQuestUndone()
        bossProgressHandler.applyQuestUndone(instanceId)
        questGeneration.lockDependentsAfterUndo(instanceId)
        if (xpReversed != 0) {
            season.addSeasonXp(-xpReversed)
        }
        achievementHandler.evaluate()
    }
}
