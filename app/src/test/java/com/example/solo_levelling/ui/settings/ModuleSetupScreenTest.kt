package com.example.solo_levelling.ui.settings

import com.example.solo_levelling.domain.service.ModuleFlags
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleSetupScreenTest {
    @Test
    fun p_careerSetup_validWhenIntentSelected() {
        assertTrue(
            isModuleSetupValid(
                module = ModuleFlags.MODULE_CAREER,
                careerIntent = "interviews",
                createOwnRoutine = false,
                workoutSplitId = "",
                splitDayMap = emptyMap(),
                preferredWorkoutDays = emptySet(),
                age = "",
                sex = "male",
                heightCm = "",
                weightKg = "",
                requireBody = false,
            ),
        )
    }

    @Test
    fun n_careerSetup_invalidWithoutIntent() {
        assertFalse(
            isModuleSetupValid(
                module = ModuleFlags.MODULE_CAREER,
                careerIntent = "",
                createOwnRoutine = false,
                workoutSplitId = "",
                splitDayMap = emptyMap(),
                preferredWorkoutDays = emptySet(),
                age = "",
                sex = "male",
                heightCm = "",
                weightKg = "",
                requireBody = false,
            ),
        )
    }

    @Test
    fun p_dietSetup_skipsBodyWhenAlreadyStored() {
        assertTrue(
            isModuleSetupValid(
                module = ModuleFlags.MODULE_DIET,
                careerIntent = "",
                createOwnRoutine = false,
                workoutSplitId = "",
                splitDayMap = emptyMap(),
                preferredWorkoutDays = emptySet(),
                age = "",
                sex = "",
                heightCm = "",
                weightKg = "",
                requireBody = false,
            ),
        )
    }

    @Test
    fun n_dietSetup_requiresBodyWhenMissing() {
        assertFalse(
            isModuleSetupValid(
                module = ModuleFlags.MODULE_DIET,
                careerIntent = "",
                createOwnRoutine = false,
                workoutSplitId = "",
                splitDayMap = emptyMap(),
                preferredWorkoutDays = emptySet(),
                age = "",
                sex = "male",
                heightCm = "",
                weightKg = "",
                requireBody = true,
            ),
        )
    }

    @Test
    fun e_unknownModule_isInvalid() {
        assertFalse(
            isModuleSetupValid(
                module = "focus",
                careerIntent = "x",
                createOwnRoutine = false,
                workoutSplitId = "ppl_ul",
                splitDayMap = mapOf(1 to 1),
                preferredWorkoutDays = emptySet(),
                age = "25",
                sex = "male",
                heightCm = "170",
                weightKg = "70",
                requireBody = false,
            ),
        )
    }

    @Test
    fun p_setupCopy_isLightweight() {
        assertEquals("Set up Nutrition", moduleSetupTitle(ModuleFlags.MODULE_DIET))
        assertTrue(moduleSetupIntro(ModuleFlags.MODULE_DIET).contains("nutrition targets"))
        assertTrue(moduleSetupIntro(ModuleFlags.MODULE_CAREER).contains("roadmap"))
        assertFalse(moduleSetupIntro(ModuleFlags.MODULE_CAREER).contains("build career quests"))
        assertEquals("Career is now active.", moduleInitializedMessage(ModuleFlags.MODULE_CAREER))
    }

    @Test
    fun p_workoutSetup_validWithStoredBodyAndSplit() {
        val split = com.example.solo_levelling.data.seed.WorkoutCatalog.findSplit("ppl_ul")!!
        val map = com.example.solo_levelling.domain.service.WorkoutSplitLogic.defaultDayMap(split)
        assertTrue(
            isModuleSetupValid(
                module = ModuleFlags.MODULE_WORKOUT,
                careerIntent = "",
                createOwnRoutine = false,
                workoutSplitId = "ppl_ul",
                splitDayMap = map,
                preferredWorkoutDays = emptySet(),
                age = "",
                sex = "male",
                heightCm = "",
                weightKg = "",
                requireBody = false,
            ),
        )
    }

    @Test
    fun n_workoutSetup_requiresBodyWhenMissing() {
        val split = com.example.solo_levelling.data.seed.WorkoutCatalog.findSplit("ppl_ul")!!
        val map = com.example.solo_levelling.domain.service.WorkoutSplitLogic.defaultDayMap(split)
        assertFalse(
            isModuleSetupValid(
                module = ModuleFlags.MODULE_WORKOUT,
                careerIntent = "",
                createOwnRoutine = false,
                workoutSplitId = "ppl_ul",
                splitDayMap = map,
                preferredWorkoutDays = emptySet(),
                age = "",
                sex = "male",
                heightCm = "",
                weightKg = "",
                requireBody = true,
            ),
        )
    }
}
