package com.example.solo_levelling.ui.levelup

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.solo_levelling.AppContainer

@Composable
fun LevelUpHost(container: AppContainer) {
    val vm: LevelUpViewModel = viewModel(factory = LevelUpViewModel.factory(container))
    val event by vm.pendingEvent.collectAsStateWithLifecycle()

    when (val e = event) {
        is LevelUpEvent.LevelUp -> {
            AlertDialog(
                onDismissRequest = { vm.dismiss() },
                title = { Text("LEVEL UP", fontWeight = FontWeight.Bold) },
                text = { Text("You reached level ${e.newLevel}!") },
                confirmButton = {
                    TextButton(onClick = { vm.dismiss() }) { Text("Continue") }
                },
            )
        }
        is LevelUpEvent.RankUp -> {
            AlertDialog(
                onDismissRequest = { vm.dismiss() },
                title = { Text("RANK UP", fontWeight = FontWeight.Bold) },
                text = { Text("Rank promoted: ${e.oldRank} → ${e.newRank}") },
                confirmButton = {
                    TextButton(onClick = { vm.dismiss() }) { Text("Continue") }
                },
            )
        }
        null -> Unit
    }
}
