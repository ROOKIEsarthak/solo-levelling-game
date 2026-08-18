package com.example.solo_levelling.ui.onboarding

import com.example.solo_levelling.domain.service.EnabledModules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingStepsTest {
    @Test
    fun p_buildOnboardingSteps_allModules_includesFullFlow() {
        val steps = buildOnboardingSteps(EnabledModules(career = true, workout = true, diet = true))
        assertEquals(
            listOf(
                OnboardingStep.NAME,
                OnboardingStep.GOALS,
                OnboardingStep.CAREER_INTENT,
                OnboardingStep.CAREER_PROFILE,
                OnboardingStep.CAREER_ASSESS,
                OnboardingStep.WORKOUT_BODY,
                OnboardingStep.WORKOUT_PLAN,
                OnboardingStep.DIET_NUTRITION,
                OnboardingStep.SUMMARY,
            ),
            steps,
        )
    }

    @Test
    fun p_buildOnboardingSteps_careerOnly() {
        val steps = buildOnboardingSteps(EnabledModules(career = true))
        assertEquals(
            listOf(
                OnboardingStep.NAME,
                OnboardingStep.GOALS,
                OnboardingStep.CAREER_INTENT,
                OnboardingStep.CAREER_PROFILE,
                OnboardingStep.CAREER_ASSESS,
                OnboardingStep.SUMMARY,
            ),
            steps,
        )
    }

    @Test
    fun p_buildOnboardingSteps_workoutAndDiet_skipsCareer() {
        val steps = buildOnboardingSteps(EnabledModules(workout = true, diet = true))
        assertEquals(
            listOf(
                OnboardingStep.NAME,
                OnboardingStep.GOALS,
                OnboardingStep.WORKOUT_BODY,
                OnboardingStep.WORKOUT_PLAN,
                OnboardingStep.DIET_NUTRITION,
                OnboardingStep.SUMMARY,
            ),
            steps,
        )
    }

    @Test
    fun e_buildOnboardingSteps_noneSelected_stillHasNameGoalsSummary() {
        val steps = buildOnboardingSteps(EnabledModules())
        assertEquals(
            listOf(OnboardingStep.NAME, OnboardingStep.GOALS, OnboardingStep.SUMMARY),
            steps,
        )
    }

    @Test
    fun p_needsBodyFieldsInDietStep_dietOnly() {
        assertTrue(needsBodyFieldsInDietStep(EnabledModules(diet = true)))
    }

    @Test
    fun n_needsBodyFieldsInDietStep_workoutAndDiet() {
        assertFalse(needsBodyFieldsInDietStep(EnabledModules(workout = true, diet = true)))
    }

    @Test
    fun n_isOnboardingStepValid_goalsRequiresModule() {
        assertFalse(
            isOnboardingStepValid(
                step = OnboardingStep.GOALS,
                name = "Hunter",
                enabledModules = EnabledModules(),
                careerIntent = "",
                createOwnRoutine = false,
                workoutSplitId = "ppl_ul",
                splitDayMap = emptyMap(),
                preferredWorkoutDays = emptySet(),
                age = "25",
                sex = "male",
                heightCm = "170",
                weightKg = "70",
            ),
        )
    }

    @Test
    fun p_isOnboardingStepValid_careerIntentRequired() {
        assertFalse(
            isOnboardingStepValid(
                step = OnboardingStep.CAREER_INTENT,
                name = "Hunter",
                enabledModules = EnabledModules(career = true),
                careerIntent = "",
                createOwnRoutine = false,
                workoutSplitId = "ppl_ul",
                splitDayMap = emptyMap(),
                preferredWorkoutDays = emptySet(),
                age = "25",
                sex = "male",
                heightCm = "170",
                weightKg = "70",
            ),
        )
        assertTrue(
            isOnboardingStepValid(
                step = OnboardingStep.CAREER_INTENT,
                name = "Hunter",
                enabledModules = EnabledModules(career = true),
                careerIntent = "learning",
                createOwnRoutine = false,
                workoutSplitId = "ppl_ul",
                splitDayMap = emptyMap(),
                preferredWorkoutDays = emptySet(),
                age = "25",
                sex = "male",
                heightCm = "170",
                weightKg = "70",
            ),
        )
    }

    @Test
    fun n_isOnboardingStepValid_createOwnRequiresDays() {
        assertFalse(
            isOnboardingStepValid(
                step = OnboardingStep.WORKOUT_PLAN,
                name = "Hunter",
                enabledModules = EnabledModules(workout = true),
                careerIntent = "",
                createOwnRoutine = true,
                workoutSplitId = "ppl_ul",
                splitDayMap = emptyMap(),
                preferredWorkoutDays = emptySet(),
                age = "25",
                sex = "male",
                heightCm = "170",
                weightKg = "70",
            ),
        )
        assertTrue(
            isOnboardingStepValid(
                step = OnboardingStep.WORKOUT_PLAN,
                name = "Hunter",
                enabledModules = EnabledModules(workout = true),
                careerIntent = "",
                createOwnRoutine = true,
                workoutSplitId = "ppl_ul",
                splitDayMap = emptyMap(),
                preferredWorkoutDays = setOf("MON", "WED"),
                age = "25",
                sex = "male",
                heightCm = "170",
                weightKg = "70",
            ),
        )
    }

    @Test
    fun p_isBodyProfileValid_whenAllFieldsPresent() {
        assertTrue(isBodyProfileValid(age = "28", sex = "male", heightCm = "178", weightKg = "74"))
        assertTrue(isBodyProfileValid(age = "30", sex = "female", heightCm = "165", weightKg = "60"))
    }

    @Test
    fun n_isBodyProfileValid_rejectsEmptyOrUnselected() {
        assertFalse(isBodyProfileValid(age = "", sex = "male", heightCm = "178", weightKg = "74"))
        assertFalse(isBodyProfileValid(age = "28", sex = "", heightCm = "178", weightKg = "74"))
        assertFalse(isBodyProfileValid(age = "28", sex = "male", heightCm = "", weightKg = "74"))
        assertFalse(isBodyProfileValid(age = "28", sex = "male", heightCm = "178", weightKg = ""))
    }

    @Test
    fun n_isBodyProfileValid_rejectsInvalidGenderOrNonPositive() {
        assertFalse(isBodyProfileValid(age = "28", sex = "other", heightCm = "178", weightKg = "74"))
        assertFalse(isBodyProfileValid(age = "0", sex = "male", heightCm = "178", weightKg = "74"))
        assertFalse(isBodyProfileValid(age = "28", sex = "male", heightCm = "-1", weightKg = "74"))
        assertFalse(isBodyProfileValid(age = "28", sex = "female", heightCm = "178", weightKg = "abc"))
    }

    @Test
    fun p_isOnboardingStepValid_workoutBodyRequiresProfile() {
        assertTrue(
            isOnboardingStepValid(
                step = OnboardingStep.WORKOUT_BODY,
                name = "Hunter",
                enabledModules = EnabledModules(workout = true),
                careerIntent = "",
                createOwnRoutine = false,
                workoutSplitId = "ppl_ul",
                splitDayMap = emptyMap(),
                preferredWorkoutDays = emptySet(),
                age = "25",
                sex = "female",
                heightCm = "170",
                weightKg = "65",
            ),
        )
    }

    @Test
    fun n_isOnboardingStepValid_workoutBodyRejectsEmptyDefaults() {
        assertFalse(
            isOnboardingStepValid(
                step = OnboardingStep.WORKOUT_BODY,
                name = "Hunter",
                enabledModules = EnabledModules(workout = true),
                careerIntent = "",
                createOwnRoutine = false,
                workoutSplitId = "ppl_ul",
                splitDayMap = emptyMap(),
                preferredWorkoutDays = emptySet(),
                age = "",
                sex = "",
                heightCm = "",
                weightKg = "",
            ),
        )
    }

    @Test
    fun n_isOnboardingStepValid_dietOnlyRequiresBodyFields() {
        assertFalse(
            isOnboardingStepValid(
                step = OnboardingStep.DIET_NUTRITION,
                name = "Hunter",
                enabledModules = EnabledModules(diet = true),
                careerIntent = "",
                createOwnRoutine = false,
                workoutSplitId = "ppl_ul",
                splitDayMap = emptyMap(),
                preferredWorkoutDays = emptySet(),
                age = "",
                sex = "",
                heightCm = "",
                weightKg = "",
            ),
        )
        assertTrue(
            isOnboardingStepValid(
                step = OnboardingStep.DIET_NUTRITION,
                name = "Hunter",
                enabledModules = EnabledModules(diet = true),
                careerIntent = "",
                createOwnRoutine = false,
                workoutSplitId = "ppl_ul",
                splitDayMap = emptyMap(),
                preferredWorkoutDays = emptySet(),
                age = "25",
                sex = "male",
                heightCm = "178",
                weightKg = "74",
            ),
        )
    }

    @Test
    fun n_isOnboardingStepValid_dietOnly_heightWeightWithoutAgeSex_isInvalid() {
        assertFalse(
            isOnboardingStepValid(
                step = OnboardingStep.DIET_NUTRITION,
                name = "Hunter",
                enabledModules = EnabledModules(diet = true),
                careerIntent = "",
                createOwnRoutine = false,
                workoutSplitId = "ppl_ul",
                splitDayMap = emptyMap(),
                preferredWorkoutDays = emptySet(),
                age = "",
                sex = "",
                heightCm = "172",
                weightKg = "73",
            ),
        )
    }

    @Test
    fun e_isOnboardingStepValid_dietAfterWorkout_skipsBodyRequirement() {
        assertTrue(
            isOnboardingStepValid(
                step = OnboardingStep.DIET_NUTRITION,
                name = "Hunter",
                enabledModules = EnabledModules(workout = true, diet = true),
                careerIntent = "",
                createOwnRoutine = false,
                workoutSplitId = "ppl_ul",
                splitDayMap = emptyMap(),
                preferredWorkoutDays = emptySet(),
                age = "",
                sex = "",
                heightCm = "",
                weightKg = "",
            ),
        )
    }

    @Test
    fun e_isOnboardingStepValid_careerOnly_summaryIgnoresEmptyBody() {
        assertTrue(
            isOnboardingStepValid(
                step = OnboardingStep.SUMMARY,
                name = "Hunter",
                enabledModules = EnabledModules(career = true),
                careerIntent = "learning",
                createOwnRoutine = false,
                workoutSplitId = "ppl_ul",
                splitDayMap = emptyMap(),
                preferredWorkoutDays = emptySet(),
                age = "",
                sex = "",
                heightCm = "",
                weightKg = "",
            ),
        )
    }
}
