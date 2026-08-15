package com.example.solo_levelling.ui.character

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.core.config.SystemDefaults

@Composable
fun CharacterScreen(container: AppContainer) {
    val vm: CharacterViewModel = viewModel(factory = CharacterViewModel.factory(container))
    val profile by vm.profile.collectAsStateWithLifecycle()
    val attrs by vm.attributes.collectAsStateWithLifecycle()
    val p = profile
    val xpInto = if (p == null) 0 else p.totalXp - SystemDefaults.totalXpForLevel(p.level)
    val need = if (p == null) 1 else SystemDefaults.xpForNextLevel(p.level)

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Character", style = MaterialTheme.typography.headlineSmall)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(p?.name ?: "Hunter", style = MaterialTheme.typography.titleLarge)
                Text("Level ${p?.level ?: 1} · Rank ${p?.rank ?: "E"}")
                Text("Lifetime XP: ${p?.totalXp ?: 0}")
                LinearProgressIndicator(
                    progress = { (xpInto.toFloat() / need).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("$xpInto / $need to next level")
            }
        }
        Text("Attributes", style = MaterialTheme.typography.titleMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(attrs, key = { it.code }) { a ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${a.code}: ${a.currentValue}")
                        Text("Lifetime attribute XP: ${a.lifetimeXp}")
                    }
                }
            }
        }
    }
}
