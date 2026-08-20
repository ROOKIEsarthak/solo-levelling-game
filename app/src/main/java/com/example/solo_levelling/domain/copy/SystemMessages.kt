package com.example.solo_levelling.domain.copy

import com.example.solo_levelling.domain.logic.DayRelation

/**
 * Mentor-tone system copy. Screens pick from categories — do not hardcode motivational lines in UI.
 */
object SystemMessages {
    enum class Category {
        LevelUp,
        Consistency,
        Workout,
        Diet,
        Streak,
        Recovery,
        PersonalBest,
        Milestone,
    }

    enum class MotivationContext {
        DailyStart,
        FirstQuest,
        QuestCompleted,
        DifficultQuestCompleted,
        StreakMilestone,
        StreakBroken,
        Recovery,
        LevelMilestone,
        RankMilestone,
        AttributeImproved,
        WeeklyReview,
        NoQuests,
        ReturningUser,
    }

    enum class Intensity {
        Small,
        Medium,
        Large,
        Major,
        Exceptional,
        Baseline,
    }

    private val levelUpByIntensity = mapOf(
        Intensity.Small to listOf(
            "Another step forward.",
            "Small gains compound.",
            "You are not where you started.",
        ),
        Intensity.Medium to listOf(
            "The work is showing.",
            "Consistency is working.",
            "You followed through again.",
        ),
        Intensity.Large to listOf(
            "You didn't change overnight. You changed every day.",
            "Discipline is starting to show.",
            "Progress does not need to be loud to be real.",
        ),
        Intensity.Major to listOf(
            "You are not where you started. And you are not done.",
            "You didn't get luckier. You got better.",
            "This is what consistency looks like.",
        ),
        Intensity.Exceptional to listOf(
            "The old version of you wouldn't recognize this one.",
            "You built this through ordinary days.",
            "The work compounds.",
        ),
        Intensity.Baseline to listOf(
            "Keep showing up. The score comes with time.",
            "Building your baseline.",
            "Start small. Start now.",
        ),
    )

    private val messages = mapOf(
        Category.Consistency to listOf(
            "You showed up again.",
            "You are becoming more consistent.",
            "You followed through.",
        ),
        Category.Workout to listOf(
            "You showed up for your body.",
            "The session counts.",
            "Strength compounds.",
        ),
        Category.Diet to listOf(
            "Consistency builds results.",
            "Fuel logged.",
            "Small choices add up.",
        ),
        Category.Streak to listOf(
            "Consistency is becoming a habit.",
            "You kept the rhythm.",
            "Ordinary days build lasting change.",
        ),
        Category.Recovery to listOf(
            "One break does not erase the journey.",
            "Reset. Reflect. Continue.",
            "You are not starting over. You are starting again with experience.",
            "Begin again.",
        ),
        Category.PersonalBest to listOf(
            "You just beat your old record.",
            "Compete with yourself.",
            "The bar moved. You moved it.",
        ),
        Category.Milestone to listOf(
            "Consistency is becoming discipline.",
            "You built a habit when nobody was watching.",
            "Progress becomes visible when you look back.",
        ),
    )

