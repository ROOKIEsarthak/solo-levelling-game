package com.example.solo_levelling.domain.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.FakeAppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.AttributeStatEntity
import com.example.solo_levelling.data.db.entity.FoodItemEntity
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.QuestInstanceEntity
import com.example.solo_levelling.data.db.entity.QuestTemplateEntity
import com.example.solo_levelling.domain.model.AttributeCode
import com.example.solo_levelling.domain.model.QuestStatus
import com.example.solo_levelling.domain.model.VerificationType
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
class QuestVerificationNutritionTest {
    private lateinit var db: JsonDatabase
    private lateinit var clock: FakeAppClock
    private lateinit var progression: ProgressionService
    private lateinit var questCompletion: QuestCompletionService
    private lateinit var verification: QuestVerificationService
    private lateinit var modules: ModuleService
    private val date = "2026-08-15"

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = JsonDatabase(File(context.cacheDir, "test-db-${System.nanoTime()}").also { it.mkdirs() })
        clock = FakeAppClock(fixedDate = LocalDate.of(2026, 8, 15))
        val eventBus = EventBus()
        progression = ProgressionService(db, eventBus, clock)
        questCompletion = QuestCompletionService(db, eventBus, clock, progression)
        verification = QuestVerificationService(db, clock, questCompletion)
        modules = ModuleService(db, eventBus, clock, progression, verification)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seedProfileAndQuest(): Long {
        db.playerDao().upsertProfile(
            PlayerProfileEntity(name = "Test", timezone = "UTC", onboardingDone = true),
        )
        db.playerDao().upsertAttributes(
            AttributeCode.entries.map { AttributeStatEntity(it.name) },
        )
        db.configDao().upsert(
            com.example.solo_levelling.data.db.entity.UserConfigEntity("module_diet", "true"),
        )
        val templateId = db.questDao().upsertTemplate(
            QuestTemplateEntity(
                key = "nutrition_daily",
                type = "DAILY",
                title = "Complete meal tracking",
                baseXp = 15,
                attributeRewardsJson = """{"VIT":15}""",
                verificationType = VerificationType.MANUAL.name,
            ),
        )
        return db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = templateId,
                scheduledDate = date,
                status = QuestStatus.AVAILABLE.name,
                title = "Complete meal tracking",
                type = "DAILY",
                baseXp = 15,
                attributeRewardsJson = """{"VIT":15}""",
                verificationType = VerificationType.MANUAL.name,
            ),
        )
    }

    private suspend fun addValidMeal(name: String) {
        val mealId = modules.addMeal(date, name)
        modules.upsertFood(
            date,
            mealId,
            FoodItemEntity(name = name, calories = 400, protein = 25, carbs = 30, fat = 10),
        )
    }

    @Test
    fun n_twoMeals_doesNotCompleteQuest() = runTest {
        seedProfileAndQuest()
        addValidMeal("Breakfast")
        addValidMeal("Lunch")
        verification.tryAutoComplete(date)
        val quest = db.questDao().getInstancesForDate(date).first()
        assertEquals(QuestStatus.AVAILABLE.name, quest.status)
    }

    @Test
    fun p_threeMeals_autoCompletesQuest() = runTest {
        seedProfileAndQuest()
        addValidMeal("Breakfast")
        addValidMeal("Lunch")
        addValidMeal("Dinner")
        verification.tryAutoComplete(date)
        val quest = db.questDao().getInstancesForDate(date).first()
        assertEquals(QuestStatus.COMPLETED.name, quest.status)
    }

    @Test
    fun n_fourthMeal_noDuplicateQuestXp() = runTest {
        seedProfileAndQuest()
        addValidMeal("Breakfast")
        addValidMeal("Lunch")
        addValidMeal("Dinner")
        verification.tryAutoComplete(date)
        val xpAfterThree = db.playerDao().getProfile(1)!!.totalXp
        addValidMeal("Snack")
        verification.tryAutoComplete(date)
        assertEquals(xpAfterThree, db.playerDao().getProfile(1)!!.totalXp)
        assertEquals(1, db.xpDao().getAllLedger().count { it.sourceType == "QUEST_INSTANCE" })
    }

    @Test
    fun n_deleteMeal_undoesQuestCompletion() = runTest {
        seedProfileAndQuest()
        addValidMeal("Breakfast")
        addValidMeal("Lunch")
        val dinnerId = modules.addMeal(date, "Dinner")
        modules.upsertFood(
            date,
            dinnerId,
            FoodItemEntity(name = "Dinner", calories = 500, protein = 30),
        )
        verification.tryAutoComplete(date)
        modules.deleteMeal(date, dinnerId)
        verification.tryAutoComplete(date)
        val quest = db.questDao().getInstancesForDate(date).first()
        assertEquals(QuestStatus.AVAILABLE.name, quest.status)
    }

    @Test
    fun p_threeMeals_awardsNutritionXpOnce() = runTest {
        seedProfileAndQuest()
        addValidMeal("Breakfast")
        addValidMeal("Lunch")
        assertEquals(0, db.xpDao().getAllLedger().count { it.sourceType == "NUTRITION" })
        addValidMeal("Dinner")
        assertEquals(1, db.xpDao().getAllLedger().count { it.sourceType == "NUTRITION" })
        addValidMeal("Snack")
        assertEquals(1, db.xpDao().getAllLedger().count { it.sourceType == "NUTRITION" })
    }
}
