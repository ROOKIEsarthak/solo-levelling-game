package com.example.solo_levelling.domain.service

import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.FakeAppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.AttributeStatEntity
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.QuestInstanceEntity
import com.example.solo_levelling.data.db.entity.StreakStateEntity
import com.example.solo_levelling.domain.handler.AchievementHandler
import com.example.solo_levelling.domain.handler.BossProgressHandler
import com.example.solo_levelling.domain.handler.StreakHandler
import com.example.solo_levelling.domain.model.AttributeCode
import com.example.solo_levelling.domain.model.QuestStatus
import kotlinx.coroutines.CoroutineScope
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
class PostQuestCompletionCoordinatorTest {
    private lateinit var db: JsonDatabase
    private lateinit var questCompletion: QuestCompletionService
    private lateinit var season: SeasonService
    private lateinit var tempDir: java.nio.file.Path
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        tempDir = Files.createTempDirectory("post-quest-")
        db = JsonDatabase(tempDir.toFile())
        val bus = EventBus()
        val clock = FakeAppClock(fixedDate = LocalDate.of(2026, 8, 15))
        val progression = ProgressionService(db, bus, clock)
        season = SeasonService(db, clock)
        val scope = CoroutineScope(dispatcher)
        val streak = StreakHandler(db, bus, clock, scope)
        val boss = BossProgressHandler(db, bus, progression, scope)
        val achievements = AchievementHandler(db, bus, clock, progression, scope)
        val questGen = QuestGenerationService(db, clock, bus, scope = scope)
        val post = PostQuestCompletionCoordinator(streak, boss, achievements, questGen, season)
        questCompletion = QuestCompletionService(db, bus, clock, progression, null, post)
    }

    @After
    fun tearDown() {
        db.close()
        tempDir.toFile().deleteRecursively()
        Dispatchers.resetMain()
    }

    private suspend fun seedPlayer() {
        db.playerDao().upsertProfile(
            PlayerProfileEntity(name = "T", timezone = "UTC", onboardingDone = true),
        )
        db.playerDao().upsertAttributes(AttributeCode.entries.map { AttributeStatEntity(it.name) })
        db.playerDao().upsertStreak(StreakStateEntity(current = 1, best = 1, lastCompletedDate = "2026-08-14"))
        season.ensureActiveSeason()
    }

    @Test
    fun p_complete_updatesCriticalState() = runTest {
        seedPlayer()
        val id = db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = 1,
                scheduledDate = "2026-08-15",
                status = QuestStatus.AVAILABLE.name,
                title = "Q",
                type = "DAILY",
                baseXp = 25,
                attributeRewardsJson = "{}",
            ),
        )
        val result = questCompletion.complete(id)
        assertTrue(result is QuestCompletionService.Result.Completed)
        assertEquals(2, db.playerDao().getStreak(1)!!.current)
        assertEquals(25, season.ensureActiveSeason().seasonXp)
        assertEquals(QuestStatus.COMPLETED.name, db.questDao().getInstance(id)!!.status)
    }

    @Test
    fun p_retryComplete_doesNotDuplicateXp() = runTest {
        seedPlayer()
        val id = db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = 1,
                scheduledDate = "2026-08-15",
                status = QuestStatus.AVAILABLE.name,
                title = "Q",
                type = "DAILY",
                baseXp = 25,
                attributeRewardsJson = "{}",
            ),
        )
        questCompletion.complete(id)
        val xpAfter = db.playerDao().getProfile(1)!!.totalXp
        val streakAfter = db.playerDao().getStreak(1)!!.current
        assertEquals(QuestCompletionService.Result.AlreadyCompleted, questCompletion.complete(id))
        assertEquals(xpAfter, db.playerDao().getProfile(1)!!.totalXp)
        assertEquals(streakAfter, db.playerDao().getStreak(1)!!.current)
    }

    @Test
    fun e_completeWithoutCoordinator_stillCommitsQuest() = runTest {
        seedPlayer()
        val bus = EventBus()
        val clock = FakeAppClock(fixedDate = LocalDate.of(2026, 8, 15))
        val progression = ProgressionService(db, bus, clock)
        val bare = QuestCompletionService(db, bus, clock, progression)
        val id = db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = 1,
                scheduledDate = "2026-08-15",
                status = QuestStatus.AVAILABLE.name,
                title = "Bare",
                type = "DAILY",
                baseXp = 10,
                attributeRewardsJson = "{}",
            ),
        )
        assertTrue(bare.complete(id) is QuestCompletionService.Result.Completed)
        assertEquals(QuestStatus.COMPLETED.name, db.questDao().getInstance(id)!!.status)
    }
}
