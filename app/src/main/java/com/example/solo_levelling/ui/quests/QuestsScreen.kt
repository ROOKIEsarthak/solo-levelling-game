package com.example.solo_levelling.ui.quests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.domain.model.QuestStatus
import kotlinx.coroutines.launch

@Composable
fun QuestsScreen(container: AppContainer) {
    val vm: QuestsViewModel = viewModel(factory = QuestsViewModel.factory(container))
    val quests by vm.quests.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Quests", style = MaterialTheme.typography.headlineSmall)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(quests, key = { it.id }) { q ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(q.title, style = MaterialTheme.typography.titleMedium)
                        Text("${q.type} · +${q.baseXp} XP · ${q.status}")
                        Row {
                            if (q.status != QuestStatus.COMPLETED.name) {
                                Button(onClick = { scope.launch { container.questCompletion.complete(q.id) } }) {
                                    Text("Complete")
                                }
                            } else {
                                TextButton(onClick = { scope.launch { container.questCompletion.undo(q.id) } }) {
                                    Text("Undo")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
