package com.example.solo_levelling.domain.service

import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.core.event.DomainEvent
import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.AppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.BossEntity
import com.example.solo_levelling.data.db.entity.BossQuestEntity
import com.example.solo_levelling.data.db.entity.CareerNodeEntity
import com.example.solo_levelling.data.db.entity.DietLogEntity
import com.example.solo_levelling.data.db.entity.DsaProblemEntity
import com.example.solo_levelling.data.db.entity.FocusSessionEntity
import com.example.solo_levelling.data.db.entity.FoodItemEntity
import com.example.solo_levelling.data.db.entity.JournalEntryEntity
import com.example.solo_levelling.data.db.entity.LoggedExerciseEntity
import com.example.solo_levelling.data.db.entity.LoggedSetEntity
import com.example.solo_levelling.data.db.entity.MealEntity
import com.example.solo_levelling.data.db.entity.NutritionLogEntity
import com.example.solo_levelling.data.db.entity.NutritionTotalsEntity
import com.example.solo_levelling.data.db.entity.PlannedExerciseEntity
import com.example.solo_levelling.data.db.entity.RoutineLogEntity
import com.example.solo_levelling.data.db.entity.SkillEntity
import com.example.solo_levelling.data.db.entity.UserConfigEntity
import com.example.solo_levelling.data.seed.SeedData
import com.example.solo_levelling.data.db.entity.WorkoutDayPlanEntity
import com.example.solo_levelling.data.db.entity.WorkoutLogEntity
import com.example.solo_levelling.data.db.entity.WorkoutRestKind
import com.example.solo_levelling.data.db.entity.WorkoutRoutineEntity
import com.example.solo_levelling.domain.logic.ActivityDatePolicy
import com.example.solo_levelling.domain.logic.MealCompletionPolicy
import com.example.solo_levelling.domain.model.AttributeCode
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class ModuleService(
    private val db: JsonDatabase,
    private val eventBus: EventBus,
    private val clock: AppClock,
    private val progression: ProgressionService,
    private val verification: QuestVerificationService,
) {
    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE
    private val career = CareerModuleService(db, eventBus, clock, progression, verification)

    private suspend fun todayStr(): String {
        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
        val zone = runCatching { ZoneId.of(profile?.timezone ?: ZoneId.systemDefault().id) }
            .getOrDefault(ZoneId.systemDefault())
        return clock.today(zone).format(dateFmt)
    }

    private suspend fun canWriteDate(date: String): Boolean {
        val today = runCatching { LocalDate.parse(todayStr()) }.getOrNull() ?: return false
        val parsed = runCatching { LocalDate.parse(date) }.getOrNull() ?: return false
        return ActivityDatePolicy.canWriteRecord(today, parsed)
    }

    suspend fun addDsaProblem(title: String, difficulty: String, topic: String) =
        career.addDsaProblem(title, difficulty, topic)

    suspend fun markAttempted(id: Long) = career.markAttempted(id)

    suspend fun solveDsa(id: Long) = career.solveDsa(id)

    suspend fun masterDsa(id: Long) = career.masterDsa(id)

    suspend fun markDsaNeedsReview(id: Long) = career.markDsaNeedsReview(id)

    suspend fun updateDsaNotes(id: Long, notes: String, mistakes: String, approach: String) =
        career.updateDsaNotes(id, notes, mistakes, approach)

    suspend fun ensureCareerCatalogsSeeded() = career.ensureCareerCatalogsSeeded()

    suspend fun markSystemDesignConcept(topicId: String, conceptId: String, status: String) =
        career.markSystemDesignConcept(topicId, conceptId, status)

    suspend fun setSystemDesignConfidence(topicId: String, confidence: Int) =
        career.setSystemDesignConfidence(topicId, confidence)

    suspend fun listCareerNodes(): List<CareerNodeEntity> = career.listCareerNodes()

    suspend fun advanceCareerNode(id: Long) = career.advanceCareerNode(id)

    suspend fun logWorkout(type: String, durationMinutes: Int, notes: String = ""): Long {
        val date = todayStr()
        val existing = db.moduleDao().getWorkoutLog(date)
        val log = (existing ?: WorkoutLogEntity(
            date = date,
            dayOfWeek = dayOfWeekKey(date),
            workoutName = type.ifBlank { "Workout" },
            durationMinutes = durationMinutes,
            notes = notes,
        )).copy(
            workoutName = type.ifBlank { existing?.workoutName ?: "Workout" },
            durationMinutes = durationMinutes,
            notes = notes.ifBlank { existing?.notes.orEmpty() },
        )
        val id = db.moduleDao().upsertWorkoutLog(log)
        progression.award(
            "WORKOUT",
            "workout_$date",
            40,
            mapOf(AttributeCode.STR to 30, AttributeCode.VIT to 10),
            applyDailyCap = true,
        )
        verification.tryAutoComplete(date)
        return id
    }

    suspend fun getWorkoutRoutine(): WorkoutRoutineEntity = db.moduleDao().getWorkoutRoutine()

    suspend fun isWorkoutSplitLocked(): Boolean =
        !db.configDao().get("workout_split_id")?.value.isNullOrBlank()

    suspend fun workoutSplitAppliedAtMs(): Long? =
        db.configDao().get(WorkoutSplitChangeLogic.KEY_APPLIED_AT)?.value?.toLongOrNull()

    suspend fun isEarlySplitChange(nowEpochMs: Long = clock.nowEpochMs()): Boolean {
        val applied = workoutSplitAppliedAtMs()
        if (applied != null) return WorkoutSplitChangeLogic.isEarlyChange(applied, nowEpochMs)
        // Locked split without timestamp (legacy) → treat as early until re-applied
        return isWorkoutSplitLocked()
    }

    suspend fun weeksOnCurrentSplit(nowEpochMs: Long = clock.nowEpochMs()): Long =
        WorkoutSplitChangeLogic.weeksHeld(workoutSplitAppliedAtMs(), nowEpochMs)

    suspend fun workoutProgressionScale(nowEpochMs: Long = clock.nowEpochMs()): Float {
        val appliedAt = workoutSplitAppliedAtMs()
        val stored = db.configDao().get(WorkoutSplitChangeLogic.KEY_SCALE)?.value?.toFloatOrNull()
        val scale = WorkoutSplitChangeLogic.resolvedScale(appliedAt, stored, nowEpochMs)
        if (scale == 1f && stored != null && stored != 1f) {
            db.configDao().upsert(UserConfigEntity(WorkoutSplitChangeLogic.KEY_SCALE, "1.0"))
        }
        return scale
    }

    /**
     * Returns error message on failure, null on success.
     * [dayMapCsv] format: `"1:1,2:3,3:5"` (split day → ISO weekday 1=Mon…7=Sun).
     * When [confirmEarlyChange] is false and the current split is held under 6 months,
     * returns a sentinel requiring UI confirmation instead of applying.
     */
    suspend fun applyWorkoutSplit(
        splitId: String,
        dayMapCsv: String,
        confirmEarlyChange: Boolean = false,
    ): String? {
        val map = WorkoutSplitLogic.parseDayMap(dayMapCsv)
        val result = WorkoutSplitLogic.buildRoutine(splitId, map)
        if (result.error != null) return result.error

        val now = clock.nowEpochMs()
        val early = isEarlySplitChange(now)
        if (early && !confirmEarlyChange) {
            return EARLY_SPLIT_CHANGE_REQUIRED
        }

        db.configDao().upsert(UserConfigEntity("workout_split_id", splitId))
        db.configDao().upsert(
            UserConfigEntity("workout_split_map", WorkoutSplitLogic.encodeDayMap(map)),
        )
        if (result.trainingIsoDays.isNotEmpty()) {
            db.configDao().upsert(
                UserConfigEntity("schedule_days_csv", result.trainingIsoDays.joinToString(",")),
            )
        }
        db.moduleDao().upsertWorkoutRoutine(result.routine!!)
        db.configDao().upsert(UserConfigEntity(WorkoutSplitChangeLogic.KEY_APPLIED_AT, now.toString()))
        val scale = WorkoutSplitChangeLogic.scaleForNewApply(wasEarly = early)
        db.configDao().upsert(UserConfigEntity(WorkoutSplitChangeLogic.KEY_SCALE, scale.toString()))
        reseedTodayLogIfEmpty()
        return null
    }

    /** Drop today's unfinished log so the next open reseeds from the new routine. */
    private suspend fun reseedTodayLogIfEmpty() {
        val today = todayStr()
        val existing = db.moduleDao().getWorkoutLog(today) ?: return
        if (existing.isTrainingDayComplete()) return
        db.moduleDao().deleteWorkoutLog(today)
    }

    companion object {
        const val EARLY_SPLIT_CHANGE_REQUIRED = "EARLY_SPLIT_CHANGE_REQUIRED"
    }

    suspend fun saveRoutineDay(dayKey: String, plan: WorkoutDayPlanEntity) {
        if (isWorkoutSplitLocked()) return
        val routine = db.moduleDao().getWorkoutRoutine()
        db.moduleDao().upsertWorkoutRoutine(routine.withDay(dayKey, plan))
    }

    suspend fun setRestDay(dayKey: String) {
        if (isWorkoutSplitLocked()) return
        saveRoutineDay(dayKey, WorkoutDayPlanEntity(enabled = false, name = "Rest", exercises = emptyList()))
    }

    suspend fun upsertPlannedExercise(
        dayKey: String,
        exercise: PlannedExerciseEntity,
        workoutName: String = "",
    ) {
        if (isWorkoutSplitLocked()) return
        val routine = db.moduleDao().getWorkoutRoutine()
        val day = routine.day(dayKey)
        val list = day.exercises.toMutableList()
        val withId = if (exercise.id == 0L) {
            exercise.copy(id = System.nanoTime())
        } else {
            exercise
        }
        val idx = list.indexOfFirst { it.id == withId.id }
        if (idx >= 0) list[idx] = withId else list.add(withId)
        val name = workoutName.ifBlank { day.name }.ifBlank { "Workout" }
        saveRoutineDay(dayKey, day.copy(enabled = true, name = name, exercises = list))
    }

    suspend fun removePlannedExercise(dayKey: String, exerciseId: Long) {
        if (isWorkoutSplitLocked()) return
        val routine = db.moduleDao().getWorkoutRoutine()
        val day = routine.day(dayKey)
        saveRoutineDay(dayKey, day.copy(exercises = day.exercises.filterNot { it.id == exerciseId }))
    }

    suspend fun reorderPlannedExercise(dayKey: String, exerciseId: Long, moveUp: Boolean) {
        if (isWorkoutSplitLocked()) return
        val routine = db.moduleDao().getWorkoutRoutine()
        val day = routine.day(dayKey)
        val list = day.exercises.toMutableList()
        val idx = list.indexOfFirst { it.id == exerciseId }
        if (idx < 0) return
        val target = if (moveUp) idx - 1 else idx + 1
        if (target !in list.indices) return
        val tmp = list[idx]
        list[idx] = list[target]
        list[target] = tmp
        saveRoutineDay(dayKey, day.copy(exercises = list))
    }

    suspend fun startOrGetWorkoutLog(date: String): WorkoutLogEntity {
        val resolved = date.ifBlank { todayStr() }
        val existing = db.moduleDao().getWorkoutLog(resolved)
        if (existing != null) return existing
        val dayKey = dayOfWeekKey(resolved)
        val plan = db.moduleDao().getWorkoutRoutine().day(dayKey)
        val exercises = if (plan.enabled) {
            plan.exercises.map { pe ->
                LoggedExerciseEntity(
                    id = System.nanoTime() + pe.id,
                    name = pe.name,
                    sets = emptyList(),
                )
            }
        } else {
            emptyList()
        }
        val log = WorkoutLogEntity(
            date = resolved,
            dayOfWeek = dayKey,
            workoutName = if (plan.enabled) plan.name.ifBlank { "Workout" } else "Rest",
            exercises = exercises,
        )
        if (!canWriteDate(resolved)) return log
        db.moduleDao().upsertWorkoutLog(log)
        return db.moduleDao().getWorkoutLog(resolved)!!
    }

    /**
     * Completes a rest day. [activeRest] awards half training XP (scaled); complete rest persists only.
     * Idempotent: no-op if the day already has sets or a restKind.
     */
    suspend fun completeRestDay(date: String, activeRest: Boolean): WorkoutLogEntity {
        val resolved = date.ifBlank { todayStr() }
        if (!canWriteDate(resolved)) {
            return db.moduleDao().getWorkoutLog(resolved) ?: startOrGetWorkoutLog(resolved)
        }
        val existing = startOrGetWorkoutLog(resolved)
        if (existing.isTrainingDayComplete()) return existing

        val kind = if (activeRest) WorkoutRestKind.ACTIVE_REST else WorkoutRestKind.COMPLETE_REST
        val updated = existing.copy(
            workoutName = "Rest",
            restKind = kind,
            exercises = emptyList(),
        )
        db.moduleDao().upsertWorkoutLog(updated)

        if (activeRest) {
            val modules = progression.currentModules()
            if (modules.workout) {
                val scale = workoutProgressionScale()
                val xp = (20 * scale).toInt().coerceAtLeast(0)
                val str = (10 * scale).toInt().coerceAtLeast(0)
                val vit = (10 * scale).toInt().coerceAtLeast(0)
                progression.award(
                    "WORKOUT",
                    "workout_${updated.date}",
                    xp,
                    mapOf(AttributeCode.STR to str, AttributeCode.VIT to vit),
                    metadataJson = """{"module":"WORKOUT","STR":$str,"VIT":$vit}""",
                    applyDailyCap = true,
                    modules = modules,
                )
            }
        }
        verification.tryAutoComplete(resolved)
        return db.moduleDao().getWorkoutLog(resolved)!!
    }

    suspend fun upsertWorkoutLog(log: WorkoutLogEntity): Long {
        if (!canWriteDate(log.date)) return log.id
        val id = db.moduleDao().upsertWorkoutLog(log)
        if (log.exercises.any { it.sets.isNotEmpty() }) {
            val modules = progression.currentModules()
            if (modules.workout) {
                val scale = workoutProgressionScale()
                val xp = (40 * scale).toInt().coerceAtLeast(0)
                val str = (30 * scale).toInt().coerceAtLeast(0)
                val vit = (10 * scale).toInt().coerceAtLeast(0)
                progression.award(
                    "WORKOUT",
                    "workout_${log.date}",
                    xp,
                    mapOf(AttributeCode.STR to str, AttributeCode.VIT to vit),
                    metadataJson = """{"module":"WORKOUT","STR":$str,"VIT":$vit}""",
                    applyDailyCap = true,
                    modules = modules,
                )
            }
        } else if (log.restKind != WorkoutRestKind.ACTIVE_REST) {
            reverseModuleAward("WORKOUT", "workout_${log.date}", "WORKOUT_UNDO", "UNDO_WORKOUT_")
        }
        verification.tryAutoComplete(log.date)
        return id
    }

    suspend fun deleteWorkoutLog(date: String) {
        if (!canWriteDate(date)) return
        db.moduleDao().deleteWorkoutLog(date)
        reverseModuleAward("WORKOUT", "workout_$date", "WORKOUT_UNDO", "UNDO_WORKOUT_")
        verification.tryAutoComplete(date)
    }

    suspend fun removeExerciseFromLog(date: String, exerciseId: Long) {
        if (isWorkoutSplitLocked()) return
        val log = db.moduleDao().getWorkoutLog(date) ?: return
        upsertWorkoutLog(log.copy(exercises = log.exercises.filterNot { it.id == exerciseId }))
    }

    suspend fun upsertLoggedExercise(date: String, exercise: LoggedExerciseEntity) {
        val log = startOrGetWorkoutLog(date)
        val list = log.exercises.toMutableList()
        if (exercise.id == 0L) {
            if (isWorkoutSplitLocked()) return
            list.add(exercise.copy(id = System.nanoTime()))
        } else {
            val idx = list.indexOfFirst { it.id == exercise.id }
            if (idx < 0) {
                if (isWorkoutSplitLocked()) return
                list.add(exercise)
            } else {
                val existing = list[idx]
                list[idx] = if (isWorkoutSplitLocked()) {
                    existing.copy(sets = exercise.sets)
                } else {
                    exercise
                }
            }
        }
        upsertWorkoutLog(log.copy(exercises = list))
    }

    suspend fun workoutsForWeek(weekStart: String): List<WorkoutLogEntity> {
        val end = LocalDate.parse(weekStart).plusDays(6).toString()
        return db.moduleDao().getAllWorkoutLogs()
            .filter { it.date >= weekStart && it.date <= end }
            .sortedBy { it.date }
    }

    suspend fun exerciseHistory(name: String, limit: Int = 5): List<Pair<String, LoggedExerciseEntity>> {
        return db.moduleDao().getAllWorkoutLogs()
            .sortedByDescending { it.date }
            .flatMap { log ->
                log.exercises
                    .filter { it.name.equals(name, ignoreCase = true) }
                    .map { log.date to it }
            }
            .take(limit)
    }

    suspend fun getDietLog(date: String): DietLogEntity? = db.moduleDao().getDietLog(date)

    suspend fun upsertDietLog(log: DietLogEntity) {
        if (!canWriteDate(log.date)) return
        val withTotals = log.copy(dailyTotals = computeDailyTotals(log.meals))
        db.moduleDao().upsertDietLog(withTotals)
        val trackingComplete = MealCompletionPolicy.isMealTrackingComplete(withTotals)
        if (trackingComplete) {
            val modules = progression.currentModules()
            if (modules.diet) {
                progression.award(
                    "NUTRITION",
                    "nutrition_${log.date}",
                    15,
                    mapOf(AttributeCode.VIT to 15),
                    metadataJson = """{"module":"DIET","VIT":15}""",
                    applyDailyCap = true,
                    modules = modules,
                )
            }
        } else {
            reverseModuleAward("NUTRITION", "nutrition_${log.date}", "NUTRITION_UNDO", "UNDO_NUTRITION_")
        }
        verification.tryAutoComplete(log.date)
    }

    suspend fun deleteDietLog(date: String) {
        if (!canWriteDate(date)) return
        db.moduleDao().deleteDietLog(date)
        reverseModuleAward("NUTRITION", "nutrition_$date", "NUTRITION_UNDO", "UNDO_NUTRITION_")
        verification.tryAutoComplete(date)
    }

    suspend fun addMeal(date: String, name: String): Long {
        val resolved = date.ifBlank { todayStr() }
        if (!canWriteDate(resolved)) return 0L
        val log = db.moduleDao().getDietLog(resolved) ?: DietLogEntity(date = resolved)
        val meal = MealEntity(id = System.nanoTime(), name = name.ifBlank { "Meal" })
        upsertDietLog(log.copy(meals = log.meals + meal))
        return meal.id
    }

    suspend fun deleteMeal(date: String, mealId: Long) {
        val resolved = date.ifBlank { todayStr() }
        if (!canWriteDate(resolved)) return
        val log = db.moduleDao().getDietLog(resolved) ?: return
        upsertDietLog(log.copy(meals = log.meals.filterNot { it.id == mealId }))
    }

    suspend fun renameMeal(date: String, mealId: Long, name: String) {
        val resolved = date.ifBlank { todayStr() }
        if (!canWriteDate(resolved)) return
        val log = db.moduleDao().getDietLog(resolved) ?: return
        upsertDietLog(
            log.copy(
                meals = log.meals.map { if (it.id == mealId) it.copy(name = name) else it },
            ),
        )
    }

    suspend fun upsertFood(date: String, mealId: Long, food: FoodItemEntity) {
        val resolved = date.ifBlank { todayStr() }
        if (!canWriteDate(resolved)) return
        val log = db.moduleDao().getDietLog(resolved) ?: DietLogEntity(date = resolved)
        val withId = if (food.id == 0L) food.copy(id = System.nanoTime()) else food
        val meals = log.meals.toMutableList()
        val mealIdx = meals.indexOfFirst { it.id == mealId }
        if (mealIdx < 0) return
        val foods = meals[mealIdx].foods.toMutableList()
        val foodIdx = foods.indexOfFirst { it.id == withId.id }
        if (foodIdx >= 0) foods[foodIdx] = withId else foods.add(withId)
        meals[mealIdx] = meals[mealIdx].copy(foods = foods)
        upsertDietLog(log.copy(meals = meals))
    }

    suspend fun deleteFood(date: String, mealId: Long, foodId: Long) {
        val resolved = date.ifBlank { todayStr() }
        if (!canWriteDate(resolved)) return
        val log = db.moduleDao().getDietLog(resolved) ?: return
        upsertDietLog(
            log.copy(
                meals = log.meals.map { meal ->
                    if (meal.id == mealId) meal.copy(foods = meal.foods.filterNot { it.id == foodId }) else meal
                },
            ),
        )
    }

    suspend fun logNutrition(calories: Int, protein: Int, carbs: Int, fat: Int) {
        val date = todayStr()
        if (!canWriteDate(date)) return
        val log = db.moduleDao().getDietLog(date) ?: DietLogEntity(date = date)
        val meal = log.meals.firstOrNull { it.name == "Logged macros" }
            ?: MealEntity(id = System.nanoTime(), name = "Logged macros")
        val food = FoodItemEntity(
            id = System.nanoTime(),
            name = "Logged macros",
            calories = calories,
            protein = protein,
            carbs = carbs,
            fat = fat,
        )
        val meals = if (log.meals.any { it.id == meal.id }) {
            log.meals.map { if (it.id == meal.id) it.copy(foods = listOf(food)) else it }
        } else {
            log.meals + meal.copy(foods = listOf(food))
        }
        upsertDietLog(log.copy(meals = meals))
    }

    fun mealTotals(meal: MealEntity): NutritionTotalsEntity = sumFoods(meal.foods)

    private fun computeDailyTotals(meals: List<MealEntity>): NutritionTotalsEntity =
        sumFoods(meals.flatMap { it.foods })

    private fun sumFoods(foods: List<FoodItemEntity>): NutritionTotalsEntity {
        var calories = 0
        var protein = 0
        var carbs = 0
        var fat = 0
        for (f in foods) {
            calories += f.calories ?: 0
            protein += f.protein ?: 0
            carbs += f.carbs ?: 0
            fat += f.fat ?: 0
        }
        return NutritionTotalsEntity(calories, protein, carbs, fat)
    }

    private fun dayOfWeekKey(date: String): String =
        runCatching {
            LocalDate.parse(date).dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH).lowercase()
        }.getOrDefault("")

    suspend fun logFocus(durationMinutes: Int, label: String) {
        val date = todayStr()
        val now = clock.nowEpochMs()
        val id = db.moduleDao().insertFocus(
            FocusSessionEntity(
                date = date,
                durationMinutes = durationMinutes,
                label = label,
                completedAtEpochMs = now,
            ),
        )
        val xp = (durationMinutes / 15).coerceAtLeast(1) * 10
        progression.award("FOCUS", "focus_$id", xp, mapOf(AttributeCode.FOC to xp), applyDailyCap = true)
        verification.tryAutoComplete(date)
    }

    suspend fun saveJournal(content: String) {
        val date = todayStr()
        val now = clock.nowEpochMs()
        db.moduleDao().upsertJournal(JournalEntryEntity(date, content, now))
        progression.award(
            "JOURNAL",
            "journal_$date",
            20,
            mapOf(AttributeCode.WIS to 15, AttributeCode.DISC to 5),
            applyDailyCap = true,
        )
        verification.tryAutoComplete(date)
    }

    suspend fun getJournal(date: String): JournalEntryEntity? = db.moduleDao().getJournal(date)

    suspend fun logRoutine(kind: String) {
        val date = todayStr()
        db.moduleDao().insertRoutineLog(
            RoutineLogEntity(date = date, kind = kind, completedAtEpochMs = clock.nowEpochMs()),
        )
        verification.tryAutoComplete(date)
    }

    suspend fun createBoss(title: String, description: String, xpReward: Int, linkDefaultQuests: Boolean = true) {
        val bossId = db.moduleDao().upsertBoss(
            BossEntity(title = title, description = description, xpReward = xpReward),
        )
        if (linkDefaultQuests) {
            val modules = progression.currentModules()
            val keys = buildList {
                if (modules.career) {
                    add("dsa_daily")
                    add("system_design")
                }
                if (modules.workout) add("workout_daily")
            }
            for (key in keys) {
                db.moduleDao().upsertBossQuest(
                    BossQuestEntity(bossId = bossId, templateKey = key, weight = 1f),
                )
            }
        }
    }

    /**
     * Debug-only additive boss progress. Production gameplay uses [BossProgressHandler] + [BossProgressLogic].
     */
    suspend fun addBossProgress(amount: Float) {
        if (!com.example.solo_levelling.BuildConfig.DEBUG) return
        val boss = db.moduleDao().getActiveBoss() ?: return
        val newValue = (boss.currentValue + amount).coerceAtMost(boss.targetValue)
        val cleared = newValue >= boss.targetValue
        db.moduleDao().updateBoss(
            boss.copy(currentValue = newValue, status = if (cleared) "CLEARED" else "ACTIVE"),
        )
        eventBus.publish(DomainEvent.BossProgressUpdated(boss.id, newValue / boss.targetValue))
        if (cleared) {
            progression.award(
                "BOSS",
                "boss_${boss.id}",
                boss.xpReward,
                mapOf(AttributeCode.DISC to (boss.xpReward / 2).coerceAtLeast(1)),
                applyDailyCap = true,
            )
            eventBus.publish(DomainEvent.BossCompleted(boss.id, boss.xpReward))
        }
    }

    suspend fun addSkillXp(domain: String, name: String, xp: Int) {
        val existing = db.moduleDao().findSkill(domain, name)
        val totalXp = (existing?.xp ?: 0) + xp
        val level = 1 + totalXp / 100
        val skill = SkillEntity(
            id = existing?.id ?: 0,
            domain = domain,
            name = name,
            xp = totalXp,
            level = level,
        )
        val newId = if (existing == null) {
            db.moduleDao().upsertSkill(skill)
        } else {
            db.moduleDao().updateSkill(skill)
            existing.id
        }
        if (existing == null || level > existing.level) {
            eventBus.publish(DomainEvent.SkillLevelUp(newId, level))
        }
    }

    private suspend fun reverseModuleAward(
        sourceType: String,
        baseSourceId: String,
        reverseSourceType: String,
        reverseIdPrefix: String,
    ) {
        val award = progression.findUnrevertedAward(sourceType, baseSourceId) ?: return
        val attrs = AttributeRewardsParser.parse(award.metadataJson).associate { it.code to it.amount }
        progression.reverse(
            originalSourceType = award.sourceType,
            originalSourceId = award.sourceId,
            reverseSourceType = reverseSourceType,
            reverseSourceId = "$reverseIdPrefix${award.id}",
            attrs = attrs,
        )
    }
}
