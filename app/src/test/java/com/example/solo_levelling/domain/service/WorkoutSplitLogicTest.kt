package com.example.solo_levelling.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutSplitLogicTest {
    @Test
    fun p_buildRoutine_upperLower_fourDays() {
        val result = WorkoutSplitLogic.buildRoutineFromScheduleCsv("upper_lower", "1,3,5,7")
        assertNull(result.error)
        assertNotNull(result.routine)
        assertEquals("Upper", result.routine!!.monday.name)
        assertEquals("Rest", result.routine.tuesday.name)
        assertEquals("Lower", result.routine.wednesday.name)
        assertEquals(false, result.routine.tuesday.enabled)
    }

    @Test
    fun p_buildRoutine_pplUl_monFri_resolvesLibraryNames() {
        val result = WorkoutSplitLogic.buildRoutineFromScheduleCsv("ppl_ul", "1,2,3,4,5")
        assertNull(result.error)
        val monday = result.routine!!.monday
        assertEquals("Push", monday.name)
        assertTrue(monday.enabled)
        assertEquals("Barbell Bench Press", monday.exercises.first().name)
        assertEquals(3, monday.exercises.first().sets)
        assertEquals(6, monday.exercises.first().repRange.min)
        assertEquals(10, monday.exercises.first().repRange.max)
        assertEquals("Pull", result.routine.tuesday.name)
        assertEquals("Rest", result.routine.sunday.name)
        assertEquals(false, result.routine.sunday.enabled)
    }

    @Test
    fun p_buildRoutine_customWeekdayAssignment() {
        // Push→Wed, Pull→Mon, Legs→Fri, Upper→Tue, Lower→Thu
        val map = mapOf(1 to 3, 2 to 1, 3 to 5, 4 to 2, 5 to 4)
        val result = WorkoutSplitLogic.buildRoutine("ppl_ul", map)
        assertNull(result.error)
        assertEquals("Pull", result.routine!!.monday.name)
        assertEquals("Upper", result.routine.tuesday.name)
        assertEquals("Push", result.routine.wednesday.name)
        assertEquals("Lower", result.routine.thursday.name)
        assertEquals("Legs", result.routine.friday.name)
        assertEquals("Rest", result.routine.saturday.name)
        assertEquals(listOf(1, 2, 3, 4, 5), result.trainingIsoDays)
    }

    @Test
    fun n_buildRoutine_duplicateWeekdayRejected() {
        val result = WorkoutSplitLogic.buildRoutine(
            "full_body_3",
            mapOf(1 to 1, 2 to 1, 3 to 3),
        )
        assertEquals("Each weekday can only have one workout", result.error)
    }

    @Test
    fun p_encodeParseDayMap_roundTrip() {
        val map = mapOf(1 to 2, 2 to 4, 3 to 6)
        assertEquals(map, WorkoutSplitLogic.parseDayMap(WorkoutSplitLogic.encodeDayMap(map)))
    }

    @Test
    fun p_parseRepRange() {
        val range = WorkoutSplitLogic.parseRepRange("8-12")
        assertEquals(8, range.min)
        assertEquals(12, range.max)
    }

    @Test
    fun n_buildRoutine_unknownSplit() {
        val result = WorkoutSplitLogic.buildRoutine("missing_split", mapOf(1 to 1, 2 to 2, 3 to 3))
        assertEquals("Unknown workout split", result.error)
        assertNull(result.routine)
    }

    @Test
    fun n_buildRoutine_dayCountMismatch() {
        val result = WorkoutSplitLogic.buildRoutineFromScheduleCsv("full_body_3", "1,2,3,4,5")
        assertTrue(result.error!!.contains("Pick 3 training days"))
        assertNull(result.routine)
    }

    @Test
    fun e_parseScheduleDaysCsv_dedupesAndSorts() {
        assertEquals(listOf(1, 3, 5), WorkoutSplitLogic.parseScheduleDaysCsv("5,1,3,1,99"))
    }

    @Test
    fun e_buildRoutine_emptySchedule() {
        val result = WorkoutSplitLogic.buildRoutineFromScheduleCsv("full_body_3", "")
        assertEquals("Select training days on your schedule", result.error)
    }
}
