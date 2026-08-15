package com.example.solo_levelling.domain.service

import com.example.solo_levelling.domain.logic.DayBoundaryLogic
import com.example.solo_levelling.domain.logic.StreakLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * AT-49 offline full-day flow using pure orchestration (no Room).
 * Validates verification math, daily XP sum, streak, and day-boundary rules.
 */
class FullDayScenarioPureTest {
    private val day1 = LocalDate.of(2026, 8, 14)
    private val day2 = LocalDate.of(2026, 8, 15)

    private object VerificationRules {
        fun countSatisfied(actual: Int, target: Float): Boolean = actual >= target

        fun timerSatisfied(totalMinutes: Int, target: Float): Boolean = totalMinutes >= target

        fun metricSatisfied(sum: Float, target: Float): Boolean = sum >= target
    }

    private data class DayQuest(
        val title: String,
        val baseXp: Int,
        val verified: Boolean,
    )

    private fun sumDayXp(quests: List<DayQuest>): Int =
        quests.filter { it.verified }.sumOf { it.baseXp }

    @Test
    fun p_at49_streakAdvancesOnConsecutiveCompletionDays() {
        var streak = 0
        var last: LocalDate? = null

        streak = StreakLogic.computeNewStreak(streak, last, day1)
        last = day1
        assertEquals(1, streak)

        streak = StreakLogic.computeNewStreak(streak, last, day2)
        assertEquals(2, streak)
        assertFalse(StreakLogic.isStreakBroken(last, day2))
    }

    @Test
    fun e_at49_missedDayResetsStreakAtBoundary() {
        val lastCompleted = day1.minusDays(2)
        assertTrue(DayBoundaryLogic.shouldResetStreak(lastCompleted, day1, graceDays = 0))
        assertEquals(1, StreakLogic.computeNewStreak(5, lastCompleted, day1))
    }

    @Test
    fun n_at49_sameDayDoubleCompleteDoesNotDoubleStreak() {
        var streak = 3
        val last = day1
        streak = StreakLogic.computeNewStreak(streak, last, day1)
        assertEquals(3, streak)
    }

    @Test
    fun p_at49_dsaCountVerification() {
        val target = 2f
        assertFalse(VerificationRules.countSatisfied(1, target))
        assertTrue(VerificationRules.countSatisfied(2, target))
        assertTrue(VerificationRules.countSatisfied(3, target))
    }

    @Test
    fun p_at49_timerFocusVerification() {
        val target = 90f
        assertFalse(VerificationRules.timerSatisfied(60, target))
        assertTrue(VerificationRules.timerSatisfied(90, target))
        assertTrue(VerificationRules.timerSatisfied(60 + 30, target))
    }

    @Test
    fun p_at49_metricStepsVerification() {
        val target = 10_000f
        assertFalse(VerificationRules.metricSatisfied(5_000f, target))
        assertTrue(VerificationRules.metricSatisfied(10_000f, target))
        assertTrue(VerificationRules.metricSatisfied(5_000f + 6_000f, target))
    }

    @Test
    fun n_at49_underTargetQuestsExcludedFromXpSum() {
        val quests = listOf(
            DayQuest("DSA daily", baseXp = 40, verified = VerificationRules.countSatisfied(1, 2f)),
            DayQuest("Deep work", baseXp = 45, verified = VerificationRules.timerSatisfied(60, 90f)),
            DayQuest("Steps", baseXp = 25, verified = VerificationRules.metricSatisfied(8_000f, 10_000f)),
        )
        assertEquals(0, sumDayXp(quests))
    }

    @Test
    fun p_at49_fullDayXpSumWhenAllVerified() {
        val quests = listOf(
            DayQuest("DSA daily", baseXp = 40, verified = VerificationRules.countSatisfied(2, 2f)),
            DayQuest("Deep work", baseXp = 45, verified = VerificationRules.timerSatisfied(90, 90f)),
            DayQuest("Steps", baseXp = 25, verified = VerificationRules.metricSatisfied(10_000f, 10_000f)),
            DayQuest("Journal", baseXp = 15, verified = true),
        )
        assertEquals(125, sumDayXp(quests))
    }

    @Test
    fun e_at49_partialDayXpSumOnlyCountsVerified() {
        val quests = listOf(
            DayQuest("DSA daily", baseXp = 40, verified = VerificationRules.countSatisfied(2, 2f)),
            DayQuest("Deep work", baseXp = 45, verified = VerificationRules.timerSatisfied(60, 90f)),
            DayQuest("Steps", baseXp = 25, verified = VerificationRules.metricSatisfied(10_000f, 10_000f)),
        )
        assertEquals(65, sumDayXp(quests))
    }
}
