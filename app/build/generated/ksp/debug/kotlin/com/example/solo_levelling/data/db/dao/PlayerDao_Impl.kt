package com.example.solo_levelling.`data`.db.dao

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.example.solo_levelling.`data`.db.entity.AttributeStatEntity
import com.example.solo_levelling.`data`.db.entity.PlayerProfileEntity
import com.example.solo_levelling.`data`.db.entity.StreakStateEntity
import javax.`annotation`.processing.Generated
import kotlin.Boolean
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
public class PlayerDao_Impl(
  __db: RoomDatabase,
) : PlayerDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfPlayerProfileEntity: EntityInsertAdapter<PlayerProfileEntity>

  private val __insertAdapterOfAttributeStatEntity: EntityInsertAdapter<AttributeStatEntity>

  private val __insertAdapterOfStreakStateEntity: EntityInsertAdapter<StreakStateEntity>

  private val __updateAdapterOfPlayerProfileEntity: EntityDeleteOrUpdateAdapter<PlayerProfileEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfPlayerProfileEntity = object : EntityInsertAdapter<PlayerProfileEntity>()
        {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `player_profile` (`id`,`name`,`level`,`totalXp`,`rank`,`timezone`,`onboardingDone`,`prioritiesCsv`,`createdAtEpochMs`) VALUES (?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: PlayerProfileEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindLong(3, entity.level.toLong())
        statement.bindLong(4, entity.totalXp.toLong())
        statement.bindText(5, entity.rank)
        statement.bindText(6, entity.timezone)
        val _tmp: Int = if (entity.onboardingDone) 1 else 0
        statement.bindLong(7, _tmp.toLong())
        statement.bindText(8, entity.prioritiesCsv)
        statement.bindLong(9, entity.createdAtEpochMs)
      }
    }
    this.__insertAdapterOfAttributeStatEntity = object : EntityInsertAdapter<AttributeStatEntity>()
        {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `attribute_stats` (`code`,`currentValue`,`lifetimeXp`) VALUES (?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: AttributeStatEntity) {
        statement.bindText(1, entity.code)
        statement.bindLong(2, entity.currentValue.toLong())
        statement.bindLong(3, entity.lifetimeXp.toLong())
      }
    }
    this.__insertAdapterOfStreakStateEntity = object : EntityInsertAdapter<StreakStateEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `streak_state` (`id`,`current`,`best`,`lastCompletedDate`,`recoveryUsedThisWeek`,`weekStartDate`) VALUES (?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: StreakStateEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.current.toLong())
        statement.bindLong(3, entity.best.toLong())
        val _tmpLastCompletedDate: String? = entity.lastCompletedDate
        if (_tmpLastCompletedDate == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpLastCompletedDate)
        }
        statement.bindLong(5, entity.recoveryUsedThisWeek.toLong())
        val _tmpWeekStartDate: String? = entity.weekStartDate
        if (_tmpWeekStartDate == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmpWeekStartDate)
        }
      }
    }
    this.__updateAdapterOfPlayerProfileEntity = object :
        EntityDeleteOrUpdateAdapter<PlayerProfileEntity>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `player_profile` SET `id` = ?,`name` = ?,`level` = ?,`totalXp` = ?,`rank` = ?,`timezone` = ?,`onboardingDone` = ?,`prioritiesCsv` = ?,`createdAtEpochMs` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: PlayerProfileEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindLong(3, entity.level.toLong())
        statement.bindLong(4, entity.totalXp.toLong())
        statement.bindText(5, entity.rank)
        statement.bindText(6, entity.timezone)
        val _tmp: Int = if (entity.onboardingDone) 1 else 0
        statement.bindLong(7, _tmp.toLong())
        statement.bindText(8, entity.prioritiesCsv)
        statement.bindLong(9, entity.createdAtEpochMs)
        statement.bindLong(10, entity.id)
      }
    }
  }

  public override suspend fun upsertProfile(profile: PlayerProfileEntity): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfPlayerProfileEntity.insert(_connection, profile)
  }

  public override suspend fun upsertAttributes(stats: List<AttributeStatEntity>): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfAttributeStatEntity.insert(_connection, stats)
  }

  public override suspend fun upsertAttribute(stat: AttributeStatEntity): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfAttributeStatEntity.insert(_connection, stat)
  }

  public override suspend fun upsertStreak(streak: StreakStateEntity): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfStreakStateEntity.insert(_connection, streak)
  }

  public override suspend fun updateProfile(profile: PlayerProfileEntity): Unit =
      performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfPlayerProfileEntity.handle(_connection, profile)
  }

  public override fun observeProfile(id: Long): Flow<PlayerProfileEntity?> {
    val _sql: String = "SELECT * FROM player_profile WHERE id = ? LIMIT 1"
    return createFlow(__db, false, arrayOf("player_profile")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfLevel: Int = getColumnIndexOrThrow(_stmt, "level")
        val _columnIndexOfTotalXp: Int = getColumnIndexOrThrow(_stmt, "totalXp")
        val _columnIndexOfRank: Int = getColumnIndexOrThrow(_stmt, "rank")
        val _columnIndexOfTimezone: Int = getColumnIndexOrThrow(_stmt, "timezone")
        val _columnIndexOfOnboardingDone: Int = getColumnIndexOrThrow(_stmt, "onboardingDone")
        val _columnIndexOfPrioritiesCsv: Int = getColumnIndexOrThrow(_stmt, "prioritiesCsv")
        val _columnIndexOfCreatedAtEpochMs: Int = getColumnIndexOrThrow(_stmt, "createdAtEpochMs")
        val _result: PlayerProfileEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpLevel: Int
          _tmpLevel = _stmt.getLong(_columnIndexOfLevel).toInt()
          val _tmpTotalXp: Int
          _tmpTotalXp = _stmt.getLong(_columnIndexOfTotalXp).toInt()
          val _tmpRank: String
          _tmpRank = _stmt.getText(_columnIndexOfRank)
          val _tmpTimezone: String
          _tmpTimezone = _stmt.getText(_columnIndexOfTimezone)
          val _tmpOnboardingDone: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfOnboardingDone).toInt()
          _tmpOnboardingDone = _tmp != 0
          val _tmpPrioritiesCsv: String
          _tmpPrioritiesCsv = _stmt.getText(_columnIndexOfPrioritiesCsv)
          val _tmpCreatedAtEpochMs: Long
          _tmpCreatedAtEpochMs = _stmt.getLong(_columnIndexOfCreatedAtEpochMs)
          _result =
              PlayerProfileEntity(_tmpId,_tmpName,_tmpLevel,_tmpTotalXp,_tmpRank,_tmpTimezone,_tmpOnboardingDone,_tmpPrioritiesCsv,_tmpCreatedAtEpochMs)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getProfile(id: Long): PlayerProfileEntity? {
    val _sql: String = "SELECT * FROM player_profile WHERE id = ? LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfLevel: Int = getColumnIndexOrThrow(_stmt, "level")
        val _columnIndexOfTotalXp: Int = getColumnIndexOrThrow(_stmt, "totalXp")
        val _columnIndexOfRank: Int = getColumnIndexOrThrow(_stmt, "rank")
        val _columnIndexOfTimezone: Int = getColumnIndexOrThrow(_stmt, "timezone")
        val _columnIndexOfOnboardingDone: Int = getColumnIndexOrThrow(_stmt, "onboardingDone")
        val _columnIndexOfPrioritiesCsv: Int = getColumnIndexOrThrow(_stmt, "prioritiesCsv")
        val _columnIndexOfCreatedAtEpochMs: Int = getColumnIndexOrThrow(_stmt, "createdAtEpochMs")
        val _result: PlayerProfileEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpLevel: Int
          _tmpLevel = _stmt.getLong(_columnIndexOfLevel).toInt()
          val _tmpTotalXp: Int
          _tmpTotalXp = _stmt.getLong(_columnIndexOfTotalXp).toInt()
          val _tmpRank: String
          _tmpRank = _stmt.getText(_columnIndexOfRank)
          val _tmpTimezone: String
          _tmpTimezone = _stmt.getText(_columnIndexOfTimezone)
          val _tmpOnboardingDone: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfOnboardingDone).toInt()
          _tmpOnboardingDone = _tmp != 0
          val _tmpPrioritiesCsv: String
          _tmpPrioritiesCsv = _stmt.getText(_columnIndexOfPrioritiesCsv)
          val _tmpCreatedAtEpochMs: Long
          _tmpCreatedAtEpochMs = _stmt.getLong(_columnIndexOfCreatedAtEpochMs)
          _result =
              PlayerProfileEntity(_tmpId,_tmpName,_tmpLevel,_tmpTotalXp,_tmpRank,_tmpTimezone,_tmpOnboardingDone,_tmpPrioritiesCsv,_tmpCreatedAtEpochMs)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeAttributes(): Flow<List<AttributeStatEntity>> {
    val _sql: String = "SELECT * FROM attribute_stats"
    return createFlow(__db, false, arrayOf("attribute_stats")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfCode: Int = getColumnIndexOrThrow(_stmt, "code")
        val _columnIndexOfCurrentValue: Int = getColumnIndexOrThrow(_stmt, "currentValue")
        val _columnIndexOfLifetimeXp: Int = getColumnIndexOrThrow(_stmt, "lifetimeXp")
        val _result: MutableList<AttributeStatEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: AttributeStatEntity
          val _tmpCode: String
          _tmpCode = _stmt.getText(_columnIndexOfCode)
          val _tmpCurrentValue: Int
          _tmpCurrentValue = _stmt.getLong(_columnIndexOfCurrentValue).toInt()
          val _tmpLifetimeXp: Int
          _tmpLifetimeXp = _stmt.getLong(_columnIndexOfLifetimeXp).toInt()
          _item = AttributeStatEntity(_tmpCode,_tmpCurrentValue,_tmpLifetimeXp)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAttributes(): List<AttributeStatEntity> {
    val _sql: String = "SELECT * FROM attribute_stats"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfCode: Int = getColumnIndexOrThrow(_stmt, "code")
        val _columnIndexOfCurrentValue: Int = getColumnIndexOrThrow(_stmt, "currentValue")
        val _columnIndexOfLifetimeXp: Int = getColumnIndexOrThrow(_stmt, "lifetimeXp")
        val _result: MutableList<AttributeStatEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: AttributeStatEntity
          val _tmpCode: String
          _tmpCode = _stmt.getText(_columnIndexOfCode)
          val _tmpCurrentValue: Int
          _tmpCurrentValue = _stmt.getLong(_columnIndexOfCurrentValue).toInt()
          val _tmpLifetimeXp: Int
          _tmpLifetimeXp = _stmt.getLong(_columnIndexOfLifetimeXp).toInt()
          _item = AttributeStatEntity(_tmpCode,_tmpCurrentValue,_tmpLifetimeXp)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeStreak(id: Long): Flow<StreakStateEntity?> {
    val _sql: String = "SELECT * FROM streak_state WHERE id = ? LIMIT 1"
    return createFlow(__db, false, arrayOf("streak_state")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfCurrent: Int = getColumnIndexOrThrow(_stmt, "current")
        val _columnIndexOfBest: Int = getColumnIndexOrThrow(_stmt, "best")
        val _columnIndexOfLastCompletedDate: Int = getColumnIndexOrThrow(_stmt, "lastCompletedDate")
        val _columnIndexOfRecoveryUsedThisWeek: Int = getColumnIndexOrThrow(_stmt,
            "recoveryUsedThisWeek")
        val _columnIndexOfWeekStartDate: Int = getColumnIndexOrThrow(_stmt, "weekStartDate")
        val _result: StreakStateEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpCurrent: Int
          _tmpCurrent = _stmt.getLong(_columnIndexOfCurrent).toInt()
          val _tmpBest: Int
          _tmpBest = _stmt.getLong(_columnIndexOfBest).toInt()
          val _tmpLastCompletedDate: String?
          if (_stmt.isNull(_columnIndexOfLastCompletedDate)) {
            _tmpLastCompletedDate = null
          } else {
            _tmpLastCompletedDate = _stmt.getText(_columnIndexOfLastCompletedDate)
          }
          val _tmpRecoveryUsedThisWeek: Int
          _tmpRecoveryUsedThisWeek = _stmt.getLong(_columnIndexOfRecoveryUsedThisWeek).toInt()
          val _tmpWeekStartDate: String?
          if (_stmt.isNull(_columnIndexOfWeekStartDate)) {
            _tmpWeekStartDate = null
          } else {
            _tmpWeekStartDate = _stmt.getText(_columnIndexOfWeekStartDate)
          }
          _result =
              StreakStateEntity(_tmpId,_tmpCurrent,_tmpBest,_tmpLastCompletedDate,_tmpRecoveryUsedThisWeek,_tmpWeekStartDate)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getStreak(id: Long): StreakStateEntity? {
    val _sql: String = "SELECT * FROM streak_state WHERE id = ? LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfCurrent: Int = getColumnIndexOrThrow(_stmt, "current")
        val _columnIndexOfBest: Int = getColumnIndexOrThrow(_stmt, "best")
        val _columnIndexOfLastCompletedDate: Int = getColumnIndexOrThrow(_stmt, "lastCompletedDate")
        val _columnIndexOfRecoveryUsedThisWeek: Int = getColumnIndexOrThrow(_stmt,
            "recoveryUsedThisWeek")
        val _columnIndexOfWeekStartDate: Int = getColumnIndexOrThrow(_stmt, "weekStartDate")
        val _result: StreakStateEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpCurrent: Int
          _tmpCurrent = _stmt.getLong(_columnIndexOfCurrent).toInt()
          val _tmpBest: Int
          _tmpBest = _stmt.getLong(_columnIndexOfBest).toInt()
          val _tmpLastCompletedDate: String?
          if (_stmt.isNull(_columnIndexOfLastCompletedDate)) {
            _tmpLastCompletedDate = null
          } else {
            _tmpLastCompletedDate = _stmt.getText(_columnIndexOfLastCompletedDate)
          }
          val _tmpRecoveryUsedThisWeek: Int
          _tmpRecoveryUsedThisWeek = _stmt.getLong(_columnIndexOfRecoveryUsedThisWeek).toInt()
          val _tmpWeekStartDate: String?
          if (_stmt.isNull(_columnIndexOfWeekStartDate)) {
            _tmpWeekStartDate = null
          } else {
            _tmpWeekStartDate = _stmt.getText(_columnIndexOfWeekStartDate)
          }
          _result =
              StreakStateEntity(_tmpId,_tmpCurrent,_tmpBest,_tmpLastCompletedDate,_tmpRecoveryUsedThisWeek,_tmpWeekStartDate)
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
