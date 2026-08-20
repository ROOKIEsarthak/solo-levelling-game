package com.example.solo_levelling.ui.settings

import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.FakeAppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.UserConfigEntity
import com.example.solo_levelling.domain.service.AdaptiveService
import com.example.solo_levelling.domain.service.EnabledModules
import com.example.solo_levelling.domain.service.ModuleFlags
import com.example.solo_levelling.domain.service.ModuleLifecycleService
import com.example.solo_levelling.domain.service.OnboardingInput
import com.example.solo_levelling.domain.service.OnboardingService
import com.example.solo_levelling.domain.service.ProgressionService
import com.example.solo_levelling.domain.service.QuestGenerationService
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

@OptIn(ExperimentalCoroutinesApi::class)
class ModuleSetupViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var db: JsonDatabase
    private lateinit var vm: ModuleSetupViewModel
    private lateinit var tempDir: java.nio.file.Path

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        tempDir = Files.createTempDirectory("module-setup-vm-")
        db = JsonDatabase(tempDir.toFile())
        val clock = FakeAppClock(fixedDate = LocalDate.of(2026, 8, 15))
        val eventBus = EventBus()
        val progression = ProgressionService(db, eventBus, clock)
        val questGeneration = QuestGenerationService(db, clock, eventBus, AdaptiveService(db, clock))
        val onboarding = OnboardingService(db, clock, questGeneration, progression)
        val lifecycle = ModuleLifecycleService(db, clock, onboarding)
        vm = ModuleSetupViewModel(db, lifecycle)
    }

    @After
    fun tearDown() {
        db.close()
        tempDir.toFile().deleteRecursively()
        Dispatchers.resetMain()
    }

    @Test
    fun p_loadInitialState_readsStoredBodyMetrics() = runTest(dispatcher) {
        db.configDao().upsert(UserConfigEntity("height_cm", "180"))
        db.configDao().upsert(UserConfigEntity("weight_kg", "75"))
        db.configDao().upsert(UserConfigEntity("career_intent", "interviews"))
        val state = vm.loadInitialState()
        assertEquals("180", state.heightCm)
        assertEquals("75", state.weightKg)
        assertEquals("interviews", state.careerIntent)
        assertFalse(state.requireBody)
    }

    @Test
    fun n_loadInitialState_requiresBodyWhenMissingMetrics() = runTest(dispatcher) {
        val state = vm.loadInitialState()
        assertTrue(state.requireBody)
    }

    @Test
    fun p_completeSetup_career_persistsIntent() = runTest(dispatcher) {
        db.playerDao().upsertProfile(
            PlayerProfileEntity(
                id = SystemDefaults.PLAYER_ID,
                name = "Test",
                timezone = "UTC",
                onboardingDone = true,
            ),
        )
        val input = OnboardingInput(
            name = "",
            modules = EnabledModules(career = true),
            careerIntent = "promotion",
            currentRole = "SDE1",
            targetRole = "SDE2",
        )
        vm.completeSetup(ModuleFlags.MODULE_CAREER, input)
        assertEquals("promotion", db.configDao().get("career_intent")?.value)
    }
}
