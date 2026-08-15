package com.example.solo_levelling.domain.service

import com.example.solo_levelling.core.time.AppClock
import com.example.solo_levelling.data.db.AppDatabase
import com.example.solo_levelling.data.db.entity.QuestInstanceEntity
import com.example.solo_levelling.domain.model.QuestStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

class QuestGenerationService(
    private val db: AppDatabase,
    private val clock: AppClock,
) {
    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE

    suspend fun generateForToday(timezone: String = ZoneId.systemDefault().id) {
        val zone = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneId.systemDefault())
        val today = clock.today(zone)
        generateForDate(today)
        generateWeeklyIfNeeded(today)
    }

    suspend fun generateForDate(date: LocalDate) {
        val dateStr = date.format(dateFmt)
        val dayNum = date.dayOfWeek.value // 1=Mon .. 7=Sun
        val templates = db.questDao().getActiveTemplates()
        for (template in templates) {
            if (template.type == "WEEKLY" || template.type == "RECOVERY") continue
            val days = template.scheduleDaysCsv.split(",").mapNotNull { it.trim().toIntOrNull() }
            if (days.isNotEmpty() && dayNum !in days) continue
            db.questDao().insertInstance(
                QuestInstanceEntity(
                    templateId = template.id,
                    scheduledDate = dateStr,
                    status = QuestStatus.AVAILABLE.name,
                    title = template.title,
                    type = template.type,
                    baseXp = template.baseXp,
                    attributeRewardsJson = template.attributeRewardsJson,
                ),
            )
        }
    }

    private suspend fun generateWeeklyIfNeeded(today: LocalDate) {
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = weekStart.plusDays(6)
        val templates = db.questDao().getActiveTemplates().filter { it.type == "WEEKLY" }
        for (template in templates) {
            val days = template.scheduleDaysCsv.split(",").mapNotNull { it.trim().toIntOrNull() }
            val targetDay = days.firstOrNull() ?: 7
            val targetDate = weekStart.plusDays((targetDay - 1).toLong())
            if (today.isBefore(targetDate)) continue
            db.questDao().insertInstance(
                QuestInstanceEntity(
                    templateId = template.id,
                    scheduledDate = weekEnd.format(dateFmt),
                    status = QuestStatus.AVAILABLE.name,
                    title = template.title,
                    type = template.type,
                    baseXp = template.baseXp,
                    attributeRewardsJson = template.attributeRewardsJson,
                ),
            )
        }
    }
}
