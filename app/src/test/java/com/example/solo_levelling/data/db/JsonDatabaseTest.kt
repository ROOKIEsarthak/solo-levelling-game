package com.example.solo_levelling.data.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.data.db.entity.LoggedExerciseEntity
import com.example.solo_levelling.data.db.entity.LoggedSetEntity
import com.example.solo_levelling.data.db.entity.NutritionLogEntity
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.QuestInstanceEntity
import com.example.solo_levelling.data.db.entity.SystemDesignTopicEntity
import com.example.solo_levelling.data.db.entity.WorkoutDayPlanEntity
import com.example.solo_levelling.data.db.entity.WorkoutLogEntity
import com.example.solo_levelling.data.db.entity.WorkoutRoutineEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JsonDatabaseTest {
    private lateinit var dbDir: File
    private lateinit var db: JsonDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        dbDir = File(context.cacheDir, "test-db-${System.nanoTime()}").also { it.mkdirs() }
        db = JsonDatabase(dbDir)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun p_upsertWorkoutLog_persistsToNestedFile() = runTest {
        db.moduleDao().upsertWorkoutLog(
            WorkoutLogEntity(
                date = "2026-08-15",
                workoutName = "STRENGTH",
                durationMinutes = 45,
                exercises = listOf(
                    LoggedExerciseEntity(
                        name = "Bench",
                        sets = listOf(LoggedSetEntity(60f, 10)),
                    ),
                ),
            ),
        )

        val file = File(dbDir, "workouts/logs/2026-08-15.json")
        assertTrue(file.exists())
        assertTrue(file.readText().contains("STRENGTH"))
        assertEquals(1, db.moduleDao().observeWorkouts().first().size)
    }

    @Test
    fun p_workoutRoutine_survivesClearProgress() = runTest {
        db.moduleDao().upsertWorkoutRoutine(
            WorkoutRoutineEntity(
                monday = WorkoutDayPlanEntity(enabled = true, name = "Chest"),
            ),
        )
        db.moduleDao().upsertWorkoutLog(
            WorkoutLogEntity(date = "2026-08-15", workoutName = "Chest"),
        )
        db.moduleDao().upsertNutrition(
            NutritionLogEntity("2026-08-15", 2100, 150, 200, 60),
        )

        db.clearProgressTables()

        assertEquals("Chest", db.moduleDao().getWorkoutRoutine().monday.name)
        assertTrue(db.moduleDao().getAllWorkoutLogs().isEmpty())
        assertNull(db.moduleDao().getDietLog("2026-08-15"))
        assertNull(db.moduleDao().getNutrition("2026-08-15"))
    }

    @Test
    fun p_insertTask_writesTaskFileOnDisk() = runTest {
        val id = db.questDao().insertInstance(
            QuestInstanceEntity(
                templateId = 1,
                scheduledDate = "2026-08-15",
                title = "Morning run",
                type = "DAILY",
                baseXp = 20,
                attributeRewardsJson = "{}",
            ),
        )

        val taskFile = File(dbDir, "tasks/task-$id.json")
        assertTrue(taskFile.exists())
        assertTrue(taskFile.readText().contains("Morning run"))
    }

    @Test
    fun p_clearProgressTables_keepsUserJsonName() = runTest {
        db.playerDao().upsertProfile(
            PlayerProfileEntity(
                name = "CustomHunter",
                totalXp = 500,
                level = 5,
                onboardingDone = true,
                prioritiesCsv = "career,fitness",
                createdAtEpochMs = 1L,
            ),
        )

        db.clearProgressTables()

        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID)!!
        assertEquals("CustomHunter", profile.name)
        assertEquals(0, profile.totalXp)
        assertEquals(false, profile.onboardingDone)
        assertEquals("career,fitness", profile.prioritiesCsv)
    }

    @Test
    fun r_clearProgressTables_clearsOnboardingDoneEvenWhenPreviouslyTrue() = runTest {
        db.playerDao().upsertProfile(
            PlayerProfileEntity(
                name = "Wiped",
                onboardingDone = true,
                createdAtEpochMs = 1L,
            ),
        )

        db.clearProgressTables()

        assertEquals(false, db.playerDao().getProfile(SystemDefaults.PLAYER_ID)!!.onboardingDone)
    }

    @Test
    fun n_findBySource_returnsNullWhenEmpty() = runTest {
        assertNull(db.xpDao().findBySource("QUEST", "missing"))
    }

    @Test
    fun e_reloadJsonDatabaseFromSameDir_restoresData() = runTest {
        db.playerDao().upsertProfile(
            PlayerProfileEntity(name = "Persisted", totalXp = 120, createdAtEpochMs = 99L),
        )
        db.moduleDao().upsertWorkoutLog(
            WorkoutLogEntity(date = "2026-08-17", workoutName = "Back"),
        )
        db.close()

        val reloaded = JsonDatabase(dbDir)
        val profile = reloaded.playerDao().getProfile(SystemDefaults.PLAYER_ID)
        assertEquals("Persisted", profile?.name)
        assertEquals(120, profile?.totalXp)
        assertNotNull(reloaded.moduleDao().getWorkoutLog("2026-08-17"))
        reloaded.close()
    }

    @Test
    fun e_migrateLegacyWorkoutsAndNutrition() = runTest {
        db.close()
        File(dbDir, "workouts.json").writeText(
            """[{"id":1,"date":"2026-08-10","type":"Push","durationMinutes":40,"notes":"","completed":true}]""",
        )
        File(dbDir, "workout_exercises.json").writeText(
            """[{"id":1,"workoutId":1,"name":"Bench","sets":2,"reps":8,"weightKg":60.0,"rir":0}]""",
        )
        File(dbDir, "nutrition.json").writeText(
            """[{"date":"2026-08-10","calories":2000,"protein":140,"carbs":180,"fat":50}]""",
        )

        val migrated = JsonDatabase(dbDir)
        val log = migrated.moduleDao().getWorkoutLog("2026-08-10")
        assertNotNull(log)
        assertEquals("Push", log!!.workoutName)
        assertEquals(2, log.exercises.first().sets.size)
        val nutrition = migrated.moduleDao().getNutrition("2026-08-10")
        assertEquals(2000, nutrition!!.calories)
        migrated.close()
    }

    @Test
    fun p_systemDesignTopics_persistToNestedFile() = runTest {
        db.moduleDao().replaceSystemDesignTopics(
            listOf(
                SystemDesignTopicEntity(id = "fundamentals", title = "Fundamentals", orderIndex = 1),
            ),
        )
        val file = File(dbDir, "career/system-design/topics.json")
        assertTrue(file.exists())
        assertTrue(file.readText().contains("Fundamentals"))
        assertEquals(1, db.moduleDao().getSystemDesignTopics().size)
    }
}
