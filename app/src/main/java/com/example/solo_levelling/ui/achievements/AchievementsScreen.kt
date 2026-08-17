package com.example.solo_levelling.ui.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.data.db.entity.AchievementDefEntity
import com.example.solo_levelling.ui.components.BracketLabel
import com.example.solo_levelling.ui.components.EnergyFieldBackground
import com.example.solo_levelling.ui.components.GlassLevel
import com.example.solo_levelling.ui.components.GlassSurface
import com.example.solo_levelling.ui.components.SystemIdleEmpty
import com.example.solo_levelling.ui.components.SystemSectionHeader
import com.example.solo_levelling.ui.theme.GlowCyan
import com.example.solo_levelling.ui.theme.JetBrainsMono
import com.example.solo_levelling.ui.theme.Spacing
import com.example.solo_levelling.ui.theme.SystemPrimary
import com.example.solo_levelling.ui.theme.SystemSuccess
import com.example.solo_levelling.ui.theme.SystemSurface

/** Hero completion fraction: unlocked / total defs. */
internal fun achievementsCompletionFraction(unlockedCount: Int, totalCount: Int): String =
    "$unlockedCount/$totalCount"

@Composable
fun AchievementsScreen(container: AppContainer) {
    val vm: AchievementsViewModel = viewModel(factory = AchievementsViewModel.factory(container))
    val defs by vm.defs.collectAsStateWithLifecycle()
    val unlocked by vm.unlocked.collectAsStateWithLifecycle()
    val unlockedKeys = unlocked.map { it.achievementKey }.toSet()
    val colors = MaterialTheme.colorScheme
    val unlockedCount = unlockedKeys.size
    val completion = achievementsCompletionFraction(unlockedCount, defs.size)

    Box(Modifier.fillMaxSize()) {
        EnergyFieldBackground(Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxSize()
                .padding(Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.section),
        ) {
            GlassSurface(modifier = Modifier.fillMaxWidth(), level = GlassLevel.Level1) {
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                        SystemSectionHeader(tag = "ACHIEVEMENTS")
                        Text(
                            "Unlocked badges and rewards from your progress",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                GlassSurface(
                    modifier = Modifier.align(Alignment.End),
                    level = GlassLevel.Level2,
                    cornerRadius = 8.dp,
                ) {
                    Column(
                        modifier = Modifier.semantics {
                            contentDescription = "Total completion $completion"
                        },
                        horizontalAlignment = Alignment.End,
                    ) {
                        Text(
                            text = "TOTAL COMPLETION",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = JetBrainsMono,
                            color = SystemPrimary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = unlockedCount.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Bold,
                                color = SystemPrimary,
                                maxLines = 1,
                                softWrap = false,
                            )
                            Text(
                                text = "/${defs.size}",
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = JetBrainsMono,
                                color = colors.outline,
                                maxLines = 1,
                                softWrap = false,
                            )
                        }
                    }
                }
            }
        }

            SystemSectionHeader(tag = "BADGE GRID")

            if (defs.isEmpty()) {
                SystemIdleEmpty(
                    title = "NO ACHIEVEMENTS",
                    subtitle = "Achievement definitions will appear once configured.",
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(defs, key = { it.key }) { def ->
                        val isUnlocked = def.key in unlockedKeys
                        AchievementBadge(def = def, isUnlocked = isUnlocked)
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementBadge(
    def: AchievementDefEntity,
    isUnlocked: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    val borderColor = if (isUnlocked) GlowCyan.copy(alpha = 0.65f) else colors.outline.copy(alpha = 0.4f)
    val bgAlpha = if (isUnlocked) 0.8f else 0.4f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .alpha(if (isUnlocked) 1f else 0.55f)
            .clip(shape)
            .background(SystemSurface.copy(alpha = bgAlpha))
            .border(
                width = if (isUnlocked) 1.5.dp else 1.dp,
                color = borderColor,
                shape = shape,
            )
            .padding(12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isUnlocked) {
                            SystemPrimary.copy(alpha = 0.15f)
                        } else {
                            colors.surfaceContainerHighest.copy(alpha = 0.5f)
                        },
                    )
                    .border(
                        width = if (isUnlocked) 2.dp else 1.dp,
                        color = if (isUnlocked) GlowCyan.copy(alpha = 0.5f) else colors.outline,
                        shape = RoundedCornerShape(50),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    def.key.take(2),
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) GlowCyan else colors.onSurfaceVariant,
                )
            }
            Text(
                def.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isUnlocked) colors.onSurface else colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            BracketLabel(
                text = if (isUnlocked) "UNLOCKED" else "LOCKED",
                color = if (isUnlocked) SystemSuccess else colors.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Text(
                def.description,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 3,
            )
            if (!isUnlocked && def.rewardXp > 0) {
                Text(
                    "+${def.rewardXp} XP",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = JetBrainsMono,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}
