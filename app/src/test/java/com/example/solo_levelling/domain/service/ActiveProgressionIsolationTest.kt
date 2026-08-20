package com.example.solo_levelling.domain.service

import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.FakeAppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.AttributeStatEntity
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.UserConfigEntity
import com.example.solo_levelling.domain.model.AttributeCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveProgressionIsolationTest {
    private lateinit var db: JsonDatabase
    private lateinit var progression: ProgressionService
    private lateinit var reader: ActiveProgressionReader
    private lateinit var clock: FakeAppClock
    private lateinit var tempDir: java.nio.file.Path
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        tempDir = Files.createTempDirectory("active-prog-")
        db = JsonDatabase(tempDir.toFile())
        clock = FakeAppClock(fixedDate = LocalDate.of(2026, 8, 15))
        val bus = EventBus()
        progression = ProgressionService(db, bus, clock)
        reader = ActiveProgressionReader(db)
    }

    @After
    fun tearDown() {
        db.close()
        tempDir.toFile().deleteRecursively()
        Dispatchers.resetMain()
    }

    private suspend fun seed() {
        db.playerDao().upsertProfile(
            PlayerProfileEntity(name = "T", timezone = "UTC", onboardingDone = true),
        )
        db.playerDao().upsertAttributes(AttributeCode.entries.map { AttributeStatEntity(it.name) })
        for ((k, v) in ModuleFlags.encode(EnabledModules(true, true, true))) {
            db.configDao().upsert(UserConfigEntity(k, v))
        }
    }

    @Test
    fun p_disableWorkout_excludesXpAndAttributes() = runTest {
        seed()
        progression.award(
            "WORKOUT",
            "w1",
            40,
            mapOf(AttributeCode.STR to 30, AttributeCode.VIT to 10),
            metadataJson = """{"module":"WORKOUT"}""",
        )
        progression.award(
            "DSA",
            "d1",
            25,
            mapOf(AttributeCode.INT to 20),
            metadataJson = """{"module":"CAREER"}""",
        )
        assertEquals(65, db.playerDao().getProfile(1)!!.totalXp)
        assertEquals(30, db.playerDao().getAttributes().first { it.code == "STR" }.currentValue)

        val disabled = EnabledModules(career = true, workout = false, diet = false)
        progression.rebuildActiveFromLedger(disabled)
        assertEquals(25, db.playerDao().getProfile(1)!!.totalXp)
        assertEquals(0, db.playerDao().getAttributes().first { it.code == "STR" }.currentValue)
        assertEquals(20, db.playerDao().getAttributes().first { it.code == "INT" }.currentValue)

        val ledger = reader.activeLedger(disabled)
        assertTrue(ledger.none { it.sourceType == "WORKOUT" })
        assertTrue(ledger.any { it.sourceType == "DSA" })
    }

    @Test
    fun p_reenableWorkout_restoresHistoryWithoutLoss() = runTest {
        seed()
        progression.award(
            "WORKOUT",
            "w2",
            40,
            mapOf(AttributeCode.STR to 30),
            metadataJson = """{"module":"WORKOUT"}""",
        )
        progression.rebuildActiveFromLedger(EnabledModules(true, false, false))
        assertEquals(0, db.playerDao().getProfile(1)!!.totalXp)
        assertEquals(1, db.xpDao().getAllLedger().size)

        progression.rebuildActiveFromLedger(EnabledModules(true, true, false))
        assertEquals(40, db.playerDao().getProfile(1)!!.totalXp)
        assertEquals(30, db.playerDao().getAttributes().first { it.code == "STR" }.currentValue)
        assertEquals(1, db.xpDao().getAllLedger().size)
    }

    @Test
    fun p_activeExport_excludesWorkoutWhenDisabled() = runTest {
        seed()
        progression.award(
            "WORKOUT",
            "w3",
            40,
            mapOf(AttributeCode.STR to 30),
            metadataJson = """{"module":"WORKOUT"}""",
        )
        progression.award(
            "DSA",
            "d3",
            25,
            mapOf(AttributeCode.INT to 20),
            metadataJson = """{"module":"CAREER"}""",
        )
        for ((k, v) in ModuleFlags.encode(EnabledModules(true, false, false))) {
            db.configDao().upsert(UserConfigEntity(k, v))
        }
        progression.rebuildActiveFromLedger(EnabledModules(true, false, false))

        val activeModules = EnabledModules(true, false, false)
        val activeLedger = reader.activeLedger(activeModules)
        assertTrue(activeLedger.none { it.sourceType == "WORKOUT" })
        assertTrue(activeLedger.any { it.sourceType == "DSA" })

        val archiveLedger = db.xpDao().getAllLedger()
        assertTrue(archiveLedger.any { it.sourceType == "WORKOUT" })
        assertTrue(archiveLedger.any { it.sourceType == "DSA" })

        val activeAttrs = reader.activeAttributes(activeModules)
        assertEquals(0, activeAttrs.first { it.code == "STR" }.currentValue)
        assertEquals(20, activeAttrs.first { it.code == "INT" }.currentValue)
    }

    @Test
    fun n_filteringIsDomainLevel_notUiOnly() = runTest {
        seed()
        progression.award(
            "WORKOUT",
            "w4",
            10,
            mapOf(AttributeCode.STR to 5),
            metadataJson = """{"module":"WORKOUT"}""",
        )
        val disabled = EnabledModules(false, false, true)
        val active = reader.activeLedger(disabled)
        assertTrue(active.isEmpty())
        assertEquals(0, reader.sumAllowedAttributeDeltas(disabled)[AttributeCode.STR] ?: 0)
    }
}
