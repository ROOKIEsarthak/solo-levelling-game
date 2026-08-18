package com.example.solo_levelling.ui.navigation

import com.example.solo_levelling.domain.model.QuestStatus
import com.example.solo_levelling.domain.model.VerificationType
import org.junit.Assert.assertEquals
import org.junit.Test

class QuestDestinationResolverTest {
    @Test
    fun p_workoutModule_startsFitness() {
        val action = QuestDestinationResolver.resolve(
            priorityTags = "module_workout",
            verificationType = VerificationType.MANUAL.name,
            status = QuestStatus.AVAILABLE.name,
        )
        assertEquals(QuestActionDestination.Fitness, action.destination)
        assertEquals("START", action.label)
        assertEquals(true, QuestDestinationResolver.navigates(action.destination))
    }

    @Test
    fun p_dietModule_logsNutrition() {
        val action = QuestDestinationResolver.resolve(
            priorityTags = "module_diet,health",
            verificationType = VerificationType.MANUAL.name,
            status = QuestStatus.AVAILABLE.name,
        )
        assertEquals(QuestActionDestination.Nutrition, action.destination)
        assertEquals("LOG", action.label)
    }

    @Test
    fun p_careerModule_startsCareer() {
        val action = QuestDestinationResolver.resolve(
            priorityTags = "module_career",
            verificationType = VerificationType.COUNT.name,
            status = QuestStatus.AVAILABLE.name,
        )
        assertEquals(QuestActionDestination.Career, action.destination)
        assertEquals("START", action.label)
    }

    @Test
    fun p_timer_startsModules() {
        val action = QuestDestinationResolver.resolve(
            priorityTags = "",
            verificationType = VerificationType.TIMER.name,
            status = QuestStatus.AVAILABLE.name,
        )
        assertEquals(QuestActionDestination.Modules, action.destination)
        assertEquals("START", action.label)
    }

    @Test
    fun p_timerInProgress_continuesModules() {
        val action = QuestDestinationResolver.resolve(
            priorityTags = "",
            verificationType = VerificationType.TIMER.name,
            status = QuestStatus.IN_PROGRESS.name,
        )
        assertEquals(QuestActionDestination.Modules, action.destination)
        assertEquals("CONTINUE", action.label)
    }

    @Test
    fun p_metricThreshold_logsModules_evenWithWorkoutTag() {
        val action = QuestDestinationResolver.resolve(
            priorityTags = "module_workout",
            verificationType = VerificationType.METRIC_THRESHOLD.name,
            status = QuestStatus.AVAILABLE.name,
        )
        assertEquals(QuestActionDestination.Modules, action.destination)
        assertEquals("LOG", action.label)
    }

    @Test
    fun p_manualGlobal_completesInPlace() {
        val action = QuestDestinationResolver.resolve(
            priorityTags = "",
            verificationType = VerificationType.MANUAL.name,
            status = QuestStatus.AVAILABLE.name,
        )
        assertEquals(QuestActionDestination.CompleteInPlace, action.destination)
        assertEquals("COMPLETE", action.label)
        assertEquals(false, QuestDestinationResolver.navigates(action.destination))
    }

    @Test
    fun p_automaticGlobal_completesInPlace() {
        val action = QuestDestinationResolver.resolve(
            priorityTags = "",
            verificationType = VerificationType.AUTOMATIC.name,
            status = QuestStatus.AVAILABLE.name,
        )
        assertEquals(QuestActionDestination.CompleteInPlace, action.destination)
        assertEquals("COMPLETE", action.label)
    }

    @Test
    fun e_manualGlobalInProgress_completesInPlace() {
        val action = QuestDestinationResolver.resolve(
            priorityTags = "",
            verificationType = VerificationType.MANUAL.name,
            status = QuestStatus.IN_PROGRESS.name,
        )
        assertEquals(QuestActionDestination.CompleteInPlace, action.destination)
        assertEquals("COMPLETE", action.label)
    }

    @Test
    fun e_inProgressWorkout_continue() {
        val action = QuestDestinationResolver.resolve(
            priorityTags = "module_workout",
            verificationType = VerificationType.MANUAL.name,
            status = QuestStatus.IN_PROGRESS.name,
        )
        assertEquals(QuestActionDestination.Fitness, action.destination)
        assertEquals("CONTINUE", action.label)
    }

    @Test
    fun n_completed_noNavigate() {
        val action = QuestDestinationResolver.resolve(
            priorityTags = "module_workout",
            verificationType = VerificationType.MANUAL.name,
            status = QuestStatus.COMPLETED.name,
        )
        assertEquals(QuestActionDestination.CompleteInPlace, action.destination)
        assertEquals(false, QuestDestinationResolver.navigates(action.destination))
    }

    @Test
    fun n_locked_noNavigate() {
        val action = QuestDestinationResolver.resolve(
            priorityTags = "module_career",
            verificationType = VerificationType.COUNT.name,
            status = QuestStatus.LOCKED.name,
        )
        assertEquals(QuestActionDestination.CompleteInPlace, action.destination)
        assertEquals(false, QuestDestinationResolver.navigates(action.destination))
    }

    @Test
    fun r_workoutDispatch_navigatesFitnessInsteadOfCompleting() {
        val action = QuestDestinationResolver.resolve(
            priorityTags = "module_workout",
            verificationType = VerificationType.MANUAL.name,
            status = QuestStatus.AVAILABLE.name,
        )
        var called = ""
        QuestDestinationResolver.dispatch(
            action = action,
            onFitness = { called = "fitness" },
            onNutrition = { called = "nutrition" },
            onCareer = { called = "career" },
            onModules = { called = "modules" },
            onCompleteInPlace = { called = "complete" },
        )
        assertEquals("fitness", called)
        assertEquals(true, QuestDestinationResolver.navigates(action.destination))
    }

    @Test
    fun p_manualDispatch_completesInPlace() {
        val action = QuestDestinationResolver.resolve(
            priorityTags = "",
            verificationType = VerificationType.MANUAL.name,
            status = QuestStatus.AVAILABLE.name,
        )
        var called = ""
        QuestDestinationResolver.dispatch(
            action = action,
            onFitness = { called = "fitness" },
            onNutrition = { called = "nutrition" },
            onCareer = { called = "career" },
            onModules = { called = "modules" },
            onCompleteInPlace = { called = "complete" },
        )
        assertEquals("complete", called)
    }

    @Test
    fun p_dietDispatch_navigatesNutrition() {
        val action = QuestDestinationResolver.resolve(
            priorityTags = "module_diet",
            verificationType = VerificationType.MANUAL.name,
            status = QuestStatus.AVAILABLE.name,
        )
        var called = ""
        QuestDestinationResolver.dispatch(
            action = action,
            onFitness = { called = "fitness" },
            onNutrition = { called = "nutrition" },
            onCareer = { called = "career" },
            onModules = { called = "modules" },
            onCompleteInPlace = { called = "complete" },
        )
        assertEquals("nutrition", called)
    }

    @Test
    fun n_completedDispatch_doesNotNavigate() {
        val action = QuestDestinationResolver.resolve(
            priorityTags = "module_workout",
            verificationType = VerificationType.MANUAL.name,
            status = QuestStatus.COMPLETED.name,
        )
        var called = ""
        QuestDestinationResolver.dispatch(
            action = action,
            onFitness = { called = "fitness" },
            onNutrition = { called = "nutrition" },
            onCareer = { called = "career" },
            onModules = { called = "modules" },
            onCompleteInPlace = { called = "complete" },
        )
        assertEquals("complete", called)
        assertEquals(false, QuestDestinationResolver.navigates(action.destination))
    }
}
