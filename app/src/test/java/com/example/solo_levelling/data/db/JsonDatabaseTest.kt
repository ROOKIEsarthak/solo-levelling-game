package com.example.solo_levelling.data.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.QuestInstanceEntity
import com.example.solo_levelling.data.db.entity.WorkoutEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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
    fun p_upsertWorkout_persistsToWorkoutsJsonOnDisk() = runTest {
        db.moduleDao().insertWorkout(
            WorkoutEntity(date = "2026-08-15", type = "STRENGTH", durationMinutes = 45),
        )

        val workoutsFile = File(dbDir, "workouts.json")
        assertTrue(workoutsFile.exists())
        assertTrue(workoutsFile.readText().contains("STRENGTH"))
        assertEquals(1, db.moduleDao().observeWorkouts().first().size)
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
        assertEquals(true, profile.onboardingDone)
        assertEquals("career,fitness", profile.prioritiesCsv)
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
        db.close()

        val reloaded = JsonDatabase(dbDir)
        val profile = reloaded.playerDao().getProfile(SystemDefaults.PLAYER_ID)
        assertEquals("Persisted", profile?.name)
        assertEquals(120, profile?.totalXp)
        reloaded.close()
    }
}
