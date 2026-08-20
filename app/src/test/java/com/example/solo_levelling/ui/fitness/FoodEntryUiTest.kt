package com.example.solo_levelling.ui.fitness

import com.example.solo_levelling.data.db.entity.DietLogEntity
import com.example.solo_levelling.data.db.entity.FoodItemEntity
import com.example.solo_levelling.data.db.entity.LoggedExerciseEntity
import com.example.solo_levelling.data.db.entity.LoggedSetEntity
import com.example.solo_levelling.data.db.entity.MealEntity
import com.example.solo_levelling.data.db.entity.NutritionTotalsEntity
import com.example.solo_levelling.data.db.entity.WorkoutLogEntity
import com.example.solo_levelling.data.db.entity.WorkoutRestKind
import com.example.solo_levelling.data.seed.FoodCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodEntryUiTest {

    @Test
    fun p_postMealConfirmationLabels() {
        assertEquals("DONE", postMealDoneLabel())
        assertEquals("ADD ANOTHER FOOD", postMealAddAnotherLabel())
        assertEquals("TODAY'S NUTRITION", mealProgressHeaderLabel())
        assertEquals("✓", mealSlotIndicator(logged = true))
        assertEquals("+", mealSlotIndicator(logged = false))
    }

    @Test
    fun p_addModeUsesAddTitleAndConfirmLabel() {
        assertEquals("ADD FOOD", foodEntryDialogTitle(isEdit = false))
        assertEquals("ADD FOOD", foodEntryConfirmLabel(isEdit = false))
    }

    @Test
    fun p_editModeUsesEditTitleAndSaveChanges() {
        assertEquals("EDIT FOOD", foodEntryDialogTitle(isEdit = true))
        assertEquals("SAVE CHANGES", foodEntryConfirmLabel(isEdit = true))
    }

    @Test
    fun p_othersModeAcceptsValidManualFood() {
        val err = validateFoodEntry(
            catalogMode = false,
            catalogSelected = false,
            foodName = "Chicken breast",
            foodQty = "200",
            foodUnit = "g",
            foodCal = "330",
            foodP = "62",
            foodC = "0",
            foodF = "7",
        )
        assertNull(err)
    }

    @Test
    fun p_catalogModeAcceptsSelectionWithMacros() {
        val err = validateFoodEntry(
            catalogMode = true,
            catalogSelected = true,
            foodName = "Whole Egg",
            foodQty = "7",
            foodUnit = "egg",
            foodCal = "504",
            foodP = "57",
            foodC = "4",
            foodF = "38",
        )
        assertNull(err)
    }

    @Test
    fun p_foodItemFromForm_addUsesZeroId() {
        val food = foodItemFromForm(
            id = 0L,
            name = "  Rice  ",
            quantity = "150",
            unit = " g ",
            calories = "180",
            protein = "4",
            carbs = "40",
            fat = "1",
        )
        assertEquals(0L, food.id)
        assertEquals("Rice", food.name)
        assertEquals(150f, food.quantity)
        assertEquals("g", food.unit)
        assertEquals(180, food.calories)
        assertEquals(4, food.protein)
        assertEquals(40, food.carbs)
        assertEquals(1, food.fat)
    }

    @Test
    fun p_foodItemFromForm_editKeepsExistingId() {
        val food = foodItemFromForm(
            id = 42L,
            name = "Rice",
            quantity = "150",
            unit = "g",
            calories = "180",
            protein = "4",
            carbs = "40",
            fat = "1",
        )
        assertEquals(42L, food.id)
    }

    @Test
    fun p_dietHeaderTotals_usesSelectedLogWhenMealsExist() {
        val log = DietLogEntity(
            date = "2026-08-17",
            meals = listOf(MealEntity(id = 1, name = "Lunch", foods = listOf(sampleFood()))),
            dailyTotals = NutritionTotalsEntity(calories = 500, protein = 40, carbs = 20, fat = 10),
        )
        val totals = dietHeaderTotals(log)
        assertNotNull(totals)
        assertEquals(500, totals!!.calories)
        assertEquals(40, totals.protein)
    }

    @Test
    fun p_emptyMealAndDeleteMealCopy() {
        assertEquals("No food logged yet.", FOOD_ENTRY_EMPTY_MEAL)
        assertEquals("Delete Lunch?", deleteMealConfirmPrompt("Lunch"))
        assertEquals(
            "This will remove the meal and all food entries inside it.",
            DELETE_MEAL_CONFIRM_DETAIL,
        )
    }

    @Test
    fun p_workoutAndDietProgressImpactFlags() {
        val workout = WorkoutLogEntity(
            date = "2026-08-15",
            exercises = listOf(LoggedExerciseEntity(name = "Squat", sets = listOf(LoggedSetEntity(60f, 5)))),
        )
        assertTrue(workoutLogAffectsProgress(workout))
        assertFalse(workoutLogAffectsProgress(WorkoutLogEntity(date = "2026-08-15")))
        assertTrue(workoutLogAffectsProgress(WorkoutLogEntity(date = "2026-08-15", restKind = WorkoutRestKind.ACTIVE_REST)))
        assertFalse(workoutLogAffectsProgress(WorkoutLogEntity(date = "2026-08-15", restKind = WorkoutRestKind.COMPLETE_REST)))
        val meal = MealEntity(id = 1, name = "Lunch", foods = listOf(sampleFood()))
        assertTrue(mealAffectsProgress(meal))
        assertFalse(mealAffectsProgress(MealEntity(id = 2, name = "Empty")))
        assertTrue(dietLogAffectsProgress(DietLogEntity(date = "2026-08-15", meals = listOf(meal))))
        assertFalse(dietLogAffectsProgress(DietLogEntity(date = "2026-08-15")))
    }

    @Test
    fun p_deletingLastFoodReversesProgress() {
        val log = DietLogEntity(
            date = "2026-08-15",
            meals = listOf(
                MealEntity(id = 1, name = "Breakfast", foods = listOf(sampleFood())),
                MealEntity(id = 2, name = "Lunch", foods = listOf(sampleFood().copy(id = 10L, name = "Rice"))),
                MealEntity(id = 3, name = "Dinner", foods = listOf(sampleFood().copy(id = 11L, name = "Chicken"))),
            ),
        )
        assertTrue(deletingFoodReversesProgress(log, mealId = 3, foodId = 11L))
        assertTrue(deletingMealReversesProgress(log, mealId = 3))
    }

    @Test
    fun n_deletingOneOfSeveralFoodsKeepsProgress() {
        val other = sampleFood().copy(id = 10L, name = "Oats")
        val log = DietLogEntity(
            date = "2026-08-15",
            meals = listOf(
                MealEntity(id = 1, name = "Lunch", foods = listOf(sampleFood(), other)),
            ),
        )
        assertFalse(deletingFoodReversesProgress(log, mealId = 1, foodId = 9L))
        assertTrue(dietLogAffectsProgress(log))
    }

    @Test
    fun n_deletingMealKeepsProgressWhenOtherMealsHaveFood() {
        val log = DietLogEntity(
            date = "2026-08-15",
            meals = listOf(
                MealEntity(id = 1, name = "Lunch", foods = listOf(sampleFood())),
                MealEntity(id = 2, name = "Dinner", foods = listOf(sampleFood().copy(id = 11L))),
            ),
        )
        assertFalse(deletingMealReversesProgress(log, mealId = 1))
        assertFalse(deletingFoodReversesProgress(log, mealId = 1, foodId = 9L))
    }

    @Test
    fun e_deletingUnknownOrEmptyMealDoesNotClaimProgress() {
        val empty = DietLogEntity(
            date = "2026-08-15",
            meals = listOf(MealEntity(id = 1, name = "Lunch")),
        )
        assertFalse(deletingMealReversesProgress(empty, mealId = 1))
        assertFalse(deletingMealReversesProgress(empty, mealId = 99))
        assertFalse(deletingFoodReversesProgress(empty, mealId = 1, foodId = 9L))
        assertFalse(deletingFoodReversesProgress(null, mealId = 1, foodId = 9L))
    }

    @Test
    fun n_catalogModeRequiresSelection() {
        val err = validateFoodEntry(
            catalogMode = true,
            catalogSelected = false,
            foodName = "",
            foodQty = "1",
            foodUnit = "g",
            foodCal = "10",
            foodP = "1",
            foodC = "1",
            foodF = "1",
        )
        assertEquals("Select a food from the catalog", err?.message)
        assertEquals(FoodEntryField.Catalog, err?.field)
    }

    @Test
    fun n_othersModeRejectsBlankName() {
        val err = validateFoodEntry(
            catalogMode = false,
            catalogSelected = false,
            foodName = "  ",
            foodQty = "100",
            foodUnit = "g",
            foodCal = "10",
            foodP = "1",
            foodC = "1",
            foodF = "1",
        )
        assertEquals("Enter food name", err?.message)
        assertEquals(FoodEntryField.Name, err?.field)
    }

    @Test
    fun n_rejectsInvalidQuantity() {
        val err = validateFoodEntry(
            catalogMode = false,
            catalogSelected = false,
            foodName = "Oats",
            foodQty = "abc",
            foodUnit = "g",
            foodCal = "10",
            foodP = "1",
            foodC = "1",
            foodF = "1",
        )
        assertEquals("Enter a valid quantity", err?.message)
        assertEquals(FoodEntryField.Quantity, err?.field)
    }

    @Test
    fun n_rejectsNegativeCalories() {
        val err = validateFoodEntry(
            catalogMode = false,
            catalogSelected = false,
            foodName = "Oats",
            foodQty = "40",
            foodUnit = "g",
            foodCal = "-1",
            foodP = "1",
            foodC = "1",
            foodF = "1",
        )
        assertEquals("calories cannot be negative", err?.message)
        assertEquals(FoodEntryField.Calories, err?.field)
    }

    @Test
    fun e_zeroCaloriesAndMacrosAreAllowed() {
        val err = validateFoodEntry(
            catalogMode = false,
            catalogSelected = false,
            foodName = "Black coffee",
            foodQty = "1",
            foodUnit = "cup",
            foodCal = "0",
            foodP = "0",
            foodC = "0",
            foodF = "0",
        )
        assertNull(err)
    }

    @Test
    fun e_zeroQuantityIsRejected() {
        val err = validateFoodEntry(
            catalogMode = false,
            catalogSelected = false,
            foodName = "Oats",
            foodQty = "0",
            foodUnit = "g",
            foodCal = "10",
            foodP = "1",
            foodC = "1",
            foodF = "1",
        )
        assertEquals("quantity must be greater than 0", err?.message)
        assertEquals(FoodEntryField.Quantity, err?.field)
    }

    @Test
    fun e_foodQuantityText_stripsWholeNumberDecimals() {
        assertEquals("", foodQuantityText(null))
        assertEquals("7", foodQuantityText(7.0f))
        assertEquals("1.5", foodQuantityText(1.5f))
    }

    @Test
    fun e_dietHeaderTotals_nullWhenNoMeals() {
        assertNull(dietHeaderTotals(null))
        assertNull(
            dietHeaderTotals(
                DietLogEntity(
                    date = "2026-08-17",
                    meals = emptyList(),
                    dailyTotals = NutritionTotalsEntity(calories = 0),
                ),
            ),
        )
    }

    @Test
    fun r_headerTotalsFollowSelectedDateNotAStaleTodaySnapshot() {
        val selected = DietLogEntity(
            date = "2026-08-16",
            meals = listOf(MealEntity(id = 1, name = "Dinner", foods = listOf(sampleFood()))),
            dailyTotals = NutritionTotalsEntity(calories = 900),
        )
        val todaySnapshot = NutritionTotalsEntity(calories = 100)
        val shown = dietHeaderTotals(selected)
        assertEquals(900, shown?.calories)
        assertEquals(false, shown?.calories == todaySnapshot.calories)
    }

    @Test
    fun p_mealsForCategory_returnsMatchingName() {
        val meals = listOf(
            MealEntity(id = 1, name = "Breakfast", foods = listOf(sampleFood())),
            MealEntity(id = 2, name = "Lunch"),
        )
        val result = mealsForCategory(meals, "Breakfast")
        assertEquals(1, result.size)
        assertEquals(1L, result[0].id)
    }

    @Test
    fun n_mealsForCategory_returnsEmptyWhenNoMatch() {
        val meals = listOf(MealEntity(id = 1, name = "Lunch"))
        assertEquals(emptyList<MealEntity>(), mealsForCategory(meals, "Breakfast"))
    }

    @Test
    fun e_mealsForCategory_matchesTrimAndCaseAndKeepsDuplicates() {
        val meals = listOf(
            MealEntity(id = 3, name = "  breakfast "),
            MealEntity(id = 4, name = "Breakfast"),
        )
        assertEquals(listOf(3L, 4L), mealsForCategory(meals, "Breakfast").map { it.id })
        assertEquals(emptyList<MealEntity>(), mealsForCategory(meals, "  "))
        assertEquals(emptyList<MealEntity>(), mealsForCategory(meals, ""))
    }

    @Test
    fun p_extraDietCategoryChips_includesCustomNames() {
        val meals = listOf(
            MealEntity(id = 1, name = "Breakfast"),
            MealEntity(id = 2, name = "Pre-workout"),
        )
        assertEquals(listOf("Pre-workout"), extraDietCategoryChips(meals))
        assertEquals(
            listOf("Breakfast", "Lunch", "Dinner", "Snack", "Pre-workout"),
            dietCategoryChips(meals),
        )
    }

    @Test
    fun n_extraDietCategoryChips_skipsDefaults() {
        val meals = listOf(
            MealEntity(id = 1, name = "Lunch"),
            MealEntity(id = 2, name = "dinner"),
        )
        assertEquals(emptyList<String>(), extraDietCategoryChips(meals))
    }

    @Test
    fun e_extraDietCategoryChips_dedupesAndSkipsBlank() {
        val meals = listOf(
            MealEntity(id = 1, name = "Pre-workout"),
            MealEntity(id = 2, name = "pre-workout"),
            MealEntity(id = 3, name = "  "),
            MealEntity(id = 4, name = "Post-workout"),
        )
        assertEquals(listOf("Pre-workout", "Post-workout"), extraDietCategoryChips(meals))
        assertEquals(emptyList<String>(), extraDietCategoryChips(emptyList()))
    }

    @Test
    fun p_repeatableMeals_includesTodayAndOtherDays() {
        val today = DietLogEntity(
            date = "2026-08-18",
            meals = listOf(MealEntity(id = 9, name = "Breakfast", foods = listOf(sampleFood()))),
        )
        val prior = DietLogEntity(
            date = "2026-08-17",
            meals = listOf(MealEntity(id = 2, name = "Lunch", foods = listOf(sampleFood().copy(id = 10)))),
        )
        val options = repeatableMeals(listOf(today, prior))
        assertEquals(listOf(9L, 2L), options.map { it.meal.id })
        assertEquals("Breakfast · today", repeatMealOptionLabel(options[0], "2026-08-18"))
        assertEquals("Lunch · 2026-08-17", repeatMealOptionLabel(options[1], "2026-08-18"))
    }

    @Test
    fun n_repeatableMeals_emptyWhenNoFoods() {
        val empty = DietLogEntity(
            date = "2026-08-18",
            meals = listOf(MealEntity(id = 1, name = "Breakfast")),
        )
        assertEquals(emptyList<RepeatMealOption>(), repeatableMeals(listOf(empty)))
        assertEquals(emptyList<RepeatMealOption>(), repeatableMeals(emptyList()))
    }

    @Test
    fun e_repeatableMeals_skipsEmptyFoodMealsAndKeepsFilled() {
        val today = DietLogEntity(
            date = "2026-08-18",
            meals = listOf(
                MealEntity(id = 1, name = "Breakfast"),
                MealEntity(id = 2, name = "Lunch", foods = listOf(sampleFood())),
            ),
        )
        val options = repeatableMeals(listOf(today))
        assertEquals(listOf(2L), options.map { it.meal.id })
    }

    @Test
    fun p_dietDailySummary_usesDailyTotals() {
        val log = DietLogEntity(
            date = "2026-08-18",
            meals = listOf(MealEntity(id = 1, name = "Lunch", foods = listOf(sampleFood()))),
            dailyTotals = NutritionTotalsEntity(calories = 504, protein = 44, carbs = 3, fat = 34),
        )
        val summary = dietDailySummary(log)
        assertEquals(504, summary.calories)
        assertEquals(44, summary.protein)
        assertEquals(3, summary.carbs)
        assertEquals(34, summary.fat)
    }

    @Test
    fun e_dietDailySummary_zerosWhenEmpty() {
        val empty = dietDailySummary(null)
        assertEquals(0, empty.calories)
        assertEquals(0, empty.protein)
        assertEquals(0, empty.carbs)
        assertEquals(0, empty.fat)
        val noMeals = dietDailySummary(
            DietLogEntity(date = "2026-08-18", meals = emptyList()),
        )
        assertEquals(0, noMeals.calories)
    }

    @Test
    fun p_quickPicksMatchImageLabelsAndCatalogIds() {
        assertEquals(listOf("Eggs", "Rice", "Chicken", "Oats"), FOOD_QUICK_PICKS.map { it.label })
        FOOD_QUICK_PICKS.forEach { pick ->
            val entry = FoodCatalog.findById(pick.id)
            assertNotNull(entry)
            assertEquals(pick.id, entry!!.id)
        }
    }

    @Test
    fun p_catalogEntryForFoodName_matchesIgnoreCase() {
        val entry = catalogEntryForFoodName("  whole egg  ")
        assertEquals("egg_whole", entry?.id)
        assertEquals("oats", catalogEntryForFoodName("Oats")?.id)
    }

    @Test
    fun n_catalogEntryForFoodName_nullWhenUnknown() {
        assertNull(catalogEntryForFoodName("mystery stew"))
        assertNull(catalogEntryForFoodName("  "))
        assertNull(catalogEntryForFoodName(""))
    }

    @Test
    fun e_foodAmountUnits_keepsDefaultsAndPrependsCustom() {
        assertEquals(FOOD_AMOUNT_UNITS, foodAmountUnits("g"))
        assertEquals(FOOD_AMOUNT_UNITS, foodAmountUnits("EGG"))
        assertEquals("cup", foodAmountUnits("cup").first())
        assertEquals(FOOD_AMOUNT_UNITS, foodAmountUnits("  "))
    }

    @Test
    fun p_dayMealStatuses_marksLoggedAndLeft() {
        val meals = listOf(
            MealEntity(id = 1, name = "Breakfast", foods = listOf(sampleFood())),
        )
        val statuses = dayMealStatuses(meals, ::sumMealFoods)
        assertEquals(listOf("Breakfast", "Lunch", "Dinner", "Snack"), statuses.map { it.name })
        assertEquals(true, statuses[0].logged)
        assertEquals(180, statuses[0].calories)
        assertEquals(4, statuses[0].protein)
        assertEquals(false, statuses[1].logged)
        assertEquals(false, statuses[2].logged)
        assertEquals(false, statuses[3].logged)
    }

    @Test
    fun n_dayMealStatuses_emptyFoodsCountAsLeft() {
        val meals = listOf(MealEntity(id = 1, name = "Breakfast"))
        val statuses = dayMealStatuses(meals, ::sumMealFoods)
        assertEquals(false, statuses.first { it.name == "Breakfast" }.logged)
        assertEquals(0, statuses.first { it.name == "Breakfast" }.calories)
    }

    @Test
    fun e_dayMealStatuses_includesExtrasAndSumsDuplicateNames() {
        val meals = listOf(
            MealEntity(id = 1, name = "Breakfast", foods = listOf(sampleFood())),
            MealEntity(id = 2, name = "Breakfast", foods = listOf(sampleFood().copy(id = 10))),
            MealEntity(id = 3, name = "Pre-workout", foods = listOf(sampleFood().copy(id = 11))),
        )
        val statuses = dayMealStatuses(meals, ::sumMealFoods)
        assertEquals(listOf("Breakfast", "Lunch", "Dinner", "Snack", "Pre-workout"), statuses.map { it.name })
        assertEquals(360, statuses.first { it.name == "Breakfast" }.calories)
        assertEquals(true, statuses.first { it.name == "Pre-workout" }.logged)
        assertEquals(180, statuses.first { it.name == "Pre-workout" }.calories)
    }

    private fun sampleFood(): FoodItemEntity = FoodItemEntity(
        id = 9L,
        name = "Rice",
        quantity = 150f,
        unit = "g",
        calories = 180,
        protein = 4,
        carbs = 40,
        fat = 1,
    )

    private fun sumMealFoods(meal: MealEntity): NutritionTotalsEntity =
        NutritionTotalsEntity(
            calories = meal.foods.sumOf { it.calories ?: 0 },
            protein = meal.foods.sumOf { it.protein ?: 0 },
            carbs = meal.foods.sumOf { it.carbs ?: 0 },
            fat = meal.foods.sumOf { it.fat ?: 0 },
        )
}
