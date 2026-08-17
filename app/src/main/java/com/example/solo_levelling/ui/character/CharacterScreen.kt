package com.example.solo_levelling.ui.character

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.ui.theme.SystemSuccess
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun CharacterScreen(container: AppContainer) {
    val vm: CharacterViewModel = viewModel(factory = CharacterViewModel.factory(container))
    val profile by vm.profile.collectAsStateWithLifecycle()
    val attrs by vm.attributes.collectAsStateWithLifecycle()
    val ledger by vm.ledgerHistory.collectAsStateWithLifecycle()
    val streak by vm.streak.collectAsStateWithLifecycle()
    val currentRole by vm.currentRole.collectAsStateWithLifecycle()
    val careerYears by vm.careerYears.collectAsStateWithLifecycle()
    val targetRole by vm.targetRole.collectAsStateWithLifecycle()
    val careerNextGoal by vm.careerNextGoal.collectAsStateWithLifecycle()
    val heightCm by vm.heightCm.collectAsStateWithLifecycle()
    val weightKg by vm.weightKg.collectAsStateWithLifecycle()
    val bmiEstimate by vm.bmiEstimate.collectAsStateWithLifecycle()
    val fitnessGoal by vm.fitnessGoal.collectAsStateWithLifecycle()
    val modules by vm.enabledModules.collectAsStateWithLifecycle()
    val p = profile
    val xpInto = if (p == null) 0 else p.totalXp - SystemDefaults.totalXpForLevel(p.level)
    val need = if (p == null) 1 else SystemDefaults.xpForNextLevel(p.level)
    val dateFmt = DateTimeFormatter.ofPattern("MMM d, HH:mm")
    val colors = MaterialTheme.colorScheme

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Self Attributes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Read-only progression readout",
            color = colors.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
            border = BorderStroke(1.dp, colors.outline),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(p?.name ?: "Hunter", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "LEVEL ${p?.level ?: 1} · RANK ${p?.rank ?: "E"}",
                    color = colors.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Lifetime XP: ${p?.totalXp ?: 0}",
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                LinearProgressIndicator(
                    progress = { (xpInto.toFloat() / need).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = colors.primary,
                    trackColor = colors.surfaceContainerHighest,
                )
                Text(
                    "${need - xpInto} XP to level ${(p?.level ?: 1) + 1}",
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        if (modules.career) {
            SelfSectionCard(title = "CAREER") {
                if (currentRole.isNotBlank()) Text("Role: $currentRole")
                if (careerYears.isNotBlank()) Text("Experience: $careerYears years")
                if (targetRole.isNotBlank()) Text("Target: $targetRole")
                if (careerNextGoal.isNotBlank()) Text("Next goal: $careerNextGoal", fontWeight = FontWeight.Bold)
                if (currentRole.isBlank() && careerYears.isBlank() && targetRole.isBlank() && careerNextGoal.isBlank()) {
                    Text("No career profile yet", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (modules.workout || modules.diet) {
            SelfSectionCard(title = "FITNESS") {
                if (heightCm.isNotBlank()) Text("Height: ${heightCm} cm")
                if (weightKg.isNotBlank()) Text("Weight: ${weightKg} kg")
                if (bmiEstimate.isNotBlank()) Text("BMI (estimate): $bmiEstimate")
                if (modules.workout && fitnessGoal.isNotBlank()) {
                    Text("Goal: ${fitnessGoal.replace('_', ' ')}")
                }
                val hasBodyMetrics = heightCm.isNotBlank() || weightKg.isNotBlank() || bmiEstimate.isNotBlank()
                val hasWorkoutGoal = modules.workout && fitnessGoal.isNotBlank()
                if (!hasBodyMetrics && !hasWorkoutGoal) {
                    Text("No fitness profile yet", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        SelfSectionCard(title = "CONSISTENCY") {
            Text("Current streak: ${streak?.current ?: 0} days", fontWeight = FontWeight.Bold)
            Text("Best streak: ${streak?.best ?: 0} days", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
            border = BorderStroke(1.dp, colors.outline),
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("SYSTEM STATS", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                attrs.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { a ->
                            Column(
                                Modifier
                                    .weight(1f)
                                    .background(colors.surfaceContainerHigh, RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                            ) {
                                Text(a.code, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                Text("${a.currentValue}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("${a.lifetimeXp} XP", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (row.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
            border = BorderStroke(1.dp, colors.outline),
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("XP History", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                if (ledger.isEmpty()) {
                    Text("No XP recorded yet", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(ledger, key = { it.id }) { entry ->
                            val whenStr = Instant.ofEpochMilli(entry.createdAtEpochMs)
                                .atZone(ZoneId.systemDefault())
                                .format(dateFmt)
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    "${if (entry.amount >= 0) "+" else ""}${entry.amount} XP",
                                    color = SystemSuccess,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(entry.sourceType, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                Text(whenStr, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelfSectionCard(title: String, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
        border = BorderStroke(1.dp, colors.outline),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            content()
        }
    }
}
