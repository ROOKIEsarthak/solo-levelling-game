package com.example.solo_levelling.ui.fitness

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.data.db.entity.DietLogEntity
import com.example.solo_levelling.data.db.entity.FoodItemEntity
import com.example.solo_levelling.data.db.entity.LoggedExerciseEntity
import com.example.solo_levelling.data.db.entity.LoggedSetEntity
import com.example.solo_levelling.data.db.entity.MealEntity
import com.example.solo_levelling.data.db.entity.NutritionLogEntity
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
import com.example.solo_levelling.domain.service.EntryValidation
import com.example.solo_levelling.domain.service.FoodMacroScaler
import com.example.solo_levelling.domain.service.ModuleService
import com.example.solo_levelling.domain.service.WorkoutProgressLogic
import com.example.solo_levelling.domain.service.WorkoutSplitLogic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.example.solo_levelling.ui.components.BracketLabel
import com.example.solo_levelling.ui.components.CyberProgressBar
import com.example.solo_levelling.ui.components.EnergyFieldBackground
import com.example.solo_levelling.ui.components.GhostTextButton
import com.example.solo_levelling.ui.components.GlassLevel
import com.example.solo_levelling.ui.components.GlassSurface
import com.example.solo_levelling.ui.components.SovereignChip
import com.example.solo_levelling.ui.components.SystemActionButton
import com.example.solo_levelling.ui.components.SystemIdleEmpty
import com.example.solo_levelling.ui.components.SystemSectionHeader
import com.example.solo_levelling.ui.components.progressFraction
import com.example.solo_levelling.ui.theme.JetBrainsMono
import com.example.solo_levelling.ui.theme.Spacing
import com.example.solo_levelling.ui.theme.SystemPrimary
import com.example.solo_levelling.ui.theme.SystemSecondary
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

private val WEEK_DAYS = listOf(
    "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
)

internal fun defaultTrainingTab(workoutSplitId: String?): String =
    if (workoutSplitId != null && workoutSplitId.isBlank()) "Routine" else "Today"

internal fun resolvedTrainingTab(userTab: String?, workoutSplitId: String?): String =
    userTab ?: defaultTrainingTab(workoutSplitId)

enum class FitnessTab { Workout, Diet, Both }

