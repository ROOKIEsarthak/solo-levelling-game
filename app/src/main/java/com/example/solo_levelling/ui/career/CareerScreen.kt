package com.example.solo_levelling.ui.career

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.data.db.entity.CareerNodeEntity
import com.example.solo_levelling.data.db.entity.DsaProblemEntity
import com.example.solo_levelling.data.db.entity.SystemDesignTopicEntity
import com.example.solo_levelling.domain.service.CareerHubLogic
import com.example.solo_levelling.domain.service.EntryValidation
import com.example.solo_levelling.ui.components.BracketLabel
import com.example.solo_levelling.ui.components.CyberProgressBar
import com.example.solo_levelling.ui.components.EnergyFieldBackground
import com.example.solo_levelling.ui.components.GlassLevel
import com.example.solo_levelling.ui.components.GlassSurface
import com.example.solo_levelling.ui.components.SystemActionButton
import com.example.solo_levelling.ui.components.SystemIdleEmpty
import com.example.solo_levelling.ui.components.SovereignChip
import com.example.solo_levelling.ui.components.SystemSectionHeader
import com.example.solo_levelling.ui.components.progressFraction
import com.example.solo_levelling.ui.theme.JetBrainsMono
import com.example.solo_levelling.ui.theme.Spacing
import com.example.solo_levelling.ui.theme.SystemError
import com.example.solo_levelling.ui.theme.SystemPrimary
import com.example.solo_levelling.ui.theme.SystemSecondary
import com.example.solo_levelling.ui.theme.SystemSuccess
import com.example.solo_levelling.ui.theme.SystemTertiary
import kotlinx.coroutines.launch

enum class CareerTab { Roadmap, Dsa, SystemDesign }

internal enum class TimelineNodeState { Cleared, Active, Locked }

internal fun timelineNodeState(status: String): TimelineNodeState = when (status.uppercase()) {
    "MASTERED", "CLEARED", "SOLVED", "KNOWN" -> TimelineNodeState.Cleared
    "ACTIVE", "IN_PROGRESS", "ATTEMPTED", "LEARNING", "STARTED" -> TimelineNodeState.Active
    else -> TimelineNodeState.Locked
}

internal fun timelineNodeAccent(state: TimelineNodeState): Color = when (state) {
    TimelineNodeState.Cleared -> SystemSuccess
    TimelineNodeState.Active -> SystemPrimary
    TimelineNodeState.Locked -> SystemTertiary.copy(alpha = 0.5f)
}

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

    var selectedTab by remember { mutableStateOf(initialTab) }
    LaunchedEffect(initialTab) { selectedTab = initialTab }

    Box(Modifier.fillMaxSize()) {
        EnergyFieldBackground(Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.screen),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            SystemSectionHeader(tag = "CAREER HUB", accent = SystemPrimary)
            Text(
                "Roadmap · DSA · System Design",
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = JetBrainsMono,
            )

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                CareerTab.entries.forEach { tab ->
                    SovereignChip(
                        label = when (tab) {
                            CareerTab.Roadmap -> "Roadmap"
                            CareerTab.Dsa -> "DSA"
                            CareerTab.SystemDesign -> "System Design"
                        },
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                    )
                }
            }

            when (selectedTab) {
                CareerTab.Roadmap -> RoadmapTab(
                    careerNodes = careerNodes,
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

            Spacer(Modifier.height(Spacing.xs))
        }
    }
}

