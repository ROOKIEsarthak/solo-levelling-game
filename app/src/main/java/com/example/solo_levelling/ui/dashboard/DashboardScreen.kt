package com.example.solo_levelling.ui.dashboard

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.domain.copy.SystemMessages
import com.example.solo_levelling.domain.model.QuestStatus
import com.example.solo_levelling.domain.service.QuestCompletionService
import com.example.solo_levelling.ui.components.AttributeSummary
import com.example.solo_levelling.ui.components.EnergyFieldBackground
import com.example.solo_levelling.ui.components.GhostTextButton
import com.example.solo_levelling.ui.components.GlassLevel
import com.example.solo_levelling.ui.components.GlassSurface
import com.example.solo_levelling.ui.components.PlayerHeader
import com.example.solo_levelling.ui.components.SystemActionButton
import com.example.solo_levelling.ui.components.SystemIdleEmpty
import com.example.solo_levelling.ui.components.SystemSectionHeader
import com.example.solo_levelling.ui.components.TodayProgressStrip
import com.example.solo_levelling.ui.components.attributeDisplays
import com.example.solo_levelling.ui.components.attributeInsight
import com.example.solo_levelling.ui.components.greetingForHour
import com.example.solo_levelling.ui.components.humanizeNextActionDetail
import com.example.solo_levelling.ui.components.humanizeSuggestionTitle
import com.example.solo_levelling.ui.theme.JetBrainsMono
import com.example.solo_levelling.ui.theme.Spacing
import com.example.solo_levelling.ui.theme.SystemPrimary
import com.example.solo_levelling.ui.theme.SystemTertiary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DashboardScreen(
    container: AppContainer,
    onOpenAchievements: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWorkout: () -> Unit = {},
    onOpenNutrition: () -> Unit = {},
    onOpenMissions: () -> Unit = {},
    onOpenCareer: () -> Unit = {},
    onOpenCharacter: () -> Unit = {},
    onMessage: (String) -> Unit = {},
) {
    val vm: DashboardViewModel = viewModel(factory = DashboardViewModel.factory(container))
    val profile by vm.profile.collectAsStateWithLifecycle()
    val quests by vm.todayQuests.collectAsStateWithLifecycle()
    val streak by vm.streak.collectAsStateWithLifecycle()
    val attributes by vm.attributes.collectAsStateWithLifecycle()
    val xpLast7Days by vm.xpLast7Days.collectAsStateWithLifecycle()
    val workoutToday by vm.workoutToday.collectAsStateWithLifecycle()
    val nutritionToday by vm.nutritionToday.collectAsStateWithLifecycle()
    val calorieTarget by vm.calorieTarget.collectAsStateWithLifecycle()
    val proteinTarget by vm.proteinTarget.collectAsStateWithLifecycle()
    val nextAction by vm.nextAction.collectAsStateWithLifecycle()
    val suggestions by vm.suggestions.collectAsStateWithLifecycle()
    val modules by vm.enabledModules.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val p = profile
    val xpIntoLevel = if (p == null) 0 else p.totalXp - SystemDefaults.totalXpForLevel(p.level)
    val xpNeed = if (p == null) 1 else SystemDefaults.xpForNextLevel(p.level)
    val completed = quests.count { it.status == QuestStatus.COMPLETED.name }
    val total = quests.size
    val name = p?.name ?: "Hunter"
    val greeting = greetingForHour(java.time.LocalTime.now().hour)
    val dateLabel = LocalDate.now().format(
        DateTimeFormatter.ofPattern("EEEE · MMMM d", Locale.getDefault()),
    )
    val attrDisplays = attributeDisplays(
        codes = attributes.map { it.code },
        values = attributes.map { it.currentValue },
        lifetimeXp = attributes.map { it.lifetimeXp },
    )
    val insight = attributeInsight(
        codes = attributes.map { it.code },
        values = attributes.map { it.currentValue },
    )
    val streakDays = streak?.current ?: 0

    Box(Modifier.fillMaxSize()) {
        EnergyFieldBackground(Modifier.fillMaxSize())
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.section),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            "SOVEREIGN OS",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = JetBrainsMono,
                            color = SystemPrimary,
                        )
                        Text(
                            dateLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row {
                        IconButton(onClick = onOpenAchievements) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = "Achievements")
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                }
            }

            item {
                PlayerHeader(
                    name = name,
                    level = p?.level ?: 1,
                    rank = p?.rank ?: "E",
                    xpIntoLevel = xpIntoLevel,
                    xpNeed = xpNeed,
                    greeting = greeting,
                )
            }

            item {
                TodayProgressStrip(
                    questsDone = completed,
                    questsTotal = total,
                    xpLabel = "+$xpLast7Days XP · 7D",
                    streakDays = streakDays,
                )
            }

            item {
                SystemSectionHeader(tag = "Today's mission")
                Spacer(Modifier.height(Spacing.xs))
                val action = nextAction
                if (action == null) {
                    SystemIdleEmpty(
                        actionLabel = "View quests",
                        onAction = onOpenMissions,
                    )
                } else {
                    GlassSurface(modifier = Modifier.fillMaxWidth(), level = GlassLevel.Level2) {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            Text(
                                humanizeSuggestionTitle(action.title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            if (action.detail.isNotBlank()) {
                                Text(
                                    humanizeNextActionDetail(action.detail),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            SystemActionButton(
                                label = "Begin",
                                onClick = {
                                    when (action.routeHint) {
                                        "quests" -> onOpenMissions()
                                        "fitness" -> onOpenWorkout()
                                        "nutrition" -> onOpenNutrition()
                                        "career" -> onOpenCareer()
                                        else -> onOpenMissions()
                                    }
                                },
                            )
                            val priorityQuest = quests.firstOrNull {
                                it.status == QuestStatus.AVAILABLE.name &&
                                    it.title.equals(action.title, ignoreCase = true)
                            } ?: quests.firstOrNull { it.status == QuestStatus.AVAILABLE.name }
                            if (priorityQuest != null) {
                                GhostTextButton(
                                    label = "Complete now",
                                    onClick = {
                                        scope.launch {
                                            val result = withContext(Dispatchers.IO) {
                                                container.questCompletion.complete(priorityQuest.id)
                                            }
                                            onMessage(questCompletionUserMessage(result))
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }

            item {
                AttributeSummary(
                    displays = attrDisplays,
                    insight = insight,
                    onViewCharacter = onOpenCharacter,
                )
            }

            if (suggestions.isNotEmpty()) {
                item {
                    GlassSurface(modifier = Modifier.fillMaxWidth(), level = GlassLevel.Level1) {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            Text(
                                "Suggestions",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            suggestions.take(2).forEach { suggestion ->
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            humanizeSuggestionTitle(suggestion.title),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            suggestion.detail,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        )
                                    }
                                    GhostTextButton(
                                        label = "Dismiss",
                                        onClick = { vm.dismissSuggestion(suggestion.key) },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (modules.workout || modules.diet || modules.career) {
                item {
                    GlassSurface(modifier = Modifier.fillMaxWidth(), level = GlassLevel.Level1) {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            SystemSectionHeader(tag = "Quick actions")
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                            ) {
                                if (modules.career) {
                                    GhostTextButton(label = "Career", onClick = onOpenCareer)
                                }
                                if (modules.workout) {
                                    GhostTextButton(
                                        label = if (workoutToday != null) "Workout" else "Add workout",
                                        onClick = onOpenWorkout,
                                    )
                                }
                                if (modules.diet) {
                                    val calories = nutritionToday?.calories ?: 0
                                    GhostTextButton(
                                        label = if (calories > 0) "Meal" else "Add meal",
                                        onClick = onOpenNutrition,
                                    )
                                }
                            }
                            if (modules.diet && (calorieTarget > 0 || proteinTarget > 0)) {
                                Text(
                                    "Nutrition · ${nutritionToday?.calories ?: 0} / $calorieTarget kcal",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMono,
                                    color = SystemTertiary,
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(Spacing.md)) }
        }
    }
}

internal fun questCompletionUserMessage(result: QuestCompletionService.Result): String = when (result) {
    is QuestCompletionService.Result.Completed ->
        SystemMessages.questCompletedFeedback(result.xp)
    QuestCompletionService.Result.AlreadyCompleted ->
        "Quest already completed"
    QuestCompletionService.Result.NotFound ->
        "Quest not found"
    QuestCompletionService.Result.InvalidStatus ->
        "Quest can't be completed right now"
    QuestCompletionService.Result.DailyCapReached ->
        "Daily XP cap reached"
}
