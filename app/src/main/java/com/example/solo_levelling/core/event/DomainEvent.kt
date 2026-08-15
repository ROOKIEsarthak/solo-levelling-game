package com.example.solo_levelling.core.event

sealed class DomainEvent {
    data class QuestCompleted(
        val instanceId: Long,
        val templateId: Long,
        val xp: Int,
        val attributeRewardsJson: String,
        val completedAtEpochMs: Long,
    ) : DomainEvent()

    data class QuestUndone(
        val instanceId: Long,
        val xpReversed: Int,
        val undoneAtEpochMs: Long,
    ) : DomainEvent()

    data class XpAwarded(
        val ledgerId: Long,
        val amount: Int,
        val sourceType: String,
        val sourceId: String,
        val totalXpAfter: Int,
    ) : DomainEvent()

    data class XpReversed(
        val ledgerId: Long,
        val amount: Int,
        val sourceType: String,
        val sourceId: String,
        val totalXpAfter: Int,
    ) : DomainEvent()

    data class LevelUp(val oldLevel: Int, val newLevel: Int) : DomainEvent()

    data class RankUp(val oldRank: String, val newRank: String) : DomainEvent()

    data class AttributesProgressed(val deltasJson: String) : DomainEvent()

    data class StreakUpdated(val current: Int, val best: Int) : DomainEvent()

    data class AchievementUnlocked(val key: String, val unlockedAtEpochMs: Long) : DomainEvent()

    data class BossProgressUpdated(val bossId: Long, val progress: Float) : DomainEvent()

    data class SkillLevelUp(val skillId: Long, val newLevel: Int) : DomainEvent()
}
