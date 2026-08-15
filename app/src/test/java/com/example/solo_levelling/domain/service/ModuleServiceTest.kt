package com.example.solo_levelling.domain.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.FakeAppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.AttributeStatEntity
import com.example.solo_levelling.data.db.entity.CareerNodeEntity
import com.example.solo_levelling.data.db.entity.DsaProblemEntity
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.domain.model.AttributeCode
import com.example.solo_levelling.domain.service.ProgressionService.AwardResult
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
class ModuleServiceTest {
    private lateinit var db: JsonDatabase
    private lateinit var eventBus: EventBus
    private lateinit var clock: FakeAppClock
    private lateinit var progression: ProgressionService
    private lateinit var verification: QuestVerificationService
    private lateinit var service: ModuleService

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = JsonDatabase(File(context.cacheDir, "test-db-${System.nanoTime()}").also { it.mkdirs() })
        eventBus = EventBus()
        clock = FakeAppClock(fixedDate = LocalDate.of(2026, 8, 15))
        progression = ProgressionService(db, eventBus, clock)
        val questCompletion = QuestCompletionService(db, eventBus, clock, progression)
        verification = QuestVerificationService(db, clock, questCompletion)
        service = ModuleService(db, eventBus, clock, progression, verification)
    }

    @After
    fun tearDown() {
        db.close()
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
        db.playerDao().upsertAttributes(
            AttributeCode.entries.map { AttributeStatEntity(it.name) },
        )
    }

    @Test
    fun p_solveDsa_awardsXpOnce() = runTest {
        seedProfile()
        val id = db.moduleDao().upsertDsa(
            DsaProblemEntity(title = "Two Sum", externalId = "two_sum", status = "ATTEMPTED"),
        )
        service.solveDsa(id)
        assertEquals(25, db.playerDao().getProfile(1)!!.totalXp)
        assertEquals("SOLVED", db.moduleDao().getDsa(id)!!.status)
    }

    @Test
    fun n_solveDsaTwice_noDoubleXp() = runTest {
        seedProfile()
        val id = db.moduleDao().upsertDsa(
            DsaProblemEntity(title = "Two Sum", externalId = "two_sum", status = "ATTEMPTED"),
        )
        service.solveDsa(id)
        service.solveDsa(id)
        assertEquals(25, db.playerDao().getProfile(1)!!.totalXp)
        assertEquals(1, db.xpDao().getAllLedger().count { it.sourceType == "DSA" })
    }

    @Test
    fun e_advanceCareerNode_unlocksNext() = runTest {
        seedProfile()
        val firstId = db.moduleDao().upsertCareerNode(
            CareerNodeEntity(track = "DSA", title = "Arrays", orderIndex = 1, status = "PRACTICED"),
        )
        val secondId = db.moduleDao().upsertCareerNode(
            CareerNodeEntity(track = "DSA", title = "Trees", orderIndex = 2, status = "LOCKED"),
        )
        service.advanceCareerNode(firstId)
        assertEquals("MASTERED", db.moduleDao().getCareerNode(firstId)!!.status)
        assertEquals("STARTED", db.moduleDao().getCareerNode(secondId)!!.status)
    }

    @Test
    fun p_logNutrition_persists() = runTest {
        seedProfile()
        service.logNutrition(2100, 150, 200, 60)
        val log = db.moduleDao().getNutrition("2026-08-15")
        assertNotNull(log)
        assertEquals(2100, log!!.calories)
        assertEquals(150, log.protein)
        assertTrue(progression.award("NUTRITION", "nutrition_2026-08-15", 15) is AwardResult.AlreadyAwarded)
    }
}
