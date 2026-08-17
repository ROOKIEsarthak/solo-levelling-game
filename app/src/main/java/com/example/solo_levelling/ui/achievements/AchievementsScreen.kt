package com.example.solo_levelling.ui.achievements

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.ui.theme.SystemSuccess

@Composable
fun AchievementsScreen(container: AppContainer) {
    val vm: AchievementsViewModel = viewModel(factory = AchievementsViewModel.factory(container))
    val defs by vm.defs.collectAsStateWithLifecycle()
    val unlocked by vm.unlocked.collectAsStateWithLifecycle()
    val unlockedKeys = unlocked.map { it.achievementKey }.toSet()
    val colors = MaterialTheme.colorScheme
    val unlockedCount = unlockedKeys.size

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Achievements", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "$unlockedCount / ${defs.size} unlocked",
            color = colors.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(defs, key = { it.key }) { def ->
                val isUnlocked = def.key in unlockedKeys
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isUnlocked) {
                            colors.surfaceContainer
                        } else {
                            colors.surfaceContainer.copy(alpha = 0.5f)
                        },
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isUnlocked) colors.primary.copy(alpha = 0.4f) else colors.outline,
                    ),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                def.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isUnlocked) colors.onSurface else colors.onSurfaceVariant,
                            )
                            Text(
                                if (isUnlocked) "UNLOCKED" else "LOCKED",
                                color = if (isUnlocked) SystemSuccess else colors.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                        Text(
                            "${def.key} · ${def.description}",
                            color = colors.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (!isUnlocked) {
                            Text(
                                "+${def.rewardXp} XP",
                                color = colors.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}