    private val contextPools = mapOf(
        MotivationContext.DailyStart to listOf(
            "You don't have to finish everything today. Just move forward.",
            "Take the next step.",
            "Discipline becomes easier when you stop waiting to feel motivated.",
        ),
        MotivationContext.FirstQuest to listOf(
            "Start small. Start now.",
            "Every journey begins with one step.",
            "You showed up. That matters.",
        ),
        MotivationContext.QuestCompleted to listOf(
            "Another step forward.",
            "You followed through.",
            "You finished what you started.",
            "One completed task is still one step closer.",
        ),
        MotivationContext.DifficultQuestCompleted to listOf(
            "The difficult work is often the work that changes us.",
            "You chose to follow through.",
            "You made time for what matters.",
        ),
        MotivationContext.StreakMilestone to listOf(
            "Consistency is becoming a habit.",
            "Consistency is built one decision at a time.",
            "The work you do today becomes the person you are tomorrow.",
        ),
        MotivationContext.StreakBroken to listOf(
            "One difficult day does not define your journey.",
            "One break does not erase the journey.",
            "Some days are for finding your footing.",
        ),
        MotivationContext.Recovery to listOf(
            "Why do we fall? So we can learn to rise again.",
            "Begin again.",
            "You are not starting over. You are starting again with experience.",
        ),
        MotivationContext.LevelMilestone to listOf(
            "You are not where you started.",
            "Progress recorded.",
            "A better version of you is built in ordinary moments.",
        ),
        MotivationContext.RankMilestone to listOf(
            "Growth becomes visible when effort becomes consistent.",
            "You are becoming more consistent.",
            "The work compounds.",
        ),
        MotivationContext.AttributeImproved to listOf(
            "Small gains compound.",
            "Where you invest your time shapes who you become.",
            "Showing improvement.",
        ),
        MotivationContext.WeeklyReview to listOf(
            "Progress becomes visible when you look back.",
            "Reflect. Then continue.",
            "The goal isn't perfection. The goal is progress.",
        ),
        MotivationContext.NoQuests to listOf(
            "Your next step will appear here.",
            "Every journey begins with one step.",
            "When you are ready, begin.",
        ),
        MotivationContext.ReturningUser to listOf(
            "Welcome back.",
            "Continue where you left off.",
            "Your future self benefits from what you choose today.",
        ),
    )

    fun intensityForImprovement(percent: Float?): Intensity {
        if (percent == null) return Intensity.Baseline
        return when {
            percent < 5f -> Intensity.Small
            percent < 12f -> Intensity.Medium
            percent < 20f -> Intensity.Large
            percent < 35f -> Intensity.Major
            else -> Intensity.Exceptional
        }
    }

    fun pick(category: Category, seed: Int): String {
        val pool = when (category) {
            Category.LevelUp -> levelUpByIntensity[Intensity.Medium].orEmpty()
            else -> messages[category].orEmpty()
        }
        if (pool.isEmpty()) return ""
        return pool[kotlin.math.abs(seed) % pool.size]
    }

    fun pickLevelUp(intensity: Intensity, seed: Int): String {
        val pool = levelUpByIntensity[intensity].orEmpty()
        if (pool.isEmpty()) return "Keep building."
        return pool[kotlin.math.abs(seed) % pool.size]
    }

    fun forContext(context: MotivationContext, seed: Int): String {
        val pool = contextPools[context].orEmpty()
        if (pool.isEmpty()) return ""
        return pool[kotlin.math.abs(seed) % pool.size]
    }

    fun questCompletedFeedback(xp: Int, seed: Int = xp, difficult: Boolean = false): String {
        val ctx = if (difficult) MotivationContext.DifficultQuestCompleted else MotivationContext.QuestCompleted
        return "+$xp XP\n${forContext(ctx, seed)}"
    }

    fun milestoneCompletedFeedback(xp: Int, seed: Int = xp): String =
        "+$xp XP\nMilestone complete.\n${pick(Category.Milestone, seed)}"

    fun missionComplete(xp: Int): String = questCompletedFeedback(xp)

    fun workoutComplete(xp: Int): String =
        "+$xp XP\n${pick(Category.Workout, xp)}"

    fun nutritionLogged(xp: Int): String =
        "+$xp XP\n${pick(Category.Diet, xp)}"

    const val MEAL_TRACKING_COMPLETE_NO_TARGETS =
        "Your meal tracking is progressing. Complete your daily meal target."
    const val MEAL_TRACKING_PROGRESS_NO_TARGETS =
        "Complete your nutrition profile to receive personalized macro guidance."
    const val MEAL_TRACKING_COMPLETE_PROFILE_HINT =
        "Complete your nutrition profile to receive personalized macro guidance."

