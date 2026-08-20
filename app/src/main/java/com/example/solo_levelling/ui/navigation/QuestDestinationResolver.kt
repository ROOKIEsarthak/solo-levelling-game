package com.example.solo_levelling.ui.navigation

import com.example.solo_levelling.domain.model.QuestStatus
import com.example.solo_levelling.domain.model.QuestType
import com.example.solo_levelling.domain.model.VerificationType
import com.example.solo_levelling.domain.service.ModuleId
import com.example.solo_levelling.domain.service.ModuleScope

sealed class QuestActionDestination {
    data object Fitness : QuestActionDestination()
    data object Nutrition : QuestActionDestination()
    data object Career : QuestActionDestination()
    data object Modules : QuestActionDestination()
    data object CompleteInPlace : QuestActionDestination()
    data object AwaitVerification : QuestActionDestination()
}

data class QuestAction(
    val destination: QuestActionDestination,
    val label: String,
    val section: String = "",
)

object QuestDestinationResolver {
    const val SECTION_JOURNAL = "journal"
    const val SECTION_FOCUS = "focus"
    const val SECTION_METRICS = "metrics"
    const val SECTION_DSA = "dsa"
    const val SECTION_SYSTEM_DESIGN = "system_design"

    fun resolve(
        priorityTags: String,
        verificationType: String,
        status: String,
        templateKey: String = "",
        questType: String = "",
    ): QuestAction {
        if (status == QuestStatus.COMPLETED.name || status == QuestStatus.LOCKED.name) {
            return QuestAction(QuestActionDestination.CompleteInPlace, "COMPLETE")
        }

        val inProgress = status == QuestStatus.IN_PROGRESS.name
        val startOrContinue = if (inProgress) "CONTINUE" else "START"
        val verification = verificationType.uppercase()
        val key = templateKey.lowercase()
        val tags = priorityTags.lowercase()

        when (verification) {
            VerificationType.TIMER.name ->
                return QuestAction(QuestActionDestination.Modules, startOrContinue, SECTION_FOCUS)
            VerificationType.METRIC_THRESHOLD.name ->
                return QuestAction(QuestActionDestination.Modules, "LOG", SECTION_METRICS)
            VerificationType.AUTOMATIC.name ->
                return QuestAction(QuestActionDestination.AwaitVerification, "")
        }

        return when (ModuleScope.moduleForPriorityTags(priorityTags)) {
            ModuleId.WORKOUT -> QuestAction(QuestActionDestination.Fitness, startOrContinue)
            ModuleId.DIET -> QuestAction(QuestActionDestination.Nutrition, "LOG")
            ModuleId.CAREER -> QuestAction(
                QuestActionDestination.Career,
                startOrContinue,
                careerSection(key, tags, verification),
            )
            ModuleId.GLOBAL -> when {
                verification == VerificationType.COUNT.name ->
                    QuestAction(QuestActionDestination.Career, startOrContinue, SECTION_DSA)
                key.contains("journal") ->
                    QuestAction(QuestActionDestination.Modules, "WRITE", SECTION_JOURNAL)
                questType == QuestType.RECOVERY.name || key.contains("recovery") ->
                    QuestAction(QuestActionDestination.CompleteInPlace, "COMPLETE")
                questType == QuestType.MILESTONE.name ->
                    QuestAction(QuestActionDestination.CompleteInPlace, "COMPLETE")
                else ->
                    QuestAction(QuestActionDestination.CompleteInPlace, "COMPLETE")
            }
        }
    }

    fun navigates(destination: QuestActionDestination): Boolean =
        destination !is QuestActionDestination.CompleteInPlace &&
            destination !is QuestActionDestination.AwaitVerification

    fun dispatch(
        action: QuestAction,
        onFitness: () -> Unit,
        onNutrition: () -> Unit,
        onCareer: (section: String) -> Unit,
        onModules: (section: String) -> Unit,
        onCompleteInPlace: () -> Unit,
    ) {
        when (action.destination) {
            QuestActionDestination.Fitness -> onFitness()
            QuestActionDestination.Nutrition -> onNutrition()
            QuestActionDestination.Career -> onCareer(action.section)
            QuestActionDestination.Modules -> onModules(action.section)
            QuestActionDestination.CompleteInPlace -> onCompleteInPlace()
            QuestActionDestination.AwaitVerification -> Unit
        }
    }

    private fun careerSection(key: String, tags: String, verification: String): String = when {
        verification == VerificationType.COUNT.name || key.contains("dsa") -> SECTION_DSA
        key.contains("system_design") || tags.contains("system_design") -> SECTION_SYSTEM_DESIGN
        else -> ""
    }
}
