package com.example.solo_levelling.ui.onboarding

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(setOf("career", "fitness", "discipline")) }
    var scheduleDays by remember { mutableStateOf(setOf("MON", "TUE", "WED", "THU", "FRI")) }
    val scope = rememberCoroutineScope()
    val priorityOptions = listOf("career", "fitness", "discipline", "focus", "health")
    val dayOptions = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("SYSTEM INITIALIZATION", style = MaterialTheme.typography.headlineSmall)
        when (step) {
            0 -> {
                Text("Step 1 — Who is the Player?")
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            1 -> {
                Text("Step 2 — Weekly priorities")
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    priorityOptions.forEach { opt ->
                        FilterChip(
                            selected = opt in selected,
                            onClick = {
                                selected = if (opt in selected) selected - opt else selected + opt
                            },
                            label = { Text(opt) },
                        )
                    }
                }
            }
            else -> {
                Text("Step 3 — Schedule days")
                Text("Select active days for career and fitness quests.")
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    dayOptions.forEach { day ->
                        FilterChip(
                            selected = day in scheduleDays,
                            onClick = {
                                scheduleDays = if (day in scheduleDays) scheduleDays - day else scheduleDays + day
                            },
                            label = { Text(day) },
                        )
                    }
                }
            }
        }
        if (step > 0) {
            OutlinedButton(onClick = { step -= 1 }, modifier = Modifier.fillMaxWidth()) {
                Text("Back")
            }
        }
        Button(
            onClick = {
                when (step) {
                    0 -> if (name.isNotBlank()) step = 1
                    1 -> if (selected.isNotEmpty()) step = 2
                    else -> scope.launch {
                        container.onboarding.completeOnboarding(
                            name,
                            selected.toList(),
                            scheduleDays.toList(),
                        )
                        onDone()
                    }
                }
            },
            enabled = when (step) {
                0 -> name.isNotBlank()
                1 -> selected.isNotEmpty()
                else -> scheduleDays.isNotEmpty()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (step < 2) "Next" else "Initialize System")
        }
    }
}
