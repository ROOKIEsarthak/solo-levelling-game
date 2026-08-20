package com.example.solo_levelling.ui.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.data.seed.WorkoutCatalog
import com.example.solo_levelling.domain.service.EnabledModules
import com.example.solo_levelling.domain.service.ModuleChangeResult
import com.example.solo_levelling.domain.service.ModuleFlags
import com.example.solo_levelling.domain.service.NutritionCalc
import com.example.solo_levelling.domain.service.OnboardingInput
import com.example.solo_levelling.domain.service.WorkoutSplitLogic
import com.example.solo_levelling.ui.components.GhostTextButton
import com.example.solo_levelling.ui.components.SystemActionButton
import com.example.solo_levelling.ui.onboarding.isBodyProfileValid
import com.example.solo_levelling.ui.onboarding.isSplitDayMapValid
import com.example.solo_levelling.ui.theme.JetBrainsMono
import com.example.solo_levelling.ui.theme.Spacing
import com.example.solo_levelling.ui.theme.SystemPrimary
import kotlinx.coroutines.launch

internal fun moduleSetupTitle(module: String): String = when (module) {
    ModuleFlags.MODULE_CAREER -> "Set up Career"
    ModuleFlags.MODULE_WORKOUT -> "Set up Fitness"
    ModuleFlags.MODULE_DIET -> "Set up Nutrition"
    else -> "Set up module"
}

internal fun moduleSetupIntro(module: String): String = when (module) {
    ModuleFlags.MODULE_CAREER ->
        "A few details unlock Career and set your roadmap and next goal."
    ModuleFlags.MODULE_WORKOUT ->
        "A few details will help the system build accurate training quests for you."
    ModuleFlags.MODULE_DIET ->
        "A few details will help the system build accurate nutrition targets and quests for you."
    else -> ""
}

internal fun moduleInitializedMessage(module: String): String =
    "${ModuleFlags.displayName(module)} is now active."

internal fun isModuleSetupValid(
    module: String,
    careerIntent: String,
    createOwnRoutine: Boolean,
    workoutSplitId: String,
    splitDayMap: Map<Int, Int>,
    preferredWorkoutDays: Set<String>,
    age: String,
    sex: String,
    heightCm: String,
    weightKg: String,
    requireBody: Boolean,
): Boolean = when (module) {
    ModuleFlags.MODULE_CAREER -> careerIntent.isNotBlank()
    ModuleFlags.MODULE_WORKOUT -> if (requireBody && !isBodyProfileValid(age, sex, heightCm, weightKg)) {
        false
    } else if (createOwnRoutine) {
        preferredWorkoutDays.isNotEmpty()
    } else {
        isSplitDayMapValid(workoutSplitId, splitDayMap)
    }
    ModuleFlags.MODULE_DIET -> if (requireBody) {
        isBodyProfileValid(age, sex, heightCm, weightKg)
    } else {
        true
    }
    else -> false
}

private val careerIntentOptions = listOf(
    "learning" to "Learning",
    "interviews" to "Interviews",
    "switch_jobs" to "Switch jobs",
    "promotion" to "Promotion",
    "next_level" to "Next level",
    "other" to "Other",
)

