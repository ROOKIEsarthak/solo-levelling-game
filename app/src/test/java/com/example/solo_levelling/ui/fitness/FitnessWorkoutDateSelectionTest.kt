package com.example.solo_levelling.ui.fitness

import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun n_unlockedSplit_defaultsToRoutineTab() {
        assertEquals("Routine", defaultTrainingTab(""))
        assertEquals("Routine", resolvedTrainingTab(null, ""))
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
}
