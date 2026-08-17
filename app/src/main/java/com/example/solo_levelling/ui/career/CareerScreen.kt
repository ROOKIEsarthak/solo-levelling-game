package com.example.solo_levelling.ui.career

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.example.solo_levelling.data.db.entity.DsaProblemEntity
import com.example.solo_levelling.data.db.entity.SystemDesignTopicEntity
import com.example.solo_levelling.domain.service.CareerHubLogic
import com.example.solo_levelling.domain.service.EntryValidation
import kotlinx.coroutines.launch

enum class CareerTab { Roadmap, Dsa, SystemDesign }

@Composable
fun CareerScreen(
    container: AppContainer,
    initialTab: CareerTab = CareerTab.Roadmap,
    onMessage: (String) -> Unit = {},
) {
    val vm: CareerViewModel = viewModel(factory = CareerViewModel.factory(container))
    val dsa by vm.dsa.collectAsStateWithLifecycle()
    val systemDesignTopics by vm.systemDesignTopics.collectAsStateWithLifecycle()
    val careerNodes by vm.careerNodes.collectAsStateWithLifecycle()
    val nextGoal by vm.nextGoal.collectAsStateWithLifecycle()
    val goalReason by vm.goalReason.collectAsStateWithLifecycle()
    val currentRole by vm.currentRole.collectAsStateWithLifecycle()
    val targetRole by vm.targetRole.collectAsStateWithLifecycle()
    val mandatoryAreasCsv by vm.mandatoryAreasCsv.collectAsStateWithLifecycle()
    val backendConfidence by vm.backendConfidence.collectAsStateWithLifecycle()
    val behavioralConfidence by vm.behavioralConfidence.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = colors.primary.copy(alpha = 0.15f),
        selectedLabelColor = colors.primary,
    )

    var selectedTab by remember { mutableStateOf(initialTab) }
    LaunchedEffect(initialTab) { selectedTab = initialTab }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Career", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CareerTab.entries.forEach { tab ->
                FilterChip(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    label = {
                        Text(
                            when (tab) {
                                CareerTab.Roadmap -> "Roadmap"
                                CareerTab.Dsa -> "DSA"
                                CareerTab.SystemDesign -> "System Design"
                            },
                        )
                    },
                    colors = chipColors,
                    border = BorderStroke(
                        1.dp,
                        if (selectedTab == tab) colors.primary else colors.outline,
                    ),
                )
            }
        }

        when (selectedTab) {
            CareerTab.Roadmap -> RoadmapTab(
                currentRole = currentRole,
                targetRole = targetRole,
                nextGoal = nextGoal,
                goalReason = goalReason,
                mandatoryAreas = CareerHubLogic.parseCsv(mandatoryAreasCsv),
                dsa = dsa,
                systemDesignTopics = systemDesignTopics,
                backendPct = CareerHubLogic.configInt(backendConfidence),
                behavioralPct = CareerHubLogic.configInt(behavioralConfidence),
            )
            CareerTab.Dsa -> DsaTab(
                dsa = dsa,
                currentGoal = nextGoal,
                nowEpochMs = vm.nowEpochMs(),
                onMessage = onMessage,
                onAttempt = { id ->
                    scope.launch {
                        container.modules.markAttempted(id)
                        onMessage("Attempt logged")
                    }
                },
                onSolve = { id ->
                    scope.launch {
                        container.modules.solveDsa(id)
                        onMessage("Problem solved — XP awarded")
                    }
                },
                onMaster = { id ->
                    scope.launch {
                        container.modules.masterDsa(id)
                        onMessage("Problem mastered")
                    }
                },
                onAdd = { title, topic, difficulty ->
                    scope.launch {
                        container.modules.addDsaProblem(title, difficulty, topic)
                        onMessage("Problem added")
                    }
                },
            )
            CareerTab.SystemDesign -> SystemDesignTab(
                topics = systemDesignTopics,
                onMarkConcept = { topicId, conceptId, nextStatus ->
                    scope.launch {
                        container.modules.markSystemDesignConcept(topicId, conceptId, nextStatus)
                        onMessage("Concept updated")
                    }
                },
            )
        }
    }
}

