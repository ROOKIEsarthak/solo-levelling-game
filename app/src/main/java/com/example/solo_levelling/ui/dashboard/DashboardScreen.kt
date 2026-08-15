package com.example.solo_levelling.ui.dashboard

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.domain.model.QuestStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    container: AppContainer,
    onOpenAchievements: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val vm: DashboardViewModel = viewModel(factory = DashboardViewModel.factory(container))
    val profile by vm.profile.collectAsStateWithLifecycle()
    val quests by vm.todayQuests.collectAsStateWithLifecycle()
    val streak by vm.streak.collectAsStateWithLifecycle()
    val attributes by vm.attributes.collectAsStateWithLifecycle()
    val activeBoss by vm.activeBoss.collectAsStateWithLifecycle()
    val weeklyPct by vm.weeklyCompletionPct.collectAsStateWithLifecycle()
    val recentAchievements by vm.recentAchievements.collectAsStateWithLifecycle()
    val xpLast7Days by vm.xpLast7Days.collectAsStateWithLifecycle()
    val activeSeason by vm.activeSeason.collectAsStateWithLifecycle()
    val suggestions by vm.suggestions.collectAsStateWithLifecycle()
    val goalTitle by vm.goalTitle.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val p = profile
    val xpIntoLevel = if (p == null) 0 else {
        p.totalXp - SystemDefaults.totalXpForLevel(p.level)
    }
    val xpNeed = if (p == null) 1 else SystemDefaults.xpForNextLevel(p.level)
    val progress = (xpIntoLevel.toFloat() / xpNeed.toFloat()).coerceIn(0f, 1f)

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("SYSTEM") },
            actions = {
                IconButton(onClick = onOpenAchievements) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = "Achievements")
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            },
        )
        LazyColumn(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(p?.name ?: "Hunter", style = MaterialTheme.typography.titleLarge)
                        if (!goalTitle.isNullOrBlank()) {
                            Text(goalTitle!!, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text("LEVEL ${p?.level ?: 1}  ·  RANK ${p?.rank ?: "E"}")
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                        Text("$xpIntoLevel / $xpNeed XP  ·  streak ${streak?.current ?: 0}")
                        activeSeason?.let { s ->
                            Text("${s.name} · ${s.seasonXp} season XP")
                        }
                    }
                }
            }
            if (attributes.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        attributes.forEach { attr ->
                            AssistChip(
                                onClick = {},
                                label = { Text("${attr.code} ${attr.currentValue}") },
                            )
                        }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("This week", style = MaterialTheme.typography.titleSmall)
                        LinearProgressIndicator(
                            progress = { weeklyPct },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text("${(weeklyPct * 100).toInt()}% quests · 7d XP: $xpLast7Days")
                    }
                }
            }
            activeBoss?.let { boss ->
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Active Boss", style = MaterialTheme.typography.titleSmall)
                            Text(boss.title)
                            val bossProgress = (boss.currentValue / boss.targetValue.coerceAtLeast(1f))
                                .coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { bossProgress },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text("${boss.currentValue.toInt()} / ${boss.targetValue.toInt()}")
                        }
                    }
                }
            }
            if (recentAchievements.isNotEmpty()) {
                item { Text("Recent Achievements", style = MaterialTheme.typography.titleMedium) }
                items(recentAchievements, key = { it.achievementKey }) { ach ->
                    Card(Modifier.fillMaxWidth()) {
                        Text(ach.achievementKey, Modifier.padding(12.dp))
                    }
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { scope.launch { container.modules.logWorkout("Quick", 30) } },
                        modifier = Modifier.weight(1f),
                    ) { Text("Workout") }
                    OutlinedButton(
                        onClick = { scope.launch { container.metricIngest.ingest("STEPS", 5000f) } },
                        modifier = Modifier.weight(1f),
                    ) { Text("Steps") }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                container.modules.saveJournal("Quick reflection from dashboard.")
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("Journal") }
                }
            }
            item { Text("Today's Quests", style = MaterialTheme.typography.titleMedium) }
            items(quests, key = { it.id }) { q ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(q.title)
                            Text("+${q.baseXp} XP · ${q.status}")
                        }
                        if (q.status != QuestStatus.COMPLETED.name) {
                            Button(onClick = { scope.launch { container.questCompletion.complete(q.id) } }) {
                                Text("Clear")
                            }
                        } else {
                            Text("Cleared")
                        }
                    }
                }
            }
            if (suggestions.isNotEmpty()) {
                item { Text("System Suggestions", style = MaterialTheme.typography.titleMedium) }
                items(suggestions, key = { it.key }) { s ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(s.title, style = MaterialTheme.typography.titleSmall)
                            Text(s.detail)
                            TextButton(onClick = { vm.dismissSuggestion(s.key) }) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }
        }
    }
}
