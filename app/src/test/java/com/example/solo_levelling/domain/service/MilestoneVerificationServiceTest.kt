package com.example.solo_levelling.domain.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.FakeAppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.AttributeStatEntity
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.QuestInstanceEntity
import com.example.solo_levelling.data.db.entity.QuestTemplateEntity
import com.example.solo_levelling.data.db.entity.UserConfigEntity
import com.example.solo_levelling.domain.model.AttributeCode
import com.example.solo_levelling.domain.model.QuestStatus
import com.example.solo_levelling.domain.model.VerificationType
import com.example.solo_levelling.ui.navigation.QuestActionDestination
import com.example.solo_levelling.ui.navigation.QuestDestinationResolver
import com.example.solo_levelling.ui.quests.milestoneRequirementAction
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MilestoneVerificationServiceTest {
    private lateinit var db: JsonDatabase
    private lateinit var clock: FakeAppClock
    private lateinit var progression: ProgressionService
    private lateinit var service: MilestoneVerificationService

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = JsonDatabase(File(context.cacheDir, "test-db-${System.nanoTime()}").also { it.mkdirs() })
        clock = FakeAppClock(fixedDate = LocalDate.of(2026, 8, 15))
        val eventBus = EventBus()
        progression = ProgressionService(db, eventBus, clock)
        service = MilestoneVerificationService(db, progression)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seedPlayer(modules: EnabledModules = EnabledModules(true, true, true)) {
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

    private suspend fun seedTemplate(
        key: String,
        type: String,
        tags: String = "",
        verificationType: String = VerificationType.MANUAL.name,
    ): Long = db.questDao().upsertTemplate(
        QuestTemplateEntity(
            key = key,
            type = type,
            title = key,
            baseXp = 20,
            attributeRewardsJson = "{}",
            priorityTags = tags,
            verificationType = verificationType,
        ),
    )

    private suspend fun seedInstance(
        templateId: Long,
        type: String,
        status: String,
        date: String = "2026-08-15",
        title: String = type,
        verificationType: String = VerificationType.MANUAL.name,
    ): Long = db.questDao().insertInstance(
        QuestInstanceEntity(
            templateId = templateId,
            scheduledDate = date,
            status = status,
            title = title,
            type = type,
            baseXp = 20,
            attributeRewardsJson = "{}",
            verificationType = verificationType,
        ),
    )

    @Test
    fun n_allRequirementsIncomplete_milestoneNotReady() = runTest {
        seedPlayer()
        val workout = seedTemplate("workout_daily", "DAILY", "module_workout")
        val diet = seedTemplate("nutrition_daily", "DAILY", "module_diet")
        val milestoneTpl = seedTemplate("first_week_complete", "MILESTONE")
        seedInstance(workout, "DAILY", QuestStatus.AVAILABLE.name, title = "Complete workout")
        seedInstance(diet, "DAILY", QuestStatus.AVAILABLE.name, title = "Log nutrition")
        val milestoneId = seedInstance(milestoneTpl, "MILESTONE", QuestStatus.AVAILABLE.name, title = "First week")
        val result = service.verify(db.questDao().getInstance(milestoneId)!!)
        assertFalse(result.ready)
        assertEquals(0, result.completedCount)
        assertEquals(2, result.totalCount)
    }

    @Test
    fun n_someRequirementsIncomplete_returnsRemainingTitles() = runTest {
        seedPlayer()
        val workout = seedTemplate("workout_daily", "DAILY", "module_workout")
        val diet = seedTemplate("nutrition_daily", "DAILY", "module_diet")
        val journal = seedTemplate("journal", "DAILY")
        val milestoneTpl = seedTemplate("first_week_complete", "MILESTONE")
        seedInstance(workout, "DAILY", QuestStatus.COMPLETED.name, title = "Complete workout")
        seedInstance(diet, "DAILY", QuestStatus.AVAILABLE.name, title = "Log nutrition")
        seedInstance(journal, "DAILY", QuestStatus.AVAILABLE.name, title = "Write a short journal")
        val milestoneId = seedInstance(milestoneTpl, "MILESTONE", QuestStatus.AVAILABLE.name)
        val result = service.verify(db.questDao().getInstance(milestoneId)!!)
        assertFalse(result.ready)
        assertEquals(1, result.completedCount)
        assertEquals(3, result.totalCount)
        assertEquals(
            listOf("Log nutrition", "Write a short journal"),
            result.requirements.filter { !it.completed }.map { it.title },
        )
    }

    @Test
    fun p_allRequirementsComplete_milestoneReady() = runTest {
        seedPlayer()
        val workout = seedTemplate("workout_daily", "DAILY", "module_workout")
        val diet = seedTemplate("nutrition_daily", "DAILY", "module_diet")
        val milestoneTpl = seedTemplate("first_week_complete", "MILESTONE")
        seedInstance(workout, "DAILY", QuestStatus.COMPLETED.name)
        seedInstance(diet, "DAILY", QuestStatus.COMPLETED.name)
        val milestoneId = seedInstance(milestoneTpl, "MILESTONE", QuestStatus.AVAILABLE.name)
        val result = service.verify(db.questDao().getInstance(milestoneId)!!)
        assertTrue(result.ready)
        assertEquals(2, result.completedCount)
        assertEquals(2, result.totalCount)
    }

    @Test
    fun n_emptyWeek_isNotReady() = runTest {
        seedPlayer()
        val milestoneTpl = seedTemplate("first_week_complete", "MILESTONE")
        val milestoneId = seedInstance(milestoneTpl, "MILESTONE", QuestStatus.AVAILABLE.name)
        val result = service.verify(db.questDao().getInstance(milestoneId)!!)
        assertFalse(result.ready)
        assertEquals(0, result.totalCount)
        assertTrue(result.requirements.isEmpty())
    }

    @Test
    fun e_invalidScheduledDate_doesNotCrash() = runTest {
        seedPlayer()
        val milestoneTpl = seedTemplate("first_week_complete", "MILESTONE")
        val milestoneId = seedInstance(
            milestoneTpl,
            "MILESTONE",
            QuestStatus.AVAILABLE.name,
            date = "not-a-date",
        )
        val result = service.verify(db.questDao().getInstance(milestoneId)!!)
        assertFalse(result.ready)
        assertEquals(0, result.totalCount)
    }

    @Test
    fun e_automaticWeeklyReview_isExcluded() = runTest {
        seedPlayer()
        val workout = seedTemplate("workout_daily", "DAILY", "module_workout")
        val review = seedTemplate(
            "weekly_review",
            "WEEKLY",
            verificationType = VerificationType.AUTOMATIC.name,
        )
        val milestoneTpl = seedTemplate("first_week_complete", "MILESTONE")
        seedInstance(workout, "DAILY", QuestStatus.COMPLETED.name)
        seedInstance(
            review,
            "WEEKLY",
            QuestStatus.AVAILABLE.name,
            date = "2026-08-16",
            verificationType = VerificationType.AUTOMATIC.name,
        )
        val milestoneId = seedInstance(milestoneTpl, "MILESTONE", QuestStatus.AVAILABLE.name)
        val result = service.verify(db.questDao().getInstance(milestoneId)!!)
        assertTrue(result.ready)
        assertEquals(1, result.totalCount)
        assertTrue(result.requirements.none { it.templateKey == "weekly_review" })
    }

    @Test
    fun e_outOfWeekAndRecoveryIgnored() = runTest {
        seedPlayer()
        val workout = seedTemplate("workout_daily", "DAILY", "module_workout")
        val recovery = seedTemplate("recovery", "RECOVERY")
        val milestoneTpl = seedTemplate("first_week_complete", "MILESTONE")
        seedInstance(workout, "DAILY", QuestStatus.COMPLETED.name)
        seedInstance(workout, "DAILY", QuestStatus.AVAILABLE.name, date = "2026-08-09", title = "Last week")
        seedInstance(recovery, "RECOVERY", QuestStatus.AVAILABLE.name, title = "Recovery")
        val milestoneId = seedInstance(milestoneTpl, "MILESTONE", QuestStatus.AVAILABLE.name)
        val result = service.verify(db.questDao().getInstance(milestoneId)!!)
        assertTrue(result.ready)
        assertEquals(1, result.totalCount)
        assertTrue(result.requirements.none { it.title == "Last week" })
        assertTrue(result.requirements.none { it.questType == "RECOVERY" })
    }

    @Test
    fun r_fitnessOnly_doesNotEvaluateCareer() = runTest {
        seedPlayer(EnabledModules(career = false, workout = true, diet = false))
        val workout = seedTemplate("workout_daily", "DAILY", "module_workout")
        val career = seedTemplate("dsa_daily", "DAILY", "module_career")
        val milestoneTpl = seedTemplate("first_week_complete", "MILESTONE")
        seedInstance(workout, "DAILY", QuestStatus.COMPLETED.name, title = "Complete workout")
        seedInstance(career, "DAILY", QuestStatus.AVAILABLE.name, title = "Solve 2 DSA problems")
        val milestoneId = seedInstance(milestoneTpl, "MILESTONE", QuestStatus.AVAILABLE.name)
        val result = service.verify(db.questDao().getInstance(milestoneId)!!)
        assertTrue(result.ready)
        assertEquals(listOf("workout_daily"), result.requirements.map { it.templateKey })
    }

    @Test
    fun r_dietOnly_doesNotEvaluateCareer() = runTest {
        seedPlayer(EnabledModules(career = false, workout = false, diet = true))
        val diet = seedTemplate("nutrition_daily", "DAILY", "module_diet")
        val career = seedTemplate("system_design", "WEEKLY", "module_career")
        val milestoneTpl = seedTemplate("first_week_complete", "MILESTONE")
        seedInstance(diet, "DAILY", QuestStatus.COMPLETED.name, title = "Log nutrition")
        seedInstance(career, "WEEKLY", QuestStatus.AVAILABLE.name, date = "2026-08-16", title = "System design")
        val milestoneId = seedInstance(milestoneTpl, "MILESTONE", QuestStatus.AVAILABLE.name)
        val result = service.verify(db.questDao().getInstance(milestoneId)!!)
        assertTrue(result.ready)
        assertEquals(listOf("nutrition_daily"), result.requirements.map { it.templateKey })
    }

    @Test
    fun r_careerOnly_doesNotEvaluateFitnessOrDiet() = runTest {
        seedPlayer(EnabledModules(career = true, workout = false, diet = false))
        val career = seedTemplate("dsa_daily", "DAILY", "module_career")
        val workout = seedTemplate("workout_daily", "DAILY", "module_workout")
        val diet = seedTemplate("nutrition_daily", "DAILY", "module_diet")
        val milestoneTpl = seedTemplate("first_week_complete", "MILESTONE")
        seedInstance(career, "DAILY", QuestStatus.COMPLETED.name, title = "Solve 2 DSA problems")
        seedInstance(workout, "DAILY", QuestStatus.AVAILABLE.name, title = "Complete workout")
        seedInstance(diet, "DAILY", QuestStatus.AVAILABLE.name, title = "Log nutrition")
        val milestoneId = seedInstance(milestoneTpl, "MILESTONE", QuestStatus.AVAILABLE.name)
        val result = service.verify(db.questDao().getInstance(milestoneId)!!)
        assertTrue(result.ready)
        assertEquals(listOf("dsa_daily"), result.requirements.map { it.templateKey })
    }

    @Test
    fun p_multipleModules_unionOfRelevantRequirements() = runTest {
        seedPlayer(EnabledModules(career = true, workout = true, diet = true))
        val career = seedTemplate("dsa_daily", "DAILY", "module_career")
        val workout = seedTemplate("workout_daily", "DAILY", "module_workout")
        val diet = seedTemplate("nutrition_daily", "DAILY", "module_diet")
        val milestoneTpl = seedTemplate("first_week_complete", "MILESTONE")
        seedInstance(career, "DAILY", QuestStatus.COMPLETED.name)
        seedInstance(workout, "DAILY", QuestStatus.AVAILABLE.name)
        seedInstance(diet, "DAILY", QuestStatus.COMPLETED.name)
        val milestoneId = seedInstance(milestoneTpl, "MILESTONE", QuestStatus.AVAILABLE.name)
        val result = service.verify(db.questDao().getInstance(milestoneId)!!)
        assertFalse(result.ready)
        assertEquals(3, result.totalCount)
        assertEquals(2, result.completedCount)
        assertEquals(listOf("workout_daily"), result.requirements.filter { !it.completed }.map { it.templateKey })
    }

    @Test
    fun p_incompleteRequirements_mapToCorrectDestinations() = runTest {
        seedPlayer()
        val workout = seedTemplate("workout_daily", "DAILY", "module_workout")
        val diet = seedTemplate("nutrition_daily", "DAILY", "module_diet")
        val career = seedTemplate(
            "dsa_daily",
            "DAILY",
            "module_career",
            VerificationType.COUNT.name,
        )
        val journal = seedTemplate("journal", "DAILY")
        val milestoneTpl = seedTemplate("first_week_complete", "MILESTONE")
        seedInstance(workout, "DAILY", QuestStatus.AVAILABLE.name, title = "Complete workout")
        seedInstance(diet, "DAILY", QuestStatus.AVAILABLE.name, title = "Log nutrition")
        seedInstance(
            career,
            "DAILY",
            QuestStatus.AVAILABLE.name,
            title = "Solve 2 DSA problems",
            verificationType = VerificationType.COUNT.name,
        )
        seedInstance(journal, "DAILY", QuestStatus.AVAILABLE.name, title = "Write a short journal")
        val milestoneId = seedInstance(milestoneTpl, "MILESTONE", QuestStatus.AVAILABLE.name)
        val result = service.verify(db.questDao().getInstance(milestoneId)!!)
        val destinations = result.requirements.associate { it.templateKey to milestoneRequirementAction(it).destination }
        assertEquals(QuestActionDestination.Fitness, destinations["workout_daily"])
        assertEquals(QuestActionDestination.Nutrition, destinations["nutrition_daily"])
        assertEquals(QuestActionDestination.Career, destinations["dsa_daily"])
        assertEquals(QuestActionDestination.Modules, destinations["journal"])
        assertEquals(
            QuestDestinationResolver.SECTION_JOURNAL,
            milestoneRequirementAction(result.requirements.first { it.templateKey == "journal" }).section,
        )
    }
}
