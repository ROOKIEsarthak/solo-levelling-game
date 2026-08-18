package com.example.solo_levelling.ui.analysis

import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.FakeAppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.domain.service.AdaptiveService
import com.example.solo_levelling.domain.service.EnabledModules
import com.example.solo_levelling.domain.service.OnboardingInput
import com.example.solo_levelling.domain.service.OnboardingService
import com.example.solo_levelling.domain.service.ProgressionService
import com.example.solo_levelling.domain.service.QuestGenerationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class SystemAnalysisViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var dbDir: File
    private lateinit var db: JsonDatabase
    private lateinit var onboarding: OnboardingService
    private val input = OnboardingInput(
        name = "Hunter",
        modules = EnabledModules(career = true),
        priorities = listOf("career"),
        experienceBand = "1-2",
        currentRole = "SDE1",
        targetRole = "SDE2",
    )

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        dbDir = File.createTempFile("solo-analysis-", "").also {
            it.delete()
            it.mkdirs()
        }
        db = JsonDatabase(dbDir)
        val clock = FakeAppClock(fixedDate = LocalDate.of(2026, 8, 15))
        val questGen = QuestGenerationService(
            db,
            clock,
            EventBus(),
            AdaptiveService(db, clock),
            CoroutineScope(dispatcher),
        )
        onboarding = OnboardingService(db, clock, questGen, ProgressionService(db, EventBus(), clock))
    }

    @After
    fun tearDown() {
        db.close()
        dbDir.deleteRecursively()
        Dispatchers.resetMain()
    }

    @Test
    fun p_start_completesOnboardingAndFinishesAfterMinDuration() = runTest(dispatcher) {
        val vm = SystemAnalysisViewModel(onboarding, input)
        vm.start()
        runCurrent()
        assertFalse(vm.finished.value)

        advanceUntilIdle()
        assertTrue(vm.finished.value)
        assertEquals(AnalysisPhase.Ready, vm.phase.value)
        assertEquals(1f, vm.progress.value, 0.0001f)
        assertTrue(db.playerDao().getProfile(SystemDefaults.PLAYER_ID)!!.onboardingDone)
    }

    @Test
    fun n_start_staysOnLoadingAnimationUntilFiveSeconds() = runTest(dispatcher) {
        val vm = SystemAnalysisViewModel(onboarding, input)
        vm.start()
        runCurrent()
        assertEquals(AnalysisPhase.Loading, vm.phase.value)
        assertFalse(vm.finished.value)

        advanceTimeBy(4_900)
        runCurrent()
        assertEquals(AnalysisPhase.Loading, vm.phase.value)
        assertFalse(vm.finished.value)
        assertTrue(vm.progress.value < 1f)

        advanceUntilIdle()
        assertEquals(AnalysisPhase.Ready, vm.phase.value)
        assertTrue(vm.finished.value)
        assertEquals(1f, vm.progress.value, 0.0001f)
    }

    @Test
    fun r_start_workDoneEarlyStillWaitsFullFiveSeconds() = runTest(dispatcher) {
        val vm = SystemAnalysisViewModel(onboarding, input)
        vm.start()
        runCurrent()
        assertFalse(vm.finished.value)

        advanceTimeBy(4_999)
        runCurrent()
        assertEquals(AnalysisPhase.Loading, vm.phase.value)
        assertFalse(vm.finished.value)

        advanceTimeBy(1)
        advanceUntilIdle()
        assertEquals(AnalysisPhase.Ready, vm.phase.value)
        assertTrue(vm.finished.value)
    }

    @Test
    fun n_start_secondCallDoesNotRestart() = runTest(dispatcher) {
        val vm = SystemAnalysisViewModel(onboarding, input)
        vm.start()
        advanceUntilIdle()
        assertTrue(vm.finished.value)
        assertEquals(AnalysisPhase.Ready, vm.phase.value)

        vm.start()
        assertTrue(vm.finished.value)
        assertEquals(AnalysisPhase.Ready, vm.phase.value)
    }

    @Test
    fun n_doesNotPersistOnboardingUntilStart() = runTest(dispatcher) {
        onboarding.ensureSeeded()
        assertFalse(db.playerDao().getProfile(SystemDefaults.PLAYER_ID)!!.onboardingDone)
        SystemAnalysisViewModel(onboarding, input)
        assertFalse(db.playerDao().getProfile(SystemDefaults.PLAYER_ID)!!.onboardingDone)
    }
}
