package com.example.solo_levelling.domain.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.FakeAppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.AttributeStatEntity
import com.example.solo_levelling.data.db.entity.DsaProblemEntity
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.domain.model.AttributeCode
import kotlinx.coroutines.runBlocking
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
class ModuleServiceExpandedTest {
    private lateinit var db: JsonDatabase
    private lateinit var clock: FakeAppClock
    private lateinit var service: ModuleService

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = JsonDatabase(File(context.cacheDir, "test-db-${System.nanoTime()}").also { it.mkdirs() })
        clock = FakeAppClock(fixedDate = LocalDate.of(2026, 8, 15))
        val eventBus = EventBus()
        val progression = ProgressionService(db, eventBus, clock)
        val questCompletion = QuestCompletionService(db, eventBus, clock, progression)
        val verification = QuestVerificationService(db, clock, questCompletion)
        service = ModuleService(db, eventBus, clock, progression, verification)
        runBlocking {
            db.playerDao().upsertProfile(
                PlayerProfileEntity(name = "Test", timezone = "UTC", onboardingDone = true),
            )
            db.playerDao().upsertAttributes(
                AttributeCode.entries.map { AttributeStatEntity(it.name) },
            )
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun p_solveDsa_setsReviewDueEpochMs() = runTest {
        val id = db.moduleDao().upsertDsa(
            DsaProblemEntity(title = "Two Sum", externalId = "two_sum", status = "ATTEMPTED"),
        )
        service.solveDsa(id)
        val problem = db.moduleDao().getDsa(id)!!
        assertNotNull(problem.reviewDueEpochMs)
        assertEquals(clock.nowEpochMs() + 3L * 24 * 60 * 60 * 1000, problem.reviewDueEpochMs!!)
    }

    @Test
    fun p_markSystemDesignConcept_masteredAwardsXp() = runTest {
        service.ensureCareerCatalogsSeeded()
        val topic = db.moduleDao().getSystemDesignTopics().first()
        val concept = topic.concepts.first()
        service.markSystemDesignConcept(topic.id, concept.id, "MASTERED")
        assertEquals(
            "MASTERED",
            db.moduleDao().getSystemDesignTopics().first { it.id == topic.id }
                .concepts.first { it.id == concept.id }.status,
        )
        assertTrue(db.xpDao().getAllLedger().any { it.sourceType == "SD_CONCEPT" })
    }

    @Test
    fun p_updateDsaNotes_persistsFields() = runTest {
        val id = db.moduleDao().upsertDsa(DsaProblemEntity(title = "A", externalId = "a"))
        service.updateDsaNotes(id, "note", "mistake", "approach")
        val problem = db.moduleDao().getDsa(id)!!
        assertEquals("note", problem.notes)
        assertEquals("mistake", problem.mistakes)
        assertEquals("approach", problem.approach)
    }
}
