package com.example.solo_levelling.domain.service

import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.core.time.AppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.SeasonEntity
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class SeasonService(
    private val db: JsonDatabase,
    private val clock: AppClock,
) {
    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE

    suspend fun ensureActiveSeason(): SeasonEntity {
        endSeasonIfPast()
        val existing = db.moduleDao().getActiveSeason()
        if (existing != null) return existing
        val zone = playerZone()
        val today = clock.today(zone)
        val endDate = today.plusWeeks(12).minusDays(1)
        val season = SeasonEntity(
            name = "Season ${today.year}-Q${((today.monthValue - 1) / 3) + 1}",
            startDate = today.format(dateFmt),
            endDate = endDate.format(dateFmt),
            status = "ACTIVE",
            seasonXp = 0,
        )
        val id = db.moduleDao().upsertSeason(season)
        return season.copy(id = id)
    }

    suspend fun addSeasonXp(amount: Int) {
        if (amount == 0) return
        val season = ensureActiveSeason()
        db.moduleDao().upsertSeason(
            season.copy(seasonXp = (season.seasonXp + amount).coerceAtLeast(0)),
        )
    }

    suspend fun rebuildFromLedger(modules: EnabledModules) {
        val season = db.moduleDao().getActiveSeason() ?: return
        val zone = playerZone()
        val startMs = LocalDate.parse(season.startDate, dateFmt).atStartOfDay(zone).toInstant().toEpochMilli()
        val endExclusive = LocalDate.parse(season.endDate, dateFmt)
            .plusDays(1)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
        val xp = db.xpDao().getAllLedger()
            .filter { it.createdAtEpochMs in startMs until endExclusive }
            .sumOf { entry -> if (allowsXpEntry(entry, modules)) entry.amount else 0 }
        db.moduleDao().upsertSeason(season.copy(seasonXp = xp.coerceAtLeast(0)))
    }

    private suspend fun allowsXpEntry(
        entry: com.example.solo_levelling.data.db.entity.XpLedgerEntryEntity,
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

    suspend fun endSeasonIfPast() {
        val season = db.moduleDao().getActiveSeason() ?: return
        val today = clock.today(playerZone())
        val endDate = LocalDate.parse(season.endDate, dateFmt)
        if (!today.isAfter(endDate)) return
        db.moduleDao().upsertSeason(season.copy(status = "ENDED"))
    }

    private suspend fun playerZone(): ZoneId {
        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
        return runCatching { ZoneId.of(profile?.timezone ?: ZoneId.systemDefault().id) }
            .getOrDefault(ZoneId.systemDefault())
    }
}
