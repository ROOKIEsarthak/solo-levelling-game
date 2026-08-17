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

fun progressFraction(current: Float, target: Float): Float {
    if (target <= 0f) return 0f
    return (current / target).coerceIn(0f, 1f)
}

fun bracketize(label: String): String {
    val trimmed = label.trim()
    if (trimmed.startsWith("[") && trimmed.endsWith("]")) return trimmed
    return "[ $trimmed ]"
}

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
        val code = match.groupValues[1]
        val amount = match.groupValues[2].toIntOrNull() ?: 0
        if (amount >= 0) "+$amount $code" else "$amount $code"
    }
}

fun questRankForXp(baseXp: Int): String = when {
    baseXp >= 100 -> "S"
    baseXp >= 50 -> "A"
    baseXp >= 25 -> "B"
    baseXp >= 10 -> "C"
    else -> "D"
}

fun greetingForHour(hour: Int): String = when (hour) {
    in 5..11 -> "GOOD MORNING"
    in 12..16 -> "GOOD AFTERNOON"
    in 17..21 -> "GOOD EVENING"
    else -> "GOOD NIGHT"
}

fun streakSupportCopy(current: Int): String = when {
    current <= 0 -> "Consistency starts with one day."
    current == 1 -> "You're building consistency."
    current < 7 -> "You're building consistency."
    else -> "Steady progress — keep the rhythm."
}

fun xpProgressLabel(intoLevel: Int, need: Int): String =
    "${intoLevel.coerceAtLeast(0)} / ${need.coerceAtLeast(1)} XP"
