package com.example.solo_levelling.domain.service

import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.core.time.AppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.domain.model.AttributeCode
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.math.min
import org.json.JSONArray
import org.json.JSONObject

data class AttributeSnapshot(
    val topCode: String?,
    val topValue: Int,
    val bottomCode: String?,
    val bottomValue: Int,
)

data class BossProgressSnapshot(
    val title: String,
    val current: Float,
    val target: Float,
)

data class WeeklyReview(
    val weekStart: String,
    val weekEnd: String,
    val questsCompleted: Int,
    val questsTotal: Int,
    val completionRate: Float,
    val xpEarned: Int,
    val dsaSolvedWeek: Int,
    val workoutCountWeek: Int,
    val workoutDaysWeek: Int,
    val bossProgress: BossProgressSnapshot?,
    val attributeSnapshot: AttributeSnapshot,
    val personalScore: Int,
    val recommendations: List<String>,
)

data class AdaptiveSuggestion(
    val key: String,
    val title: String,
    val detail: String,
)

data class PeriodScore(
    val score: Int,
    val questCompletionPct: Float,
    val workoutDays: Int,
    val workoutCount: Int,
    val dsaSolved: Int,
    val questsCompleted: Int,
    val questsTotal: Int,
    val activeDays: Int,
)

data class ImprovementSnapshot(
    val current: PeriodScore,
    val previous: PeriodScore,
    /** Null when baseline is insufficient — never fabricate a percent. */
    val improvementPercent: Float?,
)

data class BeforeVsNow(
    val taskCompletionBefore: Float,
    val taskCompletionNow: Float,
    val workoutDaysBefore: Int,
    val workoutDaysNow: Int,
    val improvementPercent: Float?,
    val dsaSolvedBefore: Int = 0,
    val dsaSolvedNow: Int = 0,
    val dietAdherenceBefore: Int? = null,
    val dietAdherenceNow: Int? = null,
    val weightBefore: Float? = null,
    val weightNow: Float? = null,
    val streakBest: Int = 0,
    val streakCurrent: Int = 0,
)

data class NextUnlock(
    val kind: String,
    val title: String,
    val detail: String,
    val mysterious: Boolean,
)

