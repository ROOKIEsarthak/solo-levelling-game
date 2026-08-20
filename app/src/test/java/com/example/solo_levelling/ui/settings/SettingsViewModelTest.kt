package com.example.solo_levelling.ui.settings

import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.FakeAppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.UserConfigEntity
import com.example.solo_levelling.domain.service.AdaptiveService
import com.example.solo_levelling.domain.service.AnalyticsService
import com.example.solo_levelling.domain.service.EnabledModules
import com.example.solo_levelling.domain.service.ModuleFlags
import com.example.solo_levelling.domain.service.ModuleLifecycleService
import com.example.solo_levelling.domain.service.ModuleService
import com.example.solo_levelling.domain.service.OnboardingService
import com.example.solo_levelling.domain.service.ProgressionService
import com.example.solo_levelling.domain.service.QuestCompletionService
import com.example.solo_levelling.domain.service.QuestGenerationService
import com.example.solo_levelling.domain.service.QuestVerificationService
import com.example.solo_levelling.domain.service.SeasonService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class SettingsViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var db: JsonDatabase
    private lateinit var vm: SettingsViewModel
    private lateinit var tempDir: java.nio.file.Path

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        tempDir = Files.createTempDirectory("settings-vm-")
        db = JsonDatabase(tempDir.toFile())
        val clock = FakeAppClock(fixedDate = LocalDate.of(2026, 8, 15))
        val eventBus = EventBus()
        val progression = ProgressionService(db, eventBus, clock)
        val season = SeasonService(db, clock)
        val questGeneration = QuestGenerationService(db, clock, eventBus, AdaptiveService(db, clock))
        val onboarding = OnboardingService(db, clock, questGeneration, progression, season)
        val lifecycle = ModuleLifecycleService(db, clock, onboarding)
        val questCompletion = QuestCompletionService(db, eventBus, clock, progression)
        val verification = QuestVerificationService(db, clock, questCompletion)
        val modules = ModuleService(db, eventBus, clock, progression, verification)
        val analytics = AnalyticsService(db, clock)
        vm = SettingsViewModel(
            db = db,
            moduleLifecycle = lifecycle,
            modules = modules,
            onboarding = onboarding,
            questGeneration = questGeneration,
            progression = progression,
            season = season,
            analytics = analytics,
        )
    }

    @After
    fun tearDown() {
        db.close()
        tempDir.toFile().deleteRecursively()
        Dispatchers.resetMain()
    }

    @Test
    fun p_setNotificationsEnabled_true() = runTest(dispatcher) {
        vm.setNotificationsEnabled(true)
        assertEquals("true", db.configDao().get("notifications_enabled")?.value)
    }

    @Test
    fun n_setNotificationsEnabled_false() = runTest(dispatcher) {
        vm.setNotificationsEnabled(false)
        assertEquals("false", db.configDao().get("notifications_enabled")?.value)
    }

    @Test
    fun p_upsertConfig_persistsValue() = runTest(dispatcher) {
        vm.upsertConfig("goal_title", "Staff Engineer")
        assertEquals("Staff Engineer", db.configDao().get("goal_title")?.value)
    }

    @Test
    fun p_upsertProfile_updatesName() = runTest(dispatcher) {
        db.playerDao().upsertProfile(
            PlayerProfileEntity(name = "Old", timezone = "UTC", onboardingDone = true),
        )
        val profile = vm.getProfile()!!
        vm.upsertProfile(profile.copy(name = "New"))
        assertEquals("New", vm.getProfile()?.name)
    }

    @Test
    fun n_applyModuleChanges_blocksWhenAllDisabled() = runTest(dispatcher) {
        db.playerDao().upsertProfile(
            PlayerProfileEntity(name = "Test", timezone = "UTC", onboardingDone = true),
        )
        for ((k, v) in ModuleFlags.encode(EnabledModules(career = true, workout = true, diet = true))) {
            db.configDao().upsert(UserConfigEntity(k, v))
        }
        val result = vm.applyModuleChanges(EnabledModules())
        assertTrue(result.blocked)
    }
}
