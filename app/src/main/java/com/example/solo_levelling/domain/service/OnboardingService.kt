package com.example.solo_levelling.domain.service

import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.core.time.AppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.AttributeStatEntity
import com.example.solo_levelling.data.db.entity.MetricLogEntity
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.StreakStateEntity
import com.example.solo_levelling.data.db.entity.UserConfigEntity
import com.example.solo_levelling.data.db.entity.WorkoutDayPlanEntity
import com.example.solo_levelling.data.db.entity.WorkoutRoutineEntity
import com.example.solo_levelling.data.seed.SeedData
import com.example.solo_levelling.domain.model.AttributeCode
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class OnboardingInput(
    val name: String,
    val priorities: List<String> = emptyList(),
    val scheduleDays: List<String> = emptyList(),
    val modules: EnabledModules = EnabledModules(career = true, workout = true, diet = true),
    val careerIntent: String = "",
    val experienceBand: String = "",
    val currentRole: String = "",
    val targetRole: String = "",
    val yearsExperience: Double = 0.0,
    val techStack: String = "",
    val targetCompanies: String = "",
    val targetComp: String = "",
    val targetTimeline: String = "",
    val dsaConfidence: Int = 50,
    val sdConfidence: Int = 50,
    val age: Int = 25,
    val sex: String = "male",
    val heightCm: Double = 170.0,
    val weightKg: Double = 70.0,
    val trainingExperience: String = "beginner",
    val fitnessGoal: String = "maintenance",
    val trainingDays: Int = 3,
    val workoutSplitId: String = "",
    val workoutDayMapCsv: String = "",
    val createOwnRoutine: Boolean = false,
    val customDayNames: Map<String, String> = emptyMap(),
    val preferredWorkoutDays: List<String> = emptyList(),
    val activityLevel: String = "moderate",
    val targetWeightKg: Double? = null,
    val bodyFatPct: Double? = null,
    val calorieOverride: Int? = null,
)

fun scheduleDaysToCsv(days: List<String>): String {
    val map = mapOf(
        "MON" to "1", "TUE" to "2", "WED" to "3", "THU" to "4",
        "FRI" to "5", "SAT" to "6", "SUN" to "7",
    )
    return days.mapNotNull { map[it.uppercase()] }.joinToString(",")
}

