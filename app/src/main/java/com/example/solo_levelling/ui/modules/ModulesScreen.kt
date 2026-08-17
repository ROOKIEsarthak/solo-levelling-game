package com.example.solo_levelling.ui.modules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.data.db.entity.CareerNodeEntity
import com.example.solo_levelling.domain.service.EntryValidation
import com.example.solo_levelling.data.db.entity.DsaProblemEntity
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
    val scope = rememberCoroutineScope()

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

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Life Modules", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Focus, journal, metrics, and extras. DSA & System Design live under Career.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )

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

        ModuleCard(title = "Focus") {
                OutlinedTextField(focusMinutes, { focusMinutes = it }, label = { Text("Minutes") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(focusLabel, { focusLabel = it }, label = { Text("Label") }, modifier = Modifier.fillMaxWidth())
                if (timerRunning) {
                    Text("Time left: ${secondsLeft / 60}:${"%02d".format(secondsLeft % 60)}")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        EntryValidation.requirePositiveInt(focusMinutes, "minutes")?.let {
                            onMessage(it)
                            return@Button
                        }
                        val mins = focusMinutes.trim().toInt()
                        secondsLeft = mins * 60
                        timerRunning = true
                    }, enabled = !timerRunning) { Text("Start timer") }
                    OutlinedButton(onClick = {
                        scope.launch {
                            EntryValidation.requirePositiveInt(focusMinutes, "minutes")?.let {
                                onMessage(it)
                                return@launch
                            }
                            val mins = focusMinutes.trim().toInt()
                            container.modules.logFocus(mins, focusLabel.ifBlank { "Focus" })
                            onMessage("Logged $mins focus minutes")
                        }
                    }) { Text("Log now") }
                }
                val focusTotal = focusToday.sumOf { it.durationMinutes }
                if (focusTotal > 0) Text("Today: ${focusTotal}m focus logged")
        }

        ModuleCard(title = "Steps") {
                OutlinedTextField(steps, { steps = it }, label = { Text("Steps") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
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
                }) { Text("Log steps") }
                val todaySteps = recentMetrics.filter { it.metricType == "STEPS" }.firstOrNull()
                todaySteps?.let { Text("Latest: ${it.value.toInt()} steps") }
        }

        ModuleCard(title = "Weight") {
                OutlinedTextField(weight, { weight = it }, label = { Text("Weight (kg)") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
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
                }) { Text("Log weight") }
                val latestWeight = recentMetrics.filter { it.metricType == "WEIGHT" }.firstOrNull()
                latestWeight?.let { Text("Latest: ${it.value} kg") }
        }

        ModuleCard(title = "Journal") {
                OutlinedTextField(
                    value = journal,
                    onValueChange = { journal = it },
                    label = { Text("Today's entry") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                Button(onClick = {
                    scope.launch {
                        EntryValidation.requireNonBlank(journal, "journal entry")?.let {
                            onMessage(it)
                            return@launch
                        }
                        container.modules.saveJournal(journal.trim())
                        onMessage("Journal saved · +10 XP")
                    }
                }) { Text("Save journal") }
        }

        ModuleCard(title = "Routines") {
                val colors = MaterialTheme.colorScheme
                val chipColors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colors.primary.copy(alpha = 0.15f),
                    selectedLabelColor = colors.primary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    routineKinds.forEach { kind ->
                        FilterChip(
                            selected = kind in loggedRoutineKinds,
                            onClick = { scope.launch { container.modules.logRoutine(kind) } },
                            label = { Text(kind.lowercase().replaceFirstChar { it.uppercase() }) },
                            colors = chipColors,
                        )
                    }
                }
                if (routinesToday.isNotEmpty()) {
                    Text("Logged today: ${routinesToday.joinToString { it.kind }}")
                }
        }

        ModuleCard(title = "Boss Quests") {
                OutlinedTextField(
                    value = bossTitle,
                    onValueChange = { bossTitle = it },
                    label = { Text("New boss") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = {
                    scope.launch {
                        if (bossTitle.isBlank()) {
                            onMessage("Boss needs a title")
                            return@launch
                        }
                        container.modules.createBoss(bossTitle, "Major objective", 200)
                        bossTitle = ""
                        onMessage("Boss created")
                    }
                }) { Text("Create boss") }
                val active = bosses.filter { it.status == "ACTIVE" }
                if (active.isEmpty()) {
                    Text("No active boss", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                active.forEach { b ->
                    Text("${b.title}: ${b.currentValue}/${b.targetValue}")
                    Button(onClick = {
                        scope.launch {
                            container.modules.addBossProgress(25f)
                            onMessage("Boss progress +25%")
                        }
                    }) {
                        Text("+25% progress")
                    }
                }
        }

        ModuleCard(title = "Skills") {
                val domains = skills.map { it.domain }.distinct().sorted()
                var domainFilter by remember { mutableStateOf<String?>(null) }
                val colors = MaterialTheme.colorScheme
                val chipColors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colors.primary.copy(alpha = 0.15f),
                    selectedLabelColor = colors.primary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = domainFilter == null,
                        onClick = { domainFilter = null },
                        label = { Text("All") },
                        colors = chipColors,
                    )
                    domains.forEach { domain ->
                        FilterChip(
                            selected = domainFilter == domain,
                            onClick = { domainFilter = domain },
                            label = { Text(domain) },
                            colors = chipColors,
                        )
                    }
                }
                val filtered = if (domainFilter == null) skills else skills.filter { it.domain == domainFilter }
                filtered.groupBy { it.domain }.forEach { (domain, domainSkills) ->
                    Text(domain, style = MaterialTheme.typography.titleSmall)
                    domainSkills.forEach { s ->
                        Text("${s.name} · Lv ${s.level} (${s.xp} XP)")
                    }
                }
                if (skills.isEmpty()) Text("Skills unlock from DSA and career progress.")
        }
    }
}

@Composable
private fun ModuleCard(
    title: String,
    content: @Composable () -> Unit,
) {
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

@Composable
private fun CareerSection(nodes: List<CareerNodeEntity>, onAdvance: (Long) -> Unit) {
    ModuleCard(title = "Career") {
            nodes.groupBy { it.track }.forEach { (track, trackNodes) ->
                Text(track, style = MaterialTheme.typography.titleSmall)
                trackNodes.sortedBy { it.orderIndex }.forEach { node ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(node.title)
                            Text("${node.status} · ${node.description}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (node.status != "MASTERED") {
                            OutlinedButton(onClick = { onAdvance(node.id) }) {
                                Text("Advance")
                            }
                        } else {
                            Text("CLEARED", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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
    ModuleCard(title = "DSA") {
            OutlinedTextField(title, onTitleChange, label = { Text("Problem") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(difficulty, onDifficultyChange, label = { Text("Difficulty") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(topic, onTopicChange, label = { Text("Topic") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = onAdd) { Text("Add problem") }
            dsa.take(10).forEach { p ->
                Text("${p.title} · ${p.difficulty} · ${p.topic} · ${p.status}")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (p.status == "NOT_STARTED") {
                        OutlinedButton(onClick = { onAttempt(p.id) }) { Text("Attempt") }
                    }
                    if (p.status == "NOT_STARTED" || p.status == "ATTEMPTED") {
                        Button(onClick = { onSolve(p.id) }) { Text("Solve") }
                    }
                    if (p.status == "SOLVED") {
                        Button(onClick = { onMaster(p.id) }) { Text("Master") }
                    }
                }
            }
    }
}
