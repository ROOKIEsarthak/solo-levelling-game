package com.example.solo_levelling.ui.levelup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.domain.copy.SystemMessages
import com.example.solo_levelling.ui.theme.GlowCyan
import com.example.solo_levelling.ui.theme.SystemBackground
import kotlinx.coroutines.delay

@Composable
fun LevelUpHost(container: AppContainer) {
    val vm: LevelUpViewModel = viewModel(factory = LevelUpViewModel.factory(container))
    val event by vm.pendingEvent.collectAsStateWithLifecycle()
    val improvement by vm.improvementPercent.collectAsStateWithLifecycle()
    val message by vm.motivationalMessage.collectAsStateWithLifecycle()

    val e = event ?: return
    var stage by remember(e) { mutableIntStateOf(0) }

    LaunchedEffect(e) {
        stage = 0
        delay(200)
        stage = 1
        delay(400)
        stage = 2
        delay(500)
        stage = 3
        delay(500)
        stage = 4
        delay(400)
        stage = 5
        delay(400)
        stage = 6
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(SystemBackground.copy(alpha = 0.88f))
            .background(
                Brush.radialGradient(
                    colors = listOf(GlowCyan.copy(alpha = 0.22f), Color.Transparent),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (e) {
                is LevelUpEvent.LevelUp -> {
                    AnimatedVisibility(visible = stage >= 1, enter = fadeIn(), exit = fadeOut()) {
                        Text(
                            "LEVEL UP",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 6.sp,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    AnimatedVisibility(visible = stage >= 2, enter = fadeIn(), exit = fadeOut()) {
                        Text(
                            "${e.newLevel}",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = GlowCyan,
                        )
                    }
                    AnimatedVisibility(visible = stage >= 3, enter = fadeIn(), exit = fadeOut()) {
                        Text(
                            if (improvement != null) {
                                "YOU ARE ${"%.1f".format(improvement)}%\nBETTER THAN BEFORE."
                            } else {
                                "BUILDING YOUR BASELINE\n\nKeep showing up.\nYour improvement score\nwill appear once enough data\nhas been collected."
                            },
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    AnimatedVisibility(visible = stage >= 4, enter = fadeIn(), exit = fadeOut()) {
                        Text(
                            message,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    AnimatedVisibility(visible = stage >= 5, enter = fadeIn(), exit = fadeOut()) {
                        Text(
                            "LEVEL ${e.oldLevel} → ${e.newLevel}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                is LevelUpEvent.RankUp -> {
                    Text(
                        "RANK UP",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                    )
                    Text(
                        "${e.oldRank} → ${e.newRank}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        SystemMessages.pick(SystemMessages.Category.Consistency, e.newRank.hashCode()),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
            AnimatedVisibility(visible = stage >= 6 || e is LevelUpEvent.RankUp, enter = fadeIn()) {
                Button(
                    onClick = { vm.dismiss() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    Text("CONTINUE")
                }
            }
        }
    }
}
