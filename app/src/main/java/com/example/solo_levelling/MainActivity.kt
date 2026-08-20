package com.example.solo_levelling

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.solo_levelling.ui.SoloLevellingAppRoot
import com.example.solo_levelling.ui.theme.SololevellingTheme
import com.example.solo_levelling.work.DailyQuestWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        DailyQuestWorker.schedule(applicationContext)
        lifecycleScope.launch(Dispatchers.IO) {
            appContainer.dayBoundaryCoordinator.ensureCatchUpAndSchedule(applicationContext)
        }
        setContent {
            SololevellingTheme {
                SoloLevellingAppRoot(container = appContainer)
            }
        }
    }
}
