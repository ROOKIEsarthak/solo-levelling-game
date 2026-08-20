package com.example.solo_levelling.domain.service

import com.example.solo_levelling.data.db.dao.ConfigDao
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class EnabledModules(
    val career: Boolean = false,
    val workout: Boolean = false,
    val diet: Boolean = false,
) {
    val anyEnabled: Boolean get() = career || workout || diet

    fun withCareer(enabled: Boolean) = copy(career = enabled)
    fun withWorkout(enabled: Boolean) = copy(workout = enabled)
    fun withDiet(enabled: Boolean) = copy(diet = enabled)

    fun isEnabled(module: String): Boolean = when (module) {
        ModuleFlags.MODULE_CAREER -> career
        ModuleFlags.MODULE_WORKOUT -> workout
        ModuleFlags.MODULE_DIET -> diet
        else -> false
    }

    fun withModule(module: String, enabled: Boolean): EnabledModules = when (module) {
        ModuleFlags.MODULE_CAREER -> withCareer(enabled)
        ModuleFlags.MODULE_WORKOUT -> withWorkout(enabled)
        ModuleFlags.MODULE_DIET -> withDiet(enabled)
        else -> this
    }
}

object ModuleFlags {
    const val MODULE_CAREER = "career"
    const val MODULE_WORKOUT = "workout"
    const val MODULE_DIET = "diet"

    const val KEY_CAREER = "module_career"
    const val KEY_WORKOUT = "module_workout"
    const val KEY_DIET = "module_diet"

    val SELECTABLE = listOf(MODULE_CAREER, MODULE_WORKOUT, MODULE_DIET)

    fun flagKey(module: String): String? = when (module) {
        MODULE_CAREER -> KEY_CAREER
        MODULE_WORKOUT -> KEY_WORKOUT
        MODULE_DIET -> KEY_DIET
        else -> null
    }

    fun setupCompletedKey(module: String): String = "module_${module}_setup_completed"
    fun enabledAtKey(module: String): String = "module_${module}_enabled_at_epoch_ms"
    fun disabledAtKey(module: String): String = "module_${module}_disabled_at_epoch_ms"

    fun displayName(module: String): String = when (module) {
        MODULE_CAREER -> "Career"
        MODULE_WORKOUT -> "Fitness"
        MODULE_DIET -> "Nutrition"
        else -> module
    }

    fun parse(
        career: String?,
        workout: String?,
        diet: String?,
    ): EnabledModules = EnabledModules(
        career = isTrue(career),
        workout = isTrue(workout),
        diet = isTrue(diet),
    )

    fun isTrue(value: String?): Boolean =
        value.equals("true", ignoreCase = true) || value == "1"

    fun needsMigration(career: String?, workout: String?, diet: String?): Boolean =
        career == null || workout == null || diet == null

    /**
     * Existing onboarded users without module keys → all modules on.
     * New users / incomplete → treat missing as false until onboarding writes flags.
     */
    fun resolve(
        onboardingDone: Boolean,
        career: String?,
        workout: String?,
        diet: String?,
    ): EnabledModules {
        if (needsMigration(career, workout, diet)) {
            return if (onboardingDone) {
                EnabledModules(career = true, workout = true, diet = true)
            } else {
                EnabledModules()
            }
        }
        return parse(career, workout, diet)
    }

    fun encode(modules: EnabledModules): Map<String, String> = mapOf(
        KEY_CAREER to modules.career.toString(),
        KEY_WORKOUT to modules.workout.toString(),
        KEY_DIET to modules.diet.toString(),
    )

    fun observeEnabledModules(
        profile: Flow<PlayerProfileEntity?>,
        configDao: ConfigDao,
    ): Flow<EnabledModules> = combine(
        profile,
        configDao.observe(KEY_CAREER),
        configDao.observe(KEY_WORKOUT),
        configDao.observe(KEY_DIET),
    ) { p, career, workout, diet ->
        resolve(
            onboardingDone = p?.onboardingDone == true,
            career = career?.value,
            workout = workout?.value,
            diet = diet?.value,
        )
    }
}
