package com.example.solo_levelling.domain.handler

import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.FakeAppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.AttributeStatEntity
import com.example.solo_levelling.data.db.entity.BossEntity
import com.example.solo_levelling.data.db.entity.BossQuestEntity
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.QuestTemplateEntity
import com.example.solo_levelling.domain.model.AttributeCode
import com.example.solo_levelling.domain.service.ProgressionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class BossProgressHandlerTest {
    private lateinit var db: JsonDatabase
    private lateinit var handler: BossProgressHandler
    private lateinit var tempDir: java.nio.file.Path
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        tempDir = Files.createTempDirectory("boss-prog-")
        db = JsonDatabase(tempDir.toFile())
        val bus = EventBus()
        val clock = FakeAppClock(fixedDate = LocalDate.of(2026, 8, 15))
        val progression = ProgressionService(db, bus, clock)
        handler = BossProgressHandler(db, bus, progression, CoroutineScope(dispatcher))
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
    }

    @Test
    fun p_completingBossQuest_setsDerivedProgress() = runTest {
        seedPlayer()
        val templateId = db.questDao().upsertTemplate(
            QuestTemplateEntity(
                key = "bq1",
                type = "DAILY",
                title = "Q",
                baseXp = 10,
                attributeRewardsJson = "{}",
            ),
        )
        val bossId = db.moduleDao().upsertBoss(BossEntity(title = "B", targetValue = 100f, xpReward = 50))
        db.moduleDao().upsertBossQuest(BossQuestEntity(bossId = bossId, templateKey = "bq1", weight = 1f))
        db.moduleDao().upsertBossQuest(BossQuestEntity(bossId = bossId, templateKey = "bq2", weight = 1f))

        handler.applyQuestCompleted(templateId)
        val boss = db.moduleDao().getBosses().single()
        assertEquals(50f, boss.currentValue, 0.01f)
        assertEquals("ACTIVE", boss.status)
    }

    @Test
    fun p_recalculateTwice_idempotent() = runTest {
        seedPlayer()
        val templateId = db.questDao().upsertTemplate(
            QuestTemplateEntity(
                key = "bq1",
                type = "DAILY",
                title = "Q",
                baseXp = 10,
                attributeRewardsJson = "{}",
            ),
        )
        val bossId = db.moduleDao().upsertBoss(BossEntity(title = "B", targetValue = 100f, xpReward = 50))
        db.moduleDao().upsertBossQuest(BossQuestEntity(bossId = bossId, templateKey = "bq1", weight = 1f))

        handler.applyQuestCompleted(templateId)
        val first = db.moduleDao().getBosses().single().currentValue
        handler.applyQuestCompleted(templateId)
        val second = db.moduleDao().getBosses().single().currentValue
        assertEquals(first, second, 0.01f)
        assertEquals(100f, first, 0.01f)
        assertEquals("CLEARED", db.moduleDao().getBosses().single().status)
        assertEquals(1, db.xpDao().getAllLedger().count { it.sourceType == "BOSS" })
    }
}
