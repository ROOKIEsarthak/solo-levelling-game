package com.example.solo_levelling.domain.service

import com.example.solo_levelling.core.time.FakeAppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.XpLedgerEntryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class SeasonRebuildIsolationTest {
    private lateinit var db: JsonDatabase
    private lateinit var clock: FakeAppClock
    private lateinit var service: SeasonService
    private lateinit var tempDir: java.nio.file.Path
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        tempDir = Files.createTempDirectory("season-iso-")
        db = JsonDatabase(tempDir.toFile())
        clock = FakeAppClock(
            epochMs = LocalDate.of(2026, 8, 18)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli() + 12L * 60 * 60 * 1000,
            fixedDate = LocalDate.of(2026, 8, 18),
        )
        service = SeasonService(db, clock)
    }

    @After
    fun tearDown() {
        db.close()
        tempDir.toFile().deleteRecursively()
        Dispatchers.resetMain()
    }

    private suspend fun seedProfile() {
        db.playerDao().upsertProfile(
            PlayerProfileEntity(
                name = "Test",
                timezone = "UTC",
                onboardingDone = true,
                createdAtEpochMs = clock.nowEpochMs(),
            ),
        )
    }

    @Test
    fun r_rebuildFromLedger_dropsCareerXpWhenCareerOff() = runTest {
        seedProfile()
        service.ensureActiveSeason()
        db.xpDao().insertLedger(
            XpLedgerEntryEntity(
                amount = 40,
                sourceType = "DSA",
                sourceId = "dsa_1",
                metadataJson = "{}",
                createdAtEpochMs = clock.nowEpochMs(),
            ),
        )
        db.xpDao().insertLedger(
            XpLedgerEntryEntity(
                amount = 50,
                sourceType = "WORKOUT",
                sourceId = "w_1",
                metadataJson = "{}",
                createdAtEpochMs = clock.nowEpochMs(),
            ),
        )
        service.addSeasonXp(90)
        assertEquals(90, db.moduleDao().getActiveSeason()!!.seasonXp)
        service.rebuildFromLedger(EnabledModules(career = false, workout = true, diet = false))
        assertEquals(50, db.moduleDao().getActiveSeason()!!.seasonXp)
    }

    @Test
    fun p_rebuildFromLedger_restoresCareerWhenReenabled() = runTest {
        seedProfile()
        service.ensureActiveSeason()
        db.xpDao().insertLedger(
            XpLedgerEntryEntity(
                amount = 40,
                sourceType = "DSA",
                sourceId = "dsa_1",
                metadataJson = "{}",
                createdAtEpochMs = clock.nowEpochMs(),
            ),
        )
        service.rebuildFromLedger(EnabledModules(career = false, workout = true, diet = false))
        assertEquals(0, db.moduleDao().getActiveSeason()!!.seasonXp)
        service.rebuildFromLedger(EnabledModules(career = true, workout = true, diet = false))
        assertEquals(40, db.moduleDao().getActiveSeason()!!.seasonXp)
    }

    @Test
    fun n_rebuildFromLedger_noActiveSeasonIsNoOp() = runTest {
        seedProfile()
        service.rebuildFromLedger(EnabledModules(career = true, workout = true, diet = true))
        assertEquals(null, db.moduleDao().getActiveSeason())
    }
}
