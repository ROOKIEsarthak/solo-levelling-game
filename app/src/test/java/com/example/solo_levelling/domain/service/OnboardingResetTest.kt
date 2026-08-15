package com.example.solo_levelling.domain.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.solo_levelling.core.config.SystemDefaults
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
class OnboardingResetTest {
    private lateinit var db: JsonDatabase
    private lateinit var clock: FakeAppClock
    private lateinit var onboarding: OnboardingService

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = JsonDatabase(File(context.cacheDir, "test-db-${System.nanoTime()}").also { it.mkdirs() })
        clock = FakeAppClock(fixedDate = LocalDate.of(2026, 8, 15))
        val questGen = QuestGenerationService(db, clock, EventBus(), AdaptiveService(db, clock), kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined))
        onboarding = OnboardingService(db, clock, questGen)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun p_resetAllProgress_clearsXpAndKeepsUserIdentity() = runTest {
        onboarding.ensureSeeded()
        onboarding.completeOnboarding("Hunter", listOf("career"))
        db.playerDao().upsertProfile(
            db.playerDao().getProfile(SystemDefaults.PLAYER_ID)!!.copy(totalXp = 500, level = 3, rank = "E"),
        )
        db.xpDao().insertLedger(
            XpLedgerEntryEntity(
                amount = 500,
                sourceType = "TEST",
                sourceId = "1",
                createdAtEpochMs = clock.nowEpochMs(),
            ),
        )
        db.playerDao().upsertAttribute(AttributeStatEntity(AttributeCode.INT.name, 40, 40))

        onboarding.resetAllProgress()

        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID)!!
        assertEquals(0, profile.totalXp)
        assertEquals(1, profile.level)
        assertEquals("Hunter", profile.name)
        assertTrue(profile.onboardingDone)
        assertEquals("career", profile.prioritiesCsv)
        assertEquals(0, db.xpDao().sumXp())
        assertTrue(db.playerDao().getAttributes().all { it.currentValue == 0 })
        assertTrue(db.achievementDao().getDefs().isNotEmpty())
    }

    @Test
    fun n_resetAllProgress_whenAlreadyEmpty_stillSeedsBaseline() = runTest {
        onboarding.resetAllProgress()
        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
        assertTrue(profile != null)
        assertEquals(0, profile!!.totalXp)
    }

    @Test
    fun e_resetAllProgress_clearsStreakAndUnlockedAchievements() = runTest {
        onboarding.ensureSeeded()
        onboarding.completeOnboarding("Hunter", listOf("fitness"))
        db.playerDao().upsertStreak(
            com.example.solo_levelling.data.db.entity.StreakStateEntity(
                current = 10,
                best = 10,
                lastCompletedDate = "2026-08-15",
            ),
        )
        db.achievementDao().unlock(
            com.example.solo_levelling.data.db.entity.PlayerAchievementEntity(
                "FIRST_QUEST",
                clock.nowEpochMs(),
            ),
        )

        onboarding.resetAllProgress()

        val streak = db.playerDao().getStreak(SystemDefaults.PLAYER_ID)!!
        assertEquals(0, streak.current)
        assertEquals(0, streak.best)
        assertTrue(db.achievementDao().getUnlocked().isEmpty())
    }
}
