package com.example.solo_levelling.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.solo_levelling.ui.theme.CyanAura
import com.example.solo_levelling.ui.theme.JetBrainsMono
import com.example.solo_levelling.ui.theme.SovereignShape
import com.example.solo_levelling.ui.theme.Spacing
import com.example.solo_levelling.ui.theme.SystemPrimary
import com.example.solo_levelling.ui.theme.SystemPrimaryContainer
import com.example.solo_levelling.ui.theme.SystemSecondary
import com.example.solo_levelling.ui.theme.SystemSuccess
import com.example.solo_levelling.ui.theme.SystemSurface
import com.example.solo_levelling.ui.theme.SystemSurface2
import com.example.solo_levelling.ui.theme.SystemTertiary

enum class GlassLevel { Level1, Level2 }

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    level: GlassLevel = GlassLevel.Level1,
    borderAlpha: Float = if (level == GlassLevel.Level2) 0.14f else 0.10f,
    cornerRadius: Dp = 12.dp,
    contentPadding: Dp = Spacing.md,
    content: @Composable () -> Unit,
) {
    val bg = when (level) {
        GlassLevel.Level1 -> SystemSurface.copy(alpha = 0.4f)
        GlassLevel.Level2 -> SystemSurface2.copy(alpha = 0.8f)
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(bg)
            .border(1.dp, SystemPrimary.copy(alpha = borderAlpha), RoundedCornerShape(cornerRadius))
            .padding(contentPadding),
    ) {
        content()
    }
}

@Composable
fun SystemSectionHeader(
    tag: String,
    modifier: Modifier = Modifier,
    accent: Color = SystemPrimary,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(16.dp)
                .background(accent),
        )
        Spacer(Modifier.width(Spacing.xs))
        Text(
            text = tag,
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun SystemActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    enabled: Boolean = true,
    bracketed: Boolean = false,
) {
    val text = displayLabel(label, bracketed)
    if (primary) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier,
            shape = SovereignShape.button,
            colors = ButtonDefaults.buttonColors(
                containerColor = SystemPrimaryContainer,
                contentColor = Color(0xFF05070D),
                disabledContainerColor = SystemPrimaryContainer.copy(alpha = 0.4f),
            ),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier,
            shape = SovereignShape.button,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = SystemPrimary),
            border = BorderStroke(1.dp, SystemPrimary.copy(alpha = 0.5f)),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun CyberProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp,
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(SovereignShape.progress)
            .background(SystemSurface.copy(alpha = 0.5f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(clamped)
                .height(height)
                .background(
                    Brush.horizontalGradient(
                        listOf(SystemPrimaryContainer, SystemSecondary),
                    ),
                ),
        ) {
            if (clamped > 0.02f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(height)
                        .background(Color.White.copy(alpha = 0.75f), CircleShape),
                )
            }
        }
    }
}

@Composable
fun SystemIdleEmpty(
    title: String = "Nothing here yet",
    subtitle: String = "Your next step will appear here.",
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    GlassSurface(modifier = modifier.fillMaxWidth(), level = GlassLevel.Level1) {
        Column(horizontalAlignment = Alignment.Start, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(Spacing.md))
                SystemActionButton(label = actionLabel, onClick = onAction, primary = false)
            }
        }
    }
}

@Composable
fun BracketLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = SystemPrimary,
) {
    Text(
        text = bracketize(text),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        fontFamily = JetBrainsMono,
        color = color,
    )
}

@Composable
fun GhostTextButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(
            text = displayLabel(label, bracketed = false),
            style = MaterialTheme.typography.labelMedium,
            color = SystemPrimary,
        )
    }
}

@Composable
fun EnergyFieldBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        CyanAura,
                        SystemSecondary.copy(alpha = 0.12f),
                        Color(0xFF05070D),
                    ),
                ),
            ),
    )
}

