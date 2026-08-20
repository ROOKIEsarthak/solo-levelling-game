package com.example.solo_levelling.ui.fitness

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.data.db.entity.DietLogEntity
import com.example.solo_levelling.data.db.entity.FoodItemEntity
import com.example.solo_levelling.data.db.entity.LoggedExerciseEntity
import com.example.solo_levelling.data.db.entity.LoggedSetEntity
import com.example.solo_levelling.data.db.entity.MealEntity
import com.example.solo_levelling.data.db.entity.NutritionTotalsEntity
import com.example.solo_levelling.data.db.entity.PlannedExerciseEntity
import com.example.solo_levelling.data.db.entity.RepRangeEntity
import com.example.solo_levelling.data.db.entity.WorkoutDayPlanEntity
import com.example.solo_levelling.data.db.entity.WorkoutLogEntity
import com.example.solo_levelling.data.db.entity.WorkoutRestKind
import com.example.solo_levelling.data.db.entity.WorkoutRoutineEntity
import com.example.solo_levelling.data.seed.FoodCatalog
import com.example.solo_levelling.data.seed.FoodCatalogEntry
import com.example.solo_levelling.data.seed.WorkoutCatalog
import com.example.solo_levelling.domain.copy.SystemMessages
import com.example.solo_levelling.domain.logic.MealProgressState
import com.example.solo_levelling.domain.service.PostMealFeedback
import com.example.solo_levelling.domain.service.EntryValidation
import com.example.solo_levelling.domain.service.FoodMacroScaler
import com.example.solo_levelling.domain.service.ModuleService
import com.example.solo_levelling.domain.service.WorkoutSplitLogic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.example.solo_levelling.ui.components.CyberProgressBar
import com.example.solo_levelling.ui.components.EnergyFieldBackground
import com.example.solo_levelling.ui.components.GhostTextButton
import com.example.solo_levelling.ui.components.GlassLevel
import com.example.solo_levelling.ui.components.GlassSurface
import com.example.solo_levelling.ui.components.SovereignChip
import com.example.solo_levelling.ui.components.SystemActionButton
import com.example.solo_levelling.ui.components.SystemConfirmDialog
import com.example.solo_levelling.ui.components.SystemIdleEmpty
import com.example.solo_levelling.ui.components.SystemSectionHeader
import com.example.solo_levelling.ui.components.progressFraction
import com.example.solo_levelling.ui.theme.CyanAura
import com.example.solo_levelling.ui.theme.JetBrainsMono
import com.example.solo_levelling.ui.theme.Spacing
import com.example.solo_levelling.ui.theme.SystemError
import com.example.solo_levelling.ui.theme.SystemOutlineVariant
import com.example.solo_levelling.ui.theme.SystemPrimary
import com.example.solo_levelling.ui.theme.SystemSurface
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

private val WEEK_DAYS = listOf(
    "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
)

internal fun defaultTrainingTab(workoutSplitId: String?): String = "Today"

internal fun resolvedTrainingTab(userTab: String?, workoutSplitId: String?): String =
    userTab ?: defaultTrainingTab(workoutSplitId)

internal const val FOOD_ENTRY_EMPTY_MEAL = "No food logged yet."
internal const val DELETE_MEAL_CONFIRM_DETAIL =
    "This will remove the meal and all food entries inside it."

internal fun workoutLogAffectsProgress(log: WorkoutLogEntity?): Boolean =
    log != null && (
        log.exercises.any { it.sets.isNotEmpty() } ||
            log.restKind == WorkoutRestKind.ACTIVE_REST
    )

internal fun dietLogAffectsProgress(log: DietLogEntity?): Boolean =
    com.example.solo_levelling.domain.logic.MealCompletionPolicy.dietLogAffectsProgress(log)

internal fun mealAffectsProgress(meal: MealEntity): Boolean =
    com.example.solo_levelling.domain.logic.MealCompletionPolicy.isValidMeal(meal)

internal fun deletingFoodReversesProgress(log: DietLogEntity?, mealId: Long, foodId: Long): Boolean =
    com.example.solo_levelling.domain.logic.MealCompletionPolicy.deletingFoodReversesProgress(log, mealId, foodId)

internal fun deletingMealReversesProgress(log: DietLogEntity?, mealId: Long): Boolean =
    com.example.solo_levelling.domain.logic.MealCompletionPolicy.deletingMealReversesProgress(log, mealId)

internal enum class FoodEntryField {
    Catalog, Name, Quantity, Unit, Calories, Protein, Carbs, Fat
}

internal data class FoodEntryError(
    val message: String,
    val field: FoodEntryField,
)

internal fun foodEntryDialogTitle(isEdit: Boolean): String =
    if (isEdit) "EDIT FOOD" else "ADD FOOD"

internal fun foodEntryConfirmLabel(isEdit: Boolean): String =
    if (isEdit) "SAVE CHANGES" else "ADD FOOD"

internal data class FoodQuickPick(val id: String, val label: String)

internal val FOOD_QUICK_PICKS = listOf(
    FoodQuickPick("egg_whole", "Eggs"),
    FoodQuickPick("white_rice_cooked", "Rice"),
    FoodQuickPick("chicken_breast_cooked", "Chicken"),
    FoodQuickPick("oats", "Oats"),
)

internal val FOOD_AMOUNT_UNITS = listOf("g", "scoop", "egg", "slice", "roti")

internal fun foodAmountUnits(current: String): List<String> {
    val unit = current.trim()
    if (unit.isEmpty() || FOOD_AMOUNT_UNITS.any { it.equals(unit, ignoreCase = true) }) {
        return FOOD_AMOUNT_UNITS
    }
    return listOf(unit) + FOOD_AMOUNT_UNITS
}

internal fun catalogEntryForFoodName(name: String): FoodCatalogEntry? {
    val key = name.trim()
    if (key.isEmpty()) return null
    return FoodCatalog.all.firstOrNull { it.name.equals(key, ignoreCase = true) }
}

internal fun deleteMealConfirmPrompt(mealName: String): String = "Delete $mealName?"

internal fun foodQuantityText(quantity: Float?): String {
    if (quantity == null) return ""
    val asInt = quantity.toInt()
    return if (quantity == asInt.toFloat()) asInt.toString() else quantity.toString()
}

internal fun dietHeaderTotals(selectedLog: DietLogEntity?): NutritionTotalsEntity? {
    if (selectedLog == null || selectedLog.meals.isEmpty()) return null
    return selectedLog.dailyTotals
}

internal val DIET_MEAL_CATEGORIES = listOf("Breakfast", "Lunch", "Dinner", "Snack")

internal fun mealsForCategory(meals: List<MealEntity>, category: String): List<MealEntity> {
    val key = category.trim()
    if (key.isEmpty()) return emptyList()
    return meals.filter { it.name.trim().equals(key, ignoreCase = true) }
}

