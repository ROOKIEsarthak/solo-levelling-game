package com.example.solo_levelling.domain.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.FakeAppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.AttributeStatEntity
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.XpLedgerEntryEntity
import com.example.solo_levelling.domain.model.AttributeCode
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProgressionServiceTest {
    private lateinit var db: JsonDatabase
    private lateinit var eventBus: EventBus
    private lateinit var clock: FakeAppClock
    private lateinit var service: ProgressionService

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = JsonDatabase(File(context.cacheDir, "test-db-${System.nanoTime()}").also { it.mkdirs() })
        eventBus = EventBus()
        clock = FakeAppClock(
            epochMs = java.time.LocalDate.of(2026, 8, 15)
                .atStartOfDay(java.time.ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli() + 12L * 60 * 60 * 1000,
            fixedDate = LocalDate.of(2026, 8, 15),
        )
        service = ProgressionService(db, eventBus, clock)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seedProfile(totalXp: Int = 0) {
        db.playerDao().upsertProfile(
            PlayerProfileEntity(
                name = "Test",
                timezone = "UTC",
                totalXp = totalXp,
                onboardingDone = true,
                createdAtEpochMs = clock.nowEpochMs(),
            ),
        )
        db.playerDao().upsertAttributes(
            AttributeCode.entries.map { AttributeStatEntity(it.name) },
        )
    }

    @Test
    fun p_award_updatesLedgerAndProfile() = runTest {
        seedProfile()
        val result = service.award(
            sourceType = "TEST",
            sourceId = "test_1",
            amount = 50,
            attrs = mapOf(AttributeCode.INT to 10),
        )
        assertTrue(result is ProgressionService.AwardResult.Success)
        val success = result as ProgressionService.AwardResult.Success
        assertEquals(50, success.awarded)
        assertEquals(50, success.newTotal)
        assertEquals(50, db.playerDao().getProfile(1)!!.totalXp)
        assertEquals(1, db.xpDao().getAllLedger().size)
        assertEquals(10, db.playerDao().getAttributes().first { it.code == "INT" }.currentValue)
    }

    @Test
    fun n_duplicateSource_returnsAlreadyAwarded() = runTest {
        seedProfile()
        service.award("TEST", "dup_1", 30)
        val second = service.award("TEST", "dup_1", 30)
        assertEquals(ProgressionService.AwardResult.AlreadyAwarded, second)
        assertEquals(1, db.xpDao().getAllLedger().size)
        assertEquals(30, db.playerDao().getProfile(1)!!.totalXp)
    }

    @Test
    fun e_rebuildFromLedger_fixesCorruptedTotalXp() = runTest {
        seedProfile(totalXp = 999)
        val now = clock.nowEpochMs()
        db.xpDao().insertLedger(
            XpLedgerEntryEntity(amount = 40, sourceType = "A", sourceId = "a1", createdAtEpochMs = now),
        )
        db.xpDao().insertLedger(
            XpLedgerEntryEntity(amount = 60, sourceType = "B", sourceId = "b1", createdAtEpochMs = now + 1),
        )

        val result = service.rebuildFromLedger()
        assertEquals(999, result.oldTotal)
        assertEquals(100, result.newTotal)
        assertEquals(100, db.playerDao().getProfile(1)!!.totalXp)
    }

    @Test
    fun n_dailyCap_returnsCapReached() = runTest {
        seedProfile()
        val now = clock.nowEpochMs()
        db.xpDao().insertLedger(
            XpLedgerEntryEntity(
                amount = com.example.solo_levelling.core.config.SystemDefaults.DAILY_XP_CAP,
                sourceType = "FILL",
                sourceId = "fill_1",
                createdAtEpochMs = now,
            ),
        )
        db.playerDao().upsertProfile(
            db.playerDao().getProfile(1)!!.copy(
                totalXp = com.example.solo_levelling.core.config.SystemDefaults.DAILY_XP_CAP,
            ),
        )

        val result = service.award("TEST", "cap_test", 10, applyDailyCap = true)
        assertEquals(ProgressionService.AwardResult.CapReached, result)
    }

    @Test
    fun e_dailyCap_awardsPartialUpToRemaining() = runTest {
        seedProfile()
        val cap = com.example.solo_levelling.core.config.SystemDefaults.DAILY_XP_CAP
        val now = clock.nowEpochMs()
        db.xpDao().insertLedger(
            XpLedgerEntryEntity(amount = cap - 20, sourceType = "FILL", sourceId = "fill_1", createdAtEpochMs = now),
        )
        db.playerDao().upsertProfile(
            db.playerDao().getProfile(1)!!.copy(totalXp = cap - 20),
        )

        val result = service.award("TEST", "partial_cap", 50, applyDailyCap = true)
        assertTrue(result is ProgressionService.AwardResult.Success)
        val success = result as ProgressionService.AwardResult.Success
        assertEquals(20, success.awarded)
        assertEquals(cap - 20 + 20, db.playerDao().getProfile(1)!!.totalXp)
    }

    @Test
    fun p_reverseThenReaward_sameSourceSucceeds() = runTest {
        seedProfile()
        val first = service.award("WORKOUT", "workout_2026-08-15", 40, mapOf(AttributeCode.STR to 30))
        assertTrue(first is ProgressionService.AwardResult.Success)
        val original = db.xpDao().findBySource("WORKOUT", "workout_2026-08-15")!!
        assertTrue(
            service.reverse(
                originalSourceType = "WORKOUT",
                originalSourceId = "workout_2026-08-15",
                reverseSourceType = "WORKOUT_UNDO",
                reverseSourceId = "UNDO_WORKOUT_${original.id}",
                attrs = mapOf(AttributeCode.STR to 30),
            ),
        )
        val second = service.award("WORKOUT", "workout_2026-08-15", 40, mapOf(AttributeCode.STR to 30))
        assertTrue(second is ProgressionService.AwardResult.Success)
        assertEquals(40, (second as ProgressionService.AwardResult.Success).awarded)
        assertEquals(40, db.playerDao().getProfile(1)!!.totalXp)
        assertEquals(30, db.playerDao().getAttributes().first { it.code == "STR" }.currentValue)
    }

    @Test
    fun n_reverseTwice_isIdempotent() = runTest {
        seedProfile()
        service.award("TEST", "once", 20)
        assertTrue(service.reverse("TEST", "once", "TEST_UNDO", "UNDO_1"))
        assertEquals(false, service.reverse("TEST", "once", "TEST_UNDO", "UNDO_1"))
        assertEquals(0, db.playerDao().getProfile(1)!!.totalXp)
        assertEquals(2, db.xpDao().getAllLedger().size)
    }
}
