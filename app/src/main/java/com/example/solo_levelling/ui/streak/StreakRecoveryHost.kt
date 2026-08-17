package com.example.solo_levelling.ui.streak

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.example.solo_levelling.ui.components.CyberProgressBar
import com.example.solo_levelling.ui.components.GlassLevel
import com.example.solo_levelling.ui.components.GlassSurface
import com.example.solo_levelling.ui.components.SystemActionButton
import com.example.solo_levelling.ui.components.SystemSectionHeader
import com.example.solo_levelling.ui.theme.JetBrainsMono
import com.example.solo_levelling.ui.theme.SystemBackground
import com.example.solo_levelling.ui.theme.SystemPrimary
import com.example.solo_levelling.ui.theme.SystemSecondary

private enum class RecoveryPhase { Reflect, Continue }

@Composable
fun StreakRecoveryHost(
    container: AppContainer,
    onBeginAgain: () -> Unit,
) {
    val vm: StreakRecoveryViewModel = viewModel(factory = StreakRecoveryViewModel.factory(container))
    val pending by vm.pending.collectAsStateWithLifecycle()
    val event = pending ?: return

    var phase by remember(event.previousStreak) { mutableIntStateOf(RecoveryPhase.Reflect.ordinal) }
    var answer by remember(event.previousStreak) { mutableStateOf("") }
    val colors = MaterialTheme.colorScheme
    val currentPhase = RecoveryPhase.entries[phase]

    Box(
        Modifier
            .fillMaxSize()
            .background(SystemBackground.copy(alpha = 0.92f))
            .background(
                Brush.radialGradient(
                    colors = listOf(SystemSecondary.copy(alpha = 0.18f), Color.Transparent),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            level = GlassLevel.Level2,
            borderAlpha = 0.25f,
            cornerRadius = 16.dp,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SystemSectionHeader(
                        tag = when (currentPhase) {
                            RecoveryPhase.Reflect -> "Reflect"
                            RecoveryPhase.Continue -> "Continue"
                        },
                        accent = SystemSecondary,
                    )
                    Text(
                        text = "Phase ${phase + 1}/2",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMono,
                        color = colors.onSurfaceVariant,
                        letterSpacing = 1.sp,
                    )
                }
                CyberProgressBar(progress = (phase + 1) / 2f)

                when (currentPhase) {
                    RecoveryPhase.Reflect -> {
                        Text(
                            text = "The path is not always linear.",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = SystemMessages.forContext(
                                SystemMessages.MotivationContext.Recovery,
                                event.previousStreak,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = "\"${SystemMessages.FALL_QUESTION}\"",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        OutlinedTextField(
                            value = answer,
                            onValueChange = { answer = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text("Your answer...", color = colors.onSurfaceVariant.copy(alpha = 0.6f))
                            },
                            minLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SystemSecondary,
                                unfocusedBorderColor = SystemSecondary.copy(alpha = 0.4f),
                                cursorColor = SystemSecondary,
                            ),
                        )
                        SystemActionButton(
                            label = "Continue",
                            onClick = { phase = RecoveryPhase.Continue.ordinal },
                            enabled = answer.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    RecoveryPhase.Continue -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            DiagnosticStat(
                                label = "Previous streak",
                                value = "${event.previousStreak}d",
                                modifier = Modifier.weight(1f),
                            )
                            DiagnosticStat(
                                label = "Previous best",
                                value = "${event.best}d",
                                modifier = Modifier.weight(1f),
                            )
                        }
                        GlassSurface(level = GlassLevel.Level1) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Current · 0 days",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMono,
                                    color = colors.onSurfaceVariant,
                                )
                                Text(
                                    text = "Memory core intact. Progress preserved.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                val retention = if (event.best > 0) {
                                    (event.previousStreak.toFloat() / event.best).coerceIn(0f, 1f)
                                } else {
                                    1f
                                }
                                CyberProgressBar(progress = retention)
                            }
                        }
                        Text(
                            text = "\"${SystemMessages.FALL_ANSWER}\"",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = SystemMessages.FALL_ATTRIBUTION,
                            color = colors.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = SystemMessages.forContext(
                                SystemMessages.MotivationContext.StreakBroken,
                                event.previousStreak,
                            ),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                        )
                        Text(
                            text = "Today's mission — complete one objective. Start rebuilding.",
                            textAlign = TextAlign.Center,
                            color = colors.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        SystemActionButton(
                            label = "Begin again",
                            onClick = {
                                vm.dismiss()
                                onBeginAgain()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    GlassSurface(
        modifier = modifier,
        level = GlassLevel.Level1,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = JetBrainsMono,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                color = SystemPrimary,
            )
        }
    }
}
