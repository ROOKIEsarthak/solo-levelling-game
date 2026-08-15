package com.example.solo_levelling.domain.service

import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.core.event.DomainEvent
import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.AppClock
import com.example.solo_levelling.data.db.AppDatabase
import com.example.solo_levelling.data.db.entity.AttributeStatEntity
import com.example.solo_levelling.data.db.entity.BossEntity
import com.example.solo_levelling.data.db.entity.DsaProblemEntity
import com.example.solo_levelling.data.db.entity.FocusSessionEntity
import com.example.solo_levelling.data.db.entity.JournalEntryEntity
import com.example.solo_levelling.data.db.entity.NutritionLogEntity
import com.example.solo_levelling.data.db.entity.SkillEntity
import com.example.solo_levelling.data.db.entity.WorkoutEntity
import com.example.solo_levelling.data.db.entity.XpLedgerEntryEntity
import com.example.solo_levelling.domain.model.AttributeCode
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ModuleService(
    private val db: AppDatabase,
    private val eventBus: EventBus,
    private val clock: AppClock,
) {
    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE

    private suspend fun todayStr(): String {
        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
        val zone = runCatching { ZoneId.of(profile?.timezone ?: ZoneId.systemDefault().id) }
            .getOrDefault(ZoneId.systemDefault())
        return clock.today(zone).format(dateFmt)
    }

    suspend fun addDsaProblem(title: String, difficulty: String, topic: String) {
        db.moduleDao().upsertDsa(
            DsaProblemEntity(
                title = title,
                difficulty = difficulty,
                topic = topic,
                externalId = "${title.hashCode()}_${clock.nowEpochMs()}",
                status = "NOT_STARTED",
            ),
        )
    }

    suspend fun solveDsa(id: Long) {
        val problem = db.moduleDao().getDsa(id) ?: return
        if (problem.status == "SOLVED" || problem.status == "MASTERED") return
        db.moduleDao().updateDsa(
            problem.copy(
                status = "SOLVED",
                attempts = problem.attempts + 1,
                confidence = (problem.confidence + 1).coerceAtMost(5),
            ),
        )
        awardXp(
            "DSA",
            "dsa_${problem.id}",
            25,
            mapOf(AttributeCode.INT to 20, AttributeCode.DISC to 5),
        )
        addSkillXp("CAREER", problem.topic.ifBlank { "DSA" }, 25)
    }

    suspend fun logWorkout(type: String, durationMinutes: Int, notes: String = "") {
        val date = todayStr()
        val id = db.moduleDao().insertWorkout(
            WorkoutEntity(date = date, type = type, durationMinutes = durationMinutes, notes = notes),
        )
        awardXp("WORKOUT", "workout_$id", 40, mapOf(AttributeCode.STR to 30, AttributeCode.VIT to 10))
    }

    suspend fun logNutrition(calories: Int, protein: Int, carbs: Int, fat: Int) {
        val date = todayStr()
        db.moduleDao().upsertNutrition(NutritionLogEntity(date, calories, protein, carbs, fat))
        awardXp("NUTRITION", "nutrition_$date", 15, mapOf(AttributeCode.VIT to 15))
    }

    suspend fun logFocus(durationMinutes: Int, label: String) {
        val date = todayStr()
        val now = clock.nowEpochMs()
        val id = db.moduleDao().insertFocus(
            FocusSessionEntity(
                date = date,
                durationMinutes = durationMinutes,
                label = label,
                completedAtEpochMs = now,
            ),
        )
        val xp = (durationMinutes / 15).coerceAtLeast(1) * 10
        awardXp("FOCUS", "focus_$id", xp, mapOf(AttributeCode.FOC to xp))
    }

    suspend fun saveJournal(content: String) {
        val date = todayStr()
        val now = clock.nowEpochMs()
        db.moduleDao().upsertJournal(JournalEntryEntity(date, content, now))
        awardXp("JOURNAL", "journal_$date", 20, mapOf(AttributeCode.WIS to 15, AttributeCode.DISC to 5))
    }

    suspend fun createBoss(title: String, description: String, xpReward: Int) {
        db.moduleDao().upsertBoss(
            BossEntity(title = title, description = description, xpReward = xpReward),
        )
    }

    suspend fun addBossProgress(amount: Float) {
        val boss = db.moduleDao().getActiveBoss() ?: return
        val newValue = (boss.currentValue + amount).coerceAtMost(boss.targetValue)
        val cleared = newValue >= boss.targetValue
        db.moduleDao().updateBoss(
            boss.copy(currentValue = newValue, status = if (cleared) "CLEARED" else "ACTIVE"),
        )
        eventBus.publish(DomainEvent.BossProgressUpdated(boss.id, newValue / boss.targetValue))
        if (cleared) {
            awardXp(
                "BOSS",
                "boss_${boss.id}",
                boss.xpReward,
                mapOf(AttributeCode.DISC to (boss.xpReward / 2).coerceAtLeast(1)),
            )
        }
    }

    suspend fun addSkillXp(domain: String, name: String, xp: Int) {
        val existing = db.moduleDao().findSkill(domain, name)
        val totalXp = (existing?.xp ?: 0) + xp
        val level = 1 + totalXp / 100
        val skill = SkillEntity(
            id = existing?.id ?: 0,
            domain = domain,
            name = name,
            xp = totalXp,
            level = level,
        )
        val newId = if (existing == null) {
            db.moduleDao().upsertSkill(skill)
        } else {
            db.moduleDao().updateSkill(skill)
            existing.id
        }
        if (existing == null || level > existing.level) {
            eventBus.publish(DomainEvent.SkillLevelUp(newId, level))
        }
    }

    private suspend fun awardXp(
        sourceType: String,
        sourceId: String,
        amount: Int,
        attrs: Map<AttributeCode, Int>,
    ) {
        if (db.xpDao().findBySource(sourceType, sourceId) != null) return
        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID) ?: return
        val now = clock.nowEpochMs()
        val newTotal = profile.totalXp + amount
        val newLevel = SystemDefaults.levelFromTotalXp(newTotal)
        val newRank = SystemDefaults.rankForLevel(newLevel)
        val ledgerId = db.xpDao().insertLedger(
            XpLedgerEntryEntity(
                amount = amount,
                sourceType = sourceType,
                sourceId = sourceId,
                createdAtEpochMs = now,
            ),
        )
        db.playerDao().upsertProfile(profile.copy(totalXp = newTotal, level = newLevel, rank = newRank))
        val existingAttrs = db.playerDao().getAttributes().associateBy { it.code }
        for ((code, delta) in attrs) {
            val existing = existingAttrs[code.name] ?: AttributeStatEntity(code = code.name)
            db.playerDao().upsertAttribute(
                existing.copy(
                    currentValue = existing.currentValue + delta,
                    lifetimeXp = existing.lifetimeXp + delta,
                ),
            )
        }
        eventBus.publish(DomainEvent.XpAwarded(ledgerId, amount, sourceType, sourceId, newTotal))
        if (newLevel > profile.level) eventBus.publish(DomainEvent.LevelUp(profile.level, newLevel))
        if (newRank != profile.rank) eventBus.publish(DomainEvent.RankUp(profile.rank, newRank))
    }
}
