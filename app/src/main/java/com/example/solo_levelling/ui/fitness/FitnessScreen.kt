package com.example.solo_levelling.ui.fitness

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
import com.example.solo_levelling.data.db.entity.WorkoutRoutineEntity
import com.example.solo_levelling.data.seed.FoodCatalog
import com.example.solo_levelling.data.seed.FoodCatalogEntry
import com.example.solo_levelling.data.seed.WorkoutCatalog
import com.example.solo_levelling.domain.service.EntryValidation
import com.example.solo_levelling.domain.service.FoodMacroScaler
import com.example.solo_levelling.domain.service.WorkoutProgressLogic
import com.example.solo_levelling.domain.service.WorkoutSplitLogic
import java.time.LocalDate
import kotlinx.coroutines.launch

private val WEEK_DAYS = listOf(
    "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
)

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
    val splitLocked = workoutSplitId.isNotBlank()

    val workoutDate = (selectedWorkoutDate ?: todayDate).ifBlank { todayDate }
    val dietDate = (selectedDietDate ?: todayDate).ifBlank { todayDate }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            when (tab) {
                FitnessTab.Workout -> "Workout"
                FitnessTab.Diet -> "Nutrition"
                FitnessTab.Both -> "Fitness"
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
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
                onApplySplit = { splitId, dayMapCsv ->
                    scope.launch {
                        val err = container.modules.applyWorkoutSplit(splitId, dayMapCsv)
                        if (err != null) onMessage(err) else onMessage("Split applied")
                    }
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
                        onMessage("Workout started — log at least one set")
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
                            onMessage("Use START WORKOUT for your split plan")
                            return@launch
                        }
                        val last = workouts.firstOrNull { it.date != workoutDate }
                        if (last != null) {
                            container.modules.upsertWorkoutLog(
                                last.copy(date = workoutDate, id = 0),
                            )
                            onMessage("Same workout started")
                        } else {
                            onMessage("NO PREVIOUS WORKOUT.\nStart your first session.")
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
                            onMessage("NO MEALS LOGGED.\nStart recording today's nutrition.")
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun DateStrip(
    label: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.secondaryContainer, RoundedCornerShape(8.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous day")
        }
        Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next day")
        }
    }
}

