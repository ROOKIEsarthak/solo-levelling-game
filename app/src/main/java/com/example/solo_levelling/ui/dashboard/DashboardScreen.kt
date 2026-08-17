package com.example.solo_levelling.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.domain.copy.SystemMessages
import com.example.solo_levelling.domain.model.QuestStatus
import com.example.solo_levelling.domain.service.QuestCompletionService
import com.example.solo_levelling.ui.theme.GlowCyan
import com.example.solo_levelling.ui.theme.SystemSuccess
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    container: AppContainer,
    onOpenAchievements: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWorkout: () -> Unit = {},
    onOpenNutrition: () -> Unit = {},
    onOpenMissions: () -> Unit = {},
    onOpenCareer: () -> Unit = {},
    onMessage: (String) -> Unit = {},
) {
    val vm: DashboardViewModel = viewModel(factory = DashboardViewModel.factory(container))
    val profile by vm.profile.collectAsStateWithLifecycle()
    val quests by vm.todayQuests.collectAsStateWithLifecycle()
    val streak by vm.streak.collectAsStateWithLifecycle()
    val workoutToday by vm.workoutToday.collectAsStateWithLifecycle()
    val nutritionToday by vm.nutritionToday.collectAsStateWithLifecycle()
    val nextUnlock by vm.nextUnlock.collectAsStateWithLifecycle()
    val calorieTarget by vm.calorieTarget.collectAsStateWithLifecycle()
    val proteinTarget by vm.proteinTarget.collectAsStateWithLifecycle()
    val nextAction by vm.nextAction.collectAsStateWithLifecycle()
    val progressSnapshot by vm.progressSnapshot.collectAsStateWithLifecycle()
    val suggestions by vm.suggestions.collectAsStateWithLifecycle()
    val modules by vm.enabledModules.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme

    val p = profile
    val xpIntoLevel = if (p == null) 0 else p.totalXp - SystemDefaults.totalXpForLevel(p.level)
    val xpNeed = if (p == null) 1 else SystemDefaults.xpForNextLevel(p.level)
    val progress = (xpIntoLevel.toFloat() / xpNeed.toFloat()).coerceIn(0f, 1f)
    val completed = quests.count { it.status == QuestStatus.COMPLETED.name }
    val total = quests.size
    val dailyPct = if (total == 0) 0f else completed.toFloat() / total.toFloat()

    val name = (p?.name ?: "Hunter").uppercase(Locale.getDefault())
    val greeting = greetingForHour(
        java.time.LocalTime.now().hour,
    )
    val dateLabel = LocalDate.now().format(
        DateTimeFormatter.ofPattern("EEEE · MMMM d", Locale.getDefault()),
    )

    Column(Modifier.fillMaxSize().background(colors.background)) {
        TopAppBar(
            title = {
                Text("SYSTEM", fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
            },
            actions = {
                IconButton(onClick = onOpenAchievements) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = "Achievements")
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = colors.background,
                titleContentColor = colors.onBackground,
            ),
        )
        LazyColumn(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "$greeting, $name",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(dateLabel, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
            nextAction?.let { action ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
                        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.5f)),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("TODAY'S PRIORITY", fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            Text(action.title, fontWeight = FontWeight.Bold, color = colors.primary)
                            Text(
                                action.detail,
                                color = colors.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Button(
                                onClick = {
                                    when {
                                        action.routeHint.startsWith("career") -> onOpenCareer()
                                        action.routeHint == "fitness" -> onOpenWorkout()
                                        action.routeHint == "nutrition" -> onOpenNutrition()
                                        action.routeHint == "quests" -> onOpenMissions()
                                        else -> onOpenCareer()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                            ) { Text("START") }
                        }
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
                    border = BorderStroke(1.dp, colors.outline),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("YOUR PROGRESS", fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        if (modules.career) {
                            Text("DSA  ${progressSnapshot.dsaPct}%")
                            Text("System Design  ${progressSnapshot.sdPct}%")
                        }
                        if (modules.workout) {
                            Text(
                                "Workout today  ${if (progressSnapshot.workoutDoneToday) "Yes" else "No"}",
                            )
                        }
                        if (modules.diet) {
                            Text("Calories adherence  ${progressSnapshot.dietAdherencePct}%")
                        }
                        Text(
                            "Streak  ${progressSnapshot.streakCurrent} days (best ${progressSnapshot.streakBest})",
                            color = GlowCyan,
                        )
                    }
                }
            }
            if (suggestions.isNotEmpty()) {
                item {
                    Text("SUGGESTIONS", fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                }
                items(suggestions, key = { it.key }) { suggestion ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
                        border = BorderStroke(1.dp, colors.outline),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(suggestion.title, fontWeight = FontWeight.Bold)
                                Text(
                                    suggestion.detail,
                                    color = colors.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            OutlinedButton(onClick = { vm.dismissSuggestion(suggestion.key) }) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "LEVEL ${p?.level ?: 1}",
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(10.dp),
                        color = GlowCyan,
                        trackColor = colors.surfaceContainerHighest,
                    )
                    Text(
                        "$xpIntoLevel / $xpNeed XP",
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
                    border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.35f)),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("DAILY PROGRESS", fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text(
                            "${(dailyPct * 100).toInt()}%",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = SystemSuccess,
                        )
                        LinearProgressIndicator(
                            progress = { dailyPct },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = SystemSuccess,
                            trackColor = colors.surfaceContainerHighest,
                        )
                        Text(
                            "$completed / $total objectives completed",
                            color = colors.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            item {
                Text("TODAY'S MISSIONS", fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
            if (quests.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
                        border = BorderStroke(1.dp, colors.outline),
                    ) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("NO MISSIONS TODAY.")
                            Text(
                                "Create your first objective.",
                                color = colors.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Button(onClick = onOpenMissions, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                                Text("ADD TASK")
                            }
                        }
                    }
                }
            }
            items(quests, key = { it.id }) { q ->
                val done = q.status == QuestStatus.COMPLETED.name
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceContainer, RoundedCornerShape(10.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${if (done) "✓" else "○"}  ${q.title}",
                        color = if (done) SystemSuccess else colors.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (!done) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val result = withContext(Dispatchers.IO) {
                                        container.questCompletion.complete(q.id)
                                    }
                                    when (result) {
                                        is QuestCompletionService.Result.Completed ->
                                            onMessage(SystemMessages.missionComplete(result.xp))
                                        QuestCompletionService.Result.AlreadyCompleted ->
                                            onMessage("Quest already completed")
                                        QuestCompletionService.Result.NotFound ->
                                            onMessage("SYSTEM ERROR\nWe couldn't update that mission.")
                                        QuestCompletionService.Result.InvalidStatus ->
                                            onMessage("Quest can't be completed right now")
                                        QuestCompletionService.Result.DailyCapReached ->
                                            onMessage("Daily XP cap reached")
                                    }
                                }
                            },
                        ) { Text("Clear") }
                    }
                }
            }
            if (modules.workout) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
                        border = BorderStroke(1.dp, colors.outline),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("TODAY'S WORKOUT", fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            if (workoutToday == null) {
                                Text(
                                    "YOUR SYSTEM IS READY.\nNo workout has been logged yet.",
                                    color = colors.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            } else {
                                Text(workoutToday!!.workoutName.ifBlank { "Session" }, fontWeight = FontWeight.Bold)
                                Text(
                                    "${workoutToday!!.exercises.size} exercises · " +
                                        "${workoutToday!!.exercises.sumOf { it.sets.size }} sets",
                                    color = colors.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Button(onClick = onOpenWorkout, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                                Text("START WORKOUT")
                            }
                        }
                    }
                }
            }
            if (modules.diet) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
                        border = BorderStroke(1.dp, colors.outline),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("TODAY'S NUTRITION", fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            val cal = nutritionToday?.calories ?: 0
                            Text("$cal / $calorieTarget kcal", fontWeight = FontWeight.Bold)
                            LinearProgressIndicator(
                                progress = { (cal.toFloat() / calorieTarget.toFloat()).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = colors.primary,
                                trackColor = colors.surfaceContainerHighest,
                            )
                            Text(
                                "Protein  ${nutritionToday?.protein ?: 0} / $proteinTarget g",
                                color = colors.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            OutlinedButton(onClick = onOpenNutrition, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                                Text("LOG MEAL")
                            }
                        }
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("CURRENT STREAK", fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text(
                        "${streak?.current ?: 0} DAYS",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = GlowCyan,
                    )
                    Text(
                        "Best ${streak?.best ?: 0}",
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
                    border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.4f)),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("NEXT UNLOCK", fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text(
                            nextUnlock?.title ?: "LEVEL ${(p?.level ?: 1) + 1}",
                            color = colors.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = colors.primary,
                            trackColor = colors.surfaceContainerHighest,
                        )
                        Text(
                            nextUnlock?.detail ?: "???\nSomething is waiting.",
                            color = colors.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

fun greetingForHour(hour: Int): String = when (hour) {
    in 5..11 -> "GOOD MORNING"
    in 12..16 -> "GOOD AFTERNOON"
    in 17..21 -> "GOOD EVENING"
    else -> "GOOD NIGHT"
}
