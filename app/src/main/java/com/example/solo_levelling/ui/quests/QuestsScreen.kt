package com.example.solo_levelling.ui.quests

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.domain.model.QuestStatus
import kotlinx.coroutines.launch

@Composable
fun QuestsScreen(container: AppContainer) {
    val vm: QuestsViewModel = viewModel(factory = QuestsViewModel.factory(container))
    val quests by vm.quests.collectAsStateWithLifecycle()
    val selectedTab by vm.selectedTab.collectAsStateWithLifecycle()
    val todayXp by vm.todayAvailableXp.collectAsStateWithLifecycle()
    val bossProgress by vm.bossProgress.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Quests", style = MaterialTheme.typography.headlineSmall)
        Text("Available today: +$todayXp XP", style = MaterialTheme.typography.titleMedium)

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuestTab.entries.forEach { tab ->
                FilterChip(
                    selected = selectedTab == tab,
                    onClick = { vm.selectTab(tab) },
                    label = { Text(tab.name.lowercase().replaceFirstChar { it.uppercase() }) },
                )
            }
        }

        if (selectedTab == QuestTab.BOSSES) {
            BossesTab(bossProgress)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(quests, key = { it.id }) { q ->
                    QuestCard(
                        quest = q,
                        onComplete = { scope.launch { container.questCompletion.complete(q.id) } },
                        onUndo = { scope.launch { container.questCompletion.undo(q.id) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun QuestCard(
    quest: com.example.solo_levelling.data.db.entity.QuestInstanceEntity,
    onComplete: () -> Unit,
    onUndo: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(quest.title, style = MaterialTheme.typography.titleMedium)
            Text("${quest.type} · +${quest.baseXp} XP · ${quest.status}")
            if (quest.verificationType != "MANUAL") {
                Text("Verify: ${quest.verificationType} ${quest.verificationTarget.toInt()} ${quest.verificationUnit}")
            }
            Row {
                if (quest.status != QuestStatus.COMPLETED.name && quest.status != QuestStatus.LOCKED.name) {
                    Button(onClick = onComplete) { Text("Complete") }
                } else if (quest.status == QuestStatus.COMPLETED.name) {
                    TextButton(onClick = onUndo) { Text("Undo") }
                }
            }
        }
    }
}

@Composable
private fun BossesTab(bossProgress: BossProgressUi?) {
    if (bossProgress == null) {
        Text("No active boss")
        return
    }
    val boss = bossProgress.boss
    val progress = if (boss.targetValue > 0f) boss.currentValue / boss.targetValue else 0f
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(boss.title, style = MaterialTheme.typography.titleMedium)
            Text(boss.description)
            LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            Text("${boss.currentValue.toInt()} / ${boss.targetValue.toInt()} · ${boss.status}")
            bossProgress.quests.forEach { bq ->
                Text(
                    "${bq.templateKey}: ${if (bq.completed) "done" else "pending"} (weight ${bq.weight})",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
