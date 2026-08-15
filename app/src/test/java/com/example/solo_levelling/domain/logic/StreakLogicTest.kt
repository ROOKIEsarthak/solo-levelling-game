package com.example.solo_levelling.domain.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StreakLogicTest {
    private val today = LocalDate.of(2026, 8, 15)

    @Test
    fun p_consecutiveDayIncrements() {
        val last = today.minusDays(1)
        assertEquals(6, StreakLogic.computeNewStreak(5, last, today))
    }

    @Test
    fun n_sameDayNoDoubleIncrement() {
        assertEquals(5, StreakLogic.computeNewStreak(5, today, today))
    }

    @Test
    fun e_gapResetsToOne() {
        val last = today.minusDays(3)
        assertEquals(1, StreakLogic.computeNewStreak(10, last, today))
    }

    @Test
    fun p_firstCompletionStartsAtOne() {
        assertEquals(1, StreakLogic.computeNewStreak(0, null, today))
    }

    @Test
    fun e_isStreakBroken_whenGapGreaterThanOneDay() {
        assertTrue(StreakLogic.isStreakBroken(today.minusDays(2), today))
    }

    @Test
    fun n_isStreakBroken_falseForConsecutiveDays() {
        assertFalse(StreakLogic.isStreakBroken(today.minusDays(1), today))
        assertFalse(StreakLogic.isStreakBroken(today, today))
    }

    @Test
    fun n_isStreakBroken_falseWhenNeverCompleted() {
        assertFalse(StreakLogic.isStreakBroken(null, today))
    }

    @Test
    fun e_gapDays_countsCalendarDaysBetween() {
        assertEquals(0, StreakLogic.gapDays(today, today))
        assertEquals(1, StreakLogic.gapDays(today.minusDays(1), today))
        assertEquals(3, StreakLogic.gapDays(today.minusDays(3), today))
    }
}
