package com.example.solo_levelling.domain.service

import com.example.solo_levelling.data.seed.FoodCatalogEntry
import kotlin.math.roundToInt

data class ScaledFoodMacros(
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val fiber: Int,
    val defaultUnit: String,
    val defaultQuantity: Float,
)

object FoodMacroScaler {
    fun isPer100g(basis: String): Boolean =
        basis.startsWith("100g", ignoreCase = true)

    fun defaultUnit(entry: FoodCatalogEntry): String = when {
        isPer100g(entry.basis) -> "g"
        entry.basis.equals("1_scoop", ignoreCase = true) -> "scoop"
        entry.basis.equals("1_egg", ignoreCase = true) -> "egg"
        entry.basis.equals("1_egg_white", ignoreCase = true) -> "egg white"
        entry.basis.equals("1_roti", ignoreCase = true) -> "roti"
        entry.basis.equals("1_slice", ignoreCase = true) -> "slice"
        else -> entry.basis.removePrefix("1_").replace('_', ' ')
    }

    fun defaultQuantity(entry: FoodCatalogEntry): Float =
        if (isPer100g(entry.basis)) 100f else 1f

    fun scale(entry: FoodCatalogEntry, quantity: Float): ScaledFoodMacros {
        if (quantity <= 0f) {
            return ScaledFoodMacros(0, 0, 0, 0, 0, defaultUnit(entry), quantity)
        }
        val factor = if (isPer100g(entry.basis)) {
            quantity / 100f
        } else {
            quantity
        }
        return ScaledFoodMacros(
            calories = (entry.calories * factor).roundToInt(),
            protein = (entry.proteinG * factor).roundToInt(),
            carbs = (entry.carbsG * factor).roundToInt(),
            fat = (entry.fatG * factor).roundToInt(),
            fiber = (entry.fiberG * factor).roundToInt(),
            defaultUnit = defaultUnit(entry),
            defaultQuantity = quantity,
        )
    }

    fun previewLine(entry: FoodCatalogEntry, quantity: Float): String {
        val scaled = scale(entry, quantity)
        val unit = scaled.defaultUnit
        val qtyLabel = if (quantity == quantity.toLong().toFloat()) {
            quantity.toLong().toString()
        } else {
            quantity.toString()
        }
        return "$qtyLabel$unit ${entry.name} → ${scaled.calories} kcal · " +
            "${scaled.protein}P · ${scaled.carbs}C · ${scaled.fat}F" +
            if (scaled.fiber > 0) " · ${scaled.fiber}g fiber" else ""
    }
}
