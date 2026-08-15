package com.example.solo_levelling.domain.logic

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DayBoundaryServicePureTest {
    private val today = LocalDate.of(2026, 8, 15)

    @Test
    fun p_shouldResetStreak_whenGapGreaterThanGracePlusOne() {
        val last = today.minusDays(2)
        assertTrue(DayBoundaryLogic.shouldResetStreak(last, today, graceDays = 0))
    }

    @Test
    fun n_shouldResetStreak_falseForNextDayWithGraceZero() {
        val last = today.minusDays(1)
        assertFalse(DayBoundaryLogic.shouldResetStreak(last, today, graceDays = 0))
    }

    @Test
    fun n_shouldResetStreak_falseForSameDay() {
        assertFalse(DayBoundaryLogic.shouldResetStreak(today, today, graceDays = 0))
    }

    @Test
    fun e_shouldResetStreak_respectsGraceDays() {
        val last = today.minusDays(2)
        assertFalse(DayBoundaryLogic.shouldResetStreak(last, today, graceDays = 1))
        assertTrue(DayBoundaryLogic.shouldResetStreak(today.minusDays(3), today, graceDays = 1))
    }

    @Test
    fun n_shouldResetStreak_falseWhenNeverCompleted() {
        assertFalse(DayBoundaryLogic.shouldResetStreak(null, today, graceDays = 0))
    }

    @Test
    fun e_shouldResetStreak_falseWhenGapExactlyGracePlusOne() {
        assertFalse(DayBoundaryLogic.shouldResetStreak(today.minusDays(2), today, graceDays = 1))
    }
}
