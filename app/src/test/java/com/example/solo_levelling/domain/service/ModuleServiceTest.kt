package com.example.solo_levelling.domain.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.FakeAppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.AttributeStatEntity
import com.example.solo_levelling.data.db.entity.CareerNodeEntity
import com.example.solo_levelling.data.db.entity.DsaProblemEntity
import com.example.solo_levelling.data.db.entity.FoodItemEntity
import com.example.solo_levelling.data.db.entity.LoggedExerciseEntity
import com.example.solo_levelling.data.db.entity.LoggedSetEntity
import com.example.solo_levelling.data.db.entity.PlannedExerciseEntity
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.RepRangeEntity
import com.example.solo_levelling.data.db.entity.WorkoutDayPlanEntity
import com.example.solo_levelling.data.db.entity.WorkoutLogEntity
import com.example.solo_levelling.domain.model.AttributeCode
import com.example.solo_levelling.domain.service.ProgressionService.AwardResult
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ModuleServiceTest {
    private lateinit var db: JsonDatabase
    private lateinit var eventBus: EventBus
    private lateinit var clock: FakeAppClock
    private lateinit var progression: ProgressionService
    private lateinit var verification: QuestVerificationService
    private lateinit var service: ModuleService

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = JsonDatabase(File(context.cacheDir, "test-db-${System.nanoTime()}").also { it.mkdirs() })
        eventBus = EventBus()
        clock = FakeAppClock(fixedDate = LocalDate.of(2026, 8, 15))
        progression = ProgressionService(db, eventBus, clock)
        val questCompletion = QuestCompletionService(db, eventBus, clock, progression)
        verification = QuestVerificationService(db, clock, questCompletion)
        service = ModuleService(db, eventBus, clock, progression, verification)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seedProfile() {
        db.playerDao().upsertProfile(
            PlayerProfileEntity(
                name = "Test",
                timezone = "UTC",
                onboardingDone = true,
                createdAtEpochMs = clock.nowEpochMs(),
            ),
        )
        db.playerDao().upsertAttributes(
            AttributeCode.entries.map { AttributeStatEntity(it.name) },
        )
    }

    @Test
    fun p_solveDsa_awardsXpOnce() = runTest {
        seedProfile()
        val id = db.moduleDao().upsertDsa(
            DsaProblemEntity(title = "Two Sum", externalId = "two_sum", status = "ATTEMPTED"),
        )
        service.solveDsa(id)
        assertEquals(25, db.playerDao().getProfile(1)!!.totalXp)
        assertEquals("SOLVED", db.moduleDao().getDsa(id)!!.status)
    }

    @Test
    fun n_solveDsaTwice_noDoubleXp() = runTest {
        seedProfile()
        val id = db.moduleDao().upsertDsa(
            DsaProblemEntity(title = "Two Sum", externalId = "two_sum", status = "ATTEMPTED"),
        )
        service.solveDsa(id)
        service.solveDsa(id)
        assertEquals(25, db.playerDao().getProfile(1)!!.totalXp)
        assertEquals(1, db.xpDao().getAllLedger().count { it.sourceType == "DSA" })
    }

    @Test
    fun e_advanceCareerNode_unlocksNext() = runTest {
        seedProfile()
        val firstId = db.moduleDao().upsertCareerNode(
            CareerNodeEntity(track = "DSA", title = "Arrays", orderIndex = 1, status = "PRACTICED"),
        )
        val secondId = db.moduleDao().upsertCareerNode(
            CareerNodeEntity(track = "DSA", title = "Trees", orderIndex = 2, status = "LOCKED"),
        )
        service.advanceCareerNode(firstId)
        assertEquals("MASTERED", db.moduleDao().getCareerNode(firstId)!!.status)
        assertEquals("STARTED", db.moduleDao().getCareerNode(secondId)!!.status)
    }

    @Test
    fun p_logNutrition_persists() = runTest {
        seedProfile()
        service.logNutrition(2100, 150, 200, 60)
        val log = db.moduleDao().getNutrition("2026-08-15")
        assertNotNull(log)
        assertEquals(2100, log!!.calories)
        assertEquals(150, log.protein)
        assertTrue(
            progression.award("NUTRITION", "nutrition_2026-08-15", 15) is AwardResult.Success,
        )
    }

    @Test
    fun p_saveRoutineAndLogSets_persistsIndependently() = runTest {
        seedProfile()
        service.saveRoutineDay(
            "saturday",
            WorkoutDayPlanEntity(
                enabled = true,
                name = "Chest + Triceps",
                exercises = listOf(
                    PlannedExerciseEntity(
                        id = 1,
                        name = "Bench Press",
                        targetMuscle = "Chest",
                        sets = 3,
                        repRange = RepRangeEntity(8, 12),
                    ),
                ),
            ),
        )
        assertEquals("Chest + Triceps", service.getWorkoutRoutine().saturday.name)

        val started = service.startOrGetWorkoutLog("2026-08-15")
        assertEquals("Chest + Triceps", started.workoutName)
        val planned = started.exercises.first { it.name == "Bench Press" }
        service.upsertLoggedExercise(
            "2026-08-15",
            planned.copy(
                sets = listOf(
                    LoggedSetEntity(60f, 10),
                    LoggedSetEntity(60f, 9),
                    LoggedSetEntity(65f, 7),
                ),
            ),
        )
        val log = db.moduleDao().getWorkoutLog("2026-08-15")!!
        assertEquals(1, log.exercises.size)
        assertEquals(3, log.exercises.first().sets.size)
        assertEquals(65f, log.exercises.first().sets[2].weight)
        assertTrue(
            progression.award("WORKOUT", "workout_2026-08-15", 40) is AwardResult.AlreadyAwarded,
        )
    }

    @Test
    fun n_deleteWorkoutLog_removesFileAndMemory() = runTest {
        seedProfile()
        service.upsertWorkoutLog(WorkoutLogEntity(date = "2026-08-15", workoutName = "Legs"))
        service.deleteWorkoutLog("2026-08-15")
        assertNull(db.moduleDao().getWorkoutLog("2026-08-15"))
    }

    @Test
    fun e_dietTotals_sumOptionalMacros() = runTest {
        seedProfile()
        val mealId = service.addMeal("2026-08-15", "Breakfast")
        service.upsertFood(
            "2026-08-15",
            mealId,
            FoodItemEntity(name = "Oats", quantity = 60f, unit = "g", calories = 228, protein = 8, carbs = 40, fat = 4),
        )
        service.upsertFood(
            "2026-08-15",
            mealId,
            FoodItemEntity(name = "Banana"),
        )
        val diet = db.moduleDao().getDietLog("2026-08-15")!!
        assertEquals(228, diet.dailyTotals.calories)
        assertEquals(8, diet.dailyTotals.protein)
        assertEquals(2, diet.meals.first().foods.size)
        val nutrition = db.moduleDao().getNutrition("2026-08-15")
        assertEquals(228, nutrition!!.calories)
    }

    @Test
    fun n_addMeal_blankDate_fallsBackToToday() = runTest {
        seedProfile()
        service.addMeal("", "Snack")
        val diet = db.moduleDao().getDietLog("2026-08-15")
        assertNotNull(diet)
        assertEquals("Snack", diet!!.meals.first().name)
        assertNull(db.moduleDao().getDietLog(""))
    }

    @Test
    fun p_upsertPlannedExercise_appliesWorkoutName() = runTest {
        seedProfile()
        service.upsertPlannedExercise(
            "tuesday",
            PlannedExerciseEntity(name = "Squat", targetMuscle = "Legs", sets = 4),
            workoutName = "Leg Day",
        )
        val day = service.getWorkoutRoutine().tuesday
        assertEquals(true, day.enabled)
        assertEquals("Leg Day", day.name)
        assertEquals("Squat", day.exercises.first().name)
    }

    @Test
    fun n_addMeal_emptyFoods_doesNotAwardNutritionXp() = runTest {
        seedProfile()
        service.addMeal("2026-08-15", "Empty")
        assertEquals(0, db.playerDao().getProfile(1)!!.totalXp)
        assertTrue(
            progression.award("NUTRITION", "nutrition_2026-08-15", 15) is AwardResult.Success,
        )
    }

    @Test
    fun p_upsertFood_awardsNutritionXpAtThirdMeal() = runTest {
        seedProfile()
        db.configDao().upsert(
            com.example.solo_levelling.data.db.entity.UserConfigEntity("module_diet", "true"),
        )
        val breakfast = service.addMeal("2026-08-15", "Breakfast")
        service.upsertFood(
            "2026-08-15",
            breakfast,
            FoodItemEntity(name = "Oats", quantity = 60f, unit = "g", calories = 228, protein = 8),
        )
        assertTrue(
            progression.award("NUTRITION", "nutrition_2026-08-15", 15) is AwardResult.Success,
        )
        val lunch = service.addMeal("2026-08-15", "Lunch")
        service.upsertFood(
            "2026-08-15",
            lunch,
            FoodItemEntity(name = "Rice", calories = 300, protein = 6),
        )
        val dinner = service.addMeal("2026-08-15", "Dinner")
        service.upsertFood(
            "2026-08-15",
            dinner,
            FoodItemEntity(name = "Chicken", calories = 400, protein = 35),
        )
        assertTrue(
            progression.award("NUTRITION", "nutrition_2026-08-15", 15) is AwardResult.AlreadyAwarded,
        )
    }

    @Test
    fun n_upsertWorkoutLog_noSets_doesNotAwardXp() = runTest {
        seedProfile()
        service.upsertWorkoutLog(
            WorkoutLogEntity(
                date = "2026-08-15",
                workoutName = "Empty",
                exercises = listOf(LoggedExerciseEntity(name = "Squat")),
            ),
        )
        assertEquals(0, db.playerDao().getProfile(1)!!.totalXp)
        assertTrue(
            progression.award("WORKOUT", "workout_2026-08-15", 40) is AwardResult.Success,
        )
    }

    @Test
    fun n_upsertWorkoutLog_futureDate_doesNotPersistOrAward() = runTest {
        seedProfile()
        service.upsertWorkoutLog(
            WorkoutLogEntity(
                date = "2026-08-17",
                workoutName = "Future",
                exercises = listOf(
                    LoggedExerciseEntity(name = "Squat", sets = listOf(LoggedSetEntity(60f, 5))),
                ),
            ),
        )
        assertNull(db.moduleDao().getWorkoutLog("2026-08-17"))
        assertEquals(0, db.playerDao().getProfile(1)!!.totalXp)
    }

    @Test
    fun n_upsertWorkoutLog_pastDate_doesNotPersistOrAward() = runTest {
        seedProfile()
        service.upsertWorkoutLog(
            WorkoutLogEntity(
                date = "2026-08-14",
                workoutName = "Yesterday",
                exercises = listOf(
                    LoggedExerciseEntity(name = "Squat", sets = listOf(LoggedSetEntity(60f, 5))),
                ),
            ),
        )
        assertNull(db.moduleDao().getWorkoutLog("2026-08-14"))
        assertEquals(0, db.playerDao().getProfile(1)!!.totalXp)
    }

    @Test
    fun n_addMeal_futureDate_doesNotPersist() = runTest {
        seedProfile()
        val id = service.addMeal("2026-08-17", "Breakfast")
        assertEquals(0L, id)
        assertNull(db.moduleDao().getDietLog("2026-08-17"))
    }

    @Test
    fun n_startOrGetWorkoutLog_pastDate_doesNotCreateFile() = runTest {
        seedProfile()
        val draft = service.startOrGetWorkoutLog("2026-08-14")
        assertEquals("2026-08-14", draft.date)
        assertNull(db.moduleDao().getWorkoutLog("2026-08-14"))
    }

    @Test
    fun n_deleteWorkoutLog_pastDate_leavesExistingRecord() = runTest {
        seedProfile()
        db.moduleDao().upsertWorkoutLog(WorkoutLogEntity(date = "2026-08-14", workoutName = "Old"))
        service.deleteWorkoutLog("2026-08-14")
        assertNotNull(db.moduleDao().getWorkoutLog("2026-08-14"))
    }

    @Test
    fun r_deleteWorkoutLog_reversesModuleXp() = runTest {
        seedProfile()
        service.upsertWorkoutLog(
            WorkoutLogEntity(
                date = "2026-08-15",
                workoutName = "Push",
                exercises = listOf(
                    LoggedExerciseEntity(name = "Bench", sets = listOf(LoggedSetEntity(60f, 8))),
                ),
            ),
        )
        assertTrue(db.playerDao().getProfile(1)!!.totalXp > 0)
        service.deleteWorkoutLog("2026-08-15")
        assertNull(db.moduleDao().getWorkoutLog("2026-08-15"))
        assertEquals(0, db.playerDao().getProfile(1)!!.totalXp)
        assertEquals(1, db.xpDao().getAllLedger().count { it.sourceType == "WORKOUT_UNDO" })
    }

    @Test
    fun p_relogWorkoutAfterDelete_awardsAgain() = runTest {
        seedProfile()
        val log = WorkoutLogEntity(
            date = "2026-08-15",
            workoutName = "Push",
            exercises = listOf(
                LoggedExerciseEntity(name = "Bench", sets = listOf(LoggedSetEntity(60f, 8))),
            ),
        )
        service.upsertWorkoutLog(log)
        service.deleteWorkoutLog("2026-08-15")
        service.upsertWorkoutLog(log)
        assertEquals(40, db.playerDao().getProfile(1)!!.totalXp)
        assertEquals(2, db.xpDao().getAllLedger().count { it.sourceType == "WORKOUT" && it.amount > 0 })
    }

    @Test
    fun r_deleteLastFood_reversesNutritionXp() = runTest {
        seedProfile()
        db.configDao().upsert(
            com.example.solo_levelling.data.db.entity.UserConfigEntity("module_diet", "true"),
        )
        val breakfast = service.addMeal("2026-08-15", "Breakfast")
        service.upsertFood("2026-08-15", breakfast, FoodItemEntity(name = "Oats", calories = 200, protein = 8))
        val lunch = service.addMeal("2026-08-15", "Lunch")
        service.upsertFood("2026-08-15", lunch, FoodItemEntity(name = "Rice", calories = 200, protein = 4))
        val dinner = service.addMeal("2026-08-15", "Dinner")
        service.upsertFood("2026-08-15", dinner, FoodItemEntity(name = "Chicken", calories = 300, protein = 30))
        assertEquals(15, db.playerDao().getProfile(1)!!.totalXp)
        val saved = db.moduleDao().getDietLog("2026-08-15")!!.meals.first { it.name == "Dinner" }.foods.first()
        service.deleteFood("2026-08-15", dinner, saved.id)
        assertEquals(0, db.playerDao().getProfile(1)!!.totalXp)
        assertEquals(1, db.xpDao().getAllLedger().count { it.sourceType == "NUTRITION_UNDO" })
    }

    @Test
    fun e_deleteOneFood_keepsNutritionXpWhenFoodRemains() = runTest {
        seedProfile()
        db.configDao().upsert(
            com.example.solo_levelling.data.db.entity.UserConfigEntity("module_diet", "true"),
        )
        val breakfast = service.addMeal("2026-08-15", "Breakfast")
        service.upsertFood("2026-08-15", breakfast, FoodItemEntity(name = "Oats", calories = 200, protein = 8))
        val lunch = service.addMeal("2026-08-15", "Lunch")
        service.upsertFood("2026-08-15", lunch, FoodItemEntity(name = "Rice", calories = 200, protein = 4))
        service.upsertFood("2026-08-15", lunch, FoodItemEntity(name = "Chicken", calories = 300, protein = 25))
        val dinner = service.addMeal("2026-08-15", "Dinner")
        service.upsertFood("2026-08-15", dinner, FoodItemEntity(name = "Fish", calories = 250, protein = 30))
        assertEquals(15, db.playerDao().getProfile(1)!!.totalXp)
        val foods = db.moduleDao().getDietLog("2026-08-15")!!.meals.first { it.name == "Lunch" }.foods
        service.deleteFood("2026-08-15", lunch, foods.first().id)
        assertEquals(15, db.playerDao().getProfile(1)!!.totalXp)
        assertEquals(0, db.xpDao().getAllLedger().count { it.sourceType == "NUTRITION_UNDO" })
    }
}
