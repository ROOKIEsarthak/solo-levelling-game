package com.example.solo_levelling.domain.service

import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.core.event.DomainEvent
import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.AppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.CareerNodeEntity
import com.example.solo_levelling.data.db.entity.DsaProblemEntity
import com.example.solo_levelling.data.db.entity.SkillEntity
import com.example.solo_levelling.data.seed.SeedData
import com.example.solo_levelling.domain.model.AttributeCode
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Career / DSA / System Design module mutations (extracted from ModuleService). */
class CareerModuleService(
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
        val now = clock.nowEpochMs()
        val reviewDue = now + 3L * 24 * 60 * 60 * 1000
        db.moduleDao().updateDsa(
            problem.copy(
                status = "SOLVED",
                attempts = problem.attempts + 1,
                confidence = (problem.confidence + 1).coerceAtMost(5),
                solvedAtEpochMs = now,
                reviewDueEpochMs = reviewDue,
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

    suspend fun markDsaNeedsReview(id: Long) {
        val problem = db.moduleDao().getDsa(id) ?: return
        if (problem.status == "NOT_STARTED") return
        db.moduleDao().updateDsa(problem.copy(status = "NEEDS_REVIEW"))
    }

    suspend fun updateDsaNotes(id: Long, notes: String, mistakes: String, approach: String) {
        val problem = db.moduleDao().getDsa(id) ?: return
        db.moduleDao().updateDsa(
            problem.copy(notes = notes, mistakes = mistakes, approach = approach),
        )
    }

    suspend fun ensureCareerCatalogsSeeded() {
        if (db.moduleDao().getDsaProblems().isEmpty()) {
            SeedData.dsaStarterProblems().forEach { db.moduleDao().upsertDsa(it) }
        }
        if (db.moduleDao().getSystemDesignTopics().isEmpty()) {
            db.moduleDao().replaceSystemDesignTopics(SeedData.systemDesignTopics())
        }
    }

    suspend fun markSystemDesignConcept(topicId: String, conceptId: String, status: String) {
        val topic = db.moduleDao().getSystemDesignTopics().find { it.id == topicId } ?: return
        val previous = topic.concepts.find { it.id == conceptId }
        val updatedConcepts = topic.concepts.map { concept ->
            if (concept.id == conceptId) concept.copy(status = status) else concept
        }
        val statuses = updatedConcepts.mapNotNull { concept ->
            runCatching { SystemDesignConceptStatus.valueOf(concept.status) }.getOrNull()
        }
        val confidence = SystemDesignProgressLogic.topicConfidence(statuses)
        db.moduleDao().upsertSystemDesignTopic(
            topic.copy(concepts = updatedConcepts, confidence = confidence),
        )
        if (previous?.status != "MASTERED" && status == "MASTERED") {
            progression.award(
                "SD_CONCEPT",
                "sd_${topicId}_$conceptId",
                10,
                mapOf(AttributeCode.INT to 8, AttributeCode.WIS to 2),
                applyDailyCap = true,
            )
        }
        if (previous?.status != status) {
            verification.tryAutoComplete(todayStr())
        }
    }

    suspend fun setSystemDesignConfidence(topicId: String, confidence: Int) {
        val topic = db.moduleDao().getSystemDesignTopics().find { it.id == topicId } ?: return
        db.moduleDao().upsertSystemDesignTopic(
            topic.copy(confidence = confidence.coerceIn(0, 100)),
        )
    }

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

    private suspend fun addSkillXp(domain: String, name: String, xp: Int) {
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
