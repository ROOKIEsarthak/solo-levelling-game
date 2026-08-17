package com.example.solo_levelling.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solo_levelling.ui.components.EnergyFieldBackground
import com.example.solo_levelling.ui.components.GlassLevel
import com.example.solo_levelling.ui.components.GlassSurface
import com.example.solo_levelling.ui.components.SystemSectionHeader
import com.example.solo_levelling.ui.theme.JetBrainsMono
import com.example.solo_levelling.ui.theme.SystemError
import com.example.solo_levelling.ui.theme.SystemPrimary
import com.example.solo_levelling.ui.theme.SystemSecondary
import com.example.solo_levelling.ui.theme.SystemTertiary

@Composable
fun MoreScreen(
    onOpenMissions: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenSelf: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenLife: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCareer: () -> Unit = {},
    onOpenWorkout: () -> Unit = {},
    onOpenNutrition: () -> Unit = {},
    showCareer: Boolean = true,
    showWorkout: Boolean = true,
    showDiet: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize()) {
        EnergyFieldBackground(Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "MORE",
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    color = SystemPrimary,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Everything else in your system",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }

            HubSection(
                tag = "[ GROW ]",
                accent = SystemTertiary,
            ) {
                HubRow("Quests", "Daily missions and objectives", onOpenMissions)
                if (showCareer) {
                    HubRow("Career", "DSA, system design, and skill tracks", onOpenCareer)
                }
                HubRow("Focus & Journal", "Deep work, journal, and life metrics", onOpenLife)
            }

            HubSection(
                tag = "[ BODY ]",
                accent = SystemError,
            ) {
                if (showWorkout) {
                    HubRow("Fitness", "Training sessions and exercise logs", onOpenWorkout)
                }
                if (showDiet) {
                    HubRow("Nutrition", "Meals, macros, and calorie targets", onOpenNutrition)
                }
            }

            HubSection(
                tag = "[ SYSTEM ]",
                accent = SystemSecondary,
            ) {
                HubRow("Settings", "Configure your system", onOpenSettings)
                HubRow("History", "Recent XP and activity", onOpenHistory)
                HubRow("Achievements", "Unlocks and milestones", onOpenAchievements)
                HubRow("Progress", "Levels, analytics, and review", onOpenProgress)
                HubRow("Character", "Attributes and XP ledger", onOpenSelf)
            }
        }
    }
}

@Composable
private fun HubSection(
    tag: String,
    accent: androidx.compose.ui.graphics.Color,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SystemSectionHeader(tag = tag, accent = accent)
        content()
    }
}

@Composable
private fun HubRow(title: String, subtitle: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        level = GlassLevel.Level1,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = subtitle,
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
