package com.example.solo_levelling.ui.modules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.solo_levelling.AppContainer
import kotlinx.coroutines.launch

@Composable
fun ModulesScreen(container: AppContainer) {
    val vm: ModulesViewModel = viewModel(factory = ModulesViewModel.factory(container))
    val dsa by vm.dsa.collectAsStateWithLifecycle()
    val workouts by vm.workouts.collectAsStateWithLifecycle()
    val bosses by vm.bosses.collectAsStateWithLifecycle()
    val skills by vm.skills.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var dsaTitle by remember { mutableStateOf("") }
    var journal by remember { mutableStateOf("") }
    var bossTitle by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Life Modules", style = MaterialTheme.typography.headlineSmall)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Career · DSA", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = dsaTitle,
                    onValueChange = { dsaTitle = it },
                    label = { Text("Problem title") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = {
                    scope.launch {
                        if (dsaTitle.isNotBlank()) {
                            container.modules.addDsaProblem(dsaTitle, "MEDIUM", "DSA")
                            dsaTitle = ""
                        }
                    }
                }) { Text("Add problem") }
                dsa.take(5).forEach { p ->
                    Text("${p.title} · ${p.status}")
                    if (p.status != "SOLVED" && p.status != "MASTERED") {
                        Button(onClick = { scope.launch { container.modules.solveDsa(p.id) } }) {
                            Text("Mark solved (+XP)")
                        }
                    }
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Fitness", style = MaterialTheme.typography.titleMedium)
                Button(onClick = { scope.launch { container.modules.logWorkout("Push", 45) } }) {
                    Text("Log 45m workout")
                }
                Button(onClick = {
                    scope.launch { container.modules.logNutrition(2200, 160, 200, 70) }
                }) { Text("Log nutrition") }
                Text("${workouts.size} workouts logged")
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Focus", style = MaterialTheme.typography.titleMedium)
                Button(onClick = { scope.launch { container.modules.logFocus(45, "Deep Work") } }) {
                    Text("Log 45m focus")
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Journal", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = journal,
                    onValueChange = { journal = it },
                    label = { Text("Reflection") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = {
                    scope.launch {
                        if (journal.isNotBlank()) {
                            container.modules.saveJournal(journal)
                            journal = ""
                        }
                    }
                }) { Text("Save journal (+WIS)") }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Boss Quests", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = bossTitle,
                    onValueChange = { bossTitle = it },
                    label = { Text("Boss title") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = {
                    scope.launch {
                        if (bossTitle.isNotBlank()) {
                            container.modules.createBoss(bossTitle, "Major objective", 200)
                            bossTitle = ""
                        }
                    }
                }) { Text("Create boss") }
                Button(onClick = { scope.launch { container.modules.addBossProgress(25f) } }) {
                    Text("Add +25% boss progress")
                }
                bosses.forEach { b ->
                    Text("${b.title}: ${b.currentValue}/${b.targetValue} · ${b.status}")
                }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Skills", style = MaterialTheme.typography.titleMedium)
                skills.forEach { s ->
                    Text("${s.domain}/${s.name} · Lv ${s.level} (${s.xp} XP)")
                }
                if (skills.isEmpty()) Text("Skills unlock from DSA and modules.")
            }
        }
    }
}
