package com.example.solo_levelling.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.domain.service.OnboardingInput
import com.example.solo_levelling.domain.service.OnboardingService
import com.example.solo_levelling.ui.navigation.AppRoute
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal const val AnalysisMinMs = 5_000L
internal const val AnalysisTickMs = 100L

internal enum class AnalysisPhase {
    Loading,
    Ready,
}

internal fun analysisPhaseAt(elapsedMs: Long, minMs: Long = AnalysisMinMs): AnalysisPhase =
    if (elapsedMs < minMs) AnalysisPhase.Loading else AnalysisPhase.Ready

internal fun analysisHeading(phase: AnalysisPhase): String = when (phase) {
    AnalysisPhase.Loading -> "SYSTEM INITIALIZING"
    AnalysisPhase.Ready -> "SYSTEM READY"
}

internal fun analysisStatus(phase: AnalysisPhase): String = when (phase) {
    AnalysisPhase.Loading -> "Preparing your path..."
    AnalysisPhase.Ready -> "Your path is ready."
}

internal fun analysisProgress(elapsedMs: Long, minMs: Long = AnalysisMinMs): Float =
    (elapsedMs.toFloat() / minMs.toFloat()).coerceIn(0f, 1f)

internal fun canLeaveAnalysis(
    elapsedMs: Long,
    workDone: Boolean,
    minMs: Long = AnalysisMinMs,
): Boolean = workDone && elapsedMs >= minMs

internal fun analysisRedirectRoute(hasInput: Boolean, onboardingDone: Boolean): String? = when {
    hasInput -> null
    onboardingDone -> AppRoute.Dashboard.route
    else -> AppRoute.SystemConsent.route
}

class SystemAnalysisViewModel(
    private val onboarding: OnboardingService,
    private val input: OnboardingInput,
    private val minMs: Long = AnalysisMinMs,
) : ViewModel() {
    private val _phase = MutableStateFlow(AnalysisPhase.Loading)
    internal val phase: StateFlow<AnalysisPhase> = _phase.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _finished = MutableStateFlow(false)
    val finished: StateFlow<Boolean> = _finished.asStateFlow()

    private var started = false

    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            val work = async { onboarding.completeOnboarding(input) }
            var elapsed = 0L
            while (true) {
                _phase.value = analysisPhaseAt(elapsed, minMs)
                _progress.value = analysisProgress(elapsed, minMs)
                if (canLeaveAnalysis(elapsed, work.isCompleted, minMs)) break
                delay(AnalysisTickMs)
                elapsed += AnalysisTickMs
            }
            work.await()
            _phase.value = AnalysisPhase.Ready
            _progress.value = 1f
            _finished.value = true
        }
    }

    companion object {
        fun factory(container: AppContainer, input: OnboardingInput) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SystemAnalysisViewModel(container.onboarding, input) as T
            }
    }
}
