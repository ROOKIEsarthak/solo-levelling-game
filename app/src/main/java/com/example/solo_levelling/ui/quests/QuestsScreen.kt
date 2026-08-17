package com.example.solo_levelling.ui.quests

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.domain.model.QuestStatus
import com.example.solo_levelling.domain.service.QuestCompletionService
import com.example.solo_levelling.ui.theme.SystemWarning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun QuestsScreen(
    container: AppContainer,
    onMessage: (String) -> Unit = {},
) {
    val vm: QuestsViewModel = viewModel(factory = QuestsViewModel.factory(container))
    val quests by vm.quests.collectAsStateWithLifecycle()
    val selectedTab by vm.selectedTab.collectAsStateWithLifecycle()
    val todayXp by vm.todayAvailableXp.collectAsStateWithLifecycle()
    val bossProgress by vm.bossProgress.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = colors.primary.copy(alpha = 0.15f),
        selectedLabelColor = colors.primary,
    )

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Missions", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Available today: +$todayXp XP",
            color = colors.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuestTab.entries.forEach { tab ->
                FilterChip(
                    selected = selectedTab == tab,
                    onClick = { vm.selectTab(tab) },
                    label = { Text(tab.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    colors = chipColors,
                    border = BorderStroke(
                        1.dp,
                        if (selectedTab == tab) colors.primary else colors.outline,
                    ),
                )
            }
        }

        if (selectedTab == QuestTab.BOSSES) {
            BossesTab(bossProgress)
        } else if (quests.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
                border = BorderStroke(1.dp, colors.outline),
            ) {
                Text(
                    "No ${selectedTab.name.lowercase()} quests generated",
                    Modifier.padding(24.dp),
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(quests, key = { it.id }) { q ->
                    QuestCard(
                        quest = q,
                        onComplete = {
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    container.questCompletion.complete(q.id)
                                }
                                when (result) {
                                    is QuestCompletionService.Result.Completed ->
                                        onMessage("Quest completed · +${result.xp} XP")
                                    QuestCompletionService.Result.AlreadyCompleted ->
                                        onMessage("Quest already completed")
                                    QuestCompletionService.Result.NotFound ->
                                        onMessage("Quest not found")
                                    QuestCompletionService.Result.InvalidStatus ->
                                        onMessage("Quest can't be completed right now")
                                    QuestCompletionService.Result.DailyCapReached ->
                                        onMessage("Daily XP cap reached")
                                }
                            }
                        },
                        onUndo = {
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) {
                                    container.questCompletion.undo(q.id)
                                }
                                if (ok) {
                                    onMessage("Quest reverted to ACTIVE")
                                } else {
                                    onMessage("Couldn't undo — window may have expired")
                                }
                            }
                        },
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
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
        border = BorderStroke(1.dp, colors.outline),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(quest.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "${quest.type.uppercase()} · +${quest.baseXp} XP · " +
                            if (quest.status == QuestStatus.COMPLETED.name) "COMPLETED" else "ACTIVE",
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (quest.verificationType != "MANUAL") {
                        Text(
                            "Verify: ${quest.verificationType} ${quest.verificationTarget.toInt()} ${quest.verificationUnit}",
                            color = colors.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                if (quest.status != QuestStatus.COMPLETED.name && quest.status != QuestStatus.LOCKED.name) {
                    Button(onClick = onComplete) { Text("Complete") }
                } else if (quest.status == QuestStatus.COMPLETED.name) {
                    OutlinedButton(onClick = onUndo) { Text("Undo") }
                }
            }
        }
    }
}

@Composable
private fun BossesTab(bossProgress: BossProgressUi?) {
    val colors = MaterialTheme.colorScheme
    if (bossProgress == null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
            border = BorderStroke(1.dp, colors.outline),
        ) {
            Text(
                "No active boss",
                Modifier.padding(24.dp),
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        return
    }
    val boss = bossProgress.boss
    val progress = if (boss.targetValue > 0f) boss.currentValue / boss.targetValue else 0f
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
        border = BorderStroke(1.dp, colors.outline),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(boss.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(boss.description, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = SystemWarning,
                trackColor = colors.surfaceContainerHighest,
            )
            Text(
                "${boss.currentValue.toInt()} / ${boss.targetValue.toInt()} · ${boss.status}",
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            bossProgress.quests.forEach { bq ->
                Text(
                    "${bq.templateKey}: ${if (bq.completed) "COMPLETED" else "PENDING"} (weight ${bq.weight})",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
