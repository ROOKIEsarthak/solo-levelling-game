package com.example.solo_levelling.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(
    container: AppContainer,
    onOpenWorkout: () -> Unit,
    onOpenDiet: () -> Unit,
) {
    val vm: HistoryViewModel = viewModel(factory = HistoryViewModel.factory(container))
    val recentXp by vm.recentXp.collectAsStateWithLifecycle()
    val recentWorkouts by vm.recentWorkouts.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val dateFmt = DateTimeFormatter.ofPattern("MMM d, HH:mm")

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("History", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        HistoryCard(title = "Recent XP") {
            if (recentXp.isEmpty()) {
                Text("No XP entries yet.", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            } else {
                recentXp.forEach { entry ->
                    val whenStr = Instant.ofEpochMilli(entry.createdAtEpochMs)
                        .atZone(ZoneId.systemDefault())
                        .format(dateFmt)
                    Text(
                        "+${entry.amount} XP · ${entry.sourceType} · $whenStr",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        HistoryCard(title = "Quick links") {
            Text(
                "Open Workout history",
                color = colors.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onOpenWorkout),
            )
            Text(
                "Open Diet history",
                color = colors.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onOpenDiet),
            )
        }

        HistoryCard(title = "Recent workouts") {
            if (recentWorkouts.isEmpty()) {
                Text("No workouts logged yet.", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            } else {
                recentWorkouts.forEach { log ->
                    Text(
                        "${log.date} · ${log.workoutName.ifBlank { "Workout" }} · ${log.durationMinutes} min",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(title: String, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
        border = BorderStroke(1.dp, colors.outline),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}
