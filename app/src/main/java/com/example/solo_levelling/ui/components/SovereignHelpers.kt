package com.example.solo_levelling.ui.components

/** Pure helpers for Sovereign OS chrome — unit-testable without Compose runtime. */

enum class NavFamily { Home, Quests, Progress, Character, More, None }

data class AttributeDisplay(
    val code: String,
    val value: Int,
    val lifetimeXp: Int,
    val fraction: Float,
)

data class AttributeInsight(
    val strongestCode: String?,
    val strongestValue: Int,
    val lowestCode: String?,
    val lowestValue: Int,
)

data class AttributePresentation(
    val code: String,
    val displayName: String,
    val cues: String,
    val meaning: String,
)

private val attributePresentations = mapOf(
    "STR" to AttributePresentation(
        "STR", "Strength",
        "Physical capability · power · effort",
        "Physical capability, power, and the ability to challenge yourself physically.",
    ),
    "END" to AttributePresentation(
        "END", "Endurance",
        "Persistence · stamina · resilience",
        "The ability to sustain effort and continue when motivation fades.",
    ),
    "INT" to AttributePresentation(
        "INT", "Intelligence",
        "Learning · reasoning · problem solving",
        "The ability to learn, understand, solve problems, and build useful knowledge.",
    ),
    "VIT" to AttributePresentation(
        "VIT", "Vitality",
        "Health · energy · recovery",
        "Physical wellbeing, energy, recovery, and care for the body.",
    ),
    "DISC" to AttributePresentation(
        "DISC", "Discipline",
        "Consistency · commitment · follow-through",
        "The ability to follow through on commitments and act when motivation is low.",
    ),
    "FOC" to AttributePresentation(
        "FOC", "Focus",
        "Attention · presence · deep work",
        "The ability to protect attention, stay present, and work without distraction.",
    ),
    "WIS" to AttributePresentation(
        "WIS", "Wisdom",
        "Reflection · perspective · learning",
        "The ability to reflect, learn from experience, and make better decisions.",
    ),
)

fun attributePresentation(code: String): AttributePresentation {
    val key = code.uppercase()
    return attributePresentations[key] ?: AttributePresentation(
        code = key,
        displayName = key,
        cues = "Growth",
        meaning = "A dimension of your progression.",
    )
}

fun attributeDisplayName(code: String): String = attributePresentation(code).displayName

fun progressFraction(current: Float, target: Float): Float {
    if (target <= 0f) return 0f
    return (current / target).coerceIn(0f, 1f)
}

fun bracketize(label: String): String {
    val trimmed = label.trim()
    if (trimmed.startsWith("[") && trimmed.endsWith("]")) return trimmed
    return "[ $trimmed ]"
}

fun displayLabel(label: String, bracketed: Boolean = false): String =
    if (bracketed) bracketize(label) else label.trim()

fun navFamilyForRoute(route: String?): NavFamily = when (route) {
    "dashboard" -> NavFamily.Home
    "quests" -> NavFamily.Quests
    "analytics", "history", "achievements" -> NavFamily.Progress
    "character" -> NavFamily.Character
    "more", "settings", "modules", "career", "fitness", "nutrition" -> NavFamily.More
    else -> NavFamily.None
}

fun primaryRouteForFamily(family: NavFamily): String? = when (family) {
    NavFamily.Home -> "dashboard"
    NavFamily.Quests -> "quests"
    NavFamily.Progress -> "analytics"
    NavFamily.Character -> "character"
    NavFamily.More -> "more"
    NavFamily.None -> null
}

/** Relative bars vs the highest attribute value — no invented weekly deltas. */
fun attributeDisplays(
    codes: List<String>,
    values: List<Int>,
    lifetimeXp: List<Int> = emptyList(),
): List<AttributeDisplay> {
    if (codes.isEmpty()) return emptyList()
    val max = values.maxOrNull()?.coerceAtLeast(1) ?: 1
    return codes.indices.map { i ->
        val value = values.getOrElse(i) { 0 }
        AttributeDisplay(
            code = codes[i],
            value = value,
            lifetimeXp = lifetimeXp.getOrElse(i) { 0 },
            fraction = (value.toFloat() / max.toFloat()).coerceIn(0f, 1f),
        )
    }
}

