package com.example.solo_levelling.domain.service

import com.example.solo_levelling.core.time.FakeAppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.AttributeStatEntity
import com.example.solo_levelling.data.db.entity.DsaProblemEntity
import com.example.solo_levelling.data.db.entity.MetricLogEntity
import com.example.solo_levelling.data.db.entity.NutritionLogEntity
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.QuestInstanceEntity
import com.example.solo_levelling.data.db.entity.QuestTemplateEntity
import com.example.solo_levelling.data.db.entity.UserConfigEntity
import com.example.solo_levelling.data.db.entity.WorkoutLogEntity
import com.example.solo_levelling.data.db.entity.XpLedgerEntryEntity
import com.example.solo_levelling.domain.model.AttributeCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Robolectric-free isolation tests (JsonDatabase only needs a temp directory).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsModuleIsolationTest {
    private lateinit var db: JsonDatabase
    private lateinit var clock: FakeAppClock
    private lateinit var analytics: AnalyticsService
    private lateinit var tempDir: java.nio.file.Path
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        tempDir = Files.createTempDirectory("analytics-iso-")
        db = JsonDatabase(tempDir.toFile())
        clock = FakeAppClock(
            epochMs = LocalDate.of(2026, 8, 15)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli() + 12L * 60 * 60 * 1000,
            fixedDate = LocalDate.of(2026, 8, 15),
        )
        analytics = AnalyticsService(db, clock)
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
        db.playerDao().upsertAttributes(
            listOf(
                AttributeStatEntity(AttributeCode.STR.name, currentValue = 1),
                AttributeStatEntity(AttributeCode.INT.name, currentValue = 80),
                AttributeStatEntity(AttributeCode.DISC.name, currentValue = 50),
                AttributeStatEntity(AttributeCode.FOC.name, currentValue = 40),
                AttributeStatEntity(AttributeCode.VIT.name, currentValue = 30),
                AttributeStatEntity(AttributeCode.END.name, currentValue = 20),
                AttributeStatEntity(AttributeCode.WIS.name, currentValue = 45),
            ),
        )
        for ((k, v) in ModuleFlags.encode(modulesFlags)) {
            db.configDao().upsert(UserConfigEntity(k, v))
        }
    }

    private suspend fun seedDsaSolvedThisWeek() {
        db.moduleDao().upsertDsa(
            DsaProblemEntity(
                title = "Two Sum",
                externalId = "two_sum",
                status = "SOLVED",
                solvedAtEpochMs = clock.nowEpochMs(),
            ),
        )
        db.moduleDao().upsertDsa(
            DsaProblemEntity(
                title = "Binary Search",
                externalId = "bin_search",
                status = "MASTERED",
                solvedAtEpochMs = clock.nowEpochMs() - 86_400_000L,
            ),
        )
    }

    private suspend fun seedWorkoutToday() {
        db.moduleDao().upsertWorkoutLog(
            WorkoutLogEntity(
                date = "2026-08-15",
                dayOfWeek = "SATURDAY",
                workoutName = "Push",
                durationMinutes = 45,
            ),
        )
    }

    @Test
    fun r_fitnessOnly_beforeVsNowOmitsDsa() = runTest {
        seed(EnabledModules(career = false, workout = true, diet = false))
        seedDsaSolvedThisWeek()
        seedWorkoutToday()

        val before = analytics.beforeVsNow()
        assertNull(before.dsaSolvedBefore)
        assertNull(before.dsaSolvedNow)
        assertNotNull(before.workoutDaysNow)
        assertEquals(1, before.workoutDaysNow)
        assertNull(before.dietAdherenceNow)
        assertNull(before.weightNow)
    }

    @Test
    fun r_fitnessOnly_weeklyReviewOmitsDsaAndIgnoresDsaInScore() = runTest {
        seed(EnabledModules(career = false, workout = true, diet = false))
        seedDsaSolvedThisWeek()
        seedWorkoutToday()

        val withoutCareerData = analytics.weeklyReview()
        assertNull(withoutCareerData.dsaSolvedWeek)
        assertEquals(1, withoutCareerData.workoutCountWeek)
        val scoreFitnessOnly = withoutCareerData.personalScore

        for ((k, v) in ModuleFlags.encode(EnabledModules(career = true, workout = true, diet = false))) {
            db.configDao().upsert(UserConfigEntity(k, v))
        }
        val withCareer = analytics.weeklyReview()
        assertEquals(2, withCareer.dsaSolvedWeek)
        assertTrue(withCareer.personalScore >= scoreFitnessOnly)
    }

    @Test
    fun n_careerOnly_omitsWorkoutAndDietMetrics() = runTest {
        seed(EnabledModules(career = true, workout = false, diet = false))
        seedDsaSolvedThisWeek()
        seedWorkoutToday()
        db.moduleDao().upsertNutrition(
            NutritionLogEntity(date = "2026-08-15", calories = 1800, protein = 120),
        )
        db.moduleDao().insertMetric(
            MetricLogEntity(
                metricType = "WEIGHT",
                value = 70f,
                recordedAtEpochMs = clock.nowEpochMs(),
                date = "2026-08-15",
            ),
        )

        val review = analytics.weeklyReview()
        assertNull(review.workoutCountWeek)
        assertNull(review.workoutDaysWeek)
        assertEquals(2, review.dsaSolvedWeek)

        val before = analytics.beforeVsNow()
        assertNull(before.workoutDaysNow)
        assertNull(before.dietAdherenceNow)
        assertNull(before.weightNow)
        assertEquals(2, before.dsaSolvedNow)
    }

    @Test
    fun p_dietOnly_computesDietRowsOmitsCareerFitness() = runTest {
        seed(EnabledModules(career = false, workout = false, diet = true))
        seedDsaSolvedThisWeek()
        seedWorkoutToday()
        for (i in 0..3) {
            val day = LocalDate.of(2026, 8, 12).plusDays(i.toLong())
            db.moduleDao().upsertNutrition(
                NutritionLogEntity(date = day.toString(), calories = 1800, protein = 100),
            )
        }

        val before = analytics.beforeVsNow()
        assertNull(before.dsaSolvedNow)
        assertNull(before.workoutDaysNow)
        assertNotNull(before.dietAdherenceNow)

        val review = analytics.weeklyReview()
        assertNull(review.dsaSolvedWeek)
        assertNull(review.workoutCountWeek)
    }

    @Test
    fun r_fitnessOnly_nextFocusIgnoresStrengthWhenNotWorkout() = runTest {
        seed(EnabledModules(career = true, workout = false, diet = true))
        val review = analytics.weeklyReview()
        assertTrue(review.attributeSnapshot.bottomCode != AttributeCode.STR.name)
        assertTrue(review.attributeSnapshot.bottomCode != AttributeCode.END.name)
        assertTrue(review.recommendations.none { it.contains("STR") })
    }

    @Test
    fun p_enableCareer_restoresDsaMetrics() = runTest {
        seed(EnabledModules(career = false, workout = true, diet = false))
        seedDsaSolvedThisWeek()

        assertNull(analytics.beforeVsNow().dsaSolvedNow)

        for ((k, v) in ModuleFlags.encode(EnabledModules(career = true, workout = true, diet = false))) {
            db.configDao().upsert(UserConfigEntity(k, v))
        }
        val after = analytics.beforeVsNow()
        assertEquals(2, after.dsaSolvedNow)
        assertNotNull(after.workoutDaysNow)
    }

    @Test
    fun r_sumXpInRange_excludesCareerQuestWithoutMetadataWhenCareerOff() = runTest {
        seed(EnabledModules(career = false, workout = true, diet = true))
        db.questDao().upsertTemplates(
            listOf(
                QuestTemplateEntity(
                    key = "dsa_daily",
                    type = "DAILY",
                    title = "Solve 2 DSA problems",
                    baseXp = 40,
                    attributeRewardsJson = "{}",
                    priorityTags = "module_career",
                ),
                QuestTemplateEntity(
                    key = "workout_daily",
                    type = "DAILY",
                    title = "Complete workout",
                    baseXp = 50,
                    attributeRewardsJson = "{}",
                    priorityTags = "module_workout",
                ),
            ),
        )
        val templates = db.questDao().getActiveTemplates().associateBy { it.key }
        val dsaId = db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = templates.getValue("dsa_daily").id,
                scheduledDate = "2026-08-15",
                title = "Solve 2 DSA problems",
                type = "DAILY",
                baseXp = 40,
                attributeRewardsJson = "{}",
            ),
        )
        val workoutId = db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = templates.getValue("workout_daily").id,
                scheduledDate = "2026-08-15",
                title = "Complete workout",
                type = "DAILY",
                baseXp = 50,
                attributeRewardsJson = "{}",
            ),
        )
        db.xpDao().insertLedger(
            XpLedgerEntryEntity(
                amount = 40,
                sourceType = "QUEST_INSTANCE",
                sourceId = "${dsaId}_1",
                metadataJson = "{}",
                createdAtEpochMs = clock.nowEpochMs(),
            ),
        )
        db.xpDao().insertLedger(
            XpLedgerEntryEntity(
                amount = 50,
                sourceType = "QUEST_INSTANCE",
                sourceId = "${workoutId}_1",
                metadataJson = "{}",
                createdAtEpochMs = clock.nowEpochMs(),
            ),
        )

        val modules = EnabledModules(career = false, workout = true, diet = true)
        val start = clock.nowEpochMs() - 1_000
        val end = clock.nowEpochMs() + 1_000
        assertEquals(50, analytics.sumXpInRange(start, end, modules))
    }

    @Test
    fun r_workoutDiet_suggestionsOmitDsa() = runTest {
        seed(EnabledModules(career = false, workout = true, diet = true))
        val adaptive = AdaptiveService(db, clock)
        val keys = adaptive.suggestions().map { it.key }
        assertTrue(keys.none { it.contains("INT") })
    }
}
