package com.example.recepy.viewmodel

import android.app.Application
import android.content.Context
import androidx.core.content.edit
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
//import org.jsoup.nodes.Document

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

    init {
        viewModelScope.launch {
            while (true) {
                fetchGroups()
                kotlinx.coroutines.delay(60_000 * 5) // Refresh every 5 minutes
            }
        }
    }

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

    private val _searchByIngredients = MutableStateFlow(false)
    val searchByIngredients: StateFlow<Boolean> = _searchByIngredients.asStateFlow()

    fun toggleSearchMode() {
        _searchByIngredients.value = !_searchByIngredients.value
    }

    private val _reportedBugs = MutableStateFlow<List<String>>(emptyList())
    val reportedBugs: StateFlow<List<String>> = _reportedBugs.asStateFlow()

    private val SCRIPT_URL = "https://script.google.com/macros/s/AKfycbwkHVM4ikm9jMKXhCg7KtYD54Q6yXOfTTtotJh1soz4qdPs1Jjmy5Aa4zupfaaeerNiLw/exec"

    fun reportBug(bug: String) {
        viewModelScope.launch {
            syncDeveloperDataWithRemote("add", "bug", bug)
        }
    }

    fun removeReportedBug(bug: String) {
        viewModelScope.launch {
            syncDeveloperDataWithRemote("delete", "bug", bug)
        }
    }

    private val _suggestedRecipesForDev = MutableStateFlow<List<String>>(emptyList())
    val suggestedRecipesForDev: StateFlow<List<String>> = _suggestedRecipesForDev.asStateFlow()

    fun suggestRecipeForDev(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            syncDeveloperDataWithRemote("add", "request", query)
        }
    }

    suspend fun searchMakoOnGoogle(query: String): String? = withContext(Dispatchers.IO) {
        try {
            val searchQuery = "site:mako.co.il $query מתכון"
            val url = "https://www.google.com/search?q=" + java.net.URLEncoder.encode(searchQuery, "UTF-8")
            
            val document = org.jsoup.Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .get()

            val links = document.select("a[href]")
            for (link in links) {
                val href = link.attr("href")
                if (href.contains("mako.co.il") && !href.contains("google.com")) {
                    val cleanUrl = if (href.startsWith("/url?q=")) {
                        href.substringAfter("/url?q=").substringBefore("&")
                    } else {
                        href
                    }
                    return@withContext cleanUrl
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    fun removeSuggestedRecipe(query: String) {
        viewModelScope.launch {
            syncDeveloperDataWithRemote("delete", "request", query)
        }
    }

    private fun syncDeveloperDataWithRemote(action: String, type: String, description: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val json = Gson().toJson(mapOf(
                    "action" to action,
                    "type" to type,
                    "description" to description,
                    "date" to System.currentTimeMillis().toString()
                ))
                org.jsoup.Jsoup.connect(SCRIPT_URL)
                    .ignoreContentType(true)
                    .requestBody(json)
                    .method(org.jsoup.Connection.Method.POST)
                    .followRedirects(true)
                    .execute()
            }
            // Always refresh data after any remote action to ensure UI reflects the "truth" from the table
            fetchDeveloperData()
            if (type == "group" || type == "group_recipe") {
                fetchGroups()
            }
        }
    }

    private val _groups = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val groups: StateFlow<List<Map<String, Any>>> = _groups.asStateFlow()

    private val deviceId: String by lazy {
        val id = prefs.getString("device_id", null)
        if (id == null) {
            val newId = java.util.UUID.randomUUID().toString()
            prefs.edit { putString("device_id", newId) }
            newId
        } else id
    }

    fun createGroup(name: String, password: String) {
        viewModelScope.launch {
            val groupId = (100000..999999).random().toString()
            val metadata = mapOf(
                "name" to name,
                "id" to groupId,
                "password" to password,
                "creatorId" to deviceId,
                "permissions" to 2
            )
            syncDeveloperDataWithRemote("add", "group", gson.toJson(metadata))
            // Automatically join the group we just created
            val joinedGroups = (prefs?.getStringSet("joined_groups", emptySet()) ?: emptySet()).toMutableSet()
            joinedGroups.add(groupId)
            prefs?.edit { putStringSet("joined_groups", joinedGroups) }
            fetchGroups()
        }
    }

    fun updateGroupPermissions(group: Map<String, Any>, newPermissions: Int) {
        viewModelScope.launch {
            val id = group["id"] as String
            
            val updatedMetadata = mapOf(
                "name" to (group["name"] as String),
                "id" to id,
                "password" to (group["password"] as String),
                "creatorId" to (group["creatorId"] as String),
                "permissions" to newPermissions
            )
            
            // Remove old entry (backend matches by type and description prefix or full match depending on script logic)
            // But with our new system, we'll just send the update action if the script supports it, 
            // or remove by ID if we update the script. For now, we follow the "remove then add" pattern but with JSON.
            syncDeveloperDataWithRemote("remove", "group", id) 
            syncDeveloperDataWithRemote("add", "group", gson.toJson(updatedMetadata))
            fetchGroups()
        }
    }

    fun joinGroup(name: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val response = org.jsoup.Jsoup.connect(SCRIPT_URL)
                    .ignoreContentType(true)
                    .method(org.jsoup.Connection.Method.GET)
                    .followRedirects(true)
                    .execute()
                
                val json = response.body()
                val listType = object : com.google.gson.reflect.TypeToken<List<Map<String, String>>>() {}.type
                val data: List<Map<String, String>> = Gson().fromJson(json, listType)
                
                val groupEntry = data.find { entry ->
                    if (entry["type"] != "group") return@find false
                    val desc = entry["description"] ?: return@find false
                    try {
                        val metadata = gson.fromJson(desc, Map::class.java)
                        metadata["name"] == name && metadata["password"] == password
                    } catch (_: Exception) { false }
                }

                if (groupEntry != null) {
                    val desc = groupEntry["description"]!!
                    val metadata = gson.fromJson(desc, Map::class.java)
                    val groupId = metadata["id"] as String
                    withContext(Dispatchers.Main) {
                        val joinedGroups = (prefs?.getStringSet("joined_groups", emptySet()) ?: emptySet()).toMutableSet()
                        joinedGroups.add(groupId)
                        prefs?.edit { putStringSet("joined_groups", joinedGroups) }
                        fetchGroups()
                    }
                }
            }.onFailure { it.printStackTrace() }
        }
    }

    fun leaveGroup(groupId: String) {
        viewModelScope.launch {
            val joinedGroups = prefs?.getStringSet("joined_groups", emptySet()) ?: emptySet()
            prefs?.edit { putStringSet("joined_groups", joinedGroups - groupId) }
            
            // Optionally notify backend if we track members there, 
            // but for now our system is ID-based only.
            
            fetchGroups()
        }
    }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            // Smart delete: removes the group entry and all associated recipes by searching for groupId
            syncDeveloperDataWithRemote("remove", "group", groupId)
            syncDeveloperDataWithRemote("remove", "group_recipe", groupId)
            
            // Also leave it locally if we were in it
            val joinedGroups = prefs.getStringSet("joined_groups", emptySet()) ?: emptySet()
            if (joinedGroups.contains(groupId)) {
                prefs.edit { putStringSet("joined_groups", joinedGroups - groupId) }
            }
            fetchGroups()
        }
    }

    private val _groupRecipes = MutableStateFlow<Map<String, List<Recipe>>>(emptyMap())
    val groupRecipes: StateFlow<Map<String, List<Recipe>>> = _groupRecipes.asStateFlow()

    fun fetchDeveloperData() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val response = org.jsoup.Jsoup.connect(SCRIPT_URL)
                    .ignoreContentType(true)
                    .method(org.jsoup.Connection.Method.GET)
                    .followRedirects(true)
                    .execute()
                
                val json = response.body()
                val listType = object : com.google.gson.reflect.TypeToken<List<Map<String, String>>>() {}.type
                val data: List<Map<String, String>> = Gson().fromJson(json, listType)
                
                val bugs = data.filter { it["type"] == "bug" }.mapNotNull { it["description"] ?: it["content"] }
                val requests = data.filter { it["type"] == "request" }.mapNotNull { it["description"] ?: it["content"] }
                
                withContext(Dispatchers.Main) {
                    _reportedBugs.value = bugs
                    _suggestedRecipesForDev.value = requests
                }
            }.onFailure { it.printStackTrace() }
        }
    }

    fun fetchGroups() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val response = org.jsoup.Jsoup.connect(SCRIPT_URL)
                    .ignoreContentType(true)
                    .method(org.jsoup.Connection.Method.GET)
                    .followRedirects(true)
                    .execute()
                
                val json = response.body()
                val listType = object : com.google.gson.reflect.TypeToken<List<Map<String, String>>>() {}.type
                val data: List<Map<String, String>> = Gson().fromJson(json, listType)
                
                val joinedGroupIds = prefs?.getStringSet("joined_groups", emptySet()) ?: emptySet()
                
                val groupsList = data.filter { it["type"] == "group" }.mapNotNull { 
                    val desc = it["description"] ?: it["content"] ?: return@mapNotNull null
                    try {
                        val metadata = gson.fromJson(desc, Map::class.java)
                        val id = metadata["id"] as String
                        if (joinedGroupIds.contains(id)) {
                            mutableMapOf(
                                "name" to (metadata["name"] ?: ""),
                                "id" to id,
                                "password" to (metadata["password"] ?: ""),
                                "creatorId" to (metadata["creatorId"] ?: ""),
                                "permissions" to (metadata["permissions"]?.toString()?.toDouble()?.toInt() ?: 2),
                                "isCreator" to (metadata["creatorId"] == deviceId)
                            )
                        } else null
                    } catch (_: Exception) {
                        // Fallback for old format
                        val parts = desc.split("|")
                        if (parts.size >= 3) {
                            val id = parts[1]
                            if (joinedGroupIds.contains(id)) {
                                mutableMapOf(
                                    "name" to parts[0],
                                    "id" to id,
                                    "password" to parts[2],
                                    "creatorId" to (parts.getOrNull(3) ?: ""),
                                    "permissions" to (parts.getOrNull(4)?.toIntOrNull() ?: 2),
                                    "isCreator" to (parts.getOrNull(3) == deviceId)
                                )
                            } else null
                        } else null
                    }
                }
                
                val groupRecipesMap = data.filter { it["type"] == "group_recipe" }.mapNotNull { 
                    val desc = it["description"] ?: it["content"] ?: return@mapNotNull null
                    try {
                        val recipeData = gson.fromJson(desc, Map::class.java)
                        val gid = recipeData["groupId"] as String
                        val recipeJson = recipeData["recipe"] as String
                        if (joinedGroupIds.contains(gid)) {
                            val recipe = gson.fromJson(recipeJson, Recipe::class.java).copy(id = -2L)
                            gid to recipe
                        } else null
                    } catch (_: Exception) {
                        // Fallback for old format
                        val parts = desc.split("|", limit = 4)
                        if (parts.size >= 2) {
                            val gid = parts[0]
                            val recipeJson = parts.last()
                            if (joinedGroupIds.contains(gid)) {
                                try {
                                    val recipe = gson.fromJson(recipeJson, Recipe::class.java).copy(id = -2L)
                                    gid to recipe
                                } catch (_: Exception) { null }
                            } else null
                        } else null
                    }
                }.groupBy({ it.first }, { it.second })
                
                withContext(Dispatchers.Main) {
                    _groups.value = groupsList
                    _groupRecipes.value = groupRecipesMap
                }
            }.onFailure { it.printStackTrace() }
        }
    }

    fun shareRecipeToGroup(recipe: Recipe, groupId: String) {
        viewModelScope.launch {
            // Clean up recipe data for sharing (remove local ID, reset favorite)
            val cleanRecipe = recipe.copy(id = 0, isFavorite = false)
            val content = mapOf(
                "groupId" to groupId,
                "recipe" to gson.toJson(cleanRecipe)
            )
            syncDeveloperDataWithRemote("add", "group_recipe", gson.toJson(content))
        }
    }

    fun deleteGroupRecipe(groupId: String, recipe: Recipe) {
        viewModelScope.launch {
            // Optimistic UI
            val currentMap = _groupRecipes.value.toMutableMap()
            val list = currentMap[groupId]?.toMutableList()
            if (list != null) {
                list.removeIf { it.title == recipe.title && it.sourceUrl == recipe.sourceUrl }
                currentMap[groupId] = list
                _groupRecipes.value = currentMap
            }
            
            syncDeveloperDataWithRemote("remove", "group_recipe", groupId + "|" + recipe.title)
        }
    }

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

    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress: StateFlow<Float?> = _downloadProgress.asStateFlow()

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

    private val allGroupRecipes: StateFlow<List<Recipe>> = groupRecipes.map { it.values.flatten() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filteredRecipes: StateFlow<List<Recipe>> = combine(
        listOf(
            savedRecipes,
            allGroupRecipes,
            searchQuery,
            sortByAlpha,
            selectedTagFilters,
            lastCookedRecipe,
            searchByIngredients
        )
    ) { array ->
        @Suppress("UNCHECKED_CAST")
        val localRecipes = array[0] as List<Recipe>
        // remoteRecipes (array[1]) is isolated from the main library view
        val query = array[2] as String
        val alpha = array[3] as Boolean
        @Suppress("UNCHECKED_CAST")
        val tagFilters = array[4] as Set<String>
        val lastCooked = array[5] as Recipe?
        val ingredientsMode = array[6] as Boolean

        val recipes = (localRecipes).distinctBy { it.title + it.sourceUrl }

        val filtered = if (query.isBlank()) {
            recipes
        } else {
            val queryWords = query.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            if (ingredientsMode) {
                recipes.filter { recipe ->
                    queryWords.all { word ->
                        recipe.title.contains(word, true) || recipe.ingredients.any { it.contains(word, true) }
                    }
                }
            } else {
                recipes.filter { recipe ->
                    queryWords.all { word -> recipe.title.contains(word, true) }
                }
            }
        }
        
        val afterTags = if (tagFilters.isNotEmpty()) {
            filtered.filter { recipe -> recipe.tags.containsAll(tagFilters) }
        } else {
            filtered
        }
        
        val sorted = if (alpha) {
            afterTags.sortedBy { recipe ->
                recipe.title.filter { it.isLetterOrDigit() }
            }
        } else {
            afterTags.sortedWith(compareByDescending<Recipe> { it.dateAdded }.thenByDescending { it.id })
        }

        val finalSorted = sorted.sortedByDescending { it.isFavorite }

        if (query.isBlank() && tagFilters.isEmpty() && lastCooked != null) {
            finalSorted.filter { it.id != lastCooked.id }
        } else {
            finalSorted
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val shoppingList: StateFlow<List<ShoppingItem>> = shoppingDao.getAllItemsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        importBundledRecipesOnAppUpdate()
        syncWithRemoteSystemRecipes()
        cleanupOldUpdates()
        checkForAppUpdate(application)
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
                    name = scaleNumbersAndPluralize(ing, multiplier),
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
        } catch (_: Exception) {
            // No action needed
        }
    }

    private fun scaleNumbersAndPluralize(text: String, multiplier: Float): String {
        if (multiplier == 1f) return text
        
        val fractions = mapOf(
            "חצי" to 0.5f,
            "רבע" to 0.25f,
            "שליש" to 0.33f,
            "שני שליש" to 0.66f,
            "שלושת רבעי" to 0.75f,
            "שלוש ורבע" to 3.25f,
            "שלושה רבעים" to 0.75f,
            "כוס וחצי" to 1.5f,
            "כפית וחצי" to 1.5f,
            "כף וחצי" to 1.5f,
            "חצי כוס" to 0.5f,
            "רבע כוס" to 0.25f
        )

        val units = listOf(
            "כפית" to "כפיות",
            "כף" to "כפות",
            "כוס" to "כוסות",
            "חבילה" to "חבילות",
            "קופסה" to "קופסאות",
            "קופסא" to "קופסאות",
            "מיכל" to "מיכלים",
            "מכל" to "מכלים",
            "שקית" to "שקיות",
            "צרור" to "צרורות",
            "גביע" to "גביעים"
        )

        var resultText = text
        
        // בדיקה אם הטקסט מכיל שברי מילים מורכבים קודם (הארוכים ביותר)
        val sortedFractions = fractions.keys.sortedByDescending { it.length }
        var baseValue: Float? = null
        for (word in sortedFractions) {
            if (resultText.contains(word)) {
                baseValue = fractions[word]
                val scaled = baseValue!! * multiplier
                val scaledStr = if (scaled % 1.0f == 0f) scaled.toInt().toString() else "%.1f".format(scaled)
                resultText = resultText.replace(word, scaledStr)
                break
            }
        }

        val numberRegex = Regex("""\b(\d+(\.\d+)?)\b""")
        val matches = numberRegex.findAll(resultText).toList()
        
        if (matches.isEmpty() && baseValue == null) {
            if (multiplier > 1f) {
                units.forEach { (singular, plural) ->
                    if (resultText.contains(singular) && !resultText.contains(plural)) {
                        resultText = resultText.replace(singular, plural)
                    }
                }
            }
            val multStr = if (multiplier % 1.0f == 0f) multiplier.toInt().toString() else "%.1f".format(multiplier)
            return "$multStr x $resultText"
        }

        // אם מצאנו מספר (או שהחלפנו מילה במספר), נבצע התאמת רבים/יחיד
        val finalNum = matches.firstOrNull()?.value?.toFloatOrNull() ?: (baseValue?.times(multiplier)) ?: 0f
        
        units.forEach { (singular, plural) ->
            if (finalNum == 1f) {
                if (resultText.contains(plural)) resultText = resultText.replace(plural, singular)
            } else if (finalNum > 1f) {
                if (resultText.contains(singular) && !resultText.contains(plural)) {
                    resultText = resultText.replace(singular, plural)
                }
            }
        }

        // החלפת שאר המספרים בטקסט (אם יש) - למעט זה שכבר טיפלנו בו אם הוא הגיע ממילה
        return numberRegex.replace(resultText) { matchResult ->
            val num = matchResult.value.toFloatOrNull() ?: return@replace matchResult.value
            // אם המספר הזה הוא תוצאה של החלפת מילה, הוא כבר מוכפל. אם לא, נכפיל אותו.
            val alreadyScaled = baseValue != null && matchResult.value == (baseValue * multiplier).let { if (it % 1.0f == 0f) it.toInt().toString() else "%.1f".format(it) }
            
            val scaled = if (alreadyScaled) num else num * multiplier
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
        prefs.edit { putBoolean("sort_alpha", newValue) }
    }
    fun toggleFavorite(recipe: Recipe) { 
        if (recipe.id == -2L) return // Cannot favorite remote group recipes for now
        viewModelScope.launch { repository.saveRecipe(recipe.copy(isFavorite = !recipe.isFavorite)) } 
    }
    fun saveRecipe(recipe: Recipe) {
        viewModelScope.launch {
            repository.saveRecipe(recipe.copy(id = 0L))
        }
    }
    
    fun saveManualRecipe(
        title: String,
        ingredients: String,
        steps: String,
        tags: List<String>,
        imageUrl: String?,
        sourceUrl: String = "הזנה ידנית"
    ) {
        val newRecipe = Recipe(
            id = 0L,
            title = title.takeIf { it.isNotBlank() } ?: "מתכון חדש",
            ingredients = ingredients.split("\n").map { it.trim() }.filter { it.isNotBlank() },
            steps = steps.split("\n").map { it.trim() }.filter { it.isNotBlank() },
            imageUrl = imageUrl,
            sourceUrl = sourceUrl,
            dateAdded = System.currentTimeMillis(),
            isFavorite = false,
            tags = tags
        )
        saveRecipe(newRecipe)
    }
    fun extractRecipe(query: String? = null) {
        val input = (query ?: _urlInput.value).trim()
        if (input.isBlank()) { 
            _recipeUiState.value = RecipeUiState.Error("אנא הזן חיפוש או קישור")
            return 
        }
        
        if (query == null) _urlInput.value = ""
        _recipeUiState.value = RecipeUiState.Loading
        
        viewModelScope.launch {
            runCatching { 
                withContext(Dispatchers.IO) { 
                    if (input.startsWith("http")) {
                        repository.extractRecipeFromUrl(input)
                    } else {
                        val finalLink = searchMakoOnGoogle(input) ?: throw Exception("לא נמצא מתכון מתאים לחיפוש זה")
                        repository.extractRecipeFromUrl(finalLink)
                    }
                } 
            }
                .onSuccess { recipe -> 
                    _selectedRecipe.value = recipe
                    _recipeUiState.value = RecipeUiState.Success(recipe) 
                }
                .onFailure { throwable -> 
                    _recipeUiState.value = RecipeUiState.Error(throwable.message?.takeIf { it.isNotBlank() } ?: "שגיאה בחילוץ המתכון")
                    // If search failed, log it as a request for developer
                    if (!input.startsWith("http")) {
                        suggestRecipeForDev(input)
                    }
                }
        }
    }

    suspend fun extractRecipeSuspend(input: String): Recipe? = withContext(Dispatchers.IO) {
        val trimmedInput = input.trim()
        if (trimmedInput.isBlank()) return@withContext null
        try {
            if (trimmedInput.startsWith("http")) {
                repository.extractRecipeFromUrl(trimmedInput)
            } else {
                val finalLink = searchMakoOnGoogle(trimmedInput)
                if (finalLink != null) {
                    repository.extractRecipeFromUrl(finalLink)
                } else null
            }
        } catch (_: Exception) {
            null
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
    fun deleteRecipe(recipeId: Long) {
        viewModelScope.launch {
            // No need for optimistic UI on savedRecipes as it's a Room StateFlow
            // However, we should also clear selectedRecipe if it matches
            if (_selectedRecipe.value?.id == recipeId) {
                _selectedRecipe.value = null
            }
            repository.deleteRecipe(recipeId)
        }
    }
    fun consumeUiState() { _recipeUiState.value = RecipeUiState.Idle }

    private fun importBundledRecipesOnAppUpdate() {
        viewModelScope.launch {
            val application = getApplication<Application>()
            val appLastUpdateTime = getAppLastUpdateTime(application)
            val lastImportedUpdateTime = updateImportPrefs.getLong(KEY_LAST_IMPORTED_UPDATE_TIME, 0L)
            if (appLastUpdateTime <= lastImportedUpdateTime) return@launch
            runCatching { withContext(Dispatchers.IO) { repository.importBundledRecipes(application) } }
                .onSuccess { updateImportPrefs.edit { putLong(KEY_LAST_IMPORTED_UPDATE_TIME, appLastUpdateTime) } }
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
                        _appUpdateMessage.value = "עדכון זמין"
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
            _downloadProgress.value = 0f
            runCatching {
                withContext(Dispatchers.IO) {
                    val apkFile = java.io.File(context.cacheDir, "update.apk")
                    val connection = java.net.URL(url).openConnection()
                    val totalSize = connection.contentLength.toLong()
                    
                    connection.getInputStream().use { input ->
                        apkFile.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var totalRead = 0L
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                totalRead += bytesRead
                                if (totalSize > 0) {
                                    _downloadProgress.value = totalRead.toFloat() / totalSize.toFloat()
                                }
                            }
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
                _downloadProgress.value = null
            }.onFailure {
                it.printStackTrace()
                _appUpdateMessage.value = "שגיאה בהורדת העדכון"
                _downloadProgress.value = null
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
            val context = getApplication<Application>().applicationContext
            
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
