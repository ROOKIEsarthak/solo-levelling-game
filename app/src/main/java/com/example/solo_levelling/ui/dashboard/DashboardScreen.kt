package com.example.solo_levelling.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
    val suggestions by vm.suggestions.collectAsStateWithLifecycle()
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
                        Text("LEVEL ${p?.level ?: 1}  ·  RANK ${p?.rank ?: "E"}")
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                        Text("$xpIntoLevel / $xpNeed XP  ·  streak ${streak?.current ?: 0}")
                    }
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
                items(suggestions) { s ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(s.title, style = MaterialTheme.typography.titleSmall)
                            Text(s.detail)
                        }
                    }
                }
            }
        }
    }
}
