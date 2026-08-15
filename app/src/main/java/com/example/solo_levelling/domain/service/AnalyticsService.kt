package com.example.solo_levelling.domain.service

import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.core.time.AppClock
import com.example.solo_levelling.data.db.AppDatabase
import com.example.solo_levelling.domain.model.AttributeCode
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import org.json.JSONArray
import org.json.JSONObject

data class WeeklyReview(
    val weekStart: String,
    val weekEnd: String,
    val questsCompleted: Int,
    val questsTotal: Int,
    val completionRate: Float,
    val xpEarned: Int,
    val recommendations: List<String>,
)

data class AdaptiveSuggestion(
    val title: String,
    val detail: String,
)

class AnalyticsService(
    private val db: AppDatabase,
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
        val rate = if (total == 0) 0f else completed.toFloat() / total.toFloat()
        val recommendations = buildList {
            if (rate < 0.5f) add("Lower daily quest load slightly and protect one deep-work block.")
            if (rate >= 0.8f) add("Strong execution — consider one harder milestone this week.")
            val attrs = db.playerDao().getAttributes()
            val neglected = attrs.minByOrNull { it.currentValue }
            if (neglected != null) add("Neglected attribute: ${neglected.code} — schedule one related quest.")
        }
        return WeeklyReview(startStr, endStr, completed, total, rate, xp, recommendations)
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
}

class AdaptiveService(
    private val db: AppDatabase,
) {
    suspend fun suggestions(): List<AdaptiveSuggestion> {
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
                    title = "Boost ${attr.code}",
                    detail = hint,
                )
            }
        }
        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
        if (profile != null && profile.level >= 5) {
            out += AdaptiveSuggestion(
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
    }
}
