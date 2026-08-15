package com.example.solo_levelling.ui.achievements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.solo_levelling.AppContainer

@Composable
fun AchievementsScreen(container: AppContainer) {
    val vm: AchievementsViewModel = viewModel(factory = AchievementsViewModel.factory(container))
    val defs by vm.defs.collectAsStateWithLifecycle()
    val unlocked by vm.unlocked.collectAsStateWithLifecycle()
    val unlockedKeys = unlocked.map { it.achievementKey }.toSet()

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Achievements", style = MaterialTheme.typography.headlineSmall)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(defs, key = { it.key }) { def ->
                val isUnlocked = def.key in unlockedKeys
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(def.name, style = MaterialTheme.typography.titleMedium)
                        Text(def.description)
                        Text(if (isUnlocked) "UNLOCKED" else "LOCKED · +${def.rewardXp} XP")
                    }
                }
            }
        }
    }
}
