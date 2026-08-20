package com.example.solo_levelling.domain.service

import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.core.time.AppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.QuestInstanceEntity
import com.example.solo_levelling.domain.logic.ActivityDatePolicy
import com.example.solo_levelling.domain.logic.MealCompletionPolicy
import com.example.solo_levelling.domain.model.QuestStatus
import com.example.solo_levelling.domain.model.QuestType
import com.example.solo_levelling.domain.model.VerificationType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

class QuestVerificationService(
    private val db: JsonDatabase,
    private val clock: AppClock,
    private val questCompletion: QuestCompletionService,
) {
    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE

    suspend fun isSatisfied(instance: QuestInstanceEntity, date: String): Boolean {
        return when (instance.verificationType) {
            VerificationType.MANUAL.name -> true
            VerificationType.COUNT.name -> isCountSatisfied(instance, date)
            VerificationType.TIMER.name -> {
                val minutes = db.moduleDao().sumFocusMinutes(date)
                minutes >= instance.verificationTarget
            }
            VerificationType.METRIC_THRESHOLD.name -> isMetricSatisfied(instance, date)
            VerificationType.AUTOMATIC.name -> isAutomaticSatisfied(instance, date)
            else -> false
        }
    }

    suspend fun tryAutoComplete(date: String) {
        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID) ?: return
        val zone = runCatching { ZoneId.of(profile.timezone) }.getOrDefault(ZoneId.systemDefault())
        val today = clock.today(zone)
        val parsed = runCatching { LocalDate.parse(date, dateFmt) }.getOrNull() ?: return
        if (!ActivityDatePolicy.canAwardProgression(today, parsed)) return
        val instances = db.questDao().getInstancesForDate(date)
            .filter { it.status == QuestStatus.AVAILABLE.name }
        val modules = ModuleFlags.resolve(
            onboardingDone = profile.onboardingDone,
            career = db.configDao().get(ModuleFlags.KEY_CAREER)?.value,
            workout = db.configDao().get(ModuleFlags.KEY_WORKOUT)?.value,
            diet = db.configDao().get(ModuleFlags.KEY_DIET)?.value,
        )
        for (instance in instances) {
            val template = db.questDao().getTemplateById(instance.templateId)
            if (!ModuleScope.allowsQuestTemplate(template?.priorityTags.orEmpty(), modules)) continue
            if (instance.verificationType == VerificationType.MANUAL.name) {
                if (template?.key == "nutrition_daily" && isMealTrackingComplete(date)) {
                    questCompletion.complete(instance.id)
                } else if (template?.key == "workout_daily" && hasWorkoutWithSets(date)) {
                    questCompletion.complete(instance.id)
                } else if (isJournalTemplate(template?.key) && hasJournalEntry(date)) {
                    questCompletion.complete(instance.id)
                } else if (isSystemDesignTemplate(template?.key) && hasSystemDesignStudyToday(date)) {
                    questCompletion.complete(instance.id)
                }
                continue
            }
            if (!isSatisfied(instance, date)) continue
            questCompletion.complete(instance.id)
        }
        val remaining = db.questDao().getInstancesForDate(date)
            .filter { it.status == QuestStatus.AVAILABLE.name }
            .filter { it.verificationType == VerificationType.AUTOMATIC.name }
        for (instance in remaining) {
            val remainingTemplate = db.questDao().getTemplateById(instance.templateId)
            if (!ModuleScope.allowsQuestTemplate(remainingTemplate?.priorityTags.orEmpty(), modules)) continue
            if (!isSatisfied(instance, date)) continue
            questCompletion.complete(instance.id)
        }
        invalidateUnsatisfied(date, modules)
    }

    private suspend fun invalidateUnsatisfied(date: String, modules: EnabledModules) {
        val completed = db.questDao().getInstancesForDate(date)
            .filter { it.status == QuestStatus.COMPLETED.name }
            .filter { it.type != QuestType.MILESTONE.name }
        for (instance in completed) {
            val template = db.questDao().getTemplateById(instance.templateId)
            if (!ModuleScope.allowsQuestTemplate(template?.priorityTags.orEmpty(), modules)) continue
            if (isAutoEvidenceSatisfied(instance, date)) continue
            questCompletion.undo(instance.id, ignoreWindow = true)
        }
    }

    private suspend fun isAutoEvidenceSatisfied(instance: QuestInstanceEntity, date: String): Boolean {
        val template = db.questDao().getTemplateById(instance.templateId)
        val key = template?.key.orEmpty()
        return when {
            key == "nutrition_daily" -> isMealTrackingComplete(date)
            key == "workout_daily" -> hasWorkoutWithSets(date)
            isJournalTemplate(key) -> hasJournalEntry(date)
            isSystemDesignTemplate(key) -> hasSystemDesignStudyToday(date)
            instance.verificationType == VerificationType.MANUAL.name -> true
            else -> isSatisfied(instance, date)
        }
    }

    private suspend fun isCountSatisfied(instance: QuestInstanceEntity, date: String): Boolean {
        val template = db.questDao().getTemplateById(instance.templateId)
        val templateKey = template?.key.orEmpty()
        if (!templateKey.contains("dsa", ignoreCase = true) &&
            !instance.title.contains("dsa", ignoreCase = true)
        ) {
            return false
        }
        val (dayStartMs, dayEndMs) = dayBoundsMs(date)
        val count = db.moduleDao().countDsaSolvedOnDate(dayStartMs, dayEndMs)
        return count >= instance.verificationTarget
    }

    private suspend fun isMetricSatisfied(instance: QuestInstanceEntity, date: String): Boolean {
        val unit = instance.verificationUnit.uppercase()
        if (unit == "CALORIES" || unit == "NUTRITION") {
            return isMealTrackingComplete(date)
        }
        val sum = db.moduleDao().sumMetricForDate(date, instance.verificationUnit)
        return sum >= instance.verificationTarget
    }

    private suspend fun isMealTrackingComplete(date: String): Boolean {
        val log = db.moduleDao().getDietLog(date) ?: return false
        return MealCompletionPolicy.isMealTrackingComplete(log)
    }

    private suspend fun hasWorkoutWithSets(date: String): Boolean {
        val log = db.moduleDao().getWorkoutLog(date) ?: return false
        return log.exercises.any { it.sets.isNotEmpty() }
    }

    private fun isJournalTemplate(templateKey: String?): Boolean =
        templateKey.orEmpty().contains("journal", ignoreCase = true)

    private suspend fun hasJournalEntry(date: String): Boolean {
        val entry = db.moduleDao().getJournal(date) ?: return false
        return entry.content.isNotBlank()
    }

    private fun isSystemDesignTemplate(templateKey: String?): Boolean =
        templateKey.orEmpty().contains("system_design", ignoreCase = true)

    private suspend fun hasSystemDesignStudyToday(date: String): Boolean {
        val (dayStartMs, dayEndMs) = dayBoundsMs(date)
        return db.xpDao().getAllLedger().any { entry ->
            entry.sourceType == "SD_CONCEPT" &&
                entry.createdAtEpochMs >= dayStartMs &&
                entry.createdAtEpochMs < dayEndMs
        }
    }

    private suspend fun isAutomaticSatisfied(instance: QuestInstanceEntity, date: String): Boolean {
        val template = db.questDao().getTemplateById(instance.templateId) ?: return false
        if (template.key != "weekly_review") return false
        val localDate = LocalDate.parse(date, dateFmt)
        val weekStart = localDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).format(dateFmt)
        val weekEnd = localDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusDays(6).format(dateFmt)
        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
        val modules = ModuleFlags.resolve(
            onboardingDone = profile?.onboardingDone == true,
            career = db.configDao().get(ModuleFlags.KEY_CAREER)?.value,
            workout = db.configDao().get(ModuleFlags.KEY_WORKOUT)?.value,
            diet = db.configDao().get(ModuleFlags.KEY_DIET)?.value,
        )
        val relevant = db.questDao().getInstancesInRange(weekStart, weekEnd).filter { row ->
            val rowTemplate = db.questDao().getTemplateById(row.templateId)
            if (rowTemplate?.key == "weekly_review") return@filter false
            ModuleScope.allowsQuestTemplate(rowTemplate?.priorityTags.orEmpty(), modules)
        }
        if (relevant.isEmpty()) return false
        return relevant.all { it.status == QuestStatus.COMPLETED.name }
    }

    private suspend fun dayBoundsMs(date: String): Pair<Long, Long> {
        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
        val zone = runCatching { ZoneId.of(profile?.timezone ?: ZoneId.systemDefault().id) }
            .getOrDefault(ZoneId.systemDefault())
        val day = LocalDate.parse(date, dateFmt)
        val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
        return start to start + 24L * 60 * 60 * 1000
    }
}
