package com.example.solo_levelling.domain.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.FakeAppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.AttributeStatEntity
import com.example.solo_levelling.data.db.entity.DsaProblemEntity
import com.example.solo_levelling.data.db.entity.FocusSessionEntity
import com.example.solo_levelling.data.db.entity.JournalEntryEntity
import com.example.solo_levelling.data.db.entity.MetricLogEntity
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.QuestInstanceEntity
import com.example.solo_levelling.data.db.entity.QuestTemplateEntity
import com.example.solo_levelling.data.db.entity.XpLedgerEntryEntity
import com.example.solo_levelling.domain.model.AttributeCode
import com.example.solo_levelling.domain.model.QuestStatus
import com.example.solo_levelling.domain.model.VerificationType
import kotlinx.coroutines.runBlocking
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

    private suspend fun seedManualQuest(key: String, title: String): Long {
        val templateId = db.questDao().upsertTemplate(
            QuestTemplateEntity(
                key = key,
                type = "DAILY",
                title = title,
                baseXp = 20,
                attributeRewardsJson = """{"WIS":15}""",
                verificationType = VerificationType.MANUAL.name,
            ),
        )
        return db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = templateId,
                scheduledDate = date,
                status = QuestStatus.AVAILABLE.name,
                title = title,
                type = "DAILY",
                baseXp = 20,
                attributeRewardsJson = """{"WIS":15}""",
                verificationType = VerificationType.MANUAL.name,
            ),
        )
    }

    @Test
    fun p_journalEntry_autoCompletesJournalQuest() = runTest {
        val questId = seedManualQuest("journal", "Write a short journal")
        db.moduleDao().upsertJournal(
            JournalEntryEntity(date = date, content = "Today I trained.", updatedAtEpochMs = clock.nowEpochMs()),
        )
        service.tryAutoComplete(date)
        assertEquals(QuestStatus.COMPLETED.name, db.questDao().getInstance(questId)!!.status)
    }

    @Test
    fun n_blankJournal_doesNotCompleteJournalQuest() = runTest {
        val questId = seedManualQuest("journal", "Write a short journal")
        db.moduleDao().upsertJournal(
            JournalEntryEntity(date = date, content = "   ", updatedAtEpochMs = clock.nowEpochMs()),
        )
        service.tryAutoComplete(date)
        assertEquals(QuestStatus.AVAILABLE.name, db.questDao().getInstance(questId)!!.status)
    }

    @Test
    fun n_missingJournal_doesNotCompleteJournalQuest() = runTest {
        val questId = seedManualQuest("journal", "Write a short journal")
        service.tryAutoComplete(date)
        assertEquals(QuestStatus.AVAILABLE.name, db.questDao().getInstance(questId)!!.status)
    }

    @Test
    fun e_journalAlreadyCompleted_tryAutoCompleteDoesNotAwardAgain() = runTest {
        val questId = seedManualQuest("journal", "Write a short journal")
        db.moduleDao().upsertJournal(
            JournalEntryEntity(date = date, content = "Done.", updatedAtEpochMs = clock.nowEpochMs()),
        )
        service.tryAutoComplete(date)
        val firstXp = db.xpDao().getAllLedger().filter { it.sourceType == "QUEST_INSTANCE" }.sumOf { it.amount }
        service.tryAutoComplete(date)
        val secondXp = db.xpDao().getAllLedger().filter { it.sourceType == "QUEST_INSTANCE" }.sumOf { it.amount }
        assertEquals(QuestStatus.COMPLETED.name, db.questDao().getInstance(questId)!!.status)
        assertEquals(firstXp, secondXp)
        assertTrue(firstXp > 0)
    }

    @Test
    fun p_systemDesignStudyToday_autoCompletesWeeklyQuest() = runTest {
        val templateId = db.questDao().upsertTemplate(
            QuestTemplateEntity(
                key = "system_design",
                type = "WEEKLY",
                title = "System design study",
                baseXp = 80,
                attributeRewardsJson = """{"INT":50}""",
                verificationType = VerificationType.MANUAL.name,
                priorityTags = "module_career,system_design",
            ),
        )
        val questId = db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = templateId,
                scheduledDate = date,
                status = QuestStatus.AVAILABLE.name,
                title = "System design study",
                type = "WEEKLY",
                baseXp = 80,
                attributeRewardsJson = """{"INT":50}""",
                verificationType = VerificationType.MANUAL.name,
            ),
        )
        db.xpDao().insertLedger(
            XpLedgerEntryEntity(
                amount = 10,
                sourceType = "SD_CONCEPT",
                sourceId = "sd_topic_c1",
                createdAtEpochMs = clock.nowEpochMs(),
            ),
        )
        service.tryAutoComplete(date)
        assertEquals(QuestStatus.COMPLETED.name, db.questDao().getInstance(questId)!!.status)
    }

    @Test
    fun n_systemDesignWithoutStudy_doesNotComplete() = runTest {
        val questId = seedManualQuest("system_design", "System design study")
        service.tryAutoComplete(date)
        assertEquals(QuestStatus.AVAILABLE.name, db.questDao().getInstance(questId)!!.status)
    }

    @Test
    fun n_tryAutoComplete_pastDate_doesNotComplete() = runTest {
        val questId = seedManualQuest("workout_daily", "Workout")
        db.questDao().updateInstance(
            db.questDao().getInstance(questId)!!.copy(scheduledDate = "2026-08-14"),
        )
        service.tryAutoComplete("2026-08-14")
        assertEquals(QuestStatus.AVAILABLE.name, db.questDao().getInstance(questId)!!.status)
    }

    @Test
    fun p_weeklyReview_ignoresDisabledModuleStaleQuests() = runTest {
        for ((k, v) in ModuleFlags.encode(EnabledModules(career = false, workout = true, diet = false))) {
            db.configDao().upsert(com.example.solo_levelling.data.db.entity.UserConfigEntity(k, v))
        }
        val reviewId = seedWeeklyReview()
        seedRangedQuest("journal", "", QuestStatus.COMPLETED.name, "2026-08-15")
        seedRangedQuest("dsa_daily", "module_career", QuestStatus.AVAILABLE.name, "2026-08-15")
        val review = db.questDao().getInstance(reviewId)!!
        assertTrue(service.isSatisfied(review, "2026-08-16"))
    }

    @Test
    fun n_weeklyReview_blockedByIncompleteActiveModuleQuest() = runTest {
        for ((k, v) in ModuleFlags.encode(EnabledModules(career = true, workout = false, diet = false))) {
            db.configDao().upsert(com.example.solo_levelling.data.db.entity.UserConfigEntity(k, v))
        }
        val reviewId = seedWeeklyReview()
        seedRangedQuest("dsa_daily", "module_career", QuestStatus.AVAILABLE.name, "2026-08-15")
        val review = db.questDao().getInstance(reviewId)!!
        assertFalse(service.isSatisfied(review, "2026-08-16"))
    }

    private suspend fun seedWeeklyReview(): Long {
        val templateId = db.questDao().upsertTemplate(
            QuestTemplateEntity(
                key = "weekly_review",
                type = "WEEKLY",
                title = "Weekly review",
                baseXp = 50,
                attributeRewardsJson = "{}",
                verificationType = VerificationType.AUTOMATIC.name,
            ),
        )
        return db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = templateId,
                scheduledDate = "2026-08-16",
                status = QuestStatus.AVAILABLE.name,
                title = "Weekly review",
                type = "WEEKLY",
                baseXp = 50,
                attributeRewardsJson = "{}",
                verificationType = VerificationType.AUTOMATIC.name,
            ),
        )
    }

    private suspend fun seedRangedQuest(
        key: String,
        tags: String,
        status: String,
        scheduledDate: String,
    ) {
        val templateId = db.questDao().upsertTemplate(
            QuestTemplateEntity(
                key = key,
                type = "DAILY",
                title = key,
                baseXp = 20,
                attributeRewardsJson = "{}",
                verificationType = VerificationType.MANUAL.name,
                priorityTags = tags,
            ),
        )
        db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = templateId,
                scheduledDate = scheduledDate,
                status = status,
                title = key,
                type = "DAILY",
                baseXp = 20,
                attributeRewardsJson = "{}",
                verificationType = VerificationType.MANUAL.name,
            ),
        )
    }
}
