package com.example.solo_levelling.domain.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.FakeAppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.AttributeStatEntity
import com.example.solo_levelling.data.db.entity.DsaProblemEntity
import com.example.solo_levelling.data.db.entity.FocusSessionEntity
import com.example.solo_levelling.data.db.entity.MetricLogEntity
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.QuestInstanceEntity
import com.example.solo_levelling.data.db.entity.QuestTemplateEntity
import com.example.solo_levelling.domain.model.AttributeCode
import com.example.solo_levelling.domain.model.QuestStatus
import com.example.solo_levelling.domain.model.VerificationType
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QuestVerificationServiceTest {
    private lateinit var db: JsonDatabase
    private lateinit var clock: FakeAppClock
    private lateinit var questCompletion: QuestCompletionService
    private lateinit var service: QuestVerificationService

    private val date = "2026-08-15"

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = JsonDatabase(File(context.cacheDir, "test-db-${System.nanoTime()}").also { it.mkdirs() })
        clock = FakeAppClock(
            epochMs = LocalDate.of(2026, 8, 15).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() + 3_600_000,
            fixedDate = LocalDate.of(2026, 8, 15),
        )
        val eventBus = EventBus()
        val progression = ProgressionService(db, eventBus, clock)
        questCompletion = QuestCompletionService(db, eventBus, clock, progression)
        service = QuestVerificationService(db, clock, questCompletion)
        runBlocking { seedBase() }
    }

    private suspend fun seedBase() {
        db.playerDao().upsertProfile(
            PlayerProfileEntity(
                name = "Test",
                timezone = "UTC",
                onboardingDone = true,
                createdAtEpochMs = clock.nowEpochMs(),
            ),
        )
        db.playerDao().upsertAttributes(
            AttributeCode.entries.map { AttributeStatEntity(it.name) },
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seedCountQuest(target: Float = 2f): QuestInstanceEntity {
        val templateId = db.questDao().upsertTemplate(
            QuestTemplateEntity(
                key = "dsa_daily",
                type = "DAILY",
                title = "Solve DSA",
                baseXp = 40,
                attributeRewardsJson = """{"INT":30}""",
                verificationType = VerificationType.COUNT.name,
                verificationTarget = target,
            ),
        )
        val id = db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = templateId,
                scheduledDate = date,
                status = QuestStatus.AVAILABLE.name,
                title = "Solve DSA",
                type = "DAILY",
                baseXp = 40,
                attributeRewardsJson = """{"INT":30}""",
                verificationType = VerificationType.COUNT.name,
                verificationTarget = target,
            ),
        )
        return db.questDao().getInstance(id)!!
    }

    @Test
    fun p_countDsa_satisfiedWhenEnoughProblems() = runTest {
        val quest = seedCountQuest(2f)
        val now = clock.nowEpochMs()
        db.moduleDao().upsertDsa(
            DsaProblemEntity(title = "A", externalId = "a", status = "SOLVED", solvedAtEpochMs = now),
        )
        db.moduleDao().upsertDsa(
            DsaProblemEntity(title = "B", externalId = "b", status = "MASTERED", solvedAtEpochMs = now),
        )
        assertTrue(service.isSatisfied(quest, date))
    }

    @Test
    fun n_countDsa_notSatisfiedWhenUnderTarget() = runTest {
        val quest = seedCountQuest(2f)
        db.moduleDao().upsertDsa(
            DsaProblemEntity(title = "A", externalId = "a", status = "SOLVED", solvedAtEpochMs = clock.nowEpochMs()),
        )
        assertFalse(service.isSatisfied(quest, date))
    }

    @Test
    fun n_countDsa_zeroProblemsNotSatisfied() = runTest {
        val quest = seedCountQuest(1f)
        assertFalse(service.isSatisfied(quest, date))
    }

    @Test
    fun e_metricSteps_satisfiedWhenSumMeetsTarget() = runTest {
        val id = db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = 1,
                scheduledDate = date,
                status = QuestStatus.AVAILABLE.name,
                title = "Steps",
                type = "DAILY",
                baseXp = 25,
                attributeRewardsJson = """{"END":20}""",
                verificationType = VerificationType.METRIC_THRESHOLD.name,
                verificationTarget = 10000f,
                verificationUnit = "STEPS",
            ),
        )
        val quest = db.questDao().getInstance(id)!!
        db.moduleDao().insertMetric(
            MetricLogEntity(metricType = "STEPS", value = 5000f, recordedAtEpochMs = clock.nowEpochMs(), date = date),
        )
        assertFalse(service.isSatisfied(quest, date))
        db.moduleDao().insertMetric(
            MetricLogEntity(metricType = "STEPS", value = 6000f, recordedAtEpochMs = clock.nowEpochMs(), date = date),
        )
        assertTrue(service.isSatisfied(quest, date))
    }

    @Test
    fun n_metricSteps_underTargetNotSatisfied() = runTest {
        val id = db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = 1,
                scheduledDate = date,
                status = QuestStatus.AVAILABLE.name,
                title = "Steps",
                type = "DAILY",
                baseXp = 25,
                attributeRewardsJson = """{"END":20}""",
                verificationType = VerificationType.METRIC_THRESHOLD.name,
                verificationTarget = 10000f,
                verificationUnit = "STEPS",
            ),
        )
        val quest = db.questDao().getInstance(id)!!
        db.moduleDao().insertMetric(
            MetricLogEntity(metricType = "STEPS", value = 9999f, recordedAtEpochMs = clock.nowEpochMs(), date = date),
        )
        assertFalse(service.isSatisfied(quest, date))
    }

    @Test
    fun p_timerFocus_satisfiedWhenMinutesMeetTarget() = runTest {
        val id = db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = 1,
                scheduledDate = date,
                status = QuestStatus.AVAILABLE.name,
                title = "Deep work",
                type = "DAILY",
                baseXp = 45,
                attributeRewardsJson = """{"FOC":30}""",
                verificationType = VerificationType.TIMER.name,
                verificationTarget = 90f,
            ),
        )
        val quest = db.questDao().getInstance(id)!!
        db.moduleDao().insertFocus(
            FocusSessionEntity(date = date, durationMinutes = 60, completedAtEpochMs = clock.nowEpochMs()),
        )
        assertFalse(service.isSatisfied(quest, date))
        db.moduleDao().insertFocus(
            FocusSessionEntity(date = date, durationMinutes = 30, completedAtEpochMs = clock.nowEpochMs()),
        )
        assertTrue(service.isSatisfied(quest, date))
    }

    @Test
    fun n_timerFocus_underTargetNotSatisfied() = runTest {
        val id = db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = 1,
                scheduledDate = date,
                status = QuestStatus.AVAILABLE.name,
                title = "Deep work",
                type = "DAILY",
                baseXp = 45,
                attributeRewardsJson = """{"FOC":30}""",
                verificationType = VerificationType.TIMER.name,
                verificationTarget = 90f,
            ),
        )
        val quest = db.questDao().getInstance(id)!!
        db.moduleDao().insertFocus(
            FocusSessionEntity(date = date, durationMinutes = 89, completedAtEpochMs = clock.nowEpochMs()),
        )
        assertFalse(service.isSatisfied(quest, date))
    }
}
