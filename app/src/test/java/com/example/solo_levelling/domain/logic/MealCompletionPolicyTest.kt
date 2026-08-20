package com.example.solo_levelling.domain.logic

import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.data.db.entity.DietLogEntity
import com.example.solo_levelling.data.db.entity.FoodItemEntity
import com.example.solo_levelling.data.db.entity.MealEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MealCompletionPolicyTest {
    private fun food(name: String = "Food", calories: Int = 300) = FoodItemEntity(
        name = name,
        calories = calories,
        protein = 20,
    )

    private fun meal(name: String, vararg foods: FoodItemEntity) =
        MealEntity(name = name, foods = foods.toList())

    private fun log(vararg meals: MealEntity) = DietLogEntity(date = "2026-08-15", meals = meals.toList())

    @Test
    fun n_zeroMeals_incomplete() {
        assertEquals(0, MealCompletionPolicy.countValidMeals(null))
        assertFalse(MealCompletionPolicy.isMealTrackingComplete(null))
        val state = MealCompletionPolicy.mealProgressState(null)
        assertEquals("0 / 3 meals logged", state.progressLabel)
        assertFalse(state.isComplete)
    }

    @Test
    fun n_oneMeal_incomplete() {
        val diet = log(meal("Breakfast", food()))
        assertEquals(1, MealCompletionPolicy.countValidMeals(diet))
        assertFalse(MealCompletionPolicy.isMealTrackingComplete(diet))
    }

    @Test
    fun n_twoMeals_incomplete() {
        val diet = log(meal("Breakfast", food()), meal("Lunch", food()))
        assertEquals(2, MealCompletionPolicy.countValidMeals(diet))
        assertFalse(MealCompletionPolicy.isMealTrackingComplete(diet))
    }

    @Test
    fun p_threeMeals_complete() {
        val diet = log(
            meal("Breakfast", food()),
            meal("Lunch", food()),
            meal("Dinner", food()),
        )
        assertEquals(3, MealCompletionPolicy.countValidMeals(diet))
        assertTrue(MealCompletionPolicy.isMealTrackingComplete(diet))
        assertTrue(MealCompletionPolicy.mealProgressState(diet).isComplete)
    }

    @Test
    fun p_fourMeals_stillComplete_noOverQuotaLabel() {
        val diet = log(
            meal("Breakfast", food()),
            meal("Lunch", food()),
            meal("Dinner", food()),
            meal("Snack", food()),
        )
        assertTrue(MealCompletionPolicy.isMealTrackingComplete(diet))
        val state = MealCompletionPolicy.mealProgressState(diet)
        assertTrue(state.progressLabel.contains("4 meals logged"))
        assertTrue(state.progressLabel.contains("Daily tracking complete"))
    }

    @Test
    fun n_threeFoodsInOneMeal_countsAsOne() {
        val diet = log(
            meal(
                "Breakfast",
                food("Eggs"),
                food("Toast"),
                food("Banana"),
            ),
        )
        assertEquals(1, MealCompletionPolicy.countValidMeals(diet))
        assertFalse(MealCompletionPolicy.isMealTrackingComplete(diet))
    }

    @Test
    fun n_emptyMealShell_doesNotCount() {
        val diet = log(meal("Breakfast"), meal("Lunch", food()), meal("Dinner", food()))
        assertEquals(2, MealCompletionPolicy.countValidMeals(diet))
    }

    @Test
    fun n_foodWithoutMacros_doesNotCount() {
        assertFalse(MealCompletionPolicy.isValidFood(FoodItemEntity(name = "Water")))
        val diet = log(meal("Breakfast", FoodItemEntity(name = "Water")))
        assertEquals(0, MealCompletionPolicy.countValidMeals(diet))
    }

    @Test
    fun p_deletingMealReversesProgress_whenCrossingBelowThreshold() {
        val diet = log(
            meal("Breakfast", food()),
            meal("Lunch", food()),
            meal("Dinner", food()),
        )
        val breakfastId = diet.meals.first { it.name == "Breakfast" }.id
        assertTrue(MealCompletionPolicy.deletingMealReversesProgress(diet, breakfastId))
    }

    @Test
    fun n_deletingMeal_beforeComplete_doesNotReverse() {
        val diet = log(meal("Breakfast", food()), meal("Lunch", food()))
        val breakfastId = diet.meals.first().id
        assertFalse(MealCompletionPolicy.deletingMealReversesProgress(diet, breakfastId))
    }

    @Test
    fun p_requiredMealsFromSystemDefaults() {
        assertEquals(3, SystemDefaults.DEFAULT_REQUIRED_MEALS_PER_DAY)
    }
}
