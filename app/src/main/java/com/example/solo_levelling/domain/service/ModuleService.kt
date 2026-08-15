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
import com.example.solo_levelling.data.db.entity.WorkoutDayPlanEntity
import com.example.solo_levelling.data.db.entity.WorkoutLogEntity
import com.example.solo_levelling.data.db.entity.WorkoutRoutineEntity
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

    private suspend fun todayStr(): String {
        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
        val zone = runCatching { ZoneId.of(profile?.timezone ?: ZoneId.systemDefault().id) }
            .getOrDefault(ZoneId.systemDefault())
        return clock.today(zone).format(dateFmt)
    }

    suspend fun addDsaProblem(title: String, difficulty: String, topic: String) {
        db.moduleDao().upsertDsa(
            DsaProblemEntity(
                title = title,
                difficulty = difficulty,
                topic = topic,
                externalId = "${title.hashCode()}_${clock.nowEpochMs()}",
                status = "NOT_STARTED",
            ),
        )
    }

    suspend fun markAttempted(id: Long) {
        val problem = db.moduleDao().getDsa(id) ?: return
        if (problem.status != "NOT_STARTED") return
        db.moduleDao().updateDsa(
            problem.copy(status = "ATTEMPTED", attempts = problem.attempts + 1),
        )
    }

    suspend fun solveDsa(id: Long) {
        val problem = db.moduleDao().getDsa(id) ?: return
        if (problem.status == "SOLVED" || problem.status == "MASTERED") return
        db.moduleDao().updateDsa(
            problem.copy(
                status = "SOLVED",
                attempts = problem.attempts + 1,
                confidence = (problem.confidence + 1).coerceAtMost(5),
                solvedAtEpochMs = clock.nowEpochMs(),
            ),
        )
        progression.award(
            "DSA",
            "dsa_${problem.id}",
            25,
            mapOf(AttributeCode.INT to 20, AttributeCode.DISC to 5),
            applyDailyCap = true,
        )
        addSkillXp("CAREER", problem.topic.ifBlank { "DSA" }, 25)
        verification.tryAutoComplete(todayStr())
    }

    suspend fun masterDsa(id: Long) {
        val problem = db.moduleDao().getDsa(id) ?: return
        if (problem.status != "SOLVED") return
        db.moduleDao().updateDsa(
            problem.copy(
                status = "MASTERED",
                confidence = (problem.confidence + 1).coerceAtMost(5),
            ),
        )
        progression.award(
            "DSA_MASTER",
            "dsa_master_${problem.id}",
            15,
            mapOf(AttributeCode.INT to 10, AttributeCode.DISC to 5),
            applyDailyCap = true,
        )
        addSkillXp("CAREER", problem.topic.ifBlank { "DSA" }, 15)
    }

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

    suspend fun saveRoutineDay(dayKey: String, plan: WorkoutDayPlanEntity) {
        val routine = db.moduleDao().getWorkoutRoutine()
        db.moduleDao().upsertWorkoutRoutine(routine.withDay(dayKey, plan))
    }

    suspend fun setRestDay(dayKey: String) {
        saveRoutineDay(dayKey, WorkoutDayPlanEntity(enabled = false, name = "Rest", exercises = emptyList()))
    }

    suspend fun upsertPlannedExercise(
        dayKey: String,
        exercise: PlannedExerciseEntity,
        workoutName: String = "",
    ) {
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
        val routine = db.moduleDao().getWorkoutRoutine()
        val day = routine.day(dayKey)
        saveRoutineDay(dayKey, day.copy(exercises = day.exercises.filterNot { it.id == exerciseId }))
    }

    suspend fun reorderPlannedExercise(dayKey: String, exerciseId: Long, moveUp: Boolean) {
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
            workoutName = if (plan.enabled) plan.name.ifBlank { "Workout" } else "Workout",
            exercises = exercises,
        )
        db.moduleDao().upsertWorkoutLog(log)
        return db.moduleDao().getWorkoutLog(resolved)!!
    }

    suspend fun upsertWorkoutLog(log: WorkoutLogEntity): Long {
        val id = db.moduleDao().upsertWorkoutLog(log)
        progression.award(
            "WORKOUT",
            "workout_${log.date}",
            40,
            mapOf(AttributeCode.STR to 30, AttributeCode.VIT to 10),
            applyDailyCap = true,
        )
        verification.tryAutoComplete(log.date)
        return id
    }

    suspend fun deleteWorkoutLog(date: String) {
        db.moduleDao().deleteWorkoutLog(date)
    }

    suspend fun removeExerciseFromLog(date: String, exerciseId: Long) {
        val log = db.moduleDao().getWorkoutLog(date) ?: return
        upsertWorkoutLog(log.copy(exercises = log.exercises.filterNot { it.id == exerciseId }))
    }

    suspend fun upsertLoggedExercise(date: String, exercise: LoggedExerciseEntity) {
        val log = startOrGetWorkoutLog(date)
        val list = log.exercises.toMutableList()
        val withId = if (exercise.id == 0L) exercise.copy(id = System.nanoTime()) else exercise
        val idx = list.indexOfFirst { it.id == withId.id }
        if (idx >= 0) list[idx] = withId else list.add(withId)
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
        val withTotals = log.copy(dailyTotals = computeDailyTotals(log.meals))
        db.moduleDao().upsertDietLog(withTotals)
        progression.award(
            "NUTRITION",
            "nutrition_${log.date}",
            15,
            mapOf(AttributeCode.VIT to 15),
            applyDailyCap = true,
        )
        verification.tryAutoComplete(log.date)
    }

    suspend fun deleteDietLog(date: String) {
        db.moduleDao().deleteDietLog(date)
    }

    suspend fun addMeal(date: String, name: String): Long {
        val resolved = date.ifBlank { todayStr() }
        val log = db.moduleDao().getDietLog(resolved) ?: DietLogEntity(date = resolved)
        val meal = MealEntity(id = System.nanoTime(), name = name.ifBlank { "Meal" })
        upsertDietLog(log.copy(meals = log.meals + meal))
        return meal.id
    }

    suspend fun deleteMeal(date: String, mealId: Long) {
        val resolved = date.ifBlank { todayStr() }
        val log = db.moduleDao().getDietLog(resolved) ?: return
        upsertDietLog(log.copy(meals = log.meals.filterNot { it.id == mealId }))
    }

    suspend fun renameMeal(date: String, mealId: Long, name: String) {
        val resolved = date.ifBlank { todayStr() }
        val log = db.moduleDao().getDietLog(resolved) ?: return
        upsertDietLog(
            log.copy(
                meals = log.meals.map { if (it.id == mealId) it.copy(name = name) else it },
            ),
        )
    }

    suspend fun upsertFood(date: String, mealId: Long, food: FoodItemEntity) {
        val resolved = date.ifBlank { todayStr() }
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
        db.moduleDao().upsertNutrition(NutritionLogEntity(date, calories, protein, carbs, fat))
        progression.award(
            "NUTRITION",
            "nutrition_$date",
            15,
            mapOf(AttributeCode.VIT to 15),
            applyDailyCap = true,
        )
        verification.tryAutoComplete(date)
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

    suspend fun listCareerNodes(): List<CareerNodeEntity> = db.moduleDao().getCareerNodes()

    suspend fun advanceCareerNode(id: Long) {
        val node = db.moduleDao().getCareerNode(id) ?: return
        val nextStatus = when (node.status) {
            "LOCKED" -> "STARTED"
            "STARTED" -> "LEARNING"
            "LEARNING" -> "PRACTICED"
            "PRACTICED" -> "MASTERED"
            else -> return
        }
        db.moduleDao().upsertCareerNode(node.copy(status = nextStatus))
        addSkillXp("CAREER", node.track, 10)
        if (nextStatus == "MASTERED") {
            val next = db.moduleDao().getCareerNodes()
                .firstOrNull { it.track == node.track && it.orderIndex == node.orderIndex + 1 && it.status == "LOCKED" }
            if (next != null) {
                db.moduleDao().upsertCareerNode(next.copy(status = "STARTED"))
            }
        }
    }

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
            for (key in listOf("dsa_daily", "workout_daily", "system_design")) {
                db.moduleDao().upsertBossQuest(
                    BossQuestEntity(bossId = bossId, templateKey = key, weight = 1f),
                )
            }
        }
    }

    suspend fun addBossProgress(amount: Float) {
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
}
