package com.example.solo_levelling.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.UserConfigEntity
import com.example.solo_levelling.domain.service.AnalyticsService
import com.example.solo_levelling.domain.service.EnabledModules
import com.example.solo_levelling.domain.service.ModuleApplyResult
import com.example.solo_levelling.domain.service.ModuleLifecycleService
import com.example.solo_levelling.domain.service.ModuleService
import com.example.solo_levelling.domain.service.OnboardingService
import com.example.solo_levelling.domain.service.ProgressionService
import com.example.solo_levelling.domain.service.QuestGenerationService
import com.example.solo_levelling.domain.service.SeasonService

class SettingsViewModel(
    private val db: JsonDatabase,
    private val moduleLifecycle: ModuleLifecycleService,
    private val modules: ModuleService,
    private val onboarding: OnboardingService,
    private val questGeneration: QuestGenerationService,
    private val progression: ProgressionService,
    private val season: SeasonService,
    private val analytics: AnalyticsService,
) : ViewModel() {
    constructor(container: AppContainer) : this(
        container.db,
        container.moduleLifecycle,
        container.modules,
        container.onboarding,
        container.questGeneration,
        container.progression,
        container.season,
        container.analytics,
    )

    suspend fun getProfile(): PlayerProfileEntity? =
        db.playerDao().getProfile(SystemDefaults.PLAYER_ID)

    suspend fun upsertProfile(profile: PlayerProfileEntity) {
        db.playerDao().upsertProfile(profile)
    }

    suspend fun upsertConfig(key: String, value: String) {
        db.configDao().upsert(UserConfigEntity(key, value))
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        upsertConfig("notifications_enabled", if (enabled) "true" else "false")
    }

    suspend fun applyModuleChanges(target: EnabledModules): ModuleApplyResult =
        moduleLifecycle.applyModuleChanges(target)

    suspend fun applyWorkoutSplit(
        workoutSplitId: String,
        dayMapCsv: String,
        confirmEarlyChange: Boolean,
    ): String? = modules.applyWorkoutSplit(workoutSplitId, dayMapCsv, confirmEarlyChange)

    suspend fun weeksOnCurrentSplit(): Long = modules.weeksOnCurrentSplit()

    suspend fun resetAllProgress() {
        onboarding.resetAllProgress()
    }

    suspend fun regenerateQuests(timezone: String) {
        questGeneration.generateForToday(timezone)
    }

    suspend fun rebuildXp(): ProgressionService.RebuildResult {
        val result = progression.rebuildFromLedger()
        season.rebuildFromLedger(onboarding.currentModules())
        return result
    }

    suspend fun exportJson(): String = analytics.exportJson()

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SettingsViewModel(container) as T
        }
    }
}
