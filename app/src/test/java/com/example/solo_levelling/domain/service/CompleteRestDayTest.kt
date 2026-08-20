package com.example.solo_levelling.domain.service

import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.FakeAppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.AttributeStatEntity
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.UserConfigEntity
import com.example.solo_levelling.data.db.entity.WorkoutDayPlanEntity
import com.example.solo_levelling.data.db.entity.WorkoutRestKind
import com.example.solo_levelling.data.db.entity.WorkoutRoutineEntity
import com.example.solo_levelling.data.seed.WorkoutCatalog
import com.example.solo_levelling.domain.model.AttributeCode
import com.example.solo_levelling.domain.service.ProgressionService.AwardResult
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
import java.io.File
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CompleteRestDayTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var db: JsonDatabase
    private lateinit var eventBus: EventBus
    private lateinit var clock: FakeAppClock
    private lateinit var progression: ProgressionService
    private lateinit var service: ModuleService
    private lateinit var dbDir: File

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        dbDir = File.createTempFile("test-db-", null).also {
            it.delete()
            it.mkdirs()
        }
        db = JsonDatabase(dbDir)
        eventBus = EventBus()
        clock = FakeAppClock(
            epochMs = LocalDate.of(2026, 8, 16)
                .atStartOfDay(java.time.ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli() + 12L * 60 * 60 * 1000,
            fixedDate = LocalDate.of(2026, 8, 16),
        )
        progression = ProgressionService(db, eventBus, clock)
        val questCompletion = QuestCompletionService(db, eventBus, clock, progression)
        service = ModuleService(
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
        dbDir.deleteRecursively()
        Dispatchers.resetMain()
    }

    private suspend fun seed(modulesFlags: EnabledModules? = null) {
        db.playerDao().upsertProfile(
            PlayerProfileEntity(
                name = "Test",
                timezone = "UTC",
                onboardingDone = true,
                createdAtEpochMs = clock.nowEpochMs(),
            ),
        )
        db.playerDao().upsertAttributes(AttributeCode.entries.map { AttributeStatEntity(it.name) })
        if (modulesFlags != null) {
            for ((k, v) in ModuleFlags.encode(modulesFlags)) {
                db.configDao().upsert(UserConfigEntity(k, v))
            }
        }
        db.moduleDao().upsertWorkoutRoutine(
            WorkoutRoutineEntity(
                sunday = WorkoutDayPlanEntity(enabled = false, name = "Rest"),
            ),
        )
    }

    @Test
    fun p_activeRest_persistsAndAwardsHalfXp() = runTest {
        seed()
        val log = service.completeRestDay("2026-08-16", activeRest = true)
        assertEquals(WorkoutRestKind.ACTIVE_REST, log.restKind)
        assertEquals("Rest", log.workoutName)
        assertTrue(log.isTrainingDayComplete())
        assertEquals(20, db.playerDao().getProfile(1)!!.totalXp)
        assertTrue(
            progression.award("WORKOUT", "workout_2026-08-16", 20) is AwardResult.AlreadyAwarded,
        )
    }

    @Test
    fun p_completeRest_persistsWithoutXp() = runTest {
        seed()
        val log = service.completeRestDay("2026-08-16", activeRest = false)
        assertEquals(WorkoutRestKind.COMPLETE_REST, log.restKind)
        assertTrue(log.isTrainingDayComplete())
        assertEquals(0, db.playerDao().getProfile(1)!!.totalXp)
        assertTrue(
            progression.award("WORKOUT", "workout_2026-08-16", 20) is AwardResult.Success,
        )
    }

    @Test
    fun n_doubleComplete_idempotent() = runTest {
        seed()
        service.completeRestDay("2026-08-16", activeRest = true)
        service.completeRestDay("2026-08-16", activeRest = true)
        service.completeRestDay("2026-08-16", activeRest = false)
        assertEquals(20, db.playerDao().getProfile(1)!!.totalXp)
        assertEquals(
            WorkoutRestKind.ACTIVE_REST,
            db.moduleDao().getWorkoutLog("2026-08-16")!!.restKind,
        )
        assertEquals(1, db.xpDao().getAllLedger().count { it.sourceType == "WORKOUT" })
    }

    @Test
    fun n_activeRest_workoutModuleDisabled_noXp() = runTest {
        seed(EnabledModules(career = false, workout = false, diet = false))
        val log = service.completeRestDay("2026-08-16", activeRest = true)
        assertEquals(WorkoutRestKind.ACTIVE_REST, log.restKind)
        assertEquals(0, db.playerDao().getProfile(1)!!.totalXp)
    }

    @Test
    fun e_activeRest_appliesEarlySplitScale() = runTest {
        seed()
        db.configDao().upsert(
            UserConfigEntity(WorkoutSplitChangeLogic.KEY_APPLIED_AT, clock.nowEpochMs().toString()),
        )
        db.configDao().upsert(
            UserConfigEntity(WorkoutSplitChangeLogic.KEY_SCALE, "0.75"),
        )
        service.completeRestDay("2026-08-16", activeRest = true)
        assertEquals(15, db.playerDao().getProfile(1)!!.totalXp)
    }

    @Test
    fun p_startOrGetRestDay_namesRest() = runTest {
        seed()
        val started = service.startOrGetWorkoutLog("2026-08-16")
        assertEquals("Rest", started.workoutName)
        assertTrue(started.exercises.isEmpty())
    }

    @Test
    fun p_applySplit_reseedsEmptyTodayLog() = runTest {
        seed()
        service.startOrGetWorkoutLog("2026-08-16")
        assertTrue(db.moduleDao().getWorkoutLog("2026-08-16") != null)

        val split = WorkoutCatalog.findSplit("ppl")!!
        val csv = WorkoutSplitLogic.encodeDayMap(WorkoutSplitLogic.defaultDayMap(split))
        assertNull(service.applyWorkoutSplit("ppl", csv, confirmEarlyChange = true))
        assertNull(db.moduleDao().getWorkoutLog("2026-08-16"))

        val reseeded = service.startOrGetWorkoutLog("2026-08-16")
        assertEquals("Rest", reseeded.workoutName)
    }

    @Test
    fun n_completeRestDay_pastDate_doesNotPersist() = runTest {
        seed()
        val log = service.completeRestDay("2026-08-15", activeRest = true)
        assertEquals("2026-08-15", log.date)
        assertNull(db.moduleDao().getWorkoutLog("2026-08-15"))
        assertEquals(0, db.playerDao().getProfile(1)!!.totalXp)
    }
}
