package com.example.solo_levelling.`data`.db.dao

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.example.solo_levelling.`data`.db.entity.BossEntity
import com.example.solo_levelling.`data`.db.entity.DsaProblemEntity
import com.example.solo_levelling.`data`.db.entity.FocusSessionEntity
import com.example.solo_levelling.`data`.db.entity.JournalEntryEntity
import com.example.solo_levelling.`data`.db.entity.MetricLogEntity
import com.example.solo_levelling.`data`.db.entity.NutritionLogEntity
import com.example.solo_levelling.`data`.db.entity.SkillEntity
import com.example.solo_levelling.`data`.db.entity.WorkoutEntity
import javax.`annotation`.processing.Generated
import kotlin.Float
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class ModuleDao_Impl(
  __db: RoomDatabase,
) : ModuleDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfBossEntity: EntityInsertAdapter<BossEntity>

  private val __insertAdapterOfSkillEntity: EntityInsertAdapter<SkillEntity>

  private val __insertAdapterOfDsaProblemEntity: EntityInsertAdapter<DsaProblemEntity>

  private val __insertAdapterOfWorkoutEntity: EntityInsertAdapter<WorkoutEntity>

  private val __insertAdapterOfNutritionLogEntity: EntityInsertAdapter<NutritionLogEntity>

  private val __insertAdapterOfFocusSessionEntity: EntityInsertAdapter<FocusSessionEntity>

  private val __insertAdapterOfJournalEntryEntity: EntityInsertAdapter<JournalEntryEntity>

  private val __insertAdapterOfMetricLogEntity: EntityInsertAdapter<MetricLogEntity>

  private val __updateAdapterOfBossEntity: EntityDeleteOrUpdateAdapter<BossEntity>

  private val __updateAdapterOfSkillEntity: EntityDeleteOrUpdateAdapter<SkillEntity>

  private val __updateAdapterOfDsaProblemEntity: EntityDeleteOrUpdateAdapter<DsaProblemEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfBossEntity = object : EntityInsertAdapter<BossEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `bosses` (`id`,`title`,`description`,`targetValue`,`currentValue`,`xpReward`,`status`,`deadlineDate`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: BossEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.title)
        statement.bindText(3, entity.description)
        statement.bindDouble(4, entity.targetValue.toDouble())
        statement.bindDouble(5, entity.currentValue.toDouble())
        statement.bindLong(6, entity.xpReward.toLong())
        statement.bindText(7, entity.status)
        val _tmpDeadlineDate: String? = entity.deadlineDate
        if (_tmpDeadlineDate == null) {
          statement.bindNull(8)
        } else {
          statement.bindText(8, _tmpDeadlineDate)
        }
      }
    }
    this.__insertAdapterOfSkillEntity = object : EntityInsertAdapter<SkillEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `skills` (`id`,`domain`,`name`,`xp`,`level`) VALUES (nullif(?, 0),?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: SkillEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.domain)
        statement.bindText(3, entity.name)
        statement.bindLong(4, entity.xp.toLong())
        statement.bindLong(5, entity.level.toLong())
      }
    }
    this.__insertAdapterOfDsaProblemEntity = object : EntityInsertAdapter<DsaProblemEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `dsa_problems` (`id`,`title`,`platform`,`externalId`,`difficulty`,`topic`,`status`,`attempts`,`confidence`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: DsaProblemEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.title)
        statement.bindText(3, entity.platform)
        statement.bindText(4, entity.externalId)
        statement.bindText(5, entity.difficulty)
        statement.bindText(6, entity.topic)
        statement.bindText(7, entity.status)
        statement.bindLong(8, entity.attempts.toLong())
        statement.bindLong(9, entity.confidence.toLong())
      }
    }
    this.__insertAdapterOfWorkoutEntity = object : EntityInsertAdapter<WorkoutEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `workouts` (`id`,`date`,`type`,`durationMinutes`,`notes`) VALUES (nullif(?, 0),?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: WorkoutEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.date)
        statement.bindText(3, entity.type)
        statement.bindLong(4, entity.durationMinutes.toLong())
        statement.bindText(5, entity.notes)
      }
    }
    this.__insertAdapterOfNutritionLogEntity = object : EntityInsertAdapter<NutritionLogEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `nutrition_logs` (`date`,`calories`,`protein`,`carbs`,`fat`) VALUES (?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: NutritionLogEntity) {
        statement.bindText(1, entity.date)
        statement.bindLong(2, entity.calories.toLong())
        statement.bindLong(3, entity.protein.toLong())
        statement.bindLong(4, entity.carbs.toLong())
        statement.bindLong(5, entity.fat.toLong())
      }
    }
    this.__insertAdapterOfFocusSessionEntity = object : EntityInsertAdapter<FocusSessionEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `focus_sessions` (`id`,`date`,`durationMinutes`,`label`,`completedAtEpochMs`) VALUES (nullif(?, 0),?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: FocusSessionEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.date)
        statement.bindLong(3, entity.durationMinutes.toLong())
        statement.bindText(4, entity.label)
        statement.bindLong(5, entity.completedAtEpochMs)
      }
    }
    this.__insertAdapterOfJournalEntryEntity = object : EntityInsertAdapter<JournalEntryEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `journal_entries` (`date`,`content`,`updatedAtEpochMs`) VALUES (?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: JournalEntryEntity) {
        statement.bindText(1, entity.date)
        statement.bindText(2, entity.content)
        statement.bindLong(3, entity.updatedAtEpochMs)
      }
    }
    this.__insertAdapterOfMetricLogEntity = object : EntityInsertAdapter<MetricLogEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `metric_logs` (`id`,`metricType`,`value`,`recordedAtEpochMs`,`date`) VALUES (nullif(?, 0),?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: MetricLogEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.metricType)
        statement.bindDouble(3, entity.value.toDouble())
        statement.bindLong(4, entity.recordedAtEpochMs)
        statement.bindText(5, entity.date)
      }
    }
    this.__updateAdapterOfBossEntity = object : EntityDeleteOrUpdateAdapter<BossEntity>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `bosses` SET `id` = ?,`title` = ?,`description` = ?,`targetValue` = ?,`currentValue` = ?,`xpReward` = ?,`status` = ?,`deadlineDate` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: BossEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.title)
        statement.bindText(3, entity.description)
        statement.bindDouble(4, entity.targetValue.toDouble())
        statement.bindDouble(5, entity.currentValue.toDouble())
        statement.bindLong(6, entity.xpReward.toLong())
        statement.bindText(7, entity.status)
        val _tmpDeadlineDate: String? = entity.deadlineDate
        if (_tmpDeadlineDate == null) {
          statement.bindNull(8)
        } else {
          statement.bindText(8, _tmpDeadlineDate)
        }
        statement.bindLong(9, entity.id)
      }
    }
    this.__updateAdapterOfSkillEntity = object : EntityDeleteOrUpdateAdapter<SkillEntity>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `skills` SET `id` = ?,`domain` = ?,`name` = ?,`xp` = ?,`level` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: SkillEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.domain)
        statement.bindText(3, entity.name)
        statement.bindLong(4, entity.xp.toLong())
        statement.bindLong(5, entity.level.toLong())
        statement.bindLong(6, entity.id)
      }
    }
    this.__updateAdapterOfDsaProblemEntity = object :
        EntityDeleteOrUpdateAdapter<DsaProblemEntity>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `dsa_problems` SET `id` = ?,`title` = ?,`platform` = ?,`externalId` = ?,`difficulty` = ?,`topic` = ?,`status` = ?,`attempts` = ?,`confidence` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: DsaProblemEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.title)
        statement.bindText(3, entity.platform)
        statement.bindText(4, entity.externalId)
        statement.bindText(5, entity.difficulty)
        statement.bindText(6, entity.topic)
        statement.bindText(7, entity.status)
        statement.bindLong(8, entity.attempts.toLong())
        statement.bindLong(9, entity.confidence.toLong())
        statement.bindLong(10, entity.id)
      }
    }
  }

  public override suspend fun upsertBoss(boss: BossEntity): Long = performSuspending(__db, false,
      true) { _connection ->
    val _result: Long = __insertAdapterOfBossEntity.insertAndReturnId(_connection, boss)
    _result
  }

  public override suspend fun upsertSkill(skill: SkillEntity): Long = performSuspending(__db, false,
      true) { _connection ->
    val _result: Long = __insertAdapterOfSkillEntity.insertAndReturnId(_connection, skill)
    _result
  }

  public override suspend fun upsertDsa(problem: DsaProblemEntity): Long = performSuspending(__db,
      false, true) { _connection ->
    val _result: Long = __insertAdapterOfDsaProblemEntity.insertAndReturnId(_connection, problem)
    _result
  }

  public override suspend fun insertWorkout(workout: WorkoutEntity): Long = performSuspending(__db,
      false, true) { _connection ->
    val _result: Long = __insertAdapterOfWorkoutEntity.insertAndReturnId(_connection, workout)
    _result
  }

  public override suspend fun upsertNutrition(log: NutritionLogEntity): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfNutritionLogEntity.insert(_connection, log)
  }

  public override suspend fun insertFocus(session: FocusSessionEntity): Long =
      performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfFocusSessionEntity.insertAndReturnId(_connection, session)
    _result
  }

  public override suspend fun upsertJournal(entry: JournalEntryEntity): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfJournalEntryEntity.insert(_connection, entry)
  }

  public override suspend fun insertMetric(metric: MetricLogEntity): Long = performSuspending(__db,
      false, true) { _connection ->
    val _result: Long = __insertAdapterOfMetricLogEntity.insertAndReturnId(_connection, metric)
    _result
  }

  public override suspend fun updateBoss(boss: BossEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __updateAdapterOfBossEntity.handle(_connection, boss)
  }

  public override suspend fun updateSkill(skill: SkillEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __updateAdapterOfSkillEntity.handle(_connection, skill)
  }

  public override suspend fun updateDsa(problem: DsaProblemEntity): Unit = performSuspending(__db,
      false, true) { _connection ->
    __updateAdapterOfDsaProblemEntity.handle(_connection, problem)
  }

  public override fun observeBosses(): Flow<List<BossEntity>> {
    val _sql: String = "SELECT * FROM bosses ORDER BY id DESC"
    return createFlow(__db, false, arrayOf("bosses")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfTargetValue: Int = getColumnIndexOrThrow(_stmt, "targetValue")
        val _columnIndexOfCurrentValue: Int = getColumnIndexOrThrow(_stmt, "currentValue")
        val _columnIndexOfXpReward: Int = getColumnIndexOrThrow(_stmt, "xpReward")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfDeadlineDate: Int = getColumnIndexOrThrow(_stmt, "deadlineDate")
        val _result: MutableList<BossEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: BossEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpTargetValue: Float
          _tmpTargetValue = _stmt.getDouble(_columnIndexOfTargetValue).toFloat()
          val _tmpCurrentValue: Float
          _tmpCurrentValue = _stmt.getDouble(_columnIndexOfCurrentValue).toFloat()
          val _tmpXpReward: Int
          _tmpXpReward = _stmt.getLong(_columnIndexOfXpReward).toInt()
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpDeadlineDate: String?
          if (_stmt.isNull(_columnIndexOfDeadlineDate)) {
            _tmpDeadlineDate = null
          } else {
            _tmpDeadlineDate = _stmt.getText(_columnIndexOfDeadlineDate)
          }
          _item =
              BossEntity(_tmpId,_tmpTitle,_tmpDescription,_tmpTargetValue,_tmpCurrentValue,_tmpXpReward,_tmpStatus,_tmpDeadlineDate)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getActiveBoss(): BossEntity? {
    val _sql: String = "SELECT * FROM bosses WHERE status = 'ACTIVE' LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfTargetValue: Int = getColumnIndexOrThrow(_stmt, "targetValue")
        val _columnIndexOfCurrentValue: Int = getColumnIndexOrThrow(_stmt, "currentValue")
        val _columnIndexOfXpReward: Int = getColumnIndexOrThrow(_stmt, "xpReward")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfDeadlineDate: Int = getColumnIndexOrThrow(_stmt, "deadlineDate")
        val _result: BossEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpTargetValue: Float
          _tmpTargetValue = _stmt.getDouble(_columnIndexOfTargetValue).toFloat()
          val _tmpCurrentValue: Float
          _tmpCurrentValue = _stmt.getDouble(_columnIndexOfCurrentValue).toFloat()
          val _tmpXpReward: Int
          _tmpXpReward = _stmt.getLong(_columnIndexOfXpReward).toInt()
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpDeadlineDate: String?
          if (_stmt.isNull(_columnIndexOfDeadlineDate)) {
            _tmpDeadlineDate = null
          } else {
            _tmpDeadlineDate = _stmt.getText(_columnIndexOfDeadlineDate)
          }
          _result =
              BossEntity(_tmpId,_tmpTitle,_tmpDescription,_tmpTargetValue,_tmpCurrentValue,_tmpXpReward,_tmpStatus,_tmpDeadlineDate)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeSkills(): Flow<List<SkillEntity>> {
    val _sql: String = "SELECT * FROM skills ORDER BY domain, name"
    return createFlow(__db, false, arrayOf("skills")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfDomain: Int = getColumnIndexOrThrow(_stmt, "domain")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfXp: Int = getColumnIndexOrThrow(_stmt, "xp")
        val _columnIndexOfLevel: Int = getColumnIndexOrThrow(_stmt, "level")
        val _result: MutableList<SkillEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: SkillEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpDomain: String
          _tmpDomain = _stmt.getText(_columnIndexOfDomain)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpXp: Int
          _tmpXp = _stmt.getLong(_columnIndexOfXp).toInt()
          val _tmpLevel: Int
          _tmpLevel = _stmt.getLong(_columnIndexOfLevel).toInt()
          _item = SkillEntity(_tmpId,_tmpDomain,_tmpName,_tmpXp,_tmpLevel)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeDsa(): Flow<List<DsaProblemEntity>> {
    val _sql: String = "SELECT * FROM dsa_problems ORDER BY id DESC"
    return createFlow(__db, false, arrayOf("dsa_problems")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfPlatform: Int = getColumnIndexOrThrow(_stmt, "platform")
        val _columnIndexOfExternalId: Int = getColumnIndexOrThrow(_stmt, "externalId")
        val _columnIndexOfDifficulty: Int = getColumnIndexOrThrow(_stmt, "difficulty")
        val _columnIndexOfTopic: Int = getColumnIndexOrThrow(_stmt, "topic")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfAttempts: Int = getColumnIndexOrThrow(_stmt, "attempts")
        val _columnIndexOfConfidence: Int = getColumnIndexOrThrow(_stmt, "confidence")
        val _result: MutableList<DsaProblemEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: DsaProblemEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpPlatform: String
          _tmpPlatform = _stmt.getText(_columnIndexOfPlatform)
          val _tmpExternalId: String
          _tmpExternalId = _stmt.getText(_columnIndexOfExternalId)
          val _tmpDifficulty: String
          _tmpDifficulty = _stmt.getText(_columnIndexOfDifficulty)
          val _tmpTopic: String
          _tmpTopic = _stmt.getText(_columnIndexOfTopic)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpAttempts: Int
          _tmpAttempts = _stmt.getLong(_columnIndexOfAttempts).toInt()
          val _tmpConfidence: Int
          _tmpConfidence = _stmt.getLong(_columnIndexOfConfidence).toInt()
          _item =
              DsaProblemEntity(_tmpId,_tmpTitle,_tmpPlatform,_tmpExternalId,_tmpDifficulty,_tmpTopic,_tmpStatus,_tmpAttempts,_tmpConfidence)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeWorkouts(): Flow<List<WorkoutEntity>> {
    val _sql: String = "SELECT * FROM workouts ORDER BY date DESC"
    return createFlow(__db, false, arrayOf("workouts")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfDate: Int = getColumnIndexOrThrow(_stmt, "date")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfDurationMinutes: Int = getColumnIndexOrThrow(_stmt, "durationMinutes")
        val _columnIndexOfNotes: Int = getColumnIndexOrThrow(_stmt, "notes")
        val _result: MutableList<WorkoutEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: WorkoutEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpDate: String
          _tmpDate = _stmt.getText(_columnIndexOfDate)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpDurationMinutes: Int
          _tmpDurationMinutes = _stmt.getLong(_columnIndexOfDurationMinutes).toInt()
          val _tmpNotes: String
          _tmpNotes = _stmt.getText(_columnIndexOfNotes)
          _item = WorkoutEntity(_tmpId,_tmpDate,_tmpType,_tmpDurationMinutes,_tmpNotes)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeNutrition(date: String): Flow<NutritionLogEntity?> {
    val _sql: String = "SELECT * FROM nutrition_logs WHERE date = ? LIMIT 1"
    return createFlow(__db, false, arrayOf("nutrition_logs")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, date)
        val _columnIndexOfDate: Int = getColumnIndexOrThrow(_stmt, "date")
        val _columnIndexOfCalories: Int = getColumnIndexOrThrow(_stmt, "calories")
        val _columnIndexOfProtein: Int = getColumnIndexOrThrow(_stmt, "protein")
        val _columnIndexOfCarbs: Int = getColumnIndexOrThrow(_stmt, "carbs")
        val _columnIndexOfFat: Int = getColumnIndexOrThrow(_stmt, "fat")
        val _result: NutritionLogEntity?
        if (_stmt.step()) {
          val _tmpDate: String
          _tmpDate = _stmt.getText(_columnIndexOfDate)
          val _tmpCalories: Int
          _tmpCalories = _stmt.getLong(_columnIndexOfCalories).toInt()
          val _tmpProtein: Int
          _tmpProtein = _stmt.getLong(_columnIndexOfProtein).toInt()
          val _tmpCarbs: Int
          _tmpCarbs = _stmt.getLong(_columnIndexOfCarbs).toInt()
          val _tmpFat: Int
          _tmpFat = _stmt.getLong(_columnIndexOfFat).toInt()
          _result = NutritionLogEntity(_tmpDate,_tmpCalories,_tmpProtein,_tmpCarbs,_tmpFat)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeFocus(date: String): Flow<List<FocusSessionEntity>> {
    val _sql: String = "SELECT * FROM focus_sessions WHERE date = ? ORDER BY id DESC"
    return createFlow(__db, false, arrayOf("focus_sessions")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, date)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfDate: Int = getColumnIndexOrThrow(_stmt, "date")
        val _columnIndexOfDurationMinutes: Int = getColumnIndexOrThrow(_stmt, "durationMinutes")
        val _columnIndexOfLabel: Int = getColumnIndexOrThrow(_stmt, "label")
        val _columnIndexOfCompletedAtEpochMs: Int = getColumnIndexOrThrow(_stmt,
            "completedAtEpochMs")
        val _result: MutableList<FocusSessionEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: FocusSessionEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpDate: String
          _tmpDate = _stmt.getText(_columnIndexOfDate)
          val _tmpDurationMinutes: Int
          _tmpDurationMinutes = _stmt.getLong(_columnIndexOfDurationMinutes).toInt()
          val _tmpLabel: String
          _tmpLabel = _stmt.getText(_columnIndexOfLabel)
          val _tmpCompletedAtEpochMs: Long
          _tmpCompletedAtEpochMs = _stmt.getLong(_columnIndexOfCompletedAtEpochMs)
          _item =
              FocusSessionEntity(_tmpId,_tmpDate,_tmpDurationMinutes,_tmpLabel,_tmpCompletedAtEpochMs)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeJournal(date: String): Flow<JournalEntryEntity?> {
    val _sql: String = "SELECT * FROM journal_entries WHERE date = ? LIMIT 1"
    return createFlow(__db, false, arrayOf("journal_entries")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, date)
        val _columnIndexOfDate: Int = getColumnIndexOrThrow(_stmt, "date")
        val _columnIndexOfContent: Int = getColumnIndexOrThrow(_stmt, "content")
        val _columnIndexOfUpdatedAtEpochMs: Int = getColumnIndexOrThrow(_stmt, "updatedAtEpochMs")
        val _result: JournalEntryEntity?
        if (_stmt.step()) {
          val _tmpDate: String
          _tmpDate = _stmt.getText(_columnIndexOfDate)
          val _tmpContent: String
          _tmpContent = _stmt.getText(_columnIndexOfContent)
          val _tmpUpdatedAtEpochMs: Long
          _tmpUpdatedAtEpochMs = _stmt.getLong(_columnIndexOfUpdatedAtEpochMs)
          _result = JournalEntryEntity(_tmpDate,_tmpContent,_tmpUpdatedAtEpochMs)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun recentMetrics(type: String, limit: Int): List<MetricLogEntity> {
    val _sql: String =
        "SELECT * FROM metric_logs WHERE metricType = ? ORDER BY recordedAtEpochMs DESC LIMIT ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, type)
        _argIndex = 2
        _stmt.bindLong(_argIndex, limit.toLong())
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfMetricType: Int = getColumnIndexOrThrow(_stmt, "metricType")
        val _columnIndexOfValue: Int = getColumnIndexOrThrow(_stmt, "value")
        val _columnIndexOfRecordedAtEpochMs: Int = getColumnIndexOrThrow(_stmt, "recordedAtEpochMs")
        val _columnIndexOfDate: Int = getColumnIndexOrThrow(_stmt, "date")
        val _result: MutableList<MetricLogEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: MetricLogEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpMetricType: String
          _tmpMetricType = _stmt.getText(_columnIndexOfMetricType)
          val _tmpValue: Float
          _tmpValue = _stmt.getDouble(_columnIndexOfValue).toFloat()
          val _tmpRecordedAtEpochMs: Long
          _tmpRecordedAtEpochMs = _stmt.getLong(_columnIndexOfRecordedAtEpochMs)
          val _tmpDate: String
          _tmpDate = _stmt.getText(_columnIndexOfDate)
          _item = MetricLogEntity(_tmpId,_tmpMetricType,_tmpValue,_tmpRecordedAtEpochMs,_tmpDate)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun countDsaSolved(): Int {
    val _sql: String = "SELECT COUNT(*) FROM dsa_problems WHERE status IN ('SOLVED','MASTERED')"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun countBossCleared(): Int {
    val _sql: String = "SELECT COUNT(*) FROM bosses WHERE status = 'CLEARED'"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getDsa(id: Long): DsaProblemEntity? {
    val _sql: String = "SELECT * FROM dsa_problems WHERE id = ? LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfPlatform: Int = getColumnIndexOrThrow(_stmt, "platform")
        val _columnIndexOfExternalId: Int = getColumnIndexOrThrow(_stmt, "externalId")
        val _columnIndexOfDifficulty: Int = getColumnIndexOrThrow(_stmt, "difficulty")
        val _columnIndexOfTopic: Int = getColumnIndexOrThrow(_stmt, "topic")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfAttempts: Int = getColumnIndexOrThrow(_stmt, "attempts")
        val _columnIndexOfConfidence: Int = getColumnIndexOrThrow(_stmt, "confidence")
        val _result: DsaProblemEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpPlatform: String
          _tmpPlatform = _stmt.getText(_columnIndexOfPlatform)
          val _tmpExternalId: String
          _tmpExternalId = _stmt.getText(_columnIndexOfExternalId)
          val _tmpDifficulty: String
          _tmpDifficulty = _stmt.getText(_columnIndexOfDifficulty)
          val _tmpTopic: String
          _tmpTopic = _stmt.getText(_columnIndexOfTopic)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpAttempts: Int
          _tmpAttempts = _stmt.getLong(_columnIndexOfAttempts).toInt()
          val _tmpConfidence: Int
          _tmpConfidence = _stmt.getLong(_columnIndexOfConfidence).toInt()
          _result =
              DsaProblemEntity(_tmpId,_tmpTitle,_tmpPlatform,_tmpExternalId,_tmpDifficulty,_tmpTopic,_tmpStatus,_tmpAttempts,_tmpConfidence)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun findSkill(domain: String, name: String): SkillEntity? {
    val _sql: String = "SELECT * FROM skills WHERE domain = ? AND name = ? LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, domain)
        _argIndex = 2
        _stmt.bindText(_argIndex, name)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfDomain: Int = getColumnIndexOrThrow(_stmt, "domain")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfXp: Int = getColumnIndexOrThrow(_stmt, "xp")
        val _columnIndexOfLevel: Int = getColumnIndexOrThrow(_stmt, "level")
        val _result: SkillEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpDomain: String
          _tmpDomain = _stmt.getText(_columnIndexOfDomain)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpXp: Int
          _tmpXp = _stmt.getLong(_columnIndexOfXp).toInt()
          val _tmpLevel: Int
          _tmpLevel = _stmt.getLong(_columnIndexOfLevel).toInt()
          _result = SkillEntity(_tmpId,_tmpDomain,_tmpName,_tmpXp,_tmpLevel)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
