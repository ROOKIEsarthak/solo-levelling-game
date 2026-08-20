package com.example.solo_levelling.domain.service

import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.AttributeStatEntity
import com.example.solo_levelling.data.db.entity.XpLedgerEntryEntity
import com.example.solo_levelling.domain.model.AttributeCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Canonical active-module read model. Storage remains archival; this filters for gameplay surfaces.
 */
class ActiveProgressionReader(
    private val db: JsonDatabase,
) {
    suspend fun currentModules(): EnabledModules {
        val profile = db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
        return ModuleFlags.resolve(
            onboardingDone = profile?.onboardingDone == true,
            career = db.configDao().get(ModuleFlags.KEY_CAREER)?.value,
            workout = db.configDao().get(ModuleFlags.KEY_WORKOUT)?.value,
            diet = db.configDao().get(ModuleFlags.KEY_DIET)?.value,
        )
    }

    suspend fun allowsEntry(entry: XpLedgerEntryEntity, modules: EnabledModules): Boolean {
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

    suspend fun allowsEntry(entry: XpLedgerEntryEntity): Boolean =
        allowsEntry(entry, currentModules())

    suspend fun activeLedger(
        modules: EnabledModules,
        limit: Int = Int.MAX_VALUE,
    ): List<XpLedgerEntryEntity> {
        return db.xpDao().getAllLedger()
            .filter { allowsEntry(it, modules) }
            .sortedByDescending { it.createdAtEpochMs }
            .take(limit)
    }

    suspend fun activeLedger(limit: Int = Int.MAX_VALUE): List<XpLedgerEntryEntity> =
        activeLedger(currentModules(), limit)

    fun observeActiveLedger(limit: Int = 50): Flow<List<XpLedgerEntryEntity>> =
        combine(
            db.xpDao().observeLedger(),
            ModuleFlags.observeEnabledModules(
                db.playerDao().observeProfile(SystemDefaults.PLAYER_ID),
                db.configDao(),
            ),
        ) { ledger, modules ->
            ledger
                .filter { entry ->
                    ModuleScope.allowsLedgerEntry(entry.sourceType, entry.metadataJson, modules)
                }
                .sortedByDescending { it.createdAtEpochMs }
                .take(limit)
        }

    suspend fun activeAttributes(modules: EnabledModules): List<AttributeStatEntity> {
        val totals = sumAllowedAttributeDeltas(modules)
        return AttributeCode.entries.map { code ->
            val value = totals[code] ?: 0
            AttributeStatEntity(code = code.name, currentValue = value.coerceAtLeast(0), lifetimeXp = value.coerceAtLeast(0))
        }
    }

    suspend fun activeAttributes(): List<AttributeStatEntity> =
        activeAttributes(currentModules())


    fun observeActiveAttributes(): Flow<List<AttributeStatEntity>> =
        combine(
            db.playerDao().observeAttributes(),
            ModuleFlags.observeEnabledModules(
                db.playerDao().observeProfile(SystemDefaults.PLAYER_ID),
                db.configDao(),
            ),
        ) { attrs, modules ->
            attrs.filter { AnalyticsService.isAttributeActionable(it.code, modules) }
        }

    suspend fun sumAllowedAttributeDeltas(modules: EnabledModules): Map<AttributeCode, Int> {
        val totals = mutableMapOf<AttributeCode, Int>()
        for (entry in db.xpDao().getAllLedger()) {
            if (!allowsEntry(entry, modules)) continue
            for (delta in AttributeRewardsParser.parse(entry.metadataJson)) {
                totals[delta.code] = (totals[delta.code] ?: 0) + delta.amount
            }
        }
        return totals
    }

    private suspend fun resolveQuestInstanceModule(sourceId: String): ModuleId {
        val instanceId = sourceId.substringBefore('_').toLongOrNull() ?: return ModuleId.GLOBAL
        val instance = db.questDao().getInstance(instanceId) ?: return ModuleId.GLOBAL
        val template = db.questDao().getTemplateById(instance.templateId)
        val tags = template?.priorityTags ?: return ModuleId.GLOBAL
        return ModuleScope.moduleForPriorityTags(tags)
    }
}
