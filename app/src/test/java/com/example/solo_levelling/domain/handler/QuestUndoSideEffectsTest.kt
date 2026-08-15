package com.example.solo_levelling.domain.handler

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.FakeAppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.AchievementDefEntity
import com.example.solo_levelling.data.db.entity.AttributeStatEntity
import com.example.solo_levelling.data.db.entity.BossEntity
import com.example.solo_levelling.data.db.entity.BossQuestEntity
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.QuestInstanceEntity
import com.example.solo_levelling.data.db.entity.QuestTemplateEntity
import com.example.solo_levelling.data.db.entity.StreakStateEntity
import com.example.solo_levelling.domain.model.AttributeCode
import com.example.solo_levelling.domain.model.QuestStatus
import com.example.solo_levelling.domain.service.ProgressionService
import com.example.solo_levelling.domain.service.QuestCompletionService
import com.example.solo_levelling.domain.service.QuestGenerationService
import com.example.solo_levelling.domain.service.SeasonService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QuestUndoSideEffectsTest {
    private lateinit var db: JsonDatabase
    private lateinit var eventBus: EventBus
    private lateinit var clock: FakeAppClock
    private lateinit var progression: ProgressionService
    private lateinit var questCompletion: QuestCompletionService
    private lateinit var seasonService: SeasonService

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = JsonDatabase(File(context.cacheDir, "test-db-${System.nanoTime()}").also { it.mkdirs() })
        eventBus = EventBus()
        clock = FakeAppClock(fixedDate = LocalDate.of(2026, 8, 15))
        progression = ProgressionService(db, eventBus, clock)
        questCompletion = QuestCompletionService(db, eventBus, clock, progression)
        seasonService = SeasonService(db, clock)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun TestScope.handlerScope(): CoroutineScope =
        CoroutineScope(UnconfinedTestDispatcher(testScheduler))

    private suspend fun seedPlayer() {
        db.playerDao().upsertProfile(
            PlayerProfileEntity(
                name = "Test",
                timezone = "UTC",
                onboardingDone = true,
                createdAtEpochMs = clock.nowEpochMs(),
            ),
        )
        db.playerDao().upsertAttributes(AttributeCode.entries.map { AttributeStatEntity(it.name) })
    }

    @Test
    fun p_undo_subtractsSeasonXp() = runTest {
        seedPlayer()
        SeasonHandler(eventBus, seasonService, handlerScope()).start()
        seasonService.ensureActiveSeason()

        val id = db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = 1,
                scheduledDate = "2026-08-15",
                status = QuestStatus.AVAILABLE.name,
                title = "Quest",
                type = "DAILY",
                baseXp = 40,
                attributeRewardsJson = "{}",
            ),
        )
        questCompletion.complete(id)
        assertEquals(40, db.moduleDao().getActiveSeason()!!.seasonXp)

        assertTrue(questCompletion.undo(id))
        assertEquals(0, db.moduleDao().getActiveSeason()!!.seasonXp)
    }

    @Test
    fun p_undo_relocksAvailableDependent() = runTest {
        seedPlayer()
        QuestGenerationService(db, clock, eventBus, scope = handlerScope()).start()

        val prereqId = db.questDao().upsertTemplate(
            QuestTemplateEntity(
                key = "prereq",
                type = "DAILY",
                title = "Prereq",
                baseXp = 20,
                attributeRewardsJson = "{}",
            ),
        )
        val depTemplateId = db.questDao().upsertTemplate(
            QuestTemplateEntity(
                key = "dep",
                type = "DAILY",
                title = "Dependent",
                baseXp = 20,
                attributeRewardsJson = "{}",
                dependsOnTemplateKey = "prereq",
            ),
        )
        val prereqInstance = db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = prereqId,
                scheduledDate = "2026-08-15",
                status = QuestStatus.AVAILABLE.name,
                title = "Prereq",
                type = "DAILY",
                baseXp = 20,
                attributeRewardsJson = "{}",
            ),
        )
        val depInstance = db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = depTemplateId,
                scheduledDate = "2026-08-15",
                status = QuestStatus.LOCKED.name,
                title = "Dependent",
                type = "DAILY",
                baseXp = 20,
                attributeRewardsJson = "{}",
            ),
        )

        questCompletion.complete(prereqInstance)
        assertEquals(QuestStatus.AVAILABLE.name, db.questDao().getInstance(depInstance)!!.status)

        assertTrue(questCompletion.undo(prereqInstance))
        assertEquals(QuestStatus.LOCKED.name, db.questDao().getInstance(depInstance)!!.status)
    }

    @Test
    fun p_undo_reversesBossProgressAndClearXp() = runTest {
        seedPlayer()
        BossProgressHandler(db, eventBus, progression, handlerScope()).start()

        val templateId = db.questDao().upsertTemplate(
            QuestTemplateEntity(
                key = "boss_q",
                type = "DAILY",
                title = "Boss quest",
                baseXp = 30,
                attributeRewardsJson = "{}",
            ),
        )
        val bossId = db.moduleDao().upsertBoss(
            BossEntity(title = "Boss", targetValue = 100f, xpReward = 100),
        )
        db.moduleDao().upsertBossQuest(
            BossQuestEntity(bossId = bossId, templateKey = "boss_q", weight = 1f),
        )
        val instanceId = db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = templateId,
                scheduledDate = "2026-08-15",
                status = QuestStatus.AVAILABLE.name,
                title = "Boss quest",
                type = "DAILY",
                baseXp = 30,
                attributeRewardsJson = "{}",
            ),
        )

        questCompletion.complete(instanceId)
        val bossAfter = db.moduleDao().getBosses().single { it.id == bossId }
        assertEquals("CLEARED", bossAfter.status)
        assertTrue(db.moduleDao().getBossQuests(bossId).single().completed)
        assertTrue(db.xpDao().findBySource("BOSS", "boss_$bossId") != null)

        assertTrue(questCompletion.undo(instanceId))
        val bossUndone = db.moduleDao().getBosses().single { it.id == bossId }
        assertEquals("ACTIVE", bossUndone.status)
        assertFalse(db.moduleDao().getBossQuests(bossId).single().completed)
        assertTrue(db.xpDao().findBySource("BOSS_UNDO", "UNDO_BOSS_$bossId") != null)
    }

    @Test
    fun p_undo_dropsStreakWhenLastCompletionOfDay() = runTest {
        seedPlayer()
        StreakHandler(db, eventBus, clock, handlerScope()).start()
        db.playerDao().upsertStreak(
            StreakStateEntity(current = 3, best = 5, lastCompletedDate = "2026-08-14"),
        )

        val id = db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = 1,
                scheduledDate = "2026-08-15",
                status = QuestStatus.AVAILABLE.name,
                title = "Quest",
                type = "DAILY",
                baseXp = 20,
                attributeRewardsJson = "{}",
            ),
        )
        questCompletion.complete(id)
        assertEquals(4, db.playerDao().getStreak(1)!!.current)
        assertEquals("2026-08-15", db.playerDao().getStreak(1)!!.lastCompletedDate)

        assertTrue(questCompletion.undo(id))
        val streak = db.playerDao().getStreak(1)!!
        assertEquals(3, streak.current)
        assertEquals("2026-08-14", streak.lastCompletedDate)
        assertEquals(5, streak.best)
    }

    @Test
    fun e_undo_keepsAchievementUnlocked() = runTest {
        seedPlayer()
        AchievementHandler(db, eventBus, clock, progression, handlerScope()).start()
        db.achievementDao().upsertDefs(
            listOf(
                AchievementDefEntity(
                    key = "first_quest",
                    name = "First",
                    description = "Complete 1",
                    criteriaType = "QUESTS_COMPLETED",
                    criteriaValue = 1,
                    rewardXp = 0,
                ),
            ),
        )

        val id = db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = 1,
                scheduledDate = "2026-08-15",
                status = QuestStatus.AVAILABLE.name,
                title = "Quest",
                type = "DAILY",
                baseXp = 20,
                attributeRewardsJson = "{}",
            ),
        )
        questCompletion.complete(id)
        assertTrue(db.achievementDao().getUnlocked().any { it.achievementKey == "first_quest" })

        assertTrue(questCompletion.undo(id))
        assertEquals(0, db.questDao().countCompletedAll())
        assertTrue(db.achievementDao().getUnlocked().any { it.achievementKey == "first_quest" })
    }
}
