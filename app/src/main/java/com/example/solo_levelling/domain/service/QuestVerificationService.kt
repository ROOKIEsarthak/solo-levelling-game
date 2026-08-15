package com.example.solo_levelling.domain.service

import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.core.time.AppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.QuestInstanceEntity
import com.example.solo_levelling.domain.model.QuestStatus
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
        val instances = db.questDao().getInstancesForDate(date)
            .filter { it.status == QuestStatus.AVAILABLE.name }
        for (instance in instances) {
            val template = db.questDao().getTemplateById(instance.templateId)
            if (instance.verificationType == VerificationType.MANUAL.name) {
                if (template?.key == "nutrition_daily" && isNutritionCalorieTargetMet(date)) {
                    questCompletion.complete(instance.id)
                }
                continue
            }
            if (!isSatisfied(instance, date)) continue
            questCompletion.complete(instance.id)
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
            return isNutritionCalorieTargetMet(date)
        }
        val sum = db.moduleDao().sumMetricForDate(date, instance.verificationUnit)
        return sum >= instance.verificationTarget
    }

    private suspend fun isNutritionCalorieTargetMet(date: String): Boolean {
        val log = db.moduleDao().getNutrition(date) ?: return false
        val target = db.configDao().get("calorie_target")?.value?.toIntOrNull() ?: 2200
        val low = (target * 0.85).toInt()
        val high = (target * 1.15).toInt()
        return log.calories in low..high
    }

    private suspend fun isAutomaticSatisfied(instance: QuestInstanceEntity, date: String): Boolean {
        val template = db.questDao().getTemplateById(instance.templateId) ?: return false
        if (template.key != "weekly_review") return false
        val localDate = LocalDate.parse(date, dateFmt)
        val weekStart = localDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).format(dateFmt)
        val weekEnd = localDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusDays(6).format(dateFmt)
        val total = db.questDao().countTotalInRange(weekStart, weekEnd)
        if (total == 0) return false
        val completed = db.questDao().countCompletedInRange(weekStart, weekEnd)
        val incomplete = db.questDao().countIncompleteInRange(weekStart, weekEnd)
        return completed == total && incomplete == 0
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