@Composable
fun ModuleSetupScreen(
    container: AppContainer,
    moduleId: String,
    onFinished: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val vm: ModuleSetupViewModel = viewModel(factory = ModuleSetupViewModel.factory(container))
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var careerIntent by remember { mutableStateOf("") }
    var experienceBand by remember { mutableStateOf("1-2") }
    var currentRole by remember { mutableStateOf("") }
    var targetRole by remember { mutableStateOf("SDE1") }
    var yearsExperience by remember { mutableStateOf("1") }
    var techStack by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("male") }
    var heightCm by remember { mutableStateOf("") }
    var weightKg by remember { mutableStateOf("") }
    var trainingExperience by remember { mutableStateOf("beginner") }
    var fitnessGoal by remember { mutableStateOf("maintenance") }
    var activityLevel by remember { mutableStateOf("moderate") }
    var workoutSplitId by remember { mutableStateOf("ppl_ul") }
    var splitDayMap by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var createOwnRoutine by remember { mutableStateOf(false) }
    var preferredWorkoutDays by remember { mutableStateOf(setOf<String>()) }
    var calorieOverride by remember { mutableStateOf("") }
    var targetWeightKg by remember { mutableStateOf("") }
    var requireBody by remember { mutableStateOf(true) }
    var submitting by remember { mutableStateOf(false) }

    LaunchedEffect(moduleId) {
        val initial = vm.loadInitialState()
        age = initial.age
        sex = initial.sex
        heightCm = initial.heightCm
        weightKg = initial.weightKg
        fitnessGoal = initial.fitnessGoal
        trainingExperience = initial.trainingExperience
        activityLevel = initial.activityLevel
        careerIntent = initial.careerIntent
        currentRole = initial.currentRole
        targetRole = initial.targetRole
        yearsExperience = initial.yearsExperience
        techStack = initial.techStack
        experienceBand = initial.experienceBand
        requireBody = initial.requireBody
        val split = WorkoutCatalog.findSplit(workoutSplitId)
        if (split != null && splitDayMap.isEmpty()) {
            splitDayMap = WorkoutSplitLogic.defaultDayMap(split)
        }
    }

    LaunchedEffect(workoutSplitId) {
        val split = WorkoutCatalog.findSplit(workoutSplitId) ?: return@LaunchedEffect
        if (splitDayMap.isEmpty() || split.schedule.any { it.day !in splitDayMap }) {
            splitDayMap = WorkoutSplitLogic.defaultDayMap(split)
        }
    }

    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = SystemPrimary.copy(alpha = 0.15f),
        selectedLabelColor = SystemPrimary,
        selectedLeadingIconColor = SystemPrimary,
    )
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = SystemPrimary,
        unfocusedBorderColor = SystemPrimary.copy(alpha = 0.3f),
        cursorColor = SystemPrimary,
    )
    val parsedHeight = heightCm.toDoubleOrNull() ?: 0.0
    val parsedWeight = weightKg.toDoubleOrNull() ?: 0.0
    val parsedAge = age.toIntOrNull() ?: 25
    val bmi = if (parsedHeight > 0 && parsedWeight > 0) NutritionCalc.bmi(parsedHeight, parsedWeight) else null
    val bmr = if (parsedHeight > 0 && parsedWeight > 0) {
        NutritionCalc.bmrMifflin(sex, parsedAge, parsedHeight, parsedWeight)
    } else {
        null
    }
    val tdee = bmr?.let { NutritionCalc.tdee(it, activityLevel) }
    val computedCalories = tdee?.let { NutritionCalc.goalCalories(it, fitnessGoal) }
    val macros = if (parsedWeight > 0 && computedCalories != null) {
        NutritionCalc.macroTargets(parsedWeight, calorieOverride.toIntOrNull() ?: computedCalories, fitnessGoal)
    } else {
        null
    }

    val valid = isModuleSetupValid(
        module = moduleId,
        careerIntent = careerIntent,
        createOwnRoutine = createOwnRoutine,
        workoutSplitId = workoutSplitId,
        splitDayMap = splitDayMap,
        preferredWorkoutDays = preferredWorkoutDays,
        age = age,
        sex = sex,
        heightCm = heightCm,
        weightKg = weightKg,
        requireBody = requireBody,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            moduleSetupTitle(moduleId),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            moduleSetupIntro(moduleId),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
        when (moduleId) {
            ModuleFlags.MODULE_CAREER -> {
                Text("Career intent", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    careerIntentOptions.forEach { (id, label) ->
                        FilterChip(
                            selected = careerIntent == id,
                            onClick = { careerIntent = id },
                            label = { Text(label) },
                            colors = chipColors,
                        )
                    }
                }
                OutlinedTextField(
                    value = currentRole,
                    onValueChange = { currentRole = it },
                    label = { Text("Current role") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = fieldColors,
                )
                OutlinedTextField(
                    value = targetRole,
                    onValueChange = { targetRole = it },
                    label = { Text("Target role") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = fieldColors,
                )
                OutlinedTextField(
                    value = yearsExperience,
                    onValueChange = { yearsExperience = it },
                    label = { Text("Years experience") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = fieldColors,
                )
                OutlinedTextField(
                    value = techStack,
                    onValueChange = { techStack = it },
                    label = { Text("Tech stack") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = fieldColors,
                )
            }
            ModuleFlags.MODULE_WORKOUT -> {
                if (requireBody) {
                    OutlinedTextField(
                        value = age,
                        onValueChange = { age = it },
                        label = { Text("Age") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = fieldColors,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = sex == "male",
                            onClick = { sex = "male" },
                            label = { Text("Male") },
                            colors = chipColors,
                        )
                        FilterChip(
                            selected = sex == "female",
                            onClick = { sex = "female" },
                            label = { Text("Female") },
                            colors = chipColors,
                        )
                    }
                    OutlinedTextField(
                        value = heightCm,
                        onValueChange = { heightCm = it },
                        label = { Text("Height (cm)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = fieldColors,
                    )
                    OutlinedTextField(
                        value = weightKg,
                        onValueChange = { weightKg = it },
                        label = { Text("Weight (kg)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = fieldColors,
                    )
                } else {
                    Text(
                        "Using stored body metrics ($heightCm cm · $weightKg kg)",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                Text("Fitness goal", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("maintenance", "fat_loss", "muscle_gain").forEach { goal ->
                        FilterChip(
                            selected = fitnessGoal == goal,
                            onClick = { fitnessGoal = goal },
                            label = { Text(goal.replace('_', ' ')) },
                            colors = chipColors,
                        )
                    }
                }
                Text("Training experience", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("beginner", "intermediate", "advanced").forEach { level ->
                        FilterChip(
                            selected = trainingExperience == level,
                            onClick = { trainingExperience = level },
                            label = { Text(level) },
                            colors = chipColors,
                        )
                    }
                }
                Text("Plan type", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !createOwnRoutine,
                        onClick = { createOwnRoutine = false },
                        label = { Text("Recommended split") },
                        colors = chipColors,
                    )
                    FilterChip(
                        selected = createOwnRoutine,
                        onClick = { createOwnRoutine = true },
                        label = { Text("Create my own") },
                        colors = chipColors,
                    )
                }
                if (!createOwnRoutine) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        WorkoutCatalog.splits.forEach { split ->
                            FilterChip(
                                selected = split.id == workoutSplitId,
                                onClick = { workoutSplitId = split.id },
                                label = { Text("${split.name} (${split.daysPerWeek}d)") },
                                colors = chipColors,
                            )
                        }
                    }
                    WorkoutCatalog.findSplit(workoutSplitId)?.schedule?.sortedBy { it.day }?.forEach { slot ->
                        Text(
                            WorkoutSplitLogic.workoutLabelForSlot(workoutSplitId, slot.day),
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN").forEach { day ->
                            FilterChip(
                                selected = day in preferredWorkoutDays,
                                onClick = {
                                    preferredWorkoutDays = if (day in preferredWorkoutDays) {
                                        preferredWorkoutDays - day
                                    } else {
                                        preferredWorkoutDays + day
                                    }
                                },
                                label = { Text(day) },
                                colors = chipColors,
                            )
                        }
                    }
                }
            }
            ModuleFlags.MODULE_DIET -> {
                if (requireBody) {
                    OutlinedTextField(
                        value = age,
                        onValueChange = { age = it },
                        label = { Text("Age") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = fieldColors,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = sex == "male",
                            onClick = { sex = "male" },
                            label = { Text("Male") },
                            colors = chipColors,
                        )
                        FilterChip(
                            selected = sex == "female",
                            onClick = { sex = "female" },
                            label = { Text("Female") },
                            colors = chipColors,
                        )
                    }
                    OutlinedTextField(
                        value = heightCm,
                        onValueChange = { heightCm = it },
                        label = { Text("Height (cm)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = fieldColors,
                    )
                    OutlinedTextField(
                        value = weightKg,
                        onValueChange = { weightKg = it },
                        label = { Text("Weight (kg)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = fieldColors,
                    )
                }
                if (bmi != null && computedCalories != null && macros != null) {
                    Text(
                        "BMI ${String.format("%.1f", bmi)} · ${computedCalories} kcal · P ${macros.proteinG}g",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = JetBrainsMono,
                    )
                }
                OutlinedTextField(
                    value = targetWeightKg,
                    onValueChange = { targetWeightKg = it },
                    label = { Text("Target weight kg (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = fieldColors,
                )
                OutlinedTextField(
                    value = calorieOverride,
                    onValueChange = { calorieOverride = it },
                    label = { Text("Calorie override (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = fieldColors,
                )
            }
            else -> {
                Text(
                    "This module cannot be configured here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        SystemActionButton(
            label = "Save",
            onClick = {
                if (submitting || !valid) return@SystemActionButton
                submitting = true
                val input = OnboardingInput(
                    name = "",
                    modules = EnabledModules().withModule(moduleId, true),
                    careerIntent = careerIntent,
                    experienceBand = experienceBand,
                    currentRole = currentRole,
                    targetRole = targetRole,
                    yearsExperience = yearsExperience.toDoubleOrNull() ?: 0.0,
                    techStack = techStack,
                    age = age.toIntOrNull() ?: 25,
                    sex = sex,
                    heightCm = heightCm.toDoubleOrNull() ?: 170.0,
                    weightKg = weightKg.toDoubleOrNull() ?: 70.0,
                    trainingExperience = trainingExperience,
                    fitnessGoal = fitnessGoal,
                    activityLevel = activityLevel,
                    workoutSplitId = if (!createOwnRoutine) workoutSplitId else "",
                    workoutDayMapCsv = if (!createOwnRoutine) WorkoutSplitLogic.encodeDayMap(splitDayMap) else "",
                    createOwnRoutine = createOwnRoutine,
                    preferredWorkoutDays = preferredWorkoutDays.toList(),
                    scheduleDays = preferredWorkoutDays.toList(),
                    trainingDays = if (createOwnRoutine) preferredWorkoutDays.size.coerceAtLeast(1) else {
                        WorkoutCatalog.findSplit(workoutSplitId)?.daysPerWeek ?: 3
                    },
                    targetWeightKg = targetWeightKg.toDoubleOrNull(),
                    calorieOverride = calorieOverride.toIntOrNull(),
                )
                scope.launch {
                    val result = vm.completeSetup(moduleId, input)
                    submitting = false
                    if (result is ModuleChangeResult.Enabled) {
                        onFinished(moduleInitializedMessage(moduleId))
                    }
                }
            },
            enabled = valid && !submitting,
            modifier = Modifier.fillMaxWidth(),
        )
        GhostTextButton(label = "Cancel", onClick = onCancel)
    }
}
