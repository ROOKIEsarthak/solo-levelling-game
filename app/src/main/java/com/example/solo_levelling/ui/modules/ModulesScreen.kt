package com.example.solo_levelling.ui.modules

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.core.config.SystemDefaults
import com.example.solo_levelling.data.db.entity.CareerNodeEntity
import com.example.solo_levelling.data.db.entity.DsaProblemEntity
import com.example.solo_levelling.domain.service.EnabledModules
import com.example.solo_levelling.domain.service.EntryValidation
import com.example.solo_levelling.domain.service.ModuleFlags
import com.example.solo_levelling.ui.components.BracketLabel
import com.example.solo_levelling.ui.components.CyberProgressBar
import com.example.solo_levelling.ui.components.EnergyFieldBackground
import com.example.solo_levelling.ui.components.GlassLevel
import com.example.solo_levelling.ui.components.GlassSurface
import com.example.solo_levelling.ui.components.SystemActionButton
import com.example.solo_levelling.ui.components.SystemIdleEmpty
import com.example.solo_levelling.ui.components.SystemSectionHeader
import com.example.solo_levelling.ui.theme.JetBrainsMono
import com.example.solo_levelling.ui.theme.Spacing
import com.example.solo_levelling.ui.theme.SystemPrimary
import com.example.solo_levelling.ui.theme.SystemSecondary
import com.example.solo_levelling.ui.theme.SystemSuccess
import com.example.solo_levelling.ui.theme.SystemSurface
import com.example.solo_levelling.ui.theme.SystemTertiary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ModulesScreen(
    container: AppContainer,
    onMessage: (String) -> Unit = {},
) {
    val vm: ModulesViewModel = viewModel(factory = ModulesViewModel.factory(container))
    val dsa by vm.dsa.collectAsStateWithLifecycle()
    val bosses by vm.bosses.collectAsStateWithLifecycle()
    val skills by vm.skills.collectAsStateWithLifecycle()
    val careerNodes by vm.careerNodes.collectAsStateWithLifecycle()
    val journalToday by vm.journalToday.collectAsStateWithLifecycle()
    val focusToday by vm.focusToday.collectAsStateWithLifecycle()
    val routinesToday by vm.routinesToday.collectAsStateWithLifecycle()
    val recentMetrics by vm.recentMetrics.collectAsStateWithLifecycle()
    val modules by ModuleFlags.observeEnabledModules(
        container.db.playerDao().observeProfile(SystemDefaults.PLAYER_ID),
        container.db.configDao(),
    ).collectAsStateWithLifecycle(initialValue = EnabledModules())
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme

    var dsaTitle by remember { mutableStateOf("") }
    var dsaDifficulty by remember { mutableStateOf("MEDIUM") }
    var dsaTopic by remember { mutableStateOf("") }
    var focusMinutes by remember { mutableStateOf("25") }
    var focusLabel by remember { mutableStateOf("Deep Work") }
    var timerRunning by remember { mutableStateOf(false) }
    var secondsLeft by remember { mutableIntStateOf(0) }
    var steps by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var journal by remember { mutableStateOf("") }
    var bossTitle by remember { mutableStateOf("") }

    LaunchedEffect(journalToday?.content) {
        journalToday?.content?.let { if (journal.isEmpty()) journal = it }
    }

    LaunchedEffect(timerRunning, secondsLeft) {
        if (timerRunning && secondsLeft > 0) {
            delay(1_000)
            secondsLeft--
        } else if (timerRunning && secondsLeft == 0) {
            timerRunning = false
            val mins = focusMinutes.toIntOrNull()?.coerceAtLeast(1) ?: 25
            container.modules.logFocus(mins, focusLabel.ifBlank { "Focus" })
        }
    }

    val routineKinds = listOf("WAKE", "SLEEP", "READ", "MEDITATE")
    val loggedRoutineKinds = routinesToday.map { it.kind }.toSet()
    val focusTotal = focusToday.sumOf { it.durationMinutes }
    val todaySteps = recentMetrics.filter { it.metricType == "STEPS" }.firstOrNull()
    val latestWeight = recentMetrics.filter { it.metricType == "WEIGHT" }.firstOrNull()
    val activeBosses = bosses.filter { it.status == "ACTIVE" }

    Box(Modifier.fillMaxSize()) {
        EnergyFieldBackground(Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            SystemSectionHeader(tag = "LIFE MODULES", accent = SystemPrimary)
            Text(
                if (modules.career) {
                    "Focus, journal, metrics, and extras. DSA & System Design live under Career."
                } else {
                    "Focus, journal, metrics, and extras."
                },
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = JetBrainsMono,
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GlassSurface(
                    modifier = Modifier.weight(1.4f),
                    level = GlassLevel.Level2,
                    borderAlpha = 0.35f,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SystemSectionHeader(tag = "FOCUS", accent = SystemSecondary)
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BracketLabel(
                                text = if (timerRunning) "RUNNING" else if (focusTotal > 0) "LOGGED" else "IDLE",
                                color = when {
                                    timerRunning -> SystemTertiary
                                    focusTotal > 0 -> SystemSuccess
                                    else -> colors.onSurfaceVariant
                                },
                            )
                            if (timerRunning) {
                                Text(
                                    "${secondsLeft / 60}:${"%02d".format(secondsLeft % 60)}",
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Bold,
                                    color = SystemPrimary,
                                    style = MaterialTheme.typography.headlineSmall,
                                )
                            }
                        }
                        OutlinedTextField(
                            focusMinutes,
                            { focusMinutes = it },
                            label = { Text("Minutes") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            focusLabel,
                            { focusLabel = it },
                            label = { Text("Label") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SystemActionButton(
                                label = "START TIMER",
                                onClick = {
                                    EntryValidation.requirePositiveInt(focusMinutes, "minutes")?.let {
                                        onMessage(it)
                                        return@SystemActionButton
                                    }
                                    val mins = focusMinutes.trim().toInt()
                                    secondsLeft = mins * 60
                                    timerRunning = true
                                },
                                enabled = !timerRunning,
                                modifier = Modifier.weight(1f),
                            )
                            SystemActionButton(
                                label = "LOG NOW",
                                onClick = {
                                    scope.launch {
                                        EntryValidation.requirePositiveInt(focusMinutes, "minutes")?.let {
                                            onMessage(it)
                                            return@launch
                                        }
                                        val mins = focusMinutes.trim().toInt()
                                        container.modules.logFocus(mins, focusLabel.ifBlank { "Focus" })
                                        onMessage("Logged $mins focus minutes")
                                    }
                                },
                                primary = false,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (focusTotal > 0) {
                            Text(
                                "Today: ${focusTotal}m focus logged",
                                color = SystemSuccess,
                                fontFamily = JetBrainsMono,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }

                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    GlassSurface(modifier = Modifier.fillMaxWidth(), level = GlassLevel.Level1) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SystemSectionHeader(tag = "STEPS", accent = SystemPrimary)
                            BracketLabel(
                                text = if (todaySteps != null) "LOGGED" else "IDLE",
                                color = if (todaySteps != null) SystemSuccess else colors.onSurfaceVariant,
                            )
                            OutlinedTextField(steps, { steps = it }, label = { Text("Steps") }, modifier = Modifier.fillMaxWidth())
                            SystemActionButton(
                                label = "LOG STEPS",
                                onClick = {
                                    scope.launch {
                                        EntryValidation.requirePositiveFloat(steps, "steps")?.let {
                                            onMessage(it)
                                            return@launch
                                        }
                                        val value = steps.trim().toFloat()
                                        container.metricIngest.ingest("STEPS", value)
                                        steps = ""
                                        onMessage("Steps logged")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                primary = false,
                            )
                            todaySteps?.let {
                                Text(
                                    "Latest: ${it.value.toInt()} steps",
                                    fontFamily = JetBrainsMono,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }

                    GlassSurface(modifier = Modifier.fillMaxWidth(), level = GlassLevel.Level1) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SystemSectionHeader(tag = "WEIGHT", accent = SystemPrimary)
                            BracketLabel(
                                text = if (latestWeight != null) "LOGGED" else "IDLE",
                                color = if (latestWeight != null) SystemSuccess else colors.onSurfaceVariant,
                            )
                            OutlinedTextField(
                                weight,
                                { weight = it },
                                label = { Text("Weight (kg)") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            SystemActionButton(
                                label = "LOG WEIGHT",
                                onClick = {
                                    scope.launch {
                                        EntryValidation.requirePositiveFloat(weight, "weight")?.let {
                                            onMessage(it)
                                            return@launch
                                        }
                                        val value = weight.trim().toFloat()
                                        container.metricIngest.ingest("WEIGHT", value)
                                        weight = ""
                                        onMessage("Weight logged")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                primary = false,
                            )
                            latestWeight?.let {
                                Text(
                                    "Latest: ${it.value} kg",
                                    fontFamily = JetBrainsMono,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }

            GlassSurface(modifier = Modifier.fillMaxWidth(), level = GlassLevel.Level1) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SystemSectionHeader(tag = "JOURNAL", accent = SystemSecondary)
                    BracketLabel(
                        text = if (journal.isNotBlank()) "Entry ready" else "Entry required",
                        color = if (journal.isNotBlank()) SystemTertiary else colors.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = journal,
                        onValueChange = { journal = it },
                        label = { Text("Today's entry") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                    )
                    SystemActionButton(
                        label = "SAVE JOURNAL",
                        onClick = {
                            scope.launch {
                                EntryValidation.requireNonBlank(journal, "journal entry")?.let {
                                    onMessage(it)
                                    return@launch
                                }
                                container.modules.saveJournal(journal.trim())
                                onMessage("Journal saved · +10 XP")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (modules.career) {
                    GlassSurface(modifier = Modifier.weight(1f), level = GlassLevel.Level1) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SystemSectionHeader(tag = "SKILLS", accent = SystemPrimary)
                            SkillsSection(skills = skills)
                        }
                    }
                }

                GlassSurface(modifier = Modifier.weight(1f), level = GlassLevel.Level1) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SystemSectionHeader(tag = "BOSS QUESTS", accent = SystemTertiary)
                        BracketLabel(
                            text = if (activeBosses.isEmpty()) "IDLE" else "ACTIVE",
                            color = if (activeBosses.isEmpty()) colors.onSurfaceVariant else SystemTertiary,
                        )
                        OutlinedTextField(
                            value = bossTitle,
                            onValueChange = { bossTitle = it },
                            label = { Text("New boss") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        SystemActionButton(
                            label = "CREATE BOSS",
                            onClick = {
                                scope.launch {
                                    if (bossTitle.isBlank()) {
                                        onMessage("Boss needs a title")
                                        return@launch
                                    }
                                    container.modules.createBoss(bossTitle, "Major objective", 200)
                                    bossTitle = ""
                                    onMessage("Boss created")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            primary = false,
                        )
                        if (activeBosses.isEmpty()) {
                            SystemIdleEmpty(
                                title = "No active boss",
                                subtitle = "Create a boss quest to track a major objective.",
                            )
                        } else {
                            activeBosses.forEach { b ->
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(b.title, fontWeight = FontWeight.Bold)
                                    CyberProgressBar(
                                        progress = (b.currentValue / b.targetValue.coerceAtLeast(1f)).coerceIn(0f, 1f),
                                        height = 6.dp,
                                    )
                                    Text(
                                        "${b.currentValue.toInt()}/${b.targetValue.toInt()}",
                                        fontFamily = JetBrainsMono,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    SystemActionButton(
                                        label = "+25% PROGRESS",
                                        onClick = {
                                            scope.launch {
                                                container.modules.addBossProgress(25f)
                                                onMessage("Boss progress +25%")
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            GlassSurface(modifier = Modifier.fillMaxWidth(), level = GlassLevel.Level1) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SystemSectionHeader(tag = "ROUTINES", accent = SystemPrimary)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        routineKinds.forEach { kind ->
                            RoutineChip(
                                label = kind.lowercase().replaceFirstChar { it.uppercase() },
                                logged = kind in loggedRoutineKinds,
                                onClick = { scope.launch { container.modules.logRoutine(kind) } },
                            )
                        }
                    }
                    if (routinesToday.isNotEmpty()) {
                        Text(
                            "Logged today: ${routinesToday.joinToString { it.kind }}",
                            fontFamily = JetBrainsMono,
                            style = MaterialTheme.typography.bodySmall,
                            color = SystemSuccess,
                        )
                    } else {
                        BracketLabel(text = "IDLE", color = colors.onSurfaceVariant)
                    }
                }
            }

            if (modules.career) {
                CareerSection(careerNodes) { id ->
                    scope.launch {
                        container.modules.advanceCareerNode(id)
                        onMessage("Node advanced · +50 XP")
                    }
                }

                DsaSection(
                    dsa = dsa,
                    title = dsaTitle,
                    difficulty = dsaDifficulty,
                    topic = dsaTopic,
                    onTitleChange = { dsaTitle = it },
                    onDifficultyChange = { dsaDifficulty = it },
                    onTopicChange = { dsaTopic = it },
                    onAdd = {
                        scope.launch {
                            EntryValidation.firstError(
                                EntryValidation.requireNonBlank(dsaTitle, "title"),
                                EntryValidation.requireNonBlank(dsaTopic, "topic"),
                            )?.let {
                                onMessage(it)
                                return@launch
                            }
                            val difficulty = dsaDifficulty.ifBlank { "MEDIUM" }
                            container.modules.addDsaProblem(dsaTitle.trim(), difficulty, dsaTopic.trim())
                            dsaTitle = ""
                            dsaTopic = ""
                            onMessage("Problem added")
                        }
                    },
                    onAttempt = { scope.launch { container.modules.markAttempted(it); onMessage("Attempted") } },
                    onSolve = { scope.launch { container.modules.solveDsa(it); onMessage("Solved") } },
                    onMaster = { scope.launch { container.modules.masterDsa(it); onMessage("Mastered") } },
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun RoutineChip(label: String, logged: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    val background = if (logged) SystemSuccess.copy(alpha = 0.15f) else SystemSurface.copy(alpha = 0.35f)
    val borderColor = if (logged) SystemSuccess else SystemPrimary.copy(alpha = 0.25f)
    val textColor = if (logged) SystemSuccess else MaterialTheme.colorScheme.onSurfaceVariant

    Text(
        text = label,
        modifier = Modifier
            .clip(shape)
            .background(background)
            .border(androidx.compose.foundation.BorderStroke(1.dp, borderColor), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelMedium,
        fontFamily = JetBrainsMono,
        color = textColor,
        fontWeight = if (logged) FontWeight.Bold else FontWeight.Medium,
    )
}

@Composable
private fun SkillsSection(skills: List<com.example.solo_levelling.data.db.entity.SkillEntity>) {
    val colors = MaterialTheme.colorScheme
    var domainFilter by remember { mutableStateOf<String?>(null) }
    val domains = skills.map { it.domain }.distinct().sorted()

    if (skills.isEmpty()) {
        SystemIdleEmpty(
            title = "Skills idle",
            subtitle = "Skills unlock from DSA and career progress.",
        )
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RoutineChip(label = "All", logged = domainFilter == null, onClick = { domainFilter = null })
        domains.forEach { domain ->
            RoutineChip(
                label = domain,
                logged = domainFilter == domain,
                onClick = { domainFilter = domain },
            )
        }
    }

    val filtered = if (domainFilter == null) skills else skills.filter { it.domain == domainFilter }
    filtered.groupBy { it.domain }.forEach { (domain, domainSkills) ->
        BracketLabel(text = domain.uppercase(), color = SystemSecondary)
        domainSkills.forEach { s ->
            Text(
                "${s.name} · Lv ${s.level} (${s.xp} XP)",
                fontFamily = JetBrainsMono,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun CareerSection(nodes: List<CareerNodeEntity>, onAdvance: (Long) -> Unit) {
    GlassSurface(modifier = Modifier.fillMaxWidth(), level = GlassLevel.Level1) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SystemSectionHeader(tag = "CAREER", accent = SystemPrimary)
            if (nodes.isEmpty()) {
                SystemIdleEmpty(
                    title = "Career idle",
                    subtitle = "Career nodes will appear as your path is configured.",
                )
            } else {
                nodes.groupBy { it.track }.forEach { (track, trackNodes) ->
                    BracketLabel(text = track.uppercase(), color = SystemSecondary)
                    trackNodes.sortedBy { it.orderIndex }.forEach { node ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(node.title, fontWeight = FontWeight.Bold)
                                BracketLabel(
                                    text = node.status,
                                    color = when (node.status) {
                                        "MASTERED" -> SystemSuccess
                                        "LOCKED" -> MaterialTheme.colorScheme.onSurfaceVariant
                                        else -> SystemPrimary
                                    },
                                )
                                Text(node.description, style = MaterialTheme.typography.bodySmall)
                            }
                            if (node.status != "MASTERED") {
                                SystemActionButton(
                                    label = "ADVANCE",
                                    onClick = { onAdvance(node.id) },
                                    primary = false,
                                )
                            } else {
                                BracketLabel(text = "CLEARED", color = SystemSuccess)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DsaSection(
    dsa: List<DsaProblemEntity>,
    title: String,
    difficulty: String,
    topic: String,
    onTitleChange: (String) -> Unit,
    onDifficultyChange: (String) -> Unit,
    onTopicChange: (String) -> Unit,
    onAdd: () -> Unit,
    onAttempt: (Long) -> Unit,
    onSolve: (Long) -> Unit,
    onMaster: (Long) -> Unit,
) {
    GlassSurface(modifier = Modifier.fillMaxWidth(), level = GlassLevel.Level1) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SystemSectionHeader(tag = "DSA", accent = SystemPrimary)
            OutlinedTextField(title, onTitleChange, label = { Text("Problem") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(difficulty, onDifficultyChange, label = { Text("Difficulty") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(topic, onTopicChange, label = { Text("Topic") }, modifier = Modifier.fillMaxWidth())
            SystemActionButton(label = "ADD PROBLEM", onClick = onAdd, modifier = Modifier.fillMaxWidth())
            if (dsa.isEmpty()) {
                SystemIdleEmpty(
                    title = "No problems",
                    subtitle = "Add your first DSA problem below.",
                )
            }
            dsa.take(10).forEach { p ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${p.title}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = JetBrainsMono,
                    )
                    BracketLabel(text = p.status, color = SystemPrimary)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (p.status == "NOT_STARTED") {
                        SystemActionButton(label = "ATTEMPT", onClick = { onAttempt(p.id) }, primary = false)
                    }
                    if (p.status == "NOT_STARTED" || p.status == "ATTEMPTED") {
                        SystemActionButton(label = "SOLVE", onClick = { onSolve(p.id) })
                    }
                    if (p.status == "SOLVED") {
                        SystemActionButton(label = "MASTER", onClick = { onMaster(p.id) })
                    }
                }
            }
        }
    }
}
