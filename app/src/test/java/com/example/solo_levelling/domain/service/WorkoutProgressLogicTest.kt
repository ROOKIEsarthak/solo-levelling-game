package com.example.solo_levelling.domain.service

import com.example.solo_levelling.data.db.entity.LoggedExerciseEntity
import com.example.solo_levelling.data.db.entity.LoggedSetEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutProgressLogicTest {
    @Test
    fun p_sessionVolume_sumsWeightTimesReps() {
        val exercises = listOf(
            LoggedExerciseEntity(
                name = "Bench",
                sets = listOf(
                    LoggedSetEntity(60f, 10),
                    LoggedSetEntity(65f, 8),
                ),
            ),
            LoggedExerciseEntity(
                name = "Row",
                sets = listOf(LoggedSetEntity(40f, 12)),
            ),
        )
        assertEquals(1600.0, WorkoutProgressLogic.sessionVolume(exercises), 0.01)
    }

    @Test
    fun n_sessionVolume_emptyExercisesReturnsZero() {
        assertEquals(0.0, WorkoutProgressLogic.sessionVolume(emptyList()), 0.01)
    }

    @Test
    fun e_sessionVolume_emptySetsIgnored() {
        val exercises = listOf(
            LoggedExerciseEntity(name = "Squat", sets = emptyList()),
        )
        assertEquals(0.0, WorkoutProgressLogic.sessionVolume(exercises), 0.01)
    }

    @Test
    fun p_isPr_trueWhenCurrentHigher() {
        assertTrue(WorkoutProgressLogic.isPr(100f, 95f))
    }

    @Test
    fun n_isPr_falseWhenNotImproved() {
        assertFalse(WorkoutProgressLogic.isPr(90f, 95f))
        assertFalse(WorkoutProgressLogic.isPr(0f, 0f))
    }

    @Test
    fun p_compareSets_positiveDiff() {
        assertEquals("+5kg", WorkoutProgressLogic.compareSets(60f, 65f))
    }

    @Test
    fun n_compareSets_negativeDiff() {
        assertEquals("-3kg", WorkoutProgressLogic.compareSets(63f, 60f))
    }

    @Test
    fun e_compareSets_sameWeight() {
        assertEquals("same", WorkoutProgressLogic.compareSets(50f, 50f))
    }
}
