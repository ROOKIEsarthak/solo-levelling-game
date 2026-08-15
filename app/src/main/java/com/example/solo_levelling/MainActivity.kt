package com.example.solo_levelling

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.solo_levelling.ui.SoloLevellingAppRoot
import com.example.solo_levelling.ui.theme.SololevellingTheme
import com.example.solo_levelling.work.DailyQuestWorker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        DailyQuestWorker.schedule(applicationContext)
        setContent {
            SololevellingTheme {
                SoloLevellingAppRoot(container = appContainer)
            }
        }
    }
}
