package com.example.recepy.`data`.local

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
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
  private val _recipeDao: Lazy<RecipeDao> = lazy {
    RecipeDao_Impl(this)
  }

  private val _shoppingDao: Lazy<ShoppingDao> = lazy {
    ShoppingDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(1, "63fae9fd7de3ad883be79aa2e1407696", "928dbd13a602da1827572561d4e5b645") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `recipes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `imageUrl` TEXT, `ingredients` TEXT NOT NULL, `steps` TEXT NOT NULL, `sourceUrl` TEXT NOT NULL, `dateAdded` INTEGER NOT NULL, `isFavorite` INTEGER NOT NULL, `tags` TEXT, `rating` INTEGER NOT NULL, `notes` TEXT NOT NULL, `lastCooked` INTEGER NOT NULL)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_recipes_title` ON `recipes` (`title`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_recipes_sourceUrl` ON `recipes` (`sourceUrl`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `shopping_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `isChecked` INTEGER NOT NULL, `recipeName` TEXT)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '63fae9fd7de3ad883be79aa2e1407696')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `recipes`")
        connection.execSQL("DROP TABLE IF EXISTS `shopping_items`")
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

      public override fun onValidateSchema(connection: SQLiteConnection): RoomOpenDelegate.ValidationResult {
        val _columnsRecipes: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsRecipes.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRecipes.put("title", TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRecipes.put("imageUrl", TableInfo.Column("imageUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRecipes.put("ingredients", TableInfo.Column("ingredients", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRecipes.put("steps", TableInfo.Column("steps", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRecipes.put("sourceUrl", TableInfo.Column("sourceUrl", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRecipes.put("dateAdded", TableInfo.Column("dateAdded", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRecipes.put("isFavorite", TableInfo.Column("isFavorite", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRecipes.put("tags", TableInfo.Column("tags", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRecipes.put("rating", TableInfo.Column("rating", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRecipes.put("notes", TableInfo.Column("notes", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRecipes.put("lastCooked", TableInfo.Column("lastCooked", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysRecipes: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesRecipes: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesRecipes.add(TableInfo.Index("index_recipes_title", false, listOf("title"), listOf("ASC")))
        _indicesRecipes.add(TableInfo.Index("index_recipes_sourceUrl", false, listOf("sourceUrl"), listOf("ASC")))
        val _infoRecipes: TableInfo = TableInfo("recipes", _columnsRecipes, _foreignKeysRecipes, _indicesRecipes)
        val _existingRecipes: TableInfo = read(connection, "recipes")
        if (!_infoRecipes.equals(_existingRecipes)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |recipes(com.example.recepy.data.local.RecipeEntity).
              | Expected:
              |""".trimMargin() + _infoRecipes + """
              |
              | Found:
              |""".trimMargin() + _existingRecipes)
        }
        val _columnsShoppingItems: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsShoppingItems.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsShoppingItems.put("name", TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsShoppingItems.put("isChecked", TableInfo.Column("isChecked", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsShoppingItems.put("recipeName", TableInfo.Column("recipeName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysShoppingItems: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesShoppingItems: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoShoppingItems: TableInfo = TableInfo("shopping_items", _columnsShoppingItems, _foreignKeysShoppingItems, _indicesShoppingItems)
        val _existingShoppingItems: TableInfo = read(connection, "shopping_items")
        if (!_infoShoppingItems.equals(_existingShoppingItems)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |shopping_items(com.example.recepy.data.local.ShoppingItem).
              | Expected:
              |""".trimMargin() + _infoShoppingItems + """
              |
              | Found:
              |""".trimMargin() + _existingShoppingItems)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "recipes", "shopping_items")
  }

  public override fun clearAllTables() {
    super.performClear(false, "recipes", "shopping_items")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(RecipeDao::class, RecipeDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(ShoppingDao::class, ShoppingDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>): List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun recipeDao(): RecipeDao = _recipeDao.value

  public override fun shoppingDao(): ShoppingDao = _shoppingDao.value
}
