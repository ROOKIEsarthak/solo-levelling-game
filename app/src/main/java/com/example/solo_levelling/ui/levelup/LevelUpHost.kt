package com.example.solo_levelling.ui.levelup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.solo_levelling.ui.components.GlassLevel
import com.example.solo_levelling.ui.components.GlassSurface
import com.example.solo_levelling.ui.components.SystemActionButton
import com.example.solo_levelling.ui.theme.GlowPurple
import com.example.solo_levelling.ui.theme.JetBrainsMono
import com.example.solo_levelling.ui.theme.SystemBackground
import com.example.solo_levelling.ui.theme.SystemPrimary
import com.example.solo_levelling.ui.theme.SystemPrimaryContainer
import com.example.solo_levelling.ui.theme.SystemSecondary
import com.example.solo_levelling.ui.theme.SystemTertiary
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

    val isRankUp = e is LevelUpEvent.RankUp
    val auraColor = if (isRankUp) SystemSecondary else SystemPrimaryContainer

    Box(
        Modifier
            .fillMaxSize()
            .background(SystemBackground.copy(alpha = 0.92f))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        auraColor.copy(alpha = if (isRankUp) 0.28f else 0.22f),
                        Color.Transparent,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (e) {
                is LevelUpEvent.LevelUp -> {
                    AnimatedVisibility(visible = stage >= 1, enter = fadeIn(), exit = fadeOut()) {
                        Text(
                            text = "Level up",
                            color = SystemPrimary,
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    }
                    AnimatedVisibility(visible = stage >= 2, enter = fadeIn(), exit = fadeOut()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${e.oldLevel}",
                                style = MaterialTheme.typography.displayMedium,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            )
                            Text(
                                text = "→",
                                style = MaterialTheme.typography.displaySmall,
                                fontFamily = JetBrainsMono,
                                color = SystemPrimary,
                            )
                            Box(
                                modifier = Modifier
                                    .border(1.dp, SystemPrimary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .background(SystemPrimary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    text = "${e.newLevel}",
                                    style = MaterialTheme.typography.displayLarge,
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Bold,
                                    color = SystemPrimary,
                                )
                            }
                        }
                    }
                    AnimatedVisibility(visible = stage >= 3, enter = fadeIn(), exit = fadeOut()) {
                        GlassSurface(
                            modifier = Modifier.fillMaxWidth(),
                            level = GlassLevel.Level2,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Rewards acquired",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontFamily = JetBrainsMono,
                                    color = SystemPrimary,
                                )
                                if (improvement != null) {
                                    RewardRow(
                                        label = "Improvement +${"%.1f".format(improvement)}%",
                                        accent = SystemPrimary,
                                    )
                                } else {
                                    RewardRow(
                                        label = "Baseline tracking active",
                                        accent = SystemPrimary,
                                    )
                                }
                                RewardRow(
                                    label = "Level ${e.oldLevel} → ${e.newLevel}",
                                    accent = SystemTertiary,
                                )
                            }
                        }
                    }
                    AnimatedVisibility(visible = stage >= 4, enter = fadeIn(), exit = fadeOut()) {
                        Text(
                            text = SystemMessages.forContext(
                                SystemMessages.MotivationContext.LevelMilestone,
                                e.newLevel,
                            ),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    AnimatedVisibility(visible = stage >= 5, enter = fadeIn(), exit = fadeOut()) {
                        Text(
                            text = message,
                            textAlign = TextAlign.Center,
                            color = SystemPrimary,
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                is LevelUpEvent.RankUp -> {
                    AnimatedVisibility(visible = stage >= 1, enter = fadeIn(), exit = fadeOut()) {
                        Text(
                            text = "Rank up",
                            color = SystemSecondary,
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    }
                    AnimatedVisibility(visible = stage >= 2, enter = fadeIn(), exit = fadeOut()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = e.oldRank,
                                style = MaterialTheme.typography.headlineLarge,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            )
                            Text(
                                text = "→",
                                style = MaterialTheme.typography.headlineMedium,
                                fontFamily = JetBrainsMono,
                                color = SystemSecondary,
                            )
                            Box(
                                modifier = Modifier
                                    .border(1.dp, GlowPurple.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .background(GlowPurple.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    text = e.newRank,
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Bold,
                                    color = SystemSecondary,
                                )
                            }
                        }
                    }
                    AnimatedVisibility(visible = stage >= 3, enter = fadeIn(), exit = fadeOut()) {
                        GlassSurface(
                            modifier = Modifier.fillMaxWidth(),
                            level = GlassLevel.Level2,
                            borderAlpha = 0.25f,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Rank transition",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontFamily = JetBrainsMono,
                                    color = SystemSecondary,
                                )
                                RewardRow(
                                    label = "${e.oldRank} → ${e.newRank}",
                                    accent = SystemSecondary,
                                )
                                RewardRow(
                                    label = "New tier privileges unlocked",
                                    accent = SystemTertiary,
                                )
                            }
                        }
                    }
                    AnimatedVisibility(visible = stage >= 4, enter = fadeIn(), exit = fadeOut()) {
                        Text(
                            text = SystemMessages.forContext(
                                SystemMessages.MotivationContext.RankMilestone,
                                e.newRank.hashCode(),
                            ),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            AnimatedVisibility(visible = stage >= 6, enter = fadeIn()) {
                Spacer(Modifier.height(8.dp))
                SystemActionButton(
                    label = "Continue",
                    onClick = { vm.dismiss() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun RewardRow(label: String, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(accent.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .border(1.dp, accent.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = JetBrainsMono,
            color = accent,
        )
    }
}
