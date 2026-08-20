package com.example.solo_levelling.domain.logic

import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.data.db.entity.DietLogEntity
import com.example.solo_levelling.data.db.entity.FoodItemEntity
import com.example.solo_levelling.data.db.entity.MealEntity
import com.example.solo_levelling.data.db.entity.NutritionTotalsEntity

val DIET_MEAL_CATEGORIES = listOf("Breakfast", "Lunch", "Dinner", "Snack")

data class MealSlotStatus(
    val name: String,
    val logged: Boolean,
    val calories: Int = 0,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fat: Int = 0,
)

data class MealProgressState(
    val loggedCount: Int,
    val requiredCount: Int,
    val isComplete: Boolean,
    val slotStatuses: List<MealSlotStatus>,
    val guidanceLine: String,
    val progressLabel: String,
)

object MealCompletionPolicy {
    fun isValidFood(food: FoodItemEntity): Boolean {
        if (food.name.isBlank()) return false
        val calories = food.calories ?: 0
        val protein = food.protein ?: 0
        val carbs = food.carbs ?: 0
        val fat = food.fat ?: 0
        return calories > 0 || protein > 0 || carbs > 0 || fat > 0
    }

    fun isValidMeal(meal: MealEntity): Boolean =
        meal.foods.any { isValidFood(it) }

    fun countValidMeals(log: DietLogEntity?): Int =
        log?.meals?.count { isValidMeal(it) } ?: 0

    fun isMealTrackingComplete(
        log: DietLogEntity?,
        required: Int = SystemDefaults.DEFAULT_REQUIRED_MEALS_PER_DAY,
    ): Boolean = countValidMeals(log) >= required

    fun mealProgressState(
        log: DietLogEntity?,
        required: Int = SystemDefaults.DEFAULT_REQUIRED_MEALS_PER_DAY,
        mealTotals: (MealEntity) -> NutritionTotalsEntity = ::sumFoodsInMeal,
    ): MealProgressState {
        val loggedCount = countValidMeals(log)
        val isComplete = loggedCount >= required
        val slotStatuses = slotStatuses(log?.meals.orEmpty(), mealTotals)
        val remaining = (required - loggedCount).coerceAtLeast(0)
        val guidanceLine = when {
            isComplete && loggedCount > required ->
                "Daily tracking complete."
            isComplete ->
                "Today's meal tracking is complete."
            remaining == 1 ->
                "Log one more meal to complete today's nutrition tracking."
            remaining > 1 ->
                "Log $remaining more meals to complete today's nutrition tracking."
            else ->
                "Start logging meals to track today's nutrition."
        }
        val progressLabel = when {
            isComplete && loggedCount > required ->
                "$loggedCount meals logged · Daily tracking complete"
            isComplete ->
                "$loggedCount / $required meals logged"
            else ->
                "$loggedCount / $required meals logged"
        }
        return MealProgressState(
            loggedCount = loggedCount,
            requiredCount = required,
            isComplete = isComplete,
            slotStatuses = slotStatuses,
            guidanceLine = guidanceLine,
            progressLabel = progressLabel,
        )
    }

    fun slotStatuses(
        meals: List<MealEntity>,
        mealTotals: (MealEntity) -> NutritionTotalsEntity,
    ): List<MealSlotStatus> =
        dietCategoryChips(meals).map { name ->
            val loggedMeals = mealsForCategory(meals, name).filter { isValidMeal(it) }
            if (loggedMeals.isEmpty()) {
                MealSlotStatus(name = name, logged = false)
            } else {
                val totals = loggedMeals.fold(NutritionTotalsEntity()) { acc, meal ->
                    val t = mealTotals(meal)
                    NutritionTotalsEntity(
                        calories = acc.calories + t.calories,
                        protein = acc.protein + t.protein,
                        carbs = acc.carbs + t.carbs,
                        fat = acc.fat + t.fat,
                    )
                }
                MealSlotStatus(
                    name = name,
                    logged = true,
                    calories = totals.calories,
                    protein = totals.protein,
                    carbs = totals.carbs,
                    fat = totals.fat,
                )
            }
        }

    fun dietLogAffectsProgress(log: DietLogEntity?): Boolean =
        countValidMeals(log) > 0

    fun deletingMealReversesProgress(log: DietLogEntity?, mealId: Long): Boolean {
        val meal = log?.meals?.find { it.id == mealId } ?: return false
        if (!isValidMeal(meal)) return false
        if (!isMealTrackingComplete(log)) return false
        val after = log.copy(meals = log.meals.filterNot { it.id == mealId })
        return !isMealTrackingComplete(after)
    }

    fun deletingFoodReversesProgress(log: DietLogEntity?, mealId: Long, foodId: Long): Boolean {
        if (!dietLogAffectsProgress(log)) return false
        if (!isMealTrackingComplete(log)) return false
        val afterMeals = log?.meals.orEmpty().map { meal ->
            if (meal.id == mealId) meal.copy(foods = meal.foods.filterNot { it.id == foodId }) else meal
        }
        return !isMealTrackingComplete(log?.copy(meals = afterMeals))
    }

    private fun mealsForCategory(meals: List<MealEntity>, category: String): List<MealEntity> {
        val key = category.trim()
        if (key.isEmpty()) return emptyList()
        return meals.filter { it.name.trim().equals(key, ignoreCase = true) }
    }

    private fun extraDietCategoryChips(meals: List<MealEntity>): List<String> {
        val defaultKeys = DIET_MEAL_CATEGORIES.map { it.trim().lowercase() }.toSet()
        val seen = mutableSetOf<String>()
        val extras = mutableListOf<String>()
        for (meal in meals) {
            val name = meal.name.trim()
            if (name.isEmpty()) continue
            val key = name.lowercase()
            if (key in defaultKeys || key in seen) continue
            seen += key
            extras += name
        }
        return extras
    }

    private fun dietCategoryChips(meals: List<MealEntity>): List<String> =
        DIET_MEAL_CATEGORIES + extraDietCategoryChips(meals)

    private fun sumFoodsInMeal(meal: MealEntity): NutritionTotalsEntity {
        var calories = 0
        var protein = 0
        var carbs = 0
        var fat = 0
        for (f in meal.foods) {
            calories += f.calories ?: 0
            protein += f.protein ?: 0
            carbs += f.carbs ?: 0
            fat += f.fat ?: 0
        }
        return NutritionTotalsEntity(calories, protein, carbs, fat)
    }
}
