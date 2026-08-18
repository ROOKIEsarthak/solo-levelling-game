package com.example.solo_levelling.ui.dashboard

import com.example.solo_levelling.data.db.entity.QuestInstanceEntity
import com.example.solo_levelling.domain.model.QuestStatus
import com.example.solo_levelling.domain.model.QuestType
import com.example.solo_levelling.domain.service.ModuleId
import com.example.solo_levelling.domain.service.ModuleScope

data class HomeQuestItem(
    val instance: QuestInstanceEntity,
    val templateKey: String,
    val priorityTags: String,
)

data class HomeQuestSections(
    val all: List<HomeQuestItem>,
    val priorities: List<HomeQuestItem>,
    val other: List<HomeQuestItem>,
    val showViewAll: Boolean,
) {
    val completedCount: Int get() = all.count { it.instance.status == QuestStatus.COMPLETED.name }
    val totalCount: Int get() = all.size
}

object HomeQuestPresentation {
    private const val PRIORITY_LIMIT = 3
    private const val OTHER_INCOMPLETE_WHEN_MANY = 2
    private const val MANY_INCOMPLETE_THRESHOLD = 5

    fun isHomeQuestType(type: String): Boolean =
        type == QuestType.DAILY.name || type == QuestType.RECOVERY.name

    fun scopeForHome(
        quests: List<QuestInstanceEntity>,
        tagsByTemplateId: Map<Long, String>,
        keysByTemplateId: Map<Long, String>,
        allowsTemplate: (priorityTags: String) -> Boolean,
    ): List<HomeQuestItem> =
        quests
            .asSequence()
            .filter { isHomeQuestType(it.type) }
            .filter { it.status != QuestStatus.MISSED.name }
            .filter { allowsTemplate(tagsByTemplateId[it.templateId].orEmpty()) }
            .map { instance ->
                HomeQuestItem(
                    instance = instance,
                    templateKey = keysByTemplateId[instance.templateId].orEmpty(),
                    priorityTags = tagsByTemplateId[instance.templateId].orEmpty(),
                )
            }
            .toList()

    fun sort(items: List<HomeQuestItem>): List<HomeQuestItem> =
        items.sortedWith(
            compareBy<HomeQuestItem> { statusRank(it.instance.status) }
                .thenBy { moduleRank(it.priorityTags) }
                .thenByDescending { it.instance.baseXp }
                .thenBy { it.instance.id },
        )

    fun split(
        items: List<HomeQuestItem>,
        priorityLimit: Int = PRIORITY_LIMIT,
        otherIncompleteWhenMany: Int = OTHER_INCOMPLETE_WHEN_MANY,
        manyThreshold: Int = MANY_INCOMPLETE_THRESHOLD,
    ): HomeQuestSections {
        val sorted = sort(items)
        val incomplete = sorted.filter { it.instance.status != QuestStatus.COMPLETED.name }
        val completed = sorted.filter { it.instance.status == QuestStatus.COMPLETED.name }

        val priorities = incomplete.take(priorityLimit)
        val remainingIncomplete = incomplete.drop(priorityLimit)
        val many = incomplete.size > manyThreshold
        val otherIncomplete = if (many) {
            remainingIncomplete.take(otherIncompleteWhenMany)
        } else {
            remainingIncomplete
        }
        return HomeQuestSections(
            all = sorted,
            priorities = priorities,
            other = otherIncomplete + completed,
            showViewAll = many,
        )
    }

    private fun statusRank(status: String): Int = when (status) {
        QuestStatus.IN_PROGRESS.name -> 0
        QuestStatus.AVAILABLE.name -> 1
        QuestStatus.LOCKED.name -> 2
        QuestStatus.COMPLETED.name -> 3
        else -> 4
    }

    private fun moduleRank(priorityTags: String): Int = when (ModuleScope.moduleForPriorityTags(priorityTags)) {
        ModuleId.WORKOUT, ModuleId.DIET, ModuleId.CAREER -> 0
        ModuleId.GLOBAL -> 1
    }
}
