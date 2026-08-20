package com.example.solo_levelling.ui.analytics

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.SeasonEntity
import com.example.solo_levelling.data.db.entity.StreakStateEntity
import com.example.solo_levelling.domain.copy.SystemMessages
import com.example.solo_levelling.domain.copy.SystemMessages.MotivationContext
import com.example.solo_levelling.domain.service.BeforeVsNow
import com.example.solo_levelling.domain.service.EnabledModules
import com.example.solo_levelling.domain.service.ImprovementSnapshot
import com.example.solo_levelling.domain.service.ModuleFlags
import com.example.solo_levelling.domain.service.ModuleScope
import com.example.solo_levelling.domain.service.WeeklyReview
import com.example.solo_levelling.ui.components.EnergyFieldBackground
import com.example.solo_levelling.ui.components.GlassLevel
import com.example.solo_levelling.ui.components.GlassSurface
import com.example.solo_levelling.ui.components.LoadingSkeleton
import com.example.solo_levelling.ui.components.SystemActionButton
import com.example.solo_levelling.ui.components.SystemSectionHeader
import com.example.solo_levelling.ui.components.areaToInvestCopy
import com.example.solo_levelling.ui.components.humanizeSuggestionTitle
import com.example.solo_levelling.ui.theme.JetBrainsMono
import com.example.solo_levelling.ui.theme.Spacing
import com.example.solo_levelling.ui.theme.SystemPrimary
import com.example.solo_levelling.ui.theme.SystemSecondary
import com.example.solo_levelling.ui.theme.SystemSuccess
import com.example.solo_levelling.ui.theme.SystemTertiary
import kotlinx.coroutines.launch

