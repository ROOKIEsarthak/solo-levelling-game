package com.example.solo_levelling.domain.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BossProgressLogicPureTest {
    @Test
    fun p_weightedProgress_allCompletedIsOne() {
        val quests = listOf(
            BossProgressLogic.QuestWeight(completed = true, weight = 2f),
            BossProgressLogic.QuestWeight(completed = true, weight = 3f),
        )
        assertEquals(1f, BossProgressLogic.weightedProgress(quests), 0.001f)
    }

    @Test
    fun p_weightedProgress_partialCompletion() {
        val quests = listOf(
            BossProgressLogic.QuestWeight(completed = true, weight = 1f),
            BossProgressLogic.QuestWeight(completed = false, weight = 3f),
        )
        assertEquals(0.25f, BossProgressLogic.weightedProgress(quests), 0.001f)
    }

    @Test
    fun n_weightedProgress_noneCompletedIsZero() {
        val quests = listOf(
            BossProgressLogic.QuestWeight(completed = false, weight = 2f),
            BossProgressLogic.QuestWeight(completed = false, weight = 2f),
        )
        assertEquals(0f, BossProgressLogic.weightedProgress(quests), 0.001f)
    }

    @Test
    fun e_weightedProgress_emptyListDefaultsToZeroProgress() {
        assertEquals(0f, BossProgressLogic.weightedProgress(emptyList()), 0.001f)
    }

    @Test
    fun e_bossCurrentValue_scalesToTarget() {
        assertEquals(50f, BossProgressLogic.bossCurrentValue(0.5f, 100f), 0.001f)
    }

    @Test
    fun e_bossCurrentValue_neverExceedsTarget() {
        assertEquals(100f, BossProgressLogic.bossCurrentValue(1.5f, 100f), 0.001f)
    }

    @Test
    fun p_isCleared_trueWhenValueMeetsTarget() {
        assertTrue(BossProgressLogic.isCleared(100f, 100f))
    }

    @Test
    fun n_isCleared_falseWhenBelowTarget() {
        assertFalse(BossProgressLogic.isCleared(99.9f, 100f))
    }
}
