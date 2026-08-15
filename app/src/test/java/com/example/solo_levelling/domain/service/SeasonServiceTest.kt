package com.example.solo_levelling.domain.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.solo_levelling.core.time.FakeAppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SeasonServiceTest {
    private lateinit var db: JsonDatabase
    private lateinit var clock: FakeAppClock
    private lateinit var service: SeasonService

    @Before
    fun setup() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = JsonDatabase(File(context.cacheDir, "test-db-${System.nanoTime()}").also { it.mkdirs() })
        clock = FakeAppClock(fixedDate = LocalDate.of(2026, 8, 15))
        service = SeasonService(db, clock)
        db.playerDao().upsertProfile(
            PlayerProfileEntity(
                name = "Test",
                onboardingDone = true,
                createdAtEpochMs = clock.nowEpochMs(),
            ),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun p_ensureActiveSeason_createsOne() = runTest {
        val season = service.ensureActiveSeason()
        assertNotNull(season)
        assertEquals("ACTIVE", season.status)
        assertEquals(0, season.seasonXp)
        val again = service.ensureActiveSeason()
        assertEquals(season.id, again.id)
    }

    @Test
    fun p_addSeasonXp_accumulatesOnActiveSeason() = runTest {
        service.ensureActiveSeason()
        service.addSeasonXp(50)
        service.addSeasonXp(25)
        val active = db.moduleDao().getActiveSeason()
        assertNotNull(active)
        assertEquals(75, active!!.seasonXp)
    }
}
