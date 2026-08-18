package com.example.solo_levelling.domain.service

import com.example.solo_levelling.core.event.DomainEvent
import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.FakeAppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.QuestInstanceEntity
import com.example.solo_levelling.data.db.entity.QuestTemplateEntity
import com.example.solo_levelling.data.db.entity.UserConfigEntity
import com.example.solo_levelling.domain.model.QuestStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class QuestLifecycleModuleIsolationTest {
    private lateinit var db: JsonDatabase
    private lateinit var clock: FakeAppClock
    private lateinit var eventBus: EventBus
    private lateinit var generation: QuestGenerationService
    private lateinit var dayBoundary: DayBoundaryService
    private lateinit var tempDir: java.nio.file.Path
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        tempDir = Files.createTempDirectory("quest-iso-")
        db = JsonDatabase(tempDir.toFile())
        clock = FakeAppClock(
            epochMs = LocalDate.of(2026, 8, 18)
                .atStartOfDay(java.time.ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli() + 12L * 60 * 60 * 1000,
            fixedDate = LocalDate.of(2026, 8, 18),
        )
        eventBus = EventBus()
        generation = QuestGenerationService(db, clock, eventBus)
        dayBoundary = DayBoundaryService(db, eventBus, clock)
    }

    @After
    fun tearDown() {
        db.close()
        tempDir.toFile().deleteRecursively()
        Dispatchers.resetMain()
    }

    private suspend fun seedModules(modules: EnabledModules) {
        db.playerDao().upsertProfile(
            PlayerProfileEntity(
                name = "Test",
                timezone = "UTC",
                onboardingDone = true,
                createdAtEpochMs = clock.nowEpochMs(),
            ),
        )
        for ((k, v) in ModuleFlags.encode(modules)) {
            db.configDao().upsert(UserConfigEntity(k, v))
        }
        db.questDao().upsertTemplates(
            listOf(
                QuestTemplateEntity(
                    key = "dsa_daily",
                    type = "DAILY",
                    title = "Solve 2 DSA problems",
                    baseXp = 40,
                    attributeRewardsJson = "{}",
                    scheduleDaysCsv = "1,2,3,4,5,6,7",
                    priorityTags = "module_career",
                ),
                QuestTemplateEntity(
                    key = "workout_daily",
                    type = "DAILY",
                    title = "Complete workout",
                    baseXp = 50,
                    attributeRewardsJson = "{}",
                    scheduleDaysCsv = "1,2,3,4,5,6,7",
                    priorityTags = "module_workout",
                ),
                QuestTemplateEntity(
                    key = "journal",
                    type = "DAILY",
                    title = "Write a short journal",
                    baseXp = 20,
                    attributeRewardsJson = "{}",
                    scheduleDaysCsv = "1,2,3,4,5,6,7",
                    priorityTags = "",
                ),
            ),
        )
    }

    @Test
    fun r_generateForToday_omitsDsaWhenCareerOff() = runTest {
        seedModules(EnabledModules(career = false, workout = true, diet = true))
        val received = mutableListOf<DomainEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            eventBus.events.collect { received += it }
        }
        generation.generateForToday("UTC")
        val titles = db.questDao().getInstancesForDate("2026-08-18").map { it.title }
        assertTrue(titles.none { it.contains("DSA") })
        assertTrue(titles.contains("Complete workout"))
        assertTrue(titles.contains("Write a short journal"))
        val ready = received.filterIsInstance<DomainEvent.DailyQuestsReady>().single()
        assertEquals(titles.size, ready.count)
    }

    @Test
    fun r_dailyQuestsReady_countIgnoresStaleCareerInstances() = runTest {
        seedModules(EnabledModules(career = false, workout = true, diet = true))
        val dsa = db.questDao().getActiveTemplates().first { it.key == "dsa_daily" }
        db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = dsa.id,
                scheduledDate = "2026-08-18",
                title = dsa.title,
                type = "DAILY",
                baseXp = dsa.baseXp,
                attributeRewardsJson = "{}",
            ),
        )
        val received = mutableListOf<DomainEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            eventBus.events.collect { received += it }
        }
        generation.generateForToday("UTC")
        val all = db.questDao().getInstancesForDate("2026-08-18")
        assertTrue(all.any { it.title.contains("DSA") })
        val ready = received.filterIsInstance<DomainEvent.DailyQuestsReady>().single()
        assertEquals(all.size - 1, ready.count)
    }

    @Test
    fun n_markMissed_skipsDisabledModuleQuests() = runTest {
        seedModules(EnabledModules(career = false, workout = true, diet = true))
        val templates = db.questDao().getActiveTemplates().associateBy { it.key }
        db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = templates.getValue("dsa_daily").id,
                scheduledDate = "2026-08-17",
                title = "Solve 2 DSA problems",
                type = "DAILY",
                baseXp = 40,
                attributeRewardsJson = "{}",
            ),
        )
        db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = templates.getValue("workout_daily").id,
                scheduledDate = "2026-08-17",
                title = "Complete workout",
                type = "DAILY",
                baseXp = 50,
                attributeRewardsJson = "{}",
            ),
        )
        dayBoundary.markMissedQuestsForYesterday(LocalDate.of(2026, 8, 17))
        val yesterday = db.questDao().getInstancesForDate("2026-08-17")
        val dsa = yesterday.first { it.title.contains("DSA") }
        val workout = yesterday.first { it.title.contains("workout") }
        assertEquals(QuestStatus.AVAILABLE.name, dsa.status)
        assertEquals(QuestStatus.MISSED.name, workout.status)
    }

    @Test
    fun p_generateForToday_includesDsaWhenCareerOn() = runTest {
        seedModules(EnabledModules(career = true, workout = false, diet = false))
        generation.generateForToday("UTC")
        val titles = db.questDao().getInstancesForDate("2026-08-18").map { it.title }
        assertTrue(titles.any { it.contains("DSA") })
        assertTrue(titles.none { it.contains("workout") })
        assertTrue(titles.contains("Write a short journal"))
    }
}
