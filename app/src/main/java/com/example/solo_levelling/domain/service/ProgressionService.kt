package com.example.solo_levelling.domain.service

import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.core.event.DomainEvent
import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.AppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.AttributeStatEntity
import com.example.solo_levelling.data.db.entity.XpLedgerEntryEntity
import com.example.solo_levelling.domain.model.AttributeCode
import java.time.ZoneId

class ProgressionService(
    private val db: JsonDatabase,
    private val eventBus: EventBus,
    private val clock: AppClock,
) {
    sealed class AwardResult {
        data class Success(
            val ledgerId: Long,
            val awarded: Int,
            val newTotal: Int,
            val newLevel: Int,
            val newRank: String,
        ) : AwardResult()

        data object AlreadyAwarded : AwardResult()
        data object CapReached : AwardResult()
        data object NoProfile : AwardResult()
        data object ModuleDisabled : AwardResult()
    }

    data class RebuildResult(
        val oldTotal: Int,
        val newTotal: Int,
        val oldLevel: Int,
        val newLevel: Int,
        val oldRank: String,
        val newRank: String,
    )

    suspend fun currentModules(): EnabledModules {
        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
        return ModuleFlags.resolve(
            onboardingDone = profile?.onboardingDone == true,
            career = db.configDao().get(ModuleFlags.KEY_CAREER)?.value,
            workout = db.configDao().get(ModuleFlags.KEY_WORKOUT)?.value,
            diet = db.configDao().get(ModuleFlags.KEY_DIET)?.value,
        )
    }

    suspend fun award(
        sourceType: String,
        sourceId: String,
        amount: Int,
        attrs: Map<AttributeCode, Int> = emptyMap(),
        metadataJson: String = "{}",
        applyDailyCap: Boolean = true,
        modules: EnabledModules? = null,
    ): AwardResult {
        val events = mutableListOf<DomainEvent>()
        val result = db.withTransaction {
            awardWithinTransaction(
                sourceType,
                sourceId,
                amount,
                attrs,
                metadataJson,
                applyDailyCap,
                events,
                modules,
            )
        }
        events.forEach { eventBus.publish(it) }
        return result
    }

    suspend fun awardWithinTransaction(
        sourceType: String,
        sourceId: String,
        amount: Int,
        attrs: Map<AttributeCode, Int> = emptyMap(),
        metadataJson: String = "{}",
        applyDailyCap: Boolean = true,
        events: MutableList<DomainEvent>,
        modules: EnabledModules? = null,
    ): AwardResult {
        val xpDao = db.xpDao()
        val playerDao = db.playerDao()
        val activeModules = modules ?: resolveModulesUnlocked()

        if (!ModuleScope.allowsLedgerEntry(sourceType, metadataJson, activeModules)) {
            return AwardResult.ModuleDisabled
        }

        if (xpDao.findBySource(sourceType, sourceId) != null) return AwardResult.AlreadyAwarded

        val profile = playerDao.getProfile(SystemDefaults.PLAYER_ID)
            ?: return AwardResult.NoProfile

        var xpToAward = amount
        if (applyDailyCap && amount > 0) {
            val dayStart = startOfDayMs(profile.timezone)
            val dayEnd = dayStart + 24L * 60 * 60 * 1000
            val earnedToday = sumAllowedXpBetween(dayStart, dayEnd, activeModules)
            if (earnedToday >= SystemDefaults.DAILY_XP_CAP) return AwardResult.CapReached
            xpToAward = minOf(amount, SystemDefaults.DAILY_XP_CAP - earnedToday)
            if (xpToAward <= 0) return AwardResult.CapReached
        }

        val now = clock.nowEpochMs()
        val oldLevel = profile.level
        val oldRank = profile.rank
        val newTotal = profile.totalXp + xpToAward
        val newLevel = SystemDefaults.levelFromTotalXp(newTotal)
        val newRank = SystemDefaults.rankForLevel(newLevel)

        val ledgerId = xpDao.insertLedger(
            XpLedgerEntryEntity(
                amount = xpToAward,
                sourceType = sourceType,
                sourceId = sourceId,
                metadataJson = metadataJson,
                createdAtEpochMs = now,
            ),
        )
        playerDao.upsertProfile(profile.copy(totalXp = newTotal, level = newLevel, rank = newRank))

        if (attrs.isNotEmpty()) {
            val existingAttrs = playerDao.getAttributes().associateBy { it.code }
            for ((code, delta) in attrs) {
                val existing = existingAttrs[code.name] ?: AttributeStatEntity(code = code.name)
                playerDao.upsertAttribute(
                    existing.copy(
                        currentValue = existing.currentValue + delta,
                        lifetimeXp = existing.lifetimeXp + delta,
                    ),
                )
            }
            events += DomainEvent.AttributesProgressed(AttributeRewardsParser.toJsonFromMap(attrs))
        }

        events += DomainEvent.XpAwarded(
            ledgerId = ledgerId,
            amount = xpToAward,
            sourceType = sourceType,
            sourceId = sourceId,
            totalXpAfter = newTotal,
        )
        if (newLevel > oldLevel) events += DomainEvent.LevelUp(oldLevel, newLevel)
        if (newRank != oldRank) events += DomainEvent.RankUp(oldRank, newRank)

        return AwardResult.Success(ledgerId, xpToAward, newTotal, newLevel, newRank)
    }

    suspend fun reverse(
        originalSourceType: String,
        originalSourceId: String,
        reverseSourceType: String,
        reverseSourceId: String,
    ): Boolean {
        val events = mutableListOf<DomainEvent>()
        val ok = db.withTransaction {
            reverseWithinTransaction(
                originalSourceType,
                originalSourceId,
                reverseSourceType,
                reverseSourceId,
                events,
            )
        }
        if (ok) events.forEach { eventBus.publish(it) }
        return ok
    }

    suspend fun reverseWithinTransaction(
        originalSourceType: String,
        originalSourceId: String,
        reverseSourceType: String,
        reverseSourceId: String,
        events: MutableList<DomainEvent>,
        attrs: Map<AttributeCode, Int> = emptyMap(),
    ): Boolean {
        val xpDao = db.xpDao()
        val playerDao = db.playerDao()

        if (xpDao.findBySource(reverseSourceType, reverseSourceId) != null) return false
        val original = xpDao.findBySource(originalSourceType, originalSourceId) ?: return false
        val profile = playerDao.getProfile(SystemDefaults.PLAYER_ID) ?: return false

        val now = clock.nowEpochMs()
        val newTotal = (profile.totalXp - original.amount).coerceAtLeast(0)
        val newLevel = SystemDefaults.levelFromTotalXp(newTotal)
        val newRank = SystemDefaults.rankForLevel(newLevel)

        val moduleMeta = ModuleScope.parseModuleFromMetadata(original.metadataJson)?.name
            ?: ModuleScope.moduleForSourceType(originalSourceType).name
        val ledgerId = xpDao.insertLedger(
            XpLedgerEntryEntity(
                amount = -original.amount,
                sourceType = reverseSourceType,
                sourceId = reverseSourceId,
                metadataJson = """{"originalLedgerId":${original.id},"module":"$moduleMeta"}""",
                createdAtEpochMs = now,
            ),
        )
        playerDao.upsertProfile(profile.copy(totalXp = newTotal, level = newLevel, rank = newRank))

        if (attrs.isNotEmpty()) {
            val existingAttrs = playerDao.getAttributes().associateBy { it.code }
            for ((code, delta) in attrs) {
                val existing = existingAttrs[code.name] ?: continue
                playerDao.upsertAttribute(
                    existing.copy(currentValue = (existing.currentValue - delta).coerceAtLeast(0)),
                )
            }
        }

        events += DomainEvent.XpReversed(
            ledgerId = ledgerId,
            amount = -original.amount,
            sourceType = reverseSourceType,
            sourceId = reverseSourceId,
            totalXpAfter = newTotal,
        )
        return true
    }

    suspend fun rebuildFromLedger(): RebuildResult =
        rebuildActiveFromLedger(currentModules())

    suspend fun rebuildActiveFromLedger(modules: EnabledModules): RebuildResult {
        val events = mutableListOf<DomainEvent>()
        val result = db.withTransaction {
            val playerDao = db.playerDao()
            val profile = playerDao.getProfile(SystemDefaults.PLAYER_ID)
                ?: return@withTransaction RebuildResult(0, 0, 1, 1, "E", "E")

            val oldTotal = profile.totalXp
            val oldLevel = profile.level
            val oldRank = profile.rank
            val newTotal = sumAllowedXp(modules).coerceAtLeast(0)
            val newLevel = SystemDefaults.levelFromTotalXp(newTotal)
            val newRank = SystemDefaults.rankForLevel(newLevel)

            playerDao.upsertProfile(profile.copy(totalXp = newTotal, level = newLevel, rank = newRank))

            if (newLevel > oldLevel) events += DomainEvent.LevelUp(oldLevel, newLevel)
            if (newRank != oldRank) events += DomainEvent.RankUp(oldRank, newRank)

            RebuildResult(oldTotal, newTotal, oldLevel, newLevel, oldRank, newRank)
        }
        events.forEach { eventBus.publish(it) }
        return result
    }

    suspend fun sumXpForModule(module: ModuleId, modules: EnabledModules): Int {
        if (!ModuleScope.isEnabled(module, modules) && module != ModuleId.GLOBAL) return 0
        return db.xpDao().getAllLedger().sumOf { entry ->
            val owner = ledgerModule(entry)
            if (owner == module) entry.amount else 0
        }
    }

    suspend fun sumXpForModule(module: ModuleId): Int =
        sumXpForModule(module, currentModules())

    private suspend fun resolveModulesUnlocked(): EnabledModules {
        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
        return ModuleFlags.resolve(
            onboardingDone = profile?.onboardingDone == true,
            career = db.configDao().get(ModuleFlags.KEY_CAREER)?.value,
            workout = db.configDao().get(ModuleFlags.KEY_WORKOUT)?.value,
            diet = db.configDao().get(ModuleFlags.KEY_DIET)?.value,
        )
    }

    private suspend fun sumAllowedXp(modules: EnabledModules): Int =
        db.xpDao().getAllLedger().sumOf { entry ->
            if (allowsEntry(entry, modules)) entry.amount else 0
        }

    private suspend fun sumAllowedXpBetween(startMs: Long, endMs: Long, modules: EnabledModules): Int =
        db.xpDao().getAllLedger()
            .filter { it.createdAtEpochMs in startMs until endMs }
            .sumOf { entry -> if (allowsEntry(entry, modules)) entry.amount else 0 }

    private suspend fun allowsEntry(entry: XpLedgerEntryEntity, modules: EnabledModules): Boolean {
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

    private suspend fun ledgerModule(entry: XpLedgerEntryEntity): ModuleId {
        ModuleScope.parseModuleFromMetadata(entry.metadataJson)?.let { return it }
        if (entry.sourceType.equals("QUEST_INSTANCE", ignoreCase = true)) {
            return resolveQuestInstanceModule(entry.sourceId)
        }
        return ModuleScope.moduleForSourceType(entry.sourceType)
    }

    private suspend fun resolveQuestInstanceModule(sourceId: String): ModuleId {
        val instanceId = sourceId.substringBefore('_').toLongOrNull() ?: return ModuleId.GLOBAL
        val instance = db.questDao().getInstance(instanceId) ?: return ModuleId.GLOBAL
        val template = db.questDao().getTemplateById(instance.templateId)
        val tags = template?.priorityTags ?: return ModuleId.GLOBAL
        return ModuleScope.moduleForPriorityTags(tags)
    }

    private fun startOfDayMs(timezone: String): Long {
        val zone = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneId.systemDefault())
        return clock.today(zone).atStartOfDay(zone).toInstant().toEpochMilli()
    }
}
