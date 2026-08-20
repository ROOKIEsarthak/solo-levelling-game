package com.example.solo_levelling.ui.settings

import android.content.Intent
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.data.seed.WorkoutCatalog
import com.example.solo_levelling.domain.service.EnabledModules
import com.example.solo_levelling.domain.service.EntryValidation
import com.example.solo_levelling.domain.service.ModuleFlags
import com.example.solo_levelling.domain.service.ModuleService
import com.example.solo_levelling.domain.service.WorkoutSplitLogic
import com.example.solo_levelling.ui.components.EnergyFieldBackground
import com.example.solo_levelling.ui.components.GlassLevel
import com.example.solo_levelling.ui.components.GlassSurface
import com.example.solo_levelling.ui.components.GhostTextButton
import com.example.solo_levelling.ui.components.SystemActionButton
import com.example.solo_levelling.ui.components.SystemSectionHeader
import com.example.solo_levelling.ui.theme.JetBrainsMono
import com.example.solo_levelling.ui.theme.Spacing
import com.example.solo_levelling.ui.theme.SystemError
import com.example.solo_levelling.ui.theme.SystemPrimary
import com.example.solo_levelling.ui.theme.SystemTertiary
import com.example.solo_levelling.ui.theme.SystemWarning
import kotlinx.coroutines.launch

internal const val WIPE_CONFIRM_PHRASE = "CONFIRM_WIPE"

internal fun lastActiveModuleMessage(): String =
    "At least one module must remain active."

internal fun lastActiveModuleAction(): String = "Choose another module"

internal fun pauseModuleTitle(label: String): String = "Pause $label?"

internal fun pauseModuleBody(label: String): String =
    "Your ${label.lowercase()} history will remain saved. " +
        "$label activity will no longer affect your current quests or progression until you enable it again."

internal fun systemUpdatedMessage(): String = "Your system has been updated."

internal fun moduleChangesSummary(added: List<String>): String {
    val names = added.map { ModuleFlags.displayName(it) }
    return when (names.size) {
        0 -> systemUpdatedMessage()
        1 -> "${names[0]} is now active."
        2 -> "${names[0]} and ${names[1]} have been added."
        else -> "${names.dropLast(1).joinToString(", ")} and ${names.last()} have been added."
    }
}

internal fun moduleSetupQueueIntro(count: Int): String =
    if (count <= 1) {
        "Before the system can continue, we need a few details."
    } else {
        "$count new modules need setup."
    }

internal fun isWipeConfirmationValid(input: String): Boolean =
    input.trim().equals(WIPE_CONFIRM_PHRASE, ignoreCase = false)

/** Wipe dialog body — progress clears; identity/configs kept; onboarding re-runs. */
internal fun systemWipeDescription(): String =
    "Clears XP, quests, streaks, achievements and module logs " +
        "(workout/diet history, metrics). Preserves your name and configs. " +
        "Onboarding will run again so you can reconfigure modules."

internal fun settingsSplitIsLocked(savedSplitId: String?): Boolean =
    !savedSplitId.isNullOrBlank()

internal fun settingsCurrentSplitLines(splitId: String, dayMap: Map<Int, Int>): List<String> {
    val split = WorkoutCatalog.findSplit(splitId) ?: return listOf(splitId)
    return split.schedule.sortedBy { it.day }.map { slot ->
        val name = WorkoutSplitLogic.workoutLabelForSlot(splitId, slot.day)
        val iso = dayMap[slot.day]
        val day = WorkoutSplitLogic.weekdayLabels.firstOrNull { it.first == iso }?.second ?: "—"
        "$name · $day"
    }
}

