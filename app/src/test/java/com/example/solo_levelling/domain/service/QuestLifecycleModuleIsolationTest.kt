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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        if (modules.career) {
            db.configDao().upsert(UserConfigEntity("career_intent", "interviews"))
        }
        if (modules.workout) {
            db.configDao().upsert(UserConfigEntity("workout_split_id", "ppl_ul"))
        }
        if (modules.diet) {
            db.configDao().upsert(UserConfigEntity("height_cm", "180"))
            db.configDao().upsert(UserConfigEntity("weight_kg", "75"))
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
                QuestTemplateEntity(
                    key = "deep_work",
                    type = "DAILY",
                    title = "90 min deep work",
                    baseXp = 45,
                    attributeRewardsJson = "{}",
                    scheduleDaysCsv = "1,2,3,4,5,6,7",
                    priorityTags = "",
                ),
                QuestTemplateEntity(
                    key = "steps",
                    type = "DAILY",
                    title = "Hit step target",
                    baseXp = 25,
                    attributeRewardsJson = "{}",
                    scheduleDaysCsv = "1,2,3,4,5,6,7",
                    priorityTags = "module_workout",
                ),
                QuestTemplateEntity(
                    key = "nutrition_daily",
                    type = "DAILY",
                    title = "Complete meal tracking",
                    baseXp = 15,
                    attributeRewardsJson = "{}",
                    scheduleDaysCsv = "1,2,3,4,5,6,7",
                    priorityTags = "module_diet",
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
    fun r_generateForToday_deletesStaleCareerInstancesWhenCareerOff() = runTest {
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
        assertTrue(all.none { it.title.contains("DSA") })
        val ready = received.filterIsInstance<DomainEvent.DailyQuestsReady>().single()
        assertEquals(all.size, ready.count)
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

    @Test
    fun p_weeklyReview_ignoresDisabledModuleStaleQuests() = runTest {
        seedModules(EnabledModules(career = false, workout = true, diet = false))
        val verification = QuestVerificationService(
            db,
            clock,
            QuestCompletionService(db, eventBus, clock, ProgressionService(db, eventBus, clock)),
        )
        val reviewTemplate = db.questDao().upsertTemplate(
            QuestTemplateEntity(
                key = "weekly_review",
                type = "WEEKLY",
                title = "Weekly review",
                baseXp = 50,
                attributeRewardsJson = "{}",
                verificationType = "AUTOMATIC",
            ),
        )
        val reviewId = db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = reviewTemplate,
                scheduledDate = "2026-08-16",
                status = QuestStatus.AVAILABLE.name,
                title = "Weekly review",
                type = "WEEKLY",
                baseXp = 50,
                attributeRewardsJson = "{}",
                verificationType = "AUTOMATIC",
            ),
        )
        val journal = db.questDao().getTemplateByKey("journal")!!
        db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = journal.id,
                scheduledDate = "2026-08-15",
                status = QuestStatus.COMPLETED.name,
                title = journal.title,
                type = "DAILY",
                baseXp = 20,
                attributeRewardsJson = "{}",
            ),
        )
        val dsa = db.questDao().getTemplateByKey("dsa_daily")!!
        db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = dsa.id,
                scheduledDate = "2026-08-15",
                status = QuestStatus.AVAILABLE.name,
                title = dsa.title,
                type = "DAILY",
                baseXp = 40,
                attributeRewardsJson = "{}",
            ),
        )
        val review = db.questDao().getInstance(reviewId)!!
        assertTrue(verification.isSatisfied(review, "2026-08-16"))
    }

    @Test
    fun n_weeklyReview_blockedByIncompleteActiveModuleQuest() = runTest {
        seedModules(EnabledModules(career = true, workout = false, diet = false))
        val verification = QuestVerificationService(
            db,
            clock,
            QuestCompletionService(db, eventBus, clock, ProgressionService(db, eventBus, clock)),
        )
        val reviewTemplate = db.questDao().upsertTemplate(
            QuestTemplateEntity(
                key = "weekly_review",
                type = "WEEKLY",
                title = "Weekly review",
                baseXp = 50,
                attributeRewardsJson = "{}",
                verificationType = "AUTOMATIC",
            ),
        )
        val reviewId = db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = reviewTemplate,
                scheduledDate = "2026-08-16",
                status = QuestStatus.AVAILABLE.name,
                title = "Weekly review",
                type = "WEEKLY",
                baseXp = 50,
                attributeRewardsJson = "{}",
                verificationType = "AUTOMATIC",
            ),
        )
        val dsa = db.questDao().getTemplateByKey("dsa_daily")!!
        db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = dsa.id,
                scheduledDate = "2026-08-15",
                status = QuestStatus.AVAILABLE.name,
                title = dsa.title,
                type = "DAILY",
                baseXp = 40,
                attributeRewardsJson = "{}",
            ),
        )
        val review = db.questDao().getInstance(reviewId)!!
        assertFalse(verification.isSatisfied(review, "2026-08-16"))
    }

    @Test
    fun p_completeSetupWorkout_generatesFitnessQuestsWithoutCareer() = runTest {
        seedModules(EnabledModules(career = false, workout = false, diet = true))
        val onboarding = OnboardingService(db, clock, generation, ProgressionService(db, eventBus, clock))
        val lifecycle = ModuleLifecycleService(db, clock, onboarding)
        val split = com.example.solo_levelling.data.seed.WorkoutCatalog.findSplit("ppl_ul")!!
        val map = WorkoutSplitLogic.defaultDayMap(split)
        lifecycle.completeSetup(
            ModuleFlags.MODULE_WORKOUT,
            OnboardingInput(
                name = "Test",
                heightCm = 180.0,
                weightKg = 75.0,
                fitnessGoal = "muscle_gain",
                workoutSplitId = "ppl_ul",
                workoutDayMapCsv = WorkoutSplitLogic.encodeDayMap(map),
            ),
        )
        val titles = db.questDao().getInstancesForDate("2026-08-18").map { it.title }
        assertTrue(titles.any { it.contains("workout", ignoreCase = true) })
        assertTrue(titles.none { it.contains("DSA") })
    }

    @Test
    fun p_careerOnly_generatesCareerAndGlobal_notFitnessOrNutrition() = runTest {
        seedModules(EnabledModules(career = true, workout = false, diet = false))
        generation.generateForToday("UTC")
        val titles = db.questDao().getInstancesForDate("2026-08-18").map { it.title }
        assertTrue(titles.any { it.contains("DSA") })
        assertTrue(titles.contains("90 min deep work"))
        assertTrue(titles.contains("Write a short journal"))
        assertTrue(titles.none { it.contains("workout", ignoreCase = true) })
        assertTrue(titles.none { it.contains("step", ignoreCase = true) })
        assertTrue(titles.none { it.contains("meal", ignoreCase = true) })
    }

    @Test
    fun p_careerAndFitness_omitsNutrition() = runTest {
        seedModules(EnabledModules(career = true, workout = true, diet = false))
        generation.generateForToday("UTC")
        val titles = db.questDao().getInstancesForDate("2026-08-18").map { it.title }
        assertTrue(titles.any { it.contains("DSA") })
        assertTrue(titles.any { it.contains("workout", ignoreCase = true) })
        assertTrue(titles.any { it.contains("step", ignoreCase = true) })
        assertTrue(titles.none { it.contains("meal", ignoreCase = true) })
    }

    @Test
    fun n_careerEnabledWithoutIntent_doesNotGenerateDsa() = runTest {
        seedModules(EnabledModules(career = true, workout = false, diet = false))
        db.configDao().upsert(UserConfigEntity("career_intent", ""))
        generation.generateForToday("UTC")
        val titles = db.questDao().getInstancesForDate("2026-08-18").map { it.title }
        assertTrue(titles.none { it.contains("DSA") })
        assertTrue(titles.contains("90 min deep work"))
    }

    @Test
    fun p_completeCareerSetup_generatesDsa() = runTest {
        seedModules(EnabledModules(career = false, workout = true, diet = false))
        db.configDao().upsert(UserConfigEntity("career_intent", ""))
        val onboarding = OnboardingService(db, clock, generation, ProgressionService(db, eventBus, clock))
        val lifecycle = ModuleLifecycleService(db, clock, onboarding)
        lifecycle.completeSetup(
            ModuleFlags.MODULE_CAREER,
            OnboardingInput(name = "Test", careerIntent = "interviews"),
        )
        assertEquals("interviews", db.configDao().get("career_intent")?.value)
        val titles = db.questDao().getInstancesForDate("2026-08-18").map { it.title }
        assertTrue(titles.any { it.contains("DSA") })
    }

    @Test
    fun r_disableFitness_deletesIncompleteKeepsCompleted() = runTest {
        seedModules(EnabledModules(career = true, workout = true, diet = false))
        generation.generateForToday("UTC")
        val workout = db.questDao().getInstancesForDate("2026-08-18")
            .first { it.title.contains("workout", ignoreCase = true) }
        val steps = db.questDao().getInstancesForDate("2026-08-18")
            .first { it.title.contains("step", ignoreCase = true) }
        db.questDao().updateInstance(workout.copy(status = QuestStatus.COMPLETED.name))
        val onboarding = OnboardingService(db, clock, generation, ProgressionService(db, eventBus, clock))
        ModuleLifecycleService(db, clock, onboarding).disable(ModuleFlags.MODULE_WORKOUT)
        assertEquals(
            QuestStatus.COMPLETED.name,
            db.questDao().getInstance(workout.id)!!.status,
        )
        assertNull(db.questDao().getInstance(steps.id))
        val titles = db.questDao().getInstancesForDate("2026-08-18").map { it.title }
        assertTrue(titles.none { it.contains("step", ignoreCase = true) })
        assertTrue(titles.any { it.contains("DSA") })
    }

    @Test
    fun p_reenableFitness_generatesWorkoutAgain() = runTest {
        seedModules(EnabledModules(career = true, workout = true, diet = false))
        generation.generateForToday("UTC")
        val onboarding = OnboardingService(db, clock, generation, ProgressionService(db, eventBus, clock))
        val lifecycle = ModuleLifecycleService(db, clock, onboarding)
        lifecycle.disable(ModuleFlags.MODULE_WORKOUT)
        assertTrue(
            db.questDao().getInstancesForDate("2026-08-18")
                .none { it.title.contains("workout", ignoreCase = true) && it.status != QuestStatus.COMPLETED.name },
        )
        lifecycle.requestEnable(ModuleFlags.MODULE_WORKOUT)
        val titles = db.questDao().getInstancesForDate("2026-08-18").map { it.title }
        assertTrue(titles.any { it.contains("workout", ignoreCase = true) })
        assertTrue(titles.any { it.contains("step", ignoreCase = true) })
    }
}