@Composable
fun AnalyticsScreen(
    container: AppContainer,
    onMessage: (String) -> Unit = {},
) {
    var review by remember { mutableStateOf<WeeklyReview?>(null) }
    var improvement by remember { mutableStateOf<ImprovementSnapshot?>(null) }
    var beforeVsNow by remember { mutableStateOf<BeforeVsNow?>(null) }
    var profile by remember { mutableStateOf<PlayerProfileEntity?>(null) }
    var streak by remember { mutableStateOf<StreakStateEntity?>(null) }
    var activeSeason by remember { mutableStateOf<SeasonEntity?>(null) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme

    val enabledModules by ModuleFlags.observeEnabledModules(
        container.db.playerDao().observeProfile(SystemDefaults.PLAYER_ID),
        container.db.configDao(),
    ).collectAsStateWithLifecycle(initialValue = EnabledModules())

    LaunchedEffect(enabledModules) {
        loading = true
        review = container.analytics.weeklyReview()
        improvement = container.analytics.improvementSnapshot()
        beforeVsNow = container.analytics.beforeVsNow()
        profile = container.db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
        streak = container.db.playerDao().getStreak(SystemDefaults.PLAYER_ID)
        activeSeason = container.season.ensureActiveSeason()
        loading = false
    }

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
                SystemSectionHeader(tag = "REFLECTION")
                Text(
                    "A quiet look at how your week is unfolding.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                Text(
                    "Active · ${ModuleScope.activeModulesSummary(enabledModules)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMono,
                    color = colors.onSurfaceVariant,
                )
                activeSeason?.let { s ->
                    Text(
                        "${s.name} · ${s.seasonXp} season XP",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMono,
                        color = colors.onSurfaceVariant,
                    )
                }
            }

            if (loading) {
                LoadingSkeleton(lines = 4)
                LoadingSkeleton(lines = 3)
            } else {
                review?.let { r ->
                    GlassSurface(modifier = Modifier.fillMaxWidth(), level = GlassLevel.Level2) {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            SystemSectionHeader(tag = "THIS WEEK")
                            Text(
                                "${r.weekStart} → ${r.weekEnd}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMono,
                                color = colors.onSurfaceVariant,
                            )
                            MetricLine(
                                label = "Quests",
                                value = "${r.questsCompleted} / ${r.questsTotal} done",
                            )
                            MetricLine(
                                label = "Completion",
                                value = formatCompletionRate(r.completionRate),
                            )
                            MetricLine(label = "XP earned", value = "${r.xpEarned}")
                            r.careerXp?.let { MetricLine(label = "Career XP", value = "$it") }
                            r.workoutXp?.let { MetricLine(label = "Fitness XP", value = "$it") }
                            r.dietXp?.let { MetricLine(label = "Nutrition XP", value = "$it") }
                            r.workoutCountWeek?.let { count ->
                                MetricLine(label = "Workouts", value = "$count sessions")
                            }
                            MetricLine(label = "Streak", value = "${streak?.current ?: 0} days")
                            MetricLine(
                                label = "Personal score",
                                value = "${r.personalScore}/100",
                                highlight = true,
                            )
                            r.bossProgress?.let { boss ->
                                MetricLine(
                                    label = "Boss · ${humanizeSuggestionTitle(boss.title)}",
                                    value = "${boss.current.toInt()} / ${boss.target.toInt()}",
                                )
                            }
                            Text(
                                weeklyReviewEncouragement(r.personalScore + r.xpEarned),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                            )
                            if (r.recommendations.isNotEmpty()) {
                                Spacer(Modifier.height(Spacing.xxs))
                                Text(
                                    "Notes for you",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMono,
                                    color = colors.onSurfaceVariant,
                                )
                                r.recommendations.forEach { rec ->
                                    Text(
                                        "· ${humanizeSuggestionTitle(rec)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                improvement?.let { snap ->
                    GlassSurface(modifier = Modifier.fillMaxWidth(), level = GlassLevel.Level1) {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            SystemSectionHeader(tag = "WEEK OVER WEEK")
                            ScoreBlock(
                                label = "This week",
                                score = snap.current.score,
                                accent = SystemSuccess,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            ScoreBlock(
                                label = "Last week",
                                score = snap.previous.score,
                                accent = colors.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            val pct = snap.improvementPercent
                            if (pct != null) {
                                Text(
                                    improvementCopy(pct),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SystemTertiary,
                                )
                            } else {
                                Text(
                                    "Still gathering a baseline — keep showing up and this will take shape.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                beforeVsNow?.let { b ->
                    GlassSurface(modifier = Modifier.fillMaxWidth(), level = GlassLevel.Level1) {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            SystemSectionHeader(tag = "BEFORE VS NOW", accent = SystemSecondary)
                            CompareRow(
                                label = "Task completion",
                                before = formatCompletionRate(b.taskCompletionBefore),
                                now = formatCompletionRate(b.taskCompletionNow),
                            )
                            if (b.workoutDaysBefore != null && b.workoutDaysNow != null) {
                                CompareRow(
                                    label = "Workout days / week",
                                    before = "${b.workoutDaysBefore}",
                                    now = "${b.workoutDaysNow}",
                                )
                            }
                            if (b.dsaSolvedBefore != null && b.dsaSolvedNow != null) {
                                CompareRow(
                                    label = "DSA solved / week",
                                    before = "${b.dsaSolvedBefore}",
                                    now = "${b.dsaSolvedNow}",
                                )
                            }
                            b.dietAdherenceBefore?.let { before ->
                                b.dietAdherenceNow?.let { now ->
                                    CompareRow(label = "Diet adherence", before = "$before%", now = "$now%")
                                }
                            }
                            if (b.weightBefore != null && b.weightNow != null) {
                                CompareRow(
                                    label = "Weight (kg)",
                                    before = "%.1f".format(b.weightBefore),
                                    now = "%.1f".format(b.weightNow),
                                )
                            }
                            CompareRow(
                                label = "Streak",
                                before = "${b.streakBest} days (best)",
                                now = "${b.streakCurrent} days (current)",
                            )
                        }
                    }
                }

                val bottomCode = review?.attributeSnapshot?.bottomCode
                if (bottomCode != null) {
                    GlassSurface(modifier = Modifier.fillMaxWidth(), level = GlassLevel.Level1) {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            SystemSectionHeader(tag = "NEXT FOCUS")
                            Text(
                                nextFocusCopy(bottomCode),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceVariant,
                            )
                        }
                    }
                }

                profile?.let { p ->
                    Text(
                        "Level ${p.level} · ${p.rank} rank",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMono,
                        color = colors.onSurfaceVariant,
                    )
                }
            }

            SystemActionButton(
                label = "EXPORT PROGRESS",
                onClick = {
                    scope.launch {
                        val json = container.analytics.exportJson()
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, json)
                        }
                        context.startActivity(Intent.createChooser(send, "Export progress"))
                        onMessage("Export ready")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading,
            )
        }
    }
}

@Composable
private fun ScoreBlock(
    label: String,
    score: Int,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = JetBrainsMono,
            color = accent,
        )
        Text(
            "$score/100",
            style = MaterialTheme.typography.titleLarge,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
            color = accent,
        )
    }
}

@Composable
private fun MetricLine(label: String, value: String, highlight: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = JetBrainsMono,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = JetBrainsMono,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            color = if (highlight) SystemPrimary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun CompareRow(label: String, before: String, now: String) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = JetBrainsMono,
            color = colors.onSurfaceVariant,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(
                    "Before",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMono,
                    color = colors.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    before,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = JetBrainsMono,
                    color = colors.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "Now",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMono,
                    color = SystemSuccess,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    now,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    color = SystemSuccess,
                )
            }
        }
    }
}

internal fun formatCompletionRate(rate: Float): String = "${(rate * 100).toInt()}%"

internal fun improvementCopy(percent: Float): String =
    "Your personal score moved about ${"%.1f".format(percent)}% compared to last week."

internal fun nextFocusCopy(bottomCode: String): String = areaToInvestCopy(bottomCode)

internal fun weeklyReviewEncouragement(seed: Int): String =
    SystemMessages.forContext(MotivationContext.WeeklyReview, seed)
