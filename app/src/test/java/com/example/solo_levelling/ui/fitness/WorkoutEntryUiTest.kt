package com.example.solo_levelling.ui.fitness

import com.example.solo_levelling.data.db.entity.LoggedSetEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutEntryUiTest {

    @Test
    fun p_addModeUsesLogTitleAndSaveSets() {
        assertEquals("LOG SETS", workoutEntryDialogTitle(hasSavedSets = false))
        assertEquals("SAVE SETS", workoutEntryConfirmLabel(hasSavedSets = false))
    }

    @Test
    fun p_editModeUsesEditTitleAndSaveChanges() {
        assertEquals("EDIT SETS", workoutEntryDialogTitle(hasSavedSets = true))
        assertEquals("SAVE CHANGES", workoutEntryConfirmLabel(hasSavedSets = true))
    }

    @Test
    fun p_validateSetDraft_acceptsWeightRepsAndOptionalRpe() {
        val err = validateSetDraft(
            listOf(WorkoutSetDraft(weight = "60", reps = "10", rpe = "7.5")),
        )
        assertNull(err)
    }

    @Test
    fun p_loggedSetFromForm_mapsWeightRepsAndRpe() {
        val set = loggedSetFromForm(weight = " 60.5 ", reps = "8", rpe = " 7 ")
        assertEquals(60.5f, set.weight)
        assertEquals(8, set.reps)
        assertEquals(7f, set.rpe)
    }

    @Test
    fun p_loggedSetsFromDraft_mapsAllRows() {
        val sets = loggedSetsFromDraft(
            listOf(
                WorkoutSetDraft(weight = "60", reps = "10", rpe = ""),
                WorkoutSetDraft(weight = "55", reps = "8", rpe = "8"),
            ),
        )
        assertEquals(2, sets.size)
        assertEquals(60f, sets[0].weight)
        assertEquals(10, sets[0].reps)
        assertNull(sets[0].rpe)
        assertEquals(55f, sets[1].weight)
        assertEquals(8, sets[1].reps)
        assertEquals(8f, sets[1].rpe)
    }

    @Test
    fun p_nextSetDraft_copiesLastRow() {
        val next = nextSetDraft(
            listOf(WorkoutSetDraft(weight = "60", reps = "10", rpe = "7")),
        )
        assertEquals("60", next.weight)
        assertEquals("10", next.reps)
        assertEquals("7", next.rpe)
    }

    @Test
    fun e_nextSetDraft_emptyListReturnsBlankRow() {
        val next = nextSetDraft(emptyList())
        assertEquals("", next.weight)
        assertEquals("", next.reps)
        assertEquals("", next.rpe)
    }

    @Test
    fun p_editActionLabel_includesExerciseName() {
        assertEquals("Edit Bench Press", workoutEditActionLabel("Bench Press"))
    }

    @Test
    fun n_rejectsBlankWeight() {
        val err = validateSetDraft(listOf(WorkoutSetDraft(weight = "  ", reps = "10")))
        assertEquals("Enter a valid weight.", err?.message)
        assertEquals(WorkoutSetField.Weight, err?.field)
        assertEquals(0, err?.index)
    }

    @Test
    fun n_rejectsInvalidWeight() {
        val err = validateSetDraft(listOf(WorkoutSetDraft(weight = "abc", reps = "10")))
        assertEquals("Enter a valid weight.", err?.message)
        assertEquals(WorkoutSetField.Weight, err?.field)
    }

    @Test
    fun n_rejectsZeroAndNegativeWeight() {
        val zero = validateSetDraft(listOf(WorkoutSetDraft(weight = "0", reps = "10")))
        assertEquals("Enter a valid weight.", zero?.message)
        val negative = validateSetDraft(listOf(WorkoutSetDraft(weight = "-5", reps = "10")))
        assertEquals("Enter a valid weight.", negative?.message)
    }

    @Test
    fun n_rejectsBlankAndInvalidReps() {
        val blank = validateSetDraft(listOf(WorkoutSetDraft(weight = "60", reps = "")))
        assertEquals("Enter the number of repetitions.", blank?.message)
        assertEquals(WorkoutSetField.Reps, blank?.field)
        val invalid = validateSetDraft(listOf(WorkoutSetDraft(weight = "60", reps = "x")))
        assertEquals("Enter the number of repetitions.", invalid?.message)
    }

    @Test
    fun n_rejectsZeroRepsOnSecondRow() {
        val err = validateSetDraft(
            listOf(
                WorkoutSetDraft(weight = "60", reps = "10"),
                WorkoutSetDraft(weight = "60", reps = "0"),
            ),
        )
        assertEquals("Enter the number of repetitions.", err?.message)
        assertEquals(1, err?.index)
        assertEquals(WorkoutSetField.Reps, err?.field)
    }

    @Test
    fun n_rejectsInvalidRpeWhenProvided() {
        val err = validateSetDraft(listOf(WorkoutSetDraft(weight = "60", reps = "10", rpe = "abc")))
        assertEquals("Enter a valid RPE.", err?.message)
        assertEquals(WorkoutSetField.Rpe, err?.field)
    }

    @Test
    fun e_emptyDraftIsAllowed() {
        assertNull(validateSetDraft(emptyList()))
    }

    @Test
    fun e_blankRpeIsOptional() {
        assertNull(validateSetDraft(listOf(WorkoutSetDraft(weight = "60", reps = "8", rpe = ""))))
        val set = loggedSetFromForm("60", "8", "")
        assertNull(set.rpe)
    }

    @Test
    fun e_progressLabel_zeroOfPlannedAndLoggedOfPlanned() {
        assertEquals("0 / 4 sets", exerciseSetProgressLabel(0, 4))
        assertEquals("3 / 4 sets", exerciseSetProgressLabel(3, 4))
        assertEquals("2 sets", exerciseSetProgressLabel(2, null))
        assertEquals("2 sets", exerciseSetProgressLabel(2, 0))
    }

    @Test
    fun e_formatLoggedSetSummary_stripsWholeNumberDecimals() {
        assertEquals("60 kg × 10", formatLoggedSetSummary(LoggedSetEntity(60f, 10)))
        assertEquals("60.5 kg × 8 @ RPE 7.5", formatLoggedSetSummary(LoggedSetEntity(60.5f, 8, 7.5f)))
    }

    @Test
    fun e_initialSetDrafts_seedsPreviousWhenEmpty() {
        val drafts = initialSetDrafts(
            savedSets = emptyList(),
            previous = LoggedSetEntity(80f, 6),
        )
        assertEquals(1, drafts.size)
        assertEquals("80", drafts[0].weight)
        assertEquals("6", drafts[0].reps)
    }

    @Test
    fun e_removeSetDraft_dropsIndexedRow() {
        val remaining = removeSetDraft(
            listOf(
                WorkoutSetDraft(weight = "60", reps = "10"),
                WorkoutSetDraft(weight = "55", reps = "8"),
            ),
            index = 0,
        )
        assertEquals(1, remaining.size)
        assertEquals("55", remaining[0].weight)
    }

    @Test
    fun e_emptySetsCopy() {
        assertEquals("No sets logged yet.", WORKOUT_ENTRY_EMPTY_SETS)
        assertEquals("60 kg × 10  55 kg × 8", formatPreviousSetsSummary(
            listOf(LoggedSetEntity(60f, 10), LoggedSetEntity(55f, 8)),
        ))
    }

    @Test
    fun r_draftFromExercise_preservesExistingSetsForEdit() {
        val drafts = draftSetsFromExercise(
            listOf(LoggedSetEntity(60f, 10, 8f), LoggedSetEntity(55f, 8)),
        )
        assertEquals(2, drafts.size)
        assertEquals("60", drafts[0].weight)
        assertEquals("10", drafts[0].reps)
        assertEquals("8", drafts[0].rpe)
        assertEquals("55", drafts[1].weight)
        assertEquals("", drafts[1].rpe)
        val seeded = initialSetDrafts(
            savedSets = listOf(LoggedSetEntity(60f, 10)),
            previous = LoggedSetEntity(80f, 6),
        )
        assertEquals("60", seeded.single().weight)
        assertEquals("10", seeded.single().reps)
    }
}
