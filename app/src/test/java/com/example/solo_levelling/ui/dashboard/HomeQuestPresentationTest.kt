package com.example.solo_levelling.ui.dashboard

import com.example.solo_levelling.data.db.entity.QuestInstanceEntity
import com.example.solo_levelling.domain.model.QuestStatus
import com.example.solo_levelling.domain.model.QuestType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeQuestPresentationTest {
    private fun quest(
        id: Long,
        templateId: Long = id,
        type: String = QuestType.DAILY.name,
        status: String = QuestStatus.AVAILABLE.name,
        baseXp: Int = 20,
        title: String = "Quest $id",
    ) = QuestInstanceEntity(
        id = id,
        templateId = templateId,
        scheduledDate = "2026-08-17",
        status = status,
        title = title,
        type = type,
        baseXp = baseXp,
        attributeRewardsJson = "{}",
    )

    @Test
    fun p_scope_includesDailyAndRecovery_excludesWeekly() {
        val quests = listOf(
            quest(1, type = QuestType.DAILY.name),
            quest(2, type = QuestType.RECOVERY.name),
            quest(3, type = QuestType.WEEKLY.name),
            quest(4, type = QuestType.MILESTONE.name),
        )
        val scoped = HomeQuestPresentation.scopeForHome(
            quests = quests,
            tagsByTemplateId = emptyMap(),
            keysByTemplateId = emptyMap(),
            allowsTemplate = { true },
        )
        assertEquals(listOf(1L, 2L), scoped.map { it.instance.id })
    }

    @Test
    fun p_scope_filtersByModuleAllow() {
        val quests = listOf(quest(1, templateId = 10), quest(2, templateId = 20))
        val tags = mapOf(10L to "module_workout", 20L to "module_career")
        val scoped = HomeQuestPresentation.scopeForHome(
            quests = quests,
            tagsByTemplateId = tags,
            keysByTemplateId = mapOf(10L to "workout_daily", 20L to "dsa_daily"),
            allowsTemplate = { it.contains("module_workout") },
        )
        assertEquals(1, scoped.size)
        assertEquals("workout_daily", scoped[0].templateKey)
        assertEquals("module_workout", scoped[0].priorityTags)
    }

    @Test
    fun n_scope_excludesMissed() {
        val quests = listOf(
            quest(1, status = QuestStatus.MISSED.name),
            quest(2, status = QuestStatus.AVAILABLE.name),
        )
        val scoped = HomeQuestPresentation.scopeForHome(
            quests = quests,
            tagsByTemplateId = emptyMap(),
            keysByTemplateId = emptyMap(),
            allowsTemplate = { true },
        )
        assertEquals(listOf(2L), scoped.map { it.instance.id })
    }

    @Test
    fun p_countMatchesVisibleAll_fiveQuests() {
        val items = (1L..5L).map {
            HomeQuestItem(quest(it, baseXp = 10 + it.toInt()), "k$it", "module_workout")
        }
        val sections = HomeQuestPresentation.split(items)
        assertEquals(5, sections.totalCount)
        assertEquals(0, sections.completedCount)
        assertEquals(sections.totalCount, sections.priorities.size + sections.other.size)
        assertFalse(sections.showViewAll)
        assertEquals(3, sections.priorities.size)
        assertEquals(2, sections.other.size)
    }

    @Test
    fun p_completeOne_updatesCompletedCount() {
        val items = listOf(
            HomeQuestItem(quest(1, status = QuestStatus.COMPLETED.name, baseXp = 50), "a", "module_workout"),
            HomeQuestItem(quest(2, baseXp = 40), "b", "module_diet"),
            HomeQuestItem(quest(3, baseXp = 30), "c", ""),
        )
        val sections = HomeQuestPresentation.split(items)
        assertEquals(1, sections.completedCount)
        assertEquals(3, sections.totalCount)
        assertTrue(sections.priorities.none { it.instance.status == QuestStatus.COMPLETED.name })
        assertTrue(sections.other.any { it.instance.status == QuestStatus.COMPLETED.name })
    }

    @Test
    fun p_sort_inProgressBeforeAvailable_moduleBeforeGlobal() {
        val items = listOf(
            HomeQuestItem(quest(1, status = QuestStatus.AVAILABLE.name, baseXp = 10), "j", ""),
            HomeQuestItem(quest(2, status = QuestStatus.IN_PROGRESS.name, baseXp = 10), "w", "module_workout"),
            HomeQuestItem(quest(3, status = QuestStatus.AVAILABLE.name, baseXp = 50), "d", "module_diet"),
            HomeQuestItem(quest(4, status = QuestStatus.COMPLETED.name, baseXp = 99), "c", "module_career"),
        )
        val sorted = HomeQuestPresentation.sort(items).map { it.instance.id }
        assertEquals(listOf(2L, 3L, 1L, 4L), sorted)
    }

    @Test
    fun e_empty_sections() {
        val sections = HomeQuestPresentation.split(emptyList())
        assertEquals(0, sections.totalCount)
        assertTrue(sections.priorities.isEmpty())
        assertTrue(sections.other.isEmpty())
        assertFalse(sections.showViewAll)
    }

    @Test
    fun e_allComplete_prioritiesEmpty_otherHasCompleted() {
        val items = (1L..3L).map {
            HomeQuestItem(quest(it, status = QuestStatus.COMPLETED.name), "k$it", "module_workout")
        }
        val sections = HomeQuestPresentation.split(items)
        assertEquals(3, sections.completedCount)
        assertEquals(3, sections.totalCount)
        assertTrue(sections.priorities.isEmpty())
        assertEquals(3, sections.other.size)
    }

    @Test
    fun e_manyIncomplete_showsViewAllAndCapsOther() {
        val items = (1L..7L).map {
            HomeQuestItem(quest(it, baseXp = 100 - it.toInt()), "k$it", "module_workout")
        }
        val sections = HomeQuestPresentation.split(items)
        assertTrue(sections.showViewAll)
        assertEquals(3, sections.priorities.size)
        assertEquals(2, sections.other.size)
        assertEquals(7, sections.totalCount)
        assertTrue(sections.priorities.size + sections.other.size < sections.totalCount)
    }

    @Test
    fun p_higherBaseXpFirstWithinSameStatusAndModule() {
        val items = listOf(
            HomeQuestItem(quest(1, baseXp = 20), "a", "module_workout"),
            HomeQuestItem(quest(2, baseXp = 50), "b", "module_workout"),
        )
        val sorted = HomeQuestPresentation.sort(items).map { it.instance.id }
        assertEquals(listOf(2L, 1L), sorted)
    }
}
