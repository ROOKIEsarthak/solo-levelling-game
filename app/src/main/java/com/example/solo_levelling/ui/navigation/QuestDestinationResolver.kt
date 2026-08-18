package com.example.solo_levelling.ui.navigation

import com.example.solo_levelling.domain.model.QuestStatus
import com.example.solo_levelling.domain.model.VerificationType
import com.example.solo_levelling.domain.service.ModuleId
import com.example.solo_levelling.domain.service.ModuleScope

sealed class QuestActionDestination {
    data object Fitness : QuestActionDestination()
    data object Nutrition : QuestActionDestination()
    data object Career : QuestActionDestination()
    data object Modules : QuestActionDestination()
    data object CompleteInPlace : QuestActionDestination()
}

data class QuestAction(
    val destination: QuestActionDestination,
    val label: String,
)

object QuestDestinationResolver {
    fun resolve(
        priorityTags: String,
        verificationType: String,
        status: String,
    ): QuestAction {
        if (status == QuestStatus.COMPLETED.name || status == QuestStatus.LOCKED.name) {
            return QuestAction(QuestActionDestination.CompleteInPlace, "COMPLETE")
        }

        val inProgress = status == QuestStatus.IN_PROGRESS.name
        val startOrContinue = if (inProgress) "CONTINUE" else "START"
        val verification = verificationType.uppercase()

        when (verification) {
            VerificationType.TIMER.name ->
                return QuestAction(QuestActionDestination.Modules, startOrContinue)
            VerificationType.METRIC_THRESHOLD.name ->
                return QuestAction(QuestActionDestination.Modules, "LOG")
        }

        return when (ModuleScope.moduleForPriorityTags(priorityTags)) {
            ModuleId.WORKOUT -> QuestAction(QuestActionDestination.Fitness, startOrContinue)
            ModuleId.DIET -> QuestAction(QuestActionDestination.Nutrition, "LOG")
            ModuleId.CAREER -> QuestAction(QuestActionDestination.Career, startOrContinue)
            ModuleId.GLOBAL -> when (verification) {
                VerificationType.COUNT.name ->
                    QuestAction(QuestActionDestination.Career, startOrContinue)
                else ->
                    QuestAction(QuestActionDestination.CompleteInPlace, "COMPLETE")
            }
        }
    }

    fun navigates(destination: QuestActionDestination): Boolean =
        destination !is QuestActionDestination.CompleteInPlace

    fun dispatch(
        action: QuestAction,
        onFitness: () -> Unit,
        onNutrition: () -> Unit,
        onCareer: () -> Unit,
        onModules: () -> Unit,
        onCompleteInPlace: () -> Unit,
    ) {
        when (action.destination) {
            QuestActionDestination.Fitness -> onFitness()
            QuestActionDestination.Nutrition -> onNutrition()
            QuestActionDestination.Career -> onCareer()
            QuestActionDestination.Modules -> onModules()
            QuestActionDestination.CompleteInPlace -> onCompleteInPlace()
        }
    }
}
