package com.example.recepy.`data`.local

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performBlocking
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
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
public class ShoppingDao_Impl(
  __db: RoomDatabase,
) : ShoppingDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfShoppingItem: EntityInsertAdapter<ShoppingItem>
  init {
    this.__db = __db
    this.__insertAdapterOfShoppingItem = object : EntityInsertAdapter<ShoppingItem>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `shopping_items` (`id`,`name`,`isChecked`,`recipeName`) VALUES (nullif(?, 0),?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ShoppingItem) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        val _tmp: Int = if (entity.isChecked) 1 else 0
        statement.bindLong(3, _tmp.toLong())
        val _tmpRecipeName: String? = entity.recipeName
        if (_tmpRecipeName == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpRecipeName)
        }
      }
    }
  }

  public override suspend fun insertItem(item: ShoppingItem): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfShoppingItem.insert(_connection, item)
  }

  public override suspend fun insertItems(items: List<ShoppingItem>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfShoppingItem.insert(_connection, items)
  }

  public override fun getAllItemsFlow(): Flow<List<ShoppingItem>> {
    val _sql: String = "SELECT * FROM shopping_items ORDER BY recipeName, id"
    return createFlow(__db, false, arrayOf("shopping_items")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfIsChecked: Int = getColumnIndexOrThrow(_stmt, "isChecked")
        val _columnIndexOfRecipeName: Int = getColumnIndexOrThrow(_stmt, "recipeName")
        val _result: MutableList<ShoppingItem> = mutableListOf()
        while (_stmt.step()) {
          val _item: ShoppingItem
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpIsChecked: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsChecked).toInt()
          _tmpIsChecked = _tmp != 0
          val _tmpRecipeName: String?
          if (_stmt.isNull(_columnIndexOfRecipeName)) {
            _tmpRecipeName = null
          } else {
            _tmpRecipeName = _stmt.getText(_columnIndexOfRecipeName)
          }
          _item = ShoppingItem(_tmpId,_tmpName,_tmpIsChecked,_tmpRecipeName)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getAllItemsSync(): List<ShoppingItem> {
    val _sql: String = "SELECT * FROM shopping_items ORDER BY recipeName, id"
    return performBlocking(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfIsChecked: Int = getColumnIndexOrThrow(_stmt, "isChecked")
        val _columnIndexOfRecipeName: Int = getColumnIndexOrThrow(_stmt, "recipeName")
        val _result: MutableList<ShoppingItem> = mutableListOf()
        while (_stmt.step()) {
          val _item: ShoppingItem
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpIsChecked: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsChecked).toInt()
          _tmpIsChecked = _tmp != 0
          val _tmpRecipeName: String?
          if (_stmt.isNull(_columnIndexOfRecipeName)) {
            _tmpRecipeName = null
          } else {
            _tmpRecipeName = _stmt.getText(_columnIndexOfRecipeName)
          }
          _item = ShoppingItem(_tmpId,_tmpName,_tmpIsChecked,_tmpRecipeName)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getItemById(itemId: Long): ShoppingItem? {
    val _sql: String = "SELECT * FROM shopping_items WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, itemId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfIsChecked: Int = getColumnIndexOrThrow(_stmt, "isChecked")
        val _columnIndexOfRecipeName: Int = getColumnIndexOrThrow(_stmt, "recipeName")
        val _result: ShoppingItem?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpIsChecked: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsChecked).toInt()
          _tmpIsChecked = _tmp != 0
          val _tmpRecipeName: String?
          if (_stmt.isNull(_columnIndexOfRecipeName)) {
            _tmpRecipeName = null
          } else {
            _tmpRecipeName = _stmt.getText(_columnIndexOfRecipeName)
          }
          _result = ShoppingItem(_tmpId,_tmpName,_tmpIsChecked,_tmpRecipeName)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun toggleItemChecked(itemId: Long) {
    val _sql: String = "UPDATE shopping_items SET isChecked = NOT isChecked WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, itemId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteItem(itemId: Long) {
    val _sql: String = "DELETE FROM shopping_items WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, itemId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clearAllItems() {
    val _sql: String = "DELETE FROM shopping_items"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteCheckedItems() {
    val _sql: String = "DELETE FROM shopping_items WHERE isChecked = 1"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
