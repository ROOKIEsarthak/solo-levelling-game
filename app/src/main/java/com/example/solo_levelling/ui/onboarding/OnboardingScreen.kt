package com.example.solo_levelling.ui.onboarding

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solo_levelling.data.seed.WorkoutCatalog
import com.example.solo_levelling.domain.service.CareerGoalEngine
import com.example.solo_levelling.domain.service.EnabledModules
import com.example.solo_levelling.domain.service.EntryValidation
import com.example.solo_levelling.domain.service.Macros
import com.example.solo_levelling.domain.service.NutritionCalc
import com.example.solo_levelling.domain.service.OnboardingInput
import com.example.solo_levelling.domain.service.WorkoutSplitLogic
import com.example.solo_levelling.ui.components.BracketLabel
import com.example.solo_levelling.ui.components.CyberProgressBar
import com.example.solo_levelling.ui.components.EnergyFieldBackground
import com.example.solo_levelling.ui.components.GlassLevel
import com.example.solo_levelling.ui.components.GlassSurface
import com.example.solo_levelling.ui.components.GhostTextButton
import com.example.solo_levelling.ui.components.SystemActionButton
import com.example.solo_levelling.ui.theme.JetBrainsMono
import com.example.solo_levelling.ui.theme.SystemError
import com.example.solo_levelling.ui.theme.SystemPrimary
import com.example.solo_levelling.ui.theme.SystemSecondary

internal enum class OnboardingStep {
    NAME,
    GOALS,
    CAREER_INTENT,
    CAREER_PROFILE,
    CAREER_ASSESS,
    WORKOUT_BODY,
    WORKOUT_PLAN,
    DIET_NUTRITION,
    SUMMARY,
}

internal fun buildOnboardingSteps(modules: EnabledModules): List<OnboardingStep> = buildList {
    add(OnboardingStep.NAME)
    add(OnboardingStep.GOALS)
    if (modules.career) {
        add(OnboardingStep.CAREER_INTENT)
        add(OnboardingStep.CAREER_PROFILE)
        add(OnboardingStep.CAREER_ASSESS)
    }
    if (modules.workout) {
        add(OnboardingStep.WORKOUT_BODY)
        add(OnboardingStep.WORKOUT_PLAN)
    }
    if (modules.diet) {
        add(OnboardingStep.DIET_NUTRITION)
    }
    add(OnboardingStep.SUMMARY)
}

internal fun needsBodyFieldsInDietStep(modules: EnabledModules): Boolean =
    modules.diet && !modules.workout

internal fun onboardingStepTitle(step: OnboardingStep): String = when (step) {
    OnboardingStep.NAME -> "Establish Identity"
    OnboardingStep.GOALS -> "Module Selection"
    OnboardingStep.CAREER_INTENT -> "Career Intent"
    OnboardingStep.CAREER_PROFILE -> "Career Profile"
    OnboardingStep.CAREER_ASSESS -> "System Assessment"
    OnboardingStep.WORKOUT_BODY -> "Fitness Profile"
    OnboardingStep.WORKOUT_PLAN -> "Workout Plan"
    OnboardingStep.DIET_NUTRITION -> "Nutrition Setup"
    OnboardingStep.SUMMARY -> "System Summary"
}

internal fun onboardingProgressFraction(stepIndex: Int, totalSteps: Int): Float {
    if (totalSteps <= 0) return 0f
    return ((stepIndex + 1).toFloat() / totalSteps).coerceIn(0f, 1f)
}

private val careerIntentOptions = listOf(
    "learning" to "Learning",
    "interviews" to "Interviews",
    "switch_jobs" to "Switch jobs",
    "promotion" to "Promotion",
    "next_level" to "Next level",
    "other" to "Other",
)

private val weekdayKeyByIso = mapOf(
    1 to "monday", 2 to "tuesday", 3 to "wednesday", 4 to "thursday",
    5 to "friday", 6 to "saturday", 7 to "sunday",
)

private val isoByWeekdayLabel = mapOf(
    "MON" to 1, "TUE" to 2, "WED" to 3, "THU" to 4,
    "FRI" to 5, "SAT" to 6, "SUN" to 7,
)

