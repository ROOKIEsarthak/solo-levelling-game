package com.example.solo_levelling.domain.service

import com.example.solo_levelling.data.db.entity.LoggedExerciseEntity

object WorkoutProgressLogic {
    fun sessionVolume(exercises: List<LoggedExerciseEntity>): Double =
        exercises.sumOf { exercise ->
            exercise.sets.sumOf { (it.weight * it.reps).toDouble() }
        }

    fun isPr(currentMaxWeight: Float, previousMaxWeight: Float): Boolean =
        currentMaxWeight > previousMaxWeight && currentMaxWeight > 0f

    fun compareSets(previousBestWeight: Float, currentWeight: Float): String {
        val diff = currentWeight - previousBestWeight
        return when {
            diff > 0f -> "+${diff.toInt()}kg"
            diff < 0f -> "${diff.toInt()}kg"
            else -> "same"
        }
    }
}
