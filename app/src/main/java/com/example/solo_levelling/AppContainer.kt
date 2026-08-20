package com.example.solo_levelling

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import java.io.File
import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.core.time.AppClock
import com.example.solo_levelling.core.time.SystemAppClock
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.domain.handler.AchievementHandler
import com.example.solo_levelling.domain.handler.BossProgressHandler
import com.example.solo_levelling.domain.handler.NotificationHandler
import com.example.solo_levelling.domain.handler.SeasonHandler
import com.example.solo_levelling.domain.handler.StreakHandler
import com.example.solo_levelling.domain.port.CalendarPort
import com.example.solo_levelling.domain.port.LocalMetricIngest
import com.example.solo_levelling.domain.port.MetricIngestPort
import com.example.solo_levelling.domain.port.NoOpCalendarPort
import com.example.solo_levelling.domain.service.ActiveProgressionReader
import com.example.solo_levelling.domain.service.AdaptiveService
import com.example.solo_levelling.domain.service.AnalyticsService
import com.example.solo_levelling.domain.service.DayBoundaryCoordinator
import com.example.solo_levelling.domain.service.DayBoundaryService
import com.example.solo_levelling.domain.service.MilestoneVerificationService
import com.example.solo_levelling.domain.service.ModuleLifecycleService
import com.example.solo_levelling.domain.service.ModuleService
import com.example.solo_levelling.domain.service.OnboardingService
import com.example.solo_levelling.domain.service.PostQuestCompletionCoordinator
import com.example.solo_levelling.domain.service.ProgressionService
import com.example.solo_levelling.domain.service.QuestCompletionService
import com.example.solo_levelling.domain.service.QuestGenerationService
import com.example.solo_levelling.domain.service.QuestVerificationService
import com.example.solo_levelling.domain.service.SeasonService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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

    val db: JsonDatabase = JsonDatabase(File(appContext.filesDir, "db"))

    val progression = ProgressionService(db, eventBus, clock)
    val milestoneVerification = MilestoneVerificationService(db, progression)
    val adaptive = AdaptiveService(db, clock)
    val season = SeasonService(db, clock)
    val questGeneration = QuestGenerationService(db, clock, eventBus, adaptive, scope)

    private val streakHandler = StreakHandler(db, eventBus, clock, scope)
    private val achievementHandler = AchievementHandler(db, eventBus, clock, progression, scope)
    private val bossProgressHandler = BossProgressHandler(db, eventBus, progression, scope)
    private val notificationHandler = NotificationHandler(appContext, eventBus, db, scope)
    private val seasonHandler = SeasonHandler(eventBus, season, scope)

    val postQuest = PostQuestCompletionCoordinator(
        streakHandler = streakHandler,
        bossProgressHandler = bossProgressHandler,
        achievementHandler = achievementHandler,
        questGeneration = questGeneration,
        season = season,
    )

    val questCompletion = QuestCompletionService(
        db,
        eventBus,
        clock,
        progression,
        milestoneVerification,
        postQuest,
    )
    val questVerification = QuestVerificationService(db, clock, questCompletion)
    val onboarding = OnboardingService(db, clock, questGeneration, progression, season)
    val moduleLifecycle = ModuleLifecycleService(db, clock, onboarding)
    val modules = ModuleService(db, eventBus, clock, progression, questVerification)
    val analytics = AnalyticsService(db, clock)
    val activeProgression = ActiveProgressionReader(db)
    val dayBoundary = DayBoundaryService(db, eventBus, clock)
    val dayBoundaryCoordinator = DayBoundaryCoordinator(db, clock, dayBoundary, questGeneration)

    val metricIngest: MetricIngestPort = LocalMetricIngest(db, clock) {
        val profile = db.playerDao().getProfile(com.example.solo_levelling.core.config.SystemDefaults.PLAYER_ID)
        val zone = runCatching { ZoneId.of(profile?.timezone ?: ZoneId.systemDefault().id) }
            .getOrDefault(ZoneId.systemDefault())
        val today = clock.today(zone).format(DateTimeFormatter.ISO_LOCAL_DATE)
        questVerification.tryAutoComplete(today)
    }

    val calendarPort: CalendarPort = NoOpCalendarPort()

    fun start() {
        streakHandler.start()
        achievementHandler.start()
        bossProgressHandler.start()
        questGeneration.start()
        notificationHandler.start()
        seasonHandler.start()
        scope.launch {
            onboarding.ensureSeeded()
            onboarding.migrateModuleFlagsIfNeeded()
            val enabled = onboarding.currentModules()
            if (enabled.career) {
                modules.ensureCareerCatalogsSeeded()
            }
            season.ensureActiveSeason()
            dayBoundaryCoordinator.syncTimezoneFromDevice()
            val profile = db.playerDao().getProfile(com.example.solo_levelling.core.config.SystemDefaults.PLAYER_ID)
            if (profile?.onboardingDone == true) {
                dayBoundaryCoordinator.runBoundaryIfNeeded(profile.timezone)
                questGeneration.generateForToday(profile.timezone)
            }
        }
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as SoloLevellingApp).container
