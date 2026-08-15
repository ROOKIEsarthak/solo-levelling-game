package com.example.solo_levelling.`data`.db

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.example.solo_levelling.`data`.db.dao.AchievementDao
import com.example.solo_levelling.`data`.db.dao.AchievementDao_Impl
import com.example.solo_levelling.`data`.db.dao.ModuleDao
import com.example.solo_levelling.`data`.db.dao.ModuleDao_Impl
import com.example.solo_levelling.`data`.db.dao.PlayerDao
import com.example.solo_levelling.`data`.db.dao.PlayerDao_Impl
import com.example.solo_levelling.`data`.db.dao.QuestDao
import com.example.solo_levelling.`data`.db.dao.QuestDao_Impl
import com.example.solo_levelling.`data`.db.dao.XpDao
import com.example.solo_levelling.`data`.db.dao.XpDao_Impl
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AppDatabase_Impl : AppDatabase() {
  private val _playerDao: Lazy<PlayerDao> = lazy {
    PlayerDao_Impl(this)
  }

  private val _questDao: Lazy<QuestDao> = lazy {
    QuestDao_Impl(this)
  }

  private val _xpDao: Lazy<XpDao> = lazy {
    XpDao_Impl(this)
  }

  private val _achievementDao: Lazy<AchievementDao> = lazy {
    AchievementDao_Impl(this)
  }

  private val _moduleDao: Lazy<ModuleDao> = lazy {
    ModuleDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(1,
        "ffd5bccbffbd4076fc5804b61b78af78", "fc9e5d8da503c862dd29d3e334c4c18a") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `player_profile` (`id` INTEGER NOT NULL, `name` TEXT NOT NULL, `level` INTEGER NOT NULL, `totalXp` INTEGER NOT NULL, `rank` TEXT NOT NULL, `timezone` TEXT NOT NULL, `onboardingDone` INTEGER NOT NULL, `prioritiesCsv` TEXT NOT NULL, `createdAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `attribute_stats` (`code` TEXT NOT NULL, `currentValue` INTEGER NOT NULL, `lifetimeXp` INTEGER NOT NULL, PRIMARY KEY(`code`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `quest_templates` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `key` TEXT NOT NULL, `type` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `baseXp` INTEGER NOT NULL, `attributeRewardsJson` TEXT NOT NULL, `scheduleDaysCsv` TEXT NOT NULL, `active` INTEGER NOT NULL, `verificationType` TEXT NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `quest_instances` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `templateId` INTEGER NOT NULL, `scheduledDate` TEXT NOT NULL, `status` TEXT NOT NULL, `title` TEXT NOT NULL, `type` TEXT NOT NULL, `baseXp` INTEGER NOT NULL, `attributeRewardsJson` TEXT NOT NULL, `completedAtEpochMs` INTEGER)")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_quest_instances_templateId_scheduledDate` ON `quest_instances` (`templateId`, `scheduledDate`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `xp_ledger` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `amount` INTEGER NOT NULL, `sourceType` TEXT NOT NULL, `sourceId` TEXT NOT NULL, `metadataJson` TEXT NOT NULL, `createdAtEpochMs` INTEGER NOT NULL)")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_xp_ledger_sourceType_sourceId` ON `xp_ledger` (`sourceType`, `sourceId`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `achievement_defs` (`key` TEXT NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL, `criteriaType` TEXT NOT NULL, `criteriaValue` INTEGER NOT NULL, `rewardXp` INTEGER NOT NULL, PRIMARY KEY(`key`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `player_achievements` (`achievementKey` TEXT NOT NULL, `unlockedAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`achievementKey`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `streak_state` (`id` INTEGER NOT NULL, `current` INTEGER NOT NULL, `best` INTEGER NOT NULL, `lastCompletedDate` TEXT, `recoveryUsedThisWeek` INTEGER NOT NULL, `weekStartDate` TEXT, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `bosses` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `targetValue` REAL NOT NULL, `currentValue` REAL NOT NULL, `xpReward` INTEGER NOT NULL, `status` TEXT NOT NULL, `deadlineDate` TEXT)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `skills` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `domain` TEXT NOT NULL, `name` TEXT NOT NULL, `xp` INTEGER NOT NULL, `level` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `dsa_problems` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `platform` TEXT NOT NULL, `externalId` TEXT NOT NULL, `difficulty` TEXT NOT NULL, `topic` TEXT NOT NULL, `status` TEXT NOT NULL, `attempts` INTEGER NOT NULL, `confidence` INTEGER NOT NULL)")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_dsa_problems_platform_externalId` ON `dsa_problems` (`platform`, `externalId`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `workouts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT NOT NULL, `type` TEXT NOT NULL, `durationMinutes` INTEGER NOT NULL, `notes` TEXT NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `nutrition_logs` (`date` TEXT NOT NULL, `calories` INTEGER NOT NULL, `protein` INTEGER NOT NULL, `carbs` INTEGER NOT NULL, `fat` INTEGER NOT NULL, PRIMARY KEY(`date`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `focus_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT NOT NULL, `durationMinutes` INTEGER NOT NULL, `label` TEXT NOT NULL, `completedAtEpochMs` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `journal_entries` (`date` TEXT NOT NULL, `content` TEXT NOT NULL, `updatedAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`date`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `metric_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `metricType` TEXT NOT NULL, `value` REAL NOT NULL, `recordedAtEpochMs` INTEGER NOT NULL, `date` TEXT NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'ffd5bccbffbd4076fc5804b61b78af78')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `player_profile`")
        connection.execSQL("DROP TABLE IF EXISTS `attribute_stats`")
        connection.execSQL("DROP TABLE IF EXISTS `quest_templates`")
        connection.execSQL("DROP TABLE IF EXISTS `quest_instances`")
        connection.execSQL("DROP TABLE IF EXISTS `xp_ledger`")
        connection.execSQL("DROP TABLE IF EXISTS `achievement_defs`")
        connection.execSQL("DROP TABLE IF EXISTS `player_achievements`")
        connection.execSQL("DROP TABLE IF EXISTS `streak_state`")
        connection.execSQL("DROP TABLE IF EXISTS `bosses`")
        connection.execSQL("DROP TABLE IF EXISTS `skills`")
        connection.execSQL("DROP TABLE IF EXISTS `dsa_problems`")
        connection.execSQL("DROP TABLE IF EXISTS `workouts`")
        connection.execSQL("DROP TABLE IF EXISTS `nutrition_logs`")
        connection.execSQL("DROP TABLE IF EXISTS `focus_sessions`")
        connection.execSQL("DROP TABLE IF EXISTS `journal_entries`")
        connection.execSQL("DROP TABLE IF EXISTS `metric_logs`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection):
          RoomOpenDelegate.ValidationResult {
        val _columnsPlayerProfile: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsPlayerProfile.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPlayerProfile.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPlayerProfile.put("level", TableInfo.Column("level", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPlayerProfile.put("totalXp", TableInfo.Column("totalXp", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPlayerProfile.put("rank", TableInfo.Column("rank", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPlayerProfile.put("timezone", TableInfo.Column("timezone", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPlayerProfile.put("onboardingDone", TableInfo.Column("onboardingDone", "INTEGER",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPlayerProfile.put("prioritiesCsv", TableInfo.Column("prioritiesCsv", "TEXT", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPlayerProfile.put("createdAtEpochMs", TableInfo.Column("createdAtEpochMs",
            "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysPlayerProfile: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesPlayerProfile: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoPlayerProfile: TableInfo = TableInfo("player_profile", _columnsPlayerProfile,
            _foreignKeysPlayerProfile, _indicesPlayerProfile)
        val _existingPlayerProfile: TableInfo = read(connection, "player_profile")
        if (!_infoPlayerProfile.equals(_existingPlayerProfile)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |player_profile(com.example.solo_levelling.data.db.entity.PlayerProfileEntity).
              | Expected:
              |""".trimMargin() + _infoPlayerProfile + """
              |
              | Found:
              |""".trimMargin() + _existingPlayerProfile)
        }
        val _columnsAttributeStats: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsAttributeStats.put("code", TableInfo.Column("code", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAttributeStats.put("currentValue", TableInfo.Column("currentValue", "INTEGER", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAttributeStats.put("lifetimeXp", TableInfo.Column("lifetimeXp", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysAttributeStats: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesAttributeStats: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoAttributeStats: TableInfo = TableInfo("attribute_stats", _columnsAttributeStats,
            _foreignKeysAttributeStats, _indicesAttributeStats)
        val _existingAttributeStats: TableInfo = read(connection, "attribute_stats")
        if (!_infoAttributeStats.equals(_existingAttributeStats)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |attribute_stats(com.example.solo_levelling.data.db.entity.AttributeStatEntity).
              | Expected:
              |""".trimMargin() + _infoAttributeStats + """
              |
              | Found:
              |""".trimMargin() + _existingAttributeStats)
        }
        val _columnsQuestTemplates: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsQuestTemplates.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsQuestTemplates.put("key", TableInfo.Column("key", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsQuestTemplates.put("type", TableInfo.Column("type", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsQuestTemplates.put("title", TableInfo.Column("title", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsQuestTemplates.put("description", TableInfo.Column("description", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsQuestTemplates.put("baseXp", TableInfo.Column("baseXp", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsQuestTemplates.put("attributeRewardsJson", TableInfo.Column("attributeRewardsJson",
            "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsQuestTemplates.put("scheduleDaysCsv", TableInfo.Column("scheduleDaysCsv", "TEXT",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsQuestTemplates.put("active", TableInfo.Column("active", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsQuestTemplates.put("verificationType", TableInfo.Column("verificationType", "TEXT",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysQuestTemplates: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesQuestTemplates: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoQuestTemplates: TableInfo = TableInfo("quest_templates", _columnsQuestTemplates,
            _foreignKeysQuestTemplates, _indicesQuestTemplates)
        val _existingQuestTemplates: TableInfo = read(connection, "quest_templates")
        if (!_infoQuestTemplates.equals(_existingQuestTemplates)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |quest_templates(com.example.solo_levelling.data.db.entity.QuestTemplateEntity).
              | Expected:
              |""".trimMargin() + _infoQuestTemplates + """
              |
              | Found:
              |""".trimMargin() + _existingQuestTemplates)
        }
        val _columnsQuestInstances: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsQuestInstances.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsQuestInstances.put("templateId", TableInfo.Column("templateId", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsQuestInstances.put("scheduledDate", TableInfo.Column("scheduledDate", "TEXT", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsQuestInstances.put("status", TableInfo.Column("status", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsQuestInstances.put("title", TableInfo.Column("title", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsQuestInstances.put("type", TableInfo.Column("type", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsQuestInstances.put("baseXp", TableInfo.Column("baseXp", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsQuestInstances.put("attributeRewardsJson", TableInfo.Column("attributeRewardsJson",
            "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsQuestInstances.put("completedAtEpochMs", TableInfo.Column("completedAtEpochMs",
            "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysQuestInstances: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesQuestInstances: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesQuestInstances.add(TableInfo.Index("index_quest_instances_templateId_scheduledDate",
            true, listOf("templateId", "scheduledDate"), listOf("ASC", "ASC")))
        val _infoQuestInstances: TableInfo = TableInfo("quest_instances", _columnsQuestInstances,
            _foreignKeysQuestInstances, _indicesQuestInstances)
        val _existingQuestInstances: TableInfo = read(connection, "quest_instances")
        if (!_infoQuestInstances.equals(_existingQuestInstances)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |quest_instances(com.example.solo_levelling.data.db.entity.QuestInstanceEntity).
              | Expected:
              |""".trimMargin() + _infoQuestInstances + """
              |
              | Found:
              |""".trimMargin() + _existingQuestInstances)
        }
        val _columnsXpLedger: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsXpLedger.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsXpLedger.put("amount", TableInfo.Column("amount", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsXpLedger.put("sourceType", TableInfo.Column("sourceType", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsXpLedger.put("sourceId", TableInfo.Column("sourceId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsXpLedger.put("metadataJson", TableInfo.Column("metadataJson", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsXpLedger.put("createdAtEpochMs", TableInfo.Column("createdAtEpochMs", "INTEGER",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysXpLedger: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesXpLedger: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesXpLedger.add(TableInfo.Index("index_xp_ledger_sourceType_sourceId", true,
            listOf("sourceType", "sourceId"), listOf("ASC", "ASC")))
        val _infoXpLedger: TableInfo = TableInfo("xp_ledger", _columnsXpLedger,
            _foreignKeysXpLedger, _indicesXpLedger)
        val _existingXpLedger: TableInfo = read(connection, "xp_ledger")
        if (!_infoXpLedger.equals(_existingXpLedger)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |xp_ledger(com.example.solo_levelling.data.db.entity.XpLedgerEntryEntity).
              | Expected:
              |""".trimMargin() + _infoXpLedger + """
              |
              | Found:
              |""".trimMargin() + _existingXpLedger)
        }
        val _columnsAchievementDefs: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsAchievementDefs.put("key", TableInfo.Column("key", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAchievementDefs.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsAchievementDefs.put("description", TableInfo.Column("description", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAchievementDefs.put("criteriaType", TableInfo.Column("criteriaType", "TEXT", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAchievementDefs.put("criteriaValue", TableInfo.Column("criteriaValue", "INTEGER",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsAchievementDefs.put("rewardXp", TableInfo.Column("rewardXp", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysAchievementDefs: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesAchievementDefs: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoAchievementDefs: TableInfo = TableInfo("achievement_defs", _columnsAchievementDefs,
            _foreignKeysAchievementDefs, _indicesAchievementDefs)
        val _existingAchievementDefs: TableInfo = read(connection, "achievement_defs")
        if (!_infoAchievementDefs.equals(_existingAchievementDefs)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |achievement_defs(com.example.solo_levelling.data.db.entity.AchievementDefEntity).
              | Expected:
              |""".trimMargin() + _infoAchievementDefs + """
              |
              | Found:
              |""".trimMargin() + _existingAchievementDefs)
        }
        val _columnsPlayerAchievements: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsPlayerAchievements.put("achievementKey", TableInfo.Column("achievementKey", "TEXT",
            true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPlayerAchievements.put("unlockedAtEpochMs", TableInfo.Column("unlockedAtEpochMs",
            "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysPlayerAchievements: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesPlayerAchievements: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoPlayerAchievements: TableInfo = TableInfo("player_achievements",
            _columnsPlayerAchievements, _foreignKeysPlayerAchievements, _indicesPlayerAchievements)
        val _existingPlayerAchievements: TableInfo = read(connection, "player_achievements")
        if (!_infoPlayerAchievements.equals(_existingPlayerAchievements)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |player_achievements(com.example.solo_levelling.data.db.entity.PlayerAchievementEntity).
              | Expected:
              |""".trimMargin() + _infoPlayerAchievements + """
              |
              | Found:
              |""".trimMargin() + _existingPlayerAchievements)
        }
        val _columnsStreakState: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsStreakState.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsStreakState.put("current", TableInfo.Column("current", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsStreakState.put("best", TableInfo.Column("best", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsStreakState.put("lastCompletedDate", TableInfo.Column("lastCompletedDate", "TEXT",
            false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStreakState.put("recoveryUsedThisWeek", TableInfo.Column("recoveryUsedThisWeek",
            "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsStreakState.put("weekStartDate", TableInfo.Column("weekStartDate", "TEXT", false, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysStreakState: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesStreakState: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoStreakState: TableInfo = TableInfo("streak_state", _columnsStreakState,
            _foreignKeysStreakState, _indicesStreakState)
        val _existingStreakState: TableInfo = read(connection, "streak_state")
        if (!_infoStreakState.equals(_existingStreakState)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |streak_state(com.example.solo_levelling.data.db.entity.StreakStateEntity).
              | Expected:
              |""".trimMargin() + _infoStreakState + """
              |
              | Found:
              |""".trimMargin() + _existingStreakState)
        }
        val _columnsBosses: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsBosses.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBosses.put("title", TableInfo.Column("title", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBosses.put("description", TableInfo.Column("description", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBosses.put("targetValue", TableInfo.Column("targetValue", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBosses.put("currentValue", TableInfo.Column("currentValue", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBosses.put("xpReward", TableInfo.Column("xpReward", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBosses.put("status", TableInfo.Column("status", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsBosses.put("deadlineDate", TableInfo.Column("deadlineDate", "TEXT", false, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysBosses: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesBosses: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoBosses: TableInfo = TableInfo("bosses", _columnsBosses, _foreignKeysBosses,
            _indicesBosses)
        val _existingBosses: TableInfo = read(connection, "bosses")
        if (!_infoBosses.equals(_existingBosses)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |bosses(com.example.solo_levelling.data.db.entity.BossEntity).
              | Expected:
              |""".trimMargin() + _infoBosses + """
              |
              | Found:
              |""".trimMargin() + _existingBosses)
        }
        val _columnsSkills: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsSkills.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSkills.put("domain", TableInfo.Column("domain", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSkills.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSkills.put("xp", TableInfo.Column("xp", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsSkills.put("level", TableInfo.Column("level", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysSkills: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesSkills: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoSkills: TableInfo = TableInfo("skills", _columnsSkills, _foreignKeysSkills,
            _indicesSkills)
        val _existingSkills: TableInfo = read(connection, "skills")
        if (!_infoSkills.equals(_existingSkills)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |skills(com.example.solo_levelling.data.db.entity.SkillEntity).
              | Expected:
              |""".trimMargin() + _infoSkills + """
              |
              | Found:
              |""".trimMargin() + _existingSkills)
        }
        val _columnsDsaProblems: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsDsaProblems.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDsaProblems.put("title", TableInfo.Column("title", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDsaProblems.put("platform", TableInfo.Column("platform", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDsaProblems.put("externalId", TableInfo.Column("externalId", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDsaProblems.put("difficulty", TableInfo.Column("difficulty", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDsaProblems.put("topic", TableInfo.Column("topic", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDsaProblems.put("status", TableInfo.Column("status", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDsaProblems.put("attempts", TableInfo.Column("attempts", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsDsaProblems.put("confidence", TableInfo.Column("confidence", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysDsaProblems: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesDsaProblems: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesDsaProblems.add(TableInfo.Index("index_dsa_problems_platform_externalId", true,
            listOf("platform", "externalId"), listOf("ASC", "ASC")))
        val _infoDsaProblems: TableInfo = TableInfo("dsa_problems", _columnsDsaProblems,
            _foreignKeysDsaProblems, _indicesDsaProblems)
        val _existingDsaProblems: TableInfo = read(connection, "dsa_problems")
        if (!_infoDsaProblems.equals(_existingDsaProblems)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |dsa_problems(com.example.solo_levelling.data.db.entity.DsaProblemEntity).
              | Expected:
              |""".trimMargin() + _infoDsaProblems + """
              |
              | Found:
              |""".trimMargin() + _existingDsaProblems)
        }
        val _columnsWorkouts: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsWorkouts.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsWorkouts.put("date", TableInfo.Column("date", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsWorkouts.put("type", TableInfo.Column("type", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsWorkouts.put("durationMinutes", TableInfo.Column("durationMinutes", "INTEGER", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsWorkouts.put("notes", TableInfo.Column("notes", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysWorkouts: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesWorkouts: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoWorkouts: TableInfo = TableInfo("workouts", _columnsWorkouts, _foreignKeysWorkouts,
            _indicesWorkouts)
        val _existingWorkouts: TableInfo = read(connection, "workouts")
        if (!_infoWorkouts.equals(_existingWorkouts)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |workouts(com.example.solo_levelling.data.db.entity.WorkoutEntity).
              | Expected:
              |""".trimMargin() + _infoWorkouts + """
              |
              | Found:
              |""".trimMargin() + _existingWorkouts)
        }
        val _columnsNutritionLogs: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsNutritionLogs.put("date", TableInfo.Column("date", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsNutritionLogs.put("calories", TableInfo.Column("calories", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsNutritionLogs.put("protein", TableInfo.Column("protein", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsNutritionLogs.put("carbs", TableInfo.Column("carbs", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsNutritionLogs.put("fat", TableInfo.Column("fat", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysNutritionLogs: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesNutritionLogs: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoNutritionLogs: TableInfo = TableInfo("nutrition_logs", _columnsNutritionLogs,
            _foreignKeysNutritionLogs, _indicesNutritionLogs)
        val _existingNutritionLogs: TableInfo = read(connection, "nutrition_logs")
        if (!_infoNutritionLogs.equals(_existingNutritionLogs)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |nutrition_logs(com.example.solo_levelling.data.db.entity.NutritionLogEntity).
              | Expected:
              |""".trimMargin() + _infoNutritionLogs + """
              |
              | Found:
              |""".trimMargin() + _existingNutritionLogs)
        }
        val _columnsFocusSessions: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsFocusSessions.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFocusSessions.put("date", TableInfo.Column("date", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFocusSessions.put("durationMinutes", TableInfo.Column("durationMinutes", "INTEGER",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFocusSessions.put("label", TableInfo.Column("label", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFocusSessions.put("completedAtEpochMs", TableInfo.Column("completedAtEpochMs",
            "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysFocusSessions: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesFocusSessions: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoFocusSessions: TableInfo = TableInfo("focus_sessions", _columnsFocusSessions,
            _foreignKeysFocusSessions, _indicesFocusSessions)
        val _existingFocusSessions: TableInfo = read(connection, "focus_sessions")
        if (!_infoFocusSessions.equals(_existingFocusSessions)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |focus_sessions(com.example.solo_levelling.data.db.entity.FocusSessionEntity).
              | Expected:
              |""".trimMargin() + _infoFocusSessions + """
              |
              | Found:
              |""".trimMargin() + _existingFocusSessions)
        }
        val _columnsJournalEntries: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsJournalEntries.put("date", TableInfo.Column("date", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsJournalEntries.put("content", TableInfo.Column("content", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsJournalEntries.put("updatedAtEpochMs", TableInfo.Column("updatedAtEpochMs",
            "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysJournalEntries: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesJournalEntries: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoJournalEntries: TableInfo = TableInfo("journal_entries", _columnsJournalEntries,
            _foreignKeysJournalEntries, _indicesJournalEntries)
        val _existingJournalEntries: TableInfo = read(connection, "journal_entries")
        if (!_infoJournalEntries.equals(_existingJournalEntries)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |journal_entries(com.example.solo_levelling.data.db.entity.JournalEntryEntity).
              | Expected:
              |""".trimMargin() + _infoJournalEntries + """
              |
              | Found:
              |""".trimMargin() + _existingJournalEntries)
        }
        val _columnsMetricLogs: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsMetricLogs.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMetricLogs.put("metricType", TableInfo.Column("metricType", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMetricLogs.put("value", TableInfo.Column("value", "REAL", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMetricLogs.put("recordedAtEpochMs", TableInfo.Column("recordedAtEpochMs", "INTEGER",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMetricLogs.put("date", TableInfo.Column("date", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysMetricLogs: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesMetricLogs: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoMetricLogs: TableInfo = TableInfo("metric_logs", _columnsMetricLogs,
            _foreignKeysMetricLogs, _indicesMetricLogs)
        val _existingMetricLogs: TableInfo = read(connection, "metric_logs")
        if (!_infoMetricLogs.equals(_existingMetricLogs)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |metric_logs(com.example.solo_levelling.data.db.entity.MetricLogEntity).
              | Expected:
              |""".trimMargin() + _infoMetricLogs + """
              |
              | Found:
              |""".trimMargin() + _existingMetricLogs)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "player_profile",
        "attribute_stats", "quest_templates", "quest_instances", "xp_ledger", "achievement_defs",
        "player_achievements", "streak_state", "bosses", "skills", "dsa_problems", "workouts",
        "nutrition_logs", "focus_sessions", "journal_entries", "metric_logs")
  }

  public override fun clearAllTables() {
    super.performClear(false, "player_profile", "attribute_stats", "quest_templates",
        "quest_instances", "xp_ledger", "achievement_defs", "player_achievements", "streak_state",
        "bosses", "skills", "dsa_problems", "workouts", "nutrition_logs", "focus_sessions",
        "journal_entries", "metric_logs")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(PlayerDao::class, PlayerDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(QuestDao::class, QuestDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(XpDao::class, XpDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(AchievementDao::class, AchievementDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(ModuleDao::class, ModuleDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override
      fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun playerDao(): PlayerDao = _playerDao.value

  public override fun questDao(): QuestDao = _questDao.value

  public override fun xpDao(): XpDao = _xpDao.value

  public override fun achievementDao(): AchievementDao = _achievementDao.value

  public override fun moduleDao(): ModuleDao = _moduleDao.value
}
