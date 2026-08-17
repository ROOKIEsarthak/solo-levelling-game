package com.example.solo_levelling.ui.character

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.R
import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.data.db.entity.XpLedgerEntryEntity
import com.example.solo_levelling.ui.components.AttributeRow
import com.example.solo_levelling.ui.components.BracketLabel
import com.example.solo_levelling.ui.components.CyberProgressBar
import com.example.solo_levelling.ui.components.EnergyFieldBackground
import com.example.solo_levelling.ui.components.GhostTextButton
import com.example.solo_levelling.ui.components.GlassLevel
import com.example.solo_levelling.ui.components.GlassSurface
import com.example.solo_levelling.ui.components.RankBadge
import com.example.solo_levelling.ui.components.StreakIndicator
import com.example.solo_levelling.ui.components.SystemIdleEmpty
import com.example.solo_levelling.ui.components.SystemSectionHeader
import com.example.solo_levelling.ui.components.attributeDisplays
import com.example.solo_levelling.ui.components.attributeGrowthInsight
import com.example.solo_levelling.ui.components.attributeInsight
import com.example.solo_levelling.ui.components.progressFraction
import com.example.solo_levelling.ui.components.xpProgressLabel
import com.example.solo_levelling.ui.theme.JetBrainsMono
import com.example.solo_levelling.ui.theme.Spacing
import com.example.solo_levelling.ui.theme.SystemPrimary
import com.example.solo_levelling.ui.theme.SystemSuccess
import com.example.solo_levelling.ui.theme.SystemSurface
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val LEDGER_COLLAPSED_LIMIT = 5

