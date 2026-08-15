package com.example.solo_levelling.domain.logic

object BossProgressLogic {
    data class QuestWeight(val completed: Boolean, val weight: Float)

    fun weightedProgress(quests: List<QuestWeight>): Float {
        val totalWeight = quests.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(1f)
        val completedWeight = quests.filter { it.completed }.sumOf { it.weight.toDouble() }.toFloat()
        return completedWeight / totalWeight
    }

    fun bossCurrentValue(progress: Float, targetValue: Float): Float =
        (progress * targetValue).coerceAtMost(targetValue)

    fun isCleared(currentValue: Float, targetValue: Float): Boolean = currentValue >= targetValue
}
