package com.example.recepy.viewmodel

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.recepy.R
import com.example.recepy.data.local.AppDatabase
import com.example.recepy.data.repository.Recipe
import com.example.recepy.data.repository.RecipeRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecipeDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RecipeRepository(
        recipeDao = AppDatabase.getDatabase(application).recipeDao()
    )

    private val _currentRecipe = MutableStateFlow<Recipe?>(null)
    val currentRecipe: StateFlow<Recipe?> = _currentRecipe.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var lastLoadedId: Long? = null

    fun loadRecipe(recipeId: Long, extractedRecipe: Recipe?, groupRecipes: Map<String, List<Recipe>>) {
        if (recipeId == -1L) {
            _currentRecipe.value = extractedRecipe
            _isSaved.value = extractedRecipe?.id?.let { it > 0L } == true
            lastLoadedId = -1L
            return
        }

        if (recipeId == -2L) {
            // Find in groupRecipes (all values flattened)
            val groupRecipe = groupRecipes.values.flatten().find { it.id == -2L && it.title == extractedRecipe?.title }
            _currentRecipe.value = groupRecipe ?: extractedRecipe
            _isSaved.value = true // Treat group recipes as "saved" (read-only)
            lastLoadedId = -2L
            return
        }

        if (lastLoadedId == recipeId && _currentRecipe.value?.id == recipeId) {
            return
        }

        lastLoadedId = recipeId
        viewModelScope.launch {
            val recipe = repository.getRecipeById(recipeId)
            if (recipe == null) {
                _currentRecipe.value = null
                _isSaved.value = false
                _messages.emit(getString(R.string.detail_not_found))
            } else {
                _currentRecipe.value = recipe
                _isSaved.value = recipe.id > 0L
            }
        }
    }

    // הוספה: קבלת פרמטר של המתכון לשמירה בעת עריכה
    fun saveRecipe(updatedRecipe: Recipe? = null) {
        val recipe = updatedRecipe ?: _currentRecipe.value ?: return

        viewModelScope.launch {
            _isSaving.value = true
            runCatching {
                val safeDate = if (recipe.dateAdded > 0L) {
                    recipe.dateAdded
                } else {
                    System.currentTimeMillis()
                }
                repository.saveRecipe(recipe.copy(dateAdded = safeDate))
            }.onSuccess { savedId ->
                _currentRecipe.value = recipe.copy(id = savedId)
                _isSaved.value = true
                _messages.emit(getString(R.string.recipe_saved))
            }.onFailure { throwable ->
                val message = throwable.message?.takeIf { it.isNotBlank() }
                    ?: getString(R.string.unknown_error)
                _messages.emit(message)
            }
            _isSaving.value = false
        }
    }

    fun deleteCurrentRecipe() {
        val recipe = _currentRecipe.value ?: return
        if (recipe.id <= 0L) return

        viewModelScope.launch {
            _isSaving.value = true
            runCatching {
                repository.deleteRecipe(recipe.id)
            }.onSuccess {
                _currentRecipe.value = recipe.copy(id = -1)
                _isSaved.value = false
                _messages.emit(getString(R.string.recipe_deleted))
            }.onFailure { throwable ->
                val message = throwable.message?.takeIf { it.isNotBlank() }
                    ?: getString(R.string.unknown_error)
                _messages.emit(message)
            }
            _isSaving.value = false
        }
    }

    // הפונקציה החדשה שמייבאת את המתכונים!
    fun loadSeedRecipes() {
        viewModelScope.launch {
            val context = getApplication<Application>().applicationContext
            val insertedCount = repository.importBundledRecipes(context)
            if (insertedCount > 0) {
                _messages.emit("שוחזרו $insertedCount מתכונים בהצלחה!")
            }
        }
    }

    private fun getString(@StringRes resId: Int): String {
        return getApplication<Application>().getString(resId)
    }
}