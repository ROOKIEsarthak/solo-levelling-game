package com.example.solo_levelling.ui.quests

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.domain.copy.SystemMessages
import com.example.solo_levelling.domain.service.QuestCompletionService
import com.example.solo_levelling.ui.dashboard.questCompletionUserMessage
import com.example.solo_levelling.ui.components.BracketLabel
import com.example.solo_levelling.ui.components.CyberProgressBar
import com.example.solo_levelling.ui.components.EnergyFieldBackground
import com.example.solo_levelling.ui.components.GlassLevel
import com.example.solo_levelling.ui.components.GlassSurface
import com.example.solo_levelling.ui.components.MissionQuestCard
import com.example.solo_levelling.ui.components.humanizeSuggestionTitle
import com.example.solo_levelling.ui.components.SovereignChip
import com.example.solo_levelling.ui.components.SystemIdleEmpty
import com.example.solo_levelling.ui.components.SystemSectionHeader
import com.example.solo_levelling.ui.components.progressFraction
import com.example.solo_levelling.ui.theme.JetBrainsMono
import com.example.solo_levelling.ui.theme.Spacing
import com.example.solo_levelling.ui.theme.SystemError
import com.example.solo_levelling.ui.theme.SystemPrimary
import com.example.solo_levelling.ui.theme.SystemSuccess
import com.example.solo_levelling.ui.theme.SystemTertiary
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

    Box(Modifier.fillMaxSize()) {
        EnergyFieldBackground(Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxSize()
                .padding(Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            SystemSectionHeader(tag = "Missions")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    selectedTab.displayLabel(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                BracketLabel(text = "+$todayXp XP today", color = SystemTertiary)
            }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                QuestTab.entries.forEach { tab ->
                    SovereignChip(
                        label = tab.displayLabel(),
                        selected = selectedTab == tab,
                        onClick = { vm.selectTab(tab) },
                    )
                }
            }

            if (selectedTab == QuestTab.BOSSES) {
                BossesTab(bossProgress)
            } else if (quests.isEmpty()) {
                SystemIdleEmpty(
                    subtitle = SystemMessages.forContext(
                        SystemMessages.MotivationContext.NoQuests,
                        selectedTab.ordinal,
                    ),
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.xs + 2.dp)) {
                    items(quests, key = { it.id }) { q ->
                        MissionQuestCard(
                            type = q.type,
                            title = humanizeSuggestionTitle(q.title),
                            baseXp = q.baseXp,
                            status = q.status,
                            rewardsJson = q.attributeRewardsJson,
                            verificationType = q.verificationType,
                            verificationTarget = q.verificationTarget,
                            verificationUnit = q.verificationUnit,
                            onPrimary = {
                                scope.launch {
                                    val result = withContext(Dispatchers.IO) {
                                        container.questCompletion.complete(q.id)
                                    }
                                    onMessage(questCompletionUserMessage(result))
                                }
                            },
                            onUndo = {
                                scope.launch {
                                    val ok = withContext(Dispatchers.IO) {
                                        container.questCompletion.undo(q.id)
                                    }
                                    if (ok) {
                                        onMessage("Quest reverted to active")
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
}

@Composable
fun BossesTab(bossProgress: BossProgressUi?) {
    if (bossProgress == null) {
        SystemIdleEmpty(
            subtitle = "No active boss challenge right now.",
        )
        return
    }

    val boss = bossProgress.boss
    val progress = progressFraction(boss.currentValue, boss.targetValue)

    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SystemError.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
        level = GlassLevel.Level2,
        borderAlpha = 0.12f,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs + 2.dp),
        ) {
            SystemSectionHeader(tag = "Boss challenge", accent = SystemError)
            Text(
                humanizeSuggestionTitle(boss.title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                boss.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CyberProgressBar(progress = progress, height = 12.dp)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "${boss.currentValue.toInt()} / ${boss.targetValue.toInt()}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMono,
                    color = SystemError,
                )
                BracketLabel(text = boss.status, color = SystemError)
            }
            if (bossProgress.quests.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.xxs))
                Text(
                    "Objectives",
                    style = MaterialTheme.typography.labelSmall,
                    color = SystemPrimary,
                    fontWeight = FontWeight.Medium,
                )
                bossProgress.quests.forEach { bq ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            bq.templateKey,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        BracketLabel(
                            text = if (bq.completed) "Done" else "Pending · W${bq.weight.toInt()}",
                            color = if (bq.completed) SystemSuccess else SystemTertiary,
                        )
                    }
                }
            }
        }
    }
}

internal fun QuestTab.displayLabel(): String = when (this) {
    QuestTab.TODAY -> "Today"
    QuestTab.WEEKLY -> "Weekly"
    QuestTab.MILESTONES -> "Milestones"
    QuestTab.RECOVERY -> "Recovery"
    QuestTab.BOSSES -> "Bosses"
}
