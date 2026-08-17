package com.example.solo_levelling.domain.service

import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.FakeAppClock
import com.example.solo_levelling.data.db.JsonDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingServiceExpandedTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var dbDir: File
    private lateinit var db: JsonDatabase
    private lateinit var clock: FakeAppClock
    private lateinit var onboarding: OnboardingService

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        dbDir = File.createTempFile("solo-onboard-", "").also {
            it.delete()
            it.mkdirs()
        }
        db = JsonDatabase(dbDir)
        clock = FakeAppClock(fixedDate = LocalDate.of(2026, 8, 15))
        val questGen = QuestGenerationService(
            db,
            clock,
            EventBus(),
            AdaptiveService(db, clock),
            CoroutineScope(testDispatcher),
        )
        onboarding = OnboardingService(db, clock, questGen)
    }

    @After
    fun tearDown() {
        db.close()
        dbDir.deleteRecursively()
        Dispatchers.resetMain()
    }

    @Test
    fun p_completeOnboarding_persistsCareerAndNutritionConfigs() = runTest(testDispatcher) {
        onboarding.completeOnboarding(
            OnboardingInput(
                name = "Hunter",
                priorities = listOf("career", "fitness"),
                scheduleDays = listOf("MON", "WED", "FRI"),
                experienceBand = "1-2",
                currentRole = "SDE1",
                targetRole = "SDE2",
                yearsExperience = 1.5,
                dsaConfidence = 40,
                sdConfidence = 30,
                age = 28,
                sex = "male",
                heightCm = 175.0,
                weightKg = 72.0,
                fitnessGoal = "maintenance",
                trainingDays = 4,
            ),
        )

        assertEquals("1,3,5", db.configDao().get("schedule_days_csv")!!.value)
        assertTrue(db.configDao().get("goal_title")!!.value.isNotBlank())
        assertEquals(db.configDao().get("goal_title")!!.value, db.configDao().get("career_next_goal")!!.value)
        assertTrue(db.configDao().get("career_mandatory_areas")!!.value.contains("DSA"))
        assertTrue(db.configDao().get("career_mandatory_areas")!!.value.contains("System Design"))
        assertNotNull(db.configDao().get("calorie_target"))
        assertNotNull(db.configDao().get("protein_target"))
        assertEquals("72.0", db.configDao().get("weight_kg")!!.value)
        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID)!!
        assertTrue(profile.onboardingDone)
        assertEquals("Hunter", profile.name)
    }

    @Test
    fun p_ensureSeeded_seedsDsaAndSystemDesignWhenEmpty() = runTest(testDispatcher) {
        onboarding.ensureSeeded()
        assertTrue(db.moduleDao().getDsaProblems().size >= 12)
        assertTrue(db.moduleDao().getSystemDesignTopics().size >= 7)
    }

    @Test
    fun p_completeOnboarding_seedsWeightMetric() = runTest(testDispatcher) {
        onboarding.completeOnboarding(
            OnboardingInput(
                name = "Hunter",
                priorities = listOf("fitness"),
                scheduleDays = listOf("MON"),
                weightKg = 80.0,
            ),
        )
        val metrics = db.moduleDao().recentMetrics("WEIGHT", 5)
        assertEquals(1, metrics.size)
        assertEquals(80f, metrics.first().value)
    }

    @Test
    fun n_completeOnboarding_emptyNameFallsBackToHunter() = runTest(testDispatcher) {
        onboarding.completeOnboarding(
            OnboardingInput(
                name = "   ",
                priorities = listOf("career"),
                scheduleDays = listOf("MON"),
            ),
        )
        assertEquals("Hunter", db.playerDao().getProfile(SystemDefaults.PLAYER_ID)!!.name)
    }

    @Test
    fun e_scheduleDaysToCsv_ignoresUnknownTokens() {
        assertEquals("1,7", scheduleDaysToCsv(listOf("MON", "FOO", "SUN")))
        assertEquals("", scheduleDaysToCsv(emptyList()))
    }
}
