package com.example.solo_levelling.ui.more

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MoreScreen(
    onOpenMissions: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenSelf: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenLife: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "◈ SYSTEM",
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp,
            color = colors.primary,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            "More",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        MoreRow("Missions", "Daily quests and tasks", onOpenMissions)
        MoreRow("Progress", "Levels, before vs now, review", onOpenProgress)
        MoreRow("Self Attributes", "Attributes and XP ledger", onOpenSelf)
        MoreRow("History", "XP, workouts, and diet logs", onOpenHistory)
        MoreRow("Life", "Focus, journal, metrics", onOpenLife)
        MoreRow("Achievements", "Unlocks and titles", onOpenAchievements)
        MoreRow("Settings", "Config and reset", onOpenSettings)
    }
}

@Composable
private fun MoreRow(title: String, subtitle: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
        border = BorderStroke(1.dp, colors.outline),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}
