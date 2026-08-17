package com.example.solo_levelling.domain.service

import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.core.event.DomainEvent
import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.AppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.QuestInstanceEntity
import com.example.solo_levelling.domain.model.QuestStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class QuestGenerationService(
    private val db: JsonDatabase,
    private val clock: AppClock,
    private val eventBus: EventBus,
    private val adaptive: AdaptiveService? = null,
    private val scope: CoroutineScope? = null,
) {
    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE

    fun start() {
        val s = scope ?: return
        s.launch {
            eventBus.events.collect { event ->
                when (event) {
                    is DomainEvent.QuestCompleted -> onQuestCompleted(event)
                    is DomainEvent.QuestUndone -> onQuestUndone(event)
                    else -> Unit
                }
            }
        }
    }

    suspend fun generateForToday(timezone: String = ZoneId.systemDefault().id) {
        val zone = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneId.systemDefault())
        val today = clock.today(zone)
        generateForDate(today)
        generateWeeklyIfNeeded(today)
        generateMilestonesIfNeeded(today)
        val dateStr = today.format(dateFmt)
        val count = db.questDao().getInstancesForDate(dateStr).size
        eventBus.publish(DomainEvent.DailyQuestsReady(dateStr, count))
    }

    suspend fun generateForDate(date: LocalDate) {
        val dateStr = date.format(dateFmt)
        val dayNum = date.dayOfWeek.value
        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
        val priorities = profile?.prioritiesCsv?.split(",")
            ?.map { it.trim().lowercase() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
        val modules = resolveModules(profile?.onboardingDone == true)
        val templates = filterByPriority(
            filterByModules(db.questDao().getActiveTemplates(), modules),
            priorities,
        )
        val completionRate = recentCompletionRate(date)
        for (template in templates) {
            if (template.type == "WEEKLY" || template.type == "RECOVERY" || template.type == "MILESTONE") continue
            val days = template.scheduleDaysCsv.split(",").mapNotNull { it.trim().toIntOrNull() }
            if (days.isNotEmpty() && dayNum !in days) continue
            val status = resolveStatus(template.dependsOnTemplateKey, dateStr)
            val xp = adaptive?.let { AdaptiveService.suggestedXp(template.baseXp, completionRate) } ?: template.baseXp
            db.questDao().insertInstance(
                QuestInstanceEntity(
                    templateId = template.id,
                    scheduledDate = dateStr,
                    status = status,
                    title = template.title,
                    type = template.type,
                    baseXp = xp,
                    attributeRewardsJson = template.attributeRewardsJson,
                    verificationType = template.verificationType,
                    verificationTarget = template.verificationTarget,
                    verificationUnit = template.verificationUnit,
                ),
            )
        }
    }

    private suspend fun generateWeeklyIfNeeded(today: LocalDate) {
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = weekStart.plusDays(6)
        val weekEndStr = weekEnd.format(dateFmt)
        val priorities = db.playerDao().getProfile(SystemDefaults.PLAYER_ID)?.prioritiesCsv
            ?.split(",")?.map { it.trim().lowercase() }?.filter { it.isNotEmpty() }.orEmpty()
        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
        val modules = resolveModules(profile?.onboardingDone == true)
        val templates = filterByPriority(
            filterByModules(
                db.questDao().getActiveTemplates().filter { it.type == "WEEKLY" },
                modules,
            ),
            priorities,
        )
        val completionRate = recentCompletionRate(today)
        for (template in templates) {
            val days = template.scheduleDaysCsv.split(",").mapNotNull { it.trim().toIntOrNull() }
            val targetDay = days.firstOrNull() ?: 7
            val targetDate = weekStart.plusDays((targetDay - 1).toLong())
            if (today.isBefore(targetDate)) continue
            val status = resolveStatus(template.dependsOnTemplateKey, weekEndStr)
            val xp = adaptive?.let { AdaptiveService.suggestedXp(template.baseXp, completionRate) } ?: template.baseXp
            db.questDao().insertInstance(
                QuestInstanceEntity(
                    templateId = template.id,
                    scheduledDate = weekEndStr,
                    status = status,
                    title = template.title,
                    type = template.type,
                    baseXp = xp,
                    attributeRewardsJson = template.attributeRewardsJson,
                    verificationType = template.verificationType,
                    verificationTarget = template.verificationTarget,
                    verificationUnit = template.verificationUnit,
                ),
            )
        }
    }

    private suspend fun generateMilestonesIfNeeded(today: LocalDate) {
        val dateStr = today.format(dateFmt)
        val weekEnd = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusDays(6).format(dateFmt)
        val templates = db.questDao().getActiveTemplates().filter { it.type == "MILESTONE" }
        val completionRate = recentCompletionRate(today)
        for (template in templates) {
            if (db.questDao().countInstancesForTemplate(template.id) > 0) continue
            val scheduled = if (template.scheduleDaysCsv.isBlank()) dateStr else weekEnd
            val xp = adaptive?.let { AdaptiveService.suggestedXp(template.baseXp, completionRate) } ?: template.baseXp
            db.questDao().insertInstance(
                QuestInstanceEntity(
                    templateId = template.id,
                    scheduledDate = scheduled,
                    status = QuestStatus.AVAILABLE.name,
                    title = template.title,
                    type = template.type,
                    baseXp = xp,
                    attributeRewardsJson = template.attributeRewardsJson,
                    verificationType = template.verificationType,
                    verificationTarget = template.verificationTarget,
                    verificationUnit = template.verificationUnit,
                ),
            )
        }
    }

    private suspend fun onQuestCompleted(event: DomainEvent.QuestCompleted) {
        val instance = db.questDao().getInstance(event.instanceId) ?: return
        val template = db.questDao().getTemplateById(instance.templateId) ?: return
        val dependents = db.questDao().getInstancesDependingOn(instance.scheduledDate, template.key)
        for (dep in dependents) {
            if (dep.status == QuestStatus.LOCKED.name) {
                db.questDao().updateInstance(dep.copy(status = QuestStatus.AVAILABLE.name))
            }
        }
    }

    private suspend fun onQuestUndone(event: DomainEvent.QuestUndone) {
        val instance = db.questDao().getInstance(event.instanceId) ?: return
        val template = db.questDao().getTemplateById(instance.templateId) ?: return
        val dependents = db.questDao().getInstancesDependingOn(instance.scheduledDate, template.key)
        for (dep in dependents) {
            if (dep.status == QuestStatus.AVAILABLE.name) {
                db.questDao().updateInstance(dep.copy(status = QuestStatus.LOCKED.name))
            }
        }
    }

    private suspend fun resolveStatus(dependsOnTemplateKey: String, date: String): String {
        if (dependsOnTemplateKey.isBlank()) return QuestStatus.AVAILABLE.name
        val depTemplate = db.questDao().getTemplateByKey(dependsOnTemplateKey) ?: return QuestStatus.LOCKED.name
        val depInstance = db.questDao().getInstancesForDate(date)
            .firstOrNull { it.templateId == depTemplate.id }
        return if (depInstance?.status == QuestStatus.COMPLETED.name) {
            QuestStatus.AVAILABLE.name
        } else {
            QuestStatus.LOCKED.name
        }
    }

    private suspend fun recentCompletionRate(today: LocalDate): Float {
        val weekStart = today.minusDays(6).format(dateFmt)
        val weekEnd = today.format(dateFmt)
        val total = db.questDao().countTotalInRange(weekStart, weekEnd)
        if (total == 0) return 0.7f
        val completed = db.questDao().countCompletedInRange(weekStart, weekEnd)
        return completed.toFloat() / total.toFloat()
    }

    private suspend fun resolveModules(onboardingDone: Boolean): EnabledModules =
        ModuleFlags.resolve(
            onboardingDone = onboardingDone,
            career = db.configDao().get(ModuleFlags.KEY_CAREER)?.value,
            workout = db.configDao().get(ModuleFlags.KEY_WORKOUT)?.value,
            diet = db.configDao().get(ModuleFlags.KEY_DIET)?.value,
        )

    private fun filterByModules(
        templates: List<com.example.solo_levelling.data.db.entity.QuestTemplateEntity>,
        modules: EnabledModules,
    ): List<com.example.solo_levelling.data.db.entity.QuestTemplateEntity> =
        templates.filter { template ->
            val tags = template.priorityTags.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
            val moduleTags = tags.filter { it.startsWith("module_") }
            if (moduleTags.isEmpty()) return@filter true
            moduleTags.any { tag ->
                when (tag) {
                    "module_career" -> modules.career
                    "module_workout" -> modules.workout
                    "module_diet" -> modules.diet
                    else -> true
                }
            }
        }

    private fun filterByPriority(
        templates: List<com.example.solo_levelling.data.db.entity.QuestTemplateEntity>,
        priorities: List<String>,
    ): List<com.example.solo_levelling.data.db.entity.QuestTemplateEntity> {
        if (priorities.isEmpty()) return templates
        return templates.filter { template ->
            if (template.priorityTags.isBlank()) return@filter true
            val tags = template.priorityTags.split(",")
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() && !it.startsWith("module_") }
            if (tags.isEmpty()) return@filter true
            tags.any { it in priorities }
        }
    }
}
