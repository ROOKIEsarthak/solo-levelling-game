package com.example.solo_levelling.domain.service

import com.example.solo_levelling.data.db.entity.FoodItemEntity
import com.example.solo_levelling.data.db.entity.NutritionTotalsEntity
import com.example.solo_levelling.domain.logic.MealCompletionPolicy
import com.example.solo_levelling.domain.logic.MealProgressState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionFeedbackServiceTest {
    private val targets = NutritionTargets(
        calorieTarget = 2200,
        proteinTarget = 140,
        carbTarget = 240,
        fatTarget = 70,
        targetsConfigured = true,
    )

    private fun progress(logged: Int, required: Int = 3, complete: Boolean = logged >= required) =
        MealProgressState(
            loggedCount = logged,
            requiredCount = required,
            isComplete = complete,
            slotStatuses = emptyList(),
            guidanceLine = "",
            progressLabel = "$logged / $required meals logged",
        )

    @Test
    fun n_noTargetsConfigured_omitsMacroLine() {
        val feedback = NutritionFeedbackService.buildPostMealFeedback(
            mealId = 1L,
            mealName = "Breakfast",
            food = FoodItemEntity(name = "Oats", calories = 300, protein = 10),
            dailyTotals = NutritionTotalsEntity(300, 10, 40, 5),
            targets = targets.copy(targetsConfigured = false, calorieTarget = 0),
            fitnessGoal = "maintenance",
            mealProgress = progress(1),
        )
        assertNull(feedback.dailyMacroLine)
        assertTrue(feedback.recommendation.isNotBlank())
    }

    @Test
    fun p_fatLossAboveTarget_usesFactualLanguage() {
        val feedback = NutritionFeedbackService.buildPostMealFeedback(
            mealId = 1L,
            mealName = "Lunch",
            food = FoodItemEntity(name = "Pizza", calories = 900, protein = 30),
            dailyTotals = NutritionTotalsEntity(2500, 135, 230, 65),
            targets = targets,
            fitnessGoal = "fat_loss",
            mealProgress = progress(2),
        )
        assertTrue(
            feedback.recommendation.contains("calorie target", ignoreCase = true),
        )
        assertFalse(feedback.recommendation.contains("failed", ignoreCase = true))
    }

    @Test
    fun p_singleMealAboveTarget_doesNotClaimFailure() {
        val feedback = NutritionFeedbackService.buildPostMealFeedback(
            mealId = 1L,
            mealName = "Dinner",
            food = FoodItemEntity(name = "Burger", calories = 800, protein = 40),
            dailyTotals = NutritionTotalsEntity(800, 40, 60, 35),
            targets = targets,
            fitnessGoal = "fat_loss",
            mealProgress = progress(1),
        )
        assertFalse(feedback.recommendation.contains("ruined", ignoreCase = true))
        assertFalse(feedback.recommendation.contains("failed", ignoreCase = true))
    }

    @Test
    fun p_multipleMacrosOffTarget_oneRecommendation() {
        val feedback = NutritionFeedbackService.buildPostMealFeedback(
            mealId = 1L,
            mealName = "Lunch",
            food = FoodItemEntity(name = "Salad", calories = 200, protein = 5),
            dailyTotals = NutritionTotalsEntity(900, 55, 80, 30),
            targets = targets,
            fitnessGoal = "maintenance",
            mealProgress = progress(2),
        )
        assertTrue(feedback.recommendation.isNotBlank())
        assertFalse(feedback.recommendation.contains("calories, protein, carbs and fats"))
    }

    @Test
    fun p_lastMeal_showsDailyReview() {
        val feedback = NutritionFeedbackService.buildPostMealFeedback(
            mealId = 3L,
            mealName = "Dinner",
            food = FoodItemEntity(name = "Chicken", calories = 400, protein = 45),
            dailyTotals = NutritionTotalsEntity(1800, 120, 180, 55),
            targets = targets,
            fitnessGoal = "maintenance",
            mealProgress = MealCompletionPolicy.mealProgressState(
                com.example.solo_levelling.data.db.entity.DietLogEntity(
                    date = "2026-08-15",
                    meals = listOf(
                        com.example.solo_levelling.data.db.entity.MealEntity(
                            name = "Breakfast",
                            foods = listOf(FoodItemEntity(name = "Oats", calories = 300, protein = 10)),
                        ),
                        com.example.solo_levelling.data.db.entity.MealEntity(
                            name = "Lunch",
                            foods = listOf(FoodItemEntity(name = "Rice", calories = 400, protein = 8)),
                        ),
                        com.example.solo_levelling.data.db.entity.MealEntity(
                            name = "Dinner",
                            foods = listOf(FoodItemEntity(name = "Chicken", calories = 400, protein = 45)),
                        ),
                    ),
                ),
            ),
        )
        assertEquals("DAILY NUTRITION REVIEW", feedback.title)
        assertTrue(feedback.isDailyReview)
    }

    @Test
    fun p_workoutAndDiet_proteinRecoveryHint() {
        val feedback = NutritionFeedbackService.buildPostMealFeedback(
            mealId = 1L,
            mealName = "Lunch",
            food = FoodItemEntity(name = "Salad", calories = 200, protein = 5),
            dailyTotals = NutritionTotalsEntity(1200, 40, 180, 20),
            targets = targets,
            fitnessGoal = "muscle_gain",
            mealProgress = progress(1),
            workoutDoneToday = true,
            dietAndWorkoutEnabled = true,
        )
        assertTrue(
            feedback.recommendation.contains("Protein", ignoreCase = true) ||
                feedback.recommendation.contains("recovery", ignoreCase = true),
        )
    }
}