class OnboardingService(
    private val db: JsonDatabase,
    private val clock: AppClock,
    private val questGeneration: QuestGenerationService,
    private val progression: ProgressionService,
    private val season: SeasonService? = null,
) {
    suspend fun ensureSeeded() {
        if (db.achievementDao().getDefs().isEmpty()) {
            db.achievementDao().upsertDefs(SeedData.achievements())
        }
        if (db.questDao().getActiveTemplates().isEmpty()) {
            db.questDao().upsertTemplates(SeedData.defaultTemplates())
        }
        if (db.playerDao().getAttributes().isEmpty()) {
            db.playerDao().upsertAttributes(
                AttributeCode.entries.map { AttributeStatEntity(code = it.name) },
            )
        }
        if (db.playerDao().getStreak(SystemDefaults.PLAYER_ID) == null) {
            db.playerDao().upsertStreak(StreakStateEntity())
        }
        if (db.playerDao().getProfile(SystemDefaults.PLAYER_ID) == null) {
            db.playerDao().upsertProfile(
                PlayerProfileEntity(createdAtEpochMs = clock.nowEpochMs()),
            )
        }
        ensureConfigDefaults()
        migrateModuleFlagsIfNeeded()
        migrateModuleLifecycleIfNeeded()
        if (db.moduleDao().getCareerNodes().isEmpty()) {
            SeedData.careerNodes().forEach { node ->
                val status = if (node.orderIndex == 1) "STARTED" else node.status
                db.moduleDao().upsertCareerNode(node.copy(status = status))
            }
        }
        seedCareerCatalogsIfEmpty()
    }

    suspend fun migrateModuleFlagsIfNeeded() {
        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
        val career = db.configDao().get(ModuleFlags.KEY_CAREER)?.value
        val workout = db.configDao().get(ModuleFlags.KEY_WORKOUT)?.value
        val diet = db.configDao().get(ModuleFlags.KEY_DIET)?.value
        if (!ModuleFlags.needsMigration(career, workout, diet)) return
        if (profile?.onboardingDone != true) return
        val inferred = EnabledModules(
            career = moduleConfigLooksInitialized(db, ModuleFlags.MODULE_CAREER),
            workout = moduleConfigLooksInitialized(db, ModuleFlags.MODULE_WORKOUT),
            diet = moduleConfigLooksInitialized(db, ModuleFlags.MODULE_DIET),
        )
        val modules = if (inferred.anyEnabled) {
            inferred
        } else {
            EnabledModules(career = true, workout = true, diet = true)
        }
        for ((key, value) in ModuleFlags.encode(modules)) {
            db.configDao().upsert(UserConfigEntity(key, value))
        }
    }

    suspend fun currentModules(): EnabledModules {
        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
        return ModuleFlags.resolve(
            onboardingDone = profile?.onboardingDone == true,
            career = db.configDao().get(ModuleFlags.KEY_CAREER)?.value,
            workout = db.configDao().get(ModuleFlags.KEY_WORKOUT)?.value,
            diet = db.configDao().get(ModuleFlags.KEY_DIET)?.value,
        )
    }

    suspend fun writeModuleFlags(modules: EnabledModules): Boolean {
        if (!modules.anyEnabled) return false
        for ((key, value) in ModuleFlags.encode(modules)) {
            db.configDao().upsert(UserConfigEntity(key, value))
        }
        progression.rebuildActiveFromLedger(modules)
        season?.rebuildFromLedger(modules)
        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
        questGeneration.generateForToday(profile?.timezone ?: ZoneId.systemDefault().id)
        return true
    }

    suspend fun applyModuleConfiguration(module: String, input: OnboardingInput) {
        val configs = mutableMapOf<String, String>()
        when (module) {
            ModuleFlags.MODULE_CAREER -> {
                val assessment = CareerGoalEngine.assess(
                    experienceBand = input.experienceBand,
                    currentRole = input.currentRole,
                    targetRole = input.targetRole,
                    yearsExperience = input.yearsExperience,
                    dsaConfidence = input.dsaConfidence,
                    sdConfidence = input.sdConfidence,
                )
                configs += mapOf(
                    "career_intent" to input.careerIntent,
                    "career_experience_band" to input.experienceBand,
                    "career_current_role" to input.currentRole,
                    "career_target_role" to input.targetRole,
                    "career_years_experience" to input.yearsExperience.toString(),
                    "career_tech_stack" to input.techStack,
                    "career_target_companies" to input.targetCompanies,
                    "career_target_comp" to input.targetComp,
                    "career_target_timeline" to input.targetTimeline,
                    "career_dsa_confidence" to input.dsaConfidence.toString(),
                    "career_sd_confidence" to input.sdConfidence.toString(),
                    "career_reason" to assessment.reason,
                    "career_goal_reason" to assessment.reason,
                    "career_recommended_areas" to assessment.recommendedAreas.joinToString(","),
                    "career_current_level" to assessment.currentLevelLabel,
                    "career_target_level" to assessment.targetLevelLabel,
                    "goal_title" to assessment.nextGoalTitle,
                    "career_next_goal" to assessment.nextGoalTitle,
                    "career_mandatory_areas" to assessment.mandatoryAreas.joinToString(","),
                    "backend_confidence" to "0",
                    "behavioral_confidence" to "0",
                )
                seedCareerCatalogsIfEmpty()
            }
            ModuleFlags.MODULE_WORKOUT, ModuleFlags.MODULE_DIET -> {
                writeBodyAndNutritionConfigs(module, input, configs)
            }
        }
        for ((key, value) in configs) {
            db.configDao().upsert(UserConfigEntity(key, value))
        }
        if (module == ModuleFlags.MODULE_WORKOUT) {
            applyWorkoutPlan(input)
        }
        val scheduleCsv = scheduleDaysToCsv(input.scheduleDays)
        if (scheduleCsv.isNotEmpty() && module == ModuleFlags.MODULE_WORKOUT) {
            db.configDao().upsert(UserConfigEntity("schedule_days_csv", scheduleCsv))
        }
    }

    private suspend fun writeBodyAndNutritionConfigs(
        module: String,
        input: OnboardingInput,
        configs: MutableMap<String, String>,
    ) {
        val bmi = NutritionCalc.bmi(input.heightCm, input.weightKg)
        configs += mapOf(
            "height_cm" to input.heightCm.toString(),
            "weight_kg" to input.weightKg.toString(),
            "age" to input.age.toString(),
            "sex" to input.sex,
            "bmi_estimate" to String.format("%.1f", bmi),
            "activity_level" to input.activityLevel,
            "fitness_goal" to input.fitnessGoal,
        )
        if (module == ModuleFlags.MODULE_WORKOUT) {
            configs += mapOf(
                "training_days" to input.trainingDays.toString(),
                "training_experience" to input.trainingExperience,
            )
            if (input.preferredWorkoutDays.isNotEmpty()) {
                configs["preferred_workout_days"] = input.preferredWorkoutDays.joinToString(",")
            }
        }
        if (module == ModuleFlags.MODULE_DIET) {
            val bmr = NutritionCalc.bmrMifflin(input.sex, input.age, input.heightCm, input.weightKg)
            val tdee = NutritionCalc.tdee(bmr, input.activityLevel)
            val goalCalories = input.calorieOverride ?: NutritionCalc.goalCalories(tdee, input.fitnessGoal)
            val macros = NutritionCalc.macroTargets(input.weightKg, goalCalories, input.fitnessGoal)
            configs += mapOf(
                "calorie_target" to goalCalories.toString(),
                "protein_target" to macros.proteinG.toString(),
                "carb_target" to macros.carbsG.toString(),
                "fat_target" to macros.fatG.toString(),
                "bmr_estimate" to bmr.toInt().toString(),
                "tdee_estimate" to tdee.toInt().toString(),
            )
            input.targetWeightKg?.let { configs["target_weight_kg"] = it.toString() }
            input.bodyFatPct?.let { configs["body_fat_pct"] = it.toString() }
        }
    }

    private suspend fun applyWorkoutPlan(input: OnboardingInput) {
        if (input.createOwnRoutine) {
            applyCustomRoutine(input)
            return
        }
        if (input.workoutSplitId.isBlank()) return
        val scheduleCsv = scheduleDaysToCsv(input.scheduleDays)
        val result = if (input.workoutDayMapCsv.isNotBlank()) {
            WorkoutSplitLogic.buildRoutine(
                input.workoutSplitId,
                WorkoutSplitLogic.parseDayMap(input.workoutDayMapCsv),
            )
        } else {
            WorkoutSplitLogic.buildRoutineFromScheduleCsv(input.workoutSplitId, scheduleCsv)
        }
        if (result.routine == null) return
        db.configDao().upsert(UserConfigEntity("workout_split_id", input.workoutSplitId))
        if (input.workoutDayMapCsv.isNotBlank()) {
            db.configDao().upsert(UserConfigEntity("workout_split_map", input.workoutDayMapCsv))
        }
        if (result.trainingIsoDays.isNotEmpty()) {
            db.configDao().upsert(
                UserConfigEntity("schedule_days_csv", result.trainingIsoDays.joinToString(",")),
            )
        }
        db.moduleDao().upsertWorkoutRoutine(result.routine)
        db.configDao().upsert(
            UserConfigEntity(WorkoutSplitChangeLogic.KEY_APPLIED_AT, clock.nowEpochMs().toString()),
        )
        db.configDao().upsert(UserConfigEntity(WorkoutSplitChangeLogic.KEY_SCALE, "1.0"))
    }

    private suspend fun markSetupCompleted(modules: EnabledModules) {
        for (module in ModuleFlags.SELECTABLE) {
            if (!modules.isEnabled(module)) continue
            db.configDao().upsert(UserConfigEntity(ModuleFlags.setupCompletedKey(module), "true"))
            if (db.configDao().get(ModuleFlags.enabledAtKey(module)) == null) {
                db.configDao().upsert(
                    UserConfigEntity(ModuleFlags.enabledAtKey(module), clock.nowEpochMs().toString()),
                )
            }
        }
    }

    suspend fun seedWeightMetricIfNeeded(weightKg: Double) {
        if (weightKg <= 0.0) return
        if (db.moduleDao().recentMetrics("WEIGHT", 1).isNotEmpty()) return
        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
        val zone = runCatching { ZoneId.of(profile?.timezone ?: ZoneId.systemDefault().id) }
            .getOrDefault(ZoneId.systemDefault())
        val date = clock.today(zone).format(DateTimeFormatter.ISO_LOCAL_DATE)
        db.moduleDao().insertMetric(
            MetricLogEntity(
                metricType = "WEIGHT",
                value = weightKg.toFloat(),
                recordedAtEpochMs = clock.nowEpochMs(),
                date = date,
            ),
        )
    }

    suspend fun migrateModuleLifecycleIfNeeded() {
        val modules = currentModules()
        val now = clock.nowEpochMs().toString()
        for (module in ModuleFlags.SELECTABLE) {
            val initialized = moduleConfigLooksInitialized(db, module)
            val setupKey = ModuleFlags.setupCompletedKey(module)
            val existing = db.configDao().get(setupKey)?.value
            if (initialized) {
                if (!ModuleFlags.isTrue(existing)) {
                    db.configDao().upsert(UserConfigEntity(setupKey, "true"))
                }
            } else if (ModuleFlags.isTrue(existing)) {
                db.configDao().upsert(UserConfigEntity(setupKey, "false"))
            }
            val enabled = modules.isEnabled(module)
            if (enabled && initialized && db.configDao().get(ModuleFlags.enabledAtKey(module)) == null) {
                db.configDao().upsert(UserConfigEntity(ModuleFlags.enabledAtKey(module), now))
            }
        }
    }

    private suspend fun seedCareerCatalogsIfEmpty() {
        if (db.moduleDao().getDsaProblems().isEmpty()) {
            SeedData.dsaStarterProblems().forEach { db.moduleDao().upsertDsa(it) }
        }
        if (db.moduleDao().getSystemDesignTopics().isEmpty()) {
            db.moduleDao().replaceSystemDesignTopics(SeedData.systemDesignTopics())
        }
    }

    private suspend fun ensureConfigDefaults() {
        val defaults = mapOf(
            "calorie_target" to "2200",
            "protein_target" to "150",
            "step_target" to "10000",
            "notifications_enabled" to "true",
            "schedule_days_csv" to "1,2,3,4,5,6,7",
            "goal_title" to "",
            "schedule_json" to """{"wake":"07:00","sleep":"23:00"}""",
        )
        for ((key, value) in defaults) {
            if (db.configDao().get(key) == null) {
                db.configDao().upsert(UserConfigEntity(key, value))
            }
        }
    }

    suspend fun completeOnboarding(name: String, priorities: List<String>, scheduleDays: List<String> = emptyList()) {
        completeOnboarding(
            OnboardingInput(
                name = name,
                priorities = priorities,
                scheduleDays = scheduleDays,
            ),
        )
    }

    suspend fun completeOnboarding(input: OnboardingInput) {
        ensureSeeded()
        val modules = if (input.modules.anyEnabled) {
            input.modules
        } else {
            EnabledModules(career = true, workout = false, diet = false)
        }
        writeModuleFlags(modules)
        if (modules.career) applyModuleConfiguration(ModuleFlags.MODULE_CAREER, input)
        if (modules.workout) applyModuleConfiguration(ModuleFlags.MODULE_WORKOUT, input)
        if (modules.diet) applyModuleConfiguration(ModuleFlags.MODULE_DIET, input)
        markSetupCompleted(modules)

        val scheduleCsv = scheduleDaysToCsv(input.scheduleDays)
        if (scheduleCsv.isNotEmpty()) {
            db.configDao().upsert(UserConfigEntity("schedule_days_csv", scheduleCsv))
        }

        val needBody = modules.workout || modules.diet
        val existing = db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
            ?: PlayerProfileEntity(createdAtEpochMs = clock.nowEpochMs())
        val priorities = input.priorities.ifEmpty {
            buildList {
                if (modules.career) add("career")
                if (modules.workout) add("fitness")
                if (modules.diet) add("health")
            }
        }
        val deviceTimezone = ZoneId.systemDefault().id
        db.playerDao().upsertProfile(
            existing.copy(
                name = input.name.ifBlank { "Hunter" },
                prioritiesCsv = priorities.joinToString(","),
                timezone = deviceTimezone,
                onboardingDone = true,
            ),
        )

        if (needBody) {
            seedWeightMetricIfNeeded(input.weightKg)
        }

        questGeneration.generateForToday(deviceTimezone)
    }

    private suspend fun applyCustomRoutine(input: OnboardingInput) {
        val isoDays = scheduleDaysToCsv(input.scheduleDays)
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..7 }
            .distinct()
        val keyByIso = mapOf(
            1 to "monday", 2 to "tuesday", 3 to "wednesday", 4 to "thursday",
            5 to "friday", 6 to "saturday", 7 to "sunday",
        )
        var routine = WorkoutRoutineEntity()
        for (iso in 1..7) {
            val key = keyByIso.getValue(iso)
            routine = if (iso in isoDays) {
                routine.withDay(
                    key,
                    WorkoutDayPlanEntity(
                        enabled = true,
                        name = input.customDayNames[key].orEmpty().ifBlank { "Workout" },
                        exercises = emptyList(),
                    ),
                )
            } else {
                routine.withDay(key, WorkoutDayPlanEntity(enabled = false, name = "Rest"))
            }
        }
        db.moduleDao().upsertWorkoutRoutine(routine)
        if (isoDays.isNotEmpty()) {
            db.configDao().upsert(UserConfigEntity("schedule_days_csv", isoDays.sorted().joinToString(",")))
        }
    }

    /** Wipes all local progress and returns the player to a fresh seeded state (onboarding required). */
    suspend fun resetAllProgress() {
        db.clearProgressTables()
        ensureSeeded()
    }
}
