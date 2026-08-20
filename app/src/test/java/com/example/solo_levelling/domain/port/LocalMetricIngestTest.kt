package com.example.solo_levelling.domain.port

import com.example.solo_levelling.core.time.FakeAppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
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
class LocalMetricIngestTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var db: JsonDatabase
    private lateinit var clock: FakeAppClock
    private lateinit var ingest: LocalMetricIngest

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        db = JsonDatabase(Files.createTempDirectory("metric-ingest-").toFile())
        clock = FakeAppClock(fixedDate = LocalDate.of(2026, 8, 15))
        ingest = LocalMetricIngest(db, clock)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private suspend fun seedProfile() {
        db.playerDao().upsertProfile(
            PlayerProfileEntity(name = "Test", timezone = "UTC", onboardingDone = true),
        )
    }

    @Test
    fun p_ingest_defaultsToToday() = runTest {
        seedProfile()
        ingest.ingest("WEIGHT", 70f)
        val logs = db.moduleDao().recentMetrics("WEIGHT", 10)
        assertEquals(1, logs.size)
        assertEquals("2026-08-15", logs.first().date)
    }

    @Test
    fun n_ingest_futureDate_isRejected() = runTest {
        seedProfile()
        ingest.ingest("WEIGHT", 70f, date = "2026-08-20")
        assertTrue(db.moduleDao().recentMetrics("WEIGHT", 10).isEmpty())
    }

    @Test
    fun n_ingest_pastDate_isRejected() = runTest {
        seedProfile()
        ingest.ingest("STEPS", 8000f, date = "2026-08-14")
        assertTrue(db.moduleDao().recentMetrics("STEPS", 10).isEmpty())
    }
}
