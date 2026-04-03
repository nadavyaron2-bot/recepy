package com.example.recepy.`data`.local

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.appendPlaceholders
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlin.text.StringBuilder
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class RecipeDao_Impl(
  __db: RoomDatabase,
) : RecipeDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfRecipeEntity: EntityInsertAdapter<RecipeEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfRecipeEntity = object : EntityInsertAdapter<RecipeEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `recipes` (`id`,`title`,`imageUrl`,`ingredients`,`steps`,`sourceUrl`,`dateAdded`,`isFavorite`,`tags`,`rating`,`notes`,`lastCooked`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: RecipeEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.title)
        val _tmpImageUrl: String? = entity.imageUrl
        if (_tmpImageUrl == null) {
          statement.bindNull(3)
        } else {
          statement.bindText(3, _tmpImageUrl)
        }
        statement.bindText(4, entity.ingredients)
        statement.bindText(5, entity.steps)
        statement.bindText(6, entity.sourceUrl)
        statement.bindLong(7, entity.dateAdded)
        val _tmp: Int = if (entity.isFavorite) 1 else 0
        statement.bindLong(8, _tmp.toLong())
        val _tmpTags: String? = entity.tags
        if (_tmpTags == null) {
          statement.bindNull(9)
        } else {
          statement.bindText(9, _tmpTags)
        }
        statement.bindLong(10, entity.rating.toLong())
        statement.bindText(11, entity.notes)
        statement.bindLong(12, entity.lastCooked)
      }
    }
  }

  public override suspend fun insert(recipe: RecipeEntity): Long = performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfRecipeEntity.insertAndReturnId(_connection, recipe)
    _result
  }

  public override fun observeRecipes(): Flow<List<RecipeEntity>> {
    val _sql: String = "SELECT * FROM recipes ORDER BY dateAdded DESC"
    return createFlow(__db, false, arrayOf("recipes")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfImageUrl: Int = getColumnIndexOrThrow(_stmt, "imageUrl")
        val _columnIndexOfIngredients: Int = getColumnIndexOrThrow(_stmt, "ingredients")
        val _columnIndexOfSteps: Int = getColumnIndexOrThrow(_stmt, "steps")
        val _columnIndexOfSourceUrl: Int = getColumnIndexOrThrow(_stmt, "sourceUrl")
        val _columnIndexOfDateAdded: Int = getColumnIndexOrThrow(_stmt, "dateAdded")
        val _columnIndexOfIsFavorite: Int = getColumnIndexOrThrow(_stmt, "isFavorite")
        val _columnIndexOfTags: Int = getColumnIndexOrThrow(_stmt, "tags")
        val _columnIndexOfRating: Int = getColumnIndexOrThrow(_stmt, "rating")
        val _columnIndexOfNotes: Int = getColumnIndexOrThrow(_stmt, "notes")
        val _columnIndexOfLastCooked: Int = getColumnIndexOrThrow(_stmt, "lastCooked")
        val _result: MutableList<RecipeEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: RecipeEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpImageUrl: String?
          if (_stmt.isNull(_columnIndexOfImageUrl)) {
            _tmpImageUrl = null
          } else {
            _tmpImageUrl = _stmt.getText(_columnIndexOfImageUrl)
          }
          val _tmpIngredients: String
          _tmpIngredients = _stmt.getText(_columnIndexOfIngredients)
          val _tmpSteps: String
          _tmpSteps = _stmt.getText(_columnIndexOfSteps)
          val _tmpSourceUrl: String
          _tmpSourceUrl = _stmt.getText(_columnIndexOfSourceUrl)
          val _tmpDateAdded: Long
          _tmpDateAdded = _stmt.getLong(_columnIndexOfDateAdded)
          val _tmpIsFavorite: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsFavorite).toInt()
          _tmpIsFavorite = _tmp != 0
          val _tmpTags: String?
          if (_stmt.isNull(_columnIndexOfTags)) {
            _tmpTags = null
          } else {
            _tmpTags = _stmt.getText(_columnIndexOfTags)
          }
          val _tmpRating: Int
          _tmpRating = _stmt.getLong(_columnIndexOfRating).toInt()
          val _tmpNotes: String
          _tmpNotes = _stmt.getText(_columnIndexOfNotes)
          val _tmpLastCooked: Long
          _tmpLastCooked = _stmt.getLong(_columnIndexOfLastCooked)
          _item = RecipeEntity(_tmpId,_tmpTitle,_tmpImageUrl,_tmpIngredients,_tmpSteps,_tmpSourceUrl,_tmpDateAdded,_tmpIsFavorite,_tmpTags,_tmpRating,_tmpNotes,_tmpLastCooked)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAllRecipesOnce(): List<RecipeEntity> {
    val _sql: String = "SELECT * FROM recipes"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfImageUrl: Int = getColumnIndexOrThrow(_stmt, "imageUrl")
        val _columnIndexOfIngredients: Int = getColumnIndexOrThrow(_stmt, "ingredients")
        val _columnIndexOfSteps: Int = getColumnIndexOrThrow(_stmt, "steps")
        val _columnIndexOfSourceUrl: Int = getColumnIndexOrThrow(_stmt, "sourceUrl")
        val _columnIndexOfDateAdded: Int = getColumnIndexOrThrow(_stmt, "dateAdded")
        val _columnIndexOfIsFavorite: Int = getColumnIndexOrThrow(_stmt, "isFavorite")
        val _columnIndexOfTags: Int = getColumnIndexOrThrow(_stmt, "tags")
        val _columnIndexOfRating: Int = getColumnIndexOrThrow(_stmt, "rating")
        val _columnIndexOfNotes: Int = getColumnIndexOrThrow(_stmt, "notes")
        val _columnIndexOfLastCooked: Int = getColumnIndexOrThrow(_stmt, "lastCooked")
        val _result: MutableList<RecipeEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: RecipeEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpImageUrl: String?
          if (_stmt.isNull(_columnIndexOfImageUrl)) {
            _tmpImageUrl = null
          } else {
            _tmpImageUrl = _stmt.getText(_columnIndexOfImageUrl)
          }
          val _tmpIngredients: String
          _tmpIngredients = _stmt.getText(_columnIndexOfIngredients)
          val _tmpSteps: String
          _tmpSteps = _stmt.getText(_columnIndexOfSteps)
          val _tmpSourceUrl: String
          _tmpSourceUrl = _stmt.getText(_columnIndexOfSourceUrl)
          val _tmpDateAdded: Long
          _tmpDateAdded = _stmt.getLong(_columnIndexOfDateAdded)
          val _tmpIsFavorite: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsFavorite).toInt()
          _tmpIsFavorite = _tmp != 0
          val _tmpTags: String?
          if (_stmt.isNull(_columnIndexOfTags)) {
            _tmpTags = null
          } else {
            _tmpTags = _stmt.getText(_columnIndexOfTags)
          }
          val _tmpRating: Int
          _tmpRating = _stmt.getLong(_columnIndexOfRating).toInt()
          val _tmpNotes: String
          _tmpNotes = _stmt.getText(_columnIndexOfNotes)
          val _tmpLastCooked: Long
          _tmpLastCooked = _stmt.getLong(_columnIndexOfLastCooked)
          _item = RecipeEntity(_tmpId,_tmpTitle,_tmpImageUrl,_tmpIngredients,_tmpSteps,_tmpSourceUrl,_tmpDateAdded,_tmpIsFavorite,_tmpTags,_tmpRating,_tmpNotes,_tmpLastCooked)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getRecipeById(id: Long): RecipeEntity? {
    val _sql: String = "SELECT * FROM recipes WHERE id = ? LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfImageUrl: Int = getColumnIndexOrThrow(_stmt, "imageUrl")
        val _columnIndexOfIngredients: Int = getColumnIndexOrThrow(_stmt, "ingredients")
        val _columnIndexOfSteps: Int = getColumnIndexOrThrow(_stmt, "steps")
        val _columnIndexOfSourceUrl: Int = getColumnIndexOrThrow(_stmt, "sourceUrl")
        val _columnIndexOfDateAdded: Int = getColumnIndexOrThrow(_stmt, "dateAdded")
        val _columnIndexOfIsFavorite: Int = getColumnIndexOrThrow(_stmt, "isFavorite")
        val _columnIndexOfTags: Int = getColumnIndexOrThrow(_stmt, "tags")
        val _columnIndexOfRating: Int = getColumnIndexOrThrow(_stmt, "rating")
        val _columnIndexOfNotes: Int = getColumnIndexOrThrow(_stmt, "notes")
        val _columnIndexOfLastCooked: Int = getColumnIndexOrThrow(_stmt, "lastCooked")
        val _result: RecipeEntity?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpImageUrl: String?
          if (_stmt.isNull(_columnIndexOfImageUrl)) {
            _tmpImageUrl = null
          } else {
            _tmpImageUrl = _stmt.getText(_columnIndexOfImageUrl)
          }
          val _tmpIngredients: String
          _tmpIngredients = _stmt.getText(_columnIndexOfIngredients)
          val _tmpSteps: String
          _tmpSteps = _stmt.getText(_columnIndexOfSteps)
          val _tmpSourceUrl: String
          _tmpSourceUrl = _stmt.getText(_columnIndexOfSourceUrl)
          val _tmpDateAdded: Long
          _tmpDateAdded = _stmt.getLong(_columnIndexOfDateAdded)
          val _tmpIsFavorite: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsFavorite).toInt()
          _tmpIsFavorite = _tmp != 0
          val _tmpTags: String?
          if (_stmt.isNull(_columnIndexOfTags)) {
            _tmpTags = null
          } else {
            _tmpTags = _stmt.getText(_columnIndexOfTags)
          }
          val _tmpRating: Int
          _tmpRating = _stmt.getLong(_columnIndexOfRating).toInt()
          val _tmpNotes: String
          _tmpNotes = _stmt.getText(_columnIndexOfNotes)
          val _tmpLastCooked: Long
          _tmpLastCooked = _stmt.getLong(_columnIndexOfLastCooked)
          _result = RecipeEntity(_tmpId,_tmpTitle,_tmpImageUrl,_tmpIngredients,_tmpSteps,_tmpSourceUrl,_tmpDateAdded,_tmpIsFavorite,_tmpTags,_tmpRating,_tmpNotes,_tmpLastCooked)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getAllSourceUrls(): Flow<List<String>> {
    val _sql: String = "SELECT sourceUrl FROM recipes"
    return createFlow(__db, false, arrayOf("recipes")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: MutableList<String> = mutableListOf()
        while (_stmt.step()) {
          val _item: String
          _item = _stmt.getText(0)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAllSourceUrlsOnce(): List<String> {
    val _sql: String = "SELECT sourceUrl FROM recipes"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: MutableList<String> = mutableListOf()
        while (_stmt.step()) {
          val _item: String
          _item = _stmt.getText(0)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteById(id: Long) {
    val _sql: String = "DELETE FROM recipes WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteAllRecipes() {
    val _sql: String = "DELETE FROM recipes"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteRecipesBySourceUrls(sourceUrls: List<String>) {
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("DELETE FROM recipes WHERE sourceUrl IN (")
    val _inputSize: Int = sourceUrls.size
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        for (_item: String in sourceUrls) {
          _stmt.bindText(_argIndex, _item)
          _argIndex++
        }
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