internal fun extraDietCategoryChips(
    meals: List<MealEntity>,
    defaults: List<String> = DIET_MEAL_CATEGORIES,
): List<String> {
    val defaultKeys = defaults.map { it.trim().lowercase() }.toSet()
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

internal fun dietCategoryChips(meals: List<MealEntity>): List<String> =
    DIET_MEAL_CATEGORIES + extraDietCategoryChips(meals)

internal data class DayMealStatus(
    val name: String,
    val logged: Boolean,
    val calories: Int = 0,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fat: Int = 0,
)

internal fun dayMealStatuses(
    meals: List<MealEntity>,
    mealTotals: (MealEntity) -> NutritionTotalsEntity,
): List<DayMealStatus> =
    dietCategoryChips(meals).map { name ->
        val loggedMeals = mealsForCategory(meals, name).filter { it.foods.isNotEmpty() }
        if (loggedMeals.isEmpty()) {
            DayMealStatus(name = name, logged = false)
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
            DayMealStatus(
                name = name,
                logged = true,
                calories = totals.calories,
                protein = totals.protein,
                carbs = totals.carbs,
                fat = totals.fat,
            )
        }
    }

internal data class RepeatMealOption(val date: String, val meal: MealEntity)

internal fun repeatableMeals(history: List<DietLogEntity>): List<RepeatMealOption> =
    history.flatMap { day ->
        day.meals.filter { it.foods.isNotEmpty() }.map { RepeatMealOption(day.date, it) }
    }

internal fun repeatMealOptionLabel(option: RepeatMealOption, currentDate: String): String =
    if (option.date == currentDate) "${option.meal.name} · today"
    else "${option.meal.name} · ${option.date}"

internal fun dietDailySummary(selectedLog: DietLogEntity?): NutritionTotalsEntity =
    selectedLog?.dailyTotals ?: NutritionTotalsEntity()

internal fun validateFoodEntry(
    catalogMode: Boolean,
    catalogSelected: Boolean,
    foodName: String,
    foodQty: String,
    foodUnit: String,
    foodCal: String,
    foodP: String,
    foodC: String,
    foodF: String,
): FoodEntryError? {
    if (catalogMode && !catalogSelected) {
        return FoodEntryError("Select a food from the catalog", FoodEntryField.Catalog)
    }
    if (catalogMode) {
        EntryValidation.requirePositiveFloat(foodQty, "quantity")?.let {
            return FoodEntryError(it, FoodEntryField.Quantity)
        }
        EntryValidation.requireNonBlank(foodName, "food name")?.let {
            return FoodEntryError(it, FoodEntryField.Name)
        }
        EntryValidation.requireNonBlank(foodUnit, "unit")?.let {
            return FoodEntryError(it, FoodEntryField.Unit)
        }
    } else {
        EntryValidation.requireNonBlank(foodName, "food name")?.let {
            return FoodEntryError(it, FoodEntryField.Name)
        }
        EntryValidation.requirePositiveFloat(foodQty, "quantity")?.let {
            return FoodEntryError(it, FoodEntryField.Quantity)
        }
        EntryValidation.requireNonBlank(foodUnit, "unit")?.let {
            return FoodEntryError(it, FoodEntryField.Unit)
        }
    }
    EntryValidation.requireNonNegativeInt(foodCal, "calories")?.let {
        return FoodEntryError(it, FoodEntryField.Calories)
    }
    EntryValidation.requireNonNegativeInt(foodP, "protein")?.let {
        return FoodEntryError(it, FoodEntryField.Protein)
    }
    EntryValidation.requireNonNegativeInt(foodC, "carbs")?.let {
        return FoodEntryError(it, FoodEntryField.Carbs)
    }
    EntryValidation.requireNonNegativeInt(foodF, "fat")?.let {
        return FoodEntryError(it, FoodEntryField.Fat)
    }
    return null
}

internal fun foodItemFromForm(
    id: Long,
    name: String,
    quantity: String,
    unit: String,
    calories: String,
    protein: String,
    carbs: String,
    fat: String,
): FoodItemEntity = FoodItemEntity(
    id = id,
    name = name.trim(),
    quantity = quantity.toFloat(),
    unit = unit.trim(),
    calories = calories.toInt(),
    protein = protein.toInt(),
    carbs = carbs.toInt(),
    fat = fat.toInt(),
)

internal const val WORKOUT_ENTRY_EMPTY_SETS = "No sets logged yet."

internal enum class WorkoutSetField { Weight, Reps, Rpe }

internal data class WorkoutSetDraft(
    val weight: String = "",
    val reps: String = "",
    val rpe: String = "",
)

internal data class WorkoutSetError(
    val message: String,
    val field: WorkoutSetField,
    val index: Int,
)

internal fun workoutEntryDialogTitle(hasSavedSets: Boolean): String =
    if (hasSavedSets) "EDIT SETS" else "LOG SETS"

internal fun workoutEntryConfirmLabel(hasSavedSets: Boolean): String =
    if (hasSavedSets) "SAVE CHANGES" else "SAVE SETS"

internal fun workoutEditActionLabel(exerciseName: String): String = "Edit $exerciseName"

internal fun exerciseSetProgressLabel(logged: Int, planned: Int?): String =
    if (planned != null && planned > 0) "$logged / $planned sets" else "$logged sets"

internal fun formatWorkoutNumber(value: Float): String {
    val asInt = value.toInt()
    return if (value == asInt.toFloat()) asInt.toString() else value.toString()
}

internal fun formatLoggedSetSummary(set: LoggedSetEntity): String {
    val rpe = set.rpe?.let { " @ RPE ${formatWorkoutNumber(it)}" } ?: ""
    return "${formatWorkoutNumber(set.weight)} kg × ${set.reps}$rpe"
}

internal fun formatPreviousSetsSummary(sets: List<LoggedSetEntity>): String =
    sets.joinToString("  ") { "${formatWorkoutNumber(it.weight)} kg × ${it.reps}" }

internal fun draftSetsFromExercise(sets: List<LoggedSetEntity>): List<WorkoutSetDraft> =
    sets.map { set ->
        WorkoutSetDraft(
            weight = formatWorkoutNumber(set.weight),
            reps = set.reps.toString(),
            rpe = set.rpe?.let { formatWorkoutNumber(it) }.orEmpty(),
        )
    }

internal fun initialSetDrafts(
    savedSets: List<LoggedSetEntity>,
    previous: LoggedSetEntity?,
): List<WorkoutSetDraft> {
    if (savedSets.isNotEmpty()) return draftSetsFromExercise(savedSets)
    if (previous != null) return draftSetsFromExercise(listOf(previous))
    return listOf(WorkoutSetDraft())
}

internal fun nextSetDraft(existing: List<WorkoutSetDraft>): WorkoutSetDraft {
    val last = existing.lastOrNull() ?: return WorkoutSetDraft()
    return WorkoutSetDraft(weight = last.weight, reps = last.reps, rpe = last.rpe)
}

internal fun removeSetDraft(existing: List<WorkoutSetDraft>, index: Int): List<WorkoutSetDraft> =
    existing.filterIndexed { i, _ -> i != index }

internal fun validateSetDraft(rows: List<WorkoutSetDraft>): WorkoutSetError? {
    rows.forEachIndexed { index, row ->
        EntryValidation.requirePositiveFloat(row.weight, "weight")?.let {
            return WorkoutSetError("Enter a valid weight.", WorkoutSetField.Weight, index)
        }
        EntryValidation.requirePositiveInt(row.reps, "reps")?.let {
            return WorkoutSetError("Enter the number of repetitions.", WorkoutSetField.Reps, index)
        }
        if (row.rpe.isNotBlank()) {
            EntryValidation.requirePositiveFloat(row.rpe, "RPE")?.let {
                return WorkoutSetError("Enter a valid RPE.", WorkoutSetField.Rpe, index)
            }
        }
    }
    return null
}

internal fun loggedSetFromForm(weight: String, reps: String, rpe: String): LoggedSetEntity =
    LoggedSetEntity(
        weight = weight.trim().toFloat(),
        reps = reps.trim().toInt(),
        rpe = rpe.trim().takeIf { it.isNotEmpty() }?.toFloatOrNull(),
    )

internal fun loggedSetsFromDraft(rows: List<WorkoutSetDraft>): List<LoggedSetEntity> =
    rows.map { loggedSetFromForm(it.weight, it.reps, it.rpe) }

internal fun postMealDoneLabel(): String = "DONE"

internal fun postMealAddAnotherLabel(): String = "ADD ANOTHER FOOD"

internal fun mealProgressHeaderLabel(): String = "TODAY'S NUTRITION"

internal fun mealSlotIndicator(logged: Boolean): String = if (logged) "✓" else "+"

enum class FitnessTab { Workout, Diet, Both }

@Composable
fun FitnessScreen(
    container: AppContainer,
    tab: FitnessTab = FitnessTab.Both,
    onMessage: (String) -> Unit = {},
    openReviewDate: String? = null,
    onReviewDateConsumed: () -> Unit = {},
) {
    val vm: FitnessViewModel = viewModel(factory = FitnessViewModel.factory(container))
    val workouts by vm.workouts.collectAsStateWithLifecycle()
    val workoutRoutine by vm.workoutRoutine.collectAsStateWithLifecycle()
    val dietLogs by vm.dietLogs.collectAsStateWithLifecycle()
    val selectedWorkoutLog by vm.selectedWorkoutLog.collectAsStateWithLifecycle()
    val selectedDietLog by vm.selectedDietLog.collectAsStateWithLifecycle()
    val todayDate by vm.todayDate.collectAsStateWithLifecycle()
    val selectedWorkoutDate by vm.selectedWorkoutDate.collectAsStateWithLifecycle()
    val selectedDietDate by vm.selectedDietDate.collectAsStateWithLifecycle()
    val proteinTarget by vm.proteinTarget.collectAsStateWithLifecycle()
    val carbTarget by vm.carbTarget.collectAsStateWithLifecycle()
    val fatTarget by vm.fatTarget.collectAsStateWithLifecycle()
    val mealProgress by vm.mealProgress.collectAsStateWithLifecycle()
    val postMealFeedback by vm.postMealFeedback.collectAsStateWithLifecycle()
    val targetsConfigured by vm.targetsConfigured.collectAsStateWithLifecycle()
    val reopenMealId by vm.reopenMealId.collectAsStateWithLifecycle()
    val workoutSplitId by vm.workoutSplitId.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val splitLocked = workoutSplitId?.isNotBlank() == true

    DisposableEffect(Unit) {
        val review = openReviewDate
        if (!review.isNullOrBlank()) {
            vm.selectWorkoutDate(review)
            vm.resetDietDateToToday()
            onReviewDateConsumed()
        } else {
            vm.resetWorkoutDateToToday()
            vm.resetDietDateToToday()
        }
        onDispose { }
    }

    val workoutDate = (selectedWorkoutDate ?: todayDate).ifBlank { todayDate }
    val dietDate = (selectedDietDate ?: todayDate).ifBlank { todayDate }

    Box(Modifier.fillMaxSize()) {
        EnergyFieldBackground(Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            SystemSectionHeader(
                tag = when (tab) {
                    FitnessTab.Workout -> if (workoutDate == todayDate) "Today's training" else "Training history"
                    FitnessTab.Diet -> if (dietDate == todayDate) "Today's fuel" else "Fuel history"
                    FitnessTab.Both -> "Fitness"
                },
                accent = SystemPrimary,
            )

            if (tab == FitnessTab.Workout || tab == FitnessTab.Both) {
            FitnessSection(
                routine = workoutRoutine,
                workoutDate = workoutDate,
                selectedLog = selectedWorkoutLog,
                history = workouts,
                splitLocked = splitLocked,
                workoutSplitId = workoutSplitId,
                todayDate = todayDate,
                canWrite = vm.canWrite(workoutDate),
                canGoNext = vm.canGoNext(workoutDate),
                dateGuidance = vm.dateGuidance(workoutDate),
                onSelectDate = { date -> vm.selectWorkoutDate(date) },
                onApplySplit = { splitId, dayMapCsv, confirmEarly ->
                    scope.launch {
                        val err = vm.applyWorkoutSplit(
                            splitId,
                            dayMapCsv,
                            confirmEarly,
                        )
                        when (err) {
                            ModuleService.EARLY_SPLIT_CHANGE_REQUIRED -> onMessage(ModuleService.EARLY_SPLIT_CHANGE_REQUIRED)
                            null -> {
                                vm.resetWorkoutDateToToday()
                                onMessage("Split applied")
                            }
                            else -> onMessage(err)
                        }
                    }
                },
                weeksOnSplit = {
                    container.modules.weeksOnCurrentSplit()
                },
                isEarlySplitChange = {
                    container.modules.isEarlySplitChange()
                },
                onPrevDate = {
                    if (workoutDate.isNotBlank()) {
                        vm.selectWorkoutDate(LocalDate.parse(workoutDate).minusDays(1).toString())
                    }
                },
                onNextDate = {
                    if (workoutDate.isNotBlank()) {
                        vm.selectWorkoutDate(LocalDate.parse(workoutDate).plusDays(1).toString())
                    }
                },
                onSaveDay = { day, plan ->
                    scope.launch {
                        vm.saveRoutineDay(day, plan)
                        onMessage("Routine day saved")
                    }
                },
                onRestDay = { day -> scope.launch { vm.setRestDay(day) } },
                onUpsertPlanned = { day, ex, name ->
                    scope.launch {
                        vm.upsertPlannedExercise(day, ex, name)
                        onMessage("Exercise added")
                    }
                },
                onRemovePlanned = { day, id -> scope.launch { vm.removePlannedExercise(day, id) } },
                onReorderPlanned = { day, id, up ->
                    scope.launch { vm.reorderPlannedExercise(day, id, up) }
                },
                onStartLog = {
                    scope.launch {
                        vm.startOrGetWorkoutLog(workoutDate)
                    }
                },
                onSaveLog = { log ->
                    scope.launch {
                        val sets = log.exercises.sumOf { it.sets.size }
                        if (sets == 0) {
                            onMessage("Log at least one set before finishing")
                            return@launch
                        }
                        vm.upsertWorkoutLog(log)
                        onMessage("Workout saved · $sets sets")
                    }
                },
                onCompleteRestDay = { activeRest ->
                    scope.launch {
                        vm.completeRestDay(workoutDate, activeRest)
                        onMessage(
                            if (activeRest) "Active rest logged. Recovery counts."
                            else "Rest day complete.",
                        )
                    }
                },
                onDeleteLog = { scope.launch { vm.deleteWorkoutLog(workoutDate) } },
                onRemoveExercise = { id ->
                    scope.launch { vm.removeExerciseFromLog(workoutDate, id) }
                },
                onUpsertExercise = { ex ->
                    scope.launch { vm.upsertLoggedExercise(workoutDate, ex) }
                },
                loadExerciseHistory = { name -> container.modules.exerciseHistory(name) },
                onMessage = onMessage,
            )
        }

        if (tab == FitnessTab.Diet || tab == FitnessTab.Both) {
            DietSection(
                dietDate = dietDate,
                todayDate = todayDate,
                canWrite = vm.canWrite(dietDate),
                canGoNext = vm.canGoNext(dietDate),
                dateGuidance = vm.dateGuidance(dietDate),
                selectedLog = selectedDietLog,
                history = dietLogs,
                proteinTarget = proteinTarget,
                carbTarget = carbTarget,
                fatTarget = fatTarget,
                mealProgress = mealProgress,
                targetsConfigured = targetsConfigured,
                reopenMealId = reopenMealId,
                onReopenMealConsumed = { vm.consumeReopenMeal() },
                onMessage = onMessage,
                onPrevDate = {
                    if (dietDate.isNotBlank()) {
                        vm.selectDietDate(LocalDate.parse(dietDate).minusDays(1).toString())
                    }
                },
                onNextDate = {
                    if (dietDate.isNotBlank()) {
                        vm.selectDietDate(LocalDate.parse(dietDate).plusDays(1).toString())
                    }
                },
                onAddMeal = { name, afterCreated ->
                    scope.launch {
                        if (dietDate.isBlank()) return@launch
                        val id = vm.addMeal(dietDate, name)
                        afterCreated(id)
                    }
                },
                onDeleteMeal = { id -> scope.launch { vm.deleteMeal(dietDate, id) } },
                onUpsertFood = { mealId, food ->
                    scope.launch {
                        vm.upsertFood(dietDate, mealId, food)
                        val log = vm.getDietLog(dietDate)
                        val mealName = log?.meals?.find { it.id == mealId }?.name ?: "Meal"
                        vm.onFoodSaved(mealId, mealName, food, log)
                    }
                },
                onDeleteFood = { mealId, foodId ->
                    scope.launch { vm.deleteFood(dietDate, mealId, foodId) }
                },
                mealTotals = { meal -> container.modules.mealTotals(meal) },
                onSelectDate = { date -> vm.selectDietDate(date) },
                onRepeatMeal = { meal, destName ->
                    scope.launch {
                        if (dietDate.isBlank()) return@launch
                        if (meal.foods.isEmpty()) {
                            onMessage("Add at least one food to the meal")
                            return@launch
                        }
                        vm.repeatMeal(dietDate, destName, meal.foods)
                        onMessage("Meal repeated")
                    }
                },
            )
        }

            Spacer(Modifier.height(Spacing.xs))
        }

        postMealFeedback?.let { feedback ->
            PostMealFeedbackDialog(
                feedback = feedback,
                onDismiss = { vm.dismissPostMealFeedback() },
                onAddAnother = {
                    vm.dismissPostMealFeedback()
                    vm.requestReopenMeal(feedback.mealId)
                },
            )
        }
    }
}

@Composable
private fun PostMealFeedbackDialog(
    feedback: PostMealFeedback,
    onDismiss: () -> Unit,
    onAddAnother: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Dialog(onDismissRequest = onDismiss) {
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            level = GlassLevel.Level2,
            cornerRadius = 16.dp,
            contentPadding = Spacing.md,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    feedback.title,
                    fontFamily = JetBrainsMono,
                    style = MaterialTheme.typography.labelSmall,
                    color = SystemPrimary,
                    letterSpacing = 1.6.sp,
                )
                Text(
                    feedback.mealName,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                )
                Text(
                    feedback.foodSummary,
                    fontFamily = JetBrainsMono,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
                Text(
                    "Today's progress",
                    fontFamily = JetBrainsMono,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
                Text(
                    feedback.progressLabel,
                    fontFamily = JetBrainsMono,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SystemPrimary,
                )
                feedback.dailyMacroLine?.let { line ->
                    Text(
                        line,
                        fontFamily = JetBrainsMono,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                Text(
                    feedback.recommendation,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    GhostTextButton(
                        label = postMealDoneLabel(),
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                    SystemActionButton(
                        label = postMealAddAnotherLabel(),
                        onClick = onAddAnother,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MacroGlassCard(label: String, current: Int, target: Int, unit: String, modifier: Modifier = Modifier) {
    GlassSurface(modifier = modifier, level = GlassLevel.Level1, cornerRadius = 12.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text(
                        "MACRO STATUS",
                        fontFamily = JetBrainsMono,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.6.sp,
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "$current",
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Text(
                            " / $target$unit",
                            fontFamily = JetBrainsMono,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                        )
                    }
                }
                Text(
                    label,
                    fontFamily = JetBrainsMono,
                    style = MaterialTheme.typography.labelSmall,
                    color = SystemPrimary,
                    modifier = Modifier
                        .border(1.dp, SystemPrimary.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .background(SystemPrimary.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            CyberProgressBar(progress = progressFraction(current.toFloat(), target.toFloat()), height = 6.dp)
        }
    }
}

@Composable
private fun DateStrip(
    label: String,
    isToday: Boolean = false,
    canGoNext: Boolean = true,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    GlassSurface(modifier = Modifier.fillMaxWidth(), level = GlassLevel.Level1, cornerRadius = 8.dp) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrev) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous day",
                    tint = SystemPrimary,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isToday) {
                    Text(
                        "TODAY",
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemPrimary,
                    )
                }
                Text(
                    label,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = JetBrainsMono,
                    color = SystemPrimary,
                )
            }
            IconButton(onClick = onNext, enabled = canGoNext) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next day",
                    tint = if (canGoNext) SystemPrimary else SystemPrimary.copy(alpha = 0.35f),
                )
            }
        }
    }
}

@Composable
private fun FitnessSection(
    routine: WorkoutRoutineEntity,
    workoutDate: String,
    todayDate: String,
    selectedLog: WorkoutLogEntity?,
    history: List<WorkoutLogEntity>,
    splitLocked: Boolean = false,
    workoutSplitId: String? = null,
    canWrite: Boolean,
    canGoNext: Boolean,
    dateGuidance: String,
    onSelectDate: (String) -> Unit = {},
    onApplySplit: (String, String, Boolean) -> Unit = { _, _, _ -> },
    weeksOnSplit: suspend () -> Long = { 0L },
    isEarlySplitChange: suspend () -> Boolean = { false },
    onPrevDate: () -> Unit,
    onNextDate: () -> Unit,
    onSaveDay: (String, WorkoutDayPlanEntity) -> Unit,
    onRestDay: (String) -> Unit,
    onUpsertPlanned: (String, PlannedExerciseEntity, String) -> Unit,
    onRemovePlanned: (String, Long) -> Unit,
    onReorderPlanned: (String, Long, Boolean) -> Unit,
    onStartLog: () -> Unit,
    onSaveLog: (WorkoutLogEntity) -> Unit,
    onCompleteRestDay: (Boolean) -> Unit = {},
    onDeleteLog: () -> Unit,
    onRemoveExercise: (Long) -> Unit,
    onUpsertExercise: (LoggedExerciseEntity) -> Unit,
    loadExerciseHistory: suspend (String) -> List<Pair<String, LoggedExerciseEntity>>,
    onMessage: (String) -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = colors.primary.copy(alpha = 0.15f),
        selectedLabelColor = colors.primary,
    )
    var userTab by remember { mutableStateOf<String?>(null) }
    val tab = resolvedTrainingTab(userTab, workoutSplitId)
    var dayKey by remember { mutableStateOf("monday") }
    var dayName by remember { mutableStateOf("") }
    var planName by remember { mutableStateOf("") }
    var planMuscle by remember { mutableStateOf("") }
    var planSets by remember { mutableStateOf("3") }
    var planRepMin by remember { mutableStateOf("8") }
    var planRepMax by remember { mutableStateOf("12") }
    var logName by remember { mutableStateOf("") }
    var exName by remember { mutableStateOf("") }
    var editingExerciseId by remember { mutableStateOf<Long?>(null) }
    var workoutEntryHadSets by remember { mutableStateOf(false) }
    var setDrafts by remember { mutableStateOf(listOf<WorkoutSetDraft>()) }
    var setFormError by remember { mutableStateOf<WorkoutSetError?>(null) }
    var compareName by remember { mutableStateOf("") }
    var compareHistory by remember { mutableStateOf<List<Pair<String, LoggedExerciseEntity>>>(emptyList()) }
    var exercisePrevious by remember { mutableStateOf<Map<Long, LoggedExerciseEntity?>>(emptyMap()) }
    var changeSplitId by remember { mutableStateOf(workoutSplitId.orEmpty().ifBlank { "ppl_ul" }) }
    var splitDayMap by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var showEarlyChangeDialog by remember { mutableStateOf(false) }
    var pendingSplitApply by remember { mutableStateOf<Pair<String, String>?>(null) }
    var weeksHeld by remember { mutableStateOf(0L) }
    var showActiveRestDialog by remember { mutableStateOf(false) }
    var showCompleteRestDialog by remember { mutableStateOf(false) }
    var logPendingDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun dismissWorkoutEntry() {
        editingExerciseId = null
        workoutEntryHadSets = false
        setDrafts = emptyList()
        setFormError = null
    }

    val selectedDayKey = remember(workoutDate) {
        runCatching {
            LocalDate.parse(workoutDate).dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH).lowercase()
        }.getOrDefault("monday")
    }
    val selectedDayPlan = routine.day(selectedDayKey)
    val isRestDay = !selectedDayPlan.enabled
    val isWorkoutDateToday = workoutDate == todayDate
    val weekdayLabel = remember(workoutDate) {
        runCatching {
            LocalDate.parse(workoutDate).dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        }.getOrDefault(workoutDate)
    }

    val dayPlan = routine.day(dayKey)
    LaunchedEffect(dayKey, dayPlan) {
        dayName = if (dayPlan.enabled) dayPlan.name else ""
    }
    LaunchedEffect(workoutSplitId) {
        if (!workoutSplitId.isNullOrBlank()) changeSplitId = workoutSplitId
    }
    LaunchedEffect(changeSplitId) {
        WorkoutCatalog.findSplit(changeSplitId)?.let { split ->
            splitDayMap = WorkoutSplitLogic.defaultDayMap(split)
        }
    }
    LaunchedEffect(splitLocked, workoutDate, canWrite) {
        if (splitLocked && canWrite && workoutDate.isNotBlank()) {
            onStartLog()
        }
    }
    LaunchedEffect(splitLocked) {
        if (splitLocked) {
            weeksHeld = weeksOnSplit()
        }
    }
    LaunchedEffect(tab, workoutDate) {
        dismissWorkoutEntry()
        showActiveRestDialog = false
        showCompleteRestDialog = false
    }

    fun tryApplySplit(confirmEarly: Boolean) {
        val mapError = WorkoutSplitLogic.buildRoutine(changeSplitId, splitDayMap).error
        if (mapError != null) {
            onMessage(mapError)
            return
        }
        val csv = WorkoutSplitLogic.encodeDayMap(splitDayMap)
        if (!confirmEarly) {
            scope.launch {
                if (isEarlySplitChange()) {
                    pendingSplitApply = changeSplitId to csv
                    weeksHeld = weeksOnSplit()
                    showEarlyChangeDialog = true
                } else {
                    onApplySplit(changeSplitId, csv, false)
                    userTab = "Today"
                }
            }
            return
        }
        onApplySplit(changeSplitId, csv, true)
        userTab = "Today"
    }

    if (showEarlyChangeDialog) {
        AlertDialog(
            onDismissRequest = {
                showEarlyChangeDialog = false
                pendingSplitApply = null
            },
            title = { Text("Your current split") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (weeksHeld > 0) {
                            "You've been following this split for $weeksHeld weeks."
                        } else {
                            "You've recently set this split."
                        },
                    )
                    Text(
                        "Consistency matters more than constantly changing the plan. " +
                            "For meaningful progress, consider following your current split " +
                            "for at least 6 months before changing it.",
                    )
                    Text(
                        "Changing early may reduce workout progression rewards. " +
                            "Your existing progress remains yours. Career and diet are unaffected.",
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pending = pendingSplitApply
                        showEarlyChangeDialog = false
                        pendingSplitApply = null
                        if (pending != null) {
                            onApplySplit(pending.first, pending.second, true)
                            userTab = "Today"
                        }
                    },
                ) { Text("Change Split") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEarlyChangeDialog = false
                        pendingSplitApply = null
                    },
                ) { Text("Keep Current Split") }
            },
        )
    }

    if (showActiveRestDialog) {
        AlertDialog(
            onDismissRequest = { showActiveRestDialog = false },
            title = { Text("Rest day") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Did you have an active rest day?")
                    Text(
                        "Active rest can include walking, light movement, mobility, or stretching.",
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showActiveRestDialog = false
                        onCompleteRestDay(true)
                    },
                ) { Text("Yes, Active Rest") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showActiveRestDialog = false
                        showCompleteRestDialog = true
                    },
                ) { Text("No") }
            },
        )
    }

    if (showCompleteRestDialog) {
        AlertDialog(
            onDismissRequest = { showCompleteRestDialog = false },
            title = { Text("Rest day") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Complete rest is okay.")
                    Text(
                        "Active recovery is recommended when possible.",
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCompleteRestDialog = false
                        onCompleteRestDay(false)
                    },
                ) { Text("Finish Rest Day") }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteRestDialog = false }) { Text("Cancel") }
            },
        )
    }

    GlassSurface(modifier = Modifier.fillMaxWidth(), level = GlassLevel.Level1) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val tabs = when {
                    workoutSplitId == null -> listOf("Today", "History")
                    splitLocked && tab == "Change Split" -> listOf("Today", "Change Split", "History")
                    splitLocked -> listOf("Today", "History")
                    else -> listOf("Routine", "Today", "History")
                }
                tabs.forEach { label ->
                    SovereignChip(
                        label = label,
                        selected = tab == label || (label == "Today" && tab == "Log"),
                        onClick = { userTab = label },
                    )
                }
            }

            when (tab) {
                "Change Split", "Routine" -> {
                    Text(
                        if (splitLocked) {
                            "Choose a new split. This regenerates your week plan."
                        } else {
                            "Choose a catalog split to lock the exercise list"
                        },
                        fontWeight = FontWeight.Bold,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        WorkoutCatalog.splits.forEach { split ->
                            FilterChip(
                                selected = changeSplitId == split.id,
                                onClick = { changeSplitId = split.id },
                                label = { Text("${split.name} (${split.daysPerWeek}d)") },
                                colors = chipColors,
                            )
                        }
                    }
                    WorkoutCatalog.findSplit(changeSplitId)?.schedule?.sortedBy { it.day }?.forEach { slot ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                WorkoutSplitLogic.workoutLabelForSlot(changeSplitId, slot.day),
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                WorkoutSplitLogic.weekdayLabels.forEach { (iso, label) ->
                                    FilterChip(
                                        selected = splitDayMap[slot.day] == iso,
                                        onClick = { splitDayMap = splitDayMap + (slot.day to iso) },
                                        label = { Text(label) },
                                        colors = chipColors,
                                    )
                                }
                            }
                        }
                    }
                    remember(changeSplitId, splitDayMap) {
                        WorkoutSplitLogic.buildRoutine(changeSplitId, splitDayMap).error
                    }?.let { err ->
                        Text(err, color = colors.error, style = MaterialTheme.typography.bodySmall)
                    }
                    SystemActionButton(
                        label = if (splitLocked) "APPLY SPLIT" else "APPLY SPLIT & LOCK",
                        onClick = { tryApplySplit(confirmEarly = false) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (tab == "Routine" && !splitLocked) {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            WEEK_DAYS.forEach { d ->
                                FilterChip(
                                    selected = dayKey == d,
                                    onClick = { dayKey = d },
                                    label = { Text(d.take(3).replaceFirstChar { it.uppercase() }) },
                                    colors = chipColors,
                                    border = BorderStroke(1.dp, if (dayKey == d) colors.primary else colors.outline),
                                )
                            }
                        }
                        OutlinedTextField(
                            dayName,
                            { dayName = it },
                            label = { Text("Workout name") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SystemActionButton(
                                label = "SAVE DAY",
                                onClick = {
                                    onSaveDay(
                                        dayKey,
                                        dayPlan.copy(enabled = true, name = dayName.ifBlank { "Workout" }),
                                    )
                                },
                            )
                            SystemActionButton(label = "MARK REST", onClick = { onRestDay(dayKey) }, primary = false)
                        }
                        if (!dayPlan.enabled) {
                            Text("Currently marked rest — Save day or Add exercise to enable.")
                        }
                        if (dayPlan.exercises.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                HorizontalDivider(color = colors.primary.copy(alpha = 0.25f))
                                dayPlan.exercises.forEach { ex ->
                                    Text(
                                        "${ex.name} · ${ex.targetMuscle} · ${ex.sets}×${ex.repRange.min}-${ex.repRange.max}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceVariant,
                                        modifier = Modifier.padding(start = Spacing.xs),
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        SystemActionButton(label = "UP", onClick = { onReorderPlanned(dayKey, ex.id, true) }, primary = false)
                                        SystemActionButton(label = "DOWN", onClick = { onReorderPlanned(dayKey, ex.id, false) }, primary = false)
                                        SystemActionButton(label = "REMOVE", onClick = { onRemovePlanned(dayKey, ex.id) }, primary = false)
                                    }
                                }
                            }
                        }
                        Text("Add exercise", style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(planName, { planName = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(planMuscle, { planMuscle = it }, label = { Text("Target muscle") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(planSets, { planSets = it }, label = { Text("Sets") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(planRepMin, { planRepMin = it }, label = { Text("Rep min") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(planRepMax, { planRepMax = it }, label = { Text("Rep max") }, modifier = Modifier.fillMaxWidth())
                        SystemActionButton(
                            label = "ADD EXERCISE",
                            onClick = {
                                EntryValidation.requireNonBlank(planName, "exercise name")?.let {
                                    onMessage(it)
                                    return@SystemActionButton
                                }
                                EntryValidation.requirePositiveInt(planSets, "sets")?.let {
                                    onMessage(it)
                                    return@SystemActionButton
                                }
                                EntryValidation.requirePositiveInt(planRepMin, "rep min")?.let {
                                    onMessage(it)
                                    return@SystemActionButton
                                }
                                EntryValidation.requirePositiveInt(planRepMax, "rep max")?.let {
                                    onMessage(it)
                                    return@SystemActionButton
                                }
                                onUpsertPlanned(
                                    dayKey,
                                    PlannedExerciseEntity(
                                        name = planName.trim(),
                                        targetMuscle = planMuscle.trim(),
                                        sets = planSets.toInt(),
                                        repRange = RepRangeEntity(
                                            planRepMin.toInt(),
                                            planRepMax.toInt(),
                                        ),
                                    ),
                                    dayName,
                                )
                                planName = ""
                                planMuscle = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                "Today", "Log" -> {
                    DateStrip(
                        label = weekdayLabel,
                        isToday = isWorkoutDateToday,
                        canGoNext = canGoNext,
                        onPrev = onPrevDate,
                        onNext = onNextDate,
                    )
                    Text(
                        dateGuidance,
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (canWrite && !splitLocked) {
                        SystemActionButton(
                            label = "START WORKOUT",
                            onClick = onStartLog,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    val log = selectedLog
                    val dayComplete = log?.isTrainingDayComplete() == true
                    LaunchedEffect(log, tab, workoutDate) {
                        if ((tab == "Log" || tab == "Today") && log != null && !isRestDay) {
                            val map = mutableMapOf<Long, LoggedExerciseEntity?>()
                            log.exercises.forEach { ex ->
                                val hist = loadExerciseHistory(ex.name)
                                map[ex.id] = hist.firstOrNull { it.first != workoutDate }?.second
                            }
                            exercisePrevious = map
                        }
                    }
                    if (isRestDay) {
                        Text(
                            "REST DAY",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SystemPrimary,
                        )
                        Text(
                            "Recovery is part of training.",
                            color = colors.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (dayComplete) {
                            val kindLabel = when (log?.restKind) {
                                WorkoutRestKind.ACTIVE_REST -> "Active rest logged."
                                WorkoutRestKind.COMPLETE_REST -> "Rest day complete."
                                else -> "Day complete."
                            }
                            Text(
                                kindLabel,
                                fontFamily = JetBrainsMono,
                                color = colors.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        } else if (canWrite) {
                            SystemActionButton(
                                label = "FINISH WORKOUT",
                                onClick = { showActiveRestDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (splitLocked) {
                            GhostTextButton(
                                label = "Change Split",
                                onClick = { userTab = "Change Split" },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    } else if (log != null) {
                        if (splitLocked || !canWrite) {
                            Text(
                                log.workoutName.ifBlank { selectedDayPlan.name }.ifBlank { "Workout" },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        } else {
                            OutlinedTextField(
                                value = logName.ifBlank { log.workoutName },
                                onValueChange = { logName = it },
                                label = { Text("Workout name") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (dayComplete) {
                            Text(
                                "Session complete.",
                                fontFamily = JetBrainsMono,
                                color = colors.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        log.exercises.forEachIndexed { index, ex ->
                            if (index > 0) {
                                HorizontalDivider(color = SystemPrimary.copy(alpha = 0.12f))
                            }
                            val planned = routine.day(log.dayOfWeek.ifBlank {
                                selectedDayKey
                            }).exercises.firstOrNull { it.name.equals(ex.name, ignoreCase = true) }
                            ExerciseLogRow(
                                exercise = ex,
                                plannedSets = planned?.sets,
                                enabled = canWrite && !dayComplete,
                                showDelete = canWrite && !splitLocked,
                                onOpen = {
                                    editingExerciseId = ex.id
                                    workoutEntryHadSets = ex.sets.isNotEmpty()
                                    setFormError = null
                                    setDrafts = initialSetDrafts(
                                        ex.sets,
                                        exercisePrevious[ex.id]?.sets?.lastOrNull(),
                                    )
                                },
                                onRemove = { onRemoveExercise(ex.id) },
                            )
                        }
                        if (canWrite && !splitLocked) {
                            Text("Unplanned exercise", style = MaterialTheme.typography.titleSmall)
                            OutlinedTextField(exName, { exName = it }, label = { Text("Exercise") }, modifier = Modifier.fillMaxWidth())
                            SystemActionButton(
                                label = "ADD",
                                onClick = {
                                    EntryValidation.requireNonBlank(exName, "exercise name")?.let {
                                        onMessage(it)
                                        return@SystemActionButton
                                    }
                                    onUpsertExercise(LoggedExerciseEntity(name = exName.trim()))
                                    exName = ""
                                },
                            )
                        }
                        if (canWrite && !dayComplete) {
                            SystemActionButton(
                                label = "FINISH WORKOUT",
                                onClick = {
                                    val sets = log.exercises.sumOf { it.sets.size }
                                    if (sets == 0) {
                                        onMessage("Log at least one set before finishing")
                                        return@SystemActionButton
                                    }
                                    onSaveLog(log.copy(workoutName = logName.ifBlank { log.workoutName }))
                                    logName = ""
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (canWrite && !splitLocked) {
                            SystemActionButton(
                                label = "DELETE",
                                onClick = { logPendingDelete = true },
                                primary = false,
                                destructive = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (splitLocked) {
                            GhostTextButton(
                                label = "Change Split",
                                onClick = { userTab = "Change Split" },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    } else if (!canWrite) {
                        SystemIdleEmpty(
                            title = "No record this day",
                            subtitle = dateGuidance,
                        )
                    } else {
                        SystemIdleEmpty(
                            title = "System ready",
                            subtitle = "No workout has been logged yet.\nYour first session starts here.",
                            actionLabel = if (splitLocked) "OPEN TODAY" else "START WORKOUT",
                            onAction = onStartLog,
                        )
                        if (splitLocked) {
                            GhostTextButton(
                                label = "Change Split",
                                onClick = { userTab = "Change Split" },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                "History" -> {
                    if (history.isEmpty()) {
                        Text("No workout history yet", color = colors.onSurfaceVariant)
                    }
                    history.take(14).forEach { w ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectDate(w.date)
                                    userTab = "Today"
                                },
                        ) {
                            Text(
                                "${w.date} · ${w.workoutName} · ${w.exercises.size} exercises · " +
                                    "${w.exercises.sumOf { it.sets.size }} sets",
                            )
                            w.exercises.forEach { ex ->
                                Text(
                                    "  ${ex.name}: " +
                                        ex.sets.joinToString { "${it.weight}kg×${it.reps}" },
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                    Text("Compare exercise", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(compareName, { compareName = it }, label = { Text("Exercise name") }, modifier = Modifier.fillMaxWidth())
                    SystemActionButton(
                        label = "LOAD HISTORY",
                        onClick = {
                            scope.launch {
                                if (compareName.isNotBlank()) {
                                    compareHistory = loadExerciseHistory(compareName)
                                }
                            }
                        },
                    )
                    compareHistory.forEachIndexed { index, (date, ex) ->
                        val label = if (index == 0) "Current" else "Previous"
                        Text("$label ($date)")
                        ex.sets.forEach { s -> Text("  ${s.weight}kg × ${s.reps}") }
                    }
                }
            }
        }
    }

    val entryExercise = selectedLog?.exercises?.firstOrNull { it.id == editingExerciseId }
    if (entryExercise != null) {
        val previousSummary = exercisePrevious[entryExercise.id]
            ?.sets
            ?.takeIf { it.isNotEmpty() }
            ?.let { formatPreviousSetsSummary(it) }
        WorkoutEntryDialog(
            exerciseName = entryExercise.name,
            hasSavedSets = workoutEntryHadSets,
            previousSummary = previousSummary,
            drafts = setDrafts,
            formError = setFormError,
            onDraftChange = { index, draft ->
                setDrafts = setDrafts.mapIndexed { i, row -> if (i == index) draft else row }
                setFormError = null
            },
            onAddSet = {
                setDrafts = setDrafts + nextSetDraft(setDrafts)
                setFormError = null
            },
            onDeleteSet = { index ->
                setDrafts = removeSetDraft(setDrafts, index)
                setFormError = null
            },
            onCancel = { dismissWorkoutEntry() },
            onSave = {
                val err = validateSetDraft(setDrafts)
                if (err != null) {
                    setFormError = err
                } else {
                    onUpsertExercise(entryExercise.copy(sets = loggedSetsFromDraft(setDrafts)))
                    dismissWorkoutEntry()
                }
            },
        )
    }

    if (logPendingDelete) {
        val affectsProgress = workoutLogAffectsProgress(selectedLog)
        SystemConfirmDialog(
            title = SystemMessages.DELETE_ENTRY_TITLE,
            explanation = if (affectsProgress) {
                SystemMessages.DELETE_ENTRY_PROGRESS_EXPLANATION
            } else {
                SystemMessages.DELETE_ENTRY_NO_PROGRESS_EXPLANATION
            },
            consequence = if (affectsProgress) SystemMessages.DELETE_ENTRY_PROGRESS_CONSEQUENCE else "",
            confirmLabel = SystemMessages.DELETE_ENTRY_CONFIRM,
            cancelLabel = SystemMessages.DELETE_ENTRY_KEEP,
            onDismiss = { logPendingDelete = false },
            onConfirm = {
                logPendingDelete = false
                onDeleteLog()
            },
        )
    }
}

@Composable
private fun ExerciseLogRow(
    exercise: LoggedExerciseEntity,
    plannedSets: Int?,
    enabled: Boolean,
    showDelete: Boolean,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onOpen)
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    exercise.name.uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                )
                Text(
                    exerciseSetProgressLabel(exercise.sets.size, plannedSets),
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMono,
                )
            }
            if (enabled) {
                TextButton(
                    onClick = onOpen,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    if (exercise.sets.isEmpty()) {
                        Icon(Icons.Default.Add, contentDescription = "Add set", tint = SystemPrimary)
                        Spacer(Modifier.width(Spacing.xxs))
                        Text(
                            "ADD SET",
                            fontFamily = JetBrainsMono,
                            color = SystemPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    } else {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = workoutEditActionLabel(exercise.name),
                            tint = SystemPrimary,
                        )
                        Spacer(Modifier.width(Spacing.xxs))
                        Text(
                            "EDIT",
                            fontFamily = JetBrainsMono,
                            color = SystemPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
            if (showDelete) {
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete ${exercise.name}",
                        tint = SystemError,
                    )
                }
            }
        }
        if (exercise.sets.isEmpty()) {
            Text(
                WORKOUT_ENTRY_EMPTY_SETS,
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            exercise.sets.forEach { set ->
                Text(
                    formatLoggedSetSummary(set),
                    fontFamily = JetBrainsMono,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun WorkoutEntryDialog(
    exerciseName: String,
    hasSavedSets: Boolean,
    previousSummary: String?,
    drafts: List<WorkoutSetDraft>,
    formError: WorkoutSetError?,
    onDraftChange: (Int, WorkoutSetDraft) -> Unit,
    onAddSet: () -> Unit,
    onDeleteSet: (Int) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.85f).dp
    var focusedIndex by remember { mutableIntStateOf(0) }
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md)
                .imePadding(),
            level = GlassLevel.Level2,
            borderAlpha = 0.25f,
            cornerRadius = 16.dp,
            contentPadding = 0.dp,
        ) {
            Column(modifier = Modifier.heightIn(max = maxHeight)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.md),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 40.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            workoutEntryDialogTitle(hasSavedSets),
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            color = SystemPrimary,
                            fontSize = 22.sp,
                            letterSpacing = 1.sp,
                        )
                        Text(
                            exerciseName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface,
                        )
                        if (!previousSummary.isNullOrBlank()) {
                            Text(
                                "Previous  $previousSummary",
                                color = colors.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = JetBrainsMono,
                            )
                        }
                    }
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.align(Alignment.TopEnd),
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = colors.onSurfaceVariant,
                        )
                    }
                }
                HorizontalDivider(color = SystemOutlineVariant.copy(alpha = 0.4f))
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.5f).dp)
                        .verticalScroll(rememberScrollState())
                        .padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (drafts.isEmpty()) {
                        Text(
                            WORKOUT_ENTRY_EMPTY_SETS,
                            color = colors.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    } else {
                        drafts.forEachIndexed { index, draft ->
                            SetDraftRow(
                                index = index,
                                draft = draft,
                                formError = formError,
                                active = focusedIndex == index,
                                onFocused = { focusedIndex = index },
                                onDraftChange = { onDraftChange(index, it) },
                                onDelete = { onDeleteSet(index) },
                            )
                        }
                    }
                    TextButton(
                        onClick = onAddSet,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add set", tint = SystemPrimary)
                        Spacer(Modifier.width(Spacing.xxs))
                        Text(
                            "ADD SET",
                            fontFamily = JetBrainsMono,
                            color = SystemPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp,
                        )
                    }
                }
                HorizontalDivider(color = SystemOutlineVariant.copy(alpha = 0.4f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SystemSurface.copy(alpha = 0.45f))
                        .padding(Spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GhostTextButton(label = "Cancel", onClick = onCancel)
                    SystemActionButton(
                        label = workoutEntryConfirmLabel(hasSavedSets),
                        onClick = onSave,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SetDraftRow(
    index: Int,
    draft: WorkoutSetDraft,
    formError: WorkoutSetError?,
    active: Boolean,
    onFocused: () -> Unit,
    onDraftChange: (WorkoutSetDraft) -> Unit,
    onDelete: () -> Unit,
) {
    fun message(field: WorkoutSetField): String? =
        formError?.takeIf { it.index == index && it.field == field }?.message
    val rowShape = RoundedCornerShape(8.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(if (active) CyanAura else Color.Transparent)
            .border(
                1.dp,
                if (active) SystemPrimary.copy(alpha = 0.3f) else Color.Transparent,
                rowShape,
            )
            .padding(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "%02d".format(index + 1),
                modifier = Modifier.width(24.dp),
                fontFamily = JetBrainsMono,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = if (active) SystemPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            CompactSetField(
                value = draft.weight,
                onValueChange = { onDraftChange(draft.copy(weight = it)) },
                placeholder = "kg",
                keyboardType = KeyboardType.Decimal,
                isError = message(WorkoutSetField.Weight) != null,
                modifier = Modifier.weight(1f),
                onFocusChanged = { if (it) onFocused() },
            )
            CompactSetField(
                value = draft.reps,
                onValueChange = { onDraftChange(draft.copy(reps = it)) },
                placeholder = "Reps",
                keyboardType = KeyboardType.Number,
                isError = message(WorkoutSetField.Reps) != null,
                modifier = Modifier.weight(1f),
                onFocusChanged = { if (it) onFocused() },
            )
            CompactSetField(
                value = draft.rpe,
                onValueChange = { onDraftChange(draft.copy(rpe = it)) },
                placeholder = "RPE",
                keyboardType = KeyboardType.Decimal,
                isError = message(WorkoutSetField.Rpe) != null,
                modifier = Modifier.width(64.dp),
                onFocusChanged = { if (it) onFocused() },
            )
            IconButton(onClick = onDelete, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete set", tint = SystemError.copy(alpha = if (active) 0.85f else 0.55f))
            }
        }
        val err = formError?.takeIf { it.index == index }
        if (err != null) {
            FoodFieldSupporting(err.message)
        }
    }
}

@Composable
private fun CompactSetField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    isError: Boolean,
    modifier: Modifier = Modifier,
    onFocusChanged: (Boolean) -> Unit = {},
) {
    val shape = RoundedCornerShape(4.dp)
    var focused by remember { mutableStateOf(false) }
    val borderColor = when {
        isError -> SystemError
        focused -> SystemPrimary
        else -> SystemOutlineVariant
    }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = JetBrainsMono,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        cursorBrush = SolidColor(SystemPrimary),
        modifier = modifier
            .onFocusChanged {
                focused = it.isFocused
                onFocusChanged(it.isFocused)
            }
            .clip(shape)
            .background(SystemSurface.copy(alpha = 0.5f), shape)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = 4.dp, vertical = 12.dp),
        decorationBox = { inner ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        fontFamily = JetBrainsMono,
                        fontSize = 12.sp,
                    )
                }
                inner()
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DietSection(
    dietDate: String,
    todayDate: String,
    canWrite: Boolean,
    canGoNext: Boolean,
    dateGuidance: String,
    selectedLog: DietLogEntity?,
    history: List<DietLogEntity>,
    proteinTarget: Int,
    carbTarget: Int,
    fatTarget: Int,
    mealProgress: MealProgressState,
    targetsConfigured: Boolean,
    reopenMealId: Long?,
    onReopenMealConsumed: () -> Unit,
    onMessage: (String) -> Unit,
    onPrevDate: () -> Unit,
    onNextDate: () -> Unit,
    onAddMeal: (String, (Long) -> Unit) -> Unit,
    onDeleteMeal: (Long) -> Unit,
    onUpsertFood: (Long, FoodItemEntity) -> Unit,
    onDeleteFood: (Long, Long) -> Unit,
    mealTotals: (MealEntity) -> NutritionTotalsEntity,
    onSelectDate: (String) -> Unit = {},
    onRepeatMeal: (MealEntity, String) -> Unit = { _, _ -> },
) {
    val colors = MaterialTheme.colorScheme
    var tab by remember { mutableStateOf("Log Food") }
    var selectedCategory by remember { mutableStateOf(DIET_MEAL_CATEGORIES.first()) }
    var foodMealId by remember { mutableStateOf<Long?>(null) }
    var editingFoodId by remember { mutableStateOf<Long?>(null) }
    var foodEntryMode by remember { mutableStateOf("Catalog") }
    var foodName by remember { mutableStateOf("") }
    var foodQty by remember { mutableStateOf("") }
    var foodUnit by remember { mutableStateOf("g") }
    var foodCal by remember { mutableStateOf("") }
    var foodP by remember { mutableStateOf("") }
    var foodC by remember { mutableStateOf("") }
    var foodF by remember { mutableStateOf("") }
    var selectedCatalog by remember { mutableStateOf<FoodCatalogEntry?>(null) }
    var catalogFilter by remember { mutableStateOf("") }
    var macrosManualOverride by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<FoodEntryError?>(null) }
    var mealPendingDelete by remember { mutableStateOf<MealEntity?>(null) }
    var foodPendingDelete by remember { mutableStateOf<Pair<Long, Long>?>(null) }
    var repeatPickerOpen by remember { mutableStateOf(false) }
    var repeatSource by remember { mutableStateOf<RepeatMealOption?>(null) }
    var repeatDestName by remember { mutableStateOf("") }

    fun clearFoodForm() {
        foodName = ""
        foodQty = ""
        foodUnit = "g"
        foodCal = ""
        foodP = ""
        foodC = ""
        foodF = ""
        selectedCatalog = null
        catalogFilter = ""
        macrosManualOverride = false
        formError = null
    }

    LaunchedEffect(dietDate, canWrite) {
        if (!canWrite) {
            foodMealId = null
            editingFoodId = null
            clearFoodForm()
            repeatSource = null
            repeatPickerOpen = false
            mealPendingDelete = null
            foodPendingDelete = null
        }
    }

    fun applyCatalogMacros(entry: FoodCatalogEntry, qtyText: String) {
        val qty = qtyText.toFloatOrNull() ?: FoodMacroScaler.defaultQuantity(entry)
        val scaled = FoodMacroScaler.scale(entry, qty)
        foodCal = scaled.calories.toString()
        foodP = scaled.protein.toString()
        foodC = scaled.carbs.toString()
        foodF = scaled.fat.toString()
        macrosManualOverride = false
    }

    fun switchFoodEntryMode(mode: String) {
        if (foodEntryMode == mode) return
        foodEntryMode = mode
        clearFoodForm()
    }

    fun openAddFood(mealId: Long) {
        foodMealId = mealId
        editingFoodId = null
        foodEntryMode = "Catalog"
        clearFoodForm()
    }

    LaunchedEffect(reopenMealId) {
        val mealId = reopenMealId ?: return@LaunchedEffect
        openAddFood(mealId)
        onReopenMealConsumed()
    }

    fun openEditFood(mealId: Long, food: FoodItemEntity) {
        foodMealId = mealId
        editingFoodId = food.id
        val catalog = catalogEntryForFoodName(food.name)
        foodName = food.name
        foodQty = foodQuantityText(food.quantity)
        foodUnit = food.unit.orEmpty().ifBlank { "g" }
        foodCal = food.calories?.toString().orEmpty()
        foodP = food.protein?.toString().orEmpty()
        foodC = food.carbs?.toString().orEmpty()
        foodF = food.fat?.toString().orEmpty()
        formError = null
        if (catalog != null) {
            foodEntryMode = "Catalog"
            selectedCatalog = catalog
            catalogFilter = catalog.name
            macrosManualOverride = false
        } else {
            foodEntryMode = "Others"
            selectedCatalog = null
            catalogFilter = ""
            macrosManualOverride = true
        }
    }

    fun dismissFoodEntry() {
        val mealId = foodMealId ?: return
        val wasEdit = editingFoodId != null
        val meal = selectedLog?.meals?.firstOrNull { it.id == mealId }
        foodMealId = null
        editingFoodId = null
        clearFoodForm()
        if (!wasEdit && meal != null && meal.foods.isEmpty()) {
            onDeleteMeal(mealId)
            onMessage("Add at least one food to the meal")
        }
    }

    fun logSelectedCategoryFood() {
        if (!canWrite) return
        tab = "Log Food"
        val existing = mealsForCategory(selectedLog?.meals.orEmpty(), selectedCategory).firstOrNull()
        if (existing != null) {
            openAddFood(existing.id)
        } else {
            onAddMeal(selectedCategory) { mealId -> openAddFood(mealId) }
        }
    }

    fun startRepeat() {
        if (!canWrite) return
        val options = repeatableMeals(history)
        if (options.isEmpty()) {
            onMessage("No meals logged.\nStart recording today's nutrition.")
            return
        }
        if (options.size == 1) {
            repeatSource = options.first()
            repeatDestName = ""
        } else {
            repeatPickerOpen = true
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        val totals = dietDailySummary(selectedLog)
        val showingHistory = tab == "History"
        val categoryChips = dietCategoryChips(selectedLog?.meals.orEmpty())
        val categoryMeals = mealsForCategory(selectedLog?.meals.orEmpty(), selectedCategory)

        MacroGlassCard("PROTEIN", totals.protein, proteinTarget, "g")
        MacroGlassCard("CARBS", totals.carbs, carbTarget, "g")
        MacroGlassCard("FATS", totals.fat, fatTarget, "g")

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            GlassSurface(
                modifier = Modifier
                    .weight(1f)
                    .then(if (canWrite) Modifier.clickable { logSelectedCategoryFood() } else Modifier),
                level = GlassLevel.Level1,
                cornerRadius = 12.dp,
                contentPadding = Spacing.sm,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Log food", tint = SystemPrimary)
                    Text(
                        "LOG",
                        fontFamily = JetBrainsMono,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            GlassSurface(
                modifier = Modifier
                    .weight(1f)
                    .then(if (canWrite) Modifier.clickable { startRepeat() } else Modifier),
                level = GlassLevel.Level1,
                cornerRadius = 12.dp,
                contentPadding = Spacing.sm,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Repeat meal", tint = SystemPrimary)
                    Text(
                        "REPEAT",
                        fontFamily = JetBrainsMono,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            GlassSurface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { tab = if (showingHistory) "Log Food" else "History" },
                level = GlassLevel.Level1,
                cornerRadius = 12.dp,
                contentPadding = Spacing.sm,
                borderAlpha = if (showingHistory) 0.35f else 0.10f,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
                ) {
                    Icon(Icons.Default.ShowChart, contentDescription = "Diet history", tint = SystemPrimary)
                    Text(
                        "HISTORY",
                        fontFamily = JetBrainsMono,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (showingHistory) SystemPrimary else colors.onSurfaceVariant,
                    )
                }
            }
        }

        if (showingHistory) {
            if (history.isEmpty()) {
                Text("No diet history yet", color = colors.onSurfaceVariant)
            } else {
                history.take(14).forEach { day ->
                    val t = day.dailyTotals
                    GlassSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectDate(day.date)
                                tab = "Log Food"
                            },
                        level = GlassLevel.Level1,
                        cornerRadius = 12.dp,
                    ) {
                        Text(
                            day.date,
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            color = SystemPrimary,
                        )
                        Text(
                            "${day.meals.size} meals · ${t.calories} kcal",
                            fontFamily = JetBrainsMono,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                        )
                        day.meals.forEach { meal ->
                            Text(
                                "${meal.name}: ${meal.foods.joinToString { it.name }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                categoryChips.forEach { preset ->
                    SovereignChip(
                        label = preset,
                        selected = preset.equals(selectedCategory, ignoreCase = true),
                        onClick = { selectedCategory = preset },
                    )
                }
            }

            DateStrip(
                label = dietDate,
                isToday = dietDate == todayDate,
                canGoNext = canGoNext,
                onPrev = onPrevDate,
                onNext = onNextDate,
            )
            Text(
                dateGuidance,
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )

            GlassSurface(modifier = Modifier.fillMaxWidth(), level = GlassLevel.Level1, cornerRadius = 12.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(
                        mealProgressHeaderLabel(),
                        fontFamily = JetBrainsMono,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        letterSpacing = 1.6.sp,
                    )
                    Text(
                        mealProgress.progressLabel,
                        fontFamily = JetBrainsMono,
                        style = MaterialTheme.typography.bodySmall,
                        color = SystemPrimary,
                    )
                    val progressFraction = if (mealProgress.requiredCount <= 0) {
                        0f
                    } else {
                        (mealProgress.loggedCount.toFloat() / mealProgress.requiredCount).coerceIn(0f, 1f)
                    }
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier.fillMaxWidth(),
                        color = SystemPrimary,
                        trackColor = colors.onSurfaceVariant.copy(alpha = 0.2f),
                    )
                    Text(
                        mealProgress.guidanceLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                    if (!targetsConfigured) {
                        Text(
                            SystemMessages.MEAL_TRACKING_COMPLETE_PROFILE_HINT,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    HorizontalDivider(color = colors.onSurfaceVariant.copy(alpha = 0.15f))
                    Text(
                        "TODAY'S MEALS",
                        fontFamily = JetBrainsMono,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        letterSpacing = 1.6.sp,
                    )
                    mealProgress.slotStatuses.filter { it.name != "Snack" }.forEach { slot ->
                        val selected = slot.name.equals(selectedCategory, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCategory = slot.name }
                                .padding(vertical = Spacing.xxs),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    slot.name,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) SystemPrimary else colors.onSurface,
                                )
                                if (slot.logged) {
                                    Text(
                                        "PROTEIN: ${slot.protein}g  CARBS: ${slot.carbs}g  FATS: ${slot.fat}g",
                                        fontFamily = JetBrainsMono,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.onSurfaceVariant,
                                    )
                                } else {
                                    Text(
                                        "Not logged",
                                        fontFamily = JetBrainsMono,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.onSurfaceVariant,
                                    )
                                }
                            }
                            Text(
                                mealSlotIndicator(slot.logged),
                                fontFamily = JetBrainsMono,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (slot.logged) SystemPrimary else colors.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            if (categoryMeals.isEmpty()) {
                GlassSurface(modifier = Modifier.fillMaxWidth(), level = GlassLevel.Level1, cornerRadius = 12.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        Text(selectedCategory, fontWeight = FontWeight.Bold, color = SystemPrimary)
                        Text(
                            "PROTEIN: 0g  CARBS: 0g  FATS: 0g",
                            fontFamily = JetBrainsMono,
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemPrimary,
                        )
                        if (canWrite) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .border(1.dp, SystemPrimary.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                .clickable { logSelectedCategoryFood() }
                                .padding(vertical = Spacing.sm),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = colors.onSurfaceVariant)
                                Spacer(Modifier.width(Spacing.xxs))
                                Text(
                                    "ADD FOOD ITEM",
                                    fontFamily = JetBrainsMono,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                        }
                        }
                    }
                }
            } else {
                categoryMeals.forEach { meal ->
                    val mealT = mealTotals(meal)
                    GlassSurface(modifier = Modifier.fillMaxWidth(), level = GlassLevel.Level1, cornerRadius = 12.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(meal.name, fontWeight = FontWeight.Bold, color = SystemPrimary)
                                    Text(
                                        "PROTEIN: ${mealT.protein}g  CARBS: ${mealT.carbs}g  FATS: ${mealT.fat}g",
                                        fontFamily = JetBrainsMono,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SystemPrimary,
                                    )
                                }
                                if (canWrite) {
                                    IconButton(onClick = { mealPendingDelete = meal }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete meal",
                                            tint = SystemError.copy(alpha = 0.5f),
                                        )
                                    }
                                }
                            }
                            meal.foods.forEachIndexed { index, food ->
                                if (index > 0) {
                                    HorizontalDivider(color = SystemPrimary.copy(alpha = 0.12f))
                                }
                                val qty = listOfNotNull(
                                    food.quantity?.let { q ->
                                        "${foodQuantityText(q)} ${food.unit.orEmpty()}".trim()
                                    },
                                    food.calories?.let { "${it} kcal" },
                                ).joinToString(" • ")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(food.name, style = MaterialTheme.typography.bodyMedium)
                                        if (qty.isNotBlank()) {
                                            Text(
                                                qty,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.onSurfaceVariant,
                                                fontFamily = JetBrainsMono,
                                            )
                                        }
                                    }
                                    if (canWrite) {
                                        IconButton(onClick = { openEditFood(meal.id, food) }) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit food",
                                                tint = colors.onSurfaceVariant,
                                            )
                                        }
                                        IconButton(onClick = { foodPendingDelete = meal.id to food.id }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete food",
                                                tint = SystemError.copy(alpha = 0.5f),
                                            )
                                        }
                                    }
                                }
                            }
                            if (canWrite) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .border(1.dp, SystemPrimary.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                    .clickable { openAddFood(meal.id) }
                                    .padding(vertical = Spacing.sm),
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = colors.onSurfaceVariant)
                                    Spacer(Modifier.width(Spacing.xxs))
                                    Text(
                                        "ADD FOOD ITEM",
                                        fontFamily = JetBrainsMono,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.onSurfaceVariant,
                                    )
                                }
                            }
                            }
                        }
                    }
                }
            }

            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                level = GlassLevel.Level1,
                cornerRadius = 12.dp,
                borderAlpha = 0.08f,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "DAILY SUMMARY",
                        fontFamily = JetBrainsMono,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        letterSpacing = 1.6.sp,
                    )
                    Text(
                        "${totals.calories} kcal",
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    listOf(
                        "PROTEIN" to totals.protein,
                        "CARBS" to totals.carbs,
                        "FATS" to totals.fat,
                    ).forEach { (label, value) ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(SystemSurface.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .border(1.dp, SystemPrimary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(Spacing.xs),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                label,
                                fontFamily = JetBrainsMono,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                            )
                            Text(
                                "${value}g",
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                color = SystemPrimary,
                            )
                        }
                    }
                }
            }
        }
    }

    val entryMealId = foodMealId
    if (entryMealId != null) {
        val isEdit = editingFoodId != null
        val mealLabel = selectedLog?.meals?.firstOrNull { it.id == entryMealId }?.name ?: "Meal"
        val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.85f).dp
        Dialog(
            onDismissRequest = { dismissFoodEntry() },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md)
                    .imePadding(),
                level = GlassLevel.Level2,
                borderAlpha = 0.25f,
                cornerRadius = 16.dp,
            ) {
                Box(modifier = Modifier.heightIn(max = maxHeight)) {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                foodEntryDialogTitle(isEdit),
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                color = SystemPrimary,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                mealLabel,
                                fontFamily = JetBrainsMono,
                                color = colors.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                        IconButton(
                            onClick = { dismissFoodEntry() },
                            modifier = Modifier.align(Alignment.TopEnd),
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = colors.onSurfaceVariant,
                            )
                        }
                    }
                    FoodEntryForm(
                        foodEntryMode = foodEntryMode,
                        onModeChange = { switchFoodEntryMode(it) },
                        catalogFilter = catalogFilter,
                        onCatalogFilterChange = {
                            catalogFilter = it
                            formError = null
                        },
                        selectedCatalog = selectedCatalog,
                        onSelectCatalog = { entry ->
                            selectedCatalog = entry
                            foodName = entry.name
                            foodUnit = FoodMacroScaler.defaultUnit(entry)
                            foodQty = FoodMacroScaler.defaultQuantity(entry).toInt().toString()
                            applyCatalogMacros(entry, foodQty)
                            catalogFilter = entry.name
                            formError = null
                        },
                        foodName = foodName,
                        onFoodNameChange = {
                            foodName = it
                            formError = null
                        },
                        foodQty = foodQty,
                        onFoodQtyChange = {
                            foodQty = it
                            formError = null
                            selectedCatalog?.let { entry ->
                                if (!macrosManualOverride) applyCatalogMacros(entry, it)
                            }
                        },
                        foodUnit = foodUnit,
                        onFoodUnitChange = {
                            foodUnit = it
                            formError = null
                        },
                        foodCal = foodCal,
                        onFoodCalChange = {
                            foodCal = it
                            macrosManualOverride = true
                            formError = null
                        },
                        foodP = foodP,
                        onFoodPChange = {
                            foodP = it
                            macrosManualOverride = true
                            formError = null
                        },
                        foodC = foodC,
                        onFoodCChange = {
                            foodC = it
                            macrosManualOverride = true
                            formError = null
                        },
                        foodF = foodF,
                        onFoodFChange = {
                            foodF = it
                            macrosManualOverride = true
                            formError = null
                        },
                        formError = formError,
                    )
                    SystemActionButton(
                        label = foodEntryConfirmLabel(isEdit),
                        onClick = {
                            if (!canWrite) return@SystemActionButton
                            if (foodEntryMode == "Catalog" &&
                                selectedCatalog != null &&
                                !macrosManualOverride
                            ) {
                                applyCatalogMacros(selectedCatalog!!, foodQty)
                            }
                            val err = validateFoodEntry(
                                catalogMode = foodEntryMode == "Catalog",
                                catalogSelected = selectedCatalog != null,
                                foodName = foodName,
                                foodQty = foodQty,
                                foodUnit = foodUnit,
                                foodCal = foodCal,
                                foodP = foodP,
                                foodC = foodC,
                                foodF = foodF,
                            )
                            if (err != null) {
                                formError = err
                                return@SystemActionButton
                            }
                            onUpsertFood(
                                entryMealId,
                                foodItemFromForm(
                                    id = editingFoodId ?: 0L,
                                    name = foodName,
                                    quantity = foodQty,
                                    unit = foodUnit,
                                    calories = foodCal,
                                    protein = foodP,
                                    carbs = foodC,
                                    fat = foodF,
                                ),
                            )
                            foodMealId = null
                            editingFoodId = null
                            clearFoodForm()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    }
                }
            }
        }
    }

    mealPendingDelete?.let { meal ->
        val reversesProgress = deletingMealReversesProgress(selectedLog, meal.id)
        SystemConfirmDialog(
            title = SystemMessages.DELETE_ENTRY_TITLE,
            explanation = DELETE_MEAL_CONFIRM_DETAIL,
            consequence = if (reversesProgress) {
                SystemMessages.DELETE_ENTRY_PROGRESS_CONSEQUENCE
            } else {
                ""
            },
            confirmLabel = SystemMessages.DELETE_ENTRY_CONFIRM,
            cancelLabel = SystemMessages.DELETE_ENTRY_KEEP,
            onDismiss = { mealPendingDelete = null },
            onConfirm = {
                mealPendingDelete = null
                onDeleteMeal(meal.id)
            },
        )
    }

    foodPendingDelete?.let { (mealId, foodId) ->
        val reversesProgress = deletingFoodReversesProgress(selectedLog, mealId, foodId)
        SystemConfirmDialog(
            title = SystemMessages.DELETE_ENTRY_TITLE,
            explanation = if (reversesProgress) {
                SystemMessages.DELETE_ENTRY_PROGRESS_EXPLANATION
            } else {
                SystemMessages.DELETE_ENTRY_NO_PROGRESS_EXPLANATION
            },
            consequence = if (reversesProgress) SystemMessages.DELETE_ENTRY_PROGRESS_CONSEQUENCE else "",
            confirmLabel = SystemMessages.DELETE_ENTRY_CONFIRM,
            cancelLabel = SystemMessages.DELETE_ENTRY_KEEP,
            onDismiss = { foodPendingDelete = null },
            onConfirm = {
                foodPendingDelete = null
                onDeleteFood(mealId, foodId)
            },
        )
    }

    if (repeatPickerOpen) {
        AlertDialog(
            onDismissRequest = { repeatPickerOpen = false },
            containerColor = colors.surface.copy(alpha = 0.95f),
            title = {
                Text(
                    text = "[ COPY WHICH MEAL ]",
                    fontFamily = JetBrainsMono,
                    color = SystemPrimary,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    repeatableMeals(history).forEach { option ->
                        Text(
                            repeatMealOptionLabel(option, dietDate),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    repeatSource = option
                                    repeatDestName = ""
                                    repeatPickerOpen = false
                                }
                                .padding(vertical = Spacing.sm),
                            fontFamily = JetBrainsMono,
                            color = SystemPrimary,
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                GhostTextButton(label = "CANCEL", onClick = { repeatPickerOpen = false })
            },
        )
    }

    repeatSource?.let { source ->
        if (!repeatPickerOpen) {
            AlertDialog(
                onDismissRequest = {
                    repeatSource = null
                    repeatDestName = ""
                },
                containerColor = colors.surface.copy(alpha = 0.95f),
                title = {
                    Text(
                        text = "[ SAVE AS ]",
                        fontFamily = JetBrainsMono,
                        color = SystemPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Text(
                            "Copy ${source.meal.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        ) {
                            DIET_MEAL_CATEGORIES.forEach { preset ->
                                SovereignChip(
                                    label = preset,
                                    selected = repeatDestName == preset,
                                    onClick = { repeatDestName = preset },
                                )
                            }
                        }
                        OutlinedTextField(
                            repeatDestName,
                            { repeatDestName = it },
                            label = { Text("Meal name") },
                            placeholder = { Text("Lunch") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                confirmButton = {
                    SystemActionButton(
                        label = "REPEAT",
                        onClick = {
                            if (!canWrite) return@SystemActionButton
                            EntryValidation.requireNonBlank(repeatDestName, "meal name")?.let {
                                onMessage(it)
                                return@SystemActionButton
                            }
                            val dest = repeatDestName.trim()
                            val meal = source.meal
                            repeatSource = null
                            repeatDestName = ""
                            selectedCategory = dest
                            onRepeatMeal(meal, dest)
                        },
                    )
                },
                dismissButton = {
                    GhostTextButton(
                        label = "CANCEL",
                        onClick = {
                            repeatSource = null
                            repeatDestName = ""
                        },
                    )
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoodEntryForm(
    foodEntryMode: String,
    onModeChange: (String) -> Unit,
    catalogFilter: String,
    onCatalogFilterChange: (String) -> Unit,
    selectedCatalog: FoodCatalogEntry?,
    onSelectCatalog: (FoodCatalogEntry) -> Unit,
    foodName: String,
    onFoodNameChange: (String) -> Unit,
    foodQty: String,
    onFoodQtyChange: (String) -> Unit,
    foodUnit: String,
    onFoodUnitChange: (String) -> Unit,
    foodCal: String,
    onFoodCalChange: (String) -> Unit,
    foodP: String,
    onFoodPChange: (String) -> Unit,
    foodC: String,
    onFoodCChange: (String) -> Unit,
    foodF: String,
    onFoodFChange: (String) -> Unit,
    formError: FoodEntryError?,
) {
    val colors = MaterialTheme.colorScheme
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = SystemPrimary,
        unfocusedBorderColor = SystemPrimary.copy(alpha = 0.3f),
        cursorColor = SystemPrimary,
        errorBorderColor = SystemError,
        errorCursorColor = SystemError,
        errorLabelColor = SystemError,
        errorSupportingTextColor = SystemError,
    )
    val catalogMode = foodEntryMode == "Catalog"
    fun message(field: FoodEntryField): String? =
        formError?.takeIf { it.field == field }?.message
    val searchHits = if (catalogMode && catalogFilter.isNotBlank() &&
        selectedCatalog?.name?.equals(catalogFilter.trim(), ignoreCase = true) != true
    ) {
        FoodCatalog.search(catalogFilter)
    } else {
        emptyList()
    }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        if (catalogMode) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                OutlinedTextField(
                    catalogFilter,
                    onCatalogFilterChange,
                    placeholder = { Text("Search foods") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant,
                        )
                    },
                    singleLine = true,
                    isError = message(FoodEntryField.Catalog) != null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                )
                FoodFieldSupporting(message(FoodEntryField.Catalog))
                if (searchHits.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SystemPrimary.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                    ) {
                        searchHits.forEach { entry ->
                            Text(
                                entry.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectCatalog(entry) }
                                    .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
                                fontFamily = JetBrainsMono,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                } else if (catalogFilter.isNotBlank() &&
                    selectedCatalog?.name?.equals(catalogFilter.trim(), ignoreCase = true) != true
                ) {
                    Text(
                        "No foods match",
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    "QUICK PICKS",
                    fontFamily = JetBrainsMono,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    letterSpacing = 1.6.sp,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    FOOD_QUICK_PICKS.forEach { pick ->
                        SovereignChip(
                            label = pick.label,
                            selected = selectedCatalog?.id == pick.id,
                            onClick = {
                                FoodCatalog.findById(pick.id)?.let { onSelectCatalog(it) }
                            },
                        )
                    }
                }
                Text(
                    "Custom food",
                    fontFamily = JetBrainsMono,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clickable { onModeChange("Others") }
                        .padding(vertical = Spacing.xs),
                )
            }
        } else {
            OutlinedTextField(
                foodName,
                onFoodNameChange,
                label = { Text("Food name") },
                placeholder = { Text("e.g. Chicken breast") },
                isError = message(FoodEntryField.Name) != null,
                supportingText = { FoodFieldSupporting(message(FoodEntryField.Name)) },
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors,
            )
            Text(
                "Use catalog",
                fontFamily = JetBrainsMono,
                style = MaterialTheme.typography.labelSmall,
                color = SystemPrimary,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable { onModeChange("Catalog") }
                    .padding(vertical = Spacing.xs),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                "AMOUNT",
                fontFamily = JetBrainsMono,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                letterSpacing = 1.6.sp,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FoodBracketField(
                    value = foodQty,
                    onValueChange = onFoodQtyChange,
                    isError = message(FoodEntryField.Quantity) != null,
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                )
                FoodUnitField(
                    value = foodUnit,
                    onValueChange = onFoodUnitChange,
                    isError = message(FoodEntryField.Unit) != null,
                    modifier = Modifier.weight(1f),
                )
            }
            FoodFieldSupporting(message(FoodEntryField.Quantity) ?: message(FoodEntryField.Unit))
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                "NUTRITION",
                fontFamily = JetBrainsMono,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                letterSpacing = 1.6.sp,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SystemPrimary.copy(alpha = 0.25f), RoundedCornerShape(8.dp)),
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    FoodNutritionCell(
                        label = "Calories",
                        value = foodCal,
                        unit = "kcal",
                        editable = !catalogMode,
                        isError = message(FoodEntryField.Calories) != null,
                        onValueChange = onFoodCalChange,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(64.dp)
                            .background(SystemPrimary.copy(alpha = 0.2f)),
                    )
                    FoodNutritionCell(
                        label = "Protein",
                        value = foodP,
                        unit = "g",
                        editable = !catalogMode,
                        isError = message(FoodEntryField.Protein) != null,
                        onValueChange = onFoodPChange,
                        modifier = Modifier.weight(1f),
                    )
                }
                HorizontalDivider(color = SystemPrimary.copy(alpha = 0.2f))
                Row(modifier = Modifier.fillMaxWidth()) {
                    FoodNutritionCell(
                        label = "Carbs",
                        value = foodC,
                        unit = "g",
                        editable = !catalogMode,
                        isError = message(FoodEntryField.Carbs) != null,
                        onValueChange = onFoodCChange,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(64.dp)
                            .background(SystemPrimary.copy(alpha = 0.2f)),
                    )
                    FoodNutritionCell(
                        label = "Fat",
                        value = foodF,
                        unit = "g",
                        editable = !catalogMode,
                        isError = message(FoodEntryField.Fat) != null,
                        onValueChange = onFoodFChange,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            FoodFieldSupporting(
                message(FoodEntryField.Calories)
                    ?: message(FoodEntryField.Protein)
                    ?: message(FoodEntryField.Carbs)
                    ?: message(FoodEntryField.Fat),
            )
        }
    }
}

@Composable
private fun FoodBracketField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            "[",
            color = if (isError) SystemError else SystemPrimary,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge,
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = SystemPrimary,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
            ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            cursorBrush = SolidColor(SystemPrimary),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = Spacing.xs),
        )
        Text(
            "]",
            color = SystemPrimary,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoodUnitField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val units = foodAmountUnits(value)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            "[",
            color = if (isError) SystemError else SystemPrimary,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge,
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.weight(1f),
        ) {
            BasicTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = SystemPrimary,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.xs),
                decorationBox = { inner ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            inner()
                        }
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    }
                },
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                units.forEach { unit ->
                    DropdownMenuItem(
                        text = { Text(unit, fontFamily = JetBrainsMono) },
                        onClick = {
                            onValueChange(unit)
                            expanded = false
                        },
                    )
                }
            }
        }
        Text(
            "]",
            color = SystemPrimary,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun FoodNutritionCell(
    label: String,
    value: String,
    unit: String,
    editable: Boolean,
    isError: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val display = value.ifBlank { "0" }
    Column(
        modifier = modifier.padding(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            label,
            fontFamily = JetBrainsMono,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            if (editable) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = if (isError) SystemError else SystemPrimary,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    cursorBrush = SolidColor(SystemPrimary),
                    modifier = Modifier.width(64.dp),
                )
            } else {
                Text(
                    display,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    color = SystemPrimary,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                " $unit",
                fontFamily = JetBrainsMono,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
    }
}

@Composable
private fun FoodFieldSupporting(message: String?) {
    if (message != null) {
        Text(
            message,
            color = SystemError,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/** Formats a biometric measure with a spaced unit for display. */
internal fun formatBiometricMeasure(value: String, unit: String): String {
    val v = value.trim()
    val u = unit.trim()
    if (v.isEmpty()) return ""
    if (u.isEmpty()) return v
    return "$v $u"
}

/** Formats a fitness goal id for display (underscores → spaces). */
internal fun formatFitnessGoalDisplay(goal: String): String =
    goal.trim().replace('_', ' ')
