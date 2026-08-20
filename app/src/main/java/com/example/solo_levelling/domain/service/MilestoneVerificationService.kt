package com.example.solo_levelling.domain.service

import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.QuestInstanceEntity
import com.example.solo_levelling.data.db.entity.QuestTemplateEntity
import com.example.solo_levelling.domain.model.QuestStatus
import com.example.solo_levelling.domain.model.QuestType
import com.example.solo_levelling.domain.model.VerificationType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

data class MilestoneRequirement(
    val instanceId: Long,
    val title: String,
    val completed: Boolean,
    val templateKey: String,
    val priorityTags: String,
    val verificationType: String,
    val questType: String,
    val status: String,
)

data class MilestoneVerificationResult(
    val ready: Boolean,
    val completedCount: Int,
    val totalCount: Int,
    val requirements: List<MilestoneRequirement>,
)

class MilestoneVerificationService(
    private val db: JsonDatabase,
    private val progression: ProgressionService,
) {
    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE

    suspend fun verify(instance: QuestInstanceEntity): MilestoneVerificationResult {
        val bounds = weekBounds(instance.scheduledDate) ?: return notReady()
        val weekInstances = db.questDao().getInstancesInRange(bounds.first, bounds.second)
        val templatesById = db.questDao().getActiveTemplates().associateBy { it.id }
        val modules = progression.currentModules()
        return evaluate(instance, weekInstances, templatesById, modules)
    }

    fun evaluate(
        milestone: QuestInstanceEntity,
        weekInstances: List<QuestInstanceEntity>,
        templatesById: Map<Long, QuestTemplateEntity>,
        modules: EnabledModules,
    ): MilestoneVerificationResult {
        val bounds = weekBounds(milestone.scheduledDate) ?: return notReady()
        val requirements = weekInstances
            .filter { it.scheduledDate >= bounds.first && it.scheduledDate <= bounds.second }
            .filter { it.id != milestone.id }
            .filter { it.type == QuestType.DAILY.name || it.type == QuestType.WEEKLY.name }
            .filter { it.verificationType != VerificationType.AUTOMATIC.name }
            .map { instance ->
                val template = templatesById[instance.templateId]
                MilestoneRequirement(
                    instanceId = instance.id,
                    title = instance.title,
                    completed = instance.status == QuestStatus.COMPLETED.name,
                    templateKey = template?.key.orEmpty(),
                    priorityTags = template?.priorityTags.orEmpty(),
                    verificationType = instance.verificationType,
                    questType = instance.type,
                    status = instance.status,
                )
            }
            .filter { ModuleScope.allowsQuestTemplate(it.priorityTags, modules) }
        if (requirements.isEmpty()) return notReady()
        val completedCount = requirements.count { it.completed }
        return MilestoneVerificationResult(
            ready = completedCount == requirements.size,
            completedCount = completedCount,
            totalCount = requirements.size,
            requirements = requirements,
        )
    }

    private fun weekBounds(scheduledDate: String): Pair<String, String>? {
        val localDate = runCatching { LocalDate.parse(scheduledDate, dateFmt) }.getOrNull() ?: return null
        val weekStart = localDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return weekStart.format(dateFmt) to weekStart.plusDays(6).format(dateFmt)
    }

    private fun notReady(): MilestoneVerificationResult =
        MilestoneVerificationResult(
            ready = false,
            completedCount = 0,
            totalCount = 0,
            requirements = emptyList(),
        )
}
