package com.example.solo_levelling.domain.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.FakeAppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.AttributeStatEntity
import com.example.solo_levelling.data.db.entity.LoggedExerciseEntity
import com.example.solo_levelling.data.db.entity.LoggedSetEntity
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.QuestInstanceEntity
import com.example.solo_levelling.data.db.entity.QuestTemplateEntity
import com.example.solo_levelling.data.db.entity.WorkoutLogEntity
import com.example.solo_levelling.domain.model.AttributeCode
import com.example.solo_levelling.domain.model.QuestStatus
import com.example.solo_levelling.domain.model.VerificationType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QuestVerificationWorkoutTest {
    private lateinit var db: JsonDatabase
    private lateinit var clock: FakeAppClock
    private lateinit var service: QuestVerificationService
    private val date = "2026-08-15"

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = JsonDatabase(File(context.cacheDir, "test-db-${System.nanoTime()}").also { it.mkdirs() })
        clock = FakeAppClock(fixedDate = LocalDate.of(2026, 8, 15))
        val eventBus = EventBus()
        val progression = ProgressionService(db, eventBus, clock)
        val questCompletion = QuestCompletionService(db, eventBus, clock, progression)
        service = QuestVerificationService(db, clock, questCompletion)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun p_workoutDaily_autoCompletesWhenLogHasSets() = runTest {
        db.playerDao().upsertProfile(
            PlayerProfileEntity(name = "Test", timezone = "UTC", onboardingDone = true),
        )
        db.playerDao().upsertAttributes(
            AttributeCode.entries.map { AttributeStatEntity(it.name) },
        )
        val templateId = db.questDao().upsertTemplate(
            QuestTemplateEntity(
                key = "workout_daily",
                type = "DAILY",
                title = "Workout",
                baseXp = 50,
                attributeRewardsJson = """{"STR":30}""",
            ),
        )
        val questId = db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = templateId,
                scheduledDate = date,
                status = QuestStatus.AVAILABLE.name,
                title = "Workout",
                type = "DAILY",
                baseXp = 50,
                attributeRewardsJson = """{"STR":30}""",
                verificationType = VerificationType.MANUAL.name,
            ),
        )
        db.moduleDao().upsertWorkoutLog(
            WorkoutLogEntity(
                date = date,
                exercises = listOf(
                    LoggedExerciseEntity(
                        name = "Squat",
                        sets = listOf(LoggedSetEntity(100f, 5)),
                    ),
                ),
            ),
        )
        service.tryAutoComplete(date)
        assertEquals(QuestStatus.COMPLETED.name, db.questDao().getInstance(questId)!!.status)
    }
}
