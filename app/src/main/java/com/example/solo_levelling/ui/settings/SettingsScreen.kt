package com.example.solo_levelling.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.data.db.entity.UserConfigEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    container: AppContainer,
    onResetComplete: () -> Unit = {},
) {
    val profile by container.db.playerDao().observeProfile(SystemDefaults.PLAYER_ID)
        .collectAsStateWithLifecycle(initialValue = null)
    val outbox by container.db.outboxDao().observeRecent(5)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val calorieConfig by container.db.configDao().observe("calorie_target")
        .collectAsStateWithLifecycle(initialValue = null)
    val proteinConfig by container.db.configDao().observe("protein_target")
        .collectAsStateWithLifecycle(initialValue = null)
    val stepConfig by container.db.configDao().observe("step_target")
        .collectAsStateWithLifecycle(initialValue = null)
    val notificationsConfig by container.db.configDao().observe("notifications_enabled")
        .collectAsStateWithLifecycle(initialValue = null)
    val scheduleDaysConfig by container.db.configDao().observe("schedule_days_csv")
        .collectAsStateWithLifecycle(initialValue = null)
    val goalTitleConfig by container.db.configDao().observe("goal_title")
        .collectAsStateWithLifecycle(initialValue = null)

    var name by remember(profile?.name) { mutableStateOf(profile?.name ?: "") }
    var calorieTarget by remember(calorieConfig?.value) { mutableStateOf(calorieConfig?.value ?: "2200") }
    var proteinTarget by remember(proteinConfig?.value) { mutableStateOf(proteinConfig?.value ?: "150") }
    var stepTarget by remember(stepConfig?.value) { mutableStateOf(stepConfig?.value ?: "10000") }
    var scheduleDays by remember(scheduleDaysConfig?.value) {
        mutableStateOf(scheduleDaysConfig?.value ?: "1,2,3,4,5,6,7")
    }
    var goalTitle by remember(goalTitleConfig?.value) { mutableStateOf(goalTitleConfig?.value ?: "") }
    var rebuildResult by remember { mutableStateOf<String?>(null) }
    var saveMessage by remember { mutableStateOf<String?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val notificationsOn = notificationsConfig?.value != "false"

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset all progress?") },
            text = {
                Text(
                    "This clears XP, quests, streaks, achievements, and module logs. " +
                        "Your name and settings are kept.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirm = false
                        scope.launch(Dispatchers.IO) {
                            container.onboarding.resetAllProgress()
                            withContext(Dispatchers.Main) {
                                onResetComplete()
                            }
                        }
                    },
                ) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            },
        )
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Player name") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = goalTitle,
            onValueChange = { goalTitle = it },
            label = { Text("Goal title (vision)") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = calorieTarget,
            onValueChange = { calorieTarget = it.filter { c -> c.isDigit() } },
            label = { Text("Calorie target") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = proteinTarget,
            onValueChange = { proteinTarget = it.filter { c -> c.isDigit() } },
            label = { Text("Protein target (g)") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = stepTarget,
            onValueChange = { stepTarget = it.filter { c -> c.isDigit() } },
            label = { Text("Step target") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = scheduleDays,
            onValueChange = { scheduleDays = it },
            label = { Text("Schedule days (1=Mon … 7=Sun, CSV)") },
            modifier = Modifier.fillMaxWidth(),
        )

        Text("Notifications", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = {
                scope.launch {
                    container.db.configDao().upsert(UserConfigEntity("notifications_enabled", "true"))
                }
            }) { Text(if (notificationsOn) "On ✓" else "On") }
            TextButton(onClick = {
                scope.launch {
                    container.db.configDao().upsert(UserConfigEntity("notifications_enabled", "false"))
                }
            }) { Text(if (!notificationsOn) "Off ✓" else "Off") }
        }

        Button(onClick = {
            scope.launch {
                val p = container.db.playerDao().getProfile(SystemDefaults.PLAYER_ID) ?: return@launch
                container.db.playerDao().upsertProfile(p.copy(name = name.ifBlank { p.name }))
                container.db.configDao().upsert(UserConfigEntity("calorie_target", calorieTarget.ifBlank { "2200" }))
                container.db.configDao().upsert(UserConfigEntity("protein_target", proteinTarget.ifBlank { "150" }))
                container.db.configDao().upsert(UserConfigEntity("step_target", stepTarget.ifBlank { "10000" }))
                container.db.configDao().upsert(UserConfigEntity("schedule_days_csv", scheduleDays))
                container.db.configDao().upsert(UserConfigEntity("goal_title", goalTitle))
                saveMessage = "Saved"
            }
        }) { Text("Save config") }
        saveMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }

        Button(onClick = {
            scope.launch {
                val p = container.db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
                container.questGeneration.generateForToday(p?.timezone ?: "Asia/Kolkata")
            }
        }) { Text("Regenerate today's quests") }

        Button(onClick = {
            scope.launch {
                val result = container.progression.rebuildFromLedger()
                rebuildResult = "XP ${result.oldTotal} → ${result.newTotal}, " +
                    "Level ${result.oldLevel} → ${result.newLevel}, " +
                    "Rank ${result.oldRank} → ${result.newRank}"
            }
        }) { Text("Rebuild XP from ledger") }

        rebuildResult?.let { Text(it, style = MaterialTheme.typography.bodySmall) }

        Button(onClick = {
            scope.launch {
                val json = container.analytics.exportJson()
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Solo Levelling export")
                    putExtra(Intent.EXTRA_TEXT, json)
                }
                context.startActivity(Intent.createChooser(share, "Export data"))
            }
        }) { Text("Export data") }

        Button(
            onClick = { showResetConfirm = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) { Text("Reset all progress") }

        Text("Recent outbox events:", style = MaterialTheme.typography.titleSmall)
        if (outbox.isEmpty()) {
            Text("None yet", style = MaterialTheme.typography.bodySmall)
        } else {
            outbox.forEach { entry ->
                Text("${entry.eventType} @ ${entry.createdAtEpochMs}", style = MaterialTheme.typography.bodySmall)
            }
        }

        Text("System caps (read-only)", style = MaterialTheme.typography.titleSmall)
        Text(
            "Daily XP cap: ${SystemDefaults.DAILY_XP_CAP}\n" +
                "Undo window: ${SystemDefaults.QUEST_UNDO_MINUTES}m\n" +
                "Recovery/week: ${SystemDefaults.WEEKLY_RECOVERY_LIMIT}\n" +
                "Streak grace days: ${SystemDefaults.STREAK_GRACE_DAYS}\n" +
                "Level base: ${SystemDefaults.LEVEL_BASE}\n" +
                "Level exponent: ${SystemDefaults.LEVEL_EXPONENT}",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