class AnalyticsService(
    private val db: JsonDatabase,
    private val clock: AppClock,
) {
    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE

    suspend fun weeklyReview(): WeeklyReview {
        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
        val zone = runCatching { ZoneId.of(profile?.timezone ?: ZoneId.systemDefault().id) }
            .getOrDefault(ZoneId.systemDefault())
        val today = clock.today(zone)
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = weekStart.plusDays(6)
        val startStr = weekStart.format(dateFmt)
        val endStr = weekEnd.format(dateFmt)
        val completed = db.questDao().countCompletedInRange(startStr, endStr)
        val total = db.questDao().countTotalInRange(startStr, endStr)
        val startMs = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMs = weekEnd.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val xp = db.xpDao().sumXpBetween(startMs, endMs)
        val rate = completionRate(completed, total)
        val dsaSolved = db.moduleDao().countDsaSolvedInRange(startMs, endMs)
        val workoutCount = db.moduleDao().countWorkoutsInRange(startStr, endStr)
        val workoutDays = db.moduleDao().countWorkoutDaysInRange(startStr, endStr)
        val streak = db.playerDao().getStreak(SystemDefaults.PLAYER_ID)?.current ?: 0
        val personalScore = personalScore(rate, streak, workoutDays, dsaSolved)
        val attrs = db.playerDao().getAttributes()
        val top = attrs.maxByOrNull { it.currentValue }
        val bottom = attrs.minByOrNull { it.currentValue }
        val attributeSnapshot = AttributeSnapshot(
            topCode = top?.code,
            topValue = top?.currentValue ?: 0,
            bottomCode = bottom?.code,
            bottomValue = bottom?.currentValue ?: 0,
        )
        val boss = db.moduleDao().getActiveBoss()
        val bossProgress = boss?.let {
            BossProgressSnapshot(it.title, it.currentValue, it.targetValue)
        }
        val recommendations = buildList {
            if (rate < 0.5f) add("Lower daily quest load slightly and protect one deep-work block.")
            if (rate >= 0.8f) add("Strong execution — consider one harder milestone this week.")
            if (bottom != null) add("Neglected attribute: ${bottom.code} — schedule one related quest.")
        }
        return WeeklyReview(
            weekStart = startStr,
            weekEnd = endStr,
            questsCompleted = completed,
            questsTotal = total,
            completionRate = rate,
            xpEarned = xp,
            dsaSolvedWeek = dsaSolved,
            workoutCountWeek = workoutCount,
            workoutDaysWeek = workoutDays,
            bossProgress = bossProgress,
            attributeSnapshot = attributeSnapshot,
            personalScore = personalScore,
            recommendations = recommendations,
        )
    }

    suspend fun exportJson(): String {
        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
        val attrs = db.playerDao().getAttributes()
        val ledger = db.xpDao().getAllLedger()
        val achievements = db.achievementDao().getUnlocked()
        val root = JSONObject()
        root.put("exportedAt", clock.nowEpochMs())
        root.put(
            "profile",
            JSONObject()
                .put("name", profile?.name)
                .put("level", profile?.level)
                .put("totalXp", profile?.totalXp)
                .put("rank", profile?.rank),
        )
        val attrArr = JSONArray()
        attrs.forEach {
            attrArr.put(JSONObject().put("code", it.code).put("value", it.currentValue))
        }
        root.put("attributes", attrArr)
        val ledgerArr = JSONArray()
        ledger.forEach {
            ledgerArr.put(
                JSONObject()
                    .put("amount", it.amount)
                    .put("sourceType", it.sourceType)
                    .put("sourceId", it.sourceId)
                    .put("at", it.createdAtEpochMs),
            )
        }
        root.put("xpLedger", ledgerArr)
        val achArr = JSONArray()
        achievements.forEach {
            achArr.put(JSONObject().put("key", it.achievementKey).put("at", it.unlockedAtEpochMs))
        }
        root.put("achievements", achArr)
        return root.toString(2)
    }

    fun personalScore(
        questCompletionPct: Float,
        streak: Int,
        workoutDays: Int,
        dsaSolvedWeek: Int,
        modules: EnabledModules = EnabledModules(career = true, workout = true, diet = true),
    ): Int = Companion.personalScore(questCompletionPct, streak, workoutDays, dsaSolvedWeek, modules)

    suspend fun periodScore(
        start: java.time.LocalDate,
        end: java.time.LocalDate,
        streakForScore: Int,
    ): PeriodScore {
        val zone = playerZone()
        val startStr = start.format(dateFmt)
        val endStr = end.format(dateFmt)
        val completed = db.questDao().countCompletedInRange(startStr, endStr)
        val total = db.questDao().countTotalInRange(startStr, endStr)
        val rate = completionRate(completed, total)
        val startMs = start.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMs = end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val dsaSolved = db.moduleDao().countDsaSolvedInRange(startMs, endMs)
        val workoutCount = db.moduleDao().countWorkoutsInRange(startStr, endStr)
        val workoutDays = db.moduleDao().countWorkoutDaysInRange(startStr, endStr)
        val activeDays = estimateActiveDays(workoutDays, completed, total, start, end)
        val modules = ModuleFlags.resolve(
            onboardingDone = db.playerDao().getProfile(SystemDefaults.PLAYER_ID)?.onboardingDone == true,
            career = db.configDao().get(ModuleFlags.KEY_CAREER)?.value,
            workout = db.configDao().get(ModuleFlags.KEY_WORKOUT)?.value,
            diet = db.configDao().get(ModuleFlags.KEY_DIET)?.value,
        )
        val score = personalScore(rate, streakForScore, workoutDays, dsaSolved, modules)
        return PeriodScore(
            score = score,
            questCompletionPct = rate,
            workoutDays = workoutDays,
            workoutCount = workoutCount,
            dsaSolved = dsaSolved,
            questsCompleted = completed,
            questsTotal = total,
            activeDays = activeDays,
        )
    }

    suspend fun improvementSnapshot(): ImprovementSnapshot {
        val zone = playerZone()
        val today = clock.today(zone)
        val currentStart = today.minusDays(6)
        val previousEnd = currentStart.minusDays(1)
        val previousStart = previousEnd.minusDays(6)
        val streak = db.playerDao().getStreak(SystemDefaults.PLAYER_ID)?.current ?: 0
        val current = periodScore(currentStart, today, streak)
        val previous = periodScore(previousStart, previousEnd, 0)
        return ImprovementSnapshot(
            current = current,
            previous = previous,
            improvementPercent = improvementPercent(current.score, previous.score, previous.activeDays),
        )
    }

    suspend fun beforeVsNow(): BeforeVsNow {
        val snap = improvementSnapshot()
        val zone = playerZone()
        val today = clock.today(zone)
        val currentStart = today.minusDays(6)
        val previousEnd = currentStart.minusDays(1)
        val previousStart = previousEnd.minusDays(6)
        val calorieTarget = db.configDao().get("calorie_target")?.value?.toIntOrNull() ?: 1800
        val streak = db.playerDao().getStreak(SystemDefaults.PLAYER_ID)
        val (weightBefore, _) = weightInRange(previousStart, previousEnd)
        val (_, weightNow) = weightInRange(currentStart, today)
        return BeforeVsNow(
            taskCompletionBefore = snap.previous.questCompletionPct,
            taskCompletionNow = snap.current.questCompletionPct,
            workoutDaysBefore = snap.previous.workoutDays,
            workoutDaysNow = snap.current.workoutDays,
            improvementPercent = snap.improvementPercent,
            dsaSolvedBefore = snap.previous.dsaSolved,
            dsaSolvedNow = snap.current.dsaSolved,
            dietAdherenceBefore = dietAdherencePercent(previousStart, previousEnd, calorieTarget),
            dietAdherenceNow = dietAdherencePercent(currentStart, today, calorieTarget),
            weightBefore = weightBefore,
            weightNow = weightNow,
            streakBest = streak?.best ?: 0,
            streakCurrent = streak?.current ?: 0,
        )
    }

    suspend fun dietAdherencePercent(
        start: java.time.LocalDate,
        end: java.time.LocalDate,
        calorieTarget: Int,
    ): Int? {
        if (calorieTarget <= 0) return null
        var daysWithData = 0
        var adherentDays = 0
        var d = start
        while (!d.isAfter(end)) {
            val nutrition = db.moduleDao().getNutrition(d.format(dateFmt))
            if (nutrition != null && nutrition.calories > 0) {
                daysWithData++
                val pct = nutrition.calories.toFloat() / calorieTarget * 100f
                if (pct in 85f..115f) adherentDays++
            }
            d = d.plusDays(1)
        }
        if (daysWithData < 3) return null
        return (adherentDays.toFloat() / daysWithData * 100f).toInt()
    }

    suspend fun weightInRange(
        start: java.time.LocalDate,
        end: java.time.LocalDate,
    ): Pair<Float?, Float?> {
        val zone = playerZone()
        val startMs = start.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMs = end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val weights = db.moduleDao().recentMetrics("WEIGHT", 500)
            .filter { it.recordedAtEpochMs in startMs until endMs }
            .sortedBy { it.recordedAtEpochMs }
        return weights.firstOrNull()?.value to weights.lastOrNull()?.value
    }

    suspend fun nextUnlock(): NextUnlock {
        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
        val level = profile?.level ?: 1
        val totalXp = profile?.totalXp ?: 0
        val into = totalXp - SystemDefaults.totalXpForLevel(level)
        val need = SystemDefaults.xpForNextLevel(level)
        val unlocked = db.achievementDao().getUnlocked().map { it.achievementKey }.toSet()
        val defs = db.achievementDao().getDefs()
        val nextAch = defs.firstOrNull { it.key !in unlocked }
        if (nextAch != null && into.toFloat() / need.toFloat() > 0.7f) {
            return NextUnlock(
                kind = "ACHIEVEMENT",
                title = "NEW ACHIEVEMENT",
                detail = "LOCKED",
                mysterious = true,
            )
        }
        return NextUnlock(
            kind = "LEVEL",
            title = "LEVEL ${level + 1}",
            detail = "???\nSomething is waiting.",
            mysterious = true,
        )
    }

    private suspend fun playerZone(): java.time.ZoneId {
        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
        return runCatching { java.time.ZoneId.of(profile?.timezone ?: java.time.ZoneId.systemDefault().id) }
            .getOrDefault(java.time.ZoneId.systemDefault())
    }

    companion object {
        fun completionRate(completed: Int, total: Int): Float =
            if (total == 0) 0f else completed.toFloat() / total.toFloat()

        fun personalScore(
            questCompletionPct: Float,
            streak: Int,
            workoutDays: Int,
            dsaSolvedWeek: Int,
            modules: EnabledModules = EnabledModules(career = true, workout = true, diet = true),
        ): Int {
            data class Part(val weight: Float, val value: Float)
            val parts = mutableListOf(
                Part(40f, questCompletionPct.coerceIn(0f, 1f)),
                Part(20f, min(streak, 7) / 7f),
            )
            if (modules.workout) {
                parts += Part(20f, (workoutDays / 7f).coerceIn(0f, 1f))
            }
            if (modules.career) {
                parts += Part(20f, (min(dsaSolvedWeek, 10) / 10f).coerceIn(0f, 1f))
            }
            val totalWeight = parts.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(1f)
            val score = parts.sumOf { (it.weight / totalWeight * it.value * 100f).toDouble() }
            return score.toInt().coerceIn(0, 100)
        }

        /**
         * ((current - previous) / previous) × 100, one decimal.
         * Null when baseline is too weak to trust.
         */
        fun improvementPercent(currentScore: Int, previousScore: Int, previousActiveDays: Int): Float? {
            if (previousScore <= 0 || previousActiveDays < 3) return null
            val raw = (currentScore - previousScore).toFloat() / previousScore.toFloat() * 100f
            return kotlin.math.round(raw * 10f) / 10f
        }

        fun estimateActiveDays(
            workoutDays: Int,
            questsCompleted: Int,
            questsTotal: Int,
            start: java.time.LocalDate,
            end: java.time.LocalDate,
        ): Int {
            val span = (java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1).toInt().coerceAtLeast(1)
            val questDaysProxy = if (questsTotal <= 0) {
                0
            } else {
                ((questsCompleted.toFloat() / questsTotal.toFloat()) * span).toInt()
            }
            return maxOf(workoutDays, questDaysProxy).coerceIn(0, span)
        }
    }
}

