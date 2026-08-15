package com.example.solo_levelling.`data`.db.dao

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.example.solo_levelling.`data`.db.entity.XpLedgerEntryEntity
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class XpDao_Impl(
  __db: RoomDatabase,
) : XpDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfXpLedgerEntryEntity: EntityInsertAdapter<XpLedgerEntryEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfXpLedgerEntryEntity = object : EntityInsertAdapter<XpLedgerEntryEntity>()
        {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `xp_ledger` (`id`,`amount`,`sourceType`,`sourceId`,`metadataJson`,`createdAtEpochMs`) VALUES (nullif(?, 0),?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: XpLedgerEntryEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.amount.toLong())
        statement.bindText(3, entity.sourceType)
        statement.bindText(4, entity.sourceId)
        statement.bindText(5, entity.metadataJson)
        statement.bindLong(6, entity.createdAtEpochMs)
      }
    }
  }

  public override suspend fun insertLedger(entry: XpLedgerEntryEntity): Long =
      performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfXpLedgerEntryEntity.insertAndReturnId(_connection, entry)
    _result
  }

  public override suspend fun findBySource(sourceType: String, sourceId: String):
      XpLedgerEntryEntity? {
    val _sql: String = "SELECT * FROM xp_ledger WHERE sourceType = ? AND sourceId = ? LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, sourceType)
        _argIndex = 2
        _stmt.bindText(_argIndex, sourceId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfSourceType: Int = getColumnIndexOrThrow(_stmt, "sourceType")
        val _columnIndexOfSourceId: Int = getColumnIndexOrThrow(_stmt, "sourceId")
        val _columnIndexOfMetadataJson: Int = getColumnIndexOrThrow(_stmt, "metadataJson")
        val _columnIndexOfCreatedAtEpochMs: Int = getColumnIndexOrThrow(_stmt, "createdAtEpochMs")
        val _result: XpLedgerEntryEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpAmount: Int
          _tmpAmount = _stmt.getLong(_columnIndexOfAmount).toInt()
          val _tmpSourceType: String
          _tmpSourceType = _stmt.getText(_columnIndexOfSourceType)
          val _tmpSourceId: String
          _tmpSourceId = _stmt.getText(_columnIndexOfSourceId)
          val _tmpMetadataJson: String
          _tmpMetadataJson = _stmt.getText(_columnIndexOfMetadataJson)
          val _tmpCreatedAtEpochMs: Long
          _tmpCreatedAtEpochMs = _stmt.getLong(_columnIndexOfCreatedAtEpochMs)
          _result =
              XpLedgerEntryEntity(_tmpId,_tmpAmount,_tmpSourceType,_tmpSourceId,_tmpMetadataJson,_tmpCreatedAtEpochMs)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeLedger(): Flow<List<XpLedgerEntryEntity>> {
    val _sql: String = "SELECT * FROM xp_ledger ORDER BY createdAtEpochMs DESC"
    return createFlow(__db, false, arrayOf("xp_ledger")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfSourceType: Int = getColumnIndexOrThrow(_stmt, "sourceType")
        val _columnIndexOfSourceId: Int = getColumnIndexOrThrow(_stmt, "sourceId")
        val _columnIndexOfMetadataJson: Int = getColumnIndexOrThrow(_stmt, "metadataJson")
        val _columnIndexOfCreatedAtEpochMs: Int = getColumnIndexOrThrow(_stmt, "createdAtEpochMs")
        val _result: MutableList<XpLedgerEntryEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: XpLedgerEntryEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpAmount: Int
          _tmpAmount = _stmt.getLong(_columnIndexOfAmount).toInt()
          val _tmpSourceType: String
          _tmpSourceType = _stmt.getText(_columnIndexOfSourceType)
          val _tmpSourceId: String
          _tmpSourceId = _stmt.getText(_columnIndexOfSourceId)
          val _tmpMetadataJson: String
          _tmpMetadataJson = _stmt.getText(_columnIndexOfMetadataJson)
          val _tmpCreatedAtEpochMs: Long
          _tmpCreatedAtEpochMs = _stmt.getLong(_columnIndexOfCreatedAtEpochMs)
          _item =
              XpLedgerEntryEntity(_tmpId,_tmpAmount,_tmpSourceType,_tmpSourceId,_tmpMetadataJson,_tmpCreatedAtEpochMs)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAllLedger(): List<XpLedgerEntryEntity> {
    val _sql: String = "SELECT * FROM xp_ledger ORDER BY createdAtEpochMs ASC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfAmount: Int = getColumnIndexOrThrow(_stmt, "amount")
        val _columnIndexOfSourceType: Int = getColumnIndexOrThrow(_stmt, "sourceType")
        val _columnIndexOfSourceId: Int = getColumnIndexOrThrow(_stmt, "sourceId")
        val _columnIndexOfMetadataJson: Int = getColumnIndexOrThrow(_stmt, "metadataJson")
        val _columnIndexOfCreatedAtEpochMs: Int = getColumnIndexOrThrow(_stmt, "createdAtEpochMs")
        val _result: MutableList<XpLedgerEntryEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: XpLedgerEntryEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpAmount: Int
          _tmpAmount = _stmt.getLong(_columnIndexOfAmount).toInt()
          val _tmpSourceType: String
          _tmpSourceType = _stmt.getText(_columnIndexOfSourceType)
          val _tmpSourceId: String
          _tmpSourceId = _stmt.getText(_columnIndexOfSourceId)
          val _tmpMetadataJson: String
          _tmpMetadataJson = _stmt.getText(_columnIndexOfMetadataJson)
          val _tmpCreatedAtEpochMs: Long
          _tmpCreatedAtEpochMs = _stmt.getLong(_columnIndexOfCreatedAtEpochMs)
          _item =
              XpLedgerEntryEntity(_tmpId,_tmpAmount,_tmpSourceType,_tmpSourceId,_tmpMetadataJson,_tmpCreatedAtEpochMs)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun sumXp(): Int {
    val _sql: String = "SELECT COALESCE(SUM(amount), 0) FROM xp_ledger"
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

  public override suspend fun sumXpBetween(startMs: Long, endMs: Long): Int {
    val _sql: String =
        "SELECT COALESCE(SUM(amount), 0) FROM xp_ledger WHERE createdAtEpochMs >= ? AND createdAtEpochMs < ? AND amount > 0"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, startMs)
        _argIndex = 2
        _stmt.bindLong(_argIndex, endMs)
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

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
