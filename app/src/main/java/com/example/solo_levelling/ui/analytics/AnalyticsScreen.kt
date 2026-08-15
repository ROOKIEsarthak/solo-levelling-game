package com.example.solo_levelling.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.example.solo_levelling.AppContainer
import com.example.solo_levelling.domain.service.AdaptiveSuggestion
import com.example.solo_levelling.domain.service.WeeklyReview
import com.example.solo_levelling.notifications.SystemNotifier
import kotlinx.coroutines.launch

@Composable
fun AnalyticsScreen(container: AppContainer) {
    var review by remember { mutableStateOf<WeeklyReview?>(null) }
    var suggestions by remember { mutableStateOf<List<AdaptiveSuggestion>>(emptyList()) }
    var exportPreview by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        review = container.analytics.weeklyReview()
        suggestions = container.adaptive.suggestions()
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Weekly Review", style = MaterialTheme.typography.headlineSmall)
        review?.let { r ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${r.weekStart} → ${r.weekEnd}")
                    Text("Quests: ${r.questsCompleted}/${r.questsTotal}")
                    LinearProgressIndicator(
                        progress = { r.completionRate },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("XP this week: ${r.xpEarned}")
                    r.recommendations.forEach { Text("• $it") }
                }
            }
        }

        Text("Adaptive suggestions", style = MaterialTheme.typography.titleMedium)
        suggestions.forEach { s ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(s.title, style = MaterialTheme.typography.titleSmall)
                    Text(s.detail)
                }
            }
        }

        Button(onClick = {
            scope.launch {
                exportPreview = container.analytics.exportJson().take(800)
            }
        }) { Text("Export data (preview)") }

        Button(onClick = {
            SystemNotifier.notify(
                context,
                "SYSTEM",
                "Weekly review ready. Open Analytics to inspect execution.",
            )
        }) { Text("Send test notification") }

        exportPreview?.let {
            Card(Modifier.fillMaxWidth()) {
                Text(it, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
