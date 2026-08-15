package com.example.solo_levelling.`data`.db.dao

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.example.solo_levelling.`data`.db.entity.QuestInstanceEntity
import com.example.solo_levelling.`data`.db.entity.QuestTemplateEntity
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
public class QuestDao_Impl(
  __db: RoomDatabase,
) : QuestDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfQuestTemplateEntity: EntityInsertAdapter<QuestTemplateEntity>

  private val __insertAdapterOfQuestInstanceEntity: EntityInsertAdapter<QuestInstanceEntity>

  private val __updateAdapterOfQuestInstanceEntity: EntityDeleteOrUpdateAdapter<QuestInstanceEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfQuestTemplateEntity = object : EntityInsertAdapter<QuestTemplateEntity>()
        {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `quest_templates` (`id`,`key`,`type`,`title`,`description`,`baseXp`,`attributeRewardsJson`,`scheduleDaysCsv`,`active`,`verificationType`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: QuestTemplateEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.key)
        statement.bindText(3, entity.type)
        statement.bindText(4, entity.title)
        statement.bindText(5, entity.description)
        statement.bindLong(6, entity.baseXp.toLong())
        statement.bindText(7, entity.attributeRewardsJson)
        statement.bindText(8, entity.scheduleDaysCsv)
        val _tmp: Int = if (entity.active) 1 else 0
        statement.bindLong(9, _tmp.toLong())
        statement.bindText(10, entity.verificationType)
      }
    }
    this.__insertAdapterOfQuestInstanceEntity = object : EntityInsertAdapter<QuestInstanceEntity>()
        {
      protected override fun createQuery(): String =
          "INSERT OR IGNORE INTO `quest_instances` (`id`,`templateId`,`scheduledDate`,`status`,`title`,`type`,`baseXp`,`attributeRewardsJson`,`completedAtEpochMs`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: QuestInstanceEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.templateId)
        statement.bindText(3, entity.scheduledDate)
        statement.bindText(4, entity.status)
        statement.bindText(5, entity.title)
        statement.bindText(6, entity.type)
        statement.bindLong(7, entity.baseXp.toLong())
        statement.bindText(8, entity.attributeRewardsJson)
        val _tmpCompletedAtEpochMs: Long? = entity.completedAtEpochMs
        if (_tmpCompletedAtEpochMs == null) {
          statement.bindNull(9)
        } else {
          statement.bindLong(9, _tmpCompletedAtEpochMs)
        }
      }
    }
    this.__updateAdapterOfQuestInstanceEntity = object :
        EntityDeleteOrUpdateAdapter<QuestInstanceEntity>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `quest_instances` SET `id` = ?,`templateId` = ?,`scheduledDate` = ?,`status` = ?,`title` = ?,`type` = ?,`baseXp` = ?,`attributeRewardsJson` = ?,`completedAtEpochMs` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: QuestInstanceEntity) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.templateId)
        statement.bindText(3, entity.scheduledDate)
        statement.bindText(4, entity.status)
        statement.bindText(5, entity.title)
        statement.bindText(6, entity.type)
        statement.bindLong(7, entity.baseXp.toLong())
        statement.bindText(8, entity.attributeRewardsJson)
        val _tmpCompletedAtEpochMs: Long? = entity.completedAtEpochMs
        if (_tmpCompletedAtEpochMs == null) {
          statement.bindNull(9)
        } else {
          statement.bindLong(9, _tmpCompletedAtEpochMs)
        }
        statement.bindLong(10, entity.id)
      }
    }
  }

  public override suspend fun upsertTemplates(templates: List<QuestTemplateEntity>): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfQuestTemplateEntity.insert(_connection, templates)
  }

  public override suspend fun upsertTemplate(template: QuestTemplateEntity): Long =
      performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfQuestTemplateEntity.insertAndReturnId(_connection,
        template)
    _result
  }

  public override suspend fun insertInstance(instance: QuestInstanceEntity): Long =
      performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfQuestInstanceEntity.insertAndReturnId(_connection,
        instance)
    _result
  }

  public override suspend fun updateInstance(instance: QuestInstanceEntity): Unit =
      performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfQuestInstanceEntity.handle(_connection, instance)
  }

  public override suspend fun getActiveTemplates(): List<QuestTemplateEntity> {
    val _sql: String = "SELECT * FROM quest_templates WHERE active = 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfKey: Int = getColumnIndexOrThrow(_stmt, "key")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfBaseXp: Int = getColumnIndexOrThrow(_stmt, "baseXp")
        val _columnIndexOfAttributeRewardsJson: Int = getColumnIndexOrThrow(_stmt,
            "attributeRewardsJson")
        val _columnIndexOfScheduleDaysCsv: Int = getColumnIndexOrThrow(_stmt, "scheduleDaysCsv")
        val _columnIndexOfActive: Int = getColumnIndexOrThrow(_stmt, "active")
        val _columnIndexOfVerificationType: Int = getColumnIndexOrThrow(_stmt, "verificationType")
        val _result: MutableList<QuestTemplateEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: QuestTemplateEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpKey: String
          _tmpKey = _stmt.getText(_columnIndexOfKey)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpBaseXp: Int
          _tmpBaseXp = _stmt.getLong(_columnIndexOfBaseXp).toInt()
          val _tmpAttributeRewardsJson: String
          _tmpAttributeRewardsJson = _stmt.getText(_columnIndexOfAttributeRewardsJson)
          val _tmpScheduleDaysCsv: String
          _tmpScheduleDaysCsv = _stmt.getText(_columnIndexOfScheduleDaysCsv)
          val _tmpActive: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfActive).toInt()
          _tmpActive = _tmp != 0
          val _tmpVerificationType: String
          _tmpVerificationType = _stmt.getText(_columnIndexOfVerificationType)
          _item =
              QuestTemplateEntity(_tmpId,_tmpKey,_tmpType,_tmpTitle,_tmpDescription,_tmpBaseXp,_tmpAttributeRewardsJson,_tmpScheduleDaysCsv,_tmpActive,_tmpVerificationType)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getTemplateByKey(key: String): QuestTemplateEntity? {
    val _sql: String = "SELECT * FROM quest_templates WHERE `key` = ? LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, key)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfKey: Int = getColumnIndexOrThrow(_stmt, "key")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfBaseXp: Int = getColumnIndexOrThrow(_stmt, "baseXp")
        val _columnIndexOfAttributeRewardsJson: Int = getColumnIndexOrThrow(_stmt,
            "attributeRewardsJson")
        val _columnIndexOfScheduleDaysCsv: Int = getColumnIndexOrThrow(_stmt, "scheduleDaysCsv")
        val _columnIndexOfActive: Int = getColumnIndexOrThrow(_stmt, "active")
        val _columnIndexOfVerificationType: Int = getColumnIndexOrThrow(_stmt, "verificationType")
        val _result: QuestTemplateEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpKey: String
          _tmpKey = _stmt.getText(_columnIndexOfKey)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpBaseXp: Int
          _tmpBaseXp = _stmt.getLong(_columnIndexOfBaseXp).toInt()
          val _tmpAttributeRewardsJson: String
          _tmpAttributeRewardsJson = _stmt.getText(_columnIndexOfAttributeRewardsJson)
          val _tmpScheduleDaysCsv: String
          _tmpScheduleDaysCsv = _stmt.getText(_columnIndexOfScheduleDaysCsv)
          val _tmpActive: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfActive).toInt()
          _tmpActive = _tmp != 0
          val _tmpVerificationType: String
          _tmpVerificationType = _stmt.getText(_columnIndexOfVerificationType)
          _result =
              QuestTemplateEntity(_tmpId,_tmpKey,_tmpType,_tmpTitle,_tmpDescription,_tmpBaseXp,_tmpAttributeRewardsJson,_tmpScheduleDaysCsv,_tmpActive,_tmpVerificationType)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeTemplates(): Flow<List<QuestTemplateEntity>> {
    val _sql: String = "SELECT * FROM quest_templates"
    return createFlow(__db, false, arrayOf("quest_templates")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfKey: Int = getColumnIndexOrThrow(_stmt, "key")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfBaseXp: Int = getColumnIndexOrThrow(_stmt, "baseXp")
        val _columnIndexOfAttributeRewardsJson: Int = getColumnIndexOrThrow(_stmt,
            "attributeRewardsJson")
        val _columnIndexOfScheduleDaysCsv: Int = getColumnIndexOrThrow(_stmt, "scheduleDaysCsv")
        val _columnIndexOfActive: Int = getColumnIndexOrThrow(_stmt, "active")
        val _columnIndexOfVerificationType: Int = getColumnIndexOrThrow(_stmt, "verificationType")
        val _result: MutableList<QuestTemplateEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: QuestTemplateEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpKey: String
          _tmpKey = _stmt.getText(_columnIndexOfKey)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpBaseXp: Int
          _tmpBaseXp = _stmt.getLong(_columnIndexOfBaseXp).toInt()
          val _tmpAttributeRewardsJson: String
          _tmpAttributeRewardsJson = _stmt.getText(_columnIndexOfAttributeRewardsJson)
          val _tmpScheduleDaysCsv: String
          _tmpScheduleDaysCsv = _stmt.getText(_columnIndexOfScheduleDaysCsv)
          val _tmpActive: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfActive).toInt()
          _tmpActive = _tmp != 0
          val _tmpVerificationType: String
          _tmpVerificationType = _stmt.getText(_columnIndexOfVerificationType)
          _item =
              QuestTemplateEntity(_tmpId,_tmpKey,_tmpType,_tmpTitle,_tmpDescription,_tmpBaseXp,_tmpAttributeRewardsJson,_tmpScheduleDaysCsv,_tmpActive,_tmpVerificationType)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun observeInstancesForDate(date: String): Flow<List<QuestInstanceEntity>> {
    val _sql: String = "SELECT * FROM quest_instances WHERE scheduledDate = ? ORDER BY id ASC"
    return createFlow(__db, false, arrayOf("quest_instances")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, date)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTemplateId: Int = getColumnIndexOrThrow(_stmt, "templateId")
        val _columnIndexOfScheduledDate: Int = getColumnIndexOrThrow(_stmt, "scheduledDate")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfBaseXp: Int = getColumnIndexOrThrow(_stmt, "baseXp")
        val _columnIndexOfAttributeRewardsJson: Int = getColumnIndexOrThrow(_stmt,
            "attributeRewardsJson")
        val _columnIndexOfCompletedAtEpochMs: Int = getColumnIndexOrThrow(_stmt,
            "completedAtEpochMs")
        val _result: MutableList<QuestInstanceEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: QuestInstanceEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTemplateId: Long
          _tmpTemplateId = _stmt.getLong(_columnIndexOfTemplateId)
          val _tmpScheduledDate: String
          _tmpScheduledDate = _stmt.getText(_columnIndexOfScheduledDate)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpBaseXp: Int
          _tmpBaseXp = _stmt.getLong(_columnIndexOfBaseXp).toInt()
          val _tmpAttributeRewardsJson: String
          _tmpAttributeRewardsJson = _stmt.getText(_columnIndexOfAttributeRewardsJson)
          val _tmpCompletedAtEpochMs: Long?
          if (_stmt.isNull(_columnIndexOfCompletedAtEpochMs)) {
            _tmpCompletedAtEpochMs = null
          } else {
            _tmpCompletedAtEpochMs = _stmt.getLong(_columnIndexOfCompletedAtEpochMs)
          }
          _item =
              QuestInstanceEntity(_tmpId,_tmpTemplateId,_tmpScheduledDate,_tmpStatus,_tmpTitle,_tmpType,_tmpBaseXp,_tmpAttributeRewardsJson,_tmpCompletedAtEpochMs)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getInstancesForDate(date: String): List<QuestInstanceEntity> {
    val _sql: String = "SELECT * FROM quest_instances WHERE scheduledDate = ? ORDER BY id ASC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, date)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTemplateId: Int = getColumnIndexOrThrow(_stmt, "templateId")
        val _columnIndexOfScheduledDate: Int = getColumnIndexOrThrow(_stmt, "scheduledDate")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfBaseXp: Int = getColumnIndexOrThrow(_stmt, "baseXp")
        val _columnIndexOfAttributeRewardsJson: Int = getColumnIndexOrThrow(_stmt,
            "attributeRewardsJson")
        val _columnIndexOfCompletedAtEpochMs: Int = getColumnIndexOrThrow(_stmt,
            "completedAtEpochMs")
        val _result: MutableList<QuestInstanceEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: QuestInstanceEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTemplateId: Long
          _tmpTemplateId = _stmt.getLong(_columnIndexOfTemplateId)
          val _tmpScheduledDate: String
          _tmpScheduledDate = _stmt.getText(_columnIndexOfScheduledDate)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpBaseXp: Int
          _tmpBaseXp = _stmt.getLong(_columnIndexOfBaseXp).toInt()
          val _tmpAttributeRewardsJson: String
          _tmpAttributeRewardsJson = _stmt.getText(_columnIndexOfAttributeRewardsJson)
          val _tmpCompletedAtEpochMs: Long?
          if (_stmt.isNull(_columnIndexOfCompletedAtEpochMs)) {
            _tmpCompletedAtEpochMs = null
          } else {
            _tmpCompletedAtEpochMs = _stmt.getLong(_columnIndexOfCompletedAtEpochMs)
          }
          _item =
              QuestInstanceEntity(_tmpId,_tmpTemplateId,_tmpScheduledDate,_tmpStatus,_tmpTitle,_tmpType,_tmpBaseXp,_tmpAttributeRewardsJson,_tmpCompletedAtEpochMs)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getInstance(id: Long): QuestInstanceEntity? {
    val _sql: String = "SELECT * FROM quest_instances WHERE id = ? LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTemplateId: Int = getColumnIndexOrThrow(_stmt, "templateId")
        val _columnIndexOfScheduledDate: Int = getColumnIndexOrThrow(_stmt, "scheduledDate")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfBaseXp: Int = getColumnIndexOrThrow(_stmt, "baseXp")
        val _columnIndexOfAttributeRewardsJson: Int = getColumnIndexOrThrow(_stmt,
            "attributeRewardsJson")
        val _columnIndexOfCompletedAtEpochMs: Int = getColumnIndexOrThrow(_stmt,
            "completedAtEpochMs")
        val _result: QuestInstanceEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTemplateId: Long
          _tmpTemplateId = _stmt.getLong(_columnIndexOfTemplateId)
          val _tmpScheduledDate: String
          _tmpScheduledDate = _stmt.getText(_columnIndexOfScheduledDate)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpBaseXp: Int
          _tmpBaseXp = _stmt.getLong(_columnIndexOfBaseXp).toInt()
          val _tmpAttributeRewardsJson: String
          _tmpAttributeRewardsJson = _stmt.getText(_columnIndexOfAttributeRewardsJson)
          val _tmpCompletedAtEpochMs: Long?
          if (_stmt.isNull(_columnIndexOfCompletedAtEpochMs)) {
            _tmpCompletedAtEpochMs = null
          } else {
            _tmpCompletedAtEpochMs = _stmt.getLong(_columnIndexOfCompletedAtEpochMs)
          }
          _result =
              QuestInstanceEntity(_tmpId,_tmpTemplateId,_tmpScheduledDate,_tmpStatus,_tmpTitle,_tmpType,_tmpBaseXp,_tmpAttributeRewardsJson,_tmpCompletedAtEpochMs)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getWeeklyInstances(weekStart: String, weekEnd: String):
      List<QuestInstanceEntity> {
    val _sql: String =
        "SELECT * FROM quest_instances WHERE type = 'WEEKLY' AND scheduledDate >= ? AND scheduledDate <= ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, weekStart)
        _argIndex = 2
        _stmt.bindText(_argIndex, weekEnd)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTemplateId: Int = getColumnIndexOrThrow(_stmt, "templateId")
        val _columnIndexOfScheduledDate: Int = getColumnIndexOrThrow(_stmt, "scheduledDate")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfBaseXp: Int = getColumnIndexOrThrow(_stmt, "baseXp")
        val _columnIndexOfAttributeRewardsJson: Int = getColumnIndexOrThrow(_stmt,
            "attributeRewardsJson")
        val _columnIndexOfCompletedAtEpochMs: Int = getColumnIndexOrThrow(_stmt,
            "completedAtEpochMs")
        val _result: MutableList<QuestInstanceEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: QuestInstanceEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTemplateId: Long
          _tmpTemplateId = _stmt.getLong(_columnIndexOfTemplateId)
          val _tmpScheduledDate: String
          _tmpScheduledDate = _stmt.getText(_columnIndexOfScheduledDate)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpBaseXp: Int
          _tmpBaseXp = _stmt.getLong(_columnIndexOfBaseXp).toInt()
          val _tmpAttributeRewardsJson: String
          _tmpAttributeRewardsJson = _stmt.getText(_columnIndexOfAttributeRewardsJson)
          val _tmpCompletedAtEpochMs: Long?
          if (_stmt.isNull(_columnIndexOfCompletedAtEpochMs)) {
            _tmpCompletedAtEpochMs = null
          } else {
            _tmpCompletedAtEpochMs = _stmt.getLong(_columnIndexOfCompletedAtEpochMs)
          }
          _item =
              QuestInstanceEntity(_tmpId,_tmpTemplateId,_tmpScheduledDate,_tmpStatus,_tmpTitle,_tmpType,_tmpBaseXp,_tmpAttributeRewardsJson,_tmpCompletedAtEpochMs)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun countCompletedInRange(weekStart: String, weekEnd: String): Int {
    val _sql: String =
        "SELECT COUNT(*) FROM quest_instances WHERE scheduledDate >= ? AND scheduledDate <= ? AND status = 'COMPLETED'"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, weekStart)
        _argIndex = 2
        _stmt.bindText(_argIndex, weekEnd)
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

  public override suspend fun countTotalInRange(weekStart: String, weekEnd: String): Int {
    val _sql: String =
        "SELECT COUNT(*) FROM quest_instances WHERE scheduledDate >= ? AND scheduledDate <= ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, weekStart)
        _argIndex = 2
        _stmt.bindText(_argIndex, weekEnd)
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

  public override suspend fun countCompletedAll(): Int {
    val _sql: String = "SELECT COUNT(*) FROM quest_instances WHERE status = 'COMPLETED'"
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

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
