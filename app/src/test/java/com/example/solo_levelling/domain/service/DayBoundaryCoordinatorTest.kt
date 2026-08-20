package com.example.solo_levelling.domain.service

import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.FakeAppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.QuestInstanceEntity
import com.example.solo_levelling.data.db.entity.QuestTemplateEntity
import com.example.solo_levelling.data.db.entity.StreakStateEntity
import com.example.solo_levelling.data.db.entity.UserConfigEntity
import com.example.solo_levelling.domain.model.QuestStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class DayBoundaryCoordinatorTest {
    private lateinit var db: JsonDatabase
    private lateinit var clock: FakeAppClock
    private lateinit var coordinator: DayBoundaryCoordinator
    private lateinit var tempDir: java.nio.file.Path
    private val zone = ZoneId.of("Asia/Kolkata")
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        tempDir = Files.createTempDirectory("day-boundary-")
        db = JsonDatabase(tempDir.toFile())
        clock = FakeAppClock()
        clock.setZoned(ZonedDateTime.of(LocalDate.of(2026, 8, 16), LocalTime.of(10, 0), zone))
        val eventBus = EventBus()
        val dayBoundary = DayBoundaryService(db, eventBus, clock)
        val adaptive = AdaptiveService(db, clock)
        val questGen = QuestGenerationService(db, clock, eventBus, adaptive, CoroutineScope(dispatcher))
        coordinator = DayBoundaryCoordinator(db, clock, dayBoundary, questGen)
    }

    @After
    fun tearDown() {
        db.close()
        tempDir.toFile().deleteRecursively()
        Dispatchers.resetMain()
    }

    private suspend fun seed() {
        db.playerDao().upsertProfile(
            PlayerProfileEntity(
                id = SystemDefaults.PLAYER_ID,
                onboardingDone = true,
                timezone = "Asia/Kolkata",
            ),
        )
        db.playerDao().upsertStreak(
            StreakStateEntity(current = 3, best = 5, lastCompletedDate = "2026-08-14"),
        )
        db.configDao().upsert(UserConfigEntity(ModuleFlags.KEY_CAREER, "true"))
        db.configDao().upsert(UserConfigEntity(ModuleFlags.KEY_WORKOUT, "true"))
        db.configDao().upsert(UserConfigEntity(ModuleFlags.KEY_DIET, "true"))
        db.questDao().upsertTemplates(
            listOf(
                QuestTemplateEntity(
                    key = "daily_test",
                    type = "DAILY",
                    title = "Test",
                    baseXp = 10,
                    attributeRewardsJson = "{}",
                    priorityTags = "module_career",
                ),
            ),
        )
        val template = db.questDao().getTemplateByKey("daily_test")!!
        db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = template.id,
                scheduledDate = "2026-08-15",
                status = QuestStatus.AVAILABLE.name,
                title = "Yesterday",
                type = "DAILY",
                baseXp = 10,
                attributeRewardsJson = "{}",
            ),
        )
    }

    @Test
    fun p_runBoundaryIfNeeded_marksMissedAndPersistsLastDate() = runTest {
        seed()
        val ran = coordinator.runBoundaryIfNeeded("Asia/Kolkata")
        assertTrue(ran)
        assertEquals("2026-08-16", db.configDao().get(DayBoundaryCoordinator.KEY_LAST_DAY_BOUNDARY)?.value)
        val missed = db.questDao().getInstancesForDate("2026-08-15")
        assertTrue(missed.any { it.status == QuestStatus.MISSED.name })
    }

    @Test
    fun p_runBoundaryIfNeeded_idempotentSameDay() = runTest {
        seed()
        assertTrue(coordinator.runBoundaryIfNeeded("Asia/Kolkata"))
        assertFalse(coordinator.runBoundaryIfNeeded("Asia/Kolkata"))
    }

    @Test
    fun e_lateExecution_stillAppliesCorrectDay() = runTest {
        seed()
        clock.setZoned(ZonedDateTime.of(LocalDate.of(2026, 8, 16), LocalTime.of(10, 30), zone))
        assertTrue(coordinator.runBoundaryIfNeeded("Asia/Kolkata"))
        assertEquals("2026-08-16", db.configDao().get(DayBoundaryCoordinator.KEY_LAST_DAY_BOUNDARY)?.value)
    }

    @Test
    fun e_duplicateBoundary_doesNotRemissQuests() = runTest {
        seed()
        coordinator.runBoundaryIfNeeded("Asia/Kolkata")
        val instances = db.questDao().getInstancesForDate("2026-08-15")
        instances.forEach {
            db.questDao().updateInstance(it.copy(status = QuestStatus.AVAILABLE.name))
        }
        assertFalse(coordinator.runBoundaryIfNeeded("Asia/Kolkata"))
        val after = db.questDao().getInstancesForDate("2026-08-15")
        assertTrue(after.all { it.status == QuestStatus.AVAILABLE.name })
    }

    @Test
    fun p_uniqueMidnightWorkName_isStable() {
        assertEquals("day_boundary_midnight", DayBoundaryCoordinator.UNIQUE_MIDNIGHT)
    }

    @Test
    fun n_doesNotSpawnRecoveryQuest() = runTest {
        seed()
        coordinator.runBoundaryIfNeeded("Asia/Kolkata")
        val recovery = db.questDao().getInstancesForDate("2026-08-16")
            .filter { it.type == "RECOVERY" }
        assertTrue(recovery.isEmpty())
    }
}
