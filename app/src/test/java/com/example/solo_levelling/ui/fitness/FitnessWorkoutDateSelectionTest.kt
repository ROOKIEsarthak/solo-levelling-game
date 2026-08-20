package com.example.solo_levelling.ui.fitness

import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure selection semantics for Training date defaults (ViewModel stores null = today).
 */
class FitnessWorkoutDateSelectionTest {
    @Test
    fun p_nullSelection_resolvesToToday() {
        val today = "2026-08-17"
        val selected: String? = null
        assertEquals(today, selected ?: today)
    }

    @Test
    fun p_resetClearsManualSelection() {
        val selected = MutableStateFlow<String?>("2026-01-01")
        selected.value = null
        assertNull(selected.value)
    }

    @Test
    fun e_differentClockDates_mapToCorrectIso() {
        val monday = java.time.LocalDate.of(2026, 8, 17)
        val wednesday = java.time.LocalDate.of(2026, 8, 19)
        assertEquals("monday", monday.dayOfWeek.name.lowercase())
        assertEquals("wednesday", wednesday.dayOfWeek.name.lowercase())
        assertEquals("2026-08-17", monday.toString())
        assertEquals("2026-08-19", wednesday.toString())
    }

    @Test
    fun p_lockedSplit_defaultsToTodayTab() {
        assertEquals("Today", defaultTrainingTab("ppl_ul"))
        assertEquals("Today", resolvedTrainingTab(null, "ppl_ul"))
    }

    @Test
    fun p_unlockedSplit_defaultsToTodayTab() {
        assertEquals("Today", defaultTrainingTab(""))
        assertEquals("Today", resolvedTrainingTab(null, ""))
    }

    @Test
    fun r_customRoutine_doesNotOpenRoutineOnEntry() {
        assertEquals("Today", defaultTrainingTab(""))
        assertEquals("Today", resolvedTrainingTab(null, ""))
        assertEquals("Today", defaultTrainingTab(null))
    }

    @Test
    fun r_loadingSplitId_doesNotOpenRoutine() {
        assertEquals("Today", defaultTrainingTab(null))
        assertEquals("Today", resolvedTrainingTab(null, null))
    }

    @Test
    fun e_userChangeSplit_staysWhileOnScreen() {
        assertEquals("Change Split", resolvedTrainingTab("Change Split", "ppl_ul"))
        assertEquals("History", resolvedTrainingTab("History", "ppl_ul"))
    }

    @Test
    fun p_applySplit_forcesTodayTab() {
        assertEquals("Today", resolvedTrainingTab("Today", "ppl_ul"))
        assertEquals("Today", resolvedTrainingTab("Today", ""))
    }

    @Test
    fun p_canGoNext_fromPastTowardToday() {
        val today = java.time.LocalDate.of(2026, 8, 18)
        assertTrue(
            com.example.solo_levelling.domain.logic.ActivityDatePolicy.canGoToNextDay(
                today,
                today.minusDays(1),
            ),
        )
    }

    @Test
    fun n_canGoNext_disabledOnToday() {
        val today = java.time.LocalDate.of(2026, 8, 18)
        assertFalse(
            com.example.solo_levelling.domain.logic.ActivityDatePolicy.canGoToNextDay(today, today),
        )
    }

    @Test
    fun n_futureDate_notWritable() {
        val today = java.time.LocalDate.of(2026, 8, 18)
        assertFalse(
            com.example.solo_levelling.domain.logic.ActivityDatePolicy.canWriteRecord(
                today,
                today.plusDays(1),
            ),
        )
    }

    @Test
    fun p_pastDate_selectableForReviewButNotWritable() {
        val today = java.time.LocalDate.of(2026, 8, 18)
        val past = today.minusDays(1)
        assertTrue(
            com.example.solo_levelling.domain.logic.ActivityDatePolicy.relation(today, past) !=
                com.example.solo_levelling.domain.logic.DayRelation.Future,
        )
        assertFalse(
            com.example.solo_levelling.domain.logic.ActivityDatePolicy.canWriteRecord(today, past),
        )
    }
}