fun attributeInsight(codes: List<String>, values: List<Int>): AttributeInsight {
    if (codes.isEmpty() || values.isEmpty()) {
        return AttributeInsight(null, 0, null, 0)
    }
    val pairs = codes.indices.map { codes[it] to values.getOrElse(it) { 0 } }
    val strongest = pairs.maxByOrNull { it.second }
    val lowest = pairs.minByOrNull { it.second }
    return AttributeInsight(
        strongestCode = strongest?.first,
        strongestValue = strongest?.second ?: 0,
        lowestCode = lowest?.first,
        lowestValue = lowest?.second ?: 0,
    )
}

fun topAttributeDisplays(
    displays: List<AttributeDisplay>,
    limit: Int = 3,
): List<AttributeDisplay> =
    displays.sortedByDescending { it.value }.take(limit.coerceAtLeast(0))

fun formatAttributeRewards(rewardsJson: String): String {
    if (rewardsJson.isBlank() || rewardsJson == "{}") return ""
    val matches = Regex("\"([A-Z]+)\"\\s*:\\s*(-?\\d+)").findAll(rewardsJson)
    return matches.joinToString("  ") { match ->
        val name = attributeDisplayName(match.groupValues[1])
        val amount = match.groupValues[2].toIntOrNull() ?: 0
        if (amount >= 0) "+$amount $name" else "$amount $name"
    }
}

/** Calm insight — never “weakest / failing / bad”. */
fun attributeGrowthInsight(insight: AttributeInsight): String {
    val lowest = insight.lowestCode ?: return ""
    val strongest = insight.strongestCode
    if (strongest != null && strongest != lowest && insight.strongestValue != insight.lowestValue) {
        return "${attributeDisplayName(strongest)} is currently your strongest developed attribute. " +
            "${attributeDisplayName(lowest)} has more room for investment."
    }
    return "${attributeDisplayName(lowest)} has more room for investment."
}

fun areaToInvestCopy(code: String?): String {
    if (code.isNullOrBlank()) return ""
    return "${attributeDisplayName(code)} — an area to invest in."
}

fun humanizeSuggestionTitle(title: String): String {
    var result = title
    attributePresentations.keys.forEach { code ->
        result = result.replace(code, attributeDisplayName(code))
    }
    return result
}

private val weakestCareerSignalDetail = Regex(
    """ is your weakest career signal at (\d+)%\.""",
    RegexOption.IGNORE_CASE,
)

/** User-facing detail from [PriorityEngine] — full names, calm tone. */
fun humanizeNextActionDetail(detail: String): String {
    var result = humanizeSuggestionTitle(detail)
    result = weakestCareerSignalDetail.replace(result) { match ->
        " is at ${match.groupValues[1]}% — a good place to focus next."
    }
    return result
}

fun questRankForXp(baseXp: Int): String = when {
    baseXp >= 100 -> "S"
    baseXp >= 50 -> "A"
    baseXp >= 25 -> "B"
    baseXp >= 10 -> "C"
    else -> "D"
}

fun greetingForHour(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Welcome back"
}

fun streakSupportCopy(current: Int): String = when {
    current <= 0 -> "Consistency starts with one day."
    current == 1 -> "You're building consistency."
    current < 7 -> "You're building consistency."
    else -> "Consistency has become part of your routine."
}

fun xpProgressLabel(intoLevel: Int, need: Int): String =
    "${intoLevel.coerceAtLeast(0)} / ${need.coerceAtLeast(1)} XP"

fun xpToNextLabel(intoLevel: Int, need: Int): String {
    val remaining = (need - intoLevel).coerceAtLeast(0)
    return if (remaining == 0) "Ready for the next level"
    else "$remaining XP to the next level"
}
