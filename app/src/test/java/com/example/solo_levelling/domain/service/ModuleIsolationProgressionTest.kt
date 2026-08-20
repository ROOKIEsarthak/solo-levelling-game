package com.example.solo_levelling.domain.service

import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.FakeAppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.AttributeStatEntity
import com.example.solo_levelling.data.db.entity.LoggedSetEntity
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.PlannedExerciseEntity
import com.example.solo_levelling.data.db.entity.RepRangeEntity
import com.example.solo_levelling.data.db.entity.UserConfigEntity
import com.example.solo_levelling.data.db.entity.WorkoutDayPlanEntity
import com.example.solo_levelling.data.db.entity.WorkoutRoutineEntity
import com.example.solo_levelling.domain.model.AttributeCode
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

@OptIn(ExperimentalCoroutinesApi::class)
class ModuleIsolationProgressionTest {
    private lateinit var db: JsonDatabase
    private lateinit var eventBus: EventBus
    private lateinit var clock: FakeAppClock
    private lateinit var progression: ProgressionService
    private lateinit var modules: ModuleService
    private lateinit var tempDir: java.nio.file.Path
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        tempDir = Files.createTempDirectory("module-iso-")
        db = JsonDatabase(tempDir.toFile())
        eventBus = EventBus()
        clock = FakeAppClock(
            epochMs = LocalDate.of(2026, 8, 15)
                .atStartOfDay(java.time.ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli() + 12L * 60 * 60 * 1000,
            fixedDate = LocalDate.of(2026, 8, 15),
        )
        progression = ProgressionService(db, eventBus, clock)
        val questCompletion = QuestCompletionService(db, eventBus, clock, progression)
        modules = ModuleService(
            db,
            eventBus,
            clock,
            progression,
            QuestVerificationService(db, clock, questCompletion),
        )
    }

    @After
    fun tearDown() {
        db.close()
        tempDir.toFile().deleteRecursively()
        Dispatchers.resetMain()
    }

    private suspend fun seed(modulesFlags: EnabledModules) {
        db.playerDao().upsertProfile(
            PlayerProfileEntity(
                name = "Test",
                timezone = "UTC",
                onboardingDone = true,
                createdAtEpochMs = clock.nowEpochMs(),
            ),
        )
        db.playerDao().upsertAttributes(AttributeCode.entries.map { AttributeStatEntity(it.name) })
        for ((k, v) in ModuleFlags.encode(modulesFlags)) {
            db.configDao().upsert(UserConfigEntity(k, v))
        }
    }

    @Test
    fun p_workoutOnly_rejectsCareerXp() = runTest {
        seed(EnabledModules(career = false, workout = true, diet = false))
        val career = progression.award("DSA", "dsa_1", 25)
        assertEquals(ProgressionService.AwardResult.ModuleDisabled, career)
        val workout = progression.award(
            "WORKOUT",
            "workout_2026-08-15",
            40,
            metadataJson = """{"module":"WORKOUT"}""",
        )
        assertTrue(workout is ProgressionService.AwardResult.Success)
        assertEquals(40, db.playerDao().getProfile(1)!!.totalXp)
    }

    @Test
    fun p_rebuildExcludesDisabledModule() = runTest {
        seed(EnabledModules(true, true, true))
        progression.award("DSA", "dsa_x", 100, metadataJson = """{"module":"CAREER"}""")
        progression.award("WORKOUT", "w_x", 40, metadataJson = """{"module":"WORKOUT"}""")
        assertEquals(140, db.playerDao().getProfile(1)!!.totalXp)

        for ((k, v) in ModuleFlags.encode(EnabledModules(career = false, workout = true, diet = false))) {
            db.configDao().upsert(UserConfigEntity(k, v))
        }
        val rebuilt = progression.rebuildActiveFromLedger(
            EnabledModules(career = false, workout = true, diet = false),
        )
        assertEquals(40, rebuilt.newTotal)
        assertEquals(40, db.playerDao().getProfile(1)!!.totalXp)
        assertEquals(2, db.xpDao().getAllLedger().size)
    }

    @Test
    fun p_reenableCareer_restoresXp() = runTest {
        seed(EnabledModules(true, true, false))
        progression.award("DSA", "dsa_y", 80, metadataJson = """{"module":"CAREER"}""")
        progression.award("WORKOUT", "w_y", 40, metadataJson = """{"module":"WORKOUT"}""")
        progression.rebuildActiveFromLedger(EnabledModules(false, true, false))
        assertEquals(40, db.playerDao().getProfile(1)!!.totalXp)
        progression.rebuildActiveFromLedger(EnabledModules(true, true, false))
        assertEquals(120, db.playerDao().getProfile(1)!!.totalXp)
    }

    @Test
    fun p_earlySplitChange_setsWorkoutScaleOnly() = runTest {
        seed(EnabledModules(true, true, true))
        val map = WorkoutSplitLogic.defaultDayMap(com.example.solo_levelling.data.seed.WorkoutCatalog.findSplit("ppl")!!)
        val csv = WorkoutSplitLogic.encodeDayMap(map)
        assertNull(modules.applyWorkoutSplit("ppl", csv, confirmEarlyChange = false))
        clock.epochMs += 30L * 24 * 60 * 60 * 1000
        clock.fixedDate = LocalDate.of(2026, 9, 14)
        val blocked = modules.applyWorkoutSplit("upper_lower", csv, confirmEarlyChange = false)
        assertEquals(ModuleService.EARLY_SPLIT_CHANGE_REQUIRED, blocked)
        assertNull(modules.applyWorkoutSplit("upper_lower", csv, confirmEarlyChange = true))
        assertEquals(0.75f, modules.workoutProgressionScale(), 0.001f)

        progression.award("DSA", "dsa_z", 50, metadataJson = """{"module":"CAREER"}""")
        val profileAfterCareer = db.playerDao().getProfile(1)!!.totalXp
        assertEquals(50, profileAfterCareer)
    }

    @Test
    fun p_workoutLog_seedsFromSplitAndPersistsSets() = runTest {
        seed(EnabledModules(false, true, false))
        val monday = WorkoutDayPlanEntity(
            enabled = true,
            name = "Push",
            exercises = listOf(
                PlannedExerciseEntity(
                    id = 1,
                    name = "Bench Press",
                    targetMuscle = "Chest",
                    sets = 3,
                    repRange = RepRangeEntity(8, 12),
                ),
            ),
        )
        db.moduleDao().upsertWorkoutRoutine(WorkoutRoutineEntity().withDay("monday", monday))
        // 2026-08-17 is Monday
        clock = FakeAppClock(fixedDate = LocalDate.of(2026, 8, 17))
        val progression2 = ProgressionService(db, eventBus, clock)
        val qc = QuestCompletionService(db, eventBus, clock, progression2)
        val svc = ModuleService(db, eventBus, clock, progression2, QuestVerificationService(db, clock, qc))
        val log = svc.startOrGetWorkoutLog("2026-08-17")
        assertEquals(1, log.exercises.size)
        assertEquals("Bench Press", log.exercises.first().name)
        svc.upsertLoggedExercise(
            "2026-08-17",
            log.exercises.first().copy(sets = listOf(LoggedSetEntity(60f, 10, null))),
        )
        val saved = db.moduleDao().getWorkoutLog("2026-08-17")!!
        assertEquals(1, saved.exercises.first().sets.size)
        assertEquals(60f, saved.exercises.first().sets.first().weight, 0.01f)
    }
}
