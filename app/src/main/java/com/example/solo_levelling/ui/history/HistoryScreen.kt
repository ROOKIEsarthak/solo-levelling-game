package com.example.solo_levelling.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.ui.components.BracketLabel
import com.example.solo_levelling.ui.components.EnergyFieldBackground
import com.example.solo_levelling.ui.components.GhostTextButton
import com.example.solo_levelling.ui.components.GlassLevel
import com.example.solo_levelling.ui.components.GlassSurface
import com.example.solo_levelling.ui.components.SystemIdleEmpty
import com.example.solo_levelling.ui.components.SystemSectionHeader
import com.example.solo_levelling.ui.theme.JetBrainsMono
import com.example.solo_levelling.ui.theme.Spacing
import com.example.solo_levelling.ui.theme.SystemPrimary
import com.example.solo_levelling.ui.theme.SystemSecondary
import com.example.solo_levelling.ui.theme.SystemSurface
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(
    container: AppContainer,
    onOpenWorkout: () -> Unit,
    onOpenDiet: () -> Unit,
    showWorkout: Boolean = true,
    showDiet: Boolean = true,
) {
    val vm: HistoryViewModel = viewModel(factory = HistoryViewModel.factory(container))
    val recentXp by vm.recentXp.collectAsStateWithLifecycle()
    val recentWorkouts by vm.recentWorkouts.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val dateFmt = DateTimeFormatter.ofPattern("MMM d, HH:mm")

    Box(Modifier.fillMaxSize()) {
        EnergyFieldBackground(Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.section),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                SystemSectionHeader(tag = "ACTIVITY LOG")
                Text(
                    when {
                        showWorkout -> "Recent XP, workouts, and quick links"
                        else -> "Recent XP and activity"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }

        SystemSectionHeader(tag = "RECENT XP")

        if (recentXp.isEmpty()) {
            SystemIdleEmpty(
                title = "No XP entries",
                subtitle = "Complete quests and missions to populate the system log.",
            )
        } else {
            recentXp.forEach { entry ->
                val whenStr = Instant.ofEpochMilli(entry.createdAtEpochMs)
                    .atZone(ZoneId.systemDefault())
                    .format(dateFmt)
                AccentLogCard(
                    accent = SystemPrimary,
                    borderAccent = SystemPrimary.copy(alpha = 0.3f),
                ) {
                    LogEntryRow(
                        title = entry.sourceType.replace('_', ' '),
                        subtitle = whenStr,
                        xpLabel = "+${entry.amount} XP",
                        xpColor = SystemPrimary,
                    )
                }
            }
        }

        if (showWorkout || showDiet) {
            GlassSurface(modifier = Modifier.fillMaxWidth(), level = GlassLevel.Level1) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SystemSectionHeader(tag = "QUICK LINKS")
                    if (showWorkout) {
                        GhostTextButton(label = "OPEN WORKOUT HISTORY", onClick = onOpenWorkout)
                    }
                    if (showDiet) {
                        GhostTextButton(label = "OPEN DIET HISTORY", onClick = onOpenDiet)
                    }
                }
            }
        }

        if (showWorkout) {
            SystemSectionHeader(tag = "RECENT WORKOUTS")

            if (recentWorkouts.isEmpty()) {
                SystemIdleEmpty(
                    title = "No workouts logged",
                    subtitle = "Training sessions will appear here once recorded.",
                    actionLabel = "OPEN WORKOUT",
                    onAction = onOpenWorkout,
                )
            } else {
                recentWorkouts.forEach { log ->
                    AccentLogCard(
                        accent = SystemSecondary,
                        borderAccent = SystemSecondary.copy(alpha = 0.35f),
                    ) {
                        LogEntryRow(
                            title = log.workoutName.ifBlank { "Workout" },
                            subtitle = "${log.date} · ${log.durationMinutes} min",
                            xpLabel = "SESSION",
                            xpColor = SystemSecondary,
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun AccentLogCard(
    accent: Color,
    borderAccent: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SystemSurface.copy(alpha = 0.4f))
            .border(1.dp, borderAccent, shape),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(3.dp)
                .heightIn(min = 72.dp)
                .fillMaxHeight()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(accent, accent.copy(alpha = 0f)),
                    ),
                ),
        )
        Box(Modifier.padding(start = 12.dp, top = 14.dp, end = 16.dp, bottom = 14.dp)) {
            content()
        }
    }
}

@Composable
private fun LogEntryRow(
    title: String,
    subtitle: String,
    xpLabel: String,
    xpColor: Color,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = colors.onSurface,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = JetBrainsMono,
                color = colors.onSurfaceVariant,
            )
        }
        BracketLabel(text = xpLabel, color = xpColor)
    }
}
