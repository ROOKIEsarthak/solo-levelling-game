package com.example.solo_levelling.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import com.example.solo_levelling.AppContainer
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(container: AppContainer, onDone: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(setOf("career", "fitness", "discipline")) }
    val scope = rememberCoroutineScope()
    val options = listOf("career", "fitness", "discipline", "focus", "health")

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("SYSTEM INITIALIZATION", style = MaterialTheme.typography.headlineSmall)
        Text("Who is the Player?")
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Weekly priorities")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { opt ->
                FilterChip(
                    selected = opt in selected,
                    onClick = {
                        selected = if (opt in selected) selected - opt else selected + opt
                    },
                    label = { Text(opt) },
                )
            }
        }
        Button(
            onClick = {
                scope.launch {
                    container.onboarding.completeOnboarding(name, selected.toList())
                    onDone()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Initialize System")
        }
    }
}
