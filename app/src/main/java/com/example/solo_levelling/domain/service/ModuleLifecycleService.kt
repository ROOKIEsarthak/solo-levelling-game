package com.example.solo_levelling.domain.service

import com.example.solo_levelling.core.time.AppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.data.db.entity.UserConfigEntity
import com.example.solo_levelling.data.db.entity.WorkoutRoutineEntity

sealed class ModuleChangeResult {
    data object Enabled : ModuleChangeResult()
    data object SetupRequired : ModuleChangeResult()
    data object Disabled : ModuleChangeResult()
    data object LastModule : ModuleChangeResult()
    data object Unchanged : ModuleChangeResult()
    data object InvalidModule : ModuleChangeResult()
}

data class ModuleChangePlan(
    val toDisable: List<String> = emptyList(),
    val toEnableImmediate: List<String> = emptyList(),
    val setupQueue: List<String> = emptyList(),
    val deferredDisables: List<String> = emptyList(),
    val blocked: Boolean = false,
)

data class ModuleApplyResult(
    val setupQueue: List<String> = emptyList(),
    val deferredDisables: List<String> = emptyList(),
    val added: List<String> = emptyList(),
    val blocked: Boolean = false,
)

internal val MODULE_SETUP_ORDER = listOf(
    ModuleFlags.MODULE_WORKOUT,
    ModuleFlags.MODULE_DIET,
    ModuleFlags.MODULE_CAREER,
)

internal suspend fun moduleConfigLooksInitialized(db: JsonDatabase, module: String): Boolean = when (module) {
    ModuleFlags.MODULE_CAREER ->
        db.configDao().get("career_intent")?.value?.isNotBlank() == true
    ModuleFlags.MODULE_WORKOUT -> {
        val split = db.configDao().get("workout_split_id")?.value.orEmpty().isNotBlank()
        val routine = db.moduleDao().getWorkoutRoutine().hasEnabledTrainingDay()
        split || routine
    }
    ModuleFlags.MODULE_DIET -> {
        val height = db.configDao().get("height_cm")?.value?.toDoubleOrNull() ?: 0.0
        val weight = db.configDao().get("weight_kg")?.value?.toDoubleOrNull() ?: 0.0
        height > 0.0 && weight > 0.0
    }
    else -> false
}

/** Enabled flags that also have required module data. GLOBAL quests ignore this. */
internal suspend fun eligibleModules(db: JsonDatabase, flags: EnabledModules): EnabledModules =
    EnabledModules(
        career = flags.career && moduleConfigLooksInitialized(db, ModuleFlags.MODULE_CAREER),
        workout = flags.workout && moduleConfigLooksInitialized(db, ModuleFlags.MODULE_WORKOUT),
        diet = flags.diet && moduleConfigLooksInitialized(db, ModuleFlags.MODULE_DIET),
    )

