package com.example.solo_levelling.ui.fitness

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
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
import java.time.LocalDate
import kotlinx.coroutines.launch

private val WEEK_DAYS = listOf(
    "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
)

@Composable
fun FitnessScreen(container: AppContainer) {
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
    val scope = rememberCoroutineScope()

    val workoutDate = (selectedWorkoutDate ?: todayDate).ifBlank { todayDate }
    val dietDate = (selectedDietDate ?: todayDate).ifBlank { todayDate }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Fitness", style = MaterialTheme.typography.headlineSmall)

        FitnessSection(
            routine = workoutRoutine,
            todaySummary = workoutLogToday,
            nutritionToday = nutritionToday?.let { "Calories: ${it.calories}" },
            workoutDate = workoutDate,
            selectedLog = selectedWorkoutLog,
            history = workouts,
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
            onSaveDay = { day, plan -> scope.launch { container.modules.saveRoutineDay(day, plan) } },
            onRestDay = { day -> scope.launch { container.modules.setRestDay(day) } },
            onUpsertPlanned = { day, ex, name ->
                scope.launch { container.modules.upsertPlannedExercise(day, ex, name) }
            },
            onRemovePlanned = { day, id -> scope.launch { container.modules.removePlannedExercise(day, id) } },
            onReorderPlanned = { day, id, up ->
                scope.launch { container.modules.reorderPlannedExercise(day, id, up) }
            },
            onStartLog = {
                scope.launch { container.modules.startOrGetWorkoutLog(workoutDate) }
            },
            onSaveLog = { log -> scope.launch { container.modules.upsertWorkoutLog(log) } },
            onDeleteLog = { scope.launch { container.modules.deleteWorkoutLog(workoutDate) } },
            onRemoveExercise = { id ->
                scope.launch { container.modules.removeExerciseFromLog(workoutDate, id) }
            },
            onUpsertExercise = { ex ->
                scope.launch { container.modules.upsertLoggedExercise(workoutDate, ex) }
            },
            loadExerciseHistory = { name -> container.modules.exerciseHistory(name) },
        )

        DietSection(
            dietDate = dietDate,
            selectedLog = selectedDietLog,
            history = dietLogs,
            todayTotals = nutritionToday,
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
            onAddMeal = { name ->
                scope.launch {
                    if (dietDate.isBlank()) return@launch
                    container.modules.addMeal(dietDate, name)
                }
            },
            onDeleteMeal = { id -> scope.launch { container.modules.deleteMeal(dietDate, id) } },
            onUpsertFood = { mealId, food ->
                scope.launch { container.modules.upsertFood(dietDate, mealId, food) }
            },
            onDeleteFood = { mealId, foodId ->
                scope.launch { container.modules.deleteFood(dietDate, mealId, foodId) }
            },
            mealTotals = { meal -> container.modules.mealTotals(meal) },
        )
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
) {
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
    val scope = rememberCoroutineScope()

    val dayPlan = routine.day(dayKey)
    LaunchedEffect(dayKey, dayPlan) {
        dayName = if (dayPlan.enabled) dayPlan.name else ""
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Fitness", style = MaterialTheme.typography.titleMedium)
            Text(
                "Today ────────────────────",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                "Workout\n${todaySummary?.workoutName ?: "Not logged"}\n" +
                    "${todaySummary?.exercises?.size ?: 0} exercises · " +
                    "${todaySummary?.exercises?.sumOf { it.sets.size } ?: 0} sets",
            )
            nutritionToday?.let { Text("Diet tip: $it", style = MaterialTheme.typography.bodySmall) }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("Routine", "Log", "History").forEach { label ->
                    FilterChip(selected = tab == label, onClick = { tab = label }, label = { Text(label) })
                }
            }

            when (tab) {
                "Routine" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        WEEK_DAYS.forEach { d ->
                            FilterChip(
                                selected = dayKey == d,
                                onClick = { dayKey = d },
                                label = { Text(d.take(3).replaceFirstChar { it.uppercase() }) },
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
                        if (planName.isBlank()) return@Button
                        onUpsertPlanned(
                            dayKey,
                            PlannedExerciseEntity(
                                name = planName,
                                targetMuscle = planMuscle,
                                sets = planSets.toIntOrNull() ?: 3,
                                repRange = RepRangeEntity(
                                    planRepMin.toIntOrNull() ?: 8,
                                    planRepMax.toIntOrNull() ?: 12,
                                ),
                            ),
                            dayName,
                        )
                        planName = ""
                        planMuscle = ""
                    }) { Text("Add exercise") }
                }

                "Log" -> {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        OutlinedButton(onClick = onPrevDate) { Text("<") }
                        Text(workoutDate)
                        OutlinedButton(onClick = onNextDate) { Text(">") }
                    }
                    Button(onClick = onStartLog) { Text("Start / load from routine") }
                    val log = selectedLog
                    if (log != null) {
                        OutlinedTextField(
                            value = logName.ifBlank { log.workoutName },
                            onValueChange = { logName = it },
                            label = { Text("Workout name") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(onClick = {
                            onSaveLog(log.copy(workoutName = logName.ifBlank { log.workoutName }))
                            logName = ""
                        }) { Text("Save name") }
                        log.exercises.forEach { ex ->
                            Text(ex.name, style = MaterialTheme.typography.titleSmall)
                            ex.sets.forEachIndexed { i, s ->
                                Text(
                                    "  Set ${i + 1}: ${s.weight}kg × ${s.reps}" +
                                        (s.rpe?.let { " @ RPE $it" } ?: ""),
                                )
                            }
                            OutlinedButton(onClick = { onRemoveExercise(ex.id) }) { Text("Delete exercise") }
                            OutlinedButton(onClick = { editingExerciseId = ex.id }) { Text("Add set") }
                        }
                        if (editingExerciseId != null) {
                            val target = log.exercises.firstOrNull { it.id == editingExerciseId }
                            if (target != null) {
                                OutlinedTextField(setWeight, { setWeight = it }, label = { Text("Weight") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(setReps, { setReps = it }, label = { Text("Reps") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(setRpe, { setRpe = it }, label = { Text("RPE (optional)") }, modifier = Modifier.fillMaxWidth())
                                Button(onClick = {
                                    val w = setWeight.toFloatOrNull() ?: return@Button
                                    val r = setReps.toIntOrNull() ?: return@Button
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
                                }) { Text("Save set") }
                            }
                        }
                        Text("Add unplanned exercise", style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(exName, { exName = it }, label = { Text("Exercise") }, modifier = Modifier.fillMaxWidth())
                        Button(onClick = {
                            if (exName.isBlank()) return@Button
                            onUpsertExercise(LoggedExerciseEntity(name = exName))
                            exName = ""
                        }) { Text("Add exercise") }
                        OutlinedButton(onClick = onDeleteLog) { Text("Delete workout") }
                    } else {
                        Text("No workout for this date")
                    }
                }

                "History" -> {
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

@Composable
private fun DietSection(
    dietDate: String,
    selectedLog: DietLogEntity?,
    history: List<DietLogEntity>,
    todayTotals: NutritionLogEntity?,
    onPrevDate: () -> Unit,
    onNextDate: () -> Unit,
    onAddMeal: (String) -> Unit,
    onDeleteMeal: (Long) -> Unit,
    onUpsertFood: (Long, FoodItemEntity) -> Unit,
    onDeleteFood: (Long, Long) -> Unit,
    mealTotals: (MealEntity) -> NutritionTotalsEntity,
) {
    var tab by remember { mutableStateOf("Log Food") }
    var mealName by remember { mutableStateOf("") }
    var foodMealId by remember { mutableStateOf<Long?>(null) }
    var foodName by remember { mutableStateOf("") }
    var foodQty by remember { mutableStateOf("") }
    var foodUnit by remember { mutableStateOf("g") }
    var foodCal by remember { mutableStateOf("") }
    var foodP by remember { mutableStateOf("") }
    var foodC by remember { mutableStateOf("") }
    var foodF by remember { mutableStateOf("") }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Diet", style = MaterialTheme.typography.titleMedium)
            Text("Today ────────────────────", style = MaterialTheme.typography.titleSmall)
            if (todayTotals != null) {
                Text(
                    "Calories: ${todayTotals.calories}\n" +
                        "Protein: ${todayTotals.protein}g\n" +
                        "Carbs: ${todayTotals.carbs}g\n" +
                        "Fat: ${todayTotals.fat}g",
                )
            } else {
                Text("No diet logged today")
            }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("Log Food", "History").forEach { label ->
                    FilterChip(selected = tab == label, onClick = { tab = label }, label = { Text(label) })
                }
            }

            when (tab) {
                "Log Food" -> {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        OutlinedButton(onClick = onPrevDate) { Text("<") }
                        Text(dietDate)
                        OutlinedButton(onClick = onNextDate) { Text(">") }
                    }
                    OutlinedTextField(mealName, { mealName = it }, label = { Text("Meal name") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = {
                        if (mealName.isBlank()) return@Button
                        onAddMeal(mealName)
                        mealName = ""
                    }) { Text("Add meal") }

                    selectedLog?.meals?.forEach { meal ->
                        val totals = mealTotals(meal)
                        Text("${meal.name} · ${totals.calories} cal P${totals.protein} C${totals.carbs} F${totals.fat}")
                        meal.foods.forEach { food ->
                            val qty = listOfNotNull(
                                food.quantity?.let { q -> "${q}${food.unit.orEmpty()}" },
                                food.calories?.let { "· ${it} cal" },
                            ).joinToString(" ")
                            Text("  ${food.name} $qty", style = MaterialTheme.typography.bodySmall)
                            OutlinedButton(onClick = { onDeleteFood(meal.id, food.id) }) { Text("Delete food") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(onClick = { foodMealId = meal.id }) { Text("Add food") }
                            OutlinedButton(onClick = { onDeleteMeal(meal.id) }) { Text("Delete meal") }
                        }
                    }

                    foodMealId?.let { mealId ->
                        Text("New food", style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(foodName, { foodName = it }, label = { Text("Food") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(foodQty, { foodQty = it }, label = { Text("Quantity") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(foodUnit, { foodUnit = it }, label = { Text("Unit") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(foodCal, { foodCal = it }, label = { Text("Calories (opt)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(foodP, { foodP = it }, label = { Text("Protein (opt)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(foodC, { foodC = it }, label = { Text("Carbs (opt)") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(foodF, { foodF = it }, label = { Text("Fat (opt)") }, modifier = Modifier.fillMaxWidth())
                        Button(onClick = {
                            if (foodName.isBlank()) return@Button
                            onUpsertFood(
                                mealId,
                                FoodItemEntity(
                                    name = foodName,
                                    quantity = foodQty.toFloatOrNull(),
                                    unit = foodUnit.ifBlank { null },
                                    calories = foodCal.toIntOrNull(),
                                    protein = foodP.toIntOrNull(),
                                    carbs = foodC.toIntOrNull(),
                                    fat = foodF.toIntOrNull(),
                                ),
                            )
                            foodName = ""
                            foodQty = ""
                            foodCal = ""
                            foodP = ""
                            foodC = ""
                            foodF = ""
                            foodMealId = null
                        }) { Text("Save food") }
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
                    if (history.isEmpty()) Text("No diet history yet")
                }
            }
        }
    }
}
