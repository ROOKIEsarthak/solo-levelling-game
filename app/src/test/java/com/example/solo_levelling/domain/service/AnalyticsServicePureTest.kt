package com.example.solo_levelling.domain.service

import com.example.solo_levelling.domain.model.AttributeCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun p_personalScore_careerOnlyIgnoresMissingWorkouts() {
        val score = AnalyticsService.personalScore(
            questCompletionPct = 1f,
            streak = 7,
            workoutDays = 0,
            dsaSolvedWeek = 10,
            modules = EnabledModules(career = true, workout = false, diet = false),
        )
        assertEquals(100, score)
    }

    @Test
    fun p_personalScore_workoutOnlyIgnoresMissingDsa() {
        val score = AnalyticsService.personalScore(
            questCompletionPct = 1f,
            streak = 7,
            workoutDays = 7,
            dsaSolvedWeek = 0,
            modules = EnabledModules(career = false, workout = true, diet = false),
        )
        assertEquals(100, score)
    }

    @Test
    fun n_personalScore_disabledCareerDoesNotCountZeroDsa() {
        val withDisabledCareer = AnalyticsService.personalScore(
            questCompletionPct = 1f,
            streak = 7,
            workoutDays = 7,
            dsaSolvedWeek = 0,
            modules = EnabledModules(career = false, workout = true, diet = false),
        )
        val withEnabledCareerZeroDsa = AnalyticsService.personalScore(
            questCompletionPct = 1f,
            streak = 7,
            workoutDays = 7,
            dsaSolvedWeek = 0,
            modules = EnabledModules(career = true, workout = true, diet = false),
        )
        assertEquals(100, withDisabledCareer)
        assertTrue(withEnabledCareerZeroDsa < withDisabledCareer)
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

    @Test
    fun p_improvementPercent_positiveDelta() {
        val pct = AnalyticsService.improvementPercent(74, 68, previousActiveDays = 5)
        assertEquals(8.8f, pct!!, 0.05f)
    }

    @Test
    fun n_improvementPercent_nullWhenNoBaseline() {
        assertNull(AnalyticsService.improvementPercent(50, 0, previousActiveDays = 5))
        assertNull(AnalyticsService.improvementPercent(50, 40, previousActiveDays = 2))
    }

    @Test
    fun e_improvementPercent_negativeDelta() {
        val pct = AnalyticsService.improvementPercent(60, 80, previousActiveDays = 4)
        assertEquals(-25.0f, pct!!, 0.05f)
    }

    @Test
    fun p_estimateActiveDays_usesWorkoutOrQuestProxy() {
        val start = java.time.LocalDate.of(2026, 1, 1)
        val end = start.plusDays(6)
        assertEquals(5, AnalyticsService.estimateActiveDays(5, 0, 0, start, end))
        assertEquals(3, AnalyticsService.estimateActiveDays(1, 3, 7, start, end))
    }

    @Test
    fun p_isAttributeActionable_careerGatesInt() {
        val careerOnly = EnabledModules(career = true, workout = false, diet = false)
        assertTrue(AnalyticsService.isAttributeActionable(AttributeCode.INT.name, careerOnly))
        assertFalse(AnalyticsService.isAttributeActionable(AttributeCode.STR.name, careerOnly))
        assertTrue(AnalyticsService.isAttributeActionable(AttributeCode.DISC.name, careerOnly))
    }

    @Test
    fun n_isAttributeActionable_workoutGatesStrength() {
        val dietOnly = EnabledModules(career = false, workout = false, diet = true)
        assertFalse(AnalyticsService.isAttributeActionable(AttributeCode.STR.name, dietOnly))
        assertFalse(AnalyticsService.isAttributeActionable(AttributeCode.INT.name, dietOnly))
        assertTrue(AnalyticsService.isAttributeActionable(AttributeCode.VIT.name, dietOnly))
    }

    @Test
    fun e_isAttributeActionable_vitWhenWorkoutWithoutDiet() {
        val workoutOnly = EnabledModules(career = false, workout = true, diet = false)
        assertTrue(AnalyticsService.isAttributeActionable(AttributeCode.VIT.name, workoutOnly))
    }

    @Test
    fun r_attributeSnapshot_ignoresDisabledModuleStrength() {
        val attrs = listOf(
            AttributeCode.STR.name to 1,
            AttributeCode.DISC.name to 50,
            AttributeCode.FOC.name to 40,
        )
        val snap = AnalyticsService.attributeSnapshot(
            attrs,
            EnabledModules(career = false, workout = false, diet = true),
        )
        assertEquals(AttributeCode.FOC.name, snap.bottomCode)
        assertEquals(AttributeCode.DISC.name, snap.topCode)
    }

    @Test
    fun p_attributeSnapshot_emptyWhenNoRelevantAttrs() {
        val snap = AnalyticsService.attributeSnapshot(
            emptyList(),
            EnabledModules(career = true, workout = true, diet = true),
        )
        assertNull(snap.bottomCode)
        assertNull(snap.topCode)
    }
}
