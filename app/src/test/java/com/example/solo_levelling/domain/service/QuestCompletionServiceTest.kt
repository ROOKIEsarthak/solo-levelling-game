package com.example.solo_levelling.domain.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.solo_levelling.core.event.DomainEvent
import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.FakeAppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.AttributeStatEntity
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.QuestInstanceEntity
import com.example.solo_levelling.domain.model.AttributeCode
import com.example.solo_levelling.domain.model.QuestStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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
        clock = FakeAppClock(fixedDate = LocalDate.of(2026, 8, 15))
        progression = ProgressionService(db, eventBus, clock)
        service = QuestCompletionService(db, eventBus, clock, progression)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seedInstance(xp: Int = 40): Long {
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
    fun n_undo_outsideWindowFails() = runTest {
        val id = seedInstance(40)
        service.complete(id)
        clock.epochMs += 16 * 60_000L
        assertEquals(false, service.undo(id))
        assertEquals(40, db.playerDao().getProfile(1)!!.totalXp)
    }
}
