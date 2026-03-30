package com.example.recepy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {

    @Query("SELECT * FROM recipes ORDER BY dateAdded DESC")
    fun observeRecipes(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes")
    suspend fun getAllRecipesOnce(): List<RecipeEntity>

    @Query("SELECT * FROM recipes WHERE id = :id LIMIT 1")
    suspend fun getRecipeById(id: Long): RecipeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recipe: RecipeEntity): Long

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM recipes")
    suspend fun deleteAllRecipes()




    // Flow של כל ה-sourceUrl (עם כפילויות — הViewModel מקבץ לפי domain)
    @Query("SELECT sourceUrl FROM recipes")
    fun getAllSourceUrls(): Flow<List<String>>

    // גרסה חד-פעמית (suspend) לשימוש ב-ViewModel בעת מחיקה
    @Query("SELECT sourceUrl FROM recipes")
    suspend fun getAllSourceUrlsOnce(): List<String>

    // מוחק לפי רשימת URL מלאים
    @Query("DELETE FROM recipes WHERE sourceUrl IN (:sourceUrls)")
    suspend fun deleteRecipesBySourceUrls(sourceUrls: List<String>)

}
