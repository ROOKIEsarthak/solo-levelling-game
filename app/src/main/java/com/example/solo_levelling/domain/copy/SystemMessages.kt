package com.example.solo_levelling.domain.copy

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
            "BETTER THAN BEFORE.\nKEEP MOVING.",
            "KEEP BUILDING.",
            "ONE LEVEL FURTHER.",
        ),
        Intensity.Medium to listOf(
            "THE WORK IS SHOWING.\nDON'T STOP NOW.",
            "CONSISTENCY IS WORKING.",
            "YOU'RE GETTING STRONGER.",
        ),
        Intensity.Large to listOf(
            "YOU DIDN'T CHANGE OVERNIGHT.\nYOU CHANGED EVERY DAY.",
            "YOU DIDN'T STOP.\nKEEP THE MOMENTUM.",
            "DISCIPLINE IS STARTING TO SHOW.",
        ),
        Intensity.Major to listOf(
            "YOU ARE NOT WHERE YOU STARTED.\nAND YOU'RE NOT DONE.",
            "YOU DIDN'T GET LUCKIER.\nYOU GOT BETTER.",
            "THIS IS WHAT CONSISTENCY LOOKS LIKE.",
        ),
        Intensity.Exceptional to listOf(
            "THE OLD VERSION OF YOU\nWOULDN'T RECOGNIZE THIS ONE.",
            "THE OLD VERSION OF YOU\nWOULDN'T HAVE MADE IT THIS FAR.",
            "YOU BUILT THIS.",
        ),
        Intensity.Baseline to listOf(
            "BUILDING YOUR BASELINE.\nKEEP SHOWING UP.",
            "KEEP BUILDING.\nTHE SCORE COMES WITH TIME.",
        ),
    )

    private val messages = mapOf(
        Category.Consistency to listOf(
            "YOU SHOWED UP. AGAIN.",
            "THE NUMBERS DON'T LIE.\nYOU'RE IMPROVING.",
            "YOU'RE BETTER THAN YOU WERE.\nKEEP GOING.",
        ),
        Category.Workout to listOf(
            "YOU SHOWED UP.",
            "THE SESSION COUNTS.",
            "STRENGTH COMPOUNDS.",
        ),
        Category.Diet to listOf(
            "CONSISTENCY BUILDS RESULTS.",
            "FUEL LOGGED.",
            "NUTRITION IS PART OF THE SYSTEM.",
        ),
        Category.Streak to listOf(
            "YOU KEPT SHOWING UP.",
            "DON'T BREAK THE MOMENTUM.",
            "CONSISTENCY WON.",
        ),
        Category.Recovery to listOf(
            "THE STREAK ENDED.\nTHE JOURNEY DIDN'T.",
            "RESET.\nREBUILD.\nCONTINUE.",
            "NOW GET BACK UP.",
            "START REBUILDING.",
        ),
        Category.PersonalBest to listOf(
            "YOU JUST BEAT\nYOUR OLD RECORD.",
            "NEW PERSONAL BEST.\nCOMPETE WITH YOURSELF.",
            "THE BAR MOVED.\nYOU MOVED IT.",
        ),
        Category.Milestone to listOf(
            "THIS IS NO LONGER\nJUST MOTIVATION.\nTHIS IS DISCIPLINE.",
            "YOU BUILT A HABIT\nWHEN NOBODY WAS WATCHING.",
            "TWO WEEKS OF CONSISTENCY.\nDON'T BREAK THE MOMENTUM.",
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
        val index = kotlin.math.abs(seed) % pool.size
        return pool[index]
    }

    fun pickLevelUp(intensity: Intensity, seed: Int): String {
        val pool = levelUpByIntensity[intensity].orEmpty()
        if (pool.isEmpty()) return "KEEP BUILDING."
        return pool[kotlin.math.abs(seed) % pool.size]
    }

    fun missionComplete(xp: Int): String = "✓ MISSION COMPLETE\n+$xp XP"

    fun workoutComplete(xp: Int): String =
        "✓ WORKOUT COMPLETE\n+$xp XP\n${pick(Category.Workout, xp)}"

    fun nutritionLogged(xp: Int): String =
        "✓ NUTRITION LOGGED\n+$xp XP\n${pick(Category.Diet, xp)}"

    fun streakMilestone(days: Int): String? = when (days) {
        7 -> "7 DAYS.\nYOU KEPT SHOWING UP."
        14 -> "14 DAYS.\nTWO WEEKS OF CONSISTENCY.\nDON'T BREAK THE MOMENTUM."
        30 -> "30 DAYS.\nTHIS IS NO LONGER\nJUST MOTIVATION.\nTHIS IS DISCIPLINE."
        60 -> "60 DAYS.\nYOU BUILT A HABIT\nWHEN NOBODY WAS WATCHING."
        100 -> "100 DAYS.\nCONSISTENCY WON."
        else -> null
    }

    const val FALL_QUESTION = "Why do we fall, Bruce?"
    const val FALL_ANSWER = "So that we can learn to pick ourselves up."
    const val FALL_ATTRIBUTION = "— Alfred, Batman Begins"
}
