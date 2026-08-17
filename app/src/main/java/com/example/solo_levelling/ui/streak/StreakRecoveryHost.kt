package com.example.solo_levelling.ui.streak

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.solo_levelling.ui.theme.GlowCyan
import com.example.solo_levelling.ui.theme.SystemBackground

@Composable
fun StreakRecoveryHost(
    container: AppContainer,
    onBeginAgain: () -> Unit,
) {
    val vm: StreakRecoveryViewModel = viewModel(factory = StreakRecoveryViewModel.factory(container))
    val pending by vm.pending.collectAsStateWithLifecycle()
    val event = pending ?: return

    var step by remember(event.previousStreak) { mutableStateOf(0) }
    var answer by remember(event.previousStreak) { mutableStateOf("") }
    val colors = MaterialTheme.colorScheme

    Box(
        Modifier
            .fillMaxSize()
            .background(SystemBackground.copy(alpha = 0.92f))
            .background(
                Brush.radialGradient(
                    colors = listOf(GlowCyan.copy(alpha = 0.12f), Color.Transparent),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (step) {
                0 -> {
                    Text(
                        "STREAK LOST",
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        "${event.previousStreak} DAYS",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.onBackground,
                    )
                    Text(
                        "CURRENT  0 DAYS",
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "PREVIOUS BEST  ${event.best} DAYS",
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(onClick = { step = 1 }, modifier = Modifier.fillMaxWidth()) {
                        Text("CONTINUE")
                    }
                }
                1 -> {
                    Text(
                        "\"${SystemMessages.FALL_QUESTION}\"",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    OutlinedTextField(
                        value = answer,
                        onValueChange = { answer = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Your answer...") },
                        minLines = 2,
                    )
                    Button(
                        onClick = { step = 2 },
                        enabled = answer.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("ANSWER")
                    }
                }
                2 -> {
                    Text(
                        "\"${SystemMessages.FALL_ANSWER}\"",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        SystemMessages.FALL_ATTRIBUTION,
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        SystemMessages.pick(SystemMessages.Category.Recovery, event.previousStreak),
                        textAlign = TextAlign.Center,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        "THE STREAK ENDED.\nYOUR PROGRESS DIDN'T.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "TODAY'S MISSION\nComplete one objective.\nStart rebuilding.",
                        textAlign = TextAlign.Center,
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(
                        onClick = {
                            vm.dismiss()
                            onBeginAgain()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("BEGIN AGAIN")
                    }
                }
            }
        }
    }
}
