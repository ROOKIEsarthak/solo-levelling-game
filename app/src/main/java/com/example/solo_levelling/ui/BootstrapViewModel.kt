package com.example.solo_levelling.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.core.config.SystemDefaults
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BootstrapViewModel(
    private val container: AppContainer,
) : ViewModel() {
    val ready: StateFlow<Boolean> = kotlinx.coroutines.flow.flow {
        container.onboarding.ensureSeeded()
        emit(true)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val onboardingDone: StateFlow<Boolean> =
        container.db.playerDao().observeProfile(SystemDefaults.PLAYER_ID)
            .map { it?.onboardingDone == true }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        viewModelScope.launch {
            container.onboarding.ensureSeeded()
            val profile = container.db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
            if (profile?.onboardingDone == true) {
                container.questGeneration.generateForToday(profile.timezone)
            }
        }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                BootstrapViewModel(container) as T
        }
    }
}
