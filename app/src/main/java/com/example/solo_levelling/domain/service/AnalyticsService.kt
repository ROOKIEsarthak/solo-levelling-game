package com.example.solo_levelling.domain.service

import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.core.time.AppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.XpLedgerEntryEntity
import com.example.solo_levelling.domain.model.AttributeCode
import com.example.solo_levelling.domain.logic.MealCompletionPolicy
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
    /** Null when Career is disabled (not applicable). */
    val dsaSolvedWeek: Int?,
    /** Null when Workout is disabled (not applicable). */
    val workoutCountWeek: Int?,
    /** Null when Workout is disabled (not applicable). */
    val workoutDaysWeek: Int?,
    val bossProgress: BossProgressSnapshot?,
    val attributeSnapshot: AttributeSnapshot,
    val personalScore: Int,
    val recommendations: List<String>,
    val modules: EnabledModules,
    val careerXp: Int? = null,
    val workoutXp: Int? = null,
    val dietXp: Int? = null,
)

data class AdaptiveSuggestion(
    val key: String,
    val title: String,
    val detail: String,
)

data class PeriodScore(
    val score: Int,
    val questCompletionPct: Float,
    /** Null when Workout is disabled. */
    val workoutDays: Int?,
    /** Null when Workout is disabled. */
    val workoutCount: Int?,
    /** Null when Career is disabled. */
    val dsaSolved: Int?,
    val questsCompleted: Int,
    val questsTotal: Int,
    val activeDays: Int,
    val modules: EnabledModules,
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
    /** Null when Workout is disabled. */
    val workoutDaysBefore: Int?,
    /** Null when Workout is disabled. */
    val workoutDaysNow: Int?,
    val improvementPercent: Float?,
    /** Null when Career is disabled. */
    val dsaSolvedBefore: Int? = null,
    /** Null when Career is disabled. */
    val dsaSolvedNow: Int? = null,
    val dietAdherenceBefore: Int? = null,
    val dietAdherenceNow: Int? = null,
    val weightBefore: Float? = null,
    val weightNow: Float? = null,
    val streakBest: Int = 0,
    val streakCurrent: Int = 0,
    val modules: EnabledModules,
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
        val workoutEndStr = minOf(weekEnd, today).format(dateFmt)
        val modules = resolveModules()
        val (completed, total) = countQuestsInRange(startStr, endStr, modules)
        val startMs = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMs = weekEnd.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val xp = sumXpInRange(startMs, endMs, modules)
        val rate = completionRate(completed, total)
        val dsaSolved = if (modules.career) db.moduleDao().countDsaSolvedInRange(startMs, endMs) else null
        val workoutCount = if (modules.workout) db.moduleDao().countWorkoutsInRange(startStr, workoutEndStr) else null
        val workoutDays = if (modules.workout) db.moduleDao().countWorkoutDaysInRange(startStr, workoutEndStr) else null
        val streak = db.playerDao().getStreak(SystemDefaults.PLAYER_ID)?.current ?: 0
        val personalScore = personalScore(
            rate,
            streak,
            workoutDays ?: 0,
            dsaSolved ?: 0,
            modules,
        )
        val attrs = db.playerDao().getAttributes()
        val attributeSnapshot = Companion.attributeSnapshot(
            attrs.map { it.code to it.currentValue },
            modules,
        )
        val boss = db.moduleDao().getActiveBoss()
        val bossProgress = boss?.let {
            BossProgressSnapshot(it.title, it.currentValue, it.targetValue)
        }
        val recommendations = buildList {
            if (rate < 0.5f) add("Lower daily quest load slightly and protect one deep-work block.")
            if (rate >= 0.8f) add("Strong execution — consider one harder milestone this week.")
            attributeSnapshot.bottomCode?.let { code ->
                add("Neglected attribute: $code — schedule one related quest.")
            }
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
            modules = modules,
            careerXp = if (modules.career) sumModuleXpInRange(startMs, endMs, modules, ModuleId.CAREER) else null,
            workoutXp = if (modules.workout) sumModuleXpInRange(startMs, endMs, modules, ModuleId.WORKOUT) else null,
            dietXp = if (modules.diet) sumModuleXpInRange(startMs, endMs, modules, ModuleId.DIET) else null,
        )
    }

    suspend fun exportJson(): String = exportActiveJson()

    /** Active gameplay export: enabled-module ledger, attributes, and achievements only. */
    suspend fun exportActiveJson(): String {
        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
        val modules = resolveModules()
        val reader = ActiveProgressionReader(db)
        val attrs = reader.activeAttributes(modules)
        val ledger = reader.activeLedger(modules)
        val achievements = db.achievementDao().getUnlocked().filter { unlocked ->
            val def = db.achievementDao().getDefs().find { it.key == unlocked.achievementKey }
            def == null || ModuleScope.allowsAchievement(def.criteriaType, modules, def.key)
        }
        return buildExportJson(profile, modules, attrs, ledger, achievements, exportMode = "active")
    }

    /** Full archive export: all stored history regardless of module flags. */
    suspend fun exportArchiveJson(): String {
        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
        val modules = resolveModules()
        val attrs = db.playerDao().getAttributes()
        val ledger = db.xpDao().getAllLedger()
        val achievements = db.achievementDao().getUnlocked()
        return buildExportJson(profile, modules, attrs, ledger, achievements, exportMode = "archive")
    }

    private fun buildExportJson(
        profile: com.example.solo_levelling.data.db.entity.PlayerProfileEntity?,
        modules: EnabledModules,
        attrs: List<com.example.solo_levelling.data.db.entity.AttributeStatEntity>,
        ledger: List<com.example.solo_levelling.data.db.entity.XpLedgerEntryEntity>,
        achievements: List<com.example.solo_levelling.data.db.entity.PlayerAchievementEntity>,
        exportMode: String,
    ): String {
        val root = JSONObject()
        root.put("exportedAt", clock.nowEpochMs())
        root.put("exportMode", exportMode)
        root.put(
            "enabledModules",
            JSONObject()
                .put("career", modules.career)
                .put("workout", modules.workout)
                .put("diet", modules.diet),
        )
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
        val today = clock.today(zone)
        val startStr = start.format(dateFmt)
        val endStr = end.format(dateFmt)
        val workoutEndStr = minOf(end, today).format(dateFmt)
        val modules = resolveModules()
        val (completed, total) = countQuestsInRange(startStr, endStr, modules)
        val rate = completionRate(completed, total)
        val startMs = start.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMs = end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val dsaSolved = if (modules.career) db.moduleDao().countDsaSolvedInRange(startMs, endMs) else null
        val workoutCount = if (modules.workout) db.moduleDao().countWorkoutsInRange(startStr, workoutEndStr) else null
        val workoutDays = if (modules.workout) db.moduleDao().countWorkoutDaysInRange(startStr, workoutEndStr) else null
        val activeDays = estimateActiveDays(workoutDays ?: 0, completed, total, start, end)
        val score = personalScore(rate, streakForScore, workoutDays ?: 0, dsaSolved ?: 0, modules)
        return PeriodScore(
            score = score,
            questCompletionPct = rate,
            workoutDays = workoutDays,
            workoutCount = workoutCount,
            dsaSolved = dsaSolved,
            questsCompleted = completed,
            questsTotal = total,
            activeDays = activeDays,
            modules = modules,
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
        val modules = snap.current.modules
        val zone = playerZone()
        val today = clock.today(zone)
        val currentStart = today.minusDays(6)
        val previousEnd = currentStart.minusDays(1)
        val previousStart = previousEnd.minusDays(6)
        val calorieTarget = db.configDao().get("calorie_target")?.value?.toIntOrNull() ?: 1800
        val streak = db.playerDao().getStreak(SystemDefaults.PLAYER_ID)
        val dietBefore = if (modules.diet) {
            dietAdherencePercent(previousStart, previousEnd, calorieTarget)
        } else {
            null
        }
        val dietNow = if (modules.diet) {
            dietAdherencePercent(currentStart, today, calorieTarget)
        } else {
            null
        }
        val (weightBefore, weightNow) = if (modules.diet) {
            val (wb, _) = weightInRange(previousStart, previousEnd)
            val (_, wn) = weightInRange(currentStart, today)
            wb to wn
        } else {
            null to null
        }
        return BeforeVsNow(
            taskCompletionBefore = snap.previous.questCompletionPct,
            taskCompletionNow = snap.current.questCompletionPct,
            workoutDaysBefore = snap.previous.workoutDays,
            workoutDaysNow = snap.current.workoutDays,
            improvementPercent = snap.improvementPercent,
            dsaSolvedBefore = snap.previous.dsaSolved,
            dsaSolvedNow = snap.current.dsaSolved,
            dietAdherenceBefore = dietBefore,
            dietAdherenceNow = dietNow,
            weightBefore = weightBefore,
            weightNow = weightNow,
            streakBest = streak?.best ?: 0,
            streakCurrent = streak?.current ?: 0,
            modules = modules,
        )
    }

    suspend fun dietAdherencePercent(
        start: java.time.LocalDate,
        end: java.time.LocalDate,
        @Suppress("UNUSED_PARAMETER") calorieTarget: Int,
    ): Int? {
        val clampedEnd = minOf(end, clock.today(playerZone()))
        var daysWithData = 0
        var adherentDays = 0
        var d = start
        while (!d.isAfter(clampedEnd)) {
            val log = db.moduleDao().getDietLog(d.format(dateFmt))
            if (log != null && MealCompletionPolicy.countValidMeals(log) > 0) {
                daysWithData++
                if (MealCompletionPolicy.isMealTrackingComplete(log)) adherentDays++
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
        val modules = resolveModules()
        val nextAch = defs.firstOrNull { def ->
            def.key !in unlocked && ModuleScope.allowsAchievement(def.criteriaType, modules, def.key)
        }
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

    private suspend fun resolveModules(): EnabledModules {
        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
        return ModuleFlags.resolve(
            onboardingDone = profile?.onboardingDone == true,
            career = db.configDao().get(ModuleFlags.KEY_CAREER)?.value,
            workout = db.configDao().get(ModuleFlags.KEY_WORKOUT)?.value,
            diet = db.configDao().get(ModuleFlags.KEY_DIET)?.value,
        )
    }

    private suspend fun countQuestsInRange(
        startStr: String,
        endStr: String,
        modules: EnabledModules,
    ): Pair<Int, Int> {
        val instances = db.questDao().getInstancesInRange(startStr, endStr)
            .filter { instance ->
                val tags = db.questDao().getTemplateById(instance.templateId)?.priorityTags.orEmpty()
                ModuleScope.allowsQuestTemplate(tags, modules)
            }
        val completed = instances.count { it.status == com.example.solo_levelling.domain.model.QuestStatus.COMPLETED.name }
        return completed to instances.size
    }

    internal suspend fun sumXpInRange(startMs: Long, endMs: Long, modules: EnabledModules): Int =
        db.xpDao().getAllLedger()
            .filter { it.createdAtEpochMs in startMs until endMs }
            .sumOf { entry ->
                if (allowsXpEntry(entry, modules)) entry.amount else 0
            }

    private suspend fun sumModuleXpInRange(
        startMs: Long,
        endMs: Long,
        modules: EnabledModules,
        module: ModuleId,
    ): Int =
        db.xpDao().getAllLedger()
            .filter { it.createdAtEpochMs in startMs until endMs }
            .sumOf { entry ->
                if (!allowsXpEntry(entry, modules)) {
                    0
                } else {
                    val owner = ModuleScope.parseModuleFromMetadata(entry.metadataJson)
                        ?: ModuleScope.moduleForSourceType(entry.sourceType)
                    if (owner == module) entry.amount else 0
                }
            }

    private suspend fun allowsXpEntry(
        entry: XpLedgerEntryEntity,
        modules: EnabledModules,
    ): Boolean {
        val questModule = if (entry.sourceType.equals("QUEST_INSTANCE", ignoreCase = true) &&
            ModuleScope.parseModuleFromMetadata(entry.metadataJson) == null
        ) {
            resolveQuestInstanceModule(entry.sourceId)
        } else {
            null
        }
        return ModuleScope.allowsLedgerEntry(
            entry.sourceType,
            entry.metadataJson,
            modules,
            questModule,
        )
    }

    private suspend fun resolveQuestInstanceModule(sourceId: String): ModuleId {
        val instanceId = sourceId.substringBefore('_').toLongOrNull() ?: return ModuleId.GLOBAL
        val instance = db.questDao().getInstance(instanceId) ?: return ModuleId.GLOBAL
        val tags = db.questDao().getTemplateById(instance.templateId)?.priorityTags.orEmpty()
        return ModuleScope.moduleForPriorityTags(tags)
    }

    companion object {
        fun completionRate(completed: Int, total: Int): Float =
            if (total == 0) 0f else completed.toFloat() / total.toFloat()

        /**
         * Weighted personal score over enabled progression parts only.
         * Diet has no dedicated weight; it participates via filtered quests/XP and diet UI metrics.
         */
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

        /** Same actionability rules as AdaptiveService suggestion hints. */
        fun isAttributeActionable(code: String, modules: EnabledModules): Boolean =
            when (code) {
                AttributeCode.INT.name -> modules.career
                AttributeCode.STR.name, AttributeCode.END.name -> modules.workout
                AttributeCode.VIT.name -> modules.diet || modules.workout
                AttributeCode.FOC.name, AttributeCode.WIS.name, AttributeCode.DISC.name -> true
                else -> true
            }

        fun attributeSnapshot(
            attrs: List<Pair<String, Int>>,
            modules: EnabledModules,
        ): AttributeSnapshot {
            val relevant = attrs.filter { isAttributeActionable(it.first, modules) }
            val top = relevant.maxByOrNull { it.second }
            val bottom = relevant.minByOrNull { it.second }
            return AttributeSnapshot(
                topCode = top?.first,
                topValue = top?.second ?: 0,
                bottomCode = bottom?.first,
                bottomValue = bottom?.second ?: 0,
            )
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
        val modules = ModuleFlags.resolve(
            onboardingDone = db.playerDao().getProfile(SystemDefaults.PLAYER_ID)?.onboardingDone == true,
            career = db.configDao().get(ModuleFlags.KEY_CAREER)?.value,
            workout = db.configDao().get(ModuleFlags.KEY_WORKOUT)?.value,
            diet = db.configDao().get(ModuleFlags.KEY_DIET)?.value,
        )
        val attrs = db.playerDao().getAttributes()
            .filter { AnalyticsService.isAttributeActionable(it.code, modules) }
        if (attrs.isEmpty()) return emptyList()
        val avg = attrs.map { it.currentValue }.average()
        val out = mutableListOf<AdaptiveSuggestion>()
        for (attr in attrs) {
            if (attr.currentValue < avg * 0.6) {
                val hint = when (attr.code) {
                    AttributeCode.INT.name -> if (modules.career) "Add one DSA quest tomorrow" else null
                    AttributeCode.STR.name -> if (modules.workout) "Schedule a short workout" else null
                    AttributeCode.FOC.name -> "Block 45 minutes of deep work"
                    AttributeCode.WIS.name -> "Write a brief journal reflection"
                    AttributeCode.END.name -> if (modules.workout) "Hit a walking/step target" else null
                    AttributeCode.VIT.name -> if (modules.diet) {
                        "Log today's meals"
                    } else if (modules.workout) {
                        "Complete today's workout"
                    } else {
                        null
                    }
                    AttributeCode.DISC.name -> "Complete the highest-priority planned quest first"
                    else -> "Take one small action for ${attr.code}"
                }
                if (hint == null) continue
                out += AdaptiveSuggestion(
                    key = "boost_${attr.code}",
                    title = "Boost ${attributeDisplayTitle(attr.code)}",
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

    private fun attributeDisplayTitle(code: String): String = when (code) {
        AttributeCode.STR.name -> "Strength"
        AttributeCode.END.name -> "Endurance"
        AttributeCode.INT.name -> "Intelligence"
        AttributeCode.VIT.name -> "Vitality"
        AttributeCode.DISC.name -> "Discipline"
        AttributeCode.FOC.name -> "Focus"
        AttributeCode.WIS.name -> "Wisdom"
        else -> code
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
