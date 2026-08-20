package com.example.solo_levelling.ui.quests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.data.db.entity.BossEntity
import com.example.solo_levelling.data.db.entity.BossQuestEntity
import com.example.solo_levelling.data.db.entity.QuestInstanceEntity
import com.example.solo_levelling.domain.model.QuestStatus
import com.example.solo_levelling.domain.service.EnabledModules
import com.example.solo_levelling.domain.service.QuestCompletionService
import com.example.solo_levelling.domain.service.ModuleFlags
import com.example.solo_levelling.domain.service.ModuleScope
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class QuestTab {
    TODAY, WEEKLY, MILESTONES, BOSSES
}

data class BossProgressUi(
    val boss: BossEntity,
    val quests: List<BossQuestEntity>,
)

data class QuestListItem(
    val instance: QuestInstanceEntity,
    val priorityTags: String,
    val templateKey: String = "",
    val completedRequirements: Int = 0,
    val totalRequirements: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class QuestsViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE

    private val _selectedTab = MutableStateFlow(QuestTab.TODAY)
    val selectedTab: StateFlow<QuestTab> = _selectedTab.asStateFlow()

    fun selectTab(tab: QuestTab) {
        _selectedTab.value = tab
    }

    suspend fun complete(instanceId: Long): QuestCompletionService.Result =
        container.questCompletion.complete(instanceId)

    suspend fun undo(instanceId: Long): Boolean =
        container.questCompletion.undo(instanceId)

    private val dateContext = container.db.playerDao().observeProfile(SystemDefaults.PLAYER_ID)
        .map { p ->
            val zone = runCatching { ZoneId.of(p?.timezone ?: ZoneId.systemDefault().id) }
                .getOrDefault(ZoneId.systemDefault())
            val today = container.clock.today(zone)
            DateContext(
                today = today.format(dateFmt),
                weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).format(dateFmt),
                weekEnd = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusDays(6).format(dateFmt),
            )
        }

    private val enabledModules = ModuleFlags.observeEnabledModules(
        container.db.playerDao().observeProfile(SystemDefaults.PLAYER_ID),
        container.db.configDao(),
    )

    val todayAvailableXp: StateFlow<Int> =
        combine(dateContext, enabledModules) { ctx, modules -> ctx to modules }
            .flatMapLatest { (ctx, modules) ->
                combine(
                    container.db.questDao().observeInstancesForDate(ctx.today),
                    container.db.questDao().observeTemplates(),
                ) { list, templates ->
                    val tagsById = templates.associate { it.id to it.priorityTags }
                    val keysById = templates.associate { it.id to it.key }
                    val items = questListItemsFromTemplates(list, tagsById, keysById)
                    availableXpForModules(items, modules)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val quests: StateFlow<List<QuestListItem>> =
        combine(dateContext, _selectedTab, enabledModules) { ctx, tab, modules -> Triple(ctx, tab, modules) }
            .flatMapLatest { (ctx, tab, modules) ->
                if (tab == QuestTab.MILESTONES) {
                    return@flatMapLatest combine(
                        container.db.questDao().observeInstancesByType("MILESTONE"),
                        container.db.questDao().observeInstancesByType("DAILY"),
                        container.db.questDao().observeInstancesByType("WEEKLY"),
                        container.db.questDao().observeTemplates(),
                    ) { milestones, dailies, weeklies, templates ->
                        val templatesById = templates.associateBy { it.id }
                        val weekPool = dailies + weeklies
                        questItemsForModules(
                            milestones.mapNotNull { instance ->
                                val template = templatesById[instance.templateId] ?: return@mapNotNull null
                                val progress = container.milestoneVerification.evaluate(
                                    instance,
                                    weekPool,
                                    templatesById,
                                    modules,
                                )
                                QuestListItem(
                                    instance = instance,
                                    priorityTags = template.priorityTags,
                                    templateKey = template.key,
                                    completedRequirements = progress.completedCount,
                                    totalRequirements = progress.totalCount,
                                )
                            },
                            modules,
                        )
                    }
                }
                val instances = when (tab) {
                    QuestTab.TODAY -> container.db.questDao().observeInstancesForDate(ctx.today)
                        .map { list -> list.filter { it.type == "DAILY" } }
                    QuestTab.WEEKLY -> container.db.questDao().observeInstancesByType("WEEKLY")
                        .map { list -> list.filter { it.scheduledDate in ctx.weekStart..ctx.weekEnd } }
                    QuestTab.MILESTONES -> container.db.questDao().observeInstancesByType("MILESTONE")
                    QuestTab.BOSSES -> container.db.questDao().observeInstancesForDate(ctx.today)
                        .map { emptyList() }
                }
                combine(instances, container.db.questDao().observeTemplates()) { list, templates ->
                    val tagsById = templates.associate { it.id to it.priorityTags }
                    val keysById = templates.associate { it.id to it.key }
                    questItemsForModules(
                        questListItemsFromTemplates(list, tagsById, keysById),
                        modules,
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val bossProgress: StateFlow<BossProgressUi?> =
        combine(
            container.db.moduleDao().observeBosses(),
            container.db.questDao().observeTemplates(),
            enabledModules,
        ) { bosses, templates, modules -> Triple(bosses, templates, modules) }
            .flatMapLatest { (bosses, templates, modules) ->
                val active = bosses.firstOrNull { it.status == "ACTIVE" }
                    ?: return@flatMapLatest flowOf(null)
                val tagsByKey = templates.associate { it.key to it.priorityTags }
                container.db.moduleDao().observeBossQuests(active.id).map { quests ->
                    BossProgressUi(active, bossQuestsForModules(quests, tagsByKey, modules))
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private data class DateContext(
        val today: String,
        val weekStart: String,
        val weekEnd: String,
    )

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                QuestsViewModel(container) as T
        }
    }
}

internal fun questListItemsFromTemplates(
    instances: List<QuestInstanceEntity>,
    tagsById: Map<Long, String>,
    keysById: Map<Long, String>,
): List<QuestListItem> = instances.mapNotNull { instance ->
    val tags = tagsById[instance.templateId] ?: return@mapNotNull null
    QuestListItem(
        instance = instance,
        priorityTags = tags,
        templateKey = keysById[instance.templateId].orEmpty(),
    )
}

internal fun questItemsForModules(
    items: List<QuestListItem>,
    modules: EnabledModules,
): List<QuestListItem> =
    items.filter { ModuleScope.allowsQuestTemplate(it.priorityTags, modules) }

internal fun availableXpForModules(
    items: List<QuestListItem>,
    modules: EnabledModules,
): Int = questItemsForModules(items, modules)
    .filter { it.instance.status == QuestStatus.AVAILABLE.name }
    .sumOf { it.instance.baseXp }

internal fun bossQuestsForModules(
    quests: List<BossQuestEntity>,
    tagsByTemplateKey: Map<String, String>,
    modules: EnabledModules,
): List<BossQuestEntity> = quests.filter { quest ->
    val tags = tagsByTemplateKey[quest.templateKey] ?: return@filter false
    ModuleScope.allowsQuestTemplate(tags, modules)
}
