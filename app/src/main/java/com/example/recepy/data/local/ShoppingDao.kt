package com.example.recepy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ShoppingItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ShoppingItem>)

    @Query("SELECT * FROM shopping_items ORDER BY recipeName, id")
    fun getAllItemsFlow(): Flow<List<ShoppingItem>>

    @Query("SELECT * FROM shopping_items ORDER BY recipeName, id")
    fun getAllItemsSync(): List<ShoppingItem>

    @Query("UPDATE shopping_items SET isChecked = NOT isChecked WHERE id = :itemId")
    suspend fun toggleItemChecked(itemId: Long)

    @Query("DELETE FROM shopping_items WHERE id = :itemId")
    suspend fun deleteItem(itemId: Long)

    @Query("DELETE FROM shopping_items")
    suspend fun clearAllItems()

    @Query("SELECT * FROM shopping_items WHERE id = :itemId")
    suspend fun getItemById(itemId: Long): ShoppingItem?

    @Query("DELETE FROM shopping_items WHERE isChecked = 1")
    suspend fun deleteCheckedItems()
}