@Composable
fun SettingsScreen(
    container: AppContainer,
    onMessage: (String) -> Unit = {},
    onResetComplete: () -> Unit = {},
    onBeginModuleSetup: (queue: List<String>, deferredDisables: List<String>) -> Unit = { _, _ -> },
    onModuleChangesApplied: () -> Unit = {},
) {
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
    val profile by container.db.playerDao().observeProfile(SystemDefaults.PLAYER_ID)
        .collectAsStateWithLifecycle(initialValue = null)
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
    var wipeConfirmInput by remember { mutableStateOf("") }
    var modules by remember { mutableStateOf(EnabledModules()) }
    var draftModules by remember { mutableStateOf(EnabledModules()) }
    var draftDirty by remember { mutableStateOf(false) }
    var pendingModuleDisable by remember { mutableStateOf<String?>(null) }
    var showLastModuleDialog by remember { mutableStateOf(false) }
    var showEarlySplitDialog by remember { mutableStateOf(false) }
    var pendingSplitCsv by remember { mutableStateOf<String?>(null) }
    var weeksOnSplit by remember { mutableStateOf(0L) }
    var changingSplit by remember { mutableStateOf(false) }
    val splitLocked = settingsSplitIsLocked(workoutSplitConfig?.value)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val notificationsOn = notificationsConfig?.value != "false"
    val colors = MaterialTheme.colorScheme
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = SystemPrimary.copy(alpha = 0.15f),
        selectedLabelColor = SystemPrimary,
        selectedLeadingIconColor = SystemPrimary,
    )
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = SystemPrimary,
        unfocusedBorderColor = SystemPrimary.copy(alpha = 0.3f),
        cursorColor = SystemPrimary,
    )

    LaunchedEffect(
        moduleCareerConfig?.value,
        moduleWorkoutConfig?.value,
        moduleDietConfig?.value,
        profile?.onboardingDone,
    ) {
        val resolved = ModuleFlags.resolve(
            onboardingDone = profile?.onboardingDone == true,
            career = moduleCareerConfig?.value,
            workout = moduleWorkoutConfig?.value,
            diet = moduleDietConfig?.value,
        )
        modules = resolved
        if (!draftDirty) {
            draftModules = resolved
        }
    }

    fun applyDraftToggle(module: String, enabled: Boolean) {
        if (enabled) {
            if (draftModules.isEnabled(module)) return
            draftDirty = true
            draftModules = draftModules.withModule(module, true)
            return
        }
        if (!draftModules.isEnabled(module)) return
        val updated = draftModules.withModule(module, false)
        if (!updated.anyEnabled) {
            showLastModuleDialog = true
            return
        }
        pendingModuleDisable = module
    }

    fun applyModuleChanges() {
        if (!draftModules.anyEnabled) {
            showLastModuleDialog = true
            return
        }
        if (draftModules == modules) return
        scope.launch {
            val result = vm.applyModuleChanges(draftModules)
            if (result.blocked) {
                showLastModuleDialog = true
                return@launch
            }
            draftDirty = false
            if (result.setupQueue.isNotEmpty()) {
                onMessage(moduleSetupQueueIntro(result.setupQueue.size))
                onBeginModuleSetup(result.setupQueue, result.deferredDisables)
            } else {
                onMessage(moduleChangesSummary(result.added))
                onModuleChangesApplied()
            }
        }
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
        val label = ModuleFlags.displayName(module)
        AlertDialog(
            onDismissRequest = { pendingModuleDisable = null },
            containerColor = colors.surface.copy(alpha = 0.95f),
            title = {
                Text(
                    text = pauseModuleTitle(label),
                    fontFamily = JetBrainsMono,
                    color = SystemWarning,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = pauseModuleBody(label),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                SystemActionButton(
                    label = "PAUSE ${label.uppercase()}",
                    onClick = {
                        pendingModuleDisable = null
                        draftDirty = true
                        draftModules = draftModules.withModule(module, false)
                    },
                )
            },
            dismissButton = {
                GhostTextButton(
                    label = "KEEP ${label.uppercase()}",
                    onClick = { pendingModuleDisable = null },
                )
            },
        )
    }

    if (showLastModuleDialog) {
        AlertDialog(
            onDismissRequest = { showLastModuleDialog = false },
            containerColor = colors.surface.copy(alpha = 0.95f),
            title = {
                Text(
                    text = lastActiveModuleMessage(),
                    fontFamily = JetBrainsMono,
                    color = SystemWarning,
                    fontWeight = FontWeight.Bold,
                )
            },
            confirmButton = {
                SystemActionButton(
                    label = lastActiveModuleAction().uppercase(),
                    onClick = { showLastModuleDialog = false },
                )
            },
        )
    }

    if (showEarlySplitDialog) {
        AlertDialog(
            onDismissRequest = {
                showEarlySplitDialog = false
                pendingSplitCsv = null
            },
            title = { Text("Your current split") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (weeksOnSplit > 0) {
                            "You've been following this split for $weeksOnSplit weeks."
                        } else {
                            "You've recently set this split."
                        },
                    )
                    Text(
                        "Consistency matters more than constantly changing the plan. " +
                            "For meaningful progress, consider following your current split " +
                            "for at least 6 months before changing it.",
                    )
                    Text(
                        "Changing early may reduce workout progression rewards. " +
                            "Your existing progress remains yours.",
                    )
                }
            },
            confirmButton = {
                SystemActionButton(
                    label = "CHANGE SPLIT",
                    onClick = {
                        val csv = pendingSplitCsv
                        showEarlySplitDialog = false
                        pendingSplitCsv = null
                        if (csv != null) {
                            scope.launch {
                                val err = vm.applyWorkoutSplit(
                                    workoutSplitId,
                                    csv,
                                    confirmEarlyChange = true,
                                )
                                if (err != null) {
                                    onMessage(err)
                                } else {
                                    changingSplit = false
                                    onMessage("Config saved · split applied")
                                }
                            }
                        }
                    },
                )
            },
            dismissButton = {
                GhostTextButton(
                    label = "KEEP CURRENT",
                    onClick = {
                        showEarlySplitDialog = false
                        pendingSplitCsv = null
                        changingSplit = false
                    },
                )
            },
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = {
                showResetConfirm = false
                wipeConfirmInput = ""
            },
            containerColor = colors.surface.copy(alpha = 0.95f),
            title = {
                Text(
                    text = "[ CRITICAL ALERT ]",
                    fontFamily = JetBrainsMono,
                    color = SystemTertiary,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "SYSTEM WIPE IMMINENT",
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                    )
                    Text(
                        text = systemWipeDescription(),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                    GlassSurface(level = GlassLevel.Level1) {
                        Text(
                            text = "> STATUS: MODULE_OFFLINE PENDING\n> OVERRIDE_REQ: MANUAL CONFIRMATION",
                            fontFamily = JetBrainsMono,
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemTertiary.copy(alpha = 0.8f),
                        )
                    }
                    Text(
                        text = "Type '$WIPE_CONFIRM_PHRASE' to proceed",
                        fontFamily = JetBrainsMono,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = wipeConfirmInput,
                        onValueChange = { wipeConfirmInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("WAITING FOR INPUT...") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SystemTertiary,
                            unfocusedBorderColor = SystemTertiary.copy(alpha = 0.4f),
                            cursorColor = SystemTertiary,
                        ),
                    )
                }
            },
            confirmButton = {
                SystemActionButton(
                    label = "INITIATE",
                    onClick = {
                        showResetConfirm = false
                        wipeConfirmInput = ""
                        scope.launch {
                            vm.resetAllProgress()
                            onMessage("Progress reset")
                            onResetComplete()
                        }
                    },
                    enabled = isWipeConfirmationValid(wipeConfirmInput),
                )
            },
            dismissButton = {
                GhostTextButton(
                    label = "ABORT",
                    onClick = {
                        showResetConfirm = false
                        wipeConfirmInput = ""
                    },
                )
            },
        )
    }

    Box(Modifier.fillMaxSize()) {
        EnergyFieldBackground(Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                SystemSectionHeader(tag = "SETTINGS", accent = SystemPrimary)
                Text(
                    text = "Profile, modules, and app preferences",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }

            SettingsSection(tag = "IDENTITY", accent = SystemPrimary) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Player name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                )
                OutlinedTextField(
                    value = goalTitle,
                    onValueChange = { goalTitle = it },
                    label = { Text("Goal title (vision)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = fieldColors,
                )
                if (modules.diet) {
                    OutlinedTextField(
                        value = calorieTarget,
                        onValueChange = { calorieTarget = it.filter { c -> c.isDigit() } },
                        label = { Text("Calorie target") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors,
                    )
                    OutlinedTextField(
                        value = proteinTarget,
                        onValueChange = { proteinTarget = it.filter { c -> c.isDigit() } },
                        label = { Text("Protein target (g)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors,
                    )
                    OutlinedTextField(
                        value = carbTarget,
                        onValueChange = { carbTarget = it.filter { c -> c.isDigit() } },
                        label = { Text("Carb target (g)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors,
                    )
                    OutlinedTextField(
                        value = fatTarget,
                        onValueChange = { fatTarget = it.filter { c -> c.isDigit() } },
                        label = { Text("Fat target (g)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors,
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
                        colors = fieldColors,
                    )
                }
                if (modules.workout) {
                    Text("Workout split", style = MaterialTheme.typography.titleSmall)
                    val selectedSplit = WorkoutCatalog.findSplit(workoutSplitId)
                    val showEditor = !splitLocked || changingSplit
                    if (!showEditor) {
                        Text(
                            selectedSplit?.let { "${it.name} (${it.daysPerWeek}d)" } ?: workoutSplitId,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        settingsCurrentSplitLines(workoutSplitId, splitDayMap).forEach { line ->
                            Text(
                                line,
                                color = colors.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        GhostTextButton(
                            label = "Change split",
                            onClick = { changingSplit = true },
                        )
                    } else {
                        Text(
                            if (splitLocked) {
                                "Pick a new split and weekdays, then save config."
                            } else {
                                "Assign each workout to a weekday (${selectedSplit?.daysPerWeek ?: "?"} days/week)."
                            },
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
                            Text(err, color = SystemError, style = MaterialTheme.typography.bodySmall)
                        }
                        if (splitLocked) {
                            GhostTextButton(
                                label = "Keep current split",
                                onClick = { changingSplit = false },
                            )
                        }
                    }
                }
                Text("Notifications", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SystemActionButton(
                        label = if (notificationsOn) "ON ✓" else "ON",
                        onClick = {
                            scope.launch {
                                vm.setNotificationsEnabled(true)
                            }
                        },
                        primary = false,
                    )
                    SystemActionButton(
                        label = if (!notificationsOn) "OFF ✓" else "OFF",
                        onClick = {
                            scope.launch {
                                vm.setNotificationsEnabled(false)
                            }
                        },
                        primary = false,
                    )
                }
            }

            SettingsSection(tag = "ACTIVE MODULES", accent = SystemTertiary) {
                Text(
                    "Turn modules on or off, then apply. Disabling keeps your history and stops new quests and XP from that module.",
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                ModuleToggleRow(
                    label = "Career",
                    enabled = draftModules.career,
                    onEnable = { applyDraftToggle(ModuleFlags.MODULE_CAREER, true) },
                    onDisable = { applyDraftToggle(ModuleFlags.MODULE_CAREER, false) },
                )
                ModuleToggleRow(
                    label = "Fitness",
                    enabled = draftModules.workout,
                    onEnable = { applyDraftToggle(ModuleFlags.MODULE_WORKOUT, true) },
                    onDisable = { applyDraftToggle(ModuleFlags.MODULE_WORKOUT, false) },
                )
                ModuleToggleRow(
                    label = "Nutrition",
                    enabled = draftModules.diet,
                    onEnable = { applyDraftToggle(ModuleFlags.MODULE_DIET, true) },
                    onDisable = { applyDraftToggle(ModuleFlags.MODULE_DIET, false) },
                )
                if (draftModules != modules) {
                    Text(
                        "Unsaved module changes",
                        color = SystemWarning,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMono,
                    )
                    SystemActionButton(
                        label = "APPLY MODULE CHANGES",
                        onClick = { applyModuleChanges() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            SettingsSection(tag = "DANGER ZONE", accent = SystemError) {
                SystemActionButton(
                    label = "SAVE CONFIG",
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
                            val p = vm.getProfile() ?: return@launch
                            vm.upsertProfile(p.copy(name = name.trim()))
                            if (modules.diet) {
                                vm.upsertConfig("calorie_target", calorieTarget.trim())
                                vm.upsertConfig("protein_target", proteinTarget.trim())
                                vm.upsertConfig("carb_target", carbTarget.trim())
                                vm.upsertConfig("fat_target", fatTarget.trim())
                                vm.upsertConfig("step_target", stepTarget.trim())
                            }
                            vm.upsertConfig("goal_title", goalTitle)
                            if (modules.career && goalTitle.isNotBlank()) {
                                vm.upsertConfig("career_next_goal", goalTitle)
                            }
                            val shouldApplySplit = modules.workout &&
                                workoutSplitId.isNotBlank() &&
                                (!splitLocked || changingSplit)
                            if (shouldApplySplit) {
                                vm.upsertConfig("schedule_days_csv", scheduleDays.trim())
                                val mapError = WorkoutSplitLogic.buildRoutine(workoutSplitId, splitDayMap).error
                                if (mapError != null) {
                                    onMessage(mapError)
                                    return@launch
                                }
                                val csv = WorkoutSplitLogic.encodeDayMap(splitDayMap)
                                val splitError = vm.applyWorkoutSplit(
                                    workoutSplitId,
                                    csv,
                                    confirmEarlyChange = false,
                                )
                                if (splitError == ModuleService.EARLY_SPLIT_CHANGE_REQUIRED) {
                                    pendingSplitCsv = csv
                                    weeksOnSplit = vm.weeksOnCurrentSplit()
                                    showEarlySplitDialog = true
                                    return@launch
                                }
                                if (splitError != null) {
                                    onMessage(splitError)
                                    return@launch
                                }
                                changingSplit = false
                                onMessage("Config saved · split applied")
                            } else {
                                onMessage("Config saved")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                SystemActionButton(
                    label = "REGENERATE QUESTS",
                    onClick = {
                        scope.launch {
                            val p = vm.getProfile()
                            vm.regenerateQuests(p?.timezone ?: "Asia/Kolkata")
                            onMessage("Today's quests regenerated")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    primary = false,
                )
                SystemActionButton(
                    label = "REBUILD XP",
                    onClick = {
                        scope.launch {
                            val result = vm.rebuildXp()
                            rebuildResult = "XP ${result.oldTotal} → ${result.newTotal}, " +
                                "Level ${result.oldLevel} → ${result.newLevel}, " +
                                "Rank ${result.oldRank} → ${result.newRank}"
                            onMessage("XP rebuilt from ledger")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    primary = false,
                )
                rebuildResult?.let {
                    Text(it, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                SystemActionButton(
                    label = "EXPORT DATA",
                    onClick = {
                        scope.launch {
                            val json = vm.exportJson()
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
                    primary = false,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SystemError.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(4.dp),
                ) {
                    SystemActionButton(
                        label = "RESET ALL PROGRESS",
                        onClick = {
                            wipeConfirmInput = ""
                            showResetConfirm = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        primary = false,
                    )
                }
                Text(
                    text = "Daily XP cap: ${SystemDefaults.DAILY_XP_CAP}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
                Text(
                    text = "Quest undo window: ${SystemDefaults.QUEST_UNDO_MINUTES}m",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
                Text(
                    text = "Streak grace days: ${SystemDefaults.STREAK_GRACE_DAYS}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    tag: String,
    accent: androidx.compose.ui.graphics.Color,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SystemSectionHeader(tag = tag, accent = accent)
        GlassSurface(modifier = Modifier.fillMaxWidth(), level = GlassLevel.Level1) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                content()
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
    GlassSurface(level = GlassLevel.Level1) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontFamily = JetBrainsMono,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SystemActionButton(
                    label = if (enabled) "ON ✓" else "ON",
                    onClick = onEnable,
                    primary = enabled,
                )
                SystemActionButton(
                    label = if (!enabled) "OFF ✓" else "OFF",
                    onClick = onDisable,
                    primary = !enabled,
                )
            }
        }
    }
}
