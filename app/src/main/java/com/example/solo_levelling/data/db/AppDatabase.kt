package com.example.solo_levelling.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.solo_levelling.data.db.dao.AchievementDao
import com.example.solo_levelling.data.db.dao.ModuleDao
import com.example.solo_levelling.data.db.dao.PlayerDao
import com.example.solo_levelling.data.db.dao.QuestDao
import com.example.solo_levelling.data.db.dao.XpDao
import com.example.solo_levelling.data.db.entity.AchievementDefEntity
import com.example.solo_levelling.data.db.entity.AttributeStatEntity
import com.example.solo_levelling.data.db.entity.BossEntity
import com.example.solo_levelling.data.db.entity.DsaProblemEntity
import com.example.solo_levelling.data.db.entity.FocusSessionEntity
import com.example.solo_levelling.data.db.entity.JournalEntryEntity
import com.example.solo_levelling.data.db.entity.MetricLogEntity
import com.example.solo_levelling.data.db.entity.NutritionLogEntity
import com.example.solo_levelling.data.db.entity.PlayerAchievementEntity
import com.example.solo_levelling.data.db.entity.PlayerProfileEntity
import com.example.solo_levelling.data.db.entity.QuestInstanceEntity
import com.example.solo_levelling.data.db.entity.QuestTemplateEntity
import com.example.solo_levelling.data.db.entity.SkillEntity
import com.example.solo_levelling.data.db.entity.StreakStateEntity
import com.example.solo_levelling.data.db.entity.WorkoutEntity
import com.example.solo_levelling.data.db.entity.XpLedgerEntryEntity

@Database(
    entities = [
        PlayerProfileEntity::class,
        AttributeStatEntity::class,
        QuestTemplateEntity::class,
        QuestInstanceEntity::class,
        XpLedgerEntryEntity::class,
        AchievementDefEntity::class,
        PlayerAchievementEntity::class,
        StreakStateEntity::class,
        BossEntity::class,
        SkillEntity::class,
        DsaProblemEntity::class,
        WorkoutEntity::class,
        NutritionLogEntity::class,
        FocusSessionEntity::class,
        JournalEntryEntity::class,
        MetricLogEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun questDao(): QuestDao
    abstract fun xpDao(): XpDao
    abstract fun achievementDao(): AchievementDao
    abstract fun moduleDao(): ModuleDao
}
