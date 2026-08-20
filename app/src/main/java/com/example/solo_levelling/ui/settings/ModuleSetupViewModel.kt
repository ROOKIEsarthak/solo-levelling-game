package com.example.solo_levelling.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.domain.service.ModuleChangeResult
import com.example.solo_levelling.domain.service.ModuleLifecycleService
import com.example.solo_levelling.domain.service.OnboardingInput

data class ModuleSetupInitialState(
    val age: String = "",
    val sex: String = "male",
    val heightCm: String = "",
    val weightKg: String = "",
    val fitnessGoal: String = "maintenance",
    val trainingExperience: String = "beginner",
    val activityLevel: String = "moderate",
    val careerIntent: String = "",
    val currentRole: String = "",
    val targetRole: String = "SDE1",
    val yearsExperience: String = "1",
    val techStack: String = "",
    val experienceBand: String = "1-2",
    val requireBody: Boolean = true,
)

class ModuleSetupViewModel(
    private val db: JsonDatabase,
    private val moduleLifecycle: ModuleLifecycleService,
) : ViewModel() {
    constructor(container: AppContainer) : this(container.db, container.moduleLifecycle)

    suspend fun loadInitialState(): ModuleSetupInitialState {
        val config = db.configDao()
        val heightCm = config.get("height_cm")?.value.orEmpty()
        val weightKg = config.get("weight_kg")?.value.orEmpty()
        return ModuleSetupInitialState(
            age = config.get("age")?.value.orEmpty(),
            sex = config.get("sex")?.value?.ifBlank { "male" } ?: "male",
            heightCm = heightCm,
            weightKg = weightKg,
            fitnessGoal = config.get("fitness_goal")?.value?.ifBlank { "maintenance" } ?: "maintenance",
            trainingExperience = config.get("training_experience")?.value?.ifBlank { "beginner" } ?: "beginner",
            activityLevel = config.get("activity_level")?.value?.ifBlank { "moderate" } ?: "moderate",
            careerIntent = config.get("career_intent")?.value.orEmpty(),
            currentRole = config.get("career_current_role")?.value.orEmpty(),
            targetRole = config.get("career_target_role")?.value?.ifBlank { "SDE1" } ?: "SDE1",
            yearsExperience = config.get("career_years_experience")?.value?.ifBlank { "1" } ?: "1",
            techStack = config.get("career_tech_stack")?.value.orEmpty(),
            experienceBand = config.get("career_experience_band")?.value?.ifBlank { "1-2" } ?: "1-2",
            requireBody = heightCm.toDoubleOrNull()?.let { it > 0 } != true ||
                weightKg.toDoubleOrNull()?.let { it > 0 } != true,
        )
    }

    suspend fun completeSetup(moduleId: String, input: OnboardingInput): ModuleChangeResult =
        moduleLifecycle.completeSetup(moduleId, input)

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ModuleSetupViewModel(container) as T
        }
    }
}
