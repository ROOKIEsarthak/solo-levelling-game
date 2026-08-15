package com.example.solo_levelling.ui.modules

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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.data.db.entity.CareerNodeEntity
import com.example.solo_levelling.data.db.entity.DsaProblemEntity
import com.example.solo_levelling.data.db.entity.WorkoutEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ModulesScreen(container: AppContainer) {
    val vm: ModulesViewModel = viewModel(factory = ModulesViewModel.factory(container))
    val dsa by vm.dsa.collectAsStateWithLifecycle()
    val workouts by vm.workouts.collectAsStateWithLifecycle()
    val bosses by vm.bosses.collectAsStateWithLifecycle()
    val skills by vm.skills.collectAsStateWithLifecycle()
    val careerNodes by vm.careerNodes.collectAsStateWithLifecycle()
    val nutritionToday by vm.nutritionToday.collectAsStateWithLifecycle()
    val journalToday by vm.journalToday.collectAsStateWithLifecycle()
    val focusToday by vm.focusToday.collectAsStateWithLifecycle()
    val routinesToday by vm.routinesToday.collectAsStateWithLifecycle()
    val recentMetrics by vm.recentMetrics.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var dsaTitle by remember { mutableStateOf("") }
    var dsaDifficulty by remember { mutableStateOf("MEDIUM") }
    var dsaTopic by remember { mutableStateOf("") }
    var workoutType by remember { mutableStateOf("") }
    var workoutDuration by remember { mutableStateOf("") }
    var exerciseName by remember { mutableStateOf("") }
    var exerciseSets by remember { mutableStateOf("") }
    var exerciseReps by remember { mutableStateOf("") }
    var exerciseWeight by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var focusMinutes by remember { mutableStateOf("25") }
    var focusLabel by remember { mutableStateOf("Deep Work") }
    var timerRunning by remember { mutableStateOf(false) }
    var secondsLeft by remember { mutableIntStateOf(0) }
    var steps by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var journal by remember { mutableStateOf("") }
    var bossTitle by remember { mutableStateOf("") }
    var workoutExercises by remember { mutableStateOf<Map<Long, List<String>>>(emptyMap()) }

    LaunchedEffect(journalToday?.content) {
        journalToday?.content?.let { if (journal.isEmpty()) journal = it }
    }

    LaunchedEffect(timerRunning, secondsLeft) {
        if (timerRunning && secondsLeft > 0) {
            delay(1_000)
            secondsLeft--
        } else if (timerRunning && secondsLeft == 0) {
            timerRunning = false
            val mins = focusMinutes.toIntOrNull()?.coerceAtLeast(1) ?: 25
            container.modules.logFocus(mins, focusLabel.ifBlank { "Focus" })
        }
    }

    LaunchedEffect(workouts.firstOrNull()?.id) {
        val latest = workouts.firstOrNull() ?: return@LaunchedEffect
        if (workoutExercises.containsKey(latest.id)) return@LaunchedEffect
        val exercises = container.modules.getWorkoutExercises(latest.id)
        workoutExercises = workoutExercises + (latest.id to exercises.map { ex ->
            "${ex.name}: ${ex.sets}x${ex.reps} @ ${ex.weightKg}kg"
        })
    }

    val routineKinds = listOf("WAKE", "SLEEP", "READ", "MEDITATE")
    val loggedRoutineKinds = routinesToday.map { it.kind }.toSet()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Life Modules", style = MaterialTheme.typography.headlineSmall)

        CareerSection(careerNodes) { id ->
            scope.launch { container.modules.advanceCareerNode(id) }
        }

        DsaSection(
            dsa = dsa,
            title = dsaTitle,
            difficulty = dsaDifficulty,
            topic = dsaTopic,
            onTitleChange = { dsaTitle = it },
            onDifficultyChange = { dsaDifficulty = it },
            onTopicChange = { dsaTopic = it },
            onAdd = {
                scope.launch {
                    if (dsaTitle.isNotBlank()) {
                        container.modules.addDsaProblem(dsaTitle, dsaDifficulty, dsaTopic)
                        dsaTitle = ""
                    }
                }
            },
            onAttempt = { scope.launch { container.modules.markAttempted(it) } },
            onSolve = { scope.launch { container.modules.solveDsa(it) } },
            onMaster = { scope.launch { container.modules.masterDsa(it) } },
        )

        FitnessSection(
            workouts = workouts.take(5),
            workoutExercises = workoutExercises,
            workoutType = workoutType,
            workoutDuration = workoutDuration,
            exerciseName = exerciseName,
            exerciseSets = exerciseSets,
            exerciseReps = exerciseReps,
            exerciseWeight = exerciseWeight,
            onWorkoutTypeChange = { workoutType = it },
            onWorkoutDurationChange = { workoutDuration = it },
            onExerciseNameChange = { exerciseName = it },
            onExerciseSetsChange = { exerciseSets = it },
            onExerciseRepsChange = { exerciseReps = it },
            onExerciseWeightChange = { exerciseWeight = it },
            onLogWorkout = {
                scope.launch {
                    val duration = workoutDuration.toIntOrNull() ?: return@launch
                    if (workoutType.isBlank()) return@launch
                    container.modules.logWorkout(workoutType, duration)
                    workoutType = ""
                    workoutDuration = ""
                }
            },
            onAddExercise = { workoutId ->
                scope.launch {
                    val sets = exerciseSets.toIntOrNull() ?: return@launch
                    val reps = exerciseReps.toIntOrNull() ?: return@launch
                    val weightKg = exerciseWeight.toFloatOrNull() ?: return@launch
                    if (exerciseName.isBlank()) return@launch
                    container.modules.addWorkoutExercise(workoutId, exerciseName, sets, reps, weightKg)
                    val exercises = container.modules.getWorkoutExercises(workoutId)
                    workoutExercises = workoutExercises + (workoutId to exercises.map { ex ->
                        "${ex.name}: ${ex.sets}x${ex.reps} @ ${ex.weightKg}kg"
                    })
                    exerciseName = ""
                    exerciseSets = ""
                    exerciseReps = ""
                    exerciseWeight = ""
                }
            },
        )

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Nutrition", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(calories, { calories = it }, label = { Text("Calories") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(protein, { protein = it }, label = { Text("Protein (g)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(carbs, { carbs = it }, label = { Text("Carbs (g)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(fat, { fat = it }, label = { Text("Fat (g)") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    scope.launch {
                        container.modules.logNutrition(
                            calories.toIntOrNull() ?: 0,
                            protein.toIntOrNull() ?: 0,
                            carbs.toIntOrNull() ?: 0,
                            fat.toIntOrNull() ?: 0,
                        )
                    }
                }) { Text("Save nutrition") }
                nutritionToday?.let { n ->
                    Text("Today: ${n.calories} cal · P${n.protein} C${n.carbs} F${n.fat}")
                } ?: Text("No nutrition logged today")
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Focus", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(focusMinutes, { focusMinutes = it }, label = { Text("Minutes") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(focusLabel, { focusLabel = it }, label = { Text("Label") }, modifier = Modifier.fillMaxWidth())
                if (timerRunning) {
                    Text("Time left: ${secondsLeft / 60}:${"%02d".format(secondsLeft % 60)}")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val mins = focusMinutes.toIntOrNull()?.coerceAtLeast(1) ?: return@Button
                        secondsLeft = mins * 60
                        timerRunning = true
                    }, enabled = !timerRunning) { Text("Start timer") }
                    OutlinedButton(onClick = {
                        scope.launch {
                            val mins = focusMinutes.toIntOrNull()?.coerceAtLeast(1) ?: return@launch
                            container.modules.logFocus(mins, focusLabel.ifBlank { "Focus" })
                        }
                    }) { Text("Log now") }
                }
                val focusTotal = focusToday.sumOf { it.durationMinutes }
                if (focusTotal > 0) Text("Today: ${focusTotal}m focus logged")
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Steps", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(steps, { steps = it }, label = { Text("Steps") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    scope.launch {
                        val value = steps.toFloatOrNull() ?: return@launch
                        container.metricIngest.ingest("STEPS", value)
                        steps = ""
                    }
                }) { Text("Log steps") }
                val todaySteps = recentMetrics.filter { it.metricType == "STEPS" }.firstOrNull()
                todaySteps?.let { Text("Latest: ${it.value.toInt()} steps") }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Weight", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(weight, { weight = it }, label = { Text("Weight (kg)") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    scope.launch {
                        val value = weight.toFloatOrNull() ?: return@launch
                        container.metricIngest.ingest("WEIGHT", value)
                        weight = ""
                    }
                }) { Text("Log weight") }
                val latestWeight = recentMetrics.filter { it.metricType == "WEIGHT" }.firstOrNull()
                latestWeight?.let { Text("Latest: ${it.value} kg") }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Journal", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = journal,
                    onValueChange = { journal = it },
                    label = { Text("Reflection") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                Button(onClick = {
                    scope.launch {
                        if (journal.isNotBlank()) container.modules.saveJournal(journal)
                    }
                }) { Text("Save journal") }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Routines", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    routineKinds.forEach { kind ->
                        FilterChip(
                            selected = kind in loggedRoutineKinds,
                            onClick = { scope.launch { container.modules.logRoutine(kind) } },
                            label = { Text(kind.lowercase().replaceFirstChar { it.uppercase() }) },
                        )
                    }
                }
                if (routinesToday.isNotEmpty()) {
                    Text("Logged today: ${routinesToday.joinToString { it.kind }}")
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Boss Quests", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = bossTitle,
                    onValueChange = { bossTitle = it },
                    label = { Text("Boss title") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = {
                    scope.launch {
                        if (bossTitle.isNotBlank()) {
                            container.modules.createBoss(bossTitle, "Major objective", 200)
                            bossTitle = ""
                        }
                    }
                }) { Text("Create boss") }
                bosses.filter { it.status == "ACTIVE" }.forEach { b ->
                    Text("${b.title}: ${b.currentValue}/${b.targetValue}")
                    Button(onClick = { scope.launch { container.modules.addBossProgress(25f) } }) {
                        Text("+25% progress")
                    }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Skills", style = MaterialTheme.typography.titleMedium)
                val domains = skills.map { it.domain }.distinct().sorted()
                var domainFilter by remember { mutableStateOf<String?>(null) }
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = domainFilter == null,
                        onClick = { domainFilter = null },
                        label = { Text("All") },
                    )
                    domains.forEach { domain ->
                        FilterChip(
                            selected = domainFilter == domain,
                            onClick = { domainFilter = domain },
                            label = { Text(domain) },
                        )
                    }
                }
                val filtered = if (domainFilter == null) skills else skills.filter { it.domain == domainFilter }
                filtered.groupBy { it.domain }.forEach { (domain, domainSkills) ->
                    Text(domain, style = MaterialTheme.typography.titleSmall)
                    domainSkills.forEach { s ->
                        Text("${s.name} · Lv ${s.level} (${s.xp} XP)")
                    }
                }
                if (skills.isEmpty()) Text("Skills unlock from DSA and career progress.")
            }
        }
    }
}

