package com.example.solo_levelling.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.data.db.entity.AttributeStatEntity
import com.example.solo_levelling.data.db.entity.BossEntity
import com.example.solo_levelling.data.db.entity.PlayerAchievementEntity
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.QuestInstanceEntity
import com.example.solo_levelling.data.db.entity.SeasonEntity
import com.example.solo_levelling.data.db.entity.StreakStateEntity
import com.example.solo_levelling.domain.service.AdaptiveSuggestion
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE
    private val refreshSuggestions = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val profile: StateFlow<PlayerProfileEntity?> =
        container.db.playerDao().observeProfile(SystemDefaults.PLAYER_ID)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val streak: StateFlow<StreakStateEntity?> =
        container.db.playerDao().observeStreak(SystemDefaults.PLAYER_ID)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val attributes: StateFlow<List<AttributeStatEntity>> =
        container.db.playerDao().observeAttributes()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todayQuests: StateFlow<List<QuestInstanceEntity>> =
        profile.flatMapLatest { p ->
            val zone = runCatching { ZoneId.of(p?.timezone ?: ZoneId.systemDefault().id) }
                .getOrDefault(ZoneId.systemDefault())
            val today = container.clock.today(zone).format(dateFmt)
            container.db.questDao().observeInstancesForDate(today)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeBoss: StateFlow<BossEntity?> =
        container.db.moduleDao().observeBosses()
            .map { bosses -> bosses.firstOrNull { it.status == "ACTIVE" } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val activeSeason: StateFlow<SeasonEntity?> =
        container.db.moduleDao().observeActiveSeason()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val recentAchievements: StateFlow<List<PlayerAchievementEntity>> =
        container.db.achievementDao().observeUnlocked()
            .map { list -> list.sortedByDescending { it.unlockedAtEpochMs }.take(3) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val weeklyCompletionPct: StateFlow<Float> =
        profile.flatMapLatest { p ->
            flow {
                val zone = runCatching { ZoneId.of(p?.timezone ?: ZoneId.systemDefault().id) }
                    .getOrDefault(ZoneId.systemDefault())
                val today = container.clock.today(zone)
                val weekStart = today.with(java.time.DayOfWeek.MONDAY).format(dateFmt)
                val weekEnd = today.with(java.time.DayOfWeek.MONDAY).plusDays(6).format(dateFmt)
                val completed = container.db.questDao().countCompletedInRange(weekStart, weekEnd)
                val total = container.db.questDao().countTotalInRange(weekStart, weekEnd)
                emit(if (total == 0) 0f else completed.toFloat() / total.toFloat())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0f)

    val xpLast7Days: StateFlow<Int> =
        profile.flatMapLatest { p ->
            flow {
                val zone = runCatching { ZoneId.of(p?.timezone ?: ZoneId.systemDefault().id) }
                    .getOrDefault(ZoneId.systemDefault())
                val today = container.clock.today(zone)
                val startMs = today.minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli()
                val endMs = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                emit(container.db.xpDao().sumXpBetween(startMs, endMs))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val suggestions: StateFlow<List<AdaptiveSuggestion>> =
        refreshSuggestions.onStart { emit(Unit) }
            .flatMapLatest {
                flow { emit(container.adaptive.suggestions()) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val goalTitle: StateFlow<String?> =
        container.db.configDao().observe("goal_title")
            .map { it?.value?.takeIf { v -> v.isNotBlank() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            val p = container.db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
            if (p?.onboardingDone == true) {
                container.questGeneration.generateForToday(p.timezone)
            }
        }
    }

    fun dismissSuggestion(key: String) {
        viewModelScope.launch {
            container.adaptive.dismissSuggestion(key)
            refreshSuggestions.emit(Unit)
        }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                DashboardViewModel(container) as T
        }
    }
}
