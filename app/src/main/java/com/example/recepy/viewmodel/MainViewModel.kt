package com.example.recepy.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.recepy.R
import com.example.recepy.data.local.AppDatabase
import com.example.recepy.data.local.ShoppingItem
import com.example.recepy.data.repository.Recipe
import com.example.recepy.data.repository.RecipeRepository
import com.example.recepy.widget.RecepyWidget
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface RecipeUiState {
    data object Idle : RecipeUiState
    data object Loading : RecipeUiState
    data class Success(val recipe: Recipe) : RecipeUiState
    data class Error(val message: String) : RecipeUiState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val predefinedTags = listOf(
        "בשרי", "חלבי", "פרווה", "טבעוני", "ללא גלוטן",
        "קינוח", "ארוחת בוקר", "ארוחת צהריים", "ארוחת ערב", "אפייה", "מהיר", "בריא"
    )

    private val db = AppDatabase.getDatabase(application)
    private val repository = RecipeRepository(recipeDao = db.recipeDao())
    private val shoppingDao = db.shoppingDao()
    private val gson = Gson()

    private val prefs = application.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _recipeUiState = MutableStateFlow<RecipeUiState>(RecipeUiState.Idle)
    val recipeUiState: StateFlow<RecipeUiState> = _recipeUiState.asStateFlow()

    private val _selectedRecipe = MutableStateFlow<Recipe?>(null)
    val selectedRecipe: StateFlow<Recipe?> = _selectedRecipe.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTagFilters = MutableStateFlow<Set<String>>(emptySet())
    val selectedTagFilters: StateFlow<Set<String>> = _selectedTagFilters.asStateFlow()

    private val _sortByAlpha = MutableStateFlow(prefs.getBoolean("sort_alpha", false))
    val sortByAlpha: StateFlow<Boolean> = _sortByAlpha.asStateFlow()

    private val _checkedIngredientsMap = MutableStateFlow<Map<Long, Set<Int>>>(emptyMap())
    val checkedIngredientsMap: StateFlow<Map<Long, Set<Int>>> = _checkedIngredientsMap.asStateFlow()

    private val _showShoppingList = MutableStateFlow(false)
    val showShoppingList: StateFlow<Boolean> = _showShoppingList.asStateFlow()

    fun setShowShoppingList(show: Boolean) {
        _showShoppingList.value = show
    }

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    fun setShowAddDialog(show: Boolean) {
        _showAddDialog.value = show
    }

    private val updateImportPrefs by lazy {
        application.getSharedPreferences("bundled_recipe_import", Context.MODE_PRIVATE)
    }

    val savedRecipes: StateFlow<List<Recipe>> = repository.observeSavedRecipes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val lastCookedRecipe: StateFlow<Recipe?> = savedRecipes.map { recipes ->
        recipes.filter { it.lastCooked > 0L }.maxByOrNull { it.lastCooked }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val filteredRecipes: StateFlow<List<Recipe>> = combine(
        savedRecipes, searchQuery, sortByAlpha, selectedTagFilters, lastCookedRecipe
    ) { recipes, query, sortByAlpha, tagFilters, lastCooked ->
        withContext(Dispatchers.Default) {
            var filtered = if (query.isBlank()) recipes else recipes.filter { it.title.contains(query, true) }
            if (tagFilters.isNotEmpty()) {
                filtered = filtered.filter { recipe -> recipe.tags.containsAll(tagFilters) }
            }
            val sorted = if (sortByAlpha) filtered.sortedBy { it.title } else filtered.sortedByDescending { it.dateAdded.takeIf { d -> d > 0L } ?: it.id }
            val finalSorted = sorted.sortedByDescending { it.isFavorite }

            if (query.isBlank() && tagFilters.isEmpty() && lastCooked != null) {
                finalSorted.filter { it.id != lastCooked.id }
            } else {
                finalSorted
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val shoppingList: StateFlow<List<ShoppingItem>> = shoppingDao.getAllItemsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        importBundledRecipesOnAppUpdate()
        syncWithRemoteSystemRecipes()
        cleanupOldUpdates()
    }

    private fun cleanupOldUpdates() {
        viewModelScope.launch(Dispatchers.IO) {
            val apkFile = java.io.File(getApplication<Application>().cacheDir, "update.apk")
            if (apkFile.exists()) {
                apkFile.delete()
            }
        }
    }

    private fun syncWithRemoteSystemRecipes() {
        viewModelScope.launch {
            val remoteUrl = "https://raw.githubusercontent.com/nadavyaron2-bot/recepy/refs/heads/main/system_recipes.json"
            runCatching { 
                val count = withContext(Dispatchers.IO) { repository.importRemoteRecipes(remoteUrl) }
                if (count > 0) {
                    val context = getApplication<Application>().applicationContext
                    RecepyWidget().updateAll(context)
                }
            }
        }
    }

    fun toggleIngredientCheck(recipeId: Long, index: Int) {
        val currentMap = _checkedIngredientsMap.value.toMutableMap()
        val currentSet = currentMap[recipeId] ?: emptySet()
        val newSet = if (currentSet.contains(index)) currentSet - index else currentSet + index
        currentMap[recipeId] = newSet
        _checkedIngredientsMap.value = currentMap
    }

    fun addIngredientsToCart(ingredients: List<String>, multiplier: Float, recipeName: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val newItems = ingredients.map { ing ->
                ShoppingItem(
                    name = scaleNumbersInText(ing, multiplier),
                    isChecked = false,
                    recipeName = recipeName
                )
            }
            shoppingDao.insertItems(newItems)
            updateWidgets()
        }
    }

    fun toggleShoppingItem(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            shoppingDao.toggleItemChecked(id)
            updateWidgets()
        }
    }

    fun removeShoppingItem(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            shoppingDao.deleteItem(id)
            updateWidgets()
        }
    }

    fun clearShoppingList() {
        viewModelScope.launch(Dispatchers.IO) {
            shoppingDao.clearAllItems()
            updateWidgets()
        }
    }

    fun clearCheckedShoppingItems() {
        viewModelScope.launch(Dispatchers.IO) {
            shoppingDao.deleteCheckedItems()
            updateWidgets()
        }
    }

    private suspend fun updateWidgets() {
        try {
            RecepyWidget().updateAll(getApplication())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scaleNumbersInText(text: String, multiplier: Float): String {
        if (multiplier == 1f) return text
        val regex = Regex("""\b(\d+(\.\d+)?)\b""")
        return regex.replace(text) { matchResult ->
            val num = matchResult.value.toFloatOrNull() ?: return@replace matchResult.value
            val scaled = num * multiplier
            if (scaled % 1.0f == 0f) scaled.toInt().toString() else "%.1f".format(scaled)
        }
    }

    fun onUrlChanged(value: String) { _urlInput.value = value }
    fun onSearchQueryChanged(value: String) { _searchQuery.value = value }
    fun toggleTagFilter(tag: String) {
        val current = _selectedTagFilters.value
        _selectedTagFilters.value = if (current.contains(tag)) current - tag else current + tag
    }
    fun toggleSort() {
        val newValue = !_sortByAlpha.value
        _sortByAlpha.value = newValue
        prefs.edit().putBoolean("sort_alpha", newValue).apply()
    }
    fun toggleFavorite(recipe: Recipe) { viewModelScope.launch { repository.saveRecipe(recipe.copy(isFavorite = !recipe.isFavorite)) } }
    fun saveManualRecipe(title: String, ingredients: String, steps: String, tags: List<String>, imageUrl: String?) {
        val newRecipe = Recipe(
            id = 0L, title = title.takeIf { it.isNotBlank() } ?: "מתכון חדש",
            ingredients = ingredients.split("\n").map { it.trim() }.filter { it.isNotBlank() },
            steps = steps.split("\n").map { it.trim() }.filter { it.isNotBlank() },
            imageUrl = imageUrl, sourceUrl = "הזנה ידנית", dateAdded = System.currentTimeMillis(), isFavorite = false, tags = tags
        )
        viewModelScope.launch { repository.saveRecipe(newRecipe) }
    }
    fun extractRecipe() {
        val input = _urlInput.value.trim()
        if (input.isBlank()) { _recipeUiState.value = RecipeUiState.Error(getString(R.string.invalid_url)); return }
        _urlInput.value = ""
        _recipeUiState.value = RecipeUiState.Loading
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { repository.extractRecipeFromUrl(input) } }
                .onSuccess { recipe -> _selectedRecipe.value = recipe; _recipeUiState.value = RecipeUiState.Success(recipe) }
                .onFailure { throwable -> _recipeUiState.value = RecipeUiState.Error(throwable.message?.takeIf { it.isNotBlank() } ?: getString(R.string.parse_error)) }
        }
    }
    fun handleSharedText(sharedText: String?): Boolean {
        val text = sharedText?.trim().orEmpty()
        if (!text.startsWith("http", ignoreCase = true)) return false
        val url = extractHttpUrl(text) ?: return false
        _urlInput.value = url; extractRecipe(); return true
    }
    fun extractRecipeFromText(recipeText: String, tags: List<String>, imageUrl: String?) {
        val normalizedText = recipeText.trim()
        if (normalizedText.isBlank()) { _recipeUiState.value = RecipeUiState.Error(getString(R.string.parse_error)); return }
        _recipeUiState.value = RecipeUiState.Loading
        viewModelScope.launch {
            runCatching { repository.extractRecipeFromText(normalizedText).copy(tags = tags, imageUrl = imageUrl) }
                .onSuccess { recipe -> _selectedRecipe.value = recipe; _recipeUiState.value = RecipeUiState.Success(recipe) }
                .onFailure { throwable -> _recipeUiState.value = RecipeUiState.Error(throwable.message?.takeIf { it.isNotBlank() } ?: getString(R.string.parse_error)) }
        }
    }
    fun deleteRecipe(recipeId: Long) { viewModelScope.launch { repository.deleteRecipe(recipeId) } }
    fun consumeUiState() { _recipeUiState.value = RecipeUiState.Idle }

    private fun importBundledRecipesOnAppUpdate() {
        viewModelScope.launch {
            val application = getApplication<Application>()
            val appLastUpdateTime = getAppLastUpdateTime(application)
            val lastImportedUpdateTime = updateImportPrefs.getLong(KEY_LAST_IMPORTED_UPDATE_TIME, 0L)
            if (appLastUpdateTime <= lastImportedUpdateTime) return@launch
            runCatching { withContext(Dispatchers.IO) { repository.importBundledRecipes(application) } }
                .onSuccess { updateImportPrefs.edit().putLong(KEY_LAST_IMPORTED_UPDATE_TIME, appLastUpdateTime).apply() }
        }
    }

    private fun getAppLastUpdateTime(application: Application): Long {
        return runCatching {
            val packageManager = application.packageManager
            val packageName = application.packageName
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0)).lastUpdateTime }
            else { @Suppress("DEPRECATION") packageManager.getPackageInfo(packageName, 0).lastUpdateTime }
        }.getOrDefault(0L)
    }

    private fun getString(resId: Int): String { return getApplication<Application>().getString(resId) }
    private fun extractHttpUrl(text: String): String? { return Regex("""https?://[^\s]+""", RegexOption.IGNORE_CASE).find(text)?.value }
    private companion object { const val KEY_LAST_IMPORTED_UPDATE_TIME = "key_last_imported_update_time" }

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _importMessage = MutableStateFlow<String?>(null)
    val importMessage: StateFlow<String?> = _importMessage.asStateFlow()

    private val _systemUpdateMessage = MutableStateFlow<String?>(null)
    val systemUpdateMessage: StateFlow<String?> = _systemUpdateMessage.asStateFlow()

    private val _appUpdateMessage = MutableStateFlow<String?>(null)
    val appUpdateMessage: StateFlow<String?> = _appUpdateMessage.asStateFlow()

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    private val _updateDownloadUrl = MutableStateFlow<String?>(null)
    val updateDownloadUrl: StateFlow<String?> = _updateDownloadUrl.asStateFlow()

    fun checkForAppUpdate(context: Context) {
        viewModelScope.launch {
            _appUpdateMessage.value = "בודק..."
            val versionUrl = "https://github.com/nadavyaron2-bot/recepy/raw/refs/heads/master/update/version.json"
            
            runCatching {
                withContext(Dispatchers.IO) {
                    val json = java.net.URL(versionUrl).readText()
                    val data: Map<String, Any> = gson.fromJson(json, object : com.google.gson.reflect.TypeToken<Map<String, Any>>() {}.type)
                    val remoteVersionCode = (data["versionCode"] as? Double)?.toInt() ?: 0
                    val downloadUrl = data["downloadUrl"] as? String
                    
                    val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
                    } else {
                        @Suppress("DEPRECATION")
                        context.packageManager.getPackageInfo(context.packageName, 0)
                    }
                    
                    val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode.toInt()
                    } else {
                        @Suppress("DEPRECATION")
                        packageInfo.versionCode
                    }
                    
                    if (remoteVersionCode > currentVersionCode && !downloadUrl.isNullOrBlank()) {
                        _updateDownloadUrl.value = downloadUrl
                        _showUpdateDialog.value = true
                        _appUpdateMessage.value = "קיימת גרסה חדשה!"
                    } else {
                        _appUpdateMessage.value = "האפליקציה מעודכנת"
                    }
                }
            }.onFailure {
                _appUpdateMessage.value = "שגיאה בבדיקת עדכון"
            }
        }
    }

    fun installUpdate(context: Context, url: String) {
        viewModelScope.launch {
            _appUpdateMessage.value = "מוריד עדכון..."
            runCatching {
                withContext(Dispatchers.IO) {
                    val apkFile = java.io.File(context.cacheDir, "update.apk")
                    java.net.URL(url).openStream().use { input ->
                        apkFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        apkFile
                    )
                    
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
                _appUpdateMessage.value = "הורדה הושלמה"
            }.onFailure {
                it.printStackTrace()
                _appUpdateMessage.value = "שגיאה בהורדת העדכון"
            }
        }
    }

    fun dismissUpdateDialog() {
        _showUpdateDialog.value = false
    }

    fun loadSeedRecipes() {
        viewModelScope.launch {
            _isImporting.value = true
            _systemUpdateMessage.value = "בודק..."
            val context = getApplication<android.app.Application>().applicationContext
            
            val remoteUrl = "https://raw.githubusercontent.com/nadavyaron2-bot/recepy/refs/heads/main/system_recipes.json"
            val insertedCount = runCatching { 
                withContext(Dispatchers.IO) { repository.importRemoteRecipes(remoteUrl) }
            }.getOrDefault(0)
            
            _isImporting.value = false
            
            if (insertedCount > 0) {
                _systemUpdateMessage.value = "נוספו $insertedCount מתכונים חדשים"
                RecepyWidget().updateAll(context)
            } else {
                _systemUpdateMessage.value = "המתכונים כבר מעודכנים"
            }
        }
    }

    fun exportAllRecipes(context: Context) {
        viewModelScope.launch {
            val json = repository.exportAllRecipesJson()
            val file = java.io.File(context.cacheDir, "recepy_backup.json")
            file.writeText(json)
            
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "שתף את כל המתכונים"))
        }
    }

    fun importRecipesFromUri(context: Context, uri: android.net.Uri) {
        viewModelScope.launch {
            _isImporting.value = true
            val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (json != null) {
                val count = repository.importRecipesFromJson(json)
                _importMessage.value = if (count > 0) "יובאו $count מתכונים חדשים" else "לא נמצאו מתכונים חדשים לייבוא"
                RecepyWidget().updateAll(context)
            }
            _isImporting.value = false
        }
    }

    fun consumeImportMessage() {
        _importMessage.value = null
    }
}
