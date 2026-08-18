package com.example.solo_levelling.ui.analysis

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.domain.service.OnboardingInput
import com.example.solo_levelling.ui.components.BracketLabel
import com.example.solo_levelling.ui.components.CyberProgressBar
import com.example.solo_levelling.ui.components.EnergyFieldBackground
import com.example.solo_levelling.ui.components.GlassLevel
import com.example.solo_levelling.ui.components.GlassSurface
import com.example.solo_levelling.ui.components.SystemSectionHeader
import com.example.solo_levelling.ui.theme.GlowCyan
import com.example.solo_levelling.ui.theme.JetBrainsMono
import com.example.solo_levelling.ui.theme.Spacing
import com.example.solo_levelling.ui.theme.SystemPrimary

@Composable
fun SystemAnalysisScreen(
    container: AppContainer,
    input: OnboardingInput,
    onReady: () -> Unit,
) {
    val vm: SystemAnalysisViewModel = viewModel(factory = SystemAnalysisViewModel.factory(container, input))
    val phase by vm.phase.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    val finished by vm.finished.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.start() }

    var navigated by remember { mutableStateOf(false) }
    LaunchedEffect(finished) {
        if (finished && !navigated) {
            navigated = true
            onReady()
        }
    }

    BackHandler { }

    val heading = analysisHeading(phase)
    val status = analysisStatus(phase)
    val scan = rememberInfiniteTransition(label = "analysisScan")
    val scanY by scan.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "scanY",
    )
    val glowPulse by scan.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowPulse",
    )

    Box(Modifier.fillMaxSize()) {
        EnergyFieldBackground(Modifier.fillMaxSize())
        Canvas(Modifier.fillMaxSize()) {
            val y = size.height * scanY
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, GlowCyan.copy(alpha = 0.45f * glowPulse), Color.Transparent),
                ),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 3f,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GlassSurface(
                level = GlassLevel.Level2,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "$heading. $status" },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    SystemSectionHeader(tag = "SYSTEM")
                    BracketLabel(text = "INIT")
                    Text(
                        text = heading,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontFamily = JetBrainsMono,
                        color = SystemPrimary,
                    )
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    CyberProgressBar(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.xs),
                        height = 6.dp,
                    )
                }
            }
        }
    }
}