@Composable
fun FitnessScreen(
    container: AppContainer,
    tab: FitnessTab = FitnessTab.Both,
    onMessage: (String) -> Unit = {},
) {
    val vm: FitnessViewModel = viewModel(factory = FitnessViewModel.factory(container))
    val workouts by vm.workouts.collectAsStateWithLifecycle()
    val workoutRoutine by vm.workoutRoutine.collectAsStateWithLifecycle()
    val nutritionToday by vm.nutritionToday.collectAsStateWithLifecycle()
    val workoutLogToday by vm.workoutLogToday.collectAsStateWithLifecycle()
    val dietLogs by vm.dietLogs.collectAsStateWithLifecycle()
    val selectedWorkoutLog by vm.selectedWorkoutLog.collectAsStateWithLifecycle()
    val selectedDietLog by vm.selectedDietLog.collectAsStateWithLifecycle()
    val todayDate by vm.todayDate.collectAsStateWithLifecycle()
    val selectedWorkoutDate by vm.selectedWorkoutDate.collectAsStateWithLifecycle()
    val selectedDietDate by vm.selectedDietDate.collectAsStateWithLifecycle()
    val heightCm by vm.heightCm.collectAsStateWithLifecycle()
    val weightKg by vm.weightKg.collectAsStateWithLifecycle()
    val bmiEstimate by vm.bmiEstimate.collectAsStateWithLifecycle()
    val fitnessGoal by vm.fitnessGoal.collectAsStateWithLifecycle()
    val proteinTarget by vm.proteinTarget.collectAsStateWithLifecycle()
    val carbTarget by vm.carbTarget.collectAsStateWithLifecycle()
    val fatTarget by vm.fatTarget.collectAsStateWithLifecycle()
    val calorieTarget by vm.calorieTarget.collectAsStateWithLifecycle()
    val workoutSplitId by vm.workoutSplitId.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val splitLocked = workoutSplitId?.isNotBlank() == true

    DisposableEffect(Unit) {
        vm.resetWorkoutDateToToday()
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
                    FitnessTab.Workout -> "Today's training"
                    FitnessTab.Diet -> "Today's fuel"
                    FitnessTab.Both -> "Fitness"
                },
                accent = SystemPrimary,
            )

            if (tab == FitnessTab.Workout || tab == FitnessTab.Both) {
            FitnessSection(
                routine = workoutRoutine,
                todaySummary = workoutLogToday,
                nutritionToday = nutritionToday?.let { "Calories: ${it.calories}" },
                workoutDate = workoutDate,
                selectedLog = selectedWorkoutLog,
                history = workouts,
                heightCm = heightCm,
                weightKg = weightKg,
                bmiEstimate = bmiEstimate,
                fitnessGoal = fitnessGoal,
                splitLocked = splitLocked,
                workoutSplitId = workoutSplitId,
                todayDate = todayDate,
                onApplySplit = { splitId, dayMapCsv, confirmEarly ->
                    scope.launch {
                        val err = container.modules.applyWorkoutSplit(
                            splitId,
                            dayMapCsv,
                            confirmEarlyChange = confirmEarly,
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
                        container.modules.saveRoutineDay(day, plan)
                        onMessage("Routine day saved")
                    }
                },
                onRestDay = { day -> scope.launch { container.modules.setRestDay(day) } },
                onUpsertPlanned = { day, ex, name ->
                    scope.launch {
                        container.modules.upsertPlannedExercise(day, ex, name)
                        onMessage("Exercise added")
                    }
                },
                onRemovePlanned = { day, id -> scope.launch { container.modules.removePlannedExercise(day, id) } },
                onReorderPlanned = { day, id, up ->
                    scope.launch { container.modules.reorderPlannedExercise(day, id, up) }
                },
                onStartLog = {
                    scope.launch {
                        container.modules.startOrGetWorkoutLog(workoutDate)
                    }
                },
                onSaveLog = { log ->
                    scope.launch {
                        val sets = log.exercises.sumOf { it.sets.size }
                        if (sets == 0) {
                            onMessage("Log at least one set before finishing")
                            return@launch
                        }
                        container.modules.upsertWorkoutLog(log)
                        onMessage("Workout saved · $sets sets")
                    }
                },
                onCompleteRestDay = { activeRest ->
                    scope.launch {
                        container.modules.completeRestDay(workoutDate, activeRest)
                        onMessage(
                            if (activeRest) "Active rest logged. Recovery counts."
                            else "Rest day complete.",
                        )
                    }
                },
                onDeleteLog = { scope.launch { container.modules.deleteWorkoutLog(workoutDate) } },
                onRemoveExercise = { id ->
                    scope.launch { container.modules.removeExerciseFromLog(workoutDate, id) }
                },
                onUpsertExercise = { ex ->
                    scope.launch { container.modules.upsertLoggedExercise(workoutDate, ex) }
                },
                loadExerciseHistory = { name -> container.modules.exerciseHistory(name) },
                onRepeatLast = {
                    scope.launch {
                        if (splitLocked) {
                            onMessage("Open today's workout to log your split")
                            return@launch
                        }
                        val last = workouts.firstOrNull { it.date != workoutDate }
                        if (last != null) {
                            container.modules.upsertWorkoutLog(
                                last.copy(date = workoutDate, id = 0),
                            )
                            onMessage("Same workout started")
                        } else {
                            onMessage("No previous workout.\nStart your first session.")
                        }
                    }
                },
                onMessage = onMessage,
            )
        }

        if (tab == FitnessTab.Diet || tab == FitnessTab.Both) {
            DietSection(
                dietDate = dietDate,
                selectedLog = selectedDietLog,
                history = dietLogs,
                todayTotals = nutritionToday,
                calorieTarget = calorieTarget,
                proteinTarget = proteinTarget,
                carbTarget = carbTarget,
                fatTarget = fatTarget,
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
                        val id = container.modules.addMeal(dietDate, name)
                        afterCreated(id)
                    }
                },
                onDeleteMeal = { id -> scope.launch { container.modules.deleteMeal(dietDate, id) } },
                onUpsertFood = { mealId, food ->
                    scope.launch {
                        container.modules.upsertFood(dietDate, mealId, food)
                        onMessage("Food saved")
                    }
                },
                onDeleteFood = { mealId, foodId ->
                    scope.launch { container.modules.deleteFood(dietDate, mealId, foodId) }
                },
                mealTotals = { meal -> container.modules.mealTotals(meal) },
                onRepeatMeal = {
                    scope.launch {
                        val yesterday = dietLogs.firstOrNull { it.date != dietDate }
                        val meal = yesterday?.meals?.lastOrNull()
                        if (meal != null && dietDate.isNotBlank()) {
                            if (meal.foods.isEmpty()) {
                                onMessage("Add at least one food to the meal")
                                return@launch
                            }
                            val id = container.modules.addMeal(dietDate, meal.name)
                            meal.foods.forEach { food ->
                                container.modules.upsertFood(dietDate, id, food.copy(id = 0))
                            }
                            onMessage("Meal repeated")
                        } else {
                            onMessage("No meals logged.\nStart recording today's nutrition.")
                        }
                    }
                },
            )
        }

            Spacer(Modifier.height(Spacing.xs))
        }
    }
}