@Composable
private fun RoadmapTab(
    careerNodes: List<CareerNodeEntity>,
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

    CareerGlassCard(title = "CAREER PATH") {
        Text(
            "${currentRole.ifBlank { "Current role" }} → ${targetRole.ifBlank { "Target role" }}",
            fontWeight = FontWeight.Bold,
            fontFamily = JetBrainsMono,
            color = SystemPrimary,
        )
        if (nextGoal.isNotBlank()) {
            BracketLabel(text = "NEXT GOAL", color = SystemSecondary)
            Text(nextGoal, color = SystemPrimary, fontWeight = FontWeight.Bold)
        }
        if (goalReason.isNotBlank()) {
            Text(goalReason, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }

    SystemSectionHeader(tag = "ROADMAP TIMELINE", accent = SystemSecondary)

    if (careerNodes.isEmpty()) {
        SystemIdleEmpty(
            title = "Timeline idle",
            subtitle = "Career nodes will appear as your path is configured.",
        )
    } else {
        careerNodes.groupBy { it.track }.forEach { (track, trackNodes) ->
            GlassSurface(modifier = Modifier.fillMaxWidth(), level = GlassLevel.Level1) {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    BracketLabel(text = track.uppercase(), color = SystemSecondary)
                    Spacer(Modifier.height(8.dp))
                    trackNodes.sortedBy { it.orderIndex }.forEachIndexed { index, node ->
                        TimelineNodeRow(
                            node = node,
                            isLast = index == trackNodes.lastIndex,
                        )
                    }
                }
            }
        }
    }

    CareerGlassCard(title = "AREA PROGRESS") {
        AreaProgressBar("DSA", dsaPct)
        AreaProgressBar("System Design", sdPct)
        AreaProgressBar("Backend", backendPct)
        AreaProgressBar("Behavioral", behavioralPct)
    }

    if (nextPriority != null) {
        GlassSurface(modifier = Modifier.fillMaxWidth(), level = GlassLevel.Level2, borderAlpha = 0.35f) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SystemSectionHeader(tag = "NEXT PRIORITY", accent = SystemTertiary)
                Text(
                    "${nextPriority.first} — ${nextPriority.second}%",
                    color = SystemPrimary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = JetBrainsMono,
                )
                Text(
                    "Lowest mandatory focus area. Strengthen this next.",
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun TimelineNodeRow(node: CareerNodeEntity, isLast: Boolean) {
    val colors = MaterialTheme.colorScheme
    val state = timelineNodeState(node.status)
    val accent = timelineNodeAccent(state)
    val statusLabel = when (state) {
        TimelineNodeState.Cleared -> "CLEARED"
        TimelineNodeState.Active -> "ACTIVE"
        TimelineNodeState.Locked -> "LOCKED"
    }

    Row(Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = if (state == TimelineNodeState.Locked) 0.25f else 0.85f))
                    .border(1.dp, accent, CircleShape),
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(36.dp)
                        .background(accent.copy(alpha = 0.35f)),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(
            Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(node.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                BracketLabel(text = statusLabel, color = accent)
            }
            if (node.description.isNotBlank()) {
                Text(
                    node.description,
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
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

    CareerGlassCard(title = "DSA OVERVIEW") {
        if (currentGoal.isNotBlank()) {
            BracketLabel(text = "CURRENT GOAL", color = SystemSecondary)
            Text(currentGoal, fontWeight = FontWeight.Bold)
        }
        Text(
            "Overall progress: $overall%",
            fontFamily = JetBrainsMono,
            color = SystemPrimary,
        )
        CyberProgressBar(progress = overall / 100f)
        Text(
            "Needs review: $reviewCount",
            color = colors.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = JetBrainsMono,
        )
    }

    if (nextProblem != null) {
        GlassSurface(modifier = Modifier.fillMaxWidth(), level = GlassLevel.Level2, borderAlpha = 0.35f) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SystemSectionHeader(tag = "RECOMMENDED", accent = SystemPrimary)
                Text(nextProblem.title, fontWeight = FontWeight.Bold)
                Text(
                    "${nextProblem.topic.ifBlank { "General" }} · ${nextProblem.difficulty}",
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = JetBrainsMono,
                )
                Text(
                    "Why? Next unfinished problem in ${currentTopic ?: "your current topic"} — keep the pattern progression moving.",
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    } else if (currentTopic != null) {
        CareerGlassCard(title = "TODAY'S OBJECTIVE") {
            Text("Continue $currentTopic — pick a problem below", style = MaterialTheme.typography.bodySmall)
        }
    }

    topics.forEach { (topicName, pct) ->
        CareerGlassCard(title = topicName.uppercase()) {
            Text("$pct% complete", fontFamily = JetBrainsMono, color = SystemPrimary)
            CyberProgressBar(progress = pct / 100f, height = 6.dp)
            dsa.filter { it.topic.equals(topicName, ignoreCase = true) || (topicName == "General" && it.topic.isBlank()) }
                .forEach { problem ->
                    DsaProblemRow(problem, onAttempt, onSolve, onMaster)
                }
        }
    }

    if (dsa.isEmpty()) {
        SystemIdleEmpty(
            title = "No problems",
            subtitle = "Add your first DSA problem below.",
        )
    }

    CareerGlassCard(title = "ADD PROBLEM") {
        OutlinedTextField(title, { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(topic, { topic = it }, label = { Text("Topic") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(difficulty, { difficulty = it }, label = { Text("Difficulty") }, modifier = Modifier.fillMaxWidth())
        SystemActionButton(
            label = "ADD PROBLEM",
            onClick = {
                val error = EntryValidation.firstError(
                    EntryValidation.requireNonBlank(title, "title"),
                    EntryValidation.requireNonBlank(topic, "topic"),
                )
                if (error != null) {
                    onMessage(error)
                    return@SystemActionButton
                }
                onAdd(title.trim(), topic.trim(), difficulty.trim().ifBlank { "MEDIUM" })
                title = ""
                topic = ""
                difficulty = "MEDIUM"
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun DsaProblemRow(
    problem: DsaProblemEntity,
    onAttempt: (Long) -> Unit,
    onSolve: (Long) -> Unit,
    onMaster: (Long) -> Unit,
) {
    val statusColor = when (problem.status) {
        "MASTERED" -> SystemSuccess
        "SOLVED" -> SystemPrimary
        "ATTEMPTED" -> SystemTertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(problem.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            BracketLabel(text = problem.status, color = statusColor)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (problem.status == "NOT_STARTED") {
                SystemActionButton(label = "ATTEMPT", onClick = { onAttempt(problem.id) }, primary = false)
            }
            if (problem.status == "NOT_STARTED" || problem.status == "ATTEMPTED") {
                SystemActionButton(label = "SOLVE", onClick = { onSolve(problem.id) })
            }
            if (problem.status == "SOLVED") {
                SystemActionButton(label = "MASTER", onClick = { onMaster(problem.id) })
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

    CareerGlassCard(title = "SYSTEM DESIGN") {
        Text("Overall progress: $overall%", fontFamily = JetBrainsMono, color = SystemPrimary)
        CyberProgressBar(progress = overall / 100f)
        currentModule?.let {
            BracketLabel(text = "CURRENT MODULE", color = SystemSecondary)
            Text(it, color = SystemPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
        }
    }

    if (sortedTopics.isEmpty()) {
        SystemIdleEmpty(
            title = "No topics",
            subtitle = "System design modules will appear here.",
        )
    } else {
        sortedTopics.forEach { topic ->
            CareerGlassCard(title = topic.title.uppercase()) {
                BracketLabel(
                    text = CareerHubLogic.confidenceLabel(topic.confidence),
                    color = if (topic.confidence >= 80) SystemSuccess else SystemPrimary,
                )
                CyberProgressBar(progress = topic.confidence / 100f, height = 6.dp)
                topic.concepts.forEach { concept ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(concept.title, fontWeight = FontWeight.Bold)
                            BracketLabel(
                                text = concept.status,
                                color = when (concept.status) {
                                    "MASTERED", "KNOWN" -> SystemSuccess
                                    "LEARNING" -> SystemTertiary
                                    else -> colors.onSurfaceVariant
                                },
                            )
                        }
                        SystemActionButton(
                            label = CareerHubLogic.nextConceptStatus(concept.status),
                            onClick = {
                                onMarkConcept(
                                    topic.id,
                                    concept.id,
                                    CareerHubLogic.nextConceptStatus(concept.status),
                                )
                            },
                            primary = false,
                        )
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
            Text(label, fontFamily = JetBrainsMono)
            Text("$percent%", fontFamily = JetBrainsMono, color = SystemPrimary)
        }
        CyberProgressBar(progress = progressFraction(percent.toFloat(), 100f), height = 6.dp)
        if (percent < 30) {
            BracketLabel(text = "Entry required", color = SystemError.copy(alpha = 0.85f))
        }
    }
}

@Composable
private fun CareerGlassCard(title: String, content: @Composable () -> Unit) {
    GlassSurface(modifier = Modifier.fillMaxWidth(), level = GlassLevel.Level1) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SystemSectionHeader(tag = title, accent = SystemPrimary)
            content()
        }
    }
}
