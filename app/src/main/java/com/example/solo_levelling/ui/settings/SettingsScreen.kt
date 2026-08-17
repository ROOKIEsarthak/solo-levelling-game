package com.example.solo_levelling.ui.settings

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.data.db.entity.UserConfigEntity
import com.example.solo_levelling.data.seed.WorkoutCatalog
import com.example.solo_levelling.domain.service.EnabledModules
import com.example.solo_levelling.domain.service.EntryValidation
import com.example.solo_levelling.domain.service.ModuleFlags
import com.example.solo_levelling.domain.service.WorkoutSplitLogic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    container: AppContainer,
    onMessage: (String) -> Unit = {},
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
    val carbConfig by container.db.configDao().observe("carb_target")
        .collectAsStateWithLifecycle(initialValue = null)
    val fatConfig by container.db.configDao().observe("fat_target")
        .collectAsStateWithLifecycle(initialValue = null)
    val stepConfig by container.db.configDao().observe("step_target")
        .collectAsStateWithLifecycle(initialValue = null)
    val notificationsConfig by container.db.configDao().observe("notifications_enabled")
        .collectAsStateWithLifecycle(initialValue = null)
    val scheduleDaysConfig by container.db.configDao().observe("schedule_days_csv")
        .collectAsStateWithLifecycle(initialValue = null)
    val workoutSplitConfig by container.db.configDao().observe("workout_split_id")
        .collectAsStateWithLifecycle(initialValue = null)
    val workoutSplitMapConfig by container.db.configDao().observe("workout_split_map")
        .collectAsStateWithLifecycle(initialValue = null)
    val goalTitleConfig by container.db.configDao().observe("goal_title")
        .collectAsStateWithLifecycle(initialValue = null)
    val moduleCareerConfig by container.db.configDao().observe(ModuleFlags.KEY_CAREER)
        .collectAsStateWithLifecycle(initialValue = null)
    val moduleWorkoutConfig by container.db.configDao().observe(ModuleFlags.KEY_WORKOUT)
        .collectAsStateWithLifecycle(initialValue = null)
    val moduleDietConfig by container.db.configDao().observe(ModuleFlags.KEY_DIET)
        .collectAsStateWithLifecycle(initialValue = null)

    var name by remember(profile?.name) { mutableStateOf(profile?.name ?: "") }
    var calorieTarget by remember(calorieConfig?.value) { mutableStateOf(calorieConfig?.value ?: "2200") }
    var proteinTarget by remember(proteinConfig?.value) { mutableStateOf(proteinConfig?.value ?: "150") }
    var carbTarget by remember(carbConfig?.value) { mutableStateOf(carbConfig?.value ?: "200") }
    var fatTarget by remember(fatConfig?.value) { mutableStateOf(fatConfig?.value ?: "60") }
    var stepTarget by remember(stepConfig?.value) { mutableStateOf(stepConfig?.value ?: "10000") }
    var scheduleDays by remember(scheduleDaysConfig?.value) {
        mutableStateOf(scheduleDaysConfig?.value ?: "1,2,3,4,5,6,7")
    }
    var workoutSplitId by remember(workoutSplitConfig?.value) {
        mutableStateOf(workoutSplitConfig?.value ?: "ppl_ul")
    }
    var splitDayMap by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var goalTitle by remember(goalTitleConfig?.value) { mutableStateOf(goalTitleConfig?.value ?: "") }
    var rebuildResult by remember { mutableStateOf<String?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var modules by remember { mutableStateOf(EnabledModules()) }
    var pendingModuleDisable by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val notificationsOn = notificationsConfig?.value != "false"
    val colors = MaterialTheme.colorScheme
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = colors.primary.copy(alpha = 0.15f),
        selectedLabelColor = colors.primary,
        selectedLeadingIconColor = colors.primary,
    )

    LaunchedEffect(
        moduleCareerConfig?.value,
        moduleWorkoutConfig?.value,
        moduleDietConfig?.value,
        profile?.onboardingDone,
    ) {
        modules = ModuleFlags.resolve(
            onboardingDone = profile?.onboardingDone == true,
            career = moduleCareerConfig?.value,
            workout = moduleWorkoutConfig?.value,
            diet = moduleDietConfig?.value,
        )
    }

    fun applyModuleToggle(module: String, enabled: Boolean) {
        if (enabled) {
            val already = when (module) {
                "career" -> modules.career
                "workout" -> modules.workout
                "diet" -> modules.diet
                else -> return
            }
            if (already) return
            val updated = when (module) {
                "career" -> modules.withCareer(true)
                "workout" -> modules.withWorkout(true)
                "diet" -> modules.withDiet(true)
                else -> return
            }
            scope.launch(Dispatchers.IO) {
                container.onboarding.writeModuleFlags(updated)
                withContext(Dispatchers.Main) {
                    if (module == "workout") {
                        onMessage("Workout enabled — configure split below")
                    }
                }
            }
            return
        }
        val alreadyOff = when (module) {
            "career" -> !modules.career
            "workout" -> !modules.workout
            "diet" -> !modules.diet
            else -> return
        }
        if (alreadyOff) return
        val updated = when (module) {
            "career" -> modules.withCareer(false)
            "workout" -> modules.withWorkout(false)
            "diet" -> modules.withDiet(false)
            else -> return
        }
        if (!updated.anyEnabled) {
            onMessage("At least one module must stay enabled")
            return
        }
        pendingModuleDisable = module
    }

    LaunchedEffect(workoutSplitId, workoutSplitMapConfig?.value, workoutSplitConfig?.value) {
        val split = WorkoutCatalog.findSplit(workoutSplitId) ?: return@LaunchedEffect
        val parsed = if (workoutSplitId == workoutSplitConfig?.value) {
            WorkoutSplitLogic.parseDayMap(workoutSplitMapConfig?.value ?: "")
        } else {
            emptyMap()
        }
        splitDayMap = if (parsed.isNotEmpty() && split.schedule.all { it.day in parsed }) {
            parsed
        } else {
            WorkoutSplitLogic.defaultDayMap(split)
        }
    }

    LaunchedEffect(splitDayMap) {
        if (splitDayMap.isNotEmpty()) {
            scheduleDays = splitDayMap.values.sorted().joinToString(",")
        }
    }

    pendingModuleDisable?.let { module ->
        val label = when (module) {
            "career" -> "Career"
            "workout" -> "Workout"
            else -> "Diet"
        }
        AlertDialog(
            onDismissRequest = { pendingModuleDisable = null },
            title = { Text("Disable $label?") },
            text = { Text("History preserved. Past logs stay in your history; new quests for this module stop.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val updated = when (module) {
                            "career" -> modules.withCareer(false)
                            "workout" -> modules.withWorkout(false)
                            "diet" -> modules.withDiet(false)
                            else -> modules
                        }
                        pendingModuleDisable = null
                        scope.launch(Dispatchers.IO) {
                            container.onboarding.writeModuleFlags(updated)
                        }
                    },
                ) { Text("Disable") }
            },
            dismissButton = {
                TextButton(onClick = { pendingModuleDisable = null }) { Text("Cancel") }
            },
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset all progress?") },
            text = {
                Text(
                    "Clears XP, quests, streaks, achievements and module logs " +
                        "(workout/diet history, metrics). Preserves your name, configs and " +
                        "onboarding flag — the workout routine survives reset.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirm = false
                        scope.launch(Dispatchers.IO) {
                            container.onboarding.resetAllProgress()
                            withContext(Dispatchers.Main) {
                                onMessage("Progress reset")
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
        Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        SettingsCard(title = "Profile") {
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
        }

        SettingsCard(title = "System Modules") {
            Text(
                "Turn modules on or off. Disabling keeps your history.",
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            ModuleToggleRow(
                label = "Career",
                enabled = modules.career,
                onEnable = { applyModuleToggle("career", true) },
                onDisable = { applyModuleToggle("career", false) },
            )
            ModuleToggleRow(
                label = "Workout",
                enabled = modules.workout,
                onEnable = { applyModuleToggle("workout", true) },
                onDisable = { applyModuleToggle("workout", false) },
            )
            ModuleToggleRow(
                label = "Diet",
                enabled = modules.diet,
                onEnable = { applyModuleToggle("diet", true) },
                onDisable = { applyModuleToggle("diet", false) },
            )
        }

        if (modules.diet) {
            SettingsCard(title = "Targets") {
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
                    value = carbTarget,
                    onValueChange = { carbTarget = it.filter { c -> c.isDigit() } },
                    label = { Text("Carb target (g)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = fatTarget,
                    onValueChange = { fatTarget = it.filter { c -> c.isDigit() } },
                    label = { Text("Fat target (g)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Nutrition targets are estimates — not medical advice.",
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = stepTarget,
                    onValueChange = { stepTarget = it.filter { c -> c.isDigit() } },
                    label = { Text("Step target") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        SettingsCard(title = "Schedule & notifications") {
            if (modules.workout) {
                Text("Workout split", style = MaterialTheme.typography.titleSmall)
                val selectedSplit = WorkoutCatalog.findSplit(workoutSplitId)
                Text(
                    "Assign each workout to a weekday (${selectedSplit?.daysPerWeek ?: "?"} days/week).",
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    WorkoutCatalog.splits.forEach { split ->
                        FilterChip(
                            selected = split.id == workoutSplitId,
                            onClick = { workoutSplitId = split.id },
                            label = { Text("${split.name} (${split.daysPerWeek}d)") },
                            colors = chipColors,
                        )
                    }
                }
                selectedSplit?.schedule?.sortedBy { it.day }?.forEach { slot ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            WorkoutSplitLogic.workoutLabelForSlot(workoutSplitId, slot.day),
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            WorkoutSplitLogic.weekdayLabels.forEach { (iso, label) ->
                                FilterChip(
                                    selected = splitDayMap[slot.day] == iso,
                                    onClick = { splitDayMap = splitDayMap + (slot.day to iso) },
                                    label = { Text(label) },
                                    colors = chipColors,
                                )
                            }
                        }
                    }
                }
                remember(workoutSplitId, splitDayMap) {
                    WorkoutSplitLogic.buildRoutine(workoutSplitId, splitDayMap).error
                }?.let { err ->
                    Text(err, color = colors.error, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text("Notifications", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    scope.launch {
                        container.db.configDao().upsert(UserConfigEntity("notifications_enabled", "true"))
                    }
                }) { Text(if (notificationsOn) "On ✓" else "On") }
                OutlinedButton(onClick = {
                    scope.launch {
                        container.db.configDao().upsert(UserConfigEntity("notifications_enabled", "false"))
                    }
                }) { Text(if (!notificationsOn) "Off ✓" else "Off") }
            }
        }

        SettingsCard(title = "Actions") {
            Button(
                onClick = {
                    scope.launch {
                        val checks = buildList {
                            add(EntryValidation.requireNonBlank(name, "player name"))
                            if (modules.diet) {
                                add(EntryValidation.requirePositiveInt(calorieTarget, "calorie target"))
                                add(EntryValidation.requirePositiveInt(proteinTarget, "protein target"))
                                add(EntryValidation.requirePositiveInt(carbTarget, "carb target"))
                                add(EntryValidation.requirePositiveInt(fatTarget, "fat target"))
                                add(EntryValidation.requirePositiveInt(stepTarget, "step target"))
                            }
                        }
                        val error = EntryValidation.firstError(*checks.toTypedArray())
                        if (error != null) {
                            onMessage(error)
                            return@launch
                        }
                        val p = container.db.playerDao().getProfile(SystemDefaults.PLAYER_ID) ?: return@launch
                        container.db.playerDao().upsertProfile(p.copy(name = name.trim()))
                        if (modules.diet) {
                            container.db.configDao().upsert(UserConfigEntity("calorie_target", calorieTarget.trim()))
                            container.db.configDao().upsert(UserConfigEntity("protein_target", proteinTarget.trim()))
                            container.db.configDao().upsert(UserConfigEntity("carb_target", carbTarget.trim()))
                            container.db.configDao().upsert(UserConfigEntity("fat_target", fatTarget.trim()))
                            container.db.configDao().upsert(UserConfigEntity("step_target", stepTarget.trim()))
                        }
                        container.db.configDao().upsert(UserConfigEntity("goal_title", goalTitle))
                        if (goalTitle.isNotBlank()) {
                            container.db.configDao().upsert(UserConfigEntity("career_next_goal", goalTitle))
                        }
                        if (modules.workout && workoutSplitId.isNotBlank()) {
                            container.db.configDao().upsert(UserConfigEntity("schedule_days_csv", scheduleDays.trim()))
                            val mapError = WorkoutSplitLogic.buildRoutine(workoutSplitId, splitDayMap).error
                            if (mapError != null) {
                                onMessage(mapError)
                                return@launch
                            }
                            val splitError = container.modules.applyWorkoutSplit(
                                workoutSplitId,
                                WorkoutSplitLogic.encodeDayMap(splitDayMap),
                            )
                            if (splitError != null) {
                                onMessage(splitError)
                                return@launch
                            }
                            onMessage("Config saved · split applied")
                        } else {
                            onMessage("Config saved")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text("Save config") }

            OutlinedButton(
                onClick = {
                    scope.launch {
                        val p = container.db.playerDao().getProfile(SystemDefaults.PLAYER_ID)
                        container.questGeneration.generateForToday(p?.timezone ?: "Asia/Kolkata")
                        onMessage("Today's quests regenerated")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Regenerate today's quests") }

            OutlinedButton(
                onClick = {
                    scope.launch {
                        val result = container.progression.rebuildFromLedger()
                        rebuildResult = "XP ${result.oldTotal} → ${result.newTotal}, " +
                            "Level ${result.oldLevel} → ${result.newLevel}, " +
                            "Rank ${result.oldRank} → ${result.newRank}"
                        onMessage("XP rebuilt from ledger")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Rebuild XP from ledger") }

            rebuildResult?.let {
                Text(it, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }

            OutlinedButton(
                onClick = {
                    scope.launch {
                        val json = container.analytics.exportJson()
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Solo Levelling export")
                            putExtra(Intent.EXTRA_TEXT, json)
                        }
                        context.startActivity(Intent.createChooser(share, "Export data"))
                        onMessage("Data exported")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Export data") }

            Button(
                onClick = { showResetConfirm = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.error,
                    contentColor = colors.onError,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Reset all progress") }
        }

        SettingsCard(title = "System caps") {
            Text("Daily XP cap: ${SystemDefaults.DAILY_XP_CAP}", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            Text("Quest undo window: ${SystemDefaults.QUEST_UNDO_MINUTES}m", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            Text("Recovery/week: ${SystemDefaults.WEEKLY_RECOVERY_LIMIT}", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            Text("Streak grace days: ${SystemDefaults.STREAK_GRACE_DAYS}", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }

        SettingsCard(title = "Recent outbox events") {
            if (outbox.isEmpty()) {
                Text("None yet", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            } else {
                outbox.forEach { entry ->
                    Text(
                        "${entry.eventType} @ ${entry.createdAtEpochMs}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ModuleToggleRow(
    label: String,
    enabled: Boolean,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onEnable) {
                Text(if (enabled) "On ✓" else "On")
            }
            OutlinedButton(onClick = onDisable) {
                Text(if (!enabled) "Off ✓" else "Off")
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
        border = BorderStroke(1.dp, colors.outline),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            content()
        }
    }
}
