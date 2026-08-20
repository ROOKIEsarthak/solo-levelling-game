package com.example.solo_levelling.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleScopeTest {
    @Test
    fun p_sourceTypeOwnership() {
        assertEquals(ModuleId.CAREER, ModuleScope.moduleForSourceType("DSA"))
        assertEquals(ModuleId.WORKOUT, ModuleScope.moduleForSourceType("WORKOUT"))
        assertEquals(ModuleId.WORKOUT, ModuleScope.moduleForSourceType("WORKOUT_UNDO"))
        assertEquals(ModuleId.DIET, ModuleScope.moduleForSourceType("NUTRITION"))
        assertEquals(ModuleId.DIET, ModuleScope.moduleForSourceType("NUTRITION_UNDO"))
        assertEquals(ModuleId.GLOBAL, ModuleScope.moduleForSourceType("QUEST_UNDO"))
        assertEquals(ModuleId.GLOBAL, ModuleScope.moduleForSourceType("QUEST_UNDO_PENALTY"))
        assertEquals(ModuleId.GLOBAL, ModuleScope.moduleForSourceType("FOCUS"))
        assertEquals(ModuleId.GLOBAL, ModuleScope.moduleForSourceType("JOURNAL"))
        assertEquals(ModuleId.GLOBAL, ModuleScope.moduleForSourceType("ACHIEVEMENT"))
    }

    @Test
    fun p_questTagsOwnership() {
        assertEquals(ModuleId.CAREER, ModuleScope.moduleForPriorityTags("module_career,growth"))
        assertEquals(ModuleId.WORKOUT, ModuleScope.moduleForPriorityTags("module_workout"))
        assertEquals(ModuleId.DIET, ModuleScope.moduleForPriorityTags("module_diet,health"))
        assertEquals(ModuleId.GLOBAL, ModuleScope.moduleForPriorityTags(""))
        assertEquals(ModuleId.GLOBAL, ModuleScope.moduleForPriorityTags("fitness,health"))
    }

    @Test
    fun p_allows_workoutOnly() {
        val m = EnabledModules(career = false, workout = true, diet = false)
        assertTrue(ModuleScope.allowsSourceType("WORKOUT", m))
        assertFalse(ModuleScope.allowsSourceType("DSA", m))
        assertFalse(ModuleScope.allowsSourceType("NUTRITION", m))
        assertTrue(ModuleScope.allowsSourceType("FOCUS", m))
        assertTrue(ModuleScope.allowsQuestTemplate("module_workout", m))
        assertFalse(ModuleScope.allowsQuestTemplate("module_career", m))
        assertTrue(ModuleScope.allowsQuestTemplate("", m))
    }

    @Test
    fun p_allows_allCombinations() {
        val all = EnabledModules(true, true, true)
        assertTrue(ModuleScope.allowsSourceType("DSA", all))
        assertTrue(ModuleScope.allowsSourceType("WORKOUT", all))
        assertTrue(ModuleScope.allowsSourceType("NUTRITION", all))

        val careerDiet = EnabledModules(career = true, workout = false, diet = true)
        assertTrue(ModuleScope.allowsSourceType("DSA", careerDiet))
        assertFalse(ModuleScope.allowsSourceType("WORKOUT", careerDiet))
        assertTrue(ModuleScope.allowsSourceType("NUTRITION", careerDiet))
    }

    @Test
    fun p_achievementOwnership() {
        assertEquals(ModuleId.CAREER, ModuleScope.moduleForAchievement("DSA_SOLVED"))
        assertEquals(ModuleId.GLOBAL, ModuleScope.moduleForAchievement("STREAK"))
        assertFalse(
            ModuleScope.allowsAchievement(
                "DSA_SOLVED",
                EnabledModules(career = false, workout = true, diet = false),
            ),
        )
        assertTrue(
            ModuleScope.allowsAchievement(
                "STREAK",
                EnabledModules(career = false, workout = true, diet = false),
            ),
        )
    }

    @Test
    fun p_activeModulesSummary() {
        assertEquals(
            "Workout · Diet",
            ModuleScope.activeModulesSummary(EnabledModules(false, true, true)),
        )
        assertEquals("None", ModuleScope.activeModulesSummary(EnabledModules()))
    }

    @Test
    fun e_ledgerMetadataModule() {
        val m = EnabledModules(career = false, workout = true, diet = false)
        assertTrue(
            ModuleScope.allowsLedgerEntry("QUEST_INSTANCE", """{"module":"WORKOUT"}""", m),
        )
        assertFalse(
            ModuleScope.allowsLedgerEntry("QUEST_INSTANCE", """{"module":"CAREER"}""", m),
        )
        assertFalse(
            ModuleScope.allowsLedgerEntry(
                "QUEST_INSTANCE",
                "{}",
                m,
                questModule = ModuleId.CAREER,
            ),
        )
        assertTrue(
            ModuleScope.allowsLedgerEntry(
                "QUEST_INSTANCE",
                "{}",
                m,
                questModule = ModuleId.WORKOUT,
            ),
        )
    }
}
