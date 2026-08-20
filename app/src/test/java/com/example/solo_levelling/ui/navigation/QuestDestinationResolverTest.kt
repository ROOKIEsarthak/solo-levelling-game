package com.example.solo_levelling.ui.navigation

import com.example.solo_levelling.domain.model.QuestStatus
import com.example.solo_levelling.domain.model.QuestType
import com.example.solo_levelling.domain.model.VerificationType
import com.example.solo_levelling.domain.service.MilestoneRequirement
import com.example.solo_levelling.ui.quests.milestoneRequirementAction
import com.example.solo_levelling.ui.quests.milestoneRequirementOpenLabel
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
    fun p_careerModule_startsCareerDsa() {
        val action = QuestDestinationResolver.resolve(
            priorityTags = "module_career",
            verificationType = VerificationType.COUNT.name,
            status = QuestStatus.AVAILABLE.name,
            templateKey = "dsa_daily",
        )
        assertEquals(QuestActionDestination.Career, action.destination)
        assertEquals("START", action.label)
        assertEquals(QuestDestinationResolver.SECTION_DSA, action.section)
    }

    @Test
    fun p_systemDesign_opensCareerSystemDesignTab() {
        val action = QuestDestinationResolver.resolve(
            priorityTags = "module_career,career,system_design",
            verificationType = VerificationType.MANUAL.name,
            status = QuestStatus.AVAILABLE.name,
            templateKey = "system_design",
        )
        assertEquals(QuestActionDestination.Career, action.destination)
        assertEquals(QuestDestinationResolver.SECTION_SYSTEM_DESIGN, action.section)
        assertEquals(true, QuestDestinationResolver.navigates(action.destination))
    }

    @Test
    fun p_timer_startsModulesFocus() {
        val action = QuestDestinationResolver.resolve(
            priorityTags = "",
            verificationType = VerificationType.TIMER.name,
            status = QuestStatus.AVAILABLE.name,
            templateKey = "deep_work",
        )
        assertEquals(QuestActionDestination.Modules, action.destination)
        assertEquals("START", action.label)
        assertEquals(QuestDestinationResolver.SECTION_FOCUS, action.section)
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
        assertEquals(QuestDestinationResolver.SECTION_FOCUS, action.section)
    }

    @Test
    fun p_metricThreshold_logsModules_evenWithWorkoutTag() {
        val action = QuestDestinationResolver.resolve(
            priorityTags = "module_workout",
            verificationType = VerificationType.METRIC_THRESHOLD.name,
            status = QuestStatus.AVAILABLE.name,
            templateKey = "steps",
        )
        assertEquals(QuestActionDestination.Modules, action.destination)
        assertEquals("LOG", action.label)
        assertEquals(QuestDestinationResolver.SECTION_METRICS, action.section)
    }

    @Test
    fun r_journal_opensModulesInsteadOfCompleting() {
        val action = QuestDestinationResolver.resolve(
            priorityTags = "",
            verificationType = VerificationType.MANUAL.name,
            status = QuestStatus.AVAILABLE.name,
            templateKey = "journal",
        )
        assertEquals(QuestActionDestination.Modules, action.destination)
        assertEquals("WRITE", action.label)
        assertEquals(QuestDestinationResolver.SECTION_JOURNAL, action.section)
        assertEquals(true, QuestDestinationResolver.navigates(action.destination))
    }

    @Test
    fun p_recovery_completesInPlace() {
        val action = QuestDestinationResolver.resolve(
            priorityTags = "",
            verificationType = VerificationType.MANUAL.name,
            status = QuestStatus.AVAILABLE.name,
            templateKey = "recovery",
            questType = QuestType.RECOVERY.name,
        )
        assertEquals(QuestActionDestination.CompleteInPlace, action.destination)
        assertEquals("COMPLETE", action.label)
        assertEquals(false, QuestDestinationResolver.navigates(action.destination))
    }

    @Test
    fun p_milestoneManual_completesInPlace() {
        val action = QuestDestinationResolver.resolve(
            priorityTags = "",
            verificationType = VerificationType.MANUAL.name,
            status = QuestStatus.AVAILABLE.name,
            templateKey = "first_week_complete",
            questType = QuestType.MILESTONE.name,
        )
        assertEquals(QuestActionDestination.CompleteInPlace, action.destination)
        assertEquals(false, QuestDestinationResolver.navigates(action.destination))
    }

    @Test
    fun n_automatic_doesNotCompleteInPlace() {
        val action = QuestDestinationResolver.resolve(
            priorityTags = "",
            verificationType = VerificationType.AUTOMATIC.name,
            status = QuestStatus.AVAILABLE.name,
            templateKey = "weekly_review",
        )
        assertEquals(QuestActionDestination.AwaitVerification, action.destination)
        assertEquals("", action.label)
        assertEquals(false, QuestDestinationResolver.navigates(action.destination))
    }

    @Test
    fun e_manualGlobalInProgress_completesInPlaceWhenNoDestination() {
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
        assertEquals("fitness", dispatchTarget(action))
        assertEquals(true, QuestDestinationResolver.navigates(action.destination))
    }

    @Test
    fun r_journalDispatch_navigatesModulesInsteadOfCompleting() {
        val action = QuestDestinationResolver.resolve(
            priorityTags = "",
            verificationType = VerificationType.MANUAL.name,
            status = QuestStatus.AVAILABLE.name,
            templateKey = "journal",
        )
        assertEquals("modules:journal", dispatchTarget(action))
    }

    @Test
    fun p_recoveryDispatch_completesInPlace() {
        val action = QuestDestinationResolver.resolve(
            priorityTags = "",
            verificationType = VerificationType.MANUAL.name,
            status = QuestStatus.AVAILABLE.name,
            templateKey = "recovery",
            questType = QuestType.RECOVERY.name,
        )
        assertEquals("complete", dispatchTarget(action))
    }

    @Test
    fun n_automaticDispatch_doesNothing() {
        val action = QuestDestinationResolver.resolve(
            priorityTags = "",
            verificationType = VerificationType.AUTOMATIC.name,
            status = QuestStatus.AVAILABLE.name,
            templateKey = "weekly_review",
        )
        assertEquals("", dispatchTarget(action))
    }

    @Test
    fun p_dietDispatch_navigatesNutrition() {
        val action = QuestDestinationResolver.resolve(
            priorityTags = "module_diet",
            verificationType = VerificationType.MANUAL.name,
            status = QuestStatus.AVAILABLE.name,
        )
        assertEquals("nutrition", dispatchTarget(action))
    }

    @Test
    fun p_systemDesignDispatch_opensCareerSection() {
        val action = QuestDestinationResolver.resolve(
            priorityTags = "module_career,system_design",
            verificationType = VerificationType.MANUAL.name,
            status = QuestStatus.AVAILABLE.name,
            templateKey = "system_design",
        )
        assertEquals("career:system_design", dispatchTarget(action))
    }

    @Test
    fun n_completedDispatch_doesNotNavigate() {
        val action = QuestDestinationResolver.resolve(
            priorityTags = "module_workout",
            verificationType = VerificationType.MANUAL.name,
            status = QuestStatus.COMPLETED.name,
        )
        assertEquals("complete", dispatchTarget(action))
        assertEquals(false, QuestDestinationResolver.navigates(action.destination))
    }

    @Test
    fun r_incompleteMilestoneRequirements_openLabels() {
        val workout = milestoneRequirementAction(
            MilestoneRequirement(
                instanceId = 1,
                title = "Complete workout",
                completed = false,
                templateKey = "workout_daily",
                priorityTags = "module_workout",
                verificationType = VerificationType.MANUAL.name,
                questType = "DAILY",
                status = QuestStatus.AVAILABLE.name,
            ),
        )
        val diet = milestoneRequirementAction(
            MilestoneRequirement(
                instanceId = 2,
                title = "Log nutrition",
                completed = false,
                templateKey = "nutrition_daily",
                priorityTags = "module_diet",
                verificationType = VerificationType.MANUAL.name,
                questType = "DAILY",
                status = QuestStatus.AVAILABLE.name,
            ),
        )
        val journal = milestoneRequirementAction(
            MilestoneRequirement(
                instanceId = 3,
                title = "Write a short journal",
                completed = false,
                templateKey = "journal",
                priorityTags = "",
                verificationType = VerificationType.MANUAL.name,
                questType = "DAILY",
                status = QuestStatus.AVAILABLE.name,
            ),
        )
        assertEquals("OPEN TRAINING", milestoneRequirementOpenLabel(workout))
        assertEquals("OPEN NUTRITION", milestoneRequirementOpenLabel(diet))
        assertEquals("OPEN JOURNAL", milestoneRequirementOpenLabel(journal))
        assertEquals(QuestActionDestination.Fitness, workout.destination)
        assertEquals(QuestActionDestination.Nutrition, diet.destination)
        assertEquals(QuestActionDestination.Modules, journal.destination)
    }

    private fun dispatchTarget(action: QuestAction): String {
        var called = ""
        QuestDestinationResolver.dispatch(
            action = action,
            onFitness = { called = "fitness" },
            onNutrition = { called = "nutrition" },
            onCareer = { called = "career:$it" },
            onModules = { called = "modules:$it" },
            onCompleteInPlace = { called = "complete" },
        )
        return called
    }
}
