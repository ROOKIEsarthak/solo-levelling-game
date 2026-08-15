package com.example.solo_levelling.domain.service

import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsServicePureTest {
    @Test
    fun p_personalScore_maxWhenAllInputsStrong() {
        val score = AnalyticsService.personalScore(
            questCompletionPct = 1f,
            streak = 7,
            workoutDays = 7,
            dsaSolvedWeek = 10,
        )
        assertEquals(100, score)
    }

    @Test
    fun n_personalScore_zeroWhenNoActivity() {
        assertEquals(0, AnalyticsService.personalScore(0f, 0, 0, 0))
    }

    @Test
    fun e_personalScore_streakCappedAtSevenDays() {
        val atSeven = AnalyticsService.personalScore(0f, 7, 0, 0)
        val atTen = AnalyticsService.personalScore(0f, 10, 0, 0)
        assertEquals(atSeven, atTen)
        assertEquals(20, atSeven)
    }

    @Test
    fun e_personalScore_dsaSolvedCappedAtTen() {
        val atTen = AnalyticsService.personalScore(0f, 0, 0, 10)
        val atFifteen = AnalyticsService.personalScore(0f, 0, 0, 15)
        assertEquals(atTen, atFifteen)
        assertEquals(20, atTen)
    }

    @Test
    fun p_personalScore_partialQuestCompletion() {
        assertEquals(20, AnalyticsService.personalScore(0.5f, 0, 0, 0))
    }

    @Test
    fun p_completionRate_perfectWeek() {
        assertEquals(1f, AnalyticsService.completionRate(7, 7), 0.001f)
    }

    @Test
    fun p_completionRate_halfComplete() {
        assertEquals(0.5f, AnalyticsService.completionRate(3, 6), 0.001f)
    }

    @Test
    fun n_completionRate_zeroWhenNoQuests() {
        assertEquals(0f, AnalyticsService.completionRate(0, 0), 0.001f)
    }

    @Test
    fun e_completionRate_zeroTotalIgnoresCompletedCount() {
        assertEquals(0f, AnalyticsService.completionRate(5, 0), 0.001f)
    }
}