@Composable
fun RankBadge(
    rank: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "Rank $rank",
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        fontFamily = JetBrainsMono,
        color = SystemSecondary,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
fun PlayerHeader(
    name: String,
    level: Int,
    rank: String,
    xpIntoLevel: Int,
    xpNeed: Int,
    modifier: Modifier = Modifier,
    greeting: String? = null,
    activeModulesLabel: String? = null,
) {
    val progress = progressFraction(xpIntoLevel.toFloat(), xpNeed.toFloat())
    GlassSurface(modifier = modifier.fillMaxWidth(), level = GlassLevel.Level2, cornerRadius = 16.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            if (greeting != null) {
                Text(
                    greeting,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "Level $level · Rank $rank",
                style = MaterialTheme.typography.titleSmall,
                fontFamily = JetBrainsMono,
                color = SystemPrimary,
            )
            if (!activeModulesLabel.isNullOrBlank()) {
                Text(
                    "Based on: $activeModulesLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            CyberProgressBar(progress = progress, height = 8.dp)
            Text(
                xpToNextLabel(xpIntoLevel, xpNeed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun AttributeRow(
    code: String,
    value: Int,
    fraction: Float,
    modifier: Modifier = Modifier,
    lifetimeXp: Int? = null,
    detailed: Boolean = false,
) {
    val presentation = attributePresentation(code)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    presentation.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (detailed) presentation.meaning else presentation.cues,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (detailed) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    value.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                )
                if (lifetimeXp != null) {
                    Text(
                        "XP $lifetimeXp",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMono,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        CyberProgressBar(progress = fraction, height = 6.dp)
    }
}

@Composable
fun AttributeSummary(
    displays: List<AttributeDisplay>,
    insight: AttributeInsight,
    modifier: Modifier = Modifier,
    compactLimit: Int = 4,
    showAll: Boolean = false,
    sectionTag: String = "YOUR CHARACTER",
    onViewCharacter: (() -> Unit)? = null,
) {
    val rows = if (showAll) displays else topAttributeDisplays(displays, compactLimit)
    GlassSurface(modifier = modifier.fillMaxWidth(), level = GlassLevel.Level1) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            SystemSectionHeader(tag = sectionTag)
            if (rows.isEmpty()) {
                Text(
                    "Attributes will appear as you progress.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                rows.forEach { attr ->
                    AttributeRow(code = attr.code, value = attr.value, fraction = attr.fraction)
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
            if (onViewCharacter != null) {
                GhostTextButton(label = "View character", onClick = onViewCharacter)
            }
        }
    }
}

@Composable
fun StreakIndicator(
    current: Int,
    modifier: Modifier = Modifier,
    best: Int? = null,
) {
    GlassSurface(modifier = modifier.fillMaxWidth(), level = GlassLevel.Level1, contentPadding = Spacing.sm) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "$current DAY${if (current == 1) "" else "S"}",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                color = SystemTertiary,
            )
            Text(
                streakSupportCopy(current),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (best != null && best > 0) {
                Text(
                    "Best $best",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMono,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun SovereignChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = if (selected) {
        SystemPrimary.copy(alpha = 0.15f)
    } else {
        SystemSurface.copy(alpha = 0.35f)
    }
    val borderColor = if (selected) SystemPrimary else SystemPrimary.copy(alpha = 0.25f)
    val textColor = if (selected) SystemPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Text(
        text = label,
        modifier = modifier
            .clip(SovereignShape.chip)
            .background(background)
            .border(BorderStroke(1.dp, borderColor), SovereignShape.chip)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelMedium,
        fontFamily = JetBrainsMono,
        color = textColor,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
    )
}

@Composable
fun MissionQuestCard(
    type: String,
    title: String,
    baseXp: Int,
    status: String,
    rewardsJson: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    verificationType: String = "MANUAL",
    verificationTarget: Float = 0f,
    verificationUnit: String = "",
    primaryLabel: String = "Complete",
    deemphasized: Boolean = false,
    onUndo: (() -> Unit)? = null,
) {
    val completed = status == "COMPLETED"
    val locked = status == "LOCKED"
    val rank = questRankForXp(baseXp)
    val rewardLine = formatAttributeRewards(rewardsJson)
    val statusColor = when {
        completed -> SystemSuccess
        locked -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> SystemPrimary
    }
    val cardAlpha = if (deemphasized || completed) 0.72f else 1f

    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .alpha(cardAlpha),
        level = GlassLevel.Level1,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BracketLabel(text = type.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        RankBadge(rank = rank)
                    }
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BracketLabel(text = "+$baseXp XP", color = SystemTertiary)
            }
            if (rewardLine.isNotBlank()) {
                Text(
                    rewardLine,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMono,
                    color = SystemSecondary,
                )
            }
            if (verificationType != "MANUAL" && !completed) {
                Text(
                    "Verify: $verificationType ${verificationTarget.toInt()} $verificationUnit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (completed) "Completed" else if (locked) "Locked" else "Active",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMono,
                    color = if (completed) statusColor.copy(alpha = 0.75f) else statusColor,
                    fontWeight = FontWeight.Medium,
                )
                when {
                    !completed && !locked -> SystemActionButton(label = primaryLabel, onClick = onPrimary)
                    completed && onUndo != null -> SystemActionButton(label = "Undo", onClick = onUndo, primary = false)
                }
            }
        }
    }
}

@Composable
fun LoadingSkeleton(
    modifier: Modifier = Modifier,
    lines: Int = 3,
) {
    GlassSurface(modifier = modifier.fillMaxWidth(), level = GlassLevel.Level1) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            repeat(lines.coerceAtLeast(1)) { index ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (index == lines - 1) 0.55f else 1f)
                        .height(12.dp)
                        .clip(SovereignShape.progress)
                        .background(SystemPrimary.copy(alpha = 0.12f)),
                )
            }
        }
    }
}

@Composable
fun TodayProgressStrip(
    questsDone: Int,
    questsTotal: Int,
    xpLabel: String,
    streakDays: Int,
    modifier: Modifier = Modifier,
) {
    val fraction = progressFraction(questsDone.toFloat(), questsTotal.toFloat().coerceAtLeast(1f))
    GlassSurface(modifier = modifier.fillMaxWidth(), level = GlassLevel.Level1) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            SystemSectionHeader(tag = "TODAY")
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    if (questsTotal == 0) {
                        "No active priorities"
                    } else {
                        "$questsDone / $questsTotal COMPLETED"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = JetBrainsMono,
                )
                Text(
                    xpLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = JetBrainsMono,
                    color = SystemTertiary,
                )
            }
            CyberProgressBar(progress = if (questsTotal == 0) 0f else fraction, height = 8.dp)
            Text(
                streakSupportCopy(streakDays),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (streakDays > 0) {
                BracketLabel(text = "$streakDays day streak", color = SystemTertiary)
            }
        }
    }
}
