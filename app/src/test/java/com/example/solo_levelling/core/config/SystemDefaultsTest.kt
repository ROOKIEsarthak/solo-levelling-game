package com.example.solo_levelling.core.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemDefaultsTest {
    @Test
    fun p_defaultRequiredMealsPerDay_isThree() {
        assertEquals(3, SystemDefaults.DEFAULT_REQUIRED_MEALS_PER_DAY)
    }

    @Test
    fun p_xpForNextLevel_usesNonlinearCurve() {
        assertEquals(100, SystemDefaults.xpForNextLevel(1))
        val level10 = SystemDefaults.xpForNextLevel(10)
        assertTrue(level10 > SystemDefaults.xpForNextLevel(5))
    }

    @Test
    fun p_levelFromTotalXp_returns1AtZeroXp() {
        assertEquals(1, SystemDefaults.levelFromTotalXp(0))
    }

    @Test
    fun p_levelFromTotalXp_advancesAfterEnoughXp() {
        val need = SystemDefaults.xpForNextLevel(1)
        assertEquals(2, SystemDefaults.levelFromTotalXp(need))
    }

    @Test
    fun e_levelFromTotalXp_staysWhenOneXpShort() {
        val need = SystemDefaults.xpForNextLevel(1)
        assertEquals(1, SystemDefaults.levelFromTotalXp(need - 1))
    }

    @Test
    fun p_rankForLevel_mapsThresholds() {
        assertEquals("E", SystemDefaults.rankForLevel(1))
        assertEquals("D", SystemDefaults.rankForLevel(6))
        assertEquals("C", SystemDefaults.rankForLevel(11))
        assertEquals("S", SystemDefaults.rankForLevel(51))
        assertEquals("MONARCH", SystemDefaults.rankForLevel(100))
    }

    @Test
    fun e_rankForLevel_belowFirstThresholdStaysE() {
        assertEquals("E", SystemDefaults.rankForLevel(0))
    }

    @Test
    fun e_rankForLevel_boundaryJustBelowNextRank() {
        assertEquals("E", SystemDefaults.rankForLevel(5))
        assertEquals("D", SystemDefaults.rankForLevel(6))
        assertEquals("D", SystemDefaults.rankForLevel(10))
        assertEquals("C", SystemDefaults.rankForLevel(11))
    }

    @Test
    fun p_totalXpForLevel_isInverseOfProgression() {
        assertEquals(0, SystemDefaults.totalXpForLevel(1))
        val total = SystemDefaults.totalXpForLevel(3)
        assertEquals(SystemDefaults.xpForNextLevel(1) + SystemDefaults.xpForNextLevel(2), total)
    }

    @Test
    fun e_streakGraceDays_defaultsToZero() {
        assertEquals(0, SystemDefaults.STREAK_GRACE_DAYS)
    }

    @Test
    fun p_dailyXpCap_isPositive() {
        assertEquals(500, SystemDefaults.DAILY_XP_CAP)
    }

    @Test
    fun p_undoPenaltyXp_zeroPercentIsZero() {
        assertEquals(0, SystemDefaults.UNDO_XP_PENALTY_PERCENT)
        assertEquals(1, SystemDefaults.UNDO_XP_PENALTY_MIN)
        assertEquals(0, SystemDefaults.undoPenaltyXp(40, 0))
        assertEquals(0, SystemDefaults.undoPenaltyXp(40))
    }

    @Test
    fun p_undoPenaltyXp_tenPercentRoundsDownWithMinOne() {
        assertEquals(4, SystemDefaults.undoPenaltyXp(40, 10))
        assertEquals(1, SystemDefaults.undoPenaltyXp(1, 10))
    }

    @Test
    fun n_undoPenaltyXp_skipsNonPositiveInputs() {
        assertEquals(0, SystemDefaults.undoPenaltyXp(0, 10))
        assertEquals(0, SystemDefaults.undoPenaltyXp(40, -5))
        assertEquals(0, SystemDefaults.undoPenaltyXp(-10, 10))
    }
}
