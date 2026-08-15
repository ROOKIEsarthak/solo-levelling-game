package com.example.solo_levelling.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.core.config.SystemDefaults
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(container: AppContainer) {
    val profile by container.db.playerDao().observeProfile(SystemDefaults.PLAYER_ID)
        .collectAsStateWithLifecycle(initialValue = null)
    var name by remember(profile?.name) { mutableStateOf(profile?.name ?: "") }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Player name") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = {
            scope.launch {
                val p = container.db.playerDao().getProfile(SystemDefaults.PLAYER_ID) ?: return@launch
                container.db.playerDao().upsertProfile(p.copy(name = name.ifBlank { p.name }))
            }
        }) { Text("Save") }

        Button(onClick = {
            scope.launch {
                val p = container.db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
                container.questGeneration.generateForToday(p?.timezone ?: "Asia/Kolkata")
            }
        }) { Text("Regenerate today's quests") }

        Text(
            "Daily XP cap: ${SystemDefaults.DAILY_XP_CAP}\n" +
                "Undo window: ${SystemDefaults.QUEST_UNDO_MINUTES}m\n" +
                "Recovery/week: ${SystemDefaults.WEEKLY_RECOVERY_LIMIT}",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
