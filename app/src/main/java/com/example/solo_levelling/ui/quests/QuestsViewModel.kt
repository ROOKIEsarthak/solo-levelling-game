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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class QuestTab {
    TODAY, WEEKLY, MILESTONES, RECOVERY, BOSSES
}

data class BossProgressUi(
    val boss: BossEntity,
    val quests: List<BossQuestEntity>,
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

    val todayAvailableXp: StateFlow<Int> = dateContext
        .flatMapLatest { ctx ->
            container.db.questDao().observeInstancesForDate(ctx.today)
                .map { list ->
                    list.filter { it.status == QuestStatus.AVAILABLE.name }.sumOf { it.baseXp }
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val quests: StateFlow<List<QuestInstanceEntity>> = combine(dateContext, _selectedTab) { ctx, tab -> ctx to tab }
        .flatMapLatest { (ctx, tab) ->
            when (tab) {
                QuestTab.TODAY -> container.db.questDao().observeInstancesForDate(ctx.today)
                    .map { list -> list.filter { it.type == "DAILY" } }
                QuestTab.WEEKLY -> container.db.questDao().observeInstancesByType("WEEKLY")
                    .map { list -> list.filter { it.scheduledDate in ctx.weekStart..ctx.weekEnd } }
                QuestTab.MILESTONES -> container.db.questDao().observeInstancesByType("MILESTONE")
                QuestTab.RECOVERY -> container.db.questDao().observeInstancesByType("RECOVERY")
                    .map { list -> list.filter { it.scheduledDate == ctx.today } }
                QuestTab.BOSSES -> container.db.questDao().observeInstancesForDate(ctx.today)
                    .map { emptyList() }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val bossProgress: StateFlow<BossProgressUi?> = container.db.moduleDao().observeBosses()
        .map { bosses ->
            val active = bosses.firstOrNull { it.status == "ACTIVE" } ?: return@map null
            BossProgressUi(active, emptyList())
        }
        .flatMapLatest { ui ->
            if (ui == null) {
                kotlinx.coroutines.flow.flowOf(null)
            } else {
                container.db.moduleDao().observeBossQuests(ui.boss.id)
                    .map { quests -> ui.copy(quests = quests) }
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
