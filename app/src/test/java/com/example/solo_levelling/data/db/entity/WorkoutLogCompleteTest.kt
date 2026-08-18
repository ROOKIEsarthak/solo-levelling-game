package com.example.solo_levelling.data.db.entity

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutLogCompleteTest {
    @Test
    fun n_emptyLog_notComplete() {
        assertFalse(
            WorkoutLogEntity(date = "2026-08-17").isTrainingDayComplete(),
        )
    }

    @Test
    fun n_exercisesWithoutSets_notComplete() {
        assertFalse(
            WorkoutLogEntity(
                date = "2026-08-17",
                exercises = listOf(LoggedExerciseEntity(name = "Squat")),
            ).isTrainingDayComplete(),
        )
    }

    @Test
    fun p_hasSets_complete() {
        assertTrue(
            WorkoutLogEntity(
                date = "2026-08-17",
                exercises = listOf(
                    LoggedExerciseEntity(
                        name = "Squat",
                        sets = listOf(LoggedSetEntity(100f, 5)),
                    ),
                ),
            ).isTrainingDayComplete(),
        )
    }

    @Test
    fun p_activeRest_complete() {
        assertTrue(
            WorkoutLogEntity(
                date = "2026-08-17",
                restKind = WorkoutRestKind.ACTIVE_REST,
            ).isTrainingDayComplete(),
        )
    }

    @Test
    fun p_completeRest_complete() {
        assertTrue(
            WorkoutLogEntity(
                date = "2026-08-17",
                restKind = WorkoutRestKind.COMPLETE_REST,
            ).isTrainingDayComplete(),
        )
    }
}
