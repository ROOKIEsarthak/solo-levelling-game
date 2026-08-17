package com.example.solo_levelling.domain.service

import kotlin.math.roundToInt

data class Macros(
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
)

object NutritionCalc {
    fun bmi(heightCm: Double, weightKg: Double): Double {
        if (heightCm <= 0 || weightKg <= 0) return 0.0
        val heightM = heightCm / 100.0
        return weightKg / (heightM * heightM)
    }

    fun bmiCategory(bmi: Double): String = when {
        bmi <= 0 -> "Unknown"
        bmi < 18.5 -> "Underweight (estimate)"
        bmi < 25.0 -> "Normal (estimate)"
        bmi < 30.0 -> "Overweight (estimate)"
        else -> "Obese (estimate)"
    }

    fun bmrMifflin(sex: String, age: Int, heightCm: Double, weightKg: Double): Double {
        val base = 10.0 * weightKg + 6.25 * heightCm - 5.0 * age
        return when (sex.trim().lowercase()) {
            "female", "f" -> base - 161.0
            else -> base + 5.0
        }
    }

    fun tdee(bmr: Double, activityLevel: String): Double {
        val multiplier = when (activityLevel.trim().lowercase()) {
            "sedentary" -> 1.2
            "light" -> 1.375
            "moderate" -> 1.55
            "active" -> 1.725
            "very_active", "very active" -> 1.9
            else -> 1.2
        }
        return bmr * multiplier
    }

    fun goalCalories(tdee: Double, fitnessGoal: String): Int {
        val adjustment = when (fitnessGoal.trim().lowercase()) {
            "fat_loss", "fat loss", "cut" -> -500
            "muscle_gain", "muscle gain", "bulk" -> 300
            else -> 0
        }
        return (tdee + adjustment).roundToInt().coerceAtLeast(1200)
    }

    fun macroTargets(weightKg: Double, goalCalories: Int, fitnessGoal: String): Macros {
        val proteinPerKg = when (fitnessGoal.trim().lowercase()) {
            "fat_loss", "fat loss", "cut" -> 2.2
            "muscle_gain", "muscle gain", "bulk" -> 2.0
            else -> 1.6
        }
        val proteinG = (weightKg * proteinPerKg).roundToInt().coerceAtLeast(0)
        val fatCalories = goalCalories * 0.25
        val fatG = (fatCalories / 9.0).roundToInt().coerceAtLeast(0)
        val carbCalories = (goalCalories - proteinG * 4 - fatG * 9).coerceAtLeast(0)
        val carbsG = (carbCalories / 4.0).roundToInt()
        return Macros(proteinG = proteinG, carbsG = carbsG, fatG = fatG)
    }
}
