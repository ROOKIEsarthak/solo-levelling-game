package com.example.solo_levelling

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.room.Room
import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.AppClock
import com.example.solo_levelling.core.time.SystemAppClock
import com.example.solo_levelling.data.db.AppDatabase
import com.example.solo_levelling.domain.handler.AchievementHandler
import com.example.solo_levelling.domain.handler.StreakHandler
import com.example.solo_levelling.domain.service.AdaptiveService
import com.example.solo_levelling.domain.service.AnalyticsService
import com.example.solo_levelling.domain.service.ModuleService
import com.example.solo_levelling.domain.service.OnboardingService
import com.example.solo_levelling.domain.service.QuestCompletionService
import com.example.solo_levelling.domain.service.QuestGenerationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SoloLevellingApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannel()
        container.start()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFY_CHANNEL_ID,
                "System",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Quest and progression notices" }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        const val NOTIFY_CHANNEL_ID = "system_events"
    }
}

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val clock: AppClock = SystemAppClock()
    val eventBus = EventBus()

    val db: AppDatabase = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        "solo_levelling.db",
    ).fallbackToDestructiveMigration(dropAllTables = true).build()

    val questGeneration = QuestGenerationService(db, clock)
    val questCompletion = QuestCompletionService(db, eventBus, clock)
    val onboarding = OnboardingService(db, clock, questGeneration)
    val modules = ModuleService(db, eventBus, clock)
    val analytics = AnalyticsService(db, clock)
    val adaptive = AdaptiveService(db)

    private val streakHandler = StreakHandler(db, eventBus, clock, scope)
    private val achievementHandler = AchievementHandler(db, eventBus, clock, scope)

    fun start() {
        streakHandler.start()
        achievementHandler.start()
        scope.launch {
            onboarding.ensureSeeded()
            val profile = db.playerDao().getProfile(com.example.solo_levelling.core.config.SystemDefaults.PLAYER_ID)
            if (profile?.onboardingDone == true) {
                questGeneration.generateForToday(profile.timezone)
            }
        }
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as SoloLevellingApp).container
