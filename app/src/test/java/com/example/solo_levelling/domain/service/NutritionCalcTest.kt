package com.example.solo_levelling.domain.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionCalcTest {
    @Test
    fun p_bmi_normalRange() {
        val bmi = NutritionCalc.bmi(heightCm = 180.0, weightKg = 75.0)
        assertEquals(23.15, bmi, 0.01)
        assertEquals("Normal (estimate)", NutritionCalc.bmiCategory(bmi))
    }

    @Test
    fun p_bmrAndTdee_maleModerate() {
        val bmr = NutritionCalc.bmrMifflin("male", age = 25, heightCm = 180.0, weightKg = 75.0)
        assertEquals(1755.0, bmr, 1.0)
        val calories = NutritionCalc.tdee(bmr, "moderate")
        assertEquals(2720.25, calories, 1.0)
    }

    @Test
    fun p_goalCalories_fatLossSubtracts500() {
        val tdee = 2500.0
        assertEquals(2000, NutritionCalc.goalCalories(tdee, "fat_loss"))
    }

    @Test
    fun p_macroTargets_muscleGainHigherProtein() {
        val macros = NutritionCalc.macroTargets(weightKg = 80.0, goalCalories = 2800, fitnessGoal = "muscle_gain")
        assertEquals(160, macros.proteinG)
        assertTrue(macros.fatG > 0)
        assertTrue(macros.carbsG > 0)
        val totalCalories = macros.proteinG * 4 + macros.carbsG * 4 + macros.fatG * 9
        assertTrue(totalCalories in 2780..2820)
    }

    @Test
    fun n_bmi_zeroHeightReturnsZero() {
        assertEquals(0.0, NutritionCalc.bmi(0.0, 70.0), 0.001)
        assertEquals("Unknown", NutritionCalc.bmiCategory(0.0))
    }

    @Test
    fun e_bmiCategory_boundaries() {
        assertEquals("Underweight (estimate)", NutritionCalc.bmiCategory(18.0))
        assertEquals("Normal (estimate)", NutritionCalc.bmiCategory(24.9))
        assertEquals("Overweight (estimate)", NutritionCalc.bmiCategory(29.9))
        assertEquals("Obese (estimate)", NutritionCalc.bmiCategory(30.0))
    }

    @Test
    fun e_goalCalories_neverBelowMinimum() {
        assertEquals(1200, NutritionCalc.goalCalories(900.0, "fat_loss"))
    }
}