    fun proteinBelowTarget(): String =
        "Protein is still below today's target. Consider prioritizing a protein-rich next meal."
    fun proteinBelowTargetTomorrow(): String =
        "Protein was below target today. Consider prioritizing it tomorrow."
    fun carbsBelowTarget(): String =
        "Carbohydrates are still below today's target. Consider adding a balanced carbohydrate source."
    fun fatHighToday(): String =
        "Fat intake is already relatively high today. Consider a lower-fat option next."
    fun caloriesNearLimit(): String =
        "You're close to today's calorie target. Consider keeping the next meal lighter."
    fun caloriesAboveTargetFatLoss(): String =
        "Today's intake is currently above your calorie target. If this pattern continues consistently, it may make fat-loss progress slower."
    fun nutritionBalanced(): String =
        "Your nutrition is tracking well today."
    fun nutritionCloseToTargetToday(): String =
        "You stayed close to your calorie target today."
    fun mealTrackingCompleteToday(): String =
        "Today's tracking is complete."
    fun proteinRecoveryAfterWorkout(): String =
        "Workout completed today. Protein target remains important for recovery."

    const val JOURNAL_SAVED_FEEDBACK = "Journal saved · +20 XP"

    const val REVERSE_COMPLETION_TITLE = "Reverse completion?"
    const val REVERSE_COMPLETION_EXPLANATION = "This quest is already recorded as completed."
    const val REVERSE_COMPLETION_CONSEQUENCE = "Reversing it will remove the progress from this completion."
    const val REVERSE_COMPLETION_KEEP = "Keep completed"
    const val REVERSE_COMPLETION_CONFIRM = "Reverse completion"
    const val COMPLETION_REVERSED = "Completion reversed."
    const val COMPLETION_REVERSE_FAILED = "Could not reverse completion. The undo window may have expired."

    const val DELETE_ENTRY_TITLE = "Delete this entry?"
    const val DELETE_ENTRY_PROGRESS_EXPLANATION = "This record contributes to your progress."
    const val DELETE_ENTRY_PROGRESS_CONSEQUENCE = "Deleting it will remove the progress associated with this activity."
    const val DELETE_ENTRY_NO_PROGRESS_EXPLANATION = "This action cannot be undone."
    const val DELETE_ENTRY_KEEP = "Keep entry"
    const val DELETE_ENTRY_CONFIRM = "Delete"

    fun streakMilestone(days: Int): String? = when (days) {
        7 -> "7 days.\n${forContext(MotivationContext.StreakMilestone, 7)}"
        14 -> "14 days.\n${forContext(MotivationContext.StreakMilestone, 14)}"
        30 -> "30 days.\n${forContext(MotivationContext.StreakMilestone, 30)}"
        60 -> "60 days.\n${forContext(MotivationContext.StreakMilestone, 60)}"
        100 -> "100 days.\n${forContext(MotivationContext.StreakMilestone, 100)}"
        else -> null
    }

    const val FALL_QUESTION = "Why do we fall, Bruce?"
    const val FALL_ANSWER = "So that we can learn to pick ourselves up."
    const val FALL_ATTRIBUTION = "— Alfred, Batman Begins"

    const val DATE_TODAY_ACTION = "Log what you actually completed today."
    const val DATE_PAST_REVIEW =
        "This day is for review. Progress is based on what you complete today."
    const val DATE_FUTURE_PLAN =
        "Upcoming days can be planned through your weekly split. Log the work when the day arrives."
    const val DATE_QUEST_WRONG_DAY =
        "This quest belongs to an earlier day. Review it in History instead of changing today's progress."

    fun dateGuidance(relation: DayRelation): String = when (relation) {
        DayRelation.Today -> DATE_TODAY_ACTION
        DayRelation.Past -> DATE_PAST_REVIEW
        DayRelation.Future -> DATE_FUTURE_PLAN
    }
}
