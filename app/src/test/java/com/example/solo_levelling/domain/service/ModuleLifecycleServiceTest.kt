package com.example.solo_levelling.domain.service

import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.FakeAppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.AttributeStatEntity
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.QuestInstanceEntity
import com.example.solo_levelling.data.db.entity.QuestTemplateEntity
import com.example.solo_levelling.data.db.entity.UserConfigEntity
import com.example.solo_levelling.data.seed.WorkoutCatalog
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ModuleLifecycleServiceTest {
    private lateinit var db: JsonDatabase
    private lateinit var clock: FakeAppClock
    private lateinit var eventBus: EventBus
    private lateinit var onboarding: OnboardingService
    private lateinit var lifecycle: ModuleLifecycleService
    private lateinit var progression: ProgressionService
    private lateinit var tempDir: java.nio.file.Path
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        tempDir = Files.createTempDirectory("module-life-")
        db = JsonDatabase(tempDir.toFile())
        clock = FakeAppClock(
            epochMs = LocalDate.of(2026, 8, 18)
                .atStartOfDay(java.time.ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli() + 12L * 60 * 60 * 1000,
            fixedDate = LocalDate.of(2026, 8, 18),
        )
        eventBus = EventBus()
        progression = ProgressionService(db, eventBus, clock)
        val generation = QuestGenerationService(db, clock, eventBus)
        onboarding = OnboardingService(db, clock, generation, progression)
        lifecycle = ModuleLifecycleService(db, clock, onboarding)
    }

    @After
    fun tearDown() {
        db.close()
        tempDir.toFile().deleteRecursively()
        Dispatchers.resetMain()
    }

    private suspend fun seed(modules: EnabledModules) {
        db.playerDao().upsertProfile(
            PlayerProfileEntity(
                name = "Test",
                timezone = "UTC",
                onboardingDone = true,
                createdAtEpochMs = clock.nowEpochMs(),
            ),
        )
        db.playerDao().upsertAttributes(AttributeCode.entries.map { AttributeStatEntity(it.name) })
        for ((k, v) in ModuleFlags.encode(modules)) {
            db.configDao().upsert(UserConfigEntity(k, v))
        }
    }

    @Test
    fun n_cannotDisableFinalActiveModule() = runTest {
        seed(EnabledModules(career = false, workout = true, diet = false))
        val result = lifecycle.disable(ModuleFlags.MODULE_WORKOUT)
        assertEquals(ModuleChangeResult.LastModule, result)
        assertTrue(onboarding.currentModules().workout)
    }

    @Test
    fun p_canDisableWhenAnotherRemainsActive() = runTest {
        seed(EnabledModules(career = true, workout = true, diet = false))
        val result = lifecycle.disable(ModuleFlags.MODULE_CAREER)
        assertEquals(ModuleChangeResult.Disabled, result)
        val current = onboarding.currentModules()
        assertFalse(current.career)
        assertTrue(current.workout)
        assertTrue(db.configDao().get(ModuleFlags.disabledAtKey(ModuleFlags.MODULE_CAREER))?.value != null)
    }

    @Test
    fun n_enableWithoutSetup_returnsSetupRequiredAndDoesNotEnable() = runTest {
        seed(EnabledModules(career = false, workout = true, diet = false))
        val result = lifecycle.requestEnable(ModuleFlags.MODULE_DIET)
        assertEquals(ModuleChangeResult.SetupRequired, result)
        assertFalse(onboarding.currentModules().diet)
    }

    @Test
    fun p_enableWithExistingSetup_enablesImmediately() = runTest {
        seed(EnabledModules(career = false, workout = true, diet = false))
        db.configDao().upsert(UserConfigEntity("career_intent", "interviews"))
        val result = lifecycle.requestEnable(ModuleFlags.MODULE_CAREER)
        assertEquals(ModuleChangeResult.Enabled, result)
        assertTrue(onboarding.currentModules().career)
        assertTrue(db.configDao().get(ModuleFlags.enabledAtKey(ModuleFlags.MODULE_CAREER))?.value != null)
    }

    @Test
    fun p_completeSetup_persistsConfigAndEnables() = runTest {
        seed(EnabledModules(career = false, workout = true, diet = false))
        val result = lifecycle.completeSetup(
            ModuleFlags.MODULE_CAREER,
            OnboardingInput(
                name = "Test",
                careerIntent = "interviews",
                currentRole = "SDE1",
                targetRole = "SDE2",
            ),
        )
        assertEquals(ModuleChangeResult.Enabled, result)
        assertTrue(onboarding.currentModules().career)
        assertEquals("interviews", db.configDao().get("career_intent")?.value)
        assertTrue(ModuleFlags.isTrue(db.configDao().get(ModuleFlags.setupCompletedKey(ModuleFlags.MODULE_CAREER))?.value))
    }

    @Test
    fun p_disablePreservesHistoryAndStopsNewXp() = runTest {
        seed(EnabledModules(career = true, workout = true, diet = false))
        progression.award("DSA", "dsa_keep", 80, metadataJson = """{"module":"CAREER"}""")
        progression.award("WORKOUT", "w_keep", 40, metadataJson = """{"module":"WORKOUT"}""")
        lifecycle.disable(ModuleFlags.MODULE_CAREER)
        assertEquals(2, db.xpDao().getAllLedger().size)
        assertEquals(40, db.playerDao().getProfile(1)!!.totalXp)
        val blocked = progression.award("DSA", "dsa_new", 25, metadataJson = """{"module":"CAREER"}""")
        assertEquals(ProgressionService.AwardResult.ModuleDisabled, blocked)
    }

    @Test
    fun p_reenableRestoresActiveXpFromLedger() = runTest {
        seed(EnabledModules(career = true, workout = true, diet = false))
        db.configDao().upsert(UserConfigEntity("career_intent", "interviews"))
        progression.award("DSA", "dsa_z", 80, metadataJson = """{"module":"CAREER"}""")
        progression.award("WORKOUT", "w_z", 40, metadataJson = """{"module":"WORKOUT"}""")
        lifecycle.disable(ModuleFlags.MODULE_CAREER)
        assertEquals(40, db.playerDao().getProfile(1)!!.totalXp)
        lifecycle.requestEnable(ModuleFlags.MODULE_CAREER)
        assertEquals(120, db.playerDao().getProfile(1)!!.totalXp)
        assertEquals(2, db.xpDao().getAllLedger().size)
    }

    @Test
    fun n_writeModuleFlags_rejectsZeroModules() = runTest {
        seed(EnabledModules(career = true, workout = false, diet = false))
        val applied = onboarding.writeModuleFlags(EnabledModules())
        assertFalse(applied)
        assertTrue(onboarding.currentModules().career)
    }

    @Test
    fun e_invalidModule_isRejected() = runTest {
        seed(EnabledModules(workout = true))
        assertEquals(ModuleChangeResult.InvalidModule, lifecycle.disable("focus"))
        assertEquals(ModuleChangeResult.InvalidModule, lifecycle.requestEnable("journal"))
    }

    @Test
    fun e_migrateLifecycle_marksEnabledModulesSetupComplete() = runTest {
        seed(EnabledModules(career = true, workout = false, diet = false))
        db.configDao().upsert(UserConfigEntity("career_intent", "interviews"))
        onboarding.migrateModuleLifecycleIfNeeded()
        assertTrue(ModuleFlags.isTrue(db.configDao().get(ModuleFlags.setupCompletedKey(ModuleFlags.MODULE_CAREER))?.value))
        assertTrue(db.configDao().get(ModuleFlags.enabledAtKey(ModuleFlags.MODULE_CAREER))?.value != null)
        assertEquals(null, db.configDao().get(ModuleFlags.setupCompletedKey(ModuleFlags.MODULE_DIET)))
    }

    @Test
    fun n_migrateLifecycle_doesNotMarkWorkoutWithoutSplit() = runTest {
        seed(EnabledModules(career = false, workout = true, diet = false))
        db.configDao().upsert(UserConfigEntity(ModuleFlags.setupCompletedKey(ModuleFlags.MODULE_WORKOUT), "true"))
        onboarding.migrateModuleLifecycleIfNeeded()
        assertFalse(ModuleFlags.isTrue(db.configDao().get(ModuleFlags.setupCompletedKey(ModuleFlags.MODULE_WORKOUT))?.value))
        assertTrue(lifecycle.needsSetup(ModuleFlags.MODULE_WORKOUT))
    }

    @Test
    fun p_migrateFlags_infersDietOnlyFromBody() = runTest {
        db.playerDao().upsertProfile(
            PlayerProfileEntity(
                name = "Test",
                timezone = "UTC",
                onboardingDone = true,
                createdAtEpochMs = clock.nowEpochMs(),
            ),
        )
        db.configDao().upsert(UserConfigEntity("height_cm", "180"))
        db.configDao().upsert(UserConfigEntity("weight_kg", "75"))
        onboarding.migrateModuleFlagsIfNeeded()
        val current = onboarding.currentModules()
        assertTrue(current.diet)
        assertFalse(current.career)
        assertFalse(current.workout)
    }

    @Test
    fun p_enabledButUninitialized_returnsSetupRequired() = runTest {
        seed(EnabledModules(career = false, workout = true, diet = false))
        val result = lifecycle.requestEnable(ModuleFlags.MODULE_WORKOUT)
        assertEquals(ModuleChangeResult.SetupRequired, result)
        assertTrue(onboarding.currentModules().workout)
    }

    @Test
    fun p_planModuleChanges_dietToFitness_queuesWorkout() = runTest {
        seed(EnabledModules(career = false, workout = false, diet = true))
        db.configDao().upsert(UserConfigEntity("height_cm", "180"))
        db.configDao().upsert(UserConfigEntity("weight_kg", "75"))
        val current = onboarding.currentModules()
        val plan = lifecycle.planModuleChanges(
            current,
            EnabledModules(career = false, workout = true, diet = false),
        )
        assertEquals(listOf(ModuleFlags.MODULE_WORKOUT), plan.setupQueue)
        assertEquals(listOf(ModuleFlags.MODULE_DIET), plan.deferredDisables)
        assertTrue(plan.toDisable.isEmpty())
        assertFalse(plan.blocked)
    }

    @Test
    fun p_planModuleChanges_multiModule_orderedQueue() = runTest {
        seed(EnabledModules(career = false, workout = false, diet = true))
        db.configDao().upsert(UserConfigEntity("height_cm", "180"))
        db.configDao().upsert(UserConfigEntity("weight_kg", "75"))
        val plan = lifecycle.planModuleChanges(
            onboarding.currentModules(),
            EnabledModules(career = true, workout = true, diet = true),
        )
        assertEquals(
            listOf(ModuleFlags.MODULE_WORKOUT, ModuleFlags.MODULE_CAREER),
            plan.setupQueue,
        )
        assertTrue(plan.toDisable.isEmpty())
    }

    @Test
    fun p_planModuleChanges_reenableInitialized_skipsQueue() = runTest {
        seed(EnabledModules(career = true, workout = false, diet = true))
        db.configDao().upsert(UserConfigEntity("career_intent", "interviews"))
        db.configDao().upsert(UserConfigEntity("height_cm", "180"))
        db.configDao().upsert(UserConfigEntity("weight_kg", "75"))
        lifecycle.disable(ModuleFlags.MODULE_CAREER)
        val plan = lifecycle.planModuleChanges(
            onboarding.currentModules(),
            EnabledModules(career = true, workout = false, diet = true),
        )
        assertEquals(listOf(ModuleFlags.MODULE_CAREER), plan.toEnableImmediate)
        assertTrue(plan.setupQueue.isEmpty())
    }

    @Test
    fun e_applyModuleChanges_blocksLastModuleDisable() = runTest {
        seed(EnabledModules(career = false, workout = false, diet = true))
        val result = lifecycle.applyModuleChanges(EnabledModules())
        assertTrue(result.blocked)
        assertTrue(onboarding.currentModules().diet)
    }

    @Test
    fun p_applyThenCompleteSetup_enablesFitnessAndSeedsWeight() = runTest {
        seed(EnabledModules(career = false, workout = false, diet = true))
        db.configDao().upsert(UserConfigEntity("height_cm", "180"))
        db.configDao().upsert(UserConfigEntity("weight_kg", "75"))
        val apply = lifecycle.applyModuleChanges(
            EnabledModules(career = false, workout = true, diet = false),
        )
        assertEquals(listOf(ModuleFlags.MODULE_WORKOUT), apply.setupQueue)
        assertFalse(onboarding.currentModules().workout)
        assertTrue(onboarding.currentModules().diet)
        val split = WorkoutCatalog.findSplit("ppl_ul")!!
        val map = WorkoutSplitLogic.defaultDayMap(split)
        val setup = lifecycle.completeSetup(
            ModuleFlags.MODULE_WORKOUT,
            OnboardingInput(
                name = "Test",
                heightCm = 180.0,
                weightKg = 75.0,
                fitnessGoal = "muscle_gain",
                trainingExperience = "beginner",
                workoutSplitId = "ppl_ul",
                workoutDayMapCsv = WorkoutSplitLogic.encodeDayMap(map),
            ),
        )
        assertEquals(ModuleChangeResult.Enabled, setup)
        assertTrue(onboarding.currentModules().workout)
        assertEquals("muscle_gain", db.configDao().get("fitness_goal")?.value)
        assertTrue(db.configDao().get("workout_split_id")?.value?.isNotBlank() == true)
        lifecycle.applyDeferredDisables(apply.deferredDisables)
        assertFalse(onboarding.currentModules().diet)
        assertTrue(onboarding.currentModules().workout)
        assertEquals(1, db.moduleDao().recentMetrics("WEIGHT", 5).size)
    }

    @Test
    fun n_undoDisabledModuleQuest_doesNotReverseXp() = runTest {
        seed(EnabledModules(career = true, workout = true, diet = false))
        val instanceId = seedCareerQuest(QuestStatus.AVAILABLE.name)
        val completion = QuestCompletionService(db, eventBus, clock, progression)
        assertTrue(completion.complete(instanceId) is QuestCompletionService.Result.Completed)
        lifecycle.disable(ModuleFlags.MODULE_CAREER)
        val xpAfterDisable = db.playerDao().getProfile(1)!!.totalXp
        assertFalse(completion.undo(instanceId, ignoreWindow = true))
        assertEquals(QuestStatus.COMPLETED.name, db.questDao().getInstance(instanceId)!!.status)
        assertEquals(xpAfterDisable, db.playerDao().getProfile(1)!!.totalXp)
        assertTrue(db.xpDao().getAllLedger().none { it.sourceType == "QUEST_UNDO" })
    }

    @Test
    fun p_inferSetupFromExistingDietBody_skipsSetup() = runTest {
        seed(EnabledModules(career = false, workout = true, diet = false))
        db.configDao().upsert(UserConfigEntity("height_cm", "180"))
        db.configDao().upsert(UserConfigEntity("weight_kg", "75"))
        val result = lifecycle.requestEnable(ModuleFlags.MODULE_DIET)
        assertEquals(ModuleChangeResult.Enabled, result)
        assertTrue(onboarding.currentModules().diet)
    }

    @Test
    fun n_disableThenComplete_deletesIncompleteQuest() = runTest {
        seed(EnabledModules(career = true, workout = true, diet = false))
        db.configDao().upsert(UserConfigEntity("career_intent", "interviews"))
        db.configDao().upsert(UserConfigEntity("workout_split_id", "ppl_ul"))
        val instanceId = seedCareerQuest(QuestStatus.AVAILABLE.name)
        lifecycle.disable(ModuleFlags.MODULE_CAREER)
        assertNull(db.questDao().getInstance(instanceId))
        val completion = QuestCompletionService(db, eventBus, clock, progression)
        assertEquals(QuestCompletionService.Result.NotFound, completion.complete(instanceId))
    }

    @Test
    fun n_tryAutoComplete_doesNotUndoDisabledModuleHistory() = runTest {
        seed(EnabledModules(career = true, workout = true, diet = false))
        val instanceId = seedCareerQuest(QuestStatus.AVAILABLE.name, verificationType = "COUNT")
        val completion = QuestCompletionService(db, eventBus, clock, progression)
        assertTrue(completion.complete(instanceId) is QuestCompletionService.Result.Completed)
        lifecycle.disable(ModuleFlags.MODULE_CAREER)
        QuestVerificationService(db, clock, completion).tryAutoComplete("2026-08-18")
        assertEquals(QuestStatus.COMPLETED.name, db.questDao().getInstance(instanceId)!!.status)
        assertEquals(1, db.xpDao().getAllLedger().count { it.amount > 0 })
    }

    @Test
    fun e_lifecycleState_survivesDatabaseReload() = runTest {
        seed(EnabledModules(career = true, workout = true, diet = false))
        db.configDao().upsert(UserConfigEntity(ModuleFlags.setupCompletedKey(ModuleFlags.MODULE_CAREER), "true"))
        db.configDao().upsert(UserConfigEntity(ModuleFlags.setupCompletedKey(ModuleFlags.MODULE_WORKOUT), "true"))
        lifecycle.disable(ModuleFlags.MODULE_CAREER)
        val disabledAt = db.configDao().get(ModuleFlags.disabledAtKey(ModuleFlags.MODULE_CAREER))?.value
        db.close()
        db = JsonDatabase(tempDir.toFile())
        progression = ProgressionService(db, eventBus, clock)
        onboarding = OnboardingService(db, clock, QuestGenerationService(db, clock, eventBus), progression)
        lifecycle = ModuleLifecycleService(db, clock, onboarding)
        val current = onboarding.currentModules()
        assertFalse(current.career)
        assertTrue(current.workout)
        assertEquals(disabledAt, db.configDao().get(ModuleFlags.disabledAtKey(ModuleFlags.MODULE_CAREER))?.value)
        assertTrue(ModuleFlags.isTrue(db.configDao().get(ModuleFlags.setupCompletedKey(ModuleFlags.MODULE_CAREER))?.value))
        assertTrue(ModuleFlags.isTrue(db.configDao().get(ModuleFlags.setupCompletedKey(ModuleFlags.MODULE_WORKOUT))?.value))
    }

    private suspend fun seedCareerQuest(
        status: String,
        verificationType: String = "MANUAL",
    ): Long {
        val templateId = db.questDao().upsertTemplate(
            QuestTemplateEntity(
                key = "dsa_daily",
                type = "DAILY",
                title = "Solve 2 DSA problems",
                baseXp = 40,
                attributeRewardsJson = "{}",
                verificationType = verificationType,
                verificationTarget = 2f,
                priorityTags = "module_career",
            ),
        )
        return db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = templateId,
                scheduledDate = "2026-08-18",
                status = status,
                title = "Solve 2 DSA problems",
                type = "DAILY",
                baseXp = 40,
                attributeRewardsJson = "{}",
                verificationType = verificationType,
                verificationTarget = 2f,
            ),
        )
    }

    @Test
    fun p_eligibleModules_requiresInitialized() = runTest {
        seed(EnabledModules(career = true, workout = true, diet = true))
        db.configDao().upsert(UserConfigEntity("career_intent", "interviews"))
        db.configDao().upsert(UserConfigEntity("workout_split_id", "ppl_ul"))
        db.configDao().upsert(UserConfigEntity("height_cm", "180"))
        db.configDao().upsert(UserConfigEntity("weight_kg", "75"))
        val eligible = eligibleModules(db, onboarding.currentModules())
        assertTrue(eligible.career)
        assertTrue(eligible.workout)
        assertTrue(eligible.diet)
    }

    @Test
    fun n_eligibleModules_enabledWithoutData_isOff() = runTest {
        seed(EnabledModules(career = true, workout = true, diet = true))
        val eligible = eligibleModules(db, onboarding.currentModules())
        assertFalse(eligible.career)
        assertFalse(eligible.workout)
        assertFalse(eligible.diet)
    }
}
