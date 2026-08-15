package com.example.solo_levelling.ui.levelup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.solo_levelling.AppContainer

@Composable
fun LevelUpHost(container: AppContainer) {
    val vm: LevelUpViewModel = viewModel(factory = LevelUpViewModel.factory(container))
    val event by vm.pendingEvent.collectAsStateWithLifecycle()

    val e = event ?: return
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (e) {
                    is LevelUpEvent.LevelUp -> {
                        Text("LEVEL UP", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("You reached level ${e.newLevel}!")
                    }
                    is LevelUpEvent.RankUp -> {
                        Text("RANK UP", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Rank promoted: ${e.oldRank} → ${e.newRank}")
                    }
                }
                TextButton(
                    onClick = { vm.dismiss() },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Continue")
                }
            }
        }
    }
}
