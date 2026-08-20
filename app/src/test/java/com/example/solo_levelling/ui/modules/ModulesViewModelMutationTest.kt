package com.example.solo_levelling.ui.modules

import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.FakeAppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.AttributeStatEntity
import com.example.solo_levelling.data.db.entity.DsaProblemEntity
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.domain.model.AttributeCode
import com.example.solo_levelling.domain.port.LocalMetricIngest
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
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class ModulesViewModelMutationTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var db: JsonDatabase
    private lateinit var clock: FakeAppClock
    private lateinit var vm: ModulesViewModel
    private lateinit var tempDir: java.nio.file.Path

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        tempDir = Files.createTempDirectory("modules-vm-")
        db = JsonDatabase(tempDir.toFile())
        clock = FakeAppClock(fixedDate = LocalDate.of(2026, 8, 15))
        val eventBus = EventBus()
        val progression = ProgressionService(db, eventBus, clock)
        val questCompletion = QuestCompletionService(db, eventBus, clock, progression)
        val verification = QuestVerificationService(db, clock, questCompletion)
        val modules = ModuleService(db, eventBus, clock, progression, verification)
        val metricIngest = LocalMetricIngest(db, clock)
        vm = ModulesViewModel(db, clock, modules, metricIngest)
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
    fun p_logFocus_persistsSession() = runTest(dispatcher) {
        seedProfile()
        vm.logFocus(25, "Deep Work")
        val date = clock.today(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_LOCAL_DATE)
        assertTrue(db.moduleDao().sumFocusMinutes(date) >= 25)
    }

    @Test
    fun p_saveJournal_persistsEntry() = runTest(dispatcher) {
        seedProfile()
        vm.saveJournal("Shipped mutation coupling")
        val date = clock.today(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_LOCAL_DATE)
        assertEquals("Shipped mutation coupling", db.moduleDao().getJournal(date)?.content)
    }

    @Test
    fun p_ingestMetric_logsSteps() = runTest(dispatcher) {
        seedProfile()
        vm.ingestMetric("STEPS", 9000f)
        val logs = db.moduleDao().recentMetrics("STEPS", 5)
        assertEquals(1, logs.size)
        assertEquals(9000f, logs.first().value)
    }

    @Test
    fun p_addDsaProblem_createsRecord() = runTest(dispatcher) {
        seedProfile()
        vm.addDsaProblem("Two Sum", "MEDIUM", "Arrays")
        assertTrue(db.moduleDao().getDsaProblems().any { it.title == "Two Sum" })
    }

    @Test
    fun p_markAttempted_updatesStatus() = runTest(dispatcher) {
        seedProfile()
        val id = db.moduleDao().upsertDsa(
            DsaProblemEntity(title = "LRU", externalId = "lru", status = "NOT_STARTED"),
        )
        vm.markAttempted(id)
        assertEquals("ATTEMPTED", db.moduleDao().getDsa(id)?.status)
    }
}
