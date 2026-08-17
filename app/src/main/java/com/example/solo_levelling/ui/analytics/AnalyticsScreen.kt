package com.example.solo_levelling.ui.analytics

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.SeasonEntity
import com.example.solo_levelling.data.db.entity.StreakStateEntity
import com.example.solo_levelling.domain.service.BeforeVsNow
import com.example.solo_levelling.domain.service.ImprovementSnapshot
import com.example.solo_levelling.domain.service.WeeklyReview
import com.example.solo_levelling.ui.theme.GlowCyan
import com.example.solo_levelling.ui.theme.SystemSuccess
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
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme

    LaunchedEffect(Unit) {
        review = container.analytics.weeklyReview()
        improvement = container.analytics.improvementSnapshot()
        beforeVsNow = container.analytics.beforeVsNow()
        profile = container.db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
        streak = container.db.playerDao().getStreak(SystemDefaults.PLAYER_ID)
        activeSeason = container.season.ensureActiveSeason()
    }

    val p = profile
    val xpInto = if (p == null) 0 else p.totalXp - SystemDefaults.totalXpForLevel(p.level)
    val xpNeed = if (p == null) 1 else SystemDefaults.xpForNextLevel(p.level)
    val xpProgress = (xpInto.toFloat() / xpNeed.toFloat()).coerceIn(0f, 1f)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("YOUR PROGRESS", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        activeSeason?.let { s ->
            Text(
                "${s.name} · ${s.seasonXp} season XP",
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
            border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.35f)),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "LEVEL ${p?.level ?: 1}",
                    color = colors.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                )
                Text("$xpInto / $xpNeed XP", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                LinearProgressIndicator(
                    progress = { xpProgress },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = GlowCyan,
                    trackColor = colors.surfaceContainerHighest,
                )
            }
        }

        improvement?.let { snap ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
                border = BorderStroke(1.dp, colors.outline),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("PERIOD SCORES", fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("NOW  ${snap.current.score}/100", color = SystemSuccess, fontWeight = FontWeight.Bold)
                    Text(
                        "BEFORE  ${snap.previous.score}/100",
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    val pct = snap.improvementPercent
                    if (pct != null) {
                        Text(
                            "YOU ARE ${"%.1f".format(pct)}%\nBETTER THAN BEFORE.",
                            fontWeight = FontWeight.Bold,
                            color = GlowCyan,
                        )
                    } else {
                        Text(
                            "BUILDING YOUR BASELINE\nKeep showing up.\nYour improvement score will appear once enough data has been collected.",
                            color = colors.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        beforeVsNow?.let { b ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
                border = BorderStroke(1.dp, colors.outline),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("BEFORE VS NOW", fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    CompareRow(
                        "Task completion",
                        "${(b.taskCompletionBefore * 100).toInt()}%",
                        "${(b.taskCompletionNow * 100).toInt()}%",
                    )
                    CompareRow(
                        "Workout days / week",
                        "${b.workoutDaysBefore}",
                        "${b.workoutDaysNow}",
                    )
                    CompareRow(
                        "DSA solved / week",
                        "${b.dsaSolvedBefore}",
                        "${b.dsaSolvedNow}",
                    )
                    b.dietAdherenceBefore?.let { before ->
                        b.dietAdherenceNow?.let { now ->
                            CompareRow("Diet adherence", "$before%", "$now%")
                        }
                    }
                    if (b.weightBefore != null && b.weightNow != null) {
                        CompareRow(
                            "Weight (kg)",
                            "%.1f".format(b.weightBefore),
                            "%.1f".format(b.weightNow),
                        )
                    }
                    CompareRow(
                        "Streak",
                        "${b.streakBest} days (best)",
                        "${b.streakCurrent} days (current)",
                    )
                }
            }
        }

        review?.let { r ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
                border = BorderStroke(1.dp, colors.outline),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("THIS WEEK", fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("TASKS  ${(r.completionRate * 100).toInt()}% completion")
                    Text("WORKOUT  ${r.workoutCountWeek} sessions")
                    Text("STREAK  ${streak?.current ?: 0} days")
                    Text("Personal score  ${r.personalScore}/100", color = colors.primary)
                    r.recommendations.forEach { Text("· $it", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
                }
            }
        }

        Button(
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
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text("Export JSON")
        }
    }
}

@Composable
private fun CompareRow(label: String, before: String, now: String) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("BEFORE  $before", style = MaterialTheme.typography.bodyMedium)
            Text("NOW  $now", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = SystemSuccess)
        }
    }
}
