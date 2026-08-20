package com.example.solo_levelling.domain.service

import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.FakeAppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.AttributeStatEntity
import com.example.solo_levelling.data.db.entity.DsaProblemEntity
import com.example.solo_levelling.data.db.entity.FoodItemEntity
import com.example.solo_levelling.data.db.entity.LoggedExerciseEntity
import com.example.solo_levelling.data.db.entity.LoggedSetEntity
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.QuestInstanceEntity
import com.example.solo_levelling.data.db.entity.WorkoutLogEntity
import com.example.solo_levelling.domain.model.AttributeCode
import com.example.solo_levelling.domain.model.QuestStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityDatePolicyIntegrationTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var db: JsonDatabase
    private lateinit var eventBus: EventBus
    private lateinit var clock: FakeAppClock
    private lateinit var progression: ProgressionService
    private lateinit var modules: ModuleService
    private lateinit var questCompletion: QuestCompletionService
    private lateinit var verification: QuestVerificationService

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        db = JsonDatabase(Files.createTempDirectory("date-policy-").toFile())
        eventBus = EventBus()
        clock = FakeAppClock(
            epochMs = LocalDate.of(2026, 8, 15).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() + 3_600_000,
            fixedDate = LocalDate.of(2026, 8, 15),
        )
        progression = ProgressionService(db, eventBus, clock)
        questCompletion = QuestCompletionService(db, eventBus, clock, progression)
        verification = QuestVerificationService(db, clock, questCompletion)
        modules = ModuleService(db, eventBus, clock, progression, verification)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
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
    fun p_upsertWorkoutLog_today_awardsXp() = runTest {
        seedProfile()
        modules.upsertWorkoutLog(
            WorkoutLogEntity(
                date = "2026-08-15",
                workoutName = "Push",
                exercises = listOf(
                    LoggedExerciseEntity(name = "Bench", sets = listOf(LoggedSetEntity(60f, 8))),
                ),
            ),
        )
        assertEquals(40, db.playerDao().getProfile(1)!!.totalXp)
    }

    @Test
    fun p_upsertWorkoutLog_today_editDoesNotDoubleXp() = runTest {
        seedProfile()
        val first = WorkoutLogEntity(
            date = "2026-08-15",
            workoutName = "Push",
            exercises = listOf(
                LoggedExerciseEntity(name = "Bench", sets = listOf(LoggedSetEntity(60f, 8))),
            ),
        )
        modules.upsertWorkoutLog(first)
        modules.upsertWorkoutLog(
            first.copy(
                exercises = listOf(
                    LoggedExerciseEntity(name = "Bench", sets = listOf(LoggedSetEntity(70f, 6))),
                ),
            ),
        )
        assertEquals(40, db.playerDao().getProfile(1)!!.totalXp)
        assertEquals(70f, db.moduleDao().getWorkoutLog("2026-08-15")!!.exercises.first().sets.first().weight)
    }

    @Test
    fun n_upsertWorkoutLog_future_doesNotPersistOrAward() = runTest {
        seedProfile()
        modules.upsertWorkoutLog(
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
    fun n_upsertWorkoutLog_past_doesNotPersistOrAward() = runTest {
        seedProfile()
        modules.upsertWorkoutLog(
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
    fun n_addMeal_future_doesNotPersist() = runTest {
        seedProfile()
        val id = modules.addMeal("2026-08-17", "Breakfast")
        assertEquals(0L, id)
        assertNull(db.moduleDao().getDietLog("2026-08-17"))
    }

    @Test
    fun n_addMeal_past_doesNotPersist() = runTest {
        seedProfile()
        val id = modules.addMeal("2026-08-14", "Breakfast")
        assertEquals(0L, id)
        assertNull(db.moduleDao().getDietLog("2026-08-14"))
    }

    @Test
    fun n_startOrGetWorkoutLog_past_doesNotCreateFile() = runTest {
        seedProfile()
        val draft = modules.startOrGetWorkoutLog("2026-08-14")
        assertEquals("2026-08-14", draft.date)
        assertNull(db.moduleDao().getWorkoutLog("2026-08-14"))
    }

    @Test
    fun n_deleteWorkoutLog_past_leavesExistingRecord() = runTest {
        seedProfile()
        db.moduleDao().upsertWorkoutLog(WorkoutLogEntity(date = "2026-08-14", workoutName = "Old"))
        modules.deleteWorkoutLog("2026-08-14")
        assertEquals("Old", db.moduleDao().getWorkoutLog("2026-08-14")!!.workoutName)
    }

    @Test
    fun p_addMeals_today_awardsNutritionXpWhenTrackingComplete() = runTest {
        seedProfile()
        for (name in listOf("Breakfast", "Lunch", "Dinner")) {
            val mealId = modules.addMeal("2026-08-15", name)
            modules.upsertFood(
                "2026-08-15",
                mealId,
                FoodItemEntity(name = "Oats", calories = 200, protein = 8),
            )
        }
        assertTrue(db.playerDao().getProfile(1)!!.totalXp >= 15)
    }

    @Test
    fun n_complete_yesterdayDaily_returnsWrongDay() = runTest {
        seedProfile()
        val id = db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = 1,
                scheduledDate = "2026-08-14",
                status = QuestStatus.AVAILABLE.name,
                title = "Yesterday workout",
                type = "DAILY",
                baseXp = 40,
                attributeRewardsJson = "{}",
            ),
        )
        assertEquals(QuestCompletionService.Result.WrongDay, questCompletion.complete(id))
        assertEquals(QuestStatus.AVAILABLE.name, db.questDao().getInstance(id)!!.status)
        assertEquals(0, db.playerDao().getProfile(1)!!.totalXp)
    }

    @Test
    fun n_complete_tomorrowDaily_returnsWrongDay() = runTest {
        seedProfile()
        val id = db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = 1,
                scheduledDate = "2026-08-16",
                status = QuestStatus.AVAILABLE.name,
                title = "Tomorrow workout",
                type = "DAILY",
                baseXp = 40,
                attributeRewardsJson = "{}",
            ),
        )
        assertEquals(QuestCompletionService.Result.WrongDay, questCompletion.complete(id))
        assertEquals(0, db.playerDao().getProfile(1)!!.totalXp)
    }

    @Test
    fun p_complete_weeklyQuest_allowedDuringSameWeek() = runTest {
        seedProfile()
        val id = db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = 1,
                scheduledDate = "2026-08-16",
                status = QuestStatus.AVAILABLE.name,
                title = "Weekly review",
                type = "WEEKLY",
                baseXp = 80,
                attributeRewardsJson = "{}",
            ),
        )
        val result = questCompletion.complete(id)
        assertTrue(result is QuestCompletionService.Result.Completed)
        assertEquals(80, (result as QuestCompletionService.Result.Completed).xp)
    }

    @Test
    fun p_complete_todayDaily_succeeds() = runTest {
        seedProfile()
        val id = db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = 1,
                scheduledDate = "2026-08-15",
                status = QuestStatus.AVAILABLE.name,
                title = "Today workout",
                type = "DAILY",
                baseXp = 40,
                attributeRewardsJson = "{}",
            ),
        )
        val result = questCompletion.complete(id)
        assertTrue(result is QuestCompletionService.Result.Completed)
        assertEquals(QuestStatus.COMPLETED.name, db.questDao().getInstance(id)!!.status)
        assertEquals(40, (result as QuestCompletionService.Result.Completed).xp)
    }

    @Test
    fun n_tryAutoComplete_pastDate_doesNotComplete() = runTest {
        seedProfile()
        val id = db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = 1,
                scheduledDate = "2026-08-14",
                status = QuestStatus.AVAILABLE.name,
                title = "Workout",
                type = "DAILY",
                baseXp = 40,
                attributeRewardsJson = "{}",
            ),
        )
        verification.tryAutoComplete("2026-08-14")
        assertEquals(QuestStatus.AVAILABLE.name, db.questDao().getInstance(id)!!.status)
    }

    @Test
    fun n_tryAutoComplete_futureDate_doesNotComplete() = runTest {
        seedProfile()
        val id = db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = 1,
                scheduledDate = "2026-08-16",
                status = QuestStatus.AVAILABLE.name,
                title = "Tomorrow workout",
                type = "DAILY",
                baseXp = 40,
                attributeRewardsJson = "{}",
            ),
        )
        verification.tryAutoComplete("2026-08-16")
        assertEquals(QuestStatus.AVAILABLE.name, db.questDao().getInstance(id)!!.status)
    }

    @Test
    fun p_countWorkoutsInRange_onlyCompletedLogs() = runTest {
        db.moduleDao().upsertWorkoutLog(WorkoutLogEntity(date = "2026-08-14", workoutName = "Empty"))
        db.moduleDao().upsertWorkoutLog(
            WorkoutLogEntity(
                date = "2026-08-15",
                workoutName = "Push",
                exercises = listOf(
                    LoggedExerciseEntity(name = "Bench", sets = listOf(LoggedSetEntity(60f, 8))),
                ),
            ),
        )
        assertEquals(1, db.moduleDao().countWorkoutsInRange("2026-08-14", "2026-08-15"))
        assertEquals(1, db.moduleDao().countWorkoutDaysInRange("2026-08-14", "2026-08-15"))
    }

    @Test
    fun n_countDsaSolvedOnDate_ignoresNullTimestamp() = runTest {
        db.moduleDao().upsertDsa(
            DsaProblemEntity(title = "Legacy", status = "SOLVED", solvedAtEpochMs = null),
        )
        assertEquals(0, db.moduleDao().countDsaSolvedOnDate(1_000L, 2_000L))
    }

    @Test
    fun p_countDsaSolvedOnDate_countsTimestampInWindow() = runTest {
        db.moduleDao().upsertDsa(
            DsaProblemEntity(title = "Two Sum", status = "SOLVED", solvedAtEpochMs = 1_500L),
        )
        assertEquals(1, db.moduleDao().countDsaSolvedOnDate(1_000L, 2_000L))
        assertEquals(0, db.moduleDao().countDsaSolvedOnDate(2_000L, 3_000L))
    }
}
