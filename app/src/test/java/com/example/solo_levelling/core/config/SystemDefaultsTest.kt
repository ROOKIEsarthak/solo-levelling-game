package com.example.solo_levelling.core.config

import org.junit.Assert.assertEquals
import org.junit.Test

class SystemDefaultsTest {
    @Test
    fun p_xpForNextLevel_usesNonlinearCurve() {
        assertEquals(100, SystemDefaults.xpForNextLevel(1))
        val level10 = SystemDefaults.xpForNextLevel(10)
        assertEquals(true, level10 > SystemDefaults.xpForNextLevel(5))
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
    fun p_totalXpForLevel_isInverseOfProgression() {
        assertEquals(0, SystemDefaults.totalXpForLevel(1))
        val total = SystemDefaults.totalXpForLevel(3)
        assertEquals(SystemDefaults.xpForNextLevel(1) + SystemDefaults.xpForNextLevel(2), total)
    }
}