@Composable
fun OnboardingScreen(onFinished: (OnboardingInput) -> Unit) {
    var stepIndex by remember { mutableIntStateOf(0) }
    var enabledModules by remember { mutableStateOf(EnabledModules()) }
    var name by remember { mutableStateOf("") }
    var careerIntent by remember { mutableStateOf("") }
    var experienceBand by remember { mutableStateOf("1-2") }
    var currentRole by remember { mutableStateOf("") }
    var targetRole by remember { mutableStateOf("SDE1") }
    var yearsExperience by remember { mutableStateOf("1") }
    var techStack by remember { mutableStateOf("") }
    var dsaConfidenceBand by remember { mutableStateOf("Med") }
    var sdConfidenceBand by remember { mutableStateOf("Med") }
    var targetCompanies by remember { mutableStateOf("") }
    var targetComp by remember { mutableStateOf("") }
    var targetTimeline by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("") }
    var heightCm by remember { mutableStateOf("") }
    var weightKg by remember { mutableStateOf("") }
    var trainingExperience by remember { mutableStateOf("beginner") }
    var fitnessGoal by remember { mutableStateOf("maintenance") }
    var workoutSplitId by remember { mutableStateOf("ppl_ul") }
    var activityLevel by remember { mutableStateOf("moderate") }
    var targetWeightKg by remember { mutableStateOf("") }
    var calorieOverride by remember { mutableStateOf("") }
    var createOwnRoutine by remember { mutableStateOf(false) }
    var preferredWorkoutDays by remember { mutableStateOf(setOf<String>()) }
    var customDayNames by remember { mutableStateOf(mapOf<String, String>()) }
    var splitDayMap by remember { mutableStateOf(mapOf<Int, Int>()) }

    LaunchedEffect(workoutSplitId) {
        splitDayMap = WorkoutCatalog.findSplit(workoutSplitId)?.let { WorkoutSplitLogic.defaultDayMap(it) }
            ?: emptyMap()
    }

    val steps = remember(enabledModules) { buildOnboardingSteps(enabledModules) }
    LaunchedEffect(steps.size) {
        if (stepIndex >= steps.size) stepIndex = steps.lastIndex.coerceAtLeast(0)
    }
    val currentStep = steps.getOrElse(stepIndex) { OnboardingStep.SUMMARY }
    var submitting by remember { mutableStateOf(false) }

    val scroll = rememberScrollState()
    val bandOptions = listOf("student", "0-1", "1-2", "2-3", "3-5", "5+")
    val confidenceBands = listOf("Low", "Med", "High")
    val fitnessGoals = listOf("maintenance", "fat_loss", "muscle_gain")
    val activityLevels = listOf("sedentary", "light", "moderate", "active", "very_active")
    val colors = MaterialTheme.colorScheme
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

    val ageInt = age.toIntOrNull()?.takeIf { it > 0 }
    val height = heightCm.toDoubleOrNull()?.takeIf { it > 0.0 }
    val weight = weightKg.toDoubleOrNull()?.takeIf { it > 0.0 }
    val years = yearsExperience.toDoubleOrNull() ?: 0.0
    val dsaConf = confidenceBandValue(dsaConfidenceBand)
    val sdConf = confidenceBandValue(sdConfidenceBand)
    val bmi: Double?
    val bmr: Double?
    val tdee: Double?
    val computedCalories: Int?
    val macros: Macros?
    if (ageInt != null && height != null && weight != null && (sex == "male" || sex == "female")) {
        bmi = NutritionCalc.bmi(height, weight)
        bmr = NutritionCalc.bmrMifflin(sex, ageInt, height, weight)
        tdee = NutritionCalc.tdee(bmr, activityLevel)
        computedCalories = NutritionCalc.goalCalories(tdee, fitnessGoal)
        macros = NutritionCalc.macroTargets(weight, computedCalories, fitnessGoal)
    } else {
        bmi = null
        bmr = null
        tdee = null
        computedCalories = null
        macros = null
    }
    val assessment = remember(
        experienceBand, currentRole, targetRole, years, dsaConf, sdConf,
    ) {
        CareerGoalEngine.assess(experienceBand, currentRole, targetRole, years, dsaConf, sdConf)
    }

    val forwardLabel = when {
        stepIndex >= steps.lastIndex -> "Begin"
        stepIndex == 0 -> "Continue"
        else -> "Next"
    }

    Box(Modifier.fillMaxSize()) {
        EnergyFieldBackground(Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Welcome",
                        color = SystemPrimary,
                        fontFamily = JetBrainsMono,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "STEP ${stepIndex + 1}/${steps.size}",
                        color = colors.onSurfaceVariant,
                        fontFamily = JetBrainsMono,
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 2.sp,
                    )
                }
                CyberProgressBar(progress = onboardingProgressFraction(stepIndex, steps.size))
                Text(
                    text = "Turn your real life into progression.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }

            GlassSurface(modifier = Modifier.fillMaxWidth(), level = GlassLevel.Level2) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BracketLabel(
                        text = onboardingStepTitle(currentStep).uppercase(),
                        color = SystemPrimary,
                    )
                    when (currentStep) {
                        OnboardingStep.NAME -> {
                            Text(
                                "Who is the Player?",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Player name") },
                                placeholder = { Text("Hunter") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = fieldColors,
                            )
                        }
                        OnboardingStep.GOALS -> {
                            Text(
                                "Choose your modules",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "Pick at least one focus area for your system.",
                                color = colors.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            ModuleGoalCard(
                                title = "Career",
                                subtitle = "DSA, system design, and role progression",
                                selected = enabledModules.career,
                                onClick = { enabledModules = enabledModules.withCareer(!enabledModules.career) },
                            )
                            ModuleGoalCard(
                                title = "Workout",
                                subtitle = "Training splits, routines, and fitness quests",
                                selected = enabledModules.workout,
                                onClick = { enabledModules = enabledModules.withWorkout(!enabledModules.workout) },
                            )
                            ModuleGoalCard(
                                title = "Diet",
                                subtitle = "Calorie targets, macros, and nutrition tracking",
                                selected = enabledModules.diet,
                                onClick = { enabledModules = enabledModules.withDiet(!enabledModules.diet) },
                            )
                        }
                        OnboardingStep.CAREER_INTENT -> {
                            Text("Career intent", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                "What are you optimizing for right now?",
                                color = colors.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
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
                        }
                        OnboardingStep.CAREER_PROFILE -> {
                            Text("Career profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Experience band", style = MaterialTheme.typography.labelMedium)
                            ChipRow(bandOptions, experienceBand, chipColors) { experienceBand = it }
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
                            Text("DSA confidence", style = MaterialTheme.typography.labelMedium)
                            ChipRow(confidenceBands, dsaConfidenceBand, chipColors) { dsaConfidenceBand = it }
                            Text("System design confidence", style = MaterialTheme.typography.labelMedium)
                            ChipRow(confidenceBands, sdConfidenceBand, chipColors) { sdConfidenceBand = it }
                            OutlinedTextField(
                                value = targetCompanies,
                                onValueChange = { targetCompanies = it },
                                label = { Text("Target companies (optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = fieldColors,
                            )
                            OutlinedTextField(
                                value = targetComp,
                                onValueChange = { targetComp = it },
                                label = { Text("Target comp (optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = fieldColors,
                            )
                            OutlinedTextField(
                                value = targetTimeline,
                                onValueChange = { targetTimeline = it },
                                label = { Text("Target timeline (optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = fieldColors,
                            )
                        }
                        OnboardingStep.CAREER_ASSESS -> {
                            Text("System assessment", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            GlassSurface(level = GlassLevel.Level1) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        assessment.nextGoalTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SystemPrimary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(assessment.reason, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "${assessment.currentLevelLabel} → ${assessment.targetLevelLabel}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceVariant,
                                        fontFamily = JetBrainsMono,
                                    )
                                    Text(
                                        "Mandatory: ${assessment.mandatoryAreas.joinToString(", ")}",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    Text(
                                        "Recommended: ${assessment.recommendedAreas.joinToString(", ")}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        OnboardingStep.WORKOUT_BODY -> {
                            Text("Fitness profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            BodyFields(
                                age = age,
                                onAgeChange = { age = it },
                                sex = sex,
                                onSexChange = { sex = it },
                                heightCm = heightCm,
                                onHeightChange = { heightCm = it },
                                weightKg = weightKg,
                                onWeightChange = { weightKg = it },
                                trainingExperience = trainingExperience,
                                onTrainingExperienceChange = { trainingExperience = it },
                                fitnessGoal = fitnessGoal,
                                onFitnessGoalChange = { fitnessGoal = it },
                                activityLevel = activityLevel,
                                onActivityLevelChange = { activityLevel = it },
                                fitnessGoals = fitnessGoals,
                                activityLevels = activityLevels,
                                chipColors = chipColors,
                                fieldColors = fieldColors,
                            )
                        }
                        OnboardingStep.WORKOUT_PLAN -> {
                            Text("Workout plan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                            if (createOwnRoutine) {
                                Text(
                                    "Pick your training days (optional names per day).",
                                    color = colors.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    WorkoutSplitLogic.weekdayLabels.forEach { (iso, label) ->
                                        val dayKey = isoToScheduleDay[iso] ?: return@forEach
                                        FilterChip(
                                            selected = dayKey in preferredWorkoutDays,
                                            onClick = {
                                                preferredWorkoutDays = if (dayKey in preferredWorkoutDays) {
                                                    preferredWorkoutDays - dayKey
                                                } else {
                                                    preferredWorkoutDays + dayKey
                                                }
                                            },
                                            label = { Text(label) },
                                            colors = chipColors,
                                        )
                                    }
                                }
                                preferredWorkoutDays.sortedBy { isoByWeekdayLabel[it] ?: 99 }.forEach { dayLabel ->
                                    val key = weekdayKeyByIso[isoByWeekdayLabel[dayLabel] ?: return@forEach] ?: return@forEach
                                    OutlinedTextField(
                                        value = customDayNames[key].orEmpty(),
                                        onValueChange = { customDayNames = customDayNames + (key to it) },
                                        label = { Text("$dayLabel day name (optional)") },
                                        placeholder = { Text("Workout") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        colors = fieldColors,
                                    )
                                }
                            } else {
                                Text("Workout split", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    "Assign each workout to a weekday below.",
                                    color = colors.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                )
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
                                Text("Assign workout days", style = MaterialTheme.typography.titleMedium)
                                val split = WorkoutCatalog.findSplit(workoutSplitId)
                                splitDayMapValidationMessage(workoutSplitId, splitDayMap)?.let { msg ->
                                    Text(
                                        msg,
                                        color = SystemError,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                split?.schedule?.sortedBy { it.day }?.forEach { slot ->
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
                            }
                        }
                        OnboardingStep.DIET_NUTRITION -> {
                            Text("Nutrition setup", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            if (needsBodyFieldsInDietStep(enabledModules)) {
                                BodyFields(
                                    age = age,
                                    onAgeChange = { age = it },
                                    sex = sex,
                                    onSexChange = { sex = it },
                                    heightCm = heightCm,
                                    onHeightChange = { heightCm = it },
                                    weightKg = weightKg,
                                    onWeightChange = { weightKg = it },
                                    trainingExperience = trainingExperience,
                                    onTrainingExperienceChange = { trainingExperience = it },
                                    fitnessGoal = fitnessGoal,
                                    onFitnessGoalChange = { fitnessGoal = it },
                                    activityLevel = activityLevel,
                                    onActivityLevelChange = { activityLevel = it },
                                    fitnessGoals = fitnessGoals,
                                    activityLevels = activityLevels,
                                    chipColors = chipColors,
                                    fieldColors = fieldColors,
                                    showTrainingExperience = false,
                                )
                            }
                            GlassSurface(level = GlassLevel.Level1) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (bmi != null && bmr != null && tdee != null && computedCalories != null && macros != null) {
                                        Text(
                                            "BMI: ${String.format("%.1f", bmi)} (${NutritionCalc.bmiCategory(bmi)})",
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                        Text("BMR: ${bmr.toInt()} kcal", style = MaterialTheme.typography.bodyMedium)
                                        Text("TDEE: ${tdee.toInt()} kcal", style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            "Goal calories: $computedCalories kcal",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = SystemPrimary,
                                        )
                                        Text(
                                            "Macros — P: ${macros.proteinG}g · C: ${macros.carbsG}g · F: ${macros.fatG}g",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.onSurfaceVariant,
                                        )
                                    } else {
                                        Text(
                                            "Enter age, height, weight, and gender to preview nutrition targets.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colors.onSurfaceVariant,
                                        )
                                    }
                                }
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
                                placeholder = {
                                    Text(computedCalories?.toString() ?: "Enter your daily target")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = fieldColors,
                            )
                        }
                        OnboardingStep.SUMMARY -> {
                            Text(
                                "Ready to initialize",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            GlassSurface(level = GlassLevel.Level1) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    BracketLabel(text = "SYSTEM SUMMARY", color = SystemSecondary)
                                    Text(
                                        "Player: ${name.ifBlank { "Hunter" }}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = JetBrainsMono,
                                    )
                                    Text("Modules:", style = MaterialTheme.typography.labelMedium)
                                    if (enabledModules.career) {
                                        Text("• Career — $careerIntent", style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            "  ${assessment.nextGoalTitle}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.onSurfaceVariant,
                                        )
                                    }
                                    if (enabledModules.workout) {
                                        val planLabel = if (createOwnRoutine) {
                                            "Custom (${preferredWorkoutDays.size} days)"
                                        } else {
                                            WorkoutCatalog.findSplit(workoutSplitId)?.name ?: workoutSplitId
                                        }
                                        Text("• Workout — $planLabel", style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (enabledModules.diet) {
                                        Text(
                                            "• Diet — ${computedCalories?.let { "$it kcal target" } ?: "nutrition setup"}",
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (stepIndex > 0) {
                    GhostTextButton(
                        label = "Back",
                        onClick = { stepIndex -= 1 },
                        modifier = Modifier.weight(1f),
                    )
                }
                SystemActionButton(
                    label = forwardLabel,
                    onClick = {
                        if (stepIndex < steps.lastIndex) {
                            stepIndex += 1
                        } else {
                            if (submitting) return@SystemActionButton
                            val needBody = enabledModules.workout || enabledModules.diet
                            val parsedAge = age.toIntOrNull()
                            val parsedHeight = heightCm.toDoubleOrNull()
                            val parsedWeight = weightKg.toDoubleOrNull()
                            if (needBody) {
                                if (parsedAge == null || parsedAge <= 0) return@SystemActionButton
                                if (sex != "male" && sex != "female") return@SystemActionButton
                                if (parsedHeight == null || parsedHeight <= 0.0) return@SystemActionButton
                                if (parsedWeight == null || parsedWeight <= 0.0) return@SystemActionButton
                            }
                            val bodyAge = parsedAge ?: 25
                            val bodyHeight = parsedHeight ?: 170.0
                            val bodyWeight = parsedWeight ?: 70.0
                            val scheduleDays = when {
                                enabledModules.workout && createOwnRoutine ->
                                    preferredWorkoutDays.sortedBy { isoByWeekdayLabel[it] ?: 99 }
                                enabledModules.workout && !createOwnRoutine ->
                                    scheduleDaysFromSplitDayMap(splitDayMap)
                                else -> emptyList()
                            }
                            submitting = true
                            onFinished(
                                OnboardingInput(
                                    name = name,
                                    modules = enabledModules,
                                    priorities = buildList {
                                        if (enabledModules.career) add("career")
                                        if (enabledModules.workout) add("fitness")
                                        if (enabledModules.diet) add("health")
                                    },
                                    scheduleDays = scheduleDays,
                                    careerIntent = if (enabledModules.career) careerIntent else "",
                                    experienceBand = if (enabledModules.career) experienceBand else "",
                                    currentRole = if (enabledModules.career) currentRole else "",
                                    targetRole = if (enabledModules.career) targetRole else "",
                                    yearsExperience = if (enabledModules.career) years else 0.0,
                                    techStack = if (enabledModules.career) techStack else "",
                                    targetCompanies = if (enabledModules.career) targetCompanies else "",
                                    targetComp = if (enabledModules.career) targetComp else "",
                                    targetTimeline = if (enabledModules.career) targetTimeline else "",
                                    dsaConfidence = if (enabledModules.career) dsaConf else 50,
                                    sdConfidence = if (enabledModules.career) sdConf else 50,
                                    age = if (needBody) bodyAge else 25,
                                    sex = if (needBody) sex else "male",
                                    heightCm = if (needBody) bodyHeight else 170.0,
                                    weightKg = if (needBody) bodyWeight else 70.0,
                                    trainingExperience = if (enabledModules.workout) trainingExperience else "beginner",
                                    fitnessGoal = if (enabledModules.workout || enabledModules.diet) fitnessGoal else "maintenance",
                                    trainingDays = if (enabledModules.workout) {
                                        if (createOwnRoutine) {
                                            preferredWorkoutDays.size.coerceAtLeast(1)
                                        } else {
                                            WorkoutCatalog.findSplit(workoutSplitId)?.daysPerWeek ?: 3
                                        }
                                    } else {
                                        3
                                    },
                                    workoutSplitId = if (enabledModules.workout && !createOwnRoutine) workoutSplitId else "",
                                    workoutDayMapCsv = if (enabledModules.workout && !createOwnRoutine) {
                                        WorkoutSplitLogic.encodeDayMap(splitDayMap)
                                    } else {
                                        ""
                                    },
                                    createOwnRoutine = enabledModules.workout && createOwnRoutine,
                                    customDayNames = if (enabledModules.workout && createOwnRoutine) customDayNames else emptyMap(),
                                    preferredWorkoutDays = if (enabledModules.workout && createOwnRoutine) {
                                        preferredWorkoutDays.sortedBy { isoByWeekdayLabel[it] ?: 99 }
                                    } else {
                                        emptyList()
                                    },
                                    activityLevel = if (enabledModules.workout || enabledModules.diet) activityLevel else "moderate",
                                    targetWeightKg = if (enabledModules.diet) targetWeightKg.toDoubleOrNull() else null,
                                    calorieOverride = if (enabledModules.diet) calorieOverride.toIntOrNull() else null,
                                ),
                            )
                        }
                    },
                    enabled = !submitting && isOnboardingStepValid(
                        step = currentStep,
                        name = name,
                        enabledModules = enabledModules,
                        careerIntent = careerIntent,
                        createOwnRoutine = createOwnRoutine,
                        workoutSplitId = workoutSplitId,
                        splitDayMap = splitDayMap,
                        preferredWorkoutDays = preferredWorkoutDays,
                        age = age,
                        sex = sex,
                        heightCm = heightCm,
                        weightKg = weightKg,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ModuleGoalCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) SystemPrimary else SystemPrimary.copy(alpha = 0.2f)
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
        level = GlassLevel.Level1,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) SystemPrimary else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BodyFields(
    age: String,
    onAgeChange: (String) -> Unit,
    sex: String,
    onSexChange: (String) -> Unit,
    heightCm: String,
    onHeightChange: (String) -> Unit,
    weightKg: String,
    onWeightChange: (String) -> Unit,
    trainingExperience: String,
    onTrainingExperienceChange: (String) -> Unit,
    fitnessGoal: String,
    onFitnessGoalChange: (String) -> Unit,
    activityLevel: String,
    onActivityLevelChange: (String) -> Unit,
    fitnessGoals: List<String>,
    activityLevels: List<String>,
    chipColors: androidx.compose.material3.SelectableChipColors,
    fieldColors: androidx.compose.material3.TextFieldColors,
    showTrainingExperience: Boolean = true,
) {
    var genderExpanded by remember { mutableStateOf(false) }
    val genderLabel = when (sex) {
        "male" -> "Male"
        "female" -> "Female"
        else -> ""
    }
    OutlinedTextField(
        value = age,
        onValueChange = onAgeChange,
        label = { Text("Age") },
        placeholder = { Text("Enter your age") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        colors = fieldColors,
    )
    ExposedDropdownMenuBox(
        expanded = genderExpanded,
        onExpandedChange = { genderExpanded = it },
    ) {
        OutlinedTextField(
            value = genderLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Gender") },
            placeholder = { Text("Select gender") },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded)
            },
            colors = fieldColors,
        )
        ExposedDropdownMenu(
            expanded = genderExpanded,
            onDismissRequest = { genderExpanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("Male") },
                onClick = {
                    onSexChange("male")
                    genderExpanded = false
                },
            )
            DropdownMenuItem(
                text = { Text("Female") },
                onClick = {
                    onSexChange("female")
                    genderExpanded = false
                },
            )
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = heightCm,
            onValueChange = onHeightChange,
            label = { Text("Height cm") },
            placeholder = { Text("Enter your height") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            colors = fieldColors,
        )
        OutlinedTextField(
            value = weightKg,
            onValueChange = onWeightChange,
            label = { Text("Weight kg") },
            placeholder = { Text("Enter your weight") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            colors = fieldColors,
        )
    }
    if (showTrainingExperience) {
        Text("Training experience", style = MaterialTheme.typography.labelMedium)
        ChipRow(listOf("beginner", "intermediate", "advanced"), trainingExperience, chipColors) {
            onTrainingExperienceChange(it)
        }
    }
    Text("Fitness goal", style = MaterialTheme.typography.labelMedium)
    ChipRow(fitnessGoals, fitnessGoal, chipColors) { onFitnessGoalChange(it) }
    Text("Activity level", style = MaterialTheme.typography.labelMedium)
    ChipRow(activityLevels, activityLevel, chipColors) { onActivityLevelChange(it) }
}

@Composable
private fun ChipRow(
    options: List<String>,
    selected: String,
    chipColors: androidx.compose.material3.SelectableChipColors,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { opt ->
            FilterChip(
                selected = opt == selected,
                onClick = { onSelect(opt) },
                label = { Text(opt) },
                colors = chipColors,
            )
        }
    }
}

internal fun isBodyProfileValid(age: String, sex: String, heightCm: String, weightKg: String): Boolean {
    if (sex != "male" && sex != "female") return false
    return EntryValidation.firstError(
        EntryValidation.requirePositiveInt(age, "age"),
        EntryValidation.requirePositiveFloat(heightCm, "height"),
        EntryValidation.requirePositiveFloat(weightKg, "weight"),
    ) == null
}

internal fun isOnboardingStepValid(
    step: OnboardingStep,
    name: String,
    enabledModules: EnabledModules,
    careerIntent: String,
    createOwnRoutine: Boolean,
    workoutSplitId: String,
    splitDayMap: Map<Int, Int>,
    preferredWorkoutDays: Set<String>,
    age: String,
    sex: String,
    heightCm: String,
    weightKg: String,
): Boolean = when (step) {
    OnboardingStep.NAME -> name.isNotBlank()
    OnboardingStep.GOALS -> enabledModules.anyEnabled
    OnboardingStep.CAREER_INTENT -> careerIntent.isNotBlank()
    OnboardingStep.WORKOUT_BODY -> isBodyProfileValid(age, sex, heightCm, weightKg)
    OnboardingStep.WORKOUT_PLAN -> if (createOwnRoutine) {
        preferredWorkoutDays.isNotEmpty()
    } else {
        isSplitDayMapValid(workoutSplitId, splitDayMap)
    }
    OnboardingStep.DIET_NUTRITION -> if (needsBodyFieldsInDietStep(enabledModules)) {
        isBodyProfileValid(age, sex, heightCm, weightKg)
    } else {
        true
    }
    else -> true
}

private fun confidenceBandValue(band: String): Int = when (band) {
    "Low" -> 25
    "High" -> 75
    else -> 50
}

private val isoToScheduleDay = mapOf(
    1 to "MON", 2 to "TUE", 3 to "WED", 4 to "THU",
    5 to "FRI", 6 to "SAT", 7 to "SUN",
)

internal fun scheduleDaysFromSplitDayMap(splitDayMap: Map<Int, Int>): List<String> =
    splitDayMap.values.distinct().sorted().mapNotNull { isoToScheduleDay[it] }

internal fun isSplitDayMapValid(splitId: String, splitDayMap: Map<Int, Int>): Boolean =
    splitDayMapValidationMessage(splitId, splitDayMap) == null

internal fun splitDayMapValidationMessage(splitId: String, splitDayMap: Map<Int, Int>): String? {
    val split = WorkoutCatalog.findSplit(splitId) ?: return "Unknown workout split"
    val requiredSlots = split.schedule.map { it.day }.toSet()
    if (!requiredSlots.all { it in splitDayMap }) {
        return "Assign a weekday for every workout"
    }
    if (splitDayMap.values.any { it !in 1..7 }) {
        return "Pick a valid weekday for each workout"
    }
    if (splitDayMap.values.size != splitDayMap.values.distinct().size) {
        return "Each weekday can only have one workout"
    }
    return null
}
