package com.example.solo_levelling.domain.handler

import android.content.Context
import com.example.solo_levelling.core.event.DomainEvent
import com.example.solo_levelling.core.event.EventBus
import com.example.solo_levelling.data.db.JsonDatabase
import com.example.solo_levelling.notifications.SystemNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class NotificationHandler(
    private val context: Context,
    private val eventBus: EventBus,
    private val db: JsonDatabase,
    private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch {
            eventBus.events.collect { event ->
                if (!notificationsEnabled()) return@collect
                when (event) {
                    is DomainEvent.LevelUp ->
                        SystemNotifier.notify(context, "Level Up!", "You reached level ${event.newLevel}")
                    is DomainEvent.RankUp ->
                        SystemNotifier.notify(context, "Rank Up!", "New rank: ${event.newRank}")
                    is DomainEvent.AchievementUnlocked ->
                        SystemNotifier.notify(context, "Achievement Unlocked", event.key)
                    is DomainEvent.DailyQuestsReady ->
                        SystemNotifier.notify(context, "Daily Quests Ready", "${event.count} quests available for ${event.date}")
                    is DomainEvent.BossCompleted ->
                        SystemNotifier.notify(context, "Boss Defeated!", "You earned ${event.xpReward} XP")
                    else -> Unit
                }
            }
        }
    }

    private suspend fun notificationsEnabled(): Boolean {
        val config = db.configDao().get("notifications_enabled")
        return config?.value != "false"
    }
}