@Composable
private fun FitnessSection(
    routine: WorkoutRoutineEntity,
    todaySummary: WorkoutLogEntity?,
    nutritionToday: String?,
    workoutDate: String,
    selectedLog: WorkoutLogEntity?,
    history: List<WorkoutLogEntity>,
    heightCm: String,
    weightKg: String,
    bmiEstimate: String,
    fitnessGoal: String,
    splitLocked: Boolean = false,
    workoutSplitId: String = "",
    onApplySplit: (String, String) -> Unit = { _, _ -> },
    onPrevDate: () -> Unit,
    onNextDate: () -> Unit,
    onSaveDay: (String, WorkoutDayPlanEntity) -> Unit,
    onRestDay: (String) -> Unit,
    onUpsertPlanned: (String, PlannedExerciseEntity, String) -> Unit,
    onRemovePlanned: (String, Long) -> Unit,
    onReorderPlanned: (String, Long, Boolean) -> Unit,
    onStartLog: () -> Unit,
    onSaveLog: (WorkoutLogEntity) -> Unit,
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
    var tab by remember { mutableStateOf("Routine") }
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
    var changeSplitId by remember { mutableStateOf(workoutSplitId.ifBlank { "ppl_ul" }) }
    var splitDayMap by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    val scope = rememberCoroutineScope()

    val dayPlan = routine.day(dayKey)
    LaunchedEffect(dayKey, dayPlan) {
        dayName = if (dayPlan.enabled) dayPlan.name else ""
    }
    LaunchedEffect(workoutSplitId) {
        if (workoutSplitId.isNotBlank()) changeSplitId = workoutSplitId
    }
    LaunchedEffect(changeSplitId) {
        WorkoutCatalog.findSplit(changeSplitId)?.let { split ->
            splitDayMap = WorkoutSplitLogic.defaultDayMap(split)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
        border = BorderStroke(1.dp, colors.outline),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Fitness", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Today", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            if (todaySummary == null) {
                Text(
                    "YOUR SYSTEM IS READY.\nNo workout has been logged yet.\nYour first session starts here.",
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                Text(
                    "Workout\n${todaySummary.workoutName}\n" +
                        "${todaySummary.exercises.size} exercises · " +
                        "${todaySummary.exercises.sumOf { it.sets.size }} sets",
                )
            }
            nutritionToday?.let {
                Text("Diet tip: $it", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(
                onClick = onRepeatLast,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = !splitLocked,
            ) {
                Text(if (splitLocked) "SPLIT LOCKED — USE START WORKOUT" else "START SAME WORKOUT")
            }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("Routine", "Log", "History").forEach { label ->
                    FilterChip(
                        selected = tab == label,
                        onClick = { tab = label },
                        label = { Text(label) },
                        colors = chipColors,
                        border = BorderStroke(1.dp, if (tab == label) colors.primary else colors.outline),
                    )
                }
            }

            when (tab) {
                "Routine", "Log" -> {
                    if (heightCm.isNotBlank() || weightKg.isNotBlank() || bmiEstimate.isNotBlank() || fitnessGoal.isNotBlank()) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(colors.surfaceContainerHigh, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            if (heightCm.isNotBlank()) Text("${heightCm}cm", style = MaterialTheme.typography.bodySmall)
                            if (weightKg.isNotBlank()) Text("${weightKg}kg", style = MaterialTheme.typography.bodySmall)
                            if (bmiEstimate.isNotBlank()) Text("BMI $bmiEstimate", style = MaterialTheme.typography.bodySmall)
                            if (fitnessGoal.isNotBlank()) {
                                Text(
                                    fitnessGoal.replace('_', ' '),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }

            when (tab) {
                "Routine" -> {
                    Text(
                        if (splitLocked) {
                            "Split: ${WorkoutCatalog.findSplit(workoutSplitId)?.name ?: workoutSplitId}"
                        } else {
                            "Choose a catalog split to lock the exercise list"
                        },
                        fontWeight = FontWeight.Bold,
                    )
                    if (splitLocked) {
                        Text(
                            "Exercises are fixed — change split below to regenerate the week plan.",
                            color = colors.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
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
                    Button(
                        onClick = {
                            val mapError = WorkoutSplitLogic.buildRoutine(changeSplitId, splitDayMap).error
                            if (mapError != null) {
                                onMessage(mapError)
                            } else {
                                onApplySplit(changeSplitId, WorkoutSplitLogic.encodeDayMap(splitDayMap))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (splitLocked) "Apply split" else "Apply split & lock") }
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
                    if (splitLocked) {
                        Text(
                            if (dayPlan.enabled) dayPlan.name.ifBlank { "Workout" } else "Rest",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        if (!dayPlan.enabled) {
                            Text("Rest day", color = colors.onSurfaceVariant)
                        }
                        dayPlan.exercises.forEach { ex ->
                            Text(
                                "${ex.name} · ${ex.targetMuscle} · ${ex.sets}×${ex.repRange.min}-${ex.repRange.max}",
                            )
                        }
                    } else {
                        OutlinedTextField(
                            dayName,
                            { dayName = it },
                            label = { Text("Workout name") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                onSaveDay(
                                    dayKey,
                                    dayPlan.copy(enabled = true, name = dayName.ifBlank { "Workout" }),
                                )
                            }) { Text("Save day") }
                            OutlinedButton(onClick = { onRestDay(dayKey) }) { Text("Mark rest") }
                        }
                        if (!dayPlan.enabled) {
                            Text("Currently marked rest — Save day or Add exercise to enable.")
                        }
                        dayPlan.exercises.forEach { ex ->
                            Text(
                                "${ex.name} · ${ex.targetMuscle} · ${ex.sets}×${ex.repRange.min}-${ex.repRange.max}",
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedButton(onClick = { onReorderPlanned(dayKey, ex.id, true) }) { Text("Up") }
                                OutlinedButton(onClick = { onReorderPlanned(dayKey, ex.id, false) }) { Text("Down") }
                                OutlinedButton(onClick = { onRemovePlanned(dayKey, ex.id) }) { Text("Remove") }
                            }
                        }
                        Text("Add exercise", style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(planName, { planName = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(planMuscle, { planMuscle = it }, label = { Text("Target muscle") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(planSets, { planSets = it }, label = { Text("Sets") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(planRepMin, { planRepMin = it }, label = { Text("Rep min") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(planRepMax, { planRepMax = it }, label = { Text("Rep max") }, modifier = Modifier.fillMaxWidth())
                        Button(onClick = {
                            EntryValidation.requireNonBlank(planName, "exercise name")?.let {
                                onMessage(it)
                                return@Button
                            }
                            EntryValidation.requirePositiveInt(planSets, "sets")?.let {
                                onMessage(it)
                                return@Button
                            }
                            EntryValidation.requirePositiveInt(planRepMin, "rep min")?.let {
                                onMessage(it)
                                return@Button
                            }
                            EntryValidation.requirePositiveInt(planRepMax, "rep max")?.let {
                                onMessage(it)
                                return@Button
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
                        }) { Text("Add exercise") }
                    }
                }

                "Log" -> {
                    DateStrip(label = workoutDate, onPrev = onPrevDate, onNext = onNextDate)
                    Button(onClick = onStartLog, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Text("START WORKOUT")
                    }
                    val log = selectedLog
                    LaunchedEffect(log, tab, workoutDate) {
                        if (tab == "Log" && log != null) {
                            val map = mutableMapOf<Long, LoggedExerciseEntity?>()
                            log.exercises.forEach { ex ->
                                val hist = loadExerciseHistory(ex.name)
                                map[ex.id] = hist.firstOrNull { it.first != workoutDate }?.second
                            }
                            exercisePrevious = map
                        }
                    }
                    if (log != null) {
                        if (splitLocked) {
                            Text(
                                log.workoutName,
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
                        log.exercises.forEach { ex ->
                            val planned = routine.day(log.dayOfWeek.ifBlank {
                                runCatching {
                                    LocalDate.parse(workoutDate).dayOfWeek.name.lowercase()
                                }.getOrDefault(dayKey)
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
                            ) { Text("+ ADD SET") }
                            if (!splitLocked) {
                                OutlinedButton(onClick = { onRemoveExercise(ex.id) }) { Text("Delete exercise") }
                            }
                        }
                        if (editingExerciseId != null) {
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
                                Button(
                                    onClick = {
                                        EntryValidation.requirePositiveFloat(setWeight, "weight")?.let {
                                            onMessage(it)
                                            return@Button
                                        }
                                        EntryValidation.requirePositiveInt(setReps, "reps")?.let {
                                            onMessage(it)
                                            return@Button
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
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                ) { Text("SAVE SET") }
                            }
                        }
                        if (!splitLocked) {
                            Text("Unplanned exercise", style = MaterialTheme.typography.titleSmall)
                            OutlinedTextField(exName, { exName = it }, label = { Text("Exercise") }, modifier = Modifier.fillMaxWidth())
                            Button(onClick = {
                                EntryValidation.requireNonBlank(exName, "exercise name")?.let {
                                    onMessage(it)
                                    return@Button
                                }
                                onUpsertExercise(LoggedExerciseEntity(name = exName.trim()))
                                exName = ""
                            }) { Text("Add") }
                        }
                        Button(
                            onClick = {
                                val sets = log.exercises.sumOf { it.sets.size }
                                if (sets == 0) {
                                    onMessage("Log at least one set before finishing")
                                    return@Button
                                }
                                onSaveLog(log.copy(workoutName = logName.ifBlank { log.workoutName }))
                                logName = ""
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                        ) { Text("FINISH WORKOUT") }
                        OutlinedButton(onClick = onDeleteLog) { Text("Delete") }
                    } else {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(colors.surfaceContainerHigh, RoundedCornerShape(8.dp))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                "YOUR SYSTEM IS READY.\nNo workout has been logged yet.\nYour first session starts here.",
                                color = colors.onSurfaceVariant,
                            )
                            Button(onClick = onStartLog, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                                Text("START WORKOUT")
                            }
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
                    Button(onClick = {
                        scope.launch {
                            if (compareName.isNotBlank()) {
                                compareHistory = loadExerciseHistory(compareName)
                            }
                        }
                    }) { Text("Load history") }
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
        border = BorderStroke(1.dp, colors.outline),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Diet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Today", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            if (todayTotals != null) {
                Text(
                    "Calories: ${todayTotals.calories}\n" +
                        "Protein: ${todayTotals.protein}g\n" +
                        "Carbs: ${todayTotals.carbs}g\n" +
                        "Fat: ${todayTotals.fat}g",
                )
            } else {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceContainerHigh, RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "NO MEALS LOGGED.\nStart recording today's nutrition.",
                        color = colors.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Button(onClick = { tab = "Log Food" }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Text("ADD FIRST MEAL")
                    }
                }
            }

            OutlinedButton(
                onClick = onRepeatMeal,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Text("REPEAT MEAL")
            }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("Log Food", "History").forEach { label ->
                    FilterChip(
                        selected = tab == label,
                        onClick = { tab = label },
                        label = { Text(label) },
                        colors = chipColors,
                        border = BorderStroke(1.dp, if (tab == label) colors.primary else colors.outline),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("Breakfast", "Lunch", "Dinner", "Snack", "Pre-workout", "Post-workout").forEach { preset ->
                    FilterChip(
                        selected = mealName == preset,
                        onClick = { mealName = preset },
                        label = { Text(preset) },
                        colors = chipColors,
                        border = BorderStroke(1.dp, if (mealName == preset) colors.primary else colors.outline),
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
                    Button(onClick = {
                        EntryValidation.requireNonBlank(mealName, "meal name")?.let {
                            onMessage(it)
                            return@Button
                        }
                        val name = mealName.trim()
                        mealName = ""
                        onAddMeal(name) { mealId ->
                            foodMealId = mealId
                            foodEntryMode = "Catalog"
                            clearFoodForm()
                        }
                    }) { Text("Add meal") }

                    if (selectedLog?.meals.isNullOrEmpty()) {
                        Text("No meals for this date", color = colors.onSurfaceVariant)
                    }

                    selectedLog?.meals?.forEach { meal ->
                        val mealT = mealTotals(meal)
                        Text("${meal.name} · ${mealT.calories} cal P${mealT.protein} C${mealT.carbs} F${mealT.fat}")
                        meal.foods.forEach { food ->
                            val qty = listOfNotNull(
                                food.quantity?.let { q -> "${q}${food.unit.orEmpty()}" },
                                food.calories?.let { "· ${it} cal" },
                            ).joinToString(" ")
                            Text("  ${food.name} $qty", style = MaterialTheme.typography.bodySmall)
                            OutlinedButton(onClick = { onDeleteFood(meal.id, food.id) }) { Text("Delete food") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(onClick = {
                                foodMealId = meal.id
                                foodEntryMode = "Catalog"
                                clearFoodForm()
                            }) { Text("Add food") }
                            OutlinedButton(onClick = { onDeleteMeal(meal.id) }) { Text("Delete meal") }
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

                        Button(onClick = {
                            if (foodEntryMode == "Catalog") {
                                if (selectedCatalog == null) {
                                    onMessage("Select a food from the catalog")
                                    return@Button
                                }
                                EntryValidation.requirePositiveFloat(foodQty, "quantity")?.let {
                                    onMessage(it)
                                    return@Button
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
                                    return@Button
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
                                    return@Button
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
                        }) { Text("Save food") }
                        OutlinedButton(onClick = {
                            val meal = selectedLog?.meals?.firstOrNull { it.id == mealId }
                            foodMealId = null
                            clearFoodForm()
                            if (meal != null && meal.foods.isEmpty()) {
                                onDeleteMeal(mealId)
                                onMessage("Add at least one food to the meal")
                            }
                        }) { Text("Cancel") }
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
    val progress = if (target <= 0) 0f else (current.toFloat() / target).coerceIn(0f, 1f)
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text("$current / $target $unit", style = MaterialTheme.typography.bodySmall)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = colors.primary,
            trackColor = colors.surfaceContainerHighest,
        )
    }
}
