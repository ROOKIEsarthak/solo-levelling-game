package com.example.solo_levelling.ui.quests

import com.example.solo_levelling.data.db.entity.BossQuestEntity
import com.example.solo_levelling.data.db.entity.QuestInstanceEntity
import com.example.solo_levelling.domain.model.QuestStatus
import com.example.solo_levelling.domain.service.EnabledModules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestsModuleIsolationTest {
    private fun item(
        id: Long,
        tags: String,
        status: String = QuestStatus.AVAILABLE.name,
        xp: Int = 10,
        title: String = "Quest $id",
    ) = QuestListItem(
        instance = QuestInstanceEntity(
            id = id,
            templateId = id,
            scheduledDate = "2026-08-18",
            status = status,
            title = title,
            type = "DAILY",
            baseXp = xp,
            attributeRewardsJson = "{}",
        ),
        priorityTags = tags,
    )

    private val workoutDiet = EnabledModules(career = false, workout = true, diet = true)
    private val careerOnly = EnabledModules(career = true, workout = false, diet = false)
    private val all = EnabledModules(career = true, workout = true, diet = true)

    @Test
    fun p_workoutDiet_keepsWorkoutDietAndGlobal() {
        val items = listOf(
            item(1, "module_workout", title = "Complete workout"),
            item(2, "module_diet", title = "Log nutrition"),
            item(3, "", title = "Write a short journal"),
        )
        val visible = questItemsForModules(items, workoutDiet)
        assertEquals(listOf(1L, 2L, 3L), visible.map { it.instance.id })
    }

    @Test
    fun r_workoutDiet_hidesStaleDsaAndSystemDesign() {
        val items = listOf(
            item(1, "module_career", title = "Solve 2 DSA problems", xp = 34),
            item(2, "module_career,career,system_design", title = "System design study"),
            item(3, "module_workout", title = "Complete workout"),
            item(4, "module_diet", title = "Log nutrition"),
        )
        val visible = questItemsForModules(items, workoutDiet)
        assertEquals(listOf(3L, 4L), visible.map { it.instance.id })
        assertTrue(visible.none { it.instance.title.contains("DSA") })
    }

    @Test
    fun n_careerOnly_hidesWorkoutAndDiet() {
        val items = listOf(
            item(1, "module_career"),
            item(2, "module_workout"),
            item(3, "module_diet"),
            item(4, ""),
        )
        assertEquals(listOf(1L, 4L), questItemsForModules(items, careerOnly).map { it.instance.id })
    }

    @Test
    fun p_allModules_keepsEverything() {
        val items = listOf(
            item(1, "module_career"),
            item(2, "module_workout"),
            item(3, "module_diet"),
            item(4, ""),
        )
        assertEquals(4, questItemsForModules(items, all).size)
    }

    @Test
    fun r_availableXp_excludesHiddenCareerQuests() {
        val items = listOf(
            item(1, "module_career", xp = 34),
            item(2, "module_workout", xp = 42),
            item(3, "module_diet", xp = 15),
            item(4, "", xp = 20, status = QuestStatus.COMPLETED.name),
        )
        assertEquals(57, availableXpForModules(items, workoutDiet))
    }

    @Test
    fun e_availableXp_emptyWhenNothingAllowed() {
        val items = listOf(item(1, "module_career", xp = 40))
        assertEquals(0, availableXpForModules(items, workoutDiet))
    }

    @Test
    fun n_bossQuests_hideCareerObjectivesWhenCareerOff() {
        val quests = listOf(
            BossQuestEntity(id = 1, bossId = 9, templateKey = "dsa_daily"),
            BossQuestEntity(id = 2, bossId = 9, templateKey = "workout_daily"),
            BossQuestEntity(id = 3, bossId = 9, templateKey = "journal"),
        )
        val tags = mapOf(
            "dsa_daily" to "module_career",
            "workout_daily" to "module_workout",
            "journal" to "",
        )
        val visible = bossQuestsForModules(quests, tags, workoutDiet)
        assertEquals(listOf("workout_daily", "journal"), visible.map { it.templateKey })
    }

    @Test
    fun p_careerWorkout_excludesDiet() {
        val items = listOf(
            item(1, "module_career"),
            item(2, "module_workout"),
            item(3, "module_diet"),
        )
        val visible = questItemsForModules(
            items,
            EnabledModules(career = true, workout = true, diet = false),
        )
        assertEquals(listOf(1L, 2L), visible.map { it.instance.id })
    }

    @Test
    fun p_careerDiet_excludesWorkout() {
        val items = listOf(
            item(1, "module_career"),
            item(2, "module_workout"),
            item(3, "module_diet"),
        )
        val visible = questItemsForModules(
            items,
            EnabledModules(career = true, workout = false, diet = true),
        )
        assertEquals(listOf(1L, 3L), visible.map { it.instance.id })
    }

    @Test
    fun e_dietOnly_keepsDietAndGlobal() {
        val items = listOf(
            item(1, "module_career"),
            item(2, "module_workout"),
            item(3, "module_diet"),
            item(4, ""),
        )
        val visible = questItemsForModules(
            items,
            EnabledModules(career = false, workout = false, diet = true),
        )
        assertEquals(listOf(3L, 4L), visible.map { it.instance.id })
    }
}
