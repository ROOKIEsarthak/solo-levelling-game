package com.example.solo_levelling.data.seed

data class FoodCatalogEntry(
    val id: String,
    val name: String,
    val category: String,
    val basis: String,
    val servingSizeG: Float? = null,
    val calories: Float,
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float,
    val fiberG: Float = 0f,
)

object FoodCatalog {
    val all: List<FoodCatalogEntry> = listOf(
        FoodCatalogEntry("oats", "Oats", "grain", "100g", null, 389f, 16.9f, 66.3f, 6.9f, 10.6f),
        FoodCatalogEntry("curd", "Curd", "dairy", "100g", null, 61f, 3.5f, 4.7f, 3.3f, 0f),
        FoodCatalogEntry("whey_protein", "Whey Protein", "supplement", "1_scoop", 30f, 120f, 24f, 3f, 2f, 0f),
        FoodCatalogEntry("low_fat_paneer", "Low Fat Paneer", "dairy", "100g", null, 145f, 25f, 3.5f, 4f, 0f),
        FoodCatalogEntry("egg_whole", "Whole Egg", "protein", "1_egg", 50f, 72f, 6.3f, 0.4f, 4.8f, 0f),
        FoodCatalogEntry("egg_white", "Egg White", "protein", "1_egg_white", 33f, 17f, 3.6f, 0.2f, 0.1f, 0f),
        FoodCatalogEntry("chicken_breast_raw", "Chicken Breast Raw", "meat", "100g", null, 120f, 22.5f, 0f, 2.6f, 0f),
        FoodCatalogEntry("chicken_breast_cooked", "Chicken Breast Cooked", "meat", "100g", null, 165f, 31f, 0f, 3.6f, 0f),
        FoodCatalogEntry("paneer", "Paneer", "dairy", "100g", null, 265f, 18.3f, 6.1f, 20.8f, 0f),
        FoodCatalogEntry("tofu", "Tofu", "protein", "100g", null, 76f, 8f, 1.9f, 4.8f, 0.3f),
        FoodCatalogEntry("soya_chunks", "Soya Chunks", "protein", "100g_dry", null, 345f, 52f, 33f, 0.5f, 13f),
        FoodCatalogEntry("banana", "Banana", "fruit", "100g", null, 89f, 1.1f, 22.8f, 0.3f, 2.6f),
        FoodCatalogEntry("papaya", "Papaya", "fruit", "100g", null, 43f, 0.5f, 10.8f, 0.3f, 1.7f),
        FoodCatalogEntry("pineapple", "Pineapple", "fruit", "100g", null, 50f, 0.5f, 13.1f, 0.1f, 1.4f),
        FoodCatalogEntry("cucumber", "Cucumber", "vegetable", "100g", null, 15f, 0.7f, 3.6f, 0.1f, 0.5f),
        FoodCatalogEntry("peanut_butter", "Peanut Butter", "fat", "100g", null, 588f, 25f, 20f, 50f, 6f),
        FoodCatalogEntry("chia_seeds", "Chia Seeds", "seed", "100g", null, 486f, 16.5f, 42.1f, 30.7f, 34.4f),
        FoodCatalogEntry("pumpkin_seeds", "Pumpkin Seeds", "seed", "100g", null, 559f, 30.2f, 10.7f, 49.1f, 6f),
        FoodCatalogEntry("cocoa_powder", "Cocoa Powder Unsweetened", "supplement", "100g", null, 228f, 19.6f, 57.9f, 13.1f, 29.8f),
        FoodCatalogEntry("roti", "Roti", "grain", "1_roti", 40f, 120f, 3.5f, 18f, 3f, 2.5f),
        FoodCatalogEntry("bread_white", "White Bread", "bread", "1_slice", 25f, 67f, 2.2f, 12.3f, 0.8f, 0.7f),
        FoodCatalogEntry("bread_brown", "Brown Bread", "bread", "1_slice", 25f, 62f, 3.3f, 10.3f, 1.1f, 1.8f),
        FoodCatalogEntry("white_rice_cooked", "White Rice Cooked", "grain", "100g", null, 130f, 2.7f, 28.2f, 0.3f, 0.4f),
        FoodCatalogEntry("mixed_vegetables", "Mixed Vegetables", "vegetable", "100g", null, 65f, 3f, 12f, 0.5f, 4f),
        FoodCatalogEntry("almonds", "Almonds", "nuts", "100g", null, 579f, 21.2f, 21.6f, 49.9f, 12.5f),
        FoodCatalogEntry("olive_oil", "Olive Oil", "oil", "100g", null, 884f, 0f, 0f, 100f, 0f),
    )

    fun findById(id: String): FoodCatalogEntry? = all.firstOrNull { it.id == id }

    fun search(query: String): List<FoodCatalogEntry> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return all
        return all.filter {
            it.name.lowercase().contains(q) ||
                it.category.lowercase().contains(q) ||
                it.id.contains(q)
        }
    }
}