@Composable
private fun BiometricPanel(label: String, value: String, modifier: Modifier = Modifier) {
    GlassSurface(
        modifier = modifier,
        level = GlassLevel.Level1,
        cornerRadius = 8.dp,
        contentPadding = Spacing.xs,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = SystemSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                value,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                color = SystemPrimary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MacroGlassCard(label: String, current: Int, target: Int, unit: String, modifier: Modifier = Modifier) {
    GlassSurface(modifier = modifier, level = GlassLevel.Level1, cornerRadius = 10.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            BracketLabel(text = label, color = SystemPrimary)
            Text(
                "$current",
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                color = SystemPrimary,
                style = MaterialTheme.typography.titleMedium,
            )
            CyberProgressBar(progress = progressFraction(current.toFloat(), target.toFloat()), height = 6.dp)
            Text(
                "/ $target $unit",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = JetBrainsMono,
            )
        }
    }
}

@Composable
private fun DateStrip(
    label: String,
    isToday: Boolean = false,
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
            IconButton(onClick = onNext) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next day",
                    tint = SystemPrimary,
                )
            }
        }
    }
}

@Composable
private fun FitnessSection(
    routine: WorkoutRoutineEntity,
    todaySummary: WorkoutLogEntity?,
    nutritionToday: String?,
    workoutDate: String,
    todayDate: String,
    selectedLog: WorkoutLogEntity?,
    history: List<WorkoutLogEntity>,
    heightCm: String,
    weightKg: String,
    bmiEstimate: String,
    fitnessGoal: String,
    splitLocked: Boolean = false,
    workoutSplitId: String? = null,
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
    onRepeatLast: () -> Unit = {},
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
    var setWeight by remember { mutableStateOf("") }
    var setReps by remember { mutableStateOf("") }
    var setRpe by remember { mutableStateOf("") }
    var editingExerciseId by remember { mutableStateOf<Long?>(null) }
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
    val scope = rememberCoroutineScope()

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
    LaunchedEffect(splitLocked, workoutDate) {
        if (splitLocked && workoutDate.isNotBlank()) {
            onStartLog()
        }
    }
    LaunchedEffect(splitLocked) {
        if (splitLocked) {
            weeksHeld = weeksOnSplit()
        }
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
                        "Active recovery can help you stay consistent without turning every day into a hard training day.",
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
            if (!splitLocked) {
                GlassSurface(modifier = Modifier.fillMaxWidth(), level = GlassLevel.Level2, borderAlpha = 0.35f) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (todaySummary == null) {
                            Text(
                                "YOUR SYSTEM IS READY.\nNo workout has been logged yet.\nYour first session starts here.",
                                color = colors.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        } else {
                            Text(
                                todaySummary.workoutName,
                                fontWeight = FontWeight.Bold,
                                color = SystemPrimary,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                "${todaySummary.exercises.size} exercises · " +
                                    "${todaySummary.exercises.sumOf { it.sets.size }} sets",
                                fontFamily = JetBrainsMono,
                                color = colors.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        nutritionToday?.let {
                            Text("Diet tip: $it", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                        SystemActionButton(
                            label = "START SAME WORKOUT",
                            onClick = onRepeatLast,
                            modifier = Modifier.fillMaxWidth(),
                            primary = false,
                        )
                    }
                }
            }

            val biometricPanels = buildList {
                if (heightCm.isNotBlank()) {
                    add("Height" to formatBiometricMeasure(heightCm, "cm"))
                }
                if (weightKg.isNotBlank()) {
                    add("Weight" to formatBiometricMeasure(weightKg, "kg"))
                }
                if (bmiEstimate.isNotBlank()) {
                    add("BMI" to bmiEstimate)
                }
                if (fitnessGoal.isNotBlank()) {
                    add("Goal" to formatFitnessGoalDisplay(fitnessGoal))
                }
            }
            if (biometricPanels.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    biometricPanels.chunked(2).forEach { row ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            row.forEach { (label, value) ->
                                BiometricPanel(
                                    label = label,
                                    value = value,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (row.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

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
                        onPrev = onPrevDate,
                        onNext = onNextDate,
                    )
                    if (!splitLocked) {
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
                            "Recovery is part of progress.",
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
                        } else {
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
                        if (splitLocked) {
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
                        log.exercises.forEach { ex ->
                            val planned = routine.day(log.dayOfWeek.ifBlank {
                                selectedDayKey
                            }).exercises.firstOrNull { it.name.equals(ex.name, ignoreCase = true) }
                            Text(ex.name.uppercase(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            if (planned != null) {
                                Text(
                                    "Plan: ${planned.sets}×${planned.repRange.min}-${planned.repRange.max}",
                                    color = colors.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                            val previous = exercisePrevious[ex.id]
                                ?: if (editingExerciseId == ex.id) {
                                    compareHistory.firstOrNull { it.first != workoutDate }?.second
                                } else {
                                    null
                                }
                            val prevBest = previous?.sets?.maxOfOrNull { it.weight } ?: 0f
                            if (previous != null && previous.sets.isNotEmpty()) {
                                Text("Previous:", color = colors.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                                previous.sets.forEach { s ->
                                    Text(
                                        "  ${s.weight.toInt()} × ${s.reps}",
                                        color = colors.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                val currentBest = ex.sets.maxOfOrNull { it.weight } ?: 0f
                                when {
                                    WorkoutProgressLogic.isPr(currentBest, prevBest) -> {
                                        Text("PR", color = colors.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                    }
                                    ex.sets.isNotEmpty() && prevBest > 0f -> {
                                        Text(
                                            WorkoutProgressLogic.compareSets(prevBest, ex.sets.last().weight),
                                            color = colors.primary,
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }
                            }
                            Text("TODAY", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            ex.sets.forEachIndexed { i, s ->
                                val hint = if (prevBest > 0f) {
                                    when {
                                        WorkoutProgressLogic.isPr(s.weight, prevBest) && s.weight == ex.sets.maxOfOrNull { it.weight } -> " PR"
                                        else -> ""
                                    }
                                } else {
                                    ""
                                }
                                Text(
                                    "  Set ${i + 1}: ${s.weight}kg × ${s.reps}$hint" +
                                        (s.rpe?.let { " @ RPE $it" } ?: ""),
                                )
                            }
                            if (!dayComplete) {
                                OutlinedButton(
                                    onClick = {
                                        editingExerciseId = ex.id
                                        scope.launch {
                                            val hist = loadExerciseHistory(ex.name)
                                            compareHistory = hist
                                            val prev = hist.firstOrNull { it.first != workoutDate }?.second
                                            val last = ex.sets.lastOrNull() ?: prev?.sets?.lastOrNull()
                                            if (last != null) {
                                                setWeight = last.weight.toString()
                                                setReps = last.reps.toString()
                                            }
                                        }
                                    },
                                    modifier = Modifier.height(48.dp),
                                    border = BorderStroke(1.dp, SystemPrimary.copy(alpha = 0.5f)),
                                ) {
                                    Text("+ ADD SET", fontFamily = JetBrainsMono, color = SystemPrimary)
                                }
                            }
                            if (!splitLocked) {
                                SystemActionButton(label = "DELETE EXERCISE", onClick = { onRemoveExercise(ex.id) }, primary = false)
                            }
                        }
                        if (!dayComplete && editingExerciseId != null) {
                            val target = log.exercises.firstOrNull { it.id == editingExerciseId }
                            if (target != null) {
                                val editPrevBest = exercisePrevious[target.id]?.sets?.maxOfOrNull { it.weight } ?: 0f
                                OutlinedTextField(setWeight, { setWeight = it }, label = { Text("Weight") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(setReps, { setReps = it }, label = { Text("Reps") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(setRpe, { setRpe = it }, label = { Text("RPE (optional)") }, modifier = Modifier.fillMaxWidth())
                                val enteredWeight = setWeight.toFloatOrNull()
                                if (enteredWeight != null && editPrevBest > 0f) {
                                    val hint = if (WorkoutProgressLogic.isPr(enteredWeight, editPrevBest)) {
                                        "PR"
                                    } else {
                                        WorkoutProgressLogic.compareSets(editPrevBest, enteredWeight)
                                    }
                                    Text(hint, color = colors.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                }
                                SystemActionButton(
                                    label = "SAVE SET",
                                    onClick = {
                                        EntryValidation.requirePositiveFloat(setWeight, "weight")?.let {
                                            onMessage(it)
                                            return@SystemActionButton
                                        }
                                        EntryValidation.requirePositiveInt(setReps, "reps")?.let {
                                            onMessage(it)
                                            return@SystemActionButton
                                        }
                                        val w = setWeight.toFloat()
                                        val r = setReps.toInt()
                                        val rpe = setRpe.toFloatOrNull()
                                        onUpsertExercise(
                                            target.copy(
                                                sets = target.sets + LoggedSetEntity(w, r, rpe),
                                            ),
                                        )
                                        setWeight = ""
                                        setReps = ""
                                        setRpe = ""
                                        editingExerciseId = null
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        if (!splitLocked) {
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
                        if (!dayComplete) {
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
                        if (!splitLocked) {
                            SystemActionButton(label = "DELETE", onClick = onDeleteLog, primary = false, modifier = Modifier.fillMaxWidth())
                        }
                        if (splitLocked) {
                            GhostTextButton(
                                label = "Change Split",
                                onClick = { userTab = "Change Split" },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DietSection(
    dietDate: String,
    selectedLog: DietLogEntity?,
    history: List<DietLogEntity>,
    todayTotals: NutritionLogEntity?,
    calorieTarget: Int,
    proteinTarget: Int,
    carbTarget: Int,
    fatTarget: Int,
    onMessage: (String) -> Unit,
    onPrevDate: () -> Unit,
    onNextDate: () -> Unit,
    onAddMeal: (String, (Long) -> Unit) -> Unit,
    onDeleteMeal: (Long) -> Unit,
    onUpsertFood: (Long, FoodItemEntity) -> Unit,
    onDeleteFood: (Long, Long) -> Unit,
    mealTotals: (MealEntity) -> NutritionTotalsEntity,
    onRepeatMeal: () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = colors.primary.copy(alpha = 0.15f),
        selectedLabelColor = colors.primary,
    )
    var tab by remember { mutableStateOf("Log Food") }
    var mealName by remember { mutableStateOf("") }
    var foodMealId by remember { mutableStateOf<Long?>(null) }
    var foodEntryMode by remember { mutableStateOf("Catalog") }
    var foodName by remember { mutableStateOf("") }
    var foodQty by remember { mutableStateOf("") }
    var foodUnit by remember { mutableStateOf("g") }
    var foodCal by remember { mutableStateOf("") }
    var foodP by remember { mutableStateOf("") }
    var foodC by remember { mutableStateOf("") }
    var foodF by remember { mutableStateOf("") }
    var selectedCatalog by remember { mutableStateOf<FoodCatalogEntry?>(null) }
    var catalogExpanded by remember { mutableStateOf(false) }
    var catalogFilter by remember { mutableStateOf("") }
    var macrosManualOverride by remember { mutableStateOf(false) }

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
        catalogExpanded = false
        macrosManualOverride = false
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

    GlassSurface(modifier = Modifier.fillMaxWidth(), level = GlassLevel.Level1) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (todayTotals != null) {
                GlassSurface(modifier = Modifier.fillMaxWidth(), level = GlassLevel.Level2, borderAlpha = 0.35f) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "${todayTotals.calories} / $calorieTarget kcal",
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            color = SystemPrimary,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        CyberProgressBar(
                            progress = progressFraction(todayTotals.calories.toFloat(), calorieTarget.toFloat()),
                        )
                        Text(
                            "Remaining ${(calorieTarget - todayTotals.calories).coerceAtLeast(0)} kcal",
                            color = colors.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = JetBrainsMono,
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MacroGlassCard("PRO", todayTotals.protein, proteinTarget, "g", Modifier.weight(1f))
                    MacroGlassCard("CRB", todayTotals.carbs, carbTarget, "g", Modifier.weight(1f))
                    MacroGlassCard("FAT", todayTotals.fat, fatTarget, "g", Modifier.weight(1f))
                }
            } else {
                SystemIdleEmpty(
                    title = "No meals logged",
                    subtitle = "Start recording today's nutrition.",
                    actionLabel = "ADD FIRST MEAL",
                    onAction = { tab = "Log Food" },
                )
            }

            SystemActionButton(
                label = "REPEAT MEAL",
                onClick = onRepeatMeal,
                modifier = Modifier.fillMaxWidth(),
                primary = false,
            )

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("Log Food", "History").forEach { label ->
                    SovereignChip(
                        label = label,
                        selected = tab == label,
                        onClick = { tab = label },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("Breakfast", "Lunch", "Dinner", "Snack", "Pre-workout", "Post-workout").forEach { preset ->
                    SovereignChip(
                        label = preset,
                        selected = mealName == preset,
                        onClick = { mealName = preset },
                    )
                }
            }

            when (tab) {
                "Log Food" -> {
                    DateStrip(label = dietDate, onPrev = onPrevDate, onNext = onNextDate)
                    val totals = todayTotals
                    if (totals != null) {
                        Text("Today vs targets", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        MacroProgressRow("Calories", totals.calories, calorieTarget, "kcal")
                        Text(
                            "Remaining ${(calorieTarget - totals.calories).coerceAtLeast(0)} kcal",
                            color = colors.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = JetBrainsMono,
                        )
                        MacroProgressRow("Protein", totals.protein, proteinTarget, "g")
                        MacroProgressRow("Carbs", totals.carbs, carbTarget, "g")
                        MacroProgressRow("Fat", totals.fat, fatTarget, "g")
                        Text(
                            "Estimates — not medical advice",
                            color = colors.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    OutlinedTextField(
                        mealName,
                        { mealName = it },
                        label = { Text("Meal name") },
                        placeholder = { Text("Pre-workout") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SystemActionButton(
                        label = "LOG MEAL",
                        onClick = {
                            EntryValidation.requireNonBlank(mealName, "meal name")?.let {
                                onMessage(it)
                                return@SystemActionButton
                            }
                            val name = mealName.trim()
                            mealName = ""
                            onAddMeal(name) { mealId ->
                                foodMealId = mealId
                                foodEntryMode = "Catalog"
                                clearFoodForm()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (selectedLog?.meals.isNullOrEmpty()) {
                        Text("No meals for this date", color = colors.onSurfaceVariant)
                    }

                    selectedLog?.meals?.forEach { meal ->
                        val mealT = mealTotals(meal)
                        GlassSurface(modifier = Modifier.fillMaxWidth(), level = GlassLevel.Level1, cornerRadius = 8.dp) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${meal.name}", fontWeight = FontWeight.Bold, color = SystemPrimary)
                                Text(
                                    "${mealT.calories} cal · P${mealT.protein} C${mealT.carbs} F${mealT.fat}",
                                    fontFamily = JetBrainsMono,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                meal.foods.forEach { food ->
                                    val qty = listOfNotNull(
                                        food.quantity?.let { q -> "${q}${food.unit.orEmpty()}" },
                                        food.calories?.let { "· ${it} cal" },
                                    ).joinToString(" ")
                                    Text("  ${food.name} $qty", style = MaterialTheme.typography.bodySmall)
                                    SystemActionButton(
                                        label = "DELETE FOOD",
                                        onClick = { onDeleteFood(meal.id, food.id) },
                                        primary = false,
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    SystemActionButton(
                                        label = "ADD FOOD",
                                        onClick = {
                                            foodMealId = meal.id
                                            foodEntryMode = "Catalog"
                                            clearFoodForm()
                                        },
                                        primary = false,
                                    )
                                    SystemActionButton(
                                        label = "DELETE MEAL",
                                        onClick = { onDeleteMeal(meal.id) },
                                        primary = false,
                                    )
                                }
                            }
                        }
                    }

                    foodMealId?.let { mealId ->
                        Text("New food", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Select a catalog food, or use Others for manual entry.",
                            color = colors.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf("Catalog", "Others").forEach { mode ->
                                FilterChip(
                                    selected = foodEntryMode == mode,
                                    onClick = { switchFoodEntryMode(mode) },
                                    label = { Text(mode) },
                                    colors = chipColors,
                                    border = BorderStroke(
                                        1.dp,
                                        if (foodEntryMode == mode) colors.primary else colors.outline,
                                    ),
                                )
                            }
                        }

                        if (foodEntryMode == "Catalog") {
                            OutlinedTextField(
                                catalogFilter,
                                { catalogFilter = it },
                                label = { Text("Filter foods") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            ExposedDropdownMenuBox(
                                expanded = catalogExpanded,
                                onExpandedChange = { catalogExpanded = it },
                            ) {
                                OutlinedTextField(
                                    value = selectedCatalog?.name ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Select food") },
                                    placeholder = { Text("Choose from list") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = catalogExpanded)
                                    },
                                )
                                val filtered = FoodCatalog.search(catalogFilter)
                                ExposedDropdownMenu(
                                    expanded = catalogExpanded,
                                    onDismissRequest = { catalogExpanded = false },
                                ) {
                                    if (filtered.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("No foods match") },
                                            onClick = { catalogExpanded = false },
                                        )
                                    } else {
                                        filtered.forEach { entry ->
                                            DropdownMenuItem(
                                                text = { Text("${entry.name} · ${entry.category}") },
                                                onClick = {
                                                    selectedCatalog = entry
                                                    foodName = entry.name
                                                    foodUnit = FoodMacroScaler.defaultUnit(entry)
                                                    foodQty = FoodMacroScaler.defaultQuantity(entry)
                                                        .toInt()
                                                        .toString()
                                                    applyCatalogMacros(entry, foodQty)
                                                    catalogExpanded = false
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                            OutlinedTextField(
                                foodQty,
                                {
                                    foodQty = it
                                    selectedCatalog?.let { entry ->
                                        if (!macrosManualOverride) applyCatalogMacros(entry, it)
                                    }
                                },
                                label = {
                                    Text(
                                        if (selectedCatalog != null &&
                                            !FoodMacroScaler.isPer100g(selectedCatalog!!.basis)
                                        ) {
                                            "Servings"
                                        } else {
                                            "Quantity"
                                        },
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                foodUnit,
                                { foodUnit = it },
                                label = { Text("Unit") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            selectedCatalog?.let { entry ->
                                val qty = foodQty.toFloatOrNull()
                                    ?: FoodMacroScaler.defaultQuantity(entry)
                                Text(
                                    FoodMacroScaler.previewLine(entry, qty),
                                    color = colors.primary,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            OutlinedTextField(
                                foodCal,
                                {
                                    foodCal = it
                                    macrosManualOverride = true
                                },
                                label = { Text("Calories") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                foodP,
                                {
                                    foodP = it
                                    macrosManualOverride = true
                                },
                                label = { Text("Protein (g)") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                foodC,
                                {
                                    foodC = it
                                    macrosManualOverride = true
                                },
                                label = { Text("Carbs (g)") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                foodF,
                                {
                                    foodF = it
                                    macrosManualOverride = true
                                },
                                label = { Text("Fat (g)") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            OutlinedTextField(
                                foodName,
                                { foodName = it },
                                label = { Text("Food name") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                foodQty,
                                { foodQty = it },
                                label = { Text("Quantity") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                foodUnit,
                                { foodUnit = it },
                                label = { Text("Unit") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                foodCal,
                                { foodCal = it },
                                label = { Text("Calories") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                foodP,
                                { foodP = it },
                                label = { Text("Protein (g)") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                foodC,
                                { foodC = it },
                                label = { Text("Carbs (g)") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                foodF,
                                { foodF = it },
                                label = { Text("Fat (g)") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        SystemActionButton(
                            label = "SAVE FOOD",
                            onClick = {
                                if (foodEntryMode == "Catalog") {
                                    if (selectedCatalog == null) {
                                        onMessage("Select a food from the catalog")
                                        return@SystemActionButton
                                    }
                                    EntryValidation.requirePositiveFloat(foodQty, "quantity")?.let {
                                        onMessage(it)
                                        return@SystemActionButton
                                    }
                                    if (!macrosManualOverride) {
                                        applyCatalogMacros(selectedCatalog!!, foodQty)
                                    }
                                    EntryValidation.firstError(
                                        EntryValidation.requireNonBlank(foodName, "food name"),
                                        EntryValidation.requireNonBlank(foodUnit, "unit"),
                                        EntryValidation.requireNonNegativeInt(foodCal, "calories"),
                                        EntryValidation.requireNonNegativeInt(foodP, "protein"),
                                        EntryValidation.requireNonNegativeInt(foodC, "carbs"),
                                        EntryValidation.requireNonNegativeInt(foodF, "fat"),
                                    )?.let {
                                        onMessage(it)
                                        return@SystemActionButton
                                    }
                                } else {
                                    EntryValidation.firstError(
                                        EntryValidation.requireNonBlank(foodName, "food name"),
                                        EntryValidation.requirePositiveFloat(foodQty, "quantity"),
                                        EntryValidation.requireNonBlank(foodUnit, "unit"),
                                        EntryValidation.requireNonNegativeInt(foodCal, "calories"),
                                        EntryValidation.requireNonNegativeInt(foodP, "protein"),
                                        EntryValidation.requireNonNegativeInt(foodC, "carbs"),
                                        EntryValidation.requireNonNegativeInt(foodF, "fat"),
                                    )?.let {
                                        onMessage(it)
                                        return@SystemActionButton
                                    }
                                }
                                onUpsertFood(
                                    mealId,
                                    FoodItemEntity(
                                        name = foodName.trim(),
                                        quantity = foodQty.toFloat(),
                                        unit = foodUnit.trim(),
                                        calories = foodCal.toInt(),
                                        protein = foodP.toInt(),
                                        carbs = foodC.toInt(),
                                        fat = foodF.toInt(),
                                    ),
                                )
                                clearFoodForm()
                                foodMealId = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        SystemActionButton(
                            label = "CANCEL",
                            onClick = {
                                val meal = selectedLog?.meals?.firstOrNull { it.id == mealId }
                                foodMealId = null
                                clearFoodForm()
                                if (meal != null && meal.foods.isEmpty()) {
                                    onDeleteMeal(mealId)
                                    onMessage("Add at least one food to the meal")
                                }
                            },
                            primary = false,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    selectedLog?.dailyTotals?.let { t ->
                        Text("Day total: ${t.calories} cal · P${t.protein} C${t.carbs} F${t.fat}")
                    }
                }

                "History" -> {
                    history.take(14).forEach { day ->
                        val t = day.dailyTotals
                        Text(
                            "${day.date} · ${day.meals.size} meals · " +
                                "${t.calories} cal P${t.protein} C${t.carbs} F${t.fat}",
                        )
                        day.meals.forEach { meal ->
                            Text(
                                "  ${meal.name}: ${meal.foods.joinToString { it.name }}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    if (history.isEmpty()) {
                        Text("No diet history yet", color = colors.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroProgressRow(label: String, current: Int, target: Int, unit: String) {
    val colors = MaterialTheme.colorScheme
    val progress = progressFraction(current.toFloat(), target.toFloat())
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, fontFamily = JetBrainsMono)
            Text("$current / $target $unit", style = MaterialTheme.typography.bodySmall, fontFamily = JetBrainsMono, color = SystemPrimary)
        }
        CyberProgressBar(progress = progress, height = 6.dp)
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