@Composable
private fun RoadmapTab(
    currentRole: String,
    targetRole: String,
    nextGoal: String,
    goalReason: String,
    mandatoryAreas: List<String>,
    dsa: List<DsaProblemEntity>,
    systemDesignTopics: List<SystemDesignTopicEntity>,
    backendPct: Int,
    behavioralPct: Int,
) {
    val colors = MaterialTheme.colorScheme
    val dsaPct = CareerHubLogic.dsaOverallProgress(dsa)
    val sdPct = CareerHubLogic.sdTopicsProgress(systemDesignTopics)
    val nextPriority = CareerHubLogic.lowestMandatoryArea(
        mandatoryAreas = mandatoryAreas,
        dsaPct = dsaPct,
        sdPct = sdPct,
        backendPct = backendPct,
        behavioralPct = behavioralPct,
    )

    CareerCard(title = "Career path") {
        Text(
            "${currentRole.ifBlank { "Current role" }} → ${targetRole.ifBlank { "Target role" }}",
            fontWeight = FontWeight.Bold,
        )
        if (nextGoal.isNotBlank()) {
            Text("Next goal: $nextGoal", color = colors.primary, fontWeight = FontWeight.Bold)
        }
        if (goalReason.isNotBlank()) {
            Text(goalReason, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }

    CareerCard(title = "Area progress") {
        AreaProgressBar("DSA", dsaPct)
        AreaProgressBar("System Design", sdPct)
        AreaProgressBar("Backend", backendPct)
        AreaProgressBar("Behavioral", behavioralPct)
    }

    if (nextPriority != null) {
        CareerCard(title = "Next priority") {
            Text(
                "${nextPriority.first} — ${nextPriority.second}%",
                color = colors.primary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Lowest mandatory focus area. Strengthen this next.",
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun DsaTab(
    dsa: List<DsaProblemEntity>,
    currentGoal: String,
    nowEpochMs: Long,
    onMessage: (String) -> Unit,
    onAttempt: (Long) -> Unit,
    onSolve: (Long) -> Unit,
    onMaster: (Long) -> Unit,
    onAdd: (String, String, String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val overall = CareerHubLogic.dsaOverallProgress(dsa)
    val topics = CareerHubLogic.dsaTopicProgress(dsa)
    val currentTopic = CareerHubLogic.currentDsaTopic(dsa)
    val nextProblem = CareerHubLogic.recommendNextProblem(dsa, currentTopic)
    val reviewCount = CareerHubLogic.needsReviewCount(dsa, nowEpochMs)

    var title by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("MEDIUM") }

    CareerCard(title = "DSA overview") {
        if (currentGoal.isNotBlank()) {
            Text("Current goal: $currentGoal", fontWeight = FontWeight.Bold)
        }
        Text("Overall progress: $overall%")
        LinearProgressIndicator(
            progress = { overall / 100f },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = colors.primary,
            trackColor = colors.surfaceContainerHighest,
        )
        Text("Needs review: $reviewCount", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }

    if (nextProblem != null) {
        CareerCard(title = "Recommended") {
            Text(nextProblem.title, fontWeight = FontWeight.Bold)
            Text(
                "${nextProblem.topic.ifBlank { "General" }} · ${nextProblem.difficulty}",
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "Why? Next unfinished problem in ${currentTopic ?: "your current topic"} — keep the pattern progression moving.",
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    } else if (currentTopic != null) {
        CareerCard(title = "Today's objective") {
            Text("Continue $currentTopic — pick a problem below", style = MaterialTheme.typography.bodySmall)
        }
    }

    topics.forEach { (topicName, pct) ->
        CareerCard(title = topicName) {
            Text("$pct% complete")
            LinearProgressIndicator(
                progress = { pct / 100f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = colors.primary,
                trackColor = colors.surfaceContainerHighest,
            )
            dsa.filter { it.topic.equals(topicName, ignoreCase = true) || (topicName == "General" && it.topic.isBlank()) }
                .forEach { problem ->
                    DsaProblemRow(problem, onAttempt, onSolve, onMaster)
                }
        }
    }

    if (dsa.isEmpty()) {
        CareerCard(title = "Problems") {
            Text("No problems yet. Add one below.", color = colors.onSurfaceVariant)
        }
    }

    CareerCard(title = "Add problem") {
        OutlinedTextField(title, { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(topic, { topic = it }, label = { Text("Topic") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(difficulty, { difficulty = it }, label = { Text("Difficulty") }, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                val error = EntryValidation.firstError(
                    EntryValidation.requireNonBlank(title, "title"),
                    EntryValidation.requireNonBlank(topic, "topic"),
                )
                if (error != null) {
                    onMessage(error)
                    return@Button
                }
                onAdd(title.trim(), topic.trim(), difficulty.trim().ifBlank { "MEDIUM" })
                title = ""
                topic = ""
                difficulty = "MEDIUM"
            },
        ) { Text("Add problem") }
    }
}

@Composable
private fun DsaProblemRow(
    problem: DsaProblemEntity,
    onAttempt: (Long) -> Unit,
    onSolve: (Long) -> Unit,
    onMaster: (Long) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("${problem.title} · ${problem.status}")
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (problem.status == "NOT_STARTED") {
                OutlinedButton(onClick = { onAttempt(problem.id) }) { Text("Attempt") }
            }
            if (problem.status == "NOT_STARTED" || problem.status == "ATTEMPTED") {
                Button(onClick = { onSolve(problem.id) }) { Text("Solve") }
            }
            if (problem.status == "SOLVED") {
                Button(onClick = { onMaster(problem.id) }) { Text("Master") }
            }
        }
    }
}

@Composable
private fun SystemDesignTab(
    topics: List<SystemDesignTopicEntity>,
    onMarkConcept: (String, String, String) -> Unit,
) {
    val overall = CareerHubLogic.sdTopicsProgress(topics)
    val currentModule = CareerHubLogic.currentSdModule(topics)
    val colors = MaterialTheme.colorScheme
    val sortedTopics = topics.sortedBy { it.orderIndex }

    CareerCard(title = "System design") {
        Text("Overall progress: $overall%")
        LinearProgressIndicator(
            progress = { overall / 100f },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = colors.primary,
            trackColor = colors.surfaceContainerHighest,
        )
        currentModule?.let {
            Text(
                "Current module: $it",
                color = colors.primary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    if (sortedTopics.isEmpty()) {
        CareerCard(title = "Topics") {
            Text("No system design topics yet.", color = colors.onSurfaceVariant)
        }
    } else {
        sortedTopics.forEach { topic ->
            CareerCard(title = topic.title) {
                Text(CareerHubLogic.confidenceLabel(topic.confidence))
                LinearProgressIndicator(
                    progress = { topic.confidence / 100f },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = colors.primary,
                    trackColor = colors.surfaceContainerHighest,
                )
                topic.concepts.forEach { concept ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(concept.title, fontWeight = FontWeight.Bold)
                            Text(
                                concept.status,
                                color = colors.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                onMarkConcept(
                                    topic.id,
                                    concept.id,
                                    CareerHubLogic.nextConceptStatus(concept.status),
                                )
                            },
                        ) { Text(CareerHubLogic.nextConceptStatus(concept.status)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AreaProgressBar(label: String, percent: Int) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text("$percent%")
        }
        LinearProgressIndicator(
            progress = { percent / 100f },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = colors.primary,
            trackColor = colors.surfaceContainerHighest,
        )
    }
}

@Composable
private fun CareerCard(title: String, content: @Composable () -> Unit) {
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
