package com.example.solo_levelling.domain.service

import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.core.event.DomainEvent
import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.AppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.BossEntity
import com.example.solo_levelling.data.db.entity.BossQuestEntity
import com.example.solo_levelling.data.db.entity.CareerNodeEntity
import com.example.solo_levelling.data.db.entity.DsaProblemEntity
import com.example.solo_levelling.data.db.entity.FocusSessionEntity
import com.example.solo_levelling.data.db.entity.JournalEntryEntity
import com.example.solo_levelling.data.db.entity.NutritionLogEntity
import com.example.solo_levelling.data.db.entity.RoutineLogEntity
import com.example.solo_levelling.data.db.entity.SkillEntity
import com.example.solo_levelling.data.db.entity.WorkoutEntity
import com.example.solo_levelling.data.db.entity.WorkoutExerciseEntity
import com.example.solo_levelling.domain.model.AttributeCode
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ModuleService(
    private val db: JsonDatabase,
    private val eventBus: EventBus,
    private val clock: AppClock,
    private val progression: ProgressionService,
    private val verification: QuestVerificationService,
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

    suspend fun markAttempted(id: Long) {
        val problem = db.moduleDao().getDsa(id) ?: return
        if (problem.status != "NOT_STARTED") return
        db.moduleDao().updateDsa(
            problem.copy(status = "ATTEMPTED", attempts = problem.attempts + 1),
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
                solvedAtEpochMs = clock.nowEpochMs(),
            ),
        )
        progression.award(
            "DSA",
            "dsa_${problem.id}",
            25,
            mapOf(AttributeCode.INT to 20, AttributeCode.DISC to 5),
            applyDailyCap = true,
        )
        addSkillXp("CAREER", problem.topic.ifBlank { "DSA" }, 25)
        verification.tryAutoComplete(todayStr())
    }

    suspend fun masterDsa(id: Long) {
        val problem = db.moduleDao().getDsa(id) ?: return
        if (problem.status != "SOLVED") return
        db.moduleDao().updateDsa(
            problem.copy(
                status = "MASTERED",
                confidence = (problem.confidence + 1).coerceAtMost(5),
            ),
        )
        progression.award(
            "DSA_MASTER",
            "dsa_master_${problem.id}",
            15,
            mapOf(AttributeCode.INT to 10, AttributeCode.DISC to 5),
            applyDailyCap = true,
        )
        addSkillXp("CAREER", problem.topic.ifBlank { "DSA" }, 15)
    }

    suspend fun logWorkout(type: String, durationMinutes: Int, notes: String = ""): Long {
        val date = todayStr()
        val id = db.moduleDao().insertWorkout(
            WorkoutEntity(date = date, type = type, durationMinutes = durationMinutes, notes = notes),
        )
        progression.award(
            "WORKOUT",
            "workout_$id",
            40,
            mapOf(AttributeCode.STR to 30, AttributeCode.VIT to 10),
            applyDailyCap = true,
        )
        verification.tryAutoComplete(date)
        return id
    }

    suspend fun addWorkoutExercise(
        workoutId: Long,
        name: String,
        sets: Int,
        reps: Int,
        weightKg: Float,
        rir: Int = 0,
    ) {
        db.moduleDao().insertWorkoutExercise(
            WorkoutExerciseEntity(
                workoutId = workoutId,
                name = name,
                sets = sets,
                reps = reps,
                weightKg = weightKg,
                rir = rir,
            ),
        )
    }

    suspend fun getWorkoutExercises(workoutId: Long): List<WorkoutExerciseEntity> =
        db.moduleDao().getWorkoutExercises(workoutId)

    suspend fun logNutrition(calories: Int, protein: Int, carbs: Int, fat: Int) {
        val date = todayStr()
        db.moduleDao().upsertNutrition(NutritionLogEntity(date, calories, protein, carbs, fat))
        progression.award(
            "NUTRITION",
            "nutrition_$date",
            15,
            mapOf(AttributeCode.VIT to 15),
            applyDailyCap = true,
        )
        verification.tryAutoComplete(date)
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
        progression.award("FOCUS", "focus_$id", xp, mapOf(AttributeCode.FOC to xp), applyDailyCap = true)
        verification.tryAutoComplete(date)
    }

    suspend fun saveJournal(content: String) {
        val date = todayStr()
        val now = clock.nowEpochMs()
        db.moduleDao().upsertJournal(JournalEntryEntity(date, content, now))
        progression.award(
            "JOURNAL",
            "journal_$date",
            20,
            mapOf(AttributeCode.WIS to 15, AttributeCode.DISC to 5),
            applyDailyCap = true,
        )
        verification.tryAutoComplete(date)
    }

    suspend fun getJournal(date: String): JournalEntryEntity? = db.moduleDao().getJournal(date)

    suspend fun listCareerNodes(): List<CareerNodeEntity> = db.moduleDao().getCareerNodes()

    suspend fun advanceCareerNode(id: Long) {
        val node = db.moduleDao().getCareerNode(id) ?: return
        val nextStatus = when (node.status) {
            "LOCKED" -> "STARTED"
            "STARTED" -> "LEARNING"
            "LEARNING" -> "PRACTICED"
            "PRACTICED" -> "MASTERED"
            else -> return
        }
        db.moduleDao().upsertCareerNode(node.copy(status = nextStatus))
        addSkillXp("CAREER", node.track, 10)
        if (nextStatus == "MASTERED") {
            val next = db.moduleDao().getCareerNodes()
                .firstOrNull { it.track == node.track && it.orderIndex == node.orderIndex + 1 && it.status == "LOCKED" }
            if (next != null) {
                db.moduleDao().upsertCareerNode(next.copy(status = "STARTED"))
            }
        }
    }

    suspend fun logRoutine(kind: String) {
        val date = todayStr()
        db.moduleDao().insertRoutineLog(
            RoutineLogEntity(date = date, kind = kind, completedAtEpochMs = clock.nowEpochMs()),
        )
        verification.tryAutoComplete(date)
    }

    suspend fun createBoss(title: String, description: String, xpReward: Int, linkDefaultQuests: Boolean = true) {
        val bossId = db.moduleDao().upsertBoss(
            BossEntity(title = title, description = description, xpReward = xpReward),
        )
        if (linkDefaultQuests) {
            for (key in listOf("dsa_daily", "workout_daily", "system_design")) {
                db.moduleDao().upsertBossQuest(
                    BossQuestEntity(bossId = bossId, templateKey = key, weight = 1f),
                )
            }
        }
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
            progression.award(
                "BOSS",
                "boss_${boss.id}",
                boss.xpReward,
                mapOf(AttributeCode.DISC to (boss.xpReward / 2).coerceAtLeast(1)),
                applyDailyCap = true,
            )
            eventBus.publish(DomainEvent.BossCompleted(boss.id, boss.xpReward))
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
}
