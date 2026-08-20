package com.example.solo_levelling.domain.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.core.event.DomainEvent
import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.FakeAppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.AttributeStatEntity
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.QuestInstanceEntity
import com.example.solo_levelling.data.db.entity.QuestTemplateEntity
import com.example.solo_levelling.domain.model.AttributeCode
import com.example.solo_levelling.domain.model.QuestStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QuestCompletionServiceTest {
    private lateinit var db: JsonDatabase
    private lateinit var eventBus: EventBus
    private lateinit var clock: FakeAppClock
    private lateinit var progression: ProgressionService
    private lateinit var service: QuestCompletionService

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = JsonDatabase(File(context.cacheDir, "test-db-${System.nanoTime()}").also { it.mkdirs() })
        eventBus = EventBus()
        clock = FakeAppClock(
            epochMs = LocalDate.of(2026, 8, 15).atStartOfDay(java.time.ZoneOffset.UTC)
                .toInstant().toEpochMilli() + 3_600_000,
            fixedDate = LocalDate.of(2026, 8, 15),
        )
        progression = ProgressionService(db, eventBus, clock)
        service = QuestCompletionService(
            db,
            eventBus,
            clock,
            progression,
            MilestoneVerificationService(db, progression),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seedPlayer() {
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

    private suspend fun seedInstance(xp: Int = 40): Long {
        seedPlayer()
        return db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = 1,
                scheduledDate = "2026-08-15",
                status = QuestStatus.AVAILABLE.name,
                title = "Solve 2 DSA",
                type = "DAILY",
                baseXp = xp,
                attributeRewardsJson = """{"INT":30,"DISC":10}""",
            ),
        )
    }

    @Test
    fun p_complete_awardsXpOnceAndUpdatesProjections() = runTest {
        val id = seedInstance(40)
        val events = mutableListOf<DomainEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            eventBus.events.collect { events += it }
        }

        val result = service.complete(id)
        assertTrue(result is QuestCompletionService.Result.Completed)
        val completed = result as QuestCompletionService.Result.Completed
        assertEquals(40, completed.xp)
        assertEquals(40, completed.newTotalXp)

        val profile = db.playerDao().getProfile(1)!!
        assertEquals(40, profile.totalXp)
        assertEquals(1, db.xpDao().getAllLedger().size)

        val attrs = db.playerDao().getAttributes().associateBy { it.code }
        assertEquals(30, attrs["INT"]!!.currentValue)
        assertEquals(10, attrs["DISC"]!!.currentValue)
        assertTrue(events.any { it is DomainEvent.QuestCompleted })
        assertTrue(events.any { it is DomainEvent.XpAwarded })
    }

    @Test
    fun e_complete_isIdempotent() = runTest {
        val id = seedInstance(40)
        val first = service.complete(id)
        val second = service.complete(id)
        assertTrue(first is QuestCompletionService.Result.Completed)
        assertEquals(QuestCompletionService.Result.AlreadyCompleted, second)
        assertEquals(1, db.xpDao().getAllLedger().size)
        assertEquals(40, db.playerDao().getProfile(1)!!.totalXp)
    }

    @Test
    fun n_complete_missingInstanceReturnsNotFound() = runTest {
        assertEquals(QuestCompletionService.Result.NotFound, service.complete(999))
    }

    @Test
    fun p_undo_reversesXpWithinWindow() = runTest {
        val id = seedInstance(40)
        service.complete(id)
        val undone = service.undo(id)
        assertTrue(undone)
        assertEquals(0, db.playerDao().getProfile(1)!!.totalXp)
        assertEquals(QuestStatus.AVAILABLE.name, db.questDao().getInstance(id)!!.status)
        assertEquals(2, db.xpDao().getAllLedger().size)
    }

    @Test
    fun p_complete_afterUndo_awardsAgain() = runTest {
        val id = seedInstance(40)
        assertTrue(service.complete(id) is QuestCompletionService.Result.Completed)
        assertTrue(service.undo(id))
        val again = service.complete(id)
        assertTrue(again is QuestCompletionService.Result.Completed)
        assertEquals(40, (again as QuestCompletionService.Result.Completed).xp)
        assertEquals(40, db.playerDao().getProfile(1)!!.totalXp)
        assertEquals(QuestStatus.COMPLETED.name, db.questDao().getInstance(id)!!.status)
        assertTrue(service.undo(id))
        assertEquals(0, db.playerDao().getProfile(1)!!.totalXp)
        assertEquals(QuestStatus.AVAILABLE.name, db.questDao().getInstance(id)!!.status)
    }

    @Test
    fun e_dailyCap_freesAfterUndo() = runTest {
        val id = seedInstance(40)
        db.xpDao().insertLedger(
            com.example.solo_levelling.data.db.entity.XpLedgerEntryEntity(
                amount = 500,
                sourceType = "OTHER",
                sourceId = "fill",
                createdAtEpochMs = clock.nowEpochMs(),
            ),
        )
        assertEquals(QuestCompletionService.Result.DailyCapReached, service.complete(id))
        db.xpDao().insertLedger(
            com.example.solo_levelling.data.db.entity.XpLedgerEntryEntity(
                amount = -500,
                sourceType = "OTHER_UNDO",
                sourceId = "fill_undo",
                createdAtEpochMs = clock.nowEpochMs(),
            ),
        )
        val result = service.complete(id)
        assertTrue(result is QuestCompletionService.Result.Completed)
    }

    @Test
    fun p_undoPenaltyXp_zeroPercentIsZero() {
        assertEquals(0, SystemDefaults.undoPenaltyXp(40, 0))
        assertEquals(0, SystemDefaults.undoPenaltyXp(40))
    }

    @Test
    fun p_undoPenaltyXp_tenPercentRoundsDownWithMinOne() {
        assertEquals(4, SystemDefaults.undoPenaltyXp(40, 10))
        assertEquals(1, SystemDefaults.undoPenaltyXp(1, 10))
        assertEquals(0, SystemDefaults.undoPenaltyXp(0, 10))
        assertEquals(0, SystemDefaults.undoPenaltyXp(40, -5))
    }

    @Test
    fun n_undo_secondCallDoesNotDeductAgain() = runTest {
        val id = seedInstance(40)
        service.complete(id)
        assertTrue(service.undo(id))
        assertEquals(false, service.undo(id))
        assertEquals(0, db.playerDao().getProfile(1)!!.totalXp)
        assertEquals(2, db.xpDao().getAllLedger().size)
        assertEquals(0, db.xpDao().getAllLedger().count { it.sourceType == "QUEST_UNDO_PENALTY" })
    }

    @Test
    fun p_undo_penaltyPercentAppliesOnce() = runTest {
        val id = seedInstance(40)
        service.complete(id)
        assertTrue(service.undo(id, penaltyPercent = 10))
        assertEquals(0, db.playerDao().getProfile(1)!!.totalXp)
        assertEquals(1, db.xpDao().getAllLedger().count { it.sourceType == "QUEST_UNDO_PENALTY" })
        assertEquals(-4, db.xpDao().getAllLedger().first { it.sourceType == "QUEST_UNDO_PENALTY" }.amount)
        assertEquals(false, service.undo(id, penaltyPercent = 10))
        assertEquals(1, db.xpDao().getAllLedger().count { it.sourceType == "QUEST_UNDO_PENALTY" })
    }

    @Test
    fun e_undo_ignoreWindow_allowsDataInvalidation() = runTest {
        val id = seedInstance(40)
        service.complete(id)
        clock.epochMs += 16 * 60_000L
        assertEquals(false, service.undo(id))
        assertEquals(40, db.playerDao().getProfile(1)!!.totalXp)
        assertTrue(service.undo(id, ignoreWindow = true))
        assertEquals(QuestStatus.AVAILABLE.name, db.questDao().getInstance(id)!!.status)
        assertEquals(0, db.playerDao().getProfile(1)!!.totalXp)
    }

    private suspend fun seedMilestone(requirementStatus: String): Pair<Long, Long> {
        seedPlayer()
        val workoutId = db.questDao().upsertTemplate(
            QuestTemplateEntity(
                key = "workout_daily",
                type = "DAILY",
                title = "Complete workout",
                baseXp = 50,
                attributeRewardsJson = "{}",
                priorityTags = "module_workout",
            ),
        )
        val reqId = db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = workoutId,
                scheduledDate = "2026-08-15",
                status = requirementStatus,
                title = "Complete workout",
                type = "DAILY",
                baseXp = 50,
                attributeRewardsJson = "{}",
            ),
        )
        val milestoneTpl = db.questDao().upsertTemplate(
            QuestTemplateEntity(
                key = "first_week_complete",
                type = "MILESTONE",
                title = "First week complete",
                baseXp = 150,
                attributeRewardsJson = """{"DISC":80,"WIS":70}""",
            ),
        )
        val milestoneId = db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = milestoneTpl,
                scheduledDate = "2026-08-15",
                status = QuestStatus.AVAILABLE.name,
                title = "First week complete",
                type = "MILESTONE",
                baseXp = 150,
                attributeRewardsJson = """{"DISC":80,"WIS":70}""",
            ),
        )
        return milestoneId to reqId
    }

    @Test
    fun n_completeMilestone_whenRequirementsIncomplete_doesNotPersist() = runTest {
        val (milestoneId, _) = seedMilestone(QuestStatus.AVAILABLE.name)
        val result = service.complete(milestoneId)
        assertTrue(result is QuestCompletionService.Result.RequirementsIncomplete)
        assertEquals(QuestStatus.AVAILABLE.name, db.questDao().getInstance(milestoneId)!!.status)
        assertEquals(0, db.xpDao().getAllLedger().size)
    }

    @Test
    fun p_completeMilestone_whenRequirementsComplete_persistsAndAwardsOnce() = runTest {
        val (milestoneId, _) = seedMilestone(QuestStatus.COMPLETED.name)
        val first = service.complete(milestoneId)
        assertTrue(first is QuestCompletionService.Result.Completed)
        assertEquals(QuestStatus.COMPLETED.name, db.questDao().getInstance(milestoneId)!!.status)
        val xpRows = db.xpDao().getAllLedger().count { it.sourceType == "QUEST_INSTANCE" }
        assertEquals(1, xpRows)

        val second = service.complete(milestoneId)
        assertEquals(QuestCompletionService.Result.AlreadyCompleted, second)
        assertEquals(1, db.xpDao().getAllLedger().count { it.sourceType == "QUEST_INSTANCE" })
    }

    @Test
    fun r_completeMilestone_hasNoUndo() = runTest {
        val (milestoneId, _) = seedMilestone(QuestStatus.COMPLETED.name)
        assertTrue(service.complete(milestoneId) is QuestCompletionService.Result.Completed)
        assertFalse(service.undo(milestoneId))
        assertEquals(QuestStatus.COMPLETED.name, db.questDao().getInstance(milestoneId)!!.status)
        assertEquals(1, db.xpDao().getAllLedger().count { it.sourceType == "QUEST_INSTANCE" })
    }

    @Test
    fun n_complete_yesterdayDaily_returnsWrongDay() = runTest {
        seedPlayer()
        val id = db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = 1,
                scheduledDate = "2026-08-14",
                status = QuestStatus.AVAILABLE.name,
                title = "Yesterday workout",
                type = "DAILY",
                baseXp = 40,
                attributeRewardsJson = "{}",
            ),
        )
        assertEquals(QuestCompletionService.Result.WrongDay, service.complete(id))
        assertEquals(QuestStatus.AVAILABLE.name, db.questDao().getInstance(id)!!.status)
        assertEquals(0, db.playerDao().getProfile(1)!!.totalXp)
    }

    @Test
    fun n_complete_tomorrowDaily_returnsWrongDay() = runTest {
        seedPlayer()
        val id = db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = 1,
                scheduledDate = "2026-08-16",
                status = QuestStatus.AVAILABLE.name,
                title = "Tomorrow workout",
                type = "DAILY",
                baseXp = 40,
                attributeRewardsJson = "{}",
            ),
        )
        assertEquals(QuestCompletionService.Result.WrongDay, service.complete(id))
        assertEquals(0, db.playerDao().getProfile(1)!!.totalXp)
    }

    @Test
    fun p_complete_weeklyQuest_allowedDuringSameWeek() = runTest {
        seedPlayer()
        val id = db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = 1,
                scheduledDate = "2026-08-16",
                status = QuestStatus.AVAILABLE.name,
                title = "Weekly review",
                type = "WEEKLY",
                baseXp = 80,
                attributeRewardsJson = "{}",
            ),
        )
        val result = service.complete(id)
        assertTrue(result is QuestCompletionService.Result.Completed)
        assertEquals(80, (result as QuestCompletionService.Result.Completed).xp)
    }
}