class AdaptiveService(
    private val db: JsonDatabase,
    private val clock: AppClock,
) {
    suspend fun suggestions(): List<AdaptiveSuggestion> {
        val dismissed = db.moduleDao().getDismissedSuggestionKeys().toSet()
        val raw = buildRawSuggestions()
        return filterDismissed(raw, dismissed)
    }

    suspend fun dismissSuggestion(key: String) {
        if (db.moduleDao().findDismissedSuggestion(key) != null) return
        db.moduleDao().insertDismissedSuggestion(
            com.example.solo_levelling.data.db.entity.DismissedSuggestionEntity(
                suggestionKey = key,
                dismissedAtEpochMs = clock.nowEpochMs(),
            ),
        )
    }

    private suspend fun buildRawSuggestions(): List<AdaptiveSuggestion> {
        val attrs = db.playerDao().getAttributes()
        if (attrs.isEmpty()) return emptyList()
        val avg = attrs.map { it.currentValue }.average()
        val out = mutableListOf<AdaptiveSuggestion>()
        for (attr in attrs) {
            if (attr.currentValue < avg * 0.6) {
                val hint = when (attr.code) {
                    AttributeCode.INT.name -> "Add one DSA quest tomorrow"
                    AttributeCode.STR.name -> "Schedule a short workout"
                    AttributeCode.FOC.name -> "Block 45 minutes of deep work"
                    AttributeCode.WIS.name -> "Write a brief journal reflection"
                    AttributeCode.END.name -> "Hit a walking/step target"
                    AttributeCode.VIT.name -> "Log nutrition once today"
                    AttributeCode.DISC.name -> "Complete the highest-priority planned quest first"
                    else -> "Take one small action for ${attr.code}"
                }
                out += AdaptiveSuggestion(
                    key = "boost_${attr.code}",
                    title = "Boost ${attr.code}",
                    detail = hint,
                )
            }
        }
        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
        if (profile != null && profile.level >= 5) {
            out += AdaptiveSuggestion(
                key = "harder_quests",
                title = "Ready for harder quests",
                detail = "Your level supports raising one daily quest XP tier.",
            )
        }
        return out
    }

    companion object {
        /** Suggest scaled XP for a template based on recent completion rate (does not auto-change goals). */
        fun suggestedXp(baseXp: Int, recentCompletionRate: Float): Int {
            return when {
                recentCompletionRate >= 0.9f -> (baseXp * 1.15f).toInt()
                recentCompletionRate <= 0.4f -> (baseXp * 0.85f).toInt()
                else -> baseXp
            }
        }

        fun filterDismissed(
            suggestions: List<AdaptiveSuggestion>,
            dismissedKeys: Set<String>,
        ): List<AdaptiveSuggestion> = suggestions.filter { it.key !in dismissedKeys }
    }
}
