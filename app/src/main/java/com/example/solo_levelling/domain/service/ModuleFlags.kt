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
}

object ModuleFlags {
    const val KEY_CAREER = "module_career"
    const val KEY_WORKOUT = "module_workout"
    const val KEY_DIET = "module_diet"

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
