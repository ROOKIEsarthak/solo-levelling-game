package com.example.solo_levelling.domain.service

import org.junit.Assert.assertEquals
import org.junit.Test

class PriorityEngineTest {
    @Test
    fun p_workoutModuleOff_skipsFitnessEvenWhenPlanned() {
        val action = PriorityEngine.nextAction(
            dsaPct = 10,
            sdPct = 10,
            backendPct = 10,
            behavioralPct = 10,
            mandatoryAreas = listOf("DSA"),
            workoutDoneToday = false,
            workoutPlannedToday = true,
            dietCaloriePctOfTarget = 80,
            proteinPctOfTarget = 80,
            openQuestsRemaining = 3,
            modules = EnabledModules(career = true, workout = false, diet = true),
        )
        assertEquals("career_dsa", action.routeHint)
    }

    @Test
    fun p_plannedWorkoutNotDone_prioritizesFitness() {
        val action = PriorityEngine.nextAction(
            dsaPct = 10,
            sdPct = 10,
            backendPct = 10,
            behavioralPct = 10,
            mandatoryAreas = listOf("DSA"),
            workoutDoneToday = false,
            workoutPlannedToday = true,
            dietCaloriePctOfTarget = 80,
            proteinPctOfTarget = 80,
            openQuestsRemaining = 3,
        )
        assertEquals("fitness", action.routeHint)
    }

    @Test
    fun p_lowProteinLateDay_prioritizesNutrition() {
        val action = PriorityEngine.nextAction(
            dsaPct = 50,
            sdPct = 50,
            backendPct = 50,
            behavioralPct = 50,
            mandatoryAreas = emptyList(),
            workoutDoneToday = true,
            workoutPlannedToday = false,
            dietCaloriePctOfTarget = 60,
            proteinPctOfTarget = 50,
            openQuestsRemaining = 0,
        )
        assertEquals("nutrition", action.routeHint)
    }

    @Test
    fun p_mandatorySdLowest_routesToCareerSd() {
        val action = PriorityEngine.nextAction(
            dsaPct = 80,
            sdPct = 20,
            backendPct = 70,
            behavioralPct = 60,
            mandatoryAreas = listOf("DSA", "System Design", "Backend"),
            workoutDoneToday = true,
            workoutPlannedToday = false,
            dietCaloriePctOfTarget = 90,
            proteinPctOfTarget = 90,
            openQuestsRemaining = 2,
        )
        assertEquals("career_sd", action.routeHint)
    }

    @Test
    fun p_openQuestsWhenNoBlockers_routesToQuests() {
        val action = PriorityEngine.nextAction(
            dsaPct = 80,
            sdPct = 80,
            backendPct = 80,
            behavioralPct = 80,
            mandatoryAreas = emptyList(),
            workoutDoneToday = true,
            workoutPlannedToday = false,
            dietCaloriePctOfTarget = 90,
            proteinPctOfTarget = 90,
            openQuestsRemaining = 2,
        )
        assertEquals("quests", action.routeHint)
    }

    @Test
    fun n_mandatoryDsaLowest_routesToCareerDsa() {
        val action = PriorityEngine.nextAction(
            dsaPct = 15,
            sdPct = 70,
            backendPct = 60,
            behavioralPct = 50,
            mandatoryAreas = listOf("DSA", "Backend"),
            workoutDoneToday = true,
            workoutPlannedToday = false,
            dietCaloriePctOfTarget = 90,
            proteinPctOfTarget = 90,
            openQuestsRemaining = 0,
        )
        assertEquals("career_dsa", action.routeHint)
    }

    @Test
    fun e_noBlockersAndNoQuests_picksWeakestCareerArea() {
        val action = PriorityEngine.nextAction(
            dsaPct = 40,
            sdPct = 55,
            backendPct = 60,
            behavioralPct = 70,
            mandatoryAreas = emptyList(),
            workoutDoneToday = true,
            workoutPlannedToday = false,
            dietCaloriePctOfTarget = 90,
            proteinPctOfTarget = 90,
            openQuestsRemaining = 0,
        )
        assertEquals("career_dsa", action.routeHint)
        assertEquals("Improve DSA", action.title)
    }

    @Test
    fun n_workoutDisabled_skipsPlannedWorkoutPriority() {
        val action = PriorityEngine.nextAction(
            dsaPct = 10,
            sdPct = 10,
            backendPct = 10,
            behavioralPct = 10,
            mandatoryAreas = listOf("DSA"),
            workoutDoneToday = false,
            workoutPlannedToday = true,
            dietCaloriePctOfTarget = 80,
            proteinPctOfTarget = 80,
            openQuestsRemaining = 0,
            modules = EnabledModules(career = true, workout = false, diet = true),
        )
        assertEquals("career_dsa", action.routeHint)
    }

    @Test
    fun n_careerDisabled_skipsMandatoryCareerActions() {
        val action = PriorityEngine.nextAction(
            dsaPct = 10,
            sdPct = 80,
            backendPct = 80,
            behavioralPct = 80,
            mandatoryAreas = listOf("DSA"),
            workoutDoneToday = true,
            workoutPlannedToday = false,
            dietCaloriePctOfTarget = 90,
            proteinPctOfTarget = 90,
            openQuestsRemaining = 0,
            modules = EnabledModules(career = false, workout = true, diet = true),
        )
        assertEquals("dashboard", action.routeHint)
    }
}
