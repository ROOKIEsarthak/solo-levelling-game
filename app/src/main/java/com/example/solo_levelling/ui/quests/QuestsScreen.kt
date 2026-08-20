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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.domain.copy.SystemMessages
import com.example.solo_levelling.domain.model.QuestType
import com.example.solo_levelling.domain.service.MilestoneRequirement
import com.example.solo_levelling.domain.service.MilestoneVerificationResult
import com.example.solo_levelling.domain.service.QuestCompletionService
import com.example.solo_levelling.ui.dashboard.questCompletionUserMessage
import com.example.solo_levelling.ui.navigation.QuestAction
import com.example.solo_levelling.ui.navigation.QuestActionDestination
import com.example.solo_levelling.ui.navigation.QuestDestinationResolver
import com.example.solo_levelling.ui.components.BracketLabel
import com.example.solo_levelling.ui.components.CyberProgressBar
import com.example.solo_levelling.ui.components.EnergyFieldBackground
import com.example.solo_levelling.ui.components.GhostTextButton
import com.example.solo_levelling.ui.components.GlassLevel
import com.example.solo_levelling.ui.components.GlassSurface
import com.example.solo_levelling.ui.components.MissionQuestCard
import com.example.solo_levelling.ui.components.humanizeSuggestionTitle
import com.example.solo_levelling.ui.components.SovereignChip
import com.example.solo_levelling.ui.components.SystemActionButton
import com.example.solo_levelling.ui.components.SystemConfirmDialog
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
    onOpenWorkout: () -> Unit = {},
    onOpenNutrition: () -> Unit = {},
    onOpenCareer: (section: String) -> Unit = {},
    onOpenModules: (section: String) -> Unit = {},
) {
    val vm: QuestsViewModel = viewModel(factory = QuestsViewModel.factory(container))
    val quests by vm.quests.collectAsStateWithLifecycle()
    val selectedTab by vm.selectedTab.collectAsStateWithLifecycle()
    val todayXp by vm.todayAvailableXp.collectAsStateWithLifecycle()
    val bossProgress by vm.bossProgress.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var pendingIncomplete by remember { mutableStateOf<MilestoneVerificationResult?>(null) }
    var pendingUndoId by remember { mutableStateOf<Long?>(null) }

    fun openRequirement(requirement: MilestoneRequirement) {
        QuestDestinationResolver.dispatch(
            action = milestoneRequirementAction(requirement),
            onFitness = onOpenWorkout,
            onNutrition = onOpenNutrition,
            onCareer = onOpenCareer,
            onModules = onOpenModules,
            onCompleteInPlace = {},
        )
    }

    fun completeQuest(id: Long, type: String) {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                vm.complete(id)
            }
            when (result) {
                is QuestCompletionService.Result.RequirementsIncomplete -> {
                    pendingIncomplete = result.verification
                }
                is QuestCompletionService.Result.Completed -> {
                    if (type == QuestType.MILESTONE.name) {
                        onMessage(SystemMessages.milestoneCompletedFeedback(result.xp))
                    } else {
                        onMessage(questCompletionUserMessage(result))
                    }
                }
                else -> onMessage(questCompletionUserMessage(result))
            }
        }
    }

    fun onQuestPrimary(item: QuestListItem) {
        val q = item.instance
        val action = QuestDestinationResolver.resolve(
            priorityTags = item.priorityTags,
            verificationType = q.verificationType,
            status = q.status,
            templateKey = item.templateKey,
            questType = q.type,
        )
        QuestDestinationResolver.dispatch(
            action = action,
            onFitness = onOpenWorkout,
            onNutrition = onOpenNutrition,
            onCareer = onOpenCareer,
            onModules = onOpenModules,
            onCompleteInPlace = { completeQuest(q.id, q.type) },
        )
    }

    pendingUndoId?.let { undoId ->
        SystemConfirmDialog(
            title = SystemMessages.REVERSE_COMPLETION_TITLE,
            explanation = SystemMessages.REVERSE_COMPLETION_EXPLANATION,
            consequence = SystemMessages.REVERSE_COMPLETION_CONSEQUENCE,
            confirmLabel = SystemMessages.REVERSE_COMPLETION_CONFIRM,
            cancelLabel = SystemMessages.REVERSE_COMPLETION_KEEP,
            onDismiss = { pendingUndoId = null },
            onConfirm = {
                pendingUndoId = null
                scope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        vm.undo(undoId)
                    }
                    onMessage(
                        if (ok) SystemMessages.COMPLETION_REVERSED
                        else SystemMessages.COMPLETION_REVERSE_FAILED,
                    )
                }
            },
        )
    }

    pendingIncomplete?.let { verification ->
        MilestoneNotReadyDialog(
            verification = verification,
            onOpenRequirement = { requirement ->
                pendingIncomplete = null
                openRequirement(requirement)
            },
            onCompleteRequirements = {
                val firstNavigable = verification.requirements.firstOrNull { requirement ->
                    !requirement.completed &&
                        QuestDestinationResolver.navigates(
                            milestoneRequirementAction(requirement).destination,
                        )
                }
                pendingIncomplete = null
                if (firstNavigable != null) openRequirement(firstNavigable)
            },
            onDismiss = { pendingIncomplete = null },
        )
    }

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
                    items(quests, key = { it.instance.id }) { item ->
                        val q = item.instance
                        val action = QuestDestinationResolver.resolve(
                            priorityTags = item.priorityTags,
                            verificationType = q.verificationType,
                            status = q.status,
                            templateKey = item.templateKey,
                            questType = q.type,
                        )
                        MissionQuestCard(
                            type = q.type,
                            title = humanizeSuggestionTitle(q.title),
                            baseXp = q.baseXp,
                            status = q.status,
                            rewardsJson = q.attributeRewardsJson,
                            verificationType = q.verificationType,
                            verificationTarget = q.verificationTarget,
                            verificationUnit = q.verificationUnit,
                            primaryLabel = action.label,
                            requirementSummary = if (q.type == QuestType.MILESTONE.name && item.totalRequirements > 0) {
                                "${item.completedRequirements} / ${item.totalRequirements} requirements"
                            } else {
                                ""
                            },
                            onPrimary = { onQuestPrimary(item) },
                            completedAtEpochMs = q.completedAtEpochMs,
                            onUndo = if (q.type == QuestType.MILESTONE.name) {
                                null
                            } else {
                                { pendingUndoId = q.id }
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
    QuestTab.BOSSES -> "Bosses"
}

@Composable
private fun MilestoneNotReadyDialog(
    verification: MilestoneVerificationResult,
    onOpenRequirement: (MilestoneRequirement) -> Unit,
    onCompleteRequirements: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        title = {
            Text(
                text = "[ MILESTONE NOT READY ]",
                fontFamily = JetBrainsMono,
                color = SystemTertiary,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(
                    "Complete the remaining requirements before finishing this milestone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${verification.completedCount} / ${verification.totalCount} requirements complete",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMono,
                    color = SystemPrimary,
                )
                if (verification.requirements.isEmpty()) {
                    Text(
                        "No qualifying requirements yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    verification.requirements.forEach { requirement ->
                        val action = milestoneRequirementAction(requirement)
                        val openLabel = milestoneRequirementOpenLabel(action)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (requirement.completed) {
                                    "✓ ${humanizeSuggestionTitle(requirement.title)}"
                                } else {
                                    "○ ${humanizeSuggestionTitle(requirement.title)}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (requirement.completed) {
                                    SystemSuccess
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                            if (!requirement.completed && openLabel != null) {
                                SystemActionButton(
                                    label = openLabel,
                                    onClick = { onOpenRequirement(requirement) },
                                    primary = false,
                                )
                            }
                        }
                    }
                }
                Text(
                    "Finish the remaining requirements first.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            SystemActionButton(
                label = "COMPLETE REQUIREMENTS",
                onClick = onCompleteRequirements,
                enabled = verification.requirements.any { !it.completed },
            )
        },
        dismissButton = {
            GhostTextButton(
                label = "NOT NOW",
                onClick = onDismiss,
            )
        },
    )
}

internal fun milestoneRequirementAction(requirement: MilestoneRequirement): QuestAction =
    QuestDestinationResolver.resolve(
        priorityTags = requirement.priorityTags,
        verificationType = requirement.verificationType,
        status = requirement.status,
        templateKey = requirement.templateKey,
        questType = requirement.questType,
    )

internal fun milestoneRequirementOpenLabel(action: QuestAction): String? {
    if (!QuestDestinationResolver.navigates(action.destination)) return null
    return when (action.destination) {
        QuestActionDestination.Fitness -> "OPEN TRAINING"
        QuestActionDestination.Nutrition -> "OPEN NUTRITION"
        QuestActionDestination.Career -> "OPEN CAREER"
        QuestActionDestination.Modules -> when (action.section) {
            QuestDestinationResolver.SECTION_JOURNAL -> "OPEN JOURNAL"
            QuestDestinationResolver.SECTION_FOCUS -> "OPEN FOCUS"
            QuestDestinationResolver.SECTION_METRICS -> "OPEN METRICS"
            else -> "OPEN MODULES"
        }
        QuestActionDestination.CompleteInPlace,
        QuestActionDestination.AwaitVerification,
        -> null
    }
}