class ModuleLifecycleService(
    private val db: JsonDatabase,
    private val clock: AppClock,
    private val onboarding: OnboardingService,
) {
    suspend fun isModuleInitialized(module: String): Boolean =
        moduleConfigLooksInitialized(db, module)

    suspend fun needsSetup(module: String): Boolean = !isModuleInitialized(module)

    suspend fun requestEnable(module: String): ModuleChangeResult {
        if (module !in ModuleFlags.SELECTABLE) return ModuleChangeResult.InvalidModule
        val current = onboarding.currentModules()
        if (needsSetup(module)) return ModuleChangeResult.SetupRequired
        if (current.isEnabled(module)) return ModuleChangeResult.Unchanged
        applyEnabled(current.withModule(module, true), previous = current)
        return ModuleChangeResult.Enabled
    }

    suspend fun disable(module: String): ModuleChangeResult {
        if (module !in ModuleFlags.SELECTABLE) return ModuleChangeResult.InvalidModule
        val current = onboarding.currentModules()
        if (!current.isEnabled(module)) return ModuleChangeResult.Unchanged
        val updated = current.withModule(module, false)
        if (!updated.anyEnabled) return ModuleChangeResult.LastModule
        applyEnabled(updated, previous = current)
        return ModuleChangeResult.Disabled
    }

    suspend fun planModuleChanges(current: EnabledModules, target: EnabledModules): ModuleChangePlan {
        if (!target.anyEnabled) return ModuleChangePlan(blocked = true)
        val toDisable = ModuleFlags.SELECTABLE.filter { current.isEnabled(it) && !target.isEnabled(it) }
        val toEnableImmediate = ModuleFlags.SELECTABLE.filter {
            target.isEnabled(it) && !current.isEnabled(it) && isModuleInitialized(it)
        }
        val setupQueue = MODULE_SETUP_ORDER.filter { target.isEnabled(it) && !isModuleInitialized(it) }
        var projected = current
        for (module in toEnableImmediate) {
            projected = projected.withModule(module, true)
        }
        var afterDisable = projected
        for (module in toDisable) {
            afterDisable = afterDisable.withModule(module, false)
        }
        return if (!afterDisable.anyEnabled && setupQueue.isNotEmpty()) {
            ModuleChangePlan(
                toEnableImmediate = toEnableImmediate,
                setupQueue = setupQueue,
                deferredDisables = toDisable,
            )
        } else {
            ModuleChangePlan(
                toDisable = toDisable,
                toEnableImmediate = toEnableImmediate,
                setupQueue = setupQueue,
            )
        }
    }

    suspend fun applyModuleChanges(target: EnabledModules): ModuleApplyResult {
        val current = onboarding.currentModules()
        val plan = planModuleChanges(current, target)
        if (plan.blocked) return ModuleApplyResult(blocked = true)
        var state = current
        if (plan.toDisable.isNotEmpty() || plan.toEnableImmediate.isNotEmpty()) {
            for (module in plan.toDisable) {
                state = state.withModule(module, false)
            }
            for (module in plan.toEnableImmediate) {
                state = state.withModule(module, true)
            }
            applyEnabled(state, previous = current)
        }
        return ModuleApplyResult(
            setupQueue = plan.setupQueue,
            deferredDisables = plan.deferredDisables,
            added = plan.toEnableImmediate + plan.setupQueue,
        )
    }

    suspend fun applyDeferredDisables(modules: List<String>) {
        for (module in modules) {
            disable(module)
        }
    }

    suspend fun completeSetup(module: String, input: OnboardingInput): ModuleChangeResult {
        if (module !in ModuleFlags.SELECTABLE) return ModuleChangeResult.InvalidModule
        onboarding.applyModuleConfiguration(module, input)
        if (module == ModuleFlags.MODULE_WORKOUT || module == ModuleFlags.MODULE_DIET) {
            onboarding.seedWeightMetricIfNeeded(input.weightKg)
        }
        markSetupCompleted(module)
        val current = onboarding.currentModules()
        if (!current.isEnabled(module)) {
            applyEnabled(current.withModule(module, true), previous = current)
        } else {
            onboarding.writeModuleFlags(current)
        }
        return ModuleChangeResult.Enabled
    }

    private suspend fun applyEnabled(updated: EnabledModules, previous: EnabledModules) {
        val now = clock.nowEpochMs().toString()
        for (module in ModuleFlags.SELECTABLE) {
            val wasOn = previous.isEnabled(module)
            val isOn = updated.isEnabled(module)
            if (!wasOn && isOn) {
                db.configDao().upsert(UserConfigEntity(ModuleFlags.enabledAtKey(module), now))
            } else if (wasOn && !isOn) {
                db.configDao().upsert(UserConfigEntity(ModuleFlags.disabledAtKey(module), now))
            }
        }
        onboarding.writeModuleFlags(updated)
    }

    private suspend fun markSetupCompleted(module: String) {
        db.configDao().upsert(UserConfigEntity(ModuleFlags.setupCompletedKey(module), "true"))
    }
}

internal fun WorkoutRoutineEntity.hasEnabledTrainingDay(): Boolean =
    monday.enabled || tuesday.enabled || wednesday.enabled || thursday.enabled ||
        friday.enabled || saturday.enabled || sunday.enabled