@Composable
private fun CareerSection(nodes: List<CareerNodeEntity>, onAdvance: (Long) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Career", style = MaterialTheme.typography.titleMedium)
            nodes.groupBy { it.track }.forEach { (track, trackNodes) ->
                Text(track, style = MaterialTheme.typography.titleSmall)
                trackNodes.sortedBy { it.orderIndex }.forEach { node ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(node.title)
                            Text("${node.status} · ${node.description}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (node.status != "MASTERED") {
                            OutlinedButton(onClick = { onAdvance(node.id) }) {
                                Text("Advance")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DsaSection(
    dsa: List<DsaProblemEntity>,
    title: String,
    difficulty: String,
    topic: String,
    onTitleChange: (String) -> Unit,
    onDifficultyChange: (String) -> Unit,
    onTopicChange: (String) -> Unit,
    onAdd: () -> Unit,
    onAttempt: (Long) -> Unit,
    onSolve: (Long) -> Unit,
    onMaster: (Long) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("DSA", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(title, onTitleChange, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(difficulty, onDifficultyChange, label = { Text("Difficulty") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(topic, onTopicChange, label = { Text("Topic") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = onAdd) { Text("Add problem") }
            dsa.take(10).forEach { p ->
                Text("${p.title} · ${p.difficulty} · ${p.topic} · ${p.status}")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (p.status == "NOT_STARTED") {
                        OutlinedButton(onClick = { onAttempt(p.id) }) { Text("Attempt") }
                    }
                    if (p.status == "NOT_STARTED" || p.status == "ATTEMPTED") {
                        Button(onClick = { onSolve(p.id) }) { Text("Solve") }
                    }
                    if (p.status == "SOLVED") {
                        Button(onClick = { onMaster(p.id) }) { Text("Master") }
                    }
                }
            }
        }
    }
}

@Composable
private fun FitnessSection(
    workouts: List<WorkoutEntity>,
    workoutExercises: Map<Long, List<String>>,
    workoutType: String,
    workoutDuration: String,
    exerciseName: String,
    exerciseSets: String,
    exerciseReps: String,
    exerciseWeight: String,
    onWorkoutTypeChange: (String) -> Unit,
    onWorkoutDurationChange: (String) -> Unit,
    onExerciseNameChange: (String) -> Unit,
    onExerciseSetsChange: (String) -> Unit,
    onExerciseRepsChange: (String) -> Unit,
    onExerciseWeightChange: (String) -> Unit,
    onLogWorkout: () -> Unit,
    onAddExercise: (Long) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Fitness", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(workoutType, onWorkoutTypeChange, label = { Text("Type") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(workoutDuration, onWorkoutDurationChange, label = { Text("Duration (min)") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = onLogWorkout) { Text("Log workout") }
            workouts.forEach { w ->
                Text("${w.date} · ${w.type} · ${w.durationMinutes}m")
                workoutExercises[w.id]?.forEach { ex -> Text("  $ex", style = MaterialTheme.typography.bodySmall) }
            }
            workouts.firstOrNull()?.let { latest ->
                Text("Add exercise to latest workout", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(exerciseName, onExerciseNameChange, label = { Text("Exercise") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(exerciseSets, onExerciseSetsChange, label = { Text("Sets") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(exerciseReps, onExerciseRepsChange, label = { Text("Reps") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(exerciseWeight, onExerciseWeightChange, label = { Text("Weight (kg)") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { onAddExercise(latest.id) }) { Text("Add exercise") }
            }
        }
    }
}
