package com.example.solo_levelling.`data`.db.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.example.solo_levelling.`data`.db.entity.AchievementDefEntity
import com.example.solo_levelling.`data`.db.entity.PlayerAchievementEntity
import javax.`annotation`.processing.Generated
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
public class AchievementDao_Impl(
  __db: RoomDatabase,
) : AchievementDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfAchievementDefEntity: EntityInsertAdapter<AchievementDefEntity>

  private val __insertAdapterOfPlayerAchievementEntity: EntityInsertAdapter<PlayerAchievementEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfAchievementDefEntity = object :
        EntityInsertAdapter<AchievementDefEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `achievement_defs` (`key`,`name`,`description`,`criteriaType`,`criteriaValue`,`rewardXp`) VALUES (?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: AchievementDefEntity) {
        statement.bindText(1, entity.key)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.description)
        statement.bindText(4, entity.criteriaType)
        statement.bindLong(5, entity.criteriaValue.toLong())
        statement.bindLong(6, entity.rewardXp.toLong())
      }
    }
    this.__insertAdapterOfPlayerAchievementEntity = object :
        EntityInsertAdapter<PlayerAchievementEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR IGNORE INTO `player_achievements` (`achievementKey`,`unlockedAtEpochMs`) VALUES (?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: PlayerAchievementEntity) {
        statement.bindText(1, entity.achievementKey)
        statement.bindLong(2, entity.unlockedAtEpochMs)
      }
    }
  }

  public override suspend fun upsertDefs(defs: List<AchievementDefEntity>): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfAchievementDefEntity.insert(_connection, defs)
  }

  public override suspend fun unlock(achievement: PlayerAchievementEntity): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfPlayerAchievementEntity.insert(_connection, achievement)
  }

  public override fun observeDefs(): Flow<List<AchievementDefEntity>> {
    val _sql: String = "SELECT * FROM achievement_defs"
    return createFlow(__db, false, arrayOf("achievement_defs")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfKey: Int = getColumnIndexOrThrow(_stmt, "key")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfCriteriaType: Int = getColumnIndexOrThrow(_stmt, "criteriaType")
        val _columnIndexOfCriteriaValue: Int = getColumnIndexOrThrow(_stmt, "criteriaValue")
        val _columnIndexOfRewardXp: Int = getColumnIndexOrThrow(_stmt, "rewardXp")
        val _result: MutableList<AchievementDefEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: AchievementDefEntity
          val _tmpKey: String
          _tmpKey = _stmt.getText(_columnIndexOfKey)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpCriteriaType: String
          _tmpCriteriaType = _stmt.getText(_columnIndexOfCriteriaType)
          val _tmpCriteriaValue: Int
          _tmpCriteriaValue = _stmt.getLong(_columnIndexOfCriteriaValue).toInt()
          val _tmpRewardXp: Int
          _tmpRewardXp = _stmt.getLong(_columnIndexOfRewardXp).toInt()
          _item =
              AchievementDefEntity(_tmpKey,_tmpName,_tmpDescription,_tmpCriteriaType,_tmpCriteriaValue,_tmpRewardXp)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getDefs(): List<AchievementDefEntity> {
    val _sql: String = "SELECT * FROM achievement_defs"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfKey: Int = getColumnIndexOrThrow(_stmt, "key")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfCriteriaType: Int = getColumnIndexOrThrow(_stmt, "criteriaType")
        val _columnIndexOfCriteriaValue: Int = getColumnIndexOrThrow(_stmt, "criteriaValue")
        val _columnIndexOfRewardXp: Int = getColumnIndexOrThrow(_stmt, "rewardXp")
        val _result: MutableList<AchievementDefEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: AchievementDefEntity
          val _tmpKey: String
          _tmpKey = _stmt.getText(_columnIndexOfKey)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpCriteriaType: String
          _tmpCriteriaType = _stmt.getText(_columnIndexOfCriteriaType)
          val _tmpCriteriaValue: Int
          _tmpCriteriaValue = _stmt.getLong(_columnIndexOfCriteriaValue).toInt()
          val _tmpRewardXp: Int
          _tmpRewardXp = _stmt.getLong(_columnIndexOfRewardXp).toInt()
          _item =
              AchievementDefEntity(_tmpKey,_tmpName,_tmpDescription,_tmpCriteriaType,_tmpCriteriaValue,_tmpRewardXp)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeUnlocked(): Flow<List<PlayerAchievementEntity>> {
    val _sql: String = "SELECT * FROM player_achievements"
    return createFlow(__db, false, arrayOf("player_achievements")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfAchievementKey: Int = getColumnIndexOrThrow(_stmt, "achievementKey")
        val _columnIndexOfUnlockedAtEpochMs: Int = getColumnIndexOrThrow(_stmt, "unlockedAtEpochMs")
        val _result: MutableList<PlayerAchievementEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: PlayerAchievementEntity
          val _tmpAchievementKey: String
          _tmpAchievementKey = _stmt.getText(_columnIndexOfAchievementKey)
          val _tmpUnlockedAtEpochMs: Long
          _tmpUnlockedAtEpochMs = _stmt.getLong(_columnIndexOfUnlockedAtEpochMs)
          _item = PlayerAchievementEntity(_tmpAchievementKey,_tmpUnlockedAtEpochMs)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getUnlocked(): List<PlayerAchievementEntity> {
    val _sql: String = "SELECT * FROM player_achievements"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfAchievementKey: Int = getColumnIndexOrThrow(_stmt, "achievementKey")
        val _columnIndexOfUnlockedAtEpochMs: Int = getColumnIndexOrThrow(_stmt, "unlockedAtEpochMs")
        val _result: MutableList<PlayerAchievementEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: PlayerAchievementEntity
          val _tmpAchievementKey: String
          _tmpAchievementKey = _stmt.getText(_columnIndexOfAchievementKey)
          val _tmpUnlockedAtEpochMs: Long
          _tmpUnlockedAtEpochMs = _stmt.getLong(_columnIndexOfUnlockedAtEpochMs)
          _item = PlayerAchievementEntity(_tmpAchievementKey,_tmpUnlockedAtEpochMs)
          _result.add(_item)
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
