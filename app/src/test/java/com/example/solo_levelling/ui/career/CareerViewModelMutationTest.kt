package com.example.solo_levelling.ui.career

import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.FakeAppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.AttributeStatEntity
import com.example.solo_levelling.data.db.entity.DsaProblemEntity
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.SystemDesignConceptEntity
import com.example.solo_levelling.data.db.entity.SystemDesignTopicEntity
import com.example.solo_levelling.domain.model.AttributeCode
import com.example.solo_levelling.domain.service.ModuleService
import com.example.solo_levelling.domain.service.ProgressionService
import com.example.solo_levelling.domain.service.QuestCompletionService
import com.example.solo_levelling.domain.service.QuestVerificationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class CareerViewModelMutationTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var db: JsonDatabase
    private lateinit var vm: CareerViewModel
    private lateinit var tempDir: java.nio.file.Path

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        tempDir = Files.createTempDirectory("career-vm-")
        db = JsonDatabase(tempDir.toFile())
        val clock = FakeAppClock(fixedDate = LocalDate.of(2026, 8, 15))
        val eventBus = EventBus()
        val progression = ProgressionService(db, eventBus, clock)
        val questCompletion = QuestCompletionService(db, eventBus, clock, progression)
        val verification = QuestVerificationService(db, clock, questCompletion)
        val modules = ModuleService(db, eventBus, clock, progression, verification)
        vm = CareerViewModel(db, clock, modules)
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
            ),
        )
        db.playerDao().upsertAttributes(
            AttributeCode.entries.map { AttributeStatEntity(it.name) },
        )
    }

    @Test
    fun p_solveDsa_marksSolved() = runTest(dispatcher) {
        seedProfile()
        val id = db.moduleDao().upsertDsa(
            DsaProblemEntity(title = "Graph", externalId = "graph", status = "ATTEMPTED"),
        )
        vm.solveDsa(id)
        assertEquals("SOLVED", db.moduleDao().getDsa(id)?.status)
    }

    @Test
    fun p_addDsaProblem_addsProblem() = runTest(dispatcher) {
        seedProfile()
        vm.addDsaProblem("Design Twitter", "HARD", "System")
        assertTrue(db.moduleDao().getDsaProblems().any { it.title == "Design Twitter" })
    }

    @Test
    fun p_markSystemDesignConcept_updatesTopic() = runTest(dispatcher) {
        seedProfile()
        db.moduleDao().upsertSystemDesignTopic(
            SystemDesignTopicEntity(
                id = "caching",
                title = "Caching",
                orderIndex = 1,
                concepts = listOf(
                    SystemDesignConceptEntity(id = "REDIS", title = "Redis", status = "NEW"),
                ),
            ),
        )
        vm.markSystemDesignConcept("caching", "REDIS", "STUDIED")
        val topic = db.moduleDao().getSystemDesignTopics().first { it.id == "caching" }
        assertEquals("STUDIED", topic.concepts.first { it.id == "REDIS" }.status)
    }
}