@Composable
fun CharacterScreen(container: AppContainer) {
    val vm: CharacterViewModel = viewModel(factory = CharacterViewModel.factory(container))
    val profile by vm.profile.collectAsStateWithLifecycle()
    val attrs by vm.attributes.collectAsStateWithLifecycle()
    val ledger by vm.ledgerHistory.collectAsStateWithLifecycle()
    val streak by vm.streak.collectAsStateWithLifecycle()
    val currentRole by vm.currentRole.collectAsStateWithLifecycle()
    val careerYears by vm.careerYears.collectAsStateWithLifecycle()
    val targetRole by vm.targetRole.collectAsStateWithLifecycle()
    val careerNextGoal by vm.careerNextGoal.collectAsStateWithLifecycle()
    val heightCm by vm.heightCm.collectAsStateWithLifecycle()
    val weightKg by vm.weightKg.collectAsStateWithLifecycle()
    val bmiEstimate by vm.bmiEstimate.collectAsStateWithLifecycle()
    val fitnessGoal by vm.fitnessGoal.collectAsStateWithLifecycle()
    val modules by vm.enabledModules.collectAsStateWithLifecycle()
    var ledgerExpanded by remember { mutableStateOf(false) }

    val p = profile
    val xpInto = if (p == null) 0 else p.totalXp - SystemDefaults.totalXpForLevel(p.level)
    val need = if (p == null) 1 else SystemDefaults.xpForNextLevel(p.level)
    val xpProgress = progressFraction(xpInto.toFloat(), need.toFloat())
    val displays = attributeDisplays(
        codes = attrs.map { it.code },
        values = attrs.map { it.currentValue },
        lifetimeXp = attrs.map { it.lifetimeXp },
    )
    val insight = attributeInsight(
        codes = attrs.map { it.code },
        values = attrs.map { it.currentValue },
    )
    val visibleLedger = visibleLedgerEntries(ledger, ledgerExpanded, LEDGER_COLLAPSED_LIMIT)
    val dateFmt = DateTimeFormatter.ofPattern("MMM d, HH:mm")

    Box(Modifier.fillMaxSize()) {
        EnergyFieldBackground(Modifier.fillMaxSize())
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.section),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                    SystemSectionHeader(tag = "CHARACTER")
                    Text(
                        "Your progression at a glance.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                CharacterIdentityHero(
                    name = p?.name ?: "Hunter",
                    level = p?.level ?: 1,
                    rank = p?.rank ?: "E",
                    totalXp = p?.totalXp ?: 0,
                    xpInto = xpInto,
                    xpNeed = need,
                    xpProgress = xpProgress,
                )
            }

            item {
                StreakIndicator(
                    current = streak?.current ?: 0,
                    best = streak?.best,
                )
            }

            item {
                SystemSectionHeader(tag = "Attributes")
            }

            if (displays.isEmpty()) {
                item {
                    SystemIdleEmpty(
                        title = "No attributes yet",
                        subtitle = "Complete quests to grow your attribute network.",
                    )
                }
            } else {
                item {
                    GlassSurface(modifier = Modifier.fillMaxWidth(), level = GlassLevel.Level1) {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            displays.forEach { attr ->
                                AttributeRow(
                                    code = attr.code,
                                    value = attr.value,
                                    fraction = attr.fraction,
                                    lifetimeXp = attr.lifetimeXp,
                                )
                            }
                            val growth = attributeGrowthInsight(insight)
                            if (growth.isNotBlank() && displays.size > 1) {
                                Text(
                                    growth,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            if (modules.career) {
                item {
                    SelfSectionCard(title = "CAREER") {
                        if (currentRole.isNotBlank()) {
                            StatLine(label = "Role", value = currentRole)
                        }
                        if (careerYears.isNotBlank()) {
                            StatLine(label = "Experience", value = "$careerYears years")
                        }
                        if (targetRole.isNotBlank()) {
                            StatLine(label = "Target", value = targetRole)
                        }
                        if (careerNextGoal.isNotBlank()) {
                            StatLine(label = "Next goal", value = careerNextGoal, highlight = true)
                        }
                        if (currentRole.isBlank() && careerYears.isBlank() && targetRole.isBlank() && careerNextGoal.isBlank()) {
                            Text(
                                "No career profile yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            if (modules.workout || modules.diet) {
                item {
                    SelfSectionCard(title = "FITNESS") {
                        if (heightCm.isNotBlank()) StatLine(label = "Height", value = "${heightCm} cm")
                        if (weightKg.isNotBlank()) StatLine(label = "Weight", value = "${weightKg} kg")
                        if (bmiEstimate.isNotBlank()) StatLine(label = "BMI (estimate)", value = bmiEstimate)
                        if (modules.workout && fitnessGoal.isNotBlank()) {
                            StatLine(label = "Goal", value = fitnessGoal.replace('_', ' '))
                        }
                        val hasBodyMetrics = heightCm.isNotBlank() || weightKg.isNotBlank() || bmiEstimate.isNotBlank()
                        val hasWorkoutGoal = modules.workout && fitnessGoal.isNotBlank()
                        if (!hasBodyMetrics && !hasWorkoutGoal) {
                            Text(
                                "No fitness profile yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item {
                SystemSectionHeader(tag = "XP LEDGER")
            }

            if (ledger.isEmpty()) {
                item {
                    SystemIdleEmpty(
                        title = "No XP recorded",
                        subtitle = "Quest completions and missions will appear here.",
                    )
                }
            } else {
                items(visibleLedger) { entry ->
                    val whenStr = Instant.ofEpochMilli(entry.createdAtEpochMs)
                        .atZone(ZoneId.systemDefault())
                        .format(dateFmt)
                    val xpColor = if (entry.amount >= 0) SystemSuccess else MaterialTheme.colorScheme.error
                    AccentLogCard(
                        accent = xpColor,
                        borderAccent = xpColor.copy(alpha = 0.35f),
                    ) {
                        LedgerEntryRow(
                            source = entry.sourceType.replace('_', ' '),
                            whenLabel = whenStr,
                            xpLabel = ledgerXpLabel(entry.amount),
                            xpColor = xpColor,
                        )
                    }
                }
                if (ledger.size > LEDGER_COLLAPSED_LIMIT) {
                    item {
                        GhostTextButton(
                            label = if (ledgerExpanded) "SHOW LESS" else "SHOW ALL (${ledger.size})",
                            onClick = { ledgerExpanded = !ledgerExpanded },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CharacterIdentityHero(
    name: String,
    level: Int,
    rank: String,
    totalXp: Int,
    xpInto: Int,
    xpNeed: Int,
    xpProgress: Float,
) {
    GlassSurface(modifier = Modifier.fillMaxWidth(), level = GlassLevel.Level2, cornerRadius = 16.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.hunter_silhouette),
                contentDescription = "Hunter avatar",
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    BracketLabel(text = "LVL $level", color = SystemPrimary)
                    RankBadge(rank = rank)
                }
                CyberProgressBar(progress = xpProgress, height = 8.dp)
                Text(
                    xpProgressLabel(xpInto, xpNeed),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMono,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Lifetime XP $totalXp",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMono,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SelfSectionCard(title: String, content: @Composable () -> Unit) {
    GlassSurface(modifier = Modifier.fillMaxWidth(), level = GlassLevel.Level1) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            SystemSectionHeader(tag = title)
            content()
        }
    }
}

@Composable
private fun StatLine(
    label: String,
    value: String,
    highlight: Boolean = false,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = JetBrainsMono,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = JetBrainsMono,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
            color = if (highlight) SystemPrimary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun AccentLogCard(
    accent: Color,
    borderAccent: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SystemSurface.copy(alpha = 0.4f))
            .border(1.dp, borderAccent, shape),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(3.dp)
                .heightIn(min = 56.dp)
                .fillMaxHeight()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(accent, accent.copy(alpha = 0f)),
                    ),
                ),
        )
        Box(Modifier.padding(start = 12.dp, top = 12.dp, end = 16.dp, bottom = 12.dp)) {
            content()
        }
    }
}

@Composable
private fun LedgerEntryRow(
    source: String,
    whenLabel: String,
    xpLabel: String,
    xpColor: Color,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                source,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                whenLabel,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = JetBrainsMono,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        BracketLabel(text = xpLabel, color = xpColor)
    }
}

internal fun visibleLedgerEntries(
    ledger: List<XpLedgerEntryEntity>,
    expanded: Boolean,
    collapsedLimit: Int,
): List<XpLedgerEntryEntity> =
    if (expanded || ledger.size <= collapsedLimit) ledger else ledger.take(collapsedLimit)

internal fun ledgerXpLabel(amount: Int): String =
    if (amount >= 0) "+$amount XP" else "$amount XP"
