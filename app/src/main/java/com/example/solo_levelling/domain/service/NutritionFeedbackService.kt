package com.example.solo_levelling.domain.service

import com.example.solo_levelling.data.db.entity.FoodItemEntity
import com.example.solo_levelling.data.db.entity.NutritionTotalsEntity
import com.example.solo_levelling.domain.copy.SystemMessages
import com.example.solo_levelling.domain.logic.MealProgressState

data class NutritionTargets(
    val calorieTarget: Int,
    val proteinTarget: Int,
    val carbTarget: Int,
    val fatTarget: Int,
    val targetsConfigured: Boolean,
)

data class PostMealFeedback(
    val mealId: Long,
    val mealName: String,
    val foodSummary: String,
    val progressLabel: String,
    val dailyMacroLine: String?,
    val recommendation: String,
    val isDailyReview: Boolean,
    val title: String,
)

enum class MacroBand { Below, Near, Above }

object NutritionFeedbackService {
    fun buildPostMealFeedback(
        mealId: Long,
        mealName: String,
        food: FoodItemEntity,
        dailyTotals: NutritionTotalsEntity,
        targets: NutritionTargets,
        fitnessGoal: String,
        mealProgress: MealProgressState,
        workoutDoneToday: Boolean = false,
        dietAndWorkoutEnabled: Boolean = false,
    ): PostMealFeedback {
        val foodSummary = buildFoodSummary(food)
        val isDailyReview = mealProgress.isComplete
        val title = if (isDailyReview) "DAILY NUTRITION REVIEW" else "MEAL LOGGED"
        val dailyMacroLine = if (targets.targetsConfigured) {
            "${dailyTotals.calories} / ${targets.calorieTarget} kcal · " +
                "${dailyTotals.protein} / ${targets.proteinTarget}g protein"
        } else {
            null
        }
        val recommendation = if (targets.targetsConfigured) {
            pickRecommendation(
                dailyTotals = dailyTotals,
                targets = targets,
                fitnessGoal = fitnessGoal,
                mealProgress = mealProgress,
                workoutDoneToday = workoutDoneToday,
                dietAndWorkoutEnabled = dietAndWorkoutEnabled,
            )
        } else {
            if (mealProgress.isComplete) {
                SystemMessages.MEAL_TRACKING_COMPLETE_NO_TARGETS
            } else {
                SystemMessages.MEAL_TRACKING_PROGRESS_NO_TARGETS
            }
        }
        return PostMealFeedback(
            mealId = mealId,
            mealName = mealName,
            foodSummary = foodSummary,
            progressLabel = mealProgress.progressLabel,
            dailyMacroLine = dailyMacroLine,
            recommendation = recommendation,
            isDailyReview = isDailyReview,
            title = title,
        )
    }

    private fun buildFoodSummary(food: FoodItemEntity): String {
        val kcal = food.calories ?: 0
        val protein = food.protein ?: 0
        return if (protein > 0) "$kcal kcal · ${protein}g protein" else "$kcal kcal"
    }

    private fun pickRecommendation(
        dailyTotals: NutritionTotalsEntity,
        targets: NutritionTargets,
        fitnessGoal: String,
        mealProgress: MealProgressState,
        workoutDoneToday: Boolean,
        dietAndWorkoutEnabled: Boolean,
    ): String {
        if (mealProgress.isComplete) {
            return dailyReviewRecommendation(dailyTotals, targets, fitnessGoal)
        }
        val gaps = macroGaps(dailyTotals, targets)
        val primary = gaps.maxByOrNull { it.relativeGap } ?: return SystemMessages.nutritionBalanced()
        val base = when (primary.metric) {
            "protein" -> SystemMessages.proteinBelowTarget()
            "carbs" -> SystemMessages.carbsBelowTarget()
            "fat" -> SystemMessages.fatHighToday()
            "calories" -> calorieGuidance(dailyTotals.calories, targets.calorieTarget, fitnessGoal)
            else -> SystemMessages.nutritionBalanced()
        }
        if (dietAndWorkoutEnabled && workoutDoneToday && primary.metric == "protein") {
            return SystemMessages.proteinRecoveryAfterWorkout()
        }
        return base
    }

    private fun dailyReviewRecommendation(
        dailyTotals: NutritionTotalsEntity,
        targets: NutritionTargets,
        fitnessGoal: String,
    ): String {
        val calBand = macroBand(dailyTotals.calories, targets.calorieTarget)
        val proteinBand = macroBand(dailyTotals.protein, targets.proteinTarget)
        return when {
            isFatLossGoal(fitnessGoal) && calBand == MacroBand.Above ->
                SystemMessages.caloriesAboveTargetFatLoss()
            proteinBand == MacroBand.Below ->
                SystemMessages.proteinBelowTargetTomorrow()
            calBand == MacroBand.Near ->
                SystemMessages.nutritionCloseToTargetToday()
            else ->
                SystemMessages.mealTrackingCompleteToday()
        }
    }

    private data class MacroGap(val metric: String, val relativeGap: Float)

    private fun macroGaps(dailyTotals: NutritionTotalsEntity, targets: NutritionTargets): List<MacroGap> {
        val gaps = mutableListOf<MacroGap>()
        if (targets.proteinTarget > 0) {
            val pct = dailyTotals.protein.toFloat() / targets.proteinTarget
            if (pct < 0.9f) gaps += MacroGap("protein", 1f - pct)
        }
        if (targets.carbTarget > 0) {
            val pct = dailyTotals.carbs.toFloat() / targets.carbTarget
            if (pct < 0.9f) gaps += MacroGap("carbs", 1f - pct)
        }
        if (targets.fatTarget > 0) {
            val pct = dailyTotals.fat.toFloat() / targets.fatTarget
            if (pct > 1.1f) gaps += MacroGap("fat", pct - 1f)
        }
        if (targets.calorieTarget > 0) {
            val pct = dailyTotals.calories.toFloat() / targets.calorieTarget
            when {
                pct > 1.05f -> gaps += MacroGap("calories", pct - 1f)
                pct < 0.85f -> gaps += MacroGap("calories", 1f - pct)
            }
        }
        return gaps
    }

    private fun calorieGuidance(consumed: Int, target: Int, fitnessGoal: String): String =
        when (macroBand(consumed, target)) {
            MacroBand.Above -> if (isFatLossGoal(fitnessGoal)) {
                SystemMessages.caloriesAboveTargetFatLoss()
            } else {
                SystemMessages.caloriesNearLimit()
            }
            MacroBand.Near -> SystemMessages.caloriesNearLimit()
            MacroBand.Below -> SystemMessages.nutritionBalanced()
        }

    private fun macroBand(consumed: Int, target: Int): MacroBand {
        if (target <= 0) return MacroBand.Near
        val pct = consumed.toFloat() / target
        return when {
            pct < 0.9f -> MacroBand.Below
            pct > 1.1f -> MacroBand.Above
            else -> MacroBand.Near
        }
    }

    private fun isFatLossGoal(fitnessGoal: String): Boolean {
        val key = fitnessGoal.trim().lowercase()
        return key == "fat_loss" || key == "fat loss" || key == "cut"
    }
}